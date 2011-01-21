package net.shyue.smurf.App;

import net.shyue.smurf.Exporter.XYZFileExporter;
import net.shyue.smurf.Structure.Molecule;

/**
 * An adaptor class which bridges between JmolPanel and the molecule format in
 * use within SMURF for display purposes.
 * @author shyue
 */
public class SMURFMolPanel extends JmolPanel{
    private Molecule mol;
    
    /**
     * Default constructor.
     */
    public SMURFMolPanel(){
        super();
    }
    
    /**
     * Constructor using supplied molecule.
     * @param mol_in
     */
    public SMURFMolPanel(Molecule mol_in){
        super();
        setMolecule(mol_in);
    }
    
    /**
     *
     * @param mol_in Sets molecule displayed to supplied molecule
     */
    public void setMolecule(Molecule mol_in)
    {
        mol = mol_in;
        XYZFileExporter exporter = new XYZFileExporter(mol);
        exporter.generate();
        viewer.openStringInline(exporter.getStringRepresentation());
    }
    
    /**
     *
     * @return Molecule currently displayed.
     */
    public Molecule getMolecule()
    {
        return mol;
    }

}
