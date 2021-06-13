package io;

import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface PolygonExtractorInterface {

    public void parseFile(URI uri) throws FileParseException, IOException;
    public int numberOfParsedGeometries();
    public Geometry getNthGeometry(int index);
    public List<Geometry> getGeometryList();
}
