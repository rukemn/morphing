package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.locationtech.jts.geom.Coordinate;

public interface OctiMatchStrategy {
    void initStrategy(OctiLineString sourceString, OctiLineString targetString);
    double match(MatrixElement previous, OctiLineSegment sourceSegment, OctiLineSegment targetSegment);
    double deleteOnto(MatrixElement previous, OctiLineSegment segmentToBeDeleted, Coordinate point);
    double createFrom(MatrixElement previous, Coordinate creationPoint, OctiLineSegment segmentToBeCreated);
}
