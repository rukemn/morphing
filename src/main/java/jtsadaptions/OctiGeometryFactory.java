package jtsadaptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;

public class OctiGeometryFactory extends GeometryFactory {
    private static Logger logger = LogManager.getLogger();
    public static final OctiGeometryFactory OCTI_FACTORY = new OctiGeometryFactory(new PrecisionModel());

    public OctiGeometryFactory(PrecisionModel pm) {
        super(pm);
    }

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
     * @return the new OctiLineString
     */
    public OctiLineString createOctiLineString(Coordinate[] coordinates) {
        return createOctiLineString(coordinates != null ? getCoordinateSequenceFactory().create(coordinates) : null);
    }

    public OctiLineString createOctiLineString(LinearRing ring) {
        return createOctiLineString(ring.getCoordinateSequence());
    }

    /**
     * Creates an OctiLineString using the given CoordinateSequence.
     * A null or empty CoordinateSequence creates an empty OctiLineString.
     *
     * @param coordinates a CoordinateSequence (possibly empty), or null
     * @return the new OctiLineString
     */
    public OctiLineString createOctiLineString(CoordinateSequence coordinates) {
        return new OctiLineString(coordinates, this);
    }

    /**
     * Helper method to extract an outer OctiLineString from a geometry
     * If the geom is a
     * <ul>
     *     <li>OctiLineStrin: geom is returned</li>
     *     <li>Polygon: exterior ring is returned</li>
     *     <li>MultiPolygon: exterior ring of the biggest polygon is returned</li>
     * </ul>
     * All else will throw an exception
     *
     * @param geom the geometry to extract from
     * @return the extracted OctiLineString
     * @throws Exception is thrown when <code>code</code> is a <code>GeometryCollection</code>
     */
    public OctiLineString convertToOctiLineString(Geometry geom) throws Exception {
        logger.debug("converting geom: " + geom.toText());
        if (geom instanceof OctiLineString) return (OctiLineString) geom;
        //assumed to be simple
        if (geom instanceof Polygon) {
            if (geom.isSimple())
                return this.createOctiLineString(((Polygon) geom).getExteriorRing().getCoordinateSequence());
            Polygon p = (Polygon) geom;
            logger.debug("internal rings: " + p.getNumInteriorRing());
            logger.debug("self intersects " + p.intersects(p));
        }
        if (geom instanceof LinearRing) return this.createOctiLineString(((LinearRing) geom).getCoordinateSequence());
        if (geom instanceof LineString) return this.createOctiLineString(((LineString) geom).getCoordinateSequence());

        //todo, probably better suited in separate logic
        if (geom instanceof MultiPolygon) {
            logger.warn("not implemented yet, returning biggest polygon");
            logger.debug("choosing out of " + geom.getNumGeometries() + " polygons");
            if (geom.getNumGeometries() == 0) return null;

            Polygon max = (Polygon) geom.getGeometryN(0);
            double maxPolygonArea = max.getArea();
            double area;
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                Polygon p = (Polygon) geom.getGeometryN(i);
                area = p.getArea();
                if (area > maxPolygonArea) {
                    maxPolygonArea = area;
                    max = p;
                }
            }
            return convertToOctiLineString(max);

        }
        if (geom instanceof GeometryCollection) logger.warn("not implemented yet");
        throw new Exception("cant convert");
    }
}
