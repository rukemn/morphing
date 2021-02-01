package morph;

import jtsadaptions.OctiLineSegment;
import org.locationtech.jts.geom.Coordinate;

public class OctiSegmentMatch implements OctiSegmentAlignment{

    private final OctiLineSegment src;
    private final OctiLineSegment tar;

    public OctiSegmentMatch(OctiLineSegment source, OctiLineSegment target){
        if(source.getOrientation() != target.getOrientation()) throw new IllegalArgumentException("source and target do not have the same orientation");
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
        return tar.p0;
    }

    @Override
    public Coordinate getTargetEnd() {
        return tar.p1;
    }

    @Override
    public OctiLineSegment.Orientation getOrientation() {
        return src.getOrientation();
    }

    @Override
    public Operation getOperation() { return Operation.Match; }
}
