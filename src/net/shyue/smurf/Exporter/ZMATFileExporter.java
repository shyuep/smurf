package net.shyue.smurf.Exporter;

import net.shyue.smurf.Structure.Molecule;

/**
 * Exports molecule to zmat file.
 * @author shyue
 */
public class ZMATFileExporter extends MolFileExporter {

    /**
     * Constructor
     * @param mol
     */
    public ZMATFileExporter(Molecule mol) {
        super(mol);
    }

    @Override
    public void generate() {
        int[] NNList;
        double bondlength, angle, dih;
        StringBuilder ZMatrix = new StringBuilder(mol.size() * 80);
        StringBuilder outputVAR = new StringBuilder(mol.size() * 3 * 20);
        for (int i = 0, n = mol.size(); i < n; i++) {
            switch (i) {
                case 0:
                    ZMatrix.append(mol.getAtomSpecies(i) + "\n");
                    break;
                case 1:
                    NNList = findNNPositionedBeforeAtom(i);
                    bondlength = mol.getDist(i,NNList[0]);
                    ZMatrix.append(String.format("%s %d B%d\n", mol.getAtomSpecies(i),
                            NNList[0] + 1, i));
                    outputVAR.append(String.format("B%d=%.6f\n", i, bondlength));
                    break;
                case 2:
                    NNList = findNNPositionedBeforeAtom(i);
                    bondlength = mol.getDist(i, NNList[0]);
                    angle = mol.angle(i, NNList[0], NNList[1]);
                    ZMatrix.append(String.format("%s %d B%d %d A%d\n", mol.getAtomSpecies(i),
                            NNList[0] + 1, i, NNList[1] + 1, i));
                    outputVAR.append(String.format("B%d=%.6f\n", i, bondlength));
                    outputVAR.append(String.format("A%d=%.6f\n", i, angle));
                    break;
                default:
                    NNList = findNNPositionedBeforeAtom(i);
                    bondlength = mol.getDist(i, NNList[0]);
                    angle = mol.angle(i,NNList[0],NNList[1]);
                    dih = mol.dihedral(i,NNList[0], NNList[1], NNList[2]);
                    //System.out.printf("Dihedral between atoms %d %d %d %d is %.4f",i,NNList[0], NNList[1], NNList[2],dih);
                    ZMatrix.append(String.format("%s %d B%d %d A%d %d D%d\n", mol.getAtomSpecies(i),
                            NNList[0] + 1, i, NNList[1] + 1, i, NNList[2] + 1, i));
                    outputVAR.append(String.format("B%d=%.6f\n", i, bondlength));
                    outputVAR.append(String.format("A%d=%.6f\n", i, angle));
                    outputVAR.append(String.format("D%d=%.6f\n", i, dih));
            }
        }

        ZMatrix.append("\n");
        ZMatrix.append(outputVAR);
        output = ZMatrix.toString();
    }

    /**
     * Returns index of nearest neighbour atoms, the atom itself excluded.
     */
    private int[] findNNPositionedBeforeAtom(int at) {
        int[] NN_index = {-1, -1, -1};
        double dist;
        double Rmin1 = 2e9, Rmin2 = 2e9, Rmin3 = 2e9;
        for (int i = 0; i < at; i++) {
            if (i != at) {
                dist = mol.getDist(mol.get(at), mol.get(i));
                if (dist < Rmin1) {
                    Rmin3 = Rmin2;
                    Rmin2 = Rmin1;
                    Rmin1 = dist;
                    NN_index[2] = NN_index[1];
                    NN_index[1] = NN_index[0];
                    NN_index[0] = i;
                } else if (dist < Rmin2) {
                    Rmin3 = Rmin2;
                    Rmin2 = dist;
                    NN_index[2] = NN_index[1];
                    NN_index[1] = i;
                } else if (dist < Rmin3) {
                    Rmin3 = dist;
                    NN_index[2] = i;
                }
            }
        }
        return NN_index;
    }
}
