package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import easik.Easik;
import easik.EasikConstants;
import easik.EasikSettings;

/**
 * The file chooser used in Easik. Rather than reimplementing the file chooser
 * code again and again, this handles all the common things done in Easik. In
 * particular, it sets the current directory to the default folder and, after a
 * file is chosen, sets the last used folder in the EasikSettings object. It
 * does all of this with single static methods calls; a FileChooser object never
 * needs to be constructed.
 *
 * Depending on the OS, we use different interfaces: we prefer FileDialog, but
 * because file filters don't work on it when running on Windows, and because it
 * isn't quite as annoying as it is on other OSes, we use JFileChooser on
 * Windows instead.
 */
public class FileChooser {
  /**
   *
   */
  public static enum Mode {
    LOAD, SAVE, DIRECTORY
  }

  ;

  /**
   * Shows a file chooser dialog for loading a file without any file filter.
   *
   * @param title the title of the file dialog
   * @return the File selected, or null if no file was selected.
   */
  public static File loadFile(String title) {
    return chooseFile(title, Mode.LOAD);
  }

  /**
   * Shows a file chooser dialog for loading a file with the specified FileFilter.
   * This is used with easik.ui.menu.FileFilter to filter by extension(s).
   *
   * @param title  the title of the file dialog
   * @param filter a FileFilter object specifying the filter
   * @return the File selected, or null if no file was selected.
   */
  public static File loadFile(String title, FileFilter filter) {
    return chooseFile(title, Mode.LOAD, filter);
  }

  /**
   * Shows a file chooser dialog for saving a file without any filter.
   *
   * @param title the title of the file dialog
   * @return the File entered, or null if no file was entered or Cancel was
   *         pressed.
   */
  public static File saveFile(String title) {
    return chooseFile(title, Mode.SAVE);
  }

  /**
   * Shows a file chooser dialog for loading a file with the specified FileFilter.
   * This is used with easik.ui.menu.FileFilter to filter by extension(s).
   *
   * @param title  the title of the file dialog
   * @param filter the FileFilter filtering the desired file type
   * @return the File entered, or null if no file was entered or Cancel was
   *         pressed.
   */
  public static File saveFile(String title, FileFilter filter) {
    return chooseFile(title, Mode.SAVE, filter);
  }

  /**
   * Shows a file chooser dialog for loading a file with the specified FileFilter,
   * and the specified default file extension. This is used with
   * easik.ui.menu.FileFilter to filter by extension(s).
   *
   * @param title     the title of the file dialog
   * @param filter    the FileFilter filtering the desired file type
   * @param extension the default extension to add to the filename if the filename
   *                  entered doesn't match the filter
   * @return the File entered, or null if no file was entered or Cancel was
   *         pressed.
   */
  public static File saveFile(String title, FileFilter filter, String extension) {
    return chooseFile(title, Mode.SAVE, filter, extension, null);
  }

  /**
   * Shows a file chooser dialog for loading a file with the specified FileFilter,
   * and the specified default file extension. This is used with
   * easik.ui.menu.FileFilter to filter by extension(s).
   *
   * @param title     the title of the file dialog
   * @param filter    the FileFilter filtering the desired file type
   * @param extension the default extension to add to the filename if the filename
   *                  entered doesn't match the filter
   * @param name      the default name used to save the file
   * @return the File entered, or null if no file was entered or Cancel was
   *         pressed.
   */
  public static File saveFile(String title, FileFilter filter, String extension, String name) {
    return chooseFile(title, Mode.SAVE, filter, extension, name);
  }

  /**
   * Shows a file chooser dialog that selects a directory.
   *
   * @param title the title of the file dialog
   * @return the File pointing to the directory selected
   */
  public static File directory(String title) {
    return chooseFile(title, Mode.DIRECTORY);
  }

  // Decides whether to use JFileChooser or AWT, and passed off to the
  // appropriate method

  /**
   *
   *
   * @param title
   * @param mode
   *
   * @return
   */
  private static File chooseFile(String title, Mode mode) {
    return chooseFile(title, mode, null);
  }

  /**
   *
   *
   * @param title
   * @param mode
   * @param filter
   *
   * @return
   */
  private static File chooseFile(String title, Mode mode, FileFilter filter) {
    return chooseFile(title, mode, filter, null, null);
  }

