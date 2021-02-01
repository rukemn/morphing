package jtsadaptions;

import morph.OctiLineMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import static org.junit.jupiter.api.Assertions.*;

class OctiLineStringTest {

    @Test
    void makeNthPointTheFirst_firstElement() {

        OctiLineString ols = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3)

        });
        OctiLineString expected = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3),
                new Coordinate(0, 0)

        });
        OctiLineString actual = ols.makeNthPointTheFirst(1);
        Assertions.assertArrayEquals(expected.getCoordinates(),actual.getCoordinates());
    }

    @Test
    void makeNthPointTheFirst() {
        OctiLineString ols = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3),

        });

        OctiLineString expected = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(2, 2),
                new Coordinate(3, 3),
                new Coordinate(0, 0),
                new Coordinate(1, 1),

        });
        OctiLineString actual = ols.makeNthPointTheFirst(2);
        Assertions.assertArrayEquals(expected.getCoordinates(),actual.getCoordinates());
    }

    @Test
    void makeNthPointTheFirst_lastElement() {
        OctiLineString ols = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3),

        });

        OctiLineString expected = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(3, 3),
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),

        });
        OctiLineString actual = ols.makeNthPointTheFirst(3);
        Assertions.assertArrayEquals(expected.getCoordinates(),actual.getCoordinates());
    }

    @Test
    void makeNthPointTheFirst_closed() {

        OctiLineString ols = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3),
                new Coordinate(0, 0)

        });

        OctiLineString expected = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3),
                new Coordinate(0, 0),
                new Coordinate(1, 1)

        });
        OctiLineString actual = ols.makeNthPointTheFirst(1);
        Assertions.assertArrayEquals(expected.getCoordinates(),actual.getCoordinates());
    }

    @Test
    void makeNthPointTheFirst_closed_LastElement() {
        OctiLineString ols = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3),
                new Coordinate(0, 0)
        });

        OctiLineString expected = OctiGeometryFactory.OCTI_FACTORY.createOctiLineString(new Coordinate[]{
                new Coordinate(3, 3),
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 2),
                new Coordinate(3, 3)
        });

        OctiLineString actual = ols.makeNthPointTheFirst(3);
        Assertions.assertArrayEquals(expected.getCoordinates(),actual.getCoordinates());
    }
}
