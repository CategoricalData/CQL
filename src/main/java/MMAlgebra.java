import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;

/**
 * 
 * @author math
 *
 *         Op,Optional,Product,and List-preserving MM-algebras as induced by the
 *         CFG.
 */
public interface MMAlgebra<obj, value, typ, ctype, dtype, sep, lst, rec, inst, poly> {

	public obj typeObj(typ t);

	public obj valueObj(value v);

	public value boolValue(boolean b);

	public value intValue(int b);

	public value strValue(String b);

	public value polyValue(poly p);

	public typ ctypeType(ctype c);

	public typ dtypeType(dtype d);

	public ctype boolType();

	public ctype polyType();

	public ctype anonType();

	public ctype intType();

	public ctype strType();

	public dtype dtype(ctype c, Optional<ctype> oc, List<inst> is);

	public sep sepSemi();

	public sep sepComma();

	public sep sepBar();

	public lst lst(obj o, List<Pair<sep, obj>> sos);

	public rec rec(obj o, List<Triple<sep, obj, obj>> soos);

	public inst inst(Op o, List<obj> os);

	public poly lstPoly(lst l);

	public poly recPoly(rec r);

	public poly instPoly(inst i);

	public enum Op {
		AOp, AddOp, AndOp, AsOp, CombineOp, CountOp, EqOp, ErrorOp, ExplainOp, FoldOp, FromOp, GetOp, GivenOp,
		GrpCountOp, GtOp, GteOp, HeadOp, IdOp, IsOp, LastOp, Lt, Lte, MapOp, MergeOp, MultOp, NegOp, NoOpOp, OneOp,
		OrOp, PathOp, PlusOp, PowOp, PutOp, QOp, RepeatOp, SplitOp, StartOp, TailOp, ToOp, TraceOp, TypeOp, ZeroOp,
		BranchOp
	}

	/**
	 * @author math
	 *
	 * A homomorphism of MMAlgebras.  Should respect various equations not captured here.
	 */
	public interface MMAlgebraMorphism<obj, value, typ, ctype, dtype, sep, lst, rec, inst, poly, obj2, value2, typ2, ctype2, dtype2, sep2, lst2, rec2, inst2, poly2> {

		public obj2 objM(obj o);

		public value2 valueM(value o);

		public typ2 typM(typ o);

		public ctype2 ctypeM(ctype o);

		public dtype2 dtypeM(dtype o);

		public sep2 sepM(sep o);

		public lst2 lstM(lst o);

		public rec2 recM(rec o);

		public inst2 instM(inst o);

		public poly2 polyM(poly o);

	}

