package morph;

import jtsadaptions.OctiLineSegment;
import org.locationtech.jts.geom.Coordinate;

public class OctiSegmentDelete implements OctiSegmentAlignment {

    private OctiLineSegment src;
    private Coordinate tar;

    public OctiSegmentDelete(OctiLineSegment source, Coordinate target){
        this.src = source;
        this.tar = target;
    }
    @Override
    public Coordinate getSourceStart() {
        return src.p0;
    }

    @Override
    public Coordinate getSourceEnd() {
        return src.p1;
    }

    @Override
    public Coordinate getTargetStart() {
        return tar;
    }

    @Override
    public Coordinate getTargetEnd() {
        return tar;
    }

    @Override
    public OctiLineSegment.Orientation getOrientation() {
        return src.getOrientation();
    }

    @Override
    public Operation getOperation() { return Operation.Delete; }
}
