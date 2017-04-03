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
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

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
	private JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
	private JRadioButton chkTimestamp = new JRadioButton("by Timestamp");
	private JRadioButton chkContent = new JRadioButton("by Content");
	private JButton btnReload = new JButton("Reload tables");
	private JToggleButton btnDownload = new JToggleButton("Pairs");
	private JToggleButton btnToLocal = new JToggleButton("");
	private JToggleButton btnToRemote = new JToggleButton("");
	private JToggleButton btnUpload = new JToggleButton("Individuals");
	private JToggleButton btnEqual = new JToggleButton("");
	private JToggleButton btnUnequal = new JToggleButton("");

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
		loadTables();
		
		JPanel pnlTop = new JPanel();
		container.add(pnlTop, BorderLayout.NORTH);
		pnlTop.setLayout(new BorderLayout(0, 0));
		
		JPanel pnlFilters = new JPanel();
		pnlFilters.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Filters", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		pnlTop.add(pnlFilters, BorderLayout.WEST);
		pnlFilters.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30px"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("30px"),
				FormFactory.UNRELATED_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("50dlu", true), Sizes.constant("70dlu", true)), 0),
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("25px"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("25px"),
				FormFactory.RELATED_GAP_ROWSPEC,}));
		
		btnUnequal.setSelected(true);
		btnUnequal.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnUnequal, "2, 2, default, fill");
		
		btnEqual.setSelected(true);
		btnEqual.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnEqual, "4, 2, default, fill");
		
		btnUpload.setSelected(true);
		btnUpload.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnUpload, "6, 2, default, center");
		
		btnToRemote.setSelected(true);
		btnToRemote.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnToRemote, "2, 4, default, fill");
		
		btnToLocal.setSelected(true);
		btnToLocal.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnToLocal, "4, 4, default, fill");
		
		btnDownload.setSelected(true);
		btnDownload.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnDownload, "6, 4, default, center");
		
		JPanel pnlOptions = new JPanel();
		pnlOptions.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		pnlTop.add(pnlOptions, BorderLayout.CENTER);
		pnlOptions.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
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
				FormFactory.UNRELATED_GAP_COLSPEC,
				FormFactory.BUTTON_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:default"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:default"),
				FormFactory.RELATED_GAP_ROWSPEC,}));
		
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
	}
	
	private void loadTables() {
		tabs.removeAll();
		try {
			commonTables = data.fetchCommonTables();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "I was unable to fetch tables from the databases.", "Unable to fetch tables", JOptionPane.ERROR_MESSAGE);
			dispose();
		}
		ArrayList<ComparisonResult> comps = data.getResults();
		
		for (int i = 0; i < comps.size(); i++) {
			ComparisonResult comp = comps.get(i);
			Pair p = comp.getTableName();
			ArrayList<String> commonFields = null;
			try {
				commonFields = data.getCommonFields(p);
			} catch (SQLException e) {
				JOptionPane.showMessageDialog(this, "I was unable to fetch common fields from the databases.", "Unable to fetch fields", JOptionPane.ERROR_MESSAGE);
				dispose();
			}
			
			Vector<Tuple<Vector<Pair>, Vector<Pair>>> conflict = comp.getConflictList();
			Vector<Tuple<Vector<Pair>, Vector<Pair>>> updateLocally = comp.getLocalUpdateList();
			Vector<Tuple<Vector<Pair>, Vector<Pair>>> updateRemotely = comp.getRemoteUpdateList();
			Vector<Integer> download = comp.getDownloadList();
			Vector<Vector<Pair>> upload = comp.getUploadList();
			
			JTable table1 = new JTable(){
                @Override
                public Point getToolTipLocation(MouseEvent event) {
                    return new Point(10, 10);
                }
            };
            table1.setDefaultRenderer(Object.class, new TestCellRenderer());
			JTable table2 = new JTable(){
                @Override
                public Point getToolTipLocation(MouseEvent event) {
                    return new Point(10, 10);
                }
            };
            table2.setDefaultRenderer(Object.class, new TestCellRenderer());
            
			JScrollPane innerLeftScroll = new JScrollPane(table1);
			JScrollPane innerRightScroll = new JScrollPane(table2);
			JPanel outerScroll = new JPanel();
			outerScroll.setLayout(new GridLayout(1, 2));
			
			//setting policies
			innerLeftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			innerLeftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			innerRightScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			innerRightScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			innerLeftScroll.getVerticalScrollBar().setModel(innerRightScroll.getVerticalScrollBar().getModel());
			
			outerScroll.add(innerLeftScroll);
			outerScroll.add(innerRightScroll);
			DefaultTableModel m1 = new DefaultTableModel();
			DefaultTableModel m2 = new DefaultTableModel();
			
			Vector<String> header = new Vector<String>();
			for (int j = 0; j < commonFields.size(); j++) {
				header.add(commonFields.get(j));
			}
			m1.setColumnIdentifiers(header);
			m2.setColumnIdentifiers(header);
			
			if (btnUnequal.isSelected()) {
				for (int j = 0; j < conflict.size(); j++) {
					m1.addRow(conflict.get(j).getLeft());
					m2.addRow(conflict.get(j).getRight());
				}
				for (int j = 0; j < upload.size(); j++) {
					m1.addRow(upload.get(j));
					m2.addRow(new String[]{});
				}
			}
			
			table1.setModel(m1);
			table2.setModel(m2);
			
			tabs.addTab(p.getLeft(), null, outerScroll, p.toString());
		}
	}

	protected void abortMission() {
		if (JOptionPane.showConfirmDialog(this, 
				"Are you sure to cancel the operation? No changes will apply.", "Really Closing?", 
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
			comp = null;
			dispose();
		}
	}

	/**
	 * Create the frame.
	 */
	public ComparisonFrame() {
		try {
			data = SQLDataModel.getInstance();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TODO doppelte progressbar 
		TableProgressDialog progress = new TableProgressDialog(MainFrame.getLog());
		progress.setLocationRelativeTo(this);
		int res = progress.showAndStart();

		initialize();
		comp = new ArrayList<ComparisonResult>();
	}

	public ArrayList<ComparisonResult> showDialog(JFrame parent) {
		this.pack();
		ModalFrameUtil.showAsModal(this, parent);
		return comp;
	}
	
	public class TestCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setToolTipText("Banana @ " + value.toString());
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

    }
}
