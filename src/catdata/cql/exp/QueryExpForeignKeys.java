package catdata.cql.exp;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Eq;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Query.Agg;
import catdata.cql.Schema;
import catdata.cql.Term;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The query that gives a schema of attributes only, such as an import gives, the foreign keys its
 * columns stand for (issue 61).
 *
 * <pre>
 * query Q = foreign_keys S {
 *   seen_rec : Observed -> Record where o_unit = c_unit  o_habitat = c_habitat
 * }
 * </pre>
 *
 * <p>Each declaration names a foreign key, the entity it leaves, the entity it reaches, and the
 * columns that must agree: on the left a column of the first entity, on the right a column of the
 * second. The query goes from {@code S} to {@code S} with the declared foreign keys added, and
 * keeps every entity and attribute of {@code S}. A row of an entity is joined, on those columns,
 * with the rows its foreign keys reach and with the rows those reach in turn, so the declared
 * foreign keys must not form a cycle. A row with no row to reach is left out, and a row that
 * reaches two rows by one foreign key gives two rows: the columns on the right are meant to be a
 * key of their entity.
 *
 * <p>Up to the columns it keeps, this is David Spivak's construction: Pi along the mapping that
 * sends each column on the left to the path through its foreign key to the column on the right. A
 * block has a variable for each path of foreign keys from its entity, and its where clause says
 * that the columns of each declaration agree.
 *
 * <p>Paths are taken as written. The schema may not have foreign keys or equations of its own, and
 * the declarations take no equations, so in the target no two paths are equal and none is proved
 * equal to another. Declarations that branch and meet again give a block one variable for each way:
 * with A -> B -> D and A -> C -> D the block of A has two variables of D, and a chain of k such
 * diamonds gives 2^k. With keys that are keys the rows do not multiply, the joins do. A cycle would
 * have infinitely many paths, so it is refused before anything is unfolded.
 */
public class QueryExpForeignKeys extends QueryExp {

  /** One declared foreign key: its name, the entities it joins, and the columns that agree. */
  public static final class Key {
    public final String fk;
    public final String from;
    public final String to;
    public final List<Pair<String, String>> on;

    public Key(String fk, String from, String to, List<Pair<String, String>> on) {
      this.fk = fk;
      this.from = from;
      this.to = to;
      this.on = Collections.unmodifiableList(new ArrayList<>(on));
    }

