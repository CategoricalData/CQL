
package easik.ui;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import easik.view.vertex.QueryNode;

/**
 * 
 * @author Sarah van der Laan
 *
 */

public class DefineQueryAddDialog extends OptionsDialog {

  private static final long serialVersionUID = 4044546505124282150L;

  /** The inputs for our fields */
  private JScrollPane insertInto;

  /** The query no which we are editing */
  private QueryNode ourNode;

  /**
   *
   * @param parent
   * @param title
   * @param inNode
   */
  public DefineQueryAddDialog(JFrame parent, String title, QueryNode inNode) {
    super(parent, title);

    setSize(300, 200);

    ourNode = inNode;

    showDialog();
  }

  /**
   *
   *
   * @return
   */
  @Override
  public List<Option> getOptions() {
    LinkedList<Option> opts = new LinkedList<>();

    // text????
    opts.add(new Option(new JLabel("'Insert Into' Statement"), insertInto = JUtils.textArea(ourNode.getUpdate())));

    return opts;
  }

  /**
   *
   *
   * @return
   */
  public String getUpdate() {
    return JUtils.taText(insertInto);
  }

}
