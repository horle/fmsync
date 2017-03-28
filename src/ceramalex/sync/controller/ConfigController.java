package ceramalex.sync.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ConfigController {

	private static ConfigController control;

	public static final String FM_URL_PREFIX = "jdbc:filemaker://";
	public static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

	private static Logger logger = Logger.getLogger(ConfigController.class);

	private File propertyFile;
	private Properties propertyList;

	private final String fmPort = "2399";

	private HashSet<String> timestampFields;
	private HashSet<String> numericFields;

	private String fileName = "sync.conf";

	public static ConfigController getInstance() throws IOException {
		if (control == null)
			control = new ConfigController();
		return control;
	}

	private boolean setStandardValues() throws IOException {
		propertyList.setProperty("MySQLURL", "arachne.dainst.org");
		propertyList.setProperty("MySQLPort", "3306");
		propertyList.setProperty("MySQLDB", "ceramalex");
		propertyList.setProperty("MySQLUser", "ceramalex");
		propertyList.setProperty("MySQLPassword", "");
		propertyList.setProperty("FMURL", "localhost");
		propertyList.setProperty("FMDB", "iDAIAbstractCeramalex");
		propertyList.setProperty("FMUser", "admin");
		propertyList.setProperty("FMPassword", "");
		return writeConfigFile();
	}
	
	private ConfigController() throws IOException {
		propertyFile = new File(fileName);
		propertyList = new Properties();
		
		readConfigFile();
	}

	private boolean createConfigFile() throws IOException {
		if (!propertyFile.exists()) {
			propertyFile = new File(fileName);
			if (!propertyFile.createNewFile())
				return false;
			else
				return true;
		} else
			return true;
	}

	private boolean readConfigFile() throws IOException {
		if(!propertyFile.exists()) {
			if (!createConfigFile()) throw new IOException("Could not create config file! Missing permissions?");
			setStandardValues();
		}
		
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
		return true;
	}

	public boolean setPrefs(String mURL, String mPort, String mUser,
			String mPwd, String mDB, String fURL, String fUser, String fPwd,
			String fDB) {

		propertyList.setProperty("MySQLURL", mURL);
		propertyList.setProperty("MySQLPort", mPort);
		propertyList.setProperty("MySQLDB", mDB);
		propertyList.setProperty("MySQLUser", mUser);
		propertyList.setProperty("MySQLPassword", mPwd);
		propertyList.setProperty("FMURL", fURL);
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

	public String getShortMySQLURL() {
		return propertyList.getProperty("MySQLURL");
	}
	
	public String getMySQLURL() {
		return MYSQL_URL_PREFIX + propertyList.getProperty("MySQLURL");
	}

	public String getMySQLUser() {
		return propertyList.getProperty("MySQLUser");
	}

	public String getMySQLPassword() {
		return propertyList.getProperty("MySQLPassword");
	}

	public String getFmURL() {
		return FM_URL_PREFIX + propertyList.getProperty("FMURL");
	}
	
	public String getShortFMURL() {
		return propertyList.getProperty("FMURL");
	}

	public String getFmUser() {
		return propertyList.getProperty("FMUser");
	}

	public String getFmPassword() {
		return propertyList.getProperty("FMPassword");
	}

	public void setMySQLURL(String mySQLURL) {
		propertyList.setProperty("MySQLURL", mySQLURL);
	}

	public void setMySQLUser(String mySQLUser) {
		propertyList.setProperty("MySQLUser", mySQLUser);
	}

	public void setMySQLPassword(String mySQLPassword) {
		propertyList.setProperty("MySQLPassword", mySQLPassword);
	}

	public void setFmURL(String fmURL) {
		propertyList.setProperty("FMURL",fmURL);
	}

	public void setFmUser(String fmUser) {
		propertyList.setProperty("FMUser", fmUser);
	}

	public void setFmPassword(String fmPassword) {
		propertyList.setProperty("FMPassword", fmPassword);
	}

	public String getMySQLDB() {
		return propertyList.getProperty("MySQLDB");
	}

	public String getFmDB() {
		return propertyList.getProperty("FMDB");
	}

	public void setMySQLDB(String mySQLDB) {
		propertyList.setProperty("MySQLDB",  mySQLDB);
	}

	public void setFmDB(String fmDB) {
		propertyList.setProperty("FMDB", fmDB);
	}

	public String getMySQLPort() {
		return propertyList.getProperty("MySQLPort");
	}

	public void setMySQLPort(String mySQLPort) {
		propertyList.setProperty("MySQLPort", mySQLPort);
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
		FileWriter fw = new FileWriter(propertyFile);
		propertyList.store(fw, "");
		fw.close();
		return true;
	}

	public String getFMPort() {
		return fmPort;
	}
}
