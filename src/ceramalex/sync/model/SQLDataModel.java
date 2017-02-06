package ceramalex.sync.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import ceramalex.sync.controller.SQLAccessController;

public class SQLDataModel {
	private SQLAccessController sqlAccess;
	private ArrayList<String> commonTables = new ArrayList<String>();

	public ResultSet getDiffByUUID(String table) throws SQLException {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ArrayList<String> fmNames = new ArrayList<String>();
		ArrayList<String> msNames = new ArrayList<String>();
	
		sqlAccess = SQLAccessController.getInstance();
		ResultSet metaFM = sqlAccess.getFMMetaData();
		ResultSet metaMS = sqlAccess.getMySQLMetaData();
				
		if (sqlAccess.isMySQLConnected()) {
//			System.out.println("mysql is connected");
			
			// get last arachne identifier per table
//			ResultSet last = sqlAccess
//					.doMySQLQuery("SELECT MAX(ArachneForeignKey), TableName "
//							+ "FROM ceramalexEntityManagement "
//							+ "GROUP BY TableName");

//			while (last.next()) {
//				ids.add(last.getInt(1));
//				msNames.add(last.getString(2));
//			}
			while (metaFM.next()) {
				fmNames.add(metaFM.getString("TABLE_NAME"));
			}
			while (metaMS.next()) {
				msNames.add(metaMS.getString("TABLE_NAME"));
			}
			
			// gemeinsame tables!
			for (int i = 0; i < msNames.size(); i++){
				for (int j = 0; j < fmNames.size(); j++){
					if (msNames.get(i).toLowerCase().equals(fmNames.get(j).toLowerCase())){
//						System.out.println("Table Match! "+ msNames.get(i) + ": "+i+" equals "+fmNames.get(j)+": " +j);
						commonTables.add(fmNames.get(j));
					}
				}
			}
			
//			for (int i = 0; i < commonTables.size(); i++){
//				System.out.println(commonTables.get(i));
//			}
			// System.out.println(ids);
			// System.out.println(names);

			// if (sqlAccess.isFMConnected()) {
//			System.out.println("fm connected");
			ResultSet f,m;
			for (int i = 0; i < commonTables.size(); i++) {
				f = sqlAccess.doFMQuery("SELECT * FROM "
						+ commonTables.get(i));
				m = sqlAccess.doMySQLQuery("SELECT * FROM "
						+ commonTables.get(i).toLowerCase());
				
				int count = 0;
				
				if (f != null && m != null) {
					while (f.next() && m.next()){	// check both
						String fT = f.getString(1);
						String mT = m.getString(1);
						
						if (fT == null || mT == null){
							System.err.println("has to be downloaded: " + (fT==null?mT+" from arachne":fT+" from filemaker"));
							continue;
						}
						// lücke zwischen einträgen? zB id: 67, 69, 70
						if (!(""+count+1).equals(fT)){
							sqlAccess.doFMUpdate(""); // id updaten?!
						}
						
						if (!fT.equals(mT)){
							System.err.println(commonTables.get(i)+ "  " + mT + " in mysql does not equal " + fT + " in filemaker");
						}
	//					else
	//						System.out.println(commonTables.get(i)+ "  " + fT + " does equal " + mT);
						count++;
					}
					while (f.next()) System.err.println(commonTables.get(i) + "  "+ f.getString(1) + " has no partner in filemaker!");
					while (m.next()) System.err.println(commonTables.get(i) + "  "+ m.getString(1) + " has no partner in mysql!");
				}
				System.out.println(commonTables.get(i) + " done with "+count+" entries. next table!");
			}
			System.out.println("done");
		}

		return null;
		// TODO
	}
	
	public static void main(String[] args) {
		try {
			(new SQLDataModel()).getDiffByUUID("");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
