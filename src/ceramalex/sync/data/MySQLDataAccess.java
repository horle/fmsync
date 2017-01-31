package ceramalex.sync.data;

import java.sql.SQLException;

public class MySQLDataAccess extends AbstractDatabase {
	
	//-------------------------------------------------------------------------------
	public MySQLDataAccess(String dbUrl,String user, String pwd, String serverDataSource) throws SQLException {
		super(dbUrl, user, pwd, serverDataSource);
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
		result += serverDataSource;
		result += "?user=" + user;
		result += "&password=" + pwd;
		
		// Encoding fest vorgeben
		result += "&useUnicode=true&characterEncoding=UTF-8";
		return result;
	}
	
	//-------------------------------------------------------------------------------


}