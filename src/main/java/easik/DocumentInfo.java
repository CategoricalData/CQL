package easik;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import easik.overview.Overview;
import easik.ui.ApplicationFrame;
import easik.ui.EasikFrame;
import easik.ui.SketchFrame;
import easik.ui.ViewFrame;

/**
 * Stores information about the current sketch. This information includes the
 * name of the sketch, the authors of sketch, a description, and creation/last
 * modification dates. <br>
 * Information about dates is created automatically, while other information is
 * provided by the user through a <b>DocInfoUI</b> object.
 */
public class DocumentInfo {
  /** The description of this document */
  private String _desc = "";

  /** The name of this document */
  private String _name = "";

  /** The authors of this document */
  private ArrayList<String> _authors = new ArrayList<>();

  /** The creation date of this document */
  private Date _creationDate;

  /** The last modification date of this document */
  private Date _lastMod;

  /** The frame for which this is its document info */
  private EasikFrame _theFrame;

  /**
   * Standard constructor with empty strings for all document information.
   *
   * @param inFrame
   */
  public DocumentInfo(EasikFrame inFrame) {
    _theFrame = inFrame;

    setCreationDate(null);
    updateModificationDate();
  }

  /**
   * Resests the fields of this document info. Doesn't touch the name field.
   */
  public void reset() {
    _authors = new ArrayList<>();
    _desc = "";

    setCreationDate(null);
    updateModificationDate();
  }

  /**
   * Gets the name of this sketch
   * 
   * @return The name of this sketch
   */
  public String getName() {
    return _name;
  }

  /**
   * Sets the name of this sketch in the information tree, document info, and the
   * node representing it in the overview
   *
   * @param name The name of this sketch
   */
  public void setName(String name) {
    _theFrame.setTreeName(name);

    _name = name;

    if (_theFrame instanceof ApplicationFrame) {
      ((ApplicationFrame) _theFrame).updateTitle();
    } else if (_theFrame instanceof SketchFrame) {
      ((SketchFrame) _theFrame).setEasikTitle();
      ((SketchFrame) _theFrame).getMModel().getOverview().refresh();
    } else if (_theFrame instanceof ViewFrame) {
      // ((ViewFrame)_theFrame).setTitle(name);
      ((ViewFrame) _theFrame).setEasikTitle();
      ((ViewFrame) _theFrame).getMModel().getOverview().refresh();
    }
  }

  /**
   * Gets the authors of this sketch/overview
   * 
   * @return An array list of the authors
   */
  public List<String> getAuthors() {
    return Collections.unmodifiableList(_authors);
  }

  /**
   * Returns a single string of the authors, with commas acting as separators
   *
   * @return A single string of all authors
   */
  public String getAuthorString() {
    if (_authors.size() != 0) {
      String author = "";

      for (String aut : _authors) {
        author += aut + ", ";
      }

      // remove last comma and space.
      author = author.substring(0, author.length() - 2);

      return author;
    }
    return "";

  }

  /**
   * Adds an author to the list of authors.
   * 
   * @param author The authors name
   */
  public void addAuthor(String author) {
    _authors.add(author);
  }

  /**
   * Gets the description of the current sketch
   * 
   * @return The description of the sketch
   */
  public String getDesc() {
    return _desc;
  }

  /**
   * Sets the description of the current sketch
   * 
   * @param desc The description of the sketch
   */
  public void setDesc(String desc) {
    _desc = desc;
  }

  /**
   * Gets the creation date.
   * 
   * @return The creation date, or <code>null</code> if no creation date has been
   *         set
   */
  public Date getCreationDate() {
    return _creationDate;
  }

  /**
   * Sets the creation date.
   *
   * @param date The date of creation of this sketch, based on the first save. If
   *             null, the current date/time will be used.
   */
  public void setCreationDate(Date date) {
    _creationDate = (date == null) ? new Date() : (Date) date.clone();
  }

  /**
   * Gets the last modification date.
   * 
   * @return The last modification date, or <code>null</code> if the last
   *         modification date has not been set.
   */
  public Date getModificationDate() {
    return _lastMod;
  }

  /**
   * Sets the last modification date.
   * 
   * @param date The date of the last modification, or null if the last
   *             modification date should be updated to the current date and time.
   */
  public void setModificationDate(Date date) {
    _lastMod = (date == null) ? new Date() : (Date) date.clone();
  }

  /**
   * Updates the last modification date to the current date/time.
   */
  public void updateModificationDate() {
    _lastMod = new Date();
  }

  /**
   * Sets all editable information, with the parameters determined by the user. If
   * any parameters have been changed, the sketch is set to dirty so user will be
   * prompted for a save if an attempt to discard the current sketch is made
   * before a save. If a name change results in a conflict, we add numbers. If no
   * name is specified, we keep the old name.
   * 
   * @param name   The name of the sketch
   * @param author The string of all authors of the sketch
   * @param desc   The description of the sketch
   */
  public void setAllInfo(String name, String author, String desc) {
    name = name.trim();

    if (!_name.equals(name) || !getAuthorString().equals(author) || !_desc.equals(desc)) {
      Overview overview = _theFrame.getOverview();

      if (_theFrame instanceof SketchFrame) {
        ((SketchFrame) _theFrame).getNode().setName(name);
        ((SketchFrame) _theFrame).getMModel().setDirty();
      } else if (_theFrame instanceof ViewFrame) {
        ((ViewFrame) _theFrame).getNode().setName(name);
        ((ViewFrame) _theFrame).getMModel().setDirty();
      } else {
        overview.setDirty(true); // The Sketch and View setDirty()'s
                      // will make this happen anyway
      }

      if (!name.equals("")) {
        setName(name);
      }

      _authors = new ArrayList<>();

      for (String aut : author.split(",")) {
        aut = aut.trim();

        if (!aut.equals("")) {
          _authors.add(aut);
        }
      }

      _desc = desc;
    }
  }
}
