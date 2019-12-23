package net.gegy1000.earth.server.world.composer;

import net.gegy1000.earth.server.world.EarthDataKeys;
import net.gegy1000.earth.server.world.biome.BiomeClassifier;
import net.gegy1000.earth.server.world.cover.Cover;
import net.gegy1000.earth.server.world.geography.Landform;
import net.gegy1000.terrarium.server.util.ArrayUtils;
import net.gegy1000.terrarium.server.world.composer.biome.BiomeComposer;
import net.gegy1000.terrarium.server.world.data.ColumnData;
import net.gegy1000.terrarium.server.world.data.raster.EnumRaster;
import net.gegy1000.terrarium.server.world.data.raster.FloatRaster;
import net.gegy1000.terrarium.server.world.data.raster.ShortRaster;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;

import java.util.Optional;

public final class EarthBiomeComposer implements BiomeComposer {
    private final Biome[] biomeBuffer = new Biome[16 * 16];

    private final BiomeClassifier.Context context = new BiomeClassifier.Context();

    @Override
    public Biome[] composeBiomes(ColumnData data, ChunkPos columnPos) {
        Optional<ShortRaster> elevationOption = data.get(EarthDataKeys.ELEVATION_METERS);
        Optional<EnumRaster<Cover>> coverOption = data.get(EarthDataKeys.COVER);
        Optional<EnumRaster<Landform>> landformOption = data.get(EarthDataKeys.LANDFORM);
        Optional<FloatRaster> minTemperatureOption = data.get(EarthDataKeys.MIN_TEMPERATURE);
        Optional<FloatRaster> meanTemperatureOption = data.get(EarthDataKeys.MEAN_TEMPERATURE);
        Optional<ShortRaster> rainfallOption = data.get(EarthDataKeys.ANNUAL_RAINFALL);

        if (!elevationOption.isPresent() || !coverOption.isPresent()
                || !minTemperatureOption.isPresent() || !meanTemperatureOption.isPresent()
                || !rainfallOption.isPresent() || !landformOption.isPresent()
        ) {
            return ArrayUtils.fill(this.biomeBuffer, Biomes.DEFAULT);
        }

        ShortRaster elevationRaster = elevationOption.get();
        EnumRaster<Cover> coverRaster = coverOption.get();
        EnumRaster<Landform> landformRaster = landformOption.get();
        FloatRaster minTemperatureRaster = minTemperatureOption.get();
        FloatRaster meanTemperatureRaster = meanTemperatureOption.get();
        ShortRaster rainfallRaster = rainfallOption.get();

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                this.context.elevation = elevationRaster.get(x, z);
                this.context.annualRainfall = rainfallRaster.get(x, z);
                this.context.minTemperature = minTemperatureRaster.get(x, z);
                this.context.meanTemperature = meanTemperatureRaster.get(x, z);
                this.context.cover = coverRaster.get(x, z);
                this.context.landform = landformRaster.get(x, z);

                this.biomeBuffer[x + z * 16] = BiomeClassifier.classify(this.context);
            }
        }

        return this.biomeBuffer;
    }
}