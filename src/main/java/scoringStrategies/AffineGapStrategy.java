package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

/** Gap Starts are penelized by an increased
 * https://de.wikipedia.org/wiki/Gotoh-Algorithmus
 */
public class AffineGapStrategy implements OctiMatchStrategy {
    private static Logger logger = LogManager.getLogger();
    double matchScore = -1.0;
    double indelScore = 1.0;
    double gapPenalty = 3.0; //adds to indel

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        // no init needed
    }

    @Override
    public double match(MatrixElement previous,OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return matchScore;
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point) {
        if(previous.deletionsInARow > 0){
            logger.trace("del "+ previous.deletionsInARow);
            return indelScore;
        }else{
            logger.trace("gap");
            return indelScore + gapPenalty;
        }
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        if(previous.insertionsInARow > 0){
            logger.trace("ins "+ previous.insertionsInARow);
            return indelScore;
        }else{ // this is a gap start
            logger.trace("gap");
            return indelScore + gapPenalty;
        }
    }
}
