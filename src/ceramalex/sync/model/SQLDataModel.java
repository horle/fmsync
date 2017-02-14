package ceramalex.sync.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.FilemakerIsCrapException;

import com.filemaker.jdbc.FMSQLException;

public class SQLDataModel {
	private SQLAccessController sqlAccess;
	private static ArrayList<Pair> commonTables = new ArrayList<Pair>();

	public SQLDataModel() throws SQLException {
		sqlAccess = SQLAccessController.getInstance();
		sqlAccess.connect();
	}
	
	private ArrayList<Pair> getCommonTables() throws SQLException {
		ArrayList<Pair> result = new ArrayList<Pair>();
		
		ArrayList<String> fmNames = new ArrayList<String>();
		ArrayList<String> msNames = new ArrayList<String>();

		ResultSet metaFM = sqlAccess.getFMDBMetaData();
		ResultSet metaMS = sqlAccess.getMySQLDBMetaData();
		
		if (sqlAccess.isMySQLConnected()) {
			// System.out.println("mysql is connected");
			while (metaFM.next()) {
				fmNames.add(metaFM.getString("TABLE_NAME"));
			}
			while (metaMS.next()) {
				msNames.add(metaMS.getString("TABLE_NAME"));
			}
		
			for (int i = 0; i < msNames.size(); i++) {
				for (int j = 0; j < fmNames.size(); j++) {
					if (msNames.get(i).toLowerCase().equals(fmNames.get(j).toLowerCase())) {
						Pair t = new Pair(fmNames.get(j), msNames.get(i));
						result.add(t);
					}
					if (fmNames.get(j).equals("IsolatedSherdMainAbstract") && msNames.get(i).equals("isolatedsherd")){
						Pair t = new Pair(fmNames.get(j), msNames.get(i));
						result.add(t);
					}
				}
			}
		}
		return result;
	}
	
