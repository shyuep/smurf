package net.shyue.smurf.Structure;

import javax.vecmath.Point3d;

/**
 * An immutable Atom object containing Atom species and representation in z-matrix and 
 * cartesian Point3d.  Implements comparable for sorting by atomic symbol.
 * 
 * @author shyue
 * @version 1.0
 */
public final class Atom implements Comparable<Atom> {

    private final Element species;
    private final Point3d coord;


    public Atom(Element _species, double x, double y, double z) {
        this(_species,new Point3d(x, y, z));
    }

    public Atom(Element _species, Point3d _coord) {
        species = _species;
        coord = _coord;
    }


    /**
     * Creates a new Atom with cartesian coordinate inputs
     * @param species_in Atomic species symbol
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
    public Atom(String species_in, double x, double y, double z) {
        this(species_in,new Point3d(x, y, z));
    }

    /**
     * Creates a new Atom with cartesian coordinate input as a matrix.
     * @param species_in Atomic species symbol
     * @param coord_in Coordinates of Atom
     */
    public Atom(String species_in, Point3d coord_in) {
        String speciesstr = species_in.substring(0, 1).toUpperCase() + species_in.substring(1, species_in.length()).toLowerCase();
        species = Element.valueOf(speciesstr);
        coord = new Point3d(coord_in);
    }

    /**
     * 
     * @return Atomic number of Atom
     */
    public int getAtNo() {

        return species.getAtNo();
    }

    /**
     * @return Atomic weight of Atom.
     */
    public double getAtWt() {
        return species.getAtWt();
    }
    
        /**
     * Checks if two vectors are equal up to a particular tolerance value.
     * @param o 
     * @return True if vectors are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Atom))
            return false;
        Atom at = (Atom) o;
        return (at.coord.equals(coord) && (at.species == species));
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.species != null ? this.species.hashCode() : 0);
        hash = 97 * hash + (this.coord != null ? this.coord.hashCode() : 0);
        return hash;
    }

    /**
     * Returns coordinates of the Atom, with defensive copying.
     * @return Cartesian coordinate of Atom.
     */
    public Point3d getCoord() {
        return new Point3d(coord);
    }
    
    /**
     * @return Atomic symbol of Atom.
     */
    public Element getSpecies() {
        return species;
    }

    /**
     * @return String representation of atom.
     */
    @Override
    public String toString() {
        return String.format("%s %.4f %.4f %.4f", species, coord.x, coord.y, coord.z);
    }

    @Override
    public int compareTo(Atom otherAtom) {
    if ( this == otherAtom ) 
        return 0;
    else 
        return this.species.compareTo(otherAtom.species);
    
    }
}
