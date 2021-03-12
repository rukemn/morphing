package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.Coordinate;

/**
 * Todo for multiple deletions/insertions have the cost be as described in
 * https://de.wikipedia.org/wiki/Gotoh-Algorithmus
 * needed: information on previous move
 * alternativly implement GapCosts in OctiLineMatcher
 */
public class AffineGapStrategy extends StrategyDecorator {

    public AffineGapStrategy(OctiMatchStrategy underlyingStrategy){
        super(underlyingStrategy);
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {

    }

    @Override
    public double match(OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return 0;
    }

    @Override
    public double deleteOnto(OctiLineSegment segmentToBeDeleted, Coordinate point) {
        return 0;
    }

    @Override
    public double createFrom(Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        return 0;
    }
}
