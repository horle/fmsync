package ceramalex.sync.controller;

import java.util.HashSet;

public class ConfigController {

	private static ConfigController control;

//	/** Tabellen, die von MySQL nach FM importiert werden sollen */
//	private String[] tables = null;

	/** URL der MySQL-Datenbank */
	private String mySQLURL = null;

	/** URL der MySQL-Datenbank */
	private String mySQLPort = null;

	/** User der MySQL-Datenbank */
	private String mySQLUser = null;

	/** Passwort der MySQL-Datenbank */
	private String mySQLPassword = null;

	/** Passwort der MySQL-Datenbank */
	private String mySQLDB = null;

	/** URL der FM-Datenbank */
	private String fmURL = null;

	/** User der FM-Datenbank */
	private String fmUser = null;

	/** Passwort der FM-Datenbank */
	private String fmPassword = null;

	/** Passwort der FM-Datenbank */
	private String fmDB = null;

	private HashSet<String> timestampFields;
	private HashSet<String> numericFields;

	public static ConfigController getInstance() {
		if (control == null)
			control = new ConfigController();
		return control;
	}

	private ConfigController() {
		setPrefs("jdbc:mysql://192.168.1.4:3306", "root", "",
				"ceramalex", "jdbc:filemaker://localhost", "admin", "btbw", "iDAIAbstractCeramalex", "3306");
	}

	public void setPrefs(String sqlURL, String sqlUser, String sqlPwd,
			String sqlDB, String fURL, String fUser, String fPwd, String fDB,
			String sqlPort) {
		// filemaker port is fix ...
		this.mySQLURL = sqlURL;
		this.mySQLPort = sqlPort;
		this.mySQLUser = sqlUser;
		this.mySQLPassword = sqlPwd;
		this.mySQLDB = sqlDB;
		this.fmURL = fURL;
		this.fmUser = fUser;
		this.fmPassword = fPwd;
		this.fmDB = fDB;
	}

	public String getMySQLURL() {
		return mySQLURL;
	}

	public String getMySQLUser() {
		return mySQLUser;
	}

	public String getMySQLPassword() {
		return mySQLPassword;
	}

	public String getFmURL() {
		return fmURL;
	}

	public String getFmUser() {
		return fmUser;
	}

	public String getFmPassword() {
		return fmPassword;
	}

	public void setMySQLURL(String mySQLURL) {
		this.mySQLURL = mySQLURL;
	}

	public void setMySQLUser(String mySQLUser) {
		this.mySQLUser = mySQLUser;
	}

	public void setMySQLPassword(String mySQLPassword) {
		this.mySQLPassword = mySQLPassword;
	}

	public void setFmURL(String fmURL) {
		this.fmURL = fmURL;
	}

	public void setFmUser(String fmUser) {
		this.fmUser = fmUser;
	}

	public void setFmPassword(String fmPassword) {
		this.fmPassword = fmPassword;
	}

//	public void setTableRewrites(Map<String, String> tableRewrites) {
//		this.tableRewrites = tableRewrites;
//	}
//
//	public void setFieldRewrites(Map<String, String> fieldRewrites) {
//		this.fieldRewrites = fieldRewrites;
//	}

	public String getMySQLDB() {
		return mySQLDB;
	}

	public String getFmDB() {
		return fmDB;
	}

	public void setMySQLDB(String mySQLDB) {
		this.mySQLDB = mySQLDB;
	}

	public void setFmDB(String fmDB) {
		this.fmDB = fmDB;
	}

	public String getMySQLPort() {
		return mySQLPort;
	}

	public void setMySQLPort(String mySQLPort) {
		this.mySQLPort = mySQLPort;
	}

	public boolean isInitialised() {
		return !( mySQLURL.isEmpty() || mySQLPort.isEmpty() || mySQLUser.isEmpty()
				|| mySQLPassword.isEmpty() || mySQLDB.isEmpty()
				|| fmURL.isEmpty() || fmUser.isEmpty() || fmPassword.isEmpty()
				|| fmDB.isEmpty() );
	}

	public HashSet<String> getNumericFields() {
		return numericFields;
	}

	public void setNumericFields(HashSet<String> list) {
		this.numericFields = list;
	}

	public HashSet<String> getTimestampFields() {
		return timestampFields;
	}

	public void setTimestampFields(HashSet<String> timestampFields) {
		this.timestampFields = timestampFields;
	}

}
