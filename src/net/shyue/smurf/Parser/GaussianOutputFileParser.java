package net.shyue.smurf.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.shyue.smurf.Structure.Element;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author shyue
 */
public class GaussianOutputFileParser extends MolFileParser {

    // Define some constants for user to specify various types of parameters.
    public static enum CORRECTION {ZEROPOINT,THERMAL,ENTHALPY,GIBBS_FREE_ENERGY};
    public static enum ORBITAL {HOMO, LUMO};
    public static enum PCMPARAMETER {EPS,EPSINF,RSOLV,DENSITY};
    public static enum SOLENERGY {DISPERSION,CAVITATION,REPULSION,TOTAL};
    private String parsedName,  functional,  basisSet,  runType,  stationaryPointType, runParameters,startingCoord,endingCoord;
    private int charge,  spinMult,  numBasisFunctions;
    private double SCFEnergy,  HOMOEnergy,  LUMOEnergy,  dispersionE, cavitationE, repulsionE,  totalEInSol;
    private boolean isPCM;
    private double ZPECorrection,  ThermalECorrection,  ThermalHCorrection,  ThermalGCorrection;
    private double eps,  epsInf,  rSolv,  density;
    private final int FILE_SIZE_CUTOFF = 10000000;

    /**
     * Creates new instance of molParser and parse file with supplied filename
     * @param filename_in
     * @throws MolParser.MolParserException
     */
    public GaussianOutputFileParser(String filename_in) throws MolParser.MolParserException {
        super(filename_in);
    }

    @Override
    public void parse() throws MolParser.MolParserException {
        //System.out.println(file.getName()+ " size : "+file.length());
        if (file.length() < FILE_SIZE_CUTOFF) {
            parseSmall(); // Use more efficient parser for small files, i.e. <10Mb.
        } else {
            parseLarge(); // Less efficient but less memory consumption parser for large files.
        }
    }

