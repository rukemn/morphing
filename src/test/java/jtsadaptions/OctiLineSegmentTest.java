package jtsadaptions;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import static org.junit.jupiter.api.Assertions.*;

public class OctiLineSegmentTest {

    @Test
    void testOrientation_upRight() {
        Coordinate x = new Coordinate(-1.0, -1.0);
        Coordinate y = new Coordinate(1.0, 1.0);

        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.UP_RIGHT, segment.getOrientation());
    }

    @Test
    void testOrientation_upLeft() {
        Coordinate x = new Coordinate(1.0, -1.0);
        Coordinate y = new Coordinate(-1.0, 1.0);

        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.UP_LEFT, segment.getOrientation());
    }

    @Test
    void testOrientation_up() {
        Coordinate x = new Coordinate(1.0, -1.0);
        Coordinate y = new Coordinate(1.0, 1.0);


        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.UP, segment.getOrientation());
    }

    @Test
    void testOrientation_downRight() {
        Coordinate x = new Coordinate(-1.0, 1.0);
        Coordinate y = new Coordinate(1.0, -1.0);


        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.DOWN_RIGHT, segment.getOrientation());
    }

    @Test
    void testOrientation_downLeft() {
        Coordinate x = new Coordinate(1.0, 1.0);
        Coordinate y = new Coordinate(-1.0, -1.0);

        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.DOWN_LEFT, segment.getOrientation());
    }

    @Test
    void testOrientation_down() {
        Coordinate x = new Coordinate(1.0, 1.0);
        Coordinate y = new Coordinate(1.0, -1.0);

        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.DOWN, segment.getOrientation());
    }

    @Test
    void testOrientation_left() {
        Coordinate x = new Coordinate(1.0, 1.0);
        Coordinate y = new Coordinate(-1.0, 1.0);

        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.LEFT, segment.getOrientation());
    }

    @Test
    void testOrientation_right() {
        Coordinate x = new Coordinate(-1.0, 1.0);
        Coordinate y = new Coordinate(1.0, 1.0);

        OctiLineSegment segment = new OctiLineSegment(x, y);
        assertEquals(OctiLineSegment.Orientation.RIGHT, segment.getOrientation());
    }

    @Test
    void testNotOctilinearOrientation() {
        Coordinate x = new Coordinate(-1.1, -1.0);
        Coordinate y = new Coordinate(1.0, 1.0);

        assertThrows(LineSegmentNotOctilinear.class, () -> new OctiLineSegment(x, y));

    }
}
