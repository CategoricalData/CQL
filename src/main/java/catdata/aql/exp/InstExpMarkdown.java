package catdata.aql.exp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.node.Visitor;
import org.commonmark.parser.Parser;

import catdata.Chc;
import catdata.Null;
import catdata.Pair;
import catdata.Quad;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.NLP;
import gnu.trove.map.hash.THashMap;

public class InstExpMarkdown extends InstExp<Gen, Null<?>, Gen, Null<?>> {

	private final Map<String, String> options;

	private final String jdbcString;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpMarkdown(String jdbcString, List<Pair<String, String>> options) {
		this.jdbcString = jdbcString;
		this.options = Util.toMapSafely(options);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.jena_reasoner);
	}

	BufferedReader reader; // , err;
	PrintWriter writer;
	Process proc;

	class WordCountVisitor implements Visitor {

		NLP nlp;

		private void doOne(Text x) {

			for (Pair<String, List<Quad<Double, String, String, String>>> y : nlp.main(x.getLiteral())) {
				// emit(parent, pre + "text", Chc.inRight(parent.toString()));
				emit(y, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Chc.inRight(pre2 + "parses"));
				emit(x, pre2 + "child", Chc.inLeft(y));
				emit(y, pre2 + "parent", Chc.inLeft(x));
				emit(y, pre2 + "sentence", Chc.inRight(y.first));

				for (Quad<Double, String, String, String> z : y.second) {
					emit(y, pre2 + "child", Chc.inLeft(z));
					emit(z, pre2 + "parent", Chc.inLeft(y));
					emit(z, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Chc.inRight(pre2 + "parse"));

					emit(z, pre2 + "confidence", Chc.inRight(Double.toString(z.first)));
					emit(z, pre2 + "subject", Chc.inRight(z.second));
					emit(z, pre2 + "verb", Chc.inRight(z.third));
					emit(z, pre2 + "object", Chc.inRight(z.fourth));
				}

			}
		}

		int index = 0;
		String pre = "cql://md/";
		String pre2 = "cql://nlp/";
		public Model model;

		private Map<Object, Resource> map = new THashMap<>();

		public WordCountVisitor() {
			this.model = ModelFactory.createDefaultModel();
			nlp = new NLP();
		}

		private void emit(Object xx, String y, Chc<Object, String> z) {
			if (!map.containsKey(xx)) {
				map.put(xx, ResourceFactory.createResource());
			}
			Resource s = map.get(xx);
			Property p = ResourceFactory.createProperty(y);
			RDFNode o;
			if (z.left) {
				if (!map.containsKey(z.l)) {
					map.put(z.l, ResourceFactory.createResource());
				}
				o = map.get(z.l);
			} else {
				o = ResourceFactory.createStringLiteral(z.r);
			}
			Statement st = ResourceFactory.createStatement(s, p, o);
			model.add(st);
		}

		@Override
		public void visit(BlockQuote arg0) {
			visitChildren(arg0);
		}

		@Override
		public void visit(BulletList arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(Code arg0) {
			// Node
			visitChildren(arg0);
		}

		@Override
		public void visit(Document arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(Emphasis arg0) {
			//
			visitChildren(arg0);
		}

		@Override
		public void visit(FencedCodeBlock arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(HardLineBreak arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(Heading arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(ThematicBreak arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(HtmlInline arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(HtmlBlock arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(Image arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(IndentedCodeBlock arg0) {

			visitChildren(arg0);
		}

		@Override
		public void visit(Link arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(ListItem arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(OrderedList arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(Paragraph arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(SoftLineBreak arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(StrongEmphasis arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(Text x) {
			emit(x, pre + "text", Chc.inRight(x.getLiteral()));
			doOne(x);

			visitChildren(x);
		}

		@Override
		public void visit(LinkReferenceDefinition arg0) {
			// node
			visitChildren(arg0);
		}

		@Override
		public void visit(CustomBlock arg0) {
			// block
			visitChildren(arg0);
		}

		@Override
		public void visit(CustomNode arg0) {
			// node
			visitChildren(arg0);
		}

		protected void visitChildren(Node parent) {
			emit(parent, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
					Chc.inRight(pre + parent.getClass().getSimpleName()));
			// emit(parent, pre + "text", Chc.inRight(parent.toString()));

			Node node = parent.getFirstChild();
			while (node != null) {
				Node next = node.getNext();
				emit(parent, pre + "child", Chc.inLeft(node));
				emit(node, pre + "parent", Chc.inLeft(parent));
				node.accept(this);
				node = next;
			}
		}

	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		AqlOptions ops = new AqlOptions(options, env.defaults);

		Parser parser = Parser.builder().build();
		try {
			Node node = parser.parse(Util.readFile(new FileReader(jdbcString)));
			WordCountVisitor visitor = new WordCountVisitor();
			node.accept(visitor);
			File f = File.createTempFile("cqltemp", ".xml");
			visitor.model.write(new FileOutputStream(f));
			return new InstExpRdfAll(f.getAbsolutePath(), Collections.emptyList()).eval(env, isC);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_md ").append(Util.quote(jdbcString));
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
		}
		return sb.toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptySet();
	}

	@Override
	public SchExp type(AqlTyping G) {
		return new SchExp.SchExpInst<>(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		InstExpMarkdown other = (InstExpMarkdown) obj;
		if (jdbcString == null) {
			if (other.jdbcString != null)
				return false;
		} else if (!jdbcString.equals(other.jdbcString))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

}
