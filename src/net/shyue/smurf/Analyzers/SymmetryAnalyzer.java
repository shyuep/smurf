package net.shyue.smurf.Analyzers;

import net.shyue.smurf.Structure.*;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import net.shyue.smurf.Utils.CollectionBinner;
import net.shyue.smurf.Utils.DistanceSimilarityComparator;
import net.shyue.smurf.Utils.SpeciesSimilarityComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import static java.lang.Math.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a Molecule symmetry finder which assigns a point group to a Molecule
 * based on symmetry operations found in the Molecule.
 *
 * @author shyue
 */
public final class SymmetryAnalyzer {

    private Molecule mol;
    private Atom origin_atom;
    private Map<Double, Map<Element, List<Atom>>> sortedAtoms;
    private Map<Vector3d, Integer> rotSymmetries = new HashMap<Vector3d, Integer>();
    private Map<Vector3d, Double> principalAxes = new HashMap<Vector3d, Double>(3);
    private String schSymbol;
    private List<SymmetryOperation> detectedSymmetries;
    private PointGroup assignedPointGroup;
    private double TOLERANCE = 0.3;
    private double EIG_TOLERANCE = 0.01;
    private double MATRIX_TOLERANCE = 0.1;
    private Logger log;

    public SymmetryAnalyzer(Molecule mol_in) {
        this(mol_in, Level.OFF);
    }

    /**
     * Constructor for new analyzer from input molecule.
     * @param mol_in
     * @param logLevel 
     */
    public SymmetryAnalyzer(Molecule mol_in, Level logLevel) {
        log = Logger.getLogger("Symmetry detection log");
        log.setLevel(logLevel);
        detectedSymmetries = new ArrayList<SymmetryOperation>(128);


        if (mol_in.size() <= 1) {
            schSymbol = "Kh";
        } else {
            /**
             * Center Molecule about center of mass, which must be a special
             * symmetry point.
             */
            mol = mol_in.getCenteredCopy();
            sortAtoms();
            processMolecule();
            assignedPointGroup = new PointGroup(schSymbol, generateFullSymmetrySet(detectedSymmetries));
            log.info("Number of symmetry operations : " + assignedPointGroup.getOperations().size());
        }
    }

    private void processMolecule() {
        // Calculate Inertia Tensor
        double Ixx, Iyy, Izz, Ixy, Iyz, Ixz, TotalI, AtWt, x, y, z;
        Ixx = Iyy = Izz = Ixy = Iyz = Ixz = TotalI = 0;
        for (Atom at : mol) {
            AtWt = at.getAtWt();
            x = at.getCoord().x;
            y = at.getCoord().y;
            z = at.getCoord().z;
            Ixx += AtWt * (pow(y, 2) + pow(z, 2));
            Iyy += AtWt * (pow(x, 2) + pow(z, 2));
            Izz += AtWt * (pow(x, 2) + pow(y, 2));
            Ixy += -AtWt * x * y;
            Iyz += -AtWt * y * z;
            Ixz += -AtWt * x * z;
            TotalI += AtWt * (pow(x, 2) + pow(y, 2) + pow(z, 2));
        }
        double[][] Imat = {{Ixx, Ixy, Ixz}, {Ixy, Iyy, Iyz}, {Ixz, Iyz, Izz}};
        Matrix inertiaTensor = new Matrix(Imat);

        /**
         * Normalize the inertia tensor so that it does not scale with size of the
         * system.  This mitigates the problem of choosing a proper comparison
         * tolerance for the eigenvalues.
         **/
        inertiaTensor = inertiaTensor.times(1.0 / TotalI);
        EigenvalueDecomposition EVD = inertiaTensor.eig();
        double[] eigenvalues_I = EVD.getRealEigenvalues();
        Matrix principalDirections = EVD.getV();
        for (int i = 0; i < 3; i++) {
            principalAxes.put(new Vector3d(principalDirections.get(0, i), principalDirections.get(1, i), principalDirections.get(2, i)),
                    eigenvalues_I[i]);
        }
        boolean eigZero = (abs(eigenvalues_I[0] * eigenvalues_I[1] *
                eigenvalues_I[2]) < pow(EIG_TOLERANCE, 3));
        boolean eigIAllSame = (abs(eigenvalues_I[0] - eigenvalues_I[1]) < EIG_TOLERANCE) && (abs(eigenvalues_I[0] - eigenvalues_I[2]) < EIG_TOLERANCE);
        boolean eigIAllDiff = (abs(eigenvalues_I[0] - eigenvalues_I[1]) > EIG_TOLERANCE) && (abs(eigenvalues_I[0] - eigenvalues_I[2]) > EIG_TOLERANCE) &
                (abs(eigenvalues_I[1] - eigenvalues_I[2]) > EIG_TOLERANCE);

        /* Separates the Molecule based on the form of its eigen values and process accordingly
         * - Linear molecules have one zero eigenvalue.  Possible symmetryOperations are C*v or D*v
         * - Asymetric top molecules have all different eigenvalues.  The maximum rotational
         *   symmetry in such molecules is 2
         * - Symmetric top molecules have 1 unique eigenvalue, which gives a unique rotation
         *   axis.  All axial point groups are possible except the cubic groups (T & O) and I.
         * - Spherical top moelcules have all three eigenvalues equal.  They have the rare T, O
         *   or I point groups.  Very difficult to handle, but rare.
         */
        if (eigZero) {
            processLinear();
        } else if (eigIAllSame) {
            processSphTop();
        } else if (eigIAllDiff) {
            processAsymTop();
        } else {
            processSymTop();
        }

    }

