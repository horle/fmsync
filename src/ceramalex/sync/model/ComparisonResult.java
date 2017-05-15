package ceramalex.sync.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

/**
 * simple struct to handle the table result of a sync comparison.
 * @author horle (Felix Kussmaul)
 *
 */
public class ComparisonResult {
	private Pair currTab;
	private ResultSet fmColumns;
	private ResultSet msColumns;
	
	public ResultSet getFmColumns() {
		return fmColumns;
	}

	public void setFmColumns(ResultSet fmColumns) {
		this.fmColumns = fmColumns;
	}

	public ResultSet getMsColumns() {
		return msColumns;
	}

	public void setMsColumns(ResultSet msColumns) {
		this.msColumns = msColumns;
	}

	private Vector<TreeMap<String, String>> toDownload;
	private Vector<TreeMap<String, String>> toUpload;
	private Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> toDelete;
	private Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> toUpdateLocally;
	private Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> toUpdateRemotely;
	private Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> conflict;
	private TreeSet<String> commonFields;
	
	public ComparisonResult(Pair table) {
		currTab = table;
		toDownload = new Vector<TreeMap<String,String>>();
		toUpload = new Vector<TreeMap<String,String>>();
		toDelete = new Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>>();
		toUpdateLocally = new Vector<Tuple<TreeMap<String,String>, TreeMap<String,String>>>();
		toUpdateRemotely = new Vector<Tuple<TreeMap<String,String>, TreeMap<String,String>>>();
		conflict = new Vector<Tuple<TreeMap<String,String>, TreeMap<String,String>>>();
	}
	
	/**
	 * Adds two lists of concurring rows to conflict list 
	 * @param rowFM LOCAL row
	 * @param rowMS REMOTE row
	 * @return local list with remote diff list
	 */
	public boolean addToConflictList(TreeMap<String,String> rowFM, TreeMap<String,String> rowMS) {
		if (rowFM.size() != rowMS.size()) throw new IllegalArgumentException("tried to add rows with different size to conflict list");
		
		TreeMap<String,String> diff = new TreeMap<String,String>();
		
		for (Entry<String,String> e : rowFM.entrySet()) {
			String key = e.getKey();
			String valFM = e.getValue() == null? "" : e.getValue();
			String valMS = rowMS.get(key) == null ? "" : rowMS.get(key);
			if (!valFM.equals(valMS)) {
				diff.put(key, valMS);
			}
		}
		return conflict.add(new Tuple<TreeMap<String,String>, TreeMap<String,String>>(rowFM, diff));
	}
	
	/**
	 * Adds the local row as string vector and all diffs to remote row in a vector containing field name and value as object.
	 * @param row LOCAL row
	 * @param diffs diffs on REMOTE
	 * @return
	 */
	public boolean addToConflictListDiff(TreeMap<String,String> row, TreeMap<String,String> diffs) {
		return conflict.add(new Tuple<TreeMap<String,String>, TreeMap<String,String>>(row, diffs));
	}

	/**
	 * Adds a list of key-value-pairs to the update list, along with a boolean to decide whether to up- or download
	 * @param setList list of key-value pairs with new content
	 * @param whereList list of key-value pairs to determine row to update
	 * @param local true, if local row shall be updated. false, if remote row shall be updated.
	 * @return true, if successfully added
	 */
	public boolean addToUpdateList(TreeMap<String,String> whereList, TreeMap<String,String> setList, boolean local) {
		if (local)
			return toUpdateLocally.add(
					new Tuple<TreeMap<String,String>,TreeMap<String,String>>
					(whereList, setList));
		else
			return toUpdateRemotely.add(
					new Tuple<TreeMap<String,String>,TreeMap<String,String>>
					(whereList, setList));
	}
	
	/**
	 * Adds both AUIDs to the delete list. 
	 * @param currCAUID int value of local AUID, may be 0 for no delete
	 * @param currAAUID int value of remote AUID, may be 0 for no delete
	 * @return true, if successfully added
	 */
	public boolean addToDeleteList(TreeMap<String,String> rowFM, TreeMap<String,String> rowMS) {
		return toDelete.add(new Tuple<TreeMap<String,String>,TreeMap<String,String>>(rowFM, rowMS));
	}
	
	/**
	 * Adds remote AAUID value to the download list
	 * @param aauid AAUID in arachne that has to be downloaded
	 * @return true, if successfully added
	 */
//	public boolean addAAUIDToDownloadList(int aauid) {
//		return toDownload.add(aauid);
//	}

	public void addToDownloadList(TreeMap<String, String> row) {
		toDownload.add(row);
	}
	
	/**
	 * Adds set of row values to local upload list
	 * @param row Vector of Pair values that represent key-value-Pairs of attributes in one row
	 * @return true, if successfully added
	 */
	public boolean addToUploadList(TreeMap<String, String> row) {
		return toUpload.add(row);
	}
	
	public Vector<TreeMap<String,String>> getDownloadList() {
		return toDownload;
	}
	public Vector<TreeMap<String, String>> getUploadList() {
		return toUpload;
	}
	public Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> getDeleteList() {
		return toDelete;
	}
	public Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> getLocalUpdateList() {
		return toUpdateLocally;
	}
	public Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> getRemoteUpdateList() {
		return toUpdateRemotely;
	}
	public Vector<Tuple<TreeMap<String, String>, TreeMap<String, String>>> getConflictList() {
		return conflict;
	}

	public Pair getTableName() {
		return currTab;
	}

	public TreeSet<String> getCommonFields() {
		return commonFields;
	}

	public void setCommonFields(TreeSet<String> commonFields2) {
		this.commonFields = commonFields2;
	}
}
