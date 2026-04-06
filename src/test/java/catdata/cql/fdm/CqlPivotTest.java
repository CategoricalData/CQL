package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class CqlPivotTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsInputInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertSame(inst, pivot.I);
    }

    @Test
    void constructorCreatesIntermediateSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertNotNull(pivot.intI);
    }

    @Test
    void constructorCreatesMapping() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertNotNull(pivot.F);
    }

    @Test
    void constructorCreatesTargetInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertNotNull(pivot.J);
    }
  }

  @Nested
  class PivotBehavior {

    @Test
    void pivotSchemaHasOneEntityPerElement() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      // single entity "E" with one element "e1" -> intermediate schema has entity "e1"
      assertTrue(pivot.intI.ens.contains("e1"));
    }

    @Test
    void pivotInstanceHasData() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      // J should have data corresponding to the pivot
      assertNotNull(pivot.J.algebra());
    }

    @Test
    void pivotMappingSourceIsIntermediateSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertSame(pivot.intI, pivot.F.src);
    }

    @Test
    void pivotMappingTargetIsOriginalSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertSame(inst.schema(), pivot.F.dst);
    }
  }
}
