package catdata.cql.fdm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.DP;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.SqlTypeSide;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.Algebra.TAlg;

public class SkolemInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Instance<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>, X, Pair<X, Att>> {

	public SkolemInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
		//if (i.algebra().hasNulls()) {
		//	throw new RuntimeException("For expediency, skolemization must start from a ground instance.");
		//}
		// TODO: check on SQL schema
		I = i;
	//	validate();
	//	validateMore();
		Instance<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>, X, Pair<X, Att>> z = this;
		trans = new Transform<>() {

			@Override
			public BiFunction<Gen, En, Term<Void, En, Void, Fk, Void, Gen, Void>> gens() {
				return (x,y) -> Term.Gen(x);
			}

			@Override
			public BiFunction<Pair<X, Att>, Ty, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks() {
				
				return (x,y) -> {
					//catdata.aql.exp.Sym isNull = catdata.aql.exp.Sym.Sym("isNull", new Pair<>( Collections.singletonList(y.toString()),"Boolean"));
					Term<Void, En, Void, Fk, Void, Gen, Void> u = algebra().repr(schema().atts.get(x.second).first, x.first);
					
				//	Term<Ty, En, catdata.aql.exp.Sym, Fk, Att, Gen, Pair<X,Att>> zzz = Term.Sym(isNull,Collections.singletonList(u.convert()));
				//	Term<Ty, En, catdata.aql.exp.Sym, Fk, Att, Gen, Pair<X,Att>> www = Term.Sym(SqlTypeSide.t, Collections.emptyList());
				//	if (dp().eq(null,(Term)zzz,(Term)www)) {
				//		return Term.Obj(Optional.empty(), y);
				//	} else {
						return Term.Att(x.second, u.convert());
				//	}
				//	return null;
				};
			}

			@Override
			public Instance<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>, X, Pair<X, Att>> src() {
				return z;
			}

			@Override
			public Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> dst() {
				return I;
			}			
		};
		trans.validate(false);
	}
	
	public final Transform<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>, Gen, Sk, X, Pair<X, Att>, X, Y> trans;
	

	public final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return I.schema();
	}

	@Override
	public IMap<Gen, En> gens() {
		return I.gens();
	}

	@Override
	public IMap<Pair<X, Att>, Ty> sks() {
		return new IMap<Pair<X, Att>, Ty>() {

			@Override
			public Ty get(Pair<X, Att> x) {
				return schema().atts.get(x.second).second;
			}

			@Override
			public boolean containsKey(Pair<X, Att> x) {
				return true; // unless illtyped
			}

			@Override
			public void entrySet(BiConsumer<? super Pair<X, Att>, ? super Ty> f) {
				for (En en : schema().ens) {
					for (Att att : schema().attsFrom(en)) {
						Ty ty = schema().atts.get(att).second;
						for (X x : I.algebra().en(en)) {
							f.accept(new Pair<>(x, att), ty);
						}
					}
				}
			}

			@Override
			public int size() {
				int ret = 0;
				for (En en : schema().ens) {
					int n = I.algebra().size(en);
					int m = schema().attsFrom(en).size();
					ret += (n * m);
				}
				return ret;
			}

			@Override
			public Ty remove(Pair<X, Att> x) {
				return Util.anomaly();
			}

			@Override
			public void put(Pair<X, Att> x, Ty y) {
				Util.anomaly();
			}

		};
	}

	@Override
	public boolean requireConsistency() {
		return false;
	}

	@Override
	public boolean allowUnsafeJava() {
		return true;
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>> dp() {
		return new DP<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>>() {

			@Override
			public String toStringProver() {
				return "skolem";
			}

			@Override
			public boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>> lhs,
					Term<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>> rhs) {
				if (!lhs.hasTypeType()) {
					return I.dp().eq(ctx, lhs.convert(), rhs.convert());
				}
				
				
				return I.algebra().schema().typeSide.js.reduce(algebra().intoY(rewriteIsNull(lhs))).equals(I.algebra().schema().typeSide.js.reduce(algebra().intoY(rewriteIsNull(rhs))));  
			}

		};
	}
	
	public Term<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>> rewriteIsNull(Term<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>> t) {
		if (t.obj() != null || t.gen() != null || t.var != null || t.sk() != null) {
			return t;
		} else if (t.att() != null) {
			return Term.Att(t.att(), rewriteIsNull(t.arg));
		} else if (t.fk() != null) {
			return Term.Fk(t.fk(), rewriteIsNull(t.arg));
		} else if (t.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>>> l = new LinkedList<>();
			for (var x : t.args) {
				l.add(rewriteIsNull(x));
			}
			if (t.sym().toString().equals("isNull")) {
				if (l.size() != 1) {
					Util.anomaly();
				}
				
				if (l.get(0).sk() != null) {
					Pair<X,Att> sk = l.get(0).sk();
					var ret = I.algebra().att(sk.second, sk.first);
					if (ret.obj() == null) {
						Util.anomaly();
					}
					Optional<Object> o = (Optional<Object>) ret.obj();
					if (o.isPresent()) {
						return SqlTypeSide.f.convert();
					} else {
						return SqlTypeSide.t.convert();
					}
				} else if (l.get(0).att() != null) {
					X x = algebra().intoX(l.get(0).arg); 
//					En en = schema().atts.get(l.get(0).att()).first;
					Pair<X,Att> sk = new Pair<>(x, l.get(0).att());
					var ret = I.algebra().att(sk.second, sk.first);
					if (ret.obj() == null) {
						Util.anomaly();
					}
					Optional<Object> o = (Optional<Object>) ret.obj();
					if (o.isPresent()) {
						return SqlTypeSide.f.convert();
					} else {
						return SqlTypeSide.t.convert();
					}					
				}
			} else {
				return Term.Sym(t.sym(), l);
			}
				
		}
		throw new RuntimeException(t.toString());
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>, X, Pair<X, Att>> algebra() {

		List<Pair<Term<Ty, Void, Sym, Void, Void, Void, Pair<X, Att>>, Term<Ty, Void, Sym, Void, Void, Void, Pair<X, Att>>>> eqs = new LinkedList<>();
		for (En en : schema().ens) {
			for (Att att : schema().attsFrom(en)) {
				for (X x : I.algebra().en(en)) {
					Term<Ty, Void, Sym, Void, Void, Void, Y> v = I.algebra().att(att, x);
					if (v.obj() == null) {
						Util.anomaly();
					}
					Optional<Object> o = ((Optional<Object>) v.obj());
					Term<Ty, Void, Sym, Void, Void, Void, Pair<X, Att>> lhs = Term.Sk(new Pair<>(x, att));
					catdata.cql.exp.Sym lhs2 = catdata.cql.exp.Sym.Sym("isNull", new Pair(Collections.singletonList(v.ty()), "Boolean"));

					if (o.isPresent()) {
						eqs.add(new Pair<>(lhs, Term.Obj(v.obj(),v.ty())));						
						eqs.add(new Pair(Term.Sym((Sym)lhs2, Collections.singletonList(lhs)), SqlTypeSide.f));
						//TODO: add isNull action
					} else {
						eqs.add(new Pair(Term.Sym((Sym)lhs2, Collections.singletonList(lhs)), SqlTypeSide.t));
					}
				}
			}
		}

		TAlg<Ty, Sym, Pair<X, Att>> theTalg = new TAlg<Ty, Sym, Pair<X, Att>>(Instance.imapToMapNoScan(sks()), eqs);

		return new Algebra<Ty, En, Sym, Fk, Att, Gen, Pair<X, Att>, X, Pair<X, Att>>() {

			@Override
			public Schema<Ty, En, Sym, Fk, Att> schema() {
				return I.schema();
			}

			@Override
			public boolean hasNulls() {
				return true; // close enough
			}

			@Override
			public Iterable<X> en(En en) {
				return I.algebra().en(en);
			}

			@Override
			public X gen(Gen gen) {
				return I.algebra().gen(gen);
			}

			@Override
			public X fk(Fk fk, X x) {
				return I.algebra().fk(fk, x);
			}

			@Override
			public Term<Ty, Void, Sym, Void, Void, Void, Pair<X, Att>> att(Att att, X x) {
				return sk(new Pair<>(x, att));
			}

			@Override
			public Term<Ty, Void, Sym, Void, Void, Void, Pair<X, Att>> sk(Pair<X, Att> sk) {
				var ret = I.algebra().att(sk.second, sk.first);
				if (ret.obj() == null) {
					Util.anomaly();
				}
				Optional<Object> o = (Optional<Object>) ret.obj();
				if (o.isPresent()) {
					return Term.Obj(o, ret.ty());
				}
				return Term.Sk(sk);
			}

			@Override
			public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x) {
				return I.algebra().repr(en, x);
			}

			@Override
			public int size(En en) {
				return I.algebra().size(en);
			}

			@Override
			protected TAlg<Ty, Sym, Pair<X, Att>> talg0() {
				return theTalg;
			}

		
			
			@Override
			public Chc<Pair<X, Att>, Pair<X, Att>> reprT_prot(Pair<X, Att> y) {
				return Chc.inRight(y);
			}

			@Override
			public String toStringProver() {
				return "Skolem Instance Algebra";
			}

			@Override
			public Object printX(En en, X x) {
				return I.algebra().printX(en, x);
			}

			@Override
			public Object printY(Ty ty, Pair<X, Att> y) {
				return y.first + "." + y.second; // TODO
			}

		};
	}

}
