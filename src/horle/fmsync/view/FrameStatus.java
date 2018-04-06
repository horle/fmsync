package horle.fmsync.view;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import horle.fmsync.controller.ConfigController;

/**
 * fabric class for constructing frontend element status
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
	
	public final static FrameStatus open() {
		return new FrameStatus("Successfully connected to MySQL.\nSuccessfully connected to FM.", false, true);
	}
	
	public final static FrameStatus closed() {
		return new FrameStatus("", true, false);
	}
	
	public final static FrameStatus closed(String msg) {
		return new FrameStatus(msg, true, false);
	}
	
	public final static FrameStatus closedError(String msg) {
		return new FrameStatus("Error: " + msg, true, false);
	}
	
	public final static FrameStatus closedError() {
		return new FrameStatus("Connection failed.", true, false);
	}
	
	public final static FrameStatus connecting() throws IOException {
		ConfigController config = ConfigController.getInstance();
		return new FrameStatus("Trying to connect to "
				+ config.getShortMySQLURL() + ":" + config.getMySQLPort() + " as "
				+ config.getMySQLUser() + " ...\nTrying to connect to "
				+ config.getShortFMURL() + ":2399" + " as " + config.getFmUser()
				+ " ...", false, false);
	}

	public static FrameStatus connecting(String msg) {
		return new FrameStatus(msg, false, true);
	}

	public static FrameStatus open(String msg) {
		return new FrameStatus(msg, false, true);
	}
	
}