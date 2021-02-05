package catdata.aql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import catdata.Chc;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage.CCollage;
import catdata.provers.CompletionProver;
import catdata.provers.CongruenceProverUniform;
import catdata.provers.DPKB;
import catdata.provers.EProver;
import catdata.provers.FailProver;
import catdata.provers.FreeProver;
import catdata.provers.KBExp;
import catdata.provers.ProgramProver;
import catdata.provers.VampireProver;

//TODO: aql cache hashcode for term?

//TODO: aql maybe easier to check queries by translation into mappings?

//TODO: aql add abort functionality

//no java here!
public class AqlProver<Ty, En, Sym, Fk, Att, Gen, Sk> implements DP<Ty, En, Sym, Fk, Att, Gen, Sk> {

	private final KBtoDP<Ty, En, Sym, Fk, Att, Gen, Sk> dp;

	public enum ProverName {
		auto, monoidal, program, completion, congruence, fail, free, e, vampire
	}

	// these provers say that x = y when that is true when all java symbols are
	// treated as free,
	// or if x and y reduce to the same java normal form. as such, the provers won't
	// actually
	// decide, for example, that e = 2 -> e + 1 = 3. So the DP is not necessarily a
	// decision
	// procedure for the input theory + java - or even any theory at all, because
	// you may not have
	// x = y and y = z -> x = z when java is involved.

	private AqlProver(AqlOptions ops, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, AqlJs<Ty, Sym> js, boolean doTrivialityCheck) {
		ProverName name = (ProverName) ops.getOrDefault(AqlOption.prover);
		long timeout = (Long) ops.getOrDefault(AqlOption.timeout);
		Integer shouldSimplifyMax = (Integer) ops.getOrDefault(AqlOption.prover_simplify_max);
		boolean shouldSimplify = col.eqs().size() < shouldSimplifyMax;
		boolean allowNew = (boolean) ops.getOrDefault(AqlOption.prover_allow_fresh_constants);

		Function<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> fn;
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col_simpl;
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col_big;

		if (col.eqs().size() == 0 && name.equals(ProverName.auto)) {
			shouldSimplify = false;
			name = ProverName.free;
		}

		if (shouldSimplify) {
			CollageSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk> simp = new CollageSimplifier<>(col);
			col_simpl = simp.simplified;
			col_big = col;
			fn = simp.simp;
		} else {
			col_simpl = col;
			col_big = col;
			fn = Function.identity();
		}

		if (name.equals(ProverName.auto)) {
			name = auto(ops, col_simpl);
			if (name == null) {
				RuntimeException ex = new RuntimeException(
						"Cannot automatically chose prover: theory is not free, ground, monoidal, or program.  Possible solutions include: \n\n0) Upgrade to Conexus CQL \n\n1) use the completion prover, possibly with an explicit precedence (see KB example) \n\n2) Reorient equations from left to right to obtain a size-reducing orthogonal rewrite system \n\n3) Remove all equations involving function symbols of arity > 1 \n\n4) Remove all type side and schema equations \n\n5) disable checking of equations in queries using dont_validate_unsafe=true as an option \n\n6) adding options program_allow_nontermination_unsafe=true \n\n7) Switching to the e prover, as described in the CQL manual \n\n8) emailing support, info@conexus.ai\n\n\n"
								+ col_simpl);
				ex.printStackTrace();
				throw ex;
			}
		}

		DPKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> dpkb = switch (name) {
  		case auto -> throw new RuntimeException("Anomaly: please report");
  		case fail -> new FailProver<>(null);
  		case free -> dpkb = new FreeProver<>(col_simpl.toKB());
  		case congruence -> new CongruenceProverUniform<>(col_simpl.toKB());
  		case program -> {
        boolean check = !(Boolean) ops.getOrDefault(AqlOption.dont_verify_is_appropriate_for_prover_unsafe);
  			boolean check2 = !(Boolean) ops.getOrDefault(AqlOption.program_allow_nonconfluence_unsafe);
  			check = check && check2;
  			boolean allowNonTerm = (Boolean) ops.getOrDefault(AqlOption.program_allow_nontermination_unsafe);
  			try {
  				if (!allowNonTerm) {
  					col_simpl = reorient(col_simpl);
  				}
  			} catch (Exception ex) {
  				throw new RuntimeException(ex.getMessage()
  						+ "\n\nPossible solution: add options program_allow_nontermination_unsafe=true, or prover=completion");
  			}
  			yield new ProgramProver<>(check, VarIt.it(), col_simpl.toKB());
      }
  		case completion -> {
        String str = (String) ops.getOrDefault(AqlOption.completion_precedence);

  			List<Head<Ty, En, Sym, Fk, Att, Gen, Sk>> prec = null;
  			if (str != null) {
  				List<String> z = Arrays.asList(str.trim().split("\\s+"));
  				prec = new ArrayList<>(z.size());
  				Collage<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, catdata.aql.exp.Sk> col2 = (Collage<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, catdata.aql.exp.Sk>) col;
  				for (String x : z) {
  					try {
  						Head o = catdata.aql.exp.RawTerm.toHeadNoPrim(x, col2);
  						prec.add(o);
  					} catch (Exception ex) {
  					}
  				}
  			}

  			yield new CompletionProver<>(ops, col_simpl.toKB(), prec);
      }
  		case monoidal -> new catdata.aql.MonoidalFreeDP<>(col_simpl.toKB());
  		case e -> {
        String exePath = (String) ops.getOrDefault(AqlOption.e_path);
  			yield new EProver<>(exePath, col_simpl.toKB(), timeout);
      }
      case vampire -> {
        String exePath = (String) ops.getOrDefault(AqlOption.vampire_path);
  			yield new VampireProver<>(exePath, col_simpl.toKB(), timeout);
      }
		};

		if (doTrivialityCheck &&
        (boolean) ops.getOrDefault(AqlOption.triviality_check_best_effort) &&
        dpkb.supportsTrivialityCheck()) {
  		List<Chc<Ty, En>> trivial = Util.list();
      try {
        Var x = Var.Var(Util.uniqueName()), y = Var.Var(Util.uniqueName());
  			KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> l = dpkb.kb.factory.KBVar(x), r = dpkb.kb.factory.KBVar(y);
  			for (Chc<Ty, En> sort : dpkb.kb.inhabGen()) {
  				Map<Var, Chc<Ty, En>> m = Util.mk();
  				m.put(x, sort);
  				m.put(y, sort);
  				if (dpkb.eq(m, l, r)) {
  					trivial.add(sort);
  				}
  			}
      } catch (Exception e) {
        throw new RuntimeException("Error in triviality check: " + e.getMessage() + "\nConsider setting the option triviality_check_best_effort = false", e);
      }
			if (!trivial.isEmpty()) {
				throw new RuntimeException("Trivial sorts detected: " + Util.map(trivial, Chc::toStringMash));
			}
		}

		dp = new KBtoDP<>(js, fn, dpkb, allowNew, col_big);
	}

