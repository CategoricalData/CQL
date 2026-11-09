package catdata.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.nested.NRSchema.Dom;
import catdata.nested.NRSchema.NRSchemaVisitor;
import catdata.nested.NRSchema.NRel;
import catdata.nested.NRSchema.Pow;
import catdata.nested.NRSchema.Rcd;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link NRSchema}, the nested relational schemas of the shredding viewer. */
class NRSchemaTest {

  @Test
  @SuppressWarnings("unchecked")
  void dispatchesEachKindToItsVisitor() {
    Dom dom = new Dom();
    Rcd rcd = new Rcd(Map.of("a", dom));
    Pow pow = new Pow(rcd);
    NRSchemaVisitor<String, Integer> v = mock(NRSchemaVisitor.class);
    when(v.visit(0, dom)).thenReturn("dom");
    when(v.visit(0, rcd)).thenReturn("rcd");
    when(v.visit(0, pow)).thenReturn("pow");
    assertEquals("dom", dom.accept(0, v));
    assertEquals("rcd", rcd.accept(0, v));
    assertEquals("pow", pow.accept(0, v));
    assertNotNull(new NRel(Map.of("a", Optional.empty())));
  }
}
