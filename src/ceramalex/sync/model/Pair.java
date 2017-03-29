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