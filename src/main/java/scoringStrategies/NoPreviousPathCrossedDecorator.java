package scoringStrategies;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineSegment;
import morph.MatrixElement;
import morph.OctiLineMatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import scoringStrategies.OctiMatchStrategy;
import scoringStrategies.StrategyDecorator;


public class NoPreviousPathCrossedDecorator extends StrategyDecorator {
    private static Logger logger = LogManager.getLogger();
    public final Table<LineString,LineString,Boolean> visibilityMap = HashBasedTable.create();


    public NoPreviousPathCrossedDecorator(OctiMatchStrategy underlyingStrategy) {
        super(underlyingStrategy);
    }

    /** Checks if the new vertex-path intersects with any previously added ones
     *
     * @param toCheckStartPoint start Point of the already added Alignment-Vertex-Path
     * @param toCheckEndPoint end point of the already added Alignment-Vertex-Path
     * @param newSegmentStartPoint start Point of the Alignment-Vertex Path to add
     * @param newSegmentEndPoint end Point of the Alignment-Vertex Path to add
     * @return wheter they intersect in any point other than the four defining points
     */
    private boolean checkIntersection(Coordinate toCheckStartPoint, Coordinate toCheckEndPoint, Coordinate newSegmentStartPoint, Coordinate newSegmentEndPoint){
        LineString previousVertexPath = OctiGeometryFactory.OCTI_FACTORY.createLineString(new Coordinate[]{toCheckStartPoint,toCheckEndPoint});
        LineString newVertexPath = OctiGeometryFactory.OCTI_FACTORY.createLineString(new Coordinate[]{newSegmentStartPoint,newSegmentEndPoint});

        Boolean preComputed = visibilityMap.get(previousVertexPath,newVertexPath);
        if(preComputed != null) return preComputed;

        Geometry intersection = previousVertexPath.intersection(newVertexPath);
        //intersections can either be an empty LineString if no intersection , a point if one intersection or a LineString if more than one point exists(verify!)
        logger.debug("intersection is a " + intersection.getGeometryType());

        //todo evaluate if this condition is suffictient to prevent loops while morphing
        if( (intersection instanceof Point)){
            if(((Point)intersection).getCoordinate().equals(toCheckStartPoint)  ||
                            ((Point)intersection).getCoordinate().equals(toCheckEndPoint)  ||
                            ((Point)intersection).getCoordinate().equals(newSegmentStartPoint)  ||
                            ((Point)intersection).getCoordinate().equals(newSegmentEndPoint)) {
                logger.debug("previous VertexPath and new VertexPath don't intersect");
                visibilityMap.put(previousVertexPath, newVertexPath, false);
                return false;
            }else{
                logger.debug("previous VertexPath and new VertexPath intersect in a Point");
                visibilityMap.put(previousVertexPath, newVertexPath, true);
                return true;
            }
        }else if((intersection instanceof LineString) && ((LineString) intersection).getCoordinates().length == 0){ //empty LineStringGeometry
            visibilityMap.put(previousVertexPath,newVertexPath,false);
            logger.debug("previous VertexPath and new VertexPath don't intersect, empty LineString");
            return false;
        }else{
            visibilityMap.put(previousVertexPath,newVertexPath,true);
            logger.debug("previous VertexPath and new VertexPath intersect in sth else");
            return true;
        }
    }

    @Override
    public double match(MatrixElement previous, OctiLineSegment sourceSegment, OctiLineSegment targetSegment){
        //match vertex paths cant intersect
        Coordinate newVertexPathStart = sourceSegment.p1;
        Coordinate newVertexPathEnd = targetSegment.p1;

        /*Coordinate previousVertexPathStart = sourceSegment.p0;
        Coordinate previousVertexPathEnd = sourceSegment.p0;*/    //equivalent definitions
        MatrixElement pre = previous;
        do{
            Coordinate previousVertexPathStart = pre.sourceEndPoint;
            Coordinate previousVertexPathEnd = pre.targetEndPoint;

            boolean intersects = checkIntersection(previousVertexPathStart,previousVertexPathEnd,newVertexPathStart,newVertexPathEnd);
            if(intersects) return OctiLineMatcher.IMPOSSIBLE;
            newVertexPathStart = previousVertexPathStart;
            newVertexPathEnd = previousVertexPathEnd;


            logger.trace("coords: " +pre.sourceEndPoint +" " + pre.targetEndPoint);
            pre = pre.getBestPreviousElement();
        }while (pre != null); //iterate till (0,0) element
        logger.warn("start reached");
        // no intersections found
        return underlyingStrategy.match(previous, sourceSegment, targetSegment);
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point){
        Coordinate newVertexPathStart = segmentToBeDeleted.p1;
        Coordinate newVertexPathEnd = point;
        MatrixElement pre = previous;
        do{
            Coordinate previousVertexPathStart = pre.sourceEndPoint;
            Coordinate previousVertexPathEnd = pre.targetEndPoint;

            boolean intersects = checkIntersection(previousVertexPathStart,previousVertexPathEnd,newVertexPathStart,newVertexPathEnd);
            if(intersects) return OctiLineMatcher.IMPOSSIBLE;
            newVertexPathStart = previousVertexPathStart;
            newVertexPathEnd = previousVertexPathEnd;


            pre = pre.getBestPreviousElement();
        }while (pre != null); //iterate till (0,0) element

        return underlyingStrategy.deleteOnto(previous,segmentToBeDeleted,point);
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated){
        Coordinate newVertexPathStart = creationPoint;
        Coordinate newVertexPathEnd = segmentToBeCreated.p1;
        MatrixElement pre = previous;
        do{
            Coordinate previousVertexPathStart = pre.sourceEndPoint;
            Coordinate previousVertexPathEnd = pre.targetEndPoint;

            boolean intersects = checkIntersection(previousVertexPathStart,previousVertexPathEnd,newVertexPathStart,newVertexPathEnd);
            if(intersects) return OctiLineMatcher.IMPOSSIBLE;
            newVertexPathStart = previousVertexPathStart;
            newVertexPathEnd = previousVertexPathEnd;


            pre = pre.getBestPreviousElement();
        }while (pre != null); //iterate till (0,0) element

        return underlyingStrategy.createFrom(previous,creationPoint,segmentToBeCreated);
    }
}
