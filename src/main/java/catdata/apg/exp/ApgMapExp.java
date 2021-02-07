package catdata.apg.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import catdata.Triple;
import catdata.Util;
import catdata.apg.ApgMapping;
import catdata.apg.ApgOps;
import catdata.apg.ApgPreTerm;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTerm;
import catdata.apg.ApgTy;
import catdata.apg.ApgTypeside;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Var;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class ApgMapExp extends Exp<ApgMapping<Object, Object>> {

	public abstract <R, P> R accept(P param, ApgMapExpVisitor<R, P> v);

	public static interface ApgMapExpCoVisitor<R, P> {

		// public abstract ApgMapExpInitial visitApgMapExpInitial(P params, R r) ;

		// public abstract ApgMapExpTerminal visitApgMapExpTerminal(P params, R r) ;

		// public abstract ApgMapExpTimes visitApgMapExpTimes(P params, R r) ;

		// public abstract ApgMapExpPlus visitApgMapExpPlus(P params, R r) ;

		public abstract ApgMapExpVar visitApgMapExpVar(P param, R exp);

		public abstract ApgMapExpRaw visitApgMapExpRaw(P param, R exp);

		// public abstract ApgMapExpCompose visitApgMapExpCompose(P param, R exp);

		// public abstract ApgMapExpEqualize visitApgMapExpEqualize(P params, R r) ;

		// public abstract ApgMapExpCoEqualize visitApgMapExpCoEqualize(P params, R r) ;
	}

	public abstract <R, P> ApgMapExp coaccept(P params, ApgMapExpCoVisitor<R, P> v, R r);

	public static interface ApgMapExpVisitor<R, P> {
		public abstract R visit(P param, ApgMapExpVar exp);

		// public abstract R visit(P params, ApgMapExpInitial exp) ;

		// public abstract R visit(P params, ApgMapExpTerminal exp) ;

		// public abstract R visit(P params, ApgMapExpTimes exp) ;

		// public abstract R visit(P params, ApgMapExpPlus exp) ;

		public abstract R visit(P param, ApgMapExpRaw exp);

		// public abstract R visit(P param, ApgMapExpCompose exp);

		// public abstract R visit(P params, ApgMapExpEqualize exp) ;

		// public abstract R visit(P params, ApgMapExpCoEqualize exp) ;

	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	public abstract Pair<ApgSchExp, ApgSchExp> type(AqlTyping G);

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Kind kind() {
		return Kind.APG_mapping;
	}

	@Override
	public Exp<ApgMapping<Object, Object>> Var(String v) {
		return new ApgMapExpVar(v);
	}

////////////////////////////////////////////////////////////////////////

	public static final class ApgMapExpVar extends ApgMapExp {
		public final String var;

		@Override
		public <R, P> R accept(P params, ApgMapExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgMapExpVar coaccept(P params, ApgMapExpCoVisitor<R, P> v, R r) {
			return v.visitApgMapExpVar(params, r);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singletonList(new Pair<>(var, Kind.APG_instance));
		}

		public ApgMapExpVar(String var) {
			this.var = var;
		}

		@Override
		public synchronized ApgMapping<Object, Object> eval0(AqlEnv env, boolean isC) {
			return env.defs.apgmappings.get(var);
		}

		public Pair<ApgSchExp, ApgSchExp> type(AqlTyping G) {
			if (!G.defs.apgmappings.containsKey(var)) {
				throw new RuntimeException("Undefined APG schema mapping variable: " + var);
			}
			return G.defs.apgmappings.get(var);
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
			ApgMapExpVar other = (ApgMapExpVar) obj;
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

	public static final class ApgMapExpRaw extends ApgMapExp implements Raw {

		@Override
		public String toString() {
			String s = "literal : " + src + " -> " + dst + " {\n"
					+ (imports.isEmpty() ? "" : ("\n" + Util.sep(imports, " "))) + "\nlabels\n\t"
					+ Util.sep(Ls, " -> ", "\n\t", x->" lambda " + x.first + " : " +x.second + " . " + x.third);
			String x = "";

			return s + x + "\n}";

		}

		public final ApgSchExp src;
		public final ApgSchExp dst;

		public final Set<ApgMapExp> imports;
		public final Map<String, Triple<String, ApgTy, ApgPreTerm>> Ls;

		public ApgMapExpRaw(ApgSchExp src, ApgSchExp dst, List<ApgMapExp> imports0,
				List<Pair<LocStr, Triple<String, ApgTy, ApgPreTerm>>> b) {
			Util.assertNotNull(src, dst, imports0, b);
			this.src = src;
			this.dst = dst;
			this.imports = Util.toSetSafely(imports0);
			this.Ls = Util.toMapSafely(LocStr.list2(b));

			doGuiIndex(b);
		}

		public void doGuiIndex(List<Pair<LocStr, Triple<String, ApgTy, ApgPreTerm>>> ls0) {

			List<InteriorLabel<Object>> f = new LinkedList<>();
			for (Pair<LocStr, Triple<String, ApgTy, ApgPreTerm>> p : ls0) {
				f.add(new InteriorLabel<>("labels", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " -> \\" + x.second.first + ":" + x.second.second + "." + x.second.third).conv());
			}
			raw.put("labels", f);

		}

		private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

		@Override
		public Map<String, List<InteriorLabel<Object>>> raw() {
			return raw;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			Set<Pair<String, Kind>> ret = new THashSet<>();
			for (ApgMapExp x : imports) {
				ret.addAll(x.deps());
			}
			ret.addAll(src.deps());
			ret.addAll(dst.deps());
			return ret;
		}

		@Override
		public <R, P> R accept(P params, ApgMapExpVisitor<R, P> v) {
			return v.visit(params, this);
		}

		@Override
		public <R, P> ApgMapExpRaw coaccept(P params, ApgMapExpCoVisitor<R, P> v, R r) {
			return v.visitApgMapExpRaw(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected synchronized ApgMapping eval0(AqlEnv env, boolean isCompileTime) {
			ApgSchema<Object> s = src.eval(env, isCompileTime);
			ApgSchema<Object> t = dst.eval(env, isCompileTime);

			Map<Object, Triple<Var, ApgTy<Object>, ApgTerm<Object, Void>>> Ls0 = new THashMap<>();
			for (ApgMapExp w : imports) {
				ApgMapping x = w.eval(env, isCompileTime);
				Util.putAllSafely(Ls0, x.mapping);
			}
			for (Entry<String, Triple<String, ApgTy, ApgPreTerm>> z : Ls.entrySet()) {
				Var var = Var.Var(z.getValue().first);
				ApgTy ty = z.getValue().second;
				ApgPreTerm term = z.getValue().third;
				Map<String, ApgTy<Object>> map = new THashMap<>();
				map.put(var.var, ty);
				Pair<ApgTy<Object>, ApgTerm<Object, Void>> u = infer(s.typeside, map, term, t.schema);
				Ls0.put(z.getKey(), new Triple<>(Var.Var(z.getValue().first), z.getValue().second, u.second));
			}
			return new ApgMapping<>(s, t, Ls0);
		}

		private Pair<ApgTy<Object>, ApgTerm<Object, Void>> infer(ApgTypeside ts, Map<String, ApgTy<Object>> ctx,
				ApgPreTerm term, Map<Object, ApgTy<Object>> atts) {
			if (term.fields != null) {
				Set<String> seen = new THashSet<>();
				Map<String, ApgTy<Object>> m = new THashMap<>();
				Map<String, ApgTerm<Object, Void>> n = new THashMap<>();
				for (Pair<String, ApgPreTerm> x : term.fields) {
					if (seen.contains(x.first)) {
						throw new RuntimeException("Duplicate field: " + x.first);
					}
					Pair<ApgTy<Object>, ApgTerm<Object, Void>> z = infer(ts, ctx, x.second, atts);
					m.put(x.first, z.first);
					n.put(x.first, z.second);
					seen.add(x.first);
				}
				return new Pair<>(ApgTy.ApgTyP(true, m), ApgTerm.ApgTermTuple(n));
			}
			if (term.str != null && term.ty == null) {
				if (!ctx.containsKey(term.str)) {
					throw new RuntimeException("Unbound variable or un-annotated value: " + term.str);
				}
				ApgTy<Object> z = ctx.get(term.str);
				return new Pair<>(z, ApgTerm.ApgTermVar(Var.Var(term.str)));
			}
			if (term.str != null && term.ty != null) {
				if (term.ty.b == null || !ts.Bs.containsKey(term.ty.b)) {
					throw new RuntimeException("Not a base type: " + term.ty);
				}
				Pair<Class<?>, Function<String, Object>> m = ts.Bs.get(term.ty.b);
				Object o = m.second.apply(term.str);
				if (!m.first.isInstance(o)) {
					throw new RuntimeException("In " + term.str + ", value is " + o + " of class "
							+ o.getClass().getSimpleName() + ", not " + m.first.getSimpleName() + " as expected.");
				}
				return new Pair<>(term.ty, ApgTerm.ApgTermV(o, term.ty.b));
			}
			if (term.inj != null) {
				if (term.ty == null || term.ty.m == null || term.ty.all || !term.ty.m.containsKey(term.inj)) {
					throw new RuntimeException("Not a variant type containing " + term.inj + ": " + term.ty);
				}
				Pair<ApgTy<Object>, ApgTerm<Object, Void>> x = infer(ts, ctx, term.arg, atts);
				if (!x.first.equals(term.ty.m.get(term.inj))) {
					throw new RuntimeException(
							x.second + "has type " + x.first + " and not " + term.ty.m.get(term.inj) + " as expected.");
				}
				Map<String, ApgTy<Object>> l = new THashMap<>(term.ty.m);
				return new Pair<>(term.ty, ApgTerm.ApgTermInj(term.inj, x.second, term.ty));
			}
			if (term.proj != null) {
				Pair<ApgTy<Object>, ApgTerm<Object, Void>> x = infer(ts, ctx, term.arg, atts);
				if (x.first.m == null || !x.first.all) {
					throw new RuntimeException(
							"Argument type " + x.first + " is not a product type containing " + term.proj);
				}
				return new Pair<>(x.first.m.get(term.proj), ApgTerm.ApgTermProj(term.proj, x.second));
			}
			if (term.deref != null) {
				Pair<ApgTy<Object>, ApgTerm<Object, Void>> x = infer(ts, ctx, term.arg, atts);
				if (x.first.l == null || !x.first.l.equals(term.deref)) {
					throw new RuntimeException("Argument type " + x.first + " is not label " + term.deref);
				}
				return new Pair<>(atts.get(term.deref), ApgTerm.ApgTermDeref(term.deref, x.second));
			}
			if (term.cases != null) {
				Pair<ApgTy<Object>, ApgTerm<Object, Void>> x = infer(ts, ctx, term.arg, atts);

				if (x.first.m == null || x.first.all) {
					throw new RuntimeException("Argument type " + x.first + " is not a variant.");
				}
				Map<String, Pair<String, ApgPreTerm>> l = Util.toMapSafely(term.cases);
				if (!l.keySet().equals(x.first.m.keySet())) {
					throw new RuntimeException("Branches are " + l.keySet() + ", not " + x.first.m.keySet()
							+ " as expected from argument.");
				}
				Map<String, Pair<Var, ApgTerm<Object, Void>>> map = new THashMap<>();

				for (Entry<String, Pair<String, ApgPreTerm>> z : l.entrySet()) {
					if (ctx.containsKey(z.getValue().first)) {
						throw new RuntimeException("Re-bound variable: " + z.getValue().first);
					}
					if (!x.first.m.containsKey(z.getKey())) {
						throw new RuntimeException("Branch missing " + z.getKey());
					}
					Map<String, ApgTy<Object>> ctx2 = new THashMap<>(ctx);
					ctx2.put(z.getValue().first, x.first.m.get(z.getKey()));
					Pair<ApgTy<Object>, ApgTerm<Object, Void>> w = infer(ts, ctx2, z.getValue().second, atts);

					if (!w.first.equals(term.ty)) {
						throw new RuntimeException(
								"Branch return type " + w.first + " is not " + term.ty + " as expected.");
					}
					map.put(z.getKey(), new Pair<>(Var.Var(z.getValue().first), w.second));
				} 

				return new Pair<>(term.ty, ApgTerm.ApgTermCase(x.second, map, term.ty));
			} else if (term.head != null) {
				Triple<List<String>, String, Function<List<Object>, Object>> t = ts.udfs.get(term.head);
				if (t == null) {
					throw new RuntimeException("Not a UDF: " + term.head);
				}
				List<Pair<ApgTy<Object>, ApgTerm<Object, Void>>> l = Util.map(term.args, x->infer(ts, ctx, x, atts));
				if (l.size() != t.first.size()) {
					throw new RuntimeException("In " + term + ", given " + l.size() + " arguments, not " + t.first.size() + " as expected.");
				}
				Iterator<Pair<ApgTy<Object>, ApgTerm<Object, Void>>> it = l.iterator();
				Iterator<String> jt = t.first.iterator();
				List<ApgTerm<Object, Void>> ll = new LinkedList<>();
				for (int i = 0; i < l.size(); i++) {
					Pair<ApgTy<Object>, ApgTerm<Object, Void>> x = it.next();
					String y = jt.next();
					if (x.first.b == null || !x.first.b.equals(y)) {
						throw new RuntimeException("In " + term + ", argument " + i + " (" + term.args.get(i) + ") has type " + x + " not " + y + " as expected.");
					}				
					ll.add(x.second);
				}
				return new Pair<>(ApgTy.ApgTyB(t.second), ApgTerm.ApgTermApp(term.head, ll));
			}

			return Util.anomaly();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((Ls == null) ? 0 : Ls.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			return result;
		}

		@Override
		public Pair<ApgSchExp, ApgSchExp> type(AqlTyping G) {
			ApgTyExp a = src.type(G);
			ApgTyExp b = dst.type(G);
			if (!a.equals(b)) {
				throw new RuntimeException("Typeside mismatch: " + a + " and " + b);
			}
			return new Pair<>(src, dst);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ApgMapExpRaw other = (ApgMapExpRaw) obj;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (Ls == null) {
				if (other.Ls != null)
					return false;
			} else if (!Ls.equals(other.Ls))
				return false;
			return true;
		}

	}

	public static final class ApgMapExpCompose extends ApgMapExp {
		public final ApgMapExp h1, h2;

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
			ApgMapExpCompose other = (ApgMapExpCompose) obj;
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

		public ApgMapExpCompose(ApgMapExp h1, ApgMapExp h2) {
			this.h1 = h1;
			this.h2 = h2;
		}

		@Override
		public <R, P> R accept(P params, ApgMapExpVisitor<R, P> v) {
			return null; // v.visit(params, this);
		}

		@Override
		public <R, P> ApgMapExp coaccept(P params, ApgMapExpCoVisitor<R, P> v, R r) {
			return null; // v.visitApgMapExpCompose(params, r);
		}

		@Override
		public Pair<ApgSchExp, ApgSchExp> type(AqlTyping G) {
			Pair<ApgSchExp, ApgSchExp> a = h1.type(G);
			Pair<ApgSchExp, ApgSchExp> b = h2.type(G);
			if (!a.second.equals(b.first)) {
				throw new RuntimeException("Intermediate schemas do not match: " + a.second + " and " + b.first);
			}
			return new Pair<>(a.first, b.second);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h1.mapSubExps(f);
			h2.mapSubExps(f);
		}

		@Override
		protected ApgMapping<Object, Object> eval0(AqlEnv env, boolean isCompileTime) {
			return ApgOps.composeMapping(h1.eval(env, isCompileTime), h2.eval(env, isCompileTime));
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(h1.deps(), h2.deps());
		}

	}
}
