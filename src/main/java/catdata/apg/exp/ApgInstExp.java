package catdata.apg.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.apg.ApgInstance;
import catdata.apg.ApgOps;
import catdata.apg.ApgPreTerm;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTerm;
import catdata.apg.ApgTy;
import catdata.apg.ApgTypeside;
import catdata.apg.exp.ApgSchExp.ApgSchExpPlus;
import catdata.apg.exp.ApgSchExp.ApgSchExpTerminal;
import catdata.apg.exp.ApgSchExp.ApgSchExpTimes;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class ApgInstExp extends Exp<ApgInstance<Object, Object>> {

	public abstract <R, P, E extends Exception> R accept(P param, ApgInstExpVisitor<R, P, E> v) throws E;

	public static interface ApgInstExpCoVisitor<R, P, E extends Exception> {

		public abstract ApgInstExpInitial visitApgInstExpInitial(P params, R r) throws E;

		public abstract ApgInstExpTerminal visitApgInstExpTerminal(P params, R r) throws E;

		public abstract ApgInstExpTimes visitApgInstExpTimes(P params, R r) throws E;

		public abstract ApgInstExpPlus visitApgInstExpPlus(P params, R r) throws E;

		public abstract ApgInstExpVar visitApgInstExpVar(P param, R exp) throws E;

		public abstract ApgInstExpRaw visitApgInstExpRaw(P param, R exp) throws E;
		
		public abstract ApgInstExpDelta visitApgInstExpDelta(P param, R exp) throws E;

		public abstract ApgInstExpEqualize visitApgInstExpEqualize(P params, R r) throws E;

		public abstract ApgInstExpCoEqualize visitApgInstExpCoEqualize(P params, R r) throws E;
	}

	public abstract <R, P, E extends Exception> ApgInstExp coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
			throws E;

	public static interface ApgInstExpVisitor<R, P, E extends Exception> {
		public abstract R visit(P param, ApgInstExpVar exp) throws E;

		public abstract R visit(P params, ApgInstExpInitial exp) throws E;

		public abstract R visit(P params, ApgInstExpTerminal exp) throws E;

		public abstract R visit(P params, ApgInstExpTimes exp) throws E;

		public abstract R visit(P params, ApgInstExpPlus exp) throws E;

		public abstract R visit(P param, ApgInstExpRaw exp) throws E;

		public abstract R visit(P params, ApgInstExpEqualize exp) throws E;

		public abstract R visit(P params, ApgInstExpCoEqualize exp) throws E;
		
		public abstract R visit(P params, ApgInstExpDelta exp) throws E;

	}

	@Override
	public abstract ApgSchExp type(AqlTyping G);

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Kind kind() {
		return Kind.APG_instance;
	}

	@Override
	public Exp<ApgInstance<Object, Object>> Var(String v) {
		return new ApgInstExpVar(v);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	public static final class ApgInstExpVar extends ApgInstExp {
		public final String var;

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpVar coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpVar(params, r);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singletonList(new Pair<>(var, Kind.APG_instance));
		}

		public ApgInstExpVar(String var) {
			this.var = var;
		}

		@Override
		public synchronized ApgInstance<Object, Object> eval0(AqlEnv env, boolean isC) {
			return env.defs.apgis.get(var);
		}

		public ApgSchExp type(AqlTyping G) {
			if (!G.defs.apgis.containsKey(var)) {
				throw new RuntimeException("Undefined APG instance variable: " + var);
			}
			return G.defs.apgis.get(var);
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
			ApgInstExpVar other = (ApgInstExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

		@Override
		public boolean isVar() {
			return true;
		}

	}

	public static final class ApgInstExpRaw extends ApgInstExp implements Raw {

		@Override
		public String toString() {
			String s = "literal : " + typeside + " {\n" + (imports.isEmpty() ? "" : ("\n" + Util.sep(imports, " ")))
					+ "\nelements\n\t";
			String x = "";
			for (Entry<String, Pair<String, ApgPreTerm>> e : Es.entrySet()) {
				x += "\t" + e.getKey() + ":" + e.getValue().first + " -> " + e.getValue().second + "\n";
			}

			return s + x + "\n}";

		}

		public final ApgSchExp typeside;
		public final Set<ApgInstExp> imports;
		public final Map<String, Pair<String, ApgPreTerm>> Es;

		public ApgInstExpRaw(ApgSchExp typeside0, List<ApgInstExp> imports0, 
				List<Pair<LocStr, Pair<String, ApgPreTerm>>> Es0) {
			Util.assertNotNull(typeside0, imports0, Es0);
			this.typeside = typeside0;
			this.imports = Util.toSetSafely(imports0);
			this.Es = Util.toMapSafely(LocStr.list2(Es0));
	
			doGuiIndex(imports0, Es0);
		}

		public void doGuiIndex(List<ApgInstExp> imports0, List<Pair<LocStr, Pair<String, ApgPreTerm>>> es0) {

			List<InteriorLabel<Object>> f = new LinkedList<>();
			for (Pair<LocStr, Pair<String, ApgPreTerm>> p : es0) {
				f.add(new InteriorLabel<>("elements", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " : " + x.second.first + " -> " + x.second.second).conv());
			}

			raw.put("elements", f);
		}

		private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

		@Override
		public Map<String, List<InteriorLabel<Object>>> raw() {
			return raw;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			Set<Pair<String, Kind>> ret = new THashSet<>();
			for (ApgInstExp x : imports) {
				ret.addAll(x.deps());
			}
			ret.addAll(typeside.deps());
			return ret;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpRaw coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpRaw(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

		private void wf(ApgTy<String> ty, ApgTypeside typeside, Map<String, ApgTy<String>> Ls0) {
			if (ty.b != null && !typeside.Bs.containsKey(ty.b)) {
				throw new RuntimeException("Type not in typeside: " + ty.b);
			}
			if (ty.l != null && !Ls0.containsKey(ty.l)) {
				throw new RuntimeException("Label not in instance: " + ty.l);
			}
			if (ty.m != null) {
				ty.m.forEach((k, v) -> wf(v, typeside, Ls0));
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected synchronized ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			ApgSchema<Object> ts = typeside.eval(env, isCompileTime);
			Map<Object, Pair<Object, ApgTerm<Object,Object>>> Es0 = new THashMap<>();
		
			for (ApgInstExp w : imports) {
				ApgInstance x = w.eval(env, isCompileTime);
				Util.putAllSafely(Es0, x.Es);
			}
			for (Entry<String, Pair<String, ApgPreTerm>> eld : Es.entrySet()) {
				Pair<Object, ApgTerm<Object,Object>> p = new Pair<>(eld.getValue().first,eval0(eld.getValue().second, ts, ts.schema.get(eld.getValue().first), Es0));
				Es0.put(eld.getKey(), p);
			}

			return new ApgInstance<>(ts, Es0);
		}

		private ApgTerm<Object,Object> eval0(ApgPreTerm term, ApgSchema<Object> ts, ApgTy<Object> ty,
				Map<Object, Pair<Object, ApgTerm<Object,Object>>> Es0) {
			if (ty.b != null) {
				if (term.str == null) {
					throw new RuntimeException("Expecting data at type " + ty.b + ", but received " + term);
				}
				Pair<Class<?>, Function<String, Object>> x = ts.typeside.Bs.get(ty.b);
				if (x == null) {
					return Util.anomaly(); // should already be checked
				}
				Object o = x.second.apply(term.str);
				if (!x.first.isInstance(o)) {
					Util.anomaly(); // should already be checked
				}
				return ApgTerm.ApgTermV(o, ty.b);
			} else if (ty.l != null) {
				if (term.str == null) {
					throw new RuntimeException("Expecting element at label " + ty.l + ", but received " + term);
				}
				if (!(Es0.containsKey(term.str) || Es.containsKey(term.str))) {
					throw new RuntimeException("Not an element: " + term.str);
				}
				Pair<Object, ApgTerm<Object, Object>> l2x = Es0.get(term.str);
				Object l2;
				if (l2x == null) {
					l2 = Es.get(term.str).first;
				} else {
					l2 = l2x.first;
				}
				if (!ty.l.equals(l2)) {
					throw new RuntimeException("Expecting element at label " + ty.l + ", but received element " + term
							+ " at label " + l2);
				}
				return ApgTerm.ApgTermE(term.str);
			} else if (ty.m != null && ty.all) {
				if (term.fields == null) {
					throw new RuntimeException("Expecting tuple at type " + ty + ", but received " + term);
				}
				Map<String, ApgTerm<Object,Object>> map = new THashMap<>();

				for (Pair<String, ApgPreTerm> x : term.fields) {
					ApgTy<Object> w = ty.m.get(x.first);
					if (w == null) {
						throw new RuntimeException("In " + term + ", " + x.first + ", is not a field in " + ty);
					}
					ApgTerm<Object,Object> o = eval0(x.second, ts, w, Es0);
					map.put(x.first, o);
				}
				for (String w : ty.m.keySet()) {
					if (!map.containsKey(w)) {
						throw new RuntimeException("In " + term + ", no field for " + w);
					}
				}
				return ApgTerm.ApgTermTuple(map);
			} else if (ty.m != null && !ty.all) {
				if (term.inj == null) {
					throw new RuntimeException("Expecting injection at type " + ty + ", but received " + term);
				}
				Map<String, ApgTy<Object>> d = ty.m;
				ApgTy<Object> w = d.get(term.inj);
				if (w == null) {
					throw new RuntimeException("In " + term + ", " + term.inj + ", is not a field in " + ty);
				}
				ApgTerm<Object,Object> o2 = eval0(term.arg, ts, w, Es0);
				ApgTerm<Object,Object> o = o2.convert();

				ApgTerm<Object,Object> z = ApgTerm.ApgTermInj(term.inj, o, d.get(term.inj));
				
				return z.convert();
			}

			return Util.anomaly();

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((Es == null) ? 0 : Es.hashCode());
			//result = prime * result + ((Ls == null) ? 0 : Ls.hashCode());
			result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
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
			ApgInstExpRaw other = (ApgInstExpRaw) obj;
			if (Es == null) {
				if (other.Es != null)
					return false;
			} else if (!Es.equals(other.Es))
				return false;
			if (typeside == null) {
				if (other.typeside != null)
					return false;
			} else if (!typeside.equals(other.typeside))
				return false;
			return true;
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			ApgTyExp w = typeside.type(G);
			for (Exp<?> z : imports()) {
				if (z.kind() != Kind.APG_instance) {
					throw new RuntimeException("Import of wrong kind: " + z);
				}
				ApgTyExp u = ((ApgInstExp) z).type(G).type(G);
				if (!w.equals(u)) {
					throw new RuntimeException("Import instance typeside mismatch on " + z + ", is " + u + " and not "
							+ u + " as expected.");
				}
			}
			return typeside;
		}

	}

	public static final class ApgInstExpInitial extends ApgInstExp {
		public final ApgSchExp typeside;

		public ApgInstExpInitial(ApgSchExp typeside) {
			this.typeside = typeside;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
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
			ApgInstExpInitial other = (ApgInstExpInitial) obj;
			if (typeside == null) {
				if (other.typeside != null)
					return false;
			} else if (!typeside.equals(other.typeside))
				return false;
			return true;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpInitial coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpInitial(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			typeside.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return typeside.deps();
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			typeside.type(G);
			return typeside;
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.initial(typeside.eval(env, isCompileTime));
		}

		@Override
		public String toString() {
			return "empty " + typeside;
		}

	}

	public static final class ApgInstExpTerminal extends ApgInstExp {

		public final ApgTyExp typeside;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
			return typeside.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ApgInstExpTerminal other = (ApgInstExpTerminal) obj;
			if (typeside == null) {
				if (other.typeside != null)
					return false;
			} else if (!typeside.equals(other.typeside))
				return false;
			return true;
		}

		public ApgInstExpTerminal(ApgTyExp typeside) {
			this.typeside = typeside;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpTerminal coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpTerminal(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			typeside.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return typeside.deps();
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			typeside.type(G);
			return new ApgSchExpTerminal(typeside);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.terminal(typeside.eval(env, isCompileTime));
		}

		@Override
		public String toString() {
			return "unit " + typeside;
		}

	}

	public static final class ApgInstExpTimes extends ApgInstExp {

		public final ApgInstExp l, r;

		public ApgInstExpTimes(ApgInstExp l, ApgInstExp r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			ApgInstExpTimes other = (ApgInstExpTimes) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpTimes coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpTimes(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			l.mapSubExps(f);
			r.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(l.deps(), r.deps());
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			ApgSchExp a = l.type(G);
			ApgSchExp b = r.type(G);
			ApgTyExp a0 = a.type(G);
			ApgTyExp b0 = b.type(G);
			if (!a0.equals(b0)) {
				throw new RuntimeException("Different typesides: " + a0 + " and " + b0);
			}
			return new ApgSchExpTimes(a,b);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.product(l.eval(env, isCompileTime), r.eval(env, isCompileTime));
		}

		@Override
		public String toString() {
			return "(" + l + " * " + r + ")";
		}
	}

	public static final class ApgInstExpPlus extends ApgInstExp {

		public final ApgInstExp l, r;

		public ApgInstExpPlus(ApgInstExp l, ApgInstExp r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			ApgInstExpPlus other = (ApgInstExpPlus) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpPlus coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpPlus(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			l.mapSubExps(f);
			r.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(l.deps(), r.deps());
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.coproduct(l.eval(env, isCompileTime), r.eval(env, isCompileTime));
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			ApgSchExp a = l.type(G);
			ApgSchExp b = r.type(G);
			ApgTyExp a0 = a.type(G);
			ApgTyExp b0 = b.type(G);
			if (!a0.equals(b0)) {
				throw new RuntimeException("Different typesides: " + a0 + " and " + b0);
			}
			return new ApgSchExpPlus(a,b);
		}

		@Override
		public String toString() {
			return "<" + l + " + " + r + ">";
		}

	}

	public static final class ApgInstExpEqualize extends ApgInstExp {

		public final ApgTransExp l, r;

		@Override
		public String toString() {
			return "equalize " + l + " " + r;
		}

		public ApgInstExpEqualize(ApgTransExp l, ApgTransExp r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			ApgInstExpEqualize other = (ApgInstExpEqualize) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpEqualize coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpEqualize(params, r);
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = l.type(G);
			Pair<ApgInstExp, ApgInstExp> b = r.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domain mismatch: " + a.first + " is not equal to " + b.first);
			} else if (!a.second.equals(b.second)) {
				throw new RuntimeException("CoDomain mismatch: " + a.second + " is not equal to " + b.second);
			}
			return a.first.type(G);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			l.mapSubExps(f);
			r.mapSubExps(f);
		}

		@Override
		protected ApgInstance<Object, Object> eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.equalize(l.eval(env, isCompileTime), r.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(l.deps(), r.deps());
		}

	}

	public static final class ApgInstExpCoEqualize extends ApgInstExp {

		public final ApgTransExp l, r;

		@Override
		public String toString() {
			return "coequalize " + l + " " + r;
		}

		public ApgInstExpCoEqualize(ApgTransExp l, ApgTransExp r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			ApgInstExpCoEqualize other = (ApgInstExpCoEqualize) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public <R, P, E extends Exception> R accept(P params, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExpCoEqualize coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r)
				throws E {
			return v.visitApgInstExpCoEqualize(params, r);
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = l.type(G);
			Pair<ApgInstExp, ApgInstExp> b = r.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domain mismatch: " + a.first + " is not equal to " + b.first);
			} else if (!a.second.equals(b.second)) {
				throw new RuntimeException("CoDomain mismatch: " + a.second + " is not equal to " + b.second);
			}
			return a.first.type(G);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			l.mapSubExps(f);
			r.mapSubExps(f);
		}

		@Override
		protected ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.coequalize(l.eval(env, isCompileTime), r.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(l.deps(), r.deps());
		}

	}
	
	//////////////////////////////////////
	
	public static final class ApgInstExpDelta extends ApgInstExp {
	
		public ApgInstExp J;
		public ApgMapExp F;
		
		public ApgInstExpDelta(ApgMapExp f, ApgInstExp j) {
			J = j;
			F = f;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((J == null) ? 0 : J.hashCode());
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
			ApgInstExpDelta other = (ApgInstExpDelta) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (J == null) {
				if (other.J != null)
					return false;
			} else if (!J.equals(other.J))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "delta " + F + " " + J;
		}

		@Override
		public <R, P, E extends Exception> R accept(P param, ApgInstExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public <R, P, E extends Exception> ApgInstExp coaccept(P params, ApgInstExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitApgInstExpDelta(params, r);
		}

		@Override
		public ApgSchExp type(AqlTyping G) {
			ApgSchExp j = J.type(G);
			Pair<ApgSchExp, ApgSchExp> f = F.type(G);
			if (!f.first.type(G).equals(j.type(G))) {
				throw new RuntimeException("Typeside mismatch");
			}
			if (!f.second.equals(j)) {
				throw new RuntimeException("Instance is on schema " + j + " but mapping target is " + f.second);
			}
			return f.first;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			F.mapSubExps(f);
			J.mapSubExps(f);
		}

		@Override
		protected ApgInstance eval0(AqlEnv env, boolean isCompileTime) {
			return F.eval(env, isCompileTime).delta(J.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(J.deps(), F.deps());
		}
		
		
		
	}
	
	
}