    /**
     * Efficient parser for small files.
     * @throws net.shyue.smurf.Parser.MolParser.MolParserException
     */
    private void parseSmall() throws MolParser.MolParserException {

        //System.out.println("Parse small");
        String filecontents = readWholeFile();
        //String [] split = filecontents.split("\n");
        //System.out.printf("Last few lines : %s\n%s\n%s\n",split[split.length-3],split[split.length-2],split[split.length-1]);
        Pattern p = Pattern.compile("\\s*\\-+[\\r\\n]+(\\s*[^#\r\n]+)[\\r\\n]+\\s*\\-+[\\r\\n]", Pattern.MULTILINE);
        Matcher m = p.matcher(filecontents);

        if (!filecontents.contains("Normal termination of Gaussian")) {
            throw new MolParserException(filename + "is an invalid Gaussian output file.  Not properly finished run.");
        }

        int i = 0;
        while (m.find() && i < 1) {
            parsedName = m.group(1).trim();
            mBuilder.setName(parsedName);
            i++;
        }


        p = Pattern.compile("-+[\n\r\\s]+(\\#[pPnNtT]*.*[\n\r\\s]*.*)[\n\r\\s]+-+", Pattern.MULTILINE);
        m = p.matcher(filecontents);
        if (m.find()) {
            parseRoute(m.group(1));
        }

        p = Pattern.compile("Charge\\s+=\\s*([-\\d]+)\\s+Multiplicity\\s+=\\s*(\\d+)", Pattern.MULTILINE);
        m = p.matcher(filecontents);
        if (m.find()) {
            charge = Integer.parseInt(m.group(1));
            spinMult = Integer.parseInt(m.group(2));
            mBuilder.setCharge(charge);
            mBuilder.setSpinMult(spinMult);
        }

        p = Pattern.compile("([0-9]+)\\s+basis functions", Pattern.MULTILINE);
        if (p.matcher(filecontents).find()) {
            m = p.matcher(filecontents);
            if (m.find()) {
                numBasisFunctions = Integer.parseInt(m.group(1));
            }
        }

        p = Pattern.compile("Optimized", Pattern.MULTILINE);
        runType = (p.matcher(filecontents).find()) ? "OPT" : "SP";


        if (runType.equals("OPT_FREQ") || runType.equals("FREQ")) {
            ZPECorrection = regexDouble("Zero-point correction=\\s+([\\d\\.-]+)", filecontents);
            ThermalECorrection = regexDouble("Thermal correction to Energy=\\s+([\\d\\.-]+)", filecontents);
            ThermalHCorrection = regexDouble("Thermal correction to Enthalpy=\\s+([\\d\\.-]+)", filecontents);
            ThermalGCorrection = regexDouble("Thermal correction to Gibbs Free Energy=\\s+([\\d\\.-]+)", filecontents);

        }

        p = Pattern.compile("imaginary frequencies", Pattern.MULTILINE);
        stationaryPointType = (p.matcher(filecontents).find()) ? "Saddle" : "Minimum";

        Pattern mp2Pat = Pattern.compile("EUMP2\\s*=\\s*(.*)", Pattern.MULTILINE);
        Pattern ONIOMPat = Pattern.compile("ONIOM:\\s+extrapolated energy\\s+=\\s+(.*)", Pattern.MULTILINE);
        if (ONIOMPat.matcher(filecontents).find())
        {
            //System.out.println("Parse ONIOM");
            m = ONIOMPat.matcher(filecontents);
            while (m.find()) {
                String tmpstr = m.group(1);
                SCFEnergy = Double.parseDouble(tmpstr);
            }
        } else if (mp2Pat.matcher(filecontents).find()) {
            m = mp2Pat.matcher(filecontents);
            while (m.find()) {
                String tmpstr = m.group(1);
                tmpstr = tmpstr.replace("D", "E");
                SCFEnergy = Double.parseDouble(tmpstr);
            }
        } else {
            SCFEnergy = regexDouble("E\\(.*\\)\\s*=\\s*([-\\.\\d]+)\\s+", filecontents);
        }

        p = Pattern.compile("Polarizable Continuum Model");
        isPCM = p.matcher(filecontents).find();
        if (isPCM) {
            cavitationE = regexDouble("Cavitation energy\\s+\\S+\\s+=\\s+(\\S*)", filecontents);
            dispersionE = regexDouble("Dispersion energy\\s+\\S+\\s+=\\s+(\\S*)", filecontents);
            repulsionE = regexDouble("Repulsion energy\\s+\\S+\\s+=\\s+(\\S*)", filecontents);
            totalEInSol = regexDouble("with all non electrostatic terms\\s+\\S+\\s+=\\s+(\\S*)", filecontents);
            eps = regexDouble("Eps\\s*=\\s*([0-9\\.]+)", filecontents);
            epsInf = regexDouble("Eps\\(inf[inity]*\\)\\s*=\\s*([0-9\\.]+)", filecontents);
            density = regexDouble("Numeral density\\s*=\\s*([0-9\\.]+)", filecontents);
            rSolv = regexDouble("RSolv\\s*=\\s*([0-9\\.]+)", filecontents);
        }

        int startindex = filecontents.indexOf("orientation");
        String coordtxt = filecontents.substring(startindex);

        //Advance to next index corresponding to 1, which is start of coordinate spec
        startindex = coordtxt.indexOf("1");
        coordtxt = coordtxt.substring(startindex);

        //Find the next index of --, which corresponds to end of coordinate spec.
        int endindex = coordtxt.indexOf("--");

        coordtxt = coordtxt.substring(0, endindex);
        parseStartingCoord(coordtxt);


        startindex = filecontents.lastIndexOf("orientation");
        coordtxt = filecontents.substring(startindex);

        //Advance to next index corresponding to 1, which is start of coordinate spec
        startindex = coordtxt.indexOf("1");
        coordtxt = coordtxt.substring(startindex);

        //Find the next index of --, which corresponds to end of coordinate spec.
        endindex = coordtxt.indexOf("--");

        coordtxt = coordtxt.substring(0, endindex);
        parseCoord(coordtxt);
        parseOrbitals(filecontents);

        fileParsed = true;
    }

