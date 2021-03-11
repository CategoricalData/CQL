package catdata.apg;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import catdata.Pair;
import catdata.Util;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

public class ApgTy<L> {

  public final Map<String,ApgTy<L>> m;
  public final boolean all;

  public final L l;
  public final String b;
  
  public <X> ApgTy<X> map(Function<L,ApgTy<X>> f) {
    if (l != null) {
      return f.apply(l);
    } else if (b != null) {
      return convert();
    } 
    return ApgTyP(all, Util.map(m, (k,v)->new Pair<>(k, v.map(f))));
  }
  
  @SuppressWarnings("rawtypes")
  private static HashingStrategy<ApgTy> strategy = new HashingStrategy<>() {
    private static final long serialVersionUID = 1L;

    @Override
    public int computeHashCode(ApgTy t) {
      return t.hashCode2();
    }

    @Override
    public boolean equals(ApgTy s, ApgTy t) {
      return s.equals2(t);
    }
  };

  @SuppressWarnings("rawtypes")
  public static Map<ApgTy, ApgTy> cache = new TCustomHashMap<>(strategy);

  
  public static synchronized <L> ApgTy<L> ApgTyL(L str) {
    return mkApgTy(str, null, false, null);
  }
  
  public static synchronized <L> ApgTy<L> ApgTyB(String str) {
    return mkApgTy(null, str, false, null);
  }
  
  public static synchronized <L> ApgTy<L> ApgTyP(boolean all, Map<String,ApgTy<L>> str) {
    return mkApgTy(null, null, all, str);
  }

  
  private static synchronized <L> ApgTy<L> mkApgTy(L l, String b, boolean all, Map<String,ApgTy<L>> m) {
    ApgTy<L> ret = new ApgTy<>(l, b, all, m);
    
    ApgTy<L> ret2 = cache.get(ret);
    if (ret2 != null) {
      return ret2;
    }
    cache.put(ret, ret);
    return ret;
  }
  
  private ApgTy(L l, String b, boolean all, Map<String,ApgTy<L>> m) {
    this.l = l;
    this.b = b;
    this.all = all;
    this.m = m;
  }
  

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this;
  } 

  public int hashCode2() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (all ? 1231 : 1237);
    result = prime * result + ((b == null) ? 0 : b.hashCode());
    result = prime * result + ((l == null) ? 0 : l.hashCode());
    result = prime * result + ((m == null) ? 0 : m.hashCode());
    return result;
  }


  public boolean equals2(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ApgTy other = (ApgTy) obj;
    if (all != other.all)
      return false;
    if (b == null) {
      if (other.b != null)
        return false;
    } else if (!b.equals(other.b))
      return false;
    if (l == null) {
      if (other.l != null)
        return false;
    } else if (!l.equals(other.l))
      return false;
    if (m == null) {
      if (other.m != null)
        return false;
    } else if (!m.equals(other.m))
      return false;
    return true;
  }


  @Override
  public String toString() {
    if (l != null) {
      return l.toString();
    } else if (b != null) {
      return b.toString();
    }
    if (all) {
      return "(" + Util.sep(m, ":", " * ") + ")";
    }
    return "<" + Util.sep(m, ":", " + ") + ">";
  }

  public <X> ApgTy<X> convert() {
    return (ApgTy<X>) this;
  }

  public void validate(ApgSchema<L> dst) {
    if (m != null) {
      for (Entry<String, ApgTy<L>> x : m.entrySet()) {
        x.getValue().validate(dst);
      }
    } else if (l != null) {
      if (!dst.schema.containsKey(l)) {
        throw new RuntimeException("Not a schema abel: " + l);
      }      
    }
  }
}
