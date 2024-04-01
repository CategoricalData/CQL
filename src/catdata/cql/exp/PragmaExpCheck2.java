package catdata.cql.exp;

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
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Head;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.CoEvalTransform;
import catdata.provers.EProver;
import catdata.provers.KBTheory;
import gnu.trove.map.hash.THashMap;

public class PragmaExpCheck2 extends PragmaExp {
	public QueryExp Q;
	public EdsExp C;
	public EdsExp D;

	@Override
	public Unit type(AqlTyping G) {
		Pair<SchExp, SchExp> x = Q.type(G);
		SchExp v1 = C.type(G);
		SchExp v2 = D.type(G);
		if (!x.first.equals(v1)) {
			throw new RuntimeException("Source of query: " + x.first + " does not match constraint schema " + v1);
		}
		if (!x.second.equals(v2)) {
			throw new RuntimeException("Target of query: " + x.second + " does not match constraint schema " + v2);
		}
		return Unit.unit;
	}

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

	public static Constraints extracted(Constraints D0, Constraints C0,
			Query<String, String, Sym, Fk, Att, String, Fk, Att> Q0, AqlOptions options) {
		if (Q0.hasAgg()) {
			throw new RuntimeException("Cannot validate aggregation");
		}
		// System.out.println("!!!!!!!!");

		for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : C0.schema.eqs) {

			Map<String, Chc<String, String>> as = Collections.singletonMap(eq.first.first,
					Chc.inRight(eq.first.second));

			Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> ew = new Pair<>(
					eq.second, eq.third);

			C0.eds.add(new ED(as, new THashMap<>(), Collections.emptySet(), Collections.singleton(ew), false, options));
		}

		List<ED> set = new LinkedList<>();
		for (Transform<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> h : D0.asTransforms(Q0.dst)) {
			// h.validate(false);
			var z = new CoEvalTransform(Q0, h, options, options);
			// z.validate(false);
			ED ed = new ED(options, z);
			set.add(ed);
			// ed.validate(Q0.src);
		}
		Constraints C1 = new Constraints(Q0.src, set, options);
		// int[] i = new int[] { 0 };
		Long timeout = (long) options.getOrDefault(AqlOption.timeout);
		String exePath = (String) options.getOrDefault(AqlOption.e_path);
		boolean auto = (boolean) options.getOrDefault(AqlOption.e_use_auto);

		KBTheory<Chc<String, String>, Head<String, String, Sym, Fk, Att, Object, Object>, String> kb = C0.schema
				.collage().toKB();

		String s1 = C0.tptp("axiom", true, kb);

		for (ED ed : C1.eds) {
			String s2 = ed.tptp("conjecture", false, kb);
			Pair<Optional<Boolean>, String> b = EProver.check(exePath, timeout, s1 + "\n" + s2, auto);
			if (b.first.isEmpty()) {
				throw new RuntimeException("Out of resources");
			}
			if (!b.first.get()) {
				throw new RuntimeException("Failed: " + C1);
			}
		}
		return C1;
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

				Query<String, String, Sym, Fk, Att, String, Fk, Att> Q0 = Q.eval(env, isC);

				Constraints C1 = extracted(D0, C0, Q0, env.defaults);

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
