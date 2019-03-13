package catdata.ide;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import catdata.Pair;
import catdata.Util;

public class GuiUtil {

	public static void show(JComponent p, int w, int h, String title) {
		JFrame f = new JFrame(title);
		f.setContentPane(p);
		f.pack();
		if (w > 0 && h > 0) {
			f.setSize(new Dimension(w, h));
		}
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}


	public static class MissingIcon implements Icon{

	    private int width;
	    private int height;
	    private Color color;
	    
	  //  private BasicStroke stroke = new BasicStroke(4);
	    
	    public MissingIcon(Color c, int w, int h) {
	    	this.color = c;
	    	this.width = w;
	    	this.height = h;
	    }

	    public void paintIcon(Component c, Graphics g, int x, int y) {
	        Graphics2D g2d = (Graphics2D) g.create();

	        g2d.setColor(color);

	        g2d.fillRect(x ,y ,width  ,height );

		    g2d.dispose();
	    }

	    public int getIconWidth() {
	        return width;
	    }

	    public int getIconHeight() {
	        return height;
	    }
	}

	
	public static JComponent makeGrid(List<JComponent> list) {
		
		JPanel ret = new JPanel();
		ret.setLayout(new BoxLayout(ret, BoxLayout.PAGE_AXIS));
		//int i = 0;
		for (JComponent x : list) {
			x.setAlignmentX(Component.LEFT_ALIGNMENT);
			x.setMinimumSize(x.getPreferredSize());
			ret.add(x);
		//	ret.add(Box.)
			//ret.add(Box.createHorizontalGlue());
		}
//		ret.add(new JLabel(""), Box.cre);

		JPanel p = new JPanel(new GridLayout(1,1));
		
		p.add(ret);
		
		JScrollPane jsp = new JScrollPane(p);//, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		ret.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());		
		//jsp.setBorder(BorderFactory.createEmptyBorder());
		
		
		return jsp;
	}
	
	public static JPanel makeGrid2(List<JComponent> list) {
		JPanel ret = new JPanel(new GridBagLayout());
		int i = 0;
		for (JComponent x : list) {
			//JScrollPane jsp = new JScrollPane(x);
			//JPanel p = new JPanel(new GridLayout(1, 1));
		//	p.add(jsp);
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.weightx = 1;
			//c.gridwidth = 1;
			//c.weighty = 1;
			c.gridy = i++;
			c.anchor = GridBagConstraints.NORTHWEST;
			//c.fill = c.HORIZONTAL;
			ret.add(x, c);
		}
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weighty = 1;
		//c.gridwidth = 1;
		//c.weighty = 1;
		c.gridy = i++;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.VERTICAL;
		ret.add(new JLabel(""), c);

		JScrollPane jsp = new JScrollPane(ret); //, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel p = new JPanel(new GridLayout(1,1));
		
		p.add(jsp);
		return p;
	}

