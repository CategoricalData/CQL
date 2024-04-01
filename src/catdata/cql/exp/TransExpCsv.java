package catdata.cql.exp;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.LocStr;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public class TransExpCsv<X1, Y1, X2, Y2>
    extends TransExpImport<String, String, String, String, X1, Y1, X2, Y2, Map<String, List<String[]>>> {

  public TransExpCsv(InstExp<String, String, X1, Y1> src, InstExp<String, String, X2, Y2> dst, List<Pair<LocStr, String>> files,
      List<Pair<String, String>> options) {
    super(src, dst, files, options);
  }

  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    src.map(f);
    dst.map(f);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.csv_field_delim_char);
    set.add(AqlOption.csv_escape_char);
    set.add(AqlOption.csv_quote_char);
    set.add(AqlOption.csv_file_extension);
    set.add(AqlOption.csv_generate_ids);
    set.add(AqlOption.csv_emit_ids);
    set.add(AqlOption.import_col_seperator);
    set.add(AqlOption.csv_import_prefix);
    set.add(AqlOption.csv_prepend_entity);
    set.add(AqlOption.prepend_entity_on_ids);
    set.add(AqlOption.prepend_entity_on_ids);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder().append("import_csv ").append(src).append(" -> ").append(dst)
        .append(" {\n\t").append(Util.sep(map, " -> ", "\n\t"));
    sb = new StringBuilder(sb.toString().trim());
    if (!options.isEmpty()) {
      sb.append("options").append(Util.sep(options, "\n\t\t", " = "));
    }
    return sb.append("}").toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof TransExpCsv) && super.equals(obj);
  }

  @Override
  protected String getHelpStr() {
    return "";
  }

  @Override
  protected void stop(Map<String, List<String[]>> h) throws Exception {
  }

  @Override
  protected void processEn(String en, Schema<String, String, Sym, Fk, Att> sch, Map<String, List<String[]>> h, String q)
      throws Exception {
    for (String[] row : h.get(en)) {
      if (row.length != 2) {
        throw new RuntimeException("On " + en + ", encountered a row of length != 2: " + Arrays.toString(row));
      }
      String gen = row[0];
      String gen2 = row[1];
      if (gen == null) {
        throw new RuntimeException("Encountered a NULL generator in column 1 of " + en);
      }
      if (gen2 == null) {
        throw new RuntimeException("Encountered a NULL generator in column 2 of " + en);
      }
      gens.put(InstExpImport.toGen(en, gen, op), Term.Gen(InstExpImport.toGen(en, gen2, op)));
    }
  }

  @Override
  protected Map<String, List<String[]>> start(Schema<String, String, Sym, Fk, Att> sch) throws Exception {
    Map<String, Reader> map2 = new THashMap<>();
    for (String q : map.keySet()) {
      map2.put(q, new InputStreamReader(new URL(map.get(q)).openStream()));
    }

    Map<String, List<String[]>> ret = InstExpCsv.start2(map2, op, sch, false);
    return ret;
  }

}
