package ceramalex.sync.model;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import ceramalex.sync.controller.ConfigController;

public class TestClass {
	private static Logger logger = Logger.getLogger(SQLDataModel.class);
	
	public static void main(String[] args) {
		try {
			DOMConfigurator.configureAndWatch("log4j.xml");
			ConfigController.getInstance();
			SQLDataModel m = SQLDataModel.getInstance();
			ArrayList<Pair> commonTables = m.fetchCommonTables();

			for (int i = 0; i < commonTables.size(); i++) {
				try {
					System.out.print("\nProcessing table "
							+ commonTables.get(i) + " ... ");
					ComparisonResult result = m.getDiffByUUID(
							commonTables.get(i), true, true);
					/**
					 * BEGIN TESTING AREA
					 */
					if (!result.getDownloadList().isEmpty()) {
						System.out.println("printing DOWNLOAD list:");
						for (Vector<String> r : result.getDownloadViewList()) {
							for (String d : r) {
								System.out.print(d + ", ");
							}
							System.out.println();
						}
						m.prepareRowsAndDownload(commonTables.get(i), result.getDownloadList(), 25);
					}
					if (!result.getLocalUpdateList().isEmpty()) {
						System.out.println("printing LOCAL UPDATE list:");
						for (Tuple<Vector<String>, Vector<String>> r : result.getLocalUpdateViewList()) {
							System.out.println("Row local:");
							for (String d : r.getLeft()) {
								System.out.print(d+ ", ");
							}
							System.out.println();
							System.out.println("Row remote: ");
							for (String d : r.getRight()) {
								System.out.print(d+ ", ");
							}
							System.out.println();
						}
					}
					if (!result.getRemoteUpdateList().isEmpty()) {
						System.out.println("printing REMOTE UPDATE list:");
						for (Tuple<Vector<String>, Vector<String>> r : result.getRemoteUpdateViewList()) {
							for (String d : r.getLeft()) {
								System.out.print(d+ ", ");
							}
							System.out.println();
							System.out.println("changes in: ");
							for (String d : r.getRight()) {
								System.out.print(d+ ", ");
							}
							System.out.println();
						}
					}
					if (!result.getUploadList().isEmpty()) {
						System.out.println("printing UPLOAD list:");
						for (Vector<Pair> r : result.getUploadList()) {
							for (Pair d : r) {
								System.out.print(d.getLeft() + ": " + d.getRight() + ", ");
							}
							System.out.println();
						}
						m.prepareRowsAndUpload(commonTables.get(i), result.getUploadList(), 25);
					}
					if (!result.getConflictList().isEmpty()) {
						System.out.println("printing CONFLICT list:");
						for (Tuple<Vector<Pair>, Vector<Pair>> r : result.getConflictList()) {
							for (Pair d : r.getLeft()) {
								System.out.print(d.getLeft() + ":"
										+ d.getRight() + ", ");
							}
							System.out.println();
							System.out.println("changes in: ");
							for (Pair d : r.getRight()) {
								System.out.print(d.getLeft() + ":"
										+ d.getRight() + ", ");
							}
							System.out.println();
						}
					}
					/**
					 * END TESTING AREA
					 */

					logger.info("Processed table " + commonTables.get(i)
							+ " without errors.");
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(commonTables.get(i) + ": " + e);
					System.out.println("Error!");
				}
			}
			System.out.println("\n Exit.");
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			logger.error(e);
			e.printStackTrace();
		}
	}
}
