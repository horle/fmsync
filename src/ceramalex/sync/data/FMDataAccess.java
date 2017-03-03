package ceramalex.sync.data;

import java.sql.SQLException;

import ceramalex.sync.controller.ConfigController;

public class FMDataAccess extends AbstractDatabase {

	//-------------------------------------------------------------------------------
	public FMDataAccess(String dbUrl, String user, String pwd, String serverDataSource) throws SQLException {
		super(dbUrl, user, pwd, serverDataSource);
	}

	//-------------------------------------------------------------------------------
	@Override
	protected String getDriverName() {
		return "com.filemaker.jdbc.Driver";
	}
	
	public int[] doSQLAlter(String sql) throws SQLException {
		if (sql.toLowerCase().startsWith("alter table datierung")) {
			ConfigController conf = ConfigController.getInstance();
			return this.doSQLModifyViaNewConnection(sql, conf.getFmURL(), conf.getFmUser(), conf.getFmPassword(), "iDAIDatierung");
			
		} else if (sql.toLowerCase().startsWith("alter table literatur") || sql.toLowerCase().contains("alter table literaturzitat")) {
			ConfigController conf = ConfigController.getInstance();
			return this.doSQLModifyViaNewConnection(sql, conf.getFmURL(), conf.getFmUser(), conf.getFmPassword(), "iDAILiteratur");
			
		} else {
			return doSQLModify(sql);
		}
	}
	
	//-------------------------------------------------------------------------------
	@Override
	protected String getConnectionURL(String url, String user, String pwd,
			String serverDataSource) {
		
		// Filemaker 10 trennt die Parameter mittels ";", Filemaker 11 verwendet "&"
		String resultURL = url;
		resultURL += "/"+ serverDataSource;
		resultURL += "?user="+ user; 
		resultURL += "&password="+ pwd;
		System.out.println(resultURL);
		return resultURL;
		
//		return "jdbc:filemaker://134.95.115.20/test?user=admin&password=";
	}
	//-------------------------------------------------------------------------------

}
