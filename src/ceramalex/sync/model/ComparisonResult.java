package ceramalex.sync.model;

import java.util.ArrayList;

/**
 * simple struct to handle the table result of a sync comparison.
 * @author horle (Felix Kussmaul)
 *
 */
public class ComparisonResult {
	private Pair currTab;
	private ArrayList<Integer> toDownload;
	private ArrayList<ArrayList<Pair>> toUpload;
	private ArrayList<Tuple<Integer,Integer>> toDelete;
	private ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> toUpdateLocally;
	private ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> toUpdateRemotely;
	private ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> conflict;
	
	public ComparisonResult(Pair table) {
		currTab = table;
		toDownload = new ArrayList<Integer>();
		toUpload = new ArrayList<ArrayList<Pair>>();
		toDelete = new ArrayList<Tuple<Integer,Integer>>();
		toUpdateLocally = new ArrayList<Tuple<ArrayList<Pair>,ArrayList<Pair>>>();
		toUpdateRemotely = new ArrayList<Tuple<ArrayList<Pair>,ArrayList<Pair>>>();
		conflict = new ArrayList<Tuple<ArrayList<Pair>,ArrayList<Pair>>>();
	}
	
	/**
	 * 
	 * @param rowFM
	 * @param rowMS
	 * @return
	 */
	public boolean addToConflictList(ArrayList<Pair> rowFM, ArrayList<Pair> rowMS) {
		return conflict.add(new Tuple<ArrayList<Pair>, ArrayList<Pair>>(rowFM, rowMS));
	}

	/**
	 * Adds a list of key-value-pairs to the update list, along with a boolean to decide whether to up- or download
	 * @param setList list of key-value pairs with new content
	 * @param whereList list of key-value pairs to determine row to update
	 * @param local true, if local row shall be updated. false, if remote row shall be updated.
	 * @return true, if successfully added
	 */
	public boolean addToUpdateList(ArrayList<Pair> setList, ArrayList<Pair> whereList, boolean local) {
		if (local)
			return toUpdateLocally.add(
					new Tuple<ArrayList<Pair>,ArrayList<Pair>>
					(setList, whereList));
		else
			return toUpdateRemotely.add(
					new Tuple<ArrayList<Pair>,ArrayList<Pair>>
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
	 * @param row ArrayList of Pair values that represent key-value-Pairs of attributes in one row
	 * @return true, if successfully added
	 */
	public boolean addRowToUploadList(ArrayList<Pair> row) {
		return toUpload.add(row);
	}
	
	public ArrayList<Integer> getDownloadList() {
		return toDownload;
	}
	public ArrayList<ArrayList<Pair>> getUploadList() {
		return toUpload;
	}
	public ArrayList<Tuple<Integer,Integer>> getDeleteList() {
		return toDelete;
	}
	public ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> getLocalUpdateList() {
		return toUpdateLocally;
	}
	public ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> getRemoteUpdateList() {
		return toUpdateRemotely;
	}
	public ArrayList<Tuple<ArrayList<Pair>, ArrayList<Pair>>> getConflictList() {
		return conflict;
	}

	public Pair getTableName() {
		return currTab;
	}
}
