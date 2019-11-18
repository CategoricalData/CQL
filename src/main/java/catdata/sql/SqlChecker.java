package catdata.sql;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.functors.Tuple3;
import org.jparsec.functors.Tuple4;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.GuiUtil;
import catdata.sql.SqlColumn;
import catdata.sql.SqlForeignKey;
import catdata.sql.SqlLoader;
import catdata.sql.SqlSchema;
import catdata.sql.SqlType;

// aql sqlchecker should use fk names

@SuppressWarnings("deprecation")
public class SqlChecker {

	private static int count = 0;

	private static String next() {
		return "v" + count++;
	}

	private static String pr(Pair<String, List<Pair<String, String>>> y) {
		List<String> l = y.second.stream().map(x -> x.first + "->" + x.second).collect(Collectors.toList());
		return y.first + "," + Util.sep(l, ",");
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void doChecks(
			List<Pair<String, Pair<Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>, Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>>>> tocheck)
			throws SQLException {
		for (Pair<String, Pair<Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>, Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>>> eq : tocheck) {
			JTabbedPane ret = new JTabbedPane();

			Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>> lhs = eq.second.first;
			Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>> rhs = eq.second.second;

			if (!lhs.first.equals(rhs.first)) {
				throw new RuntimeException(
						eq.first + " starts at two different tables, " + lhs.first + " and " + rhs.first);
			}

			Triple<String, Set<String>, String> q1 = path(lhs.first, lhs.second, lhs.third, info);
			Triple<String, Set<String>, String> q2 = path(rhs.first, rhs.second, rhs.third, info);

			endMatches(eq.first, q1.third, q2.third, lhs.third, rhs.third);

			Statement stmt1 = conn.createStatement();
			stmt1.execute(q1.first);
			ResultSet q1r = stmt1.getResultSet();
	

			Statement stmt2 = conn.createStatement();
			stmt2.execute(q2.first);
			ResultSet q2r = stmt2.getResultSet();

			Set<Map<String, String>> tuples1 = toTuples(q1r);
			Set<Map<String, String>> tuples2 = toTuples(q2r);

			boolean b = tuples1.equals(tuples2);
			if (b) {
				CodeTextPanel p2 = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "OK");
				ret.add(p2, "Result");
			} else {
				ret.add(showDiff(lhs.first, q1.third, tuples1, tuples2,
						new LinkedList<>(endType(info, q1.third, lhs.third).keySet())), "Result");
			}

			if (!q1.second.isEmpty() || !q2.second.isEmpty()) {
				String exns = "LHS warnings:\n\n" + Util.sep(q1.second, "\n") + "\n\nRHS warnings:\n\n"
						+ Util.sep(q2.second, "\n");
				CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", exns);
				ret.add("Warnings", p);
			}

			CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "",
					q1.first + "\n\n = \n\n" + q2.first);
			ret.add(p, "Query");

			frames.add(new Pair<>(eq.first, ret));
			
			q1r.close();
			q2r.close();
			stmt1.close();
			stmt2.close();
		}

	}

	private Triple<String, Set<String>, String> path(String start, List<Pair<String, List<Pair<String, String>>>> path,
			List<Pair<String, String>> last, SqlSchema info) {
		String init = start;
		String v = next();
		String init_v = v;
		Set<String> from = new HashSet<>();
		Set<String> where = new HashSet<>();

		Set<String> ret = new HashSet<>();

		from.add(start + " AS " + v);

		info.getTable(start);

		for (Pair<String, List<Pair<String, String>>> edge : path) {
			String target = edge.first;

			info.getTable(target);

			if (!match(start, target, edge.second)) {
				String exn = pr(edge) + " is a not declared as a foreign key from " + start + " to " + target;
				ret.add(exn);
				if (haltOnErrors.isSelected()) {
					throw new RuntimeException(exn);
				}
			}
			ret.addAll(typeCheck(start, target, edge));
			if (!targetIsPK(start, target, edge)) {
				String exn = pr(edge) + " does not target the primary key of " + target;
				ret.add(exn);
				if (haltOnErrors.isSelected()) {
					throw new RuntimeException(exn);
				}
			}
			String v2 = next();
			from.add(target + " AS " + v2);
			for (Pair<String, String> p : edge.second) {
				where.add(v + "." + p.first + " = " + v2 + "." + p.second);
			}
			v = v2;
			start = target;
		}

		Set<String> select = new HashSet<>();
		for (SqlColumn col : info.getTable(init).columns) {
			select.add(init_v + "." + col.name + " AS " + "I_" + col.name);
		}

		if (last != null) {
			for (Pair<String, String> col : last) {
				info.getTable(start).getColumn(col.first);
				select.add(v + "." + col.first + " AS " + "O_" + col.second);
			}
		} else {
			for (SqlColumn col : info.getTable(start).columns) {
				select.add(v + "." + col.name + " AS " + "O_" + col.name);
			}
		}
		// TODO: aql must check end is the same in path eq too

		String str = "SELECT DISTINCT " + Util.sep(select, ", ") + "\nFROM " + Util.sep(from, ", ")
				+ (where.isEmpty() ? "" : "\nWHERE " + Util.sep(where, " AND "));

		return new Triple<>(str, ret, start);
	}

	private Set<String> typeCheck(String source, String target, Pair<String, List<Pair<String, String>>> edge) {
		Set<String> ret = new HashSet<>();
		for (Pair<String, String> p : edge.second) {
			SqlType src_t = info.getTable(source).getColumn(p.first).type;
			SqlType dst_t = info.getTable(target).getColumn(p.second).type;
			if (!src_t.equals(dst_t)) {
				ret.add("In " + pr(edge) + ", types do not agree for " + p.first + "->" + p.second + ", is " + src_t
						+ "->" + dst_t);
			}
		}
		return ret;
	}

	// TODO aql sql checker
	private boolean targetIsPK(@SuppressWarnings("unused") String source, String target,
			Pair<String, List<Pair<String, String>>> edge) {
		Set<String> cand = new HashSet<>();
		for (Pair<String, String> p : edge.second) {
			cand.add(p.second);
		}
		Set<String> cand2 = info.getTable(target).pk.stream().map(x -> x.name).collect(Collectors.toSet());
		return cand2.equals(cand);
	}

	private boolean match(String source, String target, List<Pair<String, String>> cand) {
		for (SqlForeignKey fk : info.fks) {
			if (fk.source.name.equals(source.toUpperCase()) && fk.target.name.equals(target.toUpperCase())) {
				List<Pair<String, String>> cand2 = new LinkedList<>();
				for (SqlColumn tcol : fk.map.keySet()) {
					cand2.add(new Pair<>(fk.map.get(tcol).name, tcol.name));
				}
				if (new HashSet<>(cand).equals(new HashSet<>(cand2))) {
					return true;
				}
			}
		}

		return false;
	}

	private void endMatches(String err, String end1, String end2, List<Pair<String, String>> proj1,
			List<Pair<String, String>> proj2) {
		if (proj1 == null && proj2 == null) {
			if (!end1.equals(end2)) {
				throw new RuntimeException(err + " ends on two different tables, " + end1 + " and " + end2);
			}
		}

		Map<String, String> t1 = endType(info, end1, proj1);
		Map<String, String> t2 = endType(info, end2, proj2);

		if (!t1.equals(t2)) {
			throw new RuntimeException(err + " ends on two different schemas, " + t1 + " and " + t2);
		}
	}

	private static Map<String, String> endType(SqlSchema info, String target, List<Pair<String, String>> proj) {
		Map<String, String> ret = new HashMap<>();
		if (proj == null) {
			return info.getTable(target).typeMap();
		}
		for (Pair<String, String> p : proj) {
			String t = info.getTable(target).getColumn(p.first).type.name;
			if (ret.containsKey(p.second)) {
				throw new RuntimeException("Duplicate col: " + p.second);
			}
			ret.put(p.second, t);
		}
		return ret;
	}

	private JComponent showDiff(String src, String dst, Set<Map<String, String>> lhs, Set<Map<String, String>> rhs,
			List<String> tCols) {
		List<JPanel> tbls = new LinkedList<>();

		List<String> sCols = new LinkedList<>(
				info.getTable(src).columns.stream().map(x -> x.name).collect(Collectors.toList()));

		for (Map<String, String> row : lhs) {
			List<String> lhs_out = new LinkedList<>();
			List<String> rhs_out = new LinkedList<>();

			Map<String, String> lhsM = row; // match(row, lhs, sCols);
			Map<String, String> rhsM = matchRow(row, rhs, sCols);

			if (lhsM.equals(rhsM)) {
				continue;
			}

			List<String> inRow = new LinkedList<>();
			for (String sCol : sCols) {
				inRow.add(row.get("I_" + sCol));
			}

			for (String tCol : tCols) {
				lhs_out.add(lhsM.get("O_" + tCol));
				rhs_out.add(rhsM.get("O_" + tCol));
			}

			JPanel inTable = GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Input " + src,
					new Object[][] { inRow.toArray() }, sCols.toArray());
			JPanel diffTable = GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Output " + dst,
					new Object[][] { lhs_out.toArray(), rhs_out.toArray() }, tCols.toArray());
			JPanel p = new JPanel(new GridLayout(2, 1));
			p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Mismatch"));
			p.add(inTable);
			p.add(diffTable);
			tbls.add(p);
		}

		JPanel ret = new JPanel(new GridLayout(tbls.size(), 1, 0, 6));
		for (JPanel p : tbls) {
			ret.add(p);
		}
		JComponent xxx = new JScrollPane(ret);
		xxx.setBorder(BorderFactory.createEmptyBorder());
		return xxx;
	}

	private static Map<String, String> matchRow(Map<String, String> row, Set<Map<String, String>> rows,
			List<String> cols) {
		outer: for (Map<String, String> row0 : rows) {
			for (String col : cols) {
				if (!row0.get("I_" + col).equals(row.get("I_" + col))) {
					continue outer;
				}
			}
			return row0;
		}
		throw new RuntimeException("No partner for " + row + " in " + rows);
	}

	private static Set<Map<String, String>> toTuples(ResultSet resultSet) throws SQLException {
		Set<Map<String, String>> rows = new HashSet<>();

		ResultSetMetaData rsmd = resultSet.getMetaData();
		int columnsNumber = rsmd.getColumnCount();

		while (resultSet.next()) {
			Map<String, String> row = new HashMap<>();
			for (int i = 1; i <= columnsNumber; i++) {
				String columnValue = resultSet.getString(i);
				String columnName = rsmd.getColumnLabel(i);
				row.put(columnName, columnValue);
			}
			rows.add(row);
		}

		return rows;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////

	private Connection conn;
	private SqlSchema info;

	private List<Pair<String, JComponent>> frames = new LinkedList<>();

	private final JCheckBox haltOnErrors = new JCheckBox("Require FK Decls", true);

	private final CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Response", "");

	private static final Example[] examples = { new EmpExample() };

	private final CodeTextPanel input = new CodeTextPanel("Path Equalities", "");
	private final SqlLoader loader = new SqlLoader(output, "SQL Loader");

	public SqlChecker() {

		JButton transButton = new JButton("Check");

		JComboBox<Example> box = new JComboBox<>(examples);
		box.setSelectedIndex(-1);
		box.addActionListener((ActionEvent e) -> input.setText(((Example) box.getSelectedItem()).getText()));

		transButton.addActionListener(x -> check());

		JPanel p = new JPanel(new BorderLayout());

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(2);
		jsp.setResizeWeight(0.5d);
		jsp.add(input);
		jsp.add(output);

		JPanel tp = new JPanel(new GridLayout(1, 4));

		tp.add(transButton);
		tp.add(haltOnErrors);
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);

		p.add(jsp, BorderLayout.CENTER);
		p.add(tp, BorderLayout.NORTH);

		JSplitPane jspX = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jspX.setBorder(BorderFactory.createEmptyBorder());

		JPanel panX = new JPanel(new GridLayout(1, 1));
		jspX.setDividerSize(2);
		jspX.setResizeWeight(0.4d);

		panX.add(jspX);
		jspX.add(loader);
		jspX.add(p);

		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "SQL Checker"));

		JFrame f = new JFrame("SQL Checker");
		f.setContentPane(panX);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	@SuppressWarnings("unused")
	private void check() {
		if (loader.schema == null) {
			output.setText("Please Run or Load first");
			return;
		}
		conn = loader.conn;
		info = loader.schema;

		frames = new LinkedList<>();
		frames.add(new Pair<>("", new JPanel()));

		try {
			String[] strings = input.getText().trim().split(";");

			List<Pair<String, Pair<Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>, Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>>>> tocheck = new LinkedList<>();

			// TODO: aql move this into the parser
			for (String string0 : strings) {
				String string = string0.trim();
				if (string.isEmpty()) {
					continue;
				}
				if (string.equals(";")) {
					continue;
				}
				if (string.trim().startsWith("CHECK ")) {
					tocheck.add(new Pair<>(string.substring(6), eq(string.substring(6))));
					continue;
				}

				throw new RuntimeException("Does not start with CHECK " + string0);
			}

			doChecks(tocheck);
			new DisplayThingy();
			output.setText("OK");
		} catch (Exception ex) {
			ex.printStackTrace();
			output.setText(ex.getMessage());
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public class DisplayThingy {

		public DisplayThingy() {
			display("SQL Checker Result");
		}

		JFrame frame = null;
		String name;

		final CardLayout cl = new CardLayout();
		final JPanel x = new JPanel(cl);
		final JList<String> yyy = new JList<>();
		final Map<String, String> indices = new HashMap<>();

		public void display(String s) {
			frame = new JFrame();
			name = s;

			Vector<String> ooo = new Vector<>();
			int index = 0;
			for (Pair<String, JComponent> p : frames) {
				x.add(p.second, p.first);
				ooo.add(p.first);
				indices.put(Integer.toString(index++), p.first);
			}

			yyy.setListData(ooo);
			JPanel temp1 = new JPanel(new GridLayout(1, 1));
			temp1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Select:"));
			JScrollPane yyy1 = new JScrollPane(yyy);
			temp1.add(yyy1);
			yyy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			yyy.addListSelectionListener((ListSelectionEvent e) -> {
				int i = yyy.getSelectedIndex();
				if (i == -1) {
					cl.show(x, "");
				} else {
					cl.show(x, ooo.get(i));
				}
			});

			JPanel north = new JPanel(new GridLayout(1, 1));
			JSplitPane px = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			px.setDividerLocation(200);
			px.setDividerSize(4);
			frame = new JFrame(/* "Viewer for " + */ s);

			JSplitPane temp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			temp2.setResizeWeight(1);
			temp2.setDividerSize(0);
			temp2.setBorder(BorderFactory.createEmptyBorder());
			temp2.add(temp1);
			temp2.add(north);

			px.add(temp2);

			px.add(x);

			frame.setContentPane(px);
			frame.setSize(900, 600);

			ActionListener escListener = (ActionEvent e) -> frame.dispose();

			frame.getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
			KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
			KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
			frame.getRootPane().registerKeyboardAction(escListener, ctrlW, JComponent.WHEN_IN_FOCUSED_WINDOW);
			frame.getRootPane().registerKeyboardAction(escListener, commandW, JComponent.WHEN_IN_FOCUSED_WINDOW);

			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static final Parser<Integer> NUMBER = IntegerLiteral.PARSER.map(Integer::valueOf);

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(", ")", "=", "->", "+", "*", "^",
			"|" };

	private static final String[] res = new String[] {};

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers
			.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or((Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER, (Parser<?>) IntegerLiteral.TOKENIZER);

	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return Identifier.PARSER;
	}

	private static Parser<?> path() {
		Parser<?> p = Parsers.tuple(ident(), term("->"), ident());
		Parser<?> edge = Parsers.tuple(term("."), ident(), term(","), p.sepBy1(term(",")));
		Parser<?> att = Parsers.tuple(term("|"), p.sepBy1(term(",")));
		return Parsers.tuple(ident(), edge.many(), att.optional());
	}

	private static Parser<?> program() {
		return Parsers.tuple(path(), term("="), path());
	}

	@SuppressWarnings("rawtypes")
	private static Pair<String, List<Pair<String, String>>> toEdge(Object a) {
		Tuple4 t = (Tuple4) a;
		String n = (String) t.b;
		List z = (List) t.d;
		List<Pair<String, String>> y = new LinkedList<>();
		for (Object q : z) {
			Tuple3 q2 = (Tuple3) q;
			Pair<String, String> pair = new Pair<>(((String) q2.a).toUpperCase(), ((String) q2.c).toUpperCase());
			y.add(pair);
		}
		Pair<String, List<Pair<String, String>>> u = new Pair<>(n.toUpperCase(), y);
		return u;
	}

	@SuppressWarnings("rawtypes")
	private static Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>> toPath(
			Object ox) {
		Tuple3 o = (Tuple3) ox;
		String start = (String) o.a;
		List l = (List) o.b;
		List<Pair<String, List<Pair<String, String>>>> x = new LinkedList<>();
		for (Object a : l) {
			x.add(toEdge(a));
		}

		Set<String> seen = new HashSet<>();
		List<Pair<String, String>> y = null;
		org.jparsec.functors.Pair qq = (org.jparsec.functors.Pair) o.c;
		if (qq != null) {
			y = new LinkedList<>();
			List z = (List) qq.b;
			for (Object q : z) {
				Tuple3 q2 = (Tuple3) q;
				Pair<String, String> pair = new Pair<>(((String) q2.a).toUpperCase(), ((String) q2.c).toUpperCase());
				if (seen.contains(pair.second)) {
					throw new RuntimeException("Duplicate col: " + pair.second);
				}
				seen.add(pair.second);
				y.add(pair);
			}
		}
		return new Triple<>(start.toUpperCase(), x, y);

	}

	@SuppressWarnings({ "rawtypes" })
	private static Pair<Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>, Triple<String, List<Pair<String, List<Pair<String, String>>>>, List<Pair<String, String>>>> eq(
			String s) {
		Tuple3 decl = (Tuple3) program().from(TOKENIZER, IGNORED).parse(s);
		return new Pair<>(toPath(decl.a), toPath(decl.c));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	static class EmpExample extends Example {

		@Override
		public String getName() {
			return "Employees";
		}

		@Override
		public String getText() {
			return "CHECK Employee . Employee,manager->id . Employee,manager->id"
					+ "\n = Employee . Employee,manager->id;" + "\n"
					+ "\nCHECK Employee . Employee,manager->id . Department,worksIn->id"
					+ "\n = Employee . Department,worksIn->id;" + "\n"
					+ "\nCHECK Department . Employee,secretary->id . Department,worksIn->id" + "\n = Department;" + "\n"
					+ "\nCHECK Department . Employee,secretary->id | first->n" + "\n= Department | name->n";

		}
	}

}
