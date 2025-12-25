package io.github.necrashter.natural_revenge.network.server;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

import io.github.necrashter.natural_revenge.network.NetworkConfig;
import io.github.necrashter.natural_revenge.network.messages.EntityMessages.*;
import io.github.necrashter.natural_revenge.network.messages.GameMessages.*;

/**
 * Server-side world state management.
 * Contains authoritative game world state for multiplayer.
 */
public class ServerWorld {
    private final GameServer server;
    
    // World properties
    private float worldSizeX = 100f;
    private float worldSizeZ = 100f;
    private float[][] heightMap;
    private int heightMapResolution = 32;
    
    // Entities
    private final IntMap<ServerEntity> entities = new IntMap<>();
    private int nextEntityID = 1;
    private final Object entityLock = new Object();
    
    // Spawn points
    private final Array<Vector2> spawnPoints = new Array<>();
    private int nextSpawnIndex = 0;
    
    // Game time
    private float gameTime = 0;
    
    // Level info
    private int levelID;
    private float easiness;
    private long seed;
    
    public ServerWorld(GameServer server, int levelID, float easiness, long seed) {
        this.server = server;
        this.levelID = levelID;
        this.easiness = easiness;
        this.seed = seed;
        
        // Initialize terrain (simplified flat terrain for now)
        initializeTerrain();
        
        // Initialize spawn points
        initializeSpawnPoints();
    }
    
    private void initializeTerrain() {
        heightMap = new float[heightMapResolution][heightMapResolution];
        
        // Generate simple terrain
        MathUtils.random.setSeed(seed);
        for (int x = 0; x < heightMapResolution; x++) {
            for (int z = 0; z < heightMapResolution; z++) {
                // Simple height variation
                heightMap[x][z] = MathUtils.random(-0.5f, 0.5f);
            }
        }
    }
    
    private void initializeSpawnPoints() {
        // Default spawn points around the map
        float margin = 10f;
        spawnPoints.add(new Vector2(margin, margin));
        spawnPoints.add(new Vector2(worldSizeX - margin, margin));
        spawnPoints.add(new Vector2(margin, worldSizeZ - margin));
        spawnPoints.add(new Vector2(worldSizeX - margin, worldSizeZ - margin));
        spawnPoints.add(new Vector2(worldSizeX / 2, margin));
        spawnPoints.add(new Vector2(worldSizeX / 2, worldSizeZ - margin));
        spawnPoints.add(new Vector2(margin, worldSizeZ / 2));
        spawnPoints.add(new Vector2(worldSizeX - margin, worldSizeZ / 2));
    }
    
    /**
     * Update world state
     */
    public void update(float delta) {
        gameTime += delta;
        
        // Update all entities
        synchronized (entityLock) {
            Array<Integer> toRemove = new Array<>();
            
            for (ServerEntity entity : entities.values()) {
                entity.update(delta, this);
                
                if (entity.shouldRemove()) {
                    toRemove.add(entity.getEntityID());
                }
            }
            
            // Remove dead entities
            for (int id : toRemove) {
                removeEntity(id, 1); // 1 = killed
            }
        }
        
        // Update players
        for (PlayerConnection conn : getConnections()) {
            ServerPlayer player = conn.getServerPlayer();
            if (player != null) {
                player.update(delta, this);
            }
        }
    }
    
    /**
     * Get terrain height at a position
     */
    public float getTerrainHeight(float x, float z) {
        // Convert world position to height map coordinates
        float hx = (x / worldSizeX) * (heightMapResolution - 1);
        float hz = (z / worldSizeZ) * (heightMapResolution - 1);
        
        int ix = MathUtils.clamp((int) hx, 0, heightMapResolution - 2);
        int iz = MathUtils.clamp((int) hz, 0, heightMapResolution - 2);
        
        float fx = hx - ix;
        float fz = hz - iz;
        
        // Bilinear interpolation
        float h00 = heightMap[ix][iz];
        float h10 = heightMap[ix + 1][iz];
        float h01 = heightMap[ix][iz + 1];
        float h11 = heightMap[ix + 1][iz + 1];
        
        float h0 = MathUtils.lerp(h00, h10, fx);
        float h1 = MathUtils.lerp(h01, h11, fx);
        
        return MathUtils.lerp(h0, h1, fz);
    }
    
    /**
     * Clamp X position to world bounds
     */
    public float clampX(float x, float margin) {
        return MathUtils.clamp(x, margin, worldSizeX - margin);
    }
    
    /**
     * Clamp Z position to world bounds
     */
    public float clampZ(float z, float margin) {
        return MathUtils.clamp(z, margin, worldSizeZ - margin);
    }
    
    /**
     * Get a spawn point for a new player
     */
    public Vector2 getSpawnPoint() {
        Vector2 spawn = spawnPoints.get(nextSpawnIndex);
        nextSpawnIndex = (nextSpawnIndex + 1) % spawnPoints.size;
        return spawn;
    }
    
    /**
     * Get a random spawn point
     */
    public Vector2 getRandomSpawnPoint() {
        return spawnPoints.get(MathUtils.random(spawnPoints.size - 1));
    }
    
