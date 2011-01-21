package net.shyue.smurf.App;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.shyue.smurf.Exporter.GaussianInputFileExporter;
import net.shyue.smurf.Exporter.MolFileExporter;
import net.shyue.smurf.Exporter.MolFileExporterFactory;
import net.shyue.smurf.Exporter.XYZFileExporter;
import net.shyue.smurf.Exporter.ZMATFileExporter;
import net.shyue.smurf.HighThroughput.HTProcessor;
import net.shyue.smurf.Parser.MolFileParser;
import net.shyue.smurf.Parser.MolFileParserFactory;
import net.shyue.smurf.Parser.MolParser.MolParserException;
import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Analyzers.ConformerSimilarityComparator;
import net.shyue.smurf.Analyzers.NetworkRepresentation;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;
import net.shyue.smurf.Structure.MolEditor;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Analyzers.SymmetryAnalyzer;
import net.shyue.smurf.Analyzers.VolumeCalculator;
import net.shyue.smurf.Parser.GaussianOutputFileParser;
import net.shyue.smurf.Parser.GaussianOutputFileParser.CORRECTION;
import net.shyue.smurf.Structure.Element;
import net.shyue.smurf.Structure.Templates.SUBSTITUENT;

/**
 * Command line interface class for SMURF program.
 * @author shyue
 */
