package catdata.aql.fdm;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.AqlProver;
import catdata.aql.AqlProver.ProverName;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Schema;
import catdata.aql.Term;

import catdata.aql.exp.IgnoreException;
import catdata.graph.DAG;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

public final class InitialAlgebra<Ty, En, Sym, Fk, Att, Gen, Sk>
    extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>>
    implements DP<Ty, En, Sym, Fk, Att, Gen, Sk> {

  @Override
  public boolean hasNulls() {
    return talg().sks.isEmpty();
  }

  public synchronized Object printX(En en, Integer x) {
    return repr(en, x).toString(Util.voidFn(), z -> printGen.apply(z).toString());
  }

  @Override
  public synchronized Object printY(Ty ty, Chc<Sk, Pair<Integer, Att>> y) {
    return y.left ? printSk.apply(ty, y.l)
        : printX(schema.atts.get(y.r.second).first, y.r.first) + "." + y.r.second;
  }

  private final Map<En, TIntHashSet> ens;
  private final TIntObjectHashMap<TObjectIntHashMap<Fk>> fks = new TIntObjectHashMap<>(16, .75f, -1);

  private final TIntObjectHashMap<Term<Void, En, Void, Fk, Void, Gen, Void>> reprs = new TIntObjectHashMap<>(16, .75f,
      -1);

  private final TObjectIntHashMap<Term<Void, En, Void, Fk, Void, Gen, Void>> nfs = new TObjectIntHashMap<>(16, .75f,
      -1);

  private final Schema<Ty, En, Sym, Fk, Att> schema;

  private Function<Gen, Object> printGen;
  private BiFunction<Ty, Sk, Object> printSk;

  private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;

  private final DP<Void, En, Void, Fk, Void, Gen, Void> dp_en;
  private final DP<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> dp_ty;

  private TalgSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> talg;

  int fresh = 0;
  private final boolean talg_is_cons;

  @SuppressWarnings("unchecked")
  public InitialAlgebra(AqlOptions ops, Schema<Ty, En, Sym, Fk, Att> schema, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> o,
      Function<Gen, Object> printGen, BiFunction<Ty, Sk, Object> printSk) {
    // col.validate();
    this.schema = schema;
    Util.assertNotNull(printGen, printSk);
    this.printGen = printGen;
    this.printSk = printSk;
    this.col = new CCollage<>(o);
    Collage<Ty, En, Sym, Fk, Att, Gen, Sk> zzz = new CCollage<>();
    zzz.getEns().addAll(schema.ens);
    zzz.fks().putAll(schema.fks);
    zzz.gens().putAll(o.gens());
    for (Triple<Pair<String, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schema.eqs) {
      if (!schema.type(eq.first, eq.second).left) {
        zzz.eqs().add(new Eq<>(Collections.singletonMap(eq.first.first, Chc.inRight(eq.first.second)),
            eq.second.convert(), eq.third.convert()));
      }
    }
    zzz.validate();
    // System.out.println("ZZZ " + zzz);
    for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : col.eqs()) {
      if (!eq.ctx.isEmpty()) {
        continue; // easier for clients to pass in typeside equations so we dont abort here
      }
      if (!eq.lhs.hasTypeType()) {
        zzz.eqs().add(eq);
      }
    }
    // Collage<Ty, En, Sym, Fk, Att, Gen, Sk> zzz = col.entities_only(schema);
    // zzz.addAll(schema.collage());
    int limit = (int) ops.getOrDefault(AqlOption.diverge_limit);
    boolean warn = (boolean) ops.getOrDefault(AqlOption.diverge_warn);
    boolean fast = (boolean) ops.getOrDefault(AqlOption.fast_consistency_check);
    // zzz.validate();
    // System.out.println(col.eqs);
    checkTermination(schema, zzz.gens().size(), zzz.eqs().size(), warn, limit);

    this.dp_en = (DP<Void, En, Void, Fk, Void, Gen, Void>) AqlProver.createInstance(ops, zzz, schema);

    ens = new THashMap<>(schema.ens.size(), 2);
    for (En en : schema.ens) {
      ens.put(en, new TIntHashSet(128, 1, -1));
    }
    while (saturate1(col))
      ;

    talg = new TalgSimplifier<>(this, col.eqsAsPairs().iterator(), col.sks(),
        (Integer) ops.getOrDefault(AqlOption.talg_reduction));

    ProverName p = (ProverName) ops.getOrDefault(AqlOption.second_prover);
    AqlOptions lll = new AqlOptions(ops, AqlOption.prover, p);
    lll = new AqlOptions(lll, AqlOption.completion_precedence, null);

    col.addAll(schema.collage());
    col.addAll(schema.typeSide.collage());
    // System.out.println("col1 is " + col);

    // TODO AQL performance
    if (!fast && !talg.talg.out.eqsNoDefns().isEmpty() && !schema().typeSide.eqs.isEmpty()) {
      boolean b = false;
      // Util.anomaly();
      talg_is_cons = !b;
    } else {
      talg_is_cons = super.hasFreeTypeAlgebra();
    }

    this.dp_ty = AqlProver.createInstance(lll, talg().toCollage(schema().typeSide, true),
        Schema.terminal(schema().typeSide));

  //  code = System.identityHashCode(col);

  }

