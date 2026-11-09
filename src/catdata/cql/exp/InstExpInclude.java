package catdata.cql.exp;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.DP;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.fdm.LiteralInstance;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class InstExpInclude<Gen, Sk, X, Y> extends InstExp<Gen, Sk, X, Y> {

  public final InstExp<Gen, Sk, X, Y> I;
  public final SchExp S;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.map(f);
    S.map(f);
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.singleton(I);
  }

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public InstExpInclude(InstExp<Gen, Sk, X, Y> i, SchExp s) {
    I = i;
    S = s;
  }

  @Override
  public SchExp type(AqlTyping G) {
    S.type(G);
    I.type(G);
    return S;
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> eval0(
      AqlEnv env, boolean isC) {
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> i = I.eval(env, isC);
    var s = S.eval(env, isC);

    DP<String, String, Sym, Fk, Att, Gen, Sk> dp =
        new DP<String, String, Sym, Fk, Att, Gen, Sk>() {

          @Override
          public String toStringProver() {
            return "InstExpInclude of " + i.dp().toStringProver();
          }

          @Override
          public boolean eq(
              Map<String, Chc<String, String>> ctx,
              Term<String, String, Sym, Fk, Att, Gen, Sk> lhs,
              Term<String, String, Sym, Fk, Att, Gen, Sk> rhs) {
            try {
              i.dp().eq(ctx, lhs, rhs);
            } catch (Exception ex) {

            }
            return false;
          }
        };

    Algebra<String, String, Sym, Fk, Att, Gen, Sk, X, Y> alg =
        new Algebra<String, String, Sym, Fk, Att, Gen, Sk, X, Y>() {

          @Override
          public Schema<String, String, Sym, Fk, Att> schema() {
            return s;
          }

          @Override
          public boolean hasNulls() {
            return i.algebra().hasNulls();
          }

          @Override
          public Iterable<X> en(String en) {
            if (i.schema().ens.contains(en)) {
              return i.algebra().en(en);
            }
            return Collections.emptyList();
          }

          @Override
          public X gen(Gen gen) {
            return i.algebra().gen(gen);
          }

          @Override
          public X fk(Fk fk, X x) {
            return i.algebra().fk(fk, x);
          }

          @Override
          public Term<String, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {

            if (!i.schema().atts.containsKey(att)) {
              throw new RuntimeException(att + " on " + x);
            }
            return i.algebra().att(att, x);
          }

          @Override
          public Term<String, Void, Sym, Void, Void, Void, Y> sk(Sk sk) {
            return i.algebra().sk(sk);
          }

          @Override
          public Term<Void, String, Void, Fk, Void, Gen, Void> repr(String en, X x) {
            return i.algebra().repr(en, x);
          }

          @Override
          public int size(String en) {
            if (i.schema().ens.contains(en)) {
              return i.algebra().size(en);
            }
            return 0;
          }

          @Override
          protected TAlg<String, Sym, Y> talg0() {
            return i.algebra().talg();
          }

          @Override
          public Chc<Sk, Pair<X, Att>> reprT_prot(Y y) {
            return i.algebra().reprT_prot(y);
          }

          @Override
          public String toStringProver() {
            return "Inst Exp Include Inst " + i.algebra().toStringProver();
          }

          @Override
          public Object printX(String en, X x) {
            return i.algebra().printX(en, x);
          }

          @Override
          public Object printY(String ty, Y y) {
            return i.algebra().printY(ty, y);
          }
        };

    List<
            Pair<
                Term<String, String, Sym, Fk, Att, Gen, Sk>,
                Term<String, String, Sym, Fk, Att, Gen, Sk>>>
        it = new LinkedList<>();
    i.eqs((x, y) -> it.add(new Pair<>(x, y)));

    return new LiteralInstance<String, String, Sym, Fk, Att, Gen, Sk, X, Y>(
        s,
        Instance.imapToMapNoScan(i.gens()),
        Instance.imapToMapNoScan(i.sks()),
        it,
        dp,
        alg,
        false,
        true);
  }

  @Override
  public String toString() {
    return "include " + I + " : S";
  }

  @Override
  public int hashCode() {
    return Objects.hash(I, S);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    InstExpInclude other = (InstExpInclude) obj;
    return Objects.equals(I, other.I) && Objects.equals(S, other.S);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(S.deps(), I.deps());
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {}
}
