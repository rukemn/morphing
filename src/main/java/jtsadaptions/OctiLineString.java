package jtsadaptions;

import org.locationtech.jts.geom.*;

import java.util.LinkedList;
import java.util.List;

// todo subclass with OctiLinearRing
public class OctiLineString extends LineString implements Octilinear {

    //todo maybe factor out into userData Object
    private final List<OctiLineSegment> octiString;

    public OctiLineString(CoordinateSequence points, GeometryFactory factory) {
        super(points, factory);
        octiString = createOctiSegments(this.points);
    }

    protected Geometry reverseInternal(){
        CoordinateSequence seq = points.copy();
        CoordinateSequences.reverse(seq);
        return OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(seq);
    }

    public OctiLineString(LineString ls, GeometryFactory factory) {
        super(ls.getCoordinateSequence(), factory);
        octiString = createOctiSegments(points);
    }

    public OctiLineString(Coordinate[] points, PrecisionModel precisionModel, int SRID) {
        super(points, precisionModel, SRID);
        octiString = createOctiSegments(this.points);
    }

    private List<OctiLineSegment> createOctiSegments(CoordinateSequence points) {
        List<OctiLineSegment> octilinearString = new LinkedList<>();
        Coordinate prev, curr;
        if (points.size() < 2) throw new TopologyException("empty/invalid OctiLineString");

        prev = points.getCoordinate(0);
        for (int i = 1; i < points.size(); i++) {
            curr = points.getCoordinate(i);
            OctiLineSegment ols = new OctiLineSegment(prev, curr);
            octilinearString.add(ols);
            prev = curr;
        }
        return octilinearString;
    }

    public OctiLineString makeNthPointTheFirst(int index) {

        if (index == 0) return OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(this.points.copy());//(OctiLineString) this.copy();

        Coordinate[] coords = this.points.toCoordinateArray();
        Coordinate[] returnCoords = new Coordinate[this.getNumPoints()];

        //leave out the double element if Ring
        int endpoint = this.isClosed() ? coords.length - 1 : coords.length;

        for (int i = 0; i + index < endpoint; i++) returnCoords[i] = coords[index + i];
        for (int i = 0; i < index; i++) returnCoords[endpoint - index + i] = coords[i];

        // append the point that closes the linestring
        if (this.isClosed()) returnCoords[coords.length - 1] = returnCoords[0];
        return OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(returnCoords);
    }

    /**
     * Checks for consecutive OctilineSegments not to be of the same or opposite Orientation
     */
    @Override
    public boolean checkOctilinear() {
        OctiLineSegment prev = octiString.get(0);
        for (OctiLineSegment ls : octiString.subList(1, octiString.size())) {

            if (prev.getOrientation().opposite() == ls.getOrientation())
                throw new TopologyException("OctiLineString can't have two consecutive OctiLineSegments be of opposite orientation");
            if (prev.getOrientation() == ls.getOrientation())
                throw new TopologyException("OctiLineString can't have two consecutive OctiLineSegments be of the same orientation");
        }
        return true; //todo correct function
    }

    public int size() {
        return octiString.size();
    }

    public OctiLineSegment getSegment(int index) {
        return octiString.get(index);
    }

    /**
     * Helper to accomodate for matrix layout
     *
     * @param index the endpoint's index
     * @return the @{OctilineSegment} before the {index} point
     */
    public OctiLineSegment getSegmentBeforeNthPoint(int index) {
        return octiString.get(index - 1);
    }


}
