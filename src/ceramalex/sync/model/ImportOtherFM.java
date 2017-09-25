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
			ArrayList<TreeBasedTable<String, Integer, TreeMap<String, String>>> neww = new ArrayList<TreeBasedTable<String, Integer, TreeMap<String, String>>>();
			
			conf.setFmPassword("btbw");
			conf.setFmDB("iDAIAbstractCeramalex");
			
			sqlAccess.connectFM();
			
			System.out.println("STARTING? CHANGE TO REAL DB");
			scan.nextLine();
			
			for (String currTab : m.fetchFMTables()) {
				if (!"S".equals(currTab)) {
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
				int count = 0;
				if (!"S".equals(currTab)) {
					TreeBasedTable<String, Integer, TreeMap<String, String>> table = m.getWholeFMTable(currTab);
					for (Cell<String, Integer, TreeMap<String, String>> cell : table.cellSet()) {
						String tab = cell.getRowKey();
						int pkval = cell.getColumnKey();
						TreeMap<String,String> row = cell.getValue();
						
						if (old.get(currTab).contains(currTab,pkval)) {
							m.compareFields(row, old.get(currTab).get(currTab, pkval), pair);
						}
						else count++;
					}
				}
				System.out.println("table "+currTab+" has "+count +" new rows!");
			}
			
			System.out.println("done fetching");
			
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
