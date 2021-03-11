package catdata.aql;

import java.util.List;
import java.util.Map;

import catdata.Pair;

public class Mor<A,B,C,D> implements Semantics {
  
  public final Map<A,C> tys;
  public final Map<B,Pair<List<String>,Term<C,Void,D,Void,Void,Void,Void>>> syms;

  public final TypeSide<A,B> src;
  public final TypeSide<C,D> dst;
  
  public Mor(Map<A,C> tys, Map<B,Pair<List<String>,Term<C,Void,D,Void,Void,Void,Void>>> syms, TypeSide<A,B> src, TypeSide<C,D> dst, boolean dont_validate) {
    this.src = src;
    this.dst = dst;
    this.tys = tys;
    this.syms = syms;
  }
  @Override
  public Kind kind() {
    return Kind.THEORY_MORPHISM;
  }

  @Override
  public int size() {
    return src.size();
  }
  
  
}