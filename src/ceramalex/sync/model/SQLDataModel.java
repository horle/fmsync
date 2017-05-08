package ceramalex.sync.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.EntityManagementException;
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
	private static SQLDataModel instance = null;

	private SQLAccessController sqlAccess;
	private DateTimeFormatter formatTS;
	private ZoneId zoneBerlin;
	private ZoneId zoneUTC;
	private ArrayList<Pair> commonTables;
	private ArrayList<ComparisonResult> results;
	private ConfigController conf;

	public ArrayList<ComparisonResult> getResults() {
		return results;
	}

	public void addResult(ComparisonResult result) {
		this.results.add(result);
	}

	private static Logger logger = Logger.getLogger(SQLDataModel.class);

	public static SQLDataModel getInstance() throws IOException, SQLException {
		if (instance == null) {
			instance = new SQLDataModel();
		}
		return instance;
	}

	private SQLDataModel() throws IOException, SQLException {
		formatTS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		zoneBerlin = ZoneId.of("Europe/Berlin");
		zoneUTC = ZoneId.of("UTC");
		sqlAccess = SQLAccessController.getInstance();
		conf = ConfigController.getInstance();
		results = new ArrayList<ComparisonResult>();
	}

	public ArrayList<Pair> fetchCommonTables() throws SQLException {
		commonTables = new ArrayList<Pair>();

		ArrayList<String> fmNames = new ArrayList<String>();
		ArrayList<String> msNames = new ArrayList<String>();

		if (!sqlAccess.isFMConnected() || !sqlAccess.isMySQLConnected()) {
			sqlAccess.connect();
		}

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
//						sqlAccess.doFMUpdate("UPDATE "+fmNames.get(j) + " SET ArachneEntityID=null,lastModified=null,lastRemoteTS=null");
						commonTables.add(t);
					}
					// IsolatedSherdMainAbstract maps to isolatedsherd
					if (fmNames.get(j).equals("IsolatedSherdMainAbstract")
							&& msNames.get(i).equals("isolatedsherd")) {
						Pair t = new Pair(fmNames.get(j), msNames.get(i));
						commonTables.add(t);
					}
				}
			}
			sqlAccess.fetchNumericFields(commonTables);
			sqlAccess.fetchTimestampFields(commonTables);
		}
		return commonTables;
	}

	public ArrayList<Pair> getCommonTables() {
		return commonTables;
	}

	private boolean addLastModifiedField(String table) throws SQLException,
			FilemakerIsCrapException, IOException {
		try {
			ResultSet fmColumns = sqlAccess.getFMColumnMetaData(table);

			while (fmColumns.next()) {
				{
					if (fmColumns.getString("COLUMN_NAME").equalsIgnoreCase(
							"lastModified")) {
						return true;
					}
				}
			}

			if (sqlAccess.doFMAlter("ALTER TABLE " + table
					+ " ADD lastModified TIMESTAMP")) {
				throw new FilemakerIsCrapException(
						"Column lastModified has been added manually into table \""
								+ table
								+ "\", but has still to be updated in FileMaker with the following script:");// TODO
			} else
				throw new FilemakerIsCrapException(
						"Column lastModified has to be added manually into table \""
								+ table + "\"");

		} catch (FMSQLException e) {
			if (e.toString().contains("Duplicate name"))
				return true;
			else
				throw e;
		}
	}

	private boolean addLastRemoteTSField(String table) throws SQLException,
			FilemakerIsCrapException, IOException {
		try {
			ResultSet fmColumns = sqlAccess.getFMColumnMetaData(table);

			while (fmColumns.next()) {
				{
					if (fmColumns.getString("COLUMN_NAME").equalsIgnoreCase(
							"lastRemoteTS")) {
						return true;
					}
				}
			}

			if (sqlAccess.doFMAlter("ALTER TABLE " + table
					+ " ADD lastRemoteTS TIMESTAMP")) {
				return true;
			} else
				throw new FilemakerIsCrapException(
						"Column lastRemoteTS has to be added manually into table \""
								+ table + "\"");

		} catch (FMSQLException e) {
			if (e.toString().contains("Duplicate name"))
				return true;
			else
				throw e;
		}
	}

	private boolean addAUIDField(String table) throws SQLException,
			FilemakerIsCrapException, IOException {
		try {
			ResultSet fmColumns = sqlAccess.getFMColumnMetaData(table);

			while (fmColumns.next()) {
				{
					if (fmColumns.getString("COLUMN_NAME").equalsIgnoreCase(
							"ArachneEntityID")) {
						return true;
					}
				}
			}

			if (sqlAccess.doFMAlter("ALTER TABLE " + table
					+ " ADD ArachneEntityID NUMERIC")) {
				return true;
			} else
				throw new FilemakerIsCrapException(
						"Column ArachneEntityID has to be added manually into table \""
								+ table + "\"");

		} catch (FMSQLException e) {
			if (e.toString().contains("Duplicate name"))
				return true;
			else
				throw e;
		}
	}

	public boolean resetResults() {
		results.clear();
		return true;
	}
	
	public ComparisonResult calcDiff (Pair currTab, boolean upload,
			boolean download) throws SQLException, FilemakerIsCrapException,
			SyncException, EntityManagementException, IOException {

		for (ComparisonResult c : results) {
			if (c.getTableName().equals(currTab)) {
				results.remove(c);
				break;
			}
		}
		ComparisonResult result = new ComparisonResult(currTab);

		// both connected?
		if (sqlAccess.isMySQLConnected() && sqlAccess.isFMConnected()) {
			
			ResultSet fmColumnMeta = sqlAccess.getFMColumnMetaData(currTab.getLeft());
			ResultSet msColumnMeta = sqlAccess.getMySQLColumnMetaData(currTab.getRight());
			result.setFmColumns(fmColumnMeta);
			result.setMsColumns(msColumnMeta);
			
			ArrayList<Integer> handledUIDs = new ArrayList<Integer>();
			TreeSet<String> commonFields = getCommonFields(currTab, fmColumnMeta, msColumnMeta);

			// test, if fields are available in FM
			addAUIDField(currTab.getLeft());
			addLastModifiedField(currTab.getLeft());
			addLastRemoteTSField(currTab.getLeft());
			commonFields.add("ArachneEntityID");
			commonFields.add("lastModified");
			result.setCommonFields(commonFields);

			String sqlCommonFields = "";
			String sqlCommonFieldsFM = "";
			
			Iterator<String> it = commonFields.iterator();
			while (it.hasNext()) {
				String next = it.next();
				if (!it.hasNext()) {
					if (!next.equals("ArachneEntityID") && !next.equals("lastModified"))
						sqlCommonFields += currTab.getRight()+".";
					sqlCommonFields += next;
					sqlCommonFieldsFM += currTab.getLeft()+"."+next;
				}
				else{
					if (!next.equals("ArachneEntityID") && !next.equals("lastModified"))
						sqlCommonFields += currTab.getRight()+".";
					sqlCommonFields += next + ",";
					sqlCommonFieldsFM += currTab.getLeft()+"."+next + ",";
				}
			}
			// Skipping Martin Archer's entries => excel
			String archerMSSkip = "", archerFMSkip = "";
			if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
				archerMSSkip = " AND " + currTab.getRight()
						+ ".ImportSource!='Comprehensive Table'";
				archerFMSkip = " WHERE " + currTab.getLeft()
						+ ".ImportSource!='Comprehensive Table'";
			}

			String fmSQL = "SELECT " + sqlCommonFieldsFM + ", "+currTab.getLeft()+".lastRemoteTS FROM " + currTab.getLeft()
					+ archerFMSkip;
			String msSQL = "SELECT arachneentityidentification.ArachneEntityID, arachneentityidentification.isDeleted, "
					+ sqlCommonFields.replace("lastModified", "CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00') as lastModified")
					+ " FROM arachneentityidentification"
					// left join includes deleted or empty UIDs
					+ " LEFT JOIN " + currTab.getRight()
					+ " ON arachneentityidentification.ForeignKey = "
					+ currTab.getRight() + "." + sqlAccess.getMySQLTablePrimaryKey(currTab.getRight())
					+ " WHERE arachneentityidentification.TableName=\""
					+ currTab.getRight() + "\"" + archerMSSkip;

			// get only common fields from filemaker
			ResultSet fNotNull = sqlAccess.doFMQuery(fmSQL
					+ (archerFMSkip == "" ? " WHERE " : " AND ")
					+ "ArachneEntityID IS NOT NULL");
			// A-AUID + rest of table
			ResultSet mNotNull = sqlAccess.doMySQLQuery(msSQL
					+ " AND ArachneEntityID IS NOT NULL");
			
			try {
				while (fNotNull.next() & mNotNull.next()) {
					// entities:
					int currRAUID = mNotNull.getInt("ArachneEntityID"); // current arachne uid in arachne
					int currLAUID = fNotNull.getInt("ArachneEntityID"); // current arachne uid in fm
					Timestamp currATS = mNotNull.getTimestamp("lastModified"); // current arachne timestamp
					Timestamp currCTS = fNotNull.getTimestamp("lastModified"); // current fm timestamp
					Timestamp currLRTS = fNotNull.getTimestamp("lastRemoteTS"); // last remote timestamp saved in fm
					boolean isDeletedRemotely = mNotNull.getInt("isDeleted") == 1 ? true : false;

					if (isDeletedRemotely) {
						result.addToDeleteList(currLAUID, currRAUID);
						continue;
					}
					
					if (currLRTS == null) currLRTS = currCTS;
					
					// missing entry in regular table, but entry in entity
					// management! bug in db!
					// TODO: delete entry in arachne entity management
					if (currATS == null) {
						throw new EntityManagementException(
								"Missing entry in LOCAL Arachne entity management! Table "
										+ currTab.getLeft()
										+ ", remote entry: " + currRAUID);
					}
					if (currRAUID == 0 || currLAUID == 0) {
						throw new EntityManagementException(
								"Something went wrong: NULL value in NOT NULL query");
					}

					if (currRAUID == currLAUID) {
						// timestamps all equal: SKIP
						if (currATS.equals(currCTS) && currCTS.equals(currLRTS)) {
							continue;
						}
						// local after remote, remote did not change: UPDATE REMOTELY
						if (currCTS.after(currATS) && currATS.equals(currLRTS)) {
							TreeMap<String, String> row = new TreeMap<String,String>();
							TreeMap<String, String> diffs = new TreeMap<String,String>();
							for (String field : commonFields) {
								String rVal = mNotNull.getString(field);
								String lVal = fNotNull.getString(field);
								row.put(field, rVal);
								if (!rVal.equals(lVal)) {
									diffs.put(field, lVal);
								}
							}
							result.addToUpdateList(row, diffs, false);
							continue;
						}
						// remote after local AND local <= last remote <= remote: UPDATE LOCALLY
						if (currCTS.before(currATS)
								&& (currCTS.before(currLRTS) && currLRTS.before(currATS))) {
							TreeMap<String, String> row = new TreeMap<String,String>();
							TreeMap<String, String> diffs = new TreeMap<String,String>();
							for (String field : commonFields) {
								String rVal = mNotNull.getString(field);
								String lVal = fNotNull.getString(field);
								row.put(field, lVal);
								if (!rVal.equals(lVal)) {
									diffs.put(field, rVal);
								}
							}
							result.addToUpdateList(row, diffs, true);
							continue;
						}
						// local after last remote AND remote after last remote:
						// CONFLICT
						if (currCTS.after(currLRTS) && currLRTS.before(currATS)) {
							TreeMap<String,String> rowFM = new TreeMap<String,String>();
							TreeMap<String,String> rowMS = new TreeMap<String,String>();
							for (String field : commonFields) {
								rowFM.put(field, fNotNull.getString(field));
								rowMS.put(field, mNotNull.getString(field));
							}
							result.addToConflictList(rowFM, rowMS);
							continue;
						}
					}
					// only cases: not updated local AUID or bug.
					// anyway: the single side has to be handled somehow!
					else if (currLAUID > currRAUID) {
						TreeMap<String,String> rowMS = new TreeMap<String,String>();
						Integer RAUID = 0; String lmRemote = null;
						
						String pk = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
						Pair lookup = new Pair(pk, mNotNull.getString(pk));
						
						for (String field : commonFields) {
							String val = mNotNull.getString(field);
							
							if (field.equals("ArachneEntityID")) RAUID = mNotNull.getInt(field);
							if (field.equals("lastModified")) lmRemote = val;
							
							rowMS.put(field, val);
						}
						TreeMap<String,String> local = isRowOnLocal(currTab, lookup, commonFields);
						// entry already in local db, just missing LAUID? check TS first
						if (!local.isEmpty()) {
							String lmLocal = local.get("lastModified");
							String lrts = local.get("lastRemoteTS");
							// timestamp differs?!
							if (lmLocal != null && !lmLocal.isEmpty() ) {
								Timestamp lmL = Timestamp.valueOf(lmLocal);
								
								if (lrts != null && !lrts.isEmpty() ) {
									Timestamp lrTS = Timestamp.valueOf(lrts);
									// local after last remote AND remote after last remote => CONFLICT?!
									if (lmL.after(lrTS) && currATS.after(lrTS)) {
										// check for changes!
										local.remove("lastRemoteTS");
										// conflict avoidance ...
										if (compareFields(commonFields, local, rowMS, currTab) != 0) {
											// no diffs? ignore. diffs? conflict!
											result.addToConflictList(local, rowMS);
											fNotNull.previous(); // INDEX SHIFT, trying local index again!
											continue;
										}
									}
								}
								else if (!lmL.equals(currATS)) {
									// check for changes!
									local.remove("lastRemoteTS");
									// conflict avoidance ...
									if (compareFields(commonFields, local, rowMS, currTab) != 0) {
										// no diffs? ignore. diffs? conflict!
										result.addToConflictList(local, rowMS);
										fNotNull.previous(); // INDEX SHIFT, trying local index again!
										continue;
									}
								}
							}
							// update locally, overwrite TS. is equal anyway because of query ...
							TreeMap<String,String> set = new TreeMap<String,String>();
							// no RAUID, but in remote DB?!
							if (RAUID == null || RAUID.equals(""))
								throw new EntityManagementException(
										"An entry is on remote DB but has no AUID!");
							
							// fix to prevent double handling entries
							if (handledUIDs.contains(RAUID)) continue;
							
							// setting LAUID = RAUID via local update list
							set.put("ArachneEntityID", RAUID+"");
							rowMS.remove("ArachneEntityID"); // remove to avoid wrong "where"
							set.put("lastModified", lmRemote); // important due to FM change listener on this field
							set.put("lastRemoteTS", lmRemote);
							result.addToUpdateList(rowMS, set, true);
							handledUIDs.add(RAUID);
						}
						// nothing in local db. maybe deleted?
						// local deletions cannot be recognized. GUI should ask in each case.
						// idea: handle locally deleted entries like a conflict!
						else {
							local.remove("lastRemoteTS");
							result.addToConflictList(local, rowMS);
						}
						fNotNull.previous(); // INDEX SHIFT, trying local index again!
					}	
					// e.g. 1 in local and 4 in remote. this case only occurs if entry does not exist
					// in remote db (impossible due to arachne entity management)
					else if (currRAUID > currLAUID) {
						throw new EntityManagementException("The entry with AUID "+ currLAUID + " seems to not exist on remote side, but it should!");
					}
				}
				fNotNull.previous(); // neccessary because of previous next()
				while (fNotNull.next()) {
					throw new EntityManagementException("The entry with AUID "+ fNotNull.getString("ArachneEntityID") + " seems to not exist in Arachne, but it should!");
				}
				
				ResultSet fNull = null;
				if (!mNotNull.isAfterLast())
					mNotNull.previous(); // neccessary because of previous next()
				if (mNotNull.next()) {
					mNotNull.previous();
					fNull = sqlAccess.doFMQuery(fmSQL
							+ (archerFMSkip == "" ? " WHERE " : " AND ")
							+ "ArachneEntityID IS NULL");
					
					while (mNotNull.next() & fNull.next()) {
						TreeMap<String,String> rowLocal = new TreeMap<String,String>();
						TreeMap<String,String> rowRemote = new TreeMap<String,String>();
						for (String field : commonFields) {
							rowLocal.put(field, fNull.getString(field));
							rowRemote.put(field, mNotNull.getString(field));
						}
						
						switch (compareFields(commonFields, rowLocal, rowRemote, currTab)) {
						// fields all equal? update local UID to remote UID
						case 0:
							TreeMap<String,String> set = new TreeMap<String,String>();
							// get RAUID
							Integer RAUID = mNotNull.getInt("ArachneEntityID");
							String lastModified = mNotNull.getString("lastModified");
							// no RAUID, but in remote DB?!
							if (RAUID == null || RAUID == 0)
								throw new EntityManagementException(
										"An entry is on remote DB but has no AUID!");
							
							// fix to prevent double handling entries
							if (handledUIDs.contains(RAUID)) continue;
							
							// setting LAUID = RAUID via local update list
							set.put("ArachneEntityID", RAUID+"");
							// content is equal to remote. so overwrite local TS with remote
							set.put("lastModified", lastModified);
							set.put("lastRemoteTS", lastModified);
							result.addToUpdateList(rowLocal, set, true);
							handledUIDs.add(RAUID);
							break;
						// next remote index ahead of local? shift local and upload.
						case 5:
							fNull.next();
							result.addToUploadList(rowLocal);
							break;
						// next local index ahead of remote? download.
						case 6:
							mNotNull.next();
							result.addToDownloadList(rowRemote);
							break;
						default:
							throw new EntityManagementException("irgendein fehler");
						}
					}
				}
				
				if (fNull == null) 
					fNull = sqlAccess.doFMQuery(fmSQL
							+ (archerFMSkip == "" ? " WHERE " : " AND ")
							+ "ArachneEntityID IS NULL");
				
				// LAUID == null
				if (!fNull.isAfterLast()) // if there are still rows, the last next() was unnecessary
					fNull.previous();
				while (fNull.next()) {
					TreeMap<String,String> rowFM = new TreeMap<String,String>();
					Integer LAUID = fNull.getInt("ArachneEntityID");
					String lmLocal = fNull.getString("lastModified");
					String pk = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
					Pair lookup = new Pair(pk, fNull.getString(pk));
					
					// add local row content to map, lookup remotely
					for (String field : commonFields) {
						String val = fNull.getString(field);
						
						rowFM.put(field, val);
					}
					TreeMap<String,String> remote = remoteLookupByPK(currTab, lookup, commonFields);
					// entry already in local db, just missing LAUID? check TS first
					if (!remote.isEmpty()) {
						String lmRemote = remote.get("lastModified");
						String lrts = fNull.getString("lastRemoteTS");
						Integer RAUID = Integer.parseInt(remote.get("ArachneEntityID"));
						
						// fix to prevent double handling entries
						if (handledUIDs.contains(RAUID)) continue;
						
						// timestamp differs?!
						if (lmLocal != null && !lmLocal.isEmpty() 
								&& lmRemote != null && !lmRemote.isEmpty() ) {
							Timestamp lmL = Timestamp.valueOf(lmLocal);
							Timestamp currATS = Timestamp.valueOf(lmRemote);
							
							if (lrts != null && !lrts.isEmpty() ) {
								Timestamp lrTS = Timestamp.valueOf(lrts);
								// local after last remote AND remote after last remote => CONFLICT?!
								if (lmL.after(lrTS) && currATS.after(lrTS)) {
									// check for changes!
									remote.remove("lastRemoteTS");
									// conflict avoidance ...
									if (compareFields(commonFields, rowFM, remote, currTab) != 0) {
										// no diffs? ignore. diffs? conflict!
										result.addToConflictList(rowFM, remote);
										continue;
									}
								}
							}
							else if (!lmL.equals(currATS)) {
								// check for changes! onflict avoidance ...
								if (compareFields(commonFields, rowFM, remote, currTab) != 0) {
									// no diffs? ignore. diffs? conflict!
									result.addToConflictList(rowFM, remote);
									continue;
								}
							}
						}
						// no local timestamp found, compare fields ...
						else if (compareFields(commonFields, rowFM, remote, currTab) != 0) {
							// no diffs? ignore. diffs? conflict!
							result.addToConflictList(rowFM, remote);
							continue;
						}
						// does not differ? then just update LAUID!
						else {
							TreeMap<String,String> set = new TreeMap<String,String>();
							// setting LAUID = RAUID via local update list
							set.put("ArachneEntityID", RAUID+"");
							// content is equal to remote. so overwrite local TS with remote
							set.put("lastModified", lmRemote);
							set.put("lastRemoteTS", lmRemote);
							result.addToUpdateList(rowFM, set, true);
							handledUIDs.add(RAUID);
						}
					}
					// nothing remotely. upload whole row
					else {
						result.addToUploadList(rowFM);
					}
				}

				if (!mNotNull.isAfterLast()) // if there are still rows, the last next() was unnecessary
					mNotNull.previous();
				while (mNotNull.next()) {
					TreeMap<String,String> rowMS = new TreeMap<String,String>();
					String pk = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
					Pair lookup = new Pair(pk, mNotNull.getString(pk));
					String RAUID = null, lmRemote = null;
					
					for (String field : commonFields) {
						String val = mNotNull.getString(field);
						
						if (field.equals("ArachneEntityID")) RAUID = val;
						if (field.equals("lastModified")) lmRemote = val;
						
						rowMS.put(field, val);
					}
					TreeMap<String,String> local = isRowOnLocal(currTab, lookup, commonFields);
					// entry already in local db, just missing LAUID?
					if (!local.isEmpty()) {
						// update LAUID, overwrite TS. is equal anyway because of query ...
						TreeMap<String,String> set = new TreeMap<String,String>();
						// no RAUID, but in remote DB?!
						if (RAUID == null || RAUID.equals(""))
							throw new EntityManagementException(
									"An entry is on remote DB but has no AUID!");
						
						if (handledUIDs.contains(Integer.parseInt(RAUID))) continue;
						
						// setting LAUID = RAUID via local update list
						set.put("ArachneEntityID", RAUID);
						rowMS.remove("ArachneEntityID"); // remove to avoid wrong "where"
						set.put("lastModified", lmRemote); // important due to FM change listener on this field
						set.put("lastRemoteTS", lmRemote);
						result.addToUpdateList(rowMS, set, true);
						handledUIDs.add(Integer.parseInt((RAUID)));
						continue;
					}
					// nothing in local db. download whole row
					else {
						result.addToDownloadList(rowMS);
					}
				}
			} catch (SQLException e) {
				logger.error(e);
				throw e;
			}
			
			try {

				// RAUID == null
				ResultSet mNull = sqlAccess.doMySQLQuery(msSQL
						+ " AND ArachneEntityID IS NULL");

				while (mNull.next()) { // TODO
					Vector<Pair> row = new Vector<Pair>();
					for (String field : commonFields) {
						row.add(new Pair(field, mNull.getString(field)));
					}
				}
			} catch (FMSQLException e) {
				System.err.println("FEHLER: " + e);
				e.printStackTrace();
			}
		}
		results.add(result);
		return result;
	}

	
	private int compareFields(TreeMap<String, String> rowLocal,
			TreeMap<String, String> rowRemote, Pair currTab) throws SQLException, IOException {

		TreeSet<String> commonFields = new TreeSet<String>();
		//calculate common fields
		for (String key : rowRemote.keySet()) {
			if (rowLocal.containsKey(key)) {
				commonFields.add(key);
			}
		}
		return compareFields(commonFields, rowLocal, rowRemote, currTab);
	}

	private TreeMap<String, String> remoteLookupByPK(Pair currTab, Pair pk, TreeSet<String> commFields)
			throws SQLException, IOException {
		String archerMSSkip = "";
		TreeSet<String> commonFields = new TreeSet<String>(commFields);
		
		if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
			archerMSSkip = " AND " + currTab.getRight()
					+ ".ImportSource!='Comprehensive Table'";
		}
		String select = "SELECT arachneentityidentification.ArachneEntityID, "
				+ "CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00') as lastModified, ";
		String sql = " FROM arachneentityidentification"
				// left join includes deleted or empty UIDs
				+ " LEFT JOIN " + currTab.getRight()
				+ " ON arachneentityidentification.ForeignKey = "
				+ currTab.getRight() + "." + pk.getLeft()
				+ " WHERE arachneentityidentification.TableName=\""
				+ currTab.getRight() + "\"" + archerMSSkip
				+ " AND "+pk.getLeft() + "="+pk.getRight();

		commonFields.remove("ArachneEntityID");
		commonFields.remove("lastModified");
		
		Iterator<String> it = commonFields.iterator();
		while (it.hasNext()) {
			String next = it.next();
			if (it.hasNext())
				select += currTab.getRight()+"."+next + ",";
			else 
				select += currTab.getRight()+"."+next;
		}
		
		ResultSet r = sqlAccess.doMySQLQuery(select + sql);
		TreeMap<String,String> result = new TreeMap<String,String>();
		if (r.next()) {
			result.put("ArachneEntityID", r.getString("ArachneEntityID")); // RAUID
			String t = r.getTimestamp("lastModified") == null ? null : r.getTimestamp("lastModified")
					.toLocalDateTime().format(formatTS);
			result.put("lastModified", t); // lastmodified
			result.put(pk.getLeft(), r.getString(pk.getLeft())); // pk value
		}
		return result;
	}
	
	/**
	 * method checks, if row with common fields is already in remote DB
	 * 
	 * @param currTab
	 * @param row
	 * @param commonFields 
	 * @return Vector<Tuple<Pair, Object>> with Pair of RAUID, TS, and PK, if
	 *         row is already in remote db. empty list else.
	 * @throws SQLException
	 * @throws IOException
	 */
	private TreeMap<String, String> isRowOnRemote(Pair currTab, TreeMap<String, String> row, ArrayList<String> commonFields)
			throws SQLException, IOException {
		String archerMSSkip = "";
		if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
			archerMSSkip = " AND " + currTab.getRight()
					+ ".ImportSource!='Comprehensive Table'";
		}
		String pk = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
		String sql = "SELECT arachneentityidentification.ArachneEntityID, "
				+ "CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00') as lastModified, "
				+ currTab.getRight() + "." + pk
				+ " FROM arachneentityidentification"
				// left join includes deleted or empty UIDs
				+ " LEFT JOIN " + currTab.getRight()
				+ " ON arachneentityidentification.ForeignKey = "
				+ currTab.getRight() + "." + pk
				+ " WHERE arachneentityidentification.TableName=\""
				+ currTab.getRight() + "\"" + archerMSSkip;

		for (String key : commonFields) {
			
			if (key.equals("ArachneEntityID")
					|| key.equals("lastModified")
					|| key.equals("lastRemoteTS"))
				continue;
			
			String val = row.get(key) == null ? null
					: escapeChars(row.get(key));
			
			if (key.equals("lastRemoteTS")
					&& (val == null || val.equals("null")))
				continue;
			
			if (val == null || val.equals("null"))
				sql += " AND " + currTab.getRight() + "." + key
						+ " IS NULL";
			else if (isNumericalField(currTab.getLeft() + "." + key))
				sql += " AND " + currTab.getRight() + "." + key + " = "
						+ val;
			else
				sql += " AND " + currTab.getRight() + "." + key
						+ " = '" + val + "'";
		}
		ResultSet r = sqlAccess.doMySQLQuery(sql);
		TreeMap<String,String> result = new TreeMap<String,String>();
		ResultSetMetaData rmd = r.getMetaData();
		if (r.next()) {
			for (int i = 1; i <= rmd.getColumnCount(); i++) {
				result.put(rmd.getColumnLabel(i), r.getString(i));
			}
		}
		return result;
	}
	
	/**
	 * method checks, if row with common fields is already in local DB
	 * 
	 * @param currTab Pair with current table
	 * @param rowMS TreeMap of remote row
	 * @return row with RAUID, TS, and PK, if row is already in remote db. empty list else.
	 * @throws SQLException
	 * @throws IOException
	 */
	private TreeMap<String,String> isRowOnLocal(Pair currTab, Pair lookup, TreeSet<String> commonFields)
			throws SQLException, IOException {
		String archerMSSkip = "";
		if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
			archerMSSkip = " AND " + currTab.getLeft()
					+ ".ImportSource!='Comprehensive Table' AND";
		}
		String select = "SELECT lastRemoteTS,";
		String sql = " FROM " + currTab.getLeft()
				+ " WHERE " + archerMSSkip
				// if LAUID would not be null, this row would match a
				// remote row and this fct would not be invoked.
				+ " ArachneEntityID IS NULL"
				+ " AND "+lookup.getLeft()+"="+lookup.getRight();

		Iterator<String> it = commonFields.iterator();
		while (it.hasNext()) {
			String next = it.next();
			if (it.hasNext())
				select += next + ",";
			else 
				select += next;
		}
		ResultSet r = sqlAccess.doFMQuery(select + sql);
		TreeMap<String,String> result = new TreeMap<String,String>();
		ResultSetMetaData rmd = r.getMetaData();
		while (r.next()) {
			for (int i = 1; i <= rmd.getColumnCount(); i++) {
				result.put(rmd.getColumnLabel(i), r.getString(i));
			}
		}
		return result;
	}

	/**
	 * prepare row packs of size x to download into local db
	 * @param currTab
	 * @param RAUIDs
	 * @param packSize
	 * @return
	 * @throws SQLException
	 * @throws EntityManagementException
	 * @throws IOException
	 */
	public Vector<Integer> prepareRowsAndDownload(Pair currTab,
			Vector<TreeMap<String, String>> vector, int packSize) throws SQLException,
			EntityManagementException, IOException {
		int count = vector.size() / packSize;
		int mod = vector.size() % packSize;
		Vector<Integer> resultIDs = new Vector<Integer>();
		for (int k = 0; k < count; k++) {
			resultIDs.addAll(insertRowsIntoLocal(
							currTab,
							new Vector<TreeMap<String,String>>(vector.subList(k * packSize,
									(k + 1) * packSize))));
		}
		resultIDs.addAll(insertRowsIntoLocal(currTab, new Vector<TreeMap<String,String>>(
				vector.subList(count * packSize, count * packSize + mod))));
		return resultIDs;
	}
	
	private ArrayList<Integer> insertRowsIntoLocal(Pair currTab, Vector<TreeMap<String, String>> vector) throws SQLException, IOException, EntityManagementException {
		if (vector.size() == 0)
			return null;
		
		String sql = "INSERT INTO \"" + currTab.getLeft() + "\" (";
		String vals = " VALUES (";
		// iterate through rows
		for (int i = 0; i < vector.size(); i++) {
			
			TreeMap<String,String> currRow = vector.get(i);
			
			Iterator<String> it = currRow.keySet().iterator();
			// ( col1, col2, col3, ..)
			while (it.hasNext()) {
				String currFieldName = it.next();
				String longName = currTab.getLeft() + "." + currFieldName;
				if (i == 0) {
					if (currFieldName.endsWith("lastModified")){
						sql += "lastModified,lastRemoteTS";
					}
					else
						sql += currFieldName;
				}
				
				String currFieldVal = currRow.get(currFieldName);
				if (!isNumericalField(longName)
						&& !isTimestampField(longName)
						&& currFieldVal != null) {
					currFieldVal = "'" + escapeChars(currFieldVal) + "'";
				}
				else if (isTimestampField(longName)
						&& currFieldVal != null) {
					if (currFieldName.endsWith("lastModified")){
						currFieldVal = "TIMESTAMP '" + currFieldVal + "', TIMESTAMP '" + currFieldVal + "'";
					}
					else
						currFieldVal = "TIMESTAMP '" + currFieldVal + "'";
				}
				
				vals += currFieldVal;
				
				if (it.hasNext()) {
					if (i == 0) sql += ",";
					vals += ",";
				}
			}
			if (i < vector.size()-1) {
				vals += "),(";
			}
			else {
				sql += ")";
				vals += ")";
			}
		}
		// update local AUIDs
		return sqlAccess.doFMInsert(sql + vals);
	}

	/**
	 * prepare row packs of size x to upload into remote db
	 * @param currTab
	 * @param rows
	 * @param packSize
	 * @return
	 * @throws SQLException
	 * @throws EntityManagementException
	 * @throws IOException
	 */
	public Vector<Integer> prepareRowsAndUpload(Pair currTab, Vector<TreeMap<String, String>> rows, int packSize) throws SQLException,
			EntityManagementException, IOException {
		int count = rows.size() / packSize;
		int mod = rows.size() % packSize;
		Vector<Integer> resultIDs = new Vector<Integer>();
		for (int k = 0; k < count; k++) {
			resultIDs.addAll(insertRowsIntoRemote(
							currTab,
							new Vector<TreeMap<String,String>>(rows.subList(k * packSize,
									(k + 1) * packSize))));
		}
		resultIDs.addAll(insertRowsIntoRemote(currTab, new Vector<TreeMap<String,String>>(
				rows.subList(count * packSize, count * packSize + mod))));
		return resultIDs;
	}

	/**
	 * Insert given rows
	 * 
	 * @param currTab
	 * @param vector
	 * @return array of inserted IDs. null, if given row list is empty
	 * @throws SQLException
	 * @throws EntityManagementException
	 * @throws IOException
	 */
	private ArrayList<Integer> insertRowsIntoRemote(Pair currTab,
			Vector<TreeMap<String, String>> vector) throws SQLException,
			EntityManagementException, IOException {
		
		if (vector.size() == 0)
			return null;
		
		String sql = "INSERT INTO " + currTab.getRight() + " (";
		String vals = " VALUES (";
		String keyField = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
		
		// ( col1, col2, col3, ..)
		if (!vector.isEmpty()) {
			TreeSet<String> keys = new TreeSet<String>(vector.get(0).keySet());
			keys.remove("ArachneEntityID");
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				sql += "`" + it.next() + "`";
				if (it.hasNext()) sql += ",";
			}
			sql += ")";
		}
		
		// rows
		for (int i = 0; i < vector.size(); i++) {
			TreeMap<String,String> currRow = new TreeMap<String,String>(vector.get(i));
			currRow.remove("ArachneEntityID");
			Iterator<String> it = currRow.keySet().iterator();
			// values for row
			while (it.hasNext()) {
				String currFieldName = it.next();
				String currFieldVal = currRow.get(currFieldName);
				String longName = currTab.getLeft() + "." + currFieldName;
				
				if (!isNumericalField(longName)
						&& !isTimestampField(longName)
						&& currFieldVal != null) {
					currFieldVal = "'" + escapeChars(currFieldVal) + "'";
				} else if (isTimestampField(longName)
						&& currFieldVal != null) {
					currFieldVal = "TIMESTAMP '" + currFieldVal + "'";
				}
				vals += currFieldVal;
				
				if (it.hasNext())  vals += ",";
			}
			if (i < vector.size()-1)
				vals += "),(";
			else
				vals += ");";
		}
		// update local AUIDs
		ArrayList<Integer> localIDs = sqlAccess.doMySQLInsert(sql + vals);
		ArrayList<Integer> result = new ArrayList<Integer>();
		String ids = "";
		int proof = 0;

		for (int i = 0; i < localIDs.size(); i++) {
			if (i > 0) ids += " OR ";
			ids += currTab.getRight()+"."+keyField + "=" + localIDs.get(i);
		}
		
		// get remote AUID and TS
		ResultSet id = sqlAccess.doMySQLQuery("SELECT arachneentityidentification.ArachneEntityID,"
					+ currTab.getLeft() + "." + keyField + ","
					+ "CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00') as lastModified "
					+ " FROM arachneentityidentification JOIN "
					+ currTab.getRight()
					+ " ON arachneentityidentification.ForeignKey="
					+ currTab.getRight() + "." + keyField
					+ " AND arachneentityidentification.TableName='"
					+ currTab.getRight() + "'" 
					+ " WHERE " + ids + ";");
		while (id.next()) {
			proof++;
			int RAUID = id.getInt("ArachneEntityID");
			if (!updateLocalUIDAndTS(currTab, RAUID, id.getTimestamp("lastModified").toLocalDateTime(), keyField, id.getInt(keyField)))
				throw new EntityManagementException("Updating local AUID "
						+ id.getInt(1) + " and timestamp in table " + currTab.getLeft()
						+ " FAILED!");
			else {
				System.out.println("Updated local AUID " + id.getInt(1)
						+ " in table " + currTab.getLeft()
						+ " after upload.");
				result.add(RAUID);
			}
		}
		if (proof != localIDs.size())
			throw new EntityManagementException("Updating local UIDs and timestamps in table " + currTab.getLeft() + " FAILED!");

		return result;
	}

	private String escapeChars(String currFieldVal) throws IOException {
		return currFieldVal.replace("'", "\\'").replace("\"", "\\\"")
				.replace("%", "\\%");
	}

	private boolean updateLocalUIDAndTS(Pair currTab, int currRAUID,
			LocalDateTime currRemoteTS, String pkName, int pkVal)
			throws SQLException {
		
		// locate row by local ID
		if (sqlAccess.doFMUpdate("UPDATE \"" + currTab.getLeft()
				+ "\" SET ArachneEntityID=" + currRAUID
				+ ", lastModified={ts '" + currRemoteTS.format(formatTS) + "'}"
				+ ", lastRemoteTS={ts '" + currRemoteTS.format(formatTS) + "'}"
				+ " WHERE " + pkName + "=" + pkVal) != null) {
			logger.debug("Added RUID " + currRAUID
					+ " and TS to local entry with ID " + pkVal + ".");
			return true;
		} else {
			logger.error("Adding RUID " + currRAUID
					+ " and TS to local entry with ID " + pkVal + " FAILED.");
			return false;
		}
	}

	/**
	 * Method to compare field values of a row from mySQL and FileMaker
	 * 
	 * @param commonFields
	 *            ArrayList of common fields
	 * @param rowMS
	 *            Arraylist of mysql row values
	 * @param rowFM
	 *            Arraylist of fm row values
	 * @param currTab
	 *            current table as Pair object
	 * @return 0, if all except AUID and lastModified are equal and not all
	 *         empty. 1, if there exists at least one field that is not empty
	 *         for both, but also not equal. 2, if only local fields are empty
	 *         and entry needs to be downloaded. 3, if only remote fields are
	 *         empty and entry needs to be uploaded. 4, if both are empty
	 *         (deleted?). 5, if remote index is missing and has to be shifted.
	 *         6, if fm index is missing and has to be shifted. -1, if #fields differs
	 * @throws SQLException
	 * @throws IOException
	 */
	private int compareFields(TreeSet<String> commonFields, TreeMap<String, String> rowFM, TreeMap<String, String> rowMS,
			Pair currTab) throws SQLException, IOException {
		
		if (rowMS.size() != rowFM.size()) return -1;
		
		if (!rowMS.keySet().equals(rowFM.keySet())) return -1;
		
		int res = 1;
		boolean localAllNull = true; // empty local fields
		boolean remoteAllNull = true; // empty remote fields
		boolean allSame = true;
		
		int offset = 0;

		for (String currField : commonFields) {

			// skip UID field, this is not equal anyway
			if (currField.equalsIgnoreCase("ArachneEntityID")
					|| currField.equalsIgnoreCase("lastModified"))
				continue;

			// Strings to compare in field with equal name
			String myVal = rowMS.get(currField);
			String fmVal = rowFM.get(currField);

			myVal = myVal == null ? "" : myVal;
			fmVal = fmVal == null ? "" : fmVal;

			if (!myVal.isEmpty())
				remoteAllNull = false;
			if (!fmVal.isEmpty())
				localAllNull = false;

			if (!(myVal.equalsIgnoreCase(fmVal))) {

				if (isFMPrimaryKey(currTab, currField)) {

					// if local ID is 0, then entry is NULL on each field.
					if (myVal.isEmpty())
						return 3;
					if (fmVal.isEmpty())
						return 2;

					System.out.println("INDEX SHIFTING!! LocalID: " + fmVal
							+ ", RemoteID: " + myVal);
					logger.debug("INDEX SHIFTING!! LocalID: " + fmVal
							+ ", RemoteID: " + myVal);

					int fID = Integer.parseInt(fmVal);
					int mID = Integer.parseInt(myVal);
					
					offset = fID - mID;

					// missing entry in remote db
					if (fID < mID) // offset < 0
						return 5;
					// missing entry in local db
					else // offset > 0
						return 6;
				}

				// numeric fields can differ as strings, e.g. 9.0 and 9.00
				if (isNumericalField(currTab.getLeft()+"."+currField)) {
					// parsing empty string raises exc
					if (!myVal.isEmpty() && !fmVal.isEmpty()) { 
						try {
							double m = Double.parseDouble(myVal);
							double f = Double.parseDouble(fmVal);
							if (m == f) {
								continue;
							}
						} catch (NumberFormatException e) {
							logger.error("NFException! " + e);
						}
					}
				}
				allSame = false;
				System.out.println(currField + " in " + currTab
						+ " not equal: \"" + fmVal + "\" (FM) and \"" + myVal
						+ "\" (MS)");
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
	 * 
	 * @param currTab
	 *            filemaker table name
	 * @param field
	 *            filemaker field name to check
	 * @return true, if PK. false else.
	 */
	private boolean isFMPrimaryKey(Pair currTab, String field) {
		return sqlAccess.getFMTablePrimaryKey(currTab.getLeft()).equalsIgnoreCase(field);
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
				if (metaFMTab.getColumnName(i).equalsIgnoreCase(
						metaMSTab.getColumnName(j))) {
					result.add(metaFMTab.getColumnName(i));
				}
			}
		}
		return result;
	}

	/**
	 * get common fields from fm and ms tables
	 * 
	 * @param currTab
	 *            Pair of table to be processed
	 * @return common field names as Pairs in ArrayList
	 * @throws SQLException
	 */
	public TreeSet<String> getCommonFields(Pair currTab, ResultSet fmColumns, ResultSet msColumns) throws SQLException {

		TreeSet<String> result = new TreeSet<String>();

		while (fmColumns.next()) {
			msColumns.beforeFirst();
			if (fmColumns.getString("COLUMN_NAME").equalsIgnoreCase("ArachneEntityID") && !result.contains("ArachneEntityID")) {
				result.add("ArachneEntityID");
				continue;
			}
			while (msColumns.next()) {
				if (fmColumns.getString("COLUMN_NAME").equalsIgnoreCase(
						msColumns.getString("COLUMN_NAME"))) {
					result.add(fmColumns.getString("COLUMN_NAME"));
					continue;
				}
			}
		}
		return result;
	}

	private String getBerlinTimeStamp() {
		return LocalDateTime.now(zoneBerlin).format(formatTS);
	}

	private boolean isNumericalField(String field) throws IOException {
		return conf.getNumericFields().contains(field);
	}

	private boolean isTimestampField(String field) throws IOException {
		return conf.getTimestampFields().contains(field);
	}

	public boolean updateRowsLocally(Pair currTab,
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> list) throws SQLException, IOException {
		boolean res = true;
		
		for (Tuple<TreeMap<String, String>, TreeMap<String, String>> row : list) {
			res = res && updateRowOnLocal(currTab, row);
		}
		return res;
	}

	private boolean updateRowOnLocal(Pair currTab, Tuple<TreeMap<String, String>, TreeMap<String, String>> tuple) throws SQLException, IOException {
		if (tuple.getLeft().size() == 0 || tuple.getRight().size() == 0)
			return false;
		
		String sql = "UPDATE \"" + currTab.getLeft() + "\" SET ";
		
		TreeMap<String, String> set = tuple.getRight();
		TreeMap<String, String> where = tuple.getLeft();
		Iterator<String> it = set.keySet().iterator();
		
		while (it.hasNext()) {
			String currFieldName = it.next();
			String longName = currTab.getLeft() + "." + currFieldName;
			String currFieldVal = set.get(currFieldName);
			
			if (!isNumericalField(longName)
					&& !isTimestampField(longName)
					&& !currFieldName.equals("lastRemoteTS")
					&& currFieldVal != null) {
				currFieldVal = "'" + escapeChars(currFieldVal) + "'";
			}
			else if ((isTimestampField(longName) || currFieldName.equals("lastRemoteTS")) && currFieldVal != null) {
				sql += longName + "=" + "TIMESTAMP '" + currFieldVal + "'";
			}
			else {
				sql += longName + "=" + currFieldVal;
			}
			if (it.hasNext()) sql += ",";
		}
		
		it = where.keySet().iterator();
		sql += " WHERE ";
		while (it.hasNext()) {
			String currFieldName = it.next();
			String longName = currTab.getLeft() + "." + currFieldName;
			String currFieldVal = where.get(currFieldName);
			
			if (!isNumericalField(longName)
					&& !isTimestampField(longName)
					&& !currFieldName.equals("lastRemoteTS")
					&& currFieldVal != null) {
				currFieldVal = "'" + escapeChars(currFieldVal) + "'";
			}
			else if ((isTimestampField(longName) || currFieldName.equals("lastRemoteTS")) && currFieldVal != null) {
				sql += longName + "=" + "'" + currFieldVal + "'";
			}
			else {
				sql += longName + "=" + currFieldVal;
			}
			if (it.hasNext()) sql += " AND ";
		}
		
		sqlAccess.doFMUpdate(sql);
		return true;
	}

	
}
