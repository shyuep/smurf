package net.shyue.smurf.App;

import net.shyue.smurf.Structure.AtomCluster;
import net.shyue.smurf.Analyzers.ClusterAnalyzer;
import net.shyue.smurf.Analyzers.MoleculeSmartNamer;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import net.shyue.smurf.Exporter.MolFileExporter;
import net.shyue.smurf.Exporter.MolFileExporterFactory;
import net.shyue.smurf.HighThroughput.HTProcessor;
import net.shyue.smurf.Exporter.GaussianInputFileExporter;
import net.shyue.smurf.Exporter.MolFileExporterFactory.FILE_FORMATS;
import net.shyue.smurf.Parser.GaussianOutputFileParser;
import net.shyue.smurf.Parser.MolFileParser;
import net.shyue.smurf.Parser.MolParser.MolParserException;
import net.shyue.smurf.Parser.MolFileParserFactory;
import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Analyzers.ConformerSimilarityComparator;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;
import net.shyue.smurf.Structure.MolEditor;
import net.shyue.smurf.Analyzers.SymmetryAnalyzer;
import net.shyue.smurf.Analyzers.VolumeCalculator;
import net.shyue.smurf.Analyzers.BondLengthsAnalyzer;
import net.shyue.smurf.Exporter.POSCARExporter;
import net.shyue.smurf.Structure.DefaultMolBuilder;
import net.shyue.smurf.Structure.Element;
import net.shyue.smurf.Structure.Templates.SUBSTITUENT;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;

/**
 * SMURF Interface for SMURF program, based heavily on Jmol.  
 * @author shyue
 */
public class SMURF extends JFrame {

    //private JFrame frame;
    private JMenuBar menuBar;
    private JToolBar toolbar;
    private JMenu fileMenu, viewMenu, editMenu, processMenu;
    private MolFileParser fparse;
    // Common File and Directory choosers.
    private JFileChooser fileChooser;
    private JComboBox molList;
    private JmolPanel molDisplay;

    /** Creates new form SMURF */
    public SMURF() {
        this.setTitle("SMURF Molecule Universal Reader and Fiddler");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.width < 640) {
            this.setSize(screenSize.width - 10, screenSize.width - 10);
        } else {
            this.setSize(500, 600);
        }
        this.validate();                // Make sure layout is ok

