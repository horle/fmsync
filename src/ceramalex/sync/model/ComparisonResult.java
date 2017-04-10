package ceramalex.sync.model;

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
	private Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>> toUpdateLocally;
	private Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>> toUpdateRemotely;
	private Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>> conflict;
	
	private Vector<Vector<String>> toDownloadView;
	
	public ComparisonResult(Pair table) {
		currTab = table;
		toDownload = new Vector<Integer>();
		toUpload = new Vector<Vector<Pair>>();
		toDelete = new Vector<Tuple<Integer,Integer>>();
		toUpdateLocally = new Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>>();
		toUpdateRemotely = new Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>>();
		conflict = new Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>>();
		
		toDownloadView = new Vector<Vector<String>>();
	}
	
	/**
	 * Adds two lists of concurring rows to conflict list 
	 * @param rowFM LOCAL row
	 * @param rowMS REMOTE row
	 * @return
	 */
	public boolean addToConflictList(Vector<Pair> rowFM, Vector<Pair> rowMS) {
		Vector<Pair> row = new Vector<Pair>();
		Vector<Tuple<String, Object>> diff = new Vector<Tuple<String, Object>>();
		
		for (int i = 0; i < rowFM.size(); i++) {
			Pair f = rowFM.get(i);
			row.add(f);
			for (int j = 0; j < rowMS.size(); j++) {
				Pair m = rowMS.get(j);
				if (!f.getRight().equals(m.getRight()))	// if diff between values
					diff.add(
							new Tuple<String, Object>(
									f.getLeft(), // name of fm field
									m.getRight() ) // value of mysql field
						);
			}
		}
		return conflict.add(new Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>(row, diff));
	}
	
	/**
	 * Adds the local row as string vector and all diffs to remote row in a vector containing field name and value as object.
	 * @param row LOCAL row
	 * @param diffs diffs on REMOTE
	 * @return
	 */
	public boolean addToConflictListDiff(Vector<Pair> row, Vector<Tuple<String, Object>> diffs) {
		return conflict.add(new Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>(row, diffs));
	}

	/**
	 * Adds a list of key-value-pairs to the update list, along with a boolean to decide whether to up- or download
	 * @param setList list of key-value pairs with new content
	 * @param whereList list of key-value pairs to determine row to update
	 * @param local true, if local row shall be updated. false, if remote row shall be updated.
	 * @return true, if successfully added
	 */
	public boolean addToUpdateList(Vector<Pair> whereList, Vector<Tuple<String, Object>> setList, boolean local) {
		if (local)
			return toUpdateLocally.add(
					new Tuple<Vector<Pair>,Vector<Tuple<String, Object>>>
					(whereList, setList));
		else
			return toUpdateRemotely.add(
					new Tuple<Vector<Pair>,Vector<Tuple<String, Object>>>
					(whereList, setList));
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
	public Vector<Vector<String>> getDownloadViewList() {
		return toDownloadView;
	}
	public Vector<Vector<Pair>> getUploadList() {
		return toUpload;
	}
	public Vector<Vector<String>> getUploadViewList() {
		Vector<Vector<String>> v = new Vector<Vector<String>>();
		for (Vector<Pair> u : toUpload) {
			Vector<String> s = new Vector<String>();
			for (Pair p : u) {
				s.add(p.getRight());
			}
			v.add(s);
		}
		return v;
	}
	public Vector<Tuple<Integer,Integer>> getDeleteList() {
		return toDelete;
	}
	public Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>> getLocalUpdateList() {
		return toUpdateLocally;
	}
	public Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>> getRemoteUpdateList() {
		return toUpdateRemotely;
	}
	public Vector<Tuple<Vector<Pair>, Vector<Tuple<String, Object>>>> getConflictList() {
		return conflict;
	}

	public Pair getTableName() {
		return currTab;
	}
}
