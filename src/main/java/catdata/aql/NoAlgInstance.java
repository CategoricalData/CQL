package catdata.aql;

import java.util.function.BiConsumer;

import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.exp.Att;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Sym;

public class NoAlgInstance extends Instance<String, String, Sym, Fk, Att, String, String, Void, Void> {

  public final Schema<String, String, Sym, Fk, Att> schema;

  private DP<String, String, Sym, Fk, Att, String, String> dp;

  public final AqlOptions options;

  private final Collage<String, String, Sym, Fk, Att, String, String> col;

  public NoAlgInstance(Collage<String, String, Sym, Fk, Att, String, String> col, Schema<String, String, Sym, Fk, Att> schema,
      AqlOptions options) {
    Util.assertNotNull(options);
    this.schema = schema;
    this.options = options;
    this.col = col;
    validateNoTalg();
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> schema() {
    return schema;
  }

  @Override
  public IMap<String, String> gens() {
    return Instance.mapToIMap(col.gens());
  }

  @Override
  public IMap<String, String> sks() {
    return Instance.mapToIMap(col.sks());
  }

  public synchronized void eqs(
      BiConsumer<Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> f) {
    col.eqs().forEach(x->f.accept(x.lhs, x.rhs));
  }
  
  
  @Override
  public synchronized DP<String, String, Sym, Fk, Att, String, String> dp() {
    if (dp == null) {
      dp = AqlProver.createInstance(options, col, schema);
      return dp;
    }
    return dp;
  }

  @Override
  public synchronized Algebra<String, String, Sym, Fk, Att, String, String, Void, Void> algebra() {
    return Util.anomaly();
  }

  @Override
  public boolean requireConsistency() {
    return (boolean) options.getOrDefault(AqlOption.require_consistency);
  }

  @Override
  public boolean allowUnsafeJava() {
    return (boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe);
  }

  

}