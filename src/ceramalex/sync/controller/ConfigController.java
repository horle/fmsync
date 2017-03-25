package ceramalex.sync.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

import ceramalex.sync.model.SQLDataModel;

public class ConfigController {

	private static ConfigController control;

	public static final String FM_URL_PREFIX = "jdbc:filemaker://";
	public static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

	private static Logger logger = Logger.getLogger(SQLDataModel.class);

	private File propertyFile;
	private Properties propertyList;

	private String mySQLURL;
	private String mySQLPort;
	private String mySQLUser;
	private String mySQLPassword;
	private String mySQLDB;
	private String fmURL;
	private final String fmPort = "2399";
	private String fmUser;
	private String fmPassword;
	private String fmDB;

	private HashSet<String> timestampFields;
	private HashSet<String> numericFields;

	private String fileName = "sync.conf";

	public static ConfigController getInstance() throws IOException {
		if (control == null)
			control = new ConfigController();
		return control;
	}

	private boolean setStandardValues() throws IOException {
		propertyList.setProperty("mySQLDatabaseURL", "arachne.dainst.org");
		propertyList.setProperty("mySQLPort", "3306");
		propertyList.setProperty("mySQLDB", "ceramalex");
		propertyList.setProperty("mySQLUser", "ceramalex");
		propertyList.setProperty("mySQLPassword", "");
		propertyList.setProperty("FMDatabaseURL", "localhost");
		propertyList.setProperty("FMDB", "iDAIAbstractCeramalex");
		propertyList.setProperty("FMUser", "admin");
		propertyList.setProperty("FMPassword", "");
		return writeConfigFile();
	}
	
	private ConfigController() throws IOException {
		propertyFile = new File(fileName);
		propertyList = new Properties();
		
		if (!propertyFile.exists()) {
			if (!createConfigFile()) throw new IOException("Could not create config file! Missing permissions?");
			setStandardValues();
		}
		// in any case //TODO liest auch leere datei
		readConfigFile();
	}

	private boolean createConfigFile() throws IOException {
		if (!propertyFile.exists()) {
			propertyFile = new File(fileName);
			if (!propertyFile.createNewFile())
				return false;
		} else
			return true;
		return false;
	}

	private boolean readConfigFile() throws IOException {
		if(!propertyFile.exists())
			createConfigFile();
		
		propertyList = new Properties();
		try {
			propertyList.load(new FileInputStream(propertyFile));
		} catch (FileNotFoundException e) {
			logger.error(e);
			return false;
		} catch (IOException e) {
			logger.error(e);
			return false;
		}
		
		if (propertyList.isEmpty()) {
			setStandardValues();
		}
		
		mySQLURL = propertyList.getProperty("mySQLDatabaseURL", "arachne.dainst.org");
		mySQLPort = propertyList.getProperty("mySQLPort", "3306");
		mySQLDB = propertyList.getProperty("mySQLDB", "ceramalex");
		mySQLUser = propertyList.getProperty("mySQLUser", "ceramalex");
		mySQLPassword = propertyList.getProperty("mySQLPassword", "");
		fmURL = propertyList.getProperty("FMDatabaseURL", "localhost");
		fmDB = propertyList.getProperty("FMDB", "iDAIAbstractCeramalex");
		fmUser = propertyList.getProperty("FMUser", "admin");
		fmPassword = propertyList.getProperty("FMPassword", "");
		return true;
	}

	public boolean setPrefs(String mURL, String mPort, String mUser,
			String mPwd, String mDB, String fURL, String fUser, String fPwd,
			String fDB) {
		this.mySQLURL = mURL;
		this.mySQLPort = mPort;
		this.mySQLUser = mUser;
		this.mySQLPassword = mPwd;
		this.mySQLDB = mDB;
		this.fmURL = fURL;
		this.fmUser = fUser;
		this.fmPassword = fPwd;
		this.fmDB = fDB;

		propertyList.setProperty("mySQLDatabaseURL", mURL);
		propertyList.setProperty("mySQLPort", mPort);
		propertyList.setProperty("mySQLDB", mDB);
		propertyList.setProperty("mySQLUser", mUser);
		propertyList.setProperty("mySQLPassword", mPwd);
		propertyList.setProperty("FMDatabaseURL", fURL);
		propertyList.setProperty("FMDB", fDB);
		propertyList.setProperty("FMUser", fUser);
		propertyList.setProperty("FMPassword", fPwd);
		
		try {
			return writeConfigFile();
		} catch (IOException e) {
			logger.error("Error writing log file!");
			return false;
		}
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
		return !(mySQLURL.isEmpty() || mySQLPort.isEmpty()
				|| mySQLUser.isEmpty() || mySQLPassword.isEmpty()
				|| mySQLDB.isEmpty() || fmURL.isEmpty() || fmUser.isEmpty()
				|| fmPassword.isEmpty() || fmDB.isEmpty());
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

	public boolean writeConfigFile() throws IOException {
		propertyList.store(new FileWriter(propertyFile), "");
		return true;
	}
}
