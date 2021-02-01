package io;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;

import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.NoMinimumOperationException;
import morph.OctiLineMatcher;
import morph.OctiSegmentAlignment;
import morph.OctiStringAlignment;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.*;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.twak.utils.Pair;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import static morph.OctiLineMatcher.*;

/**
 * todo make factory and have config object
 * todo factor out the MatchSquence object
 */
public class SvgExporter {

    private static Logger logger = LogManager.getLogger();
    // The frame.
    protected JFrame frame = new JFrame("SVG Morph");

    // The status label.
    protected JLabel statusLabel = new JLabel();

    // The SVG canvas.
    protected JSVGCanvas svgCanvas = new JSVGCanvas();

    private int preferredHeight = 1000;
    private int preferredWidth = 1000;

    //the svg
    private SVGDocument doc;

    //factor out into dedicated object
    private OctiLineString src;
    private OctiLineString tar;

    public void show() {
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        frame.getContentPane().add(this.createPanel(doc));
        frame.setSize(preferredWidth, preferredHeight); //frame.pack();
        frame.setVisible(true);

        //frame.revalidate();
        //frame.doLayout();
        //frame.repaint(0,0,1000,1000);
    }

    /**
     * temporary helper method to extract
     * @return
     */
    private OctiStringAlignment getMatchPath(String svgPath) {

        SvgPolygonExtractor importer = new SvgPolygonExtractor();
        try {
            importer.parseSvg(svgPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        OctiGeometryFactory ogf = new OctiGeometryFactory();

        OctiLineString olsSource = ogf.createOctiLineString(importer.getNthPolygon(0).getExteriorRing().getCoordinateSequence());
        OctiLineString olsTarget = ogf.createOctiLineString(importer.getNthPolygon(1).getExteriorRing().getCoordinateSequence());


        OctiLineMatcher matcher = new OctiLineMatcher(olsSource, olsTarget);
        this.src = matcher.getSource();
        this.tar = matcher.getTarget();

        try {
            return matcher.getAlignment();
        } catch (NoMinimumOperationException e) {
            e.printStackTrace();
            logger.error("no valid match");
            return null;
        }
    }

    public SvgExporter() {
        this.doc = this.createSVGDocument();
    }

    public static void main(String[] args) {
        String svgPath;
        if(args.length == 1){ //interpret arg as path
            svgPath = args[0];
        }else{
            svgPath = "src/main/resources/squareRightBoomerang.svg";
        }
        logger.warn("test");
        SvgExporter exporter = new SvgExporter();

        OctiStringAlignment alignment = exporter.getMatchPath(svgPath);
        exporter.show();
        exporter.fillSvgDocument(alignment);

    }

    private void fillSvgDocument(OctiStringAlignment alignment){
        for(OctiSegmentAlignment sa : alignment){
            logger.trace(sa.getOrientation() +", " + sa.getOperation());
        }
        Pair<String, String> svgAlignmentStrings = parse(alignment);

        Element animation = createAnimationElement(svgAlignmentStrings.first(), svgAlignmentStrings.second(), "red");
        doc.getDocumentElement().appendChild(animation);

        Element polylineSrc = createPolyLineElement(src, "blue");
        Element polylineTar = createPolyLineElement(tar, "green");

        doc.getDocumentElement().appendChild(polylineSrc);
        doc.getDocumentElement().appendChild(polylineTar);

        //set the doc again, otherwise unable to display animation
        svgCanvas.setDocument(doc);
    }

    /** Deprecated
     * Index based parsed
     * @param match
     * @return
     */
    private Pair<String, String> parse(List<Pair<Pair<Integer, Integer>, Integer>> match) {
        StringBuilder srcBuilder = new StringBuilder();
        StringBuilder tarBuilder = new StringBuilder();

        for (int idx = 0; idx < match.size(); idx++) {
            Pair<Pair<Integer, Integer>, Integer> m = match.get(idx);
            srcBuilder.append(src.getCoordinateN(m.first().first()).x).append(",").append(src.getCoordinateN(m.first().first()).y).append(" ");
            tarBuilder.append(tar.getCoordinateN(m.first().second()).x).append(",").append(tar.getCoordinateN(m.first().second()).y).append(" ");
        }
        //from and to attributes
        String sourceString = srcBuilder.substring(0, srcBuilder.length() - 1);
        String targetString = tarBuilder.substring(0, tarBuilder.length() - 1);
        logger.trace(sourceString.toString());
        logger.trace(tarBuilder.toString());
        Pair<String, String> svgStrings = new Pair<>(sourceString, targetString);


        return svgStrings;
    }

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

    /**
     * Creates an svg marker-Element
     *
     * @param id    the id with which the marker element can be referenced
     * @param color the color in which the markers are to be painted
     * @return the created marker-Element
     */
    public Element createMarkers(String id, String color) {
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element marker = doc.createElementNS(nameSpace, "marker");

        marker.setAttributeNS(null, "id", id);
        marker.setAttributeNS(null, "markerWidth", "4");
        marker.setAttributeNS(null, "markerHeight", "4");
        marker.setAttributeNS(null, "refX", "2");
        marker.setAttributeNS(null, "refY", "2");

        //now the shape
        Element circle = doc.createElementNS(nameSpace, "circle");
        circle.setAttributeNS(null, "cx", "2");
        circle.setAttributeNS(null, "cy", "2");
        circle.setAttributeNS(null, "r", "2");
        circle.setAttributeNS(null, "stroke", "none");
        circle.setAttributeNS(null, "fill", color);

        marker.appendChild(circle);
        return marker;
    }

    /**
     * Creates an svg Polyline-Element
     *
     * @param lineString the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param color      the color in which the linestring is to be painted
     * @return the newly created polyline-Element
     */
    public Element createPolyLineElement(OctiLineString lineString, String color) {
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;

        Element polyGroup = doc.createElementNS(nameSpace, "g");
        Element polyLineElement = doc.createElementNS(nameSpace, "polyline");

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
        Element markerElement = createMarkers(markerId, color);
        polyGroup.appendChild(markerElement);
        polyLineElement.setAttributeNS(null, "marker-mid", "url(#" + markerId + ")");
        polyGroup.appendChild(polyLineElement);

        return polyGroup;
    }
    /**
     * Creates an svg polyline-Element
     *
     * @param from the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param to the linestring from which the "points"-attribute of the Polyline-Element is derived
     * @param color      the color in which the linestring is to be painted
     * @return the newly created polyline-Element
     */
    public Element createAnimationElement(String from, String to, String color) {
        String svgNameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element polyGroup = doc.createElementNS(svgNameSpace, "g");
        Element polyLineElement = doc.createElementNS(svgNameSpace, "polyline");

        polyLineElement.setAttributeNS(null, "stroke", color);
        polyLineElement.setAttributeNS(null, "stroke-width", "3");
        polyLineElement.setAttributeNS(null, "fill", "none");

        Element animateElement = doc.createElementNS(svgNameSpace, "animate");
        animateElement.setAttributeNS(null, "attributeName", "points");
        animateElement.setAttributeNS(null, "dur", "5s");
        animateElement.setAttributeNS(null, "repeatCount", "indefinite");
        animateElement.setAttributeNS(null, "from", from);
        animateElement.setAttributeNS(null, "to", to);
        polyLineElement.appendChild(animateElement);

        String markerId = color + "Circle";
        String startMarkerId = color + "StartCircle";
        Element markerElement = createMarkers(markerId, color);
        Element startMarkerElement = createMarkers(startMarkerId, "green");
        polyGroup.appendChild(markerElement);
        polyGroup.appendChild(startMarkerElement);
        polyLineElement.setAttributeNS(null, "marker-mid", "url(#" + markerId + ")");
        polyLineElement.setAttributeNS(null, "marker-start", "url(#" + startMarkerId + ")");
        polyGroup.appendChild(polyLineElement);

        return polyGroup;
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

    public JComponent createPanel(SVGDocument d) {
        svgCanvas.setDocument(d);
        final JPanel panel = new JPanel(new BorderLayout());
        final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton button = addSVGLoadFunctionality(panel, "Load svg");

        actionsPanel.add(button);
        actionsPanel.add(statusLabel);
        statusLabel.setText("status text here");
        panel.add(actionsPanel, BorderLayout.NORTH);
        panel.add(svgCanvas, BorderLayout.CENTER);

        return panel;
    }

    private String getFileExtension(String fullFileName) {
        int lastDotIndex = fullFileName.lastIndexOf('.');
        int fileNameStartWithoutDir = Math.max(fullFileName.lastIndexOf('/'), fullFileName.lastIndexOf('\\'));
        return lastDotIndex > fileNameStartWithoutDir ? fullFileName.substring(lastDotIndex + 1) : null;
    }

    private JButton addSVGLoadFunctionality(Component parent, String buttonText) {
        JButton button = new JButton(buttonText);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser("./src/main/resources");
                int choice = fc.showOpenDialog(parent);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    /*try {
                        svgCanvas.setURI(f.toURI().toString());
                        svgCanvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }*/
                    logger.trace(f.toURI().toString());
                    String ext = getFileExtension(f.toURI().toString());
                    if (ext == null || !ext.equals("svg")) {
                        logger.warn("didn't select a svg file");
                        statusLabel.setText("didn't select a svg file");
                    }
                    svgCanvas.setURI(f.toURI().toString());
                    svgCanvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
                }
            }
        });

        // Set the JSVGCanvas listeners.
        svgCanvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {
            public void documentLoadingStarted(SVGDocumentLoaderEvent e) {
                logger.trace("document loading");
                statusLabel.setText("Document Loading...");
            }

            public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {

                logger.trace("document loaded");
                statusLabel.setText("Document Loaded.");
            }

            public void documentLoadingFailed(SVGDocumentLoaderEvent e) {

                logger.trace("document loading failed");
                statusLabel.setText("Document loading failed.");
            }
        });
        return button;
    }

}
