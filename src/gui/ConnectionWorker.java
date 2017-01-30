package gui;

import gui.ConnectionWorker.SyncStatus;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Abstract SwingWorker class to update GUI elements in main window
 * 
 * @author horle (Felix Kussmaul)
 */

public abstract class ConnectionWorker extends SwingWorker<Boolean, SyncStatus> {
	
	// GUI elements to update
	private final JTextArea txtLog;
	private final JButton btnConnect;
	private final JLabel lblConnect;
	private final JFormattedTextField txtPort;
	private final JTextField txtAddress;
	
	/**
	 * Constructor: Creates a SwingWorker instance
	 * 
	 * @param tLog logging textarea
	 * @param bConnect connect button
	 * @param lConnect connect label
	 * @param tPort textfield port
	 * @param tAddress textfield address
	 */
	public ConnectionWorker(JTextArea tLog, JButton bConnect, JLabel lConnect, JFormattedTextField tPort, JTextField tAddress) {
		this.txtAddress = tAddress;
		this.btnConnect = bConnect;
		this.lblConnect = lConnect;
		this.txtPort = tPort;
		this.txtLog = tLog;
	}
	
	/**
	 * Process all GUI elements from publish calls (list)
	 * 
	 * @param statusList list of SyncStatus
	 */
	@Override
	protected void process(List<SyncStatus> statusList){
		for (SyncStatus status : statusList) {
			lblConnect.setText(status.lblConnect);
			btnConnect.setText(status.btnConnectText);
			btnConnect.setEnabled(status.btnConnectEn);
			txtPort.setEnabled(status.txtPortEn);
			txtAddress.setEnabled(status.txtAddressEn);
			txtLog.append(status.logMsg);
		}
	}
	
	/**
	 * Private class ("struct") for representing GUI element status
	 * 
	 * @author horle (Felix Kussmaul)
	 */
	protected class SyncStatus {
		private String logMsg;
		private String lblConnect;
		private String btnConnectText;
		private boolean btnConnectEn;
		private boolean txtAddressEn;
		private boolean txtPortEn;
		
		/**
		 * Constructor
		 * 
		 * @param logMsg log message
		 * @param lblConnect connect label
		 * @param btnConnectText connect button label
		 * @param btnConnectEn connect button enabled?
		 * @param txtAddressEn address textfield enabled?
		 * @param txtPortEn port textfield enabled?
		 */
		public SyncStatus(String logMsg, String lblConnect,
				String btnConnectText, boolean btnConnectEn,
				boolean txtAddressEn, boolean txtPortEn) {
			this.logMsg = logMsg;
			this.lblConnect = lblConnect;
			this.btnConnectText = btnConnectText;
			this.btnConnectEn = btnConnectEn;
			this.txtAddressEn = txtAddressEn;
			this.txtPortEn = txtPortEn;
		}
	}
}
