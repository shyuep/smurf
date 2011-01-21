package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Utils.StringConvUtils;
import java.util.List;

/**
 *
 * @author shyue
 */
public class FunctionalGroup implements Comparable<FunctionalGroup>{
    private List<Atom> atoms;

    public FunctionalGroup (List<Atom> _atoms){
        atoms = _atoms;
    }

    public String getChemicalFormula(){
        return StringConvUtils.chemicalFormulaFromAtomList(atoms);
    }

    public double getMolWt(){
        double wt = 0;
        for (Atom at:atoms)
        {
            wt+= at.getAtWt();
        }
        return wt;
    }

    @Override
    public int compareTo(FunctionalGroup o) {
        if (this.getMolWt()-o.getMolWt()<0)
            return -1;
        else if (this.getMolWt()-o.getMolWt()>0)
            return 1;
        else
            return 0;
    }



}
