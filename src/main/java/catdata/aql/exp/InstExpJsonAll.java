package catdata.aql.exp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.atomgraph.etl.json.JSON2RDF;

import catdata.Null;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import picocli.CommandLine;

public class InstExpJsonAll extends InstExp<String, Null<?>, String, Null<?>> {

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

  public InstExpJsonAll(String jdbcString, List<Pair<String, String>> options) {
    this.jdbcString = jdbcString;
    this.options = Util.toMapSafely(options);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.jena_reasoner);
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, String, Null<?>, String, Null<?>> eval0(AqlEnv env, boolean isC) {
    if (isC) {
      throw new IgnoreException();
    }

    // Open a valid json(-ld) input file
    InputStream inputStream;
    try {
      inputStream = new FileInputStream(jdbcString);

      File out = File.createTempFile("jsonload", ".ttl");
      FileOutputStream outs = new FileOutputStream(out);
      JSON2RDF json2rdf = new JSON2RDF(inputStream, outs);

      CommandLine.ParseResult parseResult = new CommandLine(json2rdf).parseArgs(new String[] { "cql://json" });
      if (!CommandLine.printHelpIfRequested(parseResult)) {
        json2rdf.convert();
      }
      outs.close();
      return new InstExpRdfAll(out.getAbsolutePath(), Collections.emptyList()).eval(env, isC);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder().append("import_json_ld_all ").append(Util.quote(jdbcString));
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
    return InstExpRdfAll.makeSch();
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
    InstExpJsonAll other = (InstExpJsonAll) obj;
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
