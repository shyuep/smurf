package net.shyue.smurf.Exporter;

import net.shyue.smurf.Structure.Molecule;

/**
 * Exports molecules to a Gaussian Input File format
 * @author shyue
 */
public class GaussianInputFileExporter extends ZMATFileExporter {

    private String link0Commands;
    private int numProcs;
    private String functional;
    private String basisSet;
    private String routeParameters;
    private String inputParameters;
    private String title;
    private int charge;
    private int spinMult;
    private String coordSpec;

    /**
     *
     * @param mol
     */
    public GaussianInputFileExporter(Molecule mol) {
        super(mol);
        title = mol.getName();
        charge = mol.getCharge();
        spinMult = mol.getSpinMult();
        functional = "B3LYP";
        basisSet = "6-31+G(D)";
        routeParameters = "Opt Freq";
        inputParameters = "";
        link0Commands = "";
        numProcs = 1;
    }

    /**
     * Set Link0 section for Gaussian input
     * @param link0_in
     */
    public void setLink0(String link0_in) {
        link0Commands = link0_in;
    }

    /**
     * Set functional for Gaussian Input File
     * @param functional_in
     */
    public void setFunctional(String functional_in) {
        functional = functional_in;
    }

    /**
     * Set basis set for Gaussian Run
     * @param basisSet_in
     */
    public void setBasisSet(String basisSet_in) {
        basisSet = basisSet_in;
    }

    /**
     * Set route parameters (e.g. run type) for Gaussian Run
     * @param routeParameters_in
     */
    public void setRouteParameters(String routeParameters_in) {
        routeParameters = routeParameters_in;
    }

    /**
     * 
     * @param inputParameters_in
     */
    public void setInputParameters(String inputParameters_in) {
        inputParameters = inputParameters_in;
    }

    /**
     * Set charge 
     * @param charge_in
     */
    public void setCharge(int charge_in) {
        charge = charge_in;
    }

    /**
     * Set spin multiplicity
     * @param spinMult_in
     */
    public void setSpinMult(int spinMult_in) {
        spinMult = spinMult_in;
    }

    /**
     * Set number of proccessors
     * @param numProcs_in
     */
    public void setNumProcs(int numProcs_in) {
        numProcs = numProcs_in;
    }

    /**
     * Set all parameters
     * @param numProcs_in
     * @param link0Commands_in
     * @param functional_in
     * @param basisSet_in
     * @param routeParameters_in
     * @param charge_in
     * @param spinMult_in
     * @param inputParameters_in
     */
    public void setParameters(int numProcs_in, String link0Commands_in,
            String functional_in, String basisSet_in, String routeParameters_in,
            int charge_in, int spinMult_in, String inputParameters_in) {
        numProcs = numProcs_in;
        link0Commands = link0Commands_in;
        functional = functional_in;
        basisSet = basisSet_in;
        routeParameters = routeParameters_in;
        inputParameters = inputParameters_in;
        charge = charge_in;
        spinMult = spinMult_in;
    }

    /**
     * Generate output based on parameters
     */
    @Override
    public void generate() {
        super.generate();
        coordSpec = super.getStringRepresentation();
        output = generateGaussianInput();
    }

    private String generateGaussianInput() {
        StringBuilder s = new StringBuilder((mol.size() * 4 + 5) * 50);
        s.append("%nprocs=" + Integer.toString(numProcs) + "\n");
        if (!link0Commands.matches("")) {
            s.append(link0Commands + "\n");
        }
        s.append("#P " + functional + "/" + basisSet + " " + routeParameters + " Test GFINPUT GFOLDPRINT\n\n");
        s.append(title);
        s.append("\n\n");
        s.append(String.format("%d %d\n", charge, spinMult));
        s.append(coordSpec);
        s.append("\n");
        s.append(inputParameters);
        s.append("\n");
        return s.toString();
    }
}