    /* General methods used by all molecules. 
     * *********************************************
     */
    /* Sorts atoms according to species AND distance from origin.*/
    private void sortAtoms() {
        sortedAtoms = new HashMap<Double, Map<Element, List<Atom>>>();
        Map<Double, List<Atom>> distanceCollected =
                    CollectionBinner.group(mol.getSites(), new DistanceSimilarityComparator(TOLERANCE));

        for (Double dist : distanceCollected.keySet()) {
            if (dist < TOLERANCE){
                List<Atom> nearOriginAtoms = distanceCollected.get(dist);
                if (nearOriginAtoms.size()>1)
                    System.out.println("Ambiguity as more than one atom near origin!");
                origin_atom = nearOriginAtoms.get(0);
            }else
            {
                Map<Element, List<Atom>> speciesCollected =
                CollectionBinner.group(distanceCollected.get(dist), new SpeciesSimilarityComparator());
                sortedAtoms.put(dist, speciesCollected);
            }
        }
//        for (Atom at : mol) {
//            testdist = at.getCoord().distance(origin);
//            if (testdist <= TOLERANCE) {
//                origin_atom = at;
//            } else {
//                boolean match_found = false;
//                for (AtomSymSet testSet : sortedAtoms) {
//                    if (testSet.match(at.getSpecies(), testdist)) {
//                        testSet.add(at);
//                        match_found = true;
//                        break;
//                    }
//                }
//                if (!match_found) {
//                    sortedAtoms.add(new AtomSymSet(at.getSpecies(), testdist, at));
//                }
//            }
//        }

    }

    /* Returns the smallest list of atoms with the same species and distance
     * from origin.  We only need to iterate through this maximal set to look
     * for symmetry operations to test against the full set of molecules. 
     * */
    private List<Atom> getSmallestSymSet() {
        List<Atom> smallestSet = null;
        int smallestsetsize = 100000000;
        for (Double dist : sortedAtoms.keySet()) {
            for (Element species : sortedAtoms.get(dist).keySet()) {
                if (sortedAtoms.get(dist).get(species).size() < smallestsetsize) {

                    smallestSet = sortedAtoms.get(dist).get(species);
                    smallestsetsize = smallestSet.size();
                }
            }
        }
        return smallestSet;
    }

