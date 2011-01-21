package net.shyue.smurf.Structure;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import net.shyue.smurf.Analyzers.SymmetryOperation;

/**
 * A Builder class to make a Molecule object.  Other classes which can make 
 * changes to molecules can inherit from this object.
 * @author Shyue Ping
 */
public class DefaultMolBuilder implements MolBuilder{


    protected String name;
    /**
     *
     */
    protected ArrayList<Atom> sites;
    /**
     *
     */
    protected int charge;
    /**
     *
     */
    protected int spinMult;

    /**
     * Default constructor
     */
    public DefaultMolBuilder() {
        name = "";
        sites = new ArrayList<Atom>();
        charge = 0;
        spinMult = 1;
    }
    
    /**
     * Constructor which initializes the builder using an existing molecule.
     * @param mol_in 
     */
    public DefaultMolBuilder(Molecule mol_in) {
        name = mol_in.getName();
        sites = new ArrayList<Atom>();
        charge = mol_in.getCharge();
        spinMult = mol_in.getSpinMult();
        for (Atom at : mol_in)
        {
            sites.add(at);
        }
    }

    /**
     * Get current name of molecule
     * @return Name of molecule
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set name of Molecule
     * @param name_in
     */
    @Override
    public void setName(String name_in) {
        name = name_in;
    }

    /**
     * Set charge of Molecule
     * @param charge_in
     */
    @Override
    public void setCharge(int charge_in) {
        charge = charge_in;
    }

    /**
     * Set spin multiplicity of Molecule
     * @param spinMult_in
     */
    @Override
    public void setSpinMult(int spinMult_in) {
        spinMult = spinMult_in;
    }


    /**
     * Add a new Atom using cartesian coord
     * @param species
     * @param x
     * @param y
     * @param z
     */
    @Override
    public void addAtom(Element species, double x, double y, double z) {
        sites.add(new Atom(species, new Point3d(x, y, z)));
    }

    /**
     * Add a new Atom using a Point3d
     * @param species
     * @param coord
     */
    @Override
    public void addAtom(Element species, Point3d coord) {
        sites.add(new Atom(species, coord));
    }

    /**
     * Add an Atom using ZMatrix specification
     * @param species
     * @param NN1_index
     * @param bondlength_in
     * @param NN2_index
     * @param angle_in
     * @param NN3_index
     * @param dih_in
     */
    @Override
    public void addAtom(Element species, int NN1_index, double bondlength_in,
            int NN2_index, double angle_in, int NN3_index, double dih_in) {
        Point3d newcoord = ZMat2Cart(sites, NN1_index, bondlength_in,
                NN2_index, angle_in, NN3_index, dih_in);
        sites.add(new Atom(species, newcoord));
    }
    
    /**
     * Builds and returns created Molecule with appropriate error checking to 
     * ensure that atoms are not too close and spin multiplicity is correct.
     * @return Created Molecule
     */
    @Override
    public Molecule build() throws BuilderException {
        // Error checking to ensure that atoms are not too close and that total
        // number of electrons and spin multiplicity are consistent.  For spin,
        // correct is made to a default singlet for atoms with even number of 
        // electrons and doublet for those with odd.
        int electrons = 0;
        for (Atom at : sites) {
            electrons += at.getAtNo();
            
            //--Disabled distance checking since it requires all permutations of bonds to be in table--
//            for (int j = i + 1; j < n; j++) {
//                if (Vector3.dist(sites.get(i).getCoord(), sites.get(j).getCoord()) <
//                        0.5 * BondLengthsData.getBondLength(sites.get(i).getSpecies(),
//                        sites.get(j).getSpecies(), 1)) {
//                    throw new BuilderException("Some atoms are too close!");
//                }
//            }
        }

        electrons = electrons - charge;
        if ((spinMult - electrons) % 2 != 0) {
            spinMult = electrons % 2 + 1;
        }

        return new Molecule(name, charge, spinMult, sites);
    }
    
    /**
     * Converts a zmatrix specification to a cartesian coordinate based on an
     * existing set of atoms
     * @param currentAtomSet
     * @param NN1_index
     * @param bondlength_in
     * @param NN2_index
     * @param angle_in
     * @param NN3_index
     * @param dih_in
     * @return
     */
    public static Point3d ZMat2Cart(List<Atom> currentAtomSet, int NN1_index, double bondlength_in,
            int NN2_index, double angle_in, int NN3_index, double dih_in) {

        if ((NN1_index > currentAtomSet.size()) || (NN1_index > currentAtomSet.size()) || (NN1_index > currentAtomSet.size())) {
            String error_msg = "Input Z-matrix refers to atoms not yet in molecule!";
            throw new IllegalArgumentException(error_msg);
        }
        Point3d NN1, NN2, NN3, coord;
        Vector3d axis = new Vector3d();
        double adj;
        switch (currentAtomSet.size()) {
            case 0:
                coord = new Point3d(0, 0, 0);
                break;
            case 1:
                coord = new Point3d(0, 0, bondlength_in);
                break;
            case 2:
                NN1 = currentAtomSet.get(NN1_index).getCoord();
                NN2 = currentAtomSet.get(NN2_index).getCoord();
                axis = new Vector3d(0, 1, 0);
                coord = SymmetryOperation.Rotation(NN1, axis, angle_in).transformPoint(NN2);
                coord = MolVecMath.scale(coord, NN1, bondlength_in);
                break;
            default:
                NN1 = currentAtomSet.get(NN1_index).getCoord();
                NN2 = currentAtomSet.get(NN2_index).getCoord();
                NN3 = currentAtomSet.get(NN3_index).getCoord();
                Vector3d vec1 = new Vector3d(NN3);
                vec1.sub(NN2);
                Vector3d vec2 = new Vector3d(NN1);
                vec2.sub(NN2);
                axis.cross(vec1, vec2);
                coord = SymmetryOperation.Rotation(NN1, axis, angle_in).transformPoint(NN2);
                vec1.sub(coord, NN1);
                vec2.sub(NN1, NN2);
                Vector3d vec3 = new Vector3d();
                vec3.cross(vec1, vec2);
                adj = vec3.angle(axis) * 180 / Math.PI;
                axis.sub(NN1,NN2);
                coord = SymmetryOperation.Rotation(NN1, axis, dih_in - adj).transformPoint(coord);
                coord = MolVecMath.scale(coord, NN1, bondlength_in);
                break;
        }
        return coord;
    }

    @Override
    public void setAtom(int i, Atom at) {
        if (sites.size() < i+1)
        {
            throw new IllegalArgumentException();
        }
        else
        {
            sites.set(i, at);
        }
    }

    @Override
    public void setAtom(Atom toBeReplaced, Atom replacementAtom) {
        if (sites.contains(toBeReplaced))
        {
            throw new IllegalArgumentException("Atom to be replaced not present");
        }
        else
        {
            sites.set(sites.indexOf(toBeReplaced), replacementAtom);
        }
    }
}
