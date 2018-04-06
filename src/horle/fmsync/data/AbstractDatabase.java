package horle.fmsync.data;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.SortedSet;

import org.apache.log4j.Logger;

/**
 * 
 * @author Patrick Gunia Abstract Basisklasse fuer alle Subklassen, die ueber
 *         JDBC eine Connection zu unterschiedlichen DBMS aufbauen wollen
 *
 */

public abstract class AbstractDatabase {
	
	private String dbURL, user, pwd;

	protected String serverDataSource;

	/** Logging-Instanz */
	protected static Logger logger = Logger.getLogger(AbstractDatabase.class);

	/** Liefert den Namen des zu ladenden Treibers */
	abstract protected String getDriverName();

	/** Baut die datenbankspezifische Connecion-URL */
	abstract protected String getConnectionURL(String url, String user,
			String pwd, String serverDataSource);

	/** Instanz der erzeugten Connection */
	protected Connection cn = null;

	/**
	 * Statement-Instanz, ueber die SQL-Befehle an die Zieldatenbank gesendet
	 * werden
	 */
	protected Statement st = null;

	/**
	 * Default-Konstruktor mit Uebergabe von URL, User und Password
	 * 
	 * @param dbUrl
	 *            URL der Zieldatenbank
	 * @param user
	 *            Username auf der Datenbank
	 * @param pwd
	 *            Passwort fuer den uerbegebenen User
	 * @param serverDataSource
	 *            Datenbankname (bsw. ceramalex oder iDAIAbstractCeramalex)
	 * @throws SQLException
	 */
	public AbstractDatabase(String dbUrl, String user, String pwd,
			String serverDataSource) {

		this.dbURL = dbUrl;
		this.user = user;
		this.pwd = pwd;
		this.serverDataSource = serverDataSource;
	}

	public boolean createConnection() throws SQLException {
		return createConnection(dbURL, user, pwd, serverDataSource);
	}
	
	// -------------------------------------------------------------------------------
	/**
	 * Methode erzeugt eine Verbindung zu einer Datenbank
	 * 
	 * @param sDbUrl
	 *            URL der Zieldatenbank
	 * @param sUser
	 *            Username auf der Datenbank
	 * @param sPwd
	 *            Passwort fuer den uerbegebenen User
	 */
	public boolean createConnection(String dbUrl, String user, String pwd,
			String serverDataSource) throws SQLException {

		boolean result = false;
		
		logger.info(this.getDriverName() + ": "+ "DBURL: '" + dbUrl + "' User: '" + user + "' Pass: '" + pwd
				+ "' DataSource: '" + serverDataSource + "'");

		// Registrieren des JDBC-Client-Treibers
		Driver d = null;
		try {
			d = (Driver) Class.forName(getDriverName()).newInstance();
		} catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
			logger.error(e);
		}

		// eine Verbindung mit DB herstellen
		try {
			DriverManager.registerDriver(d);
			DriverManager.setLoginTimeout(5);
			this.cn = DriverManager.getConnection(getConnectionURL(dbUrl, user,
					pwd, serverDataSource));
			result = isConnected();
		}
		catch (SQLException e) {
			logger.error("Driver:" + e);
			String eMsg = e.toString();
			if (getDriverName().contains("mysql"))
				eMsg = "MySQL: " + e;
			throw new SQLException(eMsg); // throw to gui ... 
		}
		// Verbindungswarnungen holen + ";serverDataSource=" + dbName
		SQLWarning warning = null;
		try {
			warning = cn.getWarnings();
			if (warning == null) {
				logger.debug("Keine Warnungen");
				return result;
			}
			while (warning != null) {
				logger.warn(warning);
				warning = warning.getNextWarning();
			}
		} catch (SQLException e) {
			logger.error(e);
			e.printStackTrace();
			return false;
		}

