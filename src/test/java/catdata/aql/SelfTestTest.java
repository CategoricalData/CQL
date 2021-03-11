package catdata.aql;

import java.util.Map;
import org.junit.jupiter.api.*;

public class SelfTestTest {

  @Test
  public void selfTest() throws java.io.IOException {
    Map<String, Throwable> result = AqlTester.doSelfTestSilent();
    if (!result.isEmpty()) {
      String msg = "Errors:\n";
      for (Map.Entry<String, Throwable> e : result.entrySet()) {
        msg += "\n" + e.getKey() + "\n" + e.getValue().getMessage() + "\n";
      }
      Assertions.fail(msg);
    }
  }
}
