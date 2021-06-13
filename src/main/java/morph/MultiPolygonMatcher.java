package morph;

import io.FileParseException;
import io.WktPolygonExtractor;
import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.twak.utils.Pair;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class not in use yet, just for some experiments to handle multipolygons
 * Purpose of this class is to have 2 Multipolygons source and target be split into OctiLineStrings and try to match
 * them as bijectively as possibly
 */
public class MultiPolygonMatcher {
    private MultiPolygon source, target;

    private static final Logger logger = LogManager.getLogger();


    public static void main(String[] args) {
        String path = "./src/main/resources/csvs/wktTestDummy3to3with2match.csv";//"./src/main/resources/csvs/wktTestDummyMultiToMulti.csv";
        URI uri = Paths.get(path).toUri();
        WktPolygonExtractor wktExtractor = new WktPolygonExtractor();
        try {
            wktExtractor.parseFile(uri);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FileParseException e) {
            e.printStackTrace();
        }

        List<Geometry> geoms = wktExtractor.getGeometryList();
        MultiPolygon src = (MultiPolygon) wktExtractor.getNthGeometry(0);
        MultiPolygon tar = (MultiPolygon) wktExtractor.getNthGeometry(1);
        logger.trace(src.toText());
        logger.trace((tar.toText()));

        MultiPolygonMatcher multiPolymatcher = new MultiPolygonMatcher();
        Pair<List<Polygon>, List<Polygon>> splitted = multiPolymatcher.splitMultiPolygons(src, tar, OctiGeometryFactory.OCTI_FACTORY.createPoint()); //dummypoint
        List<Pair<Polygon, Polygon>> polyToPolyMappings = multiPolymatcher.matchMultiPolygons(splitted.first(), splitted.second());

        List<OctiStringAlignment> alignmentList = new ArrayList<>();
        for (Pair<Polygon, Polygon> mappings : polyToPolyMappings) {
            //only do outer rings for now
            OctiStringAlignment alignment = null;
            if (mappings.first() == null) {
                Point creationPoint = mappings.second().getCentroid();
                OctiLineString second = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(mappings.second().getExteriorRing());
                alignment = new OctiStringCreation(creationPoint, second);
            } else if (mappings.second() == null) {
                Point deletionPoint = mappings.first().getCentroid();
                OctiLineString first = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(mappings.first().getExteriorRing());
                alignment = new OctiStringDeletion(first, deletionPoint);
            } else {
                OctiLineString first = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(mappings.first().getExteriorRing());
                OctiLineString second = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(mappings.second().getExteriorRing());

                OctiLineMatcher olm = new OctiLineMatcher(first, second);
                try {
                    alignment = olm.getAlignment();
                } catch (NoMinimumOperationException e) {
                    e.printStackTrace();
                }
            }
            alignmentList.add(alignment);
        }

    }

    /**
     * Assertion: for each source/target there is exactly one polygon which contains startNode
     * todo use that startNode to match the 2 polygons
     *
     * @param source
     * @param target
     * @param startNode
     * @return
     */
    public Pair<List<Polygon>, List<Polygon>> splitMultiPolygons(MultiPolygon source, MultiPolygon target, Point startNode) {
        this.source = source;
        this.target = target;
        List<Polygon> sourcePolygons = new ArrayList<>();
        List<Polygon> targetPolygons = new ArrayList<>();

        for (int srcIdx = 0; srcIdx < source.getNumGeometries(); srcIdx++) {
            sourcePolygons.add((Polygon) source.getGeometryN(srcIdx));
        }
        for (int tarIdx = 0; tarIdx < target.getNumGeometries(); tarIdx++) {
            targetPolygons.add((Polygon) target.getGeometryN(tarIdx));
        }
        return new Pair<>(sourcePolygons, targetPolygons);
    }


