package jtsadaptions;

import morph.MatrixElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scoringStrategies.VertexDistanceStrategy;
import scoringStrategies.OctiMatchStrategy;
import scoringStrategies.CompleteVisibleDecorator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;


public class OctiLineSegment extends LineSegment implements Octilinear{
    private static final Logger logger = LogManager.getLogger();
    private Orientation orientation = null;
    public static OctiMatchStrategy strategy = new CompleteVisibleDecorator(new VertexDistanceStrategy());

    public enum Orientation {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        UP_RIGHT,
        UP_LEFT,
        DOWN_RIGHT,
        DOWN_LEFT;

        static{
            UP.opposite = DOWN;
            DOWN.opposite = UP;
            LEFT.opposite = RIGHT;
            RIGHT.opposite = LEFT;
            UP_RIGHT.opposite = DOWN_LEFT;
            DOWN_RIGHT.opposite = UP_LEFT;
            UP_LEFT.opposite = DOWN_RIGHT;
            DOWN_LEFT.opposite = UP_RIGHT;
        }
        private Orientation opposite;
        public Orientation opposite(){
            return this.opposite;
        }
    }

    public static void setStrategy(OctiMatchStrategy strategy, OctiLineString source, OctiLineString target){
        OctiLineSegment.strategy = strategy;
        strategy.initStrategy(source,target);
    }

    @Override
    public boolean checkOctilinear() {
        determineOrientation(this);
        return true;
    }

    public OctiLineSegment(Coordinate p0, Coordinate p1) {
        super(p0, p1);
        //determineOrientation(this);
        checkOctilinear();
    }

    /**
     * Determines the segments orientation and sets its orientation accordingly
     * @param segment the segment to check
     * @return the octilinear orientation
     * @throws LineSegmentNotOctilinear in case the segment isn't octilinear
     */
    public static Orientation determineOrientation(OctiLineSegment segment) throws LineSegmentNotOctilinear{
        Orientation orientation = null;

        if (segment.isHorizontal()) {
            if (segment.p0.x < segment.p1.x)
                orientation = Orientation.RIGHT;
            else
                orientation = Orientation.LEFT;

        } else if (segment.isVertical()) {
            if (segment.p0.y < segment.p1.y)
                orientation = Orientation.UP;
            else
                orientation = Orientation.DOWN;

            //diagonals
        } else if ((segment.p1.x - segment.p0.x) == (segment.p1.y - segment.p0.y)) {
            if (segment.p1.x > segment.p0.x)
                orientation = Orientation.UP_RIGHT;
            else
                orientation = Orientation.DOWN_LEFT;
        } else if ((segment.p1.x - segment.p0.x) == -(segment.p1.y - segment.p0.y)) {
            if (segment.p1.x > segment.p0.x)
                orientation = Orientation.DOWN_RIGHT;
            else
                orientation = Orientation.UP_LEFT;
        }

        if(orientation == null){
            throw new LineSegmentNotOctilinear(segment);
        }
        segment.orientation = orientation;
        return orientation;
    }

    public Orientation getOrientation() {
        if (orientation != null) return orientation;
        return determineOrientation(this);
    }

    public static double match(MatrixElement previous, OctiLineSegment segment1, OctiLineSegment segment2){
        return strategy.match(previous,segment1,segment2);
    }

    public static double deleteOnto(MatrixElement previous, OctiLineSegment segmentToBeDeleted, Coordinate point){
        logger.trace("deletion row " + previous.deletionsInARow);
        return strategy.deleteOnto(previous,segmentToBeDeleted,point);
    }

    public static double createFrom(MatrixElement previous, Coordinate creationPoint, OctiLineSegment segmentToBeCreated){
        logger.trace("creation row " + previous.deletionsInARow);
        return strategy.createFrom(previous,creationPoint, segmentToBeCreated);
    }
}