    /* See similar method.  This is an overloaded version of getSmallestSymSetNotOnAxis()
     * which returns the smallest list of atoms with the same species and distance
     * from origin AND does not lie on the specified axis.  This maximal set
     * limits the possible rotational symmetryOperations, since atoms lying on a test 
     * axis is irrelevant in testing rotational symmetryOperations.
     * */
    private List<Atom> getSmallestSymSetNotOnAxis(Vector3d axis) {

        List<Atom> finallist = new ArrayList<Atom>();
        int smallestsetsize = 100000000;
        for (Double dist : sortedAtoms.keySet()) {
            for (Element species : sortedAtoms.get(dist).keySet())  {
                List<Atom> testSet = sortedAtoms.get(dist).get(species);

                List<Atom> tmplist = new ArrayList<Atom>(testSet.size());
                for (Atom at : testSet) {
                    Vector3d normal = new Vector3d(at.getCoord());
                    normal.cross(normal, axis);
                    double distanceToAxis = normal.length();
                    if (distanceToAxis > TOLERANCE) {
                        tmplist.add(at);
                    }
                }
                if ((tmplist.size() > 0) && (tmplist.size() < smallestsetsize)) {
                    finallist = tmplist;
                    smallestsetsize = tmplist.size();
                }
            }
        }
        // System.out.format("Final list size = %d",finallist.size());
        return finallist;
    }

    /* Checks if the coord and species supplied refers to an actual Atom. */
    public boolean isValidSite(Point3d coord, Element species) {
        double coordDist = coord.distance(new Point3d(0, 0, 0));
        double myTolerance = max(TOLERANCE, TOLERANCE * coordDist);
        for (Atom at : mol) {
            if (at.getSpecies() == species) {
                if (at.getCoord().distance(coord) < myTolerance) {
                    return true;
                }
            }
        }
        return false;
    }

    /* Simple algorithm to find the factors of the highest possible rotational symmetry
     * Unoptimized since numbers we are dealing with are likely to be small */
    private List<Integer> getFactors(int num) {
        List<Integer> factors = new ArrayList<Integer>(0);
        for (int i = 1; i <= sqrt(num); i++) {
            if (num % i == 0) {
                factors.add(i);
                if (num / i != i) {
                    factors.add(num / i);
                }
            }
        }
        Collections.sort(factors);
        return factors;
    }

    /* Molecule classification methods.  Based on form of inertia tensor, calls
     * various methods to look for symmetry operations and classifies the 
     * Molecule accordingly.
     * ************************************************************************* 
     */
    /* Handles linear molecules, which must belong to either the C*v or D*v 
     * points. */
    private void processLinear() {
        log.info("Linear top Molecule detected");
        boolean inversionExists = isValidOperation(SymmetryOperation.Inversion());
        if (inversionExists) {
            schSymbol = "D*h";
        } else {
            schSymbol = "C*v";
        }
    }

    /* Handles assymetric top molecules, which cannot contain rotational symmetryOperations
    larger than 2. */
    private void processAsymTop() {
        log.info("Assymetric top Molecule detected");
        checkR2axes_Asym();
        if (rotSymmetries.size() == 0) {
            processNoRotSym();
        } else if (rotSymmetries.size() == 3) {
            processDihedralGrp();
        } else {
            processCyclicGrp();
        }
    }

    /* Handles symetric top molecules which has one unique eigenvalue whose
     * corresponding principal axis is a unique rotational axis.  More complex
     * handling required to look for R2 axes perpendiarul to this unique axis.
     */
    private void processSymTop() {
        log.info("Symmetric top Molecule detected");
        checkRotationalSymmetry(getUniquePrincipalAxis());
        if (rotSymmetries.size() > 0) {
            checkPerpendicularR2Axes(getUniquePrincipalAxis());
        }

        if (rotSymmetries.size() >= 2) {
            processDihedralGrp();
        } else if (rotSymmetries.size() == 1) {
            processCyclicGrp();
        } else {
            processNoRotSym();
        }
    }