    /**
     * matches the outer rings with each other,
     * inner src rings
     * only 1:m and n:1 or 1:1 matchings, no n:m
     *
     * @param src
     * @param tar
     * @return
     */
    public List<Pair<OctiLineString, OctiLineString>> matchPolygonOnetoOne(Polygon src, Polygon tar) {
        List<Pair<OctiLineString, OctiLineString>> matchings = new ArrayList<>();

        //outer rings are matched by default
        OctiLineString outerSrc = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(src.getExteriorRing());
        OctiLineString outerTar = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(tar.getExteriorRing());
        Pair<OctiLineString, OctiLineString> outer = new Pair<>(outerSrc, outerTar);
        matchings.add(outer);


        int numberSrcInnerRings = src.getNumInteriorRing();
        int numberTarInnerRings = tar.getNumInteriorRing();
        List<LinearRing> innerSrcRings = new ArrayList<>();
        for (int srcIdx = 0; srcIdx < numberSrcInnerRings; srcIdx++) innerSrcRings.add(src.getInteriorRingN(srcIdx));

        List<LinearRing> innerTarRings = new ArrayList<>();
        for (int tarIndex = 0; tarIndex < numberTarInnerRings; tarIndex++)
            innerTarRings.add(tar.getInteriorRingN(tarIndex));


        logger.debug("matching " + numberSrcInnerRings + "inner src rings with " + numberTarInnerRings + " inner target rings");

        Table<LinearRing, LinearRing, Double> intersectionOverUnionTable = HashBasedTable.create();

        for (LinearRing innerSrc : innerSrcRings) {
            Polygon srcholePolygon = OctiGeometryFactory.OCTI_FACTORY.createPolygon(innerSrc.getCoordinateSequence());

            for (LinearRing innerTar : innerTarRings) {
                Polygon tarholePolygon = OctiGeometryFactory.OCTI_FACTORY.createPolygon(innerTar.getCoordinateSequence());

                Double iou = distanceIntersectionOverUnion(srcholePolygon, tarholePolygon); //todo maybe go for IoU of Bounding Boxes
                // only if they are "close"
                if (iou > 0) intersectionOverUnionTable.put(innerSrc, innerTar, iou);
            }
        }

        //first the despawn of source
        Set<LinearRing> despawns = new HashSet<>(innerSrcRings);
        despawns.removeAll(intersectionOverUnionTable.rowKeySet());
        logger.debug(despawns.size() + " inner src rings will be despawned");
        for (LinearRing lr : despawns)
            matchings.add(new Pair<>(OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(lr), null));

        //now spawns of target
        Set<LinearRing> spawns = new HashSet<>(innerTarRings);
        spawns.removeAll(intersectionOverUnionTable.columnKeySet());
        logger.debug(spawns.size() + " inner tar rings will be spawned");
        for (LinearRing lr : spawns)
            matchings.add(new Pair<>(null, OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(lr)));

        //remaining mappable rings
        for (LinearRing srcRing : intersectionOverUnionTable.rowKeySet()) {
            Map<LinearRing, Double> row = intersectionOverUnionTable.row(srcRing);
            OctiLineString srcOcti = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(srcRing);

            if (row.size() > 1) { // the src Ring is the "1" part, it will be split, all mapped tar Rings are only in relation with this src
                //todo, the n:m exlusion doesn't happen yet
                for (LinearRing tarRing : row.keySet())
                    matchings.add(new Pair<>(srcOcti, OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(tarRing)));
                logger.debug("src ring will be split into multiple tar rings");

            } else if (row.size() == 1) {
                Map.Entry<LinearRing, Double> mappedTar = row.entrySet().iterator().next();
                OctiLineString tarOcti = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(mappedTar.getKey());
                if (intersectionOverUnionTable.column(mappedTar.getKey()).size() == 1) { // one to one mapping
                    matchings.add(new Pair<>(srcOcti, tarOcti));
                    logger.debug("src ring will be matched into multiple tar rings");
                } else { // m src rings will be mapped to 1 tar ring
                    //todo, the n:m exlusion doesn't happen yet
                    logger.debug("src ring will be split into multiple tar rings");
                    matchings.add(new Pair<>(srcOcti, tarOcti));
                }

            } else { // should be already caught with spawns/despawns
                logger.error("expected to not happen");
                assert (false);
            }
        }
        return matchings;
    }

