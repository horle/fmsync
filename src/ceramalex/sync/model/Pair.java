package ceramalex.sync.model;

/**
 * Class to provide a tuple of strings to keep key-value-pairs or the
 * distinction of table or field names. Extends abstract class Tuple<F,M>. 
 * 
 * @author horle (Felix Kussmaul)
 * @param <T> type of left, right
 * @param <S>
 *
 */
public class Pair extends Tuple<String,String> implements Comparable<Pair> {

	public Pair(String f, String m) {
		super(f, m);
	}
	
	public String getFMString() {
		return this.left;
	}
	
	public String getMySQLString() {
		return this.right;
	}
	
	public void setFMString(String f) {
		this.left = f;
	}
	
	public void setMySQLString(String m) {
		this.right = m;
	}

	@Override
	public int compareTo(Pair other) {
		return this.left.compareTo(other.left);
	}
}