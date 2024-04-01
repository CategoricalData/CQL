
package catdata.provers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

import catdata.Pair;
import catdata.Util;

public class VampireProver<T, C, V> extends DPKB<T, C, V> {

  private static final String TRUE = "% SZS status Theorem",
                              FALSE = "% SZS status CounterSatisfiable",
                              FAILURE = "% Exception";

  private final long seconds;
  private final String exePath;

  // done elsewhere for convenience
  // TODO CQL empty sorts check
  public VampireProver(String exePath, KBTheory<T, C, V> th, long seconds) {
    super(th);
    this.seconds = seconds;
    this.exePath = exePath;

  }

  private static Process runProver(String exePath, long seconds, String tptp) throws IOException {
    File f = new File(exePath);
    if (!f.exists()) {
      throw new RuntimeException("File does not exist: " + exePath);
    }
    File g = File.createTempFile("CqlVampireProver" + System.currentTimeMillis(), ".tptp");
    if (g == null) {
      return Util.anomaly();
    }
    Util.writeFile(tptp, g.getAbsolutePath());
    // System.out.println(g.getAbsolutePath());
    String str = exePath + " --mode casc -t " + seconds + " " + g.getAbsolutePath();
    return Runtime.getRuntime().exec(str);
  }

  public static Pair<Optional<Boolean>, String> check(String exePath, long seconds, String s) {
    try {
      Process proc = runProver(exePath, seconds, s);
      BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

      String line;
      StringBuffer sb = new StringBuffer();

      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append("\n");
        if (line.contains(TRUE)) {
          return new Pair<>(Optional.of(true), sb.toString());
        } else if (line.contains(FALSE)) {
          return new Pair<>(Optional.of(false), sb.toString());
        } else if (line.contains(FAILURE)) {
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

  @Override
  public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    try {
      Process proc = runProver(exePath, seconds, kb.tff(ctx, lhs, rhs));
      BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

      String line;

      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        if (line.contains(TRUE)) {
          return true;
        } else if (line.contains(FALSE)) {
          return false;
        } else if (line.contains(FAILURE)) {
          throw new RuntimeException("Theorem prover error: did not decide " + lhs + " = " + rhs);
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
    return "Vampire prover";
  }

  @Override
  public void add(C c, T t) {

  }

  @Override
  public boolean supportsTrivialityCheck() {
    return true;
  }

}
