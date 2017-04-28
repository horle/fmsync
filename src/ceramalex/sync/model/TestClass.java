package ceramalex.sync.model;

import java.util.ArrayList;
import java.util.TreeMap;
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
						for (TreeMap<String, String> r : result.getDownloadList()) {
							for (String key : r.keySet()) {
								System.out.print(key + ":"+r.get(key)+", ");
							}
							System.out.println();
						}
						m.prepareRowsAndDownload(commonTables.get(i), result.getDownloadList(), 25);
					}
					if (!result.getLocalUpdateList().isEmpty()) {
						System.out.println("printing LOCAL UPDATE list:");
						for (Tuple<TreeMap<String, String>, TreeMap<String, String>> r : result.getLocalUpdateList()) {
							System.out.println("Row local:");
							for (String key : r.getLeft().keySet()) {
								System.out.print(key+ ":"+r.getLeft().get(key)+", ");
							}
							System.out.println();
							System.out.println("Row DIFFS: ");
							for (String key : r.getRight().keySet()) {
								System.out.print(key+ ":"+r.getRight().get(key)+", ");
							}
							System.out.println();
						}
					}
					if (!result.getRemoteUpdateList().isEmpty()) {
						System.out.println("printing REMOTE UPDATE list:");
						for (Tuple<TreeMap<String, String>, TreeMap<String, String>> r : result.getRemoteUpdateList()) {
							for (String key : r.getLeft().keySet()) {
								System.out.print(key+ ":"+r.getLeft().get(key)+", ");
							}
							System.out.println();
							System.out.println("changes in: ");
							for (String key : r.getRight().keySet()) {
								System.out.print(key+ ":"+r.getRight().get(key));
							}
							System.out.println();
						}
					}
					if (!result.getUploadList().isEmpty()) {
						System.out.println("printing UPLOAD list:");
						for (TreeMap<String, String> row : result.getUploadList()) {
							for (String key : row.keySet()) {
								System.out.print(key + ": " + row.get(key) + ", ");
							}
							System.out.println();
						}
						m.prepareRowsAndUpload(commonTables.get(i), result.getUploadList(), 25);
					}
					if (!result.getConflictList().isEmpty()) {
						System.out.println("printing CONFLICT list:");
						for (Tuple<TreeMap<String, String>, TreeMap<String, String>> con : result.getConflictList()) {
							for (String key : con.getLeft().keySet()) {
								System.out.print(key + ":"
										+ con.getLeft().get(key) + ", ");
							}
							System.out.println();
							System.out.println("changes in: ");
							for (String key : con.getRight().keySet()) {
								System.out.print(key + ":"
										+ con.getRight().get(key) + ", ");
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
