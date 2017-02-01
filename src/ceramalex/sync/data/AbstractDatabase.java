package ceramalex.sync.data;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * 
 * @author Patrick Gunia Abstract Basisklasse fuer alle Subklassen, die ueber
 *         JDBC eine Connection zu unterschiedlichen DBMS aufbauen wollen
 *
 */

public abstract class AbstractDatabase {

	/** Logging-Instanz */
	protected static Logger logger = Logger.getLogger("data.abstractdatabase");

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
			String serverDataSource) throws SQLException {

		createConnection(dbUrl, user, pwd, serverDataSource);
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
	protected void createConnection(String dbUrl, String user, String pwd,
			String serverDataSource) throws SQLException {

		logger.info("DBURL: '" + dbUrl + "' User: '" + user + "' Pass: '" + pwd
				+ "' DataSource: '" + serverDataSource + "'");

		// Registrieren des JDBC-Client-Treibers
		Driver d = null;
		try {
			d = (Driver) Class.forName(getDriverName()).newInstance();
		} catch (Exception e) {
			logger.error("::::" + e);
		}

		// eine Verbindung mit DB herstellen
		try {
			DriverManager.registerDriver(d);
			this.cn = DriverManager.getConnection(getConnectionURL(dbUrl, user,
					pwd, serverDataSource));

		} catch (SQLException e) {
			logger.error("Driver:" + e);
			System.out.println("Driver:" + e);
			String eMsg = e.getMessage();
			if (getDriverName().contains("mysql"))
				eMsg = "MySQL: " + eMsg;
			throw new SQLException(eMsg); // throw to gui ...
		}
		// Verbindungswarnungen holen + ";serverDataSource=" + dbName
		SQLWarning warning = null;
		try {
			warning = cn.getWarnings();
			if (warning == null) {
				logger.trace("Keine Warnungen");
				return;
			}
			while (warning != null) {
				logger.warn(warning);
				warning = warning.getNextWarning();
			}
		} catch (Exception e) {
			System.out.println("stacktrace");
			e.printStackTrace();
		}

		try {
			this.st = cn.createStatement();
			assert this.st != null : "FEHLER!";
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode sendet eine SQL-Anfrage an die Zieldatenbank
	 * 
	 * @param sql
	 *            SQL-SELECT-Statement
	 * @return ResultSet als Ergebnis der abgesendeten SQL-Anfrage
	 */
	public ResultSet doSqlQuery(String sql) {

		try {
			if (this.st == null)
				this.st = cn.createStatement();
			ResultSet rs = this.st.executeQuery(sql);

			// teste, ob das ResultSet leer ist
			if (!rs.next())
				return null;

			// sonst setze den Cursor wieder zurueck auf Anfang
			else {
				rs.beforeFirst();
				return rs;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode fuehrt saemtliche Modify-Anweisungen durch, also INSERT, UPDATE
	 * und DELETE
	 * 
	 * @param sql
	 *            SQL-Statement
	 * @return True, falls die Anweisung erfolgreich ausgefuehrt werden konnte,
	 *         False sonst
	 */

	public boolean doSQLModify(String sql) {

		try {
			// logger.info(sql);
			// System.out.println("ITIS"+sql.toString());
			PreparedStatement prepStatement = cn.prepareStatement(sql);
			prepStatement.execute();
			prepStatement.close();
		} catch (Exception e) {
			logger.error("ITIS:" + sql);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------------
	/**
	 * Methode liefert die Metadaten fuer das uebergebene ResultSet
	 * 
	 * @param rs
	 *            ResultSet, fuer das die Metadaten geholt werden sollen
	 * @return ResultMetaDaten fuer uebergebenes ResultSet
	 */
	public ResultSetMetaData getMetaData(ResultSet rs) {

		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			return rsmd;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
			ResultSetMetaData rsmd = this.getMetaData(rs);
			return rsmd.getColumnCount();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
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
			e.printStackTrace();
		}
		logger.info("Database-Connector erfolgreich zerstoert");
	}

	// -------------------------------------------------------------------------------
	/** Destruktor (nur fuer Verbindung!) */
	public boolean closeConnection() {
		try {
			this.cn.close();
			this.cn = null;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		logger.info("Database-Connector erfolgreich zerstoert");
		return true;
	}

	/*
	 * Liefert Status der Verbindung als boolean
	 * 
	 * @return true, falls verbunden.
	 */
	public boolean isConnected() throws SQLException {
		if (cn == null)
			return false;
		return cn.isValid(5);
	}

	// -------------------------------------------------------------------------------

}
