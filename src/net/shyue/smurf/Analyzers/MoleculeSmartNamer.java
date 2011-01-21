package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Element;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Utils.CollectionBinner;
import net.shyue.smurf.Utils.SimpleCSVParser;
import net.shyue.smurf.Utils.SpeciesSimilarityComparator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shyue
 */
public class MoleculeSmartNamer {

    private Molecule mol;
    private NetworkRepresentation molRep;
    private Map<Element, Integer> cationBondOrder;
    private Map<Element, Integer> anionBondOrder;
    private Map<String, String> ringBaseMap;
    private Map<Element, String> nonRingBaseMap;
    private String basename;
    private String name;
    private List<Atom> ringAtoms;
    private List<Atom> anchors;

    public MoleculeSmartNamer(Molecule _mol) {
        mol = _mol;
        molRep = new NetworkRepresentation(_mol);
        ringAtoms = molRep.findRing();
        initData();
        if (ringAtoms.size() > 0) {
            processRingStructure();
        } else {
            processNonRingStructure();
        }

    }

    private void initData() {
        cationBondOrder = new HashMap<Element, Integer>();
        anionBondOrder = new HashMap<Element, Integer>();
        ringBaseMap = new HashMap<String, String>();
        nonRingBaseMap = new HashMap<Element, String>();


        try {
            SimpleCSVParser csvParser = new SimpleCSVParser(MoleculeSmartNamer.class.getResourceAsStream("IonValenceElectrons.csv"));
            for (String[] row : csvParser.getData())
            {
                cationBondOrder.put(Element.valueOf(row[0]), Integer.parseInt(row[1]));
                anionBondOrder.put(Element.valueOf(row[0]), Integer.parseInt(row[2]));
            }
            csvParser = new SimpleCSVParser(MoleculeSmartNamer.class.getResourceAsStream("RingBaseNameMappings.csv"));
            for (String[] row : csvParser.getData())
            {
                ringBaseMap.put(row[0], row[1]);
            }
            csvParser = new SimpleCSVParser(MoleculeSmartNamer.class.getResourceAsStream("NonRingBaseNameMappings.csv"));
            for (String[] row : csvParser.getData())
            {
                nonRingBaseMap.put(Element.valueOf(row[0]), row[1]);
            }
            
        } catch (IOException ex) {
            System.out.println("Fatal error in MoleculeSmartNamer : " + ex.getMessage());
            System.exit(-1);
        }

    }

    private void processRingStructure() {
        determineRingBase();
        determineAnchors();
        int numAtoms = ringAtoms.size();

        Map<Atom, List<FunctionalGroup>> ringLinks = new HashMap<Atom, List<FunctionalGroup>>(ringAtoms.size());
        for (Atom at : ringAtoms) {
            ringLinks.put(at, getRingLink(at));
        }
        //int shift = ringAtoms.indexOf(anchors.get(0));
        name = basename + "-";
        sortRing(ringLinks);
        for (int i = 0; i < numAtoms; i++) {
            Atom at = ringAtoms.get(i);
            String linkStr = "";
            for (FunctionalGroup fGroup : ringLinks.get(at)) {
                linkStr += fGroup.getChemicalFormula() + "-";
            }
            if (!linkStr.equals("")) {
                linkStr = linkStr.substring(0, linkStr.length() - 1);
            }
            name += "(" + linkStr + ")-";
        }
        name = name.substring(0, name.length() - 1);
    }

    private void sortRing(Map<Atom, List<FunctionalGroup>> ringLinks) {
        int numAtoms = ringAtoms.size();
        //System.out.println(atomListToString(ringAtoms));

        if (anchors.size() > 1) //Handles cases with multiple anchors, e.g. Imi.  But works properly only for 2 anchor atoms.
        {
            /**
             * First conditional statement shifts the ringAtoms so that the
             * first atom is an anchor atom with the largest mol wt functional
             * group(s).
             */
            if (getAllFuncGroupWt(ringLinks.get(anchors.get(0))) < getAllFuncGroupWt(ringLinks.get(anchors.get(1)))) {
                //System.out.println("shift1 by "+ringAtoms.indexOf(anchors.get(1)));
                Collections.rotate(ringAtoms, -ringAtoms.indexOf(anchors.get(1)));
            } else {
                //System.out.println("shift2");
                Collections.rotate(ringAtoms, -ringAtoms.indexOf(anchors.get(0)));
            }
//System.out.println(atomListToString(ringAtoms));
            /**
             * Makes sure the iteration order of the list is such that the two
             * anchors have the minimum number of links between them, i.e.
             * changes N-C-C-N-C to N-C-N-C-C
             */
            if (Math.abs(ringAtoms.indexOf(anchors.get(0)) - ringAtoms.indexOf(anchors.get(1))) > numAtoms / 2) {
                Collections.reverse(ringAtoms);
                Collections.rotate(ringAtoms, 1);
//                System.out.println(atomListToString(ringAtoms));
            }
        } else {
            Collections.rotate(ringAtoms, -ringAtoms.indexOf(anchors.get(0)));
            //System.out.println(atomListtoString(ringAtoms));
            int shift = 1;
            while (shift < ringAtoms.size() / 2) {
                if (getAllFuncGroupWt(ringLinks.get(ringAtoms.get(shift))) >
                        getAllFuncGroupWt(ringLinks.get(ringAtoms.get(numAtoms - shift)))) {
                    break;
                } else if (getAllFuncGroupWt(ringLinks.get(ringAtoms.get(shift))) <
                        getAllFuncGroupWt(ringLinks.get(ringAtoms.get(numAtoms - shift)))) {
                    Collections.reverse(ringAtoms);
                    Collections.rotate(ringAtoms, -1);
                    break;
                } else {
                    shift++;
                }
            }
        }
    // System.out.println(atomListToString(ringAtoms));
    }

