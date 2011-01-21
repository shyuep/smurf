package net.shyue.smurf.Structure;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import static java.lang.Math.*;

/**
 * Some molecule specific functions operating on javax.vecmath class to make 
 * manipulations easier.
 * @author shyue
 */
public class MolVecMath {

    /* Returns angle specified by three points.
     * @param a First point.
     * @param b Second point.
     * @param c Third point
     * @return Angle in degrees.
     */
    public static double angle(Point3d a, Point3d b, Point3d c) {
        Vector3d vec1 = new Vector3d();
        Vector3d vec2 = new Vector3d();
        vec1.sub(a, b);
        vec2.sub(c, b);
        double ans = vec1.dot(vec2) / vec1.length() / vec2.length();
        /* Corrects for stupid numerical error which may result in acos being
         * operated on a number with absolute value larger than 1
         * */
        if (ans > 1) {
            ans = 1;
        } else if (ans < -1) {
            ans = -1;
        }
        return Math.acos(ans) * 180 / PI;
    }
    
    /* Returns angle specified by three atom.
     * @param a First point.
     * @param b Second point.
     * @param c Third point
     * @return Angle in degrees.
     */
    public static double angle(Atom a, Atom b, Atom c) {
        return angle(a.getCoord(),b.getCoord(),c.getCoord());
    }

    /**
     * Returns dihedarl angle between 3 vectors
     * @param b1 First vector
     * @param b2 Second vector
     * @param b3 Third vector
     * @return Dihedral angle in degress.
     */
    public static double dihedral(Vector3d b1, Vector3d b2, Vector3d b3) {
        Vector3d tmpVec1 = new Vector3d();
        tmpVec1.cross(b2, b3);
        double m = b2.length() * (b1.dot(tmpVec1));
        Vector3d tmpVec2 = new Vector3d();
        tmpVec2.cross(b1, b2);
        double n = tmpVec2.dot(tmpVec1);
        return atan2(m, n) * 180 / PI;
    }
    
        /**
     * Returns dihedarl angle between 3 vectors
         * @param i 
         * @param j 
         * @param k 
         * @param l 
     * @return Dihedral angle in degress.
     */
    public static double dihedral(Atom i, Atom j, Atom k, Atom l) {
        Vector3d vec1 = new Vector3d(k.getCoord());
        Vector3d vec2 = new Vector3d(j.getCoord());
        Vector3d vec3 = new Vector3d(i.getCoord());
        vec1.sub(l.getCoord());
        vec2.sub(k.getCoord());
        vec3.sub(j.getCoord());
        return dihedral(vec1, vec2, vec3);
    }

    /**
     * Scale position relative to fixed point
     * @param movablepoint 
     * @param fixedpoint 
     * @param newlength 
     * @return a-b
     */
    public static Point3d scale(Point3d movablepoint, Point3d fixedpoint, double newlength) {
        Vector3d bond = new Vector3d();
        bond.sub(movablepoint, fixedpoint);
        bond.scale(newlength / bond.length());
        Point3d newCoord = new Point3d(fixedpoint);
        newCoord.add(bond);
        return newCoord;
    }

    /**
     * Special method to determine sign of dihedral angle
     */
    private static double atan2(double b, double a) {
        double c = atan(abs(b / a));
        double signb;
        if (b > 0) {
            signb = 1;
        } else {
            signb = -1;
        }
        if (b != 0) {
            if (a > 0) {
                c = c * signb;
            } else if (a == 0) {
                c = PI / 2 * signb;
            } else {
                c = (PI - c) * signb;
            }
        } else {
            if (a > 0) {
                c = 0;
            } else if (a == 0) {
                c = PI;
            } else {
                c = PI;
            }
        }

        return c;

    }
}

