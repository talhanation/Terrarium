package net.gegy1000.terrarium.server.world.chunk;

import net.gegy1000.terrarium.server.capability.TerrariumCapabilities;
import net.gegy1000.terrarium.server.capability.TerrariumWorldData;
import net.gegy1000.terrarium.server.util.Lazy;
import net.gegy1000.terrarium.server.world.generator.ChunkCompositionProcedure;
import net.gegy1000.terrarium.server.world.region.RegionGenerationHandler;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ComposableBiomeProvider extends BiomeProvider {
    private final Lazy<Optional<TerrariumWorldData>> terrarium;

    private final BiomeCache biomeCache = new BiomeCache(this);

    public ComposableBiomeProvider(World world) {
        this.terrarium = Lazy.ofCapability(world, TerrariumCapabilities.worldDataCapability);
    }

    @Override
    public List<Biome> getBiomesToSpawnIn() {
        return allowedBiomes;
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        return this.biomeCache.getBiome(pos.getX(), pos.getZ(), defaultBiome);
    }

    @Override
    public float getTemperatureAtHeight(float biomeTemperature, int height) {
        return biomeTemperature;
    }

    @Override
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        if (biomes == null || biomes.length < width * height) {
            biomes = new Biome[width * height];
        }
        Arrays.fill(biomes, Biomes.DEFAULT);
        return biomes;
    }

    @Override
    public Biome[] getBiomes(Biome[] biomes, int x, int z, int width, int height, boolean cache) {
        if (biomes == null || biomes.length < width * height) {
            biomes = new Biome[width * height];
        }

        boolean fillsChunk = this.isChunkGeneration(x, z, width, height);
        if (cache && fillsChunk) {
            System.arraycopy(this.biomeCache.getCachedBiomes(x, z), 0, biomes, 0, width * height);
        } else {
            this.populateArea(biomes, x, z, width, height);
        }

        return biomes;
    }

    @Override
    public boolean areBiomesViable(int originX, int originZ, int radius, List<Biome> allowed) {
        int minX = originX - radius;
        int minZ = originZ - radius;
        int size = (radius * 2) + 1;

        Biome[] biomes = new Biome[size * size];
        this.populateArea(biomes, minX, minZ, size, size);
        for (Biome biome : biomes) {
            if (!allowed.contains(biome)) {
                return false;
            }
        }

        return true;
    }

    // TODO: Implement properly
    @Override
    public BlockPos findBiomePosition(int originX, int originZ, int radius, List<Biome> allowed, Random random) {
        return new BlockPos(originX, 0, originZ);
    }

    private void populateArea(Biome[] biomes, int x, int z, int width, int height) {
        Optional<TerrariumWorldData> terrariumOption = this.terrarium.get();
        if (!terrariumOption.isPresent()) {
            Arrays.fill(biomes, Biomes.PLAINS);
            return;
        }

        TerrariumWorldData terrarium = terrariumOption.get();
        ChunkCompositionProcedure compositionProcedure = terrarium.getCompositionProcedure();
        RegionGenerationHandler regionHandler = terrarium.getRegionHandler();

        if (this.isChunkGeneration(x, z, width, height)) {
            regionHandler.prepareChunk(x, z, compositionProcedure.getBiomeDependencies());
            Biome[] biomeBuffer = compositionProcedure.composeBiomes(regionHandler, x >> 4, z >> 4);
            System.arraycopy(biomeBuffer, 0, biomes, 0, biomeBuffer.length);
            return;
        }

        int chunkMinX = x >> 4;
        int chunkMinZ = z >> 4;
        int chunkMaxX = (x + width) >> 4;
        int chunkMaxZ = (z + height) >> 4;

        for (int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
            for (int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
                int minChunkX = chunkX << 4;
                int minChunkZ = chunkZ << 4;

                int minX = Math.max(minChunkX, x);
                int maxX = Math.min((chunkX + 1) << 4, x + width);
                int minZ = Math.max(minChunkZ, z);
                int maxZ = Math.min((chunkZ + 1) << 4, z + height);

                regionHandler.prepareChunk(minChunkX, minChunkZ, compositionProcedure.getBiomeDependencies());
                Biome[] biomeBuffer = compositionProcedure.composeBiomes(regionHandler, chunkX, chunkZ);

                for (int localZ = minZ; localZ < maxZ; localZ++) {
                    int srcPos = (localZ - minChunkZ) * 16;
                    int destPos = (localZ - z) * width;
                    System.arraycopy(biomeBuffer, srcPos, biomes, destPos, maxX - minX);
                }
            }
        }
    }

    private boolean isChunkGeneration(int x, int z, int width, int height) {
        return width == 16 && height == 16 && (x & 15) == 0 && (z & 15) == 0;
    }

    @Override
    public void cleanupCache() {
        this.biomeCache.cleanupCache();
    }
}
