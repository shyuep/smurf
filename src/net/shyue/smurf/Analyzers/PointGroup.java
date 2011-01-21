package net.shyue.smurf.Analyzers;

import java.util.Iterator;
import java.util.List;
import javax.vecmath.Point3d;

/**
 * Object representation of a point group.  Collects symmetry operations 
 * pertaining to a group and the Schoeflies symbol.
 * @author shyue
 * @version 1.0
 */
public final class PointGroup implements Iterable<SymmetryOperation>{

    private List<SymmetryOperation> operations;
    private String schSymbol;
    
    /**
     * Creates instance of point group.
     * @param ops_in Collection of symmetry operations.
     * @param symbol_in Schoenflies Symbol.
     */
    public PointGroup(String symbol_in, List<SymmetryOperation> ops_in){
        operations = ops_in;
        schSymbol = symbol_in;
    }
   
    /**
     * 
     * @return Full set of symmetry operations in point group.
     */
    public List<SymmetryOperation> getOperations(){
        return operations;
    }
    
    /**
     * 
     * @return Schoenflies symbol of point group.
     */
    public String getSchSym(){
        return schSymbol;
    }
  
    /**
     * Tests if two positions are symmetrically equivalent under point group within a provided tolerance.
     * @param a First comparison Point3d.
     * @param b Second comparison Point3d
     * @param tolerance Tolerance distance
     * @return True if positions are equivalent, false otherwise.
     */
    public boolean isSymmetricallyEquivalent(Point3d a, Point3d b, double tolerance){
        boolean isequivalent = false;
        for (SymmetryOperation key : operations)
        {
           Point3d testcoord = key.transformPoint(a);
           if (testcoord.distance(b)<tolerance)
           {
               isequivalent = true;
               break;
           }
        }
        return isequivalent;
        
    }
    

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuffer s = new StringBuffer((operations.size()*4+3)*40);
        s.append("Point group : " + schSymbol + "\n");
        s.append(String.format("Total no. of symmetry operations = %d\n", operations.size()));
        s.append("All symmetry operations in molecule point group : \n");
        for (SymmetryOperation symMat : operations) {
            s.append(symMat.toString() + "\n");
        }
        return s.toString();
    }

    /**
     *
     * @return
     */
    @Override
    public Iterator<SymmetryOperation> iterator() {
        return operations.iterator();
    }
    
    
}