    /**
     * Spawn an entity
     */
    public ServerEntity spawnEntity(int entityType, float x, float y, float z) {
        synchronized (entityLock) {
            int entityID = nextEntityID++;
            ServerEntity entity = new ServerEntity(entityID, entityType);
            entity.setPosition(x, y, z);
            entities.put(entityID, entity);
            
            // Broadcast entity creation
            EntityCreate create = new EntityCreate();
            create.entityID = entityID;
            create.entityType = entityType;
            create.positionX = x;
            create.positionY = y;
            create.positionZ = z;
            create.health = entity.getHealth();
            
            server.broadcast(create);
            
            return entity;
        }
    }
    
    /**
     * Remove an entity
     */
    public void removeEntity(int entityID, int reason) {
        synchronized (entityLock) {
            ServerEntity entity = entities.remove(entityID);
            if (entity != null) {
                // Broadcast removal
                server.broadcast(new EntityRemove(entityID, reason));
            }
        }
    }
    
    /**
     * Get an entity by ID
     */
    public ServerEntity getEntity(int entityID) {
        synchronized (entityLock) {
            return entities.get(entityID);
        }
    }
    
    /**
     * Create entity update batch for a client
     */
    public EntityUpdateBatch createEntityUpdateBatch(PlayerConnection client) {
        synchronized (entityLock) {
            if (entities.size == 0) return null;
            
            EntityUpdateBatch batch = new EntityUpdateBatch(Math.min(entities.size, 32));
            
            ServerPlayer player = client.getServerPlayer();
            Vector3 playerPos = player != null ? player.getPosition() : new Vector3();
            float viewDistance = 50f; // Server view distance
            
            for (ServerEntity entity : entities.values()) {
                if (batch.count >= batch.updates.length) break;
                
                // Interest management - only send nearby entities
                Vector3 entityPos = entity.getPosition();
                float dist2 = playerPos.dst2(entityPos);
                if (dist2 <= viewDistance * viewDistance) {
                    EntityUpdate update = new EntityUpdate();
                    update.entityID = entity.getEntityID();
                    update.serverTick = server.getServerTick();
                    update.setPosition(entityPos);
                    update.setVelocity(entity.getVelocity());
                    update.animationState = entity.getAnimationState();
                    
                    batch.updates[batch.count++] = update;
                }
            }
            
            return batch;
        }
    }
    
    /**
     * Create world snapshot for late joiners
     */
    public WorldSnapshot createWorldSnapshot(int serverTick, IntMap<PlayerConnection> connections) {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.serverTick = serverTick;
        snapshot.levelID = levelID;
        snapshot.easiness = easiness;
        snapshot.gameTime = gameTime;
        
        // Add player data
        int playerCount = connections.size;
        snapshot.playerIDs = new int[playerCount];
        snapshot.playerNames = new String[playerCount];
        snapshot.playerPositionsX = new float[playerCount];
        snapshot.playerPositionsY = new float[playerCount];
        snapshot.playerPositionsZ = new float[playerCount];
        snapshot.playerHealth = new float[playerCount];
        snapshot.playerTeams = new int[playerCount];
        
        int i = 0;
        for (PlayerConnection conn : connections.values()) {
            ServerPlayer player = conn.getServerPlayer();
            snapshot.playerIDs[i] = conn.getPlayerID();
            snapshot.playerNames[i] = conn.getPlayerName();
            if (player != null) {
                Vector3 pos = player.getPosition();
                snapshot.playerPositionsX[i] = pos.x;
                snapshot.playerPositionsY[i] = pos.y;
                snapshot.playerPositionsZ[i] = pos.z;
                snapshot.playerHealth[i] = player.getHealth();
                snapshot.playerTeams[i] = player.getTeam();
            }
            i++;
        }
        
        // Add entity data
        synchronized (entityLock) {
            int entityCount = entities.size;
            snapshot.entityIDs = new int[entityCount];
            snapshot.entityTypes = new int[entityCount];
            snapshot.entityPositionsX = new float[entityCount];
            snapshot.entityPositionsY = new float[entityCount];
            snapshot.entityPositionsZ = new float[entityCount];
            snapshot.entityHealth = new float[entityCount];
            
            i = 0;
            for (ServerEntity entity : entities.values()) {
                snapshot.entityIDs[i] = entity.getEntityID();
                snapshot.entityTypes[i] = entity.getEntityType();
                Vector3 pos = entity.getPosition();
                snapshot.entityPositionsX[i] = pos.x;
                snapshot.entityPositionsY[i] = pos.y;
                snapshot.entityPositionsZ[i] = pos.z;
                snapshot.entityHealth[i] = entity.getHealth();
                i++;
            }
        }
        
        return snapshot;
    }
    
    private Iterable<PlayerConnection> getConnections() {
        // This would normally get connections from the server
        // For now, return empty array
        return new Array<>();
    }
    
    /**
     * Get all entities in the world
     */
    public Iterable<ServerEntity> getEntities() {
        synchronized (entityLock) {
            return new Array<>(entities.values().toArray());
        }
    }
    
    // Getters
    public float getWorldSizeX() { return worldSizeX; }
    public float getWorldSizeZ() { return worldSizeZ; }
    public float getGameTime() { return gameTime; }
    public int getLevelID() { return levelID; }
    public float getEasiness() { return easiness; }
    public long getSeed() { return seed; }
    
    // Setters
    public void setWorldSize(float x, float z) {
        this.worldSizeX = x;
        this.worldSizeZ = z;
    }
}
