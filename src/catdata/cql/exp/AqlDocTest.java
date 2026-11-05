package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AqlDoc}, the HTML document that the IDE emits for a compiled program: each
 * declaration as preformatted text, followed by a table for an instance, a drawing for a graph, the
 * rendered text of a comment, and nothing for the other kinds.
 */
class AqlDocTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      instance I = literal : S { generators a : A equations a.n = x }
      mapping F = identity S
      query Q = identity S
      transform t = identity I
      constraints C = literal : S { }
      graph G = literal { nodes u v edges e : u -> v }
      schema_colimit L = quotient S : Ty
      command P = check C I
      md { (* "# Title" *) }
      html { (* "<b>bold</b>" *) }
      apg_typeside AT = literal { }
      apg_schema AS = literal : AT { }
      apg_instance AI = literal : AS { }
      apg_morphism AH = identity AI
      apg_mapping AM = literal : AS -> AS { labels }
      """;

  private static String doc;

  @BeforeAll
  static void emit() throws Exception {
    AqlMultiDriver d = ExpTestHelpers.run(PROGRAM);
    assertTrue(d.exn.isEmpty(), d.exn.toString());
    doc = AqlDoc.doc(d.env, d.env.prog);
  }

  @Test
  void printsEachDeclarationButTheComments() {
    assertTrue(doc.startsWith("<pre>\ntypeside Ty = literal"), doc);
    for (String name :
        new String[] {
          "schema S",
          "instance I",
          "mapping F",
          "query Q",
          "transform t",
          "constraints C",
          "graph G",
          "schema_colimit L",
          "command P",
          "apg_typeside AT",
          "apg_schema AS",
          "apg_instance AI",
          "apg_morphism AH",
          "apg_mapping AM"
        }) {
      assertTrue(doc.contains("<pre>\n" + name + " = "), name);
    }
    assertFalse(doc.contains("comment "), doc);
  }

  @Test
  void drawsAnInstanceAsATable() {
    assertTrue(doc.contains("<table"), doc);
  }

  @Test
  void drawsAGraphWithSpringy() {
    assertTrue(doc.contains("\nvar graph0 = new Springy.Graph();"), doc);
    assertTrue(doc.contains("\ngraph0.addNodes('u');"), doc);
    assertTrue(doc.contains("\ngraph0.addNodes('v');"), doc);
    assertTrue(doc.contains("\ngraph0.addEdges(['u', 'v', {label: 'e'}]);"), doc);
    assertTrue(doc.contains("<canvas id=\"canvas0\" width=\"640\" height=\"320\" />"), doc);
  }

  @Test
  void rendersAMarkdownCommentAndCopiesAnHtmlComment() {
    assertTrue(doc.contains("<h1>Title</h1>"), doc);
    assertTrue(doc.contains("<b>bold</b>"), doc);
  }

  @Test
  void printsACommandAfterItRuns() {
    assertTrue(doc.contains("command P = check C I\n</pre>\n<pre>Satisfied</pre>"), doc);
  }

  @Test
  void printsOnlyTheTextOfADeclarationThatFailed() throws Exception {
    AqlMultiDriver d =
        ExpTestHelpers.run(
            """
            typeside Ty = literal { types String }
            schema S = literal : Ty { entities A }
            instance J = literal : S { generators b : Nope }
            """);
    String text = AqlDoc.doc(d.env, d.env.prog);
    assertTrue(text.contains("<pre>\ninstance J = literal : S"), text);
    assertFalse(text.contains("<table"), text);
  }

  @Test
  void needsACompiledProgram() throws Exception {
    AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
    RuntimeException e = assertThrows(RuntimeException.class, () -> AqlDoc.doc(null, env.prog));
    assertEquals("Must compile before using HTML output", e.getMessage());
    assertThrows(RuntimeException.class, () -> AqlDoc.doc(env, null));
  }
}
