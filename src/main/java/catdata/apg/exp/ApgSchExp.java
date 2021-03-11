package catdata.apg.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.apg.ApgOps;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTy;
import catdata.apg.ApgTypeside;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class ApgSchExp extends Exp<ApgSchema<Object>> {
  
  @Override
  public abstract ApgTyExp type(AqlTyping G);
  
  
  public abstract <R, P> R accept(P param, ApgSchExpVisitor<R, P> v) ;

  public static interface ApgSchExpCoVisitor<R, P> {
    
    public abstract ApgSchExpInitial visitApgSchExpInitial(P params, R r) ;
    
    public abstract ApgSchExpTerminal visitApgSchExpTerminal(P params, R r) ;
    
    public abstract ApgSchExpTimes visitApgSchExpTimes(P params, R r) ;
    
    public abstract ApgSchExpPlus visitApgSchExpPlus(P params, R r) ;
    
    public abstract ApgSchExpVar visitApgSchExpVar(P param, R exp) ;
  
    public abstract ApgSchExpRaw visitApgSchExpRaw(P param, R exp) ;

    //public abstract ApgSchExpEqualize visitApgSchExpEqualize(P params, R r) ;
    
    //public abstract ApgSchExpCoEqualize visitApgSchExpCoEqualize(P params, R r) ;
  }

  public abstract <R, P> ApgSchExp coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) ;

  public static interface ApgSchExpVisitor<R, P> {
    public abstract R visit(P param, ApgSchExpVar exp) ;

    public abstract R visit(P params, ApgSchExpInitial exp) ;
    
    public abstract R visit(P params, ApgSchExpTerminal exp) ;
    
    public abstract R visit(P params, ApgSchExpTimes exp) ;
    
    public abstract R visit(P params, ApgSchExpPlus exp) ;

    public abstract R visit(P param, ApgSchExpRaw exp) ;

    //public abstract R visit(P params, ApgSchExpEqualize exp) ;
    
    //public abstract R visit(P params, ApgSchExpCoEqualize exp) ;

  }

  
  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    
  }


  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Kind kind() {
    return Kind.APG_schema;
  }

  @Override
  public Exp<ApgSchema<Object>> Var(String v) {
    return new ApgSchExpVar(v);
  }

  
  ////////////////////////////////////////////////////////////////////////
  
  public static final class ApgSchExpVar extends ApgSchExp {
    public final String var;

    
    @Override
    public <R, P> R accept(P params, ApgSchExpVisitor<R, P> v)  {
      return v.visit(params, this);
    }

    @Override
    public <R, P> ApgSchExpVar coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) {
      return v.visitApgSchExpVar(params, r);
    }
    
  
    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singletonList(new Pair<>(var, Kind.APG_instance));
    }

    public ApgSchExpVar(String var) {
      this.var = var;
    }

    @Override
    public synchronized ApgSchema<Object> eval0(AqlEnv env, boolean isC) {
      return env.defs.apgschemas.get(var);
    }

    public ApgTyExp type(AqlTyping G) {
      if (!G.defs.apgschemas.containsKey(var)) {
        throw new RuntimeException("Undefined APG schema variable: " + var);
      }
      return G.defs.apgschemas.get(var);
    }

    @Override
    public int hashCode() {
      return var.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ApgSchExpVar other = (ApgSchExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

    @Override
    public boolean isVar() {
      return true;
    }

    

  }
  

  public static final class ApgSchExpRaw extends ApgSchExp implements Raw {

    
    @Override
    public String toString() {
      String s = "literal : " + typeside + " {\n" + (imports.isEmpty() ? "" : ("\n" + Util.sep(imports, " ")))
          + "\nlabels\n\t" + Util.sep(Ls, " -> ", "\n\t") ;
      String x = "";
      

      return s+ x +    "\n}";
  
    }

    public final ApgTyExp typeside;
    public final Set<ApgSchExp> imports;
    public final Map<String, ApgTy<String>> Ls;

    public ApgSchExpRaw(ApgTyExp typeside0, List<ApgSchExp> imports0, List<Pair<LocStr, ApgTy<String>>> Ls0) {
      Util.assertNotNull(typeside0, imports0, Ls0);
      this.typeside = typeside0;
      this.imports = Util.toSetSafely(imports0);
      this.Ls = Util.toMapSafely(LocStr.list2(Ls0));
      
      doGuiIndex(imports0, Ls0);
    }
    
    public void doGuiIndex(List<ApgSchExp> imports0, 
        List<Pair<LocStr, ApgTy<String>>> ls0) {
      
      
      List<InteriorLabel<Object>> t = new LinkedList<>();
      for (Pair<LocStr, ApgTy<String>> p : ls0) {
        t.add(new InteriorLabel<>("labels", new Pair<>(p.first.str, p.second),
            p.first.loc,
            x -> x.first + " -> " + x.second)
                .conv());
      }
      raw.put("labels", t);
      
    }
    
    
    private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

    @Override
    public Map<String, List<InteriorLabel<Object>>> raw() {
      return raw;
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      Set<Pair<String, Kind>> ret = new THashSet<>();
      for (ApgSchExp x : imports) {
        ret.addAll(x.deps());
      }
      ret.addAll(typeside.deps());
      return ret;
    }


    @Override
    public <R, P> R accept(P params, ApgSchExpVisitor<R, P> v)  {
      return v.visit(params, this);
    }

    @Override
    public <R, P> ApgSchExpRaw coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) {
      return v.visitApgSchExpRaw(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
    }

    private void wf(ApgTy<String> ty, ApgTypeside typeside, Map<String, ApgTy<String>> Ls0) {
      if (ty.b != null && ! typeside.Bs.containsKey(ty.b)) {
        throw new RuntimeException("Type not in typeside: " + ty.b);
      }
      if (ty.l != null && ! Ls0.containsKey(ty.l)) {
        throw new RuntimeException("Label not in schema: " + ty.l);
      }
      if (ty.m != null) {
        ty.m.forEach((k,v) -> wf(v, typeside, Ls0));
      }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected synchronized ApgSchema eval0(AqlEnv env, boolean isCompileTime) {
      ApgTypeside ts = typeside.eval(env, isCompileTime);
      
      Map<String, ApgTy<String>> Ls0 = new THashMap<>(Ls);
      
      for (ApgSchExp w : imports) {
        ApgSchema x = w.eval(env, isCompileTime);
        Util.putAllSafely(Ls0, x.schema);
      }
      for (Entry<String, ApgTy<String>> l : Ls0.entrySet()) {
        wf(l.getValue(), ts, Ls0);
      }
      
      return new ApgSchema<>(ts, Ls0);
    }
/*
    private ApgTerm<String> eval0(ApgPreTerm term, ApgTypeside ts, ApgTy<String> ty, Map<String, Pair<String, ApgTerm<String>>> Es0) {
      if (ty.b != null) {
        if (term.str == null) {
          throw new RuntimeException("Expecting data at type " + ty.b + ", but received " + term);
        }
        Pair<Class<?>, Function<String, Object>> x = ts.Bs.get(ty.b);
        if (x == null) {
          return Util.anomaly(); //should already be checked
        }
        Object o = x.second.apply(term.str);
        if (!x.first.isInstance(o)) {
          Util.anomaly(); //should already be checked
        }
        return ApgTerm.ApgTermV(o);
      } else if (ty.l != null) {
        if (term.str == null) {
          throw new RuntimeException("Expecting element at label " + ty.l + ", but received " + term);
        }
        if (!(Es0.containsKey(term.str) || Es.containsKey(term.str))) {
          throw new RuntimeException("Not an element: " + term.str);
        }
        Pair<String, ApgTerm<String>> l2x = Es0.get(term.str);
        String l2;
        if (l2x == null) {
          l2 = Es.get(term.str).first;
        } else {
          l2 = l2x.first;
        }
        if (!ty.l.equals(l2)) {
          throw new RuntimeException("Expecting element at label " + ty.l + ", but received element " + term + " at label " + l2);
        }
        return ApgTerm.ApgTermE(term.str);
      } else if (ty.m != null && ty.all) {
        if (term.fields == null) {
          throw new RuntimeException("Expecting tuple at type " + ty + ", but received " + term);
        }
        Map<String, ApgTerm<String>> map = new THashMap<>();

        for (Pair<String, ApgPreTerm> x : term.fields) {
          ApgTy<String> w = ty.m.get(x.first);
          if (w == null) {
            throw new RuntimeException("In " + term + ", " + x.first + ", is not a field in " + ty);
          }
          ApgTerm<String> o = eval0(x.second, ts, w, Es0);
          map.put(x.first, o);
        }
        for (String w : ty.m.keySet()) {
          if (!map.containsKey(w)) {
            throw new RuntimeException("In " + term + ", no field for " + w);
          }
        }        
        return ApgTerm.ApgTermTuple(map);
      } else if (ty.m != null && !ty.all) {
        if (term.field == null) {
          throw new RuntimeException("Expecting injection at type " + ty + ", but received " + term);
        }
      
        ApgTy<String> w = ty.m.get(term.field);
        if (w == null) {
          throw new RuntimeException("In " + term + ", " + term.field + ", is not a field in " + ty);
        }
        ApgTerm<String> o = eval0(term.inj, ts, w, Es0);

        return ApgTerm.ApgTermInj(term.field, o);
      }
      
      return Util.anomaly();
      
    } */

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((Ls == null) ? 0 : Ls.hashCode());
      result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
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
      ApgSchExpRaw other = (ApgSchExpRaw) obj;
      if (typeside == null) {
        if (other.typeside != null)
          return false;
      } else if (!typeside.equals(other.typeside))
        return false;
      if (Ls == null) {
        if (other.Ls != null)
          return false;
      } else if (!Ls.equals(other.Ls))
        return false;
      return true;
    }

    @Override
    public ApgTyExp type(AqlTyping G) {
      typeside.type(G);
      for (Exp<?> z : imports()) {
        if (z.kind() != Kind.APG_schema) {
          throw new RuntimeException("Import of wrong kind: " + z);
        }
        ApgTyExp u = ((ApgSchExp)z).type(G);
        if (!typeside.equals(u)) {
          throw new RuntimeException("Import instance typeside mismatch on " + z + ", is " + u + " and not " + typeside + " as expected.");
        }
      }
      return typeside;
    }

  }
  
  /////////////
  
  public static final class ApgSchExpInitial extends ApgSchExp {
    public final ApgTyExp typeside;

    public ApgSchExpInitial(ApgTyExp typeside) {
      this.typeside = typeside;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
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
      ApgSchExpInitial other = (ApgSchExpInitial) obj;
      if (typeside == null) {
        if (other.typeside != null)
          return false;
      } else if (!typeside.equals(other.typeside))
        return false;
      return true;
    }

    @Override
    public <R, P> R accept(P params, ApgSchExpVisitor<R, P> v) {
      return v.visit(params, this);
    }

    @Override
    public <R, P> ApgSchExpInitial coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) {
      return v.visitApgSchExpInitial(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      typeside.mapSubExps(f);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return typeside.deps();
    }

    @Override
    public ApgTyExp type(AqlTyping G) {
      typeside.type(G);
      return typeside;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected ApgSchema eval0(AqlEnv env, boolean isCompileTime) {
      return ApgOps.initialSchema(typeside.eval(env, isCompileTime));
    }

    @Override
    public String toString() {
      return "empty " + typeside;
    }

  }

  public static final class ApgSchExpTerminal extends ApgSchExp {

    public final ApgTyExp typeside;

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
      return typeside.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ApgSchExpTerminal other = (ApgSchExpTerminal) obj;
      if (typeside == null) {
        if (other.typeside != null)
          return false;
      } else if (!typeside.equals(other.typeside))
        return false;
      return true;
    }

    public ApgSchExpTerminal(ApgTyExp typeside) {
      this.typeside = typeside;
    }

    @Override
    public <R, P> R accept(P params, ApgSchExpVisitor<R, P> v) {
      return v.visit(params, this);
    }

    @Override
    public <R, P> ApgSchExpTerminal coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) {
      return v.visitApgSchExpTerminal(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      typeside.mapSubExps(f);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return typeside.deps();
    }

    @Override
    public ApgTyExp type(AqlTyping G) {
      typeside.type(G);
      return typeside;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected ApgSchema eval0(AqlEnv env, boolean isCompileTime) {
      return ApgOps.terminalSchema(typeside.eval(env, isCompileTime));
    }

    @Override
    public String toString() {
      return "unit " + typeside;
    }

  }

  public static final class ApgSchExpTimes extends ApgSchExp {

    public final ApgSchExp l, r;

    public ApgSchExpTimes(ApgSchExp l, ApgSchExp r) {
      this.l = l;
      this.r = r;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((l == null) ? 0 : l.hashCode());
      result = prime * result + ((r == null) ? 0 : r.hashCode());
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
      ApgSchExpTimes other = (ApgSchExpTimes) obj;
      if (l == null) {
        if (other.l != null)
          return false;
      } else if (!l.equals(other.l))
        return false;
      if (r == null) {
        if (other.r != null)
          return false;
      } else if (!r.equals(other.r))
        return false;
      return true;
    }

    @Override
    public <R, P> R accept(P params, ApgSchExpVisitor<R, P> v) {
      return v.visit(params, this);
    }

    @Override
    public <R, P> ApgSchExpTimes coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) {
      return v.visitApgSchExpTimes(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      l.mapSubExps(f);
      r.mapSubExps(f);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Util.union(l.deps(), r.deps());
    }

    @Override
    public ApgTyExp type(AqlTyping G) {
      ApgTyExp a = l.type(G);
      ApgTyExp b = r.type(G);
      if (!a.equals(b)) {
        throw new RuntimeException("Different typesides: " + a + " and " + b);
      }
      return a;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected ApgSchema eval0(AqlEnv env, boolean isCompileTime) {
      return ApgOps.productSchema(l.eval(env, isCompileTime), r.eval(env, isCompileTime));
    }

    @Override
    public String toString() {
      return "(" + l + " * " + r + ")";
    }
  }

  public static final class ApgSchExpPlus extends ApgSchExp {

    public final ApgSchExp l, r;

    public ApgSchExpPlus(ApgSchExp l, ApgSchExp r) {
      this.l = l;
      this.r = r;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((l == null) ? 0 : l.hashCode());
      result = prime * result + ((r == null) ? 0 : r.hashCode());
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
      ApgSchExpPlus other = (ApgSchExpPlus) obj;
      if (l == null) {
        if (other.l != null)
          return false;
      } else if (!l.equals(other.l))
        return false;
      if (r == null) {
        if (other.r != null)
          return false;
      } else if (!r.equals(other.r))
        return false;
      return true;
    }

    @Override
    public <R, P> R accept(P params, ApgSchExpVisitor<R, P> v) {
      return v.visit(params, this);
    }

    @Override
    public <R, P> ApgSchExpPlus coaccept(P params, ApgSchExpCoVisitor<R, P> v, R r) {
      return v.visitApgSchExpPlus(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      l.mapSubExps(f);
      r.mapSubExps(f);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Util.union(l.deps(), r.deps());
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected ApgSchema eval0(AqlEnv env, boolean isCompileTime) {
      return ApgOps.coproductSchema(l.eval(env, isCompileTime), r.eval(env, isCompileTime));
    }

    @Override
    public ApgTyExp type(AqlTyping G) {
      ApgTyExp a = l.type(G);
      ApgTyExp b = r.type(G);
      if (!a.equals(b)) {
        throw new RuntimeException("Different typesides: " + a + " and " + b);
      }
      return a;
    }

    @Override
    public String toString() {
      return "<" + l + " + " + r + ">";
    }

  }
  
}
