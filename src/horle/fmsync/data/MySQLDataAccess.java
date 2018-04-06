package horle.fmsync.data;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class MySQLDataAccess extends AbstractDatabase {
	
	//-------------------------------------------------------------------------------
	public MySQLDataAccess(String dbUrl,String user, String pwd, String serverDataSource) throws SQLException {
		super(dbUrl, user, pwd, serverDataSource);
		
		logger = Logger.getLogger(this.getClass());
	}
	
	/**
	 * Method returns primary key of mysql table
	 * 
	 * @return name of primary key. if not found, returns empty string
	 * @throws SQLException 
	 */
	@Override
	public TreeSet<String> getTablePrimaryKey(String table) throws SQLException {
		TreeSet<String> result = new TreeSet<String>();
		ResultSet md = cn.getMetaData().getPrimaryKeys(null, null, table);
		if (md.next()) {
			result.add(md.getString("COLUMN_NAME"));
		}
		return result;
	}
	
	/**
	 * Method returns metadata of given table
	 * 
	 * @param table
	 *            table to get the metadata from
	 * @return ResultSet with metadata
	 * @throws SQLException 
	 */
	public ResultSet getTableMetaData(String table) throws SQLException {

		DatabaseMetaData md = cn.getMetaData();
		return md.getColumns(null, null, table, "%");
	}
	
	//-------------------------------------------------------------------------------
	@Override
	protected String getDriverName() {
		return "com.mysql.jdbc.Driver";
	}

	//-------------------------------------------------------------------------------
	@Override
	protected String getConnectionURL(String url, String user, String pwd,
			String serverDataSource) {
		String result = url;
		
		// Datenbankname
		result += "/"+serverDataSource;
		result += "?user=" + user;
		result += "&password=" + pwd;
		
		// Encoding fest vorgeben
		result += "&useUnicode=true&characterEncoding=UTF-8";
		result += "&connectTimeout=3000&zeroDateTimeBehavior=convertToNull";
		return result;
	}
	
	//-------------------------------------------------------------------------------


}