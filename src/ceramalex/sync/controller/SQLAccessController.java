package ceramalex.sync.controller;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import ceramalex.sync.data.FMDataAccess;
import ceramalex.sync.data.MySQLDataAccess;

/**
 * Singleton-Wrapper fuer MySQL-Datenzugriff
 * @author Patrick Gunia
 *
 */
public class SQLAccessController {

	/** Singleton-Instanz */
	private static SQLAccessController mInstance = null;
	
	/** Instanz des MySQL-Connectors */
	private MySQLDataAccess mDataAccess;
	private FMDataAccess fDataAccess;
	private ConfigController config;
	
	private static Logger logger = Logger.getLogger("ceramalex.sync.controller.sqlaccesscontroller");
	
	//-------------------------------------------------------------------------------

	/** Singleton-Getter 
	 * @throws SQLException */
	public static SQLAccessController getInstance() throws SQLException {
		if(mInstance == null)
			mInstance = new SQLAccessController();
		return mInstance;
	}

	//-------------------------------------------------------------------------------
	
	/** Default-Konstruktor mit Initialisierung der Datenbankverbindung 
	 * @throws SQLException */
	private SQLAccessController() throws SQLException {
		
		config = ConfigController.getInstance();
		this.mDataAccess = new MySQLDataAccess(config.getMySQLURL(), config.getMySQLUser(), config.getMySQLPassword(), "ceramalex");
		this.fDataAccess = new FMDataAccess(config.getFmURL(), config.getFmUser(), config.getFmPassword(), null);
	}
		
	public boolean close() {
		return this.mDataAccess.closeConnection();
	}
	
	public boolean isMySQLConnected() throws SQLException {
		return this.mDataAccess.isConnected();
	}
	
	public boolean isFMConnected() throws SQLException {
		return this.fDataAccess.isConnected();
	}
	
	//-------------------------------------------------------------------------------
	/** 
	 * Updatedelegate
	 * @param sql SQL-Update-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doMySQLUpdate(String sql) {
		return mDataAccess.doSQLModify(sql);
	}
	
	//-------------------------------------------------------------------------------
	/** 
	 * Insertdelegate
	 * @param sql SQL-Insert-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doMySQLInsert(String sql) {
		return mDataAccess.doSQLModify(sql);
	}
	//-------------------------------------------------------------------------------
	/**
	 * Selectdelegate
	 * @param sql SQL-Select-String
	 * @return ResultSet, das als Ergebnis der Anfrage zurueckgereicht wird
	 */
	public ResultSet doMySQLQuery(String sql) {
		return mDataAccess.doSqlQuery(sql);
	}
	//-------------------------------------------------------------------------------
	/** 
	 * Updatedelegate
	 * @param sql SQL-Update-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doFMUpdate(String sql) {
		return fDataAccess.doSQLModify(sql);
	}
	
	//-------------------------------------------------------------------------------
	/** 
	 * Insertdelegate
	 * @param sql SQL-Insert-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doFMInsert(String sql) {
		return fDataAccess.doSQLModify(sql);
	}
	//-------------------------------------------------------------------------------
	/**
	 * Selectdelegate
	 * @param sql SQL-Select-String
	 * @return ResultSet, das als Ergebnis der Anfrage zurueckgereicht wird
	 */
	public ResultSet doFMQuery(String sql) {
		return fDataAccess.doSqlQuery(sql);
	}
	//-------------------------------------------------------------------------------
}
