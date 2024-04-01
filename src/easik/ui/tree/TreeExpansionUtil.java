package easik.ui.tree;

//~--- JDK imports ------------------------------------------------------------

import java.util.StringTokenizer;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Static utility methods for describing the expansion state of a JTree Idea
 * borrowed from http://www.javalobby.org/java/forums/t19857.html
 */
public class TreeExpansionUtil {
  // is path1 descendant of path2

  /**
   *
   *
   * @param path1
   * @param path2
   *
   * @return
   */
  public static boolean isDescendant(TreePath path1, TreePath path2) {
    int count1 = path1.getPathCount();
    int count2 = path2.getPathCount();

    if (count1 <= count2) {
      return false;
    }

    while (count1 != count2) {
      path1 = path1.getParentPath();

      count1--;
    }

    return path1.equals(path2);
  }

  /**
   *
   *
   * @param tree
   * @param row
   *
   * @return
   */
  public static String getExpansionState(JTree tree, int row) {
    TreePath rowPath = tree.getPathForRow(row);
    StringBuffer buf = new StringBuffer();
    int rowCount = tree.getRowCount();

    for (int i = row; i < rowCount; i++) {
      TreePath path = tree.getPathForRow(i);

      if ((i == row) || isDescendant(path, rowPath)) {
        if (tree.isExpanded(path)) {
          buf.append("," + String.valueOf(i - row));
        }
      } else {
        break;
      }
    }

    return buf.toString();
  }

  /**
   *
   *
   * @param tree
   * @param row
   * @param expansionState
   */
  public static void restoreExpansionState(JTree tree, int row, String expansionState) {
    StringTokenizer stok = new StringTokenizer(expansionState, ",");

    while (stok.hasMoreTokens()) {
      int token = row + Integer.parseInt(stok.nextToken());

      tree.expandRow(token);
    }
  }
}