    /* Handles Sperhical Top Molecules, which belongs to the T, O or I point groups.
     * Current code only detectsh symetries up to O.  I symmetryOperations are not tested.
     */
    private void processSphTop() {
        log.info("Spherical top Molecule detected");
        findSphericalAxes();
        Vector3d mainAxis = getHighestRotationAxes();
        if (rotSymmetries.size() == 0 || rotSymmetries.get(mainAxis) < 3) {
            log.info("Accidental speherical top!");
            processSymTop();
        } else if (rotSymmetries.get(mainAxis) == 3) {
            String mirrorType = findMirror(mainAxis);
            if (!mirrorType.matches("")) {
                if (isValidOperation(SymmetryOperation.Inversion())) {
                    schSymbol = "Th";
                } else {
                    schSymbol = "Td";
                }
            } else {
                schSymbol = "T";
            }

        } else if (rotSymmetries.get(mainAxis) == 4) {
            if (isValidOperation(SymmetryOperation.Inversion())) {
                schSymbol = "Oh";
            } else {
                schSymbol = "O";
            }
        } else if (rotSymmetries.get(mainAxis) == 5) {
            if (isValidOperation(SymmetryOperation.Inversion())) {
                schSymbol = "Ih";
            } else {
                schSymbol = "I";
            }
        }
    }

    /* Handles molecules with no rotational symmetry. Only possible point groups
     * are C1, Cs and Ci.
     */
    private void processNoRotSym() {
        log.info("No rotational symmetries detected");
        schSymbol = "C1";
        if (isValidOperation(SymmetryOperation.Inversion())) {
            schSymbol = "Ci";
        } else {
            for (Vector3d key : principalAxes.keySet()) {
                String mirrorType = findMirror(key);
                if (!mirrorType.matches("")) {
                    schSymbol = "Cs";
                    break;
                }
            }
        }
    }

    /* Handles cyclic group molecules.
     */
    private void processCyclicGrp() {
        log.info("Cyclic group detected!");
        Vector3d mainaxis = getHighestRotationAxes();
        schSymbol = "C" + Integer.toString(rotSymmetries.get(mainaxis));
        String mirrorType = findMirror(mainaxis);
        if (mirrorType.matches("h")) {
            schSymbol += "h";
        } else if (mirrorType.matches("v")) {
            schSymbol += "v";
        }
        if (mirrorType.matches("")) {
            if (isValidOperation(SymmetryOperation.RotoReflection(new Point3d(0, 0, 0),
                    mainaxis, 180.0 / rotSymmetries.get(mainaxis)))) {
                schSymbol = "S" + Integer.toString(2 * rotSymmetries.get(mainaxis));
            }
        }
    }

    /* Handles dihedral group molecules, i.e those with intersecting R2 axes
     * and a main axis.
     */
    private void processDihedralGrp() {
        Vector3d mainaxis = getHighestRotationAxes();
        schSymbol = "D" + Integer.toString(rotSymmetries.get(mainaxis));
        String mirrorType = findMirror(mainaxis);
        if (mirrorType.matches("h")) {
            schSymbol += "h";
        } else if (!mirrorType.matches("")) {
            schSymbol += "d";
        }
    }

    /* Symmetry determination methods.  Called by classification methods to look
     * for various symmetry operations in Molecule.
     * ************************************************************************* 
     */
    /* Looks for R5, R4, R3 and R2 axes in speherical top molecules.  Point group
     * T molecules have only one unique 3-fold and one unique 2-fold axis.
     * O molecules have one unique 4, 3 and 2-fold axes.
     * I molecules have a unique 5-fold axis.
     */
    private void findSphericalAxes() {
        boolean R2present, R3present, R4present, R5present;
        R2present = R3present = R4present = R5present = false;
        Point3d origin = new Point3d(0, 0, 0);

        Vector3d testAxis = new Vector3d();
        List<Atom> testset = getSmallestSymSet();
        for (int i = 0; i < testset.size() - 2; i++) {
            Point3d atom1 = testset.get(i).getCoord();
            for (int j = i + 1; j < testset.size() - 1; j++) {
                Point3d atom2 = testset.get(j).getCoord();
                if (!R2present) {
                    testAxis.add(atom2, atom1);
                    if (testAxis.length() > TOLERANCE) {
                        R2present = isValidOperation(SymmetryOperation.Rotation(origin, testAxis, 180));
                        if (R2present) {
                            rotSymmetries.put(new Vector3d(testAxis), 2);
                        }
                    }
                }
                for (int k = j + 1; k < testset.size(); k++) {
                    Point3d atom3 = testset.get(k).getCoord();
                    if (!R2present) {
                        testAxis.add(atom1, atom3);
                        if (testAxis.length() > TOLERANCE) {
                            R2present = isValidOperation(SymmetryOperation.Rotation(origin, testAxis, 180));
                            if (R2present) {
                                rotSymmetries.put(new Vector3d(testAxis), 2);
                            }
                        }
                    }
                    Vector3d vec1 = new Vector3d();
                    vec1.sub(atom2, atom1);
                    Vector3d vec2 = new Vector3d();
                    vec2.sub(atom3, atom1);
                    testAxis.cross(vec1, vec2);
                    if (testAxis.length() > TOLERANCE) {
                        if (!R3present) {
                            R3present = isValidOperation(SymmetryOperation.Rotation(origin, testAxis, 120));
                            if (R3present) {
                                rotSymmetries.put(new Vector3d(testAxis), 3);
                                break;
                            }
                        }
                        if (!R4present) {
                            R4present = isValidOperation(SymmetryOperation.Rotation(origin, testAxis, 90));
                            if (R4present) {
                                rotSymmetries.put(new Vector3d(testAxis), 4);
                                break;
                            }
                        }
                        if (!R5present) {
                            R5present = isValidOperation(SymmetryOperation.Rotation(origin, testAxis, 72));
                            if (R5present) {
                                rotSymmetries.put(new Vector3d(testAxis), 5);
                                break;
                            }
                        }
                    }

                }
                if ((R3present) && (R2present) && ((R4present) || (R5present))) {
                    break;
                }
            }
            if ((R3present) && (R2present) && ((R4present) || (R5present))) {
                break;
            }
        }
    }

