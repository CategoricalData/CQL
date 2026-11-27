package catdata.provers;

import catdata.Chc;
import catdata.Pair;
import catdata.RuntimeInterruptedException;
import catdata.Triple;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.AqlProver.ProverName;
import catdata.cql.Collage;
import catdata.cql.Collage.CCollage;
import catdata.cql.Constraints;
import catdata.cql.DP;
import catdata.cql.ED;
import catdata.cql.Eq;
import catdata.cql.Head;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.InitialAlgebra;
import catdata.cql.fdm.SaturatedInstance;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * CQL's use of egglog (https://github.com/egraphs-good/egglog), a program that CQL starts and talks
 * to: as a prover ({@code prover = egglog}) and as a chase ({@code use_egglog_for_chase = true}).
 * Written for egglog 3.0.0.
 *
 * <p><b>Names.</b> No name of CQL's reaches egglog. A sort, and a symbol that takes arguments, gets
 * an egglog name made of a letter for its kind, a number, and at most 24 letters and digits of the
 * CQL name, as in {@code F3_first_name}: the number tells two names apart, and the rest is there
 * for a person who reads the program. A symbol without arguments (a generator, a value, a constant
 * of the type side, a labelled null) is a number: the term {@code (C2_Person 17)} is the constant
 * 17 of that sort. So a name is never an egglog keyword, sort, primitive or literal, has no
 * character that egglog reads as punctuation, and is short whatever the CQL name was; and egglog
 * keeps a table for each sort and each symbol with arguments, not one for each constant, which is
 * what its speed depends on. {@link Names} leads back from every name and number to the CQL symbol.
 *
 * <p><b>Conversation.</b> egglog runs with {@code --mode interactive}: for each line of commands it
 * prints what they print and then {@code (done)}, or {@code (error)} alone if one of them failed,
 * on standard output. CQL sends one command a line and reads standard output up to that mark. It
 * reads nothing from egglog's log; standard error is only kept to quote when a command is refused.
 * The facts of a program, which are most of an instance, go another way: CQL writes them to files
 * as rows of numbers, and egglog reads each file with {@code input} ({@link Session#load}).
 *
 * <p><b>Program.</b> A theory becomes a sort for each sort, with a relation that holds its
 * universe, and a constructor for each symbol with arguments. Every constant is in the universe of
 * its sort, and so is every term that egglog comes to hold. A symbol that takes a row of an entity
 * (a foreign key, an attribute) is applied to every row; a function of the type side is applied
 * only where the theory, a constraint or a question applies it, since a universe closed under, say,
 * addition has no end. An equation without variables is a {@code union}; one with variables is a
 * {@code birewrite} over the universes of its variables. A constraint is a {@code rule}.
 */
public class EgglogProver<T, C, V> extends DPKB<T, C, V> {

  /** Ends the egglog of a prover that nothing uses any more. */
  private static final Cleaner CLEANER = Cleaner.create();

  /**
   * The costs that make egglog print, for a value, a term of the type side when it has one, then a
   * labelled null of the instance, and an attribute of a row only when the value is nothing else.
   */
  private static final int LABELLED_NULL_COST = 1_000;

  private static final int ATTRIBUTE_COST = 1_000_000;

  /** The ruleset of the rules that fill the tables of {@link Model}. */
  private static final String TABLES = "R0";

  /** The ruleset of the rules that add the facts that egglog reads from files. */
  private static final String FACTS = "R1";

  private final Program program;
  private final Session egglog;
  private final int runLevel;

  public EgglogProver(String exePath, KBTheory<T, C, V> th, int r) {
    super(th);
    runLevel = r;
    program = program(th);
    egglog = Session.start(exePath);
    CLEANER.register(this, egglog.stopper());
    egglog.load(program);
  }

  /**
   * Whether the theory proves lhs = rhs, as far as {@code egglog_bound} rounds of its rules show.
   * Both terms are put into egglog first, so the answer does not depend on their being there. A
   * variable of the question is a constant that lasts for the question only.
   */
  @Override
  public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    Names names = program.names;
    Map<V, String> vars = new LinkedHashMap<>();
    List<String> commands = new ArrayList<>();
    if (ctx != null && !ctx.isEmpty()) {
      commands.add("(push)");
      for (Map.Entry<V, T> v : ctx.entrySet()) {
        String constant = names.unusedConstant(v.getValue(), vars.size());
        commands.add("(" + names.universe(v.getValue()) + " " + constant + ")");
        vars.put(v.getKey(), constant);
      }
    }
    String l = term(names, lhs, vars);
    String r = term(names, rhs, vars);
    commands.add(l);
    commands.add(r);
    commands.add("(run " + runLevel + " :until (= " + l + " " + r + "))");
    egglog.run(commands);
    boolean equal = egglog.holds("(check (= " + l + " " + r + "))");
    if (!vars.isEmpty()) {
      egglog.run(List.of("(pop)"));
    }
    return equal;
  }

  @Override
  public synchronized String toString() {
    String text = program.text();
    return "Egglog prover\n\n" + text.substring(0, Integer.min(text.length(), 4096 * 4096));
  }

  /** Adds a constant, such as a value that a question mentions and the theory does not. */
  @Override
  public synchronized void add(C c, T t) {
    if (kb.syms.containsKey(c)) {
      return;
    }
    Names names = program.names;
    List<String> commands = new ArrayList<>(names.declareConstant(c, t));
    commands.add(names.member(c, t).text());
    egglog.run(commands);
    program.later.addAll(commands);
    kb.syms.put(c, new Pair<>(Collections.emptyList(), t));
  }

  @Override
  public boolean supportsTrivialityCheck() {
    return false;
  }

  /** The egglog program of a theory. */
  public static <T, C, V> String toEgglog(KBTheory<T, C, V> th) {
    return program(th).text();
  }

  /** The egglog program whose saturation is the chase of an instance with constraints. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static String toEgglog(Constraints eds, Instance I) {
    return chaseProgram(eds, I.collage().toKB()).text();
  }

  /** The egglog program of a schema and constraints on it. The second argument is not used. */
  public static String toEgglog(Constraints eds, Constraints c2) {
    return chaseProgram(eds, eds.schema.collage().toKB()).text();
  }

  /**
   * Not written yet, and nothing calls it: it answers false whatever the constraints are, after
   * checking that egglog is there.
   */
  public static boolean entails(Constraints eds1, Constraints eds2, String exePath) {
    toEgglog(eds1, eds2);
    Session.requireFile(exePath);
    return false;
  }

  /**
   * The chase of an instance with constraints, computed by egglog. egglog runs the program of
   * {@link #toEgglog(Constraints, Instance)} to saturation, which does not end when the chase is
   * infinite; the rows of the result are then read back as tables of row numbers ({@link Model}),
   * and egglog is ended before this method returns. What the result is asked after that, CQL
   * answers without egglog: whether two rows are one by their numbers, and whether two values are
   * equal by the prover that the option {@code second_prover} names, as for any instance.
   *
   * <p>egglog applies an existential constraint to every row that matches its premise, whether a
   * witness is there already or not. The result satisfies the constraints, and it and the result of
   * CQL's own chase map into each other, but it can have rows that CQL's own chase does not add,
   * and egglog can go on without end where CQL's own chase ends.
   */
  @SuppressWarnings("unchecked")
  public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
      SaturatedInstance<
              Ty, En, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
          egglogChase(
              String exePath,
              Constraints eds,
              Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I,
              AqlOptions ops) {
    // Constraints are over CQL's own names for types, entities and symbols, so the instance is too.
    return (SaturatedInstance<
            Ty, En, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>)
        (Object)
            chase(
                exePath,
                eds,
                (Instance<
                        String,
                        String,
                        catdata.cql.exp.Sym,
                        catdata.cql.exp.Fk,
                        catdata.cql.exp.Att,
                        Gen,
                        Sk,
                        X,
                        Y>)
                    (Object) I,
                ops);
  }

  private static <Gen, Sk, X, Y>
      SaturatedInstance<
              String,
              String,
              Sym,
              Fk,
              Att,
              String,
              String,
              Integer,
              Chc<String, Pair<Integer, Att>>>
          chase(
              String exePath,
              Constraints eds,
              Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> I,
              AqlOptions ops) {
    Session.requireFile(exePath);
    Schema<String, String, Sym, Fk, Att> schema = I.schema();
    Program program = chaseProgram(eds, I.collage().toKB());
    Model model;
    try (Session egglog = Session.start(exePath)) {
      egglog.load(program);
      egglog.run(List.of("(run-schedule (saturate (run)))"));
      model = new Model(egglog, program.names, schema);
    }

    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>();
    col.addAll(schema.typeSide.collage());
    col.addAll(schema.collage());
    model.describe(col);

    DP<String, String, Sym, Fk, Att, String, String> dp =
        new DP<>() {

          @Override
          public String toStringProver() {
            return "Egglog-chase";
          }

          @Override
          public String toString() {
            return toStringProver();
          }

          @Override
          public boolean eq(
              Map<String, Chc<String, String>> ctx,
              Term<String, String, Sym, Fk, Att, String, String> lhs,
              Term<String, String, Sym, Fk, Att, String, String> rhs) {
            if (ctx != null && !ctx.isEmpty()) {
              throw new RuntimeException("Cannot solve full word problem");
            }
            return model.row(lhs) == model.row(rhs);
          }
        };

    var ret =
        new InitialAlgebra<String, String, Sym, Fk, Att, String, String>(
            ops,
            schema,
            col,
            z -> z.toString(),
            (z, zz) -> zz.toString(),
            dp,
            (ProverName) ops.getOrDefault(AqlOption.second_prover));
    return new SaturatedInstance<>(ret, ret, false, false, true, new HashMap<>());
  }

  /**
   * A theory as an egglog program, with the names it gave: commands, one a line; facts; and the
   * commands that come after the facts, which add constants. Facts of one shape are kept as rows of
   * numbers, one row a fact, which is how egglog reads them ({@link Session#load}).
   */
  static final class Program {
    final Names names = new Names();
    final List<String> lines = new ArrayList<>();

    /** For each shape, the numbers of each of its facts. */
    final Map<List<String>, List<int[]>> facts = new LinkedHashMap<>();

    final List<String> later = new ArrayList<>();

    void add(Fact fact) {
      facts.computeIfAbsent(fact.shape(), s -> new ArrayList<>()).add(fact.numbers());
    }

    /** The program as commands alone: each fact is the command that it stands for. */
    String text() {
      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        sb.append(line).append('\n');
      }
      for (Map.Entry<List<String>, List<int[]>> shape : facts.entrySet()) {
        for (int[] numbers : shape.getValue()) {
          sb.append(new Fact(shape.getKey(), numbers).text()).append('\n');
        }
      }
      for (String line : later) {
        sb.append(line).append('\n');
      }
      return sb.toString();
    }

    /**
     * Writes the facts of each shape to a file in a directory, a line of numbers separated by tabs
     * for each fact, and returns the commands that have egglog read them: for each shape a
     * relation, which {@code input} fills from the file, and a rule that adds the fact of each of
     * its rows; then a single run of those rules.
     */
    List<String> input(Path dir) throws IOException {
      List<String> commands = new ArrayList<>();
      commands.add("(ruleset " + FACTS + ")");
      int n = 0;
      for (Map.Entry<List<String>, List<int[]>> shape : facts.entrySet()) {
        String relation = Names.facts(n++);
        Path file = dir.resolve(relation + ".tsv").toAbsolutePath();
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.US_ASCII)) {
          for (int[] numbers : shape.getValue()) {
            for (int i = 0; i < numbers.length; i++) {
              if (i > 0) {
                out.write('\t');
              }
              out.write(Integer.toString(numbers[i]));
            }
            out.write('\n');
          }
        }
        int columns = shape.getKey().size() - 1;
        StringBuilder row = new StringBuilder(relation);
        for (int i = 0; i < columns; i++) {
          row.append(" V").append(i);
        }
        commands.add("(relation " + relation + " (" + "i64 ".repeat(columns).trim() + "))");
        commands.add(
            "(rule (("
                + row
                + ")) ("
                + Fact.fill(shape.getKey(), i -> "V" + i)
                + ") :ruleset "
                + FACTS
                + ")");
        commands.add("(input " + relation + " " + Session.quoted(file.toString()) + ")");
      }
      commands.add("(run " + FACTS + " 1)");
      return commands;
    }
  }

  /**
   * An action without variables, as two parts: its shape, which is the text before, between and
   * after the numbers of its constants, and those numbers. The shape of {@code (union (F0_f (C1_E
   * 2)) (C0_T 5))} is {@code "(union (F0_f (C1_E "}, {@code ")) (C0_T "} and {@code "))"}.
   */
  record Fact(List<String> shape, int[] numbers) {

    String text() {
      return fill(shape, i -> Integer.toString(numbers[i]));
    }

    /** The shape with the i-th number replaced by what is given for it. */
    static String fill(List<String> shape, IntFunction<String> number) {
      StringBuilder sb = new StringBuilder(shape.get(0));
      for (int i = 1; i < shape.size(); i++) {
        sb.append(number.apply(i - 1)).append(shape.get(i));
      }
      return sb.toString();
    }
  }

  /** Writes a fact: its text, with each constant's number kept apart. */
  private static final class FactWriter {
    private final Names names;
    private final List<String> shape = new ArrayList<>();
    private final List<Integer> numbers = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();

    FactWriter(Names names) {
      this.names = names;
    }

    FactWriter text(String s) {
      text.append(s);
      return this;
    }

    <C, V> FactWriter term(KBExp<C, V> e) {
      if (e.isVar()) {
        throw new RuntimeException("The variable " + e.getVar() + " is not in the context.");
      }
      Names.Constant constant = names.constantOf(e.f());
      if (constant != null) {
        text.append('(').append(constant.constructor()).append(' ');
        shape.add(text.toString());
        text.setLength(0);
        numbers.add(constant.number());
        text.append(')');
        return this;
      }
      text.append('(').append(names.symbol(e.f()));
      for (KBExp<C, V> arg : e.getArgs()) {
        text.append(' ');
        term(arg);
      }
      text.append(')');
      return this;
    }

    Fact fact() {
      List<String> parts = new ArrayList<>(shape);
      parts.add(text.toString());
      return new Fact(List.copyOf(parts), numbers.stream().mapToInt(Integer::intValue).toArray());
    }
  }

  private static <T, C, V> Program program(KBTheory<T, C, V> th) {
    Program program = new Program();
    Names names = program.names;
    List<String> lines = program.lines;

    // Sorts and symbols in the order of their CQL names, and equations in the order of their
    // text, so that the same theory is the same program in every run.
    List<T> sorts = new ArrayList<>(th.tys);
    sorts.sort(Comparator.comparing((T t) -> String.valueOf(t)));
    List<C> symbols = new ArrayList<>(th.syms.keySet());
    symbols.sort(
        Comparator.comparing((C c) -> String.valueOf(c))
            .thenComparing(c -> String.valueOf(th.syms.get(c))));

    for (T sort : sorts) {
      lines.addAll(names.declareSort(sort));
    }
    for (C c : symbols) {
      Pair<List<T>, T> type = th.syms.get(c);
      if (!type.first.isEmpty()) {
        int cost = c instanceof Head<?, ?, ?, ?, ?, ?, ?> h && h.att() != null ? ATTRIBUTE_COST : 1;
        String f = names.declareSymbol(c, type.second);
        lines.add(constructor(names, f, type.first, type.second, cost));
      }
    }
    for (C c : symbols) {
      Pair<List<T>, T> type = th.syms.get(c);
      if (type.first.isEmpty()) {
        lines.addAll(names.declareConstant(c, type.second));
        program.add(names.member(c, type.second));
      }
    }
    for (C c : symbols) {
      Pair<List<T>, T> type = th.syms.get(c);
      if (type.first.isEmpty()) {
        continue;
      }
      StringBuilder applied = new StringBuilder("(").append(names.symbol(c));
      List<String> arguments = new ArrayList<>();
      for (int i = 0; i < type.first.size(); i++) {
        String v = "V" + (i + 1);
        applied.append(' ').append(v);
        arguments.add("(" + names.universe(type.first.get(i)) + " " + v + ")");
      }
      applied.append(')');
      lines.add("(rule ((= V0 " + applied + ")) ((" + names.universe(type.second) + " V0)))");
      if (type.first.stream().anyMatch(EgglogProver::isEntity)) {
        lines.add("(rule (" + String.join(" ", arguments) + ") (" + applied + "))");
      }
    }
    List<String> rewrites = new ArrayList<>();
    List<Pair<String, Fact>> unions = new ArrayList<>();
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : th.eqs) {
      if (eq.first == null || eq.first.isEmpty()) {
        Fact union =
            new FactWriter(names)
                .text("(union ")
                .term(eq.second)
                .text(" ")
                .term(eq.third)
                .text(")")
                .fact();
        unions.add(new Pair<>(union.text(), union));
        continue;
      }
      Map<V, String> vars = new LinkedHashMap<>();
      List<String> universes = new ArrayList<>();
      for (Map.Entry<V, T> v : eq.first.entrySet()) {
        String name = Names.variable(vars.size(), v.getKey());
        vars.put(v.getKey(), name);
        universes.add("(" + names.universe(v.getValue()) + " " + name + ")");
      }
      String lhs = term(names, eq.second, vars);
      String rhs = term(names, eq.third, vars);
      rewrites.add(
          "(birewrite " + lhs + " " + rhs + " :when (" + String.join(" ", universes) + "))");
    }
    Collections.sort(rewrites);
    lines.addAll(rewrites);
    unions.sort(Comparator.comparing((Pair<String, Fact> u) -> u.first));
    for (Pair<String, Fact> union : unions) {
      program.add(union.second);
    }
    return program;
  }

  /**
   * The program of a theory and constraints on it. A constraint is a rule: its premise is the
   * universes of its universal variables and its equations, and its conclusion puts a witness for
   * each existential variable into the universe of its sort and unites the two sides of each
   * equation. The witness is a constructor of the constraint, applied to the universal variables;
   * unlike the symbols of the theory it is applied nowhere else, so a witness exists only where the
   * premise holds. A function of the type side that the premise applies is applied, by a rule of
   * its own, to every row the premise speaks of: egglog matches a premise against the terms it
   * holds, and would otherwise hold none of these.
   *
   * <p>A constraint that uses a function which the type side computes in Java is refused: egglog
   * knows a function by its name and the theory's equations, and does not compute it.
   */
  private static <Gen, Sk> Program chaseProgram(
      Constraints eds,
      KBTheory<Chc<String, String>, Head<String, String, Sym, Fk, Att, Gen, Sk>, String> kb) {
    // egglog does not run the Java that computes a function of the type side. A constraint that
    // uses one would be chased as if the function had no definition, so it is refused.
    Map<Sym, String> computed = eds.schema.typeSide.js.java_fns;
    // A value that only a constraint mentions is a constant of the theory too.
    for (ED ed : eds.eds) {
      for (Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, Void, Void>,
                  Term<String, String, Sym, Fk, Att, Void, Void>>>
          eqs : List.of(ed.Awh, ed.Ewh)) {
        for (Pair<
                Term<String, String, Sym, Fk, Att, Void, Void>,
                Term<String, String, Sym, Fk, Att, Void, Void>>
            eq : eqs) {
          for (Term<String, String, Sym, Fk, Att, Void, Void> side : List.of(eq.first, eq.second)) {
            for (Sym f : side.syms()) {
              if (computed.containsKey(f)) {
                throw new RuntimeException(
                    "A constraint uses "
                        + f
                        + ", which the type side computes in Java. egglog does not compute it,"
                        + " so egglog cannot chase this constraint; CQL's own chase can.");
              }
            }
            for (Pair<Object, String> value : side.objs()) {
              kb.syms.putIfAbsent(
                  Head.ObjHead(value.first, value.second),
                  new Pair<>(Collections.emptyList(), Chc.inLeft(value.second)));
            }
          }
        }
      }
    }
    Program program = program(kb);
    Names names = program.names;
    int constraint = 0;
    for (ED ed : eds.eds) {
      constraint++;
      Map<String, String> vars = new LinkedHashMap<>();
      List<Chc<String, String>> universals = new ArrayList<>();
      List<String> premise = new ArrayList<>();
      StringBuilder applied = new StringBuilder();
      for (Map.Entry<String, Chc<String, String>> v : ed.As.entrySet()) {
        String name = Names.variable(vars.size(), v.getKey());
        vars.put(v.getKey(), name);
        universals.add(v.getValue());
        applied.append(' ').append(name);
        premise.add("(" + names.universe(v.getValue()) + " " + name + ")");
      }
      // egglog matches a premise against the terms it holds. It holds every foreign key and
      // attribute of every row, and a function of the type side only where something applied
      // it, so what the premise applies is applied here, to the rows the premise speaks of.
      List<Term<String, String, Sym, Fk, Att, Void, Void>> applications = new ArrayList<>();
      for (Pair<
              Term<String, String, Sym, Fk, Att, Void, Void>,
              Term<String, String, Sym, Fk, Att, Void, Void>>
          eq : ed.Awh) {
        applications(eq.first, applications);
        applications(eq.second, applications);
      }
      Set<String> made = new LinkedHashSet<>();
      for (Term<String, String, Sym, Fk, Att, Void, Void> application : applications) {
        List<String> rows = new ArrayList<>();
        for (String v : new LinkedHashSet<>(application.vars())) {
          rows.add("(" + names.universe(ed.As.get(v)) + " " + vars.get(v) + ")");
        }
        String text = term(names, application, vars);
        made.add(rows.isEmpty() ? text : "(rule (" + String.join(" ", rows) + ") (" + text + "))");
      }
      program.lines.addAll(made);
      List<String> witnesses = new ArrayList<>();
      for (Map.Entry<String, Chc<String, String>> v : ed.Es.entrySet()) {
        String witness = names.declareWitness(constraint, v.getKey(), v.getValue());
        int cost = v.getValue().left ? ATTRIBUTE_COST : 1;
        program.lines.add(constructor(names, witness, universals, v.getValue(), cost));
        vars.put(v.getKey(), "(" + witness + applied + ")");
        witnesses.add("(" + names.universe(v.getValue()) + " (" + witness + applied + "))");
      }
      List<String> equal = new ArrayList<>();
      for (Pair<
              Term<String, String, Sym, Fk, Att, Void, Void>,
              Term<String, String, Sym, Fk, Att, Void, Void>>
          eq : ed.Awh) {
        equal.add("(= " + term(names, eq.first, vars) + " " + term(names, eq.second, vars) + ")");
      }
      Collections.sort(equal);
      premise.addAll(equal);
      List<String> unions = new ArrayList<>();
      for (Pair<
              Term<String, String, Sym, Fk, Att, Void, Void>,
              Term<String, String, Sym, Fk, Att, Void, Void>>
          eq : ed.Ewh) {
        unions.add(
            "(union " + term(names, eq.first, vars) + " " + term(names, eq.second, vars) + ")");
      }
      Collections.sort(unions);
      List<String> conclusion = new ArrayList<>(witnesses);
      conclusion.addAll(unions);
      if (conclusion.isEmpty()) {
        continue;
      }
      if (premise.isEmpty()) {
        program.lines.addAll(conclusion);
      } else {
        program.lines.add(
            "(rule (" + String.join(" ", premise) + ") (" + String.join(" ", conclusion) + "))");
      }
    }
    return program;
  }

  /** The largest parts of a term that apply a function of the type side to arguments. */
  private static void applications(
      Term<String, String, Sym, Fk, Att, Void, Void> term,
      List<Term<String, String, Sym, Fk, Att, Void, Void>> found) {
    if (term.sym() != null && !term.args.isEmpty()) {
      found.add(term);
    } else if (term.att() != null || term.fk() != null) {
      applications(term.arg, found);
    }
  }

  private static String constructor(
      Names names, String name, List<?> arguments, Object result, int cost) {
    StringBuilder sb = new StringBuilder("(constructor ").append(name).append(" (");
    for (int i = 0; i < arguments.size(); i++) {
      sb.append(i == 0 ? "" : " ").append(names.sort(arguments.get(i)));
    }
    sb.append(") ").append(names.sort(result));
    if (cost != 1) {
      sb.append(" :cost ").append(cost);
    }
    return sb.append(')').toString();
  }

  /** Whether a sort of a theory of CQL is an entity, and not a type. */
  private static boolean isEntity(Object sort) {
    return sort instanceof Chc<?, ?> c && !c.left;
  }

  /** A term in egglog's syntax; vars gives the text that stands for each variable. */
  private static <C, V> String term(Names names, KBExp<C, V> e, Map<V, String> vars) {
    StringBuilder sb = new StringBuilder();
    term(sb, names, e, vars);
    return sb.toString();
  }

  private static <C, V> void term(
      StringBuilder sb, Names names, KBExp<C, V> e, Map<V, String> vars) {
    if (e.isVar()) {
      String v = vars.get(e.getVar());
      if (v == null) {
        throw new RuntimeException("The variable " + e.getVar() + " is not in the context.");
      }
      sb.append(v);
      return;
    }
    String constant = names.constant(e.f());
    if (constant != null) {
      sb.append(constant);
      return;
    }
    sb.append('(').append(names.symbol(e.f()));
    for (KBExp<C, V> arg : e.getArgs()) {
      sb.append(' ');
      term(sb, names, arg, vars);
    }
    sb.append(')');
  }

  /**
   * The egglog names of the sorts and symbols of a theory, and the way back from them. A name is a
   * letter, a number, and after an underscore the first letters and digits of the CQL name. For a
   * sort: S, with C for the constructor of its constants, N for that of its labelled nulls, U for
   * the relation that holds its universe and I for the function that numbers its rows. F for a
   * symbol with arguments and T for the table of its values; K for the witness of an existential
   * variable of a constraint; V for a variable of a rule; L for the relation that egglog reads the
   * facts of one shape into.
   */
  static final class Names {

    /** How many letters and digits of a CQL name its egglog name keeps. */
    private static final int KEPT = 24;

    /** A constant: the constructor of its sort's constants or labelled nulls, and its number. */
    record Constant(String constructor, int number) {

      String term() {
        return "(" + constructor + " " + number + ")";
      }
    }

    private final Map<Object, String> sorts = new HashMap<>();
    private final Map<Object, String> symbols = new HashMap<>();
    private final Map<Object, Constant> constants = new HashMap<>();

    /** The CQL symbol of each name F, and the CQL symbols of each name C or N, by number. */
    private final Map<String, Object> named = new HashMap<>();

    private final Map<String, List<Object>> numbered = new HashMap<>();

    /** The name R2x, as CQL showed it before, of each name K. */
    private final Map<String, String> witnesses = new HashMap<>();

    /** The CQL sort of what each name C, N, F or K makes. */
    private final Map<String, Object> results = new HashMap<>();

    /** Names a sort, and returns the commands that declare it, its constants and its universe. */
    List<String> declareSort(Object sort) {
      String name = "S" + sorts.size() + kept(sort);
      sorts.put(sort, name);
      numbered.put(constants(sort), new ArrayList<>());
      results.put(constants(sort), sort);
      return List.of(
          "(sort " + name + ")",
          "(constructor " + constants(sort) + " (i64) " + name + ")",
          "(relation " + universe(sort) + " (" + name + "))");
    }

    String declareSymbol(Object symbol, Object result) {
      String name = "F" + symbols.size() + kept(symbol);
      symbols.put(symbol, name);
      named.put(name, symbol);
      results.put(name, result);
      return name;
    }

    /**
     * Numbers a constant. The labelled nulls of a sort are numbered apart, under a constructor that
     * costs more, so that egglog prints a value as a constant when it is equal to one; for the
     * first labelled null of a sort, this returns the command that declares that constructor, and
     * otherwise nothing.
     */
    List<String> declareConstant(Object constant, Object sort) {
      boolean labelledNull = constant instanceof Head<?, ?, ?, ?, ?, ?, ?> h && h.sk() != null;
      String constructor = labelledNull ? "N" + sort(sort).substring(1) : constants(sort);
      List<String> commands = new ArrayList<>();
      if (!numbered.containsKey(constructor)) {
        numbered.put(constructor, new ArrayList<>());
        results.put(constructor, sort);
        commands.add(
            "(constructor "
                + constructor
                + " (i64) "
                + sort(sort)
                + " :cost "
                + LABELLED_NULL_COST
                + ")");
      }
      List<Object> others = numbered.get(constructor);
      constants.put(constant, new Constant(constructor, others.size()));
      others.add(constant);
      return commands;
    }

    /** The fact that a constant is in the universe of its sort. */
    Fact member(Object constant, Object sort) {
      Constant c = constants.get(constant);
      return new Fact(
          List.of("(" + universe(sort) + " (" + c.constructor() + " ", "))"),
          new int[] {c.number()});
    }

    /** The relation that egglog reads the facts of the n-th shape into. */
    static String facts(int n) {
      return "L" + n;
    }

    /** The witness of an existential variable of the n-th constraint, shown as CQL did: R2x. */
    String declareWitness(int constraint, String variable, Object result) {
      String name = "K" + witnesses.size() + kept(variable);
      witnesses.put(name, "R" + constraint + variable);
      results.put(name, result);
      return name;
    }

    static String variable(int position, Object variable) {
      return "V" + position + kept(variable);
    }

    String sort(Object sort) {
      String name = sorts.get(sort);
      if (name == null) {
        throw new RuntimeException("The sort " + sort + " is not in the theory given to egglog.");
      }
      return name;
    }

    private String constants(Object sort) {
      return "C" + sort(sort).substring(1);
    }

    String universe(Object sort) {
      return "U" + sort(sort).substring(1);
    }

    String rowNumber(Object sort) {
      return "I" + sort(sort).substring(1);
    }

    /** The term of a constant, or null for a symbol that takes arguments. */
    String constant(Object symbol) {
      Constant c = constants.get(symbol);
      return c == null ? null : c.term();
    }

    /** A constant, or null for a symbol that takes arguments. */
    Constant constantOf(Object symbol) {
      return constants.get(symbol);
    }

    /** A constant of a sort that no symbol of the theory is: the n-th such. */
    String unusedConstant(Object sort, int n) {
      return "(" + constants(sort) + " " + (numbered.get(constants(sort)).size() + n) + ")";
    }

    String symbol(Object symbol) {
      String name = symbols.get(symbol);
      if (name == null) {
        throw new RuntimeException(
            "The symbol " + symbol + " is not in the theory given to egglog.");
      }
      return name;
    }

    String table(Object symbol) {
      return "T" + symbol(symbol).substring(1);
    }

    /** Whether a term that egglog printed is a constant: a number under the name C or N. */
    boolean isConstant(Sexp term) {
      return numbered.containsKey(term.head);
    }

    /** The CQL symbol at the head of a term that egglog printed; null for a witness. */
    Object cql(Sexp term) {
      if (isConstant(term)) {
        List<Object> others = numbered.get(term.head);
        int n = term.args.size() == 1 ? term.args.get(0).number() : -1;
        if (n >= 0 && n < others.size()) {
          return others.get(n);
        }
      } else if (named.containsKey(term.head)) {
        return named.get(term.head);
      } else if (witnesses.containsKey(term.head)) {
        return null;
      }
      throw new RuntimeException("egglog printed a name that CQL did not give it: " + term.text());
    }

    /** A term that egglog printed, as CQL writes it: a, works(a), R2m(d). */
    String shown(Sexp term) {
      Object symbol = cql(term);
      if (isConstant(term)) {
        return String.valueOf(symbol);
      }
      StringBuilder sb = new StringBuilder(symbol == null ? witnesses.get(term.head) : "" + symbol);
      for (int i = 0; i < term.args.size(); i++) {
        sb.append(i == 0 ? "(" : ", ").append(shown(term.args.get(i)));
      }
      return term.args.isEmpty() ? sb.toString() : sb.append(')').toString();
    }

    /** The CQL sort of a term that egglog printed. */
    Object result(Sexp term) {
      return results.get(term.head);
    }

    /** An underscore and the letters and digits that tell a person which CQL name this was. */
    private static String kept(Object named) {
      String s =
          String.valueOf(named instanceof Chc<?, ?> sort ? (sort.left ? sort.l : sort.r) : named);
      StringBuilder sb = new StringBuilder("_");
      for (int i = 0; i < s.length() && sb.length() <= KEPT; i++) {
        char ch = s.charAt(i);
        if (ch < 128 && Character.isLetterOrDigit(ch)) {
          sb.append(ch);
        } else if (sb.charAt(sb.length() - 1) != '_') {
          sb.append('_');
        }
      }
      while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
        sb.setLength(sb.length() - 1);
      }
      return sb.toString();
    }
  }

  /**
   * What egglog holds after saturation, as tables of row numbers. For each sort CQL prints its
   * universe, which has one row for each element of the sort, numbers the rows, and tells egglog
   * the numbers; egglog then fills, for each foreign key and attribute, a table from the number of
   * a row to the number of the row it is sent to. Which row a row is sent to is therefore egglog's
   * answer, and not a comparison of printed terms. A term is read only for the name of a row of an
   * entity, and for the value of a row of a type.
   */
  private static final class Model {
    private final Names names;
    private final Schema<String, String, Sym, Fk, Att> schema;

    /** For each sort, a term for each row, as egglog printed them; a row's place is its number. */
    private final Map<Chc<String, String>, List<Sexp>> rows = new LinkedHashMap<>();

    /** For each foreign key and attribute, the number of the row each row is sent to. */
    private final Map<Object, int[]> cells = new HashMap<>();

    /** For each entity, the number of the row that a term stands for, by the term's text. */
    private final Map<String, Map<String, Integer>> rowOf = new HashMap<>();

    /** For each entity, its generators by row number; and the entity and row of each generator. */
    private final Map<String, String[]> generators = new HashMap<>();

    private final Map<String, Pair<String, Integer>> generated = new HashMap<>();

    /** For each entity, the number of its first row when the rows of all entities are counted. */
    private final Map<String, Integer> first = new HashMap<>();

    /** The labelled null of the result that stands for a term, by the term's text. */
    private final Map<String, String> nulls = new HashMap<>();

    private final Set<String> nullsUsed = new HashSet<>();

    Model(Session egglog, Names names, Schema<String, String, Sym, Fk, Att> schema) {
      this.names = names;
      this.schema = schema;
      List<Chc<String, String>> sorts = new ArrayList<>();
      for (String ty : schema.typeSide.tys) {
        sorts.add(Chc.inLeft(ty));
      }
      for (String en : schema.ens) {
        sorts.add(Chc.inRight(en));
      }

      // The rows of each sort, and a number for each.
      List<String> commands = new ArrayList<>();
      for (Chc<String, String> sort : sorts) {
        commands.add("(print-function " + names.universe(sort) + ")");
      }
      List<List<String>> printed = egglog.ask(commands);
      commands = new ArrayList<>();
      for (int s = 0; s < sorts.size(); s++) {
        Chc<String, String> sort = sorts.get(s);
        List<Sexp> terms = new ArrayList<>();
        for (Sexp row : Sexp.rows(printed.get(s), names.universe(sort), 1)) {
          if (row.args.get(0).bare) {
            throw new RuntimeException("egglog has no term for a row of " + sort + ".");
          }
          terms.add(row.args.get(0));
        }
        terms.sort(Comparator.comparing(Sexp::text));
        rows.put(sort, terms);
        String number = names.rowNumber(sort);
        commands.add("(function " + number + " (" + names.sort(sort) + ") i64 :no-merge)");
        Map<String, Integer> numbers = new HashMap<>();
        for (int i = 0; i < terms.size(); i++) {
          commands.add("(set (" + number + " " + terms.get(i).text() + ") " + i + ")");
          numbers.put(terms.get(i).text(), i);
        }
        if (!sort.left) {
          rowOf.put(sort.r, numbers);
        }
      }
      egglog.run(commands);

      // The table of each foreign key and attribute.
      Map<Object, Pair<Chc<String, String>, Chc<String, String>>> functions = new LinkedHashMap<>();
      for (Map.Entry<Fk, Pair<String, String>> fk : schema.fks.entrySet()) {
        Object head = Head.FkHead(fk.getKey());
        functions.put(
            head, new Pair<>(Chc.inRight(fk.getValue().first), Chc.inRight(fk.getValue().second)));
      }
      for (Map.Entry<Att, Pair<String, String>> att : schema.atts.entrySet()) {
        Object head = Head.AttHead(att.getKey());
        functions.put(
            head, new Pair<>(Chc.inRight(att.getValue().first), Chc.inLeft(att.getValue().second)));
      }
      commands = new ArrayList<>();
      commands.add("(ruleset " + TABLES + ")");
      for (Map.Entry<Object, Pair<Chc<String, String>, Chc<String, String>>> f :
          functions.entrySet()) {
        String table = names.table(f.getKey());
        commands.add("(relation " + table + " (i64 i64))");
        commands.add(
            "(rule ((= V1 ("
                + names.symbol(f.getKey())
                + " V0)) (= V2 ("
                + names.rowNumber(f.getValue().first)
                + " V0)) (= V3 ("
                + names.rowNumber(f.getValue().second)
                + " V1))) (("
                + table
                + " V2 V3)) :ruleset "
                + TABLES
                + ")");
      }
      commands.add("(run " + TABLES + " 1)");
      egglog.run(commands);
      commands = new ArrayList<>();
      for (Object f : functions.keySet()) {
        commands.add("(print-function " + names.table(f) + ")");
      }
      printed = egglog.ask(commands);
      int n = 0;
      for (Map.Entry<Object, Pair<Chc<String, String>, Chc<String, String>>> f :
          functions.entrySet()) {
        int[] to = new int[rows.get(f.getValue().first).size()];
        Arrays.fill(to, -1);
        int targets = rows.get(f.getValue().second).size();
        for (Sexp row : Sexp.rows(printed.get(n++), names.table(f.getKey()), 2)) {
          int from = row.args.get(0).number();
          int target = row.args.get(1).number();
          if (from < 0 || from >= to.length || target < 0 || target >= targets || to[from] != -1) {
            throw new RuntimeException("CQL cannot read what egglog printed: " + row.text());
          }
          to[from] = target;
        }
        for (int target : to) {
          if (target == -1) {
            throw new RuntimeException("egglog gave no value for " + f.getKey() + " on a row.");
          }
        }
        cells.put(f.getKey(), to);
      }

      // The rows that the terms inside the values of attributes stand for.
      Map<String, Set<String>> asked = new LinkedHashMap<>();
      for (Map.Entry<Att, Pair<String, String>> att : schema.atts.entrySet()) {
        List<Sexp> values = rows.get(Chc.inLeft(att.getValue().second));
        BitSet seen = new BitSet();
        for (int value : cells.get(Head.AttHead(att.getKey()))) {
          if (!seen.get(value)) {
            seen.set(value);
            rowsInside(values.get(value), asked);
          }
        }
      }
      commands = new ArrayList<>();
      for (Map.Entry<String, Set<String>> en : asked.entrySet()) {
        for (String term : en.getValue()) {
          commands.add(
              "(extract (" + names.rowNumber(Chc.inRight(en.getKey())) + " " + term + "))");
        }
      }
      printed = egglog.ask(commands);
      n = 0;
      for (Map.Entry<String, Set<String>> en : asked.entrySet()) {
        for (String term : en.getValue()) {
          List<String> answer = printed.get(n++);
          if (answer.size() != 1) {
            throw new RuntimeException("CQL cannot read what egglog printed: " + answer);
          }
          rowOf.get(en.getKey()).put(term, Sexp.read(answer.get(0)).number());
        }
      }

      // A generator for each row of each entity, named by its term.
      Set<String> used = new HashSet<>();
      int rowsBefore = 0;
      for (String en : schema.ens) {
        List<Sexp> terms = rows.get(Chc.inRight(en));
        String[] gens = new String[terms.size()];
        for (int i = 0; i < gens.length; i++) {
          gens[i] = unused(names.shown(terms.get(i)), used);
          generated.put(gens[i], new Pair<>(en, i));
        }
        generators.put(en, gens);
        first.put(en, rowsBefore);
        rowsBefore += gens.length;
      }
    }

    /** Notes the terms for rows inside a value whose row numbers are not known yet. */
    private void rowsInside(Sexp value, Map<String, Set<String>> asked) {
      if (!names.isConstant(value) && names.cql(value) instanceof Head<?, ?, ?, ?, ?, ?, ?> h) {
        if (h.att() != null) {
          String en = schema.atts.get(h.att()).first;
          String row = value.args.get(0).text();
          if (!rowOf.get(en).containsKey(row)) {
            asked.computeIfAbsent(en, x -> new LinkedHashSet<>()).add(row);
          }
        } else {
          for (Sexp arg : value.args) {
            rowsInside(arg, asked);
          }
        }
      }
    }

    /** The generators of the result, its labelled nulls, and the value of each attribute. */
    void describe(Collage<String, String, Sym, Fk, Att, String, String> col) {
      for (String en : schema.ens) {
        for (String gen : generators.get(en)) {
          col.gens().put(gen, en);
        }
      }
      Map<String, List<Term<String, String, Sym, Fk, Att, String, String>>> terms = new HashMap<>();
      for (Map.Entry<Att, Pair<String, String>> att : schema.atts.entrySet()) {
        String[] gens = generators.get(att.getValue().first);
        List<Sexp> values = rows.get(Chc.inLeft(att.getValue().second));
        List<Term<String, String, Sym, Fk, Att, String, String>> done =
            terms.computeIfAbsent(
                att.getValue().second,
                type -> new ArrayList<>(Collections.nCopies(values.size(), null)));
        int[] to = cells.get(Head.AttHead(att.getKey()));
        for (int i = 0; i < gens.length; i++) {
          if (done.get(to[i]) == null) {
            done.set(to[i], value(values.get(to[i]), col));
          }
          Term<String, String, Sym, Fk, Att, String, String> lhs =
              Term.Att(att.getKey(), Term.Gen(gens[i]));
          if (!lhs.equals(done.get(to[i]))) {
            col.eqs().add(new Eq<>(null, lhs, done.get(to[i])));
          }
        }
      }
      // What follows compares rows, for which the generators and the foreign keys are enough.
      rows.clear();
      rowOf.clear();
      nulls.clear();
      nullsUsed.clear();
      for (Att att : schema.atts.keySet()) {
        cells.remove(Head.AttHead(att));
      }
    }

    /**
     * A value as a term of the result. An attribute of a row stays one, on the generator of that
     * row; a labelled null of the instance and the witness of a constraint are labelled nulls.
     */
    @SuppressWarnings("unchecked")
    private Term<String, String, Sym, Fk, Att, String, String> value(
        Sexp value, Collage<String, String, Sym, Fk, Att, String, String> col) {
      Head<String, String, Sym, Fk, Att, ?, ?> h =
          (Head<String, String, Sym, Fk, Att, ?, ?>) names.cql(value);
      if (h == null || h.sk() != null) {
        String sk = nulls.get(value.text());
        if (sk == null) {
          sk = unused(names.shown(value), nullsUsed);
          nulls.put(value.text(), sk);
          col.sks().put(sk, ((Chc<String, String>) names.result(value)).l);
        }
        return Term.Sk(sk);
      } else if (h.obj() != null) {
        return Term.Obj(h.obj(), h.ty());
      } else if (h.sym() != null) {
        List<Term<String, String, Sym, Fk, Att, String, String>> args = new ArrayList<>();
        for (Sexp arg : names.isConstant(value) ? List.<Sexp>of() : value.args) {
          args.add(value(arg, col));
        }
        return Term.Sym(h.sym(), args);
      } else if (h.att() != null) {
        String en = schema.atts.get(h.att()).first;
        Integer row = rowOf.get(en).get(value.args.get(0).text());
        return Term.Att(h.att(), Term.Gen(generators.get(en)[row]));
      }
      throw new RuntimeException("egglog printed, as a value, " + names.shown(value));
    }

    /** The number of the row, counted through all entities, that a term of an entity stands for. */
    int row(Term<String, String, Sym, Fk, Att, String, String> term) {
      if (term.gen() != null) {
        Pair<String, Integer> row = generated.get(term.gen());
        if (row != null) {
          return first.get(row.first) + row.second;
        }
      } else if (term.fk() != null) {
        Pair<String, String> fk = schema.fks.get(term.fk());
        int from = row(term.arg) - first.get(fk.first);
        return first.get(fk.second) + cells.get(Head.FkHead(term.fk()))[from];
      }
      throw new RuntimeException("Egglog-chase compares rows of the result only, and not " + term);
    }

    private static String unused(String name, Set<String> used) {
      String ret = name;
      for (int i = 2; used.contains(ret); i++) {
        ret = name + "#" + i;
      }
      used.add(ret);
      return ret;
    }
  }

  /**
   * What egglog prints for a term: a name applied to terms, as in {@code (F2_f (F0_a))}, or a bare
   * word, such as a number.
   */
  private static final class Sexp {
    final String head;
    final List<Sexp> args;
    final boolean bare;
    private final String line;
    private final int from;
    private final int to;
    private String text;

    private Sexp(String head, List<Sexp> args, boolean bare, String line, int from, int to) {
      this.head = head;
      this.args = args;
      this.bare = bare;
      this.line = line;
      this.from = from;
      this.to = to;
    }

    /** As egglog printed it, which egglog also reads. */
    String text() {
      if (text == null) {
        text = line.substring(from, to);
      }
      return text;
    }

    int number() {
      try {
        return Integer.parseInt(head);
      } catch (NumberFormatException e) {
        throw unreadable(line);
      }
    }

    /** The first term of a line. */
    static Sexp read(String line) {
      return read(line, new int[] {0});
    }

    /**
     * The rows of a table as {@code print-function} prints them: a line with an opening bracket,
     * for each row a line with the table applied to its columns, an arrow and what that makes, and
     * a line with a closing bracket.
     */
    static List<Sexp> rows(List<String> printed, String table, int columns) {
      List<Sexp> ret = new ArrayList<>(printed.size());
      for (String line : printed) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.equals("(") || trimmed.equals(")")) {
          continue;
        }
        Sexp row = read(line);
        if (!row.head.equals(table) || row.args.size() != columns) {
          throw unreadable(line);
        }
        ret.add(row);
      }
      return ret;
    }

    private static Sexp read(String line, int[] at) {
      int from = skipSpaces(line, at[0]);
      if (from == line.length() || line.charAt(from) == ')') {
        throw unreadable(line);
      }
      boolean bare = line.charAt(from) != '(';
      int start = bare ? from : skipSpaces(line, from + 1);
      int end = start;
      while (end < line.length() && " ()\t\"".indexOf(line.charAt(end)) < 0) {
        end++;
      }
      if (end == start) {
        throw unreadable(line);
      }
      String head = line.substring(start, end);
      at[0] = end;
      if (bare) {
        return new Sexp(head, List.of(), true, line, from, end);
      }
      List<Sexp> args = new ArrayList<>();
      while (true) {
        int next = skipSpaces(line, at[0]);
        if (next == line.length()) {
          throw unreadable(line);
        } else if (line.charAt(next) == ')') {
          at[0] = next + 1;
          return new Sexp(head, args, false, line, from, next + 1);
        }
        at[0] = next;
        args.add(read(line, at));
      }
    }

    private static int skipSpaces(String line, int at) {
      int i = at;
      while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
        i++;
      }
      return i;
    }

    private static RuntimeException unreadable(String line) {
      return new RuntimeException("CQL cannot read what egglog printed: " + line);
    }
  }

  /**
   * An egglog that CQL started with {@code --mode interactive}. CQL writes commands to its standard
   * input, one a line, in UTF-8, and for each line egglog writes what the command prints to its
   * standard output, and then {@code (done)}, or {@code (error)} alone. Two threads follow egglog's
   * standard output and standard error, so that egglog never waits for CQL to read, and so that the
   * thread that waits for an answer can be interrupted: an interrupt, such as the timeout of a
   * definition, ends egglog.
   */
  static final class Session implements AutoCloseable {
    private static final String DONE = "(done)";
    private static final String ERROR = "(error)";

    /** How many commands are sent before their answers are read. */
    private static final int BATCH = 512;

    /** Marks the end of egglog's standard output. */
    private static final Object END = new Object();

    private final String path;
    private final Process process;
    private final Writer commands;
    private final BlockingQueue<Object> answers = new LinkedBlockingQueue<>();
    private final Complaints complaints = new Complaints();

    static void requireFile(String exePath) {
      if (!new File(exePath).exists()) {
        throw new RuntimeException("File does not exist: " + exePath);
      }
    }

    static Session start(String exePath) {
      requireFile(exePath);
      try {
        return new Session(exePath, new ProcessBuilder(exePath, "--mode", "interactive").start());
      } catch (IOException e) {
        throw new RuntimeException("Cannot start egglog (" + exePath + "): " + e.getMessage(), e);
      }
    }

    private Session(String path, Process process) {
      this.path = path;
      this.process = process;
      commands =
          new BufferedWriter(
              new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      BlockingQueue<Object> out = answers;
      Complaints err = complaints;
      follow(process.getInputStream(), "egglog-output", out::add, () -> out.add(END));
      follow(process.getErrorStream(), "egglog-errors", err::add, err::end);
    }

    /** Neither thread refers to the session, so a prover that nothing uses can be collected. */
    private static void follow(InputStream in, String name, Consumer<String> each, Runnable end) {
      Thread thread =
          new Thread(
              () -> {
                try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                  for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    each.accept(line);
                  }
                } catch (IOException e) {
                  // egglog has ended
                } finally {
                  end.run();
                }
              },
              name);
      thread.setDaemon(true);
      thread.start();
    }

    /**
     * Has egglog run a program: its commands, and then its facts, which egglog reads from files
     * that CQL writes into a directory of its own and deletes once egglog has read them.
     */
    void load(Program program) {
      run(program.lines);
      if (program.facts.isEmpty()) {
        return;
      }
      File dir = null;
      try {
        dir = Files.createTempDirectory("cql-egglog").toFile();
        run(program.input(dir.toPath()));
      } catch (IOException e) {
        throw new RuntimeException("CQL cannot write the facts for egglog: " + e.getMessage(), e);
      } finally {
        if (dir != null) {
          delete(dir);
        }
      }
    }

    /** Deletes a directory and its files; what cannot be deleted now is deleted when Java ends. */
    private static void delete(File dir) {
      File[] files = dir.listFiles();
      List<File> left = new ArrayList<>();
      for (File file : files == null ? new File[0] : files) {
        if (!file.delete()) {
          left.add(file);
        }
      }
      if (!left.isEmpty() || !dir.delete()) {
        dir.deleteOnExit();
        left.forEach(File::deleteOnExit);
      }
    }

    /**
     * A string in egglog's syntax: in quotes, with a backslash before each backslash and quote, and
     * a newline and a tab written {@code \n} and {@code \t}.
     */
    static String quoted(String s) {
      StringBuilder sb = new StringBuilder("\"");
      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        switch (ch) {
          case '\\', '"' -> sb.append('\\').append(ch);
          case '\n' -> sb.append("\\n");
          case '\t' -> sb.append("\\t");
          default -> sb.append(ch);
        }
      }
      return sb.append('"').toString();
    }

    /** Sends commands and waits until egglog has run each. One that egglog refuses ends it. */
    void run(List<String> lines) {
      ask(lines, false);
    }

    /** Sends commands and returns what egglog printed for each. */
    List<List<String>> ask(List<String> lines) {
      return ask(lines, true);
    }

    private List<List<String>> ask(List<String> lines, boolean keep) {
      List<List<String>> ret = new ArrayList<>(keep ? lines.size() : 0);
      for (int from = 0; from < lines.size(); from += BATCH) {
        List<String> batch = lines.subList(from, Math.min(from + BATCH, lines.size()));
        long before = complaints.count();
        send(batch);
        for (String command : batch) {
          List<String> printed = keep ? new ArrayList<>() : null;
          if (!answer(printed)) {
            String said = complaints.since(before);
            close();
            throw new RuntimeException(
                "egglog refused a command of CQL's: "
                    + command
                    + (said.isEmpty() ? "" : "\n" + said));
          }
          if (keep) {
            ret.add(printed);
          }
        }
      }
      return ret;
    }

    /** Sends a check, and says whether egglog finds that it holds. */
    boolean holds(String check) {
      send(List.of(check));
      return answer(null);
    }

    private void send(List<String> lines) {
      try {
        for (String line : lines) {
          commands.write(line);
          commands.write('\n');
        }
        commands.flush();
      } catch (IOException e) {
        throw ended();
      }
    }

    /** Reads up to the mark that ends a line of commands: true for (done), false for (error). */
    private boolean answer(List<String> printed) {
      while (true) {
        Object line;
        try {
          line = answers.take();
        } catch (InterruptedException e) {
          close();
          Thread.currentThread().interrupt();
          throw new RuntimeInterruptedException(e);
        }
        if (line == END) {
          answers.add(END);
          throw ended();
        } else if (DONE.equals(line)) {
          return true;
        } else if (ERROR.equals(line)) {
          return false;
        } else if (printed != null) {
          printed.add((String) line);
        }
      }
    }

    private RuntimeException ended() {
      String said = complaints.atEnd();
      close();
      return new RuntimeException(
          "egglog ("
              + path
              + ") ended before it answered. CQL needs egglog 3.0.0 or later, which it starts"
              + " with --mode interactive."
              + (said.isEmpty() ? "" : "\n" + said));
    }

    /** Ends egglog; what a prover that is no longer used runs. It does not refer to the session. */
    Runnable stopper() {
      Process p = process;
      return p::destroy;
    }

    @Override
    public void close() {
      process.destroy();
    }
  }

  /** The last lines of egglog's standard error, to quote when it refuses a command or ends. */
  private static final class Complaints {
    private static final int KEPT = 20;
    private final ArrayDeque<String> last = new ArrayDeque<>();
    private long count = 0;
    private boolean ended = false;

    synchronized void add(String line) {
      if (last.size() == KEPT) {
        last.removeFirst();
      }
      last.addLast(line);
      count++;
      notifyAll();
    }

    synchronized void end() {
      ended = true;
      notifyAll();
    }

    synchronized long count() {
      return count;
    }

    /** The lines that came after the first so-many, waiting a moment for them to arrive. */
    synchronized String since(long before) {
      await(() -> count > before || ended, 500);
      await(() -> ended, 50);
      int n = (int) Math.min(count - before, last.size());
      List<String> lines = new ArrayList<>(last);
      return String.join("\n", lines.subList(lines.size() - n, lines.size()));
    }

    /** All the lines kept, once standard error has ended or a moment has passed. */
    synchronized String atEnd() {
      await(() -> ended, 500);
      return String.join("\n", last);
    }

    private void await(BooleanSupplier reached, long millis) {
      long deadline = System.currentTimeMillis() + millis;
      while (!reached.getAsBoolean()) {
        long left = deadline - System.currentTimeMillis();
        if (left <= 0) {
          return;
        }
        try {
          wait(left);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }
}
