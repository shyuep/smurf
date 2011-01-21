package net.shyue.smurf.Analyzers;


import edu.mit.carlf.CompGeometry.QuickHull3D.Point3d;
import edu.mit.carlf.CompGeometry.QuickHull3D.QuickHull3D;
import net.shyue.smurf.Parser.MolFileParser;
import net.shyue.smurf.Parser.MolFileParserFactory;
import net.shyue.smurf.Structure.Molecule;

/**
 *
 * @author shyue
 */
public class VolumeCalculator {

    private QuickHull3D hull;
    private Point3d[] coords;
    private Molecule mol;
    private final int numAtoms;

    public VolumeCalculator(Molecule _mol) {
        mol = _mol;
        numAtoms = mol.size();

        coords = new Point3d[mol.size()];
        for (int i = 0; i < numAtoms; i++) {
            javax.vecmath.Point3d coord = mol.getAtomCoord(i);
            coords[i] = new Point3d(coord.x, coord.y, coord.z);
        }
        if (mol.size() >= 4) {
            hull = new QuickHull3D();
            hull.build(coords);
        }


    }

    public double getVolume() {

        if (numAtoms < 4) {
            return 0;
        } else {
            return hull.getVolume();
        }
    }

    public static void main(String args[]) {
        String filename = "/media/Data/Test Structures/Amm_1_1_1_1_1_4_4_6.geom";

        try {
            MolFileParser parser = MolFileParserFactory.getParser(filename);
            parser.parse();
            Molecule mol = parser.getMolecule();
            VolumeCalculator cal = new VolumeCalculator(mol);
//            for (Line line : cal.getLines())
//            {
//                System.out.println(line);
//            }
            System.out.println("Vol = " + cal.getVolume());

            System.out.println("For matlab");
            for (int i = 0; i < mol.size(); i++) {
                javax.vecmath.Point3d coord = mol.getAtomCoord(i);
                System.out.printf("%.4f %.4f %.4f\n", coord.x, coord.y, coord.z);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }



    }
}
