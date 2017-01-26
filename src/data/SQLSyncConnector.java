package data;

import java.sql.ResultSet;
import java.sql.SQLException;

import ceramalex.filemaker.config.ImportConfig;
import ceramalex.filemaker.dataaccess.SQLDataConnector;

/**
 * Singleton-Wrapper fuer MySQL-Datenzugriff
 * @author Patrick Gunia
 *
 */
public class SQLSyncConnector {

	/** Singleton-Instanz */
	private static SQLSyncConnector mInstance = null;
	
	/** Instanz des MySQL-Connectors */
	private MySQLDataAccess mDataAccess = null;
	private static String url, user, pwd, db;
	
	//-------------------------------------------------------------------------------

	/** Singleton-Getter */
	public static SQLSyncConnector getInstance() {
		if(mInstance == null) throw new AssertionError("not initialized!");
		return mInstance;
	}

	//-------------------------------------------------------------------------------
	
	/** Default-Konstruktor mit Initialisierung der Datenbankverbindung */
	private SQLSyncConnector() {
		// JDBC-Verbindung herstellen
		if (url == null || user == null || pwd == null || db == null)
			this.mDataAccess = new MySQLDataAccess("jdbc:mysql://arachne.uni-koeln.de:3306/", "root", "Celt1!wedged", "ceramalex");
		else
			this.mDataAccess = new MySQLDataAccess(url, user, pwd, db);
	}
	
	public static SQLSyncConnector initPrefs(String u, String us, String p, String d) {
		url = u;
		user = us;
		pwd = p;
		db = d;
			
		mInstance = new SQLSyncConnector();
		return mInstance;
	}
	
	public boolean close() {
		return this.mDataAccess.closeConnection();
	}
	
	public boolean isConnected() throws SQLException {
		return this.mDataAccess.isConnected();
	}
	
	//-------------------------------------------------------------------------------
	/** 
	 * Updatedelegate
	 * @param sql SQL-Update-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doSqlUpdate(String sql) {
		return mDataAccess.doSQLModify(sql);
	}
	
	//-------------------------------------------------------------------------------
	/** 
	 * Insertdelegate
	 * @param sql SQL-Insert-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doSqlInsert(String sql) {
		return mDataAccess.doSQLModify(sql);
	}
	//-------------------------------------------------------------------------------
	/**
	 * Selectdelegate
	 * @param sql SQL-Select-String
	 * @return ResultSet, das als Ergebnis der Anfrage zurueckgereicht wird
	 */
	public ResultSet doSqlQuery(String sql) {
		return mDataAccess.doSqlQuery(sql);
	}
	//-------------------------------------------------------------------------------
}
