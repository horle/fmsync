package ceramalex.sync.exception;

import com.filemaker.jdbc.FMSQLException;

public class FilemakerIsCrapException extends FMSQLException {

	public FilemakerIsCrapException(String arg0) {
		super(arg0 + ", because Filemaker is crap.", 0);
		// TODO Auto-generated constructor stub
	}

	public FilemakerIsCrapException(String arg0, String arg1, int arg2) {
		super(arg0 + ", because Filemaker is crap.", arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	
}
