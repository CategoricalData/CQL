package catdata.mpl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.google.common.base.Function;

import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.ide.CodeTextPanel;
import catdata.mpl.Mpl.MplExp.MplEval;
import catdata.mpl.Mpl.MplExp.MplSch;
import catdata.mpl.Mpl.MplExp.MplVar;
import catdata.mpl.Mpl.MplTerm.MplAlpha;
import catdata.mpl.Mpl.MplTerm.MplComp;
import catdata.mpl.Mpl.MplTerm.MplConst;
import catdata.mpl.Mpl.MplTerm.MplId;
import catdata.mpl.Mpl.MplTerm.MplLambda;
import catdata.mpl.Mpl.MplTerm.MplPair;
import catdata.mpl.Mpl.MplTerm.MplRho;
import catdata.mpl.Mpl.MplTerm.MplSym;
import catdata.mpl.Mpl.MplTerm.MplTr;
import catdata.mpl.Mpl.MplType.MplBase;
import catdata.mpl.Mpl.MplType.MplProd;
import catdata.mpl.Mpl.MplType.MplUnit;
import catdata.mpl.MplStrict.Node;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;

public class Mpl implements MplObject {

	@Override
	public JComponent display() {
		CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
		JTabbedPane ret = new JTabbedPane();
		ret.add(p, "Text");
		return ret;
	}

	public abstract static class MplType<O> extends Mpl {

		public abstract <R, E> R accept(E env, MplTypeVisitor<O, R, E> v);

		public abstract void type(MplSch<O, ?> ctx);

		public abstract List<O> typeStrict(MplSch<O, ?> ctx);

		public static class MplBase<O> extends MplType<O> {
			final O o;

			public MplBase(O o) {
				this.o = o;
			}

			@Override
			public List<O> typeStrict(MplSch<O, ?> ctx) {
				type(ctx);
				return Collections.singletonList(o);
			}

			@Override
			public int hashCode() {
				return o.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				MplBase<?> other = (MplBase<?>) obj;
				if (o == null) {
					if (other.o != null)
						return false;
				} else if (!o.equals(other.o))
					return false;
				return true;
			}

			@Override
			public void type(MplSch<O, ?> ctx) {
				if (!ctx.sorts.contains(o)) {
					throw new RuntimeException("Undefined sort: " + o);
				}
			}

			@Override
			public <R, E> R accept(E env, MplTypeVisitor<O, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return o.toString();
			}
		}

		static class MplProd<O> extends MplType<O> {
			final MplType<O> l;
			final MplType<O> r;

			public MplProd(MplType<O> l, MplType<O> r) {
				this.l = l;
				this.r = r;
			}

			@Override
			public List<O> typeStrict(MplSch<O, ?> ctx) {
				List<O> ret = new LinkedList<>();
				ret.addAll(l.typeStrict(ctx));
				ret.addAll(r.typeStrict(ctx));
				return ret;
			}

			@Override
			public String toString() {
				return "(" + l + " * " + r + ")";
			}

			@Override
			public int hashCode() {
				int prime = 31;
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
				MplProd<?> other = (MplProd<?>) obj;
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
			public void type(MplSch<O, ?> ctx) {
				l.type(ctx);
				r.type(ctx);
			}

			@Override
			public <R, E> R accept(E env, MplTypeVisitor<O, R, E> v) {
				return v.visit(env, this);
			}

		}

		static class MplUnit<O> extends MplType<O> {

			@Override
			public String toString() {
				return "I";
			}

			@Override
			public int hashCode() {
				return 0;
			}

			@Override
			public List<O> typeStrict(MplSch<O, ?> ctx) {
				return new LinkedList<>();
			}

			@Override
			public boolean equals(Object o) {
				return (o instanceof MplUnit);
			}

			@Override
			public void type(MplSch<O, ?> ctx) {

			}

			@Override
			public <R, E> R accept(E env, MplTypeVisitor<O, R, E> v) {
				return v.visit(env, this);
			}

		}

	}

	abstract static class MplTerm<O, A> extends Mpl {

		public abstract <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v);

		public abstract Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx);

