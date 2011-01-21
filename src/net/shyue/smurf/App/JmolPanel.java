package net.shyue.smurf.App;

import net.shyue.smurf.Exporter.XYZFileExporter;
import net.shyue.smurf.Structure.Molecule;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Stack;
import javax.swing.JPanel;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.popup.JmolPopup;
import org.jmol.viewer.JmolConstants;
import org.jmol.export.dialog.Dialog;
import org.jmol.export.image.ImageCreator;

/**
 * Simple JmolPanel class with popup menus based on Jmol package.
 * @author shyue
 */
public class JmolPanel extends JPanel {

    // The viewer itself
    protected JmolViewer viewer;
    /* The adapter to parse various files or input formats. */
    private JmolAdapter adapter;
    // The popup menu on right-click.
    private JmolPopup jmolpopup;
    // Stack to hold last few selected atoms for manipulations
    private Stack<Integer> selectedAtoms;
    private Molecule mol;

    class MyStatusListener implements JmolStatusListener {

        public void notifyNewDefaultModeMeasurement(int count, String strInfo) {
            // Not implemented
        }

        public void notifyFileLoaded(String fullPathName, String fileName, String modelName, String clientFile, String errorMessage) {
            selectedAtoms = new Stack<Integer>();// Reset stack of selected atoms for new file
        }

        public void notifyAtomPicked(String strInfo) {
            String[] strarr = strInfo.split(" ");
            int atomIndex = Integer.parseInt(strarr[1].substring(1)) - 1;
            selectedAtoms.add(atomIndex);
            if (selectedAtoms.size() > 5) // Keep overall stack to 5 atoms only to minimize memory use
            {
                selectedAtoms.remove(0);
            }
        }

        @Override
        public void setCallbackFunction(String arg0, String arg1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyCallback(int type, Object[] data) {
            String strInfo = (data == null || data[1] == null ? null : data[1].toString());
            switch (type) {
                case JmolConstants.CALLBACK_LOADSTRUCT:
                    notifyFileLoaded(strInfo, (String) data[2], (String) data[3], (String) data[4], "");
                    break;
                case JmolConstants.CALLBACK_ANIMFRAME:
                    break;
                case JmolConstants.CALLBACK_ECHO:
                    break;
                case JmolConstants.CALLBACK_MEASURE:
                    if (data.length == 3) {
                        notifyAtomPicked(strInfo);
                    }
                    break;
                case JmolConstants.CALLBACK_MESSAGE:
                    break;
                case JmolConstants.CALLBACK_PICK:
                    notifyAtomPicked(strInfo);
                    break;
                case JmolConstants.CALLBACK_SCRIPT:
                    break;
                case JmolConstants.CALLBACK_RESIZE:
                case JmolConstants.CALLBACK_SYNC:
                case JmolConstants.CALLBACK_HOVER:
                case JmolConstants.CALLBACK_MINIMIZATION:
                    break;
            }
        }

        @Override
        public boolean notifyEnabled(int type) {
            switch (type) {
                case JmolConstants.CALLBACK_ANIMFRAME:
                case JmolConstants.CALLBACK_ECHO:
                case JmolConstants.CALLBACK_LOADSTRUCT:
                case JmolConstants.CALLBACK_MEASURE:
                case JmolConstants.CALLBACK_MESSAGE:
                case JmolConstants.CALLBACK_PICK:

                case JmolConstants.CALLBACK_SCRIPT:
                    return true;
                case JmolConstants.CALLBACK_HOVER:
                case JmolConstants.CALLBACK_MINIMIZATION:
                case JmolConstants.CALLBACK_RESIZE:
                case JmolConstants.CALLBACK_SYNC:
                //applet only
            }
            return false;
        }

        @Override
        public String eval(String arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float[][] functionXY(String arg0, int arg1, int arg2) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String createImage(String fileName, String type, Object text_or_bytes,
                int quality) {
            ImageCreator c = new ImageCreator(viewer);
            if (quality != Integer.MIN_VALUE && (fileName == null || fileName.equalsIgnoreCase("CLIPBOARD"))) {
                c.clipImage(null);
                return "OK";
            }
            c.createImage(fileName, type, text_or_bytes, quality);

//            if (msg == null || msg.startsWith("OK")) {
//                return msg;
//            }
            return "";
        }

        @Override
        public Hashtable getRegistryInfo() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void handlePopupMenu(int x, int y) {
            jmolpopup.show(x, y);
        }

        @Override
        public void showConsole(boolean arg0) {
            //throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void showUrl(String arg0) {
            //throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String dialogAsk(String type, String fileName) {
            if (type.equals("load")) {
                return "No supported";//getOpenFileNameFromDialog(fileName);
            }
            if (type.equals("save")) {
                return "not supported";//(new Dialog()).getSaveFileNameFromDialog(viewer, fileName, null);
            }
            if (type.equals("saveImage")) {
                Dialog sd = new Dialog();
                int qualityJPG = -1;
                int qualityPNG = -1;
                String imageType = null;
                final String[] imageChoices = {"JPEG", "PNG", "GIF", "PPM", "PDF"};
                final String[] imageExtensions = {"jpg", "png", "gif", "ppm", "pdf"};

                fileName = sd.getImageFileNameFromDialog(viewer,
                        fileName, imageType, imageChoices, imageExtensions, qualityJPG,
                        qualityPNG);
                imageType = sd.getType();
                qualityJPG = sd.getQuality("JPG");
                qualityPNG = sd.getQuality("PNG");
                return fileName;
            }
            return null;
        }
    }


    public JmolPanel() {
        adapter = new SmarterJmolAdapter();
//        viewer =     viewer = JmolViewer.allocateViewer(this, adapter,
//        null, null, null, "",
//        new MyStatusListener());
        viewer = JmolViewer.allocateViewer(this, adapter);
        //viewer.setAppletContext("", null, null, "");
        viewer.setJmolStatusListener(new MyStatusListener());
        jmolpopup = JmolPopup.newJmolPopup(viewer, false, "this", true);
    }

    public JmolViewer getViewer() {
        return viewer;
    }

    public int getLastSelectedAtom() {
        if (selectedAtoms.empty()) {
            return -1;
        } else {
            return selectedAtoms.peek();
        }
    }

    public int getSecondLastSelectedAtom() {
        if (selectedAtoms.size() < 2) {
            return -1;
        } else {
            return selectedAtoms.get(selectedAtoms.size() - 2);
        }
    }

    public int getNumberOfSelectedAtoms() {
        return selectedAtoms.size();
    }

    /**
     * Constructor using supplied molecule.
     * @param mol_in
     */
    public JmolPanel(Molecule mol_in) {
        this();
        setMolecule(mol_in);
    }

    /**
     *
     * @param mol_in Sets molecule displayed to supplied molecule
     */
    public void setMolecule(Molecule mol_in) {
        mol = mol_in;
        XYZFileExporter exporter = new XYZFileExporter(mol);
        exporter.generate();
        viewer.openStringInline(exporter.getStringRepresentation());
        selectedAtoms = new Stack<Integer>();
    }
    final Dimension currentSize = new Dimension();
    final Rectangle rectClip = new Rectangle();

    @Override
    public void paint(Graphics g) {
        getSize(currentSize);
        g.getClipBounds(rectClip);
        viewer.renderScreenImage(g, currentSize, rectClip);
    }
}
