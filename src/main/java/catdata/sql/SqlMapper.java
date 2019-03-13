package catdata.sql;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import catdata.ide.CodeTextPanel;
import catdata.ide.GuiUtil;

public class SqlMapper extends JPanel {

	public static void showGuesser() {
		GuiUtil.show(new SqlMapper(), 700, 600, "SQL Mapping Guesser");
	}

	private final CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Output", "");

	private final SqlLoader input1 = new SqlLoader(output, "SQL Source");
	private final SqlLoader input2 = new SqlLoader(output, "SQL Target");
	
	private static <X> String print(X[][] c) {
		String ret = "";
		for (X[] x : c) {
			ret += Arrays.toString(x) + "\n";
		}
		return ret;
	}
	
	private void doRun() {
		if (input1.schema == null) {
			output.setText("Please Run or Load first");
			return;
		}
		if (input2.schema == null) {
			output.setText("Please Run or Load first");
			return;
		}
		
		SqlMapping m = SqlMapping.guess(input1.schema, input2.schema);
	
		output.setText(print(m.toStrings()));
	}

	private SqlMapper() {
		super(new BorderLayout());

		JButton transButton = new JButton("Guess Mapping");

		transButton.addActionListener(x -> doRun());

		JPanel tp = new JPanel(new GridLayout(1, 4));

		tp.add(transButton);
		tp.add(new JLabel());
		tp.add(new JLabel());
		tp.add(new JLabel());

		JSplitPane jspX = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jspX.setBorder(BorderFactory.createEmptyBorder());
		jspX.setDividerSize(2);
		jspX.setResizeWeight(0.5d);
		jspX.add(input1);
		jspX.add(input2);
		
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(4);
		jsp.setResizeWeight(0.7d);
		jsp.add(jspX);
		jsp.add(output);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(jsp);

		add(ret, BorderLayout.CENTER);
		add(tp, BorderLayout.NORTH);

		setBorder(BorderFactory.createEtchedBorder());

	}

	private static final long serialVersionUID = 1L;

	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	

}
