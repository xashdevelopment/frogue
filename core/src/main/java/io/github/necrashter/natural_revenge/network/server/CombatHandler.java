package io.github.necrashter.natural_revenge.network.server;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

import io.github.necrashter.natural_revenge.network.messages.EntityMessages.*;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.*;
import io.github.necrashter.natural_revenge.modes.GameMode;

/**
 * Server-side combat handling.
 * Processes hit detection, damage application, and combat events.
 */
public class CombatHandler {
    private final GameServer server;
    private final ServerWorld world;
    
    // Projectiles in flight
    private final IntMap<ServerProjectile> projectiles = new IntMap<>();
    private int nextProjectileID = 1;
    
    // Damage types
    public static final int DAMAGE_BULLET = 0;
    public static final int DAMAGE_MELEE = 1;
    public static final int DAMAGE_EXPLOSION = 2;
    public static final int DAMAGE_ENVIRONMENT = 3;
    
    public CombatHandler(GameServer server, ServerWorld world) {
        this.server = server;
        this.world = world;
    }
    
    /**
     * Update projectiles and pending combat
     */
    public void update(float delta) {
        updateProjectiles(delta);
    }
    
    /**
     * Process a hitscan weapon fire from a player
     */
    public void processHitscan(ServerPlayer attacker, Vector3 origin, Vector3 direction, 
                                float range, float damage, float spread) {
        // Apply spread
        if (spread > 0) {
            direction.x += (float)(Math.random() - 0.5) * spread;
            direction.y += (float)(Math.random() - 0.5) * spread * 0.5f;
            direction.z += (float)(Math.random() - 0.5) * spread;
            direction.nor();
        }
        
        // Ray trace for hit detection
        HitResult hit = rayTrace(origin, direction, range, attacker.getPlayerID());
        
        if (hit != null) {
            applyHit(attacker, hit, damage, DAMAGE_BULLET);
        }
    }
    
    /**
     * Spawn a projectile (rockets, grenades, etc.)
     */
    public void spawnProjectile(ServerPlayer owner, Vector3 origin, Vector3 direction,
                                 float speed, float damage, int projectileType) {
        int projectileID = nextProjectileID++;
        
        ServerProjectile projectile = new ServerProjectile(
            projectileID, owner.getPlayerID(), origin, direction, speed, damage, projectileType
        );
        projectiles.put(projectileID, projectile);
        
        // Broadcast projectile spawn
        ProjectileSpawn spawn = new ProjectileSpawn();
        spawn.projectileID = projectileID;
        spawn.ownerPlayerID = owner.getPlayerID();
        spawn.startX = origin.x;
        spawn.startY = origin.y;
        spawn.startZ = origin.z;
        spawn.directionX = direction.x;
        spawn.directionY = direction.y;
        spawn.directionZ = direction.z;
        spawn.speed = speed;
        spawn.damage = damage;
        spawn.projectileType = projectileType;
        
        server.broadcast(spawn);
    }
    
    /**
     * Update all projectiles
     */
    private void updateProjectiles(float delta) {
        Array<Integer> toRemove = new Array<>();
        
        for (ServerProjectile proj : projectiles.values()) {
            proj.update(delta);
            
            // Check for hits
            HitResult hit = rayTrace(
                proj.getLastPosition(), proj.getPosition(), 
                proj.getOwnerID()
            );
            
            if (hit != null || proj.isExpired()) {
                // Projectile hit something or expired
                broadcastProjectileHit(proj, hit);
                
                if (hit != null) {
                    ServerPlayer owner = findPlayer(proj.getOwnerID());
                    applyHit(owner, hit, proj.getDamage(), 
                             proj.getProjectileType() == 1 ? DAMAGE_EXPLOSION : DAMAGE_BULLET);
                }
                
                toRemove.add(proj.getProjectileID());
            }
        }
        
        for (int id : toRemove) {
            projectiles.remove(id);
        }
    }
    
    /**
     * Ray trace for hit detection
     */
    private HitResult rayTrace(Vector3 origin, Vector3 direction, float maxDist, int ignorePlayerID) {
        Vector3 end = new Vector3(direction).scl(maxDist).add(origin);
        return rayTrace(origin, end, ignorePlayerID);
    }
    
