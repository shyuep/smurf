package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.Atom;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author shyue
 */
public class AtomListComparator implements Comparator<List<Atom>> {

    public AtomListComparator() {
    }

    private double sumAtWt(List<Atom> atList) {
        double wt = 0;
        for (Atom at : atList) {
            wt += at.getAtWt();
        }
        return wt;
    }

    @Override
    public int compare(List<Atom> o1, List<Atom> o2) {
        if (o1.size() == 0 && o2.size() == 0) {
            return 0;
        }
        if (o1.size() < o2.size()) {
            return -1;
        } else if (o1.size() > o2.size()) {
            return 1;
        } else {
            if (o1.get(0).getAtWt() < o2.get(0).getAtWt()) {
                return -1;
            } else if (o1.get(0).getAtWt() < o2.get(0).getAtWt()) {
                return 1;
            } else {
                if (sumAtWt(o1) < sumAtWt(o2)) {
                    return -1;
                } else if (sumAtWt(o1) > sumAtWt(o2)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

    }
}
