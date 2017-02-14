package ceramalex.sync.controller;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ceramalex.sync.data.FMDataAccess;
import ceramalex.sync.data.MySQLDataAccess;
import ceramalex.sync.model.Pair;

/**
 * Singleton-Wrapper fuer MySQL-Datenzugriff
 * 
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

	private static Logger logger = Logger
			.getLogger("ceramalex.sync.controller.sqlaccesscontroller");

	// -------------------------------------------------------------------------------

	/**
	 * Singleton-Getter
	 * 
	 * @throws SQLException
	 */
	public static SQLAccessController getInstance() throws SQLException {
		if (mInstance == null)
			mInstance = new SQLAccessController();
		return mInstance;
	}

	// -------------------------------------------------------------------------------

	/**
	 * Default-Konstruktor mit Initialisierung der Datenbankverbindung
	 * 
	 * @throws SQLException
	 */
	private SQLAccessController() throws SQLException {

		config = ConfigController.getInstance();
//		if (config.isInitialised()) {
			
//		} else throw new IllegalStateException("config has not been completely initialised!");
	}

	public boolean close() {
		return this.mDataAccess.closeConnection()
				&& this.fDataAccess.closeConnection();
	}

	public boolean isMySQLConnected() throws SQLException {
		return this.mDataAccess.isConnected();
	}

	public boolean isFMConnected() throws SQLException {
//		return this.fDataAccess.isConnected();
		return true;
	}
	
	public ResultSet getMySQLDBMetaData() throws SQLException {
		return this.mDataAccess.getDBMetaData();
	}
	
	public String getMySQLTablePrimaryKey(String table) throws SQLException {
		return this.mDataAccess.getTablePrimaryKey(table);
	}
	
	public ResultSet getFMDBMetaData() throws SQLException {
		return this.fDataAccess.getDBMetaData();
	}
	
	public ResultSetMetaData getFMRSMetaData(ResultSet r) throws SQLException {
		return this.fDataAccess.getRSMetaData(r);
	}

	// -------------------------------------------------------------------------------
	/**
	 * Updatedelegate
	 * 
	 * @param sql
	 *            SQL-Update-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doMySQLUpdate(String sql) {
		return mDataAccess.doSQLModify(sql);
	}

	// -------------------------------------------------------------------------------
	/**
	 * Insertdelegate
	 * 
	 * @param sql
	 *            SQL-Insert-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doMySQLInsert(String sql) {
		return mDataAccess.doSQLModify(sql);
	}

	// -------------------------------------------------------------------------------
	/**
	 * Selectdelegate
	 * 
	 * @param sql
	 *            SQL-Select-String
	 * @return ResultSet, das als Ergebnis der Anfrage zurueckgereicht wird
	 */
	public ResultSet doMySQLQuery(String sql) {
		return mDataAccess.doSQLQuery(sql);
	}

	// -------------------------------------------------------------------------------
	/**
	 * Updatedelegate
	 * 
	 * @param sql
	 *            SQL-Update-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doFMUpdate(String sql) {
		return fDataAccess.doSQLModify(sql);
	}

	// -------------------------------------------------------------------------------
	/**
	 * Insertdelegate
	 * 
	 * @param sql
	 *            SQL-Insert-String
	 * @return True, falls die Operation erfolgreich war, False sonst
	 */
	public boolean doFMInsert(String sql) {
		return fDataAccess.doSQLModify(sql);
	}

	// -------------------------------------------------------------------------------
	/**
	 * Selectdelegate
	 * 
	 * @param sql
	 *            SQL-Select-String
	 * @return ResultSet, das als Ergebnis der Anfrage zurueckgereicht wird
	 */
	public ResultSet doFMQuery(String sql) {
		return fDataAccess.doSQLQuery(sql);
	}
	// -------------------------------------------------------------------------------

	public boolean connect() throws SQLException {
		
		mDataAccess = new MySQLDataAccess(config.getMySQLURL(),
				config.getMySQLUser(), config.getMySQLPassword(),
				config.getMySQLDB());
		fDataAccess = new FMDataAccess(config.getFmURL(),
				config.getFmUser(), config.getFmPassword(), config.getFmDB());
		
		return mDataAccess.createConnection() && fDataAccess.createConnection();		
	}

	public HashSet<String> fetchNumericFields(ArrayList<Pair> commonTables) throws SQLException {
		ConfigController conf = ConfigController.getInstance();
		HashSet<String> list = new HashSet<String>();
		ResultSet tmp = this.mDataAccess.doSQLQuery("SELECT COLUMN_NAME FROM information_schema.COLUMNS where TABLE_SCHEMA='ceramalex' AND DATA_TYPE IN ('int','bigint','smallint','mediumint' 'tinyint','float','numeric') GROUP BY COLUMN_NAME");
		ArrayList<String> t = new ArrayList<String>();
		
		for (int i = 0; i < commonTables.size(); i++){
			t.addAll(this.getFMColumnMetaData(commonTables.get(i).getF()));
		}
		
		while (tmp.next()) {
			for (int i = 0; i < t.size(); i++) {
				if (tmp.getString(1).equalsIgnoreCase(t.get(i))){
					list.add(t.get(i));
				}
			}
		}
		tmp.close();
		conf.setNumericFields(list);
		return list;
	}
	
	private HashSet<String> getFMColumnMetaData(String table) throws SQLException {
		HashSet<String> list = new HashSet<String>();
		ResultSet s = this.fDataAccess.getColumnMetaData(table);
		while (s.next()){
			if (s.getInt(5) == java.sql.Types.DOUBLE && !s.getString(4).startsWith("["))
				list.add(s.getString(4));
		}
		return list;
	}
}