public final class CommandLineInterface {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        /* Default is to call SMURF if no arguments are passed to cmdline*/
        if (args.length == 0) {
            try {
                new SMURF();
            } catch (Exception ex) {
                ex.printStackTrace();
                displayHelp();
            }
        } else {
            //System.out.println("CLI");
            new CommandLineInterface(args);
        }

    }
    private Map<String, String> arguments;
    private List<String> filenames;

    private CommandLineInterface(String[] args) {
        processArguments(args);
        if (arguments.containsKey("help")) {
            displayHelp();
        } else if (arguments.containsKey("convert")) {
            convertFile();
        } else if (arguments.containsKey("symmetry")) {
            displaySymmetryInformation();
        } else if (arguments.containsKey("ht")) {
            highThroughputSub();
        } else if (arguments.containsKey("rotate")) {
            rotateMolecule();
        } else if (arguments.containsKey("coord")) {
            showMoleculeRepresentation();
        } else if (arguments.containsKey("chemform")) {
            showMolInfo("chemform");
        } else if (arguments.containsKey("ext")) {
            extendStructure();
        } else if (arguments.containsKey("pointgroup")) {
            showMolInfo("pointgroup");
        } else if (arguments.containsKey("network")) {
            showMolInfo("network");
        } else if (arguments.containsKey("conformer")) {
            analyzeConformer();
        } else if (arguments.containsKey("volume")) {
            showMolInfo("volume");
        } else if (arguments.containsKey("print"))
        {
            printData();
        } 
        else {
            displayErrorMessage();
        }
    }

    private void printData(){
        if (filenames.size() == 0) {
            displayErrorMessage();
        } else {
            for (String filename : filenames) {
                try {
                    GaussianOutputFileParser fparse = new GaussianOutputFileParser(filename);
                    fparse.parse();
                    String parameter = arguments.get("print");
                    if (parameter.equals("SCFENERGY"))
                    {
                        System.out.println(fparse.getSCFEnergy());
                    }else
                    {
                        System.out.println(fparse.getCorrection(CORRECTION.valueOf(parameter)));
                    }

                } catch (MolParserException ex) {
                    System.out.println(ex.getMessage());
                    System.exit(-1);
                }
            }
        }
    }



    private void showMolInfo(String infoType) {

        if (filenames.size() == 0) {
            displayErrorMessage();
        } else {
            for (String filename : filenames) {
                try {
                    Molecule mol = parseMoleculeFile(filename);

                    if (infoType.matches("chemform")) {
                        System.out.println(mol.getChemFormula());
                    } else if (infoType.matches("volume")) {
                        VolumeCalculator cal = new VolumeCalculator(mol);
                        System.out.println(cal.getVolume());
                    } else if (infoType.matches("pointgroup")) {
                        SymmetryAnalyzer symfind = new SymmetryAnalyzer(mol);
                        System.out.println(symfind.getPointGroupSymbol());
                    } else if (infoType.equals("network")) {
                        NetworkRepresentation netRep = new NetworkRepresentation(mol);
                        System.out.println("Index\tSpecies\tNearest Neighbours");
                        System.out.println("-----\t-------\t------------------");
                        for (Atom at1 : mol) {
                            System.out.println(at1);
                            for (Atom at2 : netRep.getAdjacentAtoms(at1)) {
                                System.out.format("\t\t%s\n", at2);
                            }
                            System.out.println("-----\t-------\t------------------");
                        }
                    }

                } catch (BuilderException ex) {
                    System.out.println(ex.getMessage());
                    System.exit(-1);
                }
            }
        }
    }

    private void displayErrorMessage() {
        System.out.println("Incorrect number of arguments.");
        displayHelp();
        System.exit(0);
    }

    private void showMoleculeRepresentation() {
        if (filenames.size() == 0) {
            displayErrorMessage();
        } else {
            for (String filename : filenames) {
                Molecule mol;
                try {
                    mol = parseMoleculeFile(filename);

                    if (arguments.containsKey("format")) {
                        displayMolecule(mol, arguments.get("format"));
                    } else {
                        displayMolecule(mol, "xyz");
                    }
                } catch (BuilderException ex) {
                    System.out.println(ex.getMessage());
                    System.exit(-1);
                }

            }
        }
    }

    private void displayMolecule(Molecule mol, String format) {
        MolFileExporter fout = new XYZFileExporter(mol);
        if (format.equalsIgnoreCase("Zmat")) {
            fout = new ZMATFileExporter(mol);
        } else if (format.equalsIgnoreCase("Gaussian")) {
            fout = new GaussianInputFileExporter(mol);
        }

        fout.generate();
        System.out.println(fout.getStringRepresentation());

    }

    private void processArguments(String[] args) {
        arguments = new HashMap<String, String>(args.length * 2);
        filenames = new ArrayList<String>();
        Pattern runParameters = Pattern.compile("^-(\\w+)=*([\\w-,\\.]*)");
        Matcher m;

        for (int i = 0; i <
                args.length; i++) {
            m = runParameters.matcher(args[i]);
            if (m.find()) {
                arguments.put(m.group(1), m.group(2));
            } else {
                filenames.add(args[i]);
            }

        }
    }

    private static void displayHelp() {
        try {

            InputStream is = CommandLineInterface.class.getResourceAsStream("HELP");
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            System.out.println("Error opening file!  Check permissions");
        }

    }

    private static Molecule parseMoleculeFile(String file_in) throws BuilderException {

        MolFileParser fparse = null;
        try {
            fparse = MolFileParserFactory.getParser(file_in);
            fparse.parse();
        } catch (MolParserException pe) {
            System.out.println(pe.getMessage());
        }

        return fparse.getMolecule();
    }

    private void convertFile() {
        if (filenames.size() != 2) {
            displayErrorMessage();
        } else {

            Molecule mol;
            try {
                mol = parseMoleculeFile(filenames.get(0));
                MolFileExporter fout = MolFileExporterFactory.getExporterFromFileExt(mol, filenames.get(1));
                if (arguments.containsKey("format")) {
                    if (arguments.get("format").equalsIgnoreCase("Zmat")) {
                        fout = new ZMATFileExporter(mol);
                    } else if (arguments.get("format").equalsIgnoreCase("Gaussian")) {
                        GaussianInputFileExporter gauout = new GaussianInputFileExporter(mol);
                        if (arguments.containsKey("nprocs")) {
                            gauout.setNumProcs(Integer.parseInt(arguments.get("nprocs")));
                        }
                        if (arguments.containsKey("link0")) {
                            gauout.setLink0(arguments.get("link0"));
                        }
                        if (arguments.containsKey("route")) {
                            gauout.setRouteParameters(arguments.get("route"));
                        }
                        if (arguments.containsKey("functional")) {
                            gauout.setFunctional(arguments.get("functional"));
                        }
                        if (arguments.containsKey("bset")) {
                            gauout.setBasisSet(arguments.get("bset"));
                        }
                        if (arguments.containsKey("charge")) {
                            gauout.setCharge(Integer.parseInt(arguments.get("charge")));
                        }
                        if (arguments.containsKey("spin")) {
                            gauout.setSpinMult(Integer.parseInt(arguments.get("spin")));
                        }
                        if (arguments.containsKey("inputPara")) {
                            gauout.setInputParameters(arguments.get("inputPara"));
                        }
                        fout = gauout;
                    }
                }
                fout.generate();
                fout.write(filenames.get(1));
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            } catch (BuilderException ex) {
                System.out.println(ex.getMessage());
                System.exit(-1);
            }
        }

    }

    private void analyzeConformer() {
        if (filenames.size() != 2) {
            displayErrorMessage();
        } else {

            try {
                Molecule mol1 = parseMoleculeFile(filenames.get(0));
                Molecule mol2 = parseMoleculeFile(filenames.get(1));
                ConformerSimilarityComparator netAnalyzer = new ConformerSimilarityComparator();
                boolean isConformer = netAnalyzer.areSimilar(mol1, mol2);
                String output = mol1.getName() + " & " + mol2.getName();
                if (isConformer) {
                    System.out.println(output + " are conformers.");
                } else {
                    System.out.println(output + " are not conformers.");
                }

            } catch (BuilderException ex) {
                System.out.println(ex.getMessage());
                System.exit(-1);
            }
        }

    }

    private void displaySymmetryInformation() {
        if (filenames.size() == 0) {
            displayErrorMessage();
        } else {
            for (String filename : filenames) {
                try {

                    Molecule mol = parseMoleculeFile(filename);

                    SymmetryAnalyzer symfind = new SymmetryAnalyzer(mol);

                    System.out.println("File : " + filename);
                    if (arguments.get("symmetry").matches("full")) {
                        System.out.println(symfind.getSymmetryInfo());
                    } else {
                        System.out.println("Point group : " + symfind.getPointGroupSymbol());
                    }

                } catch (Exception pe) {
                    System.out.println(pe.getMessage());
                }
            }
        }


    }

    private void highThroughputSub() {
        if (filenames.size() == 0) {
            displayErrorMessage();
        } else {
            String substituent = "METHYL";
            String substitutee = "H";
            if (arguments.containsKey("subwith")) {
                substituent = arguments.get("subwith");
            }
            if (arguments.containsKey("sub")) {
                substitutee = arguments.get("sub");
            }
            for (String filename : filenames) {
                try {
                    Molecule mol = parseMoleculeFile(filename);
                    HTProcessor HTP = new HTProcessor(mol);
                    HTP.substituteAllDistinct(Element.valueOf(substitutee), SUBSTITUENT.valueOf(substituent));
                    HTP.writeInputFiles();
                    System.out.println(HTP.getLog());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void extendStructure() {
        int index = 0;
        if (filenames.size() != 1) {
            displayErrorMessage();
        } else {
            if (!arguments.get("ext").matches("")) {

                try {
                    index = Integer.parseInt(arguments.get("ext"));
                } catch (Exception ex) {
                    System.out.println("Invalid argument passed for ext");
                    System.exit(0);
                }
            }
        }

        try {
            Molecule mol = parseMoleculeFile(filenames.get(0));
            if (mol.getAtomSpecies(index) == Element.H) {
                System.out.println("Invalid atom index choice for extension - H atom detected!");
                System.exit(0);
            }
            HTProcessor HTP = new HTProcessor(mol);
            HTP.extendBranch(index, SUBSTITUENT.METHYL);
            HTP.writeInputFiles();
            System.out.println(HTP.getLog());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void rotateMolecule() {
        if (filenames.size() != 1) {
            displayErrorMessage();
        } else {
            String tmpstr = arguments.get("rotate");
            String[] tmpstrarr = tmpstr.split(",");
            if (tmpstrarr.length == 3) {
                int atom1 = Integer.parseInt(tmpstrarr[0]);
                int atom2 = Integer.parseInt(tmpstrarr[1]);
                int angle = Integer.parseInt(tmpstrarr[2]);
                for (String filename : filenames) {
                    try {
                        Molecule mol = parseMoleculeFile(filename);
                        MolEditor mEditor = new MolEditor(mol);
                        mEditor.rotate(mol.get(atom1), mol.get(atom2), angle);
                        displayMolecule(mEditor.build(), "xyz");
                    } catch (Exception e) {
                        System.out.println("An error has occured");
                    }
                }
            } else {
                displayErrorMessage();
            }
        }


    }
}
