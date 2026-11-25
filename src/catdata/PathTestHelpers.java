package catdata;

/** Helpers for tests that write a file path into a CQL program. */
public final class PathTestHelpers {

  private PathTestHelpers() {}

  /**
   * The path as it goes between the double quotes of a CQL string. CQL reads a backslash as an
   * escape, so a Windows path keeps its backslashes only when they are doubled.
   */
  public static String inCqlString(Object path) {
    return path.toString().replace("\\", "\\\\");
  }
}
