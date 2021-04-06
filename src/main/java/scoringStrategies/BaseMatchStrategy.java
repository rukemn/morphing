package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.locationtech.jts.geom.Coordinate;

/**
 * distance based strategy, which scores the operation based on the distance all vertices
 * have to travel in the morph.
 * Additionally matches are awarded a matchDivisor to favor matches over insertions/deletions
 */
public class BaseMatchStrategy implements OctiMatchStrategy {

    public static final double MATCH_DIVISOR = 10.0;

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
       //nothing to do here
    }

    @Override
    public double match(MatrixElement previous, OctiLineSegment sourceSegment, OctiLineSegment targetSegment){
        return (sourceSegment.p0.distance(targetSegment.p0) + sourceSegment.p1.distance(targetSegment.p1)) / MATCH_DIVISOR;
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point){
        double score = segmentToBeDeleted.p0.distance(point) + segmentToBeDeleted.p1.distance(point);
        score += segmentToBeDeleted.getLength();
        return score;
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated){
        double score =  creationPoint.distance(segmentToBeCreated.p0)
                + creationPoint.distance(segmentToBeCreated.p1);
        score += segmentToBeCreated.getLength();
        return score;
    }
}