    /**
     * Less efficient parser for large files but minimizes memory consumption
     * to prevent out of memory error.
     * @throws net.shyue.smurf.Parser.MolParser.MolParserException
     */
    private void parseLarge() throws MolParser.MolParserException {
        //System.out.println("Parse large");
        Pattern startPattern = Pattern.compile(" \\(Enter \\S+l101\\.exe\\)");
        Pattern routePattern = Pattern.compile(" \\#[pPnNtT]*.*");
        Pattern chargeMulPattern = Pattern.compile("Charge\\s+=\\s*([-\\d]+)\\s+Multiplicity\\s+=\\s*(\\d+)");
        //Pattern numAtomPattern = Pattern.compile("NAtoms=\\s*([\\d]+)");
        Pattern numBasisFunctionsPattern = Pattern.compile("([0-9]+)\\s+basis functions");
        Pattern pcmPattern = Pattern.compile("Polarizable Continuum Model");
        //Pattern runTypePattern = Pattern.compile("Optimized");
        Pattern statPointType = Pattern.compile("imaginary frequencies");
        Pattern SCFEPattern = Pattern.compile("E\\(.*\\)\\s*=\\s*([-\\.\\d]+)\\s+");
        Pattern mp2Pattern = Pattern.compile("EUMP2\\s*=\\s*(.*)");
        Pattern ONIOMPat = Pattern.compile("ONIOM:\\s+extrapolated energy\\s*=\\s*(.*)");
        
        Pattern normalTermPattern = Pattern.compile("Normal termination of Gaussian");
        Pattern stdOrientationPattern = Pattern.compile("Standard orientation");
        Pattern endPattern = Pattern.compile("--+");
        Pattern orbitalPattern = Pattern.compile("Alpha\\s*\\S+\\s*eigenvalues --(.*)");
        Pattern thermochemistryPattern = Pattern.compile("Zero-point correction=\\s+([\\d\\.-]+)");

        boolean properlyTerminated = false;
        isPCM = false;
        //runType = "SP";
        stationaryPointType = "Minimum";
        StringBuilder coordTxtBuilder = new StringBuilder();
        StringBuilder orbitalsTxtBuilder = new StringBuilder();
        int parseStage = 0;
        boolean thermoParsed = false;
        boolean isMP2 = false;
        //boolean isOPT = false;
        boolean numBasisFound = false;
        boolean startingCoordFound =false;

        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    if (parseStage == 0) {
                        if (startPattern.matcher(line).matches()) {
                            input.readLine();
                            parsedName = input.readLine().trim();
                            mBuilder.setName(parsedName);
                            input.readLine();
                            parseStage = 1;
                        } else if (routePattern.matcher(line).matches()) {
                            String routeLine = "";
                            while (!endPattern.matcher(line).find()) {
                                routeLine += line + "\n";
                                line = input.readLine();
                            }
                            parseRoute(routeLine);
                            isMP2 = (functional.contains("MP2"));
                        }
                    } else if (parseStage == 1) {
                        if (chargeMulPattern.matcher(line).find()) {
                            Matcher m = chargeMulPattern.matcher(line);
                            m.find();
                            charge = Integer.parseInt(m.group(1));
                            spinMult = Integer.parseInt(m.group(2));
                            mBuilder.setCharge(charge);
                            mBuilder.setSpinMult(spinMult);
                            parseStage = 2;
                        }
                    } else if (parseStage == 2) {
                 
                        if (isPCM) {
                            checkPCM(line);
                        }
                        if ((!thermoParsed) && runType.equals("FREQ")) {

                            if (thermochemistryPattern.matcher(line).find()) {
                                String thermotxt = line + "\n";
                                thermotxt += input.readLine() + "\n";
                                thermotxt += input.readLine() + "\n";
                                thermotxt += input.readLine() + "\n";
                                checkThermochemistry(thermotxt);
                                thermoParsed = true;
                            }
                        }
                        
                        if (normalTermPattern.matcher(line).find()) {
                            properlyTerminated = true;
                        }else if ((!numBasisFound)&&(numBasisFunctionsPattern.matcher(line).find())) {
                            Matcher m = numBasisFunctionsPattern.matcher(line);
                            m.find();
                            numBasisFunctions = Integer.parseInt(m.group(1));
                            numBasisFound = true;
                        } 
                        else if ((!isPCM) && (pcmPattern.matcher(line).find())) {
                            isPCM = true;
                        } else if (runType.equals("OPT_FREQ") &&statPointType.matcher(line).find()) {
                            stationaryPointType = "Saddle";
                        } else if (isMP2){
                            if (mp2Pattern.matcher(line).find()) 
                            {
                                Matcher m = mp2Pattern.matcher(line);
                                if (m.find())
                                {
                                    String tmpstr = m.group(1);
                                    tmpstr = tmpstr.replace("D", "E");
                                    SCFEnergy = Double.parseDouble(tmpstr);
                                }
                            }
                        } else if ((!isMP2)&&ONIOMPat.matcher(line).find()) {
                            Matcher m = ONIOMPat.matcher(line);
                            m.find();
                            SCFEnergy = Double.parseDouble(m.group(1));
                        } else if ((!isMP2)&&SCFEPattern.matcher(line).find()) {
                            Matcher m = SCFEPattern.matcher(line);
                            m.find();
                            SCFEnergy = Double.parseDouble(m.group(1));
                        } else if (stdOrientationPattern.matcher(line).find()) {
                            coordTxtBuilder = new StringBuilder();
                            for (int i = 0; i < 5; i++) {
                                line = input.readLine();
                            }
                            while (!endPattern.matcher(line).find()) {
                                coordTxtBuilder.append(line);
                                coordTxtBuilder.append("\n");
                                line = input.readLine();
                            }
                            if (!startingCoordFound)
                            {
                                parseStartingCoord(coordTxtBuilder.toString());
                                startingCoordFound = true;
                            }
                            
                        } else if (orbitalPattern.matcher(line).find()) {
                            orbitalsTxtBuilder = new StringBuilder();
                            while (orbitalPattern.matcher(line).find()) {
                                orbitalsTxtBuilder.append(line);
                                orbitalsTxtBuilder.append("\n");
                                line = input.readLine();
                            }
                        }

                    }
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }


