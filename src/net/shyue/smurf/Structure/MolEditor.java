package net.shyue.smurf.Structure;

import net.shyue.smurf.Analyzers.NetworkRepresentation;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import net.shyue.smurf.Analyzers.SymmetryOperation;
import net.shyue.smurf.Structure.Templates.SUBSTITUENT;

/**
 * 
 * @author shyue
 */
public class MolEditor extends DefaultMolBuilder{

    private final Molecule startingMol;
    public enum SUBSTITUTEE {H, F, Cl};


    /**
     * Create a new Molecule editor from Molecule.  Molecule is copied so that
     * original Molecule is NOT modified
     * @param mol_in Input Molecule
     */
    public MolEditor(Molecule mol_in) {
        super(mol_in);
        startingMol = mol_in;
        
    }

    /**
     * Replaces Atom at atomToSub with a functional group.
     * @param atomToSub
     * @param substituent 
     */
    protected void addComplexGroup(Atom atomToSub, SUBSTITUENT substituent) {
        Atom[] NNList = findNNNonTerminal(atomToSub);
        Atom NN = NNList[0];
        NNList = findNN(NN);
        Atom NN2 = (NNList[0] == atomToSub) ? NNList[1] : NNList[0];

        int at_index = sites.indexOf(atomToSub);
        int NN_index = sites.indexOf(NN);
        int NN2_index = sites.indexOf(NN2);

        double bondLength, angle, dih;
        int bondOrder;
        Point3d newCoord;

        String[] lines = substituent.getTemplate().split("\n");
        for (String line : lines)
        {
                String[] tokens = line.trim().split("[ ]+");
                String species;
                switch (tokens.length) {
                    case 0: // Ignore empty lines.
                        break;
                    case 3: // Process 1st line of template
                        species = tokens[0];
                        bondOrder = Integer.parseInt(tokens[2]);
                        bondLength = BondLengthsData.getBondLength(Element.valueOf(species), NN.getSpecies(), bondOrder);
                        newCoord = MolVecMath.scale(atomToSub.getCoord(), NN.getCoord(), bondLength);
                        Atom newAtom = new Atom(species, newCoord);
                        sites.set(at_index, newAtom);
                        break;
                    default: // Process all other lines
                        species = tokens[0];
                        bondOrder = Integer.parseInt(tokens[2]);
                        int[] index = new int[3];
                        for (int i = 0; i < 3; i++) {
                            String indexStr = tokens[2 * i + 1];
                            if (indexStr.matches("SUB")) {
                                index[i] = at_index;
                            } else if (indexStr.matches("NN")) {
                                index[i] = NN_index;
                            } else if (indexStr.matches("NN2")) {
                                index[i] = NN2_index;
                            } else if (indexStr.matches("-[0-9]")) {
                                index[i] = sites.size() + Integer.parseInt(indexStr);
                            } else {
                                throw new IllegalArgumentException("Can't parse template!");
                            }
                        }
                        Element el = Element.valueOf(species);
                        bondLength = BondLengthsData.getBondLength(el, sites.get(index[0]).getSpecies(), bondOrder);
                        angle = Double.parseDouble(tokens[4]);
                        dih = Double.parseDouble(tokens[6]);
                        newCoord = ZMat2Cart(sites, index[0], bondLength,
                                index[1], angle, index[2], dih);
                        addAtom(el, newCoord);

                }
            }


    }
    
    private boolean isSupportedSubstitutee(Element substitutee)
    {
        boolean isSupported = false;
        for (SUBSTITUTEE sub : SUBSTITUTEE.values())
        {
            if (substitutee.toString().equals(sub.toString()))
            {
                isSupported = true;
                break;
            }
        }
        return isSupported;
    }
    
    /**
     * Replaces atom at atomToSub with substituent.
     * @param atomToSubIndex
     * @param substituent
     */
    public void substitute(int atomToSubIndex, SUBSTITUENT substituent) {
        Atom atomToSub = sites.get(atomToSubIndex);
        if (!isSupportedSubstitutee(atomToSub.getSpecies())) {
            throw new java.lang.IllegalArgumentException("Atom at indexed site is not a supported substitutee!");
        }

        if (substituent.isComplex()) {
            addComplexGroup(atomToSub, substituent);
        } else {
            Atom[] NNList = findNNNonTerminal(atomToSub);
            Atom NN = NNList[0];
            Atom tmpsite = atomToSub;
            double bondlength = BondLengthsData.getBondLength(Element.valueOf(substituent.toString()), NN.getSpecies(), 1); // set to experimental C-C bond length
            Point3d newcoord = MolVecMath.scale(tmpsite.getCoord(), NN.getCoord(), bondlength);
            Atom newatom = new Atom(substituent.toString(), newcoord);
            sites.set(atomToSubIndex, newatom);
        }
        name = name + "_" + substituent.getChemForm();

    }

