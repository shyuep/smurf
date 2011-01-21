package net.shyue.smurf.Exporter;

import net.shyue.smurf.Structure.Molecule;

/**
 * Basic Factory method to get the correct Parser based on either an input 
 * format or from the filename.  Relevant constant variables are provided
 * for client to select the relevant format.
 * @author shyue
 */
public class MolFileExporterFactory {

    public static enum FILE_FORMATS {

        ZMAT, XYZ, GAU, POSCAR, XYZCOORD
    };

    /**
     * Returns an appropriate 
     * @param mol
     * @param format
     * @return
     */
    public static MolFileExporter getExporter(Molecule mol, FILE_FORMATS format) {

        switch (format) {
            case GAU:
                return new GaussianInputFileExporter(mol);
            case XYZ:
                return new XYZFileExporter(mol);
            case XYZCOORD:
                return new XYZCoordFileExporter(mol);
            case ZMAT:
                return new ZMATFileExporter(mol);
            case POSCAR:
                return new POSCARExporter(mol);
            default:
                throw new IllegalArgumentException("Unsupported format!");
        }
    }

    /**
     * Returns an appropriate MolFileExporter based on file extension
     * @param mol
     * @param filename
     * @return MolFileExporter
     */
    public static MolFileExporter getExporterFromFileExt(Molecule mol, String filename) {

        String fileext = filename.contains(".") ? filename.substring(filename.lastIndexOf("."), filename.length()) : "";
        if (fileext.equalsIgnoreCase("com") | fileext.equalsIgnoreCase("gjf")) {
            return getExporter(mol, FILE_FORMATS.GAU);
        } else if (fileext.equalsIgnoreCase("xyz")) {
            return getExporter(mol, FILE_FORMATS.XYZ);
        } else {
            return getExporter(mol, FILE_FORMATS.ZMAT);
        }
    }
}