    /* Looks for testmirror symmetry of specified type about axis.  Possible types
     * are "h" or "vd".  Horizontal (h) mirrors are perpendicular to the axis
     * while vertical (v) or diagonal (d) mirrors are parallel.  v mirrors has
     * atoms lying on the testmirror plane while d mirrors do not.
     */
    private String findMirror(Vector3d axis) {
        Vector3d mirrorPlane = new Vector3d();
        boolean mirror_exists = false;
        String mirror_type = "";

        //First test whether the axis itself is the normal to a mirror plane.
        mirror_exists = isValidOperation(SymmetryOperation.Reflect(new Point3d(0, 0, 0), axis));
        if (mirror_exists) {
            mirror_type = "h";
        } else {
            // Iterate through all pairs of atoms to find mirror
            for (int i = 0; i < mol.size() - 1; i++) {
                for (int j = i + 1; j < mol.size(); j++) {
                    if (mol.getAtomSpecies(i) == mol.getAtomSpecies(j)) {
                        mirrorPlane.sub(mol.getAtomCoord(i), mol.getAtomCoord(j));
                        if (mirrorPlane.dot(axis) < TOLERANCE) {
                            mirror_exists = isValidOperation(SymmetryOperation.Reflect(new Point3d(0, 0, 0), mirrorPlane));
                            if (mirror_exists) {
                                break;
                            }
                        }
                    }
                }
                if (mirror_exists) {
                    break;
                }
            }
            if (mirror_exists) {
                if (rotSymmetries.size() > 1) {
                    mirror_type = "d";
                    for (Vector3d key : rotSymmetries.keySet()) {
                        Vector3d testVec = new Vector3d();
                        testVec.sub(key, axis);
                        if (!(testVec.length() < TOLERANCE)) {
                            if (key.dot(mirrorPlane) < TOLERANCE) {
                                mirror_type = "v";
                                break;
                            }
                        }
                    }
                } else {
                    mirror_type = "v";
                }
            }
        }
        return mirror_type;
    }


    /* This is a special method to test for 2-fold rotation along the principal
     * axes.  It is mainly used to handle asymetric top molecules.
     */
    private void checkR2axes_Asym() {
        boolean R2present;
        //For assymetric top molecules, determine all three rotation axes
        for (Vector3d key : principalAxes.keySet()) {
            R2present = isValidOperation(SymmetryOperation.Rotation(new Point3d(0, 0, 0), key, 180));
            if (R2present) {
                rotSymmetries.put(new Vector3d(key), 2);
            }
        }
    }

