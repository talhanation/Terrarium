package net.gegy1000.terrarium.server.world.data.op;

import net.gegy1000.terrarium.server.util.Voronoi;
import net.gegy1000.terrarium.server.world.coordinate.Coordinate;
import net.gegy1000.terrarium.server.world.coordinate.CoordinateReference;
import net.gegy1000.terrarium.server.world.data.DataOp;
import net.gegy1000.terrarium.server.world.data.DataView;
import net.gegy1000.terrarium.server.world.data.raster.UByteRaster;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public final class VoronoiScaleOp {
    public static <T extends UByteRaster> DataOp<T> scaleUBytesFrom(DataOp<T> data, CoordinateReference src, Function<DataView, T> function) {
        Voronoi voronoi = new Voronoi(Voronoi.DistanceFunc.EUCLIDEAN, 0.9, 1000);

        return DataOp.of(view -> {
            DataView srcView = getSourceView(view, src);

            double destToSrcX = 1.0 / src.scaleX();
            double destToSrcY = 1.0 / src.scaleZ();

            Coordinate minCoordinate = Coordinate.min(
                    view.getMinCoordinate().to(src),
                    view.getMaxCoordinate().to(src)
            );

            double offsetX = minCoordinate.getX() - srcView.getX();
            double offsetY = minCoordinate.getZ() - srcView.getY();

            return data.apply(srcView).thenApply(opt -> opt.map(source -> {
                T result = function.apply(view);
                voronoi.scaleBytes(source.getData(), result.getData(), srcView, view, destToSrcX, destToSrcY, offsetX, offsetY);
                return result;
            }));
        });
    }

    private static DataView getSourceView(DataView view, CoordinateReference src) {
        Coordinate minRegionCoordinateBlock = view.getMinCoordinate().to(src);
        Coordinate maxRegionCoordinateBlock = view.getMaxCoordinate().to(src);

        Coordinate minRegionCoordinate = Coordinate.min(minRegionCoordinateBlock, maxRegionCoordinateBlock);
        Coordinate maxRegionCoordinate = Coordinate.max(minRegionCoordinateBlock, maxRegionCoordinateBlock);

        int minSampleX = MathHelper.floor(minRegionCoordinate.getX()) - 1;
        int minSampleY = MathHelper.floor(minRegionCoordinate.getZ()) - 1;

        int maxSampleX = MathHelper.floor(maxRegionCoordinate.getX()) + 2;
        int maxSampleY = MathHelper.floor(maxRegionCoordinate.getZ()) + 2;

        return DataView.rect(minSampleX, minSampleY, maxSampleX - minSampleX, maxSampleY - minSampleY);
    }
}
