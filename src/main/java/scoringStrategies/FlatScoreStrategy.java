package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.locationtech.jts.geom.Coordinate;

/**
 * only considers whether or not its a match, in contrast to taking edge length or distance in consideration
 */
public class FlatScoreStrategy implements OctiMatchStrategy {

    double matchScore = -1;
    double indelScore = 0;

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        //maximumMatches = Math.min(sourceString.size(),targetString.size());
    }

    @Override
    public double match(MatrixElement previous, OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return matchScore;
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point) {
        return indelScore;
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        return indelScore;
    }
}
