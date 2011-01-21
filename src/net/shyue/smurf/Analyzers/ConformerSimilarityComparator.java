package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.*;
import net.shyue.smurf.Utils.CollectionBinner;
import net.shyue.smurf.Utils.SimilarityComparator;
import net.shyue.smurf.Utils.SpeciesSimilarityComparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author shyue
 */
public class ConformerSimilarityComparator implements SimilarityComparator<NetworkRepresentation, Molecule> {

    private static List<Atom> getSmallestElementSet(Molecule molecule) {
        Map<Element, List<Atom>> groupedAtoms = CollectionBinner.group(molecule.getSites(), new SpeciesSimilarityComparator());
        int minAtoms = 1000000;
        Element minKey = null;
        for (Element key : groupedAtoms.keySet()) {
            if (groupedAtoms.get(key).size() < minAtoms) {
                minKey = key;
                minAtoms = groupedAtoms.get(key).size();
            }
        }
        return groupedAtoms.get(minKey);
    }

    /**
     * Tests if comparisonMol is a conformer of the current molecule.
     * @param mol1
     * @param mol2
     * @return True if currentmolecule is a conformer of the comparison molecule
     */
    private static boolean isMatch(Stack<Atom> pathHistory1, Stack<Atom> pathHistory2, NetworkRepresentation mol1Rep, NetworkRepresentation mol2Rep) {
//        System.out.println("Stage");
//        System.out.println(atom1);
//        System.out.println(atom2);

        if (!pathHistory1.peek().getSpecies().equals(pathHistory2.peek().getSpecies())) {
            // Atoms are obviously not equlivalent if they are of different species 
            return false;
        } else {
            List<Atom> adjAtoms1 = mol1Rep.getAdjacentAtoms(pathHistory1.peek());
            List<Atom> adjAtoms2 = mol2Rep.getAdjacentAtoms(pathHistory2.peek());
            if (adjAtoms1.size() != adjAtoms2.size()) {
                // Atoms are obviously not equlivalent if the number of adjacent atoms do not match
                return false;
            } else //By this stage, it is clear that the atom species and number of adjacent atoms match.
            {
                if (adjAtoms1.size() == 1 && pathHistory1.size() > 1 && pathHistory2.size() > 1) {
                    // If atom is a terminal atom (only adjacent atom is the one it links from), then it is a definite match
                    return true;
                } else {
                    // Store already matched atoms to prevent them from being matched again.
                    List<Atom> removedList = new ArrayList<Atom>();
                    for (Atom at1 : adjAtoms1) {
                        if (!pathHistory1.contains(at1)) // Ignores the previous atom and if loop occurs, this breaks it.
                        {
                            boolean matchfound = false;
                            // Test atom against all atoms in the comparison molecule for a match.
                            for (Atom at2 : adjAtoms2) {
                                if ((!pathHistory2.contains(at2)) && (!removedList.contains(at2))) {
                                    //System.out.println(" here");
                                    Stack<Atom> newPathHistory1 = (Stack<Atom>) pathHistory1.clone();
                                    Stack<Atom> newPathHistory2 = (Stack<Atom>) pathHistory2.clone();

                                    newPathHistory1.push(at1);
                                    newPathHistory2.push(at2);

                                    if (isMatch(newPathHistory1, newPathHistory2, mol1Rep, mol2Rep)) {
                                        matchfound = true;
                                        removedList.add(at2);
                                        break;
                                    }
                                }
                            }
                            // If match cannot be found for ANY atom, then the two structures are NOT conformers.
                            if (!matchfound) {
                                return false;
                            }
                        } else if (pathHistory1.indexOf(at1) != pathHistory1.size() - 2) {
                            int ringPos = pathHistory1.indexOf(at1);
                            boolean matchfound = false;
                            for (Atom at2 : adjAtoms2) {
                                if (pathHistory2.indexOf(at2) == ringPos && (!removedList.contains(at2))) {
                                    matchfound = true;
//                                    System.out.println("Ring detected");
//                                    for (Atom attemp : pathHistory1)
//                                    {
//                                        System.out.print(attemp.getSpecies()+pathHistory1.indexOf(attemp)+"-");
//                                    }
//                                    System.out.println("-"+at1.getSpecies()+pathHistory1.indexOf(at1));
                                    removedList.add(at2);
                                    break;
                                }
                            }
                            if (!matchfound) {
                                return false;
                            }

                        }

                    }
                    // if all previous tests pass, then structures are conformers.
                    return true;
                }
            }
        }

    }

    @Override
    public boolean areSimilar(Molecule mol1, Molecule mol2) {
        if (!mol1.getChemFormula().equals(mol2.getChemFormula())) {
            return false;
        } else {
            NetworkRepresentation mol1Rep = new NetworkRepresentation(mol1);
            NetworkRepresentation mol2Rep = new NetworkRepresentation(mol2);
            List<Atom> minAtomList = getSmallestElementSet(mol1);

            Atom testAtom = minAtomList.get(0);
            boolean isConformer = false;
            for (Atom at : getSmallestElementSet(mol2)) {
                Stack<Atom> pathHistory1 = new Stack<Atom>();
                pathHistory1.push(testAtom);
                Stack<Atom> pathHistory2 = new Stack<Atom>();
                pathHistory2.push(at);
                if (isMatch(pathHistory1, pathHistory2, mol1Rep, mol2Rep)) {
                    isConformer = true;
                    break;
                }

            }
            return isConformer;

        }
    }

    @Override
    public NetworkRepresentation getIdentifier(Molecule mol1) {
        NetworkRepresentation netRep = new NetworkRepresentation(mol1);
        return netRep;
    }
}
