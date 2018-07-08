package net.gegy1000.earth.server.world.pipeline.layer;

import net.gegy1000.earth.server.world.pipeline.source.tile.OsmTile;
import net.gegy1000.terrarium.server.world.coordinate.Coordinate;
import net.gegy1000.terrarium.server.world.coordinate.CoordinateState;
import net.gegy1000.terrarium.server.world.pipeline.DataLayerProducer;
import net.gegy1000.terrarium.server.world.pipeline.DataView;
import net.gegy1000.terrarium.server.world.pipeline.source.DataTilePos;
import net.gegy1000.terrarium.server.world.pipeline.source.TiledDataSource;
import net.minecraft.util.math.MathHelper;

public class OsmSampleLayer implements DataLayerProducer<OsmTile> {
    private final TiledDataSource<OsmTile> overpassSource;
    private final CoordinateState coordinateState;

    public OsmSampleLayer(TiledDataSource<OsmTile> overpassSource, CoordinateState coordinateState) {
        this.overpassSource = overpassSource;
        this.coordinateState = coordinateState;
    }

    @Override
    public OsmTile apply(DataView view) {
        DataView bufferView = view.grow(16, 16, 16, 16);

        DataTilePos blockMinTilePos = this.getTilePos(bufferView.getMinCoordinate());
        DataTilePos blockMaxTilePos = this.getTilePos(bufferView.getMaxCoordinate());

        DataTilePos minTilePos = DataTilePos.min(blockMinTilePos, blockMaxTilePos);
        DataTilePos maxTilePos = DataTilePos.max(blockMinTilePos, blockMaxTilePos);

        OsmTile mergedTile = new OsmTile();

        for (int tileZ = minTilePos.getTileZ(); tileZ <= maxTilePos.getTileZ(); tileZ++) {
            for (int tileX = minTilePos.getTileX(); tileX <= maxTilePos.getTileX(); tileX++) {
                OsmTile tile = this.overpassSource.getTile(new DataTilePos(tileX, tileZ));
                if (tile != null) {
                    mergedTile = mergedTile.merge(tile);
                }
            }
        }

        return mergedTile;
    }

    private DataTilePos getTilePos(Coordinate coordinate) {
        coordinate = coordinate.to(this.coordinateState);

        Coordinate tileSize = this.overpassSource.getTileSize();
        int tileX = MathHelper.floor(coordinate.getX() / tileSize.getX());
        int tileZ = MathHelper.floor(coordinate.getZ() / tileSize.getZ());
        return new DataTilePos(tileX, tileZ);
    }
}
