package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.exp.SchExp.SchExpLit;
import catdata.aql.exp.SchExp.SchExpPrefix;
import gnu.trove.map.hash.THashMap;

public abstract class MapExp extends Exp<Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> {

	@Override
	public Kind kind() {
		return Kind.MAPPING;
	}

	public abstract Pair<SchExp, SchExp> type(AqlTyping G);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Exp<Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> Var(String v) {
		Exp ret = new MapExpVar(v);
		return ret;
	}

	public static interface MapExpCoVisitor<R, P, E extends Exception> {
		public MapExpComp visitMapExpComp(P params, R exp) throws E;

		public MapExpId visitMapExpId(P params, R exp) throws E;

		public MapExpLit visitMapExpLit(P params, R exp) throws E;

		public <Gen, Sk, X, Y> MapExpPivot<Gen, Sk, X, Y> visitMapExpPivot(P params, R exp) throws E;

		public MapExpVar visitMapExpVar(P params, R exp) throws E;

		public MapExpRaw visitMapExpRaw(P params, R exp) throws E;

		public MapExpColim visitMapExpColim(P params, R exp) throws E;

		public MapExpToPrefix visitMapExpToPrefix(P params, R exp) throws E;

		public MapExpFromPrefix visitMapExpFromPrefix(P params, R exp) throws E;

	}

	public abstract <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E;

	public static interface MapExpVisitor<R, P, E extends Exception> {
		public <Gen, Sk, X, Y> R visit(P params, MapExpPivot<Gen, Sk, X, Y> mapExpPivot) throws E;

		public R visit(P params, MapExpId exp) throws E;

		public R visit(P params, MapExpLit exp) throws E;

		public R visit(P params, MapExpComp exp) throws E;

		public R visit(P params, MapExpVar exp) throws E;

		public R visit(P params, MapExpRaw exp) throws E;

		public R visit(P params, MapExpColim exp) throws E;

		public R visit(P params, MapExpToPrefix exp) throws E;

		public R visit(P params, MapExpFromPrefix exp) throws E;
	}

	public abstract <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E;

	public static final class MapExpVar extends MapExp {
		public final String var;

		@Override
		public boolean isVar() {
			return true;
		}

		public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitMapExpVar(params, r);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.MAPPING));
		}

		public MapExpVar(String var) {
			this.var = var;
		}

		@Override
		public Mapping<catdata.aql.exp.Ty, En, catdata.aql.exp.Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env,
				boolean isC) {
			return env.defs.maps.get(var);
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
			MapExpVar other = (MapExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			if (!G.defs.maps.containsKey(var)) {
				throw new RuntimeException("Not a mapping: " + var);
			}
			return (Pair<SchExp, SchExp>) ((Object) G.defs.maps.get(var));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

/////////////////////////////////////////////////////////////////////

	public static final class MapExpLit extends MapExp {

		public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitMapExpLit(params, r);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		public final Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> map;

		public MapExpLit(Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> map) {
			this.map = map;
		}

		@Override
		public Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return map;
		}

		@Override
		public int hashCode() {
			return map.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MapExpLit other = (MapExpLit) obj;
			return map.equals(other.map);
		}

		@Override
		public String toString() {
			return ("constant " + map).trim();
		}

		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			return new Pair<>(new SchExpLit(map.src), new SchExpLit(map.dst));
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	/////////////////////////////////////////////////////////////////

	public static class MapExpToPrefix extends MapExp {

		public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitMapExpToPrefix(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return sch.deps();
		}

		public final SchExp sch;
		public final String prefix;

		public MapExpToPrefix(SchExp sch, String p) {
			this.sch = sch;
			this.prefix = p;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + (sch.hashCode());
			result = prime * result + (prefix.hashCode());
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
			MapExpToPrefix other = (MapExpToPrefix) obj;
			return sch.equals(other.sch) && prefix.equals(other.prefix);
		}

		@Override
		public String toString() {
			return "to_prefix " + sch + " " + prefix;

		}


		
		public static Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> toPrefix(Schema<Ty, En, Sym, Fk, Att> s,
				Schema<Ty, En, Sym, Fk, Att> t, String prefix) {
			Map<En, En> ens = new THashMap<>(s.size() * 2);
			for (En en : s.ens) {
				ens.put(en, En.En(prefix + en.str));
			}
			Map<Fk, Pair<En, List<Fk>>> fks = Util.mk();
			for (Fk fk : s.fks.keySet()) {
				fks.put(fk, new Pair<>(En.En(prefix + s.fks.get(fk).first.str),
						Collections.singletonList(Fk.Fk(En.En(prefix + s.fks.get(fk).first.str), prefix + fk.str))));
			}
			Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> atts = Util.mk();
			Var v = Var.Var("v");
			for (Att att : s.atts.keySet()) {
				atts.put(att, new Triple<>(v, En.En(prefix + s.atts.get(att).first),
						Term.Att(Att.Att(En.En(prefix + s.atts.get(att).first), prefix + att.str), Term.Var(v))));
			}
			return new Mapping<>(ens, atts, fks, s, t, true);
		}
		
	

		@Override
		public Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
			var s = sch.eval(env, isC);
			var t = new SchExpPrefix(sch, prefix).eval(env, isC);
			return toPrefix(s, t, prefix);
		}

		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			sch.type(G);
			return new Pair<>(sch, new SchExpPrefix(sch, prefix));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			sch.map(f);
		}

	}

	//////

	public static class MapExpFromPrefix extends MapExp {

		public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitMapExpFromPrefix(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return sch.deps();
		}

		public final SchExp sch;
		public final String prefix;

		public MapExpFromPrefix(SchExp sch, String p) {
			this.sch = sch;
			this.prefix = p;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + (sch.hashCode());
			result = prime * result + (prefix.hashCode());
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
			MapExpFromPrefix other = (MapExpFromPrefix) obj;
			return sch.equals(other.sch) && prefix.equals(other.prefix);
		}

		@Override
		public String toString() {
			return "from_prefix " + sch + " " + prefix;

		}
		
		public static Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> fromPrefix(Schema<Ty, En, Sym, Fk, Att> s,
				Schema<Ty, En, Sym, Fk, Att> t, String prefix) {
			Map<En, En> ens = new THashMap<>(s.size() * 2);
			for (En en : s.ens) {
				ens.put(En.En(prefix + en.str), en);
			}
			Map<Fk, Pair<En, List<Fk>>> fks = Util.mk();
			for (Fk fk : s.fks.keySet()) {
				fks.put(Fk.Fk(En.En(prefix + s.fks.get(fk).first.str), prefix + fk.str), new Pair<>(s.fks.get(fk).first,
						Collections.singletonList(fk)));
			}
			Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> atts = Util.mk();
			Var v = Var.Var("v");
			for (Att att : s.atts.keySet()) {
				atts.put(Att.Att(En.En(prefix + s.atts.get(att).first), prefix + att.str), new Triple<>(v, s.atts.get(att).first,
						Term.Att(att, Term.Var(v))));
			}
			return new Mapping<>(ens, atts, fks, t, s, true);
		}

		@Override
		public Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
			var s = sch.eval(env, isC);
			var t = new SchExpPrefix(sch, prefix).eval(env, isC);
			return fromPrefix(s, t, prefix);
		}

		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			sch.type(G);
			return new Pair<>(new SchExpPrefix(sch, prefix), sch);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			sch.map(f);
		}

	}
}