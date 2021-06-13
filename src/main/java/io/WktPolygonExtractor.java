package io;

import jtsadaptions.OctiGeometryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * parses a csv file containing a Geometry in WKT-Format and extracts its geometries
 */
public class WktPolygonExtractor implements PolygonExtractorInterface{
    private final Logger logger = LogManager.getLogger();

    private boolean withHeader = true;
    private String wktColumnName = "WKT";
    private int wktColumnIndex;

    private List<Geometry> geometries = new ArrayList<>();

    @Override
    public void parseFile(URI uri) throws IOException, FileParseException {
        logger.trace("uri path: " + uri.getPath());
        FileReader fr = new FileReader(uri.getPath());
        CSVReader reader = new CSVReader(fr);

        String[] line;
        int lineIndex = 0;

        try {
            //header
            if (withHeader&& ((line = reader.readNext()) != null)) {
                boolean foundHeader = false;
                for (int idx = 0; idx < line.length; idx++) {
                    if (line[idx].equals(wktColumnName)) {
                        wktColumnIndex = idx;
                        foundHeader = true;
                    }
                    break;
                }
                if( ! foundHeader) throw new FileParseException("couldn't find suitable wkt column header");
            }
        }catch(IOException | CsvValidationException e){
            throw new FileParseException("couldnt parse header");
        }

        //content
        try{
            while ((line = reader.readNext()) != null) {
                if (line.length < wktColumnIndex) {
                    logger.warn("line " + lineIndex + " doesn't have enough entries to parse WKT geometries");
                    throw new FileParseException("line " + lineIndex + " doesn't have enough entries to parse WKT geometries");
                }

                String wkt = line[wktColumnIndex];
                parseWkt(wkt);

                lineIndex++;
            }
        } catch (IOException | CsvValidationException e ){
            logger.warn("io or validation exception, while parsing, check ressource " + uri.toString());
        } catch (ParseException e) {
            logger.warn("wkt in line " + lineIndex + " is not a geometry.");
            e.printStackTrace();
        }
        reader.close();
    }

    @Override
    public int numberOfParsedGeometries() {
        return geometries.size();
    }

    @Override
    public Geometry getNthGeometry(int index) {
        return geometries.get(index);
    }

    @Override
    public List<Geometry> getGeometryList() {
        return geometries;
    }

    public void parseFile(String path) throws URISyntaxException, IOException, FileParseException {
        parseFile( new URI(path));
    }

    private void parseWkt(String wkt) throws ParseException {
        /*precision to reduce extracted geometries precision, data seems to be wonky, which leads to non octilinear
            edges. Reducing the precision too much leads to invalid topologies
         */
        WKTReader wktReader = new WKTReader(OctiGeometryFactory.OCTI_FACTORY);
        Geometry readGeom = wktReader.read(wkt);
        //GeometryPrecisionReducer.reduce(readGeom,pm);

        geometries.add(readGeom);
    }
}
