package catdata.mpl;

import catdata.Environment;
import catdata.Unit;
import catdata.mpl.Mpl.MplExp.MplEval;
import catdata.mpl.Mpl.MplExp.MplSch;
import catdata.mpl.Mpl.MplExp.MplVar;
import catdata.mpl.Mpl.MplExpVisitor;

public class MplOps<O, A> implements MplExpVisitor<O, A, MplObject, Unit> {

  private final Environment<MplObject> ENV;

  public MplOps(Environment<MplObject> env) {
    ENV = env;
  }

  @Override
  public MplObject visit(Unit env, MplVar<O, A> e) {
    return ENV.get(e.s);
  }

  @Override
  public MplObject visit(Unit env, MplSch<O, A> e) {
    return e;
  }

  @SuppressWarnings("unchecked")
  @Override
  public MplObject visit(Unit env, MplEval<O, A> e) {
    MplObject o = ENV.get(e.sch0);
    if (!(o instanceof MplSch)) {
      throw new RuntimeException("Not a theory: " + e.sch0);
    }
    MplSch<O, A> sch = (MplSch<O, A>) o;
    e.validte(sch);
    return e;
  }

}
