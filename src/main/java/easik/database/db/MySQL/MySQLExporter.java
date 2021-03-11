package easik.database.db.MySQL;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import easik.EasikTools;
import easik.database.api.jdbc.JDBCDriver;
import easik.database.api.jdbc.JDBCExporter;
import easik.database.base.PersistenceDriver;
import easik.model.attribute.EntityAttribute;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.keys.UniqueIndexable;
import easik.model.keys.UniqueKey;
import easik.model.path.ModelPath;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.view.vertex.QueryNode;

/**
 * EASIK MySQL sketch export driver. This driver is loaded by calling
 * getSketchExporter() on a MySQL db driver object. The following options passed
 * in to the getSketchExporter options parameter, in addition to those mentioned
 * in SketchExporter, are supported by this export driver:
 * <p/>
 * <ul>
 * <li><code>createDatabase</code> &mdash; if set to <code>"true"</code>,
 * <code>CREATE DATABASE</code> statements will be added to the SQL dump
 * <i>[{@link #exportToString()} and {@link #exportToFile(java.io.File)}
 * only]</i></li>
 * <li><code>dropDatabase</code> &mdash; if set to <code>"true"</code>, and
 * using the above <code>createDatabase</code> option,
 * <code>DROP DATABASE</code> statements will be added to the SQL dump
 * <i>[exportToString() and exportToFile() only]</i></li>
 * </ul>
 *
 */
public class MySQLExporter extends JDBCExporter {
  /**
   * The table options we want to use--usually just the storage engine we want.
   * InnoDB is by far the most reliable, and typically the fastest (especially
   * under concurrent, mixed read/write access), and most importantly, it is the
   * only engine that supports foreign keys.
   */
  public static final String TABLE_OPTIONS = " ENGINE=InnoDB";

  // MySQL doesn't support multiple triggers for the same action,
  // so we store them up here to group them together

  /**  */
  private LinkedHashMap<EntityNode, LinkedHashMap<String, LinkedList<String>>> triggers = new LinkedHashMap<>(10);

  // Will be set to true if we need to add a fail procedure

  /**  */
  private boolean addFail;

  /**
   *
   *
   * @param sketch
   * @param db
   * @param options
   *
   * @throws PersistenceDriver.LoadException
   */
  public MySQLExporter(final Sketch sketch, final JDBCDriver db, final Map<String, ?> options)
      throws PersistenceDriver.LoadException {
    super(sketch, db, options);
  }

  /**
   *
   *
   * @return
   */
  @Override
  public List<String> initialize() {
    // super.initialize() gives us the current date/time and Easik blurb in
    // a comment
    final List<String> sql = new LinkedList<>(super.initialize());
    final String dbNameQ = quoteId(dbDriver.getOption("database"));

    sql.addAll(comment("All sorts of things won't work properly without InnoDB tables:"));
    sql.add("SET SESSION sql_mode='NO_ENGINE_SUBSTITUTION'" + $);

    // If we're working on a live connection, don't try this drop/create
    // stuff: on
    // almost any secured MySQL setup, it won't work anyway. This means that
    // to
    // work on a live db, we need the user to first create the db
    // and grant permissions to use it.
    if (mode != Mode.DATABASE) {
      if (optionEnabled("createDatabase")) {
        if (optionEnabled("dropDatabase")) {
          sql.add("DROP DATABASE IF EXISTS " + dbNameQ + $);
        }

        sql.add("CREATE DATABASE IF NOT EXISTS " + dbNameQ + $);
      }

      sql.add("USE " + dbNameQ + $);
    }

    return sql;
  }

  /**
   *
   *
   * @param table
   * @param includeRefs
   *
   * @return
   */
  @Override
  public List<String> createTable(final EntityNode table, final boolean includeRefs) {
    final StringBuilder tableDef = new StringBuilder("CREATE TABLE ");

    tableDef.append(quoteId(table)).append(" (").append(lineSep);
    tableDef.append('\t').append(quoteId(tablePK(table))).append(' ').append(pkType())
        .append(" PRIMARY KEY AUTO_INCREMENT");

    for (final EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> attr : table
        .getEntityAttributes()) {
      // FIXME -- what about NOT NULL, etc. on the column? Defaults?
      tableDef.append(',').append(lineSep).append('\t').append(quoteId(attr.getName())).append(' ')
          .append(dbDriver.getTypeString(attr.getType()));
    }

    // Include the foreign key columns, and the actual foreign keys (if
    // includeRefs set)
    for (final SketchEdge edge : table.getOutgoingEdges()) {
      tableDef.append(',').append(lineSep).append('\t').append(quoteId(tableFK(edge))).append(' ')
          .append(pkType());

      if (!edge.isPartial()) {
        tableDef.append(" NOT NULL");
      }

      if (edge.isInjective()) {
        tableDef.append(" UNIQUE");
      }

      if (includeRefs) {
        tableDef.append(',').append(lineSep).append("   FOREIGN KEY (").append(quoteId(tableFK(edge)))
            .append(") REFERENCES ").append(quoteId(edge.getTargetEntity())).append(" (")
            .append(quoteId(tablePK(edge.getTargetEntity()))).append(')');

        final String refMode = (edge.getCascading() == SketchEdge.Cascade.SET_NULL) ? "SET NULL"
            : (edge.getCascading() == SketchEdge.Cascade.CASCADE) ? "CASCADE" : "NO ACTION"; // Effectively
                                                      // the
                                                      // same
                                                      // as
                                                      // RESTRICT,
                                                      // but
                                                      // RESTRICT
                                                      // won't
                                                      // even
                                                      // work
                                                      // if
                                                      // cleaned
                                                      // up
                                                      // in
                                                      // the
                                                      // same
                                                      // transaction

        tableDef.append(" ON DELETE ").append(refMode).append(" ON UPDATE ").append(refMode);
      }

    }

    // Add the unique keys
    for (final UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> key : table
        .getUniqueKeys()) {
      tableDef.append(',').append(lineSep).append("       UNIQUE ")
          .append(quoteId(table.getName() + '_' + key.getKeyName())).append(" (");

      final List<String> keyCols = new LinkedList<>();

      for (final UniqueIndexable elem : key.getElements()) {
        if (elem instanceof SketchEdge) {
          keyCols.add(quoteId(tableFK((SketchEdge) elem)));
        } else { // Probably an attribute
          keyCols.add(quoteId(elem.getName()));
        }
      }

      tableDef.append(EasikTools.join(", ", keyCols)).append(')');
    }

    // add hidden attributes last
    for (final EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> attr : table
        .getHiddenEntityAttributes()) {
      // Default is false so that when user adds rows manually we don't
      // give option for hidden attributes
      // and just leave them false
      tableDef.append(',').append(lineSep).append('\t').append(quoteId(attr.getName())).append(' ')
          .append(dbDriver.getTypeString(attr.getType())).append(" Default " + false);
    }

    // commented out by Sarah van der Laan -- caused error in generated SQL
    // file (invalid foreign keys)
    /**
     * if (includeRefs) { // We need a copy of any sum tables' not-null columns
     * (which are simply the // outgoing, non-partial edges) if this table is a
     * summand; we shadow the // columns here, then handle propagating them in the
     * SumConstraint triggers. for (final SketchEdge edge : table.getShadowEdges())
     * {
     * tableDef.append(',').append(lineSep).append('\t').append(quoteId(tableFK(edge))).append('
     * ').append(pkType()); tableDef.append(',').append(lineSep).append(" FOREIGN
     * KEY (").append(quoteId(tableFK(edge))).append(") REFERENCES ").append(
     * quoteId(edge.getTargetEntity())).append("
     * (").append(quoteId(tablePK(edge.getTargetEntity()))).append( ") ON DELETE SET
     * NULL ON UPDATE SET NULL"); } }
     */

    tableDef.append(lineSep).append(')').append(TABLE_OPTIONS).append($);

    return Collections.singletonList(tableDef.toString());
  }

