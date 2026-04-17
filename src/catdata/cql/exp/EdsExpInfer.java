package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.LocStr;
import catdata.Pair;
import catdata.Quad;
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.exp.QueryExpRaw.PreBlock;

public class EdsExpInfer extends EdsExp {
	SchExp ST;
	EdsExp S, T;
	List<Quad<String, String, String, String>> maps;

	public EdsExpInfer(EdsExp S, EdsExp T, SchExp ST, List<Quad<String, String, String, String>> maps) {
		this.S = S;
		this.T = T;
		this.ST = ST;
		this.maps = maps;
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isVar() {
		return false;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(ST.deps(), Util.union(S.deps(), T.deps()));
	}

	@Override
	public synchronized Constraints eval0(AqlEnv env, boolean isC) {
		List<ED> eds = new LinkedList<>();
		var st = ST.eval(env, isC);
		var s = S.eval(env, isC);
		var t = T.eval(env, isC);
		
		if (s.schema.fks.size() != 0 || t.schema.fks.size() != 0 || st.fks.size() != 0) {
			throw new RuntimeException("Can't have FKs in infer");
		}
		
		Set<String> srcs = new HashSet<>();
		Set<String> dsts = new HashSet<>();
		
		List<Query<String, String, Sym, Fk, Att, String, Fk, Att>> srcQs = new LinkedList<>();
		List<Query<String, String, Sym, Fk, Att, String, Fk, Att>> dstQs = new LinkedList<>();
		
		
		for (var q : maps) {
			if (!s.schema.ens.contains(q.first)) {
				throw new RuntimeException("Not a source entity, " + q.first);
			}
			srcs.add(q.first);
			if (!t.schema.ens.contains(q.third)) {
				throw new RuntimeException("Not a target entity, " + q.third);
			}
			dsts.add(q.third);
			Pair<String, String> sAtt = s.schema.atts.get(Att.Att(q.first, q.second));
			if (sAtt == null) {
				throw new RuntimeException("Not a source attribute, " + q.second);
			}
			Pair<String, String> tAtt = t.schema.atts.get(Att.Att(q.third, q.fourth));
			if (tAtt == null) {
				throw new RuntimeException("Not a target attribute, " + q.fourth);
			}
		}
		//query exp chase
		for (String en : srcs) {
			List<Pair<LocStr, String>> gens = new LinkedList<>();	
			gens.add(new Pair<>(new LocStr(0, en), en));
			
			var q = new QueryExpRawSimple(ST, 0, new PreBlock(gens, new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), true));
			var p = q.eval(env, isC);
			
			var r = QueryExpChase.chase(p, s, false);
			
			srcQs.add(r);
		}
		for (String en : dsts) {
			List<Pair<LocStr, String>> gens = new LinkedList<>();	
			gens.add(new Pair<>(new LocStr(0, en), en));
			
			var q = new QueryExpRawSimple(ST, 0, new PreBlock(gens, new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), true));
			var p = q.eval(env, isC);
			
			var r = QueryExpChase.chase(p, t, false);
			
			dstQs.add(r);
		}
		

		for (Query<String, String, Sym, Fk, Att, String, Fk, Att> q : srcQs) {
			for (Query<String, String, Sym, Fk, Att, String, Fk, Att> p : dstQs) {
				Map<String, Chc<String, String>> as = new HashMap<>();
				Map<String, Chc<String, String>> es = new HashMap<>();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> awh = new HashSet<>();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new HashSet<>();

				Map<Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> m1 = new HashMap<>();
				Map<Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> m2 = new HashMap<>();
				
				
				Map<String, Set<String>> as0 = Util.newSetsFor(st.ens);
				//Map<String, Set<String>> as1 = Util.newSetsFor(st.typeSide.tys);
				Map<String, Set<String>> es0 = Util.newSetsFor(st.ens);
			//	Map<String, Set<String>> es1 = Util.newSetsFor(st.typeSide.tys);
				
				for (var x : q.ens.get("Q").gens.entrySet()) {
					as.put(x.getKey(), Chc.inLeft(x.getValue()));
					m1.put(Term.Gen(x.getKey()), Term.Var(x.getKey()));
					as0.get(x.getValue()).add(x.getKey());
				}
				for (var x : q.ens.get("Q").sks.entrySet()) {
					as.put(x.getKey(), Chc.inRight(x.getValue()));
					m1.put(Term.Sk(x.getKey()), Term.Var(x.getKey()));
				//	as1.get(x.getValue()).add(x.getKey());
				}
				for (var x : p.ens.get("Q").gens.entrySet()) {
					es.put(x.getKey(), Chc.inLeft(x.getValue()));
					m2.put(Term.Gen(x.getKey()), Term.Var(x.getKey()));
					es0.get(x.getValue()).add(x.getKey());
				}
				for (var x : p.ens.get("Q").sks.entrySet()) {
					es.put(x.getKey(), Chc.inRight(x.getValue()));
					m2.put(Term.Sk(x.getKey()), Term.Var(x.getKey()));
			//		es1.get(x.getValue()).add(x.getKey());
				}
				
				
				for (var eq : q.ens.get("Q").eqs) {				
					Term<String, String, Sym, Fk, Att, Void, Void> lhs = eq.first.replace(m1).convert();
					Term<String, String, Sym, Fk, Att, Void, Void> rhs = eq.second.replace(m1).convert();
					awh.add(new Pair<>(lhs, rhs));
				}
				for (var eq : p.ens.get("Q").eqs) {
					Term<String, String, Sym, Fk, Att, Void, Void> lhs = eq.first.replace(m2).convert();
					Term<String, String, Sym, Fk, Att, Void, Void> rhs = eq.second.replace(m2).convert();
					ewh.add(new Pair<>(lhs, rhs));
				}
				

				for (var m : maps) {
					for (var j : as0.get(m.first)) {
						for (var k : es0.get(m.third)) {
							Term<String, String, Sym, Fk, Att, Void, Void> lhs = Term.Att(m.second, Term.Var(j)).convert();
							Term<String, String, Sym, Fk, Att, Void, Void> rhs = Term.Att(m.fourth, Term.Var(k)).convert();
							ewh.add(new Pair<>(lhs, rhs)); 

						}
					}
				}
				
				eds.add(new ED(as, es, awh, ewh, false, env.defaults));
				

			}
					
					

	}

	return new Constraints(st,eds,env.defaults);}

	@Override
	public String toString() {
		return "infer " + S + " -> " + " " + T + " : " + ST;
	}

	@Override
	public SchExp type(AqlTyping G) {
		var st = ST.type(G);
		var s = S.type(G);
		var t = T.type(G);
		// todo real checking
		return ST;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		S.mapSubExps(f);
		T.mapSubExps(f);
		ST.mapSubExps(f);

	}

	@Override
	public int hashCode() {
		return Objects.hash(S, ST, T, maps);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EdsExpInfer other = (EdsExpInfer) obj;
		return Objects.equals(S, other.S) && Objects.equals(ST, other.ST) && Objects.equals(T, other.T)
				&& Objects.equals(maps, other.maps);
	}

}