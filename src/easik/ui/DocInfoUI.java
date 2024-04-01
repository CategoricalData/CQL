package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import easik.DocumentInfo;
import easik.EasikConstants;
import easik.model.edge.ModelEdge.Cascade;
import easik.sketch.Sketch;

/**
 * Create a new User Interface for input of the document information for either
 * an overview or a sketch.
 *
 * Document information includes the name of the sketch/overview, the authors of
 * the sketch/overview, a short description of the sketch, and both the creation
 * and last modification dates. Users can manually edit the name of the sketch,
 * the authors, and the description. Creation date and last modification date
 * are displayed, but are uneditable.
 */
public class DocInfoUI extends OptionsDialog implements ContainedOptionsDialog {
  private static final long serialVersionUID = 5956751912002554276L;

  /** The frame this UI applies to */
  private EasikFrame _theFrame;

  /** The context of this document info */
  private InfoContext context;

  /**  */
  private JScrollPane description;

  /**
   * The document info object for which this UI applies
   */
  private DocumentInfo docInfo;

  /**  */
  private JComboBox<?> edgeCascading, edgeCascadingPartial;

  /** JWhatever used to edit the document information */
  private JTextField name, author;

  /**
   *
   */
  private enum InfoContext {
    OVERVIEW, SKETCH, VIEW
  }

  ;

  /**
   * Create a new instance of the document information dialog box.
   *
   * @param inFrame
   */
  public DocInfoUI(EasikFrame inFrame) {
    super(inFrame, "Document information");

    setSize(500, 400);

    _theFrame = inFrame;

    if (inFrame instanceof ApplicationFrame) {
      context = InfoContext.OVERVIEW;
      docInfo = ((ApplicationFrame) inFrame).getOverview().getDocInfo();
    } else if (inFrame instanceof SketchFrame) {
      context = InfoContext.SKETCH;

      setTitle("Sketch information");

      docInfo = ((SketchFrame) inFrame).getMModel().getDocInfo();
    } else {
      context = InfoContext.VIEW;

      setTitle("View information");

      docInfo = ((ViewFrame) inFrame).getMModel().getDocInfo();
    }

    showDialog();
  }

  /**
   *
   *
   * @return
   */
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public List<Option> getOptions() {
    LinkedList<Option> opts = new LinkedList<>();

    // Add a title:
    if (context == InfoContext.OVERVIEW) {
      opts.add(new Option.Title("Document information"));
    } else if (context == InfoContext.SKETCH) {
      opts.add(new Option.Title("Sketch information"));
    } else {
      opts.add(new Option.Title("View information"));
    }

    // Add the name field
    opts.add(new Option(new JLabel("Title"), name = JUtils.textField(docInfo.getName(), 15)));

    // Add the author field
    opts.add(new Option(new JLabel("Author(s)"), author = JUtils.textField(docInfo.getAuthorString(), 15)));

    // Add the description field
    opts.add(new Option(new JLabel("Description"), description = JUtils.textArea(docInfo.getDesc())));

    // add the time info
    Date created = docInfo.getCreationDate(), modified = docInfo.getModificationDate();
    String sCreated = (created == null) ? "N/A" : EasikConstants.DATETIME_LONG.format(created);
    String sModified = (modified == null) ? "N/A" : EasikConstants.DATETIME_LONG.format(modified);

    opts.add(new Option(new JLabel("Created"), new JLabel(sCreated)));
    opts.add(new Option(new JLabel("Last modified"), new JLabel(sModified)));

    // For a sketch, add the options for the default cascade mode for new
    // edges of this sketch
    if (context == InfoContext.SKETCH) {
      Sketch sketch = ((SketchFrame) _theFrame).getMModel();

      edgeCascading = new JComboBox(new String[] { "Restrict deletions", "Cascade deletions" });

      edgeCascading.setSelectedIndex((sketch.getDefaultCascading() == Cascade.CASCADE) ? 1 : 0);

      edgeCascadingPartial = new JComboBox(
          new String[] { "Set null", "Restrict deletions", "Cascade deletions" });

      edgeCascadingPartial.setSelectedIndex((sketch.getDefaultPartialCascading() == Cascade.CASCADE) ? 2
          : (sketch.getDefaultPartialCascading() == Cascade.RESTRICT) ? 1 : 0);

      JLabel cascadeLabel = new JLabel("Edge Cascading");
      String cascadeTT = JUtils.tooltip(
          "This option affects how new edges of this sketch are handled when exporting to a db.\n\n\"Cascade deletions\" cause deletions in one table to trigger deletions of any rows in other tables that point to the row(s) being deleted.\n\n\"Restrict deletions\" causes attempted deletions of referenced rows to fail.\n\nThis option will be used by default for any new normal or injective edges of this sketch.");

      cascadeLabel.setToolTipText(cascadeTT);
      edgeCascading.setToolTipText(cascadeTT);
      opts.add(new Option(cascadeLabel, edgeCascading));

      JLabel cascadePartialLabel = new JLabel("Partial Edge Cascading");
      String cascadePartialTT = JUtils.tooltip(
          "This option affects how EASIK creates partial edges when exporting to a db.\n\n\"Cascade deletions\" cause deletions in one table to trigger deletions of any rows in other tables that point to the row(s) being deleted.\n\n\"Restrict deletions\" cause attempted deletions of referenced rows to fail.\n\n\"Set null\" causes references to be set to NULL when the targeted row is deleted.\n\nThis option will be used by default for new partial edges of this sketch.");

      cascadePartialLabel.setToolTipText(cascadePartialTT);
      edgeCascadingPartial.setToolTipText(cascadePartialTT);
      opts.add(new Option(cascadePartialLabel, edgeCascadingPartial));
    }

    return opts;
  }

  /**
   * Called when the user clicks OK
   *
   * @param ok
   */
  @Override
  public void accepted(boolean ok) {
    if (!ok) {
      return;
    }

    // This call will setDirty() if anything has changed:
    docInfo.setAllInfo(name.getText(), author.getText(), JUtils.taText(description));

    if (context == InfoContext.SKETCH) {
      Sketch sketch = ((SketchFrame) _theFrame).getMModel();
      int cascadeIndex = edgeCascading.getSelectedIndex(), partialIndex = edgeCascadingPartial.getSelectedIndex();

      sketch.setDefaultCascading((cascadeIndex == 0) ? Cascade.RESTRICT : Cascade.CASCADE);
      sketch.setDefaultPartialCascading(
          (partialIndex == 0) ? Cascade.SET_NULL : (partialIndex == 1) ? Cascade.RESTRICT : Cascade.CASCADE);
      sketch.setDirty(); // Might already have happened above, but no big
                // deal.
    }
  }
}
