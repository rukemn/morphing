package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.Coordinate;

/**
 * only considers whether or not its a match, in contrast to taking edge length or distance in consideration
 */
public class FlatScoreStrategy extends StrategyDecorator {

    public FlatScoreStrategy(OctiMatchStrategy underlyingStrategy) {
        super(underlyingStrategy);
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        //maximum diagonal elements/ matches total score >= 0
        //maxScore = Math.min(sourceString.size() , targetString.size());
    }

    @Override
    public double match(OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return -1;
    }

    @Override
    public double deleteOnto(OctiLineSegment segmentToBeDeleted, Coordinate point) {
        return 1;
    }

    @Override
    public double createFrom(Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        return 1;
    }
}
