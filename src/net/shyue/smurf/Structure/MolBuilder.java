package net.shyue.smurf.Structure;

import javax.vecmath.Point3d;

/**
 * Interface class for Molecule builders.
 * @author Shyue Ping
 */
public interface MolBuilder {

        /**
     * Set name of Molecule
     * @param name_in
     */
    void setName(String name_in);

    /**
     * Set charge of Molecule
     * @param charge_in
     */
    void setCharge(int charge_in);

    /**
     * Set spin multiplicity of Molecule
     * @param spinMult_in
     */
    void setSpinMult(int spinMult_in);

    void addAtom(Element species, double x, double y, double z);
    void addAtom(Element species, Point3d coord);
    public void addAtom(Element species, int NN1_index, double bondlength_in,
            int NN2_index, double angle_in, int NN3_index, double dih_in);
    void setAtom(int i, Atom at);
    void setAtom(Atom toBeReplaced, Atom replacementAtom);


    /**
     * Builds and returns created Molecule with appropriate error checking to 
     * ensure that atoms are not too close and spin multiplicity is correct.
     * @return Created Molecule
     * @throws BuilderException 
     */
    public Molecule build() throws BuilderException;
    
    /**
     * Exception class for fatal errors in building the Molecule
     */
    public class BuilderException extends Exception {

        /**
         *
         */
        public BuilderException() {
            super("Error building molecule!");
        }

        /**
         *
         * @param msg
         */
        public BuilderException(String msg) {
            super(msg);
        }
    }
}
