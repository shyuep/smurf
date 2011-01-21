package net.shyue.smurf.Structure;

import net.shyue.smurf.Utils.StringConvUtils;
import java.util.HashSet;
import javax.vecmath.Point3d;

/**
 * An Atom Cluster Class to
 * @author Shyue
 */
public class AtomCluster extends HashSet<Atom>{

    public Point3d getCenter() {
        double x = 0;
        double y = 0;
        double z = 0;
        int num = this.size();
        for (Atom at : this) {
            x += at.getCoord().x;
            y += at.getCoord().y;
            z += at.getCoord().z;
        }
        return new Point3d(x / num, y / num, z / num);


    }

        public Point3d getCenterOfMass() {
        double x = 0;
        double y = 0;
        double z = 0;
        double molWt = 0;
        for (Atom at : this) {
            double atWt = at.getAtWt();
            x += at.getCoord().x * atWt;
            y += at.getCoord().y * atWt;
            z += at.getCoord().z * atWt;
            molWt += atWt;
        }
        return new Point3d(x / molWt, y / molWt, z / molWt);


    }

    public String getFormula()
    {
        return StringConvUtils.chemicalFormulaFromAtomList(this);
    }

    @Override
    public String toString()
    {
        return String.format("%s centered at %s", getFormula(),getCenter());
    }

}