    /**
     * Ray trace between two points
     */
    private HitResult rayTrace(Vector3 start, Vector3 end, int ignorePlayerID) {
        float closestDist = Float.MAX_VALUE;
        HitResult closestHit = null;
        
        Vector3 direction = new Vector3(end).sub(start);
        float maxDist = direction.len();
        direction.nor();
        
        // Check players
        for (PlayerConnection conn : getConnections()) {
            if (conn.getPlayerID() == ignorePlayerID) continue;
            
            ServerPlayer player = conn.getServerPlayer();
            if (player == null || player.isDead()) continue;
            
            // Check friendly fire
            GameMode mode = server.getGameMode();
            if (mode != null && !mode.isFriendlyFireEnabled()) {
                ServerPlayer attacker = findPlayer(ignorePlayerID);
                if (attacker != null && attacker.getTeam() == player.getTeam()) {
                    continue;
                }
            }
            
            // Simple sphere intersection for player hitbox
            float hitDist = sphereRayIntersect(player.getPosition(), 0.5f, start, direction);
            if (hitDist > 0 && hitDist < maxDist && hitDist < closestDist) {
                closestDist = hitDist;
                closestHit = new HitResult(HitResult.TYPE_PLAYER, player.getPlayerID(), 
                                           new Vector3(direction).scl(hitDist).add(start));
            }
        }
        
        // Check entities
        for (ServerEntity entity : world.getEntities()) {
            float hitDist = sphereRayIntersect(entity.getPosition(), 0.5f, start, direction);
            if (hitDist > 0 && hitDist < maxDist && hitDist < closestDist) {
                closestDist = hitDist;
                closestHit = new HitResult(HitResult.TYPE_ENTITY, entity.getEntityID(),
                                           new Vector3(direction).scl(hitDist).add(start));
            }
        }
        
        // Check terrain
        float terrainDist = terrainRayIntersect(start, direction, maxDist);
        if (terrainDist > 0 && terrainDist < closestDist) {
            closestDist = terrainDist;
            closestHit = new HitResult(HitResult.TYPE_TERRAIN, -1,
                                       new Vector3(direction).scl(terrainDist).add(start));
        }
        
        return closestHit;
    }
    
    /**
     * Apply damage from a hit
     */
    private void applyHit(ServerPlayer attacker, HitResult hit, float damage, int damageType) {
        if (hit.hitType == HitResult.TYPE_PLAYER) {
            // Damage a player
            ServerPlayer victim = findPlayer(hit.entityID);
            if (victim != null) {
                boolean killed = victim.takeDamage(damage, attacker != null ? attacker.getPlayerID() : -1);
                
                // Broadcast damage event
                PlayerDamage dmgMsg = new PlayerDamage();
                dmgMsg.victimID = victim.getPlayerID();
                dmgMsg.attackerID = attacker != null ? attacker.getPlayerID() : -1;
                dmgMsg.damage = damage;
                dmgMsg.damageType = damageType;
                server.broadcast(dmgMsg);
                
                // Broadcast health update
                PlayerHealthUpdate healthMsg = new PlayerHealthUpdate(
                    victim.getPlayerID(), victim.getHealth(), victim.getMaxHealth()
                );
                server.broadcast(healthMsg);
                
                if (killed) {
                    handlePlayerKill(victim, attacker);
                }
            }
        } else if (hit.hitType == HitResult.TYPE_ENTITY) {
            // Damage an entity
            ServerEntity entity = world.getEntity(hit.entityID);
            if (entity != null) {
                boolean killed = entity.takeDamage(damage, attacker != null ? attacker.getPlayerID() : -1);
                
                // Broadcast damage event
                EntityDamage dmgMsg = new EntityDamage();
                dmgMsg.entityID = entity.getEntityID();
                dmgMsg.attackerID = attacker != null ? attacker.getPlayerID() : -1;
                dmgMsg.damageAmount = damage;
                dmgMsg.damageType = damageType;
                dmgMsg.hitX = hit.hitPoint.x;
                dmgMsg.hitY = hit.hitPoint.y;
                dmgMsg.hitZ = hit.hitPoint.z;
                server.broadcast(dmgMsg);
                
                if (killed) {
                    handleEntityKill(entity, attacker);
                }
            }
        }
    }
    
    /**
     * Handle a player being killed
     */
    private void handlePlayerKill(ServerPlayer victim, ServerPlayer killer) {
        // Notify game mode
        GameMode mode = server.getGameMode();
        if (mode != null) {
            mode.onPlayerKilled(victim, killer);
        }
        
        // Killer gets credit
        if (killer != null && killer != victim) {
            killer.addKill();
        }
    }
    