    private double getAllFuncGroupWt(List<FunctionalGroup> fGroups) {
        double wt = 0;
        Collections.sort(fGroups);
        Collections.reverse(fGroups);
        for (FunctionalGroup fGroup : fGroups) {
            wt += fGroup.getMolWt();
        }
        return wt;
    }

    private void processNonRingStructure() {
        determineAnchors();
        determineNonRingBase(anchors.get(0));
        name = basename + "-";
        List<Atom> adjAtoms = molRep.getAdjacentAtoms(anchors.get(0));
        List<FunctionalGroup> fGroups = new ArrayList<FunctionalGroup>();
        for (Atom at : adjAtoms) {
            fGroups.add(new FunctionalGroup(molRep.getNetwork(anchors.get(0), at)));

        }
        Collections.sort(fGroups);
        Collections.reverse(fGroups);
        for (FunctionalGroup fGroup : fGroups) {
            name += "(" + fGroup.getChemicalFormula() + ")-";
        }
        name = name.substring(0, name.length() - 1);
    }

    private List<FunctionalGroup> getRingLink(Atom ringAtom) {
        List<Atom> adjAtoms = molRep.getAdjacentAtoms(ringAtom);
        List<FunctionalGroup> fGroups = new ArrayList<FunctionalGroup>();
//        System.out.println(ringAtom);
//        System.out.println(adjAtoms.size());
        for (Atom at : adjAtoms) {
            if (!ringAtoms.contains(at)) {
                fGroups.add(new FunctionalGroup(molRep.getNetwork(ringAtom, at)));
            }
        }
        Collections.sort(fGroups);
        Collections.reverse(fGroups);
        return fGroups;
    }

    private void determineAnchors() {
        if (ringAtoms.size() > 0) {
            anchors = smallestSet(ringAtoms);
        } else {
            anchors = smallestSet(mol.getSites());
        }

    }

    private List<Atom> smallestSet(List<Atom> atomSet) {
        Map<Element, List<Atom>> sortedSet = CollectionBinner.group(atomSet, new SpeciesSimilarityComparator());
        
        List<Atom> smallestSet = null;
        int smallestSize = 10000000;
        Element smallestKey = null;
        for (Element key : sortedSet.keySet()) {
            if (cationBondOrder.containsKey(key) || anionBondOrder.containsKey(key)) {
                if (sortedSet.get(key).size() < smallestSize) {
                    smallestSet = sortedSet.get(key);
                    smallestSize = sortedSet.get(key).size();
                    smallestKey = key;
                } else if (sortedSet.get(key).size() == smallestSize) {
                    if (key.getAtNo() < smallestKey.getAtNo()) {
                        smallestSet = sortedSet.get(key);
                        smallestSize = sortedSet.get(key).size();
                        smallestKey = key;
                    }
                }
            }
        }
        return smallestSet;
    }

    private void determineNonRingBase(Atom anchorAtom) {
        boolean isCation = (molRep.getAdjacentAtoms(anchorAtom).size() == cationBondOrder.get(anchorAtom.getSpecies()));
        basename = nonRingBaseMap.get(anchors.get(0).getSpecies());
        String[] tokBases = basename.split("/");
        if (tokBases.length > 1) {
            if (isCation) {
                basename = tokBases[0];
            } else {
                basename = tokBases[1];
            }

        }

    }

    private void determineRingBase() {
        boolean saturated = true;
        for (Atom at : ringAtoms) {
            if (molRep.getAdjacentAtoms(at).size() < cationBondOrder.get(at.getSpecies())) {
                saturated = false;
                break;
            }
        }
        int numAtoms = ringAtoms.size();
        String possibleBases = "";
        for (String key : ringBaseMap.keySet()) {
            String[] atomLinks = key.split("-");
            if (atomLinks.length == numAtoms) {
                boolean match = false;
                for (int shift = 0; shift < numAtoms; shift++) {
                    boolean linkMatch = true;
                    for (int i = 0; i < numAtoms; i++) {
                        if (!ringAtoms.get((i + shift) % numAtoms).getSpecies().toString().equals(atomLinks[i])) {
                            linkMatch = false;
                            break;
                        }
                    }
                    if (linkMatch) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    possibleBases = ringBaseMap.get(key);
                    break;
                }
            }
        }
        basename = possibleBases;
        String[] tokBases = possibleBases.split("/");
        if (tokBases.length > 1) {
            if (saturated) {
                basename = tokBases[0];
            } else {
                basename = tokBases[1];
            }

        }

    }

    public String getName() {
        return name;
    }
}
