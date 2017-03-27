package ceramalex.sync.view;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Private class ("struct") for representing frontend element status
 * 
 * @author horle (Felix Kussmaul)
 */
class FrameStatus {
	private String logMsg;
	private boolean btnConnectEn;
	private String logAppend;
	private boolean btnCancelEn;
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Constructor
	 * 
	 * @param logMsg
	 *            log message
	 * @param btnConnectEn
	 *            connect button enabled?
	 * @param btnCancelEn
	 *            connect button enabled?
	 */
	public FrameStatus(String logMsg, boolean btnConnectEn, boolean btnCancelEn) {
		this.logMsg = logMsg;
		this.logAppend = "";
		this.btnConnectEn = btnConnectEn;
		this.btnCancelEn = btnCancelEn;
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

	public String getLogMsg() {
		String result = "";
		if (!logAppend.equals("")){
			if (logMsg.startsWith("Connection failed"))
				result = logAppend + "\n";
			else
				result = logMsg + " " + logAppend + "\n";
		}
		else
			result = logMsg + "\n";
		
		logAppend = "";
		return result;
	}
	
	public String getLogMsgTS() {
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

	public boolean isBtnConnectEn() {
		return btnConnectEn;
	}

	public boolean isBtnCancelEn() {
		return btnCancelEn;
	}

	public static SimpleDateFormat getSdf() {
		return sdf;
	}
}