    @Override
    public int hashCode() {
      return Objects.hash(fk, from, to, on);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Key)) {
        return false;
      }
      Key other = (Key) obj;
      return fk.equals(other.fk)
          && from.equals(other.from)
          && to.equals(other.to)
          && on.equals(other.on);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(fk + " : " + from + " -> " + to + " where");
      for (Pair<String, String> p : on) {
        sb.append(" ").append(p.first).append(" = ").append(p.second);
      }
      return sb.toString();
    }
  }

  public final SchExp sch;
  public final List<Key> keys;
  public final Map<String, String> options;

  public QueryExpForeignKeys(SchExp sch, List<Key> keys, List<Pair<String, String>> options) {
    this.sch = sch;
    this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
    this.options = Util.toMapSafely(options);
  }

  /**
   * The checks that need no schema, made when the query is typed: no entity declares a foreign key
   * twice, and every declaration names its columns. As in CQL's schemas, two entities may each have
   * a foreign key of the same name.
   */
  private void checkDeclarations() {
    Set<Pair<String, String>> names = new HashSet<>();
    for (Key k : keys) {
      if (!names.add(new Pair<>(k.from, k.fk))) {
        throw new RuntimeException(
            "foreign_keys: " + k.fk + " is declared twice for " + k.from + ".");
      }
      if (k.on.isEmpty()) {
        throw new RuntimeException("foreign_keys: " + k.fk + " names no columns.");
      }
    }
  }

  /** The target: the schema with the declared foreign keys added. */
  public SchExp cod() {
    checkDeclarations();
    List<Pair<String, Pair<String, String>>> fks = new ArrayList<>(keys.size());
    for (Key k : keys) {
      fks.add(new Pair<>(k.fk, new Pair<>(k.from, k.to)));
    }
    return new SchExpRaw(
        new TyExpSch(sch),
        Collections.singletonList(sch),
        Collections.emptyList(),
        fks,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        null,
        null);
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    sch.type(G);
    return new Pair<>(sch, cod());
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    sch.map(f);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.dont_validate_unsafe);
    set.addAll(AqlOptions.proverOptionNames());
  }

  @Override
  protected Map<String, String> options() {
    return options;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return sch.deps();
  }

  /** A variable of a block: the entity it ranges over, and the path of foreign keys to it. */
  private static final class Node {
    final String en;
    final List<String> path;
    final String var;
    final Node parent;
    final Key by;
    final List<Node> children = new ArrayList<>();

    Node(String en, List<String> path, String var, Node parent, Key by) {
      this.en = en;
      this.path = path;
      this.var = var;
      this.parent = parent;
      this.by = by;
    }
  }

  /**
   * The declared foreign keys must not lead back to an entity they leave, since a block joins each
   * row with every row they reach. The cycle reported is the first met from the declarations, in
   * the order they are written.
   */
  private void acyclic(Map<String, List<Key>> out) {
    Set<String> done = new HashSet<>();
    for (Key k : keys) {
      acyclic(k.from, out, done, new LinkedList<>());
    }
  }

  private static void acyclic(
      String en, Map<String, List<Key>> out, Set<String> done, LinkedList<String> path) {
    if (path.contains(en)) {
      List<String> cycle = new ArrayList<>(path.subList(path.indexOf(en), path.size()));
      cycle.add(en);
      throw new RuntimeException(
          "foreign_keys: the declared foreign keys form a cycle, "
              + Util.sep(cycle, " -> ")
              + ". A block of the query joins each row with every row its foreign keys reach, so"
              + " they cannot lead back to an entity they leave.");
    }
    if (done.contains(en)) {
      return;
    }
    path.addLast(en);
    for (Key k : out.getOrDefault(en, Collections.emptyList())) {
      acyclic(k.to, out, done, path);
    }
    path.removeLast();
    done.add(en);
  }

  /** The variables of the block of {@code en}: one for it, and one for each path from it. */
  private static Node unfold(String en, Map<String, List<Key>> out) {
    int[] n = {0};
    return unfold(en, Collections.emptyList(), null, null, out, n);
  }

  private static Node unfold(
      String en, List<String> path, Node parent, Key by, Map<String, List<Key>> out, int[] n) {
    Node node = new Node(en, path, "v" + n[0]++, parent, by);
    for (Key k : out.getOrDefault(en, Collections.emptyList())) {
      List<String> p = new ArrayList<>(path);
      p.add(k.fk);
      node.children.add(unfold(k.to, p, node, k, out, n));
    }
    return node;
  }

  private static void collect(Node node, List<Node> into) {
    into.add(node);
    for (Node c : node.children) {
      collect(c, into);
    }
  }

  private static Node at(Node root, List<String> path) {
    Node node = root;
    for (String fk : path) {
      Node next = null;
      for (Node c : node.children) {
        if (c.by.fk.equals(fk)) {
          next = c;
          break;
        }
      }
      if (next == null) {
        return Util.anomaly();
      }
      node = next;
    }
    return node;
  }

  /** Every declaration names entities and columns of the schema, of the same type on both sides. */
  private void check(Schema<String, String, Sym, Fk, Att> src) {
    if (!src.fks.isEmpty()) {
      List<String> fks = new ArrayList<>(src.fks.size());
      for (Fk fk : src.fks.keySet()) {
        fks.add(fk.str + " : " + src.fks.get(fk).first + " -> " + src.fks.get(fk).second);
      }
      throw new RuntimeException(
          "foreign_keys: "
              + sch
              + " has foreign keys already ("
              + Util.sep(fks, ", ")
              + "). This keyword takes a schema of attributes only, such as an import gives.");
    }
    if (!src.eqs.isEmpty()) {
      throw new RuntimeException(
          "foreign_keys: "
              + sch
              + " has equations. This keyword takes a schema of attributes only, such as an import"
              + " gives.");
    }
    for (Key k : keys) {
      for (String en : new String[] {k.from, k.to}) {
        if (!src.ens.contains(en)) {
          throw new RuntimeException(
              "foreign_keys: in " + k.fk + ", " + en + " is not an entity of " + sch + ".");
        }
      }
      if (src.atts.containsKey(Att.Att(k.from, k.fk))) {
        throw new RuntimeException(
            "foreign_keys: "
                + k.fk
                + " is an attribute of "
                + k.from
                + " already. A foreign key needs a name of its own, or a path through it could"
                + " mean either.");
      }
      for (Pair<String, String> p : k.on) {
        Att a = Att.Att(k.from, p.first);
        Att b = Att.Att(k.to, p.second);
        if (!src.atts.containsKey(a)) {
          throw new RuntimeException(
              "foreign_keys: in "
                  + k.fk
                  + ", "
                  + p.first
                  + " is not an attribute of "
                  + k.from
                  + ".");
        }
        if (!src.atts.containsKey(b)) {
          throw new RuntimeException(
              "foreign_keys: in "
                  + k.fk
                  + ", "
                  + p.second
                  + " is not an attribute of "
                  + k.to
                  + ".");
        }
        String ta = src.atts.get(a).second;
        String tb = src.atts.get(b).second;
        if (!ta.equals(tb)) {
          throw new RuntimeException(
              "foreign_keys: in "
                  + k.fk
                  + ", "
                  + k.from
                  + "."
                  + p.first
                  + " is of type "
                  + ta
                  + " and "
                  + k.to
                  + "."
                  + p.second
                  + " of type "
                  + tb
                  + ".");
        }
      }
    }
  }

  @Override
  protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(
      AqlEnv env, boolean isCompileTime) {
    Schema<String, String, Sym, Fk, Att> src = sch.eval(env, isCompileTime);
    check(src);
    Schema<String, String, Sym, Fk, Att> dst = cod().eval(env, isCompileTime);
    AqlOptions ops = new AqlOptions(options, env.defaults);

    Map<String, List<Key>> out = new LinkedHashMap<>();
    for (Key k : keys) {
      out.computeIfAbsent(k.from, x -> new ArrayList<>()).add(k);
    }
    acyclic(out);
    Map<String, Node> roots = new LinkedHashMap<>();
    for (String en : src.ens) {
      roots.put(en, unfold(en, out));
    }

    Map<
            String,
            Triple<
                LinkedHashMap<String, Chc<String, String>>,
                Collection<Eq<String, String, Sym, Fk, Att, String, String>>,
                AqlOptions>>
        ens = new LinkedHashMap<>();
    for (String en : src.ens) {
      List<Node> nodes = new ArrayList<>();
      collect(roots.get(en), nodes);
      LinkedHashMap<String, Chc<String, String>> from = new LinkedHashMap<>();
      List<Eq<String, String, Sym, Fk, Att, String, String>> where = new LinkedList<>();
      for (Node node : nodes) {
        from.put(node.var, Chc.inLeft(node.en));
        if (node.parent != null) {
          for (Pair<String, String> p : node.by.on) {
            Term<String, String, Sym, Fk, Att, String, String> l =
                Term.Att(Att.Att(node.parent.en, p.first), Term.Gen(node.parent.var));
            Term<String, String, Sym, Fk, Att, String, String> r =
                Term.Att(Att.Att(node.en, p.second), Term.Gen(node.var));
            where.add(new Eq<>(null, l, r));
          }
        }
      }
      ens.put(en, new Triple<>(from, where, ops));
    }

    Map<
            Att,
            Chc<
                Term<String, String, Sym, Fk, Att, String, String>,
                Agg<String, String, Sym, Fk, Att>>>
        atts = new LinkedHashMap<>();
    for (Att att : dst.atts.keySet()) {
      String en = dst.atts.get(att).first;
      atts.put(att, Chc.inLeft(Term.Att(att, Term.Gen(roots.get(en).var))));
    }

    Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks =
        new LinkedHashMap<>();
    for (Key k : keys) {
      Node child = at(roots.get(k.from), Collections.singletonList(k.fk));
      List<Node> theirs = new ArrayList<>();
      collect(roots.get(k.to), theirs);
      Map<String, Term<Void, String, Void, Fk, Void, String, Void>> g = new LinkedHashMap<>();
      for (Node node : theirs) {
        g.put(node.var, Term.Gen(at(child, node.path).var));
      }
      fks.put(Fk.Fk(k.from, k.fk), new Pair<>(g, ops));
    }

    return new Query<>(
        Collections.emptyMap(),
        Collections.emptyMap(),
        ens,
        atts,
        fks,
        Collections.emptyMap(),
        src,
        dst,
        ops);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sch, keys, options);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    QueryExpForeignKeys other = (QueryExpForeignKeys) obj;
    return sch.equals(other.sch) && keys.equals(other.keys) && options.equals(other.options);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("foreign_keys " + sch + " {");
    for (Key k : keys) {
      sb.append("\n\t").append(k);
    }
    if (!options.isEmpty()) {
      sb.append("\n\toptions");
      for (Map.Entry<String, String> e : options.entrySet()) {
        sb.append("\n\t\t").append(e.getKey()).append(" = ").append(e.getValue());
      }
    }
    return sb.append("\n}").toString();
  }
}
