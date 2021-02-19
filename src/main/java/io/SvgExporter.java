package io;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import org.twak.utils.Pair;

import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineString;
import morph.NoMinimumOperationException;
import morph.OctiLineMatcher;
import morph.OctiStringAlignment;

import javax.swing.*;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.svg.*;
import org.w3c.dom.svg.SVGDocument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;





/**
 * todo make factory and have config object
 * todo seperate frame owner and doc creator
 */
public class SvgExporter {

    private static Logger logger = LogManager.getLogger();
    // The frame.
    protected JFrame frame = new JFrame("SVG Morph");

    // The status label.
    protected JLabel statusLabel = new JLabel();

    protected JSVGCanvas svgCanvas = new JSVGCanvas();

    private int preferredHeight = 562;
    private int preferredWidth = 1000;

    //the svg
    private SVGDocument doc;
    private SvgGenerator generator = new SvgGenerator(new SvgGenerator.Config(false, false, true, "green", "blue", "orange", "orange"));
    private String svgDirectory = "./src/main/resources/";
    private String defaultSvg = "squareRightBoomerang.svg";

    public void show() {
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        refreshCanvas(doc);
        frame.setContentPane(this.createPanel());

        frame.pack();
        frame.setVisible(true);
    }

    /**
     * temporary helper method to extract, sets src and tar fields
     *
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

        try {
            return matcher.getAlignment();
        } catch (NoMinimumOperationException e) {
            e.printStackTrace();
            logger.warn("no valid match");
            return null;
        }
    }

    public SvgExporter() {
        OctiStringAlignment alignments = getMatchPath(svgDirectory + defaultSvg);
        this.doc = generator.generateSVG(alignments);
    }

    public static void main(String[] args) {
        SvgExporter exporter = new SvgExporter();
        exporter.show();
    }

    /**
     * Deprecated
     * Index based parsed
     *
     * @param match
     * @return
     */
    private Pair<String, String> parse(OctiLineString src, OctiLineString tar, List<Pair<Pair<Integer, Integer>, Integer>> match) {
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
        logger.trace(sourceString);
        logger.trace(targetString);
        Pair<String, String> svgStrings = new Pair<>(sourceString, targetString);


        return svgStrings;
    }

    private void refreshCanvas(SVGDocument d) { //todo clean up
        svgCanvas.setDocument(d);
        svgCanvas.setSVGDocument(d);

    }


    public JComponent createPanel() {
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        final JPanel panel = new JPanel(gbl);

        final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton button = addSVGLoadFunctionality(panel, "Load svg");

        actionsPanel.add(button);
        actionsPanel.add(statusLabel);

        statusLabel.setText("status text here");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(actionsPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        //debug colors
        svgCanvas.setBackground(Color.black);
        panel.setBackground(Color.MAGENTA);
        actionsPanel.setBackground(Color.BLUE);

        panel.add(svgCanvas, gbc);
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
                JFileChooser fc = new JFileChooser(svgDirectory);
                int choice = fc.showOpenDialog(parent);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    logger.trace(f.toURI().toString());
                    String ext = getFileExtension(f.toURI().toString());
                    if (ext == null || !ext.equals("svg")) {
                        logger.warn("didn't select a svg file");
                        statusLabel.setText("didn't select a svg file");
                    }
                    SvgPolygonExtractor x = new SvgPolygonExtractor();
                    try {
                        x.parseSvg(f.toURI().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    svgCanvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
                    OctiStringAlignment newAlignment = getMatchPath(f.toURI().toString());
                    doc = generator.generateSVG(newAlignment);

                    refreshCanvas(doc);

                }
            }
        });

        svgCanvas.addGVTTreeRendererListener(new GVTTreeRendererListener() {
            @Override
            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {
                //nothing
            }

            @Override
            public void gvtRenderingStarted(GVTTreeRendererEvent e) {
                //nothing
            }

            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                logger.trace("rendering and loading complete");

                svgCanvas.setPreferredSize(SvgGenerator.retrieveDimension(doc));
                frame.pack();
            }

            @Override
            public void gvtRenderingCancelled(GVTTreeRendererEvent e) {
                //nothing
            }

            @Override
            public void gvtRenderingFailed(GVTTreeRendererEvent e) {
                //nothing
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
