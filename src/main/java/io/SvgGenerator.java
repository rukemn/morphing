package io;

import morph.OctiSegmentAlignment;
import morph.OctiStringAlignment;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.twak.utils.Pair;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

import java.awt.*;
import java.util.Iterator;
import java.util.List;


/**
 * todo maybe make Config a Builder-Pattern
 */
public class SvgGenerator {

    public static class Config {
        public Config(boolean showSource, boolean showTarget, boolean showAnimation,
                      Color sourceColor, Color targetColor, Color animationColor, Color startPointColor, Color backgroundColor) {
            this.showSource = showSource;
            this.showTarget = showTarget;
            this.showAnimation = showAnimation;
            this.sourceColor = sourceColor;
            this.targetColor = targetColor;
            this.animationColor = animationColor;
            this.startPointColor = startPointColor;
            this.backgroundColor = backgroundColor;
        }
        public boolean showSource;
        public boolean showTarget;
        public boolean showAnimation;
        public Color sourceColor;
        public Color targetColor;
        public Color animationColor;
        public Color startPointColor;
        public Color backgroundColor;
    }

    private Config config = new Config(false, false, true, Color.green, Color.blue, Color.red, Color.magenta, Color.white);
    private static double animationDurationTime = 5;
    private static final Logger logger = LogManager.getLogger();

    public SvgGenerator(){ }

    public SvgGenerator(Config config){
        this.config = config;
    }

    /**Pauses all animations in the document and returns its current animation progress/time
     * 0 represents the
     *
     * @param doc
     * @return the point in time the animation was paused at
     */
    public synchronized static float pause(SVGDocument doc){
        SVGOMDocument omDoc = (SVGOMDocument) doc;
        SVGSVGElement svgEl = omDoc.getRootElement();
        if(omDoc.getRootElement().animationsPaused()) {
            logger.warn("setting to running");
            omDoc.getRootElement().unpauseAnimations();
        }else { // is running
            logger.warn("setting to Pause");
            omDoc.getRootElement().pauseAnimations();
        }
        logger.warn("paused at" + svgEl.getCurrentTime());
        return svgEl.getCurrentTime() % (float) animationDurationTime;
    }

    /**
     * Sets the animation Time to the value specified in the interval [0,1) ,
     *  where 0 equals the start and 1 the start of the next animation loop.
     *  If the end is desired call this function with a value just below 1
     *
     * if a value smaller than 0 is given , the animation time is set to its start
     * if a value bigger than 1 is given, the animation time is set to its end (via time = 0.0.9999999f)
     *
     * @param time the time
     */
    public static void setAnimationTime(SVGDocument doc, float time){
        if(time < 0) time = 0.0f;
        if(time > 1 || time == 1) time = 0.9999999999f;
        time *= animationDurationTime;

        //cast it into stoppable svg
        SVGOMDocument omDoc = (SVGOMDocument) doc;
        omDoc.getRootElement().setCurrentTime(time);
        logger.trace("set animationTime: " + time);
    }

    public static float getAnimationTime(SVGDocument doc){
        SVGOMDocument omDoc = (SVGOMDocument) doc;
        float time = omDoc.getRootElement().getCurrentTime();
        logger.trace("time" + time);
        time %= animationDurationTime; //remainder
        time /= animationDurationTime; //scale to [0,1)
        logger.trace("time in animation" + time);
        return time;
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
        fillSvgDocument(doc, alignments, true);
        return doc;
    }

    public SVGDocument generateSVG(List<OctiStringAlignment> alignmentList){
        SVGDocument doc = createSVGDocument();
        fillSvgDocument(doc, alignmentList);
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
    private SVGDocument setBackgroundColor(SVGDocument doc, Color color, double startX, double startY){
      Element backgroundRect = doc.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "rect");
        backgroundRect.setAttributeNS(null, "x", String.valueOf(startX));
        backgroundRect.setAttributeNS(null, "y", String.valueOf(startY));
        backgroundRect.setAttributeNS(null, "width", "100%");
        backgroundRect.setAttributeNS(null, "height", "100%");
        if(color != null) backgroundRect.setAttributeNS(null,"fill", "rgb(" + color.getRed() + ", "+ color.getGreen() + ", "+ color.getBlue() + ")");
        doc.getDocumentElement().appendChild(backgroundRect);

        doc.getDocumentElement().insertBefore(backgroundRect,doc.getDocumentElement().getFirstChild());

        return doc;
    }

