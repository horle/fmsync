package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;

import ceramalex.sync.model.ComparisonResult;
import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;
import ceramalex.sync.model.Tuple;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

public class ComparisonFrame extends JFrame {

	private JPanel container;
	private final ButtonGroup buttonGroup = new ButtonGroup();

	private ArrayList<ComparisonResult> comp;
	private SQLDataModel data;
	private ArrayList<Pair> commonTables;

	private static Logger logger = Logger.getLogger(ComparisonFrame.class);
	private JTabbedPane tabs;
	private JRadioButton chkTimestamp;
	private JRadioButton chkContent;
	private JButton btnReload;
	private JToggleButton btnPairs;
	private JToggleButton btnDownload;
	private JToggleButton btnUpload;
	private JToggleButton btnIndividuals;
	private JToggleButton btnUnequal;
	// private ProgressMonitor monitor;
	private ProgressWorker worker;
	private DefaultTableModel m1;
	private DefaultTableModel m2;

	private void initialize() {
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				abortMission();
			}
		});
		setTitle("Sync databases");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 791, 472);
		container = new JPanel();
		container.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(container);
		container.setLayout(new BorderLayout(0, 0));

		tabs = new JTabbedPane(JTabbedPane.TOP);
		chkTimestamp = new JRadioButton("by Timestamp");
		chkContent = new JRadioButton("by Content");
		btnReload = new JButton("Reload tables");
		btnPairs = new JToggleButton("Pairs");
		btnDownload = new JToggleButton("<-");
		btnUpload = new JToggleButton("->");
		btnIndividuals = new JToggleButton("Individuals");
		btnUnequal = new JToggleButton("!=");

		JPanel pnlTop = new JPanel();
		container.add(pnlTop, BorderLayout.NORTH);
		pnlTop.setLayout(new BorderLayout(0, 0));

		JPanel pnlFilters = new JPanel();
		pnlFilters.setBorder(new TitledBorder(new LineBorder(new Color(184,
				207, 229)), "Filters", TitledBorder.LEADING, TitledBorder.TOP,
				null, new Color(51, 51, 51)));
		pnlTop.add(pnlFilters, BorderLayout.WEST);
		pnlFilters.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("45px"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("45px"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED,
						Sizes.constant("50dlu", true),
						Sizes.constant("70dlu", true)), 0),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("25px"),
				FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("25px"),
				FormFactory.RELATED_GAP_ROWSPEC, }));

		btnUnequal.setSelected(true);
		btnUnequal.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnUnequal, "2, 2, default, fill");

		btnIndividuals.setSelected(true);
		btnIndividuals.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnIndividuals, "6, 2, default, center");

		btnUpload.setSelected(true);
		btnUpload.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnUpload, "2, 4, default, fill");

		btnDownload.setSelected(true);
		btnDownload.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnDownload, "4, 4, default, fill");

		btnPairs.setSelected(true);
		btnPairs.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnPairs, "6, 4, default, center");

		JPanel pnlOptions = new JPanel();
		pnlOptions.setBorder(new TitledBorder(null, "Options",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51,
						51)));
		pnlTop.add(pnlOptions, BorderLayout.CENTER);
		pnlOptions.setLayout(new FormLayout(
				new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, }));

		btnReload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadTables();
			}
		});
		pnlOptions.add(btnReload, "2, 2");
		btnReload.setFont(new Font("Dialog", Font.PLAIN, 12));

		chkTimestamp.setSelected(true);
		buttonGroup.add(chkTimestamp);
		chkTimestamp.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chkTimestamp, "4, 2");

		JCheckBox chckbxNewCheckBox_2 = new JCheckBox("New check box");
		chckbxNewCheckBox_2.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chckbxNewCheckBox_2, "6, 2");

		buttonGroup.add(chkContent);
		chkContent.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chkContent, "4, 4");

		JCheckBox chckbxNewCheckBox_3 = new JCheckBox("New check box");
		chckbxNewCheckBox_3.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chckbxNewCheckBox_3, "6, 4");

		JPanel pnlActions = new JPanel();
		pnlTop.add(pnlActions, BorderLayout.EAST);
		pnlActions.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.UNRELATED_GAP_COLSPEC, FormFactory.BUTTON_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:default"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:default"),
				FormFactory.RELATED_GAP_ROWSPEC, }));

		JButton btnStart = new JButton("Start!");
		btnStart.setFont(new Font("Dialog", Font.BOLD, 12));
		pnlActions.add(btnStart, "2, 2, fill, fill");

		JButton btnCancel = new JButton("Cancel");
		btnCancel.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				abortMission();
			}
		});
		pnlActions.add(btnCancel, "2, 4, fill, fill");
		container.add(tabs, BorderLayout.CENTER);

		tabs.setFont(new Font("Dialog", Font.PLAIN, 12));
		loadTables();
	}

	private void loadTables() {
		tabs.removeAll();
		try {
			commonTables = data.fetchCommonTables();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this,
					"I was unable to fetch tables from the databases.",
					"Unable to fetch tables", JOptionPane.ERROR_MESSAGE);
			dispose();
		}
		ArrayList<ComparisonResult> comps = data.getResults();

		for (int i = 0; i < comps.size(); i++) {
			ComparisonResult comp = comps.get(i);
			Pair p = comp.getTableName();
			Vector<String> commonFields = comp.getCommonFields();
			commonFields.sort(null);

			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> conflict = comp
					.getConflictList();
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> updateLocally = comp
					.getLocalUpdateList();
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> updateRemotely = comp
					.getRemoteUpdateList();
			Vector<Tuple<Integer, Integer>> delete = comp.getDeleteList();
			Vector<TreeMap<String, String>> download = comp.getDownloadList();
			Vector<TreeMap<String, String>> upload = comp.getUploadList();

			JTable table1 = new JResizeTable();
			JTable table2 = new JResizeTable();
			table1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table2.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table1.setDefaultRenderer(Object.class, new TestCellRenderer());
			table2.setDefaultRenderer(Object.class, new TestCellRenderer());

			JScrollPane innerLeftScroll = new JScrollPane(table1);
			JScrollPane innerRightScroll = new JScrollPane(table2);
			JPanel outerScroll = new JPanel();
			outerScroll.setLayout(new GridLayout(1, 2));

			// setting policies
			innerLeftScroll
					.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			innerLeftScroll
					.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			innerRightScroll
					.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			innerRightScroll
					.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			innerLeftScroll.getVerticalScrollBar().setModel(
					innerRightScroll.getVerticalScrollBar().getModel());

			outerScroll.add(innerLeftScroll);
			outerScroll.add(innerRightScroll);

			m1 = new DefaultTableModel() {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			m2 = new DefaultTableModel() {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};

			m1.setColumnIdentifiers(commonFields);
			m2.setColumnIdentifiers(commonFields);
			
			boolean notEmpty = false;
			
			ArrayList<Tuple<Integer,Integer>> redList = new ArrayList<Tuple<Integer,Integer>>();
			
			if (btnUnequal.isSelected()) {
				if (btnPairs.isSelected()) {
					for (int j = 0; j < conflict.size(); j++) {
						notEmpty = true;
						m1.addRow(conflict.get(j).getLeft().values().toArray());
						m2.addRow(conflict.get(j).getRight().values().toArray());
					}
					for (int j = 0; j < updateLocally.size(); j++) {
						notEmpty = true;
						TreeMap<String,String> rowL = updateLocally.get(j).getLeft();
						TreeMap<String,String> rowR = updateLocally.get(j).getRight();
						m1.addRow(new String[0]);
						m2.addRow(new String[0]);
						for (String key : commonFields) {
							// filling rows on both sides
							if (rowL.containsKey(key)) {
								m1.setValueAt(rowL.get(key), m1.getRowCount()-1, m1.findColumn(key));
								m2.setValueAt(rowL.get(key), m2.getRowCount()-1, m2.findColumn(key));
							} else {
								m1.setValueAt(null, m1.getRowCount()-1, m1.findColumn(key));
								m2.setValueAt(null, m2.getRowCount()-1, m2.findColumn(key));
							}
							// overwrite diffs on right side
							if (rowR.containsKey(key)) {
								int row = m2.getRowCount()-1;
								int col = m2.findColumn(key);
								m2.setValueAt(rowR.get(key), row, col);
								redList.add(new Tuple<Integer,Integer>(row,col));
							}
						}
					}
					for (int j = 0; j < updateRemotely.size(); j++) {
						notEmpty = true;
						m1.addRow(updateRemotely.get(j).getLeft().values().toArray());
						m2.addRow(updateRemotely.get(j).getRight().values().toArray());
					}
				}
				if (btnIndividuals.isSelected()) {
					if (btnUpload.isSelected()) {
						for (int j = 0; j < upload.size(); j++) {
							notEmpty = true;
							m1.addRow(upload.get(j).values().toArray());
							m2.addRow(new String[] {});
						}
					}
					if (btnDownload.isSelected()) {
						for (int j = 0; j < download.size(); j++) {
							notEmpty = true;
							m2.addRow(download.get(j).values().toArray());
							m1.addRow(new String[] {});
						}
					}
				}
			}

			table1.setModel(m1);
			table2.setModel(m2);
			
			if (notEmpty)
				tabs.addTab(p.getLeft(), null, outerScroll, p.toString());
			
			for (Tuple<Integer,Integer> t : redList) {
				DefaultTableCellRenderer ren = (DefaultTableCellRenderer) table2.getCellRenderer(t.getLeft(), t.getRight());
				ren.setForeground(Color.RED);
			}
		}
	}

	protected void abortMission() {
		if (JOptionPane.showConfirmDialog(this,
				"Are you sure to cancel the operation? No changes will apply.",
				"Really Closing?", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			comp = null;
			dispose();
		}
	}

	/**
	 * Create the frame.
	 * 
	 * @param txtLog
	 */
	public ComparisonFrame(JTextArea txtLog) {
		try {
			data = SQLDataModel.getInstance();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ProgressMonitor monitor = ProgressUtil.createModalProgressMonitor(this, 100, false, 0); 
				//new ProgressMonitor(null,
				//"Comparing tables ...", "Current table: ", 0, 100);
//		monitor.start("Fetching ...");

		try {
			worker = new ProgressWorker(txtLog) {
				@Override
				protected void done() {
					initialize();
				}
			};
		} catch (IOException | SQLException e) {
			JOptionPane.showMessageDialog(this,
					"I couldn't create a new thread, consult the log.",
					"Error", JOptionPane.ERROR_MESSAGE);
			logger.error(e);
		}
		worker.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				if ("progress".equals(event.getPropertyName())) {
					int progress = (Integer) event.getNewValue();
					monitor.setCurrent(""+progress, 123);

					if (worker.isDone()) {
						if (monitor.isCanceled()) {
							worker.cancel(true);
							txtLog.append("Comparing tables canceled.\n");
						} else
							txtLog.append("Comparing tables completed.\n");
					}
				}
			}
		});
		worker.execute();

		comp = new ArrayList<ComparisonResult>();
	}

	public ArrayList<ComparisonResult> showDialog(JFrame parent) {
		this.pack();
		ModalFrameUtil.showAsModal(this, parent);
		return comp;
	}

	public class TestCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setToolTipText(table.getColumnName(column) + ": " + value);
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}

	}

	class JResizeTable extends JTable {
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
			Component comp = super.prepareRenderer(renderer, row, col);
			Object value = getModel().getValueAt(row, col);
			if (value.equals(false)) {
				comp.setBackground(Color.red);
			} else if (value.equals(true)) {
				comp.setBackground(Color.green);
			} else {
				comp.setBackground(Color.white);
			}
			return comp;
		}

		@Override
		public Point getToolTipLocation(MouseEvent event) {
			return new Point(10, 10);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return getPreferredSize().width < getParent().getWidth();
		}

		@Override
		public void doLayout() {
			TableColumn resizingColumn = null;

			if (tableHeader != null)
				resizingColumn = tableHeader.getResizingColumn();

			// Viewport size changed. May need to increase columns widths

			if (resizingColumn == null) {
				setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				super.doLayout();
			}

			// Specific column resized. Reset preferred widths

			else {
				TableColumnModel tcm = getColumnModel();

				for (int i = 0; i < tcm.getColumnCount(); i++) {
					TableColumn tc = tcm.getColumn(i);
					tc.setPreferredWidth(tc.getWidth());
				}

				// Columns don't fill the viewport, invoke default layout

				if (tcm.getTotalColumnWidth() < getParent().getWidth())
					setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				super.doLayout();
			}

			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		}
	}
}
