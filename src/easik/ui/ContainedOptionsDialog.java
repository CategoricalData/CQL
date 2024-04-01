package easik.ui;

/**
 * Implementing this class tells Easik to call dialogClosed() when the dialog
 * has been closed. It is generally implemented by dialogs that wish to handle
 * all their options themselves, instead of returning options to calling classes
 * (the name "contained" implies that this is a "self-contained" object, i.e.,
 * it does everything itself).
 */
public interface ContainedOptionsDialog {
  /**
   * Called when the user accepts or dismisses the dialog.
   *
   * @param accepted will be true if the user clicked OK and the data was verified
   *                 successfully; false if the user cancelled or closed the
   *                 dialog.
   */
  public void accepted(boolean accepted);
}