	public ResultSet getDiffByUUID(String table) throws SQLException,
			FilemakerIsCrapException {
		ArrayList<String> fmNames = new ArrayList<String>();
		ArrayList<String> msNames = new ArrayList<String>();

		sqlAccess = SQLAccessController.getInstance();
		sqlAccess.connect();
		ResultSet metaFM = sqlAccess.getFMDBMetaData();
		ResultSet metaMS = sqlAccess.getMySQLDBMetaData();

		if (sqlAccess.isMySQLConnected()) {
			// System.out.println("mysql is connected");
			while (metaFM.next()) {
				fmNames.add(metaFM.getString("TABLE_NAME"));
			}
			while (metaMS.next()) {
				msNames.add(metaMS.getString("TABLE_NAME"));
			}

			// calculate common tables!
			commonTables = getCommonTables();
			sqlAccess.fetchNumericFields(commonTables);

			// get common tables from DBs
			for (int i = 0; i < commonTables.size(); i++) {
				Pair currTab = commonTables.get(i);
				System.out.println(currTab);

				ResultSet fTab = sqlAccess
						.doFMQuery("SELECT * FROM " + currTab.getF());
				// A-AUID + rest of table
				ResultSet mTab = sqlAccess
						.doMySQLQuery("SELECT ceramalexEntityManagement.ArachneEntityID, "
								+ currTab.getM()
								+ ".* FROM ceramalexEntityManagement LEFT JOIN " // left includes deleted or empty UIDs
								+ currTab.getM()
								+ " ON ceramalexEntityManagement.CeramalexForeignKey = "
								+ currTab.getM()
								+ "."
								+ sqlAccess.getMySQLTablePrimaryKey(currTab.getM())
								+ " WHERE ceramalexEntityManagement.TableName=\""
								+ currTab.getM() + "\"");

				int count = 0;
				

				if (fTab != null && mTab != null) {
					
					ArrayList<String> commonFields = new ArrayList<String>();
					
					try {
						while (fTab.next() && mTab.next()) { // works, because a new entry is at the end of the list
							
							// entities:
							int currAAUID = mTab.getInt("ArachneEntityID");	// current arachne uid in arachne
							int currCAUID = fTab.getInt("ArachneEntityID"); // current arachne uid in fm
							// C-AUID differs from online ...
							if (currCAUID != currAAUID) {
								// ... because remote entry needs to be downloaded or was deleted locally (local is null)
								if (currCAUID == 0) {
									if (commonFields.isEmpty())
										commonFields = getCommonFields(mTab, fTab);
									
									int col = commonFields.size();
//									System.out.println("checking each of the fields ("+col+") in entry "
//											+ currAAUID + " of table "+commonTables.get(i));
									
									boolean res = true;
									for (int l = 0; l < col; l++){ // start after ID field, this is not equal anyway
									
										if (commonFields.get(l).equalsIgnoreCase("ArachneEntityID") || commonFields.get(l).equalsIgnoreCase("lastModified"))
											continue;
										
										// Strings to compare in field with equal name
										String sM = mTab.getString(commonFields.get(l));
										String sF = fTab.getString(commonFields.get(l));
										
										sM = sM == null ? "" : sM;
										sF = sF == null ? "" : sF;

										
										if (!(sM.equalsIgnoreCase(sF))) {
											if (isNumericalField(commonFields.get(l))){
												try {
													double m = Double.parseDouble(sM);
													double f = Double.parseDouble(sF);
													if (m == f)
														continue;
												} catch (NumberFormatException e) {
													System.out
															.println("NFException!");
												}
											}
											res = false;
											System.out.println(commonFields.get(l)+" in "+commonTables.get(i)+" not equal: \""+sM+"\" and \""+sF+"\"");
											break;
										}
									}
									// System.out.println("each field equal? "+res);
									//f.updateInt(1, count+1);
									if (!res) System.out.println("remote entry "+currAAUID+" needs to be downloaded or was deleted locally");
								}
								// ... because local entry needs to be uploaded or was deleted remote (remote is null)
								else if (currAAUID == 0) {
									System.out.println("local entry "+currCAUID+" needs to be uploaded or was deleted remote");
								}
							}
							// C-AUID == A-AUID! examine lastModified
							else {
								// arachne newer than local?
								if (mTab.getDate("lastModified").after(fTab.getDate("lastModified"))){
									// download from arachne TODO
									System.out.println("download from arachne");
								}
								else if (mTab.getDate("lastModified").before(fTab.getDate("lastModified"))) {
									// upload to arachne TODO
									System.out.println("upload to arachne");
								}
								else {
									// no changes or nothing to down-/upload, skip.
									continue;
								}
							}
						}
					} catch (FMSQLException e) {
						System.err.println("FEHLER: " + e);
						
						// TODO: add column and continue with "old" processing
						if (e.getMessage().contains(
								"Column name not found: ArachneEntityID")) {
							throw new FilemakerIsCrapException(
									"Column ArachneEntityID has to be added manually into table \""
											+ currTab.getF() + "\"", 0);
						}
						if (e.getMessage().contains(
								"Column name not found: lastModified")) {
							throw new FilemakerIsCrapException(
									"Column lastModified has to be added manually into table \""
											+ currTab.getF() + "\"", 0);
						}
					}
				}

				/**
				 * ab hier alte logik!
				 */

				// if (fTab != null && mTab != null) {
				// // iterate through both table PKs //TODO: muss nicht
				// eindeutig sein, bessere logik!
				// while (fTab.next() && mTab.next()){
				// int fID = fTab.getInt(1);
				// int mID = mTab.getInt(1);
				//
				// if (fID == 0 || mID == 0){
				// System.err.println("has to be downloaded: ID \"" +
				// (fID==0?mID+"\" from arachne":fID+"\" from filemaker"));
				// continue;
				// }
				//
				// // luecke zwischen eintraegen? zB id: 67, 69, 75
				// if (fID != mID){
				// System.out.println(commonTables.get(i)+ ": " + mID +
				// " in mysql does not equal " + fID + " in filemaker");
				//
				// // get all common field names in this table
				// ArrayList<String> commonFields = new ArrayList<String>();
				// commonFields = getCommonFields(mTab, fTab);
				//
				// //calculate ID difference:
				// boolean msSmaller;
				// int diff = 0;
				// if (fID > mID){
				// diff = fID-mID;
				// msSmaller = true; // mysql id is smaller.
				// System.out.println("mysql smaller id");
				// }
				// else{
				// diff = mID-fID;
				// msSmaller = false; // filemaker id is smaller.
				// System.out.println("fm smaller id");
				// }
				//
				// System.out.println("ID Difference is "+diff);
				//
				// // shift cursor, so that both IDs are equal
				// // TODO: THIS WILL SKIP SOME ENTRIES; RECHECK FOR
				// UP/DOWNLOAD/DELETE?!
				// if (msSmaller){
				// if (!mTab.relative(diff))
				// System.err.println("Cursor could not be decreased!");
				// }else
				// if (!fTab.relative(diff))
				// System.err.println("Cursor could not be decreased!");
				//
				// /** DEBUG
				// *
				// *
				// */
				// int f = fTab.getInt(1);
				// int m = mTab.getInt(1);
				// System.out.println("Shifting cursor! does "+f+" equal "+m+"?");
				// /**
				// *
				// */
				//
				// // check each other field for equality, if so, update ID to
				// mysql ID
				// int col = commonFields.size();
				// System.out.println("checking each of the fields ("+col+") in entry "
				// + fID + " of table "+commonTables.get(i));
				// boolean res = true;
				// for (int l = 1; l < col; l++){ // start after ID field, this
				// is not equal anyway
				//
				// // Strings to compare in field with equal name
				// String s1 =
				// mTab.getString(commonFields.get(l).toLowerCase());
				// String s2 = fTab.getString(commonFields.get(l));
				//
				// s1 = s1 == null ? "" : s1;
				// s2 = s2 == null ? "" : s2;
				//
				// if (!(s1.equals(s2))) {
				// res = false;
				// System.out.println(commonFields.get(l)+" in "+commonTables.get(i)+" not equal: \""+s1+"\" and \""+s2+"\"");
				// break;
				// }
				// }
				// System.out.println("each field equal? "+res);
				// //f.updateInt(1, count+1);
				//
				// // //reshift cursor
				// // if (msSmaller){
				// // if (!fTab.relative(diff))
				// // System.err.println("Cursor could not be increased!");
				// // }else
				// // if (!mTab.relative(diff))
				// // System.err.println("Cursor could not be increased!");
				// }
				// count++;
				// }
				// while (fTab.next()) System.err.println(commonTables.get(i) +
				// "  "+ fTab.getString(1) + " has no partner in filemaker!");
				// while (mTab.next()) System.err.println(commonTables.get(i) +
				// "  "+ mTab.getString(1) + " has no partner in mysql!");
				// }
				// System.out.println(commonTables.get(i) +
				// " done with "+count+" entries. next table!");
			}
			System.out.println("done");
		}
		return null;
	}

	/**
	 * get common fields in table x represented in fm and ms columns, etc
	 * 
	 * @param m
	 *            MySQL resultset of table
	 * @param f
	 *            FileMaker resultset of table
	 * @return common field names as Strings in ArrayList
	 */
	private ArrayList<String> getCommonFields(ResultSet m, ResultSet f)
			throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		ResultSetMetaData metaFMTab = f.getMetaData();
		ResultSetMetaData metaMSTab = m.getMetaData();

		for (int i = 1; i <= metaFMTab.getColumnCount(); i++) {
			for (int j = 1; j <= metaMSTab.getColumnCount(); j++) {
				if (metaFMTab.getColumnName(i).toLowerCase()
						.equals(metaMSTab.getColumnName(j).toLowerCase())) {
					result.add(metaFMTab.getColumnName(i));
				}
			}
		}

		return result;
	}
	
	private boolean isNumericalField(String field) {
		return ConfigController.getInstance().getNumericFields().contains(field);
	}

	public static void main(String[] args) {
		try {
			SQLDataModel m = new SQLDataModel();
			commonTables = m.getCommonTables();
			m.getDiffByUUID("");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