    /**
     * Returns atomToSub of 3 nearest neighbour atoms, the Atom itself excluded.
     * @returns Integer array containing nearest neighbours in order of increasing distance.
     */
    private Atom[] findNN(Atom at) {
        Atom[] NN_index = new Atom[3];
        double dist;
        double Rmin1 = 2e9, Rmin2 = 2e9, Rmin3 = 2e9;
        for (Atom i : sites) {
            if (i != at) {
                dist = at.getCoord().distance(i.getCoord());
                if (dist < Rmin1) {
                    Rmin3 = Rmin2;
                    Rmin2 = Rmin1;
                    Rmin1 = dist;
                    NN_index[2] = NN_index[1];
                    NN_index[1] = NN_index[0];
                    NN_index[0] = i;
                } else if (dist < Rmin2) {
                    Rmin3 = Rmin2;
                    Rmin2 = dist;
                    NN_index[2] = NN_index[1];
                    NN_index[1] = i;
                } else if (dist < Rmin3) {
                    Rmin3 = dist;
                    NN_index[2] = i;
                }
            }
        }
        return NN_index;
    }

    /**
     * Returns atomToSub of nearest neighbour heavy Atom (non hydrogen).
     */
    private Atom[] findNNNonTerminal(Atom at) {
        Atom[] NN_index = new Atom[3];
        double dist;
        double Rmin1 = 2e9, Rmin2 = 2e9, Rmin3 = 2e9;
        for (int i = 0; i < sites.size(); i++) {
            if (!((sites.get(i).getSpecies()==Element.H)||(sites.get(i).getSpecies() == Element.F))) {
                dist = at.getCoord().distance(sites.get(i).getCoord());
                if (sites.get(i) != at) {
                    if (dist < Rmin1) {
                        Rmin3 = Rmin2;
                        Rmin2 = Rmin1;
                        Rmin1 = dist;
                        NN_index[2] = NN_index[1];
                        NN_index[1] = NN_index[0];
                        NN_index[0] = sites.get(i);
                    } else if (dist < Rmin2) {
                        Rmin3 = Rmin2;
                        Rmin2 = dist;
                        NN_index[2] = NN_index[1];
                        NN_index[1] = sites.get(i);
                    } else if (dist < Rmin3) {
                        Rmin3 = dist;
                        NN_index[2] = sites.get(i);
                    }
                }
            }
        }
        return NN_index;
    }

    /**
     * Rotates the molecules about the bond connecting atoms atom1 and atom2 by angle.
     * @param atom1 Index of first Atom.
     * @param atom2 Index of second Atom.
     * @param angle Angle to rotate by in integral number of degrees.
     */
    public void rotate(Atom atom1, Atom atom2, double angle) {
        double threshold = 1.1 * BondLengthsData.getBondLength(atom1.getSpecies(),
                atom2.getSpecies(), 1);
        if (atom1.getCoord().distance(atom2.getCoord()) > threshold) {
            System.out.println("Atoms are too far apart.  Are you sure they are" +
                    "bonded atoms?");
        } else {
            NetworkRepresentation networkRep = new NetworkRepresentation(startingMol);
            List<Atom> connectedatoms = networkRep.getNetwork(atom1, atom2);
            Vector3d axis = new Vector3d();
            axis.sub(atom2.getCoord(), atom1.getCoord());
            SymmetryOperation RotM = SymmetryOperation.Rotation(atom1.getCoord(), axis, angle);
            for (Atom at : sites) {
                if (connectedatoms.contains(at)) {
                    sites.set(sites.indexOf(at), new Atom(at.getSpecies(), RotM.transformPoint(at.getCoord())));
                }
            }
        }
        name = name + "_rot" + Double.toString(angle);
    }

    /**
     * Undo all changes made and go back to starting molecule
     */
    public void undoAllChanges() {
        name = startingMol.getName();
        sites = new ArrayList<Atom>(startingMol.size());
        for (Atom at : startingMol) {
            sites.add(at);
        }
    }

}
