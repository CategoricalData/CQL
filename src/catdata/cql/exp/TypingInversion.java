package catdata.cql.exp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import catdata.LocStr;
import catdata.Pair;
import catdata.Unit;
import catdata.apg.exp.ApgInstExp;
import catdata.apg.exp.ApgInstExp.ApgInstExpCoEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpDelta;
import catdata.apg.exp.ApgInstExp.ApgInstExpEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpInitial;
import catdata.apg.exp.ApgInstExp.ApgInstExpPlus;
import catdata.apg.exp.ApgInstExp.ApgInstExpRaw;
import catdata.apg.exp.ApgInstExp.ApgInstExpTerminal;
import catdata.apg.exp.ApgInstExp.ApgInstExpTimes;
import catdata.apg.exp.ApgInstExp.ApgInstExpVar;
import catdata.apg.exp.ApgMapExp;
import catdata.apg.exp.ApgMapExp.ApgMapExpRaw;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp;
import catdata.apg.exp.ApgSchExp.ApgSchExpInitial;
import catdata.apg.exp.ApgSchExp.ApgSchExpPlus;
import catdata.apg.exp.ApgSchExp.ApgSchExpRaw;
import catdata.apg.exp.ApgSchExp.ApgSchExpTerminal;
import catdata.apg.exp.ApgSchExp.ApgSchExpTimes;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTransExp;
import catdata.apg.exp.ApgTransExp.ApgTransExpCase;
import catdata.apg.exp.ApgTransExp.ApgTransExpCoEqualize;
import catdata.apg.exp.ApgTransExp.ApgTransExpCoEqualizeU;
import catdata.apg.exp.ApgTransExp.ApgTransExpCompose;
import catdata.apg.exp.ApgTransExp.ApgTransExpDelta;
import catdata.apg.exp.ApgTransExp.ApgTransExpEqualize;
import catdata.apg.exp.ApgTransExp.ApgTransExpEqualizeU;
import catdata.apg.exp.ApgTransExp.ApgTransExpFst;
import catdata.apg.exp.ApgTransExp.ApgTransExpId;
import catdata.apg.exp.ApgTransExp.ApgTransExpInl;
import catdata.apg.exp.ApgTransExp.ApgTransExpInr;
import catdata.apg.exp.ApgTransExp.ApgTransExpPair;
import catdata.apg.exp.ApgTransExp.ApgTransExpRaw;
import catdata.apg.exp.ApgTransExp.ApgTransExpSnd;
import catdata.apg.exp.ApgTransExp.ApgTransExpVar;
import catdata.apg.exp.ApgTyExp;
import catdata.apg.exp.ApgTyExp.ApgTyExpRaw;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.exp.ColimSchExp.ColimSchExpVar;
import catdata.cql.exp.EdsExp.EdsExpSch;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.GraphExp.GraphExpLiteral;
import catdata.cql.exp.GraphExp.GraphExpRaw;
import catdata.cql.exp.GraphExp.GraphExpVar;
import catdata.cql.exp.InstExp.InstExpLit;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.MapExp.MapExpLit;
import catdata.cql.exp.MapExp.MapExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVar;
import catdata.cql.exp.QueryExp.QueryExpLit;
import catdata.cql.exp.QueryExp.QueryExpVar;
import catdata.cql.exp.SchExp.SchExpLit;
import catdata.cql.exp.SchExp.SchExpVar;
import catdata.cql.exp.TransExp.TransExpLit;
import catdata.cql.exp.TransExp.TransExpVar;
import catdata.cql.exp.TyExp.TyExpLit;
import catdata.cql.exp.TyExp.TyExpVar;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class TypingInversion implements ExpCoVisitor<AqlTyping, Unit, RuntimeException> {

	@Override
	public TyExpSch visitTyExpSch(Unit params, AqlTyping exp) {
		SchExpVar e = new SchExpVar("s");
		TyExpVar x = new TyExpVar("t");
		TyExpSch t = new TyExpSch(e);
		exp.defs.schs.put("s", x);
		exp.defs.tys.put("t", Unit.unit);
		return t;
	}

	@Override
	public TyExpEmpty visitTyExpEmpty(Unit params, AqlTyping exp) {
		return new TyExpEmpty();
	}

	@Override
	public TyExpLit visitTyExpLit(Unit params, AqlTyping exp) {
		return null;
	}

	@Override
	public TyExpVar visitTyExpVar(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar e = new TyExpVar("t");
		return e;
	}

	@Override
	public TyExpRaw visitTyExpRaw(Unit params, AqlTyping exp) {
		return new TyExpRaw(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
	}

	@Override
	public TyExpSql visitTyExpSql(Unit params, AqlTyping exp) {
		return new TyExpSql();
	}

	@Override
	public TyExpRdf visitTyExpRdf(Unit params, AqlTyping exp) {
		return new TyExpRdf();
	}

	@Override
	public SchExpCod visitSchExpCod(Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new SchExpCod(new QueryExpVar("q"));
	}

	@Override
	public SchExpDom visitSchExpDom(Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new SchExpDom(new QueryExpVar("q"));
	}

	@Override
	public SchExpDst visitSchExpDst(Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.maps.put("m", new Pair(s1, s2));
		return new SchExpDst(new MapExpVar("m"));
	}

	@Override
	public SchExpSrc visitSchExpSrc(Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.maps.put("m", new Pair(s1, s2));
		return new SchExpSrc(new MapExpVar("m"));
	}

	@Override
	public SchExpEmpty visitSchExpEmpty(Unit params, AqlTyping exp) {
		TyExp t = new TyExpVar("t");
		exp.defs.tys.put("t", Unit.unit);
		return new SchExpEmpty(t);
	}

	@Override
	public SchExpInst visitSchExpInst(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		exp.prog.exps.put("i", i);
		return new SchExpInst(i);

	}

	@Override
	public SchExpLit visitSchExpLit(Unit params, AqlTyping exp) {
		return null;
//    return new SchExpLit(Schema.terminal(TypeSide.initial()));
	}

	@Override
	public SchExpPivot visitSchExpPivot(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp x = new TyExpVar("t");
		InstExp q = new InstExpVar("i");
		exp.defs.schs.put("s", x);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new SchExpPivot(q, new LinkedList());
	}

	@Override
	public SchExpVar visitSchExpVar(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar e = new TyExpVar("t");
		exp.defs.schs.put("s", e);
		return new SchExpVar("s");
	}

	@Override
	public SchExpRaw visitSchExpRaw(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar e = new TyExpVar("t");
		return new SchExpRaw(e, new LinkedList(), new LinkedList(), new LinkedList(), new LinkedList(),
				new LinkedList(), new LinkedList(), new LinkedList(), new LinkedList());
	}

	@Override
	public SchExpColim visitSchExpColim(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		ColimSchExp x = new ColimSchExpVar("sc");
		exp.defs.scs.put("sc", new THashSet());
		ColimSchExpQuotient q = new ColimSchExpQuotient(t, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		exp.prog.exps.put("sc", q);
		return new SchExpColim(x);

	}

	@Override
	public MapExpComp visitMapExpComp(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.schs.put("s3", t);

		SchExpVar s1 = new SchExpVar("s1");
		SchExpVar s2 = new SchExpVar("s2");
		SchExpVar s3 = new SchExpVar("s3");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("s3", new SchExpEmpty(t));

		MapExpVar m12 = new MapExpVar("m12");
		MapExpVar m23 = new MapExpVar("m23");

		exp.defs.maps.put("m12", new Pair(s1, s2));
		exp.defs.maps.put("m23", new Pair(s2, s3));

		return new MapExpComp(m12, m23);
	}

	@Override
	public MapExpId visitMapExpId(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");

		exp.defs.schs.put("s", t);
		SchExpVar s = new SchExpVar("s");

		return new MapExpId(s);
	}

	@Override
	public QueryExpVar visitQueryExpVar(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new QueryExpVar("q");
	}

	@Override
	public QueryExpRaw visitQueryExpRaw(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		// exp.defs.qs.put("q", new Pair(s1, s2));
		return new QueryExpRaw(Collections.emptyList(), Collections.emptyList(), s1, s2, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> MapExpPivot<Gen, Sk, X, Y> visitMapExpPivot(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp x = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", x);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
//    String l = new SchExpPivot(q, new LinkedList());
		return new MapExpPivot(i, new LinkedList());
	}

	@Override
	public MapExpVar visitMapExpVar(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		exp.defs.maps.put("m", new Pair(s1, s2));
		return new MapExpVar("m");
	}

	@Override
	public MapExpRaw visitMapExpRaw(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
//    exp.defs.maps.put("m", new Pair(s1, s2));
		return new MapExpRaw(s1, s2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public MapExpColim visitMapExpColim(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.scs.put("sc", new THashSet());
		exp.defs.schs.put("s", t);
		ColimSchExpQuotient q = new ColimSchExpQuotient(t, Collections.singletonList(new LocStr(0, "s")),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		exp.prog.exps.put("sc", q);
		return new MapExpColim("s", q);
	}

	@Override
	public QueryExpCompose visitQueryExpCompose(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.schs.put("s3", t);

		SchExpVar s1 = new SchExpVar("s1");
		SchExpVar s2 = new SchExpVar("s2");
		SchExpVar s3 = new SchExpVar("s3");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("s3", new SchExpEmpty(t));

		QueryExpVar m12 = new QueryExpVar("q12");
		QueryExpVar m23 = new QueryExpVar("q23");

		exp.defs.qs.put("q12", new Pair(s1, s2));
		exp.defs.qs.put("q23", new Pair(s2, s3));

		return new QueryExpCompose(m12, m23, Collections.emptyList());
	}

	@Override
	public QueryExpId visitQueryExpId(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");

		exp.defs.schs.put("s", t);
		SchExpVar s = new SchExpVar("s");

		return new QueryExpId(s);
	}

	@Override
	public QueryExpLit visitQueryExpLit(Unit params, AqlTyping exp) {
		return null;
	}

	@Override
	public QueryExpDeltaCoEval visitQueryExpDeltaCoEval(Unit params, AqlTyping exp) {

		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);

		SchExpVar s1 = new SchExpVar("s1");
		SchExpVar s2 = new SchExpVar("s2");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));

		MapExp m = new MapExpVar("m");

		exp.defs.maps.put("m", new Pair(s1, s2));

		return new QueryExpDeltaCoEval(m, Collections.emptyList());
	}

	@Override
	public QueryExpDeltaEval visitQueryExpDeltaEval(Unit params, AqlTyping exp) {

		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);

		SchExpVar s1 = new SchExpVar("s1");
		SchExpVar s2 = new SchExpVar("s2");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));

		MapExpVar m = new MapExpVar("m");

		exp.defs.maps.put("m", new Pair(s1, s2));

		return new QueryExpDeltaEval(m, Collections.emptyList());

	}

	@Override
	public QueryExpRawSimple visitQueryExpRawSimple(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.schs.put("s", t);
		SchExp s1 = new SchExpVar("s");
		return new QueryExpRawSimple(s1);
	}

	@Override
	public EdsExpVar visitEdsExpVar(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		SchExpVar s = new SchExpVar("s");

		exp.defs.schs.put("s", t);
		exp.defs.eds.put("c", s);

		return new EdsExpVar("c");
	}

	@Override
	public EdsExpRaw visitEdsExpRaw(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		SchExpVar s = new SchExpVar("s");
		exp.defs.schs.put("s", t);

		return new EdsExpRaw(s, Collections.emptyList(), Collections.emptyList(), Unit.unit);
	}

	@Override
	public GraphExpRaw visitGraphExpRaw(Unit params, AqlTyping exp) {
		return new GraphExpRaw(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public GraphExpVar visitGraphExpVar(Unit params, AqlTyping exp) {
		exp.defs.gs.put("g", Unit.unit);
		return new GraphExpVar("g");
	}

	@Override
	public GraphExpLiteral visitGraphExpLiteral(Unit params, AqlTyping exp) {
		return null;
	}

	@Override
	public <X, Y> PragmaExpConsistent<X, Y> visitPragmaExpConsistent(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new PragmaExpConsistent(i);
	}

	@Override
	public <X, Y> PragmaExpCheck<X, Y> visitPragmaExpCheck(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		EdsExp c = new EdsExpVar("c");
		exp.defs.eds.put("c", s);
		return new PragmaExpCheck(i, c, Collections.emptyList());
	}

	@Override
	public PragmaExpMatch visitPragmaExpMatch(Unit params, AqlTyping exp) {
		exp.defs.gs.put("g1", Unit.unit);
		exp.defs.gs.put("g2", Unit.unit);
		GraphExp g1 = new GraphExpVar("g1");
		GraphExp g2 = new GraphExpVar("g2");
		return new PragmaExpMatch("method", g1, g2, Collections.emptyList());
	}

	@Override
	public PragmaExpSql visitPragmaExpSql(Unit params, AqlTyping exp) {
		return new PragmaExpSql("jdbc_string", Collections.emptyList(), new LinkedList());
	}

	@Override
	public <X, Y> PragmaExpToCsvInst<X, Y> visitPragmaExpToCsvInst(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new PragmaExpToCsvInst(i, "directory", new LinkedList());
	}

	@Override
	public PragmaExpVar visitPragmaExpVar(Unit params, AqlTyping exp) {
		exp.defs.ps.put("p", Unit.unit);
		PragmaExpVar e = new PragmaExpVar("p");
		return e;
	}

	@Override
	public PragmaExpJs visitPragmaExpJs(Unit params, AqlTyping exp) {
		return new PragmaExpJs(Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public PragmaExpProc visitPragmaExpProc(Unit params, AqlTyping exp) {
		return new PragmaExpProc(Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public <X, Y> PragmaExpToJdbcInst<X, Y> visitPragmaExpToJdbcInst(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new PragmaExpToJdbcInst(i, "jdbc_string", "prefix", new LinkedList());

	}

	@Override
	public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> PragmaExpToJdbcTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> visitPragmaExpToJdbcTrans(
			Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<Object, Object, Object, Object> i1 = new InstExpVar("i1");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		InstExp<Object, Object, Object, Object> i2 = new InstExpVar("i2");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i1, i2));
		return new PragmaExpToJdbcTrans(h, "jdbc_string", "prefix", new LinkedList(), new LinkedList());
	}

	@Override
	public PragmaExpToJdbcQuery visitPragmaExpToJdbcQuery(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		exp.defs.qs.put("q", new Pair(s1, s2));
		QueryExpVar q = new QueryExpVar("q");
		return new PragmaExpToJdbcQuery(q, "jdbc_string", "prefix_src", "prefix_dst", new LinkedList());
	}

	@Override
	public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> visitPragmaExpToCsvTrans(
			Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<Object, Object, Object, Object> i1 = new InstExpVar("i1");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		InstExp<Object, Object, Object, Object> i2 = new InstExpVar("i2");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i1, i2));
		return new PragmaExpToCsvTrans(h, "directory", new LinkedList(), new LinkedList());
	}

	@Override
	public PragmaExpCheck2 visitPragmaExpCheck2(Unit params, AqlTyping exp) {
		TyExpVar t = new TyExpVar("t");

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		exp.defs.qs.put("q", new Pair(s1, s2));
		QueryExp q = new QueryExpVar("q");

		exp.defs.eds.put("c1", s1);
		exp.defs.eds.put("c2", s2);

		EdsExp c1 = new EdsExpVar("c1");
		EdsExp c2 = new EdsExpVar("c2");

		return new PragmaExpCheck2(q, c1, c2);
	}

	@Override
	public ColimSchExpQuotient visitColimSchExpQuotient(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		ColimSchExpQuotient q = new ColimSchExpQuotient(t, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		return q;
	}

	@Override
	public ColimSchExpRaw visitColimSchExpRaw(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.defs.gs.put("g", Unit.unit);
		GraphExp g = new GraphExpVar("g");
		return new ColimSchExpRaw(g, t, new LinkedList(), new LinkedList(), new LinkedList());
	}

	@Override
	public ColimSchExpVar visitColimSchExpVar(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		ColimSchExpVar x = new ColimSchExpVar("sc");
		exp.defs.scs.put("sc", new THashSet());
		ColimSchExpQuotient q = new ColimSchExpQuotient(t, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		exp.prog.exps.put("sc", q);
		return x;
	}

	@Override
	public ColimSchExpWrap visitColimSchExpWrap(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		ColimSchExpVar x = new ColimSchExpVar("sc");
		exp.defs.scs.put("sc", new THashSet());
		SchExp s = new SchExpVar("s");
		SchExp ss = new SchExpColim(x);

		ColimSchExpQuotient q = new ColimSchExpQuotient(t, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		exp.prog.exps.put("sc", q);

		MapExp m1 = new MapExpVar("m1");
		MapExp m2 = new MapExpVar("m2");
		exp.defs.maps.put("m1", new Pair(ss, s));
		exp.defs.maps.put("m2", new Pair(s, ss));

		return new ColimSchExpWrap(x, m1, m2);
	}

	@Override
	public ColimSchExpModify visitColimSchExpModify(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		ColimSchExpVar x = new ColimSchExpVar("sc");
		exp.defs.scs.put("sc", new THashSet());

		ColimSchExpQuotient q = new ColimSchExpQuotient(t, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		exp.prog.exps.put("sc", q);

		return new ColimSchExpModify(x, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpSigma<Gen, Sk, X, Y> visitInstExpSigma(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.insts.put("i", s1);
		MapExpVar m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s1, s2));
		return new InstExpSigma(m, i, Collections.emptyMap());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpSigmaChase<Gen, Sk, X, Y> visitInstExpSigmaChase(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.defs.insts.put("i", s1);
		MapExpVar m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s1, s2));
		return new InstExpSigmaChase(m, i, Collections.emptyMap());
	}

	@Override
	public InstExpVar visitInstExpVar(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExpVar i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return i;
	}

	@Override
	public <Gen, Sk, X, Y> InstExpAnonymize<Gen, Sk, X, Y> visitInstExpAnonymize(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<Object, Object, Object, Object> i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new InstExpAnonymize(i);
	}

	@Override
	public <Gen, Sk, X, Y> InstExpChase<Gen, Sk, X, Y> visitInstExpChase(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		exp.defs.eds.put("c", s);
		exp.prog.exps.put("s", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		EdsExpVar c = new EdsExpVar("c");
		return new InstExpChase(c, i, Collections.emptyList());
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> InstExpCoEq<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitInstExpCoEq(
			Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		TransExpVar h1 = new TransExpVar("h1");
		exp.defs.trans.put("h1", new Pair(i1, i2));
		TransExpVar h2 = new TransExpVar("h2");
		exp.defs.trans.put("h2", new Pair(i1, i2));

		return new InstExpCoEq(h1, h2, Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpColim<Gen, Sk, X, Y> visitInstExpColim(Unit param, AqlTyping exp) {
		GraphExpVar g = new GraphExpVar("g");
		SchExp s = new SchExpVar("s");
		exp.defs.tys.put("t", Unit.unit);
		return new InstExpColim(g, s, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpCoProdFull<Gen, Sk, X, Y> visitInstExpCoProdFull(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		exp.prog.exps.put("s", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		List<String> l = new LinkedList();
		l.add("i1");
		l.add("i2");
		return new InstExpCoProdFull(l, s, Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y, Gen1, Sk1, X1> InstExpDiff<Gen, Sk, X, Y, Gen1, Sk1, X1> visitInstExpDiff(Unit param,
			AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		return new InstExpDiff(i1, i2);
	}

	@Override
	public <Gen, Sk, X, Y> InstExpDistinct<Gen, Sk, X, Y> visitInstExpDistinct(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new InstExpDistinct(i);
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> InstExpDom<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitInstExpDom(
			Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		TransExpVar h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i1, i2));
		return new InstExpDom(h);
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> InstExpCod<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitInstExpCod(
			Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		TransExpVar h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i1, i2));
		return new InstExpCod(h);
	}

	@Override
	public InstExpEmpty visitInstExpEmpty(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		return new InstExpEmpty(s);
	}

	@Override
	public <Gen, Sk, X, Y> InstExpEval<Gen, Sk, X, Y> visitInstExpEval(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		QueryExpVar q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new InstExpEval(q, i, Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpCoEval<Gen, Sk, X, Y> visitInstExpCoEval(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.insts.put("i", s2);
		QueryExpVar q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new InstExpCoEval(q, i, Collections.emptyList());
	}

	@Override
	public InstExpFrozen visitInstExpFrozen(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());

		QueryExpVar q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new InstExpFrozen(q, "an_entity_or_type");
	}

	@Override
	public <Gen, Sk, X, Y> InstExpLit<Gen, Sk, X, Y> visitInstExpLit(Unit param, AqlTyping exp) {
		return null;
	}

	@Override
	public <Gen, Sk, X, Y> InstExpPivot<Gen, Sk, X, Y> visitInstExpPivot(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new InstExpPivot(i, Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpPi<Gen, Sk, X, Y> visitInstExpPi(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		MapExpVar m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s1, s2));
		return new InstExpPi(m, i, Collections.emptyMap());
	}

	@Override
	public InstExpCsv visitInstExpCsv(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		return new InstExpCsv(s, new LinkedList(), new LinkedList(), "directory");
	}

	@Override
	public <Gen, Sk, X, Y> InstExpDelta<Gen, Sk, X, Y> visitInstExpDelta(Unit param, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());

		MapExpVar m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s2, s1));
		return new InstExpDelta(m, i);
	}

	@Override
	public InstExpJdbc visitInstExpJdbc(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpSql();
		exp.defs.schs.put("s", t);
		return new InstExpJdbc(s, new LinkedList(), "jdbc_string", new LinkedList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpQueryQuotient<Gen, Sk, X, Y> visitInstExpQueryQuotient(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new InstExpQueryQuotient(i, Collections.emptyList(), Collections.emptyList());

	}

	@Override
	public InstExpRandom visitInstExpRandom(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		return new InstExpRandom(s, new LinkedList(), new LinkedList());
	}

	@Override
	public InstExpRaw visitInstExpRaw(Unit param, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		return new InstExpRaw(s, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpEval(
			Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp j = new InstExpVar("j");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		QueryExp q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s1, s2));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.defs.insts.put("j", s1);
		InstExp i = new InstExpVar("i");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i, j));
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));
		exp.prog.exps.put("j", new InstExpEmpty(s2));

		return new TransExpEval(q, h, Collections.emptyList());
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpCoEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpCoEval(
			Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp j = new InstExpVar("j");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		QueryExp q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s2, s1));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.defs.insts.put("j", s1);
		InstExp i = new InstExpVar("i");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i, j));
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));
		exp.prog.exps.put("j", new InstExpEmpty(s2));

		return new TransExpCoEval(q, h, new LinkedList(), new LinkedList());
	}

	@Override
	public <Gen, Sk, X, Y> TransExpCoEvalEvalCoUnit<Gen, Sk, X, Y> visitTransExpCoEvalEvalCoUnit(Unit params,
			AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		QueryExp q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s1, s2));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		InstExp i = new InstExpVar("i");
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));

		return new TransExpCoEvalEvalCoUnit(q, i, new THashMap());
	}

	@Override
	public <Gen, Sk, X, Y> TransExpCoEvalEvalUnit<Gen, Sk, X, Y> visitTransExpCoEvalEvalUnit(Unit params,
			AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		QueryExp q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s2, s1));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		InstExp i = new InstExpVar("i");
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));

		return new TransExpCoEvalEvalUnit(q, i, new THashMap());
	}

	@Override
	public <Gen, Sk, X, Y> TransExpSigmaDeltaCounit<Gen, Sk, X, Y> visitTransExpSigmaDeltaCounit(Unit params,
			AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		MapExp m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s1, s2));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		InstExp i = new InstExpVar("i");
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));

		return new TransExpSigmaDeltaCounit(m, i, new THashMap());
	}

	@Override
	public <Gen, Sk, X, Y> TransExpSigmaDeltaUnit<Gen, Sk, X, Y> visitTransExpSigmaDeltaUnit(Unit params,
			AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		MapExp m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s2, s1));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		InstExp i = new InstExpVar("i");
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));

		return new TransExpSigmaDeltaUnit(m, i, new THashMap());
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> TransExpCompose<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> visitTransExpCompose(
			Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s", t);
		SchExpVar s = new SchExpVar("s");

		InstExpVar i1 = new InstExpVar("i1");
		InstExpVar i2 = new InstExpVar("i2");
		InstExpVar i3 = new InstExpVar("i3");

		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		exp.defs.insts.put("i3", s);

		exp.prog.exps.put("i1", new InstExpEmpty(s));
		exp.prog.exps.put("i2", new InstExpEmpty(s));
		exp.prog.exps.put("i3", new InstExpEmpty(s));
		exp.prog.exps.put("s", new SchExpEmpty(t));

		TransExpVar h12 = new TransExpVar("h12");
		TransExpVar h23 = new TransExpVar("h23");

		exp.defs.trans.put("h12", new Pair(i1, i2));
		exp.defs.trans.put("h23", new Pair(i2, i3));

		return new TransExpCompose(h12, h23);
	}

	@Override
	public <Gen, Sk, X, Y> TransExpId<Gen, Sk, X, Y> visitTransExpId(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.insts.put("i", s);
		return new TransExpId(i);
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpDelta<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpDelta(
			Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp j = new InstExpVar("j");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		MapExp m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s2, s1));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.defs.insts.put("j", s1);
		InstExp i = new InstExpVar("i");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i, j));
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));
		exp.prog.exps.put("j", new InstExpEmpty(s2));

		return new TransExpDelta(m, h);
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpSigma<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpSigma(
			Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp j = new InstExpVar("j");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		MapExp m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s1, s2));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.defs.insts.put("j", s1);
		InstExp i = new InstExpVar("i");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i, j));
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));
		exp.prog.exps.put("j", new InstExpEmpty(s2));

		return new TransExpSigma(m, h, new THashMap(), new THashMap());
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpPi<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpPi(
			Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp j = new InstExpVar("j");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		MapExp m = new MapExpVar("m");
		exp.defs.maps.put("m", new Pair(s1, s2));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.defs.insts.put("j", s1);
		InstExp i = new InstExpVar("i");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i, j));
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));
		exp.prog.exps.put("j", new InstExpEmpty(s2));

		return new TransExpPi(m, h, new THashMap(), new THashMap());
	}

	@Override
	public TransExpRaw visitTransExpRaw(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		exp.defs.trans.put("h", new Pair(i1, i2));
		exp.prog.exps.put("s", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		return new TransExpRaw(i1, i2, new LinkedList(), new LinkedList(), new LinkedList());
	}

	@Override
	public TransExpVar visitTransExpVar(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		TransExpVar h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i1, i2));
		exp.prog.exps.put("s", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		return h;

	}

	@Override
	public <X1, Y1, X2, Y2> TransExpCsv<X1, Y1, X2, Y2> visitTransExpCsv(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		exp.defs.trans.put("h", new Pair(i1, i2));
		exp.prog.exps.put("s", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		return new TransExpCsv(i1, i2, new LinkedList(), new LinkedList());
	}

	@Override
	public <X1, Y1, X2, Y2> TransExpJdbc<X1, Y1, X2, Y2> visitTransExpJdbc(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		exp.defs.trans.put("h", new Pair(i1, i2));
		exp.prog.exps.put("s", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		return new TransExpJdbc("jdbc_string", i1, i2, new LinkedList(), new LinkedList());
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpDistinct<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpDistinct(
			Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		InstExp j = new InstExpVar("j");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.defs.insts.put("j", s1);
		InstExp i = new InstExpVar("i");
		TransExp<Object, Object, Object, Object, Object, Object, Object, Object> h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i, j));
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));
		exp.prog.exps.put("j", new InstExpEmpty(s2));

		return new TransExpDistinct(h);
	}

	@Override
	public <Gen, Sk, X, Y> TransExpDistinct2<Gen, Sk, X, Y> visitTransExpDistinct2(Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("i", new InstExpEmpty(s1));

		return new TransExpDistinct2(i);
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpLit<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpLit(
			Unit params, AqlTyping exp) {
		return null;
	}

	@Override
	public MapExpLit visitMapExpLit(Unit params, AqlTyping exp) {
		return null;
	}

	@Override
	public CommentExp visitCommentExp(Unit params, AqlTyping exp) {
		return new CommentExp("hello world", false);
	}

	@Override
	public <Gen, Sk, X, Y> TransExpDiffReturn<Gen, Sk, X, Y> visitTransExpDiffReturn(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		return new TransExpDiffReturn(i1, i2);
	}

	@Override
	public <Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> TransExpDiff<Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> visitTransExpDiff(
			Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		InstExp i1 = new InstExpVar("i1");
		InstExp i2 = new InstExpVar("i2");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		exp.defs.insts.put("i1", s);
		exp.defs.insts.put("i2", s);
		TransExp h = new TransExpVar("h");
		exp.defs.trans.put("h", new Pair(i1, i2));
		return new TransExpDiff(i, h);
	}

	@Override
	public TransExpFrozen visitTransExpFrozen(Unit params, AqlTyping exp) {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());

		QueryExpVar q = new QueryExpVar("q");
		exp.defs.qs.put("q", new Pair(s1, s2));
		return new TransExpFrozen(q, "var", "entity", new RawTerm("term", Collections.emptyList()), "entity_or_type");
	}

	@Override
	public QueryExpFromCoSpan visitQueryExpFromCoSpan(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.schs.put("s3", t);

		SchExpVar s1 = new SchExpVar("s1");
		SchExpVar s2 = new SchExpVar("s2");
		SchExpVar s3 = new SchExpVar("s3");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("s3", new SchExpEmpty(t));

		MapExpVar m12 = new MapExpVar("m12");
		MapExpVar m32 = new MapExpVar("m32");

		exp.defs.maps.put("m12", new Pair(s1, s2));
		exp.defs.maps.put("m32", new Pair(s3, s2));

		return new QueryExpFromCoSpan(m12, m32, Collections.emptyList());
	}

	@Override
	public <Gen, Sk, X, Y> InstExpCascadeDelete<Gen, Sk, X, Y> visitInstExpCascadeDelete(Unit param, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		// exp.defs.schs.put("s2", t);

		SchExpVar s1 = new SchExpVar("s1");
//    SchExpVar s2 = new SchExpVar("s2");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
//    exp.prog.exps.put("s2", new SchExpEmpty(t));

		InstExp i = new InstExpVar("i");
		exp.defs.insts.put("i", s1);

		return new InstExpCascadeDelete(i, s1);
	}

	@Override
	public SchExpJdbcAll visitSchExpJdbcAll(Unit params, AqlTyping r) {
		return new SchExpJdbcAll("jdbc_string", new LinkedList());
	}

	@Override
	public QueryExpFromEds visitQueryExpFromEds(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.defs.schs.put("s", t);
		exp.defs.eds.put("eds", new SchExpVar("s")); // , Collections.emptyList(), Collections.emptyList(), Unit.unit));
		EdsExpVar eds = new EdsExpVar("eds");

		return new QueryExpFromEds(eds, 0);
	}

	@Override
	public EdsExpSch visitEdsExpSch(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.defs.schs.put("s", t);
		SchExpVar s = new SchExpVar("s");
		exp.prog.exps.put("s", new SchExpEmpty(t));

		return new EdsExpSch(s);
	}

	@Override
	public TyExp visitTyExpAdt(Unit params, AqlTyping r) {
		return new TyExpAdt(Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
	}

	@Override
	public ApgTyExpVar visitApgTyExpVar(Unit params, AqlTyping r) {
		r.defs.apgts.put("t", Unit.unit);
		ApgTyExpVar e = new ApgTyExpVar("t");
		return e;
	}

	@Override
	public ApgTyExpRaw visitApgTyExpRaw(Unit params, AqlTyping r) {
		return new ApgTyExpRaw(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public ApgInstExpInitial visitApgInstExpInitial(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);
		return new ApgInstExpInitial(s);
	}

	@Override
	public ApgInstExpTerminal visitApgInstExpTerminal(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		return new ApgInstExpTerminal(t);
	}

	@Override
	public ApgInstExpTimes visitApgInstExpTimes(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);
		ApgInstExpVar a = new ApgInstExpVar("G1");
		ApgInstExpVar b = new ApgInstExpVar("G2");
		r.defs.apgis.put("G1", s);
		r.defs.apgis.put("G2", s);
		return new ApgInstExpTimes(a, b);
	}

	@Override
	public ApgInstExpPlus visitApgInstExpPlus(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);
		ApgInstExpVar a = new ApgInstExpVar("G1");
		ApgInstExpVar b = new ApgInstExpVar("G2");
		r.defs.apgis.put("G1", s);
		r.defs.apgis.put("G2", s);
		return new ApgInstExpPlus(a, b);
	}

	@Override
	public ApgInstExpVar visitApgInstExpVar(Unit param, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);
		ApgInstExpVar a = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);
		return a;
	}

	@Override
	public ApgInstExpRaw visitApgInstExpRaw(Unit param, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);
		ApgInstExpRaw a = new ApgInstExpRaw(s, Collections.emptyList(), Collections.emptyList());
		r.defs.apgis.put("G", s);
		return a;
	}

	@Override
	public ApgTransExpRaw visitApgTransExpRaw(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		return new ApgTransExpRaw(G1, G2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public ApgTransExpVar visitApgTransExpVar(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		ApgInstExp G2 = new ApgInstExpVar("G2");
		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		r.defs.apgis.put("G1", s1);
		r.defs.apgis.put("G2", s2);

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		return new ApgTransExpVar("h");
	}

	@Override
	public ApgTransExp visitApgTransExpInitial(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);

		r.defs.apgms.put("h", new Pair<>(new ApgInstExpInitial(s), G));
		return new ApgTransExpVar("h");
	}

	@Override
	public ApgTransExp visitApgTransExpTerminal(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);

		r.defs.apgms.put("h", new Pair<>(G, new ApgInstExpTerminal(t)));
		return new ApgTransExpVar("h");
	}

	@Override
	public ApgTransExp visitApgTransExpFst(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		return new ApgTransExpFst(G1, G2);
	}

	@Override
	public ApgTransExp visitApgTransExpSnd(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		return new ApgTransExpSnd(G1, G2);
	}

	@Override
	public ApgTransExp visitApgTransExpInl(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		return new ApgTransExpInl(G1, G2);
	}

	@Override
	public ApgTransExp visitApgTransExpInr(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		return new ApgTransExpInr(G1, G2);
	}

	@Override
	public ApgTransExp visitApgTransExpId(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);
		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G1 = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);

		return new ApgTransExpId(G1);
	}

	@Override
	public ApgTransExp visitApgTransExpCase(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);
		ApgSchExp s3 = new ApgSchExpVar("s3");
		r.defs.apgschemas.put("s3", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);
		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s3);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G1, G));
		r.defs.apgms.put("h'", new Pair<>(G2, G));

		return new ApgTransExpCase(h1, h2);
	}

	@Override
	public ApgTransExp visitApgTransExpPair(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);
		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);
		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G, G1));
		r.defs.apgms.put("h'", new Pair<>(G, G2));

		return new ApgTransExpPair(h1, h2);
	}

	@Override
	public ApgTransExp visitApgTransExpCompose(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s3 = new ApgSchExpVar("s3");
		r.defs.apgschemas.put("s3", t);
		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);
		ApgInstExp G3 = new ApgInstExpVar("G3");
		r.defs.apgis.put("G3", s3);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G2, G3));

		return new ApgTransExpCompose(h1, h2);
	}

	@Override
	public ApgInstExpEqualize visitApgInstExpEqualize(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G1, G2));

		return new ApgInstExpEqualize(h1, h2);
	}

	@Override
	public ApgTransExp visitApgTransExpEqualize(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G1, G2));

		return new ApgTransExpEqualize(h1, h2);
	}

	@Override
	public ApgTransExp visitApgTransExpEqualizeU(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s1 = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s1", t);
		ApgSchExp s2 = new ApgSchExpVar("s2");
		r.defs.apgschemas.put("s2", t);
		ApgSchExp s = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s1);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s2);
		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");
		ApgTransExpVar k = new ApgTransExpVar("k");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G1, G2));
		r.defs.apgms.put("k", new Pair<>(G, G1));

		return new ApgTransExpEqualizeU(h1, h2, k);
	}

	@Override
	public ApgInstExpCoEqualize visitApgInstExpCoEqualize(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s = new ApgSchExpVar("s1");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G1, G2));

		return new ApgInstExpCoEqualize(h1, h2);
	}

	@Override
	public ApgTransExp visitApgTransExpCoEqualize(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G1, G2));

		return new ApgTransExpCoEqualize(h1, h2);
	}

	@Override
	public ApgTransExp visitApgTransExpCoEqualizeU(Unit params, AqlTyping r) {
		ApgTyExp t = new ApgTyExpVar("t");
		r.defs.apgts.put("t", Unit.unit);

		ApgSchExp s = new ApgSchExpVar("s");
		r.defs.apgschemas.put("s", t);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", s);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", s);
		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", s);

		ApgTransExpVar h1 = new ApgTransExpVar("h");
		ApgTransExpVar h2 = new ApgTransExpVar("h'");
		ApgTransExpVar k = new ApgTransExpVar("k");

		r.defs.apgms.put("h", new Pair<>(G1, G2));
		r.defs.apgms.put("h'", new Pair<>(G1, G2));
		r.defs.apgms.put("k", new Pair<>(G2, G));

		return new ApgTransExpCoEqualizeU(h1, h2, k);
	}

	@Override
	public ApgInstExpDelta visitApgInstExpDelta(Unit params, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExp t = new ApgSchExpVar("T");
		r.defs.apgschemas.put("T", ty);
		ApgSchExp s = new ApgSchExpVar("S");
		r.defs.apgschemas.put("S", ty);

		ApgInstExp G = new ApgInstExpVar("G");
		r.defs.apgis.put("G", t);

		ApgMapExp F = new ApgMapExpVar("F");
		r.defs.apgmappings.put("F", new Pair<>(s, t));

		return new ApgInstExpDelta(F, G);

	}

	@Override
	public ApgTransExpDelta visitApgTransExpDelta(Unit params, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExp t = new ApgSchExpVar("T");
		r.defs.apgschemas.put("T", ty);
		ApgSchExp s = new ApgSchExpVar("S");
		r.defs.apgschemas.put("S", ty);

		ApgInstExp G1 = new ApgInstExpVar("G1");
		r.defs.apgis.put("G1", t);
		ApgInstExp G2 = new ApgInstExpVar("G2");
		r.defs.apgis.put("G2", t);

		ApgMapExp F = new ApgMapExpVar("F");
		r.defs.apgmappings.put("F", new Pair<>(s, t));

		ApgTransExp h = new ApgTransExpVar("h");
		r.defs.apgms.put("h", new Pair<>(G1, G2));

		return new ApgTransExpDelta(F, h);
	}

	@Override
	public ApgSchExpInitial visitApgSchExpInitial(Unit params, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		return new ApgSchExpInitial(ty);
	}

	@Override
	public ApgSchExpTerminal visitApgSchExpTerminal(Unit params, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		return new ApgSchExpTerminal(ty);
	}

	@Override
	public ApgSchExpTimes visitApgSchExpTimes(Unit params, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExp S = new ApgSchExpVar("S");
		r.defs.apgschemas.put("S", ty);
		ApgSchExp T = new ApgSchExpVar("T");
		r.defs.apgschemas.put("T", ty);

		return new ApgSchExpTimes(S, T);
	}

	@Override
	public ApgSchExpPlus visitApgSchExpPlus(Unit params, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExp S = new ApgSchExpVar("S");
		r.defs.apgschemas.put("S", ty);
		ApgSchExp T = new ApgSchExpVar("T");
		r.defs.apgschemas.put("T", ty);

		return new ApgSchExpPlus(S, T);
	}

	@Override
	public ApgSchExpVar visitApgSchExpVar(Unit param, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExpVar x = new ApgSchExpVar("v");
		r.defs.apgschemas.put("v", ty);
		return x;
	}

	@Override
	public ApgSchExpRaw visitApgSchExpRaw(Unit param, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		return new ApgSchExpRaw(ty, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public ApgMapExpVar visitApgMapExpVar(Unit param, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExp S = new ApgSchExpVar("S");
		r.defs.apgschemas.put("S", ty);
		ApgSchExp T = new ApgSchExpVar("T");
		r.defs.apgschemas.put("T", ty);

		ApgMapExpVar x = new ApgMapExpVar("v");
		r.defs.apgmappings.put("v", new Pair<>(S, T));
		return x;
	}

	@Override
	public ApgMapExpRaw visitApgMapExpRaw(Unit param, AqlTyping r) {
		ApgTyExp ty = new ApgTyExpVar("ty");
		r.defs.apgts.put("ty", Unit.unit);

		ApgSchExp S = new ApgSchExpVar("S");
		r.defs.apgschemas.put("S", ty);
		ApgSchExp T = new ApgSchExpVar("T");
		r.defs.apgschemas.put("T", ty);

		return new ApgMapExpRaw(S, T, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public SchExpCsv visitSchExpCsv(Unit params, AqlTyping r) {
		return new SchExpCsv("filename", Collections.emptyList(), new TyExpSql());
	}

	@Override
	public InstExpRdfAll visitInstExpRdfAll(Unit param, AqlTyping exp) {
		return new InstExpRdfAll("rdf_uri", Collections.emptyList());
	}

	@Override
	public InstExpJsonAll visitInstExpJsonAll(Unit param, AqlTyping exp) {
		return new InstExpJsonAll("json_uri", Collections.emptyList());
	}

	@Override
	public InstExpXmlAll visitInstExpXmlAll(Unit param, AqlTyping exp) {
		return new InstExpXmlAll("xml_uri", Collections.emptyList());
	}

	@Override
	public <X, Y> PragmaExpJsonInstExport<X, Y> visitPragmaExpJsonInstExport(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new PragmaExpJsonInstExport(i, "json_file", new LinkedList<>(), new LinkedList<>());
	}

	@Override
	public <X, Y> PragmaExpRdfInstExport<X, Y> visitPragmaExpRdfInstExport(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpVar("t");
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new PragmaExpRdfInstExport(i, "rdf_file", new LinkedList<>(), new LinkedList<>());
	}

	@Override
	public InstExpXmlAll visitInstExpMarkdown(Unit param, AqlTyping exp) {
		return new InstExpXmlAll("md_uri", Collections.emptyList());
	}

	@Override
	public InstExpSpanify visitInstExpSpanify(Unit param, AqlTyping exp) {
		SchExp s = InstExpRdfAll.makeSch();
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s", new TyExpRdf());
		exp.defs.insts.put("i", s);
		return new InstExpSpanify(i, Collections.emptyList());
	}

	@Override
	public <X, Y> PragmaExpRdfDirectExport<X, Y> visitPragmaExpRdfDirectExport(Unit params, AqlTyping exp)
			throws RuntimeException {
		SchExp s = InstExpRdfAll.makeSch();
		TyExp t = new TyExpRdf();
		InstExp<String, String, X, Y> i = (InstExp<String, String, X, Y>) (Object) new InstExpVar("i");
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s);
		return new PragmaExpRdfDirectExport(i, "rdf_file", new LinkedList<>(), new LinkedList<>());
	}

	@Override
	public SchExpSpan visitSchExpSpan(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		return new SchExpSpan(s);
	}

	@Override
	public SchExpRdf visitSchExpRdf(Unit params, AqlTyping exp) {
		return new SchExpRdf();
	}

	@Override
	public SchExpMsCatalog visitSchExpMsCatalog(Unit params, AqlTyping r) {
		return new SchExpMsCatalog(new TyExpSql(), "Other");
	}

	@Override
	public SchExpMsQuery visitSchExpMsQuery(Unit params, AqlTyping r) {
		return new SchExpMsQuery(new TyExpSql(), "Other");
	}

	@Override
	public InstExpJdbcDirect visitInstExpJdbcDirect(Unit param, AqlTyping exp) {
		return new InstExpJdbcDirect(new SchExpEmpty(new TyExpEmpty()), Collections.emptyList(), "jdbcString",
				"ROW_NUMBER() OVER()");
	}

	@Override
	public QueryExpSpanify visitQueryExpSpanify(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		return new QueryExpSpanify(s);
	}

	@Override
	public QueryExpMapToSpanQuery visitQueryExpMapToSpanQuery(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		SchExp s2 = new SchExpVar("s2");
		exp.defs.schs.put("s2", t);
		return new QueryExpMapToSpanQuery(
				new MapExpRaw(s, s2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
	}

	@Override
	public SchExpPrefix visitSchExpPrefix(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		return new SchExpPrefix(s, "prefix");
	}

	@Override
	public MapExpToPrefix visitMapExpToPrefix(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		return new MapExpToPrefix(s, "prefix");
	}

	@Override
	public MapExpFromPrefix visitMapExpFromPrefix(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		return new MapExpFromPrefix(s, "prefix");
	}

	@Override
	public EdsExpSqlNull visitEdsExpSqlNull(Unit params, AqlTyping exp) {
		TyExp ty = new TyExpVar("ty");
		exp.defs.tys.put("ty", Unit.unit);
		return new EdsExpSqlNull(ty);
	}

	@Override
	public EdsExpInclude visitEdsExpInclude(Unit params, AqlTyping exp) {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpRdf();
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", t);
		return new EdsExpInclude(s, "old", "new", Collections.emptyList());
	}

	@Override
	public SchExpMsError visitSchExpMsError(Unit params, AqlTyping exp) {
		return new SchExpMsError("Other", "jdbc_string", new TyExpSql());
	}

	@Override
	public InstExpMsError visitInstExpMsError(Unit param, AqlTyping exp) {
		return new InstExpMsError("Other", "jdbc_string", new TyExpSql());
	}

	@Override
	public SchExpMsErrorShallow visitSchExpMsErrorShallow(Unit param, AqlTyping exp) {
		return new SchExpMsErrorShallow("Other", new TyExpSql());
	}

	@Override
	public SchExpFromMsCatalog visitSchExpFromMsCatalog(Unit params, AqlTyping exp) {
		SchExp s = new SchExpMsQuery(new TyExpSql(), "Other");
		InstExp i = new InstExpVar("i");
		exp.defs.insts.put("i", s);
		return new SchExpFromMsCatalog(i, Collections.emptyList());
	}

	@Override
	public QueryExpFront visitQueryExpFront(Unit params, AqlTyping exp) {
		SchExpVar e = new SchExpVar("s");
		TyExpVar x = new TyExpVar("t");
		EdsExpVar eds = new EdsExpVar("eds");
		exp.defs.eds.put("eds", e);
		exp.defs.schs.put("s", x);
		return new QueryExpFront(eds, "0", true, Collections.emptyList());
	}

	@Override
	public SchExpFront visitSchExpFront(Unit params, AqlTyping exp) {
		SchExpVar e = new SchExpVar("s");
		TyExpVar x = new TyExpVar("t");
		EdsExpVar eds = new EdsExpVar("eds");
		exp.defs.eds.put("eds", e);
		exp.defs.schs.put("s", x);
		return new SchExpFront(eds, "0");
	}

	@Override
	public InstExpTinkerpop visitInstExpTinkerpop(Unit param, AqlTyping exp) {
		TyExp t = new TyExpSql();
		exp.defs.schs.put("s", t);
		return new InstExpTinkerpop(new LinkedList());
	}

	@Override
	public SchExpTinkerpop visitSchExpTinkerpop(Unit params, AqlTyping exp) {
		return new SchExpTinkerpop();
	}

	@Override
	public EdsExpTinkerpop visitEdsExpTinkerpop(Unit params, AqlTyping exp) {
		return new EdsExpTinkerpop();
	}

	@Override
	public PragmaExpBitsy visitPragmaExpBitsy(Unit params, AqlTyping exp) {
		return new PragmaExpBitsy(Collections.emptyList());
	}

	@Override
	public PragmaExpTinkerpopInstExport visitPragmaExpTinkerpopInstExport(Unit params, AqlTyping exp) {
		SchExpVar e = new SchExpVar("s");
		TyExpVar x = new TyExpVar("t");
		InstExpVar i = new InstExpVar("i");
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.schs.put("s", x);
		exp.defs.insts.put("i", e);
		return new PragmaExpTinkerpopInstExport(i, Collections.emptyList());
	}

	@Override
	public PragmaExpTinkerpop visitPragmaExpTinkerpop(Unit params, AqlTyping exp) {
		return new PragmaExpTinkerpop(Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public EdsExpFromMsCatalog visitEdsExpFromMsCatalog(Unit params, AqlTyping exp) {
		SchExp s = new SchExpMsQuery(new TyExpSql(), "Other");
		InstExp i = new InstExpVar("i");
		exp.defs.insts.put("i", s);
		return new EdsExpFromMsCatalog(i, s, Collections.emptyList());
	}

	@Override
	public ColimSchExpSimplify visitColimSchExpSimplify(Unit params, AqlTyping exp) throws RuntimeException {
		ColimSchExp x = new ColimSchExpVar("sc");
		exp.defs.scs.put("sc", new THashSet());

		return new ColimSchExpSimplify(x, Collections.emptyList());
	}

	@Override
	public EdsExpSigma visitEdsExpSigma(Unit params, AqlTyping exp) throws RuntimeException {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		EdsExp c = new EdsExpVar("c");
		MapExp m = new MapExpVar("m");

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.maps.put("m", new Pair(s1, s2));
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.eds.put("c", s1);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		exp.prog.exps.put("c", new EdsExpRaw(s1, Collections.emptyList(), Collections.emptyList(), Unit.unit));
		exp.prog.exps.put("m", m);
		
		return new EdsExpSigma(m, c);
	}

	@Override
	public EdsExpSql visitEdsExpSql(Unit params, AqlTyping exp) throws RuntimeException {
		SchExp s = new SchExpVar("s");
		TyExp t = new TyExpSql();
		exp.defs.schs.put("s", t);
		exp.defs.tys.put("t", Unit.unit);
		return new EdsExpSql(s);
	}

	@Override
	public ColimSchExpPseudo visitColimSchExpPseudo(Unit params, AqlTyping exp) {
		TyExp t = new TyExpVar("t");
		exp.defs.tys.put("t", Unit.unit);
		return new ColimSchExpPseudo(t, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
	}

	@Override
	public MapExpPseudo visitMapExpPseudo(Unit params, AqlTyping exp) {
		exp.defs.scs.put("c", Collections.emptySet());
		return new MapExpPseudo(new ColimSchExpVar("c"));
	}

	@Override
	public QueryExpRext visitQueryExpRext(Unit params, AqlTyping exp) {
		exp.defs.tys.put("t", Unit.unit);
		TyExpVar t = new TyExpVar("t");
		exp.prog.exps.put("t", new TyExpEmpty());

		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.schs.put("s3", t);

		SchExpVar s1 = new SchExpVar("s1");
		SchExpVar s2 = new SchExpVar("s2");
		SchExpVar s3 = new SchExpVar("s3");

		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("s3", new SchExpEmpty(t));

		QueryExpVar m12 = new QueryExpVar("q12");
		QueryExpVar m13 = new QueryExpVar("q13");

		exp.defs.qs.put("q12", new Pair(s1, s2));
		exp.defs.qs.put("q13", new Pair(s1, s3));

		return new QueryExpRext(m12, m13, Collections.emptyList());
	}

	@Override
	public SchExpUnit visitSchExpUnit(Unit params, AqlTyping r) throws RuntimeException {
		TyExp t = new TyExpVar("t");
		r.defs.tys.put("t", Unit.unit);
		return new SchExpUnit(t);

	}

	@Override
	public QueryExpFromInst visitQueryExpFromInst(Unit params, AqlTyping exp) throws RuntimeException {
		SchExp s1 = new SchExpVar("s1");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i", s1);
		return new QueryExpFromInst(i);
	}
	
	@Override
	public EdsExpLearn visitEdsExpLearn(Unit params, AqlTyping exp) throws RuntimeException {
		SchExp s1 = new SchExpVar("s1");
		TyExp t = new TyExpVar("t");
		InstExp i = new InstExpVar("i1");
		InstExp j = new InstExpVar("i2");
		exp.defs.schs.put("s1", t);
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.insts.put("i1", s1);
		exp.defs.insts.put("i2", s1);
		
		return new EdsExpLearn(i, j);
	}

	@Override
	public QueryExpChase visitQueryExpChase(Unit params, AqlTyping exp) throws RuntimeException {
		SchExp s1 = new SchExpVar("s1");
		SchExp s2 = new SchExpVar("s2");
		TyExp t = new TyExpVar("t");
		QueryExp q = new QueryExpVar("q");
		exp.defs.schs.put("s1", t);
		exp.defs.schs.put("s2", t);
		
		exp.defs.tys.put("t", Unit.unit);
		exp.defs.qs.put("q", new Pair<>(s1,s2));
		exp.defs.eds.put("c", s1);
		exp.prog.exps.put("s1", new SchExpEmpty(t));
		exp.prog.exps.put("s2", new SchExpEmpty(t));
		exp.prog.exps.put("t", new TyExpEmpty());
		EdsExpVar c = new EdsExpVar("c");
		return new QueryExpChase(q, c);
	}
}
