package jtsadaptions;

import org.locationtech.jts.geom.*;

import javax.management.openmbean.OpenMBeanConstructorInfo;

public class OctiGeometryFactory extends GeometryFactory {

    public static final OctiGeometryFactory OCTI_FACTORY = new OctiGeometryFactory();

    /**
     * Constructs an empty {@link OctiLineString} geometry.
     *
     * @return an empty OctiLineString
     */
    public OctiLineString createOctiLineString() {
        return createOctiLineString(getCoordinateSequenceFactory().create(new Coordinate[]{}));
    }

    /**
     * Creates a OctiLineString using the given Coordinates.
     * A null or empty array creates an empty LineString.
     *
     * @param coordinates an array without null elements, or an empty array, or null
     */
    public OctiLineString createOctiLineString(Coordinate[] coordinates) {
        return createOctiLineString(coordinates != null ? getCoordinateSequenceFactory().create(coordinates) : null);
    }

    /**
     * Creates an OctiLineString using the given CoordinateSequence.
     * A null or empty CoordinateSequence creates an empty OctiLineString.
     *
     * @param coordinates a CoordinateSequence (possibly empty), or null
     */
    public OctiLineString createOctiLineString(CoordinateSequence coordinates) {
        return new OctiLineString(coordinates, this);
    }

}
