package morph;

import jtsadaptions.OctiLineSegment;
import org.locationtech.jts.geom.Coordinate;

public interface OctiSegmentAlignment {

    enum Operation{
        Match,Delete,Insert
    }
    public Coordinate getSourceStart();
    public Coordinate getSourceEnd();
    public Coordinate getTargetStart();
    public Coordinate getTargetEnd();

    public OctiLineSegment.Orientation getOrientation();
    public Operation getOperation();


}
