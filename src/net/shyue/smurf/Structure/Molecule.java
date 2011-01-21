package net.shyue.smurf.Structure;

import net.shyue.smurf.Utils.StringConvUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.vecmath.Point3d;

/**
 * Molecule object containing collection of atoms, with a name, charge
 * and spin multiplicity.  Implements the iterator interface to allow simple and 
 * efficient iteration through the atoms.  Use MolBuilder interface to
 * make new Molecules.
 * 
 * @author shyue
 * @version 2.0
 * 
 */
public final class Molecule implements Iterable<Atom> {

    private String name;
    private int charge;
    private int spinMult;
    private final List<Atom> sites;

    /**
     *
     * @param name_in
     * @param charge_in
     * @param spin_mul_in
     * @param sites_in
     */
    public Molecule(String name_in, int charge_in, int spin_mul_in, List<Atom> sites_in) {
        name = name_in;
        charge = charge_in;
        spinMult = spin_mul_in;
        sites = new ArrayList<Atom>(sites_in.size());
        for (Atom at : sites_in) {
            sites.add(at);
        }
    }

    /**
     * Default string representation of Molecule in cartesian coorindates.
     * @return Default string representation of Molecule in cartesian coordinates.
     */
    @Override
    public String toString() {
        return getName();
//        String tmpstr = "";
//        for (Atom at : sites) {
//            tmpstr += at.toString() + "\n";
//        }
//        return tmpstr;
    }

    /**
     * Returns Atom at position given by index.
     * @param index Atom index.
     * @return Atom at indexed position in Molecule.
     */
    public Atom get(int index) {
        return sites.get(index);
    }

    /**
     * Returns Atom at position given by index.
     * @param index Atom index.
     * @return Point3d Atom coordinates at indexed position in Molecule.
     */
    public Point3d getAtomCoord(int index) {
        return sites.get(index).getCoord();
    }

    /**
     * Returns Atom at position given by index.
     * @param index Atom index.
     * @return Atom at indexed position in Molecule.
     */
    public Element getAtomSpecies(int index) {
        return sites.get(index).getSpecies();
    }

    /**
     * Returns index of atom at.
     * @param at Atom.
     * @return index of at.
     */
    public int indexOf(Atom at) {
        return sites.indexOf(at);
    }

    /**
     * @return Center of mass of Molecule.
     */
    public Point3d getCenterOfMass() {
        double x = 0;
        double y = 0;
        double z = 0;
        double molWt = 0;
        for (Atom at : sites) {
            double atWt = at.getAtWt();
            x += at.getCoord().x * atWt;
            y += at.getCoord().y * atWt;
            z += at.getCoord().z * atWt;
            molWt += atWt;
        }
        return new Point3d(x / molWt, y / molWt, z / molWt);

    }


    /**
     * Returns a copy of the molecule centered at the center of mass
     * @return
     */
    public Molecule getCenteredCopy() {
        Point3d cg = getCenterOfMass();
        return getCenteredCopy(cg);
    }

    /**
     * Returns a copy of the molecule centered at a new coordinate
     * @param newCenter
     * @return
     */
    public Molecule getCenteredCopy(Point3d newCenter) {

        List<Atom> newSites = new ArrayList<Atom>(sites.size());
        for (Atom at : sites) {
            Point3d newCoord = new Point3d(at.getCoord());
            newCoord.sub(newCenter);
            newSites.add(new Atom(at.getSpecies(), newCoord));
        }
        return new Molecule(name, charge, spinMult, newSites);
    }

    /**
     * @return Charge of Molecule.
     */
    public int getCharge() {
        return charge;
    }

    /**
     *
     * @return Chemical formula of molecule.
     */
    public String getChemFormula() {
        return StringConvUtils.chemicalFormulaFromAtomList(sites);
    }

    /**
     * Returns molecular weight of Molecule.
     * @return Molecular weight
     */
    public double getMolWt() {
        double molwt = 0;

        for (Atom at : sites) {
            molwt += at.getAtWt();
        }


        return molwt;
    }

    /**
     * Returns name of Molecule.  Gives original name specified in delcaration
     * but if none was given, the chemical formula will be returned.
     * @return Name of Molecule
     */
    public String getName() {
        if (!name.matches("")) {
            return name;
        } else {
            return getChemFormula();
        }
    }

    /**
     * @return Spin multiplicity of Molecule.
     */
    public int getSpinMult() {
        // Check if charge and spin multiplicity is plausible
        if ((spinMult - getTotalElectrons()) % 2 == 0) {
            return spinMult;
        } else {
            return getTotalElectrons() % 2 + 1;
        }
    }

    /**
     * @return Total number of electrons.
     */
    public int getTotalElectrons() {
        int electrons = 0;
        for (Atom at : sites) {
            electrons += at.getAtNo();
        }
        return electrons - charge;
    }

    /**
     * Returns number of atoms in Molecule.
     * @return Number of atoms in Molecule.
     */
    public int size() {
        return sites.size();
    }

    /**
     * Returns the distance between atoms atom1 and atom2.
     * @param i Index of first Atom.
     * @param j Index of second Atom.
     * @return Distance in Angstroms.
     */
    public double getDist(Atom i, Atom j) {
        return i.getCoord().distance(j.getCoord());
    }

    /**
     * Returns the distance between atoms indexed by i and j.
     * @param i Index of first Atom.
     * @param j Index of second Atom.
     * @return Distance in Angstroms.
     */
    public double getDist(int i, int j) {
        return getDist(sites.get(i), sites.get(j));
    }

    /**
     * Returns iterator to go through all atoms in the molecule.
     * @return Iterator
     */
    @Override
    public Iterator<Atom> iterator() {

        return sites.iterator();
    }

    /**
     * Provides a copied list of all sites.
     * @return
     */
    public List<Atom> getSites() {
        List<Atom> copiedList = new ArrayList<Atom>(sites.size());
        for (Atom at : sites) {
            copiedList.add(at);
        }
        return copiedList;
    }

    /**
     * Returns a sorted molecule.
     * @return 
     */
    public Molecule sortedCopy() {
        List<Atom> copiedSites = new ArrayList<Atom>(sites.size());
        for (Atom at : sites) {
            copiedSites.add(at);
        }

        Collections.sort(copiedSites);
        return new Molecule(name, charge, spinMult, copiedSites);
    }

    /**
     * Returns angle specified by three atom in molecule.
     * @param i First point.
     * @param j Second point.
     * @param k Third point
     * @return Angle in degrees.
     */
    public double angle(int i, int j, int k) {
        return MolVecMath.angle(sites.get(i).getCoord(),
                sites.get(j).getCoord(),
                sites.get(k).getCoord());
    }

    /**
     * Returns dihedarl angle specified by 4 atoms
     * @param i First atom index
     * @param j Second atom index
     * @param k Third atom index
     * @param l Fourth atom index
     * @return Dihedral angle in degrees.
     */
    public double dihedral(int i, int j, int k, int l) {
        return MolVecMath.dihedral(sites.get(i),
                sites.get(j),
                sites.get(k),
                sites.get(l));
    }

    /**
     * Sets charge of molecule
     * @param charge
     */
    public void setCharge(int charge) {
        this.charge = charge;
    }

    /**
     * Sets name of molecule
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets spin multiplicity of molecule
     * @param spinMult
     */
    public void setSpinMult(int spinMult) {
        this.spinMult = spinMult;
    }
}
