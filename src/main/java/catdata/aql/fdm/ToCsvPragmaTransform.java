package catdata.aql.fdm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;

import catdata.Pair;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Pragma;
import catdata.aql.Transform;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

public class ToCsvPragmaTransform<Ty, En, Sym, Att, Fk, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends Pragma {

  private final File file;

  private AqlOptions options1;

  private AqlOptions options2;

  private Transform<Ty, En, Sym, Att, Fk, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;

  public ToCsvPragmaTransform(Transform<Ty, En, Sym, Att, Fk, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h, String s,
      AqlOptions options1, AqlOptions options2) {
    this.options1 = options1;
    this.options2 = options2;
    this.file = new File(s);
    this.h = h;
  }

  private void delete() {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      throw new RuntimeException("Cannot delete directory: " + file);
    }
    if (!file.delete()) {
      throw new RuntimeException("Cannot delete file: " + file);
    }
  }

  @Override
  public void execute() {
    StringBuffer sb = new StringBuffer();
    try (CSVPrinter printer = new CSVPrinter(sb, ToCsvPragmaInstance.getFormat(options1))) {
    
      int srcId = (int) options1.getOrDefault(AqlOption.start_ids_at);
      int dstId = (int) options2.getOrDefault(AqlOption.start_ids_at);

      Pair<TObjectIntMap<X1>, TIntObjectMap<X1>> a = h.src().algebra().intifyX(srcId);
      Pair<TObjectIntMap<X2>, TIntObjectMap<X2>> b = h.dst().algebra().intifyX(dstId);

      for (En en : h.src().schema().ens) {
        for (X1 x1 : h.src().algebra().en(en)) {
          List<String> row = new LinkedList<>();
          row.add(Integer.toString(a.first.get(x1)));
          X2 x2 = h.repr(en, x1);
          row.add(Integer.toString(b.first.get(x2)));
          printer.printRecord(row);
        }
      }

      delete();
      if (!file.createNewFile()) {
        throw new RuntimeException("Could not create new file: " + file);
      }
      FileWriter out = new FileWriter(file);
      out.write(sb.toString());
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "Exported to " + file + ".";
  }

}
