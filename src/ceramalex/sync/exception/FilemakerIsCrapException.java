package ceramalex.sync.exception;



public class FilemakerIsCrapException extends Exception {

	public FilemakerIsCrapException(String arg0) {
		super(arg0 + ", because Filemaker is crap.");
	}
}
