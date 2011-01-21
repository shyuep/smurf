package net.shyue.smurf.Structure.Templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author shyue
 */
public enum SUBSTITUENT {

    F("F", false), Cl("Cl", false),
    METHYL("CH3", true), AMINE("NH2", true), HYDROXYL("OH", true), CYANIDE("CN", true),
    CARBOXYL("COOH", true), TRIFLUOROMETHANE("CF3", true), NITRITE("NO2",true);
    private String chemform;
    private boolean isComplex;

    private SUBSTITUENT(String chemform, boolean isComplex) {
        this.chemform = chemform;
        this.isComplex = isComplex;
    }

    public boolean isComplex() {
        return isComplex;

    }

    public String getChemForm(){
        return chemform;
    }

    public String getTemplate() {

        if (this.isComplex) {
            try {
                InputStream is = SUBSTITUENT.class.getResourceAsStream(chemform);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuilder filecontents = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    filecontents.append(line + "\n");
                }
                return filecontents.toString();
            } catch (IOException ex) {
                System.out.println("Fatal error in SUBSTITUENT enum!");
                ex.printStackTrace();
                System.exit(-1);
            }
        } else {
            return chemform + " NN 1";
        }
        throw new RuntimeException("Illegal state");
    }
}
