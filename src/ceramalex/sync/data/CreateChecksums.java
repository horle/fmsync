package ceramalex.sync.data;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class CreateChecksums {

	/**
	 * mount -t cifs //nas10.rrz.uni-koeln.de/archaeocloud /mnt/archaeocloud/ -o user=fkussma1,password= ...,iocharset=utf8
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Files.walk(Paths.get("F:\\S-Arachne\\arachne4scans\\arachne4webimages\\objectscans\\ceramalex\\")).forEach(t -> {
				try {
					Foo.calcSum(t);
				}
				catch (AccessDeniedException e) {
					//ignore
				}
				catch (NoSuchAlgorithmException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static class Foo {
		static void calcSum(Path file) throws NoSuchAlgorithmException, IOException {
			MessageDigest md = MessageDigest.getInstance("MD5");
			String f = file.toString();

			md.update(Files.readAllBytes(Paths.get(f)));
			byte[] digest = md.digest();
			String out = DatatypeConverter.printHexBinary(digest).toUpperCase();

			System.out.println("file "+file+" has md5 "+out);
		}
	}

}
