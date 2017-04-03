package ceramalex.sync.model;

public class Tuple<T,S> {
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
		String le = this.left == null ? "null" : this.left.toString();
		String re = this.right == null ? "null" : this.right.toString();
		return le + " -> " + re;
	}
}