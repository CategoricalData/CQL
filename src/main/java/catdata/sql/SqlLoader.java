package catdata.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.GuiUtil;

public class SqlLoader extends JPanel {

	public static void showLoader() {
		CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Response", "");
		SqlLoader input = new SqlLoader(output, "");

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(4);
		jsp.setResizeWeight(0.5d);
		jsp.add(input);
		jsp.add(output);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(jsp);

		GuiUtil.show(ret, 700, 600, "SQL Loader");
	}

	public SqlSchema schema;
	public SqlInstance instance;
	public Connection conn;

	private static final String help = "";

	private static final Example[] examples = { new EmpExample(), new CompoundExample(), new EmpAutoExample() };

	private final String name;

	private final CodeTextPanel input = new CodeTextPanel(BorderFactory.createEtchedBorder(), "SQL Input", "");
	private final CodeTextPanel output; // = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Response", "");

	private final JCheckBox loadInstBox = new JCheckBox("Import Data?", true);
	private final JCheckBox showBox = new JCheckBox("Visualize?", true);

	private void handleError(String msg) {
		output.setText("Error in " + name + ": " + msg);
	}

	// TODO: aql print OK only
	private void populate() throws SQLException {
		schema = new SqlSchema(conn.getMetaData(), "\"");
		output.setText(schema.toString());

		if (loadInstBox.isSelected()) {
			instance = new SqlInstance(schema, conn, false, true, "\"");
			output.area.append("\n\n");
			output.area.append(instance.toString());
		}
		if (showBox.isSelected()) {
			GuiUtil.show(new SqlViewer(Color.RED, schema, instance), 600, 500, "Viewer");
		}
	}

	private void doRun() {
		try {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:mem:");

			String[] strings = input.getText().split(";");

			for (String string0 : strings) {
				String string = string0.trim();
				if (string.isEmpty()) {
					continue;
				}
				if (string.equals(";")) {
					continue;
				}
				try (Statement stmt = conn.createStatement()) {
					stmt.execute(string);
				}
			}

			populate();
		} catch (ClassNotFoundException | SQLException ex) {
			ex.printStackTrace();
			handleError(ex.getLocalizedMessage());
		}
	}

	private void doLoad() {
		try {
			if (!input.getText().trim().isEmpty()) {
				throw new RuntimeException("Cannot load if text entered");
			}

			JPanel pan = new JPanel(new GridLayout(2, 2));
			pan.add(new JLabel("JDBC Driver Class"));
			JTextField f1 = new JTextField("com.mysql.jdbc.Driver");
			pan.add(f1);
			JTextField f2 = new JTextField("jdbc:mysql://localhost/buzzbuilder?user=root&password=whasabi");
			pan.add(new JLabel("JDBC Connection String"));
			pan.add(f2);
			int i = JOptionPane.showConfirmDialog(null, pan);
			if (i != JOptionPane.OK_OPTION) {
				return;
			}

			Class.forName(f1.getText().trim());
			conn = DriverManager.getConnection(f2.getText().trim());
			populate();
		} catch (ClassNotFoundException | RuntimeException | SQLException ex) {
			ex.printStackTrace();
			handleError(ex.getLocalizedMessage());
		}
	}

	public SqlLoader(CodeTextPanel output, String name) {
		super(new BorderLayout());

		this.output = output;
		this.name = name;

		JButton transButton = new JButton("Run SQL");
		JButton loadButton = new JButton("Load JDBC");
		JButton helpButton = new JButton("Help");

		JComboBox<Example> box = new JComboBox<>(examples);
		box.setSelectedIndex(-1);
		box.addActionListener(x -> input.setText(((Example) box.getSelectedItem()).getText()));

		transButton.addActionListener(x -> doRun());
		loadButton.addActionListener(x -> doLoad());
		helpButton.addActionListener(x -> doHelp());

		JPanel tp = new JPanel(new GridLayout(2, 4));

		tp.add(transButton);
		tp.add(loadButton);
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);

		tp.add(helpButton);
		tp.add(loadInstBox);
		tp.add(new JLabel());
		tp.add(showBox);