	private static <Sk, En, Fk, Ty, Att, Sym, Gen> ProverName auto(AqlOptions ops,
			Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
		if (col.eqs().isEmpty()) {
			return ProverName.free;
		} else if (col.isGround()) {
			return ProverName.congruence;
		} else if (MonoidalFreeDP.ok(col)) {
			return ProverName.monoidal;
		} else if (!(Boolean) ops.getOrDefault(AqlOption.program_allow_nontermination_unsafe) && reorientable(col)
				&& ProgramProver.isProgram(VarIt.it(), reorient(col).toKB(), false)
				|| (Boolean) ops.getOrDefault(AqlOption.program_allow_nontermination_unsafe)
						&& ProgramProver.isProgram(VarIt.it(), col.toKB(), false)) {
			return ProverName.program;
		}
		return null;
	}

	private static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean reorientable(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
		try {
			reorient(col);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	private static <Ty, En, Sym, Fk, Att, Gen, Sk> Collage<Ty, En, Sym, Fk, Att, Gen, Sk> reorient(
			Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> ret = new CCollage<>(col);
		ret.eqs().clear();
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : col.eqs()) {
			if (size(eq.lhs) < size(eq.rhs)) {
				ret.eqs().add(new Eq<>(eq.ctx, eq.rhs, eq.lhs));
			} else if (size(eq.lhs) > size(eq.rhs) && numOccs(eq.lhs, eq.rhs)) {
				ret.eqs().add(eq);
			} else {
				throw new RuntimeException("Cannot orient " + eq + " in a size reducing manner.");
			}

		}
		return ret;
	}

	private static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean numOccs(Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
		for (Var v : lhs.vars()) {
			if (!(lhs.vars().stream().filter((x) -> x.equals(v)).count() >= rhs.vars().stream()
					.filter((x) -> x.equals(v)).count())) {
				return false;
			}
		}
		return true;
	}

	private static <Ty, En, Sym, Fk, Att, Gen, Sk> int size(Term<Ty, En, Sym, Fk, Att, Gen, Sk> e) {
		if (e.obj() != null || e.gen() != null || e.sk() != null || e.var != null) {
			return 1;
		} else if (e.att() != null || e.fk() != null) {
			return 1 + size(e.arg);
		}
		int ret = 1;
		for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : e.args) {
			ret += size(arg);
		}
		return ret;
	}

	@Override
	public String toString() {
		return dp.toStringProver();
	}

	@Override
	public String toStringProver() {
		return dp.toStringProver();
	}

	@Override
	public synchronized boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
		return dp.eq(ctx, lhs, rhs);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> DP<Ty, En, Sym, Fk, Att, Gen, Sk> createInstance(AqlOptions options,
			Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, Schema<Ty, En, Sym, Fk, Att> schema) {
		return new AqlProver<>(options, col, schema.typeSide.js, false);
	}

	public static <Ty, En, Sym, Fk, Att> DP<Ty, En, Sym, Fk, Att, Void, Void> createSchema(AqlOptions options,
			Collage<Ty, En, Sym, Fk, Att, Void, Void> col, TypeSide<Ty, Sym> typeSide) {
		return new AqlProver<>(options, col, typeSide.js, true);
	}

	public static <Ty, Sym> DP<Ty, Void, Sym, Void, Void, Void, Void> createTypeSide(AqlOptions options,
			Collage<Ty, Void, Sym, Void, Void, Void, Void> col, AqlJs<Ty, Sym> js) {
		return new AqlProver<>(options, col, js, true);
	}

}
