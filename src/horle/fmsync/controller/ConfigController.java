package horle.fmsync.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.google.common.collect.TreeBasedTable;

import horle.fmsync.model.Pair;

public class ConfigController {

	private static ConfigController control;

	public static final String FM_URL_PREFIX = "jdbc:filemaker://";
	public static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

	private static Logger logger = Logger.getLogger(ConfigController.class);

	private File propertyFile;
	private Properties propertyList;

	private final String fmPort = "2399";

	private TreeSet<Pair> timestampFields;
	private TreeSet<String> numericFields;
	private TreeBasedTable<String,String,HashSet<String>> colPermissions;

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
		propertyList.setProperty("ImagePath", "");
		propertyList.setProperty("ShowDetailFrame", "true");
		propertyList.setProperty("IncludeImgFiles", "true");
		propertyList.setProperty("UUIDFieldName", "ArachneEntityID");
		propertyList.setProperty("EntityDeletedFieldName", "isDeleted");
		propertyList.setProperty("ForeignKeyFieldName", "ForeignKey");
		propertyList.setProperty("ForeignKeyTableFieldName", "TableName");
		propertyList.setProperty("LastModifiedFieldName", "lastModified");
		propertyList.setProperty("LastRemoteTSFieldName", "lastRemoteTS");
		propertyList.setProperty("EntityManagementTable", "arachneentityidentification");
		return writeConfigFile();
	}
	
	private ConfigController() throws IOException {
		propertyFile = new File(fileName);
		propertyList = new Properties();
		colPermissions = TreeBasedTable.create();
		
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
			String fDB, String img) {

		propertyList.setProperty("MySQLURL", mURL);
		propertyList.setProperty("MySQLPort", mPort);
		propertyList.setProperty("MySQLDB", mDB);
		propertyList.setProperty("MySQLUser", mUser);
		propertyList.setProperty("MySQLPassword", mPwd);
		propertyList.setProperty("FMURL", fURL);
		propertyList.setProperty("FMDB", fDB);
		propertyList.setProperty("FMUser", fUser);
		propertyList.setProperty("FMPassword", fPwd);
		propertyList.setProperty("ImagePath", img);
		
		try {
			return writeConfigFile();
		} catch (IOException e) {
			logger.error("Error writing log file!");
			return false;
		}
	}

	public boolean writeConfigFile() throws IOException {
		FileWriter fw = new FileWriter(propertyFile);
		propertyList.store(fw, "");
		fw.close();
		return true;
	}

	/*
	 * database connection information
	 */
	// MySQL
	public String getShortMySQLURL() 					{return propertyList.getProperty("MySQLURL");}	
	public String getMySQLURL() 						{return MYSQL_URL_PREFIX + propertyList.getProperty("MySQLURL");}
	public String getMySQLUser() 						{return propertyList.getProperty("MySQLUser");}
	public String getMySQLPassword() 					{return propertyList.getProperty("MySQLPassword");}
	public String getMySQLPort() 						{return propertyList.getProperty("MySQLPort");}
	public void setMySQLPort(String mySQLPort) 			{propertyList.setProperty("MySQLPort", mySQLPort);}
	public void setMySQLDB(String mySQLDB) 				{propertyList.setProperty("MySQLDB",  mySQLDB);}
	public void setMySQLURL(String mySQLURL) 			{propertyList.setProperty("MySQLURL", mySQLURL);}
	public void setMySQLUser(String mySQLUser) 			{propertyList.setProperty("MySQLUser", mySQLUser);}
	public void setMySQLPassword(String mySQLPassword) 	{propertyList.setProperty("MySQLPassword", mySQLPassword);}
	public String getMySQLDB() 							{return propertyList.getProperty("MySQLDB");}
	// FM
	public String getFmURL() 							{return FM_URL_PREFIX + propertyList.getProperty("FMURL");}	
	public String getShortFMURL() 						{return propertyList.getProperty("FMURL");}
	public String getFmUser() 							{return propertyList.getProperty("FMUser");}
	public String getFmPassword() 						{return propertyList.getProperty("FMPassword");}
	public void setFmURL(String fmURL) 					{propertyList.setProperty("FMURL",fmURL);}
	public void setFmUser(String fmUser) 				{propertyList.setProperty("FMUser", fmUser);}
	public void setFmPassword(String fmPassword) 		{propertyList.setProperty("FMPassword", fmPassword);}
	public String getFmDB() 							{return propertyList.getProperty("FMDB");}
	public String getFMPort() 							{return fmPort;}
	public void setFmDB(String fmDB) 					{propertyList.setProperty("FMDB", fmDB);}
	// general
	public TreeSet<String> getNumericFields() 			{return numericFields;}
	public void setNumericFields(TreeSet<String> list) 	{this.numericFields = list;}
	public TreeSet<Pair> getTimestampFields() 			{return timestampFields;}
	public void setTimestampFields(TreeSet<Pair> list)	{this.timestampFields = list;}
	
	/*
	 * ui options
	 */
	public void setShowDetailsFrame(boolean show) 		{propertyList.setProperty("ShowDetailFrame", ""+show);}	
	public boolean getShowDetailsFrame() 				{return new Boolean(propertyList.getProperty("ShowDetailFrame"));}	
	public void setIncludeImgFiles(boolean inc) 		{propertyList.setProperty("IncludeImgFiles", ""+inc);}
	public boolean getIncludeImgFiles() 				{return new Boolean(propertyList.getProperty("IncludeImgFiles"));}
	public String getImagePath() 						{return propertyList.getProperty("ImagePath");}
	public void setImagePath(String img) 				{propertyList.setProperty("ImagePath", img);}	
	
	/*
	 * entity management field names
	 */
	public String getEntityManagementTable() 			{return propertyList.getProperty("EntityManagementTable");}	
	public void setEntityManagementTable(String table) 	{propertyList.setProperty("EntityManagementTable", table);}	
	public String getUUIDFieldName() 					{return propertyList.getProperty("UUIDFieldName");}	
	public void setUUIDFieldName(String uuid) 			{propertyList.setProperty("UUIDFieldName", uuid);}	
	public String getFKFieldName() 						{return propertyList.getProperty("ForeignKeyFieldName");}	
	public void setFKFieldName(String fk) 				{propertyList.setProperty("ForeignKeyFieldName", fk);}
	public String getFKTableFieldName() 				{return propertyList.getProperty("ForeignKeyTableFieldName");}	
	public void setFKTableFieldName(String fkt) 		{propertyList.setProperty("ForeignKeyTableFieldName", fkt);}
	public String getModificationTSFieldName()  		{return propertyList.getProperty("LastModifiedFieldName");}
	public void setModificationTSFieldName(String lm) 	{propertyList.setProperty("LastModifiedFieldName", lm);}
	public String getLastRemoteTSFieldName()  		{return propertyList.getProperty("LastRemoteTSFieldName");}
	public void setLastRemoteTSFieldName(String lrts) 	{propertyList.setProperty("LastRemoteTSFieldName", lrts);}
	public String getDeletedFieldName()  				{return propertyList.getProperty("EntityDeletedFieldName");}
	public void setDeletedFieldName(String del) 		{propertyList.setProperty("EntityDeletedFieldName", del);}

	public TreeBasedTable<String, String, HashSet<String>> getColPermissions() {
		return colPermissions;
	}

	public void setColPermissions(TreeBasedTable<String, String, HashSet<String>> colPermissions) {
		this.colPermissions = colPermissions;
	}
	
	public void addColPermission(String table, String col, HashSet<String> perm) {
		this.colPermissions.put(table, col, perm);
	}
}
