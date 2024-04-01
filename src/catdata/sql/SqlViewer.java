package catdata.sql;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import com.google.common.base.Function;

import catdata.Chc;
import catdata.ide.GuiUtil;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;

class SqlViewer extends JPanel {

  private static final long serialVersionUID = 1L;

  private final Graph<Chc<SqlType, SqlTable>, Chc<SqlColumn, SqlForeignKey>> graph = new DirectedSparseMultigraph<>();
  private final SqlSchema info;
  private final SqlInstance inst;
  private final Color color;

  private final CardLayout cards = new CardLayout();
  private final JPanel bottom = new JPanel(cards);
  private JPanel top;

  public SqlViewer(Color color, SqlSchema info, SqlInstance inst) {
    super(new GridLayout(1, 1));
    this.info = info;
    this.inst = inst;
    this.color = color;

    makeCards();
    makeGraph();
    makeUI();

    if (inst == null) {
      add(top);
    } else {
      JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      pane.setResizeWeight(.75);
      pane.setDividerSize(2);
      pane.add(top);
      pane.add(bottom);
      add(pane);
    }
    setBorder(BorderFactory.createEtchedBorder());
  }

  private void makeCards() {
    if (inst == null) {
      return;
    }
    bottom.add(new JPanel(), "");
    cards.show(bottom, "");

    for (SqlTable table : info.tables) {
      List<String> colNames = table.columns.stream().map(x -> x.name).collect(Collectors.toList());
      String[][] rowData = new String[inst.get(table).size()][colNames.size()];

      int r = 0;
      for (Map<SqlColumn, Optional<Object>> row : inst.get(table)) {
        int c = 0;
        for (SqlColumn column : table.columns) {
          Optional<Object> x = row.get(column);
          if (x == null) {
            throw new RuntimeException();
          }
          String y = x.isPresent() ? x.get().toString() : "NULL";
          rowData[r][c] = y;
          c++;
        }
        r++;
      }

      JPanel panel = GuiUtil.makeTable(BorderFactory.createEmptyBorder(),
          table.name + " (" + inst.get(table).size() + ")", rowData, colNames.toArray());
      bottom.add(panel, table.name);
    }

  }

  private void makeGraph() {
    for (SqlType ty : info.types) {
      graph.addVertex(Chc.inLeftNC(ty));
    }
    for (SqlTable table : info.tables) {
      graph.addVertex(Chc.inRightNC(table));
      for (SqlColumn column : table.columns) {
        graph.addEdge(Chc.inLeftNC(column), Chc.inRightNC(table), Chc.inLeftNC(column.type));
      }
    }
   // for (SqlForeignKey fk : info.fks) {
   //   graph.addEdge(Chc.inRightNC(fk), Chc.inRightNC(fk.source), Chc.inRightNC(fk.target));
   // }
  }

  private void makeUI() {
    if (graph.getVertexCount() == 0) {
      top = new JPanel();
      return;
    }
    Layout<Chc<SqlType, SqlTable>, Chc<SqlColumn, SqlForeignKey>> layout = new FRLayout<>(graph);
    layout.setSize(new Dimension(600, 400));
    VisualizationViewer<Chc<SqlType, SqlTable>, Chc<SqlColumn, SqlForeignKey>> vv = new VisualizationViewer<>(
        layout);
    Function<Chc<SqlType, SqlTable>, Paint> vertexPaint = x -> x.left ? UIManager.getColor("Panel.background")
        : color;
    DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
    gm.setMode(Mode.TRANSFORMING);
    vv.setGraphMouse(gm);
    gm.setMode(Mode.PICKING);

    vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

    Function<Chc<SqlType, SqlTable>, String> vt = x -> x.left ? x.l.name : x.r.name.toString();

    Function<Chc<SqlColumn, SqlForeignKey>, String> et = x -> x.left ? x.l.name : "";

    vv.getRenderContext().setVertexLabelTransformer(vt);
    vv.getRenderContext().setEdgeLabelTransformer(et);
    top = new GraphZoomScrollPane(vv);

    if (inst == null) {
      return;
    }

    vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
      if (e.getStateChange() != ItemEvent.SELECTED) {
        return;
      }
      vv.getPickedEdgeState().clear();
      @SuppressWarnings("unchecked")
      Chc<SqlType, SqlTable> x1 = (Chc<SqlType, SqlTable>) e.getItem();
      if (x1.left) {
        return;
      }
      cards.show(bottom, x1.r.name.first + "." + x1.r.name.second);
    });

  }

}
