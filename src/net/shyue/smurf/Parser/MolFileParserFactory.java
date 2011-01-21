package net.shyue.smurf.Parser;

import net.shyue.smurf.Parser.MolParser.MolParserException;

/**
 * Factory class to select appropriate parser based on supplied file.  
 * "Unintelligent" version as of now since it is purely determined by the file 
 * extension with the default being a coordinate specification type such as xyz
 * or zmatrix.  Improvements can be made to guess structure from internal file
 * contents.
 * @author shyue
 * @version 1.0
 */
public class MolFileParserFactory {

    private MolFileParserFactory() {
    }

    /**
     * 
     * @param filename
     * @return
     * @throws MolParserException
     */
    public static MolFileParser getParser(String filename) throws MolParserException {
        String fileext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1, filename.length()) : "";
        if (fileext.equalsIgnoreCase("com") | fileext.equalsIgnoreCase("gjf")) {
            return new GaussianInputFileParser(filename);
        } else if (fileext.equalsIgnoreCase("out") | fileext.equalsIgnoreCase("log")) {
            return new GaussianOutputFileParser(filename);
        } else if (fileext.equalsIgnoreCase("xyz")) {
            return new XYZFileParser(filename);
        } else if (fileext.equalsIgnoreCase("geom")){
            return new CoordFileParser(filename);
        } else{
            // Defaults to using jmol's own parsers.
            return new JmolParserAdapter(filename);
        }
    }
}
