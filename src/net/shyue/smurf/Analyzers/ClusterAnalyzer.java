package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.AtomCluster;
import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Utils.CollectionBinner;
import net.shyue.smurf.Utils.StringConvUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.vecmath.Point3d;

/**
 * A class which takes an input structure and chops it into clusters based on
 * connectivity as determined by the network representation.
 * @author Shyue
 */
public class ClusterAnalyzer {

    private final Molecule mol;
    private final NetworkRepresentation netRep;
    private Set<AtomCluster> clusters;

    /**
     * Creates a ClusterAnalyzer
     * @param mol_in Input molecule to be analyzed
     * @param threshold Percentage threshold for determining network
     */
    public ClusterAnalyzer(Molecule mol_in, double threshold) {
        mol = mol_in;
        netRep = new NetworkRepresentation(mol, threshold);
        clusters = new HashSet<AtomCluster>();
        Set<Atom> processedAtoms = new HashSet<Atom>(mol.size());
        for (Atom at : mol) {
            if (!processedAtoms.contains(at)) {
                AtomCluster newCluster = new AtomCluster();
                newCluster.add(at);
                for (Atom adjAt : netRep.getAdjacentAtoms(at)) {
                    newCluster.addAll(netRep.getNetwork(at, adjAt));
                }
                clusters.add(newCluster);
                processedAtoms.addAll(newCluster);

            }
        }

    }

    public Set<AtomCluster> getCenterClusters()
    {
        Set<AtomCluster> nearestClusters = new HashSet<AtomCluster>();
        Map<String,List<AtomCluster>> binnedClusters = CollectionBinner.group(new ArrayList(clusters), new ClusterSimilarityComparator());
        Point3d center = mol.getCenterOfMass();
        //System.out.println("Center at " +center);
        for (String key : binnedClusters.keySet())
        {
            //System.out.println("For formula "+key);
            
            double shortestDist = 1e9;
            AtomCluster nearestCluster = null;
            for (AtomCluster cluster : binnedClusters.get(key))
            {

                if (cluster.getCenter().distance(center) < shortestDist)
                {
                    nearestCluster = cluster;
                    shortestDist = cluster.getCenter().distance(center);
                }
            }
            //System.out.println("Nearest "+key+" at "+nearestCluster.getCenter()+" with dist = "+nearestCluster.getCenter().distance(center));
            nearestClusters.add(nearestCluster);
        }
        return nearestClusters;
    }

    /**
     * Returns nearest X clusters to cluster with input atom
     * @param clusterOfInterest 
     * @param numClusters
     * @param counterIonOnly
     * @return
     */
    public Set<AtomCluster> getNearestXClusters(AtomCluster clusterOfInterest, int numClusters, boolean counterIonOnly) {
        
        Point3d center = clusterOfInterest.getCenter();
        Set<AtomCluster> nearestClusters = new HashSet<AtomCluster>();
        Map<Double, AtomCluster> sortedList = new TreeMap<Double, AtomCluster>();
        String clusterFormula = clusterOfInterest.getFormula();
        for (AtomCluster cluster : clusters) {
            if (cluster != clusterOfInterest) {
                Point3d coord = cluster.getCenter();
                double dist = coord.distance(center);
                sortedList.put(dist, cluster);
            }
        }
        nearestClusters.add(clusterOfInterest);
        int counter = 1;
        int counterIonCounter = 0;
        for (Double key : sortedList.keySet()) {
            if (!sortedList.get(key).getFormula().equals(clusterFormula)) {
                if (counterIonCounter < numClusters) {
                    nearestClusters.add(sortedList.get(key));
                    counterIonCounter++;
                    //System.out.printf("Added %s, dist = %.4f\n", sortedList.get(key).getFormula(), key);
                }
            } else if (!counterIonOnly) {
                if (counter < numClusters) {
                    nearestClusters.add(sortedList.get(key));
                    counter++;
                    //System.out.printf("Added %s, dist = %.4f\n", sortedList.get(key).getFormula(), key);
                }
            }
            if ((counterIonCounter == numClusters && counterIonOnly) ||
                    (counterIonCounter == numClusters && counter == numClusters && !counterIonOnly)) {
                break;
            }
        }
        return nearestClusters;
    }

    /**
     * Returns clusters without a certain distance from cluster with selected atom
     * @param clusterOfInterest 
     * @param cutoff
     * @param counterIonOnly
     * @return
     */
    public Set<AtomCluster> getClustersWithinXDist(AtomCluster clusterOfInterest, double cutoff, boolean counterIonOnly) {
        
        Point3d center = clusterOfInterest.getCenter();
        Set<AtomCluster> nearestClusters = new HashSet<AtomCluster>();
        Map<Double, AtomCluster> sortedList = new TreeMap<Double, AtomCluster>();
        String clusterFormula = clusterOfInterest.getFormula();
        for (AtomCluster cluster : clusters) {
            if (cluster != clusterOfInterest) {
                Point3d coord = cluster.getCenter();
                double dist = coord.distance(center);
                sortedList.put(dist, cluster);
            }
        }
        nearestClusters.add(clusterOfInterest);
        for (Double key : sortedList.keySet()) {
            if (key < cutoff) {
                if ((!counterIonOnly) ||
                        (!sortedList.get(key).getFormula().equals(clusterFormula))) {
                    nearestClusters.add(sortedList.get(key));
                }
            }
        }
        return nearestClusters;
    }

    public AtomCluster getCluster(Atom at) {
        for (AtomCluster cluster : clusters) {
            if (cluster.contains(at)) {
                return cluster;
            }
        }
        throw new IllegalArgumentException("Atom not in molecule!");
    }

    @Override
    public String toString() {
        StringBuilder strB = new StringBuilder();
        for (AtomCluster cluster : clusters) {
            strB.append("Cluster - " + StringConvUtils.chemicalFormulaFromAtomList(cluster) + "\n");

            for (Atom at : cluster) {
                strB.append(at.toString() + "\n");
            }
        }
        return strB.toString();
    }

    public Set<AtomCluster> getAllClusters() {
        return clusters;
    }

    public int getTotalNumberOfPairs(){
        return clusters.size()/2;
    }

    public static int getGuessedClusterCharge(AtomCluster cluster)
    {

        if (cluster.getFormula().equals("F6P")) {
            return -1;
        } else {
            return 1;
        }
    }

    public static int getGuessedSystemCharge(Set<AtomCluster> clusters) {
        int charge = 0;
        for (AtomCluster cluster : clusters) {
            charge+= getGuessedClusterCharge(cluster);
        }
        return charge;
    }

}

