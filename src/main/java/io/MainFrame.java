package io;

import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineString;
import morph.NoMinimumOperationException;
import morph.OctiLineMatcher;
import morph.OctiStringAlignment;

import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.svg.JSVGComponent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

import org.w3c.dom.svg.SVGDocument;
import scoringStrategies.OctiMatchStrategy;
import scoringStrategies.ScoringStrategyFactory;
import scoringStrategies.StrategyInitializationException;


import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
//todo have Stringbuilder for messages for each "user story", after comletion display in statuslabel
public class MainFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger();
    private JPanel mainPanel;
    private JSplitPane split;

    private JPanel commandPanel; // left panel
    private JSVGCanvas canvas; // to the right
    private SVGDocument doc;

    public boolean isDocInUse() {
        return docInUse;
    }

    public void setDocInUse(boolean docInUse) {
        this.docInUse = docInUse;
    }

    private boolean docInUse = false;
    private boolean isPaused = false;
    private final Icon playIcon = new ImageIcon("./src/main/resources/icons/playIcon.png");
    private final Icon pauseIcon = new ImageIcon("./src/main/resources/icons/pauseIcon.png");

    private JCheckBox twoInputFilesCheckbox;
    private JButton loadSourceFileButton;
    private JButton loadTargetFileButton;
    private JCheckBox singleInputFileCheckbox;
    private JButton loadSourceAndTargetFileButton;
    private String defaultFilePath = "./src/main/resources/";
    private String defaultSavePath = "./src/main/resources/saves/";

    private JComboBox<String> strategyPicker;
    private JComboBox<String> visibilityPicker;
    private JComboBox<String> polyDistancePicker;

    private JCheckBox showAnimationCheckBox;
    private JCheckBox showSourceCheckBox;
    private JCheckBox showTargetCheckBox;

    private JLabel statusLabel;
    private JButton saveButton;
    private JButton runButton;

    private class Conig {
        private URI sourceUri, targetUri, singleUri;
        private boolean singleFileInput; //either use bothUri or (sourceUri and targetUri)
        private OctiMatchStrategy segmentStrategy;

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

        public String getPolyDistanceStrategy() {
            return polyDistanceStrategy;
        }

        public void setPolyDistanceStrategy(String polyDistanceStrategy) {
            this.polyDistanceStrategy = polyDistanceStrategy;
        }
    }

    ;
    private MainFrame.Conig conig = new Conig();
    private SvgGenerator.Config animtaionConfig = new SvgGenerator.Config(false, false, true, "green", "green", "red", "purple");

    public MainFrame() {
        super("Polygonmorphing");
        setUp();

        this.setContentPane(split);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void main(String[] args) {
        MainFrame mf = new MainFrame();
        mf.setVisible(true);
        mf.pack();
    }

    private void setUp() {
        setUpDefaultPathConfig();
        mainPanel = new JPanel();
        canvas = new JSVGCanvas();
        setUpCanvas();
        commandPanel = setUpCommandPanel();
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commandPanel, canvas);
        mainPanel.add(split);

    }

    private JPanel setUpAnimationOptionsPanel() {
        JPanel animnationOptionsPanel = new JPanel();
        BoxLayout lm = new BoxLayout(animnationOptionsPanel, BoxLayout.Y_AXIS);
        TitledBorder title = new TitledBorder("Animation Options");
        animnationOptionsPanel.setBorder(title);
        animnationOptionsPanel.setLayout(lm);

        JCheckBox showSource = new JCheckBox("show source");
        showSource.setSelected(this.animtaionConfig.showSource);
        showSource.setHorizontalTextPosition(JCheckBox.LEFT);
        showSource.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                this.animtaionConfig.showSource = true;
            } else if (event.getStateChange() == ItemEvent.DESELECTED) {
                this.animtaionConfig.showSource = false;
            }
        });
        animnationOptionsPanel.add(showSource);

        JCheckBox showTarget = new JCheckBox("show target");
        showTarget.setSelected(this.animtaionConfig.showTarget);
        showTarget.setHorizontalTextPosition(JCheckBox.LEFT);
        showTarget.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                this.animtaionConfig.showTarget = true;
            } else if (event.getStateChange() == ItemEvent.DESELECTED) {
                this.animtaionConfig.showTarget = false;
            }
        });
        animnationOptionsPanel.add(showTarget);

        JCheckBox showAnimation = new JCheckBox("show animation");
        showAnimation.setSelected(this.animtaionConfig.showAnimation);
        showAnimation.setHorizontalTextPosition(JCheckBox.LEFT);
        showAnimation.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                this.animtaionConfig.showAnimation = true;
            } else if (event.getStateChange() == ItemEvent.DESELECTED) {
                this.animtaionConfig.showAnimation = false;
            }
        });
        animnationOptionsPanel.add(showAnimation);
        return animnationOptionsPanel;
    }

    private void setUpDefaultPathConfig() {
        File sourceFile = new File("./src/main/resources/svg/bone.svg");
        this.conig.setSourceUri(sourceFile.toURI());

        File targetFile = new File("./src/main/resources/svg/octagonSquare.svg");
        this.conig.setTargetUri(targetFile.toURI());

        File bothInOneFile = new File("./src/main/resources/svg/octagonSquare.svg");
        this.conig.setBothUri(bothInOneFile.toURI());
    }

    private JPanel setUpCommandPanel() {
        commandPanel = new JPanel(new GridBagLayout());
        BoxLayout lm = new BoxLayout(commandPanel, BoxLayout.Y_AXIS);
        commandPanel.setLayout(lm);
        JPanel filechooser = setUpFileChooserPanel();
        filechooser.setMaximumSize(new Dimension(filechooser.getMaximumSize().width, filechooser.getMinimumSize().height)); // dont let it grow vertically
        commandPanel.add(filechooser);
        JPanel stratChooser = setUpStrategyChooserPanel();
        stratChooser.setMaximumSize(new Dimension(stratChooser.getMaximumSize().width, stratChooser.getMinimumSize().height)); // dont let it grow vertically
        commandPanel.add(stratChooser);
        commandPanel.add(Box.createVerticalStrut(5));
        commandPanel.add(setUpAnimationOptionsPanel());
        commandPanel.add(setUpStatusPanel());
        commandPanel.add(Box.createVerticalGlue());
        commandPanel.add(setUpAnimationControlPanel());
        commandPanel.add(setUpRunPanel());
        return commandPanel;
    }

    private String shortenedFileButtonText(String text) {
        if (text.length() <= 25) {
            return text;
        } else {
            return "..." + text.substring(text.length() - 22);
        }
    }

    private JPanel setUpStatusPanel() {
        JPanel statusPanel = new JPanel();
        TitledBorder title = new TitledBorder("Status");
        statusPanel.setBorder(title);
        statusLabel = new JLabel("status here");
        statusPanel.add(statusLabel);
        return statusPanel;
    }

    private JPanel setUpFileChooserPanel() {
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
            if (e.getStateChange() == ItemEvent.SELECTED) {
                logger.debug("selected dual mode");
                this.conig.setSingleFileInput(false);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                logger.debug("selected mono mode");
                this.conig.setSingleFileInput(true);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 0;
        filePanel.add(twoInputFilesCheckbox, gbc);

        JPanel fill1 = new JPanel(); //filler to move Label and Button to the right
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filePanel.add(fill1, gbc);

        JLabel sourceFileLabel = new JLabel("Source");
        //have a margin of 10 for the button labels
        Border labelBorder = new EmptyBorder(0, 0, 0, 10);
        sourceFileLabel.setBorder(labelBorder);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST; // align with its file chooser
        gbc.fill = GridBagConstraints.NONE;
        filePanel.add(sourceFileLabel, gbc);

        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL; // take up the max width of a button in this column, no weight though
        loadSourceFileButton = new JButton();
        loadSourceFileButton.setText(shortenedFileButtonText(this.conig.getSourceUri().toString())); //set to default
        loadSourceFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaultFilePath);
            int choice = fc.showOpenDialog(filePanel);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                this.conig.setSourceUri(file.toURI());
                loadSourceFileButton.setText(shortenedFileButtonText(file.toURI().toString()));
                logger.trace("source set to " + file.toURI().toString());
            }
        });

        filePanel.add(loadSourceFileButton, gbc);

        JLabel tagetFileLabel = new JLabel("Target");
        tagetFileLabel.setBorder(labelBorder);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        filePanel.add(tagetFileLabel, gbc);

        // no spacer needed this row
        loadTargetFileButton = new JButton();
        loadTargetFileButton.setText(shortenedFileButtonText(this.conig.getTargetUri().toString())); //set to default
        loadTargetFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaultFilePath);
            int choice = fc.showOpenDialog(filePanel);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                this.conig.setTargetUri(file.toURI());
                loadTargetFileButton.setText(shortenedFileButtonText(file.toURI().toString()));
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
        filePanel.add(loadBothLabel, gbc);

        loadSourceAndTargetFileButton = new JButton();
        loadSourceAndTargetFileButton.setText(shortenedFileButtonText(this.conig.getBothUri().toString())); //set to default
        loadSourceAndTargetFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaultFilePath);
            int choice = fc.showOpenDialog(loadSourceAndTargetFileButton);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                this.conig.setBothUri(file.toURI());
                String uriString = file.toURI().toString();
                loadSourceAndTargetFileButton.setText(shortenedFileButtonText(uriString));
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

    private JPanel setUpHorizonatlSeperator(String text) {
        JSeparator lsep = new JSeparator(SwingConstants.HORIZONTAL);

        lsep.setPreferredSize(new Dimension(100, 5));
        lsep.setMinimumSize((new Dimension(5, 5)));
        lsep.setMaximumSize((new Dimension(100000, 5)));
        lsep.setBackground(new Color(68, 66, 66));

        JSeparator rsep = new JSeparator(SwingConstants.HORIZONTAL);
        rsep.setPreferredSize(new Dimension(100, 5));
        rsep.setMinimumSize((new Dimension(5, 5)));
        rsep.setMaximumSize((new Dimension(100000, 5)));
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
        seperatorPanel.add(seperatorLabel, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        seperatorPanel.add(rsep, gbc);
        return seperatorPanel;
    }

    private JPanel setUpStrategyChooserPanel() {
        JPanel strategyPanel = new JPanel(new GridBagLayout());

        Border border = new TitledBorder("Strategy");
        strategyPanel.setBorder(border);
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        JLabel operationCostLabel = new JLabel("Operation Stategy");
        strategyPanel.add(operationCostLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        // placeholder , should probably be gathered from factory dynamically

        String[] strategyStrings = ScoringStrategyFactory.getStrategies().toArray(new String[0]);
        strategyPicker = new JComboBox<>(strategyStrings); //listmodel , comboboxmodel
        strategyPicker.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) setStrategy();
        });
        strategyPanel.add(strategyPicker, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel visibilityLabel = new JLabel("Decorator");
        strategyPanel.add(visibilityLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0; //dynamically also here
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        String[] visibilityDecoratorStrings = ScoringStrategyFactory.getDecorators().toArray(new String[0]);
        visibilityPicker = new JComboBox<>(visibilityDecoratorStrings);
        visibilityPicker.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) setStrategy();
        });

        strategyPanel.add(visibilityPicker, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel polyDistanceLabel = new JLabel("PolygonMatch Distance");
        strategyPanel.add(polyDistanceLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;//dynamically also here
        gbc.fill = GridBagConstraints.HORIZONTAL;
        String[] polyDistanceStrings = new String[]{"Intersection over Union", "centroid distance"};
        polyDistancePicker = new JComboBox<>(polyDistanceStrings);
        polyDistancePicker.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) setStrategy();
        });
        strategyPanel.add(polyDistancePicker, gbc);

        //now fill the config object with the first items of the comboBoxes
        this.setStrategy();

        return strategyPanel;
    }

    private void setUpCanvas() {

        this.canvas.addGVTTreeRendererListener(new GVTTreeRendererListener() {
            @Override
            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingStarted(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                setDocInUse(true);
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

    /**
     * Whenever a strategy-{@link JComboBox} changes, call this method to get the new total {@link OctiMatchStrategy}
     * from {@link ScoringStrategyFactory}.
     * This method sets the {@link MainFrame.Conig}
     */
    private void setStrategy() {
        String strategyName = (String) this.strategyPicker.getSelectedItem();
        String decorators = (String) this.visibilityPicker.getSelectedItem();
        logger.trace("setting base strat to " + strategyName);
        logger.trace("setting decorators to " + decorators);
        List<String> decoratorList = new ArrayList<>();
        decoratorList.add(decorators);
        try {
            this.conig.setSegmentStrategy(ScoringStrategyFactory.getStrategy(strategyName, decoratorList));
        } catch (StrategyInitializationException e) {
            logger.trace("Cant build a strategy from current selection");
            statusLabel.setText("Cant build a strategy from current selection");
            e.printStackTrace();
        }
    }

    private void calcSvg(MainFrame.Conig configuration) {
        Geometry sourceGeometry, targetGeometry;
        StringBuilder sb = new StringBuilder();
        if (configuration.isSingleFileInput()) {
            PolygonExtractorInterface dualExtractor = new PolygonExtractor();
            try {
                dualExtractor.parseFile(configuration.getBothUri());
            } catch (FileParseException e) {
                sb.append(e.getMessage());
                e.printStackTrace();
                return;
            } catch (IOException e) {
                sb.append(e.getMessage());
                e.printStackTrace();
                return;
            }
            if (dualExtractor.numberOfParsedGeometries() != 2) {
                logger.warn("Dual File must contain exactly 2 parsable Geometries");
                sb.append("Dual File must contain exactly 2 parsable Geometries");
                return;
            }
            sourceGeometry = dualExtractor.getNthGeometry(0);
            targetGeometry = dualExtractor.getNthGeometry(1);

        } else {
            PolygonExtractorInterface sourceExtractor = new PolygonExtractor();
            PolygonExtractorInterface targetExtractor = new PolygonExtractor();
            try {
                sourceExtractor.parseFile(configuration.getSourceUri());
                targetExtractor.parseFile(configuration.getTargetUri());
            } catch (FileParseException e) {
                //statusLabel.setText(e.getMessage());
                logger.warn(e.getMessage());
                sb.append(e.getMessage());
                e.printStackTrace();
                return;
            } catch (IOException e) {
                statusLabel.setText(e.getMessage());
                sb.append(e.getMessage());
                e.printStackTrace();
                return;
            }

            if (sourceExtractor.numberOfParsedGeometries() != 1)
                sb.append("Source File must contain exactly 1 parsable Geometry");
            if (targetExtractor.numberOfParsedGeometries() != 1)
                sb.append("Target File must contain exactly 1 parsable Geometry");

            if (sourceExtractor.numberOfParsedGeometries() > 1 || targetExtractor.numberOfParsedGeometries() > 1) {
                List<Geometry> sourceGeoms = sourceExtractor.getGeometryList();
                List<Geometry> targetGeoms = targetExtractor.getGeometryList();
                sourceGeoms.sort(Comparator.comparingDouble(Geometry::getArea));
                targetGeoms.sort(Comparator.comparingDouble(Geometry::getArea));
                sourceGeometry = sourceGeoms.get(0);
                targetGeometry = targetGeoms.get(0);
            } else {
                sourceGeometry = sourceExtractor.getNthGeometry(0);
                targetGeometry = targetExtractor.getNthGeometry(0);
            }
        }

        //normally match the polygons first, for now just take one outer ring
        if ((!(sourceGeometry instanceof org.locationtech.jts.geom.Polygon || sourceGeometry instanceof OctiLineString))
                &&
                (!(targetGeometry instanceof org.locationtech.jts.geom.Polygon || targetGeometry instanceof OctiLineString))) {

            if (sourceGeometry instanceof MultiPolygon && sourceGeometry.getNumGeometries() == 1) {
                sourceGeometry = sourceGeometry.getGeometryN(0);
            } else {
                statusLabel.setText("Geometries not supported yet");
                sb.append("Geometries not supported yet");
                return;
            }
            if (targetGeometry instanceof MultiPolygon && targetGeometry.getNumGeometries() == 1) {
                targetGeometry = targetGeometry.getGeometryN(0);
            } else {
                statusLabel.setText("Geometries not supported yet");
                sb.append("Geometries not supported yet");
                return;
            }
        }

        OctiLineString srcString, tarString;
        try {
            srcString = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(sourceGeometry);
            tarString = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(targetGeometry);
        } catch (Exception e) {
            statusLabel.setText("couldn't extract the OctilineString from provided Geometry");
            sb.append("couldn't extract the OctilineString from provided Geometry");
            e.printStackTrace();
            return;
        }

        OctiLineMatcher olm = new OctiLineMatcher(srcString, tarString, this.conig.getSegmentStrategy());
        OctiStringAlignment stringAlignment;
        try {
            stringAlignment = olm.getAlignment();
        } catch (NoMinimumOperationException e) {
            statusLabel.setText("couldn't calculate alignment");
            sb.append("couldn't calculate alignment");
            e.printStackTrace();
            return;
        }

        SvgGenerator svgGenerator = new SvgGenerator(animtaionConfig);
        this.doc = svgGenerator.generateSVG(stringAlignment);
        this.canvas.setSVGDocument(doc);
        /*this.canvas.addGVTTreeRendererListener(new GVTTreeRendererListener() {
            @Override
            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingStarted(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingCancelled(GVTTreeRendererEvent e) {

            }

            @Override
            public void gvtRenderingFailed(GVTTreeRendererEvent e) {

            }
        });
        */
        this.canvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
    }

    private JPanel setUpAnimationControlPanel() {

        JPanel actionsPanel = new JPanel(new FlowLayout());
        Border border = new TitledBorder("Animation Control");
        actionsPanel.setBorder(border);
        JSlider slider = createAnimationSlider();
        actionsPanel.add(createAnimationPauseButton(slider)); //button to the left
        actionsPanel.add(slider); //slider to the right
        return actionsPanel;
    }

    private JButton createAnimationPauseButton(JSlider animationSlider){
        //initial state is "running", so set it to show that user can pause
        JButton pauseButton = new JButton(pauseIcon);

        //Image img = ImageIO.read(getClass().getResource("./src/ressources/icons/playIcon.png"));
        //pauseButton.setIcon(img);

        pauseButton.addActionListener(actionEvent -> {
            if (isDocInUse()) {

                    canvas.getUpdateManager().getUpdateRunnableQueue().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            SvgGenerator.pause(doc);
                        }
                    });
                /*catch (InterruptedException e) { // run method is synchronized, so its either all or nothing
                    e.printStackTrace();
                }*/
                //flip and  reflect change
                isPaused = !isPaused;
                if (isPaused) { // now paused
                    statusLabel.setText("doc paused");
                    pauseButton.setIcon(playIcon);

                    //update slider to show current animation progress
                    float time = SvgGenerator.getAnimationTime(doc);

                    //inclusive on both end-> +1
                    int numberOfValues = (animationSlider.getMaximum() -animationSlider.getMinimum()) +1;
                    logger.warn("time:" + time + ", numberOfvalues" + numberOfValues);
                    animationSlider.setValue(Math.round(time * numberOfValues));
                }
                if (!isPaused) { //was just set if to paused
                    statusLabel.setText("doc resumed");
                    pauseButton.setIcon(pauseIcon);
                }

            } else {
                statusLabel.setText("no doc on canvas");
            }
        });
        return pauseButton;
    }

    private JSlider createAnimationSlider(){
        int precision = 1000; // states the slider has, mapped to [0,1) intervall later for animation state
        JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, precision, 0); //slider start at start of animation
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                logger.trace("changed time to" + slider.getValue());
                //canvas.getUpdateManager().getBridgeContext()
                if (isDocInUse()) {
                    float time = ((float) slider.getValue()) / precision;
                    canvas.getUpdateManager().getUpdateRunnableQueue().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            SvgGenerator.setAnimationTime(doc, time);
                        }
                    });
                } else {
                    statusLabel.setText("no doc on canvas");
                }

            }
        });
        return slider;
    }
    private JPanel setUpRunPanel() {
        JPanel actionsPanel = new JPanel(new FlowLayout());
        Border border = new TitledBorder("Run");
        actionsPanel.setBorder(border);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(actionEvent -> {
            if (docInUse) {
                logger.trace("saving file");
                saveDocument(this.doc);
            }
        });
        actionsPanel.add(saveButton);
        JButton runButton = new JButton("Run");

        runButton.addActionListener(actionEvent -> {
            statusLabel.setText("");
            try {
                calcSvg(this.conig);
            } catch (Exception e) {
                logger.warn("couldn't calc svg :" + e.getMessage());
                statusLabel.setText(e.getMessage());
            }
        });

        actionsPanel.add(runButton);
        actionsPanel.setMaximumSize(new Dimension(actionsPanel.getMaximumSize().width, actionsPanel.getMinimumSize().height));

        return actionsPanel;
    }

    private void saveDocument(SVGDocument doc) {
        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(doc);
        Instant currentTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        logger.trace("save as " + currentTime.toString());
        try {
            String filePath = this.defaultSavePath + currentTime.toString() + ".svg";
            logger.trace(filePath);
            FileOutputStream fos = new FileOutputStream(filePath);
            Writer out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            svgGenerator.stream(doc.getDocumentElement(), out, false, false);
            statusLabel.setText("saved as " + filePath);
        } catch (FileNotFoundException | SVGGraphics2DIOException e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
    }

}
