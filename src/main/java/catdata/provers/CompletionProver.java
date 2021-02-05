package catdata.provers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.list.TreeList;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import catdata.Chc;
import catdata.Triple;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Head;
import catdata.aql.Var;
import catdata.aql.VarIt;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class CompletionProver<Ty, En, Sym, Fk, Att, Gen, Sk>
		extends DPKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> {

	private final LPOUKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> cp;

	public CompletionProver(AqlOptions ops, KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> xx,
			List<Head<Ty, En, Sym, Fk, Att, Gen, Sk>> prec2) {
		super(xx);
		boolean sort = (Boolean) ops.getOrDefault(AqlOption.completion_sort);
		boolean filter_subsumed = (Boolean) ops.getOrDefault(AqlOption.completion_filter_subsumed);
		boolean compose = (Boolean) ops.getOrDefault(AqlOption.completion_compose);
		boolean syntactic_ac = (Boolean) ops.getOrDefault(AqlOption.completion_syntactic_ac);
		boolean unfailing = (Boolean) ops.getOrDefault(AqlOption.completion_unfailing);

		Iterable<Triple<KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var>, KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var>, Map<Var, Chc<Ty, En>>>> E0 = Iterables
				.transform(kb.eqs, x -> new Triple<>(x.second, x.third, x.first));

		if (prec2 == null) {
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Integer> m = new THashMap<>();
			for (Head<Ty, En, Sym, Fk, Att, Gen, Sk> c : kb.syms.keySet()) {
				m.put(c, kb.syms.get(c).first.size());
			}
			prec2 = LPOUKB.inferPrec(m, E0);
		}

		List<Head<Ty, En, Sym, Fk, Att, Gen, Sk>> prec = new TreeList<>(prec2);
		KBOptions options = new KBOptions(unfailing, sort, false, true, Integer.MAX_VALUE, Integer.MAX_VALUE,
				filter_subsumed, compose, syntactic_ac); // this ignores all but 4 options, see LPOUKB

		// Util.assertNoDups(prec);

		Set<Head<Ty, En, Sym, Fk, Att, Gen, Sk>> sigMinusPrec = (new THashSet<>(kb.syms.keySet()));
		sigMinusPrec.removeAll(prec);
		if (!sigMinusPrec.isEmpty() && !kb.syms.keySet().isEmpty()) {

			throw new RuntimeException(
					"Incorrect precedence. Symbols in signature but not precedence: " + sigMinusPrec);
		}
		cp = new LPOUKB<>(Lists.newArrayList(E0), VarIt.it(), Collections.emptySet(), options, prec, kb);

	}

	@Override
	public boolean eq(Map<Var, Chc<Ty, En>> ctx, KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> lhs,
			KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> rhs) {
		return cp.eq(ctx, lhs, rhs);
	}

	@Override
	public String toString() {
		return cp.toString();
	}

	@Override
	public void add(Head<Ty, En, Sym, Fk, Att, Gen, Sk> c, Chc<Ty, En> t) {
		throw new RuntimeException("Completion does not support adding new constants.");
	}

	@Override
	public boolean supportsTrivialityCheck() {
		return cp.supportsTrivialityCheck();
	}

}
