package net.gegy1000.earth.server.world.data.source;

import net.gegy1000.terrarium.server.world.coordinate.CoordinateReference;
import org.tukaani.xz.SingleXZInputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static net.gegy1000.earth.server.world.EarthWorldType.CLIMATE_SCALE;

public final class WorldClimateRaster {
    public static final int WIDTH = 4320;
    public static final int HEIGHT = 2160;

    private static final float STANDARD_TEMPERATURE = 14.0F;
    private static final short STANDARD_RAINFALL = 600;

    private static final int PACK_RANGE = 255;

    private static final float TEMP_MIN = -40.0F;
    private static final float TEMP_MAX = 45.0F;
    private static final float TEMP_RANGE = TEMP_MAX - TEMP_MIN;
    private static final float TEMP_CURVE = 1.0F;

    private static final float RAIN_MIN = 0.0F;
    private static final float RAIN_RANGE = 7200.0F;
    private static final float RAIN_CURVE = 2.3F;

    private final byte[] meanTemperature;
    private final byte[] minTemperature;
    private final byte[] annualRainfall;

    private WorldClimateRaster(byte[] meanTemperature, byte[] minTemperature, byte[] annualRainfall) {
        this.meanTemperature = meanTemperature;
        this.minTemperature = minTemperature;
        this.annualRainfall = annualRainfall;
    }

    public static WorldClimateRaster parse(InputStream in) throws IOException {
        DataInputStream data = new DataInputStream(new SingleXZInputStream(in));

        byte[] meanTemperature = new byte[WIDTH * HEIGHT];
        data.readFully(meanTemperature);
        byte[] minTemperature = new byte[WIDTH * HEIGHT];
        data.readFully(minTemperature);

        byte[] annualRainfall = new byte[WIDTH * HEIGHT];
        data.readFully(annualRainfall);

        return new WorldClimateRaster(meanTemperature, minTemperature, annualRainfall);
    }

    public static CoordinateReference crs(double worldScale) {
        double scale = CLIMATE_SCALE / worldScale;
        return CoordinateReference.scaleAndOffset(scale, scale, -WIDTH / 2.0, -HEIGHT / 2.0);
    }

    public float getMeanTemperature(int x, int y) {
        if (outOfBounds(x, y)) return STANDARD_TEMPERATURE;

        byte packed = this.meanTemperature[index(x, y)];
        return this.unpackTemperature(packed);
    }

    public float getMinTemperature(int x, int y) {
        if (outOfBounds(x, y)) return STANDARD_TEMPERATURE;

        byte packed = this.minTemperature[index(x, y)];
        return this.unpackTemperature(packed);
    }

    public short getAnnualRainfall(int x, int y) {
        if (outOfBounds(x, y)) return STANDARD_RAINFALL;

        byte packed = this.annualRainfall[index(x, y)];
        return this.unpackRainfall(packed);
    }

    private float unpackTemperature(byte packed) {
        float shifted = (float) (packed & 0xFF);
        double unpacked = TEMP_RANGE * Math.pow(shifted / PACK_RANGE, TEMP_CURVE);
        return (float) (TEMP_MIN + unpacked);
    }

    private short unpackRainfall(byte packed) {
        float shifted = (float) (packed & 0xFF);
        double unpacked = RAIN_RANGE * Math.pow(shifted / PACK_RANGE, RAIN_CURVE);
        return (short) (RAIN_MIN + unpacked);
    }

    private static int index(int x, int y) {
        return x + y * WIDTH;
    }

    private static boolean outOfBounds(int x, int y) {
        return x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }
}
