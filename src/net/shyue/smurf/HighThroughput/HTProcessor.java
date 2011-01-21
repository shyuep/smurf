package net.shyue.smurf.HighThroughput;

import net.shyue.smurf.Analyzers.NetworkRepresentation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.shyue.smurf.Exporter.MolFileExporter;
import net.shyue.smurf.Exporter.ZMATFileExporter;
import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;
import net.shyue.smurf.Analyzers.SymmetryAnalyzer;
import net.shyue.smurf.Structure.Element;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Structure.MolEditor;
import net.shyue.smurf.Structure.Templates.SUBSTITUENT;

/**
 * High throughput processor.  Generates test molecules from a starting structure.
 * @author shyue
 */
public final class HTProcessor {

    private List<Molecule> newStructures;
    private StringBuilder log;
    private Molecule centeredMolecule;
    private SymmetryAnalyzer msf;
    private MolEditor mEdit;
    private NetworkRepresentation netRep;

    /**
     * Creates a new instance of the HTProcessor and initializes the variables.
     * @param startingStructure Starting base structure.ls
     * 
     */
    public HTProcessor(Molecule startingStructure) {
        msf = new SymmetryAnalyzer(startingStructure);
        centeredMolecule = msf.getCenteredMolecule();
        mEdit = new MolEditor(centeredMolecule);
        netRep = new NetworkRepresentation(centeredMolecule);
        log = new StringBuilder();
    }

    /**
     * Generate new structures up to depth using substituent.
     * @param speciesToSub 
     * @param substituent
     */
    public void substituteAllDistinct(Element speciesToSub, SUBSTITUENT substituent) {
        newStructures = new ArrayList<Molecule>();
        List<Atom> distinctH = msf.getAllDistinct(speciesToSub);
        log.append("\tPoint group : " + msf.getSymmetryInfo() + "\n" +
                "\t\t" + Integer.toString(distinctH.size()) + " distinct hydrogens\n");
        int counter = 1;
        for (Atom at : distinctH) {
            mEdit.substitute(centeredMolecule.indexOf(at), substituent);
            String newName = mEdit.getName();
            if ((centeredMolecule.getCharge()==1 && substituent == SUBSTITUENT.METHYL)||
                    (centeredMolecule.getCharge()==-1 && substituent == SUBSTITUENT.TRIFLUOROMETHANE))
            {
                newName = newName.substring(0,newName.length()-4);
            }
            mEdit.setName(newName+ "_" + String.valueOf(counter));
            try {
                newStructures.add(mEdit.build());
            } catch (BuilderException ex) {
                System.out.println("Error in building molecule : " + ex.getMessage());
                System.exit(0);
            }
            counter++;
            mEdit.undoAllChanges();
        }
        log.append(String.format("\t%d new structures generated.\n", newStructures.size()));

    }

    /**
     * Extend Molecule by substituting hydrogens attached to a particular heavy atom
     * @param heavyAtomIndex Index of heavy atom to be extended.
     * @param substituent substituent
     */
    public void extendBranch(int heavyAtomIndex, SUBSTITUENT substituent) {
        newStructures = new ArrayList<Molecule>();
        Atom heavyAtom = centeredMolecule.get(heavyAtomIndex);
        List<Atom> connectedAtoms = netRep.getAdjacentAtoms(heavyAtom);
        List<Atom> distinctH = msf.getDistinctSet(Element.H, connectedAtoms);
        log.append("Point group : " + msf.getSymmetryInfo() + "\n\t");
        log.append(distinctH.size());
        log.append(" distinct hydrogens connected to atom ");
        log.append(heavyAtomIndex);
        int counter = 1;
        for (Atom at : distinctH) {
            mEdit.substitute(centeredMolecule.indexOf(at), substituent);
            mEdit.setName(mEdit.getName() + "_" + String.valueOf(counter));
            try {
                newStructures.add(mEdit.build());
            } catch (BuilderException ex) {
                System.out.println("Error in building molecule : " + ex.getMessage());
                System.exit(0);
            }
            counter++;
            mEdit.undoAllChanges();
        }

        log.append("\n%d new structures generated.\n");
        log.append(newStructures.size());
    }

    /**
     * Write input files.
     * @throws java.io.IOException
     */
    public void writeInputFiles() throws IOException {
        String outfilename;
        int i = 1;
        for (Molecule m : newStructures) {
            outfilename = m.getName() + ".geom";
            MolFileExporter mExporter = new ZMATFileExporter(m);
            mExporter.generate();
            mExporter.write(outfilename);
            i++;
        }
    }

    /**
     * Write input files.
     * @param directory 
     * @throws java.io.IOException
     */
    public void writeInputFiles(String directory) throws IOException {
        String outfilename;
        for (Molecule m : newStructures) {
            outfilename = m.getName().replace(" ", "_") + ".geom";
            MolFileExporter mExporter = new ZMATFileExporter(m);
            mExporter.generate();
            mExporter.write(directory + "/" + outfilename);
        }
    }

    /**
     * Get log of analysis and structure generation.
     * @return Log
     */
    public List<Molecule> getNewStructures() {
        return newStructures;
    }

    public String getLog() {
        return log.toString();
    }

}
