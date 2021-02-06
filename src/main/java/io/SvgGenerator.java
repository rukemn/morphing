package io;

import jtsadaptions.OctiLineString;
import morph.OctiSegmentAlignment;
import morph.OctiStringAlignment;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.twak.utils.Pair;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;
import java.util.Iterator;


/**
 * todo envelope function to draw surrounding frame with appropriate sizes
 */
public class SvgGenerator {

    private static final Logger logger = LogManager.getLogger();

    private Pair<String, String> parse(OctiStringAlignment alignments) {
        StringBuilder srcBuilder = new StringBuilder();
        StringBuilder tarBuilder = new StringBuilder();

        Iterator<OctiSegmentAlignment> segmentIter = alignments.iterator();
        if(segmentIter.hasNext()){
            OctiSegmentAlignment first = segmentIter.next();
            srcBuilder.append(first.getSourceStart().x).append(",").append(first.getSourceStart().y).append(" ");
            tarBuilder.append(first.getTargetStart().x).append(",").append(first.getTargetStart().y).append(" ");

            srcBuilder.append(first.getSourceEnd().x).append(",").append(first.getSourceEnd().y).append(" ");
            tarBuilder.append(first.getTargetEnd().x).append(",").append(first.getTargetEnd().y).append(" ");
        }

        while(segmentIter.hasNext()) {
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

    public SVGDocument generateSVG(OctiStringAlignment alignments){
        SVGDocument doc = createSVGDocument();
        Pair<String, String> fromAndTo = parse(alignments);
        fillSvgDocument(doc,alignments);
        return doc;

    }

    public SVGDocument createSVGDocument() {
        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        String svgNameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        SVGDocument doc = (SVGDocument) impl.createDocument(svgNameSpace, "svg", null);
        Element svgRoot = doc.getDocumentElement();
        svgRoot.setAttributeNS(null, "width", "1000");
        svgRoot.setAttributeNS(null, "height", "1000");

        return doc;
    }

    private SVGDocument fillSvgDocument(SVGDocument doc, OctiStringAlignment alignment){
        for(OctiSegmentAlignment sa : alignment){
            logger.trace(sa.getOrientation() +", " + sa.getOperation());
        }
        Pair<String, String> svgAlignmentStrings = parse(alignment);

        Element animation = createAnimationElement(doc, svgAlignmentStrings.first(), svgAlignmentStrings.second(), "red");
        doc.getDocumentElement().appendChild(animation);

        Element polylineSrc = createPolyLineElement(doc, alignment.getSourceString(), "blue");
        Element polylineTar = createPolyLineElement(doc, alignment.getTargetString(), "green");

        doc.getDocumentElement().appendChild(polylineSrc);
        doc.getDocumentElement().appendChild(polylineTar);

        return doc;
    }

    /**
     * Creates an svg polyline-Element
     * @param creationDoc the document used to create elements
     * @param from the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param to the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param color      the color in which the linestring is to be painted
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
        Element markerElement = createMarkers(creationDoc,markerId, color);
        Element startMarkerElement = createMarkers(creationDoc, startMarkerId, "green");
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
     * @param lineString the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param color      the color in which the linestring is to be painted
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
     * @param id    the id with which the marker element can be referenced
     * @param color the color in which the markers are to be painted
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
