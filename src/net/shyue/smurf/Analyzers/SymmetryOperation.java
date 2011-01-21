package net.shyue.smurf.Analyzers;

import Jama.Matrix;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import static java.lang.Math.*;

/**
 * Symmetry operation object with appropriate static factories to create the
 * various common operations such as inversion, reflection, rotation and roto-
 * reflection.
 * @author shyue
 */
public final class SymmetryOperation {

    private final Matrix OpMatrix;
    private final String type;
    
    /**
     * Returns an inversion symmetry operation
     * @return Inversion symmetry operation
     */
    public static SymmetryOperation Inversion() {
        double [][] M_arr = {{-1, 0,0,0},{0,-1,0,0},{0,0,-1,0},{0,0,0,1}};
        return new SymmetryOperation("i",new Matrix(M_arr));
    }
    
    
    /**
     * Returns a rotation symmetry operation
     * @param origin Origin of rotation
     * @param axis Rotation axis
     * @param theta_in Angle to rotate in degrees.
     * @return Rotation symmetry operation
     */
    public static SymmetryOperation Rotation(Point3d origin, Vector3d axis,double theta_in) {
        double theta = theta_in*PI/180;
        String name = "R" + Long.toString(round(2*PI/theta));
        double a = origin.x;
        double b = origin.y;
        double c = origin.z;
        double u = axis.x;
        double v = axis.y;
        double w = axis.z;
        // Set some intermediate values.
        double u2 = u*u;
        double v2 = v*v;
        double w2 = w*w;
        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);
        double l2 = u2 + v2 + w2;
        double l =  Math.sqrt(l2);

        if(l2 < 0.000000001) {
            System.err.println("RotationMatrix: direction vector too short!");
        }

        // Build the matrix entries element by element. 
        double m11 = (u2 + (v2 + w2) * cosT)/l2;
        double m12 = (u*v * (1 - cosT) - w*l*sinT)/l2;
        double m13 = (u*w * (1 - cosT) + v*l*sinT)/l2;
        double m14 = (a*(v2 + w2) - u*(b*v + c*w) 
            + (u*(b*v + c*w) - a*(v2 + w2))*cosT + (b*w - c*v)*l*sinT)/l2;
        
        double m21 = (u*v * (1 - cosT) + w*l*sinT)/l2;
        double m22 = (v2 + (u2 + w2) * cosT)/l2;
        double m23 = (v*w * (1 - cosT) - u*l*sinT)/l2;
        double m24 = (b*(u2 + w2) - v*(a*u + c*w) 
            + (v*(a*u + c*w) - b*(u2 + w2))*cosT + (c*u - a*w)*l*sinT)/l2;

        double m31 = (u*w * (1 - cosT) - v*l*sinT)/l2;
        double m32 = (v*w * (1 - cosT) + u*l*sinT)/l2;
        double m33 = (w2 + (u2 + v2) * cosT)/l2;
        double m34 = (c*(u2 + v2) - w*(a*u + b*v) 
            + (w*(a*u + b*v) - c*(u2 + v2))*cosT + (a*v - b*u)*l*sinT)/l2;
        
        Matrix newM = new Matrix(new double[][] {{m11, m12, m13, m14}, 
                                 {m21, m22, m23, m24},
                                 {m31, m32, m33, m34},
                                 {0,   0,   0,   1}});
        
