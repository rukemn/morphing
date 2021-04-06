package scoringStrategies;

import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import morph.OctiLineMatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.twak.utils.Pair;

import java.util.HashMap;
import java.util.Map;

public class CompleteVisibleDecorator extends StrategyDecorator {
    private OctiLineString source;
    private OctiLineString target;
    private static Logger logger = LogManager.getLogger();
    public final Map<Pair<Coordinate,Coordinate>, Boolean> visibilityMap = new HashMap<>();

    public CompleteVisibleDecorator(OctiMatchStrategy underlyingStrategy) {
        super(underlyingStrategy);
    }

    private void logVisibility(){
        for (Map.Entry<Pair<Coordinate, Coordinate>, Boolean> e : visibilityMap.entrySet()) {
            logger.trace(e.getKey().first() + " can see " + e.getKey().second() + " : " + e.getValue().toString());
        }
    }
    /**
     * @param srcPoint
     * @param targetPoint
     * @return
     */
    private boolean checkVisible(Coordinate srcPoint, Coordinate targetPoint){
        Boolean preComputed = visibilityMap.get(new Pair<>(srcPoint,targetPoint));
        if(preComputed != null) return preComputed;

        LineString connectingLine = OctiGeometryFactory.OCTI_FACTORY.createLineString(new Coordinate[]{srcPoint,targetPoint});
        Geometry sourceIntersection = source.intersection(connectingLine);
        Geometry targetIntersection = target.intersection(connectingLine);
        logger.debug("source intersection is a " + sourceIntersection.getGeometryType());

        if( (sourceIntersection instanceof Point) && ((Point)sourceIntersection).getCoordinate().equals(srcPoint) &&
            (targetIntersection instanceof Point) && ((Point)targetIntersection).getCoordinate().equals(targetPoint))
        {
            logger.debug(srcPoint + "can see " + targetPoint);
            visibilityMap.put(new Pair<>(srcPoint,targetPoint),true);
            return true;
        }else{
            visibilityMap.put(new Pair<>(srcPoint,targetPoint),false);
            logger.debug(srcPoint + "can't see " + targetPoint);
            return false;
        }
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        underlyingStrategy.initStrategy(sourceString, targetString);
        source = sourceString;
        target = targetString;
    }

    @Override
    public double match(MatrixElement previous,OctiLineSegment sourceSegment, OctiLineSegment targetSegment){
        if(sourceSegment.getOrientation() != targetSegment.getOrientation()) logger.error("match operation called with non-matching octisegments");
        boolean startVisible = checkVisible(sourceSegment.p0,targetSegment.p0);
        boolean endVisible = checkVisible(sourceSegment.p1, targetSegment.p1);
        if(! (startVisible && endVisible)){
            logger.info("visibility constraint not met");
            return OctiLineMatcher.IMPOSSIBLE;
        }
        return underlyingStrategy.match(previous,sourceSegment,targetSegment);
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point){

        boolean startVisible = checkVisible(segmentToBeDeleted.p0,point);
        boolean endVisible = checkVisible(segmentToBeDeleted.p1, point);
        if(! (startVisible && endVisible)){
            logger.info("visibility constraint not met");
            return OctiLineMatcher.IMPOSSIBLE;
        }
        return underlyingStrategy.deleteOnto(previous,segmentToBeDeleted,point);
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated){
        boolean startVisible = checkVisible(creationPoint,segmentToBeCreated.p0);
        boolean endVisible = checkVisible(creationPoint, segmentToBeCreated.p1);
        if(! (startVisible && endVisible)) {
            logger.info("visibility constraint not met");
            return OctiLineMatcher.IMPOSSIBLE;
        }
        return underlyingStrategy.createFrom(previous,creationPoint,segmentToBeCreated);
    }
}