    /* Returns the principal axis which has the unique eigenvalue.  For handling
     * symmetric top molecules.
     */
    private Vector3d getUniquePrincipalAxis() {
        Object[] allkeys = principalAxes.keySet().toArray();
        if (abs(principalAxes.get(allkeys[0]) - principalAxes.get(allkeys[1])) < 1e-2) {
            return (Vector3d) allkeys[2];
        } else if (abs(principalAxes.get(allkeys[0]) - principalAxes.get(allkeys[2])) < 1e-2) {
            return (Vector3d) allkeys[1];
        } else {
            return (Vector3d) allkeys[0];
        }
    }

    /* Checks for R2 axes perpendicular to unique axis.  For handling
     * symmetric top molecules.
     */
    private void checkPerpendicularR2Axes(Vector3d mainaxis) {
        //System.out.println("Checking perpendicular R2!");
        boolean R2present = false;
        List<Atom> possibleSymAtomSet = getSmallestSymSetNotOnAxis(mainaxis);
        for (int i = 0; i < possibleSymAtomSet.size() - 1; i++) {
            for (int j = i + 1; j < possibleSymAtomSet.size(); j++) {
                Vector3d testAxis = new Vector3d();
                testAxis.sub(possibleSymAtomSet.get(i).getCoord(), possibleSymAtomSet.get(j).getCoord());
                testAxis.cross(testAxis, mainaxis);
                if (testAxis.length() > TOLERANCE) {
                    R2present = isValidOperation(SymmetryOperation.Rotation(new Point3d(0, 0, 0), testAxis, 180));
                    if (R2present) {
                        rotSymmetries.put(new Vector3d(testAxis), 2);
                        break;
                    }
                }
            }
            if (R2present) {
                break;
            }
        }

    }

    /* Determines the rotational symmetry about supplied axis.  Used only for
     * symmetric top molecules which has possible rotational symmetryOperations > 2.
     */
    private int checkRotationalSymmetry(Vector3d axis) {
        int maxsym = 3000;
        List<Atom> smallestsym = getSmallestSymSetNotOnAxis(axis);
        maxsym = smallestsym.size();
        //System.out.println(smallestsym.get(0).getSpecies());
        //System.out.format("Maximum Rotational Symmetry : %d \n", maxsym);
        /* Get the factors of the maximum possible symmetry to more efficiently
         * test for rotational symmetryOperations
         */
        List<Integer> test_symmetries = getFactors(maxsym);
        int finalsym = 1;
        for (int i = test_symmetries.size() - 1; i > 0; i--) {
            boolean rotvalid = isValidOperation(SymmetryOperation.Rotation(new Point3d(0, 0, 0), axis, 360 / test_symmetries.get(i)));
            if (rotvalid) {
                finalsym = test_symmetries.get(i);
                rotSymmetries.put(new Vector3d(axis), finalsym);
                break;
            }
        }

        return finalsym;
    }

    /* Checks if supplied operation is a valid symmetry operation for Molecule
     */
    private boolean isValidOperation(SymmetryOperation symop) {
        for (Atom at : mol) {
            if (!isValidSite(symop.transformPoint(at.getCoord()), at.getSpecies())) {
                return false;
            }
        }
        
        detectedSymmetries.add(symop);
        return true;
    }


    /*Returns the axis with the highest rotational symmetry*/
    private Vector3d getHighestRotationAxes() {
        int highestsym = 1;
        Vector3d highestrotAxis = new Vector3d();
        for (Vector3d testvec : rotSymmetries.keySet()) {
            if (rotSymmetries.get(testvec) > highestsym) {
                highestsym = rotSymmetries.get(testvec);
                highestrotAxis = testvec;
            }
        }
        return highestrotAxis;
    }

    /**
     * Returns point group symbol of Molecule
     * @return Schoenflies symbol
     */
    public String getPointGroupSymbol() {
        return schSymbol;
    }

    /**
     * Returns symmetry information with all symmetry operations
     * @return Symmetry information.
     */
    public String getSymmetryInfo() {
        return assignedPointGroup.toString();
    }

    /**
     * Return point group of molecule
     * @return Point group
     */
    public PointGroup getPointGroup() {
        return assignedPointGroup;
    }

    /**
     * Return point group of molecule
     * @return Point group
     */
    public Molecule getCenteredMolecule() {
        return mol;
    }

