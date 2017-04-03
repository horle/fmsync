package ceramalex.sync.model;

import java.util.ArrayList;
import java.util.Vector;

/**
 * simple struct to handle the table result of a sync comparison.
 * @author horle (Felix Kussmaul)
 *
 */
public class ComparisonResult {
	private Pair currTab;
	private Vector<Integer> toDownload;
	private Vector<Vector<Pair>> toUpload;
	private Vector<Tuple<Integer,Integer>> toDelete;
	private Vector<Tuple<Vector<Pair>, Vector<Pair>>> toUpdateLocally;
	private Vector<Tuple<Vector<Pair>, Vector<Pair>>> toUpdateRemotely;
	private Vector<Tuple<Vector<Pair>, Vector<Pair>>> conflict;
	
	public ComparisonResult(Pair table) {
		currTab = table;
		toDownload = new Vector<Integer>();
		toUpload = new Vector<Vector<Pair>>();
		toDelete = new Vector<Tuple<Integer,Integer>>();
		toUpdateLocally = new Vector<Tuple<Vector<Pair>,Vector<Pair>>>();
		toUpdateRemotely = new Vector<Tuple<Vector<Pair>,Vector<Pair>>>();
		conflict = new Vector<Tuple<Vector<Pair>, Vector<Pair>>>();
	}
	
	/**
	 * Adds two lists of concurring rows to conflict list 
	 * @param rowFM
	 * @param rowMS
	 * @return
	 */
	public boolean addToConflictList(Vector<Pair> rowFM, Vector<Pair> rowMS) {
		return conflict.add(new Tuple<Vector<Pair>, Vector<Pair>>(rowFM, rowMS));
	}

	/**
	 * Adds a list of key-value-pairs to the update list, along with a boolean to decide whether to up- or download
	 * @param setList list of key-value pairs with new content
	 * @param whereList list of key-value pairs to determine row to update
	 * @param local true, if local row shall be updated. false, if remote row shall be updated.
	 * @return true, if successfully added
	 */
	public boolean addToUpdateList(Vector<Pair> setList, Vector<Pair> whereList, boolean local) {
		if (local)
			return toUpdateLocally.add(
					new Tuple<Vector<Pair>,Vector<Pair>>
					(setList, whereList));
		else
			return toUpdateRemotely.add(
					new Tuple<Vector<Pair>,Vector<Pair>>
					(setList, whereList));
	}
	
	/**
	 * Adds both AUIDs to the delete list. 
	 * @param currCAUID int value of local AUID, may be 0 for no delete
	 * @param currAAUID int value of remote AUID, may be 0 for no delete
	 * @return true, if successfully added
	 */
	public boolean addToDeleteList(int currCAUID, int currAAUID) {		
		return toDelete.add(new Tuple<Integer,Integer>(currCAUID, currAAUID));
	}
	
	/**
	 * Adds remote AAUID value to the download list
	 * @param aauid AAUID in arachne that has to be downloaded
	 * @return true, if successfully added
	 */
	public boolean addAAUIDToDownloadList(int aauid) {
		return toDownload.add(aauid);
	}
	
	/**
	 * Adds set of row values to local upload list
	 * @param row Vector of Pair values that represent key-value-Pairs of attributes in one row
	 * @return true, if successfully added
	 */
	public boolean addRowToUploadList(Vector<Pair> row) {
		return toUpload.add(row);
	}
	
	public Vector<Integer> getDownloadList() {
		return toDownload;
	}
	public Vector<Vector<Pair>> getUploadList() {
		return toUpload;
	}
	public Vector<Tuple<Integer,Integer>> getDeleteList() {
		return toDelete;
	}
	public Vector<Tuple<Vector<Pair>, Vector<Pair>>> getLocalUpdateList() {
		return toUpdateLocally;
	}
	public Vector<Tuple<Vector<Pair>, Vector<Pair>>> getRemoteUpdateList() {
		return toUpdateRemotely;
	}
	public Vector<Tuple<Vector<Pair>, Vector<Pair>>> getConflictList() {
		return conflict;
	}

	public Pair getTableName() {
		return currTab;
	}
}
