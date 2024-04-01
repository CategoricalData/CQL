package catdata.cql;

public class Comment implements Semantics {

  @Override
  public int size() {
    return 0;
  }

  public final String comment;
  public final boolean isMarkdown;

  public Comment(String comment, boolean isMarkdown) {
    this.comment = comment;
    this.isMarkdown = isMarkdown;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((comment == null) ? 0 : comment.hashCode());
    result = prime * result + (isMarkdown ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Comment other = (Comment) obj;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    if (isMarkdown != other.isMarkdown)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return comment.trim();
  }

  @Override
  public Kind kind() {
    return Kind.COMMENT;
  }

}
