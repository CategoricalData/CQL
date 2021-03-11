package catdata.ide;

import java.util.Comparator;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import catdata.AlphanumComparator;

public class MyTableRowSorter extends TableRowSorter<TableModel> {

  public MyTableRowSorter(TableModel model) {
    super(model);
  }

  @Override
  protected boolean useToString(int c) {
    return false;
  }

  AlphanumComparator noc = new AlphanumComparator();
//    NaturalOrderComparator noc = new NaturalOrderComparator();

  @Override
  public Comparator<?> getComparator(int c) {
    return (o1, o2) -> {
      if (o1 instanceof Integer && o2 instanceof Integer) {
        return ((Integer) o1).compareTo((Integer) o2);
      }
      return noc.compare(o1.toString(), o2.toString());
    };
  }
}