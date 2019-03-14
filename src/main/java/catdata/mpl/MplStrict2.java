package catdata.mpl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import catdata.Pair;
import catdata.Unit;
import catdata.mpl.Mpl.MplExp.MplSch;
import catdata.mpl.Mpl.MplTerm;
import catdata.mpl.Mpl.MplTerm.MplAlpha;
import catdata.mpl.Mpl.MplTerm.MplComp;
import catdata.mpl.Mpl.MplTerm.MplConst;
import catdata.mpl.Mpl.MplTerm.MplId;
import catdata.mpl.Mpl.MplTerm.MplLambda;
import catdata.mpl.Mpl.MplTerm.MplPair;
import catdata.mpl.Mpl.MplTerm.MplRho;
import catdata.mpl.Mpl.MplTerm.MplSym;
import catdata.mpl.Mpl.MplTerm.MplTr;
import catdata.mpl.Mpl.MplTermVisitor;
import catdata.mpl.Mpl.MplType.MplBase;
import catdata.mpl.Mpl.MplType.MplProd;
import catdata.mpl.Mpl.MplType.MplUnit;
import catdata.mpl.Mpl.MplTypeVisitor;
import catdata.mpl.MplStrict2.Node;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

public class MplStrict2<O, A>
		implements MplTypeVisitor<O, List<O>, Unit>, MplTermVisitor<O, A, Pair<Node<O, A>, String>, Unit> {

	static class Node<O, A> {

		final MplTerm<O, A> term;

		static int global_id = 0;
		final int id;

		public Node(MplTerm<O, A> term) {
			this.term = term;
			id = global_id++;
		}

		@Override
		public String toString() {
			return "v" + id; // + " [label=\"" + term + " " + which + " \"]";
		}

		public String label() {
			// String str = isInput ? "in" : "out";
			return toString() + " [label=\"" + term + " \"]; ";
			// return term;
		}
	}

	private int counter = 0;

	private Integer fresh() {
		return counter++;
	}

	// private final MplSch<O, A> ctx;
	public MplStrict2(@SuppressWarnings("unused") MplSch<O, A> ctx) {
		// this.ctx = ctx;
	}

	private final Graph<Node<O, A>, Integer> g = new DirectedSparseMultigraph<>();

	@Override
	public List<O> visit(Unit env, MplBase<O> e) {
		return Collections.singletonList(e.o);
	}

	@Override
	public List<O> visit(Unit env, MplProd<O> e) {
		List<O> ret = new LinkedList<>();
		ret.addAll(e.l.accept(env, this));
		ret.addAll(e.r.accept(env, this));
		return ret;
	}

	@Override
	public List<O> visit(Unit env, MplUnit<O> e) {
		return new LinkedList<>();
	}

	////////////////

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplConst<O, A> e) {
		String ret = "subgraph cluster" + fresh() + "{ label=\"" + e + "\"; ";

		Node<O, A> n = new Node<>(e);
		g.addVertex(n);

		ret += n.label();

		return new Pair<>(n, ret + " }");
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplId<O, A> e) {
		return makeId(e);
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplComp<O, A> e) {

		Pair<Node<O, A>, String> l = e.l.accept(env, this);
		Pair<Node<O, A>, String> r = e.r.accept(env, this);

		String ret = "subgraph cluster" + fresh() + " { label=\"" + e + "\"; " + l.first + " " + r.first;

		Node<O, A> n = new Node<>(e);
		g.addVertex(n);
		ret += n.label();

		g.addEdge(fresh(), n, l.first);
		ret += n + " -> " + l.first + ";";

		g.addEdge(fresh(), r.first, n);
		ret += r.first + " -> " + n + ";";

		return new Pair<>(n, ret + " }");
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplPair<O, A> e) {

		Pair<Node<O, A>, String> l = e.l.accept(env, this);
		Pair<Node<O, A>, String> r = e.r.accept(env, this);

		String ret = "subgraph cluster" + fresh() + "{ label=\"" + e + "\"; " + l.first + " " + r.first;

		Node<O, A> n = new Node<>(e);
		g.addVertex(n);
		ret += n.label();

		g.addEdge(fresh(), n, l.first);
		ret += n + " -> " + l.first + " ;";

		g.addEdge(fresh(), l.first, n);
		ret += l.first + " -> " + n + " ;";

		g.addEdge(fresh(), n, r.first);
		ret += n + " -> " + r.first + " ;";

		g.addEdge(fresh(), r.first, n);
		ret += r.first + " -> " + n + " ;";

		return new Pair<>(n, ret + " }");
	}

	private Pair<Node<O, A>, String> makeId(MplTerm<O, A> e) {

		String ret = "subgraph cluster" + fresh() + "{ label=\"" + e + "\"; ";

		Node<O, A> n = new Node<>(e);
		ret += n.label();
		g.addVertex(n);

		return new Pair<>(n, ret + " }");
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplAlpha<O, A> e) {
		return makeId(e);
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplRho<O, A> e) {
		return makeId(e);
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplLambda<O, A> e) {
		return makeId(e);
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplTr<O, A> e) {
		Pair<Node<O, A>, String> xs = e.t.accept(env, this);

//		List<O> ft = e.t.type(ctx).first.accept(env, this);
//		List<O> et = e.type(ctx).first.accept(env, this);

		String ret = "subgraph cluster" + fresh() + "{ label=\"" + e + "\"; " + xs.first;

		Node<O, A> n = new Node<>(e);
		ret += n.label();
		g.addVertex(n);

		g.addEdge(fresh(), n, xs.first);
		ret += n + " -> " + xs.first + ";";

		g.addEdge(fresh(), xs.first, n);
		ret += xs.first + " -> " + n + ";";

		g.addEdge(fresh(), xs.first, xs.first);
		ret += xs.first + " -> " + xs.first + ";";

		return new Pair<>(n, ret + " }");
	}

	@Override
	public Pair<Node<O, A>, String> visit(Unit env, MplSym<O, A> e) {
		throw new RuntimeException();
	}

	///////////////

}
