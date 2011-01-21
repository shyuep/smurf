package net.shyue.smurf.Parser;

import net.shyue.smurf.Structure.Element;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Structure.DefaultMolBuilder;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;
import javax.vecmath.Point3d;

/**
 * A parser for a mixed coordinate string list.  Support for Zmatrix and XYZ.
 * @author shyue
 */
public class MixedCoordParser implements MolParser {

    private String coordTxt;
    private DefaultMolBuilder mBuilder;
    private boolean fileParsed;

    /**
     * Creates new instance of molParser and parse file with supplied filename
     * @param coordTxt_in 
     */
    public MixedCoordParser(String coordTxt_in) {
        coordTxt = coordTxt_in;
        mBuilder = new DefaultMolBuilder();
        fileParsed = false;
    }

    @Override
    public void parse() throws MolParserException {

        Map<String, Double> variables = parseVariables(coordTxt);
        String[] coordarr = coordTxt.split("\n");

        Pattern zmatPattern = Pattern.compile("^\\s*([A-Za-z]+)[\\w\\d\\-\\_]*([\\s,]+(\\w+)[\\s,]+(\\w+))*[\\-\\.\\s,\\w]*$");
        Pattern mixedSpeciesPattern = Pattern.compile("([A-Za-z]+)[\\d\\-\\_]+");
        Pattern xyzPattern = Pattern.compile("^\\s*([A-Za-z]+[\\w\\d\\-\\_]*)\\s+([\\d\\.eE\\-]+)\\s+([\\d\\.eE\\-]+)\\s+([\\d\\.eE\\-]+)[\\-\\.\\s,\\w.]*$");
        Matcher speciesMatcher, xyzMatcher;

        List<String> parsedSpecies = new ArrayList<String>(coordarr.length * 2);

        String species = "";
        for (int i = 0; i < coordarr.length; i++) {
            //System.out.println("Parsing " + coordarr[i]);
            if (xyzPattern.matcher(coordarr[i]).matches()) {
                //System.out.println("XYZ found " + coordarr[i]);
                xyzMatcher = xyzPattern.matcher(coordarr[i]);
                while (xyzMatcher.find()) {

                    speciesMatcher = mixedSpeciesPattern.matcher(xyzMatcher.group(1));

                    if (speciesMatcher.find()) {
                        parsedSpecies.add(xyzMatcher.group(1));
                        species = speciesMatcher.group(1);
                    } else {
                        species = xyzMatcher.group(1);
                    }
                    String[] tokenizedCoord = coordarr[i].trim().split("[,\\s]+");
                    Point3d coord;
                    if (tokenizedCoord.length > 4) {
                        coord = new Point3d(Double.parseDouble(tokenizedCoord[2]),
                                Double.parseDouble(tokenizedCoord[3]), Double.parseDouble(tokenizedCoord[4]));
                    } else {
                        coord = new Point3d(Double.parseDouble(tokenizedCoord[1]),
                                Double.parseDouble(tokenizedCoord[2]), Double.parseDouble(tokenizedCoord[3]));
                    }
                    //System.out.println(coord);
                    mBuilder.addAtom(Element.valueOf(species), coord);
                }
            }else if (zmatPattern.matcher(coordarr[i]).matches()) {
                //System.out.println("ZMAT found " + coordarr[i]);
                String[] tokenizedCoord = coordarr[i].trim().split("[,\\s]+");
                if (tokenizedCoord.length % 2 != 0) {
                    double[] var = new double[3];
                    int[] NN = new int[3];
                    speciesMatcher = mixedSpeciesPattern.matcher(tokenizedCoord[0]);
                    if (speciesMatcher.matches()) {
                        parsedSpecies.add(tokenizedCoord[0]);
                        if (speciesMatcher.find()) {
                            species = speciesMatcher.group(1);
                        }
                        for (int j = 1; j <= (tokenizedCoord.length - 1) / 2; j++) {
                            NN[j - 1] = parsedSpecies.indexOf(tokenizedCoord[2 * j - 1]) + 1;
                            var[j - 1] = variables.get(tokenizedCoord[2 * j]);
                        }
                    } else {
                        species = tokenizedCoord[0];
                        for (int j = 1; j <= (tokenizedCoord.length - 1) / 2; j++) {
                            NN[j - 1] = Integer.parseInt(tokenizedCoord[2 * j - 1]);
                            var[j - 1] = variables.get(tokenizedCoord[2 * j]);
                        }
                    }
                    mBuilder.addAtom(Element.valueOf(species), NN[0] - 1, var[0], NN[1] - 1, var[1], NN[2] - 1, var[2]);
                }
            } 
        }
        fileParsed = true;
    }


//    @Override
//    public void parse() throws MolParserException {
//
//        Map<String, Double> variables = parseVariables(coordTxt);
//        String[] coordarr = coordTxt.split("\n");
//
//        //Pattern zmatPattern = Pattern.compile("^\\s*([A-Za-z]+)[\\w\\d\\-\\_]*([\\s,]+(\\w+)[\\s,]+(\\w+))*[\\-\\.\\s,\\w]*$");
//        Pattern mixedSpeciesPattern = Pattern.compile("([A-Za-z]+)[\\d\\-\\_]+");
//        Pattern xyzPattern = Pattern.compile("^\\s*([A-Za-z]+[\\w\\d\\-\\_]*)\\s+([\\d\\.eE\\-]+)\\s+([\\d\\.eE\\-]+)\\s+([\\d\\.eE\\-]+)[\\-\\.\\s,\\w.]*$");
//
//
//        List<String> parsedSpecies = new ArrayList<String>(coordarr.length * 2);
//
//        String species = "";
//        for (int i = 0; i < coordarr.length; i++) {
//            String[] tokenizedCoord = coordarr[i].trim().split("[,\\s]+");
//            Matcher speciesMatcher= mixedSpeciesPattern.matcher(tokenizedCoord[0]);
//            //Matcher zmatMatcher = zmatPattern.matcher(coordarr[i]);
//
////            boolean zmat = false;
////            if (tokenizedCoord.length < 4 || zmatMatcher.matches())
////                zmat = true;
//            System.out.println("Parsing " + coordarr[i]);
//            if (xyzPattern.matcher(coordarr[i]).matches()) {
//                System.out.println("XYZ found " + coordarr[i]);
//                speciesMatcher = mixedSpeciesPattern.matcher(tokenizedCoord[0]);
//
//                    if (speciesMatcher.find()) {
//                        species = speciesMatcher.group(1);
//                        parsedSpecies.add(tokenizedCoord[0]);
//                    } else {
//                        species = tokenizedCoord[0];
//                    }
//                    Point3d coord;
//                    if (tokenizedCoord.length > 4) {
//                        coord = new Point3d(Double.parseDouble(tokenizedCoord[2]),
//                                Double.parseDouble(tokenizedCoord[3]), Double.parseDouble(tokenizedCoord[4]));
//                    } else {
//                        coord = new Point3d(Double.parseDouble(tokenizedCoord[1]),
//                                Double.parseDouble(tokenizedCoord[2]), Double.parseDouble(tokenizedCoord[3]));
//                    }
//                    //System.out.println(coord);
//                    mBuilder.addAtom(Element.valueOf(species), coord);
//
//            } else{
//                System.out.println("ZMAT found " + coordarr[i]);
//                if (tokenizedCoord.length % 2 != 0) {
//                    double[] var = new double[3];
//                    int[] NN = new int[3];
//                    speciesMatcher = mixedSpeciesPattern.matcher(tokenizedCoord[0]);
//                    if (speciesMatcher.find()) {
//                        parsedSpecies.add(tokenizedCoord[0]);
//                        species = speciesMatcher.group(1);
//
//                    } else
//                    {
//                        species = tokenizedCoord[0];
//                    }
//                    System.out.println(species);
//                        for (int j = 1; j <= (tokenizedCoord.length - 1) / 2; j++) {
//                            NN[j - 1] = parsedSpecies.indexOf(tokenizedCoord[2 * j - 1]) + 1;
//                            var[j - 1] = variables.get(tokenizedCoord[2 * j]);
//                            System.out.println(NN[j-1]);
//                            System.out.println(var[j-1]);
//                        }
//                    mBuilder.addAtom(Element.valueOf(species), NN[0] - 1, var[0], NN[1] - 1, var[1], NN[2] - 1, var[2]);
//                }
//            }
//
//
//        }
//        fileParsed = true;
//    }



    private Map<String, Double> parseVariables(String coordtxt) throws MolParserException {
        Pattern varPattern = Pattern.compile("^\\s*([A-Za-z]+\\S*)[\\s=,]+([\\d-\\.]+)\\s*$", Pattern.MULTILINE);
        Matcher m = varPattern.matcher(coordtxt);
        Map<String, Double> variables = new HashMap<String, Double>();
        while (m.find()) {
            //System.out.println("Variables found "+m.group());
            variables.put(m.group(1), Double.parseDouble(m.group(2)));
        }
        return variables;
    }

    @Override
    public final Molecule getMolecule() throws BuilderException {
        if (fileParsed) {
            Molecule mol = null;
            mol = mBuilder.build();
            return mol;
        } else {
            throw new IllegalStateException("File not yet parsed! Call parse() first!");
        }
    }
}
