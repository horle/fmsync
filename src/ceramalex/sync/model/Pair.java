package ceramalex.sync.model;

/**
 * Class to provide a tuple of two Strings (filemaker, mysql) to keep
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

	public F getF() {
		return fileMaker;
	}

	public M getM() {
		return mySQL;
	}

	public void setF(F f) {
		this.fileMaker = f;
	}

	public void setM(M m) {
		this.mySQL = m;
	}

	@Override
	public String toString() {
		return this.fileMaker.toString() + " -> " + this.mySQL.toString();
	}
}