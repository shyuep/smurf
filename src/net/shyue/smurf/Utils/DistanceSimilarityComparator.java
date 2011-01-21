package net.shyue.smurf.Utils;

import net.shyue.smurf.Structure.Atom;
import javax.vecmath.Point3d;

/**
 * SimilarityComparator for binning by distance to origin within a certain tolerance.
 * @author shyue
 */
public class DistanceSimilarityComparator implements SimilarityComparator<Double, Atom>{
    
    double tolerance;

    public DistanceSimilarityComparator (double _tolerance){
        tolerance = _tolerance;
    }

    @Override
    public boolean areSimilar(Atom o1, Atom o2) {
        if (o1==o2){
            return true;
        }
        double dist1 = o1.getCoord().distance(new Point3d(0,0,0));
        double dist2 = o2.getCoord().distance(new Point3d(0,0,0));
        if (Math.abs(dist1-dist2)<tolerance)
        {
            return true;
        }
        return false;
    }

    @Override
    public Double getIdentifier(Atom o1) {
        return o1.getCoord().distance(new Point3d(0,0,0));
    }

}
