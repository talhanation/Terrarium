package net.gegy1000.earth.server.world.data.op;

import net.gegy1000.terrarium.server.world.data.DataOp;
import net.gegy1000.terrarium.server.world.data.raster.BitRaster;
import net.gegy1000.terrarium.server.world.rasterization.RasterCanvas;

import java.awt.geom.Area;

public final class RasterizeAreaOp {
    public static DataOp<BitRaster> apply(DataOp<Area> areaOp) {
        return areaOp.mapBlocking((area, view) -> {
            BitRaster raster = BitRaster.create(view);

            RasterCanvas canvas = RasterCanvas.of(view);
            canvas.setColor(1);
            canvas.fill(area);

            for (int y = 0; y < view.getHeight(); y++) {
                for (int x = 0; x < view.getWidth(); x++) {
                    if (canvas.getData(x, y) == 1) {
                        raster.put(x, y);
                    }
                }
            }

            return raster;
        });
    }
}
