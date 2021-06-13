package jtsadaptions;

import org.locationtech.jts.geom.TopologyException;

public class LineSegmentNotOctilinear extends TopologyException {

    public LineSegmentNotOctilinear(String msg) {
        super(msg);
    }

    public LineSegmentNotOctilinear(OctiLineSegment ls){
        super("LineSegment (" + ls.p0.x + " " + ls.p0.y + ", " +ls.p1.x +" " + ls.p1.y +") is not octilinear" );
    }
}
