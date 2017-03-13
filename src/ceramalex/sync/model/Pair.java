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
public class Pair extends Tuple<String,String> {

	public Pair(String f, String m) {
		super(f, m);
	}
}

class Tuple<T,S> {
	private T left;
	private S right;

	public Tuple(T f, S m) {
		this.left = f;
		this.right = m;
	}

	/**
	 * filemaker string
	 * @return
	 */
	public T getLeft() {
		return left;
	}

	/**
	 * mysql string
	 * @return
	 */
	public S getRight() {
		return right;
	}

	public void setLeft(T f) {
		this.left = f;
	}

	public void setRight(S m) {
		this.right = m;
	}

	@Override
	public String toString() {
		return this.left.toString() + " -> " + this.right.toString();
	}
}