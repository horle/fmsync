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
	private ArrayList<Pair> commonTables;
	private ArrayList<ComparisonResult> results;

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
		sqlAccess = SQLAccessController.getInstance();
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

	public ComparisonResult getDiffByUUID(Pair currTab, boolean upload,
			boolean download) throws SQLException, FilemakerIsCrapException,
			SyncException, EntityManagementException, IOException {

		ComparisonResult result = new ComparisonResult(currTab);

		// both connected?
		if (sqlAccess.isMySQLConnected() && sqlAccess.isFMConnected()) {
			
			ResultSet fmColumnMeta = sqlAccess.getFMColumnMetaData(currTab.getLeft());
			ResultSet msColumnMeta = sqlAccess.getMySQLColumnMetaData(currTab.getRight());
			result.setFmColumns(fmColumnMeta);
			result.setMsColumns(msColumnMeta);
			
			ArrayList<String> commonFields = getCommonFields(currTab, fmColumnMeta, msColumnMeta);

			// test, if fields are available in FM
			if (!addAUIDField(currTab.getLeft())) {
				commonFields.add("ArachneEntityID");
			}
			if (!commonFields.contains("lastModified")) {
				addLastModifiedField(currTab.getLeft());
				commonFields.add("lastModified");
			}
			addLastRemoteTSField(currTab.getLeft());

			String sqlCommonFields = "";
			for (int j = 0; j < commonFields.size(); j++) {
				if (j == commonFields.size() - 1)
					sqlCommonFields += commonFields.get(j);
				else
					sqlCommonFields += commonFields.get(j) + ",";
			}
			// Skipping Martin Archer's entries => excel
			String archerMSSkip = "", archerFMSkip = "";
			if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
				archerMSSkip = " AND " + currTab.getRight()
						+ ".ImportSource!='Comprehensive Table'";
				archerFMSkip = " WHERE " + currTab.getLeft()
						+ ".ImportSource!='Comprehensive Table'";
			}

			String fmSQL = "SELECT " + sqlCommonFields + ", lastRemoteTS FROM " + currTab.getLeft()
					+ archerFMSkip;
			String msSQL = "SELECT arachneentityidentification.ArachneEntityID, "
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

			// entities:
			int currAAUID = 0; // current arachne uid in arachne
			int currCAUID = 0; // current arachne uid in fm
			Timestamp currATS = null; // current arachne timestamp
			Timestamp currCTS = null; // current fm timestamp
			Timestamp currLRTS = null; // last remote timstamp saved in fm
			// String currLocalTS = getBerlinTimeStamp(); // current timestamp
			// in CET format (arachne is in germany ...)

			try {
				while (fNotNull.next() & mNotNull.next()) {
					currAAUID = mNotNull.getInt("ArachneEntityID");
					currCAUID = fNotNull.getInt("ArachneEntityID");
					currATS = mNotNull.getTimestamp("lastModified");
					currCTS = fNotNull.getTimestamp("lastModified");
					currLRTS = fNotNull.getTimestamp("lastRemoteTS");

					// missing entry in regular table, but entry in entity
					// management! bug in db!
					// TODO: delete entry in arachne entity management
					if (currATS == null) {
						throw new EntityManagementException(
								"Missing entry in LOCAL Arachne entity management! Table "
										+ currTab.getLeft()
										+ ", remote entry: " + currAAUID);
					}
					if (currAAUID == 0 || currCAUID == 0) {
						throw new EntityManagementException(
								"Something went wrong: NULL value in NOT NULL query");
					}

					if (currAAUID == currCAUID) {
						// timestamps all equal: SKIP
						if (currATS.equals(currCTS) && currCTS.equals(currLRTS)) {
							continue;
						}
						// local after remote, remote did not change: SAFE UPLOAD
						if (currCTS.after(currATS) && currATS.equals(currLRTS)) {
							Vector<Pair> row = new Vector<Pair>();
							for (int l = 0; l < commonFields.size(); l++) {
								row.add(new Pair(commonFields.get(l), fNotNull
										.getString(commonFields.get(l))));
							}
							result.addRowToUploadList(row);
							continue;
						}
						// remote after local AND local <= last remote <= remote: DOWNLOAD
						if (currCTS.before(currATS)
								&& (currCTS.before(currLRTS) && currLRTS.before(currATS))) {
							Vector<Pair> row = new Vector<Pair>();
							for (int l = 0; l < commonFields.size(); l++) {
								row.add(new Pair(commonFields.get(l), mNotNull
										.getString(commonFields.get(l))));
							}
							result.addRowToDownloadList(row);
							continue;
						}
						// local after last remote AND remote after last remote:
						// CONFLICT
						if (currCTS.after(currLRTS) && currLRTS.before(currATS)) {
							Vector<Pair> rowFM = new Vector<Pair>();
							Vector<Pair> rowMS = new Vector<Pair>();
							for (int l = 0; l < commonFields.size(); l++) {
								rowFM.add(new Pair(commonFields.get(l),
										fNotNull.getString(commonFields.get(l))));
								rowMS.add(new Pair(commonFields.get(l),
										mNotNull.getString(commonFields.get(l))));
							}
							result.addToConflictList(rowFM, rowMS);
							continue;
						}
						// e.g. 
					}
					else if (currAAUID < currCAUID) {
						
						//TODO PLACEHOLDER for better logic at this point..
						throw new EntityManagementException("The entry with AUID "+ currCAUID + " seems to not exist in Arachne, but it should!");
						
						
					}	
					// e.g. 1 in local and 4 in remote. this case only occurs if entry does not exist
					// in remote db (impossible due to arachne entity management)
					else if (currAAUID > currCAUID) {
						throw new EntityManagementException("The entry with AUID "+ currCAUID + " seems to not exist in Arachne, but it should!");
					}
				}
				fNotNull.previous(); // neccessary because of next()
				while (fNotNull.next()) {
					throw new EntityManagementException("The entry with AAID "+ fNotNull.getString("ArachneEntityID") + " seems to not exist in Arachne, but it should!");
				}
				mNotNull.previous(); // neccessary because of next()
				while (mNotNull.next()) {
					Vector<Pair> rowMS = new Vector<Pair>();
					Vector<Pair> lookup = new Vector<Pair>();
					Pair aauid = null, lmRemote = null;
					
					for (int l = 0; l < commonFields.size(); l++) {
						Pair p = new Pair(commonFields.get(l), mNotNull
								.getString(commonFields.get(l)));
						if (!commonFields.get(l).equals("ArachneEntityID")
								&& !commonFields.get(l).equals("lastModified")) {
							lookup.add(p);
							rowMS.add(p);
						}
						else {
							if (commonFields.get(l).equals("ArachneEntityID")) aauid = p;
							if (commonFields.get(l).equals("lastModified")) lmRemote = p;
						}
					}
					Vector<Pair> local = isRowOnLocal(currTab, lookup);
					// entry already in local db
					if (!local.isEmpty()) {
						Timestamp remoteTS = Timestamp.valueOf(lmRemote.getRight());
						Timestamp localTS = Timestamp.valueOf(local.get(0).getRight());
						// just missing CAUID, TS equal?
						// update locally ...
						if (remoteTS.equals(localTS)) {
							Vector<Pair> set = new Vector<Pair>();
							// no AAUID, but in remote DB?!
							if (aauid.getRight() == null || aauid.getRight().equals(""))
								throw new EntityManagementException(
										"An entry is on remote DB but has no AUID!");
							// setting cauid = aauid via local update list
							set.add(aauid);
							rowMS.remove(aauid); // remove to avoid wrong "where"
							rowMS.remove(lmRemote); // remove to avoid wrong "where"
							set.add(lmRemote);
							set.add(new Pair(
									"lastRemoteTS", lmRemote.getRight()));
							result.addToUpdateList(rowMS, set, true);
						}
						else { //TODO Conflict
							rowMS.remove(aauid);
							rowMS.remove(lmRemote);
							local.remove(0);
							if (compareFields(rowMS, local, currTab) != 0)
								result.addToConflictList(local, rowMS);
						}
						
					}
					// nothing in local db. download whole row
					else {
						result.addRowToDownloadList(rowMS);
					}
				}
			} catch (SQLException e) {
				logger.error(e);
				throw e;
			}

			if (upload) {
				try {
					ResultSet fNull = sqlAccess.doFMQuery(fmSQL
							+ (archerFMSkip == "" ? " WHERE " : " AND ")
							+ "ArachneEntityID IS NULL");

					// CAUID == null
					while (fNull.next()) {
						// add local row content to Vector, lookup remotely
						Vector<Pair> row = new Vector<Pair>();
						for (int i = 0; i < commonFields.size(); i++) {
//							if (commonFields.get(i).equalsIgnoreCase("ArachneEntityID")
//									|| commonFields.get(i).equalsIgnoreCase("lastModified")
//									|| commonFields.get(i).equalsIgnoreCase("lastRemoteTS")
//								) continue;
							
							row.add(new Pair(commonFields.get(i), fNull
									.getString(commonFields.get(i))));
						}
						Vector<Pair> remote = isRowOnRemote(currTab, row);
						// already uploaded, same content, just missing CAUID?
						// update locally ...
						if (!remote.isEmpty()) {
							Vector<Pair> set = new Vector<Pair>();
							// get AAUID
							Pair aauid = remote.get(0);
							Pair lastModified = remote.get(1);
							// no AAUID, but in remote DB?!
							if (aauid.getRight() == null || aauid.getRight().equals(""))
								throw new EntityManagementException(
										"An entry is on remote DB but has no AUID!");
							// setting cauid = aauid via local update list
//							where.remove(aauid);
							set.add(aauid);
							// content is equal to remote. so overwrite local TS with remote
//							where.remove(lastModified);
							set.add(lastModified);
							set.add(new Pair(
									"lastRemoteTS", lastModified.getRight()));
							result.addToUpdateList(row, set, true);
						}
						// nothing remotely. upload whole row and update CAUID
						else {
							result.addRowToUploadList(row);
						}
					}
				} catch (FMSQLException e) {
					throw e;
				}
			}
			try {

				// AAUID == null
				ResultSet mNull = sqlAccess.doMySQLQuery(msSQL
						+ " AND ArachneEntityID IS NULL");

				while (mNull.next()) { // TODO
					Vector<Pair> row = new Vector<Pair>();
					for (int i = 0; i < commonFields.size(); i++) {
						row.add(new Pair(commonFields.get(i), mNull
								.getString(commonFields.get(i))));
					}
				}
			} catch (FMSQLException e) {
				System.err.println("FEHLER: " + e);
				e.printStackTrace();
			}

			System.out.println("done.");

			/**
			 * OLD LOGIC TODO
			 */
			if (new Boolean("false"))
				try {
					ResultSet fNull = null;
					ResultSet mNull = null;

					while (new Boolean("false")) {
						// missing entry in regular table, but entry in entity
						// management! bug in db!
						// TODO: delete entry in arachne entity management
						if (currATS == null) {
							throw new EntityManagementException(
									"Missing entry in LOCAL Arachne entity management! Table "
											+ currTab.getLeft()
											+ ", remote entry: " + currAAUID);
						}
						LocalDateTime currArachneTS = currATS.toLocalDateTime();

						// C-AUID differs from online ...
						if (currCAUID != currAAUID) {

							// ... because C-AUID is missing
							if (currCAUID == 0) {

								ArrayList<String> fmVals = new ArrayList<String>();
								ArrayList<String> msVals = new ArrayList<String>();

								for (int j = 0; j < commonFields.size(); j++) {
									fmVals.add(fNull.getString(commonFields
											.get(j)));
									msVals.add(mNull.getString(commonFields
											.get(j)));
								}

								// Check all other fields. Is just C-AUID
								// missing?
								switch (3) {//compareFields(commonFields, msVals, fmVals, currTab)) {

								// all fields equal (not null), then just update
								// local UID field and TS
								case 0:
									String fmKeyName = sqlAccess
											.getFMTablePrimaryKey(currTab
													.getLeft());
									String msKeyName = sqlAccess
											.getMySQLTablePrimaryKey(currTab
													.getRight());
									int fmKeyVal = fNull.getInt(fmKeyName);
									int msKeyVal = mNull.getInt(msKeyName);

									if (fmKeyVal == msKeyVal
											&& !updateLocalUIDAndTS(currTab,
													currAAUID, currArachneTS,
													fmKeyName, fmKeyVal))
										throw new SQLException(
												"local ID/TS could not be updated!");

									break;

								case -1:
									throw new SQLException(
											"Something went wrong when comparing fields!");

									// both sets are empty, delete both!
								case 4:
									result.addToDeleteList(currCAUID, currAAUID);
									break;

								// local fields are all empty and need to be
								// downloaded
								case 2:
//									result.addAAUIDToDownloadList(currAAUID);
									break;

								// remote fields are empty, but AAUID != 0!
								// -> check lastModified, deleted or not?
								case 3:
									// entry has been deleted on remote, delete
									// also locally
									if (currATS.after(currCTS)) {
										result.addToDeleteList(currCAUID, 0);
										// deleted remote, but new local one
										// with same UID
									} else if (currATS.before(currCTS)) {
										// update entry on remote, do not
										// overwrite UID
										Vector<Pair> row = new Vector<Pair>();
										for (int l = 0; l < commonFields.size(); l++) {
											row.add(new Pair(commonFields
													.get(l), fmVals.get(l)));
										}
										result.addToUpdateList(row, null, true);
										// Conflict!
										// same TS and same UID, but different
										// content!
									} else {
										throw new SyncException(
												"UID and Timestamp same, but content differs!");
									}
									break;

								// fields both not empty and not equal
								case 1:
									// timestamp NOT available, otherwise CAUID
									// would also be av.
									// better: search for local entry in
									// arachne, set CAUID to found match!
									// otherwise, upload! TODO
									throw new SyncException(
											"CONFLICT! search for local entry in arachne, set CAUID to found match! otherwise, upload! "
													+ currAAUID);
								case 5:
									fNull.next();
									continue;
								case 6:
									mNull.next();
									continue;
								}
							}

							// ... because A-AUID is missing
							else if (currAAUID == 0) {

								ArrayList<String> fmVals = new ArrayList<String>();
								ArrayList<String> msVals = new ArrayList<String>();

								for (int j = 0; j < commonFields.size(); j++) {
									fmVals.add(fNull.getString(commonFields
											.get(j)));
									msVals.add(mNull.getString(commonFields
											.get(j)));
								}

								// Check all other fields. Is just A-AUID
								// missing?
								switch (3) {//compareFields(commonFields, msVals,fmVals, currTab)) {

								// all fields equal (not null), then entity
								// management has not been updated in arachne!!
								case 0:
									throw new EntityManagementException(
											"Content equal and not null, but missing remote ArachneEntitiyID! Just updating? :/");

									// both empty, entry seems to be deleted.
									// delete locally
								case 4:
									result.addToDeleteList(currCAUID, 0);
									continue;

									// all remote fields are empty AND A-AUID
									// doesn't exist. upload to arachne
								case 3:
									Vector<Pair> row = new Vector<Pair>();
									for (int l = 0; l < commonFields.size(); l++) {
										row.add(new Pair(commonFields.get(l),
												fmVals.get(l)));
									}
									result.addRowToUploadList(row);
									break;
								}
							}

							// ... because they differ and are both not 0.

							// this is a hard one, because each upload to
							// arachne
							// should create a new AUUID.
							// deleting the entry there should NOT cause the
							// AUUID to be
							// deleted as well (instead: isDeleted = 1).
							// but the local client cannot get a CUUID without
							// doing an
							// update.
							// therefore, local client cannot have a CCUID that
							// is not
							// in arachne. CCUID would be 0 instead.
							// EDIT: possible is a change of the remote AUID.
							else {
								throw new SyncException("weird stuff");
							}
						}
						// C-AUID == A-AUID! examine lastModified
						else {
							// timestamps all equal: SKIP
							if (currATS.equals(currCTS)
									&& currCTS.equals(currLRTS)) {
								continue;
							}
							// local after remote, remote did not change: SAFE
							// UPLOAD
							if (currCTS.after(currATS)
									&& currATS.equals(currLRTS)) {
								Vector<Pair> row = new Vector<Pair>();
								for (int l = 0; l < commonFields.size(); l++) {
									row.add(new Pair(commonFields.get(l), fNull
											.getString(commonFields.get(l))));
								}
								result.addRowToUploadList(row);
								continue;
							}
							// remote after local AND local <= last remote <=
							// remote: DOWNLOAD
							if (currCTS.before(currATS)
									&& (currCTS.compareTo(currLRTS) <= 0 && currLRTS
											.compareTo(currATS) <= 0)) {
//								result.addAAUIDToDownloadList(currAAUID);
								continue;
							}
							// local after last remote AND remote after last
							// remote: CONFLICT
							if (currCTS.after(currLRTS)
									&& currLRTS.before(currATS)) {
								throw new SyncException("Conflict!!");// TODO
																		// alle
																		// felder
																		// checken,
																		// merge,
																		// ...
																		// user
																		// decision
							}
						}
					}

					// if fm has more entries than mysql ... //TODO, noch nicht
					// schön
					if (mNull.isAfterLast() || !mNull.next()) {
						fNull.previous();
						String pk = sqlAccess.getFMTablePrimaryKey(currTab
								.getLeft());
						int first = 0, last = 0;
						Vector<Vector<Pair>> inserts = new Vector<Vector<Pair>>();
						while (fNull.next()) {
							Vector<Pair> ins = new Vector<Pair>();
							ArrayList<String> names = getCommonFields(currTab, fmColumnMeta, msColumnMeta);
							if (first == 0)
								first = fNull.getInt(pk);
							last = fNull.getInt(pk);

							for (int k = 0; k < names.size(); k++) {
								Pair p = new Pair(names.get(k),
										fNull.getString(names.get(k)));
								ins.add(p);
							}
							result.addRowToUploadList(ins);
							inserts.add(ins);
						}
						// System.out.println("insert entries "+first+"-"+last+" into arachne ... ");
						// insert packs of size 25
						prepareRowsAndUpload(currTab, inserts, 25);

					} else {
						// if mysql has more entries than fm ...
						mNull.previous(); // neccessary because of !mTab.next()
											// in if
						while (mNull.next()) {
//							result.addAAUIDToDownloadList(currAAUID);
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

	/**
	 * method checks, if row with common fields is already in remote DB
	 * 
	 * @param currTab
	 * @param row
	 * @return Vector<Tuple<Pair, Object>> with Pair of AAUID, TS, and PK, if
	 *         row is already in remote db. empty list else.
	 * @throws SQLException
	 * @throws IOException
	 */
	private Vector<Pair> isRowOnRemote(Pair currTab, Vector<Pair> row)
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

		for (int i = 0; i < row.size(); i++) {
			// pair of key-value
			Pair p = row.get(i);
			
			if ( p.getLeft().equals("ArachneEntityID")
					|| p.getLeft().equals("lastModified")
					|| p.getLeft().equals("lastRemoteTS"))
				continue;
			
			String val = p.getRight() == null ? null
					: escapeChars(p.getRight());
			
			if (p.getLeft().equals("lastRemoteTS")
					&& (val == null || val.equals("null")))
				continue;
			
			if (val == null || val.equals("null"))
				sql += " AND " + currTab.getRight() + "." + p.getLeft()
						+ " IS NULL";
			else if (isNumericalField(currTab.getLeft() + "." + p.getLeft()))
				sql += " AND " + currTab.getRight() + "." + p.getLeft() + " = "
						+ val;
			else
				sql += " AND " + currTab.getRight() + "." + p.getLeft()
						+ " = '" + val + "'";
		}
		ResultSet r = sqlAccess.doMySQLQuery(sql);
		Vector<Pair> result = new Vector<Pair>();
		while (r.next()) {
			result.add(new Pair("ArachneEntityID", r.getString("ArachneEntityID"))); // aauid
			String t = r.getTimestamp("lastModified") == null ? null : r.getTimestamp("lastModified")
					.toLocalDateTime().format(formatTS);
			result.add(new Pair("lastModified", t)); // lastmodified
			result.add(new Pair(pk, r.getString(pk))); // pk value
		}
		return result;
	}
	
	/**
	 * method checks, if row with common fields is already in local DB
	 * 
	 * @param currTab
	 * @param row
	 * @return Vector<Tuple<Pair, Object>> with Pair of AAUID, TS, and PK, if
	 *         row is already in remote db. empty list else.
	 * @throws SQLException
	 * @throws IOException
	 */
	private Vector<Pair> isRowOnLocal(Pair currTab, Vector<Pair> row)
			throws SQLException, IOException {
		String archerMSSkip = "";
		if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
			archerMSSkip = " AND " + currTab.getLeft()
					+ ".ImportSource!='Comprehensive Table' AND";
		}
//		String pk = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
		String select = "SELECT lastModified,";
		String sql = " FROM " + currTab.getLeft()
				+ " WHERE " + archerMSSkip
				// if AAUID would not be null, the row would be in the
				// not null query result and this fct not be invoked.
				+ " ArachneEntityID IS NULL";

		for (int i = 0; i < row.size(); i++) {
			// pair of key-value
			Pair p = row.get(i);
			
			select += p.getLeft();
			if (i < row.size()-1) select += ",";
			
			String val = p.getRight() == null ? null
					: escapeChars(p.getRight());
			
			if (val == null || val.equals("null"))
				sql += " AND " + currTab.getLeft() + "." + p.getLeft()
						+ " IS NULL";
			else if (isNumericalField(currTab.getLeft() + "." + p.getLeft()))
				sql += " AND " + currTab.getLeft() + "." + p.getLeft() + " = " + val;
			else
				sql += " AND " + currTab.getLeft() + "." + p.getLeft()
						+ " = '" + val + "'";
		}
		ResultSet r = sqlAccess.doFMQuery(select + sql);
		Vector<Pair> result = new Vector<Pair>();
		ResultSetMetaData rmd = r.getMetaData();
		while (r.next()) {
			for (int i = 1; i <= rmd.getColumnCount(); i++) {
				result.add(new Pair(rmd.getColumnLabel(i), r.getString(i)));
			}
		}
		return result;
	}

	/**
	 * prepare row packs of size x to download into local db
	 * @param currTab
	 * @param aauids
	 * @param packSize
	 * @return
	 * @throws SQLException
	 * @throws EntityManagementException
	 * @throws IOException
	 */
	public Vector<Integer> prepareRowsAndDownload(Pair currTab,
			Vector<Vector<Pair>> rows, int packSize) throws SQLException,
			EntityManagementException, IOException {
		int count = rows.size() / packSize;
		int mod = rows.size() % packSize;
		Vector<Integer> resultIDs = new Vector<Integer>();
		for (int k = 0; k < count; k++) {
			resultIDs.addAll(insertRowsIntoLocal(
							currTab,
							new Vector<Vector<Pair>>(rows.subList(k * packSize,
									(k + 1) * packSize))));
		}
		resultIDs.addAll(insertRowsIntoLocal(currTab, new Vector<Vector<Pair>>(
				rows.subList(count * packSize, count * packSize + mod))));
		return resultIDs;
	}
	
	private ArrayList<Integer> insertRowsIntoLocal(Pair currTab, Vector<Vector<Pair>> rows) throws SQLException, IOException, EntityManagementException {
		if (rows.size() == 0)
			return null;
		
		String sql = "INSERT INTO \"" + currTab.getLeft() + "\" (";
		String vals = " VALUES (";
		for (int i = 0; i <= rows.size(); i++) {
			
			Vector<Pair> currRow = i != rows.size() ? rows.get(i) : null;
			
			// ( col1, col2, col3, ..)
			if (i == 0) {
				for (int j = 0; j < currRow.size(); j++) {
					String currFieldName = currRow.get(j).getLeft();
					if (j > 0 && j < currRow.size()) {
						sql += ",";
					}
					if (currFieldName.equals("lastModified")){
						sql += "\"lastModified\", \"lastRemoteTS\"";
					}
					else
						sql += "\"" + currFieldName + "\"";
				}
				sql += ")";
			}
			
			// final paranthese
			if (i == rows.size()) {
				vals += ")";
				break;
			}
			
			for (int j = 0; j < currRow.size(); j++) {
				String currFieldName = currTab.getLeft() + "." + currRow.get(j).getLeft();
				// set comma before next field entry ...
				if (j > 0 && j < currRow.size()) {
					vals += ",";
				}
				
				String currFieldVal = currRow.get(j).getRight();
				if (!isNumericalField(currFieldName)
						&& !isTimestampField(currFieldName)
						&& currFieldVal != null) {
					currFieldVal = "'" + escapeChars(currFieldVal) + "'";
				}
				else if (isTimestampField(currFieldName)
						&& currFieldVal != null) {
					if (currFieldName.endsWith(".lastModified")){
						currFieldVal = "TIMESTAMP '" + currFieldVal + "', TIMESTAMP '" + currFieldVal + "'";
					}
					else
						currFieldVal = "TIMESTAMP '" + currFieldVal + "'";
				}
				
				vals += currFieldVal;
			}
			if (i < rows.size() - 1)
				vals += "),(";
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
	public Vector<Integer> prepareRowsAndUpload(Pair currTab,
			Vector<Vector<Pair>> rows, int packSize) throws SQLException,
			EntityManagementException, IOException {
		int count = rows.size() / packSize;
		int mod = rows.size() % packSize;
		Vector<Integer> resultIDs = new Vector<Integer>();
		for (int k = 0; k < count; k++) {
			resultIDs.addAll(insertRowsIntoRemote(
							currTab,
							new Vector<Vector<Pair>>(rows.subList(k * packSize,
									(k + 1) * packSize))));
		}
		resultIDs.addAll(insertRowsIntoRemote(currTab, new Vector<Vector<Pair>>(
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
			Vector<Vector<Pair>> vector) throws SQLException,
			EntityManagementException, IOException {
		
		if (vector.size() == 0)
			return null;
		
		String sql = "INSERT INTO " + currTab.getRight() + " (";
		String vals = " VALUES (";
		String keyField = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
		
		boolean isLastOut = false; // prevent comma before skipped field
		for (int i = 0; i <= vector.size(); i++) {
			
			Vector<Pair> currRow = i != vector.size() ? vector.get(i) : null;
			
			// ( col1, col2, col3, ..)
			if (i == 0) {
				for (int j = 0; j < currRow.size(); j++) {
					String currFieldName = currRow.get(j).getLeft();
					if (currFieldName.equalsIgnoreCase(keyField)
							|| currFieldName.equals("ArachneEntityID")) {
						isLastOut = true;
						continue;
					}
					if (j > 0 && j < currRow.size() && !isLastOut) {
						sql += ",";
					} else if (isLastOut)
						isLastOut = false;
					sql += "`" + currFieldName + "`";
				}
				sql += ")";
			}
			
			// final paranthese
			if (i == vector.size()) {
				vals += ");";
				break;
			}
			
			for (int j = 0; j < currRow.size(); j++) {
				String currFieldName = currRow.get(j).getLeft();
				//TODO: wegen FKs den PK nicht rausschmeissen!!!!
				if (currFieldName.equalsIgnoreCase(keyField)
						|| currFieldName.equals("ArachneEntityID")) {
					isLastOut = true;
					continue;
				}
				// set comma before next field entry ...
				if (j > 0 && j < currRow.size() && !isLastOut) {
					vals += ",";
				} else if (isLastOut)
					isLastOut = false;
				
				String currFieldVal = currRow.get(j).getRight();
				if (!isNumericalField(currFieldName)
						&& !isTimestampField(currFieldName)
						&& currFieldVal != null) {
					currFieldVal = "'" + escapeChars(currFieldVal) + "'";
				} else if (isTimestampField(currFieldName)
						&& currFieldVal != null) {
					currFieldVal = "TIMESTAMP '" + currFieldVal + "'";
				}
				
				vals += currFieldVal;
			}
			if (i < vector.size() - 1)
				vals += "),(";

		}
		// update local AUIDs
		ArrayList<Integer> localIDs = sqlAccess.doMySQLInsert(sql + vals);
		ArrayList<Integer> result = new ArrayList<Integer>();

		// get remote AUID and TS
		for (int i = 0; i < localIDs.size(); i++) {
			ResultSet id = sqlAccess.doMySQLQuery("SELECT arachneentityidentification.ArachneEntityID,"
						+ currTab.getLeft() + "." + keyField + ","
						+ "CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00') as lastModified "
						+ " FROM arachneentityidentification RIGHT JOIN "
						+ currTab.getRight()
						+ " ON arachneentityidentification.ForeignKey="
						+ currTab.getRight() + "." + keyField
						+ " AND arachneentityidentification.TableName='"
						+ currTab.getRight() + "'" + " WHERE "+currTab.getRight()+"."+keyField+"="
						+ localIDs.get(i) + ";");
			if (id.next()) {
				int aauid = id.getInt("ArachneEntityID");
				if (!updateLocalUIDAndTS(currTab, aauid, id.getTimestamp("lastModified")
						.toLocalDateTime(), keyField, localIDs.get(i)))
					throw new EntityManagementException("Updating local AUID "
							+ id.getInt(1) + " and timestamp in table " + currTab.getLeft()
							+ " FAILED!");
				else {
					logger.debug("Updated local AUID " + id.getInt(1)
							+ " in table " + currTab.getLeft()
							+ " after upload.");
					result.add(aauid);
				}
			} else 
				throw new EntityManagementException("Updating local AUID and timestamp in table " + currTab.getLeft()
					+ " FAILED!");
		}

		return result;
	}

	private String escapeChars(String currFieldVal) throws IOException {
		return currFieldVal.replace("'", "\\'").replace("\"", "\\\"")
				.replace("%", "\\%");
	}

	private boolean updateLocalUIDAndTS(Pair currTab, int currAAUID,
			LocalDateTime currArachneTS, String pkName, int pkVal)
			throws SQLException {
		
		// locate row by local ID
		if (sqlAccess.doFMUpdate("UPDATE \"" + currTab.getLeft()
				+ "\" SET ArachneEntityID=" + currAAUID
				+ ", lastModified={ts '" + currArachneTS.format(formatTS) + "'}"
				+ ", lastRemoteTS={ts '" + currArachneTS.format(formatTS) + "'}"
				+ " WHERE " + pkName + "=" + pkVal) != null) {
			logger.debug("Added Arachne UID " + currAAUID
					+ " and TS to local entry with ID " + pkVal + ".");
			return true;
		} else {
			logger.error("Adding Arachne UID " + currAAUID
					+ " and TS to local entry with ID " + pkVal + " FAILED.");
			return false;
		}
	}

	/**
	 * Method to compare field values of a row from mySQL and FileMaker
	 * 
	 * @param commonFields
	 *            ArrayList of common fields
	 * @param msVals
	 *            Arraylist of mysql row values
	 * @param fmVals
	 *            Arraylist of fm row values
	 * @param currentTable
	 *            current table as Pair object
	 * @return 0, if all except AUID and lastModified are equal and not all
	 *         empty. 1, if there exists at least one field that is not empty
	 *         for both, but also not equal. 2, if only local fields are empty
	 *         and entry needs to be downloaded. 3, if only remote fields are
	 *         empty and entry needs to be uploaded. 4, if both are empty
	 *         (deleted?). 5, if arachne index is missing and has to be shifted.
	 *         6, if fm index is missing and has to be shifted.
	 * @throws SQLException
	 * @throws IOException
	 */
	private int compareFields(AbstractList<Pair> msVals, AbstractList<Pair> fmVals,
			Pair currentTable) throws SQLException, IOException {

		Vector<String> commonFields = new Vector<String>();
		
		for (int i = 0; i < msVals.size(); i++) {
			for (int j = 0; j < fmVals.size(); j++) {
				if (msVals.get(i).getLeft().equalsIgnoreCase(fmVals.get(j).getLeft())) {
					commonFields.add(fmVals.get(j).getLeft());
					break;
				}
			}
		}
		
		int col = commonFields.size();
		int res = 1;
		boolean localAllNull = true; // empty local fields
		boolean remoteAllNull = true; // empty remote fields
		boolean allSame = true;

		for (int l = 0; l < col; l++) {

			String currField = commonFields.get(l);

			// skip UID field, this is not equal anyway
			if (currField.equalsIgnoreCase("ArachneEntityID")
					|| currField.equalsIgnoreCase("lastModified"))
				continue;

			// Strings to compare in field with equal name
			String myVal = msVals.get(l).getRight();
			String fmVal = fmVals.get(l).getRight();

			myVal = myVal == null ? "" : myVal;
			fmVal = fmVal == null ? "" : fmVal;

			if (!myVal.isEmpty())
				remoteAllNull = false;
			if (!fmVal.isEmpty())
				localAllNull = false;

			if (!(myVal.equalsIgnoreCase(fmVal))) {

				if (isFMPrimaryKey(currentTable.getLeft(), currField)) {

					// if ID is 0, then entry is completely NULL.
					if (myVal.isEmpty())
						return 3;
					if (fmVal.isEmpty())
						return 2;

					System.out.println("INDEX SHIFTING!! LocalID: " + fmVal
							+ ", RemoteID: " + myVal);

					int fID = Integer.parseInt(fmVal);
					int mID = Integer.parseInt(myVal);

					// missing entry in arachne
					if (fID < mID)
						return 5;
					else
						// missing entry in FM
						return 6;
				}

				// numeric fields can differ as strings, e.g. 9.0 and 9.00
				if (isNumericalField(currField)) {
					if (!myVal.isEmpty() && !fmVal.isEmpty()) { // parsing empty
																// string raises
																// exc
						try {
							double m = Double.parseDouble(myVal);
							double f = Double.parseDouble(fmVal);
							if (m == f) {
								continue;
							}
						} catch (NumberFormatException e) {
							System.err.println("NFException! " + e);
						}
					}
				}
				allSame = false;
				System.out.println(currField + " in " + currentTable
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
	 * @param table
	 *            filemaker table name
	 * @param field
	 *            filemaker field name to check
	 * @return true, if PK. false else.
	 */
	private boolean isFMPrimaryKey(String table, String field) {
		return sqlAccess.getFMTablePrimaryKey(table).equalsIgnoreCase(field);
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
		// // DEBUG: get all fields just in FM
		// for (int i = 1; i <= metaFMTab.getColumnCount(); i++) {
		// if (!result.contains(metaFMTab.getColumnName(i))
		// && !metaFMTab.getColumnName(i).startsWith("["))
		// fs.add(metaFMTab.getColumnName(i));
		// }
		// if (!fs.isEmpty())
		// System.out.println(fs);
		// // DEBUG end
		System.out.println(result);
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
	public ArrayList<String> getCommonFields(Pair currTab, ResultSet fmColumns, ResultSet msColumns) throws SQLException {

		ArrayList<String> result = new ArrayList<String>();

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
		return ConfigController.getInstance().getNumericFields()
				.contains(field);
	}

	private boolean isTimestampField(String field) throws IOException {
		return ConfigController.getInstance().getTimestampFields()
				.contains(field);
	}

	
}
