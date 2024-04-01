package catdata.ide;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JPanel;

import catdata.Unit;

public class LazyPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private volatile boolean lazyConstructorCalled = false;
  private final Function<Unit, JComponent> lazyConstructor;

  public LazyPanel(Function<Unit, JComponent> lazyConstructor) {
    super(new GridLayout(1, 1));
    this.lazyConstructor = lazyConstructor;
  }

  public void paint(Graphics g) {
    callLazyConstructor();
    super.paint(g);
  }

  public void paintAll(Graphics g) {
    callLazyConstructor();
    super.paintAll(g);
  }

  public void paintComponents(Graphics g) {
    callLazyConstructor();
    super.paintComponents(g);
  }

  public void repaint() {
    callLazyConstructor();

    super.repaint();
  }

  public void repaint(long l) {
    callLazyConstructor();
    super.repaint(l);
  }

  public void repaint(int i1, int i2, int i3, int i4) {
    callLazyConstructor();
    super.repaint(i1, i2, i3, i4);
  }

  public void repaint(long l, int i1, int i2, int i3, int i4) {
    callLazyConstructor();
    super.repaint(l, i1, i2, i3, i4);
  }

  public void update(Graphics g) {
    callLazyConstructor();
    super.update(g);
  }

  public synchronized final void callLazyConstructor() {
    if ((lazyConstructorCalled == false) && (getParent() != null)) {
      add(lazyConstructor.apply(Unit.unit));
      lazyConstructorCalled = true;
      validate();
    }
  }

}
