package catdata.provers;

public class KBOptions {

  public boolean filter_subsumed_by_self = true;
  public boolean unfailing = true;
  public boolean sort_cps = true;
  public boolean semantic_ac = true;
  public boolean compose = true;
  public boolean syntactic_ac = true;

  @Deprecated
  public boolean horn = false;

  @Deprecated
  public int iterations = 200000;

  @Deprecated
  public int red_its = 32;

  private KBOptions() {
  }

  public static final KBOptions defaultOptions = new KBOptions();

  public KBOptions(boolean xunfailing, boolean xsort_cps, boolean xhorn, boolean xsemantic_ac, int xiterations,
      int xred_its, boolean xfilter_subsumed_by_self, /* boolean simplify, */boolean xcompose,
      boolean xsyntactic_ac) {
    unfailing = xunfailing;
    sort_cps = xsort_cps;
    horn = xhorn;
    semantic_ac = xsemantic_ac;
    iterations = xiterations;
    red_its = xred_its;
    filter_subsumed_by_self = xfilter_subsumed_by_self;
    // this.simplify = simplify;
    compose = xcompose;
    syntactic_ac = xsyntactic_ac;
  }

}
