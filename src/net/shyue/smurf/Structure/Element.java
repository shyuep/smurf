package net.shyue.smurf.Structure;

public enum Element {

    H(1, "Hydrogen", 1.0079),
    He(2, "Helium", 4.0026),
    Li(3, "Lithium", 6.9412),
    Be(4, "Beryllium", 9.0122),
    B(5, "Boron", 10.8117),
    C(6, "Carbon", 12.0108),
    N(7, "Nitrogen", 14.0067),
    O(8, "Oxygen", 15.9994),
    F(9, "Fluorine", 18.9984),
    Ne(10, "Neon", 20.1798),
    Na(11, "Sodium", 22.9898),
    Mg(12, "Magnesium", 24.3051),
    Al(13, "Aluminium", 26.9815),
    Si(14, "Silicon", 28.0855),
    P(15, "Phosphorus", 30.9738),
    S(16, "Sulfur", 32.0655),
    Cl(17, "Chlorine", 35.4532),
    Ar(18, "Argon", 39.9481),
    K(19, "Potassium", 39.0983),
    Ca(20, "Calcium", 40.0784),
    Sc(21, "Scandium", 44.9559),
    Ti(22, "Titanium", 47.8671),
    V(23, "Vanadium", 50.9415),
    Cr(24, "Chromium", 51.9962),
    Mn(25, "Manganese", 54.938),
    Fe(26, "Iron", 55.8452),
    Co(27, "Cobalt", 58.9332),
    Ni(28, "Nickel", 58.6934),
    Cu(29, "Copper", 63.5463),
    Zn(30, "Zinc", 65.382),
    Ga(31, "Gallium", 69.7231),
    Ge(32, "Germanium", 72.641),
    As(33, "Arsenic", 74.9216),
    Se(34, "Selenium", 78.963),
    Br(35, "Bromine", 79.9041),
    Kr(36, "Krypton", 83.7982),
    Rb(37, "Rubidium", 85.4678),
    Sr(38, "Strontium", 87.621),
    Y(39, "Yttrium", 88.9059),
    Zr(40, "Zirconium", 91.2242),
    Nb(41, "Niobium", 92.9064),
    Mo(42, "Molybdenum", 95.962),
    Tc(43, "Technetium", 98),
    Ru(44, "Ruthenium", 101.072),
    Rh(45, "Rhodium", 102.9055),
    Pd(46, "Palladium", 106.421),
    Ag(47, "Silver", 107.8682),
    Cd(48, "Cadmium", 112.4118),
    In(49, "Indium", 114.8183),
    Sn(50, "Tin", 118.7107),
    Sb(51, "Antimony", 121.7601),
    Te(52, "Tellurium", 127.603),
    I(53, "Iodine", 126.9045),
    Xe(54, "Xenon", 131.2936),
    Cs(55, "Caesium", 132.9055),
    Ba(56, "Barium", 137.3277),
    La(57, "Lanthanum", 138.9055),
    Ce(58, "Cerium", 140.1161),
    Pr(59, "Praseodymium", 140.9077),
    Nd(60, "Neodymium", 144.2423),
    Pm(61, "Promethium", 145),
    Sm(62, "Samarium", 150.362),
    Eu(63, "Europium", 151.9641),
    Gd(64, "Gadolinium", 157.253),
    Tb(65, "Terbium", 158.9254),
    Dy(66, "Dysprosium", 162.5001),
    Ho(67, "Holmium", 164.9303),
    Er(68, "Erbium", 167.2593),
    Tm(69, "Thulium", 168.9342),
    Yb(70, "Ytterbium", 173.0545),
    Lu(71, "Lutetium", 174.9668),
    Hf(72, "Hafnium", 178.492),
    Ta(73, "Tantalum", 180.9479),
    W(74, "Tungsten", 183.841),
    Re(75, "Rhenium", 186.2071),
    Os(76, "Osmium", 190.233),
    Ir(77, "Iridium", 192.2173),
    Pt(78, "Platinum", 195.0849),
    Au(79, "Gold", 196.9666),
    Hg(80, "Mercury", 200.592),
    Tl(81, "Thallium", 204.3833),
    Pb(82, "Lead", 207.21),
    Bi(83, "Bismuth", 208.9804),
    Po(84, "Polonium", 209),
    At(85, "Astatine", 210),
    Rn(86, "Radon", 222),
    Fr(87, "Francium", 223),
    Ra(88, "Radium", 226),
    Ac(89, "Actinium", 227),
    Th(90, "Thorium", 232.0381),
    Pa(91, "Protactinium", 231.0359),
    U(92, "Uranium", 238.0289);
    
    private final int atNo;
    private final String name;
    private final double atWt;

    Element(int _atNo, String _name, double _atWt) {
        atNo = _atNo;
        name = _name;
        atWt = _atWt;
    }

    public int getAtNo() {
        return atNo;
    }

    public String getName() {
        return name;
    }

    public double getAtWt() {
        return atWt;
    }

    public static boolean isValidSymbol(String sym){
        for (Element el : Element.values())
        {
            if (el.toString().equals(sym))
            {
                return true;
            }
        }
        return false;
    }

    public static Element getSpecies(int atomicNo){
        for (Element el : Element.values())
        {
            if (el.getAtNo()==atomicNo)
            {
                return el;
            }
        }
        throw new IllegalArgumentException("Element for atomic number not found!");
    }

}