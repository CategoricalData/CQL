package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.provers.KBExp;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.strategy.HashingStrategy;

public final class Term<Ty, En, Sym, Fk, Att, Gen, Sk> 
 implements KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> {
	
	
	public Sym sym() {
		if (_head == null) {
			return null;
		}
		return _head.sym();
	}

	public Fk fk() {
		if (_head == null) {
			return null;
		}
		return _head.fk();
	}

	public Att att() {
		if (_head == null) {
			return null;
		}
		return _head.att();
	}

	public Gen gen() {
		if (_head == null) {
			return null;
		}
		return _head.gen();
	}

	public Sk sk() {
		if (_head == null) {
			return null;
		}
		return _head.sk();
	}

	public Object obj() {
		if (_head == null) {
			return null;
		}
		return _head.obj();
	}

	public Ty ty() {
		if (_head == null) {
			return null;
		}
		return _head.ty();
	}
	
	public <X> X visit(Function<Var, X> varf, BiFunction<Object, Ty, X> tyf, BiFunction<Sym, List<X>, X> symf, BiFunction<Fk, X, X> fkf, BiFunction<Att, X, X> attf, Function<Gen, X> genf, Function<Sk, X> skf) {
		if (var != null) {
			return varf.apply(var);
		} else if (obj() != null) {
			return tyf.apply(obj(), ty());
		} else if (fk() != null) {
			return fkf.apply(fk(), arg.visit(varf, tyf, symf, fkf, attf, genf, skf));
		} else if (att() != null) {
			return attf.apply(att(), arg.visit(varf, tyf, symf, fkf, attf, genf, skf));
		} else if (gen() != null) {
			return genf.apply(gen());
		} else if (sk() != null) {
			return skf.apply(sk());
		} else if (sym() != null) {
			List<X> l = (new ArrayList<>(args.size()));
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : args) {
				l.add(x.visit(varf, tyf, symf, fkf, attf, genf, skf));
			}
			return symf.apply(sym(), l);
		} 
		throw new RuntimeException("Anomaly: please report");
	}
	
	
	private static <Ty, En, Sym, Fk, Att, Gen, Sk,Ty2, En2, Sym2, Fk2, Att2, Gen2, Sk2> Term<Ty2, En2, Sym2, Fk2, Att2, Gen2, Sk2>
	map(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term, Function<Ty,Ty2> tyf, Function<Sym, Sym2> symf, Function<Fk, Fk2> fkf, Function<Att, Att2> attf, Function<Gen, Gen2> genf, Function<Sk, Sk2> skf) {
		return term.visit(Term::Var, (obj,ty) -> Obj(obj, tyf.apply(ty)), (sym,args) -> Sym(symf.apply(sym), args), (fk,arg) -> Fk(fkf.apply(fk),arg), (att,arg) -> Att(attf.apply(att),arg), gen -> Gen(genf.apply(gen)), sk -> Sk(skf.apply(sk)));
	}
	
	public <Ty2, En2, Sym2, Fk2, Att2, Gen2, Sk2> Term<Ty2, En2, Sym2, Fk2, Att2, Gen2, Sk2>
	map(Function<Ty,Ty2> tyf, Function<Sym, Sym2> symf, Function<Fk, Fk2> fkf, Function<Att, Att2> attf, Function<Gen, Gen2> genf, Function<Sk, Sk2> skf) {
		return map(this, tyf, symf, fkf, attf, genf, skf);
	}
	
	public <Fk2> Term<Ty, En, Sym, Fk2, Att, Gen, Sk> mapFk(Function<Fk, Fk2> f) {
		return map(this, Function.identity(), Function.identity(), f, Function.identity(), Function.identity(), Function.identity());
	}
	
	public <Att2> Term<Ty, En, Sym, Fk, Att2, Gen, Sk> mapAtt(Function<Att, Att2> f) {
		return map(this, Function.identity(), Function.identity(), Function.identity(), f, Function.identity(), Function.identity());
	}
	
	public <Gen2> Term<Ty, En, Sym, Fk, Att, Gen2, Sk> mapGen(Function<Gen, Gen2> genf) {
		return map(this, Function.identity(), Function.identity(), Function.identity(), Function.identity(), genf, Function.identity());
	}
	
	public <Gen2,Sk2> Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> mapGenSk(Function<Gen, Gen2> genf, Function<Sk, Sk2> skf) {
		return map(this, Function.identity(), Function.identity(), Function.identity(), Function.identity(), genf, skf);
	}

	public final Var var;
	public final List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args;
	public final Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg;
	
	public Term<Void, En, Void, Fk, Void, Gen, Void> asArgForAtt() {
		if (gen() != null) {
			return convert();
		} else if (var != null) {
			return convert();
		} else if (fk() != null) {
			return asArgForFk();
		}
		throw new RuntimeException("Anomaly: please report " + this);
	}
	
	public Term<Void, En, Void, Fk, Void, Gen, Void> asArgForFk() {
		if (fk() != null) {
			return convert();
		} else if (gen() != null) {
			return convert();
		} else if (var != null) {
			return convert();
		}
		throw new RuntimeException("Anomaly: please report " + this);
	}
	
	@SuppressWarnings("hiding")
	public <Ty, En, Sym, Fk, Att, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> asGen() {
		if (gen() != null) {
			return Gen(gen());
		}
		throw new RuntimeException("Anomaly: please report");
	}
	@SuppressWarnings("hiding")
	public <Ty, En, Sym, Fk, Att, Gen> Term<Ty, En, Sym, Fk, Att, Gen, Sk> asSk() {
		if (sk() != null) {
			return Sk(sk());
		}
		throw new RuntimeException("Anomaly: please report");
	}
	@SuppressWarnings("hiding")
    private <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> asVar() {
		if (var != null) {
			return Var(var);
		}
		throw new RuntimeException("Anomaly: please report");
	}
	@SuppressWarnings("hiding")
	public <En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> asObj() {
		if (obj() != null) {
			return Obj(obj(), ty());
		}
		throw new RuntimeException("Anomaly: please report");
	}

	//these do not care about java
	public boolean isTypeSide() {
		if (var != null) {
			return true;
		} else if (sym() != null) {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> t : args) {
				if (!t.isTypeSide()) {
					return false;
				}
			}
			return true;
		} else if (obj() != null) {
			return true;
		}
		return false;
	}
	
	public boolean hasTypeType(Map<Var, Chc<Ty,En>> map) {
		if (var != null) {
			return map.get(var).left;
		}
		if (obj() != null) {
			return true;
		} else if (sk() != null) {
			return true;
		} else if (gen() != null) {
			return false; 
		} else if (sym() != null) {
			return true;
		} else if (fk() != null) {
			return arg.hasTypeType(map);
		} else if (att() != null) {
			return true;
		}
		return Util.anomaly();
	}
	
	public boolean hasTypeType() {
		if (obj() != null) {
			return true;
		} else if (sk() != null) {
			return true;
		} else if (gen() != null) {
			return false; 
		} else if (sym() != null) {
			return true;
		} else if (fk() != null) {
			return arg.hasTypeType();
		} else if (att() != null) {
			return true;
		} 
		throw new RuntimeException("Encountered variable: " + this + " in hasTypeType, please report.");
	}
	
	public boolean monoidal(boolean varIsTy) {
		if (obj() != null) {
			return true;
		} else if (sk() != null) {
			return true;
		} else if (gen() != null) {
			return false; 
		} else if (sym() != null) {
			return true;
		} else if (fk() != null) {
			return arg.monoidal(varIsTy);
		} else if (att() != null) {
			return false;
		} else if (var != null) {
			return varIsTy;
		}
		return Util.anomaly();
	}

	public <Ty,En> boolean monoidal(Map<Var,Chc<Ty,En>> ctx) {
		if (obj() != null) {
			return true;
		} else if (sk() != null) {
			return true;
		} else if (gen() != null) {
			return false; 
		} else if (sym() != null) {
			return true;
		} else if (fk() != null) {
			return arg.monoidal(ctx);
		} else if (att() != null) {
			return false;
		} else if (var != null) {
			return ctx.get(var).left;
		}
		return Util.anomaly();
	}

	boolean isGround() {
		if (var != null) {
			return false;
		} else if (args != null) {
            for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> t : args) {
                if (!t.isGround()) {
                    return false;
                }
            }
            return true;
        } else
            return obj() != null || arg.isGround();
    }

	
	public Chc<Ty, En> type(Map<Var, Ty> ctxt, Map<Var, En> ctxe, Set<Ty> tys, Map<Sym, Pair<List<Ty>, Ty>> syms, Map<Ty, String> java_tys_string, Collection<En> ens, Map<Att, Pair<En, Ty>> atts, Map<Fk, Pair<En, En>> fks, Map<Gen, En> gens, Map<Sk, Ty> sks) {
		Chc<Ty, En> ret = null;
		if (var != null) {
			if (ctxt.containsKey(var) && ctxe.containsKey(var)) {
				throw new RuntimeException("In " + this + ", " + "name collision on " + var + " in " + ctxt + " and " + ctxe);
			}
			if (ctxe.containsKey(var)) {
				ret = Chc.inRight(ctxe.get(var));
			} else if (ctxt.containsKey(var)) {
				ret = Chc.inLeft(ctxt.get(var));
			} else {
				throw new RuntimeException("In " + this + ", " + var + " is not a variable in context [" + ctxt + "] and [" + ctxe + "]");
			}
		} else if (obj() != null) {
			if (!java_tys_string.containsKey(ty())) {
				throw new RuntimeException("In " + this + ", not a declared type: " + ty());
			} 
			Class<?> c = Util.load(java_tys_string.get(ty()));
			if (!c.isInstance(obj())) {
				throw new RuntimeException("In " + this + ", " + "primitive " + obj() + " is given type " + ty() + " but is not an instance of " + c + ", is an instance of " + obj().getClass());
			}
			ret =  Chc.inLeft(ty());
		} else if (sym() != null) {
			Pair<List<Ty>, Ty> t = syms.get(sym());
			if (t == null) {
				throw new RuntimeException("In " + this + ", " + sym() + " is not a typeside symbol.  Typeside symbols:\n\n" + syms);
			} else if (t.first.size() != args.size()) {
				throw new RuntimeException("In " + this + ", " + sym() + " given " + args.size() + "arguments but requires " + t.first.size());
			}
			for (int i = 0; i < t.first.size(); i++) {
				Chc<Ty, En> u = args.get(i).type(ctxt, ctxe, tys, syms, java_tys_string, ens, atts, fks, gens, sks);
				if (!Chc.inLeft(t.first.get(i)).equals(u)) {
					throw new RuntimeException("In " + this + ", " + "Argument " + args.get(i) + " has sort " + u.toStringMash() + " but requires " + t.first.get(i));
				}
			}
			ret =  Chc.inLeft(t.second);
		} else if (att() != null) {
			Pair<En, Ty> t = atts.get(att());
			if (t == null) {
				throw new RuntimeException("In " + this + ", " + att() + " is not an attribute");
			} 
			Chc<Ty, En> u = arg.type(ctxt, ctxe, tys, syms, java_tys_string, ens, atts, fks, gens, sks);
			if (!Chc.inRight(t.first).equals(u)) {
				throw new RuntimeException("In " + this + ", " + "argument " + arg + " has sort " + u.toStringMash() + " but requires " + t.first);
			}
			ret =  Chc.inLeft(t.second);
		} else if (fk() != null) {
			Chc<Ty, En> u = arg.type(ctxt, ctxe, tys, syms, java_tys_string, ens, atts, fks, gens, sks);
			if (u.left) {
				throw new RuntimeException("In " + this + ", " + arg + " has type " + u.toStringMash() + " which is not an entity");
			}
			Pair<En, En> t = fks.get(fk());
			if (t == null) {
				throw new RuntimeException("In " + this + ", " + fk() + " is not a foreign key.  Possibilities: " + fks.keySet());
			}		
			if (!Chc.inRight(t.first).equals(u)) {
				throw new RuntimeException("In " + this + ", " + "argument " + arg + " has sort " + u.toStringMash() + " but requires " + t.first);
			}
			ret = Chc.inRight(t.second);
		} else if (gen() != null) {
			En en = gens.get(gen());
			if (en == null) {
				throw new RuntimeException("In " + toStringUnambig() + ", " + "the entity for generator " + gen() + " is not defined.  Types of available generators are:\n" + gens );	
			}
			ret = Chc.inRight(en);
		} else if (sk() != null) {
			Ty tye = sks.get(sk());
			if (tye == null) {
				String xxx = sks.size() > 1024 ? " too big to print " : Util.sep(sks, ":", ", ");
				throw new RuntimeException("In " + this + ", " + "the type for labelled null " + sk() + " is not defined.\n\nAvailable: " + xxx);	
			}
			ret = Chc.inLeft(tye);
		}
		if (ret == null || (ret.left && ret.l == null) || (!ret.left && ret.r == null)) {
			throw new RuntimeException("In " + this + "," + " typing encountered an ill-formed term.  Should be impossible, report to Ryan.  " + this);
		} else if (ret.left && !tys.contains(ret.l)) {
			throw new RuntimeException("In " + this + "," + " return type is " + ret.l + " which is not a type");
		} else if (!ret.left && !ens.contains(ret.r)) {
			throw new RuntimeException("In " + this + "," + " return type is " + ret.r + " which is not a entity");		
		}
		return ret;
	}

	private static HashingStrategy<Term> strategy = new HashingStrategy<>() {
		@Override
		public int computeHashCode(Term t) {
			return t.hashCode2();
		}

		@Override
		public boolean equals(Term s, Term t) {
			return s.equals2(t);
		}
	};
		
	public static Map<Term, Term> cache = 
			new TCustomHashMap<>(strategy);
	
	private synchronized static  <Ty, En, Sym, Fk, Att, Gen, Sk>  Term<Ty, En, Sym, Fk, Att, Gen, Sk>  mkTerm
	(Var var, Sym sym, Fk fks, Att att, Gen gen, Sk sk, List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args, Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg, Object obj, Ty ty) {
		Term ret = new Term<>(var, sym, fks, att, gen, sk, args, arg, obj, ty);
		
		Term ret2 = cache.get(ret);
		if (ret2 != null) {
			return ret2;
		}
		cache.put(ret, ret);
		return ret;
	}
	
	//private NonConsTerm back;
	
	 private Term(Var var, Sym sym, Fk fk, Att att, Gen gen, Sk sk, List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args, Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg, Object obj, Ty ty) {
		this.var = var;
		this.args = args;
		this.arg = arg;
		if (var == null) {
			this._head = Head.mkHead(sym, fk, att, gen, sk, obj, ty);
	 	} else {
	 		this._head = null;
	 	}
		//_hashCode = hashCode2();
	} 
	
	public String toStringSql(String tick) {
		if (var != null) {
			return var.toString();
		} else if (sym() != null) {
			if (args.isEmpty()) {
				return sym().toString();
			} 
			return sym().toString() + "(" + Util.sep(args.stream().map(x -> x.toStringSql(tick)).collect(Collectors.toList()), ", ") + ")";
			
		} else if (att() != null) {
			return arg.toStringSql(tick) + "." + tick + att().toString() + tick;
		} else if (fk() != null) {
			return arg.toStringSql(tick) + "." + tick + fk().toString() + tick;
		} else if (gen() != null) {
			return gen().toString(); 
		} else if (sk() != null) {
			return sk().toString();
		} else if (obj() != null) {
			return obj().toString(); // + "@" + ty;
		}
		throw new RuntimeException("Anomaly: please report");
	}

	//TODO: eventually, will want to quote, escape, etc
	public String toString(Function<Sk, String> sk_printer, Function<Gen, String> gen_printer) {
		if (var != null) {
			return var.toString();
		} else if (sym() != null) {
			if (args == null || args.isEmpty()) {
				return sym().toString();
			} /* else if (args.size() == 1) {
				return args.get(0).toString(sk_printer, gen_printer) + "." + sym().toString();
			} */ else if (args.size() == 2) {
				return "(" + args.get(0).toString(sk_printer, gen_printer) + " " + sym().toString() + " " + args.get(1).toString(sk_printer, gen_printer) + ")";
			} else {
				return sym().toString() + "(" + Util.sep(args.stream().map(x -> x.toString(sk_printer, gen_printer)).collect(Collectors.toList()), ", ") + ")";
			}
		} else if (att() != null) {
			return arg.toString(sk_printer, gen_printer)+ "." + att().toString();
		} else if (fk() != null) {
			return arg.toString(sk_printer, gen_printer)+ "." + fk().toString();
		} else if (gen() != null) {
			return gen_printer.apply(gen()); 
		} else if (sk() != null) {
			return sk_printer.apply(sk());
		} else if (obj() != null) {
			return obj().toString(); // + "@" + ty;
		}
		throw new RuntimeException("Anomaly: please report");
	}
	
	public String toStringUnambig() {
		if (var != null) {
			return "VAR " + var + "[" + var.getClass() + "]";
		} else if (sym() != null) {
			return "SYM " + sym() + "[" + sym().getClass() + "]" + "(" + Util.sep(args.stream().map(x -> x.toStringUnambig()).collect(Collectors.toList()), ", ") + ")";
		} else if (att() != null) {
			return "ATT " + arg + "[" + att().getClass() + "]"+ "." + att().toString() + "[" + arg.getClass() + "]";
		} else if (fk() != null) {
			return "FK " + arg + "[" + fk().getClass() + "]" + "." + fk().toString() + "[" + arg.getClass() + "]";
		} else if (gen() != null) {
			return "GEN " + gen() + "[" + gen().getClass() + "]";
		} else if (sk() != null) {
			return "SK " + sk() + "[" + sk().getClass() + "]";
		} else if (obj() != null) {
			return obj().toString() + "@" + ty() + "[" + obj().getClass() + "]";
		}
		return ("Anomaly: please report");
	}
	
	
	@Override
	public String toString() {
		return toString(Object::toString, Object::toString);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Head(Head<Ty, En, Sym, Fk, Att, Gen, Sk> head, List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args) {
		Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret = null;
		if (head.gen() != null) {
			ret = Gen(head.gen());
		} else if (head.sk() != null) {
			ret = Sk(head.sk());
		} else if (head.obj() != null) {
			ret = Obj(head.obj(), head.ty());
		} else if (head.att() != null) {
			ret = Att(head.att(), args.get(0));
		} else if (head.fk() != null) {
			ret = Fk(head.fk(), args.get(0));
		} else if (head.sym() != null) {
			ret = Sym(head.sym(), args);
		}
		if (ret != null) {
			return ret;
		}
		throw new RuntimeException("Anomaly: please report: " + head + "(" + args + ")");
	}
	

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Fks(List<Fk> fks, Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg) {
		for (Fk fk : fks) {
			arg = Fk(fk, arg);
		}
		return arg;
	}


	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Var(Var var) {
		return mkTerm(var, null, null, null, null, null, null, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Sym(Sym sym, List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args) {
		return mkTerm(null, sym, null, null, null, null, args, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Att(Att att, Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg) {
		return mkTerm(null, null, null, att, null, null, null, arg, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Fk(Fk fk, Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg) {
		return mkTerm(null, null, fk, null, null, null, null, arg, null, null);
	}
	
	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Gen(Gen gen) {
		return mkTerm(null, null, null, null, gen, null, null, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Sk(Sk sk) {
		return mkTerm(null, null, null, null, null, sk, null, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> Obj(Object obj, Ty ty) {
		return mkTerm(null, null, null, null, null, null, null, null, obj, ty);
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}


	@Override
	 public boolean equals(Object x) {
		//boolean b = (this == x);
		//boolean c = (hashCode() == x.hashCode());
		//if (b != c) {
		//	Util.anomaly();
		//}
		return this == x;
	 }
	

	//returns null if no var
    Var getOnlyVar() {
		if (var != null) {
			return var;
		} else if (sym() != null) {
			Var var = null;
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args) {
				Var var2 = arg.getOnlyVar();
				if (var2 == null) {
					continue;
				}
				if (var == null) {
					var = var2;
					continue;
				}
				if (!var.equals(var2)) {
					return null;
				}
			}
			return var;
		} else if (fk() != null || att() != null) {
			return arg.getOnlyVar();
		} else if (gen() != null || sk() != null || obj() != null) {
			return null;
		}
		throw new RuntimeException("Anomaly: please report");
	}

	

	public boolean containsProper(Head<Ty, En, Sym, Fk, Att, Gen, Sk> head) {
		return !equalsH(head) && contains(head);
	}
	public boolean contains(Head<Ty, En, Sym, Fk, Att, Gen, Sk> head) {
		if (var != null) {
			return false;
		} else if (equalsH(head)) {
			return true;
		}
		for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
			if (arg.contains(head)) {
				return true;
			}
		}
		return false;
	}
	

	public synchronized void forEachArg(Function<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Unit> f) {
		if (arg != null) {
			f.apply(arg);
		} else if (args != null) {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : args) {
				f.apply(x);
			}
		}
	}
	
	//private List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> a;
	public synchronized List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args() {
		if (args != null) {
			return args;
		} else if (arg != null) {
			return Collections.singletonList(arg);
		} else {
			return Collections.emptyList();
		}
	}

	public synchronized Term<Ty, En, Sym, Fk, Att, Gen, Sk> replaceHead(Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>,Term<Ty, En, Sym, Fk, Att, Gen, Sk>> replacee, List<Var> vars) {
		if (var != null) {
			return this;
		}
		
		List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args = (new ArrayList<>(argSize()));
		forEachArg((arg) -> {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> x = arg.replaceHead(replacee, vars);
			args.add(x);
			return null;
		});
		Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret = replacee.get(Head.mkHead(this));
		if (ret != null) {
			if (vars == null) {
				return make(ret, args);
			}
			Map<Var, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> map = new THashMap<>(vars.size());
			int i = 0;
			for (Var var : vars) {
				map.put(var, args.get(i++));
			}
			return ret.subst(map);	
		}
				
		return make(this, args);		
	}
	
	private int argSize() {
		if (arg != null) {
			return 1;
		} else if (args != null) {
			return args.size();
		}
		return 0;
	}

	public synchronized Term<Ty, En, Sym, Fk, Att, Gen, Sk> replaceHead(Head<Ty, En, Sym, Fk, Att, Gen, Sk> replacee, List<Var> vars, Term<Ty, En, Sym, Fk, Att, Gen, Sk> replacer) {
		if (var != null) {
			return this;
		}
		
		List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args = new ArrayList<>(argSize());
		forEachArg((arg) -> {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> x = arg.replaceHead(replacee, vars, replacer);
			args.add(x);
			return null;
		});

		if (!equalsH(replacee)) {
			return make(this, args);
		}
				
		Map<Var, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> map = new THashMap<>(vars.size());
		int i = 0;
		for (Var var : vars) {
			map.put(var, args.get(i++));
		}
		return replacer.subst(map);
				
	}

	private Term<Ty, En, Sym, Fk, Att, Gen, Sk> make(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term,
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args2) {
		if (ty() != null || gen() != null || sk() != null) {
			return term;
		}
		if (sym() != null) {
			return Term.Sym(sym(), args2);
		}
		if (fk() != null) {
			return Term.Fk(fk(), args2.get(0));
		}
		if (att() != null) {
			return Term.Att(att(), args2.get(0));
		}
	
		return Util.anomaly();
	}


	private boolean equalsH(Head<Ty, En, Sym, Fk, Att, Gen, Sk> o) {
		return _head.equals(o);
	}

	public synchronized Term<Ty, En, Sym, Fk, Att, Gen, Sk> subst(Map<Var, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> map) {
		if (var != null) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> z = map.get(var);
			if (z != null) {
				return z;
			}
			return this;
		} 
		List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args = (new ArrayList<>(args().size()));
		for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : args()) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> z = x.subst(map);
			args.add(z);
		}
		Term<Ty, En, Sym, Fk, Att, Gen, Sk> x = make(this, args);
		return x;
	}

	public KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> toKB() {
		return this;
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> fromKB(KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> e) {
		return (Term<Ty, En, Sym, Fk, Att, Gen, Sk>) e;
	}
	
	public synchronized Set<Sk> sks() {
		Set<Sk> ret = new THashSet<>();
		sks(ret);
		return ret;
	}
	
	public void gens(Set<Gen> gens) {
		if (var != null) {
		} else if (gen() != null) {
			gens.add(gen());
		} else {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
				arg.gens(gens);
			}
		} 
	}
	
	public void sks(Set<Sk> sks) {
		if (var != null) {
			
		} else if (sk() != null) {
			sks.add(sk());
		} else {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
				arg.sks(sks);
			}
		} 
	}
	
	public void fks(Set<Fk> fks) {
		if (var != null) {
			
		} else if (fk() != null) {
			fks.add(fk());
		} else {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
				arg.fks(fks);
			}
		} 
	}
	
	public void atts(Set<Att> atts) {
		if (var != null) {
			
		} else if (att() != null) {
			atts.add(att());
		} else {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
				arg.atts(atts);
			}
		} 
	}
	
	public void syms(Set<Sym> syms) {
		if (var != null) {
			
		} else if (sym() != null) {
			syms.add(sym());
		} else {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
				arg.syms(syms);
			}
		} 
	}

	public void objs(Set<Pair<Object, Ty>> objs) {
		if (var != null) {
			return;
		} else if (obj() != null) {
			objs.add(new Pair<>(obj(), ty()));
		} else {
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : args()) {
				arg.objs(objs);
			}
		} 
	}

	@SuppressWarnings({ "unchecked", "hiding" })
	//@Deprecated
	public <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> convert() {
		return (Term<Ty, En, Sym, Fk, Att, Gen, Sk>) this;
	}


	public Collection<Var> vars() {
		return toKB().getVars();
	} 
	
	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> upTalg(Term<Ty, Void, Sym, Void, Void, Void, Sk> term) {
		return term.convert(); //.map(Function.identity(), Function.identity(), Util.voidFn(), Util.voidFn(), Util.voidFn(), Function.identity()); 
	}	
 

	public Set<Gen> gens() {
		Set<Gen> ret = new THashSet<>();
		gens(ret);
		return Collections.unmodifiableSet(ret);
	}
	
	public Set<Fk> fks() {
		Set<Fk> ret = new THashSet<>();
		fks(ret);
		return Collections.unmodifiableSet(ret);
	}
	
	public Set<Att> atts() {
		Set<Att> ret = new THashSet<>();
		atts(ret);
		return Collections.unmodifiableSet(ret);
	}
	
	public Set<Sym> syms() {
		Set<Sym> ret = new THashSet<>();
		syms(ret);
		return Collections.unmodifiableSet(ret);
	}
	
	Set<Pair<Object,Ty>> objs;
	public synchronized Set<Pair<Object,Ty>> objs() {
		if (objs != null) {
			return objs;
		}
		objs = new THashSet<>();
		objs(objs);
		return objs;
	}
	
	public void toFkList(List<Fk> l) {
		if (var != null || gen() != null) {
		} else if (fk() != null) {
			arg.toFkList(l);
			l.add(fk()); 
		} else {
			Util.anomaly();
		}
	}
	
	public List<Fk> toFkList() {
		List<Fk> l = new LinkedList<>();
		toFkList(l);
		return Collections.unmodifiableList(l);
	}

	public Term<Ty, En, Sym, Fk, Att, Gen, Sk> replace(Map<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> g) {
		if (g.containsKey(this)) {
			return g.get(this);
		} else if (obj() != null || gen() != null || sk() != null || var != null) {
			return this;
		} else if (fk() != null) {
			return Term.Fk(fk(), arg.replace(g));
		} else if (att() != null) {
			return Term.Att(att(), arg.replace(g));
		} else if (sym() != null) {
			if (args.size() == 0) {
				return this;
			} 
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> l = new ArrayList<>(args.size());
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : args) {
				l.add(x.replace(g));
			}
			return Term.Sym(sym(), l);
		}
		return Util.anomaly();
	}
	
	public Term<Ty, En, Sym, Fk, Att, Gen, Sk> replace(Term<Ty, En, Sym, Fk, Att, Gen, Sk> s, Term<Ty, En, Sym, Fk, Att, Gen, Sk> t) {
		return replace(Collections.singletonMap(s, t));
	}


	public String tptp() {
		if (var != null) {
			return var.var.toUpperCase();
		} else if (obj() != null) {
			return obj().toString().toLowerCase();
		} else if (gen() != null) {
			return gen().toString().toLowerCase();
		} else if (sk() != null) {
			return sk().toString().toLowerCase();
		} else if (fk() != null) {
			return fk().toString().toLowerCase() + "(" + arg.tptp() + ")";
		} else if (att() != null) {
			return att().toString().toLowerCase() + "(" + arg.tptp() + ")";
		} else if (sym() != null) {
			if (args.isEmpty()) {
				return sym().toString().toLowerCase();
			}
			List<String> l = args.stream().map(x->x.tptp()).collect(Collectors.toList());
			return sym().toString().toLowerCase() + "(" + Util.sep(l, ",") + ")";
		}
		return Util.anomaly();
	}


	@SuppressWarnings("unchecked")
	public <En> Term<Ty, En, Sym, Fk, Att, Gen, Sk> mapEn() {
		return (Term<Ty, En, Sym, Fk, Att, Gen, Sk>) this;
	}

	
	/////////////////////////////////////////////
	
	@Override
	public Var getVar() {
		return var;
	}

	@Override
	public boolean isVar() {
		return var != null;
	}

	private Set<Var> vars;

	@Override
	public synchronized Set<Var> getVars() {
		if (vars != null) {
			return vars;
		}
		vars = new THashSet<>();
		vars(vars);
		return vars;
	}

	private final Head<Ty, En, Sym, Fk, Att, Gen, Sk> _head;
	@Override
	public Head<Ty, En, Sym, Fk, Att, Gen, Sk> f() {
		return _head;
	}


	@Override
	public List<KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var>> getArgs() {
		return (List<KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, catdata.aql.Var>>) (Object) args();
	}


	@Override
	public KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> substitute(
			Map sigma) {
		return subst(sigma);
	}


	@Override
	public KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> replace(List<Integer> l,
			KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> r) {
		if (isVar()) {
			if (l.isEmpty()) {
				return r;
			}
			throw new RuntimeException("Cannot replace");		
		}
		if (l.isEmpty()) {
			return r;
		}
		Integer x = l.get(0);
		List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> new_args = new ArrayList<>(getArgs().size());
		Iterator<KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var>> it = getArgs().iterator();
		int i = 0;
		while (it.hasNext()) {
			KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> a = it.next(); 
			if (i == x) {
				a = a.replace(l.subList(1, l.size()), r);
			}
			new_args.add((Term<Ty, En, Sym, Fk, Att, Gen, Sk>) a);
			i++;
		}
		return Head(f(), new_args);
	}


	public int hashCode2() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arg == null) ? 0 : arg.hashCode());
		result = prime * result + ((args == null) ? 0 : args.hashCode());
		result = prime * result + ((var == null) ? 0 : var.hashCode());
		result = prime * result + ((_head == null) ? 0 : _head.hashCode());		
		return result;
	}


	public boolean equals2(Object x) {
		if (this == x) {
			return true;
		}
		if (getClass() != x.getClass()) {
			return false;
		}
		Term o = (Term) x;
		
		if (ty() != null) {
			return (o.ty() != null && ty().equals(o.ty()) && obj().equals(o.obj()));
		}
		if (gen() != null) {
			return o.gen() != null && gen().equals(o.gen());
		}
		if (fk() != null) {
			return o.fk() != null && fk().equals(o.fk()) && arg.equals2(o.arg);
		}
		if (att() != null) {
			return o.att() != null && att().equals(o.att()) && arg.equals2(o.arg);
		}
		if (var != null) {
			return o.var != null && var.equals(o.var);
		}
		if (sym() != null) {
			if (!sym().equals(o.sym()) || args.size() != o.args.size()) {
				return false;
			}
			Iterator<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> a = args.iterator();
			Iterator<Term> b = o.args.iterator();
			while (a.hasNext()) {
				if (!a.next().equals2(b.next())) {
					return false;
				}
			}
			return true;
		}
		if (sk() != null) {
			return o.sk() != null && sk().equals(o.sk());
		}
		return Util.anomaly();
	}
	
	

	
}