  /**
   *
   *
   * @param table
   *
   * @return
   */
  @Override
  public List<String> createReferences(final EntityNode table) {
    final List<String> refs = new LinkedList<>();
    final String tableQ = quoteId(table);

    for (final SketchEdge edge : table.getOutgoingEdges()) {
      final EntityNode target = edge.getTargetEntity();
      final String refMode = (edge.getCascading() == SketchEdge.Cascade.SET_NULL) ? "SET NULL"
          : (edge.getCascading() == SketchEdge.Cascade.CASCADE) ? "CASCADE" : "NO ACTION"; // No
                                                    // different
                                                    // from
                                                    // RESTRICT
                                                    // under
                                                    // MySQL
                                                    // (in
                                                    // theory,
                                                    // RESTRICT
                                                    // shouldn't
                                                    // work
                                                    // even
                                                    // if
                                                    // cleaned
                                                    // up
                                                    // in
                                                    // the
                                                    // same
                                                    // transaction,
                                                    // while
                                                    // NO
                                                    // ACTION
                                                    // should)

      refs.add("ALTER TABLE " + tableQ + " ADD FOREIGN KEY (" + quoteId(tableFK(edge)) + ") " + "REFERENCES "
          + quoteId(target) + " (" + quoteId(tablePK(target)) + ") " + "ON DELETE " + refMode + " ON UPDATE "
          + refMode + $);
    }

    // commented out by Sarah van der Laan -- caused error in generated SQL
    // file (invalid foreign keys)
    /**
     * // We need a copy of any sum tables' not-null columns (which are simply the
     * // outgoing, non-partial edges) if this table is a summand; we shadow the //
     * columns here, then handle propagating them in the SumConstraint triggers. for
     * (final SketchEdge edge : table.getShadowEdges()) { refs.add("ALTER TABLE " +
     * tableQ + " ADD COLUMN " + quoteId(tableFK(edge)) + ' ' + pkType());
     * refs.add("ALTER TABLE " + tableQ + " ADD FOREIGN KEY (" +
     * quoteId(tableFK(edge)) + ") REFERENCES " + quoteId(edge.getTargetEntity()) +
     * " (" + quoteId(tablePK(edge.getTargetEntity())) + ") " + "ON DELETE SET NULL
     * ON UPDATE SET NULL"); }
     */
    return refs;

  }

  /**
   *
   *
   * @param cd
   * @param id
   *
   * @return
   */
  @Override
  public List<String> createConstraint(
      final CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> cd,
      final String id) {
    final List<String> sql = new LinkedList<>();
    final List<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = cd.getPaths();
    final EntityNode dom = paths.get(0).getDomain();
    final StringBuilder proc = new StringBuilder("");
    final List<String> declarations = new LinkedList<>();
    final List<String> values = new LinkedList<>();
    final List<String> args = new LinkedList<>();
    final List<String> params = new LinkedList<>();
    int targetNum = 0;

    for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : paths) {
      ++targetNum;

      final LinkedList<SketchEdge> tmpPath = new LinkedList<>(path.getEdges());

      tmpPath.removeFirst();

      final String fk = "_path" + targetNum + "fk";

      args.add(fk + ' ' + pkType());
      params.add("NEW." + quoteId(tableFK(path.getFirstEdge())));
      declarations.add("_cdTarget" + targetNum);

      if (tmpPath.size() == 0) {
        values.add("    SELECT " + fk + " INTO _cdTarget" + targetNum + ';' + lineSep);
      } else {
        values.add("    SELECT " + qualifiedFK(path.getLastEdge()) + " INTO _cdTarget" + targetNum + " FROM "
            + joinPath(tmpPath, false) + " WHERE " + qualifiedPK(tmpPath.getFirst().getSourceEntity())
            + " = " + fk + ';' + lineSep);
      }
    }

    proc.append("CREATE PROCEDURE ").append(quoteId("commutativeDiagram" + id)).append('(')
        .append(EasikTools.join(", ", args)).append(") BEGIN").append(lineSep).append("       DECLARE ")
        .append(EasikTools.join(", ", declarations)).append(' ').append(pkType()).append(';').append(lineSep)
        .append(EasikTools.join("", values)).append("       IF").append(lineSep);

    for (int i = 2; i <= targetNum; i++) {
      proc.append("               NOT (_cdTarget1 <=> _cdTarget").append(i).append(')');

      if (i < targetNum) {
        proc.append(" OR");
      }

      proc.append(lineSep);
    }

    proc.append("       THEN CALL constraint_failure('Commutative diagram constraint failure.');").append(lineSep)
        .append("       END IF;").append(lineSep).append("END");

    addFail = true;

    sql.addAll(delimit("$$", proc));
    addTrigger(dom, "BEFORE INSERT",
        "CALL " + quoteId("commutativeDiagram" + id) + '(' + EasikTools.join(", ", params) + ')');

