package morph;

import io.SvgPolygonExtractor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class SvgPolygonExtractorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "M 192 640 L 192 704 L 256 704 L 256 640 Z M 192 640 ",
            "M 192 640 L 192 704 L 256 704 L 256 640 Z",
            "M 100 100 M 192 640 L 192 704 L 256 704 L 256 640 Z"})
    void testparsePath(String pathString) {
        List<Coordinate> expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coordinate(192.0, 640.0));
        expectedCoords.add(new Coordinate(192.0, 704.0));
        expectedCoords.add(new Coordinate(256.0, 704));
        expectedCoords.add(new Coordinate(256.0, 640.0));
        expectedCoords.add(new Coordinate(192.0, 640.0));

        SvgPolygonExtractor importer = new SvgPolygonExtractor();

        Assertions.assertIterableEquals(expectedCoords, importer.parsePath(pathString));
        //Assertions.assertEquals(expectedCoords,importer.parsePath(pathString));
    }
}
