package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Head;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.Query;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;
import catdata.aql.fdm.CoEvalTransform;
import catdata.provers.EProver;
import catdata.provers.KBTheory;
import gnu.trove.map.hash.THashMap;

public class PragmaExpCheck2 extends PragmaExp {
	public QueryExp Q;
	public EdsExp C;
	public EdsExp D;

	public PragmaExpCheck2(QueryExp q, EdsExp c, EdsExp d) {
		Q = q;
		C = c;
		D = d;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((C == null) ? 0 : C.hashCode());
		result = prime * result + ((D == null) ? 0 : D.hashCode());
		result = prime * result + ((Q == null) ? 0 : Q.hashCode());
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
		PragmaExpCheck2 other = (PragmaExpCheck2) obj;
		if (C == null) {
			if (other.C != null)
				return false;
		} else if (!C.equals(other.C))
			return false;
		if (D == null) {
			if (other.D != null)
				return false;
		} else if (!D.equals(other.D))
			return false;
		if (Q == null) {
			if (other.Q != null)
				return false;
		} else if (!Q.equals(other.Q))
			return false;
		return true;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public String toString() {
		return "check_query " + Q + " " + C + " " + D;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		return new Pragma() {
			String ret = "";

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void execute() {
				Constraints D0 = D.eval(env, isC);
				if (D0.eds.isEmpty()) {
					return;
				}
				Constraints C0 = C.eval(env, isC);
				for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : C0.schema.eqs) {

					Map<Var, Chc<Ty, En>> as = Collections.singletonMap(eq.first.first, Chc.inRight(eq.first.second));

					Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> ew = new Pair<>(
							eq.second, eq.third);

					C0.eds.add(new ED(as, new THashMap<>(), Collections.emptySet(), Collections.singleton(ew), false, env.defaults));
				}
				
				Query<Ty, En, Sym, Fk, Att, En, Fk, Att> Q0 = Q.eval(env, isC);
				
				
				List<ED> set = new LinkedList<>();
				for (Transform<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> h : D0.asTransforms(Q0.dst)) {
					ED ed = new ED(env.defaults, new CoEvalTransform(Q0, h, env.defaults, env.defaults));
					set.add(ed);
				}
				Constraints C1 = new Constraints(Q0.src, set, env.defaults);
				int[] i = new int[] { 0 };
				Long timeout = (long) env.defaults.getOrDefault(AqlOption.timeout);
				String exePath = (String) env.defaults.getOrDefault(AqlOption.e_path);

				KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Object, Object>, Var> kb = C0.schema.collage().toKB();

				String s1 = C0.tptp("axiom", true, kb);
				
				for (ED ed : C1.eds) {
					String s2 = ed.tptp("conjecture", false, kb);
					Pair<Optional<Boolean>, String> b = EProver.check(exePath, timeout, s1 + "\n" + s2);
					if (b.first.isEmpty()) {
						throw new RuntimeException("Out of resources");					
					}
					if (!b.first.get()) {
						throw new RuntimeException("Failed: " + C1);
					}				
				}
				
				ret = "Success\n\n" + C0.toString() + "\n\n\n==>\n\n\n" + C1.toString();
			}

			@Override
			public String toString() {
				return ret;
			}

		};
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(Q.deps(), Util.union(C.deps(), D.deps()));
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.map(f);
		C.map(f);
		D.map(f);
	}

}
