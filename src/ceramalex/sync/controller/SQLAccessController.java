package ceramalex.sync.controller;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;

import ceramalex.sync.data.FMDataAccess;
import ceramalex.sync.data.MySQLDataAccess;
import ceramalex.sync.exception.FilemakerIsCrapException;
import ceramalex.sync.model.Pair;

/**
 * Singleton class for FileMaker and MySQL access control
 * 
 * @author horle (Felix Kussmaul)
 */
public class SQLAccessController {

	private static SQLAccessController mInstance = null;
	private MySQLDataAccess mDataAccess;
	private FMDataAccess fDataAccess;
	private ConfigController config;

	private static Logger logger = Logger
			.getLogger("ceramalex.sync.controller.sqlaccesscontroller");

	/**
	 * singleton getter
	 * 
	 * @throws SQLException
	 */
	public static SQLAccessController getInstance() {
		if (mInstance == null)
			mInstance = new SQLAccessController();
		return mInstance;
	}

	/**
	 * constructor. defines config controller object
	 * 
	 * @throws SQLException
	 */
	private SQLAccessController() {
		config = ConfigController.getInstance();
	}

	/**
	 * closes both connections
	 * @return true, if both successfully closed
	 */
	public boolean close() {
		return this.mDataAccess.closeConnection()
				&& this.fDataAccess.closeConnection();
	}

	/**
	 * check, if MySQL connection is established
	 * @return true, if connected
	 * @throws SQLException
	 */
	public boolean isMySQLConnected() throws SQLException {
		return this.mDataAccess.isConnected();
	}

	/**
	 * check, if FM connection is established
	 * @return true, if connected
	 * @throws SQLException
	 */
	public boolean isFMConnected() throws SQLException {
		return this.fDataAccess.isConnected();
		// return true;
	}

	/**
	 * get mySQL database metadata object
	 * @return mySQL database metadata object as ResultSet
	 * @throws SQLException
	 */
	public ResultSet getMySQLDBMetaData() throws SQLException {
		return this.mDataAccess.getDBMetaData();
	}

	/**
	 * get name of primary key in MySQL table 
	 * @param table name of MySQL table
	 * @return name of primary key attribute
	 * @throws SQLException
	 */
	public String getMySQLTablePrimaryKey(String table) throws SQLException {
		return this.mDataAccess.getMySQLTablePrimaryKey(table);
	}

	/**
	 * get FM database metadata object
	 * @return FM database metadata object as ResultSet
	 * @throws SQLException
	 */
	public ResultSet getFMDBMetaData() throws SQLException {
		return this.fDataAccess.getDBMetaData();
	}

	/**
	 * get metadata of resultSet
	 * @param r resultset to get metadata from
	 * @return metadata of r as ResultSetMetaData object
	 * @throws SQLException
	 */
	public ResultSetMetaData getFMRSMetaData(ResultSet r) throws SQLException {
		return this.fDataAccess.getRSMetaData(r);
	}

	/**
	 * /**
	 * execute update query on FM
	 * @param table String after UPDATE (just table name)
	 * @param set String after SET
	 * @param where String after WHERE
	 * @return true, if success. false else
	 * @throws SQLException 
	 */
	public boolean doMySQLUpdate(String table, String set, String where) throws SQLException {
		if (table.isEmpty() || set.isEmpty() || where.isEmpty())
			return false;
		return mDataAccess.doSQLModify("UPDATE "+table+" SET "+set+" WHERE "+where+";");
	}

	/**
	 * execute insert query on FM
	 * 
	 * @param sql
	 *            SQL-Insert-String
	 * @return true, if success. false else
	 * @throws SQLException 
	 */
	public boolean doMySQLInsert(String sql) throws SQLException {
		return mDataAccess.doSQLModify(sql);
	}

	/**
	 * execute select query on MySQL
	 * 
	 * @param sql
	 *            SQL-Select-String
	 * @return resultset
	 */
	public ResultSet doMySQLQuery(String sql) {
		return mDataAccess.doSQLQuery(sql);
	}
	
