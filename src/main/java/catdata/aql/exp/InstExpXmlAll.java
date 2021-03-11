package catdata.aql.exp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.json.XML;

import catdata.Null;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;

public class InstExpXmlAll extends InstExp<String, Null<?>, String, Null<?>> {

  private final Map<String, String> options;

  private final String jdbcString;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
  }

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.emptySet();
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  public InstExpXmlAll(String jdbcString, List<Pair<String, String>> options) {
    this.jdbcString = jdbcString;
    this.options = Util.toMapSafely(options);
    this.o2 = options;
  }

  private List<Pair<String, String>> o2;

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.jena_reasoner);
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, String, Null<?>, String, Null<?>> eval0(AqlEnv env, boolean isC) {
    if (isC) {
      throw new IgnoreException();
    }

    try {
      String in = Util.readFile(new FileReader(new File(jdbcString)));
      JSONObject json = XML.toJSONObject(in); // converts xml to json
      String s = json.toString(4); // json pretty print

      File f = File.createTempFile("xml_cql_import", ".json");
      FileWriter w = new FileWriter(f);
      w.write(s);
      w.close();
      return new InstExpJsonAll(f.getAbsolutePath(), o2).eval(env, isC);

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder().append("import_xml_all ").append(Util.quote(jdbcString));
    if (!options.isEmpty()) {
      sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
    }
    return sb.toString();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Collections.emptySet();
  }

  @Override
  public SchExp type(AqlTyping G) {
    return new SchExpInst<>(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
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
    InstExpXmlAll other = (InstExpXmlAll) obj;
    if (jdbcString == null) {
      if (other.jdbcString != null)
        return false;
    } else if (!jdbcString.equals(other.jdbcString))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    return true;
  }

}
