package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.Coordinate;


// experimental
public interface OctiMatchStrategy {
    void initStrategy(OctiLineString sourceString, OctiLineString targetString);
    double match(OctiLineSegment sourceSegment, OctiLineSegment targetSegment);
    double deleteOnto(OctiLineSegment segmentToBeDeleted, Coordinate point);
    double createFrom(Coordinate creationPoint, OctiLineSegment segmentToBeCreated);
}
