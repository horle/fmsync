package ceramalex.sync.controller;

import java.sql.SQLException;
import java.util.Map;

public class ConfigController {

	private static ConfigController control;
	
	/** Tabellen, die von MySQL nach FM importiert werden sollen */
	private String[] tables = null;
	
	/** URL der MySQL-Datenbank */
	private String mySQLURL = null;
	
	/** User der MySQL-Datenbank */
	private String mySQLUser = null;
	
	/** Passwort der MySQL-Datenbank */
	private String mySQLPassword = null;
	
	/** URL der FM-Datenbank */
	private String fmURL = null;
	
	/** User der FM-Datenbank */
	private String fmUser = null;
	
	/** Passwort der FM-Datenbank */
	private String fmPassword = null;
	
	/** Array mit Feldern, die aus den Key-Value-Maps geloescht werden */
	private String[] removeFields = null;
	
	/** Map mit Rewrite-Rules fuer Tabellen => aendert die Tabelle vom Key-Wert auf den Value-Wert */
	private Map<String, String> tableRewrites = null;
	
	/** Map mit Rewrite-Rules fuer Felder => aendert das Mapping von Quelle auf Ziel */
	private Map<String, String> fieldRewrites = null;
	
	/** Map mit Rewrite-Rules fuer Felder => aendert das Mapping von Quelle auf Ziel */
	private String numericFields[] = null;

	public static ConfigController getInstance() {
		if(control == null) throw new AssertionError("not initialized! initPrefs()");
		return control;
	}
	
	private ConfigController() {
		
	}
	
	public ConfigController initPrefs(String sqlURL, String sqlUser, String sqlPwd, String fURL, String fUser, String fPwd) throws SQLException {
		this.mySQLURL = sqlURL;
		this.mySQLUser = sqlUser;
		this.mySQLPassword = sqlPwd;
		this.fmURL = fURL;
		this.fmUser = fUser;
		this.fmPassword = fPwd;
			
		control = new ConfigController();
		return control;
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

	public String[] getRemoveFields() {
		return removeFields;
	}

	public Map<String, String> getTableRewrites() {
		return tableRewrites;
	}

	public Map<String, String> getFieldRewrites() {
		return fieldRewrites;
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

	public void setRemoveFields(String[] removeFields) {
		this.removeFields = removeFields;
	}

	public void setTableRewrites(Map<String, String> tableRewrites) {
		this.tableRewrites = tableRewrites;
	}

	public void setFieldRewrites(Map<String, String> fieldRewrites) {
		this.fieldRewrites = fieldRewrites;
	}

}