    private SVGDocument setViewBox(SVGDocument doc, Envelope env) {
        Element svgRoot = doc.getDocumentElement();
        double margin = 0.1;
        double marginX = env.getWidth() * margin;
        double marginY = env.getHeight() * margin;

        //make it int
        int viewBoxStartX = Double.valueOf(Math.floor(env.getMinX() - marginX)).intValue();
        int viewBoxStartY = Double.valueOf(Math.floor(env.getMinY() - marginY)).intValue();
        int viewBoxWidth = Double.valueOf(Math.ceil(env.getWidth() + 2 * marginX)).intValue();
        int viewBoxHeight = Double.valueOf(Math.ceil(env.getHeight() + 2 * marginY)).intValue();

        String viewBoxValue = "" + viewBoxStartX + " " + viewBoxStartY + " " +
                                    viewBoxWidth + " " + viewBoxHeight;
        logger.warn("viewBox: " + viewBoxValue);
        svgRoot.setAttributeNS(null, "viewBox", viewBoxValue);

        setBackgroundColor(doc, config.backgroundColor,viewBoxStartX, viewBoxStartY);
        return doc;
    }

    private SVGDocument fillSvgDocument(SVGDocument doc, List<OctiStringAlignment> alignmentList){
        Envelope env = new Envelope();
        for(OctiStringAlignment alignment : alignmentList) {
            fillSvgDocument(doc,alignment, false);
            env.expandToInclude(alignment.getEnvelope());

        }
        setViewBox(doc,env);
        return doc;

    }

    private SVGDocument fillSvgDocument(SVGDocument doc, OctiStringAlignment alignment, boolean setViewBox) {
        for (OctiSegmentAlignment sa : alignment) {
            logger.trace(sa.getOrientation() + ", " + sa.getOperation());
        }
        String svgNameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element alignmentGroup = doc.createElementNS(svgNameSpace, "g");



        if (config.showAnimation) {
            Pair<String, String> svgAlignmentStrings = parse(alignment);
            Element animation = createAnimationElement(doc, svgAlignmentStrings.first(), svgAlignmentStrings.second(), "ANIMATION-" + alignment.getId(), config.animationColor);
            alignmentGroup.appendChild(animation);
        }

        if (config.showSource) {
            Element polylineSrc = createPolyLineElement(doc, alignment.getSourceSequence(), "SOURCE-" + alignment.getId(), config.sourceColor, config.startPointColor);
            alignmentGroup.appendChild(polylineSrc);
        }
        if (config.showTarget) {
            Element polylineTar = createPolyLineElement(doc, alignment.getTargetSequence(),"TARGET-" + alignment.getId(), config.targetColor, config.startPointColor);
            alignmentGroup.appendChild(polylineTar);
        }

        doc.getDocumentElement().appendChild(alignmentGroup);

        if(setViewBox) setViewBox(doc,alignment.getEnvelope());
        return doc;
    }

