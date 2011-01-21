package net.shyue.smurf.Structure;

/**
 *
 * @author Shyue
 */
public class Bond {

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Bond other = (Bond) obj;
        if (this.at1 == other.at1 && this.at2 == other.at2) {
            return true;
        }
        if (this.at1 == other.at2 && this.at2 == other.at1) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        if (at1.getAtNo() > at2.getAtNo())
        {
            hash = 67 * hash + (this.at1 != null ? this.at1.getAtNo() : 0);
            hash = 67 * hash + (this.at2 != null ? this.at2.getAtNo() : 0);
        }
        else
        {
            hash = 67 * hash + (this.at2 != null ? this.at2.getAtNo() : 0);
            hash = 67 * hash + (this.at1 != null ? this.at1.getAtNo() : 0);
        }
        
        return hash;
    }

    private final Atom at1;
    private final Atom at2;


    public Bond (Atom _at1, Atom _at2)
    {
        at1 = _at1;
        at2 = _at2;
    }

    public double getLength()
    {
        return at1.getCoord().distance(at2.getCoord());
    }

    public String getType(){
        if (at1.getAtNo() > at2.getAtNo())
        {
            return at1.getSpecies()+"-"+at2.getSpecies();
        }
        else
        {
            return at2.getSpecies()+"-"+at1.getSpecies();
        }
    }

}
