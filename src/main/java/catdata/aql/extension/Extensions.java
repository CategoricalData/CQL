package catdata.aql.extension;

import javax.swing.JOptionPane;

import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.IAqlParser;
import catdata.aql.gui.AqlCodeEditor;

public class Extensions {

  public void visualEdit(AqlCodeEditor editor) {
    JOptionPane.showMessageDialog(null, "Upgrade to Conexus CQL for visual editing.");
  }

  public void deploy(AqlCodeEditor editor) {
    JOptionPane.showMessageDialog(null, "Upgrade to Conexus CQL to deploy to SQL, Hadoop, Java, Spark, and more.");
  }

  public static Extensions extensions;

  public synchronized static Extensions getExtensions() {
    if (extensions != null) {
      return extensions;
    }
    try {
      Class<?> c = Class.forName("ai.conexus.ConexusExtensions");
      extensions = (Extensions) c.getConstructors()[0].newInstance();
    } catch (Exception ex) {
      extensions = new Extensions();
    }
    return extensions;
  }
}