    return sql;
  }

  /**
   *
   *
   * @param constraint
   * @param id
   *
   * @return
   */
  @Override
  public List<String> createConstraint(
      final ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint,
      final String id) {
    final List<String> sql = new LinkedList<>();
    final String delConName = "productConstraint" + id + "Delete";
    StringBuilder proc = new StringBuilder(500);

    proc.append("CREATE PROCEDURE ").append(quoteId(delConName)).append("(id ").append(pkType()).append(") BEGIN")
        .append(lineSep);

    EntityNode begin = null;

    int j = 0;
    // for every path
    // Delete <p.getCodDomain> FROM <a JOIN b ON b.a_id = a.id JOIN c ON
    // c.b_id = b.id JOIN d ON d.c_id = c.id...> WHERE <begin> = id;
    for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p : constraint.getPaths()) {
      if (begin == null) {
        begin = p.getDomain();
      }

      proc.append("   DELETE ").append(quoteId(p.getCoDomain())).append(" FROM ").append(joinPath(p))
          .append(lineSep).append("           WHERE ").append(qualifiedPK(begin)).append(" = id;")
          .append(lineSep);

      // Federico Mora
      StringBuilder proc1 = new StringBuilder(500);
      StringBuilder body = new StringBuilder(50);

      proc1.append("CREATE PROCEDURE ").append("Update" + constraint.getID() + "Proj" + j++).append("() BEGIN")
          .append(lineSep);
      for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> q : constraint
          .getPaths()) {
        if (p == q) {
          // do nothing, cascade will take care of it
        } else {
          // making the update string which is used by all the delete
          // procedures.
          final LinkedList<SketchEdge> edges = q.getEdges();
          if (edges.size() >= 2) {
            Iterator<SketchEdge> iter = edges.iterator();
            SketchEdge e = iter.next();
            body.append("   UPDATE ").append(leftJoinPath(q)).append(lineSep);
            body.append("   SET");
            while (iter.hasNext()) {
              SketchEdge ske = iter.next();
              body.append(" " + quoteId(ske.getSourceEntity()) + ".BC" + constraint.getID() + " = "
                  + false + ",");
            }
            body.delete(body.length() - 1, body.length());
            body.append(" WHERE " + qualifiedFK(e) + " IS NULL;" + lineSep);
          }
        }
      }

      if (body.length() > 0) {
        proc1.append(body);
        proc1.append("END");
        sql.add(proc1.toString());
        // Now create the triggers to call the new procedure
        addTrigger(p.getCoDomain(), "AFTER DELETE",
            "CALL " + quoteId("Update" + constraint.getID() + "Proj" + (j - 1)) + "()");
      }
      // end Federico Mora
    }
    // Select B.id From A Join B On A.f1 = B.id Join D On A.f3 = D.id Where
    // D.id = 1

    proc.append("END");
    sql.add(proc.toString());

    // Now create the trigger to call the new procedure
    addTrigger(begin, "BEFORE DELETE", "CALL " + quoteId(delConName) + "(OLD." + quoteId(tablePK(begin)) + ')');

    for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p : constraint.getPaths()) {
      final EntityNode dest = p.getCoDomain();
      final String conName = "productConstraint" + id + "Insert" + cleanId(dest);
      final Collection<String> args = new LinkedList<>();
      final Collection<String> params = new LinkedList<>();

      args.add("id " + pkType());
      params.add("NEW." + quoteId(tablePK(dest)));

      // commented out by Sarah van der Laan -- caused error in generated
      // SQL file (invalid foreign keys)
      /**
       * for (final SketchEdge shadow : dest.getShadowEdges()) {
       * args.add(quoteId("NEW_shadow_" + tableFK(shadow)) + ' ' + pkType());
       * params.add("NEW." + quoteId(tableFK(shadow))); }
       */

      proc = new StringBuilder(500);

      proc.append("CREATE PROCEDURE ").append(quoteId(conName)).append('(').append(EasikTools.join(", ", args))
          .append(") BEGIN").append(lineSep).append(" DECLARE _lastId ").append(pkType()).append(';')
          .append(lineSep);

      final StringBuilder createIntermediates = new StringBuilder(250);
      final Collection<String> clauses = new LinkedList<>();

      for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> q : constraint
          .getPaths()) {
        if (p == q) {
          continue;
        }

        // Older versions were doing "SELECT COUNT(*) FROM TABLE", but
        // that has a big
        // performance hit for very large, busy InnoDB tables, when all
        // we really care
        // about is non-empty:
        clauses.add("(SELECT 1 FROM " + quoteId(q.getCoDomain()) + " LIMIT 1) = 1");

        // If we end up putting anything in createIntermediate, we'll
        // use these
        // insertions when we insert the first row into one of the path
        // targets
        final LinkedList<SketchEdge> sketchEdgeLinkedList = q.getEdges();
        final SketchEdge[] edges = sketchEdgeLinkedList.toArray(new SketchEdge[sketchEdgeLinkedList.size()]);

        for (int i = edges.length - 1; i > 0; i--) {
          final SketchEdge e = edges[i];
          final EntityNode source = e.getSourceEntity();
          final EntityNode target = e.getTargetEntity();

          // after source there used to be dest.getShadowEdges();
          createIntermediates.append("                        ")
              .append(insertInto(true, source, qualifiedPK(target), quoteId(target),
                  Collections.singletonList(quoteId(tableFK(e))), null));
        }
      }

      // In words: If the tables forming the domains of the other paths
      // contain items.
      proc.append("       IF ").append(EasikTools.join(" AND ", clauses)).append(" THEN").append(lineSep);

      if (createIntermediates.length() > 0) {
        // If we just inserted the first row, we're going to have to
        // build a path of intermediate tables
        // for the other paths, which we built in createIntermediate.
        proc.append("           IF (SELECT COUNT(*) FROM (SELECT 1 FROM ").append(quoteId(dest))
            .append(" LIMIT 2) a) = 1 THEN").append(lineSep).append(createIntermediates)
            .append("               END IF;").append(lineSep).append("").append(lineSep);
      }

      // Produce the intermediate path insertion strings
      final LinkedList<SketchEdge> sketchEdgeLinkedList = p.getEdges();
      final SketchEdge[] edges = sketchEdgeLinkedList.toArray(new SketchEdge[sketchEdgeLinkedList.size()]);

      if (edges.length > 1) {
        for (int i = edges.length - 1; i > 0; i--) {
          final SketchEdge e = edges[i];
          final EntityNode source = e.getSourceEntity();
          @SuppressWarnings("unused")
          final EntityNode target = e.getTargetEntity();

          // after source there used to be dest.getShadowEdges();
          proc.append("               ")
              .append(insertInto(true, source, null, null, Collections.singletonList(quoteId(tableFK(e))),
                  Collections.singletonList((i == edges.length - 1) ? "id" : "LAST_INSERT_ID()")));
        }

        proc.append("           SET _lastId = LAST_INSERT_ID();").append(lineSep);
      } else {
        proc.append("           SET _lastId = id;").append(lineSep);
      }

      // Now the proper insertion
      final List<String> columns = new LinkedList<>();
      final Collection<String> values = new LinkedList<>();
      final Collection<String> from = new LinkedList<>();
      EntityNode thisTarget = null;

      for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> q : constraint
          .getPaths()) {
        final EntityNode target = q.getFirstEdge().getTargetEntity();

        columns.add(quoteId(tableFK(q.getFirstEdge())));
        values.add(qualifiedPK(target));
        from.add(quoteId(target));

        if (q == p) {
          thisTarget = target;
        }
      }
      // after begin there used to be dest.getShadowEdges();
      proc.append("               ")
          .append(insertInto(true, begin, EasikTools.join(", ", values),
              EasikTools.join(" CROSS JOIN ", from) + " WHERE " + qualifiedPK(thisTarget) + " = _lastId",
              columns, null))
          .append("   END IF;").append(lineSep).append("END");
      sql.add(proc.toString());

      addTrigger(dest, "AFTER INSERT", "CALL " + quoteId(conName) + '(' + EasikTools.join(", ", params) + ')');
    }

    return delimit("$$", sql);
  }

  /**
   *
   *
   * @param constraint
   * @param id
   *
   * @return
   */
  @Override
  public List<String> createConstraint(
      final PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint,
      final String id) {
    final List<String> sql = new LinkedList<>();
    String tmpS = null;

    // Create the trigger for inserting into the pullback table:
    final EntityNode pb = constraint.getSource();
    final EntityNode target = constraint.getTarget();
    final String BASE_PATH_NAME = "_pathId";
    final int constraintWidth = constraint.getWidth();
    final String insConName = "pullbackConstraint" + id + "Insert" + cleanId(pb);
    StringBuilder proc = new StringBuilder(500);

    proc.append("CREATE PROCEDURE ");
    proc.append(quoteId(insConName));
    proc.append("(_newPBid ");
    proc.append(pkType());
    proc.append(") BEGIN");
    proc.append(lineSep);
    proc.append("   DECLARE ");

    for (int i = 0; i < constraintWidth; i++) {
      proc.append(BASE_PATH_NAME);
      proc.append(i);

      if (i != constraintWidth - 1) {
        proc.append(", ");
      } else {
        proc.append(" ");
      }
    }

    proc.append(pkType()).append(';').append(lineSep);

    final String basePK = qualifiedPK(pb);
    final String targetPK = qualifiedPK(target);

    for (int i = 0; i < constraintWidth; i++) {
      proc.append("       SELECT ").append(targetPK).append(" INTO " + BASE_PATH_NAME + i).append(lineSep)
          .append("           FROM ").append(joinPath(constraint.getFullPath(i))).append(lineSep)
          .append("           WHERE ").append(basePK).append(" = _newPBid;").append(lineSep);
    }

    // Make sure the paths match
    proc.append("   IF NOT");

    for (int i = 1; i < constraintWidth; i++) {
      proc.append(" (" + BASE_PATH_NAME + (i - 1) + " <=> " + BASE_PATH_NAME + i + ')');

      if (i != constraintWidth - 1) {
        proc.append(" OR NOT");
      }
    }

    proc.append(" THEN").append(lineSep)
        .append("               CALL constraint_failure('Invalid entry in pullback constraint.');")
        .append(lineSep).append("       END IF;").append(lineSep);

    addFail = true;

    final String pbQ = quoteId(pb);
    SketchEdge[] firstEdges = new SketchEdge[constraintWidth];

    for (int i = 0; i < constraintWidth; i++) {
      firstEdges[i] = constraint.getFullPath(i).getFirstEdge();
    }

    if (!addedUnique.isUnique(pb, firstEdges)) {
      tmpS = "ALTER TABLE " + pbQ + " ADD UNIQUE " + quoteId("pullbackConstraint" + id + "UniqueIndex") + " (";

      for (int i = 0; i < constraintWidth; i++) {
        tmpS += quoteId(tableFK(firstEdges[i]));

        if (i != constraintWidth - 1) {
          tmpS += ", ";
        }
      }

      tmpS += ')' + $;

      sql.add(tmpS);
      addedUnique.add(pb, firstEdges);
    }

    proc.append("END");
    sql.add(proc.toString());
    addTrigger(pb, "AFTER INSERT", "CALL " + quoteId(insConName) + "(NEW." + quoteId(tablePK(pb)) + ')');

    for (int i = 0; i < constraintWidth; i++) {
      ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> tar = constraint.getTargetPath(i);
      ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> pro = constraint
          .getProjectionPath(i);

      pbAfterInsertProcs(sql, id, constraint, tar, pro, i);
    }

    // Finally, the before-delete on the pullback:
    final String delConName = "pullbackConstraint" + id + "Delete" + cleanId(pb);

    proc = new StringBuilder(500);

    proc.append("CREATE PROCEDURE ").append(quoteId(delConName)).append("(toDeleteId ").append(pkType())
        .append(") BEGIN").append(lineSep);
    proc.append("   DELETE ").append(quoteId(target)).append(" FROM ").append(pbQ).append(" JOIN ").append('(');

    for (int i = 0; i < constraintWidth; i++) {
      LinkedList<SketchEdge> tmpPath = new LinkedList<>(constraint.getFullPath(i).getEdges());
      SketchEdge firstEdge = tmpPath.removeFirst();
      @SuppressWarnings("unused")
      SketchEdge lastEdge = tmpPath.getLast();
      @SuppressWarnings("unused")
      SketchEdge secondEdge = tmpPath.removeFirst();
      EntityNode first = firstEdge.getTargetEntity();

      proc.append(quoteId(first));

      if (i != constraintWidth - 1) {
        proc.append(", ");
      }
    }

    proc.append(") ON ");

    for (int i = 0; i < constraintWidth; i++) {
      LinkedList<SketchEdge> tmpPath = new LinkedList<>(constraint.getFullPath(i).getEdges());
      SketchEdge firstEdge = tmpPath.removeFirst();
      @SuppressWarnings("unused")
      SketchEdge lastEdge = tmpPath.getLast();
      @SuppressWarnings("unused")
      SketchEdge secondEdge = tmpPath.removeFirst();
      EntityNode first = firstEdge.getTargetEntity();

      proc.append(qualifiedFK(firstEdge)).append(" = ").append(qualifiedPK(first));

      if (i != constraintWidth - 1) {
        proc.append(" AND ");
      }
    }

    for (int i = 0; i < constraintWidth; i++) {
      LinkedList<SketchEdge> tmpPath = new LinkedList<>(constraint.getFullPath(i).getEdges());
      SketchEdge firstEdge = tmpPath.removeFirst();
      @SuppressWarnings("unused")
      SketchEdge lastEdge = tmpPath.getLast();
      SketchEdge secondEdge = tmpPath.removeFirst();
      @SuppressWarnings("unused")
      EntityNode first = firstEdge.getTargetEntity();

      if (tmpPath.size() > 0) {
        proc.append(" JOIN ").append(joinPath(tmpPath, false,
            qualifiedFK(secondEdge) + " = " + qualifiedPK(secondEdge.getTargetEntity())));
      }
    }

    proc.append(" JOIN ").append(quoteId(target)).append(" ON ");

    for (int i = 0; i < constraintWidth; i++) {
      LinkedList<SketchEdge> tmpPath = new LinkedList<>(constraint.getFullPath(i).getEdges());
      SketchEdge firstEdge = tmpPath.removeFirst();
      SketchEdge lastEdge = tmpPath.getLast();
      @SuppressWarnings("unused")
      SketchEdge secondEdge = tmpPath.removeFirst();
      @SuppressWarnings("unused")
      EntityNode first = firstEdge.getTargetEntity();

      proc.append(qualifiedFK(lastEdge)).append(" = ").append(qualifiedPK(target));

      if (i != constraintWidth - 1) {
        proc.append(" AND ");
      }
    }

    proc.append(lineSep).append("           WHERE ").append(qualifiedPK(pb)).append(" = toDeleteId;")
        .append(lineSep).append("END");
    sql.add(proc.toString());
    addTrigger(pb, "BEFORE DELETE", "CALL " + quoteId(delConName) + "(OLD." + quoteId(tablePK(pb)) + ')');

    // Federico Mora
    int j = 0;
    // for every path
    // Delete <p.getCodDomain> FROM <a JOIN b ON b.a_id = a.id JOIN c ON
    // c.b_id = b.id JOIN d ON d.c_id = c.id...> WHERE <begin> = id;
    for (int i = 0; i < constraintWidth; i++) {
      ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p = constraint
          .getProjectionPath(i);
      StringBuilder proc1 = new StringBuilder(500);
      StringBuilder body = new StringBuilder(50);

      proc1.append("CREATE PROCEDURE ").append("Update" + constraint.getID() + "Proj" + j++).append("() BEGIN")
          .append(lineSep);
      for (int k = 0; k < constraintWidth; k++) {
        ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> q = constraint
            .getProjectionPath(k);
        if (p == q) {
          // do nothing, cascade will take care of it
        } else {
          // making the update string which is used by all the delete
          // procedures.
          final LinkedList<SketchEdge> edges = q.getEdges();
          if (edges.size() >= 2) {
            Iterator<SketchEdge> iter = edges.iterator();
            SketchEdge e = iter.next();
            body.append("   UPDATE ").append(leftJoinPath(q)).append(lineSep);
            body.append("   SET");
            while (iter.hasNext()) {
              SketchEdge ske = iter.next();
              body.append(" " + quoteId(ske.getSourceEntity()) + ".BC" + constraint.getID() + " = "
                  + false + ",");
            }
            body.delete(body.length() - 1, body.length());
            body.append(" WHERE " + qualifiedFK(e) + " IS NULL;" + lineSep);
          }
        }
      }

      if (body.length() > 0) {
        proc1.append(body);
        proc1.append("END");
        sql.add(proc1.toString());
        // Now create the triggers to call the new procedure
        addTrigger(p.getCoDomain(), "AFTER DELETE",
            "CALL Update" + constraint.getID() + "Proj" + (j - 1) + "()");
        addTrigger(p.getCoDomain(), "AFTER UPDATE",
            "CALL Update" + constraint.getID() + "Proj" + (j - 1) + "()");
      }
    }
    // end Federico Mora

    return delimit("$$", sql);
  }

  /**
   *
   *
   * @param sql
   * @param id
   * @param constraint
   * @param thisPath
   * @param thisFirstPath
   * @param index
   */
  private void pbAfterInsertProcs(final List<String> sql, final String id,
      final PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint,
      final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> thisPath,
      final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> thisFirstPath, int index) {
    final EntityNode source = thisPath.getDomain();
    final EntityNode pb = thisFirstPath.getDomain();
    final EntityNode target = thisPath.getCoDomain();
    final String insConName = "pullbackConstraint" + id + "Insert" + cleanId(source);
    final Collection<String> args = new LinkedList<>();
    final Collection<String> params = new LinkedList<>();

    args.add("_newId " + pkType());
    params.add("NEW." + quoteId(tablePK(source)));

    // commented out by Sarah van der Laan -- caused error in generated SQL
    // file (invalid foreign keys)
    /**
     * for (final SketchEdge shadow : source.getShadowEdges()) {
     * args.add(quoteId("NEW_shadow_" + tableFK(shadow)) + ' ' + pkType());
     * params.add("NEW." + quoteId(tableFK(shadow))); }
     */

    final StringBuilder proc = new StringBuilder(500);

    proc.append("CREATE PROCEDURE ").append(quoteId(insConName)).append('(').append(EasikTools.join(", ", args))
        .append(") BEGIN").append(lineSep);

    // trigger body
    final String thisJoinPath = joinPath(thisPath, false); // omitting last
                                // edge
    String lastJoinOn = "JOIN " + quoteId(target) + " ON " + qualifiedFK(thisPath.getLastEdge()) + " = "
        + qualifiedPK(target);

    for (int i = 0; i < constraint.getWidth(); i++) {
      if (i == index) {
        continue;
      }

      ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> otherPath = constraint
          .getTargetPath(i);

      lastJoinOn += " AND ";
      lastJoinOn += qualifiedFK(otherPath.getLastEdge()) + " = " + qualifiedPK(target);
    }

    proc.append("\tIF (SELECT COUNT(*) FROM (").append(thisJoinPath);

    for (int i = 0; i < constraint.getWidth(); i++) {
      if (i == index) {
        continue;
      }

      ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> otherPath = constraint
          .getTargetPath(i);

      proc.append(", ").append(joinPath(otherPath, false));
    }

    proc.append(") ").append(lastJoinOn).append(" WHERE ").append(qualifiedPK(source)).append(" = _newId) > 0 THEN")
        .append(lineSep);

    // constraint condition met
    SketchEdge[] edges;

    for (int i = 0; i < constraint.getWidth(); i++) {
      if (i == index) {
        continue;
      }

      final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> otherPath = constraint
          .getTargetPath(i);
      final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> otherFirstPath = constraint
          .getProjectionPath(i);
      final LinkedList<SketchEdge> sketchEdgeLinkedList = otherFirstPath.getEdges();

      edges = sketchEdgeLinkedList.toArray(new SketchEdge[sketchEdgeLinkedList.size()]);

      if (edges.length >= 2) {
        final String joinOn = "JOIN " + quoteId(target) + " ON " + qualifiedFK(thisPath.getLastEdge()) + " = "
            + qualifiedPK(target) + " AND " + qualifiedFK(otherPath.getLastEdge()) + " = "
            + qualifiedPK(target);

        // proc.append("\t\tIF (SELECT COUNT(*) FROM
        // (").append(joinPath(sketchEdgeLinkedList, true));
        proc.append("\t\tIF (SELECT COUNT(*) FROM (")
            .append(joinPath(constraint.getFullPath(i).getEdges(), false));

        // TODO might have the same bug if the target path is longer
        // than 1, check this
        proc.append(", ").append(thisJoinPath).append(") ").append(joinOn).append(" WHERE ")
            .append(qualifiedPK(source)).append(" = _newId) = 0 THEN").append(lineSep);

        final LinkedList<SketchEdge> partialPath = new LinkedList<>(otherPath.getEdges());

        for (int j = edges.length - 1; j > 0; j--) {
          final EntityNode s = edges[j].getSourceEntity();
          final EntityNode t = edges[j].getTargetEntity();

          // after s there used to be dest.getShadowEdges();
          proc.append("\t\t\t").append(insertInto(true, s, qualifiedPK(t), // SELECT
                                            // this
                                            // FROM:
              '(' + joinPath(partialPath, false) + ", " + thisJoinPath + ") " + joinOn + " WHERE "
                  + qualifiedPK(source) + " = _newId",
              Collections.singletonList(quoteId(tableFK(edges[j]))), null));
          partialPath.addFirst(edges[j]);
        }

        proc.append("\t\tEND IF;").append(lineSep);
      }
    }

    final LinkedList<SketchEdge> firstPathEdges = thisFirstPath.getEdges();

    edges = firstPathEdges.toArray(new SketchEdge[firstPathEdges.size()]);

    final LinkedList<SketchEdge> partialPath = new LinkedList<>(thisPath.getEdges());

    for (int i = edges.length - 1; i > 0; i--) {
      final SketchEdge e = edges[i];
      final EntityNode s = e.getSourceEntity();
      final EntityNode t = e.getTargetEntity();

      // after s there used to be dest.getShadowEdges();
      proc.append("\t\t")
          .append(insertInto(true, s, qualifiedPK(t),
              joinPath(partialPath) + " WHERE " + qualifiedPK(source) + " = _newId",
              Collections.singletonList(quoteId(tableFK(e))), null));
      partialPath.addFirst(e);
    }

    final EntityNode firstStop = thisFirstPath.getFirstCoDomain();
    final List<String> cols = new LinkedList<>();

    cols.add(quoteId(tableFK(thisFirstPath.getFirstEdge())));

    String tmpStr1 = qualifiedPK(firstStop);
    LinkedList<SketchEdge> tmpPath = new LinkedList<>(constraint.getFullPath(0).getEdges());

    tmpPath.removeFirst();

    String tmpStr2 = '(' + joinPath(tmpPath, false);

    for (int i = 0; i < constraint.getWidth(); i++) {
      final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> otherFirstPath = constraint
          .getProjectionPath(i);
      final EntityNode otherFirstStop = otherFirstPath.getFirstCoDomain();

      if (otherFirstPath != thisFirstPath) {
        tmpStr1 += ", " + qualifiedPK(otherFirstStop);

        cols.add(quoteId(tableFK(otherFirstPath.getFirstEdge())));
      }

      if (i > 0) {
        tmpPath = new LinkedList<>(constraint.getFullPath(i).getEdges());

        tmpPath.removeFirst();

        tmpStr2 += ", " + joinPath(tmpPath, false);
      }
    }

    // after pb there used to be dest.getShadowEdges();
    proc.append("\t\t").append(insertInto(true, pb, tmpStr1,
        tmpStr2 + ") " + lastJoinOn + " WHERE " + qualifiedPK(source) + " = _newId", cols, null));

    // end constraint condition met
    proc.append("\tEND IF;");

    // end trigger body
    proc.append(lineSep).append("END");
    sql.add(proc.toString());

    // Now create the trigger to call the new procedure
    addTrigger(source, "AFTER INSERT", "CALL " + quoteId(insConName) + '(' + EasikTools.join(", ", params) + ')');
  }

  /**
   *
   *
   * @param constraint
   * @param id
   *
   * @return
   */
  @Override
  public List<String> createConstraint(
      final EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint,
      final String id) {
    final List<String> sql = new LinkedList<>();
    final EntityNode eq = constraint.getEqualizerEntity();
    final EntityNode source = constraint.getSourceEntity();
    final EntityNode target = constraint.getTargetEntity();
    final InjectiveEdge projEdge = (InjectiveEdge) constraint.getProjection().getFirstEdge();
    final List<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = constraint
        .getEqualizerPaths();
    final String insConName = "eqCon" + id + "InsUpd" + cleanId(source);
    final Collection<String> args = new LinkedList<>();
    final Collection<String> params = new LinkedList<>();

    args.add("_newId " + pkType());
    params.add("NEW." + quoteId(tablePK(source)));

    // commented out by Sarah van der Laan -- caused error in generated SQL
    // file (invalid foreign keys)
    /**
     * for (final SketchEdge shadow : source.getShadowEdges()) {
     * args.add(quoteId("NEW_shadow_" + tableFK(shadow)) + ' ' + pkType());
     * params.add("NEW." + quoteId(tableFK(shadow))); }
     */

    final StringBuilder proc = new StringBuilder(500);

    proc.append("CREATE PROCEDURE ").append(quoteId(insConName)).append('(').append(EasikTools.join(", ", args))
        .append(") BEGIN").append(lineSep);
    proc.append("   DECLARE _path0Id");

    // Add a variable for each path, _path0Id, _path1Id, etc.:
    for (int i = 1; i < paths.size(); i++) {
      proc.append(", _path").append(i).append("Id");
    }

    proc.append(' ').append(pkType()).append(';').append(lineSep);

    final String sourcePK = qualifiedPK(source);
    final String targetPK = qualifiedPK(target);
    final String newPK = "_newId";
    int i = 0;

    for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p : paths) {
      proc.append("   SELECT ").append(targetPK).append(" INTO _path").append(i).append("Id").append(lineSep)
          .append("           FROM ").append(joinPath(p)).append(lineSep).append("           WHERE ")
          .append(sourcePK).append(" = ").append(newPK).append(';').append(lineSep);

      ++i;
    }

    // Build equality clause, such as: _path0Id IS DISTINCT FROM _path1Id OR
    // _path0Id IS DISTINCT FROM _path2Id OR ...
    proc.append("   IF NOT (_path0Id <=> _path1Id)");

    for (int j = 2; j < paths.size(); j++) {
      proc.append(" OR NOT (_path0Id <=> _path").append(j).append("Id)");
    }

    proc.append(" THEN").append(lineSep).append(

        // If one of the paths doesn't match, we need to need to
        // delete it from the equalizer (if it's there)
        "               DELETE FROM ").append(quoteId(eq)).append(" WHERE ").append(qualifiedFK(projEdge))
        .append(" = ").append(newPK).append(';').append(lineSep).append("       ELSEIF (SELECT COUNT(*) FROM ")
        .append(quoteId(eq)).append(" WHERE ").append(qualifiedFK(projEdge)).append(" = ").append(newPK)
        .append(") = 0 THEN").append(lineSep).append(

            // Otherwise, if it isn't already in the equalizer, we
            // need to
            // add it:
            "               ");
    /*
     * Deal with intermediate nodes in projection Federico Mora
     */
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path = constraint.getProjection();
    final SketchEdge[] edges = path.getEdges().toArray(new SketchEdge[path.getEdges().size()]);

    for (int j = edges.length - 1; j > 0; j--) {
      final EntityNode s = edges[j].getSourceEntity();

      // for the first one insert pointing to the new id

      if (j == edges.length - 1) {
        // after s there used to be dest.getShadowEdges();
        proc.append(insertInto(false, s, null, null, Collections.singletonList(quoteId(tableFK(edges[j]))),
            Collections.singletonList("_newId")));
      } else {
        // after source there used to be dest.getShadowEdges();
        proc.append(insertInto(false, s, null, null, Collections.singletonList(quoteId(tableFK(edges[j]))),
            Collections.singletonList("LAST_INSERT_ID()")));
      }
    }

    // done - deal with intermediate nodes in projection
    // after eq there used to be dest.getShadowEdges();
    proc.append("               ")
        .append(insertInto(true, eq, null, null, Collections.singletonList(quoteId(tableFK(projEdge))),
            Collections.singletonList("LAST_INSERT_ID()")))
        .append("       END IF;").append(lineSep).append("END").append(lineSep);
    sql.add(proc.toString());

    // Create the trigger for inserting into the source table:
    final String call = "CALL " + quoteId(insConName) + '(' + EasikTools.join(", ", params) + ')';

    addTrigger(source, "AFTER INSERT", call);
    addTrigger(source, "AFTER UPDATE", call);

    // If the projection isn't a cascading edge, cascade:
    if (projEdge.getCascading() != SketchEdge.Cascade.CASCADE) {
      addTrigger(source, "BEFORE DELETE", "DELETE FROM " + quoteId(eq) + " WHERE " + qualifiedFK(projEdge)
          + " = OLD." + quoteId(tablePK(source)));
    }

    // Federico Mora
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p = constraint.getProjection();
    StringBuilder proc1 = new StringBuilder(500);
    proc1.append("CREATE PROCEDURE ").append("Update" + constraint.getID() + "Proj").append("() BEGIN")
        .append(lineSep);
    // making the update string which is used by all the delete
    // procedures.
    final LinkedList<SketchEdge> egs = p.getEdges();
    if (egs.size() >= 2) {
      Iterator<SketchEdge> iter = egs.iterator();
      SketchEdge e = iter.next();
      proc1.append("   UPDATE ").append(leftJoinPath(p)).append(lineSep);
      proc1.append("   SET");
      while (iter.hasNext()) {
        SketchEdge ske = iter.next();
        proc1.append(" " + quoteId(ske.getSourceEntity()) + ".BC" + constraint.getID() + " = " + false + ",");
      }
      proc1.delete(proc1.length() - 1, proc1.length());
      proc1.append(" WHERE " + qualifiedFK(e) + " IS NULL;" + lineSep);
      proc1.append("END");
      sql.add(proc1.toString());
      // Now create the triggers to call the new procedure
      addTrigger(p.getCoDomain(), "AFTER UPDATE",
          "CALL " + quoteId("Update" + constraint.getID() + "Proj") + "()");
    }
    // end Federico Mora

    return delimit("$$", sql);
  }

  /**
   *
   *
   * @param constraint
   * @param id
   *
   * @return
   */
  @Override
  public List<String> createConstraint(
      final SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint,
      final String id) {
    final List<String> sql = new LinkedList<>();

    for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : constraint
        .getPaths()) {
      final EntityNode dom = path.getDomain();
      final EntityNode codo = path.getCoDomain();
      final String baseConName = "sumConstraint" + id + cleanId(dom);
      final StringBuilder proc = new StringBuilder(500);

      proc.append("CREATE PROCEDURE ").append(quoteId(baseConName + "Delete")).append("(_deleteFK ")
          .append(pkType()).append(") BEGIN").append(lineSep).append("   DELETE FROM ").append(quoteId(codo))
          .append(" WHERE ").append(qualifiedPK(codo)).append(" = ");

      if (path.getEdges().size() == 1) {
        proc.append("_deleteFK");
      } else {
        final LinkedList<SketchEdge> trailing = new LinkedList<>(path.getEdges());

        trailing.removeFirst();
        proc.append("(SELECT ").append(qualifiedFK(trailing.getLast())).append(" FROM ")
            .append(joinPath(trailing, false)).append(" WHERE ")
            .append(qualifiedPK(path.getFirstCoDomain())).append(" = _deleteFK)");
      }

      proc.append(';').append(lineSep).append("END");
      sql.add(proc.toString());
      addTrigger(dom, "AFTER DELETE",
          "CALL " + quoteId(baseConName + "Delete") + "(OLD." + quoteId(tableFK(path.getFirstEdge())) + ')');

      final List<String> insertions = new LinkedList<>();

      // after codo there used to be dest.getShadowEdges();
      insertions.add(insertInto(false, codo, null, null, null, null));

      final SketchEdge[] edges = path.getEdges().toArray(new SketchEdge[path.getEdges().size()]);

      for (int i = edges.length - 1; i > 0; i--) {
        final EntityNode source = edges[i].getSourceEntity();

        // after source there used to be dest.getShadowEdges();
        insertions.add(
            insertInto(false, source, null, null, Collections.singletonList(quoteId(tableFK(edges[i]))),
                Collections.singletonList("LAST_INSERT_ID()")));
      }

      insertions.add("SET NEW." + quoteId(tableFK(edges[0])) + " = LAST_INSERT_ID()");
      addTrigger(dom, "BEFORE INSERT", insertions);
    }

    return delimit("$$", sql);
  }

  /**
   *
   *
   * @param constraint
   * @param id
   *
   * @return
   */
  @Override
  public List<String> createConstraint(
      final LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint,
      final String id) {
    final List<String> sql = new LinkedList<>();
    StringBuilder proc = new StringBuilder(500);
    final List<String> declarations = new LinkedList<>();
    final List<String> values = new LinkedList<>();
    final List<String> args = new LinkedList<>();
    final List<String> params = new LinkedList<>();

    // getting the paths involved in this limit constraint
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> coneAB = constraint.getCone().AB;
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> coneBC = constraint.getCone().BC;
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> coneAC = constraint.getCone().AC;

    // TODO this should be in the LimitConstraint
    List<SketchEdge> tmpEdges = coneAB.getEdges();

    tmpEdges.addAll(coneBC.getEdges());

    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> coneABC = new ModelPath<>(tmpEdges);
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> limitConeAB = constraint
        .getLimitCone1().AB;
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> limitCone1BC = constraint
        .getLimitCone1().BC;
    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> limitCone2BC = constraint
        .getLimitCone2().BC;

    tmpEdges = limitConeAB.getEdges();

    tmpEdges.addAll(limitCone1BC.getEdges());

    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> limitCone1ABC = new ModelPath<>(
        tmpEdges);

    tmpEdges = limitConeAB.getEdges();

    tmpEdges.addAll(limitCone2BC.getEdges());

    ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> limitCone2ABC = new ModelPath<>(
        tmpEdges);
    ArrayList<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = new ArrayList<>();

    paths.add(coneABC);
    paths.add(coneAC);
    paths.add(limitCone1ABC);
    paths.add(coneAB);
    paths.add(limitCone2ABC);

    // would add coneAC, but it's already been added
    int targetNum = 0;

    for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : paths) {
      ++targetNum;

      final LinkedList<SketchEdge> tmpPath = new LinkedList<>(path.getEdges());

      tmpPath.removeFirst();

      final String fk = "_path" + targetNum + "fk";

      args.add(fk + ' ' + pkType());
      params.add("NEW." + quoteId(tableFK(path.getFirstEdge())));
      declarations.add("_cdTarget" + targetNum);

      if (tmpPath.size() == 0) {
        values.add("    SELECT " + fk + " INTO _cdTarget" + targetNum + ';' + lineSep);
      } else {
        values.add("    SELECT " + qualifiedFK(path.getLastEdge()) + " INTO _cdTarget" + targetNum + " FROM "
            + joinPath(tmpPath, false) + " WHERE " + qualifiedPK(tmpPath.getFirst().getSourceEntity())
            + " = " + fk + ';' + lineSep);
      }
    }

    proc.append("CREATE PROCEDURE ").append(quoteId("limitConstraint" + id)).append('(')
        .append(EasikTools.join(", ", args)).append(") BEGIN").append(lineSep).append("    DECLARE ")
        .append(EasikTools.join(", ", declarations)).append(' ').append(pkType()).append(';').append(lineSep)
        .append(EasikTools.join("", values)).append("    IF").append(lineSep);

    // cone commutativity check
    proc.append("        NOT (_cdTarget1 <=> _cdTarget2) OR");
    proc.append(lineSep);

    // limitCone1 commutativity check
    proc.append("        NOT (_cdTarget3 <=> _cdTarget4) OR");
    proc.append(lineSep);

    // limitCone2 commutativity check
    proc.append("        NOT (_cdTarget5 <=> _cdTarget2)");
    proc.append(lineSep);
    proc.append("    THEN CALL constraint_failure('Limit constraint failure.');").append(lineSep)
        .append("    END IF;").append(lineSep).append("END");

    addFail = true;

    sql.addAll(delimit("$$", proc));
    // cast because we will only do this in sketches
    addTrigger(constraint.getCone().getA(), "BEFORE INSERT",
        "CALL " + quoteId("limitConstraint" + id) + '(' + EasikTools.join(", ", params) + ')');

    return sql;
  }

  /**
   * Creates various extra statements that we need.
   * <p/>
   * One of these is to create a constraint_failure() procedure, since MySQL has
   * no way to abort a transaction other than performing an invalid query
   * (constraint_failure() attempts an "UPDATE" of "fail = 1" on a table named
   * whatever failure message is passed in; that way at least the error appears in
   * the MySQL error message. This sucks, I know. Blame MySQL.)
   * <p/>
   * FIXME: Right now, MySQL <b>does not support</b> updating the same table a
   * trigger applies to in an AFTER trigger, so we have no way to clear shadow
   * columns. This was presumably a result of MySQL incompetence, or MySQL's
   * philosophy of limiting itself in order to prevent idiot PHP programmers from
   * shooting themselves in the foot with--*GASP*--a recursive trigger. While it
   * would be very nice to clear the shadow columns, we simply can't do so: the
   * values are often needed in AFTER INSERT or AFTER UPDATE triggers, so we can't
   * clear them before then, however the only entry point for clearing these with
   * MySQL is doing a 'SET NEW.col = NULL' inside the BEFORE trigger.
   * <p/>
   * If MySQL some day supports an update on the same table a trigger is applied
   * to, the following will work. Until then, MySQL tables will end up retaining
   * values in the shadow columns. N.B. that these values are actual references to
   * the appropriate tables and columns, but are 'ON DELETE SET NULL ON UPDATE SET
   * NULL', so should not pose data manipulation problems.
   *
   * @param toggleTriggers
   *
   * @return
   */

  // DTRIG CF2012
  @Override
  public List<String> createExtras(boolean toggleTriggers) {
    final List<String> extras = new LinkedList<>();

    if (addFail) {
      // This hack is needed for MySQL to abort an insert if a trigger
      // fails.
      // This is disgusting, but is what MySQL documentation recommends
      // doing.
      extras.addAll(delimit("$$",
          "CREATE PROCEDURE constraint_failure(_message VARCHAR(255)) BEGIN" + lineSep
              + "   -- This update is going to fail: this hack is needed because MySQL" + lineSep
              + "   -- lacks the ability to do an (SQL-standard) SIGNAL from a procedure." + lineSep
              + "   SET @sql = CONCAT('UPDATE `', _message, '` SET fail=1');" + lineSep
              + "   PREPARE constraint_fail_statement_handle FROM @sql;" + lineSep
              + "   EXECUTE contraint_fail_statement_handle;" + lineSep
              + "   DEALLOCATE PREPARE contraint_fail_statement_handle;" + lineSep + "END"));
    }

    final List<StringBuilder> trigs = new LinkedList<>();

    for (final EntityNode table : triggers.keySet()) {
      final LinkedHashMap<String, LinkedList<String>> tableTriggers = triggers.get(table);

      for (final String when : tableTriggers.keySet()) {
        final LinkedList<String> actions = tableTriggers.get(when);
        final StringBuilder commands = new StringBuilder(200);

        commands.append("CREATE TRIGGER ")
            .append(quoteId(table + "_" + when.replaceAll("(\\w)\\w*\\s*", "$1").toLowerCase() + "Trig"))
            .append(' ').append(when).append(" ON ").append(quoteId(table)).append(" FOR EACH ROW")
            .append(lineSep);

        if (toggleTriggers) {
          commands.append("\tIF (@DISABLE_TRIGGER IS NULL) THEN ").append(lineSep);
        }

        if (actions.size() == 1) {
          commands.append(toggleTriggers ? "\t\t" : "\t").append(actions.getFirst()).append(";");
        } else {
          commands.append(toggleTriggers ? "\t\t" : "\t").append("BEGIN").append(lineSep);

          for (final String action : actions) {
            commands.append(toggleTriggers ? "\t\t\t" : "\t\t").append(action.replaceFirst(";\\s*$", ""))
                .append(';').append(lineSep);
          }

          commands.append(toggleTriggers ? "\t\t" : "\t").append("END;");
        }

        if (toggleTriggers) {
          commands.append(lineSep).append("\tEND IF;").append(lineSep);
        }

        trigs.add(commands);
      }
    }

    if (!trigs.isEmpty()) {
      extras.addAll(delimit("$$", trigs));
    }

    return extras;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public List<String> createExtras() {
    return createExtras(false);
  }

  /**
   * Adds a trigger to triggers, which is used in createExtras to add the
   * triggers.
   *
   * @param table  the table the trigger applies to
   * @param when   the trigger time, such as "BEFORE UPDATE", "AFTER DELETE",
   *               "BEFORE INSERT", etc.
   * @param action one or more SQL statements (or array or list of SQL statements)
   *               to execute in the trigger (statements should not end with ; or
   *               $).
   */

  // triggers is a: LinkedHashMap<EntityNode, LinkedHashMap<String,
  // LinkedList<String>>>
  // table when action
  private void addTrigger(final EntityNode table, final String when, final String... action) {
    addTrigger(table, when, Arrays.asList(action));
  }

  // BUGFIX CF2012

  /**
   *
   *
   * @param table
   * @param when
   * @param action
   */
  private void addTrigger(final EntityNode table, final String when, final List<String> action) {
    final String ucWhen = when.toUpperCase();
    final LinkedHashMap<String, LinkedList<String>> tableTriggers;

    if (!triggers.containsKey(table)) {
      triggers.put(table, new LinkedHashMap<String, LinkedList<String>>(10));
    }

    tableTriggers = triggers.get(table);

    final LinkedList<String> actions;

    if (!tableTriggers.containsKey(ucWhen)) {
      tableTriggers.put(ucWhen, new LinkedList<String>());
    }

    actions = tableTriggers.get(ucWhen);

    actions.addAll(action);
  }

  /**
   * Takes statement(s) (typically of a procedure) and, if for a non-db export,
   * adds the delimiter change statement and adds the specified delimiter to the
   * end of each provided statement.
   *
   * @param delimiter  The delimiter
   * @param statements Which statements need it
   * @return a delimited list of strings
   */
  private List<String> delimit(final String delimiter, final List<? extends CharSequence> statements) {
    final List<String> sql = new LinkedList<>();

    if (mode == Mode.DATABASE) {
      for (final CharSequence st : statements) {
        sql.add(st.toString());
      }
    } else {
      sql.add("DELIMITER " + delimiter);

      for (final CharSequence st : statements) {
        sql.add(st + delimiter);
      }

      sql.add("DELIMITER ;");
    }

    return sql;
  }

  /**
   *
   *
   * @param delimiter
   * @param statements
   *
   * @return
   */
  private List<String> delimit(final String delimiter, final CharSequence... statements) {
    return delimit(delimiter, Arrays.asList(statements));
  }

  /**
   *
   *
   * @return
   */
  private String pkType() {
    return optionEnabled("bigKeys") ? "BIGINT" : "INTEGER";
  }

  /**
   * Appends an INSERT INTO statement to the provided StringBuilder, and stores
   * the shadow edges to be cleared at the end of the trigger. Note that MySQL
   * currently has limitations preventing us from clearing shadow edges; see the
   * createExtra() documentation for why.
   *
   * @param inProcedure   true if the INSERT is in a procedure called by a
   *                      trigger, false if the INSERT is to be used directly
   *                      inside the trigger. If in a procedure,
   *                      NEW_shadow_shadowCol will be used (and assumed passed in
   *                      to the procedure); when used within a trigger the row
   *                      value will be accessed using NEW.shadowCol. This
   *                      shouldn't be necessary, but MySQL doesn't support
   *                      passing a row to a procedure.
   * @param node          the EntityNode of the table the trigger applies to
   * @param inShadowEdges the shadowEdges available from the original insert,
   *                      which may not have been into node
   * @param selectWhat    if non-null, this generates a "INSERT INTO ... SELECT
   *                      <i>selectWhat</i> FROM <i>selectFrom</i>" statement,
   *                      with appropriate shadow columns added.
   * @param selectFrom    see the selectWhat parameter.
   * @param cols          if selectWhat is non-null, this is a list of column
   *                      names that are being inserted (not including the shadow
   *                      columns). This will be treated as an empty list if null.
   * @param vals          if selectWhat is non-null, this is a list of values
   *                      associated with the names provided by cols that are to
   *                      be inserted. This will be treated as an empty list if
   *                      null.
   * @return A string that is typically appended to the ongoing stringbuilder
   */
  private String insertInto(final boolean inProcedure, // If we're producing
                              // code for direct
                              // inclusion in a
                              // trigger
      final EntityNode node, // final Collection<SketchEdge>
                  // inShadowEdges,
      final String selectWhat, final String selectFrom, final List<String> cols, final List<String> vals) {
    final StringBuilder proc = new StringBuilder(500);

    // final Collection<SketchEdge> shadowCols = new HashSet<SketchEdge>();
    final List<String> theCols = (null == cols) ? new LinkedList<>() : cols;
    final List<String> theVals = (null == vals) ? new LinkedList<>() : vals;
    // final Collection<SketchEdge> shadowEdges = node.getNonPartialEdges();

    /**
     * Getting rid of shadow edges Federico
     * shadowEdges.addAll(node.getShadowEdges());
     * shadowEdges.retainAll(inShadowEdges);
     */

    proc.append("INSERT INTO ").append(quoteId(node)).append(' ');
    proc.append('(');

    final StringBuilder values = new StringBuilder(300);

    for (final String col : theCols) {
      proc.append(col).append(", ");
    }

    for (final String val : theVals) {
      values.append(val).append(", ");
    }

    // add the hidden attributes. If we are adding it through here
    // then it is because it is part of a constraint so we want to label it
    // as such
    // if not then lets label it as false
    for (final EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> col : node
        .getHiddenEntityAttributes()) {
      proc.append(col.getName()).append(", ");
      values.append(true).append(", ");
    }

    // commented out by Sarah van der Laan -- caused error in generated SQL
    // file (invalid foreign keys)
    /**
     * for (final SketchEdge e : shadowEdges) {
     * proc.append(quoteId(tableFK(e))).append(", ");
     * 
     * if (inProcedure) { values.append(quoteId("NEW_shadow_" +
     * tableFK(e))).append(", "); } else {
     * values.append("NEW.").append(quoteId(tableFK(e))).append(", "); } }
     */

    // Remove the last ", " (unless both are empty, since then we didn't add
    // any):
    if (!(theCols.isEmpty())) // && shadowEdges.isEmpty()))
    {
      proc.delete(proc.length() - 2, proc.length());
    }

    // And remove it from values, if it has anything:
    if (values.length() > 0) {
      values.delete(values.length() - 2, values.length());
    }

    if (selectWhat != null) {
      proc.append(") SELECT ").append(selectWhat).append((values.length() > 0) ? ", " + values : "")
          .append(" FROM ").append(selectFrom);
    } else {
      proc.append(") VALUES (").append(values).append(')');
    }

    proc.append(';').append(lineSep);

    return proc.toString();
  }

  /**
   *
   *
   * @param node
   *
   * @return
   */
  @Override
  public List<String> createView(final ViewNode node) {
    final List<String> viewSQL = new LinkedList<>();

    for (final QueryNode qn : node.getFrame().getMModel().getEntities()) {
      viewSQL.add(createView(qn));
    }

    return viewSQL;
  }

  /**
   *
   *
   * @param node
   *
   * @return
   */
  @Override
  public String createView(final QueryNode node) {
    return "CREATE VIEW " + node.getName() + " AS " + node.getQuery() + $;
  }

  /**
   * Simple wrapper around the driver's cleanId method.
   *
   * @param id
   *
   * @return
   */
  @Override
  public String cleanId(final Object id) {
    return dbDriver.cleanId(id);
  }

  /**
   * Simple wrapper around the driver's quoteId method.
   *
   * @param id
   *
   * @return
   */
  @Override
  public String quoteId(final Object id) {
    return dbDriver.quoteId(id);
  }
}
