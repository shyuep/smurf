package net.shyue.smurf.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.shyue.smurf.Parser.MolParser.MolParserException;
import net.shyue.smurf.Structure.Element;

/**
 * A standard XYZ file parser.
 * @author shyue
 */
public class XYZFileParser extends MolFileParser {

    /**
     * Creates new instance of molParser and parse file with supplied filename
     * @param filename_in
     * @throws ParserException 
     */
    public XYZFileParser(String filename_in) throws MolParserException {
        super(filename_in);
    }

    /**
     * Method to parse Gaussian Input files
     */
    @Override
    public void parse() throws MolParserException {
        String filecontents = readWholeFile();
        Pattern numAtomsPattern = Pattern.compile("\\d+");
        Pattern xyzPattern = Pattern.compile("(\\w+)\\s+([\\d\\.eE\\-]+)\\s+([\\d\\.eE\\-]+)\\s+([\\d\\.eE\\-]+)");
        Pattern mixedSpeciesPattern = Pattern.compile("([A-Za-z]+)\\d+");
        boolean numAtomsParsed = false;
        String name = null;
        int numAtoms = 0;
        int atomCount = 0;
        BufferedReader br = new BufferedReader(new StringReader(filecontents));
        String str;
        try {
            while ((str = br.readLine()) != null) {
                str = str.trim();
                if (!str.matches("")) {
                    if (numAtomsPattern.matcher(str).matches()) {
                        numAtoms = Integer.parseInt(str);
                        //System.out.println(numAtoms);
                        numAtomsParsed = true;
                    } else if (numAtomsParsed) {
                        Matcher xyzMatcher = xyzPattern.matcher(str);

                        if (xyzMatcher.find()) {
                            String species = xyzMatcher.group(1);
                            Matcher mixedSpeciesMatcher = mixedSpeciesPattern.matcher(species);
                            if (mixedSpeciesMatcher.find()) {
                                species = mixedSpeciesMatcher.group(1);
                            } else if (species.matches("\\d+")) {
                                species = Element.getSpecies(Integer.parseInt(species)).toString();
                            } else {
                                species = xyzMatcher.group(1);
                            }
                            mBuilder.addAtom(Element.valueOf(species), Double.parseDouble(xyzMatcher.group(2)),
                                    Double.parseDouble(xyzMatcher.group(3)), Double.parseDouble(xyzMatcher.group(4)));
                            atomCount++;


                        } else {
                            name = str;
                        }
                    }

                }
            }
            br.close();
        } catch (IOException ioe) {
            throw new MolParserException("Can't read string!");
        }

        if (numAtomsParsed && (numAtoms != atomCount)) {
            String output = "Number of atoms specification and actual number do not match!\n";
            output += String.format("Read atoms = %d, Counted atoms = %d", numAtoms, atomCount);
            throw new MolParserException(output);
        }
        if (name != null) {
            mBuilder.setName(name);
        } else {
            mBuilder.setName(filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename);
        }
        fileParsed = true;
    }
}
