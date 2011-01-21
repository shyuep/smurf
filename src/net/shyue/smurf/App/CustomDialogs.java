package net.shyue.smurf.App;

import net.shyue.smurf.Structure.Atom;
import net.shyue.smurf.Structure.Element;
import net.shyue.smurf.Structure.Molecule;
import net.shyue.smurf.Structure.Templates.SUBSTITUENT;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author shyue
 */
public class CustomDialogs {

    public static String selectSubstituteeDialog(Molecule mol) {
        List<Element> possSub = new ArrayList<Element>();
        for (Atom at : mol)
        {
            if (!possSub.contains(at.getSpecies()))
                    possSub.add(at.getSpecies());
        }
        Object[] possibilities = possSub.toArray();
        return JOptionPane.showInputDialog(null, "Choose substitutee atom type:",
                "Substitutee selection",
                JOptionPane.PLAIN_MESSAGE,
                null,
                possibilities,
                possibilities[0]).toString();
    }

    public static SUBSTITUENT selectSubstituentDialog() {
        SUBSTITUENT [] possibilities = SUBSTITUENT.values();
        return (SUBSTITUENT) JOptionPane.showInputDialog(null, "Choose substituent:",
                "Substituent selection",
                JOptionPane.PLAIN_MESSAGE,
                null,
                possibilities,
                possibilities[0]);
    }

    public static void infoTextAreaDialog(String title, String output){
        JTextArea outputTextArea = new JTextArea(output);
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setRows(10);
        outputTextArea.setColumns(30);
        JScrollPane outputScroll = new JScrollPane();
        outputScroll.setViewportView(outputTextArea);
        JOptionPane.showMessageDialog(null, outputScroll, title, JOptionPane.INFORMATION_MESSAGE);
    }


}
