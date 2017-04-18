package ceramalex.sync.controller;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;

import ceramalex.sync.data.FMDataAccess;
import ceramalex.sync.data.MySQLDataAccess;
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
	private ConfigController conf;

	private static Logger logger = Logger.getLogger(SQLAccessController.class);

	/**
	 * singleton getter
	 * @throws IOException 
	 * 
	 * @throws SQLException
	 */
	public static SQLAccessController getInstance() throws IOException, SQLException {
		if (mInstance == null)
			mInstance = new SQLAccessController();
		return mInstance;
	}

	/**
	 * constructor. defines config controller object
	 * @throws IOException 
	 * 
	 * @throws SQLException
	 */
	private SQLAccessController() throws IOException, SQLException {
		conf = ConfigController.getInstance();
		mDataAccess = new MySQLDataAccess(conf.getMySQLURL(),
				conf.getMySQLUser(), conf.getMySQLPassword(),
				conf.getMySQLDB());
		fDataAccess = new FMDataAccess(conf.getFmURL(),
				conf.getFmUser(), conf.getFmPassword(),
				conf.getFmDB());

	}

	/**
	 * closes both connections
	 * @return true, if both successfully closed
	 */
	public boolean close() {
		boolean mClosed = false;
		boolean fClosed = false;
		if (mDataAccess == null) {
			mClosed = true;
		} else
			mClosed = mDataAccess.closeConnection();
		if (fDataAccess == null) {
			fClosed = true;
		} else
			fClosed = fDataAccess.closeConnection();
		
		return mClosed && fClosed;
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
		return this.mDataAccess.getTablePrimaryKey(table);
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
	 * execute insert query on FM
	 * 
	 * @param sql
	 *            SQL-Insert-String
	 * @return true, if success. false else
	 * @throws SQLException 
	 */
	public ArrayList<Integer> doMySQLInsert(String sql) throws SQLException {
		return mDataAccess.doSQLModify(sql);
	}

	/**
	 * execute select query on MySQL
	 * 
	 * @param sql
	 *            SQL-Select-String
	 * @return resultset
	 * @throws SQLException 
	 */
	public ResultSet doMySQLQuery(String sql) throws SQLException {
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
	public ArrayList<Integer> doFMUpdate(String sql) throws SQLException {
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
	public ArrayList<Integer> doFMInsert(String sql) throws SQLException {
		return fDataAccess.doSQLModify(sql);
	}

	/**
	 * execute select query on FM
	 * 
	 * @param sql
	 *            SQL-Select-String
	 * @return resultset
	 * @throws SQLException 
	 */
	public ResultSet doFMQuery(String sql) throws SQLException {
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

		mDataAccess = new MySQLDataAccess(conf.getMySQLURL(),
					conf.getMySQLUser(), conf.getMySQLPassword(),
					conf.getMySQLDB());
		fDataAccess = new FMDataAccess(conf.getFmURL(),
					conf.getFmUser(), conf.getFmPassword(),
					conf.getFmDB());
		
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
	 * @throws IOException 
	 */
	public HashSet<String> fetchNumericFields(ArrayList<Pair> commonTables)
			throws SQLException {
		HashSet<String> list = new HashSet<String>();
		ArrayList<String> fm = new ArrayList<String>();
		String sqlCommonTables = "AND (";
		
		for (int i = 0; i < commonTables.size(); i++) {
			list.add(commonTables.get(i).getLeft() + ".ArachneEntityID");
			fm.addAll(getFMNumericFields(commonTables.get(i).getLeft()));
			sqlCommonTables += "TABLE_NAME = '" + commonTables.get(i).getRight() + "'";
			if (i < commonTables.size()-1) sqlCommonTables += " OR ";
		}
		sqlCommonTables += ") ";
		
		ResultSet mysql = this.mDataAccess
				.doSQLQuery("SELECT COLUMN_NAME, TABLE_NAME "
						+ "FROM information_schema.COLUMNS "
						+ "WHERE TABLE_SCHEMA='"+conf.getMySQLDB()+"' "
						+ sqlCommonTables
						+ "AND DATA_TYPE IN ('int','bigint','smallint','mediumint','tinyint','float','numeric')");
		
		// get common numeric fields
		while (mysql.next()) {
			String bla = (mysql.getString(2)+"."+mysql.getString(1));
			for (int i = 0; i < fm.size(); i++) {
				String fmField = fm.get(i);
				if (bla.equalsIgnoreCase(fmField)) {
					list.add(fmField);
					logger.debug("New common numeric field: "+fmField);
					break;
				}
			}
		}
		mysql.close();
		conf.setNumericFields(list);
		return list;
	}

	/**
	 * fetch all fields (in all tables) with type "timestamp"
	 * from FM and MySQL
	 * 
	 * @param commonTables
	 *            all common tables
	 * @return set (without duplicates) of timestamp fields
	 * @throws SQLException
	 * @throws IOException 
	 */
	public HashSet<String> fetchTimestampFields(ArrayList<Pair> commonTables)
			throws SQLException {
		HashSet<String> list = new HashSet<String>();
		ArrayList<String> fm = new ArrayList<String>();
		String sqlCommonTables = "AND (";

		for (int i = 0; i < commonTables.size(); i++) {
			fm.addAll(getFMTimestampFields(commonTables.get(i).getLeft()));
			sqlCommonTables += "TABLE_NAME = '" + commonTables.get(i).getRight() + "'";
			if (i < commonTables.size()-1) sqlCommonTables += " OR ";
		}
		sqlCommonTables += ") ";
		
		ResultSet mysql = this.mDataAccess
				.doSQLQuery("SELECT COLUMN_NAME, TABLE_NAME "
						+ "FROM information_schema.COLUMNS "
						+ "WHERE TABLE_SCHEMA='"+conf.getMySQLDB()+"' "
						+ sqlCommonTables
						+ "AND DATA_TYPE IN ('timestamp','date','datetime')");
		
		// get common timestamp fields
		while (mysql.next()) {
			String bla = (mysql.getString(2)+"."+mysql.getString(1));
			for (int i = 0; i < fm.size(); i++) {
				String fmField = fm.get(i);
				if (bla.equalsIgnoreCase(fmField)) {
					list.add(fmField);
					logger.debug("New common timestamp field: "+fmField);
					break;
				}
			}
		}
		mysql.close();
		conf.setTimestampFields(list);
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
		ResultSet s = this.fDataAccess.getColumnMetaData(table);
		while (s.next()) {
			if (s.getInt(5) == java.sql.Types.DOUBLE
					&& !s.getString(4).startsWith("["))
				list.add(table+"."+s.getString(4));
		}
		return list;
	}
	
	/**
	 * helper method to fetch filemaker timestamp fields from particular table
	 * 
	 * @param table
	 *            name of table in filemaker
	 * @return set of filemaker timestamp fields in table
	 * @throws SQLException
	 */
	private HashSet<String> getFMTimestampFields(String table)
			throws SQLException {
		HashSet<String> list = new HashSet<String>();
		ResultSet s = this.fDataAccess.getColumnMetaData(table);
		while (s.next()) {
			if (s.getInt(5) == java.sql.Types.TIMESTAMP
					&& !s.getString(4).startsWith("["))
				list.add(table+"."+s.getString(4));
		}
		return list;
	}

	public ResultSet getFMColumnMetaData(String f) throws SQLException {
		return this.fDataAccess.getColumnMetaData(f);
	}

	public ResultSet getMySQLColumnMetaData(String m) throws SQLException {
		return this.mDataAccess.getColumnMetaData(m);
	}

	public String getFMTablePrimaryKey(String f) {
		return this.fDataAccess.getTablePrimaryKey(f);
	}

	public boolean doFMAlter(String sql) throws SQLException, IOException {
		return fDataAccess.doSQLAlter(sql);
	}
}
