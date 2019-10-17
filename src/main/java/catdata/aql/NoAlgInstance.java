package catdata.aql;

import java.util.Map;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;

public class NoAlgInstance extends Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Void, Void> {

	public final Schema<Ty, En, Sym, Fk, Att> schema;

	private DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp;

	public final AqlOptions options;

	private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;

	public NoAlgInstance(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, Schema<Ty, En, Sym, Fk, Att> schema,
			AqlOptions options) {
		Util.assertNotNull(options);
		this.schema = schema;
		this.options = options;
		this.col = col;
		validateNoTalg();
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public Map<Gen, En> gens() {
		return col.gens;
	}

	@Override
	public Map<Sk, Ty> sks() {
		return col.sks;
	}

	@Override
	public Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs() {
		return col.eqsAsPairs(); // eqs.stream().map(x -> new Pair<>(x.lhs, x.rhs)).collect(Collectors.toSet());
	}

	@Override
	public synchronized DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		if (dp == null) {
			dp = AqlProver.createInstance(options, collage(), schema);
			return dp;
		}
		return dp;
	}

	@Override
	public synchronized Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, Void, Void> algebra() {
		return Util.anomaly();
	}

	@Override
	public boolean requireConsistency() {
		return (boolean) options.getOrDefault(AqlOption.require_consistency);
	}

	@Override
	public boolean allowUnsafeJava() {
		return (boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe);
	}

	

}