	@SuppressWarnings("serial")
	public static JPanel makeTable(Border b, String border, Object[][] rowData, Object... colNames) {
		JTable t = new JTable(rowData, colNames) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension d = getPreferredSize();
				return new Dimension(Integer.max(640, d.width), (d.height));
			}
		};
		JPanel p = new JPanel(new GridLayout(1,1));
		TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());
		if (colNames.length > 0) {
			sorter.toggleSortOrder(0);
		}
		t.setRowSorter(sorter);
		sorter.allRowsChanged();
		p.add(new JScrollPane(t));

		for (int row = 0; row < t.getRowCount(); row++) {
			int rowHeight = t.getRowHeight();

			for (int column = 0; column < t.getColumnCount(); column++) {
				Component comp = t.prepareRenderer(t.getCellRenderer(row, column), row, column);
				rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
			}

			t.setRowHeight(row, rowHeight);
		}

		Font font = UIManager.getFont("TableHeader.font");
		p.setBorder(BorderFactory.createTitledBorder(b, border, TitledBorder.DEFAULT_JUSTIFICATION,
				TitledBorder.DEFAULT_POSITION, font, Color.black));
		return p;

	}

	@SuppressWarnings("serial")
	// TODO aql merge with other makeTable method
	public static JComponent makeBoldHeaderTable(Set<Pair<Integer, Integer>> nulls, Collection<String> atts, Border b, String border, Object[][] rowData,
			String... colNames) {
		JTable t = new JTable(rowData, colNames) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension d = getPreferredSize();
				int m = this.getVisibleRect().width;
				int n = Integer.max(d.height,this.getVisibleRect().height);
				return new Dimension(m, n);
			}		
			
			@Override
			 public TableCellRenderer getCellRenderer(int row, int col) {
			    TableCellRenderer l = super.getCellRenderer(row, col);

				if (col > 0 && col < atts.size() + 1) {
			    	return new DefaultTableCellRenderer() {
						  @Override
						  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
						    JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

							l.setFont(l.getFont().deriveFont(Font.BOLD));
							int row0 = convertRowIndexToModel(row);
							int col0 = convertColumnIndexToModel(col);							
					    	if (nulls != null && nulls.contains(new Pair<>(row0, col0))) {
						    	l.setForeground(Color.RED);			    		
					    	}
					    	return l;
						}
						  
					 };
				}
				return l;
			 }
		};
		
		
		JPanel tt = new JPanel(new GridLayout(1,1));
		tt.add(new PDControlScrollPane(t));
		//TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());
		//if (colNames.length > 0) {
		//	sorter.toggleSortOrder(0);
		//}
		//t.setRowSorter(sorter);

		//sorter.allRowsChanged();
		

		

		for (int row = 0; row < t.getRowCount(); row++) {
			int rowHeight = t.getRowHeight();

			for (int column = 0; column < t.getColumnCount(); column++) {
				Component comp = t.prepareRenderer(t.getCellRenderer(row, column), row, column);
				rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
			}

			t.setRowHeight(row, rowHeight);
		}

		tt.setBorder(BorderFactory.createTitledBorder(b, border));
		//for (int i = 0; i < t.getColumnModel().getColumnCount(); i++) {
		//	TableColumn col = t.getColumnModel().getColumn(i);

		//	col.setHeaderRenderer(new BoldifyingColumnHeaderRenderer(atts, t.getTableHeader().getDefaultRenderer()));
		//}

		Font font = UIManager.getFont("Label.font");

		tt.setBorder(BorderFactory.createTitledBorder(b, border, TitledBorder.DEFAULT_JUSTIFICATION,
				TitledBorder.DEFAULT_POSITION, font, Color.black));

		return tt;
	}

	public static void centerLineInScrollPane(JTextComponent component) {
		Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);

		if (container == null)
			return;

		try {
			Rectangle r = component.modelToView(component.getCaretPosition());
			// Rectangle2D r = component.modelToView2D(component.getCaretPosition());
			JViewport viewport = (JViewport) container;
			if (r == null) { // || viewport == null) {
				return;
			}
			int extentHeight = viewport.getExtentSize().height;
			int viewHeight = viewport.getViewSize().height;
			
			int y = Math.max(0, r.y - ((extentHeight - r.height) / 2));
			// double y = Math.max(0, r.getY() - ((extentHeight - r.getHeight()) / 2));
			y = Math.min(y, viewHeight - extentHeight);

			viewport.setViewPosition(new Point(0, y));
			// viewport.setViewPosition(new Point(0, (int) y));
		} catch (BadLocationException ble) {
		}
	}

	public static String readFile(InputStream file) {
		Util.assertNotNull(file);
		try (InputStreamReader r = new InputStreamReader(file)) {

			return Util.readFile(r);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not read from " + file);
		}
		return null;
	}

	public static String readFile(String file) {
		try (FileReader r = new FileReader(file)) {
			return Util.readFile(r);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not read from " + file);
		}
		return null;
	}

	public static String readFile(File file) {
		try (FileReader r = new FileReader(file)) {
			return Util.readFile(r);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not read from " + file);
		}
		return null;
	}

	public static JList<String> makeList() {
		JList<String> list = new JList<>() {
			private static final long serialVersionUID = 1L;

			@Override
			public int locationToIndex(Point location) {
				int index = super.locationToIndex(location);
				if (index != -1 && !getCellBounds(index, index).contains(location)) {
					return -1;
				}
					return index;
				
			}
		};
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		return list;
	}
	
	static class PDControlScrollPane extends JScrollPane {

		private static final long serialVersionUID = 1L;

		public PDControlScrollPane(JComponent o) {
		    super(o);
		    addMouseWheelListener(new PDMouseWheelListener());
		}

		class PDMouseWheelListener implements MouseWheelListener {

		    private JScrollBar bar;
		    private int previousValue = 0;
		    private JScrollPane parentScrollPane; 
		    

		    private JScrollPane getParentScrollPane() {
		        if (parentScrollPane == null) {
		            Component parent = getParent();
		            while (!(parent instanceof JScrollPane) && parent != null) {
		                parent = parent.getParent();
		            }
		            parentScrollPane = (JScrollPane)parent;
		        }
		        return parentScrollPane;
		    }

		    public PDMouseWheelListener() {
		        bar = PDControlScrollPane.this.getVerticalScrollBar();
		    }
		    public void mouseWheelMoved(MouseWheelEvent e) {
		        JScrollPane parent = getParentScrollPane();
		        if (parent != null) {
		            /*
		             * Only dispatch if we have reached top/bottom on previous scroll
		             */
		            if (e.getWheelRotation() < 0) {
		                if (bar.getValue() == 0 && previousValue == 0) {
		                    parent.dispatchEvent(cloneEvent(e));
		                }
		            } else {
		                if (bar.getValue() == getMax() && previousValue == getMax()) {
		                    parent.dispatchEvent(cloneEvent(e));
		                }
		            }
		            previousValue = bar.getValue();
		        }
		        /* 
		         * If parent scrollpane doesn't exist, remove this as a listener.
		         * We have to defer this till now (vs doing it in constructor) 
		         * because in the constructor this item has no parent yet.
		         */
		        else {
		            PDControlScrollPane.this.removeMouseWheelListener(this);
		        }
		    }
		    private int getMax() {
		        return bar.getMaximum() - bar.getVisibleAmount();
		    }
		    private MouseWheelEvent cloneEvent(MouseWheelEvent e) {
		        return new MouseWheelEvent(getParentScrollPane(), e.getID(), e
		                .getWhen(), e.getModifiers(), 1, 1, e
		                .getClickCount(), false, e.getScrollType(), e
		                .getScrollAmount(), e.getWheelRotation());
		    }
		}
		}

}
