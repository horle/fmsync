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
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;

public class TableProgressDialog extends JDialog {
	
	private JProgressBar proTotal = new JProgressBar();
	private JTextArea txtLog;
	private JLabel lblCurrTab = new JLabel("Current Table:");
	
	/**
	 * Create the dialog.
	 */
	public TableProgressDialog(JTextArea txtLog) {
		setResizable(false);
		this.txtLog = txtLog;
		
		setModal(true);
		setTitle("Processing ...");
		setBounds(100, 100, 400, 135);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 434, 0, 0};
		gridBagLayout.rowHeights = new int[]{10, 14, 20, 14, 1, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		lblCurrTab.setFont(new Font("Dialog", Font.PLAIN, 12));
		GridBagConstraints gbc_lblCurrTab = new GridBagConstraints();
		gbc_lblCurrTab.anchor = GridBagConstraints.WEST;
		gbc_lblCurrTab.insets = new Insets(0, 0, 5, 5);
		gbc_lblCurrTab.gridx = 1;
		gbc_lblCurrTab.gridy = 1;
		getContentPane().add(lblCurrTab, gbc_lblCurrTab);
		
		proTotal.setFont(new Font("Dialog", Font.PLAIN, 12));
		proTotal.setPreferredSize(new Dimension(146, 18));
		proTotal.setMinimumSize(new Dimension(10, 18));
		GridBagConstraints gbc_proEntire = new GridBagConstraints();
		gbc_proEntire.fill = GridBagConstraints.HORIZONTAL;
		gbc_proEntire.insets = new Insets(0, 0, 5, 5);
		gbc_proEntire.gridx = 1;
		gbc_proEntire.gridy = 2;
		getContentPane().add(proTotal, gbc_proEntire);
		
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
		gbc_btnCancel.gridy = 3;
		getContentPane().add(btnCancel, gbc_btnCancel);

	}

	public int showAndStart() {
		int result = 0;
		try {
			ProgressWorker worker = new ProgressWorker(lblCurrTab, txtLog);
			worker.addPropertyChangeListener(new PropertyChangeListener() {
	
				@Override
				public void propertyChange(final PropertyChangeEvent event) {
					switch (event.getPropertyName()) {
					case "progress":
						proTotal.setIndeterminate(false);
						proTotal.setValue((int) event.getNewValue());
						break;
					case "state":
						switch ((StateValue) event.getNewValue()) {
						case DONE:
							dispose(); break;
						}
					}
				}
			});
			worker.execute();
			
			setVisible(true);
			return result;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "An error occured.", "Error", JOptionPane.ERROR_MESSAGE);
			return -1;
		}
	}
}
