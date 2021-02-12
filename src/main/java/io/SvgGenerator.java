package io;

import jtsadaptions.OctiLineString;
import morph.OctiSegmentAlignment;
import morph.OctiStringAlignment;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.twak.utils.Pair;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import java.awt.*;
import java.util.Iterator;


/**
 * todo maybe make Config a Builder-Pattern
 */
public class SvgGenerator {

    public static class Config {

        public Config(boolean showSource, boolean showTarget, boolean showAnimation,
                      String sourceColor, String targetColor, String animationColor, String startPointColor) {

            this.showSource = showSource;
            this.showTarget = showTarget;
            this.showAnimation = showAnimation;
            this.sourceColor = sourceColor;
            this.targetColor = targetColor;
            this.animationColor = animationColor;
            this.startPointColor = startPointColor;
        }

        public boolean showSource;
        public boolean showTarget;
        public boolean showAnimation;
        public String sourceColor;
        public String targetColor;
        public String animationColor;
        public String startPointColor;
    }

    private Config config = new Config(true, true, true, "blue", "green", "red", "purple");
    private static final Logger logger = LogManager.getLogger();

    public SvgGenerator(){ }

    public SvgGenerator(Config config){
        this.config = config;
    }

    private Pair<String, String> parse(OctiStringAlignment alignments) {
        StringBuilder srcBuilder = new StringBuilder();
        StringBuilder tarBuilder = new StringBuilder();

        Iterator<OctiSegmentAlignment> segmentIter = alignments.iterator();
        if (segmentIter.hasNext()) {
            OctiSegmentAlignment first = segmentIter.next();
            srcBuilder.append(first.getSourceStart().x).append(",").append(first.getSourceStart().y).append(" ");
            tarBuilder.append(first.getTargetStart().x).append(",").append(first.getTargetStart().y).append(" ");

            srcBuilder.append(first.getSourceEnd().x).append(",").append(first.getSourceEnd().y).append(" ");
            tarBuilder.append(first.getTargetEnd().x).append(",").append(first.getTargetEnd().y).append(" ");
        }

        while (segmentIter.hasNext()) {
            OctiSegmentAlignment alignment = segmentIter.next();
            srcBuilder.append(alignment.getSourceEnd().x).append(",").append(alignment.getSourceEnd().y).append(" ");
            tarBuilder.append(alignment.getTargetEnd().x).append(",").append(alignment.getTargetEnd().y).append(" ");

        }

        String sourceString = srcBuilder.toString();
        String targetString = tarBuilder.toString();
        logger.trace(sourceString);
        logger.trace(targetString);
        return new Pair<>(sourceString, targetString);
    }

    public SVGDocument generateSVG(OctiStringAlignment alignments) {
        SVGDocument doc = createSVGDocument();
        fillSvgDocument(doc, alignments);
        return doc;

    }

    public SVGDocument createSVGDocument() {
        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        String svgNameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        SVGDocument doc = (SVGDocument) impl.createDocument(svgNameSpace, "svg", null);


        return doc;
    }

    /** returns the dimensions of the doc
     * maybe move to more appropriate place
     *      currently searching for:
     *      <l>
     *          <li>viewBox</li>
     *      </l>
     *
     * @param doc
     * @return
     */
    public static Dimension retrieveDimension(SVGDocument doc){
        int width = Integer.parseInt(doc.getDocumentElement().getAttributeNodeNS(null,"viewBox").getValue().split(" ")[2]);
        int height = Integer.parseInt(doc.getDocumentElement().getAttributeNodeNS(null,"viewBox").getValue().split(" ")[3]);
        logger.debug("width "+ width + " height " + height);
        return new java.awt.Dimension(width,height);

    }

