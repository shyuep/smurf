package net.shyue.smurf.Exporter;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Molecule;

/**
 *
 * @author shyue
 */
public class XYZCoordFileExporter extends MolFileExporter {

    /**
     * 
     * @param mol
     */
    public XYZCoordFileExporter(Molecule mol) {
        super(mol);
    }

    @Override
    public void generate() {
        output = "";
        for (Atom at : mol) {
            output += at.toString() + "\n";
        }
    }
}
