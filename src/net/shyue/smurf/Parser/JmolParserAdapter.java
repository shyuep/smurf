package net.shyue.smurf.Parser;

import Jama.Matrix;
import net.shyue.smurf.Parser.MolParser.MolParserException;
import net.shyue.smurf.Structure.DefaultMolBuilder;
import net.shyue.smurf.Structure.Element;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Hashtable;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapter.AtomIterator;
import static java.lang.Math.*;

/**
 * An Adapter class to use Jmol's smarter adapters as a parser for file formats which I
 * do not have support for.  The file is converted to a xyz coord string which
 * is then parsed through the coord parser I have written.
 * @author shyue
 */
public class JmolParserAdapter extends MolFileParser {

    private JmolAdapter adapter;

    /**
     *
     * @param filename_in
     * @throws MolParserException
     */
    public JmolParserAdapter(String filename_in) throws MolParserException {
        super(filename_in);

    }

    @Override
    public void parse() throws MolParserException {
        adapter = new SmarterJmolAdapter();
        try {
            Object clientFile = adapter.openBufferedReader(file.getAbsolutePath(), new BufferedReader(new FileReader(file)));

            if (adapter.getAtomSetCount(clientFile) == 1) {
                Matrix latticeVec = Matrix.identity(3, 3);
                Hashtable info = adapter.getAtomSetAuxiliaryInfo(clientFile, 0);
                float[] lattParameters = (float[]) info.get("notionalUnitcell");
                if (Boolean.valueOf(info.get("coordinatesAreFractional").toString())) {
                    latticeVec = GenLattVec(lattParameters[0], lattParameters[1], lattParameters[2], lattParameters[3], lattParameters[4], lattParameters[5]);
                }

                AtomIterator iterator = adapter.getAtomIterator(clientFile);

                mBuilder = new DefaultMolBuilder();
                while (iterator.hasNext()) {
                    double x = iterator.getX();
                    double y = iterator.getY();
                    double z = iterator.getZ();
                    double[][] coordarr = {{x, y, z}};
                    Matrix coord = new Matrix(coordarr);
                    coord = coord.times(latticeVec);

                    Element species = Element.valueOf(iterator.getElementSymbol());
                    mBuilder.addAtom(species, coord.get(0, 0), coord.get(0, 1), coord.get(0, 2));
                }
            } else {
                throw new MolParserException("Only single atom set supported for JmolParserAdapter for now.");
            }
        } catch (FileNotFoundException ex) {
            throw new MolParserException(ex.getMessage());
        }
        mBuilder.setName(filename.substring(0, filename.lastIndexOf(".")));
        fileParsed = true;
    }

    public static Matrix GenLattVec(double a, double b, double c, double alpha, double beta, double gamma) {
        double[][] unit_a = {{1, 0, 0}};
        Matrix unitvec = new Matrix(unit_a);
        Matrix LatticeVectors = new Matrix(3, 3);
        LatticeVectors.setMatrix(0, 0, 0, 2, unitvec.times(a));
        Matrix RotM = GenRotMatrix(gamma, 3);
        unitvec = RotM.times(unitvec.transpose());
        LatticeVectors.setMatrix(1, 1, 0, 2, unitvec.times(b).transpose());
        double c_x = c * cos(toRadians(beta));
        double c_y = c * cos(toRadians(alpha) - c_x * unitvec.get(0, 0)) / (unitvec.get(1, 0));
        double c_z = sqrt(pow(c, 2) - pow(c_x, 2) - pow(c_y, 2));
        LatticeVectors.set(2, 0, c_x);
        LatticeVectors.set(2, 1, c_y);
        LatticeVectors.set(2, 2, c_z);
        return LatticeVectors;
    }

    public static Matrix GenRotMatrix(double angle, int axis) {
        double cos_theta = cos(toRadians(angle));
        double sin_theta = sin(toRadians(angle));
        int i = 0, j = 0;
        Matrix tempRotM = new Matrix(3, 3);
        tempRotM.set(axis - 1, axis - 1, 1);
        switch (axis) {
            case 1:
                i = 1;
                j = 2;
                break;
            case 2:
                i = 0;
                j = 2;
                break;
            case 3:
                i = 0;
                j = 1;
                break;
        }
        tempRotM.set(i, i, cos_theta);
        tempRotM.set(i, j, -sin_theta);
        tempRotM.set(j, i, sin_theta);
        tempRotM.set(j, j, cos_theta);
        return tempRotM;
    }
}
