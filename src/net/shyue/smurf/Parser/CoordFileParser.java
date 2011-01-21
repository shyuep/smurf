package net.shyue.smurf.Parser;

import net.shyue.smurf.Structure.DefaultMolBuilder;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;

/**
 *
 * @author shyue
 */
public class CoordFileParser extends MolFileParser {
    
    /**
     * Creates new instance of molParser and parse file with supplied filename
     * @param filename_in
     * @throws MolParserException
     */
    public CoordFileParser(String filename_in) throws MolParserException {
        super(filename_in);
    }
    
  
    /**
     * Method to parse Gaussian Input files
     */
    @Override
    public void parse() throws MolParserException {
        String filecontents = readWholeFile();
        MixedCoordParser coordParser = new MixedCoordParser(filecontents);
        coordParser.parse();
        try {
            mBuilder = new DefaultMolBuilder(coordParser.getMolecule());
        } catch (BuilderException ex) {
            throw new MolParserException(ex.getMessage());
        }
        mBuilder.setName(filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename);
        mBuilder.setCharge(0);
        mBuilder.setSpinMult(1);
        fileParsed = true;
    }

}
