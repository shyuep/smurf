package net.shyue.smurf.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Structure.DefaultMolBuilder;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;

/**
 * Abstract class for a molecule input file parser containing minimal 
 * implementation common to all parsers. All parsers should extend 
 * this class.
 * 
 * @author shyue
 */
public abstract class MolFileParser implements MolParser {

    /**
     * MoleculeBuilder used to create new molecule.
     */
    protected DefaultMolBuilder mBuilder;
    /**
     * Name of file without the full path.
     */
    protected File file;
    protected String filename;
    protected boolean fileParsed;

    /**
     *
     * @param filename_in
     * @throws MolParser.MolParserException 
     */
    public MolFileParser(String filename_in) throws MolParser.MolParserException {

        mBuilder = new DefaultMolBuilder();
        fileParsed = false;
        file = new File(filename_in);
        filename = file.getName();
    }

    /**
     */
    @Override
    public abstract void parse() throws MolParser.MolParserException;

    /**
     * Contents of file read.
     * @return
     * @throws MolParser.MolParserException
     */
    protected String readWholeFile() throws MolParser.MolParserException {
        String filecontents = "";
        try {
            filecontents = readWholeFile(file.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            throw new MolParserException(ex.getMessage());
        } catch (IOException ex) {
            throw new MolParserException(ex.getMessage());
        }
        return filecontents;
    }

    public static String readWholeFile(String filename) throws FileNotFoundException, IOException {
        String filecontents = "";
        File myfile = new File(filename);
        FileInputStream fis = new FileInputStream(myfile);
        InputStreamReader isr = new InputStreamReader(fis);
        char[] chrArr = new char[(int) myfile.length()];
        isr.read(chrArr);
        isr.close();
        filecontents = (new String(chrArr)).trim();
        return filecontents;
    }

    @Override
    public Molecule getMolecule() throws BuilderException {
        if (fileParsed) {
            Molecule mol = null;
            mol = mBuilder.build();
            return mol;
        } else {
            throw new IllegalStateException("File not yet parsed! Call parse() first!");
        }

    }
}