    /**
     * todo improve
     * Constraints for matching
     * <ul>
     *     <li>every polygon must be matched, despawned(src) or spawned(tar)</li>
     *     <li> for any isochrone with less travelTime than another it is contained in this isochrone( given same starting node and startingtime)</li>
     *     <li> for a given isochrone all polygons within it do not overlap (otherwise, they'd be one)</li>
     *     <li> for now only 1:m and n:1 (in contrast to m:n) matchings are allowed (in a table: for all cells,
     *     the condition holds, that the cell is alone in atleast one of its row or column</li>
     * </ul>
     * <p>
     * Assumptions:
     * <ul>
     *     <li>src is the isochrone with less travel time</li>
     * </ul>
     * multiple vaiants to solve this
     *      Bipartite Graph way:
     *          Use some distance measure:
     *          <ul>
     *               <li>Distance of centroids</li>
     *               <li>Intersection over union</li>
     *          </ul>
     * <p>
     *      this way (greedy experiment, currently IoU)
     *          <ul>
     *              <li> IoU= 0 ==> spawn/despawn </li>
     *          </ul>
     *
     * @param src the sourcePolygons
     * @param tar the targetPolygons
     * @return
     */
    public List<Pair<Polygon, Polygon>> matchMultiPolygons(List<Polygon> src, List<Polygon> tar) {
        logger.debug("matching " + src.size() + " source with " + tar.size() + " target polygons");
        Table<Polygon, Polygon, Boolean> containTable = HashBasedTable.create();

        for (Polygon s : src) {
            for (Polygon t : tar) {
                if (t.contains(s))
                    containTable.put(s, t, true); //less strong condition, could also use covers to include boundary
            }
        }
        logger.debug(containTable.toString());

        List<Pair<Polygon, Polygon>> matchings = new ArrayList<>();

        //first the despawns
        Set<Polygon> srcSet = new HashSet<>(src);
        srcSet.removeAll(containTable.rowKeySet()); //non overlapping polygons remain
        logger.info(srcSet.size() + " source Polygons will be despawned");
        for (Polygon poly : srcSet) matchings.add(new Pair<>(poly, null));

        //secondly the spawns
        Set<Polygon> targetSet = new HashSet<>(tar);
        targetSet.removeAll(containTable.columnKeySet());
        logger.info(targetSet.size() + " target Polygons will be spawned");
        for (Polygon poly : targetSet) matchings.add(new Pair<>(null, poly));


        for (Polygon srcPoly : containTable.columnKeySet()) { //matchable src polys
            for (Polygon tarPoly : containTable.column(srcPoly).keySet()) {
                matchings.add(new Pair<>(srcPoly, tarPoly));
            }
        }
        logger.debug("mappings:");
        for (Polygon srcPoly : containTable.columnKeySet()) {
            logger.debug("src maps to : " + containTable.column(srcPoly).size());
        }
        for (Polygon tarPoly : containTable.rowKeySet()) {
            logger.debug("tar mapped from : " + containTable.row(tarPoly).size());
        }

        return matchings;
    }

    /**
     * commutative distance function
     *
     * @param a first polygon
     * @param b second polygon
     * @return the percentage of overlap, lies between 0 and 1(including both)
     */
    private double distanceIntersectionOverUnion(Polygon a, Polygon b) {
        Geometry intersection = a.intersection(b);
        Geometry union = a.union(b);
        return intersection.getArea() / union.getArea();
    }

    /**
     * commutative distance function
     *
     * @param a first polygon
     * @param b second polygon
     * @return the distance of the polygon's centroids
     */
    private double distanceCentroids(Polygon a, Polygon b) {
        return a.getCentroid().distance(b.getCentroid());
    }
}
