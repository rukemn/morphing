package morph;

import jtsadaptions.OctiLineSegment;
import org.locationtech.jts.geom.Coordinate;

public class OctiSegmentInsert implements OctiSegmentAlignment{

    private final Coordinate srcPoint;
    private final OctiLineSegment tar;

    public OctiSegmentInsert(Coordinate source, OctiLineSegment target){
        this.srcPoint = source;
        this.tar = target;
    }

    @Override
    public Coordinate getSourceStart() {
        return srcPoint;
    }

    @Override
    public Coordinate getSourceEnd() {
        return srcPoint;
    }

    @Override
    public Coordinate getTargetStart() {
        return tar.p0;
    }

    @Override
    public Coordinate getTargetEnd() {
        return tar.p1;
    }

    @Override
    public OctiLineSegment.Orientation getOrientation() {
        return tar.getOrientation();
    }

    @Override
    public Operation getOperation() {
        return Operation.Insert;
    }
}
