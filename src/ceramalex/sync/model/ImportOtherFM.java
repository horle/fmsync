package ceramalex.sync.model;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.FilemakerIsCrapException;
import javafx.scene.control.TabPaneBuilder;

public class ImportOtherFM {
	
	private static Logger logger = Logger.getLogger(SQLDataModel.class);
	
	/**
	 * import datasets from luana, check for existing, referenced entities, e.g. fabrics, and if they are reused in luana db
	 * or if there exist new fabrics with same ID
	 * 
	 * @author horle (Felix Kussmaul)
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DOMConfigurator.configureAndWatch("log4j.xml");
			ConfigController conf = ConfigController.getInstance();
			SQLAccessController sqlAccess = SQLAccessController.getInstance();
			SQLDataModel m = SQLDataModel.getInstance();
			
			Scanner scan = new Scanner(System.in);
			
			TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>> newList = new TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>>();
			
			conf.setFmPassword("btbw");
			conf.setFmDB("iDAIAbstractCeramalex");
			
			System.out.println("STARTING? CHANGE TO NEW DB");
			scan.nextLine();
			
			sqlAccess.connectFM();
			
			for (String currTab : m.fetchFMTables()) {
				if (!("S".equals(currTab) || "Sprache".equals(currTab))) {
					TreeBasedTable<String, Integer, TreeMap<String, String>> table = m.getWholeFMTable(currTab);
					newList.put(currTab, table);
				}
			}

			sqlAccess.close();
			System.out.println("DONE; CHANGE DB AND PRESS ENTER WHEN READY!");
			scan.nextLine();
			scan.close();
			
			sqlAccess.connectFM();
			
			for (String currTab : m.fetchFMTables()) {
				// NEW -> RIGHT
				// OLD -> LEFT
				Pair pair = new Pair(currTab,currTab);
				if (currTab.equals("Ort")) 
					pair = new Pair(currTab, "Place");
				if (currTab.equals("Ortsbezug")) 
					pair = new Pair(currTab, "PlaceConnection");
				if (currTab.equals("XOrtX")) 
					pair = new Pair(currTab, "XPlaceX");
				
				int count = 0;
				int existCount = 0;
				int diffCount = 0;
				
				if (!("S".equals(currTab) || currTab.startsWith("["))) {			
					int smID = 0;
					int bgID = 0;
					TreeBasedTable<String, Integer, TreeMap<String, String>> newTable = newList.get(pair.getRight());
					if (newTable != null) {
						String pkold = m.getActualPrimaryKey(currTab);
						String pknew = m.getActualPrimaryKey(currTab);
						if (pkold.contains("Ortsbezug")) pknew = "PS_PlaceConnectionID";
						else if (pkold.contains("XOrtX")) pknew = "PS_XPlaceIDX";
						else if (pkold.contains("Ort")) pknew = "PS_PlaceID";
						
						for (Cell<String, Integer, TreeMap<String, String>> newCell : newTable.cellSet()) {
							int pkval = newCell.getColumnKey();
							TreeMap<String,String> newRow = new TreeMap<String,String>(newCell.getValue());

							newRow.remove(pknew);
							for (String key : newCell.getValue().keySet()) {
								if (key.startsWith("[")) {
									newRow.remove(key);
								}
							}
							
							TreeMap<String, String> oldRow = m.isRowOnLocal(pair.getLeft(), pkold, newRow);
							
							oldRow.remove(pkold);
							for (String key : newCell.getValue().keySet()) {
								if (key.startsWith("[")) {
									oldRow.remove(key);
								}
							}
							
							if (!oldRow.isEmpty()) {
								existCount++;
								if (m.compareFields(oldRow, newRow, pair) != 0) {
									if (smID == 0) smID = pkval;
									bgID = pkval;
									diffCount++;
									// pk raus, zur add liste
								}
							}
							else {
								count++;
								// pk raus, zur add liste :>
							}
						}
						if (count + diffCount != 0) {
							System.out.println(count +" n,\t"+existCount+" e,\t"+diffCount +" d\t"+currTab+(diffCount!=0?": "+smID+"-"+bgID:""));
						}
						
					} else 
						System.out.println("Table "+pair+" in new but not in old");
				}
			}
			
			System.out.println("done");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (FilemakerIsCrapException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
