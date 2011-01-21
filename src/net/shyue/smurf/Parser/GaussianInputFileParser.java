package net.shyue.smurf.Parser;

import net.shyue.smurf.Structure.DefaultMolBuilder;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

/**
 * Paerse for Gaussian Input file types.  Tested with G03 input files.
 * @author shyue
 */
public class GaussianInputFileParser extends MolFileParser {

    /**
     * Creates new instance of molParser and parse file with supplied filename

     * @param filename_in
     * @throws MolParser.MolParserException
     */
    public GaussianInputFileParser(String filename_in) throws MolParser.MolParserException{
        super(filename_in);
    }

    @Override
    public void parse() throws MolParser.MolParserException {
        String filecontents = readWholeFile();
        Pattern link0Pattern = Pattern.compile("%.*");
        Pattern routePattern = Pattern.compile("^#[spn]*.*");
        Pattern chargeSpinMultPattern = Pattern.compile("^\\s*([-\\d]+\\s+\\d+\\s*)+$");
        boolean routeParsed = false;
        boolean titleParsed = false;
        boolean chargespinParsed = false;
        String name = "";
        int charge = 0;
        int spinMult = 1;
        StringBuffer coordtxtBuffer = new StringBuffer();
        BufferedReader br = new BufferedReader(new StringReader(filecontents));
        String str;
        try {
            while ((str = br.readLine()) != null) {
                if (link0Pattern.matcher(str).matches()) {
                    continue;
                } else if (!routeParsed) {
                    if (routePattern.matcher(str).matches()) {
                        routeParsed = true;
                        //System.out.println("Route section found: " + str);
                    } else {
                        throw new MolParserException("Missing route section!");
                    }
                } else if (!titleParsed) {
                    if (str.matches("^\\s*\\w+.*$")) {
                        titleParsed = true;
                        name = str.trim();
                        //System.out.println("Title section found : " + str.trim());
                    }
                } else if (!chargespinParsed) {
                    if (chargeSpinMultPattern.matcher(str).matches()) {
                        chargespinParsed = true;
                        String[] chargespin = str.trim().split("\\s");
                        charge = Integer.parseInt(chargespin[0]);
                        spinMult = Integer.parseInt(chargespin[1]);
                        //System.out.println("Charge found : " + str.trim());
                    }
                } else {

                    //System.out.println("Coord found : " + str.trim());
                    coordtxtBuffer.append(str);
                    coordtxtBuffer.append("\n");
                }
            }
            br.close();
        } catch (IOException ioe) {
            throw new MolParserException("Can't read string!");
        }
        if (!(routeParsed && titleParsed && chargespinParsed)) {
            throw new MolParserException("End of file reached with sections missing in Gaussian input file!");
        }
        String coord_txt = coordtxtBuffer.toString();
        MixedCoordParser coordParser = new MixedCoordParser(coord_txt);
        coordParser.parse();
        try {
            mBuilder = new DefaultMolBuilder(coordParser.getMolecule());
        } catch (BuilderException ex) {
            throw new MolParserException(ex.getMessage());
        }
        mBuilder.setName(name);
        mBuilder.setCharge(charge);
        mBuilder.setSpinMult(spinMult);
        fileParsed = true;
    }

}