        if (!properlyTerminated) {
            throw new MolParserException(filename + "is an invalid Gaussian output file.  Not properly finished run.");
        }

        parseCoord(coordTxtBuilder.toString());
        parseOrbitals(orbitalsTxtBuilder.toString());

        fileParsed = true;
    }
    

    private double regexDouble(String regex, String text) {
        Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher m = p.matcher(text);
        double value = 0;
        while (m.find()) {
            value = Double.parseDouble(m.group(1));
        }
        return value;
    }

    private void parseStartingCoord(String coordTxt)
    {
        StringBuilder startingCoordBuilder = new StringBuilder();
        Pattern coordmatch = Pattern.compile("[\\d]+\\s+([\\d]+)\\s+[\\d]+\\s+([\\d\\.-]+)\\s+([\\d\\.-]+)\\s+([\\d\\.-]+)", Pattern.MULTILINE);
        Matcher m = coordmatch.matcher(coordTxt);
        while (m.find()) {
            Element species = Element.getSpecies(Integer.parseInt(m.group(1)));
            double x = Double.parseDouble(m.group(2));
            double y = Double.parseDouble(m.group(3));
            double z = Double.parseDouble(m.group(4));
            startingCoordBuilder.append(species + " " + x + " " +y +" "+z + "\n");
        }
        startingCoord = startingCoordBuilder.toString();
    }

    public String getStartingCoord(){
        return startingCoord;
    }

    public String getEndingCoord(){
        return endingCoord;
    }

    private void parseCoord(String coordTxt) {
        StringBuilder coordBuilder = new StringBuilder();
        Pattern coordmatch = Pattern.compile("[\\d]+\\s+([\\d]+)\\s+[\\d]+\\s+([\\d\\.-]+)\\s+([\\d\\.-]+)\\s+([\\d\\.-]+)", Pattern.MULTILINE);
        Matcher m = coordmatch.matcher(coordTxt);
        while (m.find()) {
            Element species = Element.getSpecies(Integer.parseInt(m.group(1)));
            double x = Double.parseDouble(m.group(2));
            double y = Double.parseDouble(m.group(3));
            double z = Double.parseDouble(m.group(4));
            mBuilder.addAtom(species, x, y, z);
            coordBuilder.append(species + " " + x + " " +y +" "+z + "\n");
        }
        endingCoord = coordBuilder.toString();
    }

    private void parseOrbitals(String orbitalsTxt) {
        //System.out.println(orbitalsTxt);
        Pattern p = Pattern.compile("Alpha  occ. eigenvalues --(.*)\\n\\s+Alpha virt. eigenvalues -- (.*)", Pattern.MULTILINE);
        Matcher m = p.matcher(orbitalsTxt);
        while (m.find()) {
            String[] homoValues = m.group(1).trim().split("\\s+");
            String[] lumoValues = m.group(2).trim().split("\\s+");
            HOMOEnergy = Double.parseDouble(homoValues[homoValues.length - 1]);
            LUMOEnergy = Double.parseDouble(lumoValues[0]);
        }

    }

    private void parseRoute(String line) {
        String oneLine = line.replaceAll("[\n\r]+\\s+", "");      
        oneLine = oneLine.trim();

        Pattern optPattern = Pattern.compile("Opt",Pattern.CASE_INSENSITIVE);
        Pattern freqPattern = Pattern.compile("Freq",Pattern.CASE_INSENSITIVE);
        if (optPattern.matcher(oneLine).find() && freqPattern.matcher(oneLine).find())
            runType = "OPT_FREQ";
        else if (freqPattern.matcher(oneLine).find())
            runType = "FREQ";
        else
            runType = "SP";

        String [] tokStr = oneLine.split("\\s+");
        Pattern p = Pattern.compile("(\\w*((B3LYP)|(MP2)|(PBEPBE))+)\\/(\\S+)");
        Pattern outputmatch = Pattern.compile("#[pnt]");
        runParameters= "";
        for (String str : tokStr)
        {
            Matcher m = p.matcher(str);
            if (m.find()) {
                functional = m.group(2);
                basisSet = m.group(6);
            }
            else {
                if (!outputmatch.matcher(str).matches())
                {
                    runParameters += str + ",";
                }
            }
        }
        if (!runParameters.matches(""))
            runParameters = runParameters.substring(0, runParameters.length()-1);

    }

    private void checkPCM(String line) {
        Pattern dispersionEPattern = Pattern.compile("Dispersion energy\\s+\\S+\\s+=\\s+(\\S*)");
        Pattern cavitationEPattern = Pattern.compile("Cavitation energy\\s+\\S+\\s+=\\s+(\\S*)");
        Pattern repulsionEPattern = Pattern.compile("Repulsion energy\\s+\\S+\\s+=\\s+(\\S*)");
        Pattern totalEPattern = Pattern.compile("with all non electrostatic terms\\s+\\S+\\s+=\\s+(\\S*)");
        Pattern epsPattern = Pattern.compile("Eps\\s*=\\s*(\\S*)");
        Pattern epsInfPattern = Pattern.compile("Eps\\(inf[inity]*\\)\\s*=\\s*(\\S+)");
        Pattern densityPattern = Pattern.compile("Numeral density\\s*=\\s*(\\S+)");
        Pattern rSolvPattern = Pattern.compile("RSolv\\s*=\\s*(\\S+)");

        if (dispersionEPattern.matcher(line).find()) {
            Matcher m = dispersionEPattern.matcher(line);
            if (m.find()) {
                dispersionE = Double.parseDouble(m.group(1));
            }
        } else if (cavitationEPattern.matcher(line).find()) {
            Matcher m = cavitationEPattern.matcher(line);
            if (m.find()) {
                cavitationE = Double.parseDouble(m.group(1));
            }
        } else if (repulsionEPattern.matcher(line).find()) {
            Matcher m = repulsionEPattern.matcher(line);
            if (m.find()) {
                repulsionE = Double.parseDouble(m.group(1));
            }
        } else if (totalEPattern.matcher(line).find()) {
            Matcher m = totalEPattern.matcher(line);
            if (m.find()) {
                totalEInSol = Double.parseDouble(m.group(1));
            }
        } else if (epsPattern.matcher(line).find()) {
            Matcher m = epsPattern.matcher(line);
            if (m.find()) {
                eps = Double.parseDouble(m.group(1));
            }
        } else if (epsInfPattern.matcher(line).find()) {
            Matcher m = totalEPattern.matcher(line);
            if (m.find()) {
                epsInf = Double.parseDouble(m.group(1));
            }
        } else if (densityPattern.matcher(line).find()) {
            Matcher m = totalEPattern.matcher(line);
            if (m.find()) {
                density = Double.parseDouble(m.group(1));
            }
        } else if (rSolvPattern.matcher(line).find()) {
            Matcher m = totalEPattern.matcher(line);
            if (m.find()) {
                rSolv = Double.parseDouble(m.group(1));
            }
        }

    }

    private void checkThermochemistry(String thermoTxt) {
        Pattern p = Pattern.compile("Zero-point correction=\\s+([\\d\\.-]+)", Pattern.MULTILINE);

        Matcher m = p.matcher(thermoTxt);
        if (m.find()) {
            ZPECorrection = Double.parseDouble(m.group(1));
        }

        p = Pattern.compile("Thermal correction to Energy=\\s+([\\d\\.-]+)", Pattern.MULTILINE);
        m = p.matcher(thermoTxt);
        if (m.find()) {
            ThermalECorrection = Double.parseDouble(m.group(1));
        }

        p = Pattern.compile("Thermal correction to Enthalpy=\\s+([\\d\\.-]+)", Pattern.MULTILINE);
        m = p.matcher(thermoTxt);
        if (m.find()) {
            ThermalHCorrection = Double.parseDouble(m.group(1));
        }

        p = Pattern.compile("Thermal correction to Gibbs Free Energy=\\s+([\\d\\.-]+)", Pattern.MULTILINE);
        m = p.matcher(thermoTxt);
        if (m.find()) {
            ThermalGCorrection = Double.parseDouble(m.group(1));
        }

    }

    /**
     * 
     * @return True if run is a PCM run.
     */
    public boolean isPCMRun() {
        if (fileParsed) {
            return isPCM;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * 
     * @return "SP" if single point energy calculation, "OPT" if optimization.
     */
    public String getRunType() {
        if (fileParsed) {
            return runType;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * 
     * @return Functional used in calculation.
     */
    public String getFunctional() {
        if (fileParsed) {
            return functional;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * 
     * @return Basis Set used in Calculation
     */
    public String getBasisSet() {
        if (fileParsed) {
            return basisSet;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    public String getOtherParameters(){
        if (fileParsed) {
            return runParameters;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * 
     * @return Charge of Molecule
     */
    public int getCharge() {
        if (fileParsed) {
            return charge;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * 
     * @return Spin multiplicity of Molecule
     */
    public int getSpinMultiplicity() {
        if (fileParsed) {
            return spinMult;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * 
     * @return Stationary Point Type
     */
    public String getStationaryPointType() {

        if (fileParsed) {
            return stationaryPointType;
        }
        throw new IllegalStateException("File not yet parsed!");


    }

    /**
     * Return zero-point, thermal, enthalpy or Gibbs Free Energy Correction
     * @param correctionType GaussianOutputFileParser.ZEROPOINT, 
     * GaussianOutputFileParser.THERMAL, GaussianOutputFileParser.ENTHALPY or 
     * GaussianOutputFileParser.GIBBS_FREE_ENERGY
     * @return Correction value in Hartrees
     */
    public double getCorrection(CORRECTION correctionType) {

        if (fileParsed) {
            switch (correctionType) {
                case ZEROPOINT:
                    return ZPECorrection;
                case THERMAL:
                    return ThermalECorrection;
                case ENTHALPY:
                    return ThermalHCorrection;
                case GIBBS_FREE_ENERGY:
                    return ThermalGCorrection;
                default:
                    throw new IllegalArgumentException("Unsupported correction type");
            }


        }
        throw new IllegalStateException("File not yet parsed!");

    }

    /**
     * Return energy of SCF Run.
     * @return
     */
    public double getSCFEnergy() {
        if (fileParsed) {
            return SCFEnergy;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * Return value for parameter type
     * @param type GaussianOutputFileParser.EPS, GaussianOutputFileParser.EPSINF, GaussianOutputFileParser.RSOLV or GaussianOutputFileParser.DENSITY
     * @return Value of parameter in PCM run.
     */
    public double getSolventParameters(PCMPARAMETER type) {
        if (fileParsed) {
            if (isPCMRun()) {
                switch (type) {
                    case EPS:
                        return eps;
                    case EPSINF:
                        return epsInf;
                    case DENSITY:
                        return density;
                    case RSOLV:
                        return rSolv;
                    default:
                        throw new IllegalArgumentException("Unsupported solvent parameter type");
                }
            }
            else
                return 0;
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * Return total energy in solution for PCM runs.
     * @param type Solution energy type
     * @return Total energy in solution
     */
    public double getSOLEnergy(SOLENERGY type) {
        if (fileParsed) {
            if (isPCM) {
                switch (type){
                    case DISPERSION:
                        return dispersionE;
                    case REPULSION:
                        return repulsionE;
                    case CAVITATION:
                        return cavitationE;
                    case TOTAL:
                        return totalEInSol;
                }
            } else {
                return 0;
            }
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     * Return number of basis functions used in run
     * @return number of basis functions used in run
     */
    public int getNumberOfBasisFunctions() {

        if (fileParsed) {
            return numBasisFunctions;
        }
        throw new IllegalStateException("File not yet parsed!");


    }

    /**
     * Get HOMO or LUMO
     * @param type GaussianOutputFileParser.HOMO or GaussianOutputFileParser.LUMO
     * @return
     */
    public double getOrbital(ORBITAL type) {
        if (fileParsed) {
            switch (type) {
                case HOMO:
                    return HOMOEnergy;
                case LUMO:
                    return LUMOEnergy;
                default:
                    throw new IllegalArgumentException("Unsupported orbital type");
            }
        }
        throw new IllegalStateException("File not yet parsed!");
    }

    /**
     *
     * @return
     */
    public String getRunSummaryXML() {
        StringBuffer summaryBuf = new StringBuffer();
        summaryBuf.append("<?xml version=\"1.0\"?>\n<Run>");
        if (parsedName == null) {
            throw new IllegalStateException("File not yet parsed!");
        } else {
            summaryBuf.append("<Title>" + parsedName + "</Title>\n");
            summaryBuf.append("<Type>" + runType + "</Type>\n");
            summaryBuf.append("<OtherRunParameters>" + runParameters + "</OtherRunParameters>\n");
            summaryBuf.append("<Functional>" + getFunctional() + "</Functional>\n");
            summaryBuf.append("<BasisSet>" + basisSet + "</BasisSet>\n");
            summaryBuf.append("<Charge>" + getCharge() + "</Charge>\n");
            summaryBuf.append("<SpinMultiplicity>" + getSpinMultiplicity() + "</SpinMultiplicity>\n");
            //summaryBuf.append("<FinalStructure>"+mol.toString()+"</FinalStructure>\n");

            summaryBuf.append("<SCF>\n");
            summaryBuf.append("<Energy>" + getSCFEnergy() + "</Energy>\n");
            summaryBuf.append("</SCF>\n");
            summaryBuf.append("<STATIONARY_POINT_TYPE>" + stationaryPointType + "</STATIONARY_POINT_TYPE>\n");
            if (runType.matches("OPT_FREQ") || runType.matches("FREQ")) {

                summaryBuf.append("<ZERO_POINT_CORRECTION>" + getCorrection(CORRECTION.ZEROPOINT) + "</ZERO_POINT_CORRECTION>\n");
                summaryBuf.append("<THERMALE_CORRECTION>" + getCorrection(CORRECTION.THERMAL) + "</THERMALE_CORRECTION>\n");
                summaryBuf.append("<ENTHALPY_CORRECTION>" + getCorrection(CORRECTION.ENTHALPY) + "</ENTHALPY_CORRECTION>\n");
                summaryBuf.append("<GIBBS_FREE_ENERGY_CORRECTION>" + getCorrection(CORRECTION.GIBBS_FREE_ENERGY) + "</GIBBS_FREE_ENERGY_CORRECTION>\n");
            }

            summaryBuf.append("<HOMO>" + getOrbital(ORBITAL.HOMO) + "</HOMO>\n");
            summaryBuf.append("<LUMO>" + getOrbital(ORBITAL.LUMO) + "</LUMO>\n");


            summaryBuf.append("<PCM>\n");
            summaryBuf.append("<SolventPresent>" + isPCMRun() + "</SolventPresent>\n");
            if (isPCMRun()) {
                summaryBuf.append("<EPS>" + getSolventParameters(PCMPARAMETER.EPS) + "</EPS>\n");
                summaryBuf.append("<EPSINF>" + getSolventParameters(PCMPARAMETER.EPSINF) + "</EPSINF>\n");
                summaryBuf.append("<RSOLV>" + getSolventParameters(PCMPARAMETER.RSOLV) + "</RSOLV>\n");
                summaryBuf.append("<DENSITY>" + getSolventParameters(PCMPARAMETER.DENSITY) + "</DENSITY>\n");
            }

            summaryBuf.append("</PCM>\n");
        }
        summaryBuf.append("</Run>\n");

        return summaryBuf.toString();
    }
}
