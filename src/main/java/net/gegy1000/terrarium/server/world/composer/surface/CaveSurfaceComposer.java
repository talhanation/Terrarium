package net.gegy1000.terrarium.server.world.composer.surface;

import net.gegy1000.gengen.api.CubicPos;
import net.gegy1000.gengen.api.generator.GenericChunkPrimer;
import net.gegy1000.gengen.api.writer.ChunkPrimeWriter;
import net.gegy1000.gengen.util.primer.GenericCavePrimer;
import net.gegy1000.gengen.util.primer.GenericRavinePrimer;
import net.gegy1000.terrarium.server.world.data.ColumnData;
import net.minecraft.world.World;

public class CaveSurfaceComposer implements SurfaceComposer {
    private final GenericChunkPrimer caveGenerator;
    private final GenericChunkPrimer ravineGenerator;

    public CaveSurfaceComposer(World world) {
        this.caveGenerator = new GenericCavePrimer(world);
        this.ravineGenerator = new GenericRavinePrimer(world, world.getSeaLevel());
    }

    @Override
    public void composeSurface(ColumnData data, CubicPos pos, ChunkPrimeWriter writer) {
        this.caveGenerator.primeChunk(pos, writer);
        this.ravineGenerator.primeChunk(pos, writer);
    }
}