    /**
     * Returns indices of set of symmetrically distinct atoms of a particular species
     * @param species 
     * @return Indices of symmetrically distinct atoms of particular species.
     */
    public List<Atom> getAllDistinct(Element species) {
        List<Atom> uniqueatoms = new ArrayList<Atom>();
        for (Atom at : mol) {
            if (at.getSpecies() == species) {
                boolean found = false;
                for (Atom at2 : uniqueatoms) {
                    if (assignedPointGroup.isSymmetricallyEquivalent(at.getCoord(), at2.getCoord(), TOLERANCE)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    uniqueatoms.add(at);
                }
            }
        }
        return uniqueatoms;
    }

    /**
     * Return atoms which are symmetrically distinct within a provided set.
     * @param species
     * @param testSet
     * @return List of atoms which are distinct
     */
    public List<Atom> getDistinctSet(Element species, List<Atom> testSet) {
        List<Atom> uniqueatoms = new ArrayList<Atom>();
        for (Atom at : testSet) {

            if (at.getSpecies() == species) {
                boolean found = false;
                for (Atom compareAt : uniqueatoms) {
                    if (assignedPointGroup.isSymmetricallyEquivalent(at.getCoord(), compareAt.getCoord(), TOLERANCE)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    uniqueatoms.add(at);
                }
            }
        }
        return uniqueatoms;
    }

    /* Recursive algorithm to permutate through all possible combinations of
     * the initially supplied symmetry operations to arrive at a complete
     * set of operations mapping a single atom to all other equivalent atoms
     * in the point group.  This assumes that the initial number already uniquely
     * identifies all operations.*/
    private List<SymmetryOperation> generateFullSymmetrySet(List<SymmetryOperation> operations) {
        List<SymmetryOperation> newSet = operations;
        SymmetryOperation testSym;
        boolean complete = true;
        for (SymmetryOperation sm1 : operations) {
            for (SymmetryOperation sm2 : operations) {
                testSym = SymmetryOperation.product(sm1, sm2);
                if (!inSet(testSym, operations)) {
                    newSet.add(testSym);
                    complete = false;
                    break;
                }
                testSym = SymmetryOperation.product(sm2, sm1);
                if (!inSet(testSym, operations)) {
                    newSet.add(testSym);
                    complete = false;
                    break;
                }

            }
            if (!complete) {
                break;
            }
        }

        if (operations.size() > 200) {
            System.err.println("Generation of symmetry operations in infinite loop.  " +
                    "Possible error in initial operations or tolerance too low.");
            return newSet;
        //System.exit(-1);
        }
        if (!complete) {
            return generateFullSymmetrySet(newSet);
        } else {
            return newSet;
        }
    }

    /**
     * Checks if symmetry matrix, testMat is already within the comparison set.
     * @param testMat
     * @param comparisonSet
     * @return
     */
    private boolean inSet(SymmetryOperation testMat, List<SymmetryOperation> comparisonSet) {
        boolean inset = false;
        for (SymmetryOperation cmp : comparisonSet) {
            if (SymmetryOperation.isEqualWithinTolerance(cmp, testMat, MATRIX_TOLERANCE)) {
                inset = true;
                break;
            }
        }
        return inset;
    }

//    private class AtomSymSet {
//
//        private String species;
//        private double distanceFromOrigin;
//        private List<Atom> atomList;
//
//        public AtomSymSet(String species_in, double dist_in, List<Atom> atomListIn) {
//            distanceFromOrigin = dist_in;
//            atomList = atomListIn;
//            species = species_in;
//        }
//
//        public AtomSymSet(String species_in, double dist_in, Atom atom_In) {
//            distanceFromOrigin = dist_in;
//            atomList = new ArrayList<Atom>();
//            atomList.add(atom_In);
//            species = species_in;
//        }
//
//        public boolean match(String species_in, double dist_in) {
//            if ((species.matches(species_in)) && (distanceFromOrigin - dist_in < TOLERANCE)) {
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        public List<Atom> getAtomList() {
//            return atomList;
//        }
//
//        public void add(Atom atom_In) {
//            atomList.add(atom_In);
//        }
//
//        public int size() {
//            return atomList.size();
//        }
//    }
}



