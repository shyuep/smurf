package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Utils.*;
import java.util.Set;

/**
 *
 * @author Shyue
 */
public class ClusterSimilarityComparator implements SimilarityComparator<String, Set<Atom>> {

    @Override
    public boolean areSimilar(Set<Atom> o1, Set<Atom> o2) {
        return StringConvUtils.chemicalFormulaFromAtomList(o1).equals(StringConvUtils.chemicalFormulaFromAtomList(o2));
    }

    @Override
    public String getIdentifier(Set<Atom> o1) {
        return StringConvUtils.chemicalFormulaFromAtomList(o1);
    }


}
