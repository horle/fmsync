package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.metal.MetalToggleButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

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

	private final Color DARK_GREEN = new Color(0,153,0);
	private final Color DARK_ORANGE = new Color(249,141,0);
	private final Color LIGHT_GREEN = new Color(152,251,152);
	private final Color LIGHT_RED = new Color(240,128,128);
	private final Color LIGHT_ORANGE = new Color(254, 216, 177);
	private final Color LIGHT_BLUE = new Color(173,216,230);
	
	private JPanel container;
	private final ButtonGroup buttonGroup = new ButtonGroup();

	private TreeMap<Pair, ComparisonResult> comps;
	private SQLDataModel data;
	private ArrayList<Pair> commonTables;

	private static Logger logger = Logger.getLogger(ComparisonFrame.class);
	private JTabbedPane tabs;
	private JRadioButton chkTimestamp;
	private JRadioButton chkContent;
	private JButton btnReload;
	private JToggleButton btnPairs;
	private JGradientButton btnDownload;
	private JGradientButton btnUpload;
	private JToggleButton btnIndividuals;
	private JGradientButton btnDeleted;
	private JGradientButton btnUnequal;
	private JTextArea txtLog;
	// private ProgressMonitor monitor;
	private ProgressWorker worker;
	private JCheckBox chkSyncAttr;

	private JPopupMenu popup;
	private TreeMap<Integer, JTable[]> tables;

	private String lastTab;

	private ArrayList<Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>>> unsafeRows;

	private void initialize() {
		commonTables = new ArrayList<Pair>();
		tables = new TreeMap<Integer, JTable[]>();
		unsafeRows = new ArrayList<Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>>>();
		ActionListener simpleReload = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				simpleReloadTables();
			}
		};

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
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
		btnReload = new JButton("Refetch tables");
		btnPairs = new JToggleButton("Pairs");
		btnDownload = new JGradientButton("<-");
		btnUpload = new JGradientButton("->");
		btnIndividuals = new JToggleButton("Individuals");
		btnUnequal = new JGradientButton("!=");
		btnDeleted = new JGradientButton("X");
		chkSyncAttr = new JCheckBox("Show sync relevant fields");

		JPanel pnlTop = new JPanel();
		container.add(pnlTop, BorderLayout.NORTH);
		pnlTop.setLayout(new BorderLayout(0, 0));

		JPanel pnlFilters = new JPanel();
		pnlFilters.setBorder(new TitledBorder(new LineBorder(new Color(184,
				207, 229)), "Filters", TitledBorder.LEADING, TitledBorder.TOP,
				null, new Color(51, 51, 51)));
		pnlTop.add(pnlFilters, BorderLayout.WEST);
		pnlFilters.setLayout(new FormLayout(
				new ColumnSpec[] {
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("50px"),
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("50px"),
						FormFactory.UNRELATED_GAP_COLSPEC,
						new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED,
								Sizes.constant("50dlu", true),
								Sizes.constant("70dlu", true)),
								0),
								FormFactory.RELATED_GAP_COLSPEC, 
				},
				new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("25px"),
						FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("25px"),
						FormFactory.RELATED_GAP_ROWSPEC,
				}));

		btnUnequal.setSelected(true);
		btnUnequal.setFont(new Font("monospaced", Font.BOLD, 12));
		btnUnequal.setBackground(LIGHT_ORANGE);
		btnUnequal.addActionListener(simpleReload);
		pnlFilters.add(btnUnequal, "2, 2, default, fill");

		btnDeleted.setSelected(true);
		btnDeleted.setFont(new Font("monospaced", Font.BOLD, 12));
		btnDeleted.setBackground(LIGHT_RED);
		btnDeleted.addActionListener(simpleReload);
		pnlFilters.add(btnDeleted, "4, 2, default, fill");

		btnIndividuals.setSelected(true);
		btnIndividuals.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnIndividuals.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnUpload.isEnabled())
					btnUpload.setEnabled(false);
				else btnUpload.setEnabled(true);
				if (btnDownload.isEnabled())
					btnDownload.setEnabled(false);
				else btnDownload.setEnabled(true);
				simpleReloadTables();
			}
		});
		pnlFilters.add(btnIndividuals, "6, 4, default, center");

		btnUpload.setSelected(true);
		btnUpload.setFont(new Font("monospaced", Font.BOLD, 12));
		btnUpload.setBackground(LIGHT_GREEN);
		btnUpload.addActionListener(simpleReload);
		pnlFilters.add(btnUpload, "2, 4, default, fill");

		btnDownload.setSelected(true);
		btnDownload.setFont(new Font("monospaced", Font.BOLD, 12));
		btnDownload.setBackground(LIGHT_BLUE);
		btnDownload.addActionListener(simpleReload);
		pnlFilters.add(btnDownload, "4, 4, default, fill");

		btnPairs.setSelected(true);
		btnPairs.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnPairs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnUnequal.isEnabled())
					btnUnequal.setEnabled(false);
				else btnUnequal.setEnabled(true);
				simpleReloadTables();
			}
		});
		pnlFilters.add(btnPairs, "6, 2, default, center");

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
				refetchTables();
			}
		});
		pnlOptions.add(btnReload, "2, 2");
		btnReload.setFont(new Font("Dialog", Font.PLAIN, 12));

		chkTimestamp.setSelected(true);
		buttonGroup.add(chkTimestamp);
		chkTimestamp.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chkTimestamp, "4, 2");

		chkSyncAttr.setFont(new Font("Dialog", Font.PLAIN, 12));
		chkSyncAttr.setSelected(true);
		chkSyncAttr.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				simpleReloadTables();
			}
		});
		pnlOptions.add(chkSyncAttr, "6, 2");

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
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean ready = true;
				for (Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> rowsPerTable : unsafeRows) {
					if (!rowsPerTable.isEmpty()) ready = false;
				}
				if (!ready) {
					int dialogConfirm = JOptionPane.showConfirmDialog(null,
							"There are still unsafe conflicts to clear up. Would you like to resolve them now?",
							"Now resolving conflicts?", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					if (dialogConfirm == JOptionPane.YES_OPTION) {
						comps = null;
						dispose();
					}
					else if (dialogConfirm == JOptionPane.NO_OPTION) {
						if (startMission())
							handleStartBtn();
					}
				}
				else if (startMission())
					handleStartBtn();
			}
		});
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
		
		refetchTables();
	}

	private void handleStartBtn() {
		dispose();
		ProgressMonitor monitor = ProgressUtil.createModalProgressMonitor(this, 100, false, 0); 

		try {
			worker = new ProgressWorker(txtLog, ProgressWorker.JOB_APPLY_CHANGES) {
				@Override
				protected void done() {
					System.out.println("applying worker done.");
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
		try {
			worker.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refetchTables() {
		try {
			commonTables = data.fetchCommonTables();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this,
					"I was unable to fetch tables from the databases.",
					"Unable to fetch tables", JOptionPane.ERROR_MESSAGE);
			dispose();
		}
		simpleReloadTables(false);
	}

	/**
	 * Wrapper class to force remembering last tab
	 */
	private void simpleReloadTables() {
		simpleReloadTables(true);
	}

	private void simpleReloadTables(boolean rememberTab) {
		if (rememberTab && tabs.getTabCount() != 0)
			lastTab = tabs.getTitleAt(tabs.getSelectedIndex());
		tabs.removeAll();
		comps = data.getResults();

		unsafeRows.clear();

		for (ComparisonResult comp : comps.values()) {
			Pair p = comp.getTableName();
			TreeSet<String> commonFields = new TreeSet<String>(comp.getCommonFields());

			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> conflict = comp
					.getConflictList();
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> deleteOrDownload = comp
					.getDeleteOrDownloadList();
			this.unsafeRows.add(deleteOrDownload);
			this.unsafeRows.add(conflict);
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> updateLocally = comp
					.getLocalUpdateList();
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> updateRemotely = comp
					.getRemoteUpdateList();
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> delete = comp.getDeleteList();
			Vector<TreeMap<String, String>> download = comp.getDownloadList();
			Vector<TreeMap<String, String>> upload = comp.getUploadList();

			JTable table1 = new JResizeTable();
			JTable table2 = new JResizeTable();
			table1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table2.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

			popup = new JPopupMenu();

			MouseListener pListener = new PopupListener();
			table1.addMouseListener(pListener);
			table2.addMouseListener(pListener);

			JScrollPane innerLeftScroll = new JScrollPane(table1);
			JScrollPane innerRightScroll = new JScrollPane(table2);
			JPanel outerScroll = new JPanel();
			outerScroll.setLayout(new GridLayout(1, 2));

			// setting policies
			innerLeftScroll
			.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			innerLeftScroll
			.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			innerRightScroll
			.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			innerRightScroll
			.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			innerLeftScroll.getVerticalScrollBar().setModel(
					innerRightScroll.getVerticalScrollBar().getModel());
			innerLeftScroll.getHorizontalScrollBar().setModel(
					innerRightScroll.getHorizontalScrollBar().getModel());

			table1.setSelectionModel(table2.getSelectionModel());

			outerScroll.add(innerLeftScroll);
			outerScroll.add(innerRightScroll);

			if (!chkSyncAttr.isSelected()) {
				commonFields.remove("ArachneEntityID");
				commonFields.remove("lastModified");
			}
			commonFields.add("A");
			
			ColourTableModel m1 = new ColourTableModel(commonFields.toArray(), 0);
			ColourTableModel m2 = new ColourTableModel(commonFields.toArray(), 0);

			commonFields.remove("A");

			table1.setDefaultRenderer(Object.class, new ColourCellRenderer());
			table2.setDefaultRenderer(Object.class, new ColourCellRenderer());

			boolean notEmpty = false;

			if (btnDeleted.isSelected()) {
				for (int j = 0; j < delete.size(); j++) {
					notEmpty = true;
					TreeMap<String,String> rowL = delete.get(j).getLeft();
					TreeMap<String,String> rowR = delete.get(j).getRight();
					m1.addRow(new String[0]);
					m2.addRow(new String[0]);
					int row = m1.getRowCount()-1;

					// update action label 
					m1.setValueAt("D", row, 0);
					m1.setCellColour(row, 0, LIGHT_RED);
					m2.setValueAt("D", row, 0);
					m2.setCellColour(row, 0, LIGHT_RED);

					for (String key : commonFields) {
						// filling rows on both sides
						if (rowL != null && rowL.containsKey(key)) {
							int col = m1.findColumn(key);
							m1.setValueAt(rowL.get(key), row, col);
							m1.setCellColour(row, col, LIGHT_RED);
						} else {
							m1.setValueAt(null, m1.getRowCount()-1, m1.findColumn(key));
						}
						if (rowR != null && rowR.containsKey(key)) {
							int col = m2.findColumn(key);
							m2.setValueAt(rowR.get(key), row, col);
							m2.setCellColour(row, col, LIGHT_RED);
						} else {
							m2.setValueAt(null, m1.getRowCount()-1, m1.findColumn(key));
						}
					}
				}
			}

			if (btnUnequal.isSelected()) {
				if (btnPairs.isSelected()) {
					for (int j = 0; j < conflict.size(); j++) {
						notEmpty = true;
						TreeMap<String,String> rowL = conflict.get(j).getLeft();
						TreeMap<String,String> diffs = conflict.get(j).getRight();
						m1.addRow(new String[0]);
						m2.addRow(new String[0]);
						int row = m2.getRowCount()-1;
						// update action label 
						m1.setValueAt("C", row, 0);
						m1.setCellColour(row, 0, LIGHT_ORANGE);
						m2.setValueAt("C", row, 0);
						m2.setCellColour(row, 0, LIGHT_ORANGE);

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
							if (diffs.containsKey(key)) {
								int col = m2.findColumn(key);
								m2.setValueAt(diffs.get(key), row, col);
								m1.setCellColour(row, col, LIGHT_ORANGE);
								m2.setCellColour(row, col, LIGHT_ORANGE);
							}
						}
					}
					for (int j = 0; j < deleteOrDownload.size(); j++) {
						notEmpty = true;
//						TreeMap<String,String> rowL = deleteOrDownload.get(j).getLeft();
						TreeMap<String,String> diffs = deleteOrDownload.get(j).getRight();
						m1.addRow(new String[0]);
						m2.addRow(new String[0]);
						int row = m2.getRowCount()-1;
						// update action label 
						m1.setValueAt("??", row, 0);
						m1.setCellColour(row, 0, LIGHT_RED);
						m2.setValueAt("??", row, 0);
						m2.setCellColour(row, 0, LIGHT_RED);

						for (String key : commonFields) {
							// filling rows on both sides
//							if (rowL.containsKey(key)) {
//								m1.setValueAt(rowL.get(key), m1.getRowCount()-1, m1.findColumn(key));
//								m2.setValueAt(rowL.get(key), m2.getRowCount()-1, m2.findColumn(key));
//							} else {
//								m1.setValueAt(null, m1.getRowCount()-1, m1.findColumn(key));
//								m2.setValueAt(null, m2.getRowCount()-1, m2.findColumn(key));
//							}
							// overwrite diffs on right side
							if (diffs.containsKey(key)) {
								int col = m2.findColumn(key);
								m2.setValueAt(diffs.get(key), row, col);
								m1.setCellColour(row, col, LIGHT_RED);
								m2.setCellColour(row, col, LIGHT_RED);
							}
						}

					}
					if (btnDownload.isSelected()) {
						for (int j = 0; j < updateLocally.size(); j++) {
							notEmpty = true;
							TreeMap<String,String> rowL = updateLocally.get(j).getLeft();
							TreeMap<String,String> diffs = updateLocally.get(j).getRight();
							m1.addRow(new String[0]);
							m2.addRow(new String[0]);

							int row = m1.getRowCount()-1;
							// update action label 
							m1.setValueAt("U", row, m1.findColumn("A"));
							m1.setCellColour(row, 0, LIGHT_BLUE);
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
								if (diffs.containsKey(key)) {
									int col = m1.findColumn(key);
									m1.setValueAt(diffs.get(key), row, col);
									m2.setValueAt(diffs.get(key), row, col);
									m1.setCellColour(row, col, LIGHT_BLUE);
								}
							}
						}
					}
					if (btnUpload.isSelected()) {
						for (int j = 0; j < updateRemotely.size(); j++) {
							notEmpty = true;
							TreeMap<String,String> rowL = updateRemotely.get(j).getLeft();
							TreeMap<String,String> diffs = updateRemotely.get(j).getRight();
							m1.addRow(new String[0]);
							m2.addRow(new String[0]);

							int row = m2.getRowCount()-1;
							// update action label 
							m2.setValueAt("U", row, m1.findColumn("A"));
							m2.setCellColour(row, 0, LIGHT_GREEN);

							for (String key : commonFields) {
								// filling rows on both sides. if val = null, fill with null.
								if (rowL.containsKey(key)) {
									m1.setValueAt(rowL.get(key), m1.getRowCount()-1, m1.findColumn(key));
									m2.setValueAt(rowL.get(key), m2.getRowCount()-1, m2.findColumn(key));
								} else {
									m1.setValueAt(null, m1.getRowCount()-1, m1.findColumn(key));
									m2.setValueAt(null, m2.getRowCount()-1, m2.findColumn(key));
								}
								// overwrite diffs on left side
								if (diffs.containsKey(key)) {
									int col = m2.findColumn(key);
									m1.setValueAt(diffs.get(key), row, col);
									m2.setValueAt(diffs.get(key), row, col);
									m2.setCellColour(row, col, LIGHT_GREEN);
								}
							}
						}
					}
				}
				if (btnIndividuals.isSelected()) {
					if (btnUpload.isSelected()) {
						for (int j = 0; j < upload.size(); j++) {
							TreeMap<String,String> row = upload.get(j);
							notEmpty = true;
							m1.addRow(new String[0]);
							int r = m1.getRowCount()-1;
							for (String key : commonFields) {
								int c = m1.findColumn(key);
								if (row.containsKey(key)) 
									m1.setValueAt(row.get(key), r, c);
								else
									m1.setValueAt(null, r, c);

								m1.setCellColour(r, c, LIGHT_GREEN);
							}
							m2.addRow(new String[] {});// update action label
							m1.setValueAt("UL", r, 0);
							m1.setCellColour(r, 0, LIGHT_GREEN);
						}
					}
					if (btnDownload.isSelected()) {
						for (int j = 0; j < download.size(); j++) {
							TreeMap<String,String> row = download.get(j);
							notEmpty = true;
							m2.addRow(new String[0]);
							int r = m2.getRowCount()-1;
							for (String key : commonFields) {
								int c = m2.findColumn(key);
								if (row.containsKey(key)) 
									m2.setValueAt(row.get(key), r, c);
								else
									m2.setValueAt(null, r, c);

								m2.setCellColour(r, c, LIGHT_BLUE);
							}
							m1.addRow(new String[] {});
							m2.setValueAt("DL", r, 0);
							m2.setCellColour(r, 0, LIGHT_BLUE);
						}
					}
				}
			}
			
			table1.setModel(m1);
			table2.setModel(m2);

			table1.getColumnModel().getColumn(0).setMaxWidth(25);
			table2.getColumnModel().getColumn(0).setMaxWidth(25);

			if (notEmpty)
				tabs.addTab(p.getLeft(), null, outerScroll, p.toString());

			JTable[] tableList = new JTable[] {table1, table2};
			tables.put(tabs.getTabCount()-1, tableList);
		}
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (tabs.getTitleAt(i).equals(lastTab)) {
				tabs.setSelectedIndex(i);
				break;
			}
		}
	}

	protected void abortMission() {
		if (JOptionPane.showConfirmDialog(this,
				"Are you sure to cancel the operation? No changes will apply.",
				"Really closing?", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			comps = null;
			dispose();
		}
	}
	protected boolean startMission() {
		if (JOptionPane.showConfirmDialog(this,
				"Are you sure to apply all changes to the databases?",
				"Really applying changes?", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			return true;
		}
		return false;
	}

	/**
	 * Create the frame.
	 * 
	 * @param txtLog
	 */
	public ComparisonFrame(JTextArea txtLog) {
		this.txtLog = txtLog;
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
			worker = new ProgressWorker(txtLog, ProgressWorker.JOB_CALC_DIFF) {
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

		try {
			worker.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		comps = new TreeMap<Pair, ComparisonResult>();
	}

	/**
	 * method to determine the actual row in the db table from the given row no. in JTable
	 * @param currTab current table name
	 * @param row row number in current JTable
	 * @return complete row of db
	 */
	private TreeMap<String, String> getTupleToMatchingRow(JTable tab, Vector<TreeMap<String, String>> vector, int row) {
		TreeMap<String,String> resultRow = new TreeMap<String,String>();
		boolean res = false;
//		if (vector.size() == 1) return vector.firstElement();
		
		for (TreeMap<String, String> t : vector) {
			for (int c = 1; c < tab.getColumnCount(); c++) {
				// jtable value eq. db value?
				String v = t.get(tab.getColumnName(c));
				if (!(tab.getValueAt(row, c)+"").equals(v)) {
					res = false;
					break;
				} else res = true;
			}
			if (res) {
				for (int c = 1; c < tab.getColumnCount(); c++) {
					resultRow.put(tab.getColumnName(c), (String)tab.getValueAt(row, c));
				}
			}
		}

		return resultRow;
	}

	public TreeMap<Pair, ComparisonResult> showDialog(JFrame parent) {
		this.pack();
		ModalFrameUtil.showAsModal(this, parent);
		return comps;
	}

	public class ColourCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			ColourTableModel model = (ColourTableModel) table.getModel();
			String toolTip;
			if (column == 0 && model.getColumnName(column).equals("A"))
				switch (model.getValueAt(row, column) == null ? "" : model.getValueAt(row, column).toString()) {
				case "U": toolTip = "U: This row will be updated on this side."; break;
				case "C": toolTip = "C: This row differs on both sides. Conflict! Has to be resolved."; break;
				case "DL": toolTip = "DL: This row will be downloaded."; break;
				case "UL": toolTip = "UL: This row will be uploaded."; break;
				case "??": toolTip = "??: It is not determinable if this row shall be downloaded or deleted. Conflict! Has to be resolved."; break;
				case "D": toolTip = "D: This row will be deleted on both sides."; break;
				default: toolTip = ""; break;
				}
			else {
				toolTip = table.getColumnName(column) + ": " + value;
			}
			setToolTipText(toolTip);

			Component c = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			Color color = model.getCellColour(row, column);
			c.setBackground(color);
			if (!color.equals(Color.WHITE))
				c.setFont(c.getFont().deriveFont(Font.BOLD));
			return c;
		}
	}

	static class ColourTableModel extends DefaultTableModel {
		ArrayList<ArrayList<Color>> cellColours;
		public ColourTableModel(Object[] array, int rowCount) {
			super(array, rowCount);
			cellColours = new ArrayList<ArrayList<Color>>();
		}
		@Override
		public void addRow(Object[] rowData) {
			cellColours.add(new ArrayList<Color>());
			super.addRow(rowData);
		}
		@Override
		public void addRow(Vector rowData) {
			cellColours.add(new ArrayList<Color>());
			super.addRow(rowData);
		}
		public void setCellColour (int row, int col, Color c) {
			ArrayList<Color> r = cellColours.get(row);
			if (r.isEmpty()) {
				for (int i = 0; i < this.getColumnCount(); i++) {
					r.add(null);
				}
			}
			r.set(col, c);
		}
		public Color getCellColour (int row, int col) {
			ArrayList<Color> r = cellColours.get(row);
			if (r.isEmpty()) {
				for (int i = 0; i < this.getColumnCount(); i++) {
					r.add(null);
				}
			}
			return r.get(col) == null ? Color.WHITE : r.get(col);
		}
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	}

	class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JTable source = (JTable)e.getSource();
				JTable[] currTables = tables.get(tabs.getSelectedIndex());

				String label = "";
				int row = source.rowAtPoint( e.getPoint() );
				int column = source.columnAtPoint( e.getPoint() );

				if (!source.isRowSelected(row))
					source.changeSelection(row, column, false, false);

				// empty label? try other table! (rhymes, lol)
				if (source.getValueAt(row, 0) == null) {
					System.out.println(tabs.getTitleAt(tabs.getSelectedIndex()));
					if (source.equals(currTables[0])) source = currTables[1];
					else source = currTables[0];
				}

				label = (String)source.getValueAt(row, 0);

				popup.removeAll();
				JMenuItem menuItem = null;
				// conflict
				if (label.equals("C")) {
					menuItem = new JMenuItem("Resolve conflict ...");
					menuItem.addActionListener(new ActionListener(){
						// click on menu item
						@Override
						public void actionPerformed(ActionEvent arg0) {
							simpleReloadTables(true);
						}
					});
					popup.add(menuItem);
				}
				// delete or download?
				if (label.equals("??")) {
					menuItem = new JMenuItem("Download from remote");
					menuItem.addActionListener(new ActionListener(){
						// click on menu item
						@Override
						public void actionPerformed(ActionEvent arg0) {
							ComparisonResult c = null;
							String tab = tabs.getTitleAt(tabs.getSelectedIndex());
							TreeMap<String,String> tabRow = new TreeMap<String,String>();
							TableModel m = currTables[1].getModel();
							
							for (Pair pa : commonTables) {
								if (pa.getFMString().equals(tab)) {
									// get lists for current tab
									c = comps.get(pa); break;
								}
							}
							for (int i = 1; i < m.getColumnCount(); i++) {
								String val = m.getValueAt(row, i) == null ? null : (String)m.getValueAt(row, i);
								tabRow.put(m.getColumnName(i), val);
							}
							if (c != null) {
								// delete from unsure list
								for (Tuple<TreeMap<String, String>, TreeMap<String, String>> t : c.getDeleteOrDownloadList()) {
									TreeMap<String, String> maa = t.getRight();
									if (maa.equals(tabRow)) {
										c.getDeleteOrDownloadList().remove(t);
										// add to download list
										c.addToDownloadList(tabRow);
										break;
									}
								}
							}
							simpleReloadTables(true);
						}
					});
					popup.add(menuItem);
					menuItem = new JMenuItem("Delete on both sides");
					menuItem.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e) {
							ComparisonResult c = null;
							String tab = tabs.getTitleAt(tabs.getSelectedIndex());
							TreeMap<String,String> tabRow = new TreeMap<String,String>();
							ColourTableModel m = (ColourTableModel) currTables[1].getModel();
							
							for (Pair pa : commonTables) {
								if (pa.getFMString().equals(tab)) {
									// get lists for current tab
									c = comps.get(pa); break;
								}
							}
							System.out.println("rows: "+m.getRowCount() + " cols: "+m.getColumnCount()+ " clickedrow:"+row);
							for (int i = 1; i < m.getColumnCount(); i++) {
								String val = m.getValueAt(row, i) == null ? null : (String)m.getValueAt(row, i);
								tabRow.put(m.getColumnName(i), val);
							}
							if (c != null) {
								// delete from unsure list
								for (Tuple<TreeMap<String, String>, TreeMap<String, String>> t : c.getDeleteOrDownloadList()) {
									if (t.getRight().equals(tabRow)) {
										c.getDeleteOrDownloadList().remove(t);
										// add to delete list
										c.addToDeleteList(t.getLeft(), t.getRight());
										m.fireTableDataChanged();
										break;
									}
								}
							}
							simpleReloadTables(true);
						}
					});
					popup.add(menuItem);
				}

				if (popup.getComponentCount() != 0)
					popup.show(e.getComponent(),e.getX(), e.getY());
			}
		}
	}

	class JResizeTable extends JTable {

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
	private static final class JGradientButton extends JToggleButton{
		String s;

		public JGradientButton(String str) {
			super(str);
			s = str;
			setContentAreaFilled(false);
		}
		
		@Override
		protected void paintComponent(Graphics g){
			if (this.isSelected()) {
				int w = getWidth();
				int h = getHeight();
				g.setColor(getBackground()); // selected color
				g.fillRect(0, 0, w, h);
			} else {
				Graphics2D g2 = (Graphics2D)g.create();
				g2.setPaint(new GradientPaint(
						new Point(0, 0), 
						getBackground(), 
						new Point(0, getHeight()/3), 
						Color.WHITE));
				g2.fillRect(0, 0, getWidth(), getHeight()/3);
				g2.setPaint(new GradientPaint(
						new Point(0, getHeight()/3), 
						Color.WHITE, 
						new Point(0, getHeight()), 
						getBackground()));
				g2.fillRect(0, getHeight()/3, getWidth(), getHeight());
				g2.dispose();
			}
			super.paintComponent(g);
		}
	}
}
