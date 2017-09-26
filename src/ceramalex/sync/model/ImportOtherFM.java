package ceramalex.sync.model;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeMap;

import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.FilemakerIsCrapException;
import javafx.scene.control.TabPaneBuilder;

public class ImportOtherFM {
	
	/**
	 * import datasets from luana, check for existing, referenced entities, e.g. fabrics, and if they are reused in luana db
	 * or if there exist new fabrics with same ID
	 * 
	 * @author horle (Felix Kussmaul)
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ConfigController conf = ConfigController.getInstance();
			SQLAccessController sqlAccess = SQLAccessController.getInstance();
			SQLDataModel m = SQLDataModel.getInstance();
			
			Scanner scan = new Scanner(System.in);
			
			TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>> old = new TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>>();
			
			conf.setFmPassword("btbw");
			conf.setFmDB("iDAIAbstractCeramalex");
			
			System.out.println("STARTING? CHANGE TO REAL DB");
			scan.nextLine();
			
			sqlAccess.connectFM();
			
			for (String currTab : m.fetchFMTables()) {
				if (!("S".equals(currTab) || "Sprache".equals(currTab))) {
					TreeBasedTable<String, Integer, TreeMap<String, String>> table = m.getWholeFMTable(currTab);
					old.put(currTab, table);
				}
			}

			sqlAccess.close();
			System.out.println("DONE WITH FIRST DB; PRESS ENTER WHEN READY!");
			scan.nextLine();
			scan.close();
			
			sqlAccess.close();
			
			sqlAccess.connectFM();
			
			for (String currTab : m.fetchFMTables()) {
				Pair pair = new Pair(currTab,currTab);
				if (currTab.equals("Place")) 
					pair = new Pair(currTab, "Ort");
				if (currTab.equals("PlaceConnection")) 
					pair = new Pair(currTab, "Ortsbezug");
				if (currTab.equals("XPlaceX")) 
					pair = new Pair(currTab, "XOrtX");
				
				int count = 0;
				int diffCount = 0;
				if (!("S".equals(currTab) || currTab.startsWith("["))) {
					TreeBasedTable<String, Integer, TreeMap<String, String>> table = m.getWholeFMTable(pair.getLeft());					
					int smID = 0;
					int bgID = 0;
					if (old.get(pair.getRight()) != null) {
						for (Cell<String, Integer, TreeMap<String, String>> cell : table.cellSet()) {
							String tab = cell.getRowKey();
							int pkval = cell.getColumnKey();
							String pk = m.getActualPrimaryKey(currTab);
							TreeMap<String,String> row = cell.getValue();
							row.remove(pk);
							
							if (!m.isRowOnLocal(pair, pk, row).isEmpty()) {
//							if (old.get(pair.getRight()).contains(pair.getRight(),pkval)) {
								
								TreeMap<String, String> oldRow = old.get(pair.getRight()).get(pair.getRight(), pkval);
								TreeMap<String, String> oldRowIt = new TreeMap<String, String>(oldRow);
								oldRow.remove(pk);
								for (String key : oldRowIt.keySet()) {
									if (key.startsWith("[")) {
										oldRow.remove(key);
										row.remove(key);
									}
								}
								if (m.compareFields(row, oldRow, pair) != 0) {
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
							System.out.println(+count +" n,\t"+diffCount +" d\t"+currTab+(diffCount!=0?": "+smID+"-"+bgID:""));
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
