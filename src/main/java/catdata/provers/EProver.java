
package catdata.provers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

import catdata.Pair;
import catdata.Util;

public class EProver<T, C, V> extends DPKB<T, C, V> {

  // done elsewhere for convenience
  // TODO CQL empty sorts check
  public EProver(String exePath, KBTheory<T, C, V> th, long seconds) {
    super(th);
    this.seconds = seconds;
    this.exePath = exePath;

  }

  public static Pair<Optional<Boolean>, String> check(String exePath, long seconds, String s) {
    Process proc;
    BufferedReader reader;

    File f = new File(exePath);
    if (!f.exists()) {
      throw new RuntimeException("File does not exist: " + exePath);
    }

    try {
      File g = File.createTempFile("AqlEProver" + System.currentTimeMillis(), ".tptp");
      if (g == null) {
        return Util.anomaly();
      }
      Util.writeFile(s, g.getAbsolutePath());
      // System.out.println(g.getAbsolutePath());
      //--proof-object
      String str = exePath + "  --auto --proof-object --cpu-limit=" + seconds + " " + g.getAbsolutePath();
      proc = Runtime.getRuntime().exec(str);

      reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

      String line;
      StringBuffer sb = new StringBuffer();

      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append("\n");
        if (line.contains("# Proof found!")) {
          return new Pair<>(Optional.of(true), sb.toString());
        } else if (line.contains("# No proof found!")) {
          return new Pair<>(Optional.of(false), sb.toString());
        } else if (line.contains("# Failure:")) {
          return new Pair<>(Optional.empty(), sb.toString());
        }
      }
      System.err.println(sb.toString());
      reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      
      while ((line = reader.readLine()) != null) {
        System.err.print(line);
      }

      throw new RuntimeException("0Internal theorem prover anomaly.");
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("1Internal theorem prover anomaly: " + e.getLocalizedMessage());
    }
  }

  private long seconds;
  private String exePath;

  @Override
  public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {

    Process proc;
    BufferedReader reader;

    File f = new File(exePath);
    if (!f.exists()) {
      throw new RuntimeException("File does not exist: " + exePath);
    }

    try {
      File g = File.createTempFile("AqlEProver" + System.currentTimeMillis(), ".tptp");
      if (g == null) {
        return Util.anomaly();
      }
      Util.writeFile(kb.tff(ctx, lhs, rhs), g.getAbsolutePath());
      // System.out.println(g.getAbsolutePath());

      String str = exePath + " --auto --silent --cpu-limit=" + seconds + " " + g.getAbsolutePath();
      proc = Runtime.getRuntime().exec(str);

      reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

      String line;

      while ((line = reader.readLine()) != null) {
         System.out.println(line);
        if (line.contains("# Proof found!")) {
          return true;
        } else if (line.contains("# No proof found!")) {
          return false;
        }
      }
      reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      
      while ((line = reader.readLine()) != null) {
        System.err.print(line);
      }
      throw new RuntimeException("Theorem prover error: did not decide " + lhs + " = " + rhs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "E prover";
  }

  @Override
  public void add(C c, T t) {

  }

  @Override
  public boolean supportsTrivialityCheck() {
    return true;
  }

}