	/**
	 * 
	 * @author math
	 *
	 * A free MM-algebra, using abstract syntax trees, with fold implementation.
	 */
	static class FreeMMAlgebra implements
			MMAlgebra<FreeMMAlgebra.Obj, FreeMMAlgebra.Value, FreeMMAlgebra.Typ, FreeMMAlgebra.Ctype, FreeMMAlgebra.Dtype, FreeMMAlgebra.Sep, FreeMMAlgebra.Lst, FreeMMAlgebra.Rec, FreeMMAlgebra.Inst, FreeMMAlgebra.Poly> {

		public <obj, value, typ, ctype, dtype, sep, lst, rec, inst, poly> MMAlgebraMorphism<FreeMMAlgebra.Obj, FreeMMAlgebra.Value, FreeMMAlgebra.Typ, FreeMMAlgebra.Ctype, FreeMMAlgebra.Dtype, FreeMMAlgebra.Sep, FreeMMAlgebra.Lst, FreeMMAlgebra.Rec, FreeMMAlgebra.Inst, FreeMMAlgebra.Poly, obj, value, typ, ctype, dtype, sep, lst, rec, inst, poly> fold(
				MMAlgebra<obj, value, typ, ctype, dtype, sep, lst, rec, inst, poly> target) {

			return new MMAlgebraMorphism<FreeMMAlgebra.Obj, FreeMMAlgebra.Value, FreeMMAlgebra.Typ, FreeMMAlgebra.Ctype, FreeMMAlgebra.Dtype, FreeMMAlgebra.Sep, FreeMMAlgebra.Lst, FreeMMAlgebra.Rec, FreeMMAlgebra.Inst, FreeMMAlgebra.Poly, obj, value, typ, ctype, dtype, sep, lst, rec, inst, poly>() {

				@Override
				public obj objM(Obj o) {
					if (o.v.left) {
						return target.typeObj(typM(o.v.l));
					}
					return target.valueObj(valueM(o.v.r));
				}

				@Override
				public value valueM(Value o) {
					if (o.v.left) {
						return target.boolValue(o.v.l);
					} else if (o.v.r.left) {
						return target.intValue(o.v.r.l);
					} else if (o.v.r.r.left) {
						return target.strValue(o.v.r.r.l);
					}
					return target.polyValue(polyM(o.v.r.r.r));
				}

				@Override
				public typ typM(Typ o) {
					if (o.v.left) {
						return target.ctypeType(ctypeM(o.v.l));
					}
					return target.dtypeType(dtypeM(o.v.r));
				}

				@Override
				public ctype ctypeM(Ctype o) {
					switch (o) {
					case ANON:
						return target.anonType();
					case BOOL:
						return target.boolType();
					case INT:
						return target.intType();
					case POLY:
						return target.polyType();
					case STR:
						return target.strType();
					}
					throw new RuntimeException();
				}

				@Override
				public dtype dtypeM(Dtype o) {
					return target.dtype(ctypeM(o.v.first), o.v.second.first.map(this::ctypeM),
							o.v.second.second.stream().map(this::instM).collect(Collectors.toList()));
				}

				@Override
				public sep sepM(Sep o) {
					switch (o) {
					case BAR:
						return target.sepBar();
					case COMMA:
						return target.sepComma();
					case SEMI:
						return target.sepSemi();
					}
					throw new RuntimeException();
				}

				@Override
				public lst lstM(Lst o) {
					return target.lst(objM(o.v.first), o.v.second.stream()
							.map(x -> new Pair<>(sepM(x.first), objM(x.second))).collect(Collectors.toList()));
				}

				@Override
				public rec recM(Rec o) {
					return target.rec(objM(o.v.first),
							o.v.second.stream().map(x -> new Triple<>(sepM(x.first), objM(x.second), objM(x.third)))
									.collect(Collectors.toList()));
				}

				@Override
				public inst instM(Inst o) {
					return target.inst(o.v.first, o.v.second.stream().map(this::objM).collect(Collectors.toList()));
				}

				@Override
				public poly polyM(Poly o) {
					if (o.v.left) {
						return target.lstPoly(lstM(o.v.l));
					} else if (o.v.r.left) {
						return target.recPoly(recM(o.v.r.l));
					}
					return target.instPoly(instM(o.v.r.r));

				}

			};

		}

		static class Obj {
			public final Chc<Typ, Value> v;

			public Obj(Chc<Typ, Value> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Obj other = (Obj) obj;
				return v.equals(other.v);
			}

		}

		static class Value {
			public final Chc<Boolean, Chc<Integer, Chc<String, Poly>>> v;

			public Value(Chc<Boolean, Chc<Integer, Chc<String, Poly>>> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Value other = (Value) obj;
				return v.equals(other.v);

			}

		}

		static class Typ {
			public final Chc<Ctype, Dtype> v;

			public Typ(Chc<Ctype, Dtype> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Typ other = (Typ) obj;
				return v.equals(other.v);

			}

		}

		static enum Ctype {
			BOOL, POLY, ANON, INT, STR
		}

		static enum Sep {
			SEMI, COMMA, BAR
		}

		static class Poly {
			public final Chc<Lst, Chc<Rec, Inst>> v;

			public Poly(Chc<Lst, Chc<Rec, Inst>> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Poly other = (Poly) obj;
				return v.equals(other.v);
			}

		}

		static class Lst {
			public final Pair<Obj, List<Pair<Sep, Obj>>> v;

			public Lst(Pair<Obj, List<Pair<Sep, Obj>>> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Lst other = (Lst) obj;
				return v.equals(other.v);
			}

		}

		static class Rec {
			public final Pair<Obj, List<Triple<Sep, Obj, Obj>>> v;

			public Rec(Pair<Obj, List<Triple<Sep, Obj, Obj>>> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Rec other = (Rec) obj;
				return v.equals(other.v);
			}

		}

		static class Inst {
			public final Pair<Op, List<Obj>> v;

			public Inst(Pair<Op, List<Obj>> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Inst other = (Inst) obj;
				return v.equals(other.v);
			}

		}

		static class Dtype {
			public final Pair<Ctype, Pair<Optional<Ctype>, List<Inst>>> v;

			public Dtype(Pair<Ctype, Pair<Optional<Ctype>, List<Inst>>> v) {
				this.v = v;
			}

			@Override
			public int hashCode() {
				return v.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Dtype other = (Dtype) obj;
				if (v == null) {
					if (other.v != null)
						return false;
				} else if (!v.equals(other.v))
					return false;
				return true;
			}

		}

		@Override
		public Obj typeObj(Typ t) {
			return new Obj(Chc.inLeft(t));
		}

		@Override
		public Obj valueObj(Value v) {
			return new Obj(Chc.inRight(v));
		}

		@Override
		public Value boolValue(boolean b) {
			return new Value(Chc.inLeft(b));
		}

		@Override
		public Value intValue(int b) {
			return new Value(Chc.inRight(Chc.inLeft(b)));
		}

		@Override
		public Value strValue(String b) {
			return new Value(Chc.inRight(Chc.inRight(Chc.inLeft(b))));
		}

		@Override
		public Value polyValue(Poly b) {
			return new Value(Chc.inRight(Chc.inRight(Chc.inRight(b))));
		}

		@Override
		public Typ ctypeType(Ctype c) {
			return new Typ(Chc.inLeft(c));
		}

		@Override
		public Typ dtypeType(Dtype d) {
			return new Typ(Chc.inRight(d));
		}

		@Override
		public Ctype boolType() {
			return Ctype.BOOL;
		}

		@Override
		public Ctype polyType() {
			return Ctype.POLY;
		}

		@Override
		public Ctype anonType() {
			return Ctype.ANON;
		}

		@Override
		public Ctype intType() {
			return Ctype.INT;
		}

		@Override
		public Ctype strType() {
			return Ctype.STR;
		}

		@Override
		public Sep sepSemi() {
			return Sep.SEMI;
		}

		@Override
		public Sep sepComma() {
			return Sep.COMMA;
		}

		@Override
		public Sep sepBar() {
			return Sep.BAR;
		}

		@Override
		public Lst lst(Obj o, List<Pair<Sep, Obj>> sos) {
			return new Lst(new Pair<>(o, sos));
		}

		@Override
		public Rec rec(Obj o, List<Triple<Sep, Obj, Obj>> soos) {
			return new Rec(new Pair<>(o, soos));
		}

		@Override
		public Inst inst(Op o, List<Obj> os) {
			return new Inst(new Pair<>(o, os));
		}

		@Override
		public Dtype dtype(Ctype c, Optional<Ctype> oc, List<Inst> is) {
			return new Dtype(new Pair<>(c, new Pair<>(oc, is)));
		}

		@Override
		public Poly lstPoly(Lst l) {
			return new Poly(Chc.inLeft(l));
		}

		@Override
		public Poly recPoly(Rec r) {
			return new Poly(Chc.inRight(Chc.inLeft(r)));
		}

		@Override
		public Poly instPoly(Inst i) {
			return new Poly(Chc.inRight(Chc.inRight(i)));
		}

	}
	
	
}
