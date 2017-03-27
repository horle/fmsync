package ceramalex.sync.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker.StateValue;

public class TableProgressDialog extends JDialog {
	
	private JProgressBar proEntire = new JProgressBar();
	private JProgressBar proTab = new JProgressBar();
	private JLabel lblCurrTab = new JLabel("nix");
	private JLabel lblEntire = new JLabel("nix");

	private JTextArea txtLog;
	
	/**
	 * Create the dialog.
	 */
	public TableProgressDialog(JTextArea txtLog) {
		this.txtLog = txtLog;
		
		setModal(true);
		setTitle("Processing ...");
		setBounds(100, 100, 400, 220);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 434, 0, 0};
		gridBagLayout.rowHeights = new int[]{10, 14, 0, 14, 14, 0, 14, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel("Entire progress:");
		lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 1;
		gbc_lblNewLabel.gridy = 1;
		getContentPane().add(lblNewLabel, gbc_lblNewLabel);
		
		lblEntire.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblEntire = new GridBagConstraints();
		gbc_lblEntire.anchor = GridBagConstraints.WEST;
		gbc_lblEntire.insets = new Insets(0, 0, 5, 5);
		gbc_lblEntire.gridx = 1;
		gbc_lblEntire.gridy = 2;
		getContentPane().add(lblEntire, gbc_lblEntire);
		
		proEntire.setFont(new Font("Dialog", Font.PLAIN, 12));
		proEntire.setPreferredSize(new Dimension(146, 18));
		proEntire.setMinimumSize(new Dimension(10, 18));
		GridBagConstraints gbc_proEntire = new GridBagConstraints();
		gbc_proEntire.fill = GridBagConstraints.HORIZONTAL;
		gbc_proEntire.insets = new Insets(0, 0, 5, 5);
		gbc_proEntire.gridx = 1;
		gbc_proEntire.gridy = 3;
		getContentPane().add(proEntire, gbc_proEntire);
		
		JLabel lblNewLabel_1 = new JLabel("Current Table:");
		lblNewLabel_1.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 4;
		getContentPane().add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		lblCurrTab.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblCurrTab = new GridBagConstraints();
		gbc_lblCurrTab.anchor = GridBagConstraints.WEST;
		gbc_lblCurrTab.insets = new Insets(0, 0, 5, 5);
		gbc_lblCurrTab.gridx = 1;
		gbc_lblCurrTab.gridy = 5;
		getContentPane().add(lblCurrTab, gbc_lblCurrTab);
		
		proTab.setFont(new Font("Dialog", Font.PLAIN, 12));
		proTab.setMinimumSize(new Dimension(10, 18));
		proTab.setPreferredSize(new Dimension(146, 18));
		GridBagConstraints gbc_proTab = new GridBagConstraints();
		gbc_proTab.fill = GridBagConstraints.HORIZONTAL;
		gbc_proTab.insets = new Insets(0, 0, 5, 5);
		gbc_proTab.gridx = 1;
		gbc_proTab.gridy = 6;
		getContentPane().add(proTab, gbc_proTab);
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO interrupt query
				dispose();
			}
		});
		btnCancel.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.insets = new Insets(0, 0, 5, 5);
		gbc_btnCancel.gridx = 1;
		gbc_btnCancel.gridy = 7;
		getContentPane().add(btnCancel, gbc_btnCancel);

	}

	public void showAndStart() {
		ProgressWorker worker = new ProgressWorker(lblEntire, lblCurrTab, txtLog);
		worker.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				switch (event.getPropertyName()) {
				case "progress":
					proTab.setIndeterminate(false);
					proTab.setValue((int) event.getNewValue());
					break;
				case "state":
					switch ((StateValue) event.getNewValue()) {
					case DONE: dispose(); break;
					}
				}
			}
		});
		worker.execute();
		
		setVisible(true);
	}

}
