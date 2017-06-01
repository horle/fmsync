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
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

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

	private Timestamp normaliseTS(Timestamp t) {
		return Timestamp.valueOf(t.toLocalDateTime().format(formatTS));
	}
	private Timestamp normaliseTS(String t) {
		LocalDateTime bla = LocalDateTime.parse(t);
		return Timestamp.valueOf(bla.format(formatTS));
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

	public boolean resetResults() {
		results.clear();
		return true;
	}
	
	private boolean performLocalTest() {
		return true; //TODO
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
		
		for (ComparisonResult c : results) {
			if (c.getTableName().equals(currTab)) {
				results.remove(c);
				break;
			}
		}
		ComparisonResult result = new ComparisonResult(currTab);
		TreeSet<String> commonFields = new TreeSet<String>();
		
		String fmpk = "";
		String mspk = "";
		
		TreeBasedTable<Integer,Integer,TreeMap<String,String>> local = TreeBasedTable.create();
		TreeBasedTable<Integer,Integer,TreeMap<String,String>> remote = TreeBasedTable.create();		
		
		if (sqlAccess.isMySQLConnected() && sqlAccess.isFMConnected()) {
			
			ResultSet fmColumnMeta = sqlAccess.getFMColumnMetaData(currTab.getLeft());
			ResultSet msColumnMeta = sqlAccess.getMySQLColumnMetaData(currTab.getRight());
			result.setFmColumns(fmColumnMeta);
			result.setMsColumns(msColumnMeta);
			
			commonFields = getCommonFields(currTab, fmColumnMeta, msColumnMeta);

			// test, if fields are available in FM
			addAUIDField(currTab.getLeft());
			addLastModifiedField(currTab.getLeft());
			addLastRemoteTSField(currTab.getLeft());
			commonFields.add("ArachneEntityID");
			commonFields.add("lastModified");
			result.setCommonFields(commonFields);
			
			fmpk = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
			mspk = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
			// pk, map
			TreeMap<Integer, TreeMap<String, String>> remotePKs = getRemoteSyncInfo(currTab, mspk);	
			
			//DEBUG
//			sqlAccess.doFMUpdate("UPDATE "+currTab.getLeft() + " SET ArachneEntityID=null,lastModified=null,lastRemoteTS=null");
			
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
				archerMSSkip = " WHERE " + currTab.getRight()
						+ ".ImportSource!='Comprehensive Table'";
				archerFMSkip = " WHERE " + currTab.getLeft()
						+ ".ImportSource!='Comprehensive Table'";
			}

			String fmSQL = "SELECT " + sqlCommonFieldsFM + ", "+currTab.getLeft()+".lastRemoteTS FROM " + currTab.getLeft()
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
				
				local.put(filemaker.getInt("ArachneEntityID"), filemaker.getInt(fmpk), row);
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
						it.remove();
						local.remove(uid, lID);
					}
					else {
						diffs.put("lastModified", currRTS);
						diffs.put("lastRemoteTS", currRTS);
						result.addToUpdateList(localRow, diffs, true);
						it.remove();
						local.remove(uid, lID);
					}
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
					break;
				// local after last remote AND remote after last remote: CONFLICT
				case 3: case 4:
					if (compareFields(commonFields, localRow, remoteRow, currTab) != 0)
						result.addToConflictList(localRow, remoteRow);
					break;
				}
				// in each case update local info
				localDiffs.put("lastModified", currRTS);
				localDiffs.put("lastRemoteTS", currRTS);
				localDiffs.put("ArachneEntityID", ""+uid);
				result.addToUpdateList(localRow, localDiffs, true);
				
				it.remove();
				local.remove(0, rID);
				continue;
			}
			/**
			 * LID == NULL and RID != NULL
			 */
			else if (!local.containsColumn(rID)) {
				String currRTS = remoteRow.get("lastModified"); // current arachne timestamp
				TreeMap<String, String> localRow = new TreeMap<String,String>();
				for (String field : commonFields)
					localRow.put(field, null);
				
				if (!isDeletedRemotely) {
					remoteRow.remove("ForeignKey");
					remoteRow.remove("isDeleted");
					result.addToDeleteOrDownloadList(localRow, remoteRow);
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
		
		results.add(result);
		return result;
	}
	
	public ComparisonResult calcDiffResultSet (Pair currTab, boolean upload,
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
			
			TreeSet<Integer> handledUIDs = new TreeSet<Integer>();
			TreeSet<Integer> handledLocalIDs = new TreeSet<Integer>();
			TreeSet<String> commonFields = getCommonFields(currTab, fmColumnMeta, msColumnMeta);

			// test, if fields are available in FM
			addAUIDField(currTab.getLeft());
			addLastModifiedField(currTab.getLeft());
			addLastRemoteTSField(currTab.getLeft());
			commonFields.add("ArachneEntityID");
			commonFields.add("lastModified");
			result.setCommonFields(commonFields);
			
			String fmpk = sqlAccess.getFMTablePrimaryKey(currTab.getLeft());
			String mspk = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
			// pk, map
			TreeMap<Integer, TreeMap<String, String>> remotePKs = getRemoteSyncInfo(currTab, mspk);	
			
			//DEBUG
//			sqlAccess.doFMUpdate("UPDATE "+currTab.getLeft() + " SET ArachneEntityID=null,lastModified=null,lastRemoteTS=null");
			
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
			String msSQL = "SELECT arachneentityidentification.ArachneEntityID, arachneentityidentification.isDeleted, arachneentityidentification.ForeignKey,arachneentityidentification.lastModified AS deletedTS,"
					+ sqlCommonFields.replace("lastModified", "DATE_FORMAT(CONVERT_TZ(" + currTab.getRight() + ".`lastModified`, @@session.time_zone, '+00:00'),'%Y-%m-%d %H:%i:%s') as lastModified")
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
					+ "ArachneEntityID IS NOT NULL"
					+ " ORDER BY ArachneEntityID");
			// A-AUID + rest of table
			ResultSet mNotNull = sqlAccess.doMySQLQuery(msSQL
					+ " AND ArachneEntityID IS NOT NULL"
					+ " ORDER BY ArachneEntityID");
			
			try {
				while (fNotNull.next() & mNotNull.next()) {
					// entities:
					int currRAUID = mNotNull.getInt("ArachneEntityID"); // current arachne uid in arachne
					int currLAUID = fNotNull.getInt("ArachneEntityID"); // current arachne uid in fm
					// to check, if deleted
					boolean isDeletedRemotely = mNotNull.getInt("isDeleted") == 1 ? true : false;
					int currRemoteID = mNotNull.getInt("ForeignKey");
					int currLocalID = fNotNull.getInt(fmpk);

					if (isDeletedRemotely && currRemoteID == currLocalID) {
						TreeMap<String,String> rowFM = new TreeMap<String,String>();
						TreeMap<String,String> rowMS = new TreeMap<String,String>();
						for (String field : commonFields) {
							rowFM.put(field, fNotNull.getString(field));
							rowMS.put(field, mNotNull.getString(field));
						}
						result.addToDeleteList(rowFM, rowMS);
						handledUIDs.add(currRAUID);
						continue;
					}

					if (currRAUID == currLAUID) {
						Timestamp currRTS = mNotNull.getTimestamp("lastModified"); // current arachne timestamp
						Timestamp currLTS = normaliseTS(fNotNull.getTimestamp("lastModified")); // current fm timestamp
						Timestamp currLRTS = normaliseTS(fNotNull.getTimestamp("lastRemoteTS")); // last remote timestamp saved in fm
						// missing entry in regular table, but entry in entity
						// management! bug in db!
						if (currRTS == null) {
							throw new EntityManagementException(
									"Missing entry in LOCAL Arachne entity management! Table "
											+ currTab.getLeft()
											+ ", remote entry: " + currRAUID);
						}
						int timestampResult = examineTimestamps(currRTS, currLTS, currLRTS);
						// timestamps all equal: SKIP
						if (timestampResult == 0) continue;
						
						TreeMap<String, String> row = new TreeMap<String,String>();
						TreeMap<String, String> diffs = new TreeMap<String,String>();
						
						switch (timestampResult) {
						// local after remote, remote did not change: UPDATE REMOTELY
						case 1:
							for (String field : commonFields) {
								String rVal = mNotNull.getString(field);
								String lVal = fNotNull.getString(field);
								row.put(field, rVal);
								if (!rVal.equals(lVal)) {
									diffs.put(field, lVal);
								}
							}
							result.addToUpdateList(row, diffs, false);
							handledUIDs.add(currRAUID);
							continue;
						// remote after local AND (local <= last remote <= remote): UPDATE LOCALLY
						case 2:
							for (String field : commonFields) {
								String rVal = mNotNull.getString(field);
								String lVal = fNotNull.getString(field);
								row.put(field, lVal);
								if (!rVal.equals(lVal)) {
									diffs.put(field, rVal);
								}
							}
							result.addToUpdateList(row, diffs, true);
							handledUIDs.add(currRAUID);
							continue;
						// local after last remote AND remote after last remote: CONFLICT
						case 3: case 4:
							TreeMap<String, String> rowLocal = new TreeMap<String,String>();
							TreeMap<String, String> rowRemote = new TreeMap<String,String>();
							for (String field : commonFields) {
								rowLocal.put(field, fNotNull.getString(field));
								rowRemote.put(field, mNotNull.getString(field));
							}
							if (compareFields(commonFields, rowLocal, rowRemote, currTab) != 0) {
								result.addToConflictList(rowLocal, rowRemote);
							}
							else {
								diffs.put("lastModified", currRTS.toLocalDateTime().format(formatTS));
								diffs.put("lastRemoteTS", currRTS.toLocalDateTime().format(formatTS));
								result.addToUpdateList(rowLocal, diffs, true);
							}
							handledUIDs.add(currRAUID);
							continue;
						}
					}
					// missing local entry!
					// anyway: the single side has to be handled somehow!
					else if (currLAUID > currRAUID) {
						// deleted on remote? skip this entry.
						if (isDeletedRemotely) continue;
						
						TreeMap<String,String> rowMS = new TreeMap<String,String>();
						Integer rUID = 0;
						
						// lookup remote ID on local
						Pair lookup = new Pair(fmpk, mNotNull.getString(mspk));
						
						for (String field : commonFields) {
							if (field.equals("ArachneEntityID")) rUID = mNotNull.getInt(field);
							rowMS.put(field, mNotNull.getString(field));
						}
						
						TreeMap<String,String> local = isRowOnLocal(currTab, lookup, commonFields);
						// entry already in local db, just missing LAUID? check TS first
						if (!local.isEmpty()) {
							// prevent double handling in fNull section
							handledLocalIDs.add(Integer.parseInt(local.get(fmpk)));
							
							String lUID = local.get("ArachneEntityID");
							String lTS = local.get("lastModified");
							String lrts = local.get("lastRemoteTS");
							String rTS = mNotNull.getString("lastModified");
							if (lUID != null && !lUID.isEmpty()) {
								if (Integer.parseInt(lUID) == rUID) {
									handledUIDs.add(rUID);
									local.remove("lastRemoteTS");
									if (lTS != null && !lTS.isEmpty()) {
										int timestampResult = examineTimestamps(rTS, lTS, lrts);
										if (timestampResult == 0) continue;
										
										TreeMap<String,String> row = new TreeMap<String,String>();
										TreeMap<String,String> diffs = new TreeMap<String,String>();
										local.remove("lastRemoteTS");
										
										switch (timestampResult){
										// update on remote
										case 1:
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
										// update on local
										case 2:
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
										case 3: case 4:
											// all fields equal? no conflict. just update TS to remote values
											if (compareFields(commonFields, local, rowMS, currTab) != 0) {
												result.addToConflictList(local, rowMS);
											}
											else {
												diffs.put("lastModified", rTS);
												diffs.put("lastRemoteTS", rTS);
												result.addToUpdateList(row, diffs, true);
											}
											continue;
										}
									}
									else {
										// all fields equal? no conflict. just update TS to remote values
										if (compareFields(commonFields, local, rowMS, currTab) != 0) {
											result.addToConflictList(local, rowMS);
										}
										else {
											TreeMap<String,String> row = new TreeMap<String,String>();
											TreeMap<String,String> set = new TreeMap<String,String>();
											for (String field : commonFields) {
												String lVal = fNotNull.getString(field);
												row.put(field, lVal);
											}
											set.put("lastModified", rTS);
											set.put("lastRemoteTS", rTS);
											result.addToUpdateList(row, set, true);
										}
										continue;
									}
								}
								// UIDs differ after lookup?!
								else {
									throw new EntityManagementException("Different UID value for same PKs in table "+currTab.getLeft()+"!");
								}
							}
							else {
								local.remove("lastRemoteTS");
								// all fields equal? no conflict. just update TS to remote values
								if (compareFields(commonFields, local, rowMS, currTab) != 0) {
									result.addToConflictList(local, rowMS);
								}
								else {
									TreeMap<String,String> set = new TreeMap<String,String>();
									set.put("ArachneEntityID", lUID);
									set.put("lastModified", rTS);
									set.put("lastRemoteTS", rTS);
									result.addToUpdateList(local, set, true);
								}
								continue;
							}
						}
						// nothing in local db. maybe deleted?
						// local deletions cannot be recognized. GUI should ask in each case.
						// idea: handle locally deleted entries like a conflict!
						else {
							// deleted on remote anyway!
							result.addToDeleteList(null, rowMS);
							handledUIDs.add(rUID);
						}
						fNotNull.previous(); // INDEX SHIFT, trying local index again!
					}	
					// e.g. 1 in local and 4 in remote. this case only occurs if entry does not exist
					// in remote db (impossible due to arachne entity management)
					else if (currRAUID > currLAUID) {
						throw new EntityManagementException("The entry in table "+currTab.getRight()+" with AUID "+ currLAUID + " seems to not exist on remote side, but it should!");
					}
				}
				if (!fNotNull.isAfterLast())
					fNotNull.previous(); // neccessary because of previous next()
				while (fNotNull.next()) {
					throw new EntityManagementException("The entry in table "+currTab.getRight()+" with AUID "+ fNotNull.getString("ArachneEntityID") + " seems to not exist in Arachne, but it should!");
				}
				
				ResultSet fNull = null;
				if (!mNotNull.isAfterLast())
					mNotNull.previous(); // necessary because of previous next()
				if (mNotNull.next()) {
					mNotNull.previous();
					fNull = sqlAccess.doFMQuery(fmSQL
							+ (archerFMSkip == "" ? " WHERE " : " AND ")
							+ "ArachneEntityID IS NULL");
					
					TreeMap<Integer, TreeMap<String,String>> fN = new TreeMap<Integer,TreeMap<String,String>>();
					TreeMap<Integer, TreeMap<String,String>> mNN = new TreeMap<Integer,TreeMap<String,String>>();
					
					while (fNull.next()) {
						TreeMap<String,String> row = new TreeMap<String,String>();
						for (String field : commonFields) {
							row.put(field, fNull.getString(field));
						}
						row.put("lastRemoteTS", fNull.getString("lastRemoteTS"));
						fN.put(fNull.getInt(fmpk), row);
					}
					while (mNotNull.next()) {
						TreeMap<String,String> row = new TreeMap<String,String>();
						for (String field : commonFields) {
							row.put(field, mNotNull.getString(field));
						}
						row.put("isDeleted", mNotNull.getString("isDeleted"));
						row.put("ForeignKey", mNotNull.getString("ForeignKey"));
						mNN.put(mNotNull.getInt("ForeignKey"), row);
					}
					
					System.out.println("");
					
					for (int rID : mNN.keySet()) {
						/**
						 * IDs EQUAL!!!
						 */
						if (fN.containsKey(rID)) {
							TreeMap<String, String> remoteRow = fN.get(rID);
							TreeMap<String, String> localRow = mNN.get(rID);
							
							boolean isDeletedRemotely = mNN.get("isDeleted").equals("1") ? true : false;

							// important! check foreignKey field of deleted UID,
							// to keep track of local entries without UID!
							// currRemoteID cannot be null, due to foreign key!
							int currID = Integer.parseInt(remoteRow.get("ForeignKey"));
							int currRUID = Integer.parseInt(remoteRow.get("ArachneEntityID"));
							
							// check, if deleted remotely. if so, continue.
							if (isDeletedRemotely) {
								remoteRow.put(mspk, ""+currID);
								result.addToDeleteList(localRow, remoteRow);
								continue;
							}
							String currRTS = remoteRow.get("lastModified");
							String currLTS = localRow.get("lastModified");
							String currLRTS = localRow.get("lastRemoteTS");
							
							int resultTS = examineTimestamps(currRTS, currLTS, currLRTS);
							if (resultTS == 0) continue;
							
							TreeMap<String,String> diffs = new TreeMap<String,String>();
							
							switch (resultTS) {
							// update on remote
							case 1:
								for (String field : localRow.keySet()) {
									String rVal = remoteRow.get(field);
									String lVal = localRow.get(field);
									if (!rVal.equals(lVal)) {
										diffs.put(field, lVal);
									}
								}
								result.addToUpdateList(remoteRow, diffs, false);
								break;
							// update on local
							case 2:
								for (String field : commonFields) {
									String rVal = mNotNull.getString(field);
									String lVal = fNotNull.getString(field);
									if (!rVal.equals(lVal)) {
										diffs.put(field, rVal);
									}
								}
								diffs.put("lastRemoteTS", currRTS);
								result.addToUpdateList(localRow, diffs, true);
								break;
							case 3: case 4:
								// all fields equal? no conflict. just update TS to remote values
								if (compareFields(commonFields, localRow, remoteRow, currTab) != 0) {
									result.addToConflictList(localRow, remoteRow);
								}
								else {
									diffs.put("lastModified", currRTS);
									diffs.put("lastRemoteTS", currRTS);
									diffs.put("ArachneEntityID", currRUID+"");
									result.addToUpdateList(localRow, diffs, true);
								}
								break;
							}
							
							fN.remove(rID);
							mNN.remove(mNN);
						}
					}
					
					while (mNotNull.next() & fNull.next()) {
						boolean isDeletedRemotely = mNotNull.getInt("isDeleted") == 1 ? true : false;

						TreeMap<String,String> rowLocal = new TreeMap<String,String>();
						TreeMap<String,String> rowRemote = new TreeMap<String,String>();
						
						
//						if (handledLocalIDs.contains(currLocalID) && handledUIDs.contains(currRUID)) continue;

						for (String field : commonFields) {
							rowLocal.put(field, fNull.getString(field));
							rowRemote.put(field, mNotNull.getString(field));
						}
						
						/**
						 * LID > RID
						 */
						if (3 > 23) {
							fNull.previous();
							if (isDeletedRemotely) continue;
							
							System.out.println("remote nicht gelöscht, hier breakpoint. lokal löschen???");
							if (!handledUIDs.contains(2))
								result.addToDownloadList(rowRemote);
						}
						
						/**
						 * LID < RID
						 */
						if (22 < 3) {
							mNotNull.previous();
							if (!handledLocalIDs.contains(2))
								result.addToUploadList(rowLocal);
							continue;
						}
						
//						switch (compareFields(commonFields, rowLocal, rowRemote, currTab)) {
//						// fields all equal? update local UID field
//						case 0:
//							TreeMap<String,String> set = new TreeMap<String,String>();
//							String lastModified = mNotNull.getString("lastModified");
//							// no RAUID, but in remote DB?!
//							if (rUID == null || rUID == 0)
//								throw new EntityManagementException(
//										"An entry is on remote DB but has no AUID!");
//							
//							// fix to prevent double handling entries
//							if (handledUIDs.contains(rUID)) continue;
//							
//							// setting LAUID = RAUID via local update list
//							set.put("ArachneEntityID", rUID+"");
//							// content is equal to remote. so overwrite local TS with remote
//							set.put("lastModified", lastModified);
//							set.put("lastRemoteTS", lastModified);
//							result.addToUpdateList(rowLocal, set, true);
//							handledUIDs.add(rUID);
//							break;
//						// next remote index ahead of local? shift local and upload.
//						case 5:
//							mNotNull.previous();
//							if (!handledLocalIDs.contains(Integer.parseInt(rowLocal.get(fmpk))))
//								result.addToUploadList(rowLocal);
//							break;
//						// next local index ahead of remote? download.
//						case 6:
//							fNull.previous();
//							if (!handledUIDs.contains((Integer.parseInt(rowRemote.get("ArachneEntityID")))))
//								result.addToDownloadList(rowRemote);
//							break;
//						// at least 1 field not equal, but same PK, conflict!
//						case 1:
//							result.addToConflictList(rowLocal, rowRemote);
//							handledUIDs.add(rUID);
//							break;
//						// all remote fields empty, deleted!
//						case 3:
//							if (isDeletedRemotely) {
//								Pair lookup = new Pair(fmpk, ""+currRemoteID);
//								TreeMap<String, String> local = isRowOnLocal(currTab, lookup, commonFields);
//								if (local.isEmpty()) {
//									result.addToDeleteList(null, rowRemote);
//									handledUIDs.add(rUID);
//									break;
//								}
//								else {
//									Timestamp deleted = mNotNull.getTimestamp("deletedTS");
//									String localTS = local.get("lastModified");
//									String lastRemoteTS = local.get("lastRemoteTS");
//									if (deleted != null && localTS != null && lastRemoteTS != null) {
//										// deleted remotely, not changed locally => delete locally
//										if (deleted.after(Timestamp.valueOf(localTS))
//												&& deleted.after(Timestamp.valueOf(lastRemoteTS))) {
//											result.addToDeleteList(rowLocal, rowRemote);
//											handledLocalIDs.add(currRemoteID);
//											handledUIDs.add(rUID);
//											break;
//										}
//									}
//									local.remove("lastRemoteTS");
//									rowRemote.put(fmpk, ""+currRemoteID);
//									result.addToConflictList(local, rowRemote);
//									handledLocalIDs.add(currRemoteID);
//									handledUIDs.add(rUID);
//									break;
//								}
//							}
//						}	
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
					String lmLocal = fNull.getString("lastModified");
					String pkVal = fNull.getString(fmpk);
					Pair lookup = new Pair(mspk, pkVal);
					
					if (handledLocalIDs.contains(Integer.parseInt(lookup.getRight())))
						continue;
					
					// add local row content to map, lookup remotely
					for (String field : commonFields) {
						String val = fNull.getString(field);
						rowFM.put(field, val);
					}
//					TreeMap<String,String> remote = remoteLookupByPK(currTab, lookup, commonFields);
					TreeMap<String,String> remote = remotePKs.get(Integer.parseInt(pkVal));
					
					// entry already in local db, just missing LAUID? check TS first
//					if (!remote.isEmpty()) {
					if (remote != null) {
						String lmRemote = remote.get("lastModified");
						String lrts = fNull.getString("lastRemoteTS");
						Integer RAUID = Integer.parseInt(remote.get("ArachneEntityID"));
						
						// fi x to prevent double handling entries
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
									// get all other information from row
									remote = remoteLookupByPK(currTab, lookup, commonFields);
									// check for changes!
									remote.remove("lastRemoteTS");
									// conflict avoidance ...
									if (compareFields(commonFields, rowFM, remote, currTab) != 0) {
										// no diffs? ignore. diffs? conflict!
										result.addToConflictList(rowFM, remote);
										handledUIDs.add(RAUID);
										continue;
									}
								}
							}
							else if (!lmL.equals(currATS)) {
								// get all other information from row
								remote = remoteLookupByPK(currTab, lookup, commonFields);
								// check for changes! onflict avoidance ...
								if (compareFields(commonFields, rowFM, remote, currTab) != 0) {
									// no diffs? ignore. diffs? conflict!
									result.addToConflictList(rowFM, remote);
									handledUIDs.add(RAUID);
									continue;
								}
							}
						}
						// no local timestamp found, compare fields ...
						else {
							// get all other information from row
							remote = remoteLookupByPK(currTab, lookup, commonFields);
							// no diffs? ignore. diffs? conflict!
							if (compareFields(commonFields, rowFM, remote, currTab) != 0) {
								result.addToConflictList(rowFM, remote);
								handledUIDs.add(RAUID);
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
					String pkMySQL = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
					Pair lookup = new Pair(pkMySQL, mNotNull.getString(pkMySQL));
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
		ResultSetMetaData rmd = r.getMetaData();
		TreeMap<String,String> result = new TreeMap<String,String>();
		if (r.next()) {
			for (int i = 1; i <= rmd.getColumnCount(); i++) {
				result.put(rmd.getColumnLabel(i), r.getString(i));
			}
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
		TreeMap<String,String> result = new TreeMap<String,String>();
		
		if (lookup.getRight() == null || lookup.getRight().isEmpty())
			return result;
		
		if (currTab.getLeft().equalsIgnoreCase("mainabstract")) {
			archerMSSkip = " AND " + currTab.getLeft()
					+ ".ImportSource!='Comprehensive Table' AND";
		}
		String select = "SELECT lastRemoteTS,";
		String sql = " FROM " + currTab.getLeft()
				+ " WHERE " + archerMSSkip
				+ " "+lookup.getLeft()+"="+lookup.getRight();

		Iterator<String> it = commonFields.iterator();
		while (it.hasNext()) {
			String next = it.next();
			if (it.hasNext())
				select += next + ",";
			else 
				select += next;
		}
		ResultSet r = sqlAccess.doFMQuery(select + sql);
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
		String keyField = sqlAccess.getMySQLTablePrimaryKey(currTab.getRight());
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
				
				if (currFieldName.equalsIgnoreCase(keyField) && currFieldVal != null)
					localIDs.add(Integer.parseInt(currFieldVal));
				
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
			//TODO change to add to local update list ...
			if (!updateLocalUIDAndTS(currTab, RAUID, id.getTimestamp("lastModified").toLocalDateTime(), keyField, id.getInt(keyField)))
				throw new EntityManagementException("Updating local AUID "
						+ id.getInt(1) + " and timestamp in table " + currTab.getLeft()
						+ " FAILED!");
			else {
				logger.debug("Updated local AUID " + id.getInt(1)
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
				.replace("%", "%");
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
		
		if (rowMS.size() != rowFM.size()) throw new IllegalArgumentException("trying to compare rows with different field count");
		
		if (!rowMS.keySet().equals(rowFM.keySet())) return -1;
		
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
				if (isTimestampField(currTab.getLeft()+"."+currField)) {
					// parsing empty string raises exc
					if (!myVal.isEmpty() && !fmVal.isEmpty()) { 
						try {
							Timestamp m = Timestamp.valueOf(myVal);
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
			Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> list) throws IOException, SQLException {
		boolean res = true;
		
		for (Tuple<TreeMap<String, String>, TreeMap<String, String>> row : list) {
			res = res && updateRowOnRemote(currTab, row);
		}
		return res;
	}

	private boolean updateRowOnRemote(Pair currTab, Tuple<TreeMap<String, String>, TreeMap<String, String>> tuple) throws IOException, SQLException {
		if (tuple.getLeft().size() == 0 || tuple.getRight().size() == 0)
			return false;
		
		String sql = "UPDATE " + currTab.getLeft() + " SET ";
		
		TreeMap<String, String> set = tuple.getRight();
		TreeMap<String, String> where = new TreeMap<String,String>(tuple.getLeft());
		Iterator<String> it = set.keySet().iterator();
		
		while (it.hasNext()) {
			String currFieldName = it.next();
			String longName = currTab.getLeft() + "." + currFieldName;
			String currFieldVal = set.get(currFieldName);
			
			if (!isNumericalField(longName)
					&& !isTimestampField(longName)
					&& !currFieldName.equals("lastRemoteTS")
					&& currFieldVal != null) {
				sql += longName + "=" + "'" + escapeChars(currFieldVal) + "'";
			}
			else if ((isTimestampField(longName) || currFieldName.equals("lastRemoteTS")) && currFieldVal != null) {
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
			String longName = currTab.getLeft() + "." + currFieldName;
			String currFieldVal = where.get(currFieldName);
			
			if (!isNumericalField(longName)
					&& !isTimestampField(longName)
					&& !currFieldName.equals("lastRemoteTS")
					&& currFieldVal != null) {
				sql += longName + "=" + "'" + escapeChars(currFieldVal) + "'";
			}
			else if (isTimestampField(longName) && currFieldVal != null) {
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
	
}
