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
			
			// get common tables from DBs
			ResultSet fTab,mTab;
			for (int i = 0; i < commonTables.size(); i++) {
				fTab = sqlAccess.doFMQuery("SELECT * FROM "
						+ commonTables.get(i));
				mTab = sqlAccess.doMySQLQuery("SELECT * FROM "
						+ commonTables.get(i).toLowerCase());
				
				int count = 0;
				
				if (fTab != null && mTab != null) {
					// iterate through both table PKs //TODO: muss nicht eindeutig sein, bessere logik!
					while (fTab.next() && mTab.next()){
						int fID = fTab.getInt(1);
						int mID = mTab.getInt(1);
						
						if (fID == 0 || mID == 0){
							System.err.println("has to be downloaded: ID \"" + (fID==0?mID+"\" from arachne":fID+"\" from filemaker"));
							continue;
						}
						
						// luecke zwischen eintraegen? zB id: 67, 69, 75
						if (fID != mID){
							System.out.println(commonTables.get(i)+ ": " + mID + " in mysql does not equal " + fID + " in filemaker");
														
							// get all common field names in this table
							ArrayList<String> commonFields = new ArrayList<String>();
							commonFields = getCommonFields(mTab, fTab);
							
							//calculate ID difference:
							boolean msSmaller;
							int diff = 0;
							if (fID > mID){
								diff = fID-mID;
								msSmaller = true;	// mysql id is smaller.
								System.out.println("mysql smaller id");
							}
							else{
								diff = mID-fID;
								msSmaller = false;	// filemaker id is smaller.
								System.out.println("fm smaller id");
							}
							
							System.out.println("ID Difference is "+diff);
							
							// shift cursor, so that both IDs are equal
							// TODO: THIS WILL SKIP SOME ENTRIES; RECHECK FOR UP/DOWNLOAD/DELETE?!
							if (msSmaller){
								if (!mTab.relative(diff))
									System.err.println("Cursor could not be decreased!");
							}else
								if (!fTab.relative(diff))
									System.err.println("Cursor could not be decreased!");
							
							/** DEBUG
							 * 
							 * 
							 */
							int f = fTab.getInt(1);
							int m = mTab.getInt(1);
							System.out.println("Shifting cursor! does "+f+" equal "+m+"?");
							/**
							 * 
							 */
							
							// check each other field for equality, if so, update ID to mysql ID
							int col = commonFields.size();
							System.out.println("checking each of the fields ("+col+") in entry " + fID + " of table "+commonTables.get(i));
							boolean res = true;
							for (int l = 1; l < col; l++){ // start after ID field, this is not equal anyway
								
								// Strings to compare in field with equal name
								String s1 = mTab.getString(commonFields.get(l).toLowerCase());
								String s2 = fTab.getString(commonFields.get(l));
								
								s1 = s1 == null ? "" : s1;
								s2 = s2 == null ? "" : s2;
								
								if (!(s1.equals(s2))) {
									res = false;
									System.out.println(commonFields.get(l)+" in "+commonTables.get(i)+" not equal: \""+s1+"\" and \""+s2+"\"");
									break;
								}
							}
							System.out.println("each field equal? "+res);
							//f.updateInt(1, count+1);
							
//							//reshift cursor
//							if (msSmaller){
//								if (!fTab.relative(diff))
//									System.err.println("Cursor could not be increased!");
//							}else
//								if (!mTab.relative(diff))
//									System.err.println("Cursor could not be increased!");
						}
						count++;
					}
					while (fTab.next()) System.err.println(commonTables.get(i) + "  "+ fTab.getString(1) + " has no partner in filemaker!");
					while (mTab.next()) System.err.println(commonTables.get(i) + "  "+ mTab.getString(1) + " has no partner in mysql!");
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
