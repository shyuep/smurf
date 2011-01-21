package net.shyue.smurf.Structure;

import net.shyue.smurf.Utils.SimpleCSVParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple class to hold typical bond lengths for efficient access.  This is 
 * preferred over using a external data file since these data are accessed 
 * frequently.
 * @author shyue
 */
public final class BondLengthsData {

    private List<Element> SPECIES1;
    private List<Float> BONDORDER;
    private List<Element> SPECIES2;
    private List<Double> BONDLENGTHS;
    private static BondLengthsData instance = new BondLengthsData();

    private BondLengthsData() {
        

        SPECIES1 = new ArrayList<Element>(100);
        SPECIES2 = new ArrayList<Element>(100);
        BONDORDER = new ArrayList<Float>(100);
        BONDLENGTHS = new ArrayList<Double>(100);
        try {
            SimpleCSVParser parser = new SimpleCSVParser(BondLengthsData.class.getResourceAsStream("BondLengthsData.csv"));
            for (String [] row : parser.getData()){
                SPECIES1.add(Element.valueOf(row[0]));
                BONDORDER.add(Float.parseFloat(row[1]));
                SPECIES2.add(Element.valueOf(row[2]));
                BONDLENGTHS.add(Double.parseDouble(row[3]));
            }

        } catch (IOException ex) {
            System.out.println("Fatal error in BondLengthsData : " + ex.getMessage());
            System.exit(-1);
        } catch (Exception ex)
        {
            System.out.println("Fatal error in BondLengthsData : " + ex.getMessage());
            
        }
    }

    /**
     * Returns typical bond length between 2 species.  Only single bonds are 
     * supported currently.
     * @param species1 Species 1
     * @param species2 Species 2
     * @param bondOrder Bond order
     * @return Bond length
     */
    public static double getBondLength(Element species1, Element species2, float bondOrder) {
        for (int i = 0; i < instance.SPECIES1.size(); i++) {
            if ((bondOrder == instance.BONDORDER.get(i)) &&
           (((species1 == instance.SPECIES1.get(i)) && (species2 == instance.SPECIES2.get(i))) ||
                    ((species1 == instance.SPECIES2.get(i)) &&
                    (species2 == instance.SPECIES1.get(i))))) {
                return instance.BONDLENGTHS.get(i);
            }
        }

        throw new IllegalArgumentException("Bond lengths not found in table for " + species1 + species2 + Float.toString(bondOrder) + "!");
    }
}
