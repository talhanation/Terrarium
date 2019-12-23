package net.gegy1000.terrarium.server.world.data.source;

import net.gegy1000.terrarium.server.util.Vec2i;
import net.gegy1000.terrarium.server.world.coordinate.CoordinateReference;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class TiledDataSource<T> {
    public static final Path LEGACY_CACHE_ROOT = Paths.get(".", "mods/terrarium/cache");
    public static final Path GLOBAL_CACHE_ROOT = Paths.get(".", "mods/terrarium/cache2");

    protected final CoordinateReference crs;
    protected final double tileWidth;
    protected final double tileHeight;

    protected TiledDataSource(CoordinateReference crs, double tileWidth, double tileHeight) {
        this.crs = crs;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    protected TiledDataSource(CoordinateReference crs, double size) {
        this(crs, size, size);
    }

    public final CoordinateReference getCrs() {
        return this.crs;
    }

    public final double getTileWidth() {
        return this.tileWidth;
    }

    public final double getTileHeight() {
        return this.tileHeight;
    }

    public abstract Optional<T> load(Vec2i pos) throws IOException;
}