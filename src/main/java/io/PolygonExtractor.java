package io;

import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class PolygonExtractor implements PolygonExtractorInterface{
    private static final Logger logger = LogManager.getLogger();
    private PolygonExtractorInterface extractorStrategy;

    @Override
    public void parseFile(URI uri) throws FileParseException, IOException {
        switch (getFileExtension(uri.toString())){
            case "svg":
                extractorStrategy = new SvgPolygonExtractor();
                logger.trace("parsing svg file");
                break;
            case "csv":
                logger.trace("parsing csv file");
                extractorStrategy = new WktPolygonExtractor();
                break;
            default:
                throw new IOException("unsupported file extension");
        }
        extractorStrategy.parseFile(uri);
    }

    @Override
    public int numberOfParsedGeometries() {
        return extractorStrategy.numberOfParsedGeometries();
    }

    @Override
    public Geometry getNthGeometry(int index) {
        Geometry geom = extractorStrategy.getNthGeometry(index);
        logger.debug("returning:" + geom.toText());
        return geom;
    }

    @Override
    public List<Geometry> getGeometryList() {
        return extractorStrategy.getGeometryList();
    }

    /** Derives the context from the file extension to apply the correct strategy
     *
     * @param fullFileName the files name
     * @return the extension, excluding the '.'
     */
    private String getFileExtension(String fullFileName) {
        int lastDotIndex = fullFileName.lastIndexOf('.');
        int fileNameStartWithoutDir = Math.max(fullFileName.lastIndexOf('/'), fullFileName.lastIndexOf('\\'));
        return lastDotIndex > fileNameStartWithoutDir ? fullFileName.substring(lastDotIndex + 1) : "";
    }
}
