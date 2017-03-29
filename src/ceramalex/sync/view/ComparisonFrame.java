package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import nl.jj.swingx.gui.modal.JModalFrame;

import org.apache.log4j.Logger;

import ceramalex.sync.exception.EntityManagementException;
import ceramalex.sync.exception.FilemakerIsCrapException;
import ceramalex.sync.exception.SyncException;
import ceramalex.sync.model.ComparisonResult;
import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;
import ceramalex.sync.model.Tuple;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

public class ComparisonFrame extends JModalFrame {

	private JPanel container;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	
	private ArrayList<ComparisonResult> comp;
	private SQLDataModel data;
	private ArrayList<Pair> commonTables;
	
	private static Logger logger = Logger.getLogger(ComparisonFrame.class);
	private JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);

	private void initialize() {		
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				abortMission();
			}
		});
		setTitle("Sync databases");
		this.setModal(true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 791, 472);
		container = new JPanel();
		container.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(container);
		container.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollTables = new JScrollPane();
		container.add(scrollTables, BorderLayout.CENTER);
		
		tabs.setFont(new Font("Dialog", Font.PLAIN, 12));
		scrollTables.setViewportView(tabs);
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
		
		JToggleButton btnUnequal = new JToggleButton("");
		btnUnequal.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnUnequal, "2, 2, default, fill");
		
		JToggleButton btnEqual = new JToggleButton("");
		btnEqual.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnEqual, "4, 2, default, fill");
		
		JToggleButton tglbtnUpload = new JToggleButton("Individuals");
		tglbtnUpload.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(tglbtnUpload, "6, 2, default, center");
		
		JToggleButton btnToRemote = new JToggleButton("");
		btnToRemote.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnToRemote, "2, 4, default, fill");
		
		JToggleButton btnToLocal = new JToggleButton("");
		btnToLocal.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(btnToLocal, "4, 4, default, fill");
		
		JToggleButton tglbtnDownload = new JToggleButton("Pairs");
		tglbtnDownload.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlFilters.add(tglbtnDownload, "6, 4, default, center");
		
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
		
		JButton btnReload = new JButton("Reload tables");
		btnReload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadTables();
			}
		});
		pnlOptions.add(btnReload, "2, 2");
		btnReload.setFont(new Font("Dialog", Font.PLAIN, 12));
		
		JRadioButton chckbxNewCheckBox_1 = new JRadioButton("by Timestamp");
		buttonGroup.add(chckbxNewCheckBox_1);
		chckbxNewCheckBox_1.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chckbxNewCheckBox_1, "4, 2");
		
		JCheckBox chckbxNewCheckBox_2 = new JCheckBox("New check box");
		chckbxNewCheckBox_2.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chckbxNewCheckBox_2, "6, 2");
		
		JRadioButton chckbxNewCheckBox = new JRadioButton("by Content");
		buttonGroup.add(chckbxNewCheckBox);
		chckbxNewCheckBox.setFont(new Font("Dialog", Font.PLAIN, 12));
		pnlOptions.add(chckbxNewCheckBox, "4, 4");
		
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
	}
	
	private void loadTables(){
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
			
			ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> conflict = comp.getConflictList();
			ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> updateLocally = comp.getLocalUpdateList();
			ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> updateRemotely = comp.getRemoteUpdateList();
			ArrayList<Integer> download = comp.getDownloadList();
			ArrayList<ArrayList<Pair>> upload = comp.getUploadList();
			
			JTable table1 = new JTable();
			JTable table2 = new JTable();
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1, 2));
			panel.add(table1);
			panel.add(table2);
			
			tabs.addTab(p.getLeft(), null, new JScrollPane(panel), p.toString());
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
	public ComparisonFrame(ArrayList<Pair> commonTables) {
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
		progress.showAndStart();

		initialize();
		comp = new ArrayList<ComparisonResult>();
	}

	public ArrayList<ComparisonResult> showDialog() {
		this.pack();
		this.setVisible(true);
		return comp;
	}
}
