package jtsadaptions;

import scoringStrategies.BaseMatchStrategy;
import scoringStrategies.OctiMatchStrategy;
import scoringStrategies.VisibilityMatchStrategy;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import morph.LineSegmentNotOctilinear;


public class OctiLineSegment extends LineSegment implements Octilinear{
    private Orientation orientation = null;
    public static OctiMatchStrategy strategy = new VisibilityMatchStrategy(new BaseMatchStrategy());

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

    public static double match(OctiLineSegment segment1, OctiLineSegment segment2){
        return strategy.match(segment1,segment2);
    }

    public static double deleteOnto(OctiLineSegment segmentToBeDeleted, Coordinate point){
        return strategy.deleteOnto(segmentToBeDeleted,point);
    }

    public static double createFrom(Coordinate creationPoint, OctiLineSegment segmentToBeCreated){
        return strategy.createFrom(creationPoint, segmentToBeCreated);
    }
}
