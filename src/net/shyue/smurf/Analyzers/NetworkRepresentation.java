package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.BondLengthsData;
import net.shyue.smurf.Structure.Molecule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;

/**
 *
 * @author shyue
 */
public class NetworkRepresentation {

    private Map<Atom, List<Atom>> adjacencyList;
    private Molecule mol;
    private double distThresholdPer = 1.1;

    public NetworkRepresentation(Molecule _mol) {
        mol = _mol;
        adjacencyList = computeAdjacencyList();

    }

    public NetworkRepresentation(Molecule _mol, double _distThresholdPer) {
        mol = _mol;
        distThresholdPer = _distThresholdPer;
        adjacencyList = computeAdjacencyList();

    }

    private Map<Atom, List<Atom>> computeAdjacencyList() {
        Map<Atom, List<Atom>> adjList = new HashMap<Atom, List<Atom>>(mol.size());
        for (Atom at1 : mol) {
            List<Atom> adjAtoms = new ArrayList<Atom>();
            for (Atom at2 : mol) {
                if (at1 != at2) {
                    double bondlength = 2.5;

                    try {
                        bondlength = BondLengthsData.getBondLength(at1.getSpecies(), at2.getSpecies(), 1);
                    } catch (Exception ex) {
//                        System.out.println(ex.getMessage());
//                        System.out.println(at1.getSpecies().toString() + at2.getSpecies().toString() + 1);
//                        System.out.println("Using generic bond length of 2.5A");
                        bondlength = 2.5;
                    }
                    // Only single bond lengths are used, since they are usually the longest.
                    // Since no connectivity information is assumed, the program does not support folded molecules.
                    double comparisonDist = distThresholdPer * bondlength;
                    if (mol.getDist(at1, at2) < comparisonDist) {
                        adjAtoms.add(at2);
                    }
                }
            }
            adjList.put(at1, adjAtoms);
        }
        return adjList;
    }

    public List<Atom> findRing() {
        if (adjacencyList.size() == 0) {
            return new ArrayList<Atom>();
        }
        Atom[] at = adjacencyList.keySet().toArray(new Atom[adjacencyList.size()]);
        Stack<Atom> path = new Stack<Atom>();
        path.add(at[0]);
        List<Stack<Atom>> paths = new ArrayList<Stack<Atom>>();
        paths.add(path);
        while (paths.size() > 0) {
            List<Stack<Atom>> newpaths = new ArrayList<Stack<Atom>>();
            for (Stack<Atom> temppath : paths) {
                List<Atom> adjAtoms = getAdjacentAtoms(temppath.peek());
                for (Atom at1 : adjAtoms) {
                    if (!temppath.contains(at1)) // Ignores the previous atom and if loop occurs, this breaks it.
                    {
                        @SuppressWarnings("unchecked") //Stack is created internally.  The conversion below is always valid.
                        Stack<Atom> newpath = (Stack<Atom>) temppath.clone();
                        newpath.push(at1);
                        newpaths.add(newpath);
                    } else if (temppath.indexOf(at1) != temppath.size() - 2) {
                        return temppath.subList(temppath.indexOf(at1), temppath.size());
                    }

                }
            }
            paths = newpaths;
        }

        return new Stack<Atom>();
    }

    public List<Atom> getNetwork(Atom anchorAtom, Atom axialAtom) {

        return getTree(anchorAtom, axialAtom, new ArrayList<Atom>());
    }

    private List<Atom> getTree(Atom anchorAtom, Atom axialAtom, List<Atom> history) {
        history.add(anchorAtom);
        history.add(axialAtom);
        List<Atom> network = new ArrayList<Atom>();
        network.add(axialAtom);
        List<Atom> connectedatoms = getAdjacentAtoms(axialAtom);
        for (Atom at : connectedatoms) {
            if (!history.contains(at)) {
                network.addAll(getTree(axialAtom, at, history));
            }
        }
        return network;
    }

    /**
     * Returns the adjacency list of the provided atom
     * @param at
     * @return List of adjacent atoms
     */
    public List<Atom> getAdjacentAtoms(Atom at) {
        return adjacencyList.get(at);
    }

    @Override
    public String toString() {
        StringBuilder sBuilder = new StringBuilder();
        for (Atom at : mol) {
            sBuilder.append(at.getSpecies().toString() + mol.indexOf(at));
            sBuilder.append("\n");
            for (Atom adjAt : getAdjacentAtoms(at)) {
                sBuilder.append("\t" + adjAt.getSpecies().toString() + mol.indexOf(adjAt) + "\n");
            }
        }
        return sBuilder.toString();
    }

