package net.gegy1000.earth.server.world.data.source;

import net.gegy1000.earth.TerrariumEarth;
import net.gegy1000.earth.server.shared.SharedEarthData;
import net.gegy1000.earth.server.world.data.index.EarthRemoteIndex;
import net.gegy1000.earth.server.world.data.source.cache.AbstractRegionKey;
import net.gegy1000.earth.server.world.data.source.cache.CachingInput;
import net.gegy1000.earth.server.world.data.source.cache.FileTileCache;
import net.gegy1000.terrarium.server.util.Vec2i;
import net.gegy1000.terrarium.server.world.coordinate.CoordinateReference;
import net.gegy1000.terrarium.server.world.data.raster.UByteRaster;
import net.gegy1000.terrarium.server.world.data.source.TiledDataSource;
import org.apache.commons.io.IOUtils;
import org.tukaani.xz.SingleXZInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class LandCoverSource extends TiledDataSource<UByteRaster> {
    public static final int TILE_SIZE = 1800;
    public static final int GLOBAL_WIDTH = 129600;
    public static final int GLOBAL_HEIGHT = 64800;

    //    private static final TileCache<Key> CACHE = RegionTileCache.<Key>builder()
//            .keyProvider(new KeyProvider())
//            .inDirectory(GLOBAL_CACHE_ROOT.resolve("landcover"))
//            .sectorSize(128 * 1024)
//            .build();

    private static final Path CACHE_ROOT = GLOBAL_CACHE_ROOT.resolve("landcover");
    private static final FileTileCache<Vec2i> CACHE = new FileTileCache<>(pos -> CACHE_ROOT.resolve(pos.x + "/" + pos.y));

    private static final CachingInput<Vec2i> CACHING_INPUT = new CachingInput<>(CACHE);

    public LandCoverSource(CoordinateReference crs) {
        super(crs, TILE_SIZE);
    }

    @Override
    public Optional<UByteRaster> load(Vec2i pos) throws IOException {
        SharedEarthData sharedData = SharedEarthData.instance();
        EarthRemoteIndex remoteIndex = sharedData.get(SharedEarthData.REMOTE_INDEX);
        if (remoteIndex == null) {
            return Optional.empty();
        }

        String url = remoteIndex.landcover.getUrlFor(pos);
        if (url == null) {
            return Optional.empty();
        }

        InputStream sourceInput = CACHING_INPUT.getInputStream(pos, p -> {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", TerrariumEarth.USER_AGENT);
            return connection.getInputStream();
        });

        try (InputStream input = new SingleXZInputStream(new BufferedInputStream(sourceInput))) {
            return Optional.of(this.parseStream(input));
        }
    }

    private UByteRaster parseStream(InputStream input) throws IOException {
        byte[] bytes = IOUtils.readFully(input, TILE_SIZE * TILE_SIZE);

        UByteRaster raster = UByteRaster.create(TILE_SIZE, TILE_SIZE);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                byte id = bytes[x + y * TILE_SIZE];
                raster.set(x, y, id);
            }
        }

        return raster;
    }

    private static final int LOC_BITS = 2;

    private static class Key extends AbstractRegionKey<Key> {
        Key(int x, int z) {
            super(x, z);
        }

        @Override
        protected int bits() {
            return LOC_BITS;
        }
    }

    private static class KeyProvider extends AbstractRegionKey.Provider<Key> {
        @Override
        protected Key create(int x, int z) {
            return new Key(x, z);
        }

        @Override
        protected int bits() {
            return LOC_BITS;
        }
    }
}
