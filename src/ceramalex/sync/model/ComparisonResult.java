package ceramalex.sync.model;

import java.util.ArrayList;

/**
 * simple struct to handle the result of a sync comparison.
 * @author horle (Felix Kussmaul)
 *
 */
public class ComparisonResult {
	private ArrayList<Integer> toDownload;
	private ArrayList<ArrayList<Pair>> toUpload;
	private ArrayList<Tuple<Integer,Integer>> toDelete;
	private ArrayList<Tuple<ArrayList<Pair>, Boolean>> toUpdate;
	
	public ComparisonResult() {
		toDownload = new ArrayList<Integer>();
		toUpload = new ArrayList<ArrayList<Pair>>();
		toDelete = new ArrayList<Tuple<Integer,Integer>>();
		toUpdate = new ArrayList<Tuple<ArrayList<Pair>,Boolean>>();
	}
	
	/**
	 * Adds a list of key-value-pairs to the update list, along with a boolean to decide whether to up- or download
	 * @param list
	 * @param upload true, if row shall be uploaded to remote. false, if download
	 * @return true, if successfully added
	 */
	public boolean addToUpdate(ArrayList<Pair> list, boolean upload) {
		return toUpdate.add(new Tuple<ArrayList<Pair>, Boolean>(list, upload));
	}
	
	/**
	 * Adds both AUIDs to the delete list. 
	 * @param currCAUID int value of local AUID, may be 0 for no delete
	 * @param currAAUID int value of remote AUID, may be 0 for no delete
	 * @return true, if successfully added
	 */
	public boolean addToDelete(int currCAUID, int currAAUID) {		
		return toDelete.add(new Tuple<Integer,Integer>(currCAUID, currAAUID));
	}
	
	/**
	 * Adds remote AAUID value to the download list
	 * @param aauid AAUID in arachne that has to be downloaded
	 * @return true, if successfully added
	 */
	public boolean addAAUIDToDownload(int aauid) {
		return toDownload.add(aauid);
	}
	
	/**
	 * Adds set of row values to local upload list
	 * @param row ArrayList of Pair values that represent key-value-Pairs of attributes in one row
	 * @return true, if successfully added
	 */
	public boolean addRowToUpload(ArrayList<Pair> row) {
		return toUpload.add(row);
	}
	
	public ArrayList<Integer> getToDownload() {
		return toDownload;
	}
	public ArrayList<ArrayList<Pair>> getToUpload() {
		return toUpload;
	}
	public ArrayList<Tuple<Integer,Integer>> getToDelete() {
		return toDelete;
	}
	public ArrayList<Tuple<ArrayList<Pair>, Boolean>> getToUpdate() {
		return toUpdate;
	}
}
