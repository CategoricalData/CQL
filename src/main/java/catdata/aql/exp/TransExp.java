package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Transform;
import catdata.aql.exp.InstExp.InstExpLit;
import catdata.aql.exp.InstExp.InstExpVar;
import catdata.aql.fdm.IdentityTransform;

public abstract class TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
		extends Exp<Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>> {

	@Override
	public Kind kind() {
		return Kind.TRANSFORM;
	}

	public abstract Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> type(AqlTyping G);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Exp<Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>> Var(String v) {
		Exp ret = new TransExpVar(v);
		return ret;
	}

	public abstract <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E;

	public static final class TransExpId<Gen, Sk, X, Y> extends TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y> {

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			if (inst2.isEmpty()) {
				return inst.deps();
			}
			return Util.union(inst.deps(), inst2.get().deps());
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public final InstExp<Gen, Sk, X, Y> inst;
		public final Optional<InstExp<Gen, Sk, X, Y>> inst2;

		public TransExpId(InstExp<Gen, Sk, X, Y> inst) {
			this.inst = inst;
			this.inst2 = Optional.empty();
		}

		public TransExpId(InstExp<Gen, Sk, X, Y> inst, InstExp<Gen, Sk, X, Y> inst2) {
			this.inst = inst;
			this.inst2 = Optional.of(inst2);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + inst.hashCode();
			result = prime * result + inst2.hashCode();
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
			TransExpId<?, ?, ?, ?> other = (TransExpId<?, ?, ?, ?>) obj;
			return inst.equals(other.inst) && inst2.equals(other.inst2);
		}

		@Override
		public String toString() {
			if (inst2.isEmpty()) {
				return "identity " + inst;
			}
			return "include " + inst + " " + inst2.get();
		}

		@Override
		public synchronized Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> eval0(AqlEnv env, boolean isC) {
			if (inst2.isEmpty()) {
				return new IdentityTransform<>(inst.eval(env, isC), Optional.empty());
			}
			return new IdentityTransform<>(inst.eval(env, isC), Optional.of(inst2.get().eval(env, isC)));
		}

		@Override
		public Pair<InstExp<Gen, Sk, X, Y>, InstExp<Gen, Sk, X, Y>> type(AqlTyping G) {
			SchExp sch = inst.type(G);
			if (inst2.isEmpty()) {
				return new Pair<>(inst, inst);
			}
			SchExp sch2 = inst2.get().type(G);
			if (!sch.equals(sch2)) {
				throw new RuntimeException("Schema mismatch: " + sch + " and " + sch2);
			}
			if (inst instanceof InstExpVar && inst2.get() instanceof InstExpVar) {
				if (!((G.prog.exps.get(((InstExpVar)inst).var) instanceof InstExpSigma) || (G.prog.exps.get(((InstExpVar)inst).var) instanceof InstExpRaw))) {
					throw new RuntimeException(inst + " not bound to a literal or sigma instance, as required for inclusion.");
				}
				if (!((G.prog.exps.get(((InstExpVar)inst2.get()).var) instanceof InstExpSigma) || (G.prog.exps.get(((InstExpVar)inst2.get()).var) instanceof InstExpRaw))) {
					throw new RuntimeException(inst2.get() + " not bound to a literal instance, as required for inclusion.");
				}
				return new Pair<>(inst, inst2.get());
			}
			throw new RuntimeException("Inclusion not of form include var var, as required.");
			
		}

		public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			inst.map(f);
			if (inst2.isPresent()) {
				inst2.get().map(f);
			}
		}

	}

	///////////////////////////////////////////////////////////////////////////////////////

	public static final class TransExpVar
			extends TransExp<Object, Object, Object, Object, Object, Object, Object, Object> {
		public final String var;

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.TRANSFORM));
		}

		public TransExpVar(String var) {
			this.var = var;
		}

		@Override
		public Transform<Ty, En, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> eval0(
				AqlEnv env, boolean isC) {
			return env.defs.trans.get(var);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

		@Override
		public int hashCode() {
			return var.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TransExpVar other = (TransExpVar) obj;
			if (var == null) {
				if (other.var != null)
					return false;
			} else if (!var.equals(other.var))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return var;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Pair<InstExp<Object, Object, Object, Object>, InstExp<Object, Object, Object, Object>> type(
				AqlTyping G) {
			if (!G.defs.trans.containsKey(var)) {
				throw new RuntimeException("Not a transform: " + var);
			}
			return (Pair<InstExp<Object, Object, Object, Object>, InstExp<Object, Object, Object, Object>>) ((Object) G.defs.trans
					.get(var));
		}

		public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class TransExpLit<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
			extends TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

		public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public final Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> trans;

		public TransExpLit(Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> trans) {
			this.trans = trans;
		}

		@Override
		public Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> eval0(AqlEnv env, boolean isC) {
			return trans;
		}

		@Override
		public int hashCode() {
			return trans.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TransExpLit<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpLit<?, ?, ?, ?, ?, ?, ?, ?>) obj;
			return trans.equals(other.trans);
		}

		@Override
		public String toString() {
			return trans.toString();
		}

		@Override
		public Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> type(AqlTyping G) {
			return new Pair<>(new InstExpLit<>(trans.src()), new InstExpLit<>(trans.dst()));
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	///////////////////////////////////////////////////////////////////////////

}