	/**
	 * execute update query on FM
	 * 
	 * @param sql
	 *            SQL-Update-String
	 * @return true, if success. false else
	 * @throws SQLException 
	 */
	public boolean doFMUpdate(String sql) throws SQLException {
		return fDataAccess.doSQLModify(sql);
	}

	/**
	 * execute insert query on FM
	 * 
	 * @param sql
	 *            SQL-Insert-String
	 * @return true, if success. false else
	 * @throws SQLException 
	 */
	public boolean doFMInsert(String sql) throws SQLException {
		return fDataAccess.doSQLModify(sql);
	}

	/**
	 * execute select query on FM
	 * 
	 * @param sql
	 *            SQL-Select-String
	 * @return resultset
	 */
	public ResultSet doFMQuery(String sql) {
		return fDataAccess.doSQLQuery(sql);
	}


	/**
	 * creates connections to filemaker and mysql, if they don't exist.
	 * 
	 * @return true, if both are established.
	 * @throws SQLException
	 */
	public boolean connect() throws SQLException {

		boolean mRes = true;
		boolean fRes = true;

		if (mDataAccess == null) {
			mDataAccess = new MySQLDataAccess(config.getMySQLURL(),
					config.getMySQLUser(), config.getMySQLPassword(),
					config.getMySQLDB());
		}
		if (fDataAccess == null) {
			fDataAccess = new FMDataAccess(config.getFmURL(),
					config.getFmUser(), config.getFmPassword(),
					config.getFmDB());
		}
		if (!mDataAccess.isConnected()) {
			mRes = mDataAccess.createConnection();
		}
		if (!fDataAccess.isConnected()) {
			fRes = fDataAccess.createConnection();
		}
		
		return mRes && fRes;
	}

	/**
	 * fetch all fields (in all tables) with type "numeric" (i.e. double for FM)
	 * from FM and MySQL
	 * 
	 * @param commonTables
	 *            all common tables
	 * @return set (without duplicates) of numeric fields
	 * @throws SQLException
	 */
	public HashSet<String> fetchNumericFields(ArrayList<Pair> commonTables)
			throws SQLException {
		ConfigController conf = ConfigController.getInstance();
		HashSet<String> list = new HashSet<String>();
		ResultSet tmp = this.mDataAccess
				.doSQLQuery("SELECT COLUMN_NAME "
						+ "FROM information_schema.COLUMNS "
						+ "WHERE TABLE_SCHEMA='ceramalex' "
						+ "AND DATA_TYPE IN ('int','bigint','smallint','mediumint','tinyint','float','numeric') "
						+ "GROUP BY COLUMN_NAME");
		ArrayList<String> t = new ArrayList<String>();

		for (int i = 0; i < commonTables.size(); i++) {
			t.addAll(this.getFMNumericFields(commonTables.get(i).getF()));
		}

		// get common numeric fields
		while (tmp.next()) {
			for (int i = 0; i < t.size(); i++) {
				if (tmp.getString(1).equalsIgnoreCase(t.get(i))) {
					list.add(t.get(i));
				}
			}
		}
		tmp.close();
		conf.setNumericFields(list);
		return list;
	}

	/**
	 * helper method to fetch filemaker numeric fields from particular table
	 * 
	 * @param table
	 *            name of table in filemaker
	 * @return set of filemaker numeric fields in table
	 * @throws SQLException
	 */
	private HashSet<String> getFMNumericFields(String table)
			throws SQLException {
		HashSet<String> list = new HashSet<String>();
		ResultSet s = this.fDataAccess.getFMColumnMetaData(table);
		while (s.next()) {
			if (s.getInt(5) == java.sql.Types.DOUBLE
					&& !s.getString(4).startsWith("["))
				list.add(s.getString(4));
		}
		return list;
	}

	public ResultSet getFMColumnMetaData(String f) throws SQLException {
		return this.fDataAccess.getFMColumnMetaData(f);
	}

	public ResultSet getMySQLColumnMetaData(String m) throws SQLException {
		return this.mDataAccess.getMySQLColumnMetaData(m);
	}

	public String getFMTablePrimaryKey(String f) {
		return this.fDataAccess.getFMTablePrimaryKey(f);
	}

	public boolean doFMAlter(String sql) throws SQLException {
		return fDataAccess.doSQLAlter(sql);
	}
}