        this.setLocationRelativeTo(null);
        this.setVisible(true);

    }

    private void initComponents() {

        initToolBar();
        initMenus();

        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(editMenu);
        menuBar.add(processMenu);
        setMenusEnabled(false);

        this.setJMenuBar(menuBar);

        molList = new JComboBox();
        molList.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent evt) {
                if (evt.getStateChange() == ItemEvent.SELECTED) {

                    if (molList.getItemCount() > 0) {
                        Molecule mol = (Molecule) evt.getItem();
                        molDisplay.setMolecule(mol);
                        setMenusEnabled(true);
                    } else {
                        setMenusEnabled(false);
                    }
                }
            }
        });

        molDisplay = new JmolPanel();

        Container pane = this.getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        pane.add(toolbar, c);
        c.weighty = 0;
        c.gridy = 1;
        pane.add(molList, c);
        c.weighty = 1;
        c.gridy = 2;
        pane.add(molDisplay, c);
        //molDisplayMappings = new HashMap<Component, JmolPanel>();
        fileChooser = new JFileChooser("Choose File");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
    }

    private void initToolBar() {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JButton openFileButton = new JButton(new OpenFileAction());
        openFileButton.setText("");
        openFileButton.setToolTipText("Open File (Alt + O)");
        openFileButton.setMnemonic(KeyEvent.VK_O);
        toolbar.add(openFileButton);

        JButton deleteButton = new JButton(createImageIcon("images/Delete24.gif", "Delete"));
        deleteButton.setMnemonic(KeyEvent.VK_D);

        deleteButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                molList.removeItemAt(molList.getSelectedIndex());
            }
        });
        toolbar.add(deleteButton);

        JButton deleteAllButton = new JButton(createImageIcon("images/Remove24.gif", "Delete All"));
        deleteAllButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                molList.removeAllItems();
            }
        });
        toolbar.add(deleteAllButton);
    }

    private void setMenusEnabled(boolean isEnabled) {
        viewMenu.setEnabled(isEnabled);
        editMenu.setEnabled(isEnabled);
        processMenu.setEnabled(isEnabled);
    }

    private void initMenus() {
        fileMenu = new JMenu("File");

        fileMenu.add(new OpenFileAction());
        fileMenu.add(new OpenAllSupportedFilesAction());

        fileMenu.add(new ExportAllToFileAction(FILE_FORMATS.ZMAT));
        for (FILE_FORMATS format : FILE_FORMATS.values()) {
            fileMenu.add(new ExportAction(format));
        }

        editMenu = new JMenu("Edit");
        editMenu.add(new RotateMoleculeAction());
        editMenu.add(new SubstituteAction());
        editMenu.add(new ExtractClustersAction());

        processMenu = new JMenu("Process");
        processMenu.add(new SubstituteAllAction());
        processMenu.add(new ExtendSiteAction());
        processMenu.add(new StandardizeBondLengthsAction());

        viewMenu = new JMenu("View");

        viewMenu.add(new ShowRepresentationAction(FILE_FORMATS.XYZ));
        viewMenu.add(new ShowRepresentationAction(FILE_FORMATS.ZMAT));
        viewMenu.add(new ShowRepresentationAction(FILE_FORMATS.POSCAR));
        viewMenu.add(new ShowMoleculePropAction(PROPERTY.CHEM_FORM));
        viewMenu.add(new ShowMoleculePropAction(PROPERTY.MOL_WT));
        viewMenu.add(new ShowMoleculePropAction(PROPERTY.SYMMETRY));
        viewMenu.add(new ShowMoleculePropAction(PROPERTY.SMART_NAME));
        viewMenu.add(new ShowMoleculePropAction(PROPERTY.VOLUME));
        viewMenu.add(new ShowRunInfoAction());
        viewMenu.add(new ConformationAnalysisAction());
        viewMenu.add(new BondLengthsAnalysisAction());



    }

    public List<Molecule> getAllMolecules() {
        List<Molecule> allMol = new ArrayList<Molecule>(molList.getItemCount() * 2);
        for (int i = 0; i < molList.getItemCount(); i++) {
            allMol.add((Molecule) molList.getItemAt(i));
        }
        return allMol;
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public enum PROPERTY {

        CHEM_FORM, MOL_WT, SYMMETRY, SMART_NAME, VOLUME
    };

    private class ShowMoleculePropAction extends AbstractAction {

        private final PROPERTY prop;

        public ShowMoleculePropAction(PROPERTY _prop) {
            super(_prop.toString());
            prop = _prop;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Molecule mol = (Molecule) molList.getSelectedItem();
            String output = "";
            switch (prop) {
                case CHEM_FORM:
                    output = mol.getChemFormula();
                    break;
                case MOL_WT:
                    output = String.format("%s %.4f\n", "Total Molecular Weight", mol.getMolWt());
                    break;
                case SYMMETRY:
                    SymmetryAnalyzer symfind = new SymmetryAnalyzer(mol);
                    output = "The point group of this molecule is " + symfind.getSymmetryInfo();
                    break;
                case SMART_NAME:
                    MoleculeSmartNamer namer = new MoleculeSmartNamer(mol);
                    output = namer.getName();
                    break;
                case VOLUME:
                    VolumeCalculator vCalc = new VolumeCalculator(mol);
                    output = String.format("Volume=%.4fA3\n", vCalc.getVolume());
            }
            CustomDialogs.infoTextAreaDialog(prop.toString(), output);
        }
    }

    private class ExportAllToFileAction extends AbstractAction {

        private final FILE_FORMATS format;

        public ExportAllToFileAction(FILE_FORMATS _format) {
            super("Export All to " + _format.toString(), createImageIcon("images/SaveAll24.gif", "Export"));
            format = _format;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String dirname = selectDirectoryDialog();
            if (!dirname.matches("")) {
                for (int i = 0; i < molList.getItemCount(); i++) {
                    Molecule currentMol = (Molecule) molList.getItemAt(i);
                    MolFileExporter mExporter = MolFileExporterFactory.getExporter(currentMol, format);
                    mExporter.generate();
                    try {
                        String outfilename = currentMol.getName().replace(" ", "_") + ".geom";
                        mExporter.write(dirname + "/" + outfilename);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Error!",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private class ExtendSiteAction extends AbstractAction {

        public ExtendSiteAction() {
            super("Extend Site");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int currentSelectedAtom = molDisplay.getLastSelectedAtom();
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            if ((currentSelectedAtom != -1) && (!(currentMol.getAtomSpecies(currentSelectedAtom) == Element.H))) {

                SUBSTITUENT substituent = CustomDialogs.selectSubstituentDialog();
                if (substituent != null) {
                    HTProcessor HTP = new HTProcessor(currentMol);
                    HTP.extendBranch(currentSelectedAtom, substituent);
                    for (Molecule mol : HTP.getNewStructures()) {
                        setMolecule(mol);
                    }

                    CustomDialogs.infoTextAreaDialog("Processing Complete!", HTP.getLog());
                }


            } else {
                JOptionPane.showMessageDialog(null, "Please select an atom first.", "No atom selected!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class ConformationAnalysisAction extends AbstractAction {

        public ConformationAnalysisAction() {
            super("Conf. Analysis");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (molList.getItemCount() > 1) {
                int numItems = molList.getItemCount();
                JFrame frame = new JFrame();
                String[] molListArr = new String[numItems];
                for (int i = 0; i < numItems; i++) {
                    molListArr[i] = ((Molecule) molList.getItemAt(i)).getName();
                }

                JComboBox mol1List = new JComboBox(molListArr);
                JComboBox mol2List = new JComboBox(molListArr);
                Object[] components = {mol1List, mol2List};
                int selectedOption = JOptionPane.showConfirmDialog(frame, components,
                        "Select Molecules to Compare", JOptionPane.OK_CANCEL_OPTION);
                if (selectedOption == 0) {
                    Molecule currentMol = (Molecule) molList.getItemAt(mol1List.getSelectedIndex());
                    Molecule comparisonMolecule = (Molecule) molList.getItemAt(mol2List.getSelectedIndex());
                    ConformerSimilarityComparator netAnalyzer = new ConformerSimilarityComparator();
                    boolean isConformer = netAnalyzer.areSimilar(currentMol, comparisonMolecule);
                    String message = String.format("Are %s and %s conformers? %s",
                            currentMol.getName(), comparisonMolecule.getName(), isConformer);
                    CustomDialogs.infoTextAreaDialog("Conformation Analysis", message);

                }



            } else {
                JOptionPane.showMessageDialog(null,
                        "At least 2 molecules must be open to do conformation analysis.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class BondLengthsAnalysisAction extends AbstractAction {

        public BondLengthsAnalysisAction() {
            super("Bond Analysis");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            BondLengthsAnalyzer analyzer = new BondLengthsAnalyzer(currentMol,1.3);
            CustomDialogs.infoTextAreaDialog("Summary of Bond lengths",
                    analyzer.getBondLengthsSummary());



        }
    }

    private class ShowRunInfoAction extends AbstractAction {

        public ShowRunInfoAction() {
            super("Run Info");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (fparse instanceof GaussianOutputFileParser) {
                GaussianOutputFileParser gparse = (GaussianOutputFileParser) fparse;
                CustomDialogs.infoTextAreaDialog("Run Info", gparse.getRunSummaryXML());
            } else {
                JOptionPane.showMessageDialog(null,
                        "Parsed file is not a Gaussian Output file",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    public void setMolecule(Molecule mol) {
        molList.addItem(mol);
        molList.setSelectedItem(mol);
    }

    private String selectDirectoryDialog() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retval = fileChooser.showOpenDialog(null);
        if (retval == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        } else {
            return "";
        }
    }

    private String selectFileDialog(boolean save) {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int retval = -1;
        if (save)
            retval = fileChooser.showSaveDialog(null);
        else
            retval = fileChooser.showOpenDialog(null);
        if (retval == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        } else {
            return "";
        }
    }

    private class ShowRepresentationAction extends AbstractAction {

        private final FILE_FORMATS repFormat;

        public ShowRepresentationAction(FILE_FORMATS format) {
            super(format.toString());
            repFormat = format;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            MolFileExporter mExport = MolFileExporterFactory.getExporter(currentMol, repFormat);
            mExport.generate();
            CustomDialogs.infoTextAreaDialog("Molecule representation",
                    mExport.getStringRepresentation());

        }
    }

    private class StandardizeBondLengthsAction extends AbstractAction {

        public StandardizeBondLengthsAction() {
            super("Standardize Bond Lengths");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            BondLengthsAnalyzer corrector = new BondLengthsAnalyzer(currentMol,1.3);
            String output = "Bond lengths before correction\n" + corrector.getBondLengthsSummary();
            try {
                setMolecule(corrector.correctBondLengths(0.01));
                output += "\n\nBond lengths after correction\n" + corrector.getBondLengthsSummary();
                CustomDialogs.infoTextAreaDialog("Bond lengths", output);
            } catch (BuilderException ex) {
                JOptionPane.showConfirmDialog(rootPane, "Builder error!");
            }

        }
    }


    private class ExtractClustersAction extends AbstractAction {

        public ExtractClustersAction() {
            super("Extract Clusters");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {

            JFrame frame = new JFrame();
            double min = 1.0;
            double value = 10.0;
            double max = 128;
            double stepSize = 0.1;
            JLabel distLabel = new JLabel("Select cutoff");
            SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, stepSize);
            JSpinner distSpinner = new JSpinner(model);
            min = 1.0;
            value = 1.5;
            max = 2;
            stepSize = 0.1;
            JLabel perLabel = new JLabel("Select % threshold for network analysis");
            SpinnerNumberModel permodel = new SpinnerNumberModel(value, min, max, stepSize);
            JSpinner perSpinner = new JSpinner(permodel);
            JSpinner.NumberEditor editor = (JSpinner.NumberEditor) distSpinner.getEditor();
            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(1);
            JCheckBox counterIonOnlyCheck = new JCheckBox("Counter Ion Only?");
            JCheckBox selectByNumberClustersCheck = new JCheckBox("Select by Number of Clusters?");
            Object[] components = {distLabel, distSpinner, perLabel, perSpinner, counterIonOnlyCheck, selectByNumberClustersCheck};
            int selectedOption = JOptionPane.showConfirmDialog(frame, components,
                    "Cutoff distance for cluster in A", JOptionPane.OK_CANCEL_OPTION);
            if (selectedOption == 0) {
                Molecule currentMol = (Molecule) molList.getSelectedItem();
                int currentSelectedAtom = molDisplay.getLastSelectedAtom();
                double cutoff = Double.parseDouble(distSpinner.getValue().toString());
                double perThreshold = Double.parseDouble(perSpinner.getValue().toString());
                ClusterAnalyzer analyzer = new ClusterAnalyzer(currentMol, perThreshold);
                AtomCluster clusterOfInterest = null;
                if (currentSelectedAtom != -1) {
                    clusterOfInterest = analyzer.getCluster(currentMol.get(currentSelectedAtom));
                } else {
                    AtomCluster[] possibilities = analyzer.getCenterClusters().toArray(new AtomCluster[2]);
                    clusterOfInterest = (AtomCluster) JOptionPane.showInputDialog(null, "Choose substituent:",
                            "Substituent selection",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            possibilities,
                            possibilities[0]);

                }

                try {

                    Set<AtomCluster> nearestClusters = null;
                    if (selectByNumberClustersCheck.isSelected()) {
                        nearestClusters = analyzer.getNearestXClusters(clusterOfInterest, (int) Math.round(cutoff), counterIonOnlyCheck.isSelected());
                    } else {
                        nearestClusters = analyzer.getClustersWithinXDist(clusterOfInterest, cutoff, counterIonOnlyCheck.isSelected());
                    }
                    DefaultMolBuilder mBuild = new DefaultMolBuilder();
                    for (AtomCluster cluster : nearestClusters) {
                        for (Atom at : cluster) {
                            mBuild.addAtom(at.getSpecies(), at.getCoord());
                        }
                    }

                    setMolecule(mBuild.build());
                } catch (BuilderException ex) {
                    JOptionPane.showMessageDialog(frame, "Builder error " + ex.getMessage(), "Error!", JOptionPane.OK_OPTION);
                }

            }



//            CustomDialogs.infoTextAreaDialog("Clusters",
//                    analyzer.toString());

        }
    }

    private class SubstituteAction extends AbstractAction {

        public SubstituteAction() {
            super("Substitute");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int currentSelectedAtom = molDisplay.getLastSelectedAtom();
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            if (currentSelectedAtom != -1) {
                SUBSTITUENT substituent = CustomDialogs.selectSubstituentDialog();
                if (substituent != null) {
                    MolEditor mEdit = new MolEditor(currentMol);
                    mEdit.substitute(currentSelectedAtom, substituent);
                    try {
                        setMolecule(mEdit.build());
                    } catch (BuilderException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Please select an atom first.", "No atom selected!", JOptionPane.OK_OPTION);
            }
        }
    }

    private class SubstituteAllAction extends AbstractAction {

        public SubstituteAllAction() {
            super("Substitute All");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            String substitutee = CustomDialogs.selectSubstituteeDialog(currentMol);
            SUBSTITUENT substituent = CustomDialogs.selectSubstituentDialog();
            if (substituent != null) {
                HTProcessor HTP = new HTProcessor(currentMol);
                HTP.substituteAllDistinct(Element.valueOf(substitutee), substituent);
                for (Molecule mol : HTP.getNewStructures()) {
                    molList.addItem(mol);
                }
                molList.setSelectedIndex(molList.getItemCount() - 1);
                CustomDialogs.infoTextAreaDialog("HT Processor Log", HTP.getLog());

            }

        }
    }

    private class OpenFileAction extends AbstractAction {

        public OpenFileAction() {
            super("Open File", createImageIcon("images/Open24.gif",
                    "Open File"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String filename = selectFileDialog(false);
            if (!filename.matches("")) {
                try {
                    openFile(filename);
                } catch (MolParserException pe) {
                    JOptionPane.showMessageDialog(null, pe.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
                } catch (BuilderException be) {
                    JOptionPane.showMessageDialog(null, be.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
                }

            }

        }
    }

    private void openFile(String filename) throws MolParserException, BuilderException {
        fparse = MolFileParserFactory.getParser(filename);
        fparse.parse();
        setMolecule(fparse.getMolecule());
    }

    private class OpenAllSupportedFilesAction extends AbstractAction {

        public OpenAllSupportedFilesAction() {
            super("Open All Supported Files", createImageIcon("images/Open24.gif",
                    "Open All Supported Files"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String dirname = selectDirectoryDialog();
            if (!dirname.matches("")) {
                File dir = new File(dirname);
                File[] filelist = dir.listFiles();
                for (File file : filelist) {
                    String filePath = file.getAbsolutePath();
                    if (filePath.endsWith("log") || filePath.endsWith("out") || filePath.endsWith("gjf") || filePath.endsWith("com") ||
                            filePath.endsWith("geom")) {

                        try {
                            openFile(filePath);
                        } catch (MolParserException pe) {
                            JOptionPane.showMessageDialog(null, pe.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
                        } catch (BuilderException be) {
                            JOptionPane.showMessageDialog(null, be.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
                        }
                    }

                }

            }
        }
    }

    private class RotateMoleculeAction extends AbstractAction {

        public RotateMoleculeAction() {
            super("Rotate Molecule");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Molecule currentMol = (Molecule) molList.getSelectedItem();
            if ((molDisplay.getNumberOfSelectedAtoms() >= 2) &&
                    (molDisplay.getLastSelectedAtom() != molDisplay.getSecondLastSelectedAtom())) {
                JFrame frame = new JFrame();
                JSpinner angleSpinner = new JSpinner();
                Object[] components = {angleSpinner};
                int selectedOption = JOptionPane.showConfirmDialog(frame, components,
                        "Rotate Molecule", JOptionPane.OK_CANCEL_OPTION);
                if (selectedOption == 0) {
                    int angle = Integer.parseInt(angleSpinner.getValue().toString());
                    MolEditor mGen = new MolEditor(currentMol);
                    Atom at1 = currentMol.get(molDisplay.getLastSelectedAtom());
                    Atom at2 = currentMol.get(molDisplay.getSecondLastSelectedAtom());
                    mGen.rotate(at2, at1, angle);
                    try {
                        setMolecule(mGen.build());
                    } catch (BuilderException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        "Please select at least 2 distinct atoms by clicking on them first.",
                        "Not enough atoms selected!", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private class ExportAction extends AbstractAction {

        private final FILE_FORMATS exportFormat;

        public ExportAction(FILE_FORMATS format) {
            super("Export to " + format.toString(), createImageIcon("images/Save24.gif", "Export"));
            exportFormat = format;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {

            Molecule currentMol = (Molecule) molList.getSelectedItem();
            String outfilename = selectFileDialog(true);
            if (!outfilename.matches("")) {
                MolFileExporter mExporter = MolFileExporterFactory.getExporter(currentMol, exportFormat);
                if (mExporter instanceof GaussianInputFileExporter) {
                    GaussianInputDialog gauDialog = new GaussianInputDialog(null);
                    if (gauDialog.getReturnStatus() == 1) {
                        GaussianInputFileExporter gauExporter = (GaussianInputFileExporter) mExporter;
                        gauExporter.setFunctional(gauDialog.getFunctional());
                        gauExporter.setBasisSet(gauDialog.getBasisSet());
                        gauExporter.setLink0(gauDialog.getLink0());
                        gauExporter.setRouteParameters(gauDialog.getRoute());
                        gauExporter.setInputParameters(gauDialog.getAdditionalInput());
                        mExporter = gauExporter;
                    }
                } else if (mExporter instanceof POSCARExporter) {
                    String s = JOptionPane.showInputDialog(null, "Dimensions of cubic cell", "10");
                    double cellDim = Double.parseDouble(s);
                    POSCARExporter poscarExporter = (POSCARExporter) mExporter;
                    poscarExporter.setCellDimensions(cellDim);
                    mExporter = poscarExporter;
                }
                mExporter.generate();
                try {
                    mExporter.write(outfilename);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Error!",
                            JOptionPane.ERROR_MESSAGE);
                }

            }
        }
    }
}