		add(input, BorderLayout.CENTER);
		add(tp, BorderLayout.NORTH);

		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), name));
	}

	private static void doHelp() {
		JTextArea jta = new JTextArea(help);
		jta.setWrapStyleWord(true);
		jta.setLineWrap(true);
		JScrollPane p = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		p.setPreferredSize(new Dimension(300, 200));

		JOptionPane pane = new JOptionPane(p);
		JDialog dialog = pane.createDialog(null, "Help on SQL Loader");
		dialog.setModal(false);
		dialog.setVisible(true);
		dialog.setResizable(true);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final long serialVersionUID = 1L;

	private static class EmpExample extends Example {

		@Override
		public String getName() {
			return "Employees";
		}

		@Override
		public String getText() {
			return "CREATE TABLE Employee(" + "\n id INT PRIMARY KEY," + "\n first VARCHAR(255),"
					+ "\n last VARCHAR(255)," + "\n manager INT," + "\n worksIn INT" + "\n);" + "\n"
					+ "\nCREATE TABLE Department(" + "\n id INT PRIMARY KEY," + "\n name VARCHAR(255),"
					+ "\n secretary INT," + "\n);" + "\n " + "\nINSERT INTO Employee VALUES "
					+ "\n (101, 'Alan', 'Turing', 103, 10), " + "\n (102, 'Camille', 'Jordan', 102, 2), "
					+ "\n (103, 'Andrey', 'Markov', 103, 10);" + "\n" + "\nINSERT INTO Department VALUES"
					+ "\n (10, 'Applied Math', 101)," + "\n (2, 'Pure Math', 102);" + "\n"
					+ "\nALTER TABLE Employee ADD CONSTRAINT e1" + "\n FOREIGN KEY (manager) REFERENCES Employee (id);"
					+ "\n" + "\nALTER TABLE Employee ADD CONSTRAINT e2 "
					+ "\n FOREIGN KEY (worksIn) REFERENCES Department (id);" + "\n"
					+ "\nALTER TABLE Department ADD CONSTRAINT d1"
					+ "\n FOREIGN KEY (secretary) REFERENCES Employee (id);";

		}

	}

	private static class EmpAutoExample extends Example {

		@Override
		public String getName() {
			return "Employees CNF";
		}

		@Override
		public String getText() {
			return "CREATE TABLE Employee(" + "\n id INT PRIMARY KEY AUTO_INCREMENT," + "\n first VARCHAR(255),"
					+ "\n last VARCHAR(255)," + "\n manager INT," + "\n worksIn INT" + "\n);" + "\n"
					+ "\nCREATE TABLE Department(" + "\n id INT PRIMARY KEY AUTO_INCREMENT," + "\n name VARCHAR(255),"
					+ "\n secretary INT," + "\n);" + "\n " + "\nINSERT INTO Employee VALUES "
					+ "\n (101, 'Alan', 'Turing', 103, 10), " + "\n (102, 'Camille', 'Jordan', 102, 2), "
					+ "\n (103, 'Andrey', 'Markov', 103, 10);" + "\n" + "\nINSERT INTO Department VALUES"
					+ "\n (10, 'Applied Math', 101)," + "\n (2, 'Pure Math', 102);" + "\n"
					+ "\nALTER TABLE Employee ADD CONSTRAINT e1" + "\n FOREIGN KEY (manager) REFERENCES Employee (id);"
					+ "\n" + "\nALTER TABLE Employee ADD CONSTRAINT e2 "
					+ "\n FOREIGN KEY (worksIn) REFERENCES Department (id);" + "\n"
					+ "\nALTER TABLE Department ADD CONSTRAINT d1"
					+ "\n FOREIGN KEY (secretary) REFERENCES Employee (id);" + "\n";

		}

	}

	private static class CompoundExample extends Example {
		@Override
		public String getName() {
			return "Compound";
		}

		@Override
		public String getText() {
			return "CREATE TABLE CUSTOMER(" + "\nSID integer primary key," + "\nLast_Name varchar(255),"
					+ "\nFirst_Name varchar(255));" + "\n" + "\nCREATE TABLE ORDERS("
					+ "\nOrder_ID integer primary key," + "\nOrder_Date date,"
					+ "\nCustomer_SID integer REFERENCES CUSTOMER(SID)," + "\nAmount double);" + "\n"
					+ "\nCREATE TABLE INVOICE(" + "\nInvoice_ID integer," + "\nStore_ID integer,"
					+ "\nCUSTOMER_ID integer," + "\nFOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMER (SID),"
					+ "\nPRIMARY KEY(Invoice_ID, Store_ID));" + "\n" + "\nCREATE TABLE PAYMENT("
					+ "\nPayment_ID integer," + "\nInvoice_ID integer," + "\nStore_ID integer,"
					+ "\nPayment_Date datetime," + "\nPayment_Amount float," + "\nPRIMARY KEY (Payment_ID),"
					+ "\nFOREIGN KEY (Invoice_ID, Store_ID) REFERENCES INVOICE (Invoice_ID, Store_ID));" + "\n";
		}

	}

}
