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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
 * <p>or, with path equations written as in a schema:
 *
 * <pre>
 * query Q = foreign_keys S {
 *   next : Step -> Step where s_next = s_id
 *   path_equations Step.next.next.next = Step.next.next
 * }
 * </pre>
 *
 * <p>Each declaration names a foreign key, the entity it leaves, the entity it reaches, and the
 * columns that must agree: on the left a column of the first entity, on the right a column of the
 * second. The query goes from {@code S} to {@code S} with the declared foreign keys added, and
 * keeps every entity and attribute of {@code S}. A row of an entity is joined, on those columns,
 * with the rows its foreign keys reach and with the rows those reach in turn. A row with no row to
 * reach is left out, and a row that reaches two rows by one foreign key gives two rows: the columns
 * on the right are meant to be a key of their entity.
 *
 * <p>Up to the columns it keeps, this is David Spivak's construction: Pi along the mapping that
 * sends each column on the left to the path through its foreign key to the column on the right. A
 * block has a variable for each path of foreign keys from its entity, up to the path equations, and
 * its where clause says that the columns of each declaration agree. Prefixes are shared: the
 * variable of a path hangs off the variable of the path one foreign key shorter.
 *
 * <p>Without path equations no two paths are equal. Declarations that branch and meet again give a
 * block one variable for each way: with A -> B -> D and A -> C -> D the block of A has two
 * variables of D, and a chain of k such diamonds gives 2^k. With keys that are keys the rows do not
 * multiply, the joins do. A cycle would have infinitely many paths, so it is refused before
 * anything is unfolded.
 *
 * <p>With path equations, paths are compared up to the equations by the target schema's prover, as
 * the schema counts its own paths ({@link Schema#pathsFrom}, the paths the query viewer's Neo4j tab
 * walks). A block has one variable for each path up to the equations: the diamond above has one
 * variable of D when its two ways are declared equal, and a loop that an equation stops after a few
 * steps unfolds to its few paths. Where a foreign key leads to a path equal to one already in the
 * block, the where clause joins the two variables, so a row whose paths break an equation is left
 * out. The paths are unfolded, shortest first, until no new one is found, so the query is built
 * exactly when the schema's paths up to its equations are finitely many; when they are not, it runs
 * until the timeout, as counting the schema's own paths would. The schema itself may not have
 * foreign keys or equations of its own.
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

  /** The path equations, each side a path written as in a schema: an entity, then foreign keys. */
  public final List<Pair<List<String>, List<String>>> eqs;

  public final Map<String, String> options;

  public QueryExpForeignKeys(SchExp sch, List<Key> keys, List<Pair<String, String>> options) {
    this(sch, keys, Collections.emptyList(), options);
  }

  public QueryExpForeignKeys(
      SchExp sch,
      List<Key> keys,
      List<Pair<List<String>, List<String>>> eqs,
      List<Pair<String, String>> options) {
    this.sch = sch;
    this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
    List<Pair<List<String>, List<String>>> l = new ArrayList<>(eqs.size());
    for (Pair<List<String>, List<String>> eq : eqs) {
      l.add(new Pair<>(List.copyOf(eq.first), List.copyOf(eq.second)));
    }
    this.eqs = Collections.unmodifiableList(l);
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
    for (Pair<List<String>, List<String>> eq : eqs) {
      if (eq.first.isEmpty() || eq.second.isEmpty()) {
        throw new RuntimeException("foreign_keys: a path equation has a side with no path.");
      }
    }
  }

  /** The target: the schema with the declared foreign keys and path equations added. */
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
        eqs,
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

    /** The foreign keys from this variable that lead, up to the equations, to one found before. */
    final List<Pair<Key, Node>> back = new ArrayList<>();

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

  /**
   * The variables of the block of {@code en} when there are path equations: one for each path from
   * it up to the equations, found shortest first. A foreign key that leads to a path equal to one
   * found before is kept as a back edge to that path's variable. It ends when no new path is found,
   * as {@link Schema#pathsFrom} does, or when the timeout interrupts it.
   */
  private static Node unfoldUpTo(
      String en, Map<String, List<Key>> out, Schema<String, String, Sym, Fk, Att> dst) {
    Map<String, Chc<String, String>> ctx = Collections.singletonMap("v", Chc.inRight(en));
    Node root = new Node(en, Collections.emptyList(), "v0", null, null);
    List<Node> found = new ArrayList<>();
    List<Term<String, String, Sym, Fk, Att, Void, Void>> terms = new ArrayList<>();
    found.add(root);
    terms.add(path(en, root.path, dst));
    Deque<Node> todo = new ArrayDeque<>();
    todo.add(root);
    while (!todo.isEmpty()) {
      Node node = todo.removeFirst();
      for (Key k : out.getOrDefault(node.en, Collections.emptyList())) {
        if (Thread.currentThread().isInterrupted()) {
          throw new IgnoreException();
        }
        List<String> p = new ArrayList<>(node.path);
        p.add(k.fk);
        Term<String, String, Sym, Fk, Att, Void, Void> t = path(en, p, dst);
        Node same = null;
        for (int i = 0; i < found.size() && same == null; i++) {
          if (found.get(i).en.equals(k.to) && dst.dp.eq(ctx, terms.get(i), t)) {
            same = found.get(i);
          }
        }
        if (same != null) {
          node.back.add(new Pair<>(k, same));
          continue;
        }
        Node child = new Node(k.to, p, "v" + found.size(), node, k);
        node.children.add(child);
        found.add(child);
        terms.add(t);
        todo.addLast(child);
      }
    }
    return root;
  }

  /** A path from {@code en}, as a term of the target in the variable {@code v}. */
  private static Term<String, String, Sym, Fk, Att, Void, Void> path(
      String en, List<String> p, Schema<String, String, Sym, Fk, Att> dst) {
    List<Fk> fks = new ArrayList<>(p.size());
    for (String name : p) {
      Fk fk = Fk.Fk(en, name);
      fks.add(fk);
      en = dst.fks.get(fk).second;
    }
    return Term.Fks(fks, Term.Var("v"));
  }

  private static void collect(Node node, List<Node> into) {
    into.add(node);
    for (Node c : node.children) {
      collect(c, into);
    }
  }

  /** The variable a foreign key leads to from a variable: a child, or the end of a back edge. */
  private static Node step(Node node, String fk) {
    for (Node c : node.children) {
      if (c.by.fk.equals(fk)) {
        return c;
      }
    }
    for (Pair<Key, Node> b : node.back) {
      if (b.first.fk.equals(fk)) {
        return b.second;
      }
    }
    return Util.anomaly();
  }

  private static Node at(Node root, List<String> path) {
    Node node = root;
    for (String fk : path) {
      node = step(node, fk);
    }
    return node;
  }

  /** The where clause of a foreign key from one variable to another: its columns agree. */
  private static void join(
      List<Eq<String, String, Sym, Fk, Att, String, String>> where, Node from, Key by, Node to) {
    for (Pair<String, String> p : by.on) {
      Term<String, String, Sym, Fk, Att, String, String> l =
          Term.Att(Att.Att(from.en, p.first), Term.Gen(from.var));
      Term<String, String, Sym, Fk, Att, String, String> r =
          Term.Att(Att.Att(to.en, p.second), Term.Gen(to.var));
      where.add(new Eq<>(null, l, r));
    }
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
    Map<Pair<String, String>, Key> declared = new LinkedHashMap<>();
    for (Key k : keys) {
      declared.put(new Pair<>(k.from, k.fk), k);
    }
    for (Pair<List<String>, List<String>> eq : eqs) {
      String shown = Util.sep(eq.first, ".") + " = " + Util.sep(eq.second, ".");
      String start = eq.first.get(0);
      if (!src.ens.contains(start)) {
        throw new RuntimeException(
            "foreign_keys: in the path equation "
                + shown
                + ", "
                + start
                + " is not an entity of "
                + sch
                + ".");
      }
      if (!eq.second.get(0).equals(start)) {
        throw new RuntimeException(
            "foreign_keys: in the path equation "
                + shown
                + ", the two sides start at "
                + start
                + " and "
                + eq.second.get(0)
                + ".");
      }
      String left = end(shown, eq.first, declared);
      String right = end(shown, eq.second, declared);
      if (!left.equals(right)) {
        throw new RuntimeException(
            "foreign_keys: in the path equation "
                + shown
                + ", the left side ends at "
                + left
                + " and the right side at "
                + right
                + ".");
      }
    }
  }

  /** The entity a side of a path equation ends at, each of its foreign keys declared here. */
  private static String end(
      String shown, List<String> side, Map<Pair<String, String>, Key> declared) {
    String en = side.get(0);
    for (String fk : side.subList(1, side.size())) {
      Key k = declared.get(new Pair<>(en, fk));
      if (k == null) {
        throw new RuntimeException(
            "foreign_keys: in the path equation "
                + shown
                + ", "
                + fk
                + " is not a foreign key declared from "
                + en
                + ".");
      }
      en = k.to;
    }
    return en;
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
    Map<String, Node> roots = new LinkedHashMap<>();
    if (eqs.isEmpty()) {
      acyclic(out);
      for (String en : src.ens) {
        roots.put(en, unfold(en, out));
      }
    } else {
      for (String en : src.ens) {
        roots.put(en, unfoldUpTo(en, out, dst));
      }
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
          join(where, node.parent, node.by, node);
        }
        for (Pair<Key, Node> b : node.back) {
          join(where, node, b.first, b.second);
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
      Node child = step(roots.get(k.from), k.fk);
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
    return Objects.hash(sch, keys, eqs, options);
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
    return sch.equals(other.sch)
        && keys.equals(other.keys)
        && eqs.equals(other.eqs)
        && options.equals(other.options);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("foreign_keys " + sch + " {");
    for (Key k : keys) {
      sb.append("\n\t").append(k);
    }
    if (!eqs.isEmpty()) {
      sb.append("\n\tpath_equations");
      for (Pair<List<String>, List<String>> eq : eqs) {
        sb.append("\n\t\t")
            .append(Util.sep(eq.first, "."))
            .append(" = ")
            .append(Util.sep(eq.second, "."));
      }
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
