package ceramalex.sync.model;

/**
 * Class to provide a tuple of two Strings to keep key-value-pairs or the
 * distinction of table or field names. Extends abstract class Tuple<F,M>. 
 * 
 * @author horle (Felix Kussmaul)
 *
 */
public class Pair extends Tuple<String, String> {

	public Pair(String f, String m) {
		super(f, m);
	}
}

abstract class Tuple<F, M> {
	private F fileMaker;
	private M mySQL;

	public Tuple(F f, M m) {
		this.fileMaker = f;
		this.mySQL = m;
	}

	/**
	 * filemaker string
	 * @return
	 */
	public F getLeft() {
		return fileMaker;
	}

	/**
	 * mysql string
	 * @return
	 */
	public M getRight() {
		return mySQL;
	}

	public void setLeft(F f) {
		this.fileMaker = f;
	}

	public void setRight(M m) {
		this.mySQL = m;
	}

	@Override
	public String toString() {
		return this.fileMaker.toString() + " -> " + this.mySQL.toString();
	}
}