package net.shyue.smurf.Parser;

import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;

/**
 *
 * @author shyue
 */
public interface MolParser {

    /**
     * Parse input into molecule object
     * @throws MolParserException
     */
    void parse() throws MolParserException;

    /**
     * Returns molecule
     * @return
     * @throws BuilderException 
     */
    Molecule getMolecule() throws BuilderException;

    /**
     *
     */
    class MolParserException extends Exception {

        MolParserException() {
            super("Error parsing file!");
        }

        MolParserException(String msg) {
            super(msg);
        }
    }
}
