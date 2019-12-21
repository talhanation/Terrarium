package net.gegy1000.terrarium.server.world.chunk;

import net.gegy1000.terrarium.server.capability.TerrariumCapabilities;
import net.gegy1000.terrarium.server.capability.TerrariumWorldData;
import net.gegy1000.terrarium.server.util.Lazy;
import net.gegy1000.terrarium.server.world.generator.ChunkCompositionProcedure;
import net.gegy1000.terrarium.server.world.region.RegionGenerationHandler;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ComposableChunkGenerator implements TerrariumChunkGenerator {
    private final World world;
    private final Random random;

    private final Lazy<Optional<TerrariumWorldData>> terrarium;

    private final Biome[] biomeBuffer = new Biome[16 * 16];

    public ComposableChunkGenerator(World world) {
        this.world = world;
        this.random = new Random(world.getWorldInfo().getSeed());

        this.terrarium = Lazy.ofCapability(world, TerrariumCapabilities.worldDataCapability);
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        ChunkPrimer primer = this.generatePrimer(chunkX, chunkZ);

        Chunk chunk = new Chunk(this.world, primer, chunkX, chunkZ);

        Biome[] biomeBuffer = this.provideBiomes(chunkX, chunkZ);

        byte[] biomeArray = chunk.getBiomeArray();
        for (int i = 0; i < biomeBuffer.length; i++) {
            biomeArray[i] = (byte) Biome.getIdForBiome(biomeBuffer[i]);
        }

        chunk.generateSkylightMap();

        return chunk;
    }

    public ChunkPrimer generatePrimer(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();
        this.populateTerrain(chunkX, chunkZ, primer);

        this.terrarium.get().ifPresent(terrarium -> {
            RegionGenerationHandler regionHandler = terrarium.getRegionHandler();
            ChunkCompositionProcedure compositionProcedure = terrarium.getCompositionProcedure();
            compositionProcedure.composeStructures(this, primer, regionHandler, chunkX, chunkZ);
        });

        return primer;
    }

    @Override
    public void populateTerrain(int chunkX, int chunkZ, ChunkPrimer primer) {
        this.terrarium.get().ifPresent(terrarium -> {
            RegionGenerationHandler regionHandler = terrarium.getRegionHandler();
            regionHandler.prepareChunk(chunkX << 4, chunkZ << 4);

            ChunkCompositionProcedure compositionProcedure = terrarium.getCompositionProcedure();
            compositionProcedure.composeSurface(this, primer, regionHandler, chunkX, chunkZ);
        });
    }

    public Biome[] provideBiomes(int chunkX, int chunkZ) {
        return this.world.getBiomeProvider().getBiomes(this.biomeBuffer, chunkX << 4, chunkZ << 4, 16, 16);
    }

    @Override
    public void populate(int chunkX, int chunkZ) {
        this.terrarium.get().ifPresent(terrarium -> {
            int globalX = chunkX << 4;
            int globalZ = chunkZ << 4;

            this.random.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);

            BlockFalling.fallInstantly = true;

            RegionGenerationHandler regionHandler = terrarium.getRegionHandler();
            regionHandler.prepareChunk(globalX + 8, globalZ + 8);

            ForgeEventFactory.onChunkPopulate(true, this, this.world, this.random, chunkX, chunkZ, false);

            ChunkCompositionProcedure compositionProcedure = terrarium.getCompositionProcedure();
            compositionProcedure.composeDecoration(this, this.world, regionHandler, chunkX, chunkZ);
            compositionProcedure.populateStructures(this.world, regionHandler, chunkX, chunkZ);

            ForgeEventFactory.onChunkPopulate(false, this, this.world, this.random, chunkX, chunkZ, false);

            BlockFalling.fallInstantly = false;
        });
    }

    @Override
    public boolean generateStructures(Chunk chunk, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return this.world.getBiome(pos).getSpawnableList(creatureType);
    }

    @Override
    public BlockPos getNearestStructurePos(World world, String structureName, BlockPos pos, boolean findUnexplored) {
        return this.terrarium.get().map(terrarium -> {
            ChunkCompositionProcedure compositionProcedure = terrarium.getCompositionProcedure();
            return compositionProcedure.getNearestStructure(this.world, structureName, pos, findUnexplored);
        }).orElse(null);
    }

    @Override
    public boolean isInsideStructure(World world, String structureName, BlockPos pos) {
        Optional<TerrariumWorldData> terrariumOption = this.terrarium.get();
        if (terrariumOption.isPresent()) {
            ChunkCompositionProcedure compositionProcedure = terrariumOption.get().getCompositionProcedure();
            return compositionProcedure.isInsideStructure(this.world, structureName, pos);
        }
        return false;
    }

    @Override
    public void recreateStructures(Chunk chunk, int chunkX, int chunkZ) {
        this.terrarium.get().ifPresent(terrarium -> {
            RegionGenerationHandler regionHandler = terrarium.getRegionHandler();
            ChunkCompositionProcedure compositionProcedure = terrarium.getCompositionProcedure();
            compositionProcedure.composeStructures(this, null, regionHandler, chunkX, chunkZ);
        });
    }
}
