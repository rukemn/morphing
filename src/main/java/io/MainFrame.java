package io;

import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineString;
import morph.NoMinimumOperationException;
import morph.OctiLineMatcher;
import morph.OctiStringAlignment;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.svg.JSVGComponent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.svg.SVGDocument;
import scoringStrategies.OctiMatchStrategy;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class MainFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger();
    private JPanel mainPanel;
    private JSplitPane split;

    private JPanel commandPanel; // left panel
    private JSVGCanvas canvas; // to the right
    private SVGDocument doc;

    private JCheckBox twoInputFilesCheckbox;
    private JButton loadSourceFileButton;
    private JButton loadTargetFileButton;
    private JCheckBox singleInputFileCheckbox;
    private JButton loadSourceAndTargetFileButton;
    private String defaltFilePath = "./src/main/resources/";

    private JComboBox scoringStrategy;

    private JCheckBox showAnimationCheckBox;
    private JCheckBox showSourceCheckBox;
    private JCheckBox showTargetCheckBox;

    private JLabel statusLabel;
    private JButton saveButton;
    private JButton runButton;

    private class Conig{
        private URI sourceUri, targetUri, singleUri;
        private boolean singleFileInput; //either use bothUri or (sourceUri and targetUri)
        private OctiMatchStrategy segmentStrategy;
        private OctiMatchStrategy visibilityConstraints;
        private String polyDistanceStrategy;

        public URI getSourceUri() {
            return sourceUri;
        }

        public void setSourceUri(URI sourceUri) {
            this.sourceUri = sourceUri;
        }

        public URI getTargetUri() {
            return targetUri;
        }

        public void setTargetUri(URI targetUri) {
            this.targetUri = targetUri;
        }

        public URI getBothUri() {
            return singleUri;
        }

        public void setBothUri(URI bothUri) {
            this.singleUri = bothUri;
        }

        public boolean isSingleFileInput() {
            return singleFileInput;
        }

        public void setSingleFileInput(boolean singleFileInput) {
            this.singleFileInput = singleFileInput;
        }

        public OctiMatchStrategy getSegmentStrategy() {
            return segmentStrategy;
        }

        public void setSegmentStrategy(OctiMatchStrategy segmentStrategy) {
            this.segmentStrategy = segmentStrategy;
        }

        public OctiMatchStrategy getVisibilityConstraints() {
            return visibilityConstraints;
        }

        public void setVisibilityConstraints(OctiMatchStrategy visibilityConstraints) {
            this.visibilityConstraints = visibilityConstraints;
        }

        public String getPolyDistanceStrategy() {
            return polyDistanceStrategy;
        }

        public void setPolyDistanceStrategy(String polyDistanceStrategy) {
            this.polyDistanceStrategy = polyDistanceStrategy;
        }
    };
    private MainFrame.Conig conig = new Conig();


    public MainFrame(){
        super("Polygonmorphing");
        setUp();

        this.setContentPane(split);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void main(String[] args){
        MainFrame mf = new MainFrame();
        mf.setVisible(true);
        mf.pack();
    }
    private void setUp(){
        mainPanel = new JPanel();
        canvas = new JSVGCanvas();
        commandPanel = setUpCommandPanel();
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commandPanel, canvas);
        mainPanel.add(split);

    }

    private JPanel setUpCommandPanel(){
        commandPanel = new JPanel(new GridBagLayout());
        BoxLayout lm = new BoxLayout(commandPanel, BoxLayout.Y_AXIS);
        commandPanel.setLayout(lm);
        JPanel filechooser = setUpFileChooserPanel();
        filechooser.setMaximumSize(new Dimension(filechooser.getMaximumSize().width, filechooser.getMinimumSize().height)); // dont let it grow vertically
        commandPanel.add(filechooser);
        JPanel stratChooser = setUpStrategyChooserPanel();
        stratChooser.setMaximumSize(new Dimension(stratChooser.getMaximumSize().width, stratChooser.getMinimumSize().height)); // dont let it grow vertically
        commandPanel.add(stratChooser);
        statusLabel = new JLabel("status here");
        commandPanel.add(statusLabel);
        commandPanel.add(Box.createVerticalGlue());
        commandPanel.add(setUpRunPanel());

        return commandPanel;
    }

    private String shortenedFileButtonText(String text){
        if(text.length() <= 28) {
             return text;
        }else{
            return text.substring(text.length()-25);
        }
    }



    // no comps have weighty > 0 , so all vertically centered if more space than needed
    private JPanel setUpFileChooserPanel(){
        GridBagLayout gbl = new GridBagLayout();
        JPanel filePanel = new JPanel(gbl);
        TitledBorder tb = new TitledBorder("Input files");
        filePanel.setBorder(tb);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        twoInputFilesCheckbox = new JCheckBox("check");
        //only manipulator of the config object, if the other checkbox is check this one will
        // be unchecked by Buttongroup and fire ItemEvent
        twoInputFilesCheckbox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED){
                logger.debug("selected dual mode");
                this.conig.setSingleFileInput(false);
            }else if(e.getStateChange() == ItemEvent.DESELECTED){
                logger.debug("selected mono mode");
                this.conig.setSingleFileInput(true);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 0;
        filePanel.add(twoInputFilesCheckbox,gbc);

        JPanel fill1 = new JPanel(); //filler to move Label and Button to the right
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filePanel.add(fill1, gbc);

        JLabel sourceFileLabel = new JLabel("Source");
        //have a margin of 10 for the button labels
        Border labelBorder = new EmptyBorder(0,0,0,10);
        sourceFileLabel.setBorder(labelBorder);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST; // align with its file chooser
        gbc.fill = GridBagConstraints.NONE;
        filePanel.add(sourceFileLabel,gbc);

        loadSourceFileButton = new JButton("test/dir/tohave/somelength.txt");
        gbc.insets = new Insets(0,0,5,0);
        loadSourceFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaltFilePath);
            int choice = fc.showOpenDialog(filePanel);
            if (choice == JFileChooser.APPROVE_OPTION){
                File file = fc.getSelectedFile();
                this.conig.setSourceUri(file.toURI());
                loadSourceFileButton.setText(file.toURI().toString());
                logger.trace("source set to " + file.toURI().toString());
            }
        });
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL; // take up the max width of a button in this column, no weight though
        filePanel.add(loadSourceFileButton, gbc);
        gbc.anchor = GridBagConstraints.CENTER;


        JLabel tagetFileLabel = new JLabel("Target");
        tagetFileLabel.setBorder(labelBorder);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        filePanel.add(tagetFileLabel,gbc);

        // no spacer needed this row
        loadTargetFileButton = new JButton("load...");
        loadTargetFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaltFilePath);
            int choice = fc.showOpenDialog(filePanel);
            if (choice == JFileChooser.APPROVE_OPTION){
                File file = fc.getSelectedFile();
                this.conig.setTargetUri(file.toURI());
                loadTargetFileButton.setText(file.toURI().toString());
                logger.trace("target set to " + file.toURI().toString());
            }
        });
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filePanel.add(loadTargetFileButton, gbc);


        //or seperator
        GridBagConstraints seperatorConstraints = new GridBagConstraints();
        seperatorConstraints.gridy = 3; // between the two cases
        seperatorConstraints.gridx = 0;
        seperatorConstraints.weightx = 1.0; // grow the whole width
        seperatorConstraints.fill = GridBagConstraints.HORIZONTAL;
        seperatorConstraints.gridwidth = GridBagConstraints.REMAINDER;
        filePanel.add(setUpHorizonatlSeperator("OR"), seperatorConstraints);


        singleInputFileCheckbox = new JCheckBox("check");
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        filePanel.add(singleInputFileCheckbox, gbc);

        JLabel loadBothLabel = new JLabel("Both");
        loadBothLabel.setBorder(labelBorder);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        filePanel.add(loadBothLabel,gbc);

        loadSourceAndTargetFileButton = new JButton("load...");
        loadSourceAndTargetFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaltFilePath);
            int choice = fc.showOpenDialog(loadSourceAndTargetFileButton);
            if (choice == JFileChooser.APPROVE_OPTION){
                File file = fc.getSelectedFile();
                this.conig.setBothUri(file.toURI());
                String uriString = file.toURI().toString();
                if(uriString.length() <= 28) {
                    loadSourceAndTargetFileButton.setText(uriString);
                }else{
                    loadSourceAndTargetFileButton.setText("..." + uriString.substring(uriString.length()-25));
                }
                logger.trace("mono set to " + file.toURI().toString());
            }
        });
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        filePanel.add(loadSourceAndTargetFileButton, gbc);


        ButtonGroup selectionModes = new ButtonGroup();
        selectionModes.add(twoInputFilesCheckbox);
        selectionModes.add(singleInputFileCheckbox);
        singleInputFileCheckbox.setSelected(true);
        this.conig.setSingleFileInput(true);
        return filePanel;
    }

    private JPanel setUpHorizonatlSeperator(String text){
        JSeparator lsep = new JSeparator(SwingConstants.HORIZONTAL);

        lsep.setPreferredSize(new Dimension(100, 5));
        lsep.setMinimumSize((new Dimension(5,5)));
        lsep.setMaximumSize((new Dimension(100000,5)));
        lsep.setBackground(new Color(68, 66, 66));

        JSeparator rsep = new JSeparator(SwingConstants.HORIZONTAL);
        rsep.setPreferredSize(new Dimension(100, 5));
        rsep.setMinimumSize((new Dimension(5,5)));
        rsep.setMaximumSize((new Dimension(100000,5)));
        rsep.setBackground(new Color(68, 66, 66));

        JLabel seperatorLabel = new JLabel(text);
        JPanel seperatorPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        seperatorPanel.add(lsep, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.weightx = 0;
        seperatorPanel.add(seperatorLabel,gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        seperatorPanel.add(rsep,gbc);
        return seperatorPanel;
    }

    private JPanel setUpStrategyChooserPanel(){
        JPanel strategyPanel = new JPanel(new GridBagLayout());

        Border border = new TitledBorder("Strategy");
        strategyPanel.setBorder(border);
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        JLabel operationCostLabel = new JLabel("Operation Stategy");
        strategyPanel.add(operationCostLabel,gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,5,0);
        // placeholder , should probably be gathered from factory dynamically
        String[] strategyStrings = new String[]{"BaseMatch", "FlatScore", "AffineGap(todo)" };
        JComboBox<String> strategyPicker = new JComboBox<>(strategyStrings); //listmmdel , comboboxmodel
        strategyPanel.add(strategyPicker, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0,0,0,0);
        JLabel visibilityLabel = new JLabel("Visibility");
        strategyPanel.add(visibilityLabel,gbc);

        gbc.gridx = 1;
        gbc.weightx = 0; //dynamically also here
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,5,0);
        String[] visibilityDecoratorStrings = new String[]{"No constraints", "src doesn't obstruct", "Total"};
        JComboBox<String> visibilityPicker = new JComboBox<>(visibilityDecoratorStrings);
        strategyPanel.add(visibilityPicker,gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0,0,0,0);
        JLabel polyDistanceLabel = new JLabel("PolygonMatch Distance");
        strategyPanel.add(polyDistanceLabel,gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;//dynamically also here
        gbc.fill = GridBagConstraints.HORIZONTAL;
        String[] polyDistanceStrings = new String[]{"Intersetcion over Union", "centroid distance"};
        JComboBox<String> polyDistancePicker = new JComboBox<>(polyDistanceStrings);
        strategyPanel.add(polyDistancePicker,gbc);

        return strategyPanel;
    }

    private void setUpCanvas(){

        this.canvas.addGVTTreeRendererListener(new GVTTreeRendererListener() {
            @Override
            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingStarted(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                /*ogger.trace("rendering and loading complete");
                Dimension svgDim = SvgGenerator.retrieveDimension(doc);
                if(svgDim.height < minimumCanvasDimension.height || svgDim.width < minimumCanvasDimension.width) svgDim = minimumCanvasDimension;
                svgCanvas.setPreferredSize(svgDim);
                frame.pack();*/
            }

            @Override
            public void gvtRenderingCancelled(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingFailed(GVTTreeRendererEvent e) {

            }
        });

        this.canvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {
            @Override
            public void documentLoadingStarted(SVGDocumentLoaderEvent e) {
                super.documentLoadingStarted(e);
                logger.trace("document loading started...");
            }

            @Override
            public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
                super.documentLoadingCompleted(e);
                logger.trace("document loaded");
                statusLabel.setText("Document Loaded.");
            }

            @Override
            public void documentLoadingFailed(SVGDocumentLoaderEvent e) {
                super.documentLoadingFailed(e);
                logger.trace("document loading failed");
            }
        });

    }

    private JPanel setUpRunPanel(){
        JPanel actionsPanel = new JPanel(new FlowLayout());
        Border border = new TitledBorder("Run");
        actionsPanel.setBorder(border);
        JButton saveButton = new JButton("S");
        actionsPanel.add(saveButton);
        JButton runButton = new JButton("Run");

        runButton.addActionListener(actionEvent -> {
            Geometry sourceGeometry,targetGeometry;

            if(! conig.isSingleFileInput()){
                PolygonExtractorInterface sourceExtractor = new PolygonExtractor();
                PolygonExtractorInterface targetExtractor = new PolygonExtractor();
                try {
                    sourceExtractor.parseFile(this.conig.getSourceUri());
                    targetExtractor.parseFile(this.conig.getTargetUri());
                } catch (FileParseException e) {
                    statusLabel.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    statusLabel.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                }
                if(sourceExtractor.numberOfParsedGeometries() != 1){
                    statusLabel.setText("Source File must contain exactly 1 parsable Geometry");
                    return;
                }
                if(targetExtractor.numberOfParsedGeometries() != 1){
                    statusLabel.setText("Target File must contain exactly 1 parsable Geometry");
                    return;
                }
                sourceGeometry = sourceExtractor.getNthGeometry(0);
                targetGeometry = targetExtractor.getNthGeometry(0);

            }else {
                PolygonExtractorInterface dualExtractor = new PolygonExtractor();
                try {
                    dualExtractor.parseFile(this.conig.getBothUri());
                } catch (FileParseException e) {
                    statusLabel.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    statusLabel.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                }
                if(dualExtractor.numberOfParsedGeometries() != 2){
                    statusLabel.setText("Dual File must contain exactly 1 parsable Geometry");
                    return;
                }
                sourceGeometry = dualExtractor.getNthGeometry(0);
                targetGeometry = dualExtractor.getNthGeometry(1);
            }

            //normally match the polygons first, for now just take one outer ring
            if(     (! (sourceGeometry instanceof org.locationtech.jts.geom.Polygon || sourceGeometry instanceof OctiLineString))
                    &&
                    (! (targetGeometry instanceof org.locationtech.jts.geom.Polygon || targetGeometry instanceof OctiLineString)))
            {
                statusLabel.setText("Geometries not supported yet");
                return;
            }

            OctiLineString srcString,tarString;
            try {
                srcString = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(sourceGeometry);
                tarString = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(targetGeometry);
            } catch (Exception e) {
                statusLabel.setText("couldn't extract the OctilineString from provided Geometry");
                e.printStackTrace();
                return;
            }

            OctiLineMatcher olm = new OctiLineMatcher(srcString,tarString);
            OctiStringAlignment stringAlignment;
            try {
                stringAlignment = olm.getAlignment();
            } catch (NoMinimumOperationException e) {
                statusLabel.setText("couldn't calculate alignment");
                e.printStackTrace();
                return;
            }
            SvgGenerator svgGenerator = new SvgGenerator();
            this.doc = svgGenerator.generateSVG(stringAlignment);
            this.canvas.setSVGDocument(doc);
            this.canvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);

        });

        actionsPanel.add(runButton);
        actionsPanel.setMaximumSize(new Dimension(actionsPanel.getMaximumSize().width,actionsPanel.getMinimumSize().height));

        return actionsPanel;
    }


}
