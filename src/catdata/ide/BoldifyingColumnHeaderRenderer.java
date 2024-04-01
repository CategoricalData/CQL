package catdata.ide;

import java.awt.Component;
import java.awt.Font;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class BoldifyingColumnHeaderRenderer extends JLabel implements TableCellRenderer {

  private final Collection<?> boldify;
  private final Font normal = UIManager.getFont("TableHeader.font");
  private final Font bold = normal.deriveFont(Font.BOLD);
  private final TableCellRenderer r; // = new DefaultTableCellRenderer();

  public BoldifyingColumnHeaderRenderer(Collection<?> boldify, TableCellRenderer r) {
    this.boldify = boldify;
    this.r = r;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    JLabel ret = (JLabel) r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    if (boldify.contains(value)) {
      ret.setFont(bold);
    } else {
      ret.setFont(normal);
    }
    return ret;
  }

}