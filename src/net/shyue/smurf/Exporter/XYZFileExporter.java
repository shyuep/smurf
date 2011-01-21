package net.shyue.smurf.Exporter;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Molecule;

/**
 *
 * @author shyue
 */
public class XYZFileExporter extends MolFileExporter {

    /**
     * 
     * @param mol
     */
    public XYZFileExporter(Molecule mol) {
        super(mol);
    }

    @Override
    public void generate() {
        output = Integer.toString(mol.size())+ "\n\n";
        for (Atom at : mol) {
            output += at.toString() + "\n";
        }
    }
}
