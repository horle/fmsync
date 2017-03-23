package ceramalex.sync.view;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Private class ("struct") for representing GUI element status
 * 
 * @author horle (Felix Kussmaul)
 */
class FrameStatus {
	private String lblConnectFM;
	private String lblConnectMySQL;
	private String logMsg;
	private String btnConnectText;
	private boolean btnConnectEn;
	private boolean txtEn;
	private boolean actionEn;
	private String logAppend;
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Constructor
	 * 
	 * @param logMsg
	 *            log message
	 * @param lblConnectMySQL
	 *            connect button label
	 * @param lblConnectFM
	 *            connect button label
	 * @param btnConnectText
	 *            connect button enabled?
	 * @param btnConnectEn
	 *            connect button enabled?
	 * @param txtEn
	 *            addressMySQL textfield enabled? addressFM textfield
	 *            enabled? mysql port textfield enabled? fm db textfield
	 *            enabled? db mysql textfield enabled? fm user textfield
	 *            enabled? mysql user textfield enabled? fm pass textfield
	 *            enabled? mysql pass textfield enabled?
	 */
	public FrameStatus(String logMsg, String lblConnectMySQL,
			String lblConnectFM, String btnConnectText,
			boolean btnConnectEn, boolean txtEn, boolean actionEn) {
		this.logMsg = logMsg;
		this.logAppend = "";
		this.lblConnectMySQL = lblConnectMySQL;
		this.lblConnectFM = lblConnectFM;
		this.btnConnectText = btnConnectText;
		this.btnConnectEn = btnConnectEn;
		this.txtEn = txtEn;
		this.actionEn = actionEn;
	}

	public FrameStatus setLogMsg(String logMsg) {
		this.logMsg = logMsg;
		return this;
	}
	
	public FrameStatus setLogAppend(String logApp) {
		this.logAppend = logApp;
		return this;
	}
	
	private String getTS() {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		return sdf.format(timestamp);
	}

	public String getLblConnectFM() {
		return lblConnectFM;
	}

	public String getLblConnectMySQL() {
		return lblConnectMySQL;
	}

	public String getLogMsg() {
		String result = "";
		if (!logAppend.equals("")){
			if (logMsg.startsWith("Connection failed"))
				result = getTS() + " " + logAppend + "\n";
			else
				result = getTS() + " " + logMsg + " " + logAppend + "\n";
		}
		else
			result = getTS() + " " + logMsg + "\n";
		
		logAppend = "";
		return result;
	}

	public String getBtnConnectText() {
		return btnConnectText;
	}

	public boolean isBtnConnectEn() {
		return btnConnectEn;
	}

	public boolean isTxtEn() {
		return txtEn;
	}

	public boolean isActionEn() {
		return actionEn;
	}

	public static SimpleDateFormat getSdf() {
		return sdf;
	}
}