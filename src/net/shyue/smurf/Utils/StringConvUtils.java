package net.shyue.smurf.Utils;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Element;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author shyue
 */
public final class StringConvUtils {

    private StringConvUtils (){

    }

    /**
     * Generates the chemical formula from a list of atoms.  Does not need to
     * be the whole molecule.
     * @param atomList
     * @return
     */
    public static String chemicalFormulaFromAtomList(Collection<Atom> atomList)
    {
        
        Map<Element, Integer> formula = new TreeMap<Element,Integer>();
        for (Atom at : atomList) {
            if (formula.containsKey(at.getSpecies()))
            {
                formula.put(at.getSpecies(), formula.get(at.getSpecies())+1);
            }
            else
            {
                formula.put(at.getSpecies(), 1);
            }
        }
        String chemform = "";
        for (Element key : formula.keySet())
        {
            if (formula.get(key)==1)
            {
                chemform += key;
            }
            else{
                chemform += key.toString()+formula.get(key);
            }
        }
        return chemform;
    }

    /**
     * Convert a list of atoms to a string representation of species.
     * @param atomList
     * @return
     */
    public static String atomListToSpeciesString(List<Atom> atomList) {
        String output = "";
        for (Atom at : atomList) {
            output += at.getSpecies();
        }
        return output;
    }

}
