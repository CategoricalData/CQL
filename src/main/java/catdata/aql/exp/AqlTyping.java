package catdata.aql.exp;

import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.list.TreeList;

import catdata.LineException;
import catdata.Pair;
import catdata.Program;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;

public class AqlTyping {

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		if (!defs.gs.isEmpty()) {
			sb.append("graph " + Util.sep(defs.gs.keySet(), "\ngraph ") + "\n");
		}
		if (!defs.tys.isEmpty()) {
			sb.append("typeside " + Util.sep(defs.tys.keySet(), "\ntypeside ") + "\n");
		}
		if (!defs.schs.isEmpty()) {
			sb.append("schema " + Util.sep(defs.schs, " : ", "\nschema ") + "\n");
		}
		if (!defs.eds.isEmpty()) {
			sb.append("constraints " + Util.sep(defs.eds, " : ", "\nconstraints ") + "\n");
		}
		if (!defs.scs.isEmpty()) {
			sb.append("schema_colimit " + Util.sep(defs.scs, " : ", "\nschema_colimit ") + "\n");
		}
		if (!defs.maps.isEmpty()) {
			sb.append("mapping " + Util.sep(defs.maps, " : ", "\nmapping ", x -> x.first + " -> " + x.second) + "\n");
		}
		if (!defs.qs.isEmpty()) {
			sb.append("query " + Util.sep(defs.qs, " : ", "\nquery ", x -> x.first + " -> " + x.second) + "\n");
		}
		if (!defs.insts.isEmpty()) {
			sb.append("instance " + Util.sep(defs.insts, " : ", "\ninstance ") + "\n");
		}
		if (!defs.trans.isEmpty()) {
			sb.append("transform " + Util.sep(defs.trans, " : ", "\ntransform ", x -> x.first + " -> " + x.second)
					+ "\n");
		}
		if (!defs.ps.isEmpty()) {
			sb.append("command " + Util.sep(defs.ps.keySet(), "\ncommand ") + "\n");
		}
		if (!defs.tms.isEmpty()) {
			sb.append("theory_morphism " + Util.sep(defs.tms.keySet(), "\ntheory_morphism ") + "\n");
		}

		return sb.toString();
	}

	public boolean eq(TyExp t1, TyExp t2) {
		return t1.resolve(prog).equals(t2.resolve(prog));
	}

	public boolean eq(SchExp s1, SchExp s2) {
		return s1.resolve(this, prog).equals(s2.resolve(this, prog));
	}

	public final Program<Exp<?>> prog;

	public AqlTyping() {
		prog = new Program<>(new TreeList<>(), "");
	}

	public AqlTyping(Program<Exp<?>> p) {
		prog = p;
	}

	public AqlTyping(Program<Exp<?>> prog, boolean continue0) {
		this.prog = prog;
		for (String s : prog.order) {
			try {
				Exp<?> e = prog.exps.get(s);
				for (Entry<String, String> k : e.options().entrySet()) {
					if (!e.allowedOptions().contains(AqlOption.valueOf(k.getKey()))) {
						throw new RuntimeException("Option not allowed: " + k.getKey() + ".  Allowed: "
								+ Util.sep(e.allowedOptions(), ", ") + ". Class: " + e.getClass());
					}
				}
				switch (e.kind()) {
				case INSTANCE:
					defs.insts.put(s, ((InstExp<?, ?, ?, ?>) e).type(this));
					continue;
				case MAPPING:
					Pair<? extends SchExp, ? extends SchExp> p = ((MapExp) e).type(this);
					defs.maps.put(s, new Pair<>(p.first, p.second));
					continue;
				case THEORY_MORPHISM:
					Pair<? extends TyExp, ? extends TyExp> tp = ((MorExp) e).type(this);
					defs.tms.put(s, new Pair<>(tp.first, tp.second));
					continue;
				case PRAGMA:
					continue;
				case QUERY:
					p = ((QueryExp) e).type(this);
					defs.qs.put(s, new Pair<>(p.first, p.second));
					continue;
				case SCHEMA:
					TyExp qq = ((SchExp) e).type(this);
					defs.schs.put(s, qq);
					continue;
				case TRANSFORM:
					Pair<? extends InstExp<?, ?, ?, ?>, ? extends InstExp<?, ?, ?, ?>> q = ((TransExp<?, ?, ?, ?, ?, ?, ?, ?>) e)
							.type(this);
					defs.trans.put(s, new Pair<>(q.first, q.second));
					continue;
				case TYPESIDE:
					Unit tt = (Unit) ((TyExp) e).type(this);
					defs.tys.put(s, tt);
					continue;
				case GRAPH:
					defs.gs.put(s, ((GraphExp) e).type(this));
					continue;
				case COMMENT:
					continue;
				case SCHEMA_COLIMIT:
					defs.scs.put(s, ((ColimSchExp) e).type(this));
					continue;
				case CONSTRAINTS:
					defs.eds.put(s, ((EdsExp) e).type(this));
					continue;
				default:
					throw new RuntimeException("Anomaly: please report");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				if (!continue0) {
					throw new LineException(ex.getMessage(), s, prog.exps.get(s).kind().toString());
				}
			}
		}
	}

	public final KindCtx<String, Unit, Unit, TyExp, SchExp, Pair<InstExp<?, ?, ?, ?>, InstExp<?, ?, ?, ?>>, Pair<SchExp, SchExp>, Pair<SchExp, SchExp>, Unit, Unit, Set<String>, SchExp> defs = new KindCtx<>();

}