  /**
   *
   *
   * @param title
   * @param mode
   * @param filter
   * @param extension
   *
   * @return
   */
  private static File chooseFile(String title, Mode mode, FileFilter filter, String extension, String defaultName) {
    boolean useSwing = "swing".equals(System.getenv("EASIK_FILECHOOSER")) ? true
        : "awt".equals(System.getenv("EASIK_FILECHOOSER")) ? false : EasikConstants.RUNNING_ON_MAC ? false : // On
                                                            // Mac
                                                            // we
                                                            // always
                                                            // want
                                                            // to
                                                            // use
                                                            // AWT
                                                            // because
                                                            // JFileChooser
                                                            // is
                                                            // terrible
            true; // Otherwise, use JFileChooser
    File result = useSwing ? chooseFileSwing(title, mode, filter, defaultName) : chooseFileAWT(title, mode, filter);

    // User hit cancel
    if (result == null) {
      return null;
    }

    // If we have a file and a default extension was specified and the
    // filename doesn't match the filter, add the extension.
    if ((mode == Mode.SAVE) && (extension != null) && (filter != null) && !filter.accept(result)) {
      if (extension.startsWith(".")) {
        extension = extension.substring(1);
      }

      result = new File(result.getParentFile(), result.getName() + "." + extension);
    }

    // If we're saving a file, and the file already exists, prompt the user
    // to confirm
    // that they want to overwrite the file. If they don't, recurse back to
    // the choose file menu.
    // DON'T do this on OS X with AWT, because there the native control
    // already asks
    if ((!EasikConstants.RUNNING_ON_MAC || useSwing) && (mode == Mode.SAVE) && result.exists()) {
      int choice = JOptionPane.showConfirmDialog(null,
          "A file with this name already exists and will be overwritten.  Continue anyway?", "Warning",
          JOptionPane.WARNING_MESSAGE);

      if (choice == JOptionPane.CANCEL_OPTION) {
        return chooseFile(title, mode, filter, extension, defaultName);
      }
    }

    return result;
  }

  /*
   * AWT FileDialog implementation of the file chooser. This is because
   * JFileChooser doesn't work very well on non-Windows, but FileDialog doesn't
   * work on Windows (it doesn't support file filters on Windows).
   */

  /**
   *
   *
   * @param title
   * @param mode
   * @param filter
   *
   * @return
   */
  private static File chooseFileAWT(String title, Mode mode, FileFilter filter) {
    Easik e = Easik.getInstance();
    EasikSettings s = e.getSettings();
    FileDialog dialog = new FileDialog(e.getFrame(), title,
        (mode == Mode.SAVE) ? FileDialog.SAVE : FileDialog.LOAD);

    dialog.setDirectory(s.getDefaultFolder());

    if ((mode == Mode.DIRECTORY) && EasikConstants.RUNNING_ON_MAC) {
      System.setProperty("apple.awt.fileDialogForDirectories", "true");
    } else if (filter != null) {
      dialog.setFilenameFilter(filter);
    }

    // Show the dialog (this blocks until the user is done)
    dialog.setVisible(true);

    if ((mode == Mode.DIRECTORY) && EasikConstants.RUNNING_ON_MAC) {
      System.setProperty("apple.awt.fileDialogForDirectories", "false");
    }

    String filename = dialog.getFile();

    if (filename == null) {
      return null;
    }

    File selected = new File(dialog.getDirectory(), filename);

    if (mode != Mode.DIRECTORY) {
      s.setProperty("folder_last", selected.getParentFile().getAbsolutePath());
    }

    return selected;
  }

  /**
   *
   *
   * @param title
   * @param mode
   * @param filter
   *
   * @return
   */
  private static File chooseFileSwing(String title, Mode mode, FileFilter filter, String defaultName) {
    Easik e = Easik.getInstance();
    EasikSettings s = e.getSettings();
    JFileChooser dialog = new JFileChooser(s.getDefaultFolder());

    if (filter != null) {
      dialog.setFileFilter(filter);
    }

    int result;

    if (mode == Mode.DIRECTORY) {
      dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      result = dialog.showDialog(e.getFrame(), "Select folder");
    } else if (mode == Mode.SAVE) {
      if (defaultName != null) {
        dialog.setSelectedFile(new File(defaultName));
      }
      result = dialog.showSaveDialog(e.getFrame());
    } else {
      result = dialog.showOpenDialog(e.getFrame());
    }

    if (result != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    File selFile = dialog.getSelectedFile();

    if (mode != Mode.DIRECTORY) {
      s.setProperty("folder_last", selFile.getParentFile().getAbsolutePath());
    }

    return selFile;
  }
}
