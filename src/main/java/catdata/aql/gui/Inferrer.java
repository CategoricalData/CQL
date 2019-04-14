package catdata.aql.gui;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.exp.CombinatorParser;
import gnu.trove.set.hash.THashSet;

class Inferrer {

	@SuppressWarnings("unchecked")
	public static void infer(AqlCodeEditor editor, Kind k) {
		if (editor.last_env == null) {
			throw new RuntimeException("Must compile before using inference");
		}
		String in = editor.topArea.getSelectedText();
		String repl = " {\n";

		if (k.equals(Kind.MAPPING) || k.equals(Kind.QUERY) || k.equals(Kind.TRANSFORM)) {
			Pair<String, String> s = CombinatorParser.parseInfer(in);
			String a = s.first;
			String b = s.second;
			switch (k) {
			case MAPPING:
				repl += inferMapping(editor.last_env.defs.schs.get(a), editor.last_env.defs.schs.get(b));
				break;
			case QUERY:
				repl += inferQuery(editor.last_env.defs.schs.get(a), editor.last_env.defs.schs.get(b));
				break;
			case TRANSFORM:
				repl += inferTransform(editor.last_env.defs.insts.get(a), editor.last_env.defs.insts.get(b));
				break;
			case GRAPH:
			case INSTANCE:
			case PRAGMA:
			case SCHEMA:
			case TYPESIDE:
			case COMMENT:
			case SCHEMA_COLIMIT:
			case THEORY_MORPHISM:
			case CONSTRAINTS:
			default:
				break;
			}
		} else if (k.equals(Kind.INSTANCE)) {
			String a = CombinatorParser.parseInfer1(in);
			repl += inferInstance(editor.last_env.defs.schs.get(a));
		}
		repl += "\n}";
		editor.topArea.insert(repl, editor.topArea.getSelectionEnd());
	}

	private static <Ty, En, Sym, Fk, Att> String inferInstance(Schema<Ty, En, Sym, Fk, Att> a) {
		if (a == null) {
			throw new RuntimeException("Compiled schema not found - try compiling before using inference.");
		}
		String ens = "\tgenerators\n" + Util.sep(Util.alphabetical(a.ens).stream()
				.map(z -> "\t\t[list Generators here] : " + z).collect(Collectors.toList()), "\n");
		String eqs = "\tmulti_equations\n" + Util.sep(Util.alphabetical(a.fks.keySet()).stream().map(
				z -> "\t\t" + z + " -> [list assignments here] // " + a.fks.get(z).first + " -> " + a.fks.get(z).second)
				.collect(Collectors.toList()), "\n");
		String eqs2 = "\t\n" + Util
				.sep(Util.alphabetical(a.atts.keySet()).stream().map(z -> "\t\t" + z + " -> [list assignments here] // "
						+ a.atts.get(z).first + " -> " + a.atts.get(z).second).collect(Collectors.toList()), "\n");

		return ens + "\n" + eqs + eqs2;
	}

	@SuppressWarnings("unchecked")
	private static <X> String pr(Collection<X>... cs) {
		Collection<X> ret = new THashSet<>();
		for (Collection<X> col : cs) {
			ret.addAll(col);
		}
		return Util.sep(Util.alphabetical(ret), " ");
	}

	private static <X, Y, Z> String pr3(Map<X, Pair<Y, Z>> cs) {
		Collection<String> ret = new THashSet<>();
		for (X x : cs.keySet()) {
			ret.add(x + ":" + cs.get(x).first + "->" + cs.get(x).second);
		}
		return Util.sep(Util.alphabetical(ret), " ");
	}

	private static <X, Y, Z> String pr4(Map<X, Pair<List<Y>, Z>> cs) {
		Collection<String> ret = new THashSet<>();
		for (X x : cs.keySet()) {
			ret.add(x + ":" + Util.sep(cs.get(x).first, ",") + "->" + cs.get(x).second);
		}
		return Util.sep(Util.alphabetical(ret), " ");
	}

	@SuppressWarnings("unchecked")
	private static <X> String pr2(String sep, Collection<X>... cs) {
		Collection<X> ret = new THashSet<>();
		for (Collection<X> col : cs) {
			ret.addAll(col);
		}
		return Util.sep(Util.alphabetical(ret), sep);
	}

	@SuppressWarnings("unchecked")
	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0, Gen, Sk, Gen0, Sk0> String inferTransform(
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, ?, ?> a, Instance<Ty0, En0, Sym0, Fk0, Att0, Gen0, Sk0, ?, ?> b) {
		if (a == null || b == null) {
			throw new RuntimeException("Compiled instances(s) not found - try compiling before using inference.");
		}
		if (!a.schema().equals(b.schema())) {
			throw new RuntimeException("Source instance is on schema " + a.schema()
					+ "\n\nand target instance is on schema " + b.schema());
		}

		String sks = Util.sep(
				a.sks().keySet().stream()
						.map(z -> "\t\t" + z + " -> some type side symbols [" + pr(b.schema().typeSide.syms.keySet())
								+ "] applied to Attributes [" + pr(b.schema().atts.keySet())
								+ "] applied to paths of foreign keys [" + pr(b.schema().fks.keySet())
								+ "] applied to Generators [" + pr(b.gens().keySet()) + "] and labelled nulls ["
								+ pr(b.sks().keySet()) + "]  ending at " + a.sks().get(z))
						.collect(Collectors.toList()),
				"\n");

		String gens = Util.sep(Util.alphabetical(a.gens().keySet()).stream()
				.map(z -> "\t\t" + z + " -> a generator [" + pr(a.gens().keySet()) + "] ." + " a path of foreign keys ["
						+ pr(b.schema().fks.keySet()) + "] ending at " + a.gens().get(z))
				.collect(Collectors.toList()), "\n");

		return "\tgenerators\n" + gens + "\n" + sks;

	}

