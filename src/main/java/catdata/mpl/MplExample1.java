package catdata.mpl;

import catdata.ide.Example;
import catdata.ide.Language;

public class MplExample1 extends Example {

  @Override
  public Language lang() {
    return null; // Language.MPL;
  }

  @Override
  public String getName() {
    return "Example 1";
  }

  @Override
  public String getText() {
    return s;
  }

  private final String s = "T = theory {" + "\n  sorts" + "\n    A, B, C;" + "\n  symbols"
      + "\n    e : (A*C) -> (B*C), " + "\n    f : A -> B," + "\n    g : A -> C,"
      + "\n    h : (A*A) -> (B*C)," + "\n    i : I -> I;" + "\n  equations" + "\n    h = (f*g),"
      + "\n    (h*f) = (h*f)," + "\n    (rho1 A * id B) = (alpha1 A I B ; (id A * lambda1 B)),"
      + "\n    tr (id A * id B) = id A;" + "\n}" + "\n" + "\nf = eval T f" + "\nh = eval T h"
      + "\ni = eval T i" + "\n" + "\nfg = eval T (f*g)" + "\nidA = eval T id A"
      + "\nidAidA = eval T (id A ; id A)" + "\ntre = eval T tr e" + "\n" + "\nS = theory {" + "\n  sorts"
      + "\n    X;" + "\n  symbols" + "\n    F : (X*X) -> (X*X)," + "\n    G : (X*X) -> (X*X);"
      + "\n  equations;" + "\n}" + "\n"
      + "\nf = eval S tr ((((F * id X) ; alpha1 X X X); (id X * G)) ; (alpha2 X X X ; (sym X X * id X)))" + "\n";

}
