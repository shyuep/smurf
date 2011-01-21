package net.shyue.smurf.Exporter;

/**
 * Interface for a molecule exporter. 
 * @author shyue
 */
public interface MolExporter {
    
    /**
     *
     */
    public void generate();

    /**
     *
     * @return
     */
    public String getStringRepresentation();

}