//  private final int code;

  private void checkTermination(Schema<Ty, En, Sym, Fk, Att> c, int genSize, int eqSize, boolean check, int limit) {
    if (!check || c.fks.size() > limit || genSize == 0 || c.eqs.size() > 0 || eqSize > 0) {
      return;
    }
    DAG<En> dag = new DAG<>();

    for (Entry<Fk, Pair<En, En>> fk : c.fks.entrySet()) {
      if (!dag.addEdge(fk.getValue().second, fk.getValue().first)) {
        throw new RuntimeException(
            "An instance with a cyclic schema, generators, and no equations may diverge.  Set diverge_warn=false to continue.  This warning may be the benign consequence of theory simplification; consider prover_simplify_max = 0 as well.");
      }
    }

  }

  private boolean add(En en, Term<Void, En, Void, Fk, Void, Gen, Void> term) {
    int x = nf0(en, term);
    if (x != -1) {
      return false;
    }
    x = fresh++;

    nfs.put(term, x);
    ens.get(en).add(x);
    reprs.put(x, term);

    TObjectIntHashMap<Fk> map = new TObjectIntHashMap<>(16, .75f, -1);
    for (Fk fk : schema().fksFrom(en)) {
      En e = schema().fks.get(fk).second;
      Term<Void, En, Void, Fk, Void, Gen, Void> z = Term.Fk(fk, term);
      add(e, z);
      map.put(fk, nf0(e, z));
    }
    fks.put(x, map);

    if (fresh % 10000 == 0) {
      if (Thread.currentThread().isInterrupted()) {

        throw new IgnoreException();
      }
    }
    return true;
  }

  private boolean saturate1(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
    boolean changed = false;
    for (Gen gen : col.gens().keySet()) {
      En en = col.gens().get(gen);
      Term<Void, En, Void, Fk, Void, Gen, Void> xx = Term.Gen(gen);
      changed = changed | add(en, xx);
    }
    for (Fk fk : schema().fks.keySet()) {
      Pair<En, En> e = schema().fks.get(fk);
      TIntIterator it = ens.get(e.first).iterator();
      while (it.hasNext()) {
        int x = it.next();
        changed = changed | add(e.second, Term.Fk(fk, repr(e.first, x)));
      }
    }
    return changed;
  }

  @Override
  public Schema<Ty, En, Sym, Fk, Att> schema() {
    return schema;
  }

  @Override
  public Collection<Integer> en(En en) {
    Collection<Integer> ret = new AbstractCollection<>() {
      @Override
      public Iterator<Integer> iterator() {
        TIntIterator it = ens.get(en).iterator();
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return it.hasNext();
          }

          @Override
          public Integer next() {
            return it.next();
          }

        };
      }

      @Override
      public int size() {
        return ens.get(en).size();
      }
    };

    int j = 0;
    for (@SuppressWarnings("unused") Integer i : ret) {
      j++;
    }
    if (j != ret.size()) {
      Util.anomaly();
    }
    return ret;
  }

  @Override
  public Integer fk(Fk fk, Integer x) {
    Integer r = fks.get(x).get(fk);
    return r;
  }

  @Override
  public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, Integer x) {
    Term<Void, En, Void, Fk, Void, Gen, Void> ret = reprs.get(x);
    return ret;
  }

  private synchronized int nf0(En en, Term<Void, En, Void, Fk, Void, Gen, Void> term) {
    int xx = nfs.get(term);
    if (xx != -1) {
      return xx;
    }
    TIntIterator it = ens.get(en).iterator();
    while (it.hasNext()) {
      int x = it.next();
      if (dp_en.eq(null, term, repr(en, x))) {
        nfs.put(term, x);
        return x;
      }
    }
    return -1;
  }

  @Override
  public Integer gen(Gen gen) {
    Integer x = nf0(col.gens().get(gen), Term.Gen(gen));
    return x;
  }

  @Override
  public synchronized boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
      Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
    if (ctx != null && !ctx.isEmpty()) {
      throw new RuntimeException("Cannot answer a non-ground equation");
    }
    // System.out.println(System.identityHashCode(col) + "in eq col is " + col);

    Chc<Ty, En> x = col.type(Collections.emptyMap(), lhs);
    if (!x.left) {
      return nf0(x.r, lhs.convert()) == nf0(x.r, rhs.convert());
    }
    return dp_ty.eq(null, intoY(lhs.convert()), intoY(rhs.convert()));
  }

  @Override
  public synchronized TAlg<Ty, Sym, Chc<Sk, Pair<Integer, Att>>> talg0() {
    return talg.talg.out;
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> att(Att att, Integer x) {
    return reprT0(Chc.inRight(new Pair<>(x, att)));
  }

  private Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> reprT0(Chc<Sk, Pair<Integer, Att>> y) {
    talg();
    return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(y))
        : schema.typeSide.js.reduce(talg.simpl(Term.Sk(y)));
  }

  public boolean hasFreeTypeAlgebra() {
    return talg_is_cons;
  }

  /*
   * public boolean hasFreeTypeAlgebraOnJava() { return
   * talg().eqsNoDefns().stream() .filter(x ->
   * schema().typeSide.js.java_tys.containsKey(talg().type(schema().typeSide,
   * x.first))) .collect(Collectors.toList()).isEmpty(); }
   */

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> sk(Sk sk) {
    return reprT0(Chc.inLeft(sk));
  }

  @Override
  public String toStringProver() {
    return dp_en + "\n\n-------------\n\n" + dp_ty.toStringProver();
  }

//    @Override
  public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
    return this; // definitely this - not dp bc dp may be for entity side only
  }

  public String talgToString() {
    if (talg == null) {
      return "";
    }
    return this.talg.toString();
  }

  @Override
  public int size(En en) {
    return ens.get(en).size();
  }

  @Override
  public Chc<Sk, Pair<Integer, Att>> reprT_prot(Chc<Sk, Pair<Integer, Att>> y) {
    return y;
  }

}
