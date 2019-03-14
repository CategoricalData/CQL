package catdata.nested;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public abstract class NRSchema {

	public static class NRel {
		private final Map<String, Optional<NRel>> t;

		public NRel(Map<String, Optional<NRel>> t) {
			this.t = t;
		}
	}
	/*
	 * public static class NRSchemaToNRel implements NRSchemaVisitor<NRel>, Object>
	 * {
	 * 
	 * @Override public NRel visit(Object env, Dom e) { // Auto-generated method
	 * stub return null; }
	 * 
	 * @Override public NRel visit(Object env, Rcd e) { // Auto-generated method
	 * stub return null; }
	 * 
	 * @Override public NRel visit(Object env, Pow e) { if (e.t instanceof Pow) {
	 * 
	 * } else if (e.t instanceof Dom) {
	 * 
	 * } else if (e.t instanceof Rcd) { Map<String, Optional<NRel>> map = new
	 * HashMap<>(); Rcd r = (Rcd) e.t; for (Entry<String, NRSchema> p :
	 * r.row.entrySet()) { if (p.getValue() instanceof Dom) { map.put(p.getKey(),
	 * Optional.empty()); } else { Pair<NRSchema.NRel,Object> k =
	 * p.getValue().accept(null, this); map.put(p.getKey(), k.first); } } NRel ret =
	 * new NRel(map); } else { throw new RuntimeException(); }
	 * 
	 * // Auto-generated method stub return null; }
	 * 
	 * }
	 */

	public abstract <R, E> R accept(E env, NRSchemaVisitor<R, E> v);

	public static class Dom extends NRSchema {
		@Override
		public <R, E> R accept(E env, NRSchemaVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Rcd extends NRSchema {

		private final Map<String, NRSchema> row;

		public Rcd(Map<String, NRSchema> row) {
			this.row = row;
		}

		@Override
		public <R, E> R accept(E env, NRSchemaVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Pow extends NRSchema {
		private final NRSchema t;

		public Pow(NRSchema t) {
			this.t = t;
		}

		@Override
		public <R, E> R accept(E env, NRSchemaVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public interface NRSchemaVisitor<R, E> {
		R visit(E env, Dom e);

		R visit(E env, Rcd e);

		R visit(E env, Pow e);
	}
}
