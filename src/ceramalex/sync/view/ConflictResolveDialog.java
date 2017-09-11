package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.ListSelectionModel;

import ceramalex.sync.model.Pair;
import ceramalex.sync.model.Tuple;

public class ConflictResolveDialog extends JDialog {

	private JTable table;
	private JCheckBox chkApplyForAll = new JCheckBox("Apply for all conflicts of this kind");
	private JButton btnMerge = new JButton("Merge");
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnLocal = new JButton("Choose local");
	private JButton btnRemote = new JButton("Choose remote");
	
	private Object[] headers;
	private TreeMap<String,String> localRow;
	private TreeMap<String,String> remoteRow;

	private String label;
	private TreeMap<String, String> result;
	
	public static final int UPDATE_LOCALLY = 1;
	public static final int UPDATE_REMOTELY = 2;
	public static final int SKIP = 0;
	
	private int action = SKIP;
	
	/**
	 * Create the dialog.
	 */
	public ConflictResolveDialog(TreeMap<String,String> lRow, TreeMap<String,String> rRow) {
		label = lRow.remove("A");
		rRow.remove("A");
		
		headers = lRow.keySet().toArray();
		int colCount = lRow.size();
		this.localRow = lRow;
		this.remoteRow = rRow;
		
		String[] localR = new String[colCount];
		String[] remoteR = new String[colCount];
		
		for (int i = 0; i < colCount; i++) {
			localR[i] = lRow.get(headers[i]);
			remoteR[i] = rRow.get(headers[i]);
		}
		
		Font f = new Font("Dialog", Font.PLAIN, 12);
		setResizable(false);
		setTitle("Resolve Conflicts");
		setSize(750, 190);
		setModal(true);
		getContentPane().setLayout(new BorderLayout(5, 0));
		
		JLabel lblText = new JLabel("Please select the row you want to keep.");
		lblText.setBorder(new EmptyBorder(10, 10, 10, 10));
		lblText.setFont(f);
		getContentPane().add(lblText, BorderLayout.NORTH);
		
		JPanel pnlMerge = new JPanel();
		pnlMerge.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(pnlMerge, BorderLayout.SOUTH);

		chkApplyForAll.setFont(f);
		pnlMerge.add(chkApplyForAll);

		btnMerge.setFont(f);
		btnMerge.setActionCommand("OK");
		pnlMerge.add(btnMerge);
		
		btnCancel.setFont(f);
		btnCancel.setActionCommand("Cancel");
		pnlMerge.add(btnCancel);
		
		
		JPanel pnlButtons = new JPanel();
		pnlButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(pnlButtons, BorderLayout.EAST);
		GridBagLayout gbl_pnlButtons = new GridBagLayout();
		gbl_pnlButtons.columnWidths = new int[]{97};
		gbl_pnlButtons.rowHeights = new int[]{23};
		gbl_pnlButtons.columnWeights = new double[]{0.0};
		gbl_pnlButtons.rowWeights = new double[]{0.0};
		pnlButtons.setLayout(gbl_pnlButtons);
		
		btnLocal.setActionCommand("OK");
		btnLocal.setFont(f);
		btnLocal.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (JOptionPane.showConfirmDialog(null,
						"Are you sure to apply the LOCAL row to the online database?",
						"Really applying local row?", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					// choose local row, so update remote row.
					action = UPDATE_REMOTELY;
					result = localRow;
					dispose();
				}
			}
		});
		GridBagConstraints gbc_btnLocal = new GridBagConstraints();
		gbc_btnLocal.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLocal.insets = new Insets(5, 5, 5, 0);
		gbc_btnLocal.gridx = 0;
		gbc_btnLocal.gridy = 0;
		pnlButtons.add(btnLocal, gbc_btnLocal);
		
		btnRemote.setFont(f);
		btnRemote.setActionCommand("OK");
		btnRemote.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (JOptionPane.showConfirmDialog(null,
						"Are you sure to apply the REMOTE row to the local database?",
						"Really applying remote row?", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					// choose remote row, so update local row.
					action = UPDATE_LOCALLY;
					result = localRow;
					dispose();
				}
			}
		});
		GridBagConstraints gbc_btnRemote = new GridBagConstraints();
		gbc_btnRemote.insets = new Insets(5, 5, 0, 0);
		gbc_btnRemote.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemote.gridx = 0;
		gbc_btnRemote.gridy = 1;
		pnlButtons.add(btnRemote, gbc_btnRemote);
		
		ListModel<String> lm = new AbstractListModel<String>() {
			String headers[] = {"Local", "Remote"};
			@Override
			public String getElementAt(int arg0) { return headers[arg0]; }
			@Override
			public int getSize() { return headers.length; }
		};
		
		DefaultTableModel dm = new DefaultTableModel(0, colCount);
		dm.setColumnIdentifiers(headers);
		dm.addRow(localR);
		dm.addRow(remoteR);
		
		table = new JTable(dm);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setCellSelectionEnabled(true);
		
		JList<String> rowHeaders = new JList<String>(lm);
		rowHeaders.setBackground(UIManager.getColor("Label.background"));
		rowHeaders.setFixedCellWidth(50);
		rowHeaders.setFixedCellHeight(table.getRowHeight());
		rowHeaders.setCellRenderer(new RowHeaderRenderer(table));
		
		JScrollPane scroll = new JScrollPane(table);
		scroll.setRowHeaderView(rowHeaders);
		scroll.setBorder(new EmptyBorder(5,5,5,5));
		getContentPane().add(scroll, BorderLayout.CENTER);
	}

	public Tuple<Integer, TreeMap<String, String>> showDialog() {
		this.pack();
		this.setVisible(true);
		return new Tuple<Integer,TreeMap<String,String>>(action, result);
	}
}

class RowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

	RowHeaderRenderer(JTable table) {
		JTableHeader header = table.getTableHeader();
//		setOpaque(true);
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		setHorizontalAlignment(CENTER);
		setForeground(header.getForeground());
		setBackground(header.getBackground());
		setFont(header.getFont());
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value,
			int index, boolean isSelected, boolean cellHasFocus) {
		setText((value == null) ? "" : value.toString());
		return this;
	}
}
