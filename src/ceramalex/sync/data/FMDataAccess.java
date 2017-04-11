package ceramalex.sync.data;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import ceramalex.sync.controller.ConfigController;

public class FMDataAccess extends AbstractDatabase {

	//-------------------------------------------------------------------------------
	public FMDataAccess(String dbUrl, String user, String pwd, String serverDataSource) throws SQLException {
		super(dbUrl, user, pwd, serverDataSource);
	}
	
	public FMDataAccess() throws SQLException {
		super("","","","");
	}

	//-------------------------------------------------------------------------------
	@Override
	protected String getDriverName() {
		return "com.filemaker.jdbc.Driver";
	}
	
	public boolean doSQLAlter(String sql) throws SQLException, IOException {
		if (sql.toLowerCase().startsWith("alter table datierung")) {
			ConfigController conf = ConfigController.getInstance();
			this.doSQLModifyViaNewConnection(sql, conf.getFmURL(), conf.getFmUser(), conf.getFmPassword(), "iDAIDatierung");
			return true;
		} else if (sql.toLowerCase().startsWith("alter table literatur") || sql.toLowerCase().contains("alter table literaturzitat")) {
			ConfigController conf = ConfigController.getInstance();
			this.doSQLModifyViaNewConnection(sql, conf.getFmURL(), conf.getFmUser(), conf.getFmPassword(), "iDAILiteratur");
			return true;
		} else {
			doSQLModify(sql);
			return true;
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
		return resultURL;
	}
	//-------------------------------------------------------------------------------

}