    /**
     * Creates an svg polyline-Element
     *
     * @param creationDoc the document used to create elements
     * @param from        the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param to          the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param id          the id the Polyline-Element is tagged with
     * @param color       the color in which the linestring is to be painted
     * @return the newly created polyline-Element
     */
    public Element createAnimationElement(SVGDocument creationDoc, String from, String to,String id, Color color) {
        String svgNameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element polyGroup = creationDoc.createElementNS(svgNameSpace, "g");
        Element polyLineElement = creationDoc.createElementNS(svgNameSpace, "polyline");

        polyLineElement.setAttributeNS(null, "id", id);
        polyLineElement.setAttributeNS(null, "stroke", "rgb(" + color.getRed() + ", "+ color.getGreen() + ", "+ color.getBlue() + ")");
        polyLineElement.setAttributeNS(null, "stroke-width", "1%");
        polyLineElement.setAttributeNS(null, "fill", "none");
        polyLineElement.setAttributeNS(null, "points", ""); // animate

        Element animateElement = creationDoc.createElementNS(svgNameSpace, "animate");
        animateElement.setAttributeNS(null, "attributeName", "points");
        animateElement.setAttributeNS(null, "dur", animationDurationTime + "s");
        animateElement.setAttributeNS(null, "repeatCount", "indefinite");
        animateElement.setAttributeNS(null, "from", from);
        animateElement.setAttributeNS(null, "to", to);
        polyLineElement.appendChild(animateElement);
        String markerId = "MARKER-" +id;
        Element markerElement = createMarkers(creationDoc, markerId, config.animationColor);
        polyGroup.appendChild(markerElement);
        polyLineElement.setAttributeNS(null, "marker-mid", "url(#" + markerId + ")");

        String startMarkerId = "MARKERSTART-" + id;
        Element startMarkerElement = createMarkers(creationDoc, startMarkerId, config.startPointColor);
        polyGroup.appendChild(startMarkerElement);
        polyLineElement.setAttributeNS(null, "marker-start", "url(#" + startMarkerId + ")");

        polyGroup.appendChild(polyLineElement);

        return polyGroup;
    }

    /**
     * Creates an svg Polyline-Element
     *
     * @param creationDoc the document used to create elements
     * @param coordinates  the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param id          the id the Polyline-Element is tagged with
     * @param baseColor       the color in which the linestring is to be painted
     * @return the newly created polyline-Element
     */
    public Element createPolyLineElement(SVGDocument creationDoc, CoordinateSequence coordinates,String id, Color baseColor, Color startPointColor) {
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;

        Element polyGroup = creationDoc.createElementNS(nameSpace, "g");
        Element polyLineElement = creationDoc.createElementNS(nameSpace, "polyline");

        polyLineElement.setAttributeNS(null, "id", id);
        polyLineElement.setAttributeNS(null, "stroke", "rgb(" + baseColor.getRed() + ", "+ baseColor.getGreen() + ", "+ baseColor.getBlue() + ")");
        polyLineElement.setAttributeNS(null, "stroke-width", "1%");
        polyLineElement.setAttributeNS(null, "fill", "none");

        StringBuilder sb = new StringBuilder();
        for (Coordinate c : coordinates.toCoordinateArray()) {
            sb.append(c.x).append(",").append(c.y).append(" ");
        }
        String polyLineString = sb.substring(0, sb.length() - 1);
        polyLineElement.setAttributeNS(null, "points", polyLineString);

        String baseMarkerId = "MARKER-" + id;
        String startPointmarkerId = "MARKER-start-" +id;
        Element markerElement = createMarkers(creationDoc, baseMarkerId, baseColor);
        Element startMarkerElement = createMarkers(creationDoc, startPointmarkerId, startPointColor);
        polyGroup.appendChild(markerElement);
        polyGroup.appendChild(startMarkerElement);
        polyLineElement.setAttributeNS(null, "marker-mid", "url(#" + baseMarkerId + ")");
        polyLineElement.setAttributeNS(null, "marker-start", "url(#" + startPointmarkerId + ")");
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
    public Element createMarkers(SVGDocument creationDoc, String id, Color color) {
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element marker = creationDoc.createElementNS(nameSpace, "marker");

        marker.setAttributeNS(null, "id", id);
        marker.setAttributeNS(null, "markerWidth", "2");
        marker.setAttributeNS(null, "markerHeight", "2");
        marker.setAttributeNS(null, "refX", "1");
        marker.setAttributeNS(null, "refY", "1");
        //marker.setAttributeNS(null, "markerUnits", "userSpaceOnUse");
        //marker.setAttributeNS(null, "viewport", );

        //now the shape
        Element circle = creationDoc.createElementNS(nameSpace, "circle");
        circle.setAttributeNS(null, "cx", "1");
        circle.setAttributeNS(null, "cy", "1");
        circle.setAttributeNS(null, "r", "1");
        circle.setAttributeNS(null, "stroke", "none");
        circle.setAttributeNS(null, "fill", "rgb(" + color.getRed() + ", "+ color.getGreen() + ", "+ color.getBlue() + ")");

        marker.appendChild(circle);
        return marker;
    }

}