		public abstract Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx);

		protected abstract MplTerm<O, A> forget0();

		public MplTerm<O, A> forget() {
			MplTerm<O, A> ret = this;
			while (true) {
				MplTerm<O, A> ret2 = ret.forget0();
				if (ret.toString().equals(ret2.toString())) {
					return ret2;
				}
				ret = ret2;
			}
		}

		static class MplTr<O, A> extends MplTerm<O, A> {
			final MplTerm<O, A> t;

			public MplTr(MplTerm<O, A> t) {
				this.t = t;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				return new MplTr<>(t.forget0());
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				Pair<MplType<O>, MplType<O>> x = t.type(ctx);
				if (!(x.first instanceof MplProd)) {
					throw new RuntimeException("Dom of " + this + " not of product type");
				}
				if (!(x.second instanceof MplProd)) {
					throw new RuntimeException("Cod of " + this + " not of product type");
				}
				MplProd<O> dom = (MplProd<O>) x.first;
				MplProd<O> cod = (MplProd<O>) x.second;
				if (!dom.r.equals(cod.r)) {
					throw new RuntimeException("Second component of dom and cod do not match: are " + dom.r + " and "
							+ cod.r + " in " + x.first + " -> " + x.second);
				}
				return new Pair<>(dom.l, cod.l);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				Pair<List<O>, List<O>> x = t.typeStrict(ctx);
				if (x.first.size() < 1) {
					throw new RuntimeException("need an input in " + this);
				}
				if (x.second.size() < 1) {
					throw new RuntimeException("need an output in " + this);
				}
				O dom = x.first.get(0);
				O cod = x.second.get(0);
				if (!dom.equals(cod)) {
					throw new RuntimeException("Second component of dom and cod do not match: are " + dom + " and "
							+ cod + " in " + x.first + " -> " + x.second);
				}
				return new Pair<>(x.first.subList(1, x.first.size()), x.second.subList(1, x.second.size()));
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return "tr " + t;
			}

		}

		static class MplConst<O, A> extends MplTerm<O, A> {
			final A a;

			public MplConst(A a) {
				this.a = a;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				return this;
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				return ctx.getSymbol(a);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				return new Pair<>(ctx.getSymbol(a).first.typeStrict(ctx), ctx.getSymbol(a).second.typeStrict(ctx));
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return a.toString();
			}

		}

		static class MplId<O, A> extends MplTerm<O, A> {
			final MplType<O> o;

			public MplId(MplType<O> o) {
				this.o = o;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				return this;
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				o.type(ctx);
				return new Pair<>(o, o);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				return new Pair<>(o.typeStrict(ctx), o.typeStrict(ctx));
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return "id " + o;
			}

		}

		static class MplComp<O, A> extends MplTerm<O, A> {
			final MplTerm<O, A> l;
			final MplTerm<O, A> r;

			public MplComp(MplTerm<O, A> l, MplTerm<O, A> r) {
				this.l = l;
				this.r = r;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				MplTerm<O, A> l0 = l.forget0();
				MplTerm<O, A> r0 = r.forget0();
				if (l0 instanceof MplAlpha) {
					return r0;
				}
				if (r0 instanceof MplAlpha) {
					return l0;
				}
				if (l0 instanceof MplRho) {
					return r0;
				}
				if (r0 instanceof MplRho) {
					return l0;
				}
				return new MplComp<>(l0, r0);
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				Pair<MplType<O>, MplType<O>> p1 = l.type(ctx);
				Pair<MplType<O>, MplType<O>> p2 = r.type(ctx);
				if (!p1.second.equals(p2.first)) {
					throw new RuntimeException("cod=" + p1.second + " dom=" + p2.first + " mismatch on " + this);
				}
				return new Pair<>(p1.first, p2.second);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				Pair<List<O>, List<O>> p1 = l.typeStrict(ctx);
				Pair<List<O>, List<O>> p2 = r.typeStrict(ctx);
				if (!p1.second.equals(p2.first)) {
					throw new RuntimeException("cod=" + p1.second + " dom=" + p2.first + " mismatch on " + this);
				}
				return new Pair<>(p1.first, p2.second);
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return "(" + l + " ; " + r + ")";
			}

		}

		static class MplPair<O, A> extends MplTerm<O, A> {
			final MplTerm<O, A> l;
			final MplTerm<O, A> r;

			public MplPair(MplTerm<O, A> l, MplTerm<O, A> r) {
				this.l = l;
				this.r = r;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				return new MplPair<>(l.forget0(), r.forget0());
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				Pair<MplType<O>, MplType<O>> p1 = l.type(ctx);
				Pair<MplType<O>, MplType<O>> p2 = r.type(ctx);
				return new Pair<>(new MplProd<>(p1.first, p2.first), new MplProd<>(p1.second, p2.second));
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				Pair<List<O>, List<O>> p1 = l.typeStrict(ctx);
				Pair<List<O>, List<O>> p2 = r.typeStrict(ctx);
				List<O> ret1 = new LinkedList<>();
				ret1.addAll(p1.first);
				ret1.addAll(p2.first);
				List<O> ret2 = new LinkedList<>();
				ret2.addAll(p1.second);
				ret2.addAll(p2.second);
				return new Pair<>(ret1, ret2);
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return "(" + l + " * " + r + ")";
			}

		}

		static class MplAlpha<O, A> extends MplTerm<O, A> {
			final MplType<O> a;
			final MplType<O> b;
			final MplType<O> c;
			final boolean leftToRight;

			public MplAlpha(MplType<O> a, MplType<O> b, MplType<O> c, boolean leftToRight) {
				this.a = a;
				this.b = b;
				this.c = c;
				this.leftToRight = leftToRight;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				return this;
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				a.type(ctx);
				b.type(ctx);
				c.type(ctx);
				MplType<O> r = new MplProd<>(a, new MplProd<>(b, c));
				MplType<O> l = new MplProd<>(new MplProd<>(a, b), c);

				return leftToRight ? new Pair<>(l, r) : new Pair<>(r, l);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				throw new RuntimeException();
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return leftToRight ? "alpha1 " + a + " " + b + " " + c : "alpha2 " + a + " " + b + " " + c;
			}

		}

		static class MplSym<O, A> extends MplTerm<O, A> {
			final MplType<O> a;
			final MplType<O> b;

			public MplSym(MplType<O> a, MplType<O> b) {
				this.a = a;
				this.b = b;
			}

			@Override
			protected MplTerm<O, A> forget0() {
				return this;
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				a.type(ctx);
				b.type(ctx);
				MplType<O> l = new MplProd<>(a, b);
				MplType<O> r = new MplProd<>(b, a);
				return new Pair<>(l, r);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				List<O> a0 = a.typeStrict(ctx);
				List<O> b0 = b.typeStrict(ctx);
				List<O> x = new LinkedList<>();
				List<O> y = new LinkedList<>();
				x.addAll(a0);
				x.addAll(b0);
				y.addAll(b0);
				y.addAll(a0);
				return new Pair<>(x, y);
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return "sym " + a + " " + b;
			}

		}

		// ai->a
		static class MplRho<O, A> extends MplTerm<O, A> {
			final MplType<O> a;
			final boolean leftToRight;

			@Override
			protected MplTerm<O, A> forget0() {
				return this;
			}

			public MplRho(MplType<O> a, boolean leftToRight) {
				this.a = a;
				this.leftToRight = leftToRight;
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				a.type(ctx);
				MplType<O> l = new MplProd<>(a, new MplUnit<>());

				return leftToRight ? new Pair<>(l, new MplUnit<>()) : new Pair<>(new MplUnit<>(), l);
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				throw new RuntimeException();
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return leftToRight ? "rho1 " + a : "rho2 " + a;
			}

		}

		// ia->a
		static class MplLambda<O, A> extends MplTerm<O, A> {
			final MplType<O> a;
			final boolean leftToRight;

			@Override
			protected MplTerm<O, A> forget0() {
				return this;
			}

			@Override
			public Pair<List<O>, List<O>> typeStrict(MplSch<O, A> ctx) {
				throw new RuntimeException();
			}

			public MplLambda(MplType<O> a, boolean leftToRight) {
				this.a = a;
				this.leftToRight = leftToRight;
			}

			@Override
			public Pair<MplType<O>, MplType<O>> type(MplSch<O, A> ctx) {
				a.type(ctx);
				MplType<O> l = new MplProd<>(new MplUnit<>(), a);

				return leftToRight ? new Pair<>(l, new MplUnit<>()) : new Pair<>(new MplUnit<>(), l);
			}

			@Override
			public <R, E> R accept(E env, MplTermVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return leftToRight ? "lambda1 " + a : "lambda2 " + a;
			}
		}

	}

	abstract static class MplExp<O, A> extends Mpl {

		public abstract <R, E> R accept(E env, MplExpVisitor<O, A, R, E> v);

		static class MplVar<O, A> extends MplExp<O, A> {
			final String s;

			public MplVar(String s) {
				this.s = s;
			}

			@Override
			public int hashCode() {
				int prime = 31;
				int result = 1;
				result = prime * result + ((s == null) ? 0 : s.hashCode());
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
				MplVar<?, ?> other = (MplVar<?, ?>) obj;
				if (s == null) {
					if (other.s != null)
						return false;
				} else if (!s.equals(other.s))
					return false;
				return true;
			}

			@Override
			public <R, E> R accept(E env, MplExpVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return s;
			}

		}

		static class MplSch<O, A> extends MplExp<O, A> {
			final Set<O> sorts;
			final Map<A, Pair<MplType<O>, MplType<O>>> symbols;
			final Set<Pair<MplTerm<O, A>, MplTerm<O, A>>> eqs;

			public MplSch(Set<O> sorts, Map<A, Pair<MplType<O>, MplType<O>>> symbols,
					Set<Pair<MplTerm<O, A>, MplTerm<O, A>>> eqs) {
				this.sorts = sorts;
				this.symbols = symbols;
				this.eqs = eqs;
				validate();
			}

			public Pair<MplType<O>, MplType<O>> getSymbol(A a) {
				Pair<MplType<O>, MplType<O>> ret = symbols.get(a);
				if (ret == null) {
					throw new RuntimeException("Undefined symbol: " + a);
				}
				return ret;
			}

			@Override
			public <R, E> R accept(E env, MplExpVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			public void validate() {
				for (A a : symbols.keySet()) {
					symbols.get(a).first.type(this);
					symbols.get(a).second.type(this);
				}
				for (Pair<MplTerm<O, A>, MplTerm<O, A>> eq : eqs) {
					eq.first.type(this);
					eq.second.type(this);
				}
			}
		}

		static class MplEval<O, A> extends MplExp<O, A> {
			MplSch<O, A> sch;
			final MplTerm<O, A> a;
			final String sch0;

			public MplEval(String sch0, MplTerm<O, A> a) {
				this.sch0 = sch0;
				this.a = a;
			}

			public void validte(MplSch<O, A> sch) {
				this.sch = sch;
				a.type(sch);
			}

			@Override
			public <R, E> R accept(E env, MplExpVisitor<O, A, R, E> v) {
				return v.visit(env, this);
			}

			@Override
			public String toString() {
				return "eval " + sch + " " + a;
			}

			@Override
			public JComponent display() {

				JTabbedPane p = new JTabbedPane();

				MplStrict<O, A> op = new MplStrict<>(sch);
				Triple<List<Node<O, A>>, List<Node<O, A>>, String> r = a.forget().accept(Unit.unit, op);
				JComponent g = doTermView(Color.green, Color.red, op.g);
				p.addTab("Graph1", g);
				p.addTab("Dot1", new CodeTextPanel("", "digraph foo { " + r.third + " }"));

				/*
				 * MplStrict2<O, A> op2 = new MplStrict2<O,A>(sch);
				 * Pair<MplStrict2.Node<O,A>,String> r2 = a.accept(new Unit(), op2); JComponent
				 * g2 = doTermView2(Color.green, Color.red, op2.g); p.addTab("Graph2", g2);
				 * p.addTab("Dot2", new CodeTextPanel("", "digraph foo { " + r2.second + " }"));
				 */

				return p;

			}
		}

	}

	public interface MplTypeVisitor<O, R, E> {
		R visit(E env, MplBase<O> e);

		R visit(E env, MplProd<O> e);

		R visit(E env, MplUnit<O> e);
	}

	public interface MplTermVisitor<O, A, R, E> {
		R visit(E env, MplConst<O, A> e);

		R visit(E env, MplId<O, A> e);

		R visit(E env, MplComp<O, A> e);

		R visit(E env, MplPair<O, A> e);

		R visit(E env, MplAlpha<O, A> e);

		R visit(E env, MplRho<O, A> e);

		R visit(E env, MplLambda<O, A> e);

		R visit(E env, MplTr<O, A> e);

		R visit(E env, MplSym<O, A> e);
	}

	public interface MplExpVisitor<O, A, R, E> {
		R visit(E env, MplVar<O, A> e);

		R visit(E env, MplSch<O, A> e);

		R visit(E env, MplEval<O, A> e);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <O, A> JComponent doTermView(Color src, Color dst, Graph<Node<O, A>, Integer> sgv) {
		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		Layout layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer vv = new VisualizationViewer<>(layout);
		Function<Node<O, A>, Color> vertexPaint = x -> {
			if (x.isInput) {
				return src;
			}
			return dst;
		};
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<Node<O, A>, String> ttt = arg0 -> {
			String w = arg0.isInput ? "in" : "out";
			return arg0.term + " #" + arg0.which + " " + w;
		};
		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(xx -> "");

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <O, A> JComponent doTermView2(Graph<MplStrict2.Node<O, A>, Integer> sgv) {
		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		Layout layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer vv = new VisualizationViewer<>(layout);

		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		// vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

//		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(xx -> "");

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}
}
