package ceramalex.sync.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.text.JTextComponent;

import ceramalex.sync.controller.SQLAccessController;

/**
 * Abstract SwingWorker class to update GUI elements in main window
 * 
 * @author horle (Felix Kussmaul)
 */

public abstract class ConnectionWorker extends SwingWorker<Boolean, FrameStatus> {

	private SQLAccessController sqlControl;
	// GUI elements to update
	private final JButton btnStart;
	private final JButton btnConnect;
	private final JTextArea txtLog;

	/**
	 * Constructor: Creates a SwingWorker instance
	 * 
	 * @param tLog
	 *            logging textarea
	 * @param bConnect
	 *            connect button
	 * @param bStart
	 *            start button
	 */
	public ConnectionWorker(JButton bConnect, JButton bStart, JTextArea tLog) {

		this.btnConnect = bConnect;
		this.btnStart = bStart;
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
			btnConnect.setText(status.getBtnConnectText());
			txtLog.append(status.getLogMsg());
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