    private Map<Atom, String> getUniqueLabels() {
        Map<Atom, Integer> labels = new HashMap<Atom, Integer>();
        for (Atom at : adjacencyList.keySet()) {
            labels.put(at, getAdjacentAtoms(at).size());
        }

        while (true) {
            int numUnique = numUniqueLabels(labels);
            Map<Atom, Integer> newLabels = new HashMap<Atom, Integer>();
            for (Atom at : labels.keySet()) {
                int newNum = 0;
                for (Atom adjAt : getAdjacentAtoms(at)) {
                    newNum += labels.get(adjAt);
                }
                newLabels.put(at, newNum);
            }
            if (numUniqueLabels(newLabels) > numUnique) {
                labels = newLabels;
            } else {
                break;
            }
        }
        labels=reassignLabels(labels);
        Map<Atom, String> strLabels = new HashMap<Atom, String>();
        for (Atom at : labels.keySet()) {
            String label = at.getSpecies() + Integer.toString(labels.get(at));
            strLabels.put(at, label);
        }
        return strLabels;

    }

    private Map<Atom, Integer> reassignLabels(Map<Atom, Integer> originalLabels) {
        TreeSet<Integer> values = new TreeSet<Integer>(originalLabels.values());
        Map<Atom, Integer> newLabels = new HashMap<Atom, Integer>();
        int counter = 1;
        int currPos = 0;
        System.out.println(originalLabels.size());
        while (counter <= originalLabels.size()) {
            
            if (currPos == 0) {
                List<Atom> startAtoms = new ArrayList<Atom>();
                for (Atom at : originalLabels.keySet()) {
                    if (originalLabels.get(at) == values.last()) {
                        startAtoms.add(at);
                        
                    }
                }
                Collections.sort(startAtoms);
                for (Atom at : startAtoms)
                {
                    newLabels.put(at, counter);
                    counter++;
                }
                currPos++;
            } else {

                Atom at = reverseLookup(newLabels,currPos);
                System.out.println(at.getSpecies()+Integer.toString(originalLabels.get(at)));
                System.out.println(at.getSpecies()+Integer.toString(newLabels.get(at)));
                System.out.println(newLabels.size());
                currPos++;
                Map<Atom,Integer> adjLabels = new HashMap<Atom,Integer>();
                List<Atom> adjAtoms = getAdjacentAtoms(at);
                Collections.sort(adjAtoms);
                for (Atom adjAt : adjAtoms)
                {
                    if (!newLabels.containsKey(adjAt))
                    {
                        adjLabels.put(adjAt, originalLabels.get(adjAt));
                    }
                }
                TreeSet<Integer> adjValues = new TreeSet<Integer>(adjLabels.values());
                while (!adjValues.isEmpty()){
                    for (Atom at3 : adjLabels.keySet()) {
                        if (originalLabels.get(at3) == adjValues.last()) {
                            newLabels.put(at3, counter);
                            counter++;
                            
                        }
                    }
                    adjValues.remove(adjValues.last());
                }
            }
        }

        return newLabels;
    }

    private Atom reverseLookup(Map<Atom,Integer> map, int value)
    {
        for (Atom at : map.keySet())
        {
            if (map.get(at)==value)
            {
                    return at;
            }
        }
        throw new IllegalArgumentException(value+" not found in map!");
    }

    private int numUniqueLabels(Map<Atom, Integer> labels) {
        List<Integer> currentLabels = new ArrayList<Integer>();
        for (Atom at : labels.keySet()) {
            int label = labels.get(at);
            if (!currentLabels.contains(label)) {
                currentLabels.add(label);
            }
        }
        return currentLabels.size();
    }

//    public String getUniqueRep() {
//
//        Map<Atom, String> uniqueLabels = getUniqueLabels();
//
//        List<String> allLabels = new ArrayList<String>();
//
//        for (Atom at : mol) {
//            String label = uniqueLabels.get(at) + "-";
//            List<String> adjAtomStr = new ArrayList<String>();
//            for (Atom adjAt : getAdjacentAtoms(at)) {
//                adjAtomStr.add(uniqueLabels.get(adjAt));
//            }
//            Collections.sort(adjAtomStr);
//            for (String str : adjAtomStr) {
//                label += str;
//            }
//            allLabels.add(label);
//        }
//        Collections.sort(allLabels);
//        StringBuilder sBuilder = new StringBuilder();
//        for (String str : allLabels) {
//            sBuilder.append(str + "\n");
//        }
//        return sBuilder.toString();
//    }
}
