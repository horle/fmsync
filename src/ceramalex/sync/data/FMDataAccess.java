package ceramalex.sync.data;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
	
	
	/**
	 * Method returns primary key of filemaker table
	 * 
	 * @return name of primary key for certain table. if not found, returns empty string
	 */
	@Override
	public String getTablePrimaryKey(String table) {
		String result = "";
		
		try {
			ResultSet r = cn.getMetaData().getColumns(null, "iDAIAbstractCeramalex", table, "PS%");
			if (r.next())
				result = r.getString("COLUMN_NAME");
			while (r.next()) { // another PS_* column?
				result += "," + r.getString("COLUMN_NAME");
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return result;
		}
	}

	/**
	 * Method returns metadata of given FM table
	 * 
	 * @param table
	 *            table to get the metadata from
	 * @return ResultSet with metadata
	 * @throws SQLException 
	 */
	@Override
	public ResultSet getColumnMetaData(String table) throws SQLException {
			DatabaseMetaData md = cn.getMetaData();
			return md.getColumns(null, "iDAIAbstractCeramalex", table, "%");
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
