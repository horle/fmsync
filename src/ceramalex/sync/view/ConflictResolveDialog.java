package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class ConflictResolveDialog extends JDialog {

	private JTable table;
	private JCheckBox chkApplyForAll = new JCheckBox("Apply for all conflicts of this kind");
	private JButton btnMerge = new JButton("Merge");
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnLocal = new JButton("Choose local");
	private JButton btnRemote = new JButton("Choose remote");

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ConflictResolveDialog dialog = new ConflictResolveDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public ConflictResolveDialog() {
		Font f = new Font("Dialog", Font.PLAIN, 12);
		setTitle("Resolve Conflicts");
		setSize(750, 180);
		getContentPane().setLayout(new BorderLayout(5, 0));
		
		JLabel lblText = new JLabel("You have to resolve this shit!");
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
		GridBagConstraints gbc_btnLocal = new GridBagConstraints();
		gbc_btnLocal.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLocal.insets = new Insets(5, 5, 5, 0);
		gbc_btnLocal.gridx = 0;
		gbc_btnLocal.gridy = 0;
		pnlButtons.add(btnLocal, gbc_btnLocal);
		
		btnRemote.setFont(f);
		btnLocal.setActionCommand("OK");
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
		
		DefaultTableModel dm = new DefaultTableModel(lm.getSize(), 10);
		table = new JTable(dm);
		JList<String> rowHeaders = new JList(lm);
		rowHeaders.setFixedCellWidth(50);
		rowHeaders.setFixedCellHeight(table.getRowHeight());
		rowHeaders.setCellRenderer(new RowHeaderRenderer(table));
		JScrollPane scroll = new JScrollPane(table);
		scroll.setRowHeaderView(rowHeaders);
		scroll.setBorder(new EmptyBorder(5,5,5,5));
		getContentPane().add(scroll, BorderLayout.CENTER);
		
	
	}

}

class RowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

	RowHeaderRenderer(JTable table) {
		JTableHeader header = table.getTableHeader();
		setOpaque(true);
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