    /** mostly debuggin purposes
     * no background is set if color == null
     *
     * @param doc the doc to have its background set
     * @param color the background color
     * @param startX describes to top-left corner's x-Coordinate
     * @param startY describes to top-left corner's y-Coordinate
     * @return the modified document
     */
    private SVGDocument setBackgroundColor(SVGDocument doc, String color, double startX, double startY){
        Element backgroundRect = doc.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "rect");
        backgroundRect.setAttributeNS(null, "x", String.valueOf(startX));
        backgroundRect.setAttributeNS(null, "y", String.valueOf(startY));
        backgroundRect.setAttributeNS(null, "width", "100%");
        backgroundRect.setAttributeNS(null, "height", "100%");
        if(color != null) backgroundRect.setAttributeNS(null,"fill", "yellow");
        doc.getDocumentElement().appendChild(backgroundRect);
        return doc;
    }

    private SVGDocument setViewBox(SVGDocument doc, OctiStringAlignment alignment) {
        Element svgRoot = doc.getDocumentElement();
        Envelope env = alignment.getEnvelope();
        double margin = 0.1;
        double marginX = env.getWidth() * margin;
        double marginY = env.getHeight() * margin;

        //make it int
        int viewBoxStartX = Double.valueOf(Math.floor(env.getMinX() - marginX)).intValue();
        int viewBoxStartY = Double.valueOf(Math.floor(env.getMinY() - marginY)).intValue();
        int viewBoxWidth = Double.valueOf(env.getWidth() + 2 * marginX).intValue();
        int viewBoxHeight = Double.valueOf(env.getHeight() + 2 * marginY).intValue();

        String viewBoxValue = "" + viewBoxStartX + " " + viewBoxStartY + " " +
                                    viewBoxWidth + " " + viewBoxHeight;
        logger.warn("viewBox" + viewBoxValue);
        svgRoot.setAttributeNS(null, "viewBox", viewBoxValue);

        setBackgroundColor(doc,"yellow", viewBoxStartX, viewBoxStartY);

        return doc;
    }

    private SVGDocument fillSvgDocument(SVGDocument doc, OctiStringAlignment alignment) {
        for (OctiSegmentAlignment sa : alignment) {
            logger.trace(sa.getOrientation() + ", " + sa.getOperation());
        }
        Pair<String, String> svgAlignmentStrings = parse(alignment);

        setViewBox(doc,alignment);


        if (config.showAnimation) {
            Element animation = createAnimationElement(doc, svgAlignmentStrings.first(), svgAlignmentStrings.second(), config.animationColor);
            doc.getDocumentElement().appendChild(animation);
        }

        if (config.showSource) {
            Element polylineSrc = createPolyLineElement(doc, alignment.getSourceString(), config.sourceColor);
            doc.getDocumentElement().appendChild(polylineSrc);
        }
        if (config.showTarget) {
            Element polylineTar = createPolyLineElement(doc, alignment.getTargetString(), config.targetColor);
            doc.getDocumentElement().appendChild(polylineTar);
        }
        return doc;
    }

    /**
     * Creates an svg polyline-Element
     *
     * @param creationDoc the document used to create elements
     * @param from        the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param to          the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param color       the color in which the linestring is to be painted
     * @return the newly created polyline-Element
     */
    public Element createAnimationElement(SVGDocument creationDoc, String from, String to, String color) {
        String svgNameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element polyGroup = creationDoc.createElementNS(svgNameSpace, "g");
        Element polyLineElement = creationDoc.createElementNS(svgNameSpace, "polyline");

        polyLineElement.setAttributeNS(null, "stroke", color);
        polyLineElement.setAttributeNS(null, "stroke-width", "3");
        polyLineElement.setAttributeNS(null, "fill", "none");

        Element animateElement = creationDoc.createElementNS(svgNameSpace, "animate");
        animateElement.setAttributeNS(null, "attributeName", "points");
        animateElement.setAttributeNS(null, "dur", "5s");
        animateElement.setAttributeNS(null, "repeatCount", "indefinite");
        animateElement.setAttributeNS(null, "from", from);
        animateElement.setAttributeNS(null, "to", to);
        polyLineElement.appendChild(animateElement);

        String markerId = color + "Circle";
        String startMarkerId = color + "StartCircle";
        Element markerElement = createMarkers(creationDoc, markerId, color);
        Element startMarkerElement = createMarkers(creationDoc, startMarkerId, config.startPointColor);
        polyGroup.appendChild(markerElement);
        polyGroup.appendChild(startMarkerElement);
        polyLineElement.setAttributeNS(null, "marker-mid", "url(#" + markerId + ")");
        polyLineElement.setAttributeNS(null, "marker-start", "url(#" + startMarkerId + ")");
        polyGroup.appendChild(polyLineElement);

        return polyGroup;
    }

    /**
     * Creates an svg Polyline-Element
     *
     * @param creationDoc the document used to create elements
     * @param lineString  the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param color       the color in which the linestring is to be painted
     * @return the newly created polyline-Element
     */
    public Element createPolyLineElement(SVGDocument creationDoc, OctiLineString lineString, String color) {
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;

        Element polyGroup = creationDoc.createElementNS(nameSpace, "g");
        Element polyLineElement = creationDoc.createElementNS(nameSpace, "polyline");

        polyLineElement.setAttributeNS(null, "stroke", color);
        polyLineElement.setAttributeNS(null, "stroke-width", "3");
        polyLineElement.setAttributeNS(null, "fill", "none");

        StringBuilder sb = new StringBuilder();
        for (Coordinate c : lineString.getCoordinates()) {
            sb.append(c.x).append(",").append(c.y).append(" ");
        }
        String polyLineString = sb.substring(0, sb.length() - 1);
        polyLineElement.setAttributeNS(null, "points", polyLineString);

        String markerId = color + "Circle";
        Element markerElement = createMarkers(creationDoc, markerId, color);
        polyGroup.appendChild(markerElement);
        polyLineElement.setAttributeNS(null, "marker-mid", "url(#" + markerId + ")");
        polyGroup.appendChild(polyLineElement);

        return polyGroup;
    }

    /**
     * Creates an svg marker-Element
     *
     * @param creationDoc the document used to create elements
     * @param id          the id with which the marker element can be referenced
     * @param color       the color in which the markers are to be painted
     * @return the created marker-Element
     */
    public Element createMarkers(SVGDocument creationDoc, String id, String color) {
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element marker = creationDoc.createElementNS(nameSpace, "marker");

        marker.setAttributeNS(null, "id", id);
        marker.setAttributeNS(null, "markerWidth", "4");
        marker.setAttributeNS(null, "markerHeight", "4");
        marker.setAttributeNS(null, "refX", "2");
        marker.setAttributeNS(null, "refY", "2");

        //now the shape
        Element circle = creationDoc.createElementNS(nameSpace, "circle");
        circle.setAttributeNS(null, "cx", "2");
        circle.setAttributeNS(null, "cy", "2");
        circle.setAttributeNS(null, "r", "2");
        circle.setAttributeNS(null, "stroke", "none");
        circle.setAttributeNS(null, "fill", color);

        marker.appendChild(circle);
        return marker;
    }

}
