package gui;

import gui.ConnectionWorker.SyncStatus;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public abstract class ConnectionWorker extends SwingWorker<Void, SyncStatus> {
	
	// gui elements to update
	private final JTextArea txtLog;
	private final JButton btnConnect;
	private final JLabel lblConnect;
	private final JFormattedTextField txtPort;
	private final JTextField txtAddress;
	
	/**
	 * Creates a worker instance
	 * 
	 * @param a address from textfield
	 * @param p port from textfield
	 * @param u user from textfield
	 * @param pw password from textfield
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
	
	//verarbeite status in der gui
	@Override
	protected void process(List<SyncStatus> statusList){
		SyncStatus last = statusList.get(statusList.size()-1);
		lblConnect.setText(last.lblConnect);
		btnConnect.setText(last.btnConnectText);
		btnConnect.setEnabled(last.btnConnectEn);
		txtPort.setEnabled(last.txtPortEn);
		txtAddress.setEnabled(last.txtAddressEn);
		txtLog.append(last.logMsg);
	}
	
	protected class SyncStatus {
		private String logMsg;
		private String lblConnect;
		private String btnConnectText;
		private boolean btnConnectEn;
		private boolean txtAddressEn;
		private boolean txtPortEn;
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
