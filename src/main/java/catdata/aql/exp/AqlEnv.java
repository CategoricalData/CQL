package catdata.aql.exp;

import java.util.Map;

import catdata.Program;
import catdata.Util;
import catdata.apg.ApgInstance;
import catdata.apg.ApgMapping;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTransform;
import catdata.apg.ApgTypeside;
import catdata.aql.AqlOptions;
import catdata.aql.ColimitSchema;
import catdata.aql.Comment;
import catdata.aql.Constraints;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Pragma;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.Semantics;
import catdata.aql.Transform;
import catdata.aql.TypeSide;
import catdata.graph.DMG;

public final class AqlEnv {
	
	


	public AqlEnv(Program<Exp<?>> prog) {
		Util.assertNotNull(prog);
		this.prog = prog;
		defaults = prog.options;
	}

	public final Program<Exp<?>> prog;

	@SuppressWarnings("rawtypes")
	public final KindCtx<String, DMG, TypeSide, Schema, Instance, Transform, Mapping, Query, Pragma, Comment, ColimitSchema, Constraints, ApgTypeside, ApgInstance, ApgTransform, ApgSchema, ApgMapping> defs = new KindCtx<>();

	public RuntimeException exn = null;

	public AqlTyping typing = null;

	public AqlOptions defaults;

	public final Map<String, Float> performance = Util.mk();

	Map<Exp<?>, Object> cache = Util.mk();

	public long fd = Long.MAX_VALUE, md = Long.MAX_VALUE, ud = Long.MAX_VALUE; //memory deltas

	public long fl = Long.MAX_VALUE, mh = Long.MIN_VALUE, uh = Long.MIN_VALUE;
	
	public Semantics get(Kind k, String s) {
		return (Semantics) defs.get(s, k);
	}

}
