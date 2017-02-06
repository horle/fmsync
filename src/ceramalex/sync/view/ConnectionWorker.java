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

public abstract class ConnectionWorker extends SwingWorker<Boolean, SyncStatus> {

	private SQLAccessController sqlControl;
	// GUI elements to update
	private final JTextArea txtLog;
	private final JButton btnConnect;
	private final JButton btnStart;
	private final JLabel lblConnectMySQL;
	private final JLabel lblConnectFM;
	// list of textfields that have just to be enabled
	private final ArrayList<JTextComponent> txtList;
	// list of elements in action panel to be enabled
	private final ArrayList<JComponent> actionList;

	/**
	 * Constructor: Creates a SwingWorker instance
	 * 
	 * @param tLog
	 *            logging textarea
	 * @param bConnect
	 *            connect button
	 * @param lConnect
	 *            connect label
	 * @param tPort
	 *            textfield port
	 * @param tAddressMS
	 *            textfield mysql address
	 * @param tAddressFM
	 *            textfield fm address
	 * @param tDBMS
	 *            textfield mysql db
	 * @param tDBFM
	 *            textfield fm db
	 * @param tUserMS
	 *            textfield mysql user
	 * @param tUserFM
	 *            textfield fm user
	 * @param tPassMS
	 *            textfield mysql pass
	 * @param tPassFM
	 *            textfield fm pass
	 * @param btnDownload
	 *            toggle btn download
	 * @param btnUpload
	 *            toggle btn upload
	 * @param btnStart
	 *            start btn
	 * @param cmbPlace
	 *            dropdown place
	 * @param cmbAuthor
	 *            dropdown author
	 */
	public ConnectionWorker(JTextArea tLog, JButton bConnect,
			JLabel lConnectMS, JLabel lConnectFM, JFormattedTextField tPort,
			JTextField tAddressMS, JTextField tAddressFM, JTextField tDBMS,
			JTextField tDBFM, JTextField tUserMS, JTextField tUserFM,
			JTextField tPassMS, JTextField tPassFM, JToggleButton btnUpload,
			JToggleButton btnDownload, JButton btnStart,
			JComboBox<String> cmbAuthor, JComboBox<String> cmbPlace) {

		this.btnConnect = bConnect;
		this.btnStart = btnStart;
		this.lblConnectMySQL = lConnectMS;
		this.lblConnectFM = lConnectFM;
		this.txtLog = tLog;

		this.txtList = new ArrayList<JTextComponent>();
		this.actionList = new ArrayList<JComponent>();

		txtList.add(tAddressMS);
		txtList.add(tAddressFM);
		txtList.add(tAddressMS);
		txtList.add(tPort);
		txtList.add(tDBMS);
		txtList.add(tDBFM);
		txtList.add(tUserMS);
		txtList.add(tUserFM);
		txtList.add(tPassMS);
		txtList.add(tPassFM);
		
		actionList.add(btnUpload);
		actionList.add(btnDownload);
		actionList.add(cmbAuthor);
		actionList.add(cmbPlace);
	}

	/**
	 * Process all GUI elements from publish calls (list)
	 * 
	 * @param statusList
	 *            list of SyncStatus
	 */
	@Override
	protected void process(List<SyncStatus> statusList) {
		for (SyncStatus status : statusList) {
			lblConnectMySQL.setText(status.getLblConnectMySQL());
			lblConnectFM.setText(status.getLblConnectFM());
			btnConnect.setText(status.getBtnConnectText());
			txtLog.append(status.getLogMsg());

			btnConnect.setEnabled(status.isBtnConnectEn());

			for (JTextComponent comp : txtList) {
				comp.setEnabled(status.isTxtEn());
			}
			for (JComponent comp : actionList) {
				comp.setEnabled(status.isActionEn());
			}
		}
	}

	public SQLAccessController getSqlControl() {
		return sqlControl;
	}

	public void setSqlControl(SQLAccessController sqlControl) {
		this.sqlControl = sqlControl;
	}
}
