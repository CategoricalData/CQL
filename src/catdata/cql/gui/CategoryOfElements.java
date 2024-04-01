package catdata.cql.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import com.google.common.base.Function;

import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.ide.ProgressPanel;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;

public class CategoryOfElements<Node, Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

  Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

  public CategoryOfElements(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
    I = i;
  }

  private Pair<Graph<Pair<En, X>, Pair<Fk, Integer>>, Map<Pair<En, X>, Map<Att, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>> build() {

    Graph<Pair<En, X>, Pair<Fk, Integer>> g2 = new DirectedSparseMultigraph<>();
    Map<Pair<En, X>, Map<Att, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> map = new HashMap<>();

    // FinCat<Node, Term> c = i.thesig.toCategory2().first;
    for (En n : I.schema().ens) {
      for (X x : I.algebra().en(n)) {
        Pair<En, X> xx = new Pair<>(n, x);
        g2.addVertex(xx);

        Map<Att, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> m = new HashMap<>();
        for (Att attr : I.schema().attsFrom(n)) {
          m.put(attr, I.algebra().reprT(I.algebra().att(attr, x)));
        }
        map.put(xx, m);
      }
    }

    int j = 0;
    for (En n : I.schema().ens) {
      for (X x : I.algebra().en(n)) {
        for (Fk fk : I.schema().fksFrom(n)) {
          X y = I.algebra().fk(fk, x);
          g2.addEdge(new Pair<>(fk, j++), new Pair<>(n, x), new Pair<>(I.schema().fks.get(fk).second, y));
        }
      }
    }

    return new Pair<>(g2, map);
  }

  private JPanel doView(Color clr, Graph<Pair<En, X>, Pair<Fk, Integer>> sgv,
      Map<Pair<En, X>, Map<Att, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> map0) {
    JPanel cards = new JPanel(new CardLayout());

    Layout<Pair<En, X>, Pair<Fk, Integer>> layout = new FRLayout<>(sgv);
    layout.setSize(new Dimension(600, 400));
    VisualizationViewer<Pair<En, X>, Pair<Fk, Integer>> vv = new VisualizationViewer<>(layout);
    Function<Pair<En, X>, Paint> vertexPaint = i -> clr;
    DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
    gm.setMode(Mode.TRANSFORMING);
    vv.setGraphMouse(gm);
    gm.setMode(Mode.PICKING);
    vv.getRenderContext().setVertexLabelRenderer(new MyVertexT(cards));
    vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
    vv.getRenderContext().setEdgeLabelTransformer(t -> t.first.toString());

    JPanel ret = new JPanel(new GridLayout(1, 1));
    JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    for (Pair<En, X> n : sgv.getVertices()) {
      Map<Att, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> s = map0.get(n);
      Object[] columnNames = new Object[s.keySet().size()];
      Object[][] rowData = new Object[1][s.keySet().size()];

      int i = 0;
      for (Att a : s.keySet()) {
        columnNames[i] = a.toString();
        rowData[0][i] = s.get(a);
        i++;
      }
      JPanel p = new JPanel(new GridLayout(1, 1));
      JTable table = new JTable(rowData, columnNames);
      JScrollPane jsp = new JScrollPane(table);
      p.setBorder(
          BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Attributes for " + n.second));

      p.add(jsp);
      cards.add(p, n.second.toString());
    }
    cards.add(new JPanel(), "blank");
    CardLayout cl = (CardLayout) (cards.getLayout());
    cl.show(cards, "blank");

    pane.add(new GraphZoomScrollPane(vv));
    pane.add(cards);
    pane.setResizeWeight(.8d);
    ret.add(pane);

    return ret;
  }

  public JPanel makePanelLazy(String name, Color c) {
    JButton b = new JButton("Show elements");
    return new ProgressPanel(b, b, x -> makePanel(name, c));
  }

  private JPanel makePanel(String name, Color c) {
    try {
      JPanel ret;
      Pair<Graph<Pair<En, X>, Pair<Fk, Integer>>, Map<Pair<En, X>, Map<Att, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>> g = build();
      ret = g.first.getVertexCount() == 0 ? new JPanel() : doView(c, g.first, g.second);
      return ret;
    } catch (Exception e) {
      e.printStackTrace();
      JPanel p = new JPanel(new GridLayout(1, 1));
      JTextArea a = new JTextArea(e.getMessage());
      p.add(new JScrollPane(a));
      return p;
    }
  }

  private class MyVertexT implements VertexLabelRenderer {

    final JPanel cards;

    public MyVertexT(JPanel cards) {
      this.cards = cards;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3,
        T arg4) {
      Pair<En, X> p = (Pair<En, X>) arg4;
      if (arg3) {
        CardLayout c = (CardLayout) cards.getLayout();
        c.show(cards, p.second.toString());
      }

      return new JLabel(I.algebra().repr(p.first, p.second).toString());
    }
  }

}