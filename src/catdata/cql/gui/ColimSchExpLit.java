package catdata.cql.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.ColimitSchema;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.AqlEnv;
import catdata.cql.exp.AqlTyping;
import catdata.cql.exp.ColimSchExp;
import catdata.cql.exp.Exp;
import catdata.cql.exp.SchExp;
import catdata.cql.exp.TyExp;
import catdata.cql.exp.ColimSchExp.ColimSchExpVisitor;
import catdata.cql.exp.SchExp.SchExpLit;
import catdata.cql.exp.TyExp.TyExpLit;

public class ColimSchExpLit extends ColimSchExp {

	@Override
	public int hashCode() {
		return Objects.hash(c);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColimSchExpLit other = (ColimSchExpLit) obj;
		return Objects.equals(c, other.c);
	}

	public final ColimitSchema<String> c;

	public ColimSchExpLit(ColimitSchema<String> c) {
		if (c == null)
			Util.anomaly();
		this.c = c;
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Kind kind() {
		return Kind.SCHEMA_COLIMIT;
	}

	@Override
	protected ColimitSchema<String> eval0(AqlEnv env, boolean isCompileTime) {
		return c;
	}

	@Override
	public String toString() {
		return c.toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptyList();
	}

	@Override
	public SchExp getNode(String n, AqlTyping G) {
		return new SchExpLit(c.nodes.get(n));
	}

	@Override
	public TyExp typeOf(AqlTyping G) {
		return new TyExpLit(c.ty);
	}

	@Override
	public Set<String> type(AqlTyping G) {
		return c.nodes.keySet();
	}

	@Override
	public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
		return Collections.emptySet();
	}

	@Override
	public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

}
