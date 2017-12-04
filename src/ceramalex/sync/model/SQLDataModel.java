package ceramalex.sync.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.filemaker.jdbc.FMSQLException;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.EntityManagementException;
import ceramalex.sync.exception.FilemakerIsCrapException;
import ceramalex.sync.exception.MissingPermissionException;
import ceramalex.sync.exception.SyncException;

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
	private ArrayList<Pair> commonTables;
	private TreeMap<Pair,ComparisonResult> results;
	private ConfigController conf;
	
	private final TreeMap<String,String> internalFMmapping;

	public TreeMap<Pair,ComparisonResult> getResults() {
		return results;
	}

	public void addResult(ComparisonResult result) {
		this.results.put(result.getTableName(), result);
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
		sqlAccess = SQLAccessController.getInstance();
		conf = ConfigController.getInstance();
		results = new TreeMap<Pair, ComparisonResult>();
		
		internalFMmapping = new TreeMap<String,String>();
		internalFMmapping.put("DatierungMainAbstract", "Datierung");
		internalFMmapping.put("DatierungBefund", "Datierung");
	}
	
	/**
	 * only for importOtherFM
	 * fetch all columns for given table
	 * @param table
	 * @return list of columns for table
	 * @throws SQLException
	 */
	public ArrayList<String> fetchFMColumns(String table) throws SQLException {
		ArrayList<String> columns = new ArrayList<String>();
		
		if (!sqlAccess.isFMConnected()) {
			sqlAccess.connect();
		}
		
		ResultSet metaFM = sqlAccess.getFMTableMetaData(table);
		
		while (metaFM.next()) {
			String col = metaFM.getString("COLUMN_NAME");
			if (!columns.contains(col))
				columns.add(col);
		}
		return columns;
	}
	
	/**
	 * only for importOtherFM
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<String> fetchFMTables() throws SQLException {
		ArrayList<String> tables = new ArrayList<String>();
		
		if (!sqlAccess.isFMConnected()) {
			sqlAccess.connect();
		}
		
		System.out.println("IDENTIFIER QUOTE: "+sqlAccess.getFMDBMetaData().getIdentifierQuoteString());
		
		for (String table : sqlAccess.getLocalTables()) {
			if (!tables.contains(table))
				tables.add(table);
		}
		
		conf.setNumericFields(sqlAccess.fetchFMNumericFields(tables));
		conf.setTimestampFields(sqlAccess.fetchFMTimestampFields(tables));
		conf.setColPermissions(sqlAccess.fetchColPermissionsFM(tables));
		return tables;
	}
	
	/**
	 * method checks, if row with common fields is already in local DB
	 * 
	 * @param currTab Pair with current table
	 * @param pk 
	 * @param row remote row
	 * @return row without RAUID, TS, and PK, if row is already in remote db. empty list else.
	 * @throws SQLException
	 * @throws IOException
	 */
	public TreeMap<String,String> isRowOnLocal(String currTab, String pk, TreeMap<String,String> row)
			throws SQLException, IOException {
		String archerMSSkip = "";
		TreeMap<String,String> result = new TreeMap<String,String>();
		TreeMap<String,String> remoteRow = new TreeMap<String,String>(row);
		ArrayList<String> commonColumns = new ArrayList<String>();
		remoteRow.remove("ArachneEntityID");
		remoteRow.remove(pk);
		
		ArrayList<String> localCols = fetchFMColumns(currTab);
		Set<String> remoteCols = row.keySet();

		for (String rem : remoteCols) {
			String altRem = rem;
			if (rem.startsWith("[")) remoteRow.remove(rem);
			if (rem.contains("�") || rem.contains("�") || rem.contains("�")) remoteRow.remove(rem);
			if (rem.equals("Length")) altRem = "LengthSize";
			if (rem.equals("PS_PlaceID")) altRem = "PS_OrtID";
			if (rem.equals("PS_PlaceConnectionID")) altRem = "PS_OrtsbezugID";
			if (rem.equals("PS_XPlaceIDX")) altRem = "PS_XOrtIDX";
			
			if (localCols.contains(altRem)) {
				commonColumns.add(altRem);
				String val = remoteRow.get(rem);
				remoteRow.remove(rem);
				remoteRow.put(altRem, val);
			}
			else {
				remoteRow.remove(rem);
			}
		}
		
		if (currTab.equalsIgnoreCase("mainabstract")) {
			archerMSSkip = currTab + ".ImportSource!='Comprehensive Table' AND";
		}
		
		String select = "SELECT ";
		String sql = " FROM " + currTab + " WHERE " + archerMSSkip
				+ " ";

		Iterator<String> it = remoteRow.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String val = remoteRow.get(key);
			String longName = currTab+"."+key;
			key = "\""+key+"\"";
			select += key;
			
			if (!isNumericalField(longName)
					&& !isTimestampField(longName)
					&& val != null) {
				val = "'" + escapeCharsFM(val) + "'";
			}
			else if (isTimestampField(longName)
					&& val != null) {
				val = "TIMESTAMP '" + val;
			}
			
			if (val == null || val.equals("null"))
				sql += key + " IS NULL";
			else
				sql += key + "="+val;
			
			if (it.hasNext()) {
				select += ",";
				sql += " AND ";
			}
		}
		ResultSet r = sqlAccess.doFMQuery((select + sql));
		ResultSetMetaData rmd = r.getMetaData();
		while (r.next()) {
			for (int i = 1; i <= rmd.getColumnCount(); i++) {
				result.put(rmd.getColumnLabel(i), r.getString(i));
			}
		}
		return result;
	}
	
	public ArrayList<Pair> fetchCommonTables() throws SQLException {
		commonTables = new ArrayList<Pair>();

		HashSet<String> fmNames = sqlAccess.getLocalTables();
		HashSet<String> msNames = sqlAccess.getRemoteTables();

		if (!sqlAccess.areBothConnected()) {
			sqlAccess.connect();
		}
		
		ResultSet metaMS = sqlAccess.getMySQLDBMetaData();
		if (sqlAccess.areBothConnected()) {
			for (String ms : msNames) {
				for (String fm : fmNames) {
					if (ms.toLowerCase().equals(fm.toLowerCase())) {
						Pair t = new Pair(fm, ms);
						commonTables.add(t);
					}
					// IsolatedSherdMainAbstract maps to isolatedsherd
					if (fm.equals("IsolatedSherdMainAbstract") && ms.equals("isolatedsherd")) {
						Pair t = new Pair(fm, ms);
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
			if (sqlAccess.getLocalColumns(table).contains("lastModified")) {
				return true;
			}

			if (sqlAccess.doFMAlter("ALTER TABLE " + table
					+ " ADD lastModified TIMESTAMP")) {
				throw new FilemakerIsCrapException(
						"Column \"lastModified\" has been added into table \""+ table+ "\", but has still to be updated in FileMaker with the following script:\n"
								+ "SetzeVar (\n" + 
								"   [\n" + 
								"      trigger = HoleFeldwert ( \"\" ) ; // note the innovative use of GetField\n" + 
								"      ros = Hole ( DatensatzOffenStatus ) ;\n" + 
								"      ies = IstLeer ( Hole ( ScriptName ) ) ;\n" + 
								"      ts = LiesAlsZeitstempel( Hole ( SystemUhrzeitUTCMillisekunden )/1000)\n" + 
								"   ] ;\n" + 
								"   Falls (\n" + 
								"      ros = 1 ; \"\" ;\n" + 
								"         ros = 2 UND ies = 1 ; ts ; Selbst\n" + 
								"   )\n" + 
								")\n");
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
			if (sqlAccess.getLocalColumns(table).contains("lastRemoteTS")) {
				return true;
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
			if (sqlAccess.getLocalColumns(table).contains("ArachneEntityID")) {
				return true;
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
	
	public String performLocalDBTest() throws SQLException, FilemakerIsCrapException {
		ArrayList<String> tables = new ArrayList<String>();
		String lastModifiedScript = "";
		try {
			for (Pair table : commonTables) {
				tables.add(table.getFMString());
				addAUIDField(table.getFMString());
				try {
					addLastModifiedField(table.getFMString());
				} catch (FilemakerIsCrapException e) {
					lastModifiedScript = e.getMessage();
				}
				addLastRemoteTSField(table.getFMString());
			}
			sqlAccess.fetchColPermissionsFM(tables);
		} catch (SQLException | IOException e) {
			return null;
		}
		return lastModifiedScript;
	}
	
	/**
	 * method to examine the correlation between local and remote timestamps. timestamps may be null.
	 * @param rts remote timestamp
	 * @param lts local timestamp
	 * @param lrts last remote timestamp, saved locally
	 * @return 0, if all equal. 1: update to remote. 2: update to local. 3: conflict. 4: no local timestamp. -1 else.
	 */
	private int examineTimestamps (Timestamp rts, Timestamp lts, Timestamp lrts) {
		// must not be null.
		if (rts != null) {
			// can be null, e.g. for new and unmodified local entries.
			if (lts != null) {
				if (lrts != null) {
					// all same, skip.
					if (rts.equals(lts) && lts.equals(lrts)) return 0;
					// local after remote and lrts, update to remote.
					if (lts.after(rts) && rts.equals(lrts)) return 1;
					// remote after local and lrts >= local and remote >= lrts, update to local.
					if (rts.after(lts) &&
							( (lts.before(lrts) || lts.equals(lrts)) // local <= last remote
									&& (lrts.before(rts) || lrts.equals(rts)) ) ) return 2; // last remote <= remote
					// local after lrts and remote after lrts, changes on both sides, conflict.
					if (lts.after(lrts) && rts.after(lrts)) return 3;
				}
				// no last remote TS, then just check local and remote TS.
				else {
					if (rts.equals(lts)) return 0;
					else return 3;
				}
			}
			// no local timestamp.
			else {
				return 4;
			}
		}
		return -1;
	}
	
	/**
	 * method to examine the correlation between local and remote timestamps. timestamps may be null.
	 * @param rTS remote timestamp string
	 * @param lTS local timestamp string
	 * @param lrTS last remote timestamp string, saved locally
	 * @return 0, if all equal. 1: update to remote. 2: update to local. 3: conflict. 4: no local timestamp. -1 else.
	 */
	private int examineTimestamps(String rTS, String lTS, String lrTS) {
		Timestamp rts = rTS == null ? null : Timestamp.valueOf(rTS);
		Timestamp lts = lTS == null ? null : Timestamp.valueOf(lTS);
		Timestamp lrts = lrTS == null ? null : Timestamp.valueOf(lrTS);
		
		return examineTimestamps(rts, lts, lrts);
	}
	
	public ComparisonResult calcDiff (Pair currTab, boolean upload,
			boolean download) throws SQLException, FilemakerIsCrapException,
			SyncException, EntityManagementException, IOException {
		
		results.remove(currTab);
		
		ComparisonResult result = new ComparisonResult(currTab);
		TreeSet<String> commonFields = new TreeSet<String>();
		
		String fmpk = "";
		String mspk = "";
		
		// table with uuid and ID
		TreeBasedTable<Integer,Integer,TreeMap<String,String>> local = TreeBasedTable.create();
		TreeBasedTable<Integer,Integer,TreeMap<String,String>> remote = TreeBasedTable.create();		
		
		if (sqlAccess.areBothConnected()) {
			
			ResultSet fmColumnMeta = sqlAccess.getFMColumnMetaData(currTab.getLeft());
			ResultSet msColumnMeta = sqlAccess.getMySQLColumnMetaData(currTab.getRight());
			result.setFmColumns(fmColumnMeta);
			result.setMsColumns(msColumnMeta);
			
			commonFields = getCommonFields(currTab, fmColumnMeta, msColumnMeta);
			commonFields.add("ArachneEntityID");
			commonFields.add("lastModified");
			result.setCommonFields(commonFields);
			
			fmpk = getActualPrimaryKey((currTab.getLeft()));
			mspk = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
			
//			if (!fmpk.equalsIgnoreCase(mspk)) throw new EntityManagementException("Primary key fields in table "+currTab.getFMString()+" do not have the same name. Cannot synchronize!");
			
			// pk, map
			TreeMap<Integer, TreeMap<String, String>> remotePKs = getRemoteSyncInfo(currTab, mspk);	
			
			//DEBUG
//			sqlAccess.doFMUpdate("UPDATE "+currTab.getLeft() + " SET ArachneEntityID=null,lastModified=null,lastRemoteTS=null");
			
			String sqlCommonFields = "";
			String sqlCommonFieldsFM = "";
			
			Iterator<String> it = commonFields.iterator();
			while (it.hasNext()) {
				String next = it.next();
				if (next.equals("ArachneEntityID")) continue;
				if (!it.hasNext()) {
					if (!next.equals("lastModified"))
						sqlCommonFields += currTab.getRight()+".";
					sqlCommonFields += next;
					sqlCommonFieldsFM += currTab.getLeft()+"."+next;
				}
				else{
					if (!next.equals("lastModified"))
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

			String fmSQL = "SELECT ArachneEntityID, " + sqlCommonFieldsFM + ", "+currTab.getLeft()+".lastRemoteTS FROM " + currTab.getLeft()
					+ archerFMSkip;
			String msSQL = "SELECT arachneentityidentification.ArachneEntityID, arachneentityidentification.isDeleted, arachneentityidentification.ForeignKey,arachneentityidentification.lastModified AS deletedTS,"
					+ sqlCommonFields.replace("lastModified", "DATE_FORMAT(CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00'),'%Y-%m-%d %H:%i:%s') as lastModified")
					+ " FROM arachneentityidentification"
					// left join includes deleted or empty entries
					+ " LEFT JOIN " + currTab.getRight()
					+ " ON arachneentityidentification.ForeignKey = "
					+ currTab.getRight() + "." + sqlAccess.getMySQLTablePrimaryKey(currTab.getRight())
					+ " WHERE arachneentityidentification.TableName=\""
					+ currTab.getRight() + "\"" + archerMSSkip;

			// get only common fields from filemaker
			ResultSet filemaker = sqlAccess.doFMQuery(fmSQL);
			// A-AUID + rest of table
			ResultSet mysql = sqlAccess.doMySQLQuery(msSQL);
			
			while (filemaker.next()) {
				TreeMap<String,String> row = new TreeMap<String,String>();
				for (String field : commonFields) {
					row.put(field, filemaker.getString(field));
				}
				row.put("lastRemoteTS", filemaker.getString("lastRemoteTS"));
				int uuID = filemaker.getInt("ArachneEntityID");
				int lID = filemaker.getInt(fmpk);
				local.put(uuID, lID, row);
			}
			while (mysql.next()) {
				TreeMap<String,String> row = new TreeMap<String,String>();
				for (String field : commonFields) {
					row.put(field, mysql.getString(field));
				}
				row.put("isDeleted", mysql.getString("isDeleted"));
				row.put("ForeignKey", mysql.getString("ForeignKey"));
				
				remote.put(mysql.getInt("ArachneEntityID"), mysql.getInt("ForeignKey"), row);
			}
		}
		
		Iterator<Cell<Integer, Integer, TreeMap<String, String>>> it = remote.cellSet().iterator();
		
		while (it.hasNext()) {
			Cell<Integer, Integer, TreeMap<String, String>> cell = it.next();
			
			int uid = cell.getRowKey();
			int rID = cell.getColumnKey();
			TreeMap<String, String> remoteRow = cell.getValue();
			boolean isDeletedRemotely = remoteRow.get("isDeleted").equals("1") ? true : false;
			
			/**
			 *  RUID == LUID
			 */
			if (local.containsRow(uid)) {
				TreeMap<String, String> localRow = local.get(uid, rID);
				
				if (localRow == null) {
					throw new EntityManagementException("Your local database seems to be corrupted (UUID not the same as in remote DB). Please re-download the whole database!");
				}
				
				int lID = Integer.parseInt(localRow.get(fmpk));
				
				if (isDeletedRemotely) {
					localRow.remove("lastRemoteTS");
					remoteRow.remove("ForeignKey");
					remoteRow.remove("isDeleted");
					remoteRow.put(mspk, rID+"");
					result.addToDeleteList(localRow, remoteRow);
					it.remove();
					local.remove(uid, lID);
					continue;
				}
			
				String currRTS = remoteRow.get("lastModified"); // current arachne timestamp
				String currLTS = localRow.get("lastModified"); // current fm timestamp
				String currLRTS = localRow.get("lastRemoteTS"); // last remote timestamp saved in fm
				
				localRow.remove("lastRemoteTS");
				remoteRow.remove("ForeignKey");
				remoteRow.remove("isDeleted");
				
				int timestampResult = examineTimestamps(currRTS, currLTS, currLRTS);
				// timestamps all equal: remote and SKIP
				if (timestampResult == 0) {
					local.remove(uid, lID);
					it.remove();
					continue;
				}
				
				TreeMap<String, String> diffs = new TreeMap<String,String>();
				
				switch (timestampResult) {
				// local after remote, remote did not change: UPDATE REMOTELY
				case 1:
					for (String field : commonFields) {
						if (field.equals("lastModified")) continue;
						String rVal = remoteRow.get(field);
						String lVal = localRow.get(field);
						if (rVal != null && lVal != null && !rVal.equals(lVal))
							diffs.put(field, lVal);
						else if (rVal == null && lVal != null || lVal == null && rVal != null)
							diffs.put(field, lVal);
					}
					diffs.put("lastModified", currLTS);
					result.addToUpdateList(remoteRow, diffs, false);
					it.remove();
					local.remove(uid, lID);
					continue;
				// remote after local AND (local <= last remote <= remote): UPDATE LOCALLY
				case 2:
					for (String field : commonFields) {
						if (field.equals("lastModified")) continue;
						String rVal = remoteRow.get(field);
						String lVal = localRow.get(field);
						if (rVal != null && lVal != null && !rVal.equals(lVal))
							diffs.put(field, rVal);
						else if (rVal == null && lVal != null || lVal == null && rVal != null)
							diffs.put(field, rVal);
					}
					diffs.put("lastModified", currRTS);
					diffs.put("lastRemoteTS", currRTS);
					result.addToUpdateList(localRow, diffs, true);
					it.remove();
					local.remove(uid, lID);
					continue;
				// local after last remote AND remote after last remote: CONFLICT
				case 3: case 4:
					if (compareFields(commonFields, localRow, remoteRow, currTab) != 0) {
						result.addToConflictList(localRow, remoteRow);
					}
					else {
						diffs.put("lastModified", currRTS);
						diffs.put("lastRemoteTS", currRTS);
						result.addToUpdateList(localRow, diffs, true);
					}
					it.remove();
					local.remove(uid, lID);
					continue;
				}
			}
			
			/**
			 * LUID == NULL and RID == LID
			 */
			else if (local.contains(0, rID)) {
				TreeMap<String, String> localRow = local.get(0, rID);
				
				String currRTS = remoteRow.get("lastModified"); // current arachne timestamp
				String currLTS = localRow.get("lastModified"); // current fm timestamp
				String currLRTS = localRow.get("lastRemoteTS"); // last remote timestamp saved in fm
				
				localRow.remove("lastRemoteTS");
				remoteRow.remove("ForeignKey");
				remoteRow.remove("isDeleted");
				
				if (isDeletedRemotely) {
					remoteRow.put(mspk, rID+"");
					result.addToDeleteList(localRow, remoteRow);
					it.remove();
					local.remove(0, rID);
					continue;
				}
				
				TreeMap<String, String> remoteDiffs = new TreeMap<String,String>();
				TreeMap<String, String> localDiffs = new TreeMap<String,String>();
								
				int timestampResult = examineTimestamps(currRTS, currLTS, currLRTS);
				
				switch (timestampResult) {
				// local after remote, remote did not change: UPDATE REMOTELY
				case 1:
					for (String field : commonFields) {
						String rVal = remoteRow.get(field);
						String lVal = localRow.get(field);
						if (rVal != null && lVal != null && !rVal.equals(lVal))
							remoteDiffs.put(field, lVal);
						else if (rVal == null && lVal != null || lVal == null && rVal != null)
							remoteDiffs.put(field, lVal);
					}
					result.addToUpdateList(remoteRow, remoteDiffs, false);
					localDiffs.put("lastModified", currRTS);
					localDiffs.put("lastRemoteTS", currRTS);
					localDiffs.put("ArachneEntityID", ""+uid);
					result.addToUpdateList(localRow, localDiffs, true);
					break;
				// remote after local AND (local <= last remote <= remote): UPDATE LOCALLY
				case 2:
					for (String field : commonFields) {
						String rVal = remoteRow.get(field);
						String lVal = localRow.get(field);
						if (rVal != null && lVal != null && !rVal.equals(lVal))
							localDiffs.put(field, rVal);
						else if (rVal == null && lVal != null || lVal == null && rVal != null)
							localDiffs.put(field, rVal);
					}
					localDiffs.put("lastModified", currRTS);
					localDiffs.put("lastRemoteTS", currRTS);
					localDiffs.put("ArachneEntityID", ""+uid);
					result.addToUpdateList(localRow, localDiffs, true);
					break;
				// local after last remote AND remote after last remote: CONFLICT
				// alternative: no local timestamps.
				case 3: case 4:
					if (compareFields(commonFields, localRow, remoteRow, currTab) != 0)
						result.addToConflictList(localRow, remoteRow);
					break;
				}
				
				it.remove();
				local.remove(0, rID);
				continue;
			}
			/**
			 * LID == NULL and RID != NULL
			 */
			else if (!local.containsColumn(rID)) {
				
				if (!isDeletedRemotely) {
					remoteRow.remove("ForeignKey");
					remoteRow.remove("isDeleted");
					result.addToDeleteOrDownloadList(remoteRow);
				}
				it.remove();
				continue;
			}
		}
		
		it = local.cellSet().iterator();
		
		while (it.hasNext()) {
			Cell<Integer, Integer, TreeMap<String, String>> cell = it.next();

			int uid = cell.getRowKey();
			int lID = cell.getColumnKey();
			TreeMap<String, String> localRow = cell.getValue();
			
			/**
			 *  RUID == LUID
			 */
			if (!remote.containsColumn(lID)) {
				//TODO vllt ts, uid updaten?!
				localRow.remove("lastRemoteTS");
				result.addToUploadList(localRow);
				it.remove();
				continue;
			}
		}
		
		results.put(currTab, result);
		return result;
	}
	
	/**
	 * retrieves all sync relevant information for the given table from remote, e.g. UUID and timestamp
	 *  
	 * @param currTab the given table
	 * @param mspk the private key name
	 * @return Map of maps with integer primary key and the relevant fields as string maps
	 * @throws SQLException
	 */
	private TreeMap<Integer, TreeMap<String, String>> getRemoteSyncInfo(Pair currTab, String mspk) throws SQLException {
		TreeMap<Integer, TreeMap<String, String>> list = new TreeMap<Integer, TreeMap<String, String>>();
		String tab = currTab.getRight();
		ResultSet r = sqlAccess.doMySQLQuery("SELECT "+tab+"."+mspk+", arachneentityidentification.ArachneEntityID, "+tab+".lastModified FROM "+tab
				+ " JOIN arachneentityidentification on arachneentityidentification.ForeignKey = " + tab+"."+mspk 
				+ " WHERE arachneentityidentification.TableName='"+tab+"'");
		while (r.next()) {
			TreeMap<String, String> tmp = new TreeMap<String,String>();
			Integer pk = r.getInt(mspk);
			String lm = r.getString("lastModified");
			String uid = r.getString("ArachneEntityID");
			tmp.put("lastModified", lm);
			tmp.put("ArachneEntityID", uid);
			list.put(pk, tmp);
		}
		return list;
	}

	int compareFields(TreeMap<String, String> rowLocal,
			TreeMap<String, String> rowRemote, Pair currTab) throws SQLException, IOException, NumberFormatException, FilemakerIsCrapException {

		TreeSet<String> commonFields = new TreeSet<String>();
		//calculate common fields
		for (String key : rowRemote.keySet()) {
			if (rowLocal.containsKey(key)) {
				commonFields.add(key);
			}
		}
		return compareFields(commonFields, rowLocal, rowRemote, currTab);
	}

	/**
	 * prepare row packs of size x to download into local db
	 * @param currTab
	 * @param vector
	 * @param packSize
	 * @return
	 * @throws SQLException
	 * @throws EntityManagementException
	 * @throws IOException
	 * @throws MissingPermissionException 
	 */
	public boolean prepareRowsAndDownload(Pair currTab,
			Vector<TreeMap<String, String>> vector, int packSize) throws SQLException,
			EntityManagementException, IOException, MissingPermissionException {
		int count = vector.size() / packSize;
		int mod = vector.size() % packSize;
		boolean result = true;
		for (int k = 0; k < count; k++) {
			result = result && insertRowsIntoLocal(currTab,new Vector<TreeMap<String,String>>(vector.subList(k * packSize,
									(k + 1) * packSize)));
		}
		result = result && insertRowsIntoLocal(currTab, new Vector<TreeMap<String,String>>(
				vector.subList(count * packSize, count * packSize + mod)));
		return result;
	}
	
	/**
	 * inserts single row.
	 * @param currTab table as string
	 * @param pk 
	 * @param row row as string treemap
	 * @return integer value of new generated pk
	 * @throws SQLException
	 * @throws IOException
	 * @throws EntityManagementException
	 * @throws FilemakerIsCrapException
	 * @throws MissingPermissionException 
	 */
	public int insertRowIntoLocal(String currTab, String pk, TreeMap<String, String> row) throws SQLException, IOException, EntityManagementException, FilemakerIsCrapException, MissingPermissionException {
		Vector<TreeMap<String, String>> v = new Vector<TreeMap<String,String>>();
		int lastID = sqlAccess.getLastIndexFM(currTab, pk);
		row.put(pk, ""+(lastID+1));
		
		v.add(row);
		if (!insertRowsIntoLocal(new Pair(currTab,null), v)) {
			throw new SQLException("Row could not be inserted into local db!");
		}
		
		return lastID+1;
	}
	
	private boolean insertRowsIntoLocal(Pair currTab, Vector<TreeMap<String, String>> vector) throws SQLException, IOException, EntityManagementException, MissingPermissionException {
		if (vector.size() == 0)
			return true;
		
		String sql = "INSERT INTO \"" + currTab.getFMString() + "\" (";
		String vals = " VALUES (";
		// iterate through rows
		for (int i = 0; i < vector.size(); i++) {
			
			TreeMap<String,String> currRow = vector.get(i);
			// do all fields have the insert privilege? 
			if (i == 0) {
				Set<String> keys = new HashSet<String>(currRow.keySet());
				for (String key : keys) {
					// kick out those who don't
					if (!sqlAccess.isColInsertable(currTab.getFMString(), key)) currRow.remove(key);
					String altKey = key;
					if (key.startsWith("[")) currRow.remove(key);
					if (key.equals("PS_GLOBALAbstractID")) currRow.remove(key);
					if (key.contains("�") || key.contains("�") || key.contains("�")) currRow.remove(key);
					if (key.equals("Length")) altKey = "LengthSize";
					if (key.equals("PS_PlaceID")) altKey = "PS_OrtID";
					if (key.equals("PS_PlaceConnectionID")) altKey = "PS_OrtsbezugID";
					if (key.equals("PS_XPlaceIDX")) altKey = "PS_XOrtIDX";
					
					if (!altKey.equals(key)) {
						String val = currRow.get(key);
						currRow.remove(key);
						currRow.put(altKey, val);
					}
				}
				if (currRow.isEmpty()) throw new MissingPermissionException("Cannot write to any local db field in table "+currTab.getFMString()+"!");
			}
			
			Iterator<String> it = currRow.keySet().iterator();
			// ( col1, col2, col3, ..)
			while (it.hasNext()) {
				String currFieldName = it.next();
				String longName = currTab.getFMString() + "." + currFieldName;
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
		// unfortunately, filemaker is crap and cannot return generated keys.
		sqlAccess.doFMInsert(sql + vals);
		return true;
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
	 * @throws FilemakerIsCrapException 
	 */
	public Vector<Integer> prepareRowsAndUpload(Pair currTab, Vector<TreeMap<String, String>> rows, int packSize) throws SQLException,
			EntityManagementException, IOException, FilemakerIsCrapException {
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
	 * @throws FilemakerIsCrapException 
	 */
	private ArrayList<Integer> insertRowsIntoRemote(Pair currTab,
			Vector<TreeMap<String, String>> vector) throws SQLException,
			EntityManagementException, IOException, FilemakerIsCrapException {
		
		if (vector.size() == 0)
			return null;
		
		String sql = "INSERT INTO " + currTab.getRight() + " (";
		String vals = " VALUES (";
		String keyField = sqlAccess.getMySQLTablePrimaryKey(currTab.getMySQLString());
		ArrayList<Integer> localIDs = new ArrayList<Integer>();
		
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
		else return new ArrayList<Integer>();
		
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
				
				if (currFieldName.equalsIgnoreCase(keyField)) {
					if (currFieldVal != null) localIDs.add(Integer.parseInt(currFieldVal));
					else throw new FilemakerIsCrapException("Primary key field "+keyField+" in table " +currTab.getFMString()+" is NULL!");
				}
					
				
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
		sqlAccess.doMySQLInsert(sql + vals);
		ArrayList<Integer> result = new ArrayList<Integer>();
		String ids = "";
		int proof = 0;

		for (int i = 0; i < localIDs.size(); i++) {
			if (i > 0) ids += " OR ";
			ids += currTab.getRight()+"."+keyField + "=" + localIDs.get(i);
		}
		
		// get remote AUID and TS
		ResultSet id = sqlAccess.doMySQLQuery("SELECT arachneentityidentification.ArachneEntityID,"
					+ currTab.getRight() + "." + keyField + ","
					+ "CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00') as lastModified "
					+ " FROM arachneentityidentification JOIN "
					+ currTab.getRight()
					+ " ON arachneentityidentification.ForeignKey="
					+ currTab.getRight() + "." + keyField
					+ " AND arachneentityidentification.TableName='"
					+ currTab.getRight() + "'" 
					+ (ids.equals("") ? ";" : " WHERE " + ids + ";"));
		while (id.next()) {
			proof++;
			int RAUID = id.getInt("ArachneEntityID");
			int lID =  id.getInt(keyField);
			Timestamp ts = id.getTimestamp("lastModified");
			
			if (!updateLocalUIDAndTS(currTab, RAUID, ts, keyField, lID))
				throw new EntityManagementException("Updating local AUID "
						+ id.getInt(1) + " and timestamp in table " + currTab.getLeft()
						+ " FAILED!");
			else {
				logger.debug("Updated local entry "+lID+"; added UUID " + RAUID
						+ " and TS in table " + currTab.getLeft()
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
				.replace("%", "%");
	}
	
	private String escapeCharsFM(String currFieldVal) throws IOException {
		return currFieldVal.replace("'", "''").replace("\"", "\\\"")
				.replace("%", "%");
	}

	private boolean updateLocalUIDAndTS(Pair currTab, int currRAUID,
			Timestamp ts, String pkName, int pkVal)
			throws SQLException {
		
		LocalDateTime ldt = ts.toLocalDateTime();
		
		// locate row by local ID
		if (sqlAccess.doFMUpdate("UPDATE \"" + currTab.getLeft()
				+ "\" SET ArachneEntityID=" + currRAUID
				+ ", lastModified={ts '" + ldt.format(formatTS) + "'}"
				+ ", lastRemoteTS={ts '" + ldt.format(formatTS) + "'}"
				+ " WHERE " + pkName + "=" + pkVal) != null) {
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
	 * @throws FilemakerIsCrapException 
	 * @throws NumberFormatException 
	 */
	private int compareFields(TreeSet<String> commonFields, TreeMap<String, String> rowFM, TreeMap<String, String> rowMS,
			Pair currTab) throws SQLException, IOException, NumberFormatException, FilemakerIsCrapException {
		
//		if (rowMS.size() != rowFM.size()) {
//			throw new IllegalArgumentException("trying to compare rows with different field count");
//		}
		
//		if (!rowMS.keySet().equals(rowFM.keySet())) return -1;
		
		int res = 1;
		boolean localAllNull = true; // empty local fields
		boolean remoteAllNull = true; // empty remote fields
		boolean allSame = true;

		for (String currField : commonFields) {

			// skip UID field, this is not equal anyway
			if (currField.equalsIgnoreCase("ArachneEntityID")
					|| currField.equalsIgnoreCase("lastModified"))
				continue;

			// Strings to compare in field with equal name
			String msVal = rowMS.get(currField);
			String fmVal = rowFM.get(currField);

			msVal = msVal == null ? "" : msVal;
			fmVal = fmVal == null ? "" : fmVal;

			if (!msVal.isEmpty())
				remoteAllNull = false;
			if (!fmVal.isEmpty())
				localAllNull = false;

			if (!(msVal.equalsIgnoreCase(fmVal))) {

				if (isFMPrimaryKey(currTab.getFMString(), currField)) {

					// if local ID is 0, then entry is NULL on each field.
					if (msVal.isEmpty())
						return 3;
					if (fmVal.isEmpty())
						return 2;

					System.out.println("INDEX SHIFTING!! LocalID: " + fmVal
							+ ", RemoteID: " + msVal);
					logger.debug("INDEX SHIFTING!! LocalID: " + fmVal
							+ ", RemoteID: " + msVal);

					int fID = Integer.parseInt(fmVal);
					int mID = Integer.parseInt(msVal);

					// missing entry in remote db
					if (fID < mID) // offset < 0
						return 5;
					// missing entry in local db
					else // offset > 0
						return 6;
				}

				// numeric fields can differ as strings, e.g. 9.0 and 9.00
				if (isNumericalField(currTab.getFMString()+"."+currField)) {
					// parsing empty string raises exc
					if (!msVal.isEmpty() && !fmVal.isEmpty()) { 
						try {
							double m = Double.parseDouble(msVal);
							double f = Double.parseDouble(fmVal);
							if (m == f) {
								continue;
							}
						} catch (NumberFormatException e) {
							logger.error("NFException! " + e);
						}
					}
				}
				if (isTimestampField(currTab.getFMString()+"."+currField)) {
					// parsing empty string raises exc
					if (!msVal.isEmpty() && !fmVal.isEmpty()) { 
						try {
							Timestamp m = Timestamp.valueOf(msVal);
							Timestamp f = Timestamp.valueOf(fmVal);
							if (m.equals(f)) {
								continue;
							}
						} catch (IllegalArgumentException e) {
							logger.error("NFException! " + e);
						}
					}
				}
				allSame = false;
//				System.out.println(currField + " in " + currTab
//						+ " not equal: \"" + fmVal + "\" (FM) and \"" + msVal
//						+ "\" (MS)");
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
	 * @throws FilemakerIsCrapException 
	 * @throws SQLException 
	 */
	private boolean isFMPrimaryKey(String currTab, String field) throws FilemakerIsCrapException, SQLException {
		return getActualPrimaryKey(currTab).equalsIgnoreCase(field);
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

	private boolean isNumericalField(String field) throws IOException {
		return conf.getNumericFields().contains(field);
	}

	private boolean isTimestampField(String field) throws IOException {
		for (Pair p : conf.getTimestampFields()) {
			if (p.getFMString().equalsIgnoreCase(field)) return true;
			if (p.getMySQLString().equalsIgnoreCase(field)) return true;
		}
		return false;
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
				sql += currFieldName + "='" + escapeChars(currFieldVal) + "'";
			}
			else if ((isTimestampField(longName) || currFieldName.equals("lastRemoteTS")) && currFieldVal != null) {
				sql += currFieldName + "={ts '" + currFieldVal + "'}";
			}
			else {
				sql += currFieldName + "=" + currFieldVal;
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
				sql += currFieldName + "='" + escapeChars(currFieldVal) + "'";
			}
			else if ((isTimestampField(longName) || currFieldName.equals("lastRemoteTS")) && currFieldVal != null) {
				sql += currFieldName + "=TIMESTAMP '" + currFieldVal + "'";
			}
			else if (currFieldVal == null) {
				sql += currFieldName + " IS NULL";
			}
			else {
				sql += currFieldName + "=" + currFieldVal;
			}
			if (it.hasNext()) sql += " AND ";
		}
		
		sqlAccess.doFMUpdate(sql);
		return true;
	}


	public boolean updateRowsRemotely(Pair currTab,
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> list) throws IOException, SQLException, FilemakerIsCrapException {
		boolean res = true;
		
		for (Tuple<TreeMap<String, String>, TreeMap<String, String>> row : list) {
			res = res && updateRowOnRemote(currTab, row);
		}
		return res;
	}

	private boolean updateRowOnRemote(Pair currTab, Tuple<TreeMap<String, String>, TreeMap<String, String>> tuple) throws IOException, SQLException, FilemakerIsCrapException {
		if (tuple.getLeft().size() == 0 || tuple.getRight().size() == 0)
			return false;
		
		String sql = "UPDATE " + currTab.getMySQLString() + " SET ";
		
		TreeMap<String, String> set = tuple.getRight();
		TreeMap<String, String> where = new TreeMap<String,String>(tuple.getLeft());
		
		// local UUID = null? update locally!
		if (set.containsKey("ArachneEntityID")) {
			if (set.get("ArachneEntityID") == null) {
				if (where.containsKey("ArachneEntityID")) {
					String keyField = getActualPrimaryKey(currTab.getFMString());
					Timestamp ts = where.get("lastModified") == null ? null : Timestamp.valueOf(where.get("lastModified"));
					updateLocalUIDAndTS(currTab, Integer.parseInt(where.get("ArachneEntityID")), ts, keyField, Integer.parseInt(where.get(keyField)));
				}
			}
		}
		
		set.remove("ArachneEntityID");
		
		Iterator<String> it = set.keySet().iterator();
		
		while (it.hasNext()) {
			String currFieldName = it.next();
			String longName = currTab.getMySQLString() + "." + currFieldName;
			String lookupName = currTab.getFMString() + "." + currFieldName;
			String currFieldVal = set.get(currFieldName);
			
			if (!isNumericalField(lookupName)
					&& !isTimestampField(lookupName)
					&& !currFieldName.equals("lastRemoteTS")
					&& currFieldVal != null) {
				sql += longName + "=" + "'" + escapeChars(currFieldVal) + "'";
			}
			else if ((isTimestampField(lookupName) || currFieldName.equals("lastRemoteTS")) && currFieldVal != null) {
				sql += longName + "=" + "TIMESTAMP '" + currFieldVal + "'";
			}
			else {
				sql += longName + "=" + currFieldVal;
			}
			if (it.hasNext()) sql += ",";
		}
		// ArachneEntityID is not in same table
		where.remove("ArachneEntityID");
		// lastModified: problem with timezone...
		where.remove("lastModified");
		
		it = where.keySet().iterator();
		sql += " WHERE ";
		while (it.hasNext()) {
			String currFieldName = it.next();
			String longName = currTab.getMySQLString() + "." + currFieldName;
			String lookupName = currTab.getFMString() + "." + currFieldName;
			String currFieldVal = where.get(currFieldName);
			
			if (!isNumericalField(lookupName)
					&& !isTimestampField(lookupName)
					&& !currFieldName.equals("lastRemoteTS")
					&& currFieldVal != null) {
				sql += longName + "=" + "'" + escapeChars(currFieldVal) + "'";
			}
			else if (isTimestampField(lookupName) && currFieldVal != null) {
				sql += longName + "=TIMESTAMP '" + currFieldVal + "'";
			}
			else {
				sql += longName + "=" + currFieldVal;
			}
			if (it.hasNext()) sql += " AND ";
		}
		
		sqlAccess.doMySQLUpdate(sql);
		return true;
	}

	public boolean deleteRows(Pair currTab, boolean local, Vector<TreeMap<String, String>> delList, int packSize) throws SQLException, IOException, FilemakerIsCrapException {
		if (delList.size() == 0)
			return true;
		
		String pk = local ? getActualPrimaryKey(currTab.getFMString()) : sqlAccess.getMySQLTablePrimaryKey(currTab.getMySQLString());
		String sql = " WHERE " + pk + "=";
		
		Iterator<TreeMap<String, String>> it = delList.iterator();
		
		while (it.hasNext()) {
			TreeMap<String, String> map = it.next();
			if (map == null) continue;
			
			if (map.containsKey(pk)) {
				// empty primary key?!
				if (map.get(pk) == null || map.get(pk).isEmpty())
					return true;
				sql += map.get(pk);
				if (it.hasNext()) {
					sql += " OR " + pk + "=";
				}
			}
			else
				throw new SQLException("Primary key "+pk+ " in "+(local?"local":"remote")+" table "+currTab+" not found!");
		}
		
		if (!sql.endsWith("=")) {
			if (local)
				sqlAccess.doFMUpdate("DELETE FROM " + currTab.getFMString() + sql);
			else
				sqlAccess.doMySQLUpdate("DELETE FROM " + currTab.getMySQLString() + sql);
		}
		return true;
	}

	public ComparisonResultImg calcImgDiff() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getActualPrimaryKey(String currTab) throws FilemakerIsCrapException, SQLException {
		TreeSet<String> pks = sqlAccess.getFMTablePrimaryKey(currTab);
		if (pks.size() == 1) return pks.first();
		else if (pks.size() <= 0) {
			logger.warn("No primary key in table "+currTab+"!");
			return "";
		}
		else {
			for (String key : pks) {
				if (key.toLowerCase().contains(currTab.toLowerCase())) return key;
			}
			throw new FilemakerIsCrapException("LALALALA");
		}
	}
	
	/**
	 * ONLY for import tool
	 * @param currTab
	 * @return
	 * @throws SQLException
	 * @throws FilemakerIsCrapException
	 * @throws IOException
	 */
	public TreeBasedTable<String, Integer, TreeMap<String, String>> getWholeFMTable(String currTab) throws SQLException, FilemakerIsCrapException, IOException {
		TreeBasedTable<String,Integer,TreeMap<String,String>> table = TreeBasedTable.create();
		
		if (currTab.equals("Wertelisten")) return table;
		
		String fmpk = getActualPrimaryKey(currTab);
		
		String fmSQL = "SELECT * FROM " + currTab +(fmpk.equals("")?"":(" WHERE \""+fmpk+"\" >= 100000"));
		
		// get only common fields from filemaker
		ResultSet filemaker = sqlAccess.doFMQuery(fmSQL);
		ResultSetMetaData meta = sqlAccess.getFMRSMetaData(filemaker);
		
		int rowCount = 0;
		while (filemaker.next()) {
			TreeMap<String,String> row = new TreeMap<String,String>();
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				String field = meta.getColumnName(i);
				if (!(field.startsWith("[") ||field.equals("lastModified") || field.equals("lastRemoteTS") || field.equals("ArachneEntityID"))) {
					row.put(field, filemaker.getString(field));
				}
				if (field.equals("Dateipfad"))
					row.put(field, filemaker.getString("[DateipfadRelativeExport]"));
				if (field.equals("Dateiname"))
					row.put(field, filemaker.getString("[Dateiname]"));
			}
			try {
				Integer pk = filemaker.getInt(fmpk);
				table.put(currTab, pk, row);
				rowCount++;
			} catch (SQLException e) {
				System.out.println("error while accessing pk "+fmpk +" in table "+currTab);
				break;
			}
		}
		return table;
	}
	
}
