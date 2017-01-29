package data;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Singleton-Wrapper fuer MySQL-Datenzugriff
 * @author Patrick Gunia
 *
 */
public class SQLSyncConnector {

	/** Singleton-Instanz */
	private static SQLSyncConnector mInstance = null;
	
	/** Instanz des MySQL-Connectors */
	private SQLDataAccess mDataAccess = null;
	private static String url, user, pwd, db;
	
	//-------------------------------------------------------------------------------

	/** Singleton-Getter */
	public static SQLSyncConnector getInstance() {
		if(mInstance == null) throw new AssertionError("not initialized!");
		return mInstance;
	}

	//-------------------------------------------------------------------------------
	
	/** Default-Konstruktor mit Initialisierung der Datenbankverbindung 
	 * @throws SQLException */
	private SQLSyncConnector() throws SQLException {
		// JDBC-Verbindung herstellen
		if (url == null || user == null || pwd == null || db == null)
			this.mDataAccess = new SQLDataAccess("jdbc:mysql://arachne.uni-koeln.de:3306/", "root", "Celt1!wedged", "ceramalex");
		else
			this.mDataAccess = new SQLDataAccess(url, user, pwd, db);
	}
	
	public static SQLSyncConnector initPrefs(String u, String us, String p, String d) throws SQLException {
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
