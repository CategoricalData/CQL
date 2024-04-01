package catdata.cql.exp;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.RdfExporter;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public class PragmaExpJsonInstExport<X, Y> extends PragmaExp {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((file == null) ? 0 : file.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + ((tycons == null) ? 0 : tycons.hashCode());
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
    PragmaExpJsonInstExport other = (PragmaExpJsonInstExport) obj;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (file == null) {
      if (other.file != null)
        return false;
    } else if (!file.equals(other.file))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    if (tycons == null) {
      if (other.tycons != null)
        return false;
    } else if (!tycons.equals(other.tycons))
      return false;
    return true;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.start_ids_at);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.map(f);
  }

  public final Map<String, String> options;
  public final String file;
  public final InstExp<String, String, X, Y> I;
  public final Map<String, Pair<String, String>> tycons;

  @Override
  public Map<String, String> options() {
    return options;
  }

  public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  public PragmaExpJsonInstExport(InstExp<String, String, X, Y> i, String f, List<Pair<String, String>> options,
      List<Pair<String, Pair<String, String>>> l) {
    this.options = Util.toMapSafely(options);
    I = i;
    this.file = f;
    if (l == null) {
      this.tycons = Collections.emptyMap();
    } else {
      this.tycons = Util.toMapSafely(l);
    }
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return I.deps();
  }

  catdata.cql.ExternalCodeUtils ext;

  @Override
  public synchronized Pragma eval0(AqlEnv env, boolean isC) {
    AqlOptions op = new AqlOptions(options, env.defaults);
    if (isC) {
      throw new IgnoreException();
    }
    Instance<String, String, Sym, Fk, Att, String, String, X, Y> I0 = I.eval(env, isC);
    int start = (int) op.getOrDefault(AqlOption.start_ids_at);

    Map<String, String> map1 = new THashMap<>();
    Map<String, Function<Object, String>> map2 = new THashMap<>();
    ext = new catdata.cql.ExternalCodeUtils();
    String lang = (String) op.getOrDefault(AqlOptions.AqlOption.graal_language);
    for (Entry<String, Pair<String, String>> k : tycons.entrySet()) {
      Function<Object, String> f = x -> ext.invoke(lang, String.class, k.getValue().second, x);
      map1.put((k.getKey()), k.getValue().first);
      map2.put((k.getKey()), f);
    }
    return new Pragma() {

      String s = null;

      @Override
      public synchronized void execute() {
        if (s == null) {

          try {
            Model m = RdfExporter.xmlExport1(I0, start, map2, map1);
            RDFDataMgr.write(new FileOutputStream(file), m, RDFFormat.JSONLD_PRETTY);
            if (m.size() < 8096) {
              s = Util.readFile(new FileReader(file));
            }
            s = "Size " + m.size();
            ext.close();
          } catch (Exception e) {
            e.printStackTrace();
            s = file + "\n\n" + e.getMessage() + "\n\n" + s;
          }
        }
      }

      @Override
      public synchronized String toString() {
        return s;
      }

    };
  }

  // TODO: add map
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder().append("export_json_instance ").append(I).append(" ");
    if (!options.isEmpty()) {
      sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
    }
    return sb.toString();
  }

  @Override
  public Unit type(AqlTyping G) {
    I.type(G);
    return Unit.unit;
  }

}