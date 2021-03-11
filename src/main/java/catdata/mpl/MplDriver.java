package catdata.mpl;

import catdata.Environment;
import catdata.LineException;
import catdata.Program;
import catdata.Unit;
import catdata.mpl.Mpl.MplExp;

class MplDriver {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static Environment<MplObject> makeEnv(@SuppressWarnings("unused") String str,
      Program<MplExp<String, String>> init) {
    Environment<MplObject> ret = new Environment<>();
    // Map<String, Integer> extra = new HashMap<>();

    for (String k : init.order) {
      MplExp se = init.exps.get(k);
      try {
        MplObject xxx = (MplObject) se.accept(Unit.unit, new MplOps(ret));
        ret.put(k, xxx);
      } catch (Throwable t) {
        t.printStackTrace();
        throw new LineException(t.getLocalizedMessage(), k, "");
      }
    }

    return ret;
  }

}