        return new SymmetryOperation(name,newM);
        
        
    }

    /**
     * Returns reflection symmetry operation
     * @param origin A point in which the mirror plane passes through
     * @param normal Normal of the plane
     * @return Reflection symmetry operation
     */
    public static SymmetryOperation Reflect(Point3d origin, Vector3d normal) {
        Vector3d n = new Vector3d(normal);
        n.normalize();
        double a = origin.x;
        double b = origin.y;
        double c = origin.z;
        double u = n.x;
        double v = n.y;
        double w = n.z;

        double [][] TranslationArr = {{0,0,0,-a},{0,0,0,-b},{0,0,0,-c},{0,0,0,1}};
        Matrix TranslationMat = new Matrix(TranslationArr);
        double Mxx = 1-2*pow(u,2);
        double Myy = 1-2*pow(v,2);
        double Mzz = 1-2*pow(w,2);
        double Mxy = -2*u*v;
        double Mxz = -2*u*w;
        double Myz = -2*v*w;
        double [][] M_Arr = {{Mxx,Mxy,Mxz,0},{Mxy,Myy,Myz,0},{Mxz,Myz,Mzz,0},{0,0,0,1}};
        Matrix MirrorMat = new Matrix(M_Arr);
        
        if (sqrt(pow(a,2)+pow(b,2)+pow(c,2))>1e-6)
            MirrorMat= TranslationMat.inverse().times(MirrorMat.times(TranslationMat));
        
        return new SymmetryOperation("\u03C3",MirrorMat);
    }
    
    

    /**
     * Returns a roto-reflection symmetry operation
     * @param origin Point left invariant by roto-reflection
     * @param axis Axis of rotation / mirror normal
     * @param theta_in Angle in degrees
     * @return Roto-reflection operation
     */
    public static SymmetryOperation RotoReflection(Point3d origin, Vector3d axis,double theta_in) {
        String name = "S" + Long.toString(round(360/theta_in));
        Matrix newMat = Rotation(origin,axis,theta_in).OpMatrix.times(Reflect(origin,axis).OpMatrix);
        return new SymmetryOperation(name,newMat);
    }
    
    private SymmetryOperation(String type_in, Matrix M_in) {
        OpMatrix=M_in; // no error checking for M_in required since private use only
        type = type_in;
    }

    
    /**
     * Transform supplied Point3d with Symmetry Operation
     * @param coord Point to transform
     * @return Coordinates of transformed point.
     */
    public Point3d transformPoint(Point3d coord) {
        double [][] tmpcoord_arr = {{coord.x,coord.y,coord.z,1}};
        Matrix tmpcoord = new Matrix(tmpcoord_arr);
        tmpcoord = OpMatrix.times(tmpcoord.transpose()).transpose().getMatrix(0,0,0,2);
        return new Point3d(tmpcoord.get(0, 0),tmpcoord.get(0, 1),tmpcoord.get(0, 2));
    }
    

    /**
     * Returns a new symmetry matrix which is a product of two symmetry operations.
     * @param symOp1 First Symmetry operation
     * @param symOp2 Second Symmetry operation
     * @return symOp1.symOp2
     */
    public static SymmetryOperation product(SymmetryOperation symOp1, SymmetryOperation symOp2) {
        return new SymmetryOperation(symOp1.type+"."+symOp2.type,symOp1.OpMatrix.times(symOp2.OpMatrix));
    }
    
    /**
     * Returns symmetry matrix corresponding to inverse oepration.
     * @return Inverse of symmetry operation
     */
    public SymmetryOperation inverse() {
        return new SymmetryOperation(this.type+"^-1",this.OpMatrix.inverse());
    }

    /**
     * String representation of symmetry operation.
     * @return String representation of symmetry operation.
     */
    @Override
    public String toString(){
        String output = type +"\n";
        for (int i=0;i<3;i++)
                output += String.format("%.4f %.4f %.4f\n", OpMatrix.get(i, 0), OpMatrix.get(i, 1), OpMatrix.get(i, 2));
        return output;
    }


    /**
     * Compares if the matrix elements of two Symmetry Matrices are equal within
     * a specified tolerance
     * @param symOp1 First symmetry operation
     * @param symOp2 Second symmetry operation
     * @param tolerance
     * @return
     */
    public static boolean isEqualWithinTolerance(SymmetryOperation symOp1, SymmetryOperation symOp2, double tolerance) {
        boolean equal = true;
        for (int i=0;i<4;i++)
        {
            for(int j=0;j<4;j++)
            {

                if(abs(symOp1.OpMatrix.get(i, j)-symOp2.OpMatrix.get(i, j))>tolerance)
                {
                    equal = false;
                    break;
                }
            }
            if (!equal)
                break;
        }
        return equal;
    }
    
}
