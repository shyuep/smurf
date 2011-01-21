package net.shyue.smurf.Utils;

import net.shyue.smurf.Structure.Bond;

/**
 *
 * @author Shyue
 */
public class BondSimilarityComparator implements SimilarityComparator<String, Bond> {

    @Override
    public boolean areSimilar(Bond o1, Bond o2) {

        return o1.getType().equals(o2.getType());
    }

    @Override
    public String getIdentifier(Bond o1) {
        return o1.getType();
    }


}
