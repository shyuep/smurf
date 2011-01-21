package net.shyue.smurf.Exporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import net.shyue.smurf.Structure.Molecule;

/**
 * A master implementation for a molecule file exporter which takes 
 * @author shyue
 */
public abstract class MolFileExporter implements MolExporter{

    /**
     * Molecule supplied.
     */
    protected Molecule mol;
    /**
     * Output generated.
     */
    protected String output;

    /**
     *
     * @param mol_in
     */
    public MolFileExporter(Molecule mol_in) {
        mol = mol_in;
    }

    /**
     * Abstract method to generate necessary output.  Individual parsers implement
     * their own output.
     */
    @Override
    public abstract void generate();

    /**
     *
     * @param filename
     * @throws java.io.IOException
     */
    public void write(String filename) throws IOException {
        if (output == null) {
            throw new IllegalStateException("Output not yet generated before writing!");
        }
        BufferedWriter fileout = new BufferedWriter(new FileWriter(filename));
        fileout.write(output, 0, output.length());
        fileout.close();
    }

    /**
     * Returns file contents parsed.
     * @return File contents.
     */
    @Override
    public String getStringRepresentation() {
        if (output == null) {
            throw new IllegalStateException("Output not yet generated!");
        } else {
            return output;
        }
    }
}
