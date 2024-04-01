package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Comment;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;

public class CommentExp extends Exp<Comment> {

  @Override
  public Optional<Chc<String, Object>> type(AqlTyping G) {
    return Optional.empty();
  }

  public static interface CommentExpVisitor<R, P, E extends Exception> {
    public abstract R visit(P params, CommentExp exp) throws E;
  }

  public static interface CommentExpCoVisitor<R, P, E extends Exception> {
    public abstract CommentExp visitCommentExp(P params, R exp) throws E;
  }

  public <R, P, E extends Exception> R accept(P params, CommentExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  public final String s;
  public final boolean isM;

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isM ? 1231 : 1237);
    result = prime * result + ((s == null) ? 0 : s.hashCode());
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
    CommentExp other = (CommentExp) obj;
    if (isM != other.isM)
      return false;
    if (s == null) {
      if (other.s != null)
        return false;
    } else if (!s.equals(other.s))
      return false;
    return true;
  }

  public CommentExp(String s, boolean isM) {
    this.s = s;
    this.isM = isM;
  }

  @Override
  public Kind kind() {
    return Kind.COMMENT;
  }

  @Override
  public synchronized Comment eval0(AqlEnv env, boolean isC) {
    return new Comment(s, isM);
  }

  @Override
  public String toString() {
    return s;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Collections.emptyList();
  }

  @Override
  public Exp<Comment> Var(String v) {
    return Util.anomaly();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {

  }

}
