package ceramalex.sync.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
		sqlAccess.connect();
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
			
			// common tables!
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
						// luecke zwischen eintraegen? zB id: 67, 69, 70
						if (!(""+count+1).equals(fT) && !(fT.equals(mT))){
							System.out.println("update "+fT+" with "+(count+1)+", ms is "+mT);
							
							// get all common fields in this table
							System.out.println("calculating common fields");
							ArrayList<String> commonFields = new ArrayList<String>();
							commonFields = getCommonFields(m, f);
							
							// check each other field for equality, if so, update ID to mysql
							int col = commonFields.size();
							System.out.println("checking each of the fields ("+col+") in fm entry " + fT + " of table "+commonTables.get(i));
							boolean res = true;
							for (int l = 1; l < col; l++){ // start after ID field, this is not equal anyway
								String s1 = m.getString(commonFields.get(l).toLowerCase());
								String s2 = f.getString(commonFields.get(l));
								s1 = s1 == null ? "" : s1;
								s2 = s2 == null ? "" : s2;

								System.out.println("testing "+s1+" and "+s2+" for equality:");
								
								if (!(s1.equals(s2))) {
									res = false;
									System.out.println(commonFields.get(l) +" not equal!");
								}
							}
							System.out.println("each field equal? "+res);
							//f.updateInt(1, count+1);
//							sqlAccess.doFMUpdate(""); // id updaten?!
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
	
	/**
	 * get common fields in table x represented in fm and ms
	 * filter "FMP" columns, etc
	 * @param m
	 * @param f
	 * @return
	 */
	private ArrayList<String> getCommonFields(ResultSet m, ResultSet f) throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		ResultSetMetaData metaFMTab = f.getMetaData();
		ResultSetMetaData metaMSTab = m.getMetaData();
		
		for (int i = 1; i <= metaFMTab.getColumnCount(); i++){
			for (int j = 1; j <= metaMSTab.getColumnCount(); j++){
				if (metaFMTab.getColumnName(i).toLowerCase().equals(metaMSTab.getColumnName(j).toLowerCase())){
					result.add(metaFMTab.getColumnName(i));
				}
			}
		}
		
		return result;
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
