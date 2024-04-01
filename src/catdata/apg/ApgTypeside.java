package catdata.apg;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Semantics;

public class ApgTypeside implements Semantics {

  public ApgTypeside(Map<String, Pair<Class<?>, Function<String, Object>>> tys,
      Map<String, Triple<List<String>, String, Function<List<Object>, Object>>> udfs) {
    this.Bs = tys;
    this.udfs = udfs;
  }

  // equality can't take the function into account
  public final Map<String, Pair<Class<?>, Function<String, Object>>> Bs;

  public final Map<String, Triple<List<String>, String, Function<List<Object>, Object>>> udfs;

  @Override
  public Kind kind() {
    return Kind.APG_typeside;
  }

  @Override
  public int size() {
    return Bs.size() + udfs.size();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((Bs == null) ? 0 : Bs.hashCode());
    result = prime * result + ((udfs == null) ? 0 : udfs.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ApgTypeside other = (ApgTypeside) obj;
    if (Bs == null) {
      if (other.Bs != null)
        return false;
    } else if (!Bs.equals(other.Bs))
      return false;
    if (udfs == null) {
      if (other.udfs != null)
        return false;
    } else if (!udfs.equals(other.udfs))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return Util.sep(Bs, " -> ", "\n", x -> x.first.toString())
        + Util.sep(udfs, " : ", "\n\t", x -> Util.sep(x.first, ",") + " -> " + x.second + " = " + x.third);

  }

}
