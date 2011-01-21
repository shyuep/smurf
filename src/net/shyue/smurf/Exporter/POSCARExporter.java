package net.shyue.smurf.Exporter;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Element;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Utils.CollectionBinner;
import net.shyue.smurf.Utils.SpeciesSimilarityComparator;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point3d;

/**
 *
 * @author shyue
 */
public class POSCARExporter extends MolFileExporter {

    double [] cellDim = {10,10,10};
    

    /**
     * 
     * @param mol
     */
    public POSCARExporter(Molecule mol) {
        super(mol);
    }

    public void setCellDimensions(double [] _cellDim){
        cellDim = _cellDim;
    }

    public void setCellDimensions(double _cellDim){
        cellDim[0] = _cellDim;
        cellDim[1] = _cellDim;
        cellDim[2] = _cellDim;
    }


    @Override
    public void generate() {
        output = mol.getName()+"\n";
        output += "1.0\n";
        output += String.format("%.4f 0.0000 0.0000\n0.0000 %.4f 0.0000\n0.0000 0.0000 %.4f\n", cellDim[0], cellDim[1], cellDim[2]);
        List<Atom> sites = mol.getSites();
        Map<Element,List<Atom>> binnedAtoms = CollectionBinner.group(sites, new SpeciesSimilarityComparator());
        String elementLine = "";
        String coords = "";
        for (Element key : binnedAtoms.keySet())
        {
            List<Atom> atomsOfSpecies = binnedAtoms.get(key);
            elementLine += atomsOfSpecies.size()+" ";
            for (Atom at : atomsOfSpecies)
            {
                Point3d coord = at.getCoord();
                coords += String.format("%.5f %.5f %.5f %s\n", coord.x, coord.y, coord.z, at.getSpecies());
            }
        }
        output += elementLine + "\n";
        output += "Cartesian\n" + coords;

    }
}