    /**
     * Handle an entity being killed
     */
    private void handleEntityKill(ServerEntity entity, ServerPlayer killer) {
        // Notify game mode
        GameMode mode = server.getGameMode();
        if (mode != null) {
            mode.onEntityKilled(entity, killer);
        }
        
        // Remove entity
        world.removeEntity(entity.getEntityID(), 1); // 1 = killed
    }
    
    /**
     * Broadcast projectile hit event
     */
    private void broadcastProjectileHit(ServerProjectile proj, HitResult hit) {
        ProjectileHit hitMsg = new ProjectileHit();
        hitMsg.projectileID = proj.getProjectileID();
        
        if (hit != null) {
            hitMsg.hitX = hit.hitPoint.x;
            hitMsg.hitY = hit.hitPoint.y;
            hitMsg.hitZ = hit.hitPoint.z;
            hitMsg.hitEntityID = hit.entityID;
            hitMsg.hitType = hit.hitType;
        } else {
            Vector3 pos = proj.getPosition();
            hitMsg.hitX = pos.x;
            hitMsg.hitY = pos.y;
            hitMsg.hitZ = pos.z;
            hitMsg.hitEntityID = -1;
            hitMsg.hitType = 0;
        }
        
        server.broadcast(hitMsg);
    }
    
    /**
     * Simple sphere-ray intersection
     */
    private float sphereRayIntersect(Vector3 center, float radius, Vector3 rayOrigin, Vector3 rayDir) {
        Vector3 oc = new Vector3(rayOrigin).sub(center);
        float a = rayDir.dot(rayDir);
        float b = 2.0f * oc.dot(rayDir);
        float c = oc.dot(oc) - radius * radius;
        float discriminant = b * b - 4 * a * c;
        
        if (discriminant < 0) return -1;
        
        float t = (-b - (float)Math.sqrt(discriminant)) / (2.0f * a);
        return t > 0 ? t : -1;
    }
    
    /**
     * Simplified terrain ray intersection
     */
    private float terrainRayIntersect(Vector3 origin, Vector3 direction, float maxDist) {
        // Step along ray checking terrain height
        float stepSize = 0.5f;
        for (float t = 0; t < maxDist; t += stepSize) {
            Vector3 pos = new Vector3(direction).scl(t).add(origin);
            float terrainHeight = world.getTerrainHeight(pos.x, pos.z);
            
            if (pos.y <= terrainHeight) {
                return t;
            }
        }
        return -1;
    }
    
    private ServerPlayer findPlayer(int playerID) {
        for (PlayerConnection conn : getConnections()) {
            if (conn.getPlayerID() == playerID) {
                return conn.getServerPlayer();
            }
        }
        return null;
    }
    
    private Iterable<PlayerConnection> getConnections() {
        // Get connections from server - simplified placeholder
        return new Array<>();
    }
    
    /**
     * Hit result data class
     */
    public static class HitResult {
        public static final int TYPE_TERRAIN = 0;
        public static final int TYPE_ENTITY = 1;
        public static final int TYPE_PLAYER = 2;
        
        public final int hitType;
        public final int entityID;
        public final Vector3 hitPoint;
        
        public HitResult(int hitType, int entityID, Vector3 hitPoint) {
            this.hitType = hitType;
            this.entityID = entityID;
            this.hitPoint = hitPoint;
        }
    }
    
    /**
     * Server-side projectile
     */
    private static class ServerProjectile {
        private final int projectileID;
        private final int ownerID;
        private final Vector3 position = new Vector3();
        private final Vector3 lastPosition = new Vector3();
        private final Vector3 velocity = new Vector3();
        private final float damage;
        private final int projectileType;
        private float lifetime = 0;
        private static final float MAX_LIFETIME = 5f;
        
        public ServerProjectile(int id, int ownerID, Vector3 start, Vector3 dir, 
                                 float speed, float damage, int type) {
            this.projectileID = id;
            this.ownerID = ownerID;
            this.position.set(start);
            this.lastPosition.set(start);
            this.velocity.set(dir).scl(speed);
            this.damage = damage;
            this.projectileType = type;
        }
        
        public void update(float delta) {
            lastPosition.set(position);
            position.mulAdd(velocity, delta);
            velocity.y -= 9.8f * delta; // Gravity
            lifetime += delta;
        }
        
        public boolean isExpired() {
            return lifetime >= MAX_LIFETIME;
        }
        
        public int getProjectileID() { return projectileID; }
        public int getOwnerID() { return ownerID; }
        public Vector3 getPosition() { return position; }
        public Vector3 getLastPosition() { return lastPosition; }
        public float getDamage() { return damage; }
        public int getProjectileType() { return projectileType; }
    }
}
