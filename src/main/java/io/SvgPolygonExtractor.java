package io;

import jtsadaptions.OctiGeometryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//todo interface this to allow for more Extractors which can be plugged in
public class SvgPolygonExtractor {

    private static Logger logger = LogManager.getLogger();
    enum action {STARTPOINT, LINETO_SECOND, LINETO_THIRD, NEXTLINE_OR_END}

    private List<Polygon> polygons = new ArrayList<>();

    public static void main(String[] args) {
        SvgPolygonExtractor x = new SvgPolygonExtractor();
        try {
            x.parseSvg("src/main/resources/SquareSquare");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Extracted " + x.numberOfPolygons() + " polygons");
    }

    public void parseSvg(String uri) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        NodeList paths = null;
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document document = docBuilder.parse(uri);

            String xpathExpression = "//path/@d";

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            XPathExpression expression = xpath.compile(xpathExpression);

            paths = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < paths.getLength(); i++) {

                List<Coordinate> points = parsePath(paths.item(i).getNodeValue());
                logger.trace("path " + i + " beginn");
                for (Coordinate c : points) logger.trace("path" + i +": Point " + c.toString());

                Coordinate[] coords = points.toArray(Coordinate[]::new);
                polygons.add(OctiGeometryFactory.OCTI_FACTORY.createPolygon(coords));
            }

        } catch(IOException io) {
            io.printStackTrace();
            throw io;
        }catch(ParserConfigurationException | SAXException |  XPathExpressionException parsing) {
            parsing.printStackTrace();
        }
    }

    public int numberOfPolygons(){
        return polygons.size();
    }
    public Polygon getNthPolygon(int index){
        return polygons.get(index);
    }

    /**
     * Parses valid LinearRings
     * @param svgPathString the svg path string representing a "d" element
     * @return the Coordinates associated with the LinearRing
     * @throws IllegalArgumentException if the argument string can't be parsed
     */
    public List<Coordinate> parsePath(String svgPathString) throws IllegalArgumentException {
        String[] splitted = svgPathString.split(" ");
        List<Coordinate> coords = new ArrayList<>();

        action expectedAction = SvgPolygonExtractor.action.STARTPOINT;

        for (int i = 0; i < splitted.length; i++) {
            logger.trace("token: " + splitted[i]);
            double x_coord, y_coord;

            switch (splitted[i]) {
                case "M":
                    if (expectedAction != SvgPolygonExtractor.action.STARTPOINT &&
                            expectedAction != action.LINETO_SECOND) throw new IllegalArgumentException();
                    if (i + 2 >= splitted.length) throw new IllegalArgumentException();

                    x_coord = Double.parseDouble(splitted[i + 1]);
                    y_coord = Double.parseDouble(splitted[i + 2]);

                    //in case of redundant prefix M's
                    if(expectedAction == action.LINETO_SECOND) coords.remove(0);
                    coords.add(new Coordinate(x_coord, y_coord));

                    expectedAction = SvgPolygonExtractor.action.LINETO_SECOND;
                    i += 2;
                    break;

                case "L":
                    if (expectedAction != action.LINETO_SECOND &&
                        expectedAction != action.LINETO_THIRD &&
                        expectedAction != action.NEXTLINE_OR_END) throw new IllegalArgumentException(); //error

                    if (i + 2 >= splitted.length) return coords;

                    x_coord = Double.parseDouble(splitted[i + 1]);
                    y_coord = Double.parseDouble(splitted[i + 2]);
                    coords.add(new Coordinate(x_coord, y_coord));

                    expectedAction = expectedAction == SvgPolygonExtractor.action.LINETO_SECOND
                            ? SvgPolygonExtractor.action.LINETO_THIRD : SvgPolygonExtractor.action.NEXTLINE_OR_END;

                    i += 2;
                    break;
                case "Z":

                    if (expectedAction != SvgPolygonExtractor.action.NEXTLINE_OR_END)
                        throw new IllegalArgumentException();
                    coords.add(coords.get(0));

                    //finished, ignore all other elements of the path
                    return coords;

                default:
                    logger.error("cant parse token");
                    throw new IllegalArgumentException("unexpected token");
            }
        }
        return coords;
    }

}