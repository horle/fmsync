package ceramalex.sync.view;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import ceramalex.sync.controller.SQLAccessController;

/**
 * Abstract SwingWorker class to update GUI elements in main window
 * 
 * @author horle (Felix Kussmaul)
 */

public abstract class ConnectionWorker extends SwingWorker<Boolean, FrameStatus> {

	private SQLAccessController sqlControl;
	// GUI elements to update
	private final JButton btnCancel;
	private final JButton btnConnect;
	private final JTextArea txtLog;

	/**
	 * Constructor: Creates a SwingWorker instance
	 * 
	 * @param tLog
	 *            logging textarea
	 * @param bConnect
	 *            connect button
	 * @param bCancel
	 *            start button
	 */
	public ConnectionWorker(JButton bConnect, JButton bCancel, JTextArea tLog) {

		this.btnConnect = bConnect;
		this.btnCancel = bCancel;
		this.txtLog = tLog;
	}

	/**
	 * Process all GUI elements from publish calls (list)
	 * 
	 * @param statusList
	 *            list of SyncStatus
	 */
	@Override
	protected void process(List<FrameStatus> statusList) {
		for (FrameStatus status : statusList) {
			txtLog.append(status.getLogMsg());
			btnCancel.setEnabled(status.isBtnCancelEn());
			btnConnect.setEnabled(status.isBtnConnectEn());
		}
	}

	public SQLAccessController getSQLControl() {
		return sqlControl;
	}

	public void setSQLControl(SQLAccessController sqlControl) {
		this.sqlControl = sqlControl;
	}
}
