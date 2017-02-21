package ceramalex.sync.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.EntityException;
import ceramalex.sync.exception.FilemakerIsCrapException;
import ceramalex.sync.exception.SyncException;

import com.filemaker.jdbc.FMSQLException;

/**
 * Internal logic for comparing databases
 * 
 * @author horle (Felix Kussmaul)
 *
 */
public class SQLDataModel {
	private SQLAccessController sqlAccess;
	private DateTimeFormatter formatTS;
	private ZoneId zoneBerlin;
	private static ArrayList<Pair> commonTables = new ArrayList<Pair>();

	public SQLDataModel() throws SQLException {
		formatTS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");//yyyy-MM-dd HH:mm:ss");
		zoneBerlin = ZoneId.of("Europe/Berlin");
		sqlAccess = SQLAccessController.getInstance();
		sqlAccess.connect();
	}

	private ArrayList<Pair> getCommonTables() throws SQLException {
		ArrayList<Pair> result = new ArrayList<Pair>();

		ArrayList<String> fmNames = new ArrayList<String>();
		ArrayList<String> msNames = new ArrayList<String>();

		ResultSet metaFM = sqlAccess.getFMDBMetaData();
		ResultSet metaMS = sqlAccess.getMySQLDBMetaData();

		if (sqlAccess.isMySQLConnected() && sqlAccess.isFMConnected()) {
			while (metaFM.next()) {
				fmNames.add(metaFM.getString("TABLE_NAME"));
			}
			while (metaMS.next()) {
				msNames.add(metaMS.getString("TABLE_NAME"));
			}

			for (int i = 0; i < msNames.size(); i++) {
				for (int j = 0; j < fmNames.size(); j++) {
					if (msNames.get(i).toLowerCase()
							.equals(fmNames.get(j).toLowerCase())) {
						Pair t = new Pair(fmNames.get(j), msNames.get(i));
						result.add(t);
					}
					// IsolatedSherdMainAbstract maps to isolatedsherd
					if (fmNames.get(j).equals("IsolatedSherdMainAbstract")
							&& msNames.get(i).equals("isolatedsherd")) {
						Pair t = new Pair(fmNames.get(j), msNames.get(i));
						result.add(t);
					}
				}
			}
		}
		sqlAccess.fetchNumericFields(result);
		return result;
	}

	public ResultSet getDiffByUUID(String table) throws SQLException,
			FilemakerIsCrapException, SyncException, EntityException {

		// both connected?
		if (sqlAccess.isMySQLConnected() && sqlAccess.isFMConnected()) {

			// calculate common tables!
			commonTables = getCommonTables();

			// get common tables from DBs
			for (int i = 0; i < commonTables.size(); i++) {
				Pair currTab = commonTables.get(i);
				System.out.println(currTab);
				
				ArrayList<String> commonFields = new ArrayList<String>();
				
				if (commonFields.isEmpty())
					commonFields = getCommonFields(currTab);
				commonFields.add("ArachneEntityID");

				String fSQL = "";
				for (int j = 0; j < commonFields.size(); j++){
					if (j == commonFields.size()-1)
						fSQL += commonFields.get(j);
					else
						fSQL += commonFields.get(j)+",";
				}
				
				ResultSet fTab = sqlAccess.doFMQuery("SELECT "+fSQL+" FROM "
						+ currTab.getF());
				// A-AUID + rest of table
				ResultSet mTab = sqlAccess
						.doMySQLQuery("SELECT ceramalexEntityManagement.ArachneEntityID, "
								+ currTab.getM()
								+ ".* FROM ceramalexEntityManagement LEFT JOIN " // left includes deleted or empty UIDs
								+ currTab.getM()
								+ " ON ceramalexEntityManagement.CeramalexForeignKey = "
								+ currTab.getM()
								+ "."
								+ sqlAccess.getMySQLTablePrimaryKey(currTab
										.getM())
								+ " WHERE ceramalexEntityManagement.TableName=\""
								+ currTab.getM() + "\"");

				if (fTab != null && mTab != null) {
					
					// entities:
					int currAAUID = 0; // current arachne uid in arachne
					int currCAUID = 0; // current arachne uid in fm
					Timestamp currATS = null; // current arachne timestamp
					Timestamp currCTS = null; // current fm timestamp
//					String currLocalTS = getBerlinTimeStamp(); // current timestamp in CET format (arachne is in germany ...)
					
					// test, if table empty
					if (mTab.next() && fTab.next()) {
						
						// test, if table contains UUID and TS fields
						try {
							// entities:
							currAAUID = mTab.getInt("ArachneEntityID");
							currCAUID = fTab.getInt("ArachneEntityID");
							currATS = mTab.getTimestamp("lastModified");
							currCTS = fTab.getTimestamp("lastModified");
							
						} catch (FMSQLException e) {
							System.err.println("FEHLER: " + e);
	
							// add column and proceed
							if (e.getMessage().contains(
									"Column name not found: ArachneEntityID")) {
								if (!sqlAccess.doFMInsert("ALTER TABLE "+currTab.getF()+" ADD ArachneEntityID NUMERIC"))
									throw new FilemakerIsCrapException(
											"Column ArachneEntityID has to be added manually into table \""
													+ currTab.getF() + "\"");
							} else 
							if (e.getMessage().contains(
									"Column name not found: lastModified")) {
								if (!sqlAccess.doFMInsert("ALTER TABLE "+currTab.getF()+" ADD lastModified TIMESTAMP"))
									throw new FilemakerIsCrapException(
											"Column lastModified has to be added manually into table \""
													+ currTab.getF() + "\"");
								throw new FilemakerIsCrapException("You have to enter the following script to each lastModified field: ...");
							}else
								throw e;
						}
					}
					
					// reset cursor for loop
					mTab.beforeFirst();
					fTab.beforeFirst();
					
					try {
						while (fTab.next() & mTab.next()) {
							
							currAAUID = mTab.getInt("ArachneEntityID");
							currCAUID = fTab.getInt("ArachneEntityID");
							currATS = mTab.getTimestamp("lastModified");
							currCTS = fTab.getTimestamp("lastModified");
							
							// missing entry in regular table, but entry in entity management! bug in db!
							// TODO: delete entry in arachne entity management
							if (currATS == null) {
								new EntityException("Missing entry in LOCAL Arachne entity management! Table "+currTab.getF()+", remote entry: "+currAAUID).printStackTrace();
								continue;
							}
							LocalDateTime currArachneTS = currATS.toLocalDateTime();
							
							// C-AUID differs from online ...
							if (currCAUID != currAAUID) {
								
								ArrayList<String> fmVals = new ArrayList<String>();
								ArrayList<String> msVals = new ArrayList<String>();
								
								for (int j = 0; j < commonFields.size(); j++){
									fmVals.add(fTab.getString(commonFields.get(j)));
									msVals.add(mTab.getString(commonFields.get(j)));
								}
								
								// ... because C-AUID is missing
								if (currCAUID == 0) {
									
									// Check all other fields. Is just C-AUID missing?
									switch (compareFields(commonFields, msVals,
											fmVals, currTab)) {
									
									// all fields equal (not null), then just update local UID field and TS
									case 0:
										String fmKeyName = sqlAccess.getFMTablePrimaryKey(currTab.getF());
										String msKeyName = sqlAccess.getMySQLTablePrimaryKey(currTab.getM());
										int fmKeyVal = fTab.getInt(fmKeyName);
										int msKeyVal = mTab.getInt(msKeyName);
										
										if (fmKeyVal == msKeyVal && !updateLocalUID(currTab.getF(), currAAUID, currArachneTS, fmKeyName, fmKeyVal))
											throw new SQLException("local ID/TS could not be updated!");
										
										break;
									
									case -1:
										throw new SQLException("Something went wrong when comparing fields!");
										
									// both sets are empty, delete both!
									case 4:
										//TODO delete both
										System.out.println("delete both!");
										break;
										
									// local fields are all empty and need to be downloaded
									case 2:
										System.out.println("remote entry "
															+ currAAUID
															+ " needs to be downloaded");
										// TODO: download and insert procedure with UID!
										break;
									
									// remote fields are empty, but AAUID != 0!
									// -> check lastModified, deleted or not?
									case 3:
										// entry has been deleted remote
										if (currATS.after(currCTS)){
											//TODO delete locally!
											System.out.println(currAAUID + " locally deleted!");
										// deleted remote, but new local one with same UID
										} else if (currATS.before(currCTS)) {
											//TODO update entry on remote, do not overwrite UID
											System.out.println(currAAUID + " exists but needs to updated on remote!");
										// Conflict!
										// same TS and same UID, but different content!
										} else {
											throw new SyncException("UID and Timestamp same, but content differs!");
										}
										break;
									
									// fields not both empty and not equal
									case 1:
										// timestamp NOT available, otherwise CAUID would also be av.
										// better: search for local entry in arachne, set CAUID to found match! otherwise, upload! TODO 
										System.out.println("CONFLICT! search for local entry in arachne, set CAUID to found match! otherwise, upload! "+currAAUID);
										break;
									case 5:
										fTab.next();
										continue;
									case 6:
										mTab.next();
										continue;
									}
								}
								
								// ... because A-AUID is missing
								else if (currAAUID == 0) {
									
									// Check all other fields. Is just A-AUID missing?
									switch (compareFields(commonFields, msVals,
											fmVals, currTab)) {
									
									// all fields equal (not null), then entity management has not been updated in arachne!!
									case 0:
										throw new EntityException("Content equal and not null, but missing remote ArachneEntitiyID! Just updating? :/");
									
									// both empty, entry seems to be deleted
									case 4:
										//TODO delete local C-AUID
										continue;
										
									// all remote fields are empty AND A-AUID doesn't exist
									case 3:
										//TODO upload into arachne
										System.out
												.println("local entry "
														+ currCAUID
														+ " needs to be uploadedto the server");
										break;
									}
								}
								
								// ... because they differ and are both not 0.
								
								// this is a hard one, because each upload to arachne
								// should create a new AUUID.
								// deleting the entry there should NOT cause the AUUID to be
								// deleted as well (instead: isDeleted = 1).
								// but the local client cannot get a CUUID without doing an
								// update.
								// therefore, local client cannot have a CCUID that is not
								// in arachne. CCUID would be 0 instead.
								else {
									throw new SyncException("weird stuff");
								}
							}
							// C-AUID == A-AUID! examine lastModified
							else {								
								// arachne newer than local?
								if (currATS.after(currCTS)) {
									// download from arachne TODO
									System.out.println(currATS.toString()+" after "+currCTS.toString()+"; download from arachne");
								} else if (currATS.before(currCTS)) {
									// upload to arachne TODO
									System.out.println(currATS.toString()+" before "+currCTS.toString()+"; upload to arachne");
								} else {
									// no changes or nothing to down-/upload, skip.
//									System.out.println("Skipping update, same!");
									continue;
								}
							}
						}
						
						// if fm had more entries than mysql ...
						if (mTab.isAfterLast()) {
							while (fTab.next()) {
								//TODO
							}
						} else {
						// if mysql had more entries than fm ...
							while (mTab.next()) {
								//TODO
							}
						}
					} catch (FMSQLException e) {
						System.err.println("FEHLER: " + e);
						e.printStackTrace();
					}
				}
			}
			System.out.println("done");
		}
		return null;
	}

	private boolean updateLocalUID(String currTab, int currAAUID, LocalDateTime currArachneTS, String pkName, int pkVal) throws SQLException {
		
		// locate row by local ID
		System.out.print("add Arachne UID "+currAAUID+" and TS to local entry with ID "+pkVal+" ...");
		
		if (sqlAccess.doFMUpdate("UPDATE \"" + currTab 
				+ "\" SET ArachneEntityID="+currAAUID
				+", lastModified={ts '"+currArachneTS.format(formatTS)+"'}"
				+ " WHERE "+pkName+"="+pkVal)){
			System.out.print(" done\n");
			return true;
		}
		else{
			System.out.print(" FAILED!\n");
			return false;
		}
	}

	/**
	 * Method to compare field values of a row from mySQL and FileMaker
	 * 
	 * @param commonFields ArrayList of common fields
	 * @param msVals Arraylist of mysql row values
	 * @param fmVals Arraylist of fm row values
	 * @param currentTable current table as Pair object
	 * @return 	0, if all except AUID and lastModified are equal and not all empty.
	 * 			1, if there exists at least one field that is not empty for both, but also not equal.
	 * 			2, if only local fields are empty and entry needs to be downloaded.
	 * 			3, if only remote fields are empty and entry needs to be uploaded.
	 * 			4, if both are empty (deleted?).
	 * 			5, if arachne index is missing and has to be shifted.
	 * 			6, if fm index is missing and has to be shifted.
	 * @throws SQLException
	 */
	private int compareFields(ArrayList<String> commonFields,
			ArrayList<String> msVals, ArrayList<String> fmVals, Pair currentTable)
			throws SQLException {
		
		int col = commonFields.size();
		int res = 1;
		boolean localAllNull = true;	// empty local fields
		boolean remoteAllNull = true;	// empty remote fields
		boolean allSame = true;
		
		for (int l = 0; l < col; l++) { 
			
			String currField = commonFields.get(l);
			
			// skip UID field, this is not equal anyway
			if (currField.equalsIgnoreCase("ArachneEntityID")
					|| currField.equalsIgnoreCase("lastModified"))
				continue;

			// Strings to compare in field with equal name
			String myVal = msVals.get(l);
			String fmVal = fmVals.get(l);

			myVal = myVal == null ? "" : myVal;
			fmVal = fmVal == null ? "" : fmVal;
			
			if (!myVal.isEmpty()) remoteAllNull = false;
			if (!fmVal.isEmpty()) localAllNull = false;

			if (!(myVal.equalsIgnoreCase(fmVal))) {
				
				// index shifting? TODO
				if (isFMPrimaryKey(currentTable.getF(), currField)) {
					
					// if ID is 0, then entry is completely NULL.
					if (myVal.isEmpty())
						return 3;
					if (fmVal.isEmpty())
						return 2;
					
					System.out.println("INDEX SHIFTING!! LocalID: "+fmVal+", RemoteID: "+myVal);
					
					int fID = Integer.parseInt(fmVal);
					int mID = Integer.parseInt(myVal);
					
					// missing entry in arachne
					if (fID < mID)
						return 5;
					else // missing entry in FM
						return 6;
				}
				
				// numeric fields can differ as strings, e.g. 9.0 and 9.00
				if (isNumericalField(currField)) {
					if (!myVal.isEmpty() && !fmVal.isEmpty()) { // parsing empty string raises exc
						try {
							double m = Double.parseDouble(myVal);
							double f = Double.parseDouble(fmVal);
							if (m == f) {
								continue;
							}
						} catch (NumberFormatException e) {
							System.err.println("NFException! "+e.getMessage());
						}
					}
				}
				allSame = false;
				System.out.println(currField + " in " + currentTable
					+ " not equal: \"" + fmVal + "\" (FM) and \"" + myVal + "\" (MS)");
				return 1;
			}
		}
		if (remoteAllNull && localAllNull)
			return 4; // both empty
		
		if (localAllNull)
			res = 2; // local empty, remote needs to be DL'd
		
		if (remoteAllNull)
			res = 3; // remote empty, local needs to be UL'd
		
		if (allSame) // has to checked last!
			return 0; // all same and not empty
		
		return res;
	}

	private boolean isFMPrimaryKey(String table, String field) {
		return sqlAccess.getFMTablePrimaryKey(table).equals(field);
	}
	
	/**
	 * get common fields from fm and ms tables
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

		// columns start at 1
		for (int i = 1; i <= metaFMTab.getColumnCount(); i++) {
			for (int j = 1; j <= metaMSTab.getColumnCount(); j++) {
				if (metaFMTab.getColumnName(i)
						.equalsIgnoreCase(metaMSTab.getColumnName(j))) {
					result.add(metaFMTab.getColumnName(i));
				}
			}
		}
//		// DEBUG: get all fields just in FM
//		for (int i = 1; i <= metaFMTab.getColumnCount(); i++) {
//			if (!result.contains(metaFMTab.getColumnName(i))
//					&& !metaFMTab.getColumnName(i).startsWith("["))
//				fs.add(metaFMTab.getColumnName(i));
//		}
//		if (!fs.isEmpty())
//			System.out.println(fs);
//		// DEBUG end
		System.out.println(result);
		return result;
	}
	
	/**
	 * get common fields from fm and ms tables
	 * 
	 * @param currTab Pair of table to be processed
	 * @return common field names as Pairs in ArrayList
	 * @throws SQLException
	 */
	private ArrayList<String> getCommonFields(Pair currTab)
			throws SQLException {
		
		ArrayList<String> result = new ArrayList<String>();
		ResultSet fmColumns = sqlAccess.getFMColumnMetaData(currTab.getF());
		ResultSet msColumns = sqlAccess.getMySQLColumnMetaData(currTab.getM());

		while (fmColumns.next()) {
			msColumns.beforeFirst();
			while (msColumns.next()) {
				if (fmColumns.getString("COLUMN_NAME")
						.equalsIgnoreCase(msColumns.getString("COLUMN_NAME"))) {
					result.add(fmColumns.getString("COLUMN_NAME"));
				}
			}
		}
		return result;
	}

	private String getBerlinTimeStamp() {
		return LocalDateTime.now(zoneBerlin).format(formatTS); 
	}

	private boolean isNumericalField(String field) {
		return ConfigController.getInstance().getNumericFields()
				.contains(field);
	}

	public static void main(String[] args) {
		try {
			SQLDataModel m = new SQLDataModel();
			commonTables = m.getCommonTables();
			m.getDiffByUUID("");
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
}