	@SuppressWarnings("unchecked")
	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0> String inferMapping(Schema<Ty, En, Sym, Fk, Att> a,
			Schema<Ty0, En0, Sym0, Fk0, Att0> b) {
		if (a == null || b == null) {
			throw new RuntimeException("Compiled schema(s) not found - try compiling before using inference.");
		}
		String ens = "\tentities\n" + Util.sep(Util.alphabetical(a.ens).stream()
				.map(z -> "\t\t" + z + " -> an entity [" + pr(b.ens) + "]").collect(Collectors.toList()), "\n");
		String fks = "\tforeign_keys\n" + Util.sep(
				Util.alphabetical(a.fks.keySet()).stream()
						.map(z -> "\t\t" + z + " -> a path of foreign keys [" + pr(b.fks.keySet()) + "] // "
								+ a.fks.get(z).first + " -> " + a.fks.get(z).second)
						.collect(Collectors.toList()),
				"\n");
		String atts = "\tattributes\n" + Util.sep(Util.alphabetical(a.atts.keySet()).stream()
				.map(z -> "\t\t" + z + " -> lambda v. some type side symbols [" + pr(b.typeSide.syms.keySet())
						+ "] applied to Attributes [" + pr(b.atts.keySet()) + "] "
						+ "applied to paths of foreign keys [" + pr(b.fks.keySet()) + "] ending on variable [v] "
						+ " // " + a.atts.get(z).first + " -> " + a.atts.get(z).second + " ")
				.collect(Collectors.toList()), "\n");
		return ens + "\n" + fks + "\n" + atts;
	}

	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0> String inferQuery(Schema<Ty, En, Sym, Fk, Att> a,
			Schema<Ty0, En0, Sym0, Fk0, Att0> b) {
		if (a == null || b == null) {
			throw new RuntimeException("Compiled schema(s) not found - try compiling before using inference.");
		}
		@SuppressWarnings("unchecked")
		String ens = "\tentities //source entities: " + pr(a.ens) + "\n" + Util.sep(Util.alphabetical(b.ens).stream()
				.map(z -> "\t\t" + z + " -> " + inferBlock(z, a, b)).collect(Collectors.toList()), "\n");
		String fks = "\tforeign_keys\n" + Util
				.sep(Util.alphabetical(b.fks.keySet()).stream().map(z -> "\t\t" + z + " [:" + b.fks.get(z).first + " ->"
						+ b.fks.get(z).second + "] -> " + inferTrans(z, a, b)).collect(Collectors.toList()), "\n");
		return ens + "\n\n" + fks;

	}

	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0> List<String> varsColon2(En0 en,
			Schema<Ty, En, Sym, Fk, Att> a, @SuppressWarnings("unused") Schema<Ty0, En0, Sym0, Fk0, Att0> b) {
		return a.ens.stream().map(x -> "v_" + en + "_" + x + "[:" + x + "]").collect(Collectors.toList());
	}

	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0> List<String> varsColon(En0 en,
			Schema<Ty, En, Sym, Fk, Att> a, @SuppressWarnings("unused") Schema<Ty0, En0, Sym0, Fk0, Att0> b) {
		return a.ens.stream().map(x -> "v_" + en + "_" + x + ":" + x).collect(Collectors.toList());
	}
	/*
	 * private static <Ty, En, Sym, Fk, Att,Ty0, En0, Sym0, Fk0, Att0> List<String>
	 * vars(En0 en, Schema<Ty, En, Sym, Fk, Att> a, Schema<Ty0, En0, Sym0, Fk0,
	 * Att0> b) { return a.ens.stream().map(x -> "v_" + en + "_" +
	 * x).collect(Collectors.toList()); }
	 */

	@SuppressWarnings("unchecked")
	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0> String inferTrans(Fk0 fk,
			Schema<Ty, En, Sym, Fk, Att> a, Schema<Ty0, En0, Sym0, Fk0, Att0> b) {
		List<String> dom = varsColon2(b.fks.get(fk).second, a, b);
		List<String> cod = varsColon(b.fks.get(fk).first, a, b);

		String gens = Util.sep(Util.alphabetical(dom).stream()
				.map(z -> z + " -> a generator [" + pr(cod) + "] ." + " a path of foreign keys [" + pr3(a.fks) + "]")
				.collect(Collectors.toList()), "\n\t\t\t\t");
		return "{" + gens + "}";

	}

	@SuppressWarnings("unchecked")
	private static <Ty, En, Sym, Fk, Att, Ty0, En0, Sym0, Fk0, Att0> String inferBlock(En0 en,
			Schema<Ty, En, Sym, Fk, Att> a, Schema<Ty0, En0, Sym0, Fk0, Att0> b) {
		String s = "some type side symbols [" + pr4(b.typeSide.syms) + "]\n\t\t\t\t\tapplied to Attributes ["
				+ pr3(b.atts) + "]\n\t\t\t\t\tapplied to paths of foreign keys [" + pr3(a.fks)
				+ "]\n\t\t\t\t\tending on variables [" + pr(varsColon(en, a, b)) + "]";

		List<String> as = Util.alphabetical(b.attsFrom(en)).stream()
				.map(x -> x + " [:" + b.atts.get(x).second + "] -> " + s).collect(Collectors.toList());

		return "{from\n\t\t\t\t" + pr2("\n\t\t\t\t", varsColon(en, a, b))
				+ "\n\t\t\twhere\n\t\t\t\tequalities of terms using " + s + "\n\t\t\treturn\n\t\t\t\t"
				+ Util.sep(as, "\n\t\t\t\t") + "}";
	}

}
