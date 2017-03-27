package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import ceramalex.sync.model.ComparisonResult;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

public class ComparisonDialog extends JDialog {

	private JPanel container;
	private JTable table;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	
	private ComparisonResult comp;

	private void initialize() {
		setModal(true);
		setTitle("Sync databases");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 791, 472);
		container = new JPanel();
		container.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(container);
		container.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollTables = new JScrollPane();
		container.add(scrollTables, BorderLayout.CENTER);
		
		JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		tabs.setFont(new Font("Dialog", Font.PLAIN, 12));
		scrollTables.setViewportView(tabs);
		
		table = new JTable();
		tabs.addTab("New tab", null, table, null);
		
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
		pnlActions.add(btnCancel, "2, 4, fill, fill");
	}
	
	/**
	 * Create the frame.
	 */
	public ComparisonDialog() {
		initialize();
		comp = new ComparisonResult();
	}

	public ComparisonResult showDialog() {
		this.pack();
		this.setVisible(true);
		return comp;
	}
}
