package catdata.ide;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serializable;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import catdata.Pair;
import catdata.Unit;

public abstract class Options implements Serializable {

  private static final long serialVersionUID = 1L;

  public abstract String getName();

  public abstract Pair<JComponent, Function<Unit, Unit>> display();

  public abstract int size();

  protected static JPanel wrap(JTextField f) {
    GridBagConstraints c1 = new GridBagConstraints();
    c1.weightx = 1.0;
    c1.fill = GridBagConstraints.HORIZONTAL;

    JPanel p1 = new JPanel(new GridBagLayout());
    p1.add(f, c1);
    return p1;
  }

  public static int biggestSize = 0;
}
