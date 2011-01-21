package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.*;
import net.shyue.smurf.Structure.MolBuilder.BuilderException;
import net.shyue.smurf.Utils.BondSimilarityComparator;
import net.shyue.smurf.Utils.CollectionBinner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3d;

/**
 *
 * @author Shyue
 */
public class BondLengthsAnalyzer {

    private Molecule mol;
    private double networkThreshold;

    /**
     * Create a new Molecule editor from Molecule.  Molecule is copied so that
     * original Molecule is NOT modified
     * @param mol_in Input Molecule
     */
    public BondLengthsAnalyzer(Molecule mol_in, double _networkThreshold) {
        
        mol = mol_in;
        networkThreshold = _networkThreshold;

    }

    public Molecule correctBondLengths(double correctionThreshold) throws BuilderException {

        NetworkRepresentation networkRep = new NetworkRepresentation(mol, networkThreshold);
        MolBuilder builder = new DefaultMolBuilder(mol);
        for (int i = 0, n = mol.size(); i < n; i++) {
            if (mol.get(i).getSpecies() == Element.H) {
                List<Atom> NNs = networkRep.getAdjacentAtoms(mol.get(i));
                if (NNs.size() != 1) {
                    throw new IllegalStateException("H"+i+" should only have 1 nearest" +
                            " neighbour, not " + NNs.size() + "!");
                } else {
                    Atom Hatom = mol.get(i);
                    Atom NN = NNs.get(0);
                    double expectedBL = BondLengthsData.getBondLength(Hatom.getSpecies(), NN.getSpecies(), 1);
                    double currentBL = NN.getCoord().distance(Hatom.getCoord());
                    
                    if (Math.abs(currentBL/expectedBL - 1) > correctionThreshold) {
                        Point3d newCoord = MolVecMath.scale(Hatom.getCoord(), NN.getCoord(), expectedBL);
                        builder.setAtom(i, new Atom(Hatom.getSpecies(), newCoord));
                    }

                }
            }
        }
        mol = builder.build();
        return mol;
    }

    public Set<Bond> getBonds(){
        NetworkRepresentation networkRep = new NetworkRepresentation(mol);
        Set<Bond> bonds = new HashSet<Bond>();
        for (Atom at : mol)
        {
            List<Atom> adjAtoms = networkRep.getAdjacentAtoms(at);
            for (Atom adjAtom : adjAtoms)
            {
                bonds.add(new Bond(at,adjAtom));
            }
        }

        return bonds;
    }


    public String getBondLengthsSummary() {
        Set<Bond> bonds = getBonds();
        List<Bond> bondList = new ArrayList<Bond> (bonds);
        Map<String, List<Bond>> binnedBonds = CollectionBinner.group(bondList, new BondSimilarityComparator());
        StringBuilder sbuilder = new StringBuilder();
        for (String bondType : binnedBonds.keySet()) {
            
            double avgBL = 0;
            double longestBL = 0;
            double shortestBL = 1e9;
            int num = binnedBonds.get(bondType).size();
            sbuilder.append(num+ " "+bondType+" bonds\n");
            for (Bond b : binnedBonds.get(bondType))
            {
                avgBL += b.getLength()/num;
                longestBL = (b.getLength() > longestBL) ? b.getLength() : longestBL;
                shortestBL = (b.getLength() < shortestBL) ? b.getLength() : shortestBL;
            }
            sbuilder.append(String.format("Avg BL = %.4f A\nshortest BL=%.4f A\nlongest BL=%.4f A\n\n", avgBL, shortestBL, longestBL));

        }
        return sbuilder.toString();
    }


}
