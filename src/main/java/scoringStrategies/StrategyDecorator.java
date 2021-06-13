package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.locationtech.jts.geom.Coordinate;

public abstract class StrategyDecorator implements OctiMatchStrategy{

    protected OctiMatchStrategy underlyingStrategy;

    private StrategyDecorator() { }

    public StrategyDecorator(OctiMatchStrategy underlyingStrategy){
        this.underlyingStrategy = underlyingStrategy;
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        underlyingStrategy.initStrategy(sourceString,targetString);
    }

    @Override
    public double match(MatrixElement previous, OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return underlyingStrategy.match(previous,sourceSegment,targetSegment);
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point) {
        return underlyingStrategy.deleteOnto(previous, segmentToBeDeleted, point);
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        return underlyingStrategy.createFrom(previous,creationPoint, segmentToBeCreated);
    }
}
