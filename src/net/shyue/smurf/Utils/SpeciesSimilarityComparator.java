package net.shyue.smurf.Utils;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Element;

/**
 * Binning by element.
 * @author shyue
 */
public class SpeciesSimilarityComparator implements SimilarityComparator<Element,Atom>{

    public SpeciesSimilarityComparator (){

    }

    @Override
    public boolean areSimilar(Atom o1, Atom o2) {
        if (o1==o2){
            return true;
        }
        if (o1.getSpecies() == o2.getSpecies())
        {
            return true;
        }
        return false;
    }

    @Override
    public Element getIdentifier(Atom o1) {
        return o1.getSpecies();
    }

}