		try {
			this.st = cn.createStatement();
			assert this.st != null : "FEHLER!";
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode sendet eine SQL-Anfrage an die Zieldatenbank
	 * 
	 * @param sql
	 *            SQL-SELECT-Statement
	 * @return ResultSet als Ergebnis der abgesendeten SQL-Anfrage
	 * @throws SQLException 
	 */
	public ResultSet doSQLQuery(String sql) throws SQLException {
		logger.debug(this.getDriverName() + ": "+ sql);
		if (isConnected()) {
			PreparedStatement st = cn.prepareStatement(sql);
			return st.executeQuery();
		}
		return null;
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode fuehrt saemtliche Modify-Anweisungen durch, also INSERT, UPDATE
	 * und DELETE
	 * 
	 * @param sql
	 *            SQL-Statement
	 * @return IDs of modified entries, null if no success.
	 * @throws SQLException 
	 */

	public ArrayList<Integer> doSQLModify(String sql) throws SQLException {

		logger.debug(this.getDriverName() + ": "+ sql);
		PreparedStatement statement = null;

		statement = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		statement.executeUpdate();

		ResultSet r = statement.getGeneratedKeys();
		
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		r.beforeFirst();
		while (r.next()) {
			result.add(r.getInt(1));
		}
		
		statement.close();
		return result;
	}
	
	/**
	 * Opens a new connection to the given database. Most suitable for altering table schemas in FileMaker, as this is not possible for "external tables". Closes connection afterwards.
	 * @param sql SQL modify query
	 * @param dbUrl URL for connection
	 * @param user user name for connection
	 * @param pwd password for connection
	 * @param serverDataSource server database name
	 * @return ID of modified entry, -1 if no success.
	 * @throws SQLException Throws exception if sql error occurred.
	 */
	public ArrayList<Integer> doSQLModifyViaNewConnection(String sql, String dbUrl, String user, String pwd, String serverDataSource) throws SQLException {
		Connection con;
		try {
			DriverManager.setLoginTimeout(5);
			con = DriverManager.getConnection(getConnectionURL(dbUrl, user,
					pwd, serverDataSource));
		}
		catch (SQLException e) {
			logger.error("Driver:" + e);
			String eMsg = e.getMessage();
			if (getDriverName().contains("mysql"))
				eMsg = "MySQL: " + eMsg;
			throw new SQLException(eMsg); // throw to gui ... 
		}
		logger.debug(this.getDriverName() + ": "+ sql);
		PreparedStatement statement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		statement.executeUpdate();
				
		ResultSet r = statement.getGeneratedKeys();

		ArrayList<Integer> result = new ArrayList<Integer>();
		
		r.beforeFirst();
		while (r.next()) {
			result.add(r.getInt(1));
		}
		
		statement.close();
		con.close();
		
		return result;
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode liefert die Metadaten fuer das uebergebene ResultSet
	 * 
	 * @param rs
	 *            ResultSet, fuer das die Metadaten geholt werden sollen
	 * @return ResultMetaDaten fuer uebergebenes ResultSet
	 * @throws SQLException 
	 */
	public ResultSetMetaData getRSMetaData(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		return rsmd;
	}
	
	/**
	 * Method returns metadata of given table
	 * 
	 * @param table
	 *            table to get the metadata from
	 * @return ResultSet with metadata
	 * @throws SQLException 
	 */
	public abstract ResultSet getTableMetaData(String table) throws SQLException;
	
	/**
	 * Method returns primary key of given table
	 * 
	 * @return name of primary key. if not found, returns empty string
	 * @throws SQLException 
	 */
	public abstract SortedSet<String> getTablePrimaryKey(String table) throws SQLException;

	/**
	 * method returns metadata object for db
	 * 
	 * @return metadata for db
	 * @throws SQLException 
	 */
	public DatabaseMetaData getDBMetaData() throws SQLException {
		
		return cn.getMetaData();
	}
	
	/**
	 * method returns metadata for all tables
	 * 
	 * @return metadata for all tables as ResultSet
	 * @throws SQLException 
	 */
	public ResultSet getTableMetaData() throws SQLException {

		String[] types = {"TABLE"};
		ResultSet md = cn.getMetaData().getTables(null, null, "%", types);
		return md;
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode liefert die Anzahl von Ergebnisreihen im Uebergabe-ResultSet
	 * 
	 * @param rs
	 *            ResultSet, fuer das die Anzahl an Zeilen bestimmt werden
	 *            sollen
	 * @return Anzahl Ergebniszeilen
	 */
	public int getColumnCount(ResultSet rs) {
		try {
			ResultSetMetaData rsmd = this.getRSMetaData(rs);
			return rsmd.getColumnCount();
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	// -------------------------------------------------------------------------------
	/** Destruktor */
	@Override
	public void finalize() {
		try {
			this.st.close();
			this.st = null;
			this.cn.close();
			this.cn = null;
		} catch (SQLException e) {
			if (!e.getMessage().contains("[FileMaker JDBC] Connection is closed. Execute operation is not permitted."))
				e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------------
	/** Destruktor (nur fuer Verbindung!) */
	public boolean closeConnection() {
		try {
			if (cn != null)
				this.cn.close();
			else
				return true;
		} catch (SQLException e) {
			logger.error(e);
			return false;
		}
		return true;
	}

	/*
	 * Liefert Status der Verbindung als boolean
	 * 
	 * @return true, falls verbunden.
	 */
	public boolean isConnected() {
		if (cn == null){
			return false;
		}
		try {
			getDBMetaData();
			return true;
		} catch (SQLException e) {
			if (e.toString().contains("Connection is closed") || e.toString().contains("connection closed")) return false;
			
			logger.error(e);
			System.out.println(e);
			return false;
		}
	}

	// -------------------------------------------------------------------------------

}
