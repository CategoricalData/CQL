package catdata.apg.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.apg.ApgInstance;
import catdata.apg.ApgOps;
import catdata.apg.ApgTransform;
import catdata.apg.exp.ApgInstExp.ApgInstExpCoEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpDelta;
import catdata.apg.exp.ApgInstExp.ApgInstExpEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpInitial;
import catdata.apg.exp.ApgInstExp.ApgInstExpPlus;
import catdata.apg.exp.ApgInstExp.ApgInstExpTerminal;
import catdata.apg.exp.ApgInstExp.ApgInstExpTimes;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class ApgTransExp extends Exp<ApgTransform<Object, Object, Object, Object>> {

	public abstract <R, P> R accept(P params, ApgTransExpVisitor<R, P> v);

	public abstract <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r);

	public interface ApgTransExpVisitor<R, P> {

		public R visit(P params, ApgTransExpRaw exp);

		public R visit(P params, ApgTransExpVar exp);

		public R visit(P params, ApgTransExpTerminal exp);

		public R visit(P params, ApgTransExpInitial exp);

		public R visit(P params, ApgTransExpFst exp);

		public R visit(P params, ApgTransExpSnd exp);

		public R visit(P params, ApgTransExpPair exp);

		public R visit(P params, ApgTransExpInl exp);

		public R visit(P params, ApgTransExpInr exp);

		public R visit(P params, ApgTransExpCase exp);

		public R visit(P params, ApgTransExpId exp);

		public R visit(P params, ApgTransExpCompose exp);

		public R visit(P params, ApgTransExpEqualize exp);

		public R visit(P params, ApgTransExpEqualizeU exp);

		public R visit(P params, ApgTransExpCoEqualize exp);

		public R visit(P params, ApgTransExpCoEqualizeU exp);

		public R visit(P params, ApgTransExpDelta exp);

	}

	public interface ApgTransExpCoVisitor<R, P> {

		public ApgTransExpDelta visitApgTransExpDelta(P params, R exp);

		public ApgTransExpRaw visitApgTransExpRaw(P params, R exp);

		public ApgTransExpVar visitApgTransExpVar(P params, R exp);

		public ApgTransExp visitApgTransExpCase(P params, R r);

		public ApgTransExp visitApgTransExpInitial(P params, R r);

		public ApgTransExp visitApgTransExpFst(P params, R r);

		public ApgTransExp visitApgTransExpTerminal(P params, R r);

		public ApgTransExp visitApgTransExpSnd(P params, R r);

		public ApgTransExp visitApgTransExpPair(P params, R r);

		public ApgTransExp visitApgTransExpInl(P params, R r);

		public ApgTransExp visitApgTransExpInr(P params, R r);

		public ApgTransExp visitApgTransExpId(P params, R r);

		public ApgTransExp visitApgTransExpCompose(P params, R r);

		public ApgTransExp visitApgTransExpEqualize(P params, R r);

		public ApgTransExp visitApgTransExpEqualizeU(P params, R r);

		public ApgTransExp visitApgTransExpCoEqualize(P params, R r);

		public ApgTransExp visitApgTransExpCoEqualizeU(P params, R r);

	}

	@Override
	public abstract Pair<ApgInstExp, ApgInstExp> type(AqlTyping G);

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Kind kind() {
		return Kind.APG_morphism;
	}

	@Override
	public Exp<ApgTransform<Object, Object, Object, Object>> Var(String v) {
		return new ApgTransExpVar(v);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	public static final class ApgTransExpVar extends ApgTransExp {
		public final String var;

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExpVar coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpVar(params, r);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singletonList(new Pair<>(var, Kind.APG_morphism));
		}

		public ApgTransExpVar(String var) {
			this.var = var;
		}

		@Override
		public synchronized ApgTransform<Object, Object, Object, Object> eval0(AqlEnv env, boolean isC) {
			return env.defs.apgms.get(var);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			if (!G.defs.apgms.containsKey(var)) {
				throw new RuntimeException("Undefined APG morphism variable: " + var);
			}
			return G.defs.apgms.get(var);
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
			ApgTransExpVar other = (ApgTransExpVar) obj;
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

	public static final class ApgTransExpRaw extends ApgTransExp implements Raw {

		@Override
		public String toString() {
			String s = "literal : " + src + " -> " + dst + " {\n"
					+ (imports.isEmpty() ? "" : ("\n" + Util.sep(imports, " "))) + "\nlabels\n\t"
					+ Util.sep(Ls, " -> ", "\n\t") + "\nelements\n\t" + Util.sep(Es, " -> ", "\n\t");

			return s + "\n}";
		}

		public final ApgInstExp src, dst;
		public final Set<ApgTransExp> imports;
		public final Map<String, String> Es;
		public final Map<String, String> Ls;

		public ApgTransExpRaw(ApgInstExp src0, ApgInstExp dst0, List<ApgTransExp> imports0,
				List<Pair<LocStr, String>> Ls0, List<Pair<LocStr, String>> Es0) {
			this.src = src0;
			this.dst = dst0;
			this.imports = Util.toSetSafely(imports0);
			this.Es = Util.toMapSafely(LocStr.list2(Es0));
			this.Ls = Util.toMapSafely(LocStr.list2(Ls0));

			doGuiIndex(Es0, Ls0);
		}

		public void doGuiIndex(List<Pair<LocStr, String>> ls0,
				List<Pair<LocStr, String>> es0) {

			List<InteriorLabel<Object>> f = new LinkedList<>();
			for (Pair<LocStr, String> p : es0) {
				f.add(new InteriorLabel<>("values", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " -> " + x.second).conv());
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
			for (ApgTransExp x : imports) {
				ret.addAll(x.deps());
			}
			ret.addAll(src.deps());
			ret.addAll(dst.deps());
			return ret;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExpRaw coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpRaw(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			ApgInstance s = src.eval(env, isCompileTime);
			ApgInstance t = dst.eval(env, isCompileTime);
			if (imports.isEmpty()) {
				return new ApgTransform(s, t, Ls, Es);
			}
			Map<String, String> Es0 = new THashMap<>(Es);
			Map<String, String> Ls0 = new THashMap<>(Ls);
			for (ApgTransExp w : imports) {
				ApgTransform x = w.eval(env, isCompileTime);
				Util.putAllSafely(Es0, x.eMap);
				Util.putAllSafely(Ls0, x.lMap);
			}
			return new ApgTransform<>(s, t, Ls0, Es0);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			ApgSchExp a = src.type(G);
			ApgSchExp b = dst.type(G);
			ApgTyExp ao = a.type(G);
			ApgTyExp bo = b.type(G);
			if (!ao.equals(bo)) {
				throw new RuntimeException("Not the same typeside: " + ao + " and " + bo);
			}

			return new Pair<>(src, dst);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((Es == null) ? 0 : Es.hashCode());
			result = prime * result + ((Ls == null) ? 0 : Ls.hashCode());
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
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
			ApgTransExpRaw other = (ApgTransExpRaw) obj;
			if (Es == null) {
				if (other.Es != null)
					return false;
			} else if (!Es.equals(other.Es))
				return false;
			if (Ls == null) {
				if (other.Ls != null)
					return false;
			} else if (!Ls.equals(other.Ls))
				return false;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

	}

	public static final class ApgTransExpTerminal extends ApgTransExp {

		public final ApgInstExp G;

		public ApgTransExpTerminal(ApgInstExp g) {
			G = g;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G == null) ? 0 : G.hashCode());
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
			ApgTransExpTerminal other = (ApgTransExpTerminal) obj;
			if (G == null) {
				if (other.G != null)
					return false;
			} else if (!G.equals(other.G))
				return false;
			return true;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return G.deps();
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpTerminal(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			ApgSchExp s = this.G.type(G);
			return new Pair<>(this.G, new ApgInstExpTerminal(s.type(G)));
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.terminal(G.eval(env, isCompileTime));
		}

		@Override
		public String toString() {
			return "unit " + G;
		}

	}

	public static final class ApgTransExpInitial extends ApgTransExp {

		public final ApgInstExp G;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G == null) ? 0 : G.hashCode());
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
			ApgTransExpInitial other = (ApgTransExpInitial) obj;
			if (G == null) {
				if (other.G != null)
					return false;
			} else if (!G.equals(other.G))
				return false;
			return true;
		}

		public ApgTransExpInitial(ApgInstExp g) {
			G = g;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpInitial(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			return new Pair<>(new ApgInstExpInitial(this.G.type(G)), this.G);

		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.initial(G.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return G.deps();
		}

		@Override
		public String toString() {
			return "empty " + G;
		}

	}

	public static final class ApgTransExpFst extends ApgTransExp {
		public final ApgInstExp G1, G2;

		@Override
		public String toString() {
			return "fst " + G1 + " " + G2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G1 == null) ? 0 : G1.hashCode());
			result = prime * result + ((G2 == null) ? 0 : G2.hashCode());
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
			ApgTransExpFst other = (ApgTransExpFst) obj;
			if (G1 == null) {
				if (other.G1 != null)
					return false;
			} else if (!G1.equals(other.G1))
				return false;
			if (G2 == null) {
				if (other.G2 != null)
					return false;
			} else if (!G2.equals(other.G2))
				return false;
			return true;
		}

		public ApgTransExpFst(ApgInstExp g1, ApgInstExp g2) {
			G1 = g1;
			G2 = g2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpFst(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			ApgTyExp a = G1.type(G).type(G);
			ApgTyExp b = G2.type(G).type(G);
			if (!a.equals(b)) {
				throw new RuntimeException("Typesides " + a + " and " + b + " not equal.");
			}
			return new Pair<>(new ApgInstExpTimes(G1, G2), G1);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.fst(G1.eval(env, isCompileTime), G2.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G1.mapSubExps(f);
			G2.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(G1.deps(), G2.deps());
		}

	}

	public static final class ApgTransExpSnd extends ApgTransExp {
		public final ApgInstExp G1, G2;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G1 == null) ? 0 : G1.hashCode());
			result = prime * result + ((G2 == null) ? 0 : G2.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "snd " + G1 + " " + G2;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ApgTransExpSnd other = (ApgTransExpSnd) obj;
			if (G1 == null) {
				if (other.G1 != null)
					return false;
			} else if (!G1.equals(other.G1))
				return false;
			if (G2 == null) {
				if (other.G2 != null)
					return false;
			} else if (!G2.equals(other.G2))
				return false;
			return true;
		}

		public ApgTransExpSnd(ApgInstExp g1, ApgInstExp g2) {
			G1 = g1;
			G2 = g2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpSnd(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			ApgTyExp a = G1.type(G).type(G);
			ApgTyExp b = G2.type(G).type(G);
			if (!a.equals(b)) {
				throw new RuntimeException("Typesides " + a + " and " + b + " not equal.");
			}
			return new Pair<>(new ApgInstExpTimes(G1, G2), G2);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.snd(G1.eval(env, isCompileTime), G2.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G1.mapSubExps(f);
			G2.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(G1.deps(), G2.deps());
		}
	}

	public static final class ApgTransExpPair extends ApgTransExp {
		public final ApgTransExp h1, h2;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpPair other = (ApgTransExpPair) obj;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		public ApgTransExpPair(ApgTransExp h1, ApgTransExp h2) {
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpPair(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domains do not match: " + a.first + " and " + b.first);
			}
			return new Pair<>(a.first, new ApgInstExpTimes(a.second, b.second));
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.pair(h1.eval(env, isCompileTime), h2.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h1.deps(), h2.deps());
		}

		@Override
		public String toString() {
			return "( " + h1 + " , " + h2 + ")";
		}
	}

	public static final class ApgTransExpInl extends ApgTransExp {
		public final ApgInstExp G1, G2;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G1 == null) ? 0 : G1.hashCode());
			result = prime * result + ((G2 == null) ? 0 : G2.hashCode());
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
			ApgTransExpInl other = (ApgTransExpInl) obj;
			if (G1 == null) {
				if (other.G1 != null)
					return false;
			} else if (!G1.equals(other.G1))
				return false;
			if (G2 == null) {
				if (other.G2 != null)
					return false;
			} else if (!G2.equals(other.G2))
				return false;
			return true;
		}

		public ApgTransExpInl(ApgInstExp g1, ApgInstExp g2) {
			G1 = g1;
			G2 = g2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpInl(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			ApgTyExp a = G1.type(G).type(G);
			ApgTyExp b = G2.type(G).type(G);
			if (!a.equals(b)) {
				throw new RuntimeException("Typesides " + a + " and " + b + " not equal.");
			}
			return new Pair<>(G1, new ApgInstExpPlus(G1, G2));
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.inl(G1.eval(env, isCompileTime), G2.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G1.mapSubExps(f);
			G2.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(G1.deps(), G2.deps());
		}

		@Override
		public String toString() {
			return "inl " + G1 + " " + G2;
		}
	}

	public static final class ApgTransExpInr extends ApgTransExp {
		public final ApgInstExp G1, G2;

		public ApgTransExpInr(ApgInstExp g1, ApgInstExp g2) {
			G1 = g1;
			G2 = g2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G1 == null) ? 0 : G1.hashCode());
			result = prime * result + ((G2 == null) ? 0 : G2.hashCode());
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
			ApgTransExpInr other = (ApgTransExpInr) obj;
			if (G1 == null) {
				if (other.G1 != null)
					return false;
			} else if (!G1.equals(other.G1))
				return false;
			if (G2 == null) {
				if (other.G2 != null)
					return false;
			} else if (!G2.equals(other.G2))
				return false;
			return true;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpInr(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			ApgTyExp a = G1.type(G).type(G);
			ApgTyExp b = G2.type(G).type(G);
			if (!a.equals(b)) {
				throw new RuntimeException("Typesides " + a + " and " + b + " not equal.");
			}
			return new Pair<>(G2, new ApgInstExpPlus(G1, G2));
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.inr(G1.eval(env, isCompileTime), G2.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G1.mapSubExps(f);
			G2.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(G1.deps(), G2.deps());
		}

		@Override
		public String toString() {
			return "inr " + G1 + " " + G2;
		}
	}

	public static final class ApgTransExpCase extends ApgTransExp {
		public final ApgTransExp h1, h2;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpCase other = (ApgTransExpCase) obj;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		public ApgTransExpCase(ApgTransExp h1, ApgTransExp h2) {
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpCase(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			if (!a.second.equals(b.second)) {
				throw new RuntimeException("Co-domains do not match: " + a.second + " and " + b.second);
			}
			return new Pair<>(new ApgInstExpPlus(a.first, b.first), b.second);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.Case(h1.eval(env, isCompileTime), h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h1.deps(), h2.deps());
		}

		@Override
		public String toString() {
			return "<" + h1 + " | " + h2 + ">";
		}
	}

	public static final class ApgTransExpId extends ApgTransExp {
		public final ApgInstExp G;

		@Override
		public String toString() {
			return "identity " + G;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((G == null) ? 0 : G.hashCode());
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
			ApgTransExpId other = (ApgTransExpId) obj;
			if (G == null) {
				if (other.G != null)
					return false;
			} else if (!G.equals(other.G))
				return false;

			return true;
		}

		public ApgTransExpId(ApgInstExp g) {
			G = g;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpId(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			this.G.type(G);
			return new Pair<>(this.G, this.G);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.id(G.eval(env, isCompileTime));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			G.mapSubExps(f);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return G.deps();
		}
	}

	public static final class ApgTransExpCompose extends ApgTransExp {
		public final ApgTransExp h1, h2;

		@Override
		public String toString() {
			return "[" + h1 + " ; " + h2 + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpCompose other = (ApgTransExpCompose) obj;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		public ApgTransExpCompose(ApgTransExp h1, ApgTransExp h2) {
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpCompose(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			if (!a.second.equals(b.first)) {
				throw new RuntimeException("Intermediate APGs do not match: " + a.second + " and " + b.first);
			}
			return new Pair<>(a.first, b.second);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
		}

		@Override
		protected ApgTransform<Object, Object, Object, Object> eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.compose(h1.eval(env, isCompileTime), h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h1.deps(), h2.deps());
		}

	}

	public static final class ApgTransExpEqualize extends ApgTransExp {
		public final ApgTransExp h1, h2;

		public ApgTransExpEqualize(ApgTransExp h1, ApgTransExp h2) {
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpEqualize other = (ApgTransExpEqualize) obj;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "equalize " + h1 + " " + h2;
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domain mismatch: " + a.first + " is not equal to " + b.first);
			} else if (!a.second.equals(b.second)) {
				throw new RuntimeException("CoDomain mismatch: " + a.second + " is not equal to " + b.second);
			}
			return new Pair<>(new ApgInstExpEqualize(h1, h2), a.first);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.equalizeT(h1.eval(env, isCompileTime), h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h1.deps(), h2.deps());
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpEqualize(params, r);
		}

	}

	public static final class ApgTransExpEqualizeU extends ApgTransExp {
		public final ApgTransExp h, h1, h2;

		public ApgTransExpEqualizeU(ApgTransExp h1, ApgTransExp h2, ApgTransExp h) {
			this.h = h;
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpEqualizeU(params, r);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpEqualizeU other = (ApgTransExpEqualizeU) obj;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
			h.mapSubExps(f);
		}

		@Override
		protected ApgTransform<Object, Object, Object, Object> eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.equalizeU(h.eval(env, isCompileTime), h1.eval(env, isCompileTime),
					h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h.deps(), Util.union(h1.deps(), h2.deps()));
		}

		@Override
		public String toString() {
			return "equalize_u " + h1 + " " + h2 + " " + h;
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			Pair<ApgInstExp, ApgInstExp> c = h.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domain mismatch: " + a.first + " is not equal to " + b.first);
			} else if (!a.second.equals(b.second)) {
				throw new RuntimeException("CoDomain mismatch: " + a.second + " is not equal to " + b.second);
			} else if (!c.second.equals(a.first)) {
				throw new RuntimeException("Domain and CoDomain mismatch: " + a.first + " is not equal to " + c.second);
			}

			return new Pair<>(c.first, new ApgInstExpEqualize(h1, h2));
		}

	}

	public static final class ApgTransExpCoEqualize extends ApgTransExp {
		public final ApgTransExp h1, h2;

		public ApgTransExpCoEqualize(ApgTransExp h1, ApgTransExp h2) {
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpCoEqualize other = (ApgTransExpCoEqualize) obj;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "coequalize " + h1 + " " + h2;
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domain mismatch: " + a.first + " is not equal to " + b.first);
			} else if (!a.second.equals(b.second)) {
				throw new RuntimeException("Codomain mismatch: " + a.second + " is not equal to " + b.second);
			}
			return new Pair<>(a.second, new ApgInstExpCoEqualize(h1, h2));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.coequalizeT(h1.eval(env, isCompileTime), h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h1.deps(), h2.deps());
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpCoEqualize(params, r);
		}

	}

	public static final class ApgTransExpCoEqualizeU extends ApgTransExp {
		public final ApgTransExp h, h1, h2;

		public ApgTransExpCoEqualizeU(ApgTransExp h1, ApgTransExp h2, ApgTransExp h) {
			this.h = h;
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpCoEqualizeU(params, r);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
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
			ApgTransExpCoEqualizeU other = (ApgTransExpCoEqualizeU) obj;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (h1 == null) {
				if (other.h1 != null)
					return false;
			} else if (!h1.equals(other.h1))
				return false;
			if (h2 == null) {
				if (other.h2 != null)
					return false;
			} else if (!h2.equals(other.h2))
				return false;
			return true;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
			h.mapSubExps(f);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.coequalizeU(h.eval(env, isCompileTime), h1.eval(env, isCompileTime),
					h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h.deps(), Util.union(h1.deps(), h2.deps()));
		}

		@Override
		public String toString() {
			return "coequalize_u " + h1 + " " + h2 + " " + h;
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h1.type(G);
			Pair<ApgInstExp, ApgInstExp> b = h2.type(G);
			Pair<ApgInstExp, ApgInstExp> c = h.type(G);
			if (!a.first.equals(b.first)) {
				throw new RuntimeException("Domain mismatch: " + a.first + " is not equal to " + b.first);
			} else if (!a.second.equals(b.second)) {
				throw new RuntimeException("CoDomain mismatch: " + a.second + " is not equal to " + b.second);
			} else if (!c.first.equals(a.second)) {
				throw new RuntimeException("Domain and CoDomain mismatch: " + a.second + " is not equal to " + c.first);
			}

			return new Pair<>(new ApgInstExpCoEqualize(h1, h2), c.second);
		}

	}

	public static class ApgTransExpDelta extends ApgTransExp {

		public final ApgMapExp F;

		public final ApgTransExp h;

		public ApgTransExpDelta(ApgMapExp f, ApgTransExp j) {
			F = f;
			h = j;
		}

		@Override
		public String toString() {
			return "delta " + F + " " + h;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((h == null) ? 0 : h.hashCode());
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
			ApgTransExpDelta other = (ApgTransExpDelta) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			return true;
		}

		@Override
		public <R, P> R accept(P params, ApgTransExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgTransExp coaccept(P params, ApgTransExpCoVisitor<R, P> v, R r) {
			return v.visitApgTransExpDelta(params, r);
		}

		@Override
		public Pair<ApgInstExp, ApgInstExp> type(AqlTyping G) {
			Pair<ApgInstExp, ApgInstExp> a = h.type(G);
			ApgSchExp z = a.first.type(G);

			Pair<ApgSchExp, ApgSchExp> b = F.type(G);
			ApgTyExp y = b.first.type(G);

			if (!z.type(G).equals(y)) {
				throw new RuntimeException("Typeside mismatch: " + z.type(G) + " and " + y);
			}

			if (!z.equals(b.second)) {
				throw new RuntimeException("Target of mapping is " + b.second + " but transform is on " + z);
			}

			return new Pair<>(new ApgInstExpDelta(F, a.first), new ApgInstExpDelta(F, a.second));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			F.mapSubExps(f);
			h.mapSubExps(f);
		}

		@Override
		protected ApgTransform eval0(AqlEnv env, boolean isCompileTime) {
			return F.eval(env, isCompileTime).deltaT(h.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(F.deps(), h.deps());
		}

	}

}
