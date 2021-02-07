package catdata.aql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class AqlJs<Ty, Sym> {

	private static final String postfix = ""; //\n\nPossibly helpful info: 32-bit Java integers cannot exceed 2 billion; if you need larger numbers please use strings \n\nPossibly useful link: http://docs.oracle.com/javase/8/docs/api/ .";

	private final Map<Ty, String> iso1;
	private final Map<Sym, String> iso2;

	private final Map<Sym, Pair<List<Ty>, Ty>> syms;
	public final Map<Ty, String> java_tys;
	public final Map<Ty, String> java_parsers;
	public final Map<Sym, String> java_fns;

	private final ExternalCodeUtils external;

	private final String languageId, languageName;

	public AqlJs(String languageId, Map<Sym, Pair<List<Ty>, Ty>> syms, Map<Ty, String> java_tys,
			Map<Ty, String> java_parsers, Map<Sym, String> java_fns) {
		this.languageId = languageId;
		this.syms = syms;
		this.java_fns = java_fns;
		this.java_parsers = java_parsers;
		this.java_tys = java_tys;
		Object last = "";

		if (java_tys.isEmpty()) {
			iso1 = null;
			iso2 = null;
			external = null;
			languageName = null;
			return;
		}

		iso1 = new THashMap<>(java_parsers.size());
		iso2 = new THashMap<>(java_fns.size());

		external = new ExternalCodeUtils();
		languageName = external.getLanguageName(languageId)
				.orElseThrow(() -> new RuntimeException("Graal language not installed: " + languageId));

		try {
			for (Map.Entry<Ty, String> entry : java_parsers.entrySet()) {
				iso1.put(entry.getKey(), entry.getValue());
				last = entry.getKey();
			}
			for (Map.Entry<Sym, String> entry : java_fns.entrySet()) {
				iso2.put(entry.getKey(), entry.getValue());
				last = entry.getKey();
			}
		} catch (Throwable e) {
			throw new RuntimeException("In " + languageName + " execution, " + e.getMessage() + postfix
					+ "\n\nlast binding evaluated: " + last);
		}
	}

	public AqlJs(AqlOptions ops, Map<Sym, Pair<List<Ty>, Ty>> syms, Map<Ty, String> java_tys,
			Map<Ty, String> java_parsers, Map<Sym, String> java_fns) {
		this((String) ops.getOrDefault(AqlOptions.AqlOption.graal_language), syms, java_tys, java_parsers, java_fns);
	}

	public synchronized Object invoke(Ty ty, String source, Object... args) {
		return external.invoke(languageId, Util.load(java_tys.get(ty)), source, args);
	}

	private synchronized Object apply(Sym name, List<Object> args) {
		try {
			// TODO aql check inputs and outputs here?
			return invoke(syms.get(name).second, iso2.get(name), args.toArray());
		} catch (Throwable e) {
			throw new RuntimeException("In " + languageName + " execution of " + name + " on arguments " + args + ", "
					+ Util.sep(args.stream().map(x -> x.getClass()).collect(Collectors.toList()), ",") + " , "
					+ e.getClass() + " error: " + e.getMessage() + postfix, e);
		}
	}

	public synchronized Object parse(Ty ty, String toParse) {
		String parser = iso1.get(ty);
		if (parser == null) {
			throw new RuntimeException(
					"In " + languageName + " execution of " + toParse + " no javascript definition for " + ty);
		}
		try {
			return invoke(ty, parser, toParse);
		} catch (Throwable e) {
			throw new RuntimeException("In " + languageName + " execution of " + toParse + " cannot convert to " + ty
					+ " error: " + e.getMessage() + postfix
					+ "\n\nPossible fix: check the external_parsers of the typeside for type conversion errors.");
		}
	}

	public synchronized <En, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> reduce(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (java_tys.isEmpty()) {
			return term;
		}
		while (true) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> next = reduce1(term);
			if (next.equals(term)) {
				return next;
			}
			term = next;
		}
	}

	private synchronized <En, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> reduce1(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (term.var != null || term.gen() != null || term.sk() != null || term.obj() != null) {
			return term;
		}

		Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg = null;
		if (term.arg != null) {
			arg = reduce1(term.arg);
		}

		if (term.fk() != null) {
			return Term.Fk(term.fk(), arg);
		} else if (term.att() != null) {
			return Term.Att(term.att(), arg);
		} else if (term.args != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args = (new ArrayList<>(term.args.size()));
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : term.args) {
				args.add(reduce1(x));
			}
			if (!java_fns.containsKey(term.sym())) {
				return Term.Sym(term.sym(), args);
			}
			List<Object> unwrapped_args = (new ArrayList<>(args.size()));
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> t : args) {
				if (t.obj() != null) {
					unwrapped_args.add(t.obj());
				}
			}
			if (unwrapped_args.size() != args.size()) {
				return Term.Sym(term.sym(), args);
			}
			Object result = apply(term.sym(), unwrapped_args);
			Ty ty = syms.get(term.sym()).second;
			return Term.Obj(result, ty);
		}
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((java_fns == null) ? 0 : java_fns.hashCode());
		result = prime * result + ((java_parsers == null) ? 0 : java_parsers.hashCode());
		result = prime * result + ((java_tys == null) ? 0 : java_tys.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		AqlJs<?, ?> other = (AqlJs<?, ?>) obj;
		if (java_fns == null) {
			if (other.java_fns != null)
				return false;
		} else if (!java_fns.equals(other.java_fns))
			return false;
		if (java_parsers == null) {
			if (other.java_parsers != null)
				return false;
		} else if (!java_parsers.equals(other.java_parsers))
			return false;
		if (java_tys == null) {
			if (other.java_tys != null)
				return false;
		} else if (!java_tys.equals(other.java_tys))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AqlJs [java_tys=" + java_tys + ", java_parsers=" + java_parsers + ", java_fns=" + java_fns + "]";
	}
}
