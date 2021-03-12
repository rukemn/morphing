package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.Coordinate;

public abstract class StrategyDecorator implements OctiMatchStrategy{

    protected OctiMatchStrategy underlyingStrategy;

    public StrategyDecorator(OctiMatchStrategy underlyingStrategy){
        this.underlyingStrategy = underlyingStrategy;
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        underlyingStrategy.initStrategy(sourceString,targetString);
    }

    @Override
    public double match(OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return underlyingStrategy.match(sourceSegment,targetSegment);
    }

    @Override
    public double deleteOnto(OctiLineSegment segmentToBeDeleted, Coordinate point) {
        return underlyingStrategy.deleteOnto(segmentToBeDeleted, point);
    }

    @Override
    public double createFrom(Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        return underlyingStrategy.createFrom(creationPoint, segmentToBeCreated);
    }
}
