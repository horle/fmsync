package ceramalex.sync.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

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
	
	private static Logger logger = Logger.getLogger(SQLDataModel.class);

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
		sqlAccess.fetchTimestampFields(result);
		return result;
	}
	
	private boolean addLastModifiedField(String table) throws SQLException, FilemakerIsCrapException {
		try {
			ResultSet fmColumns = sqlAccess.getFMColumnMetaData(table);

			while (fmColumns.next()) {{
					if (fmColumns.getString("COLUMN_NAME")
							.equalsIgnoreCase("lastModified")) {
						return true;
					}
				}
			}
				
			if (sqlAccess.doFMAlter("ALTER TABLE "+table+" ADD lastModified TIMESTAMP") != null) {
				throw new FilemakerIsCrapException(
						"Column lastModified has been added manually into table \""
								+ table + "\", but has still to be updated in FileMaker with the following script:");//TODO
			}
			else
				throw new FilemakerIsCrapException(
						"Column lastModified has to be added manually into table \""
								+ table + "\"");
			
		} catch (FMSQLException e) {
			if (e.getMessage().contains("Duplicate name")) return true;
			else throw e;
		}
	}
	
	private boolean addAUIDField(String table) throws SQLException, FilemakerIsCrapException {
		try {
			ResultSet fmColumns = sqlAccess.getFMColumnMetaData(table);

			while (fmColumns.next()) {{
					if (fmColumns.getString("COLUMN_NAME")
							.equalsIgnoreCase("ArachneEntityID")) {
						return true;
					}
				}
			}
			
			if (sqlAccess.doFMAlter("ALTER TABLE "+table+" ADD ArachneEntityID NUMERIC") != null) {
				return true;
			}
			else
				throw new FilemakerIsCrapException(
						"Column ArachneEntityID has to be added manually into table \""
								+ table + "\"");
			
		} catch (FMSQLException e) {
			if (e.getMessage().contains("Duplicate name")) return true;
			else throw e;
		}
	}

	public ComparisonResult getDiffByUUID(Pair currTab) throws SQLException,
			FilemakerIsCrapException, SyncException, EntityException {
		
		ComparisonResult result = new ComparisonResult();
		
		// both connected?
		if (sqlAccess.isMySQLConnected() && sqlAccess.isFMConnected()) {

			// calculate common tables!
			if (commonTables == null)
				commonTables = getCommonTables();

			ArrayList<String> commonFields = new ArrayList<String>();
			
			if (commonFields.isEmpty())
				commonFields = getCommonFields(currTab);
			
			// test, if fields are available in FM				
			if (!commonFields.contains("ArachneEntityID")){
				addAUIDField(currTab.getLeft());
				commonFields.add("ArachneEntityID");
			}
				
			if (!commonFields.contains("lastModified")){
				addLastModifiedField(currTab.getLeft());
				commonFields.add("lastModified");
			}

			String fSQL = "";
			for (int j = 0; j < commonFields.size(); j++){
				if (j == commonFields.size()-1)
					fSQL += commonFields.get(j);
				else
					fSQL += commonFields.get(j)+",";
			}
			// Skipping Martin Archer's entries => excel
			String archerSkip = "",archerFMSkip = "";
			if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
				archerSkip = " AND " + currTab.getRight() + ".ImportSource!='Comprehensive Table'";
				archerFMSkip = " WHERE " + currTab.getLeft() + ".ImportSource!='Comprehensive Table'";
			}
			
			// get only common fields from filemaker
			ResultSet fTab = sqlAccess.doFMQuery("SELECT "+fSQL+" FROM "
					+ currTab.getLeft() + archerFMSkip);

			// A-AUID + rest of table
			ResultSet mTab = sqlAccess
					.doMySQLQuery("SELECT arachneentityidentification.ArachneEntityID, "
							+ currTab.getRight()
							+ ".* FROM arachneentityidentification LEFT JOIN " // left join includes deleted or empty UIDs
							+ currTab.getRight()
							+ " ON arachneentityidentification.ForeignKey = "
							+ currTab.getRight()
							+ "."
							+ sqlAccess.getMySQLTablePrimaryKey(currTab
									.getRight())
							+ " WHERE arachneentityidentification.TableName=\""
							+ currTab.getRight() + "\"" +  archerSkip);
				
			// entities:
			int currAAUID = 0; // current arachne uid in arachne
			int currCAUID = 0; // current arachne uid in fm
			Timestamp currATS = null; // current arachne timestamp
			Timestamp currCTS = null; // current fm timestamp
//					String currLocalTS = getBerlinTimeStamp(); // current timestamp in CET format (arachne is in germany ...)
			
			try {
				while (fTab.next() & mTab.next()) {
					
					currAAUID = mTab.getInt("ArachneEntityID");
					currCAUID = fTab.getInt("ArachneEntityID");
					currATS = mTab.getTimestamp("lastModified");
					currCTS = fTab.getTimestamp("lastModified");
					
					// missing entry in regular table, but entry in entity management! bug in db!
					// TODO: delete entry in arachne entity management
					if (currATS == null) {
						throw new EntityException("Missing entry in LOCAL Arachne entity management! Table "+currTab.getLeft()+", remote entry: "+currAAUID);
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
								String fmKeyName = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
								String msKeyName = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
								int fmKeyVal = fTab.getInt(fmKeyName);
								int msKeyVal = mTab.getInt(msKeyName);
								
								if (fmKeyVal == msKeyVal && !updateLocalUID(currTab, currAAUID, currArachneTS, fmKeyName, fmKeyVal))
									throw new SQLException("local ID/TS could not be updated!");
								
								break;
							
							case -1:
								throw new SQLException("Something went wrong when comparing fields!");
								
							// both sets are empty, delete both!
							case 4:
								result.addToDelete(currCAUID, currAAUID);
								break;
								
							// local fields are all empty and need to be downloaded
							case 2:
								result.addAAUIDToDownload(currAAUID);
								break;
							
							// remote fields are empty, but AAUID != 0!
							// -> check lastModified, deleted or not?
							case 3:
								// entry has been deleted on remote, delete also locally
								if (currATS.after(currCTS)){
									result.addToDelete(currCAUID, 0);
								// deleted remote, but new local one with same UID
								} else if (currATS.before(currCTS)) {
									// update entry on remote, do not overwrite UID
									ArrayList<Pair> row = new ArrayList<Pair>();
									for (int l = 0; l < commonFields.size(); l++) {
										row.add(new Pair(commonFields.get(l),fmVals.get(l)));
									}
									result.addToUpdate(row, true);
								// Conflict!
								// same TS and same UID, but different content!
								} else {
									throw new SyncException("UID and Timestamp same, but content differs!");
								}
								break;
							
							// fields both not empty and not equal
							case 1:
								// timestamp NOT available, otherwise CAUID would also be av.
								// better: search for local entry in arachne, set CAUID to found match! otherwise, upload! TODO 
								throw new SyncException("CONFLICT! search for local entry in arachne, set CAUID to found match! otherwise, upload! "+currAAUID);
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
							
							// both empty, entry seems to be deleted. delete locally
							case 4:
								result.addToDelete(currCAUID, 0);
								continue;
								
							// all remote fields are empty AND A-AUID doesn't exist. upload to arachne
							case 3:
								ArrayList<Pair> row = new ArrayList<Pair>();
								for (int l = 0; l < commonFields.size(); l++) {
									row.add(new Pair(commonFields.get(l),fmVals.get(l)));
								}
								result.addRowToUpload(row);
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
						// EDIT: possible is a change of the remote AUID.
						else {
							throw new SyncException("weird stuff");
						}
					}
					// C-AUID == A-AUID! examine lastModified
					else {								
						// arachne newer than local? download from arachne.
						if (currATS.after(currCTS)) {
							result.addAAUIDToDownload(currAAUID);
						// local db newer than arachne? upload to arachne.
						} else if (currATS.before(currCTS)) {
							ArrayList<Pair> row = new ArrayList<Pair>();
							for (int l = 0; l < commonFields.size(); l++) {
								row.add(new Pair(commonFields.get(l),fTab.getString(commonFields.get(l))));
							}
							result.addRowToUpload(row);
						} else {
							// no changes or nothing to down-/upload, skip.
							continue;
						}
					}
				}
				
				// if fm has more entries than mysql ... //TODO, noch nicht schön
				if (mTab.isAfterLast() || !mTab.next()) {
					fTab.previous();
					String pk = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
					int first=0, last=0;
					ArrayList<ArrayList<Pair>> inserts = new ArrayList<ArrayList<Pair>>();
					while (fTab.next()) {
						ArrayList<Pair> ins = new ArrayList<Pair>();
						ArrayList<String> names = getCommonFields(currTab);
						if (first == 0) first = fTab.getInt(pk);
						last = fTab.getInt(pk);
						
						for (int k = 0; k < names.size(); k++) {
							Pair p = new Pair(names.get(k), fTab.getString(names.get(k)));
							ins.add(p);
						}
						result.addRowToUpload(ins);
						inserts.add(ins);
					}
//					System.out.println("insert entries "+first+"-"+last+" into arachne ... ");
					// insert packs of size 25
					prepareRowsAndInsert(currTab, inserts, 25);
					
				} else {
				// if mysql has more entries than fm ...
					mTab.previous(); // neccessary because of !mTab.next() in if 
					while (mTab.next()) {
						result.addAAUIDToDownload(currAAUID);
					}
				}
			} catch (FMSQLException e) {
				System.err.println("FEHLER: " + e);
				e.printStackTrace();
			}
		}
		System.out.println("done");
		return result;
	}

	/**
	 * prepare row packs of size 25 to upload into arachne
	 * @throws SQLException 
	 * @throws EntityException 
	 *  
	 */
	public ArrayList<Integer> prepareRowsAndInsert(Pair currTab, ArrayList<ArrayList<Pair>> rows, int packSize) throws SQLException, EntityException {
		int count = rows.size()/25;
		int mod = rows.size() % 25;
		ArrayList<Integer> resultIDs = new ArrayList<Integer>();
		for (int k = 0; k < count; k++) {
			resultIDs.addAll(
					uploadRowsToArachne(
							currTab, 
							new ArrayList<ArrayList<Pair>>(
									rows.subList(k*25, (k+1)*25))));
		}
		resultIDs.addAll(
				uploadRowsToArachne(
						currTab, 
						new ArrayList<ArrayList<Pair>>(
								rows.subList(count*25, count*25+mod))));
		return resultIDs;
	}
	
	/**
	 * Insert given rows
	 * @param currTab
	 * @param rows
	 * @return array of inserted IDs. null, if given row list is empty
	 * @throws SQLException
	 * @throws EntityException 
	 */
	private ArrayList<Integer> uploadRowsToArachne(Pair currTab, ArrayList<ArrayList<Pair>> rows) throws SQLException, EntityException{
		
		if (rows.size() == 0) return null;
		
		String sql = "INSERT INTO "+currTab.getRight()+ " (";
		String vals = " VALUES (";
		
		boolean isLastOut = false; // prevent comma before skipped field
		for (int i = 0; i <= rows.size(); i++) {
			
			ArrayList<Pair> currRow = i != rows.size() ? rows.get(i) : null;
			
			// ( col1, col2, col3, ..)
			if (i == 0) {
				for (int j = 0; j < currRow.size(); j++) {
					String currFieldName = currRow.get(j).getLeft();
					if (isFMPrimaryKey(currTab.getLeft(), currFieldName) || currFieldName.equals("ArachneEntityID")){
						isLastOut = true;
						continue;
					}
					if (j > 0 && j < currRow.size() && !isLastOut) {
						sql += ",";
					} else if (isLastOut) isLastOut = false;
					sql += "`"+currFieldName+"`";
				}
				sql += ")";
			}
			
			// final paranthese
			if (i == rows.size()) {
				vals += ");";
				break;
			}
			
			for (int j = 0; j < currRow.size(); j++) {
				String currFieldName = currRow.get(j).getLeft();
				if (isFMPrimaryKey(currTab.getLeft(), currFieldName) || currFieldName.equals("ArachneEntityID")) {
					isLastOut = true;
					continue;
				}
				// set comma before next field entry ...
				if (j > 0 && j < currRow.size() && !isLastOut) {
					vals += ",";
				} else if (isLastOut) isLastOut = false;
				
				String currFieldVal = currRow.get(j).getRight();
				
				if (!isNumericalField(currFieldName) && !isTimestampField(currFieldName) && currFieldVal != null) {
					currFieldVal = "'" + currFieldVal.replace("'", "\\'").replace("\"", "\\\"").replace("%", "\\%") + "'";
				}
				else if (isTimestampField(currFieldName) && currFieldVal != null) {
					currFieldVal = "TIMESTAMP '" + currFieldVal + "'";
				}
				
				vals += currFieldVal;
			}
			if (i < rows.size()-1)
				vals += "),(";
			
		}
		ArrayList<Integer> localIDs = sqlAccess.doMySQLInsert(sql + vals);
		ArrayList<Integer> result = new ArrayList<Integer>();
		String key = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
		
		for (int i = 0; i < localIDs.size(); i++) {
			ResultSet id = sqlAccess.doMySQLQuery("select ArachneEntityID,"+key+".lastModified"
					+ " from arachneentityidentification right join "+currTab.getRight()
					+ " on arachneentityidentification.ForeignKey="+currTab.getRight()+"."+key
					+ " AND arachneentityidentification.TableName='"+currTab.getRight()+"'"
					+ " WHERE ForeignKey="+localIDs.get(i)+";");
			if (id.next()) {
				int aauid = id.getInt(1);
				if (!updateLocalUID(currTab, aauid, id.getTimestamp(2).toLocalDateTime(), key, localIDs.get(i)))
					throw new EntityException("Updating local UID "+ id.getInt(1)+" in table "+currTab.getLeft()+" FAILED!");
				else
					result.add(aauid);
			}
		}
		
		return result;
	}
	
	private boolean updateLocalUID(Pair currTab, int currAAUID, LocalDateTime currArachneTS, String pkName, int pkVal) throws SQLException {
		
		// locate row by local ID
		if (sqlAccess.doFMUpdate("UPDATE \"" + currTab.getLeft() 
				+ "\" SET ArachneEntityID="+currAAUID
				+", lastModified={ts '"+currArachneTS.format(formatTS)+"'}"
				+ " WHERE "+pkName+"="+pkVal) != null){
			logger.debug("Added Arachne UID "+currAAUID+" and TS to local entry with ID "+pkVal+".");
			return true;
		}
		else{
			logger.error("Adding Arachne UID "+currAAUID+" and TS to local entry with ID "+pkVal+" FAILED.");
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
				
				if (isFMPrimaryKey(currentTable.getLeft(), currField)) {
					
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

	/**
	 * Checks if field name is PK for given FM table.
	 * @param table filemaker table name
	 * @param field filemaker field name to check
	 * @return true, if PK. false else.
	 */
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
		ResultSet fmColumns = sqlAccess.getFMColumnMetaData(currTab.getLeft());
		ResultSet msColumns = sqlAccess.getMySQLColumnMetaData(currTab.getRight());

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
	
	private boolean isTimestampField(String field) {
		return ConfigController.getInstance().getTimestampFields()
				.contains(field);
	}

	public static void main(String[] args) {
		try {
			DOMConfigurator.configureAndWatch("log4j.xml");
			ConfigController.getInstance().setPrefs("jdbc:mysql://134.95.115.21:3306", "root", "",
					"ceramalex", "jdbc:filemaker://localhost", "admin", "btbw", "iDAIAbstractCeramalex", "3306");
			SQLDataModel m = new SQLDataModel();
			commonTables = m.getCommonTables();
			
			for (int i = 0; i < commonTables.size(); i++){
				try {
					System.out.print("Processing table "+commonTables.get(i)+ " ... ");
					m.getDiffByUUID(commonTables.get(i));
					logger.info("Processed table "+commonTables.get(i) + " without errors.");
				} catch (Exception e) {
					logger.error(commonTables.get(i) + ": "+ e.getMessage());
				}
			}
			
		} catch (Exception e) {
			System.err.println("ERROR: "+e.getMessage());
			logger.error(e.getMessage());
		}
	}
	enum FieldComparison {
		//TODO
	}
}
