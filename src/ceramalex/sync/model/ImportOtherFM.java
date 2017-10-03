package ceramalex.sync.model;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

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
	private static TreeMap<String, Vector<TreeMap<String,String>>> newRowsPerTable;
	private static TreeMap<String, Vector<TreeMap<String,String>>> localDB;
	private static ArrayDeque<String> stack;
	private static ArrayList<String> tables;
	private static HashSet<String> cycles;
	// string: table name + ":" +  old id; int: new id
	private static HashMap<String,Integer> addedList;
	private static SQLDataModel m;
	
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
			m = SQLDataModel.getInstance();
			newRowsPerTable = new TreeMap<String, Vector<TreeMap<String,String>>>();
			
			Scanner scan = new Scanner(System.in);
			
			TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>> newList = new TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>>();
			
			conf.setFmPassword("btbw");
			conf.setFmDB("iDAIAbstractCeramalex");
			
			System.out.println("STARTING? CHANGE TO NEW DB");
			scan.nextLine();
			
			sqlAccess.connectFM();
			tables = m.fetchFMTables();
			
			for (String currTab : tables) {
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
			tables = m.fetchFMTables();
			
			for (String currTab : tables) {
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
				Vector<TreeMap<String,String>> newRows = new Vector<TreeMap<String,String>>();
				
				if (!("S".equals(currTab) || currTab.startsWith("["))) {
					int smID = 0;
					int bgID = 0;
					TreeBasedTable<String, Integer, TreeMap<String, String>> newTable = newList.get(pair.getRight());
					if (newTable != null) {
						String pkold = m.getActualPrimaryKey(currTab);
						String pknew = pkold;
						if (pkold.contains("Ortsbezug")) pknew = "PS_PlaceConnectionID";
						else if (pkold.contains("XOrtX")) pknew = "PS_XPlaceIDX";
						else if (pkold.contains("Ort")) pknew = "PS_PlaceID";
						
						for (Cell<String, Integer, TreeMap<String, String>> newCell : newTable.cellSet()) {
							int pkval = newCell.getColumnKey();
							TreeMap<String,String> newRow = new TreeMap<String,String>(newCell.getValue());

							String pknewval = newRow.remove(pknew);
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
									// pk raus, zur add liste TODO?
								}
							}
							else {
								count++;
								// pk dazu, zur add liste :>
								newRow.put(pknew, pknewval);
								newRows.addElement(newRow);
							}
						}
						if (count + diffCount != 0) {
							System.out.println(count +" n,\t"+existCount+" e,\t"+diffCount +" d\t"+currTab+(diffCount!=0?": "+smID+"-"+bgID:""));
						}
						
					} else 
						System.out.println("Table "+pair+" in new but not in old");
				}
				if (!newRows.isEmpty())
					newRowsPerTable.put(currTab, newRows);
			}
			
			for (String currTab : newRowsPerTable.keySet()) {
				for (TreeMap<String,String> newRow : newRowsPerTable.get(currTab)) {
					
					editFKsRecursively(newRow, currTab, m.getActualPrimaryKey(currTab));
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

	private static TreeMap<String,String> getRowForFK(String table, String pk, String pkval) {
		for (TreeMap<String,String> row : newRowsPerTable.get(table)) {
			if (row.get(pk).equals(pkval)) return row;
		}
		return null;
	}
	
	private static void setFKForRow(String table, String fk, String oldval, String newval) {
		for (TreeMap<String,String> row : newRowsPerTable.get(table)) {
			if (row.get(fk).equals(oldval)) {
				row.put(fk, newval);
				return;
			}
		}
	}
	
	private static String getTableFromFK(String fk) {
		return fk.replace("FS_", "").replace("ID", "");
	}
	
	private static void editFKsRecursively(TreeMap<String, String> newRow, String table, String pk) throws FilemakerIsCrapException, SQLException {
		if (stack == null) stack = new ArrayDeque<String>();
		if (cycles == null) cycles = new HashSet<String>();
		// table name + oldID + newID
		if (addedList == null) addedList = new HashMap<String,Integer>();
		
		// copy row content for editing
		TreeMap<String, String> rowToApply = new TreeMap<String, String>(newRow);
		ArrayList<String> fks = new ArrayList<String>();
		
		// initial table		
		stack.push(table);
		for (String key : newRow.keySet()) {
			if (key.startsWith("FS_"))
				fks.add(key);
		}

		Iterator<String> it = fks.iterator();
		// a FK existent?
		if (it.hasNext()) {
			// loop over all foreign keys
			while (it.hasNext()) {
				String key = it.next();
				String keyval = rowToApply.get(key)==null?"0":rowToApply.get(key);
				if (!keyval.equals("Deutsch") && stack.size()>= 1 && Integer.parseInt(keyval) >= 100000) {
					
					// if row behind fk already added
					Integer alreadyAdded = addedList.get(getTableFromFK(key)+":"+newRow.get(key));
					
					if (alreadyAdded != null) {
						rowToApply.put(key, ""+alreadyAdded);
						continue;
					}
					
//					// FK table in stack? => cycle
//					if (stack.contains(getTableFromFK(key))) {
//						cycles.add(stack.peek() + "." + key);
//						// if no other FKs, but cycle, then just pop up
//						if (!it.hasNext()) stack.pop();
//						break;
//					}
					// FK not in stack? => jump into table of fk
					else {
						String tableName = getTableFromFK(key);
						// table existent?
						if (tables.contains(tableName)) {
							// jump into table
							// look for correct row
							for (TreeMap<String,String> row : newRowsPerTable.get(tableName)) {
								String pknew = m.getActualPrimaryKey(tableName);
								// pk of child row equal to fk of current row?
								String currFK = newRow.get(key);
								if (row.get(pknew).equals(currFK)) {
									editFKsRecursively(row, tableName, pknew);
									break;
								}
							}
						}
					}
				}
			}
			// looped through all FKs? add row to local db
			
//			localDB.get(table).add();
		}
		// no FK's
		else {
			String tab = stack.pop();
			int pkNew = 0;
			int pkOld = Integer.parseInt(newRow.get(pk));
			// if this row already added, get this id
			if (addedList.containsKey(tab+":"+pkOld)){
				pkNew = addedList.get(tab+":"+pkOld);
			}
			// else add row, keep new id!
			else {
				// TODO: add row, pkNew = return value
				pkNew = 0; //TODO
				// add row with OLD pk to addedList
				addedList.put(tab+":"+pkOld, pkNew);
			}
			// != stack top?
			if (stack.size() > 0) {
				// replace old fk value in parent row with new value
				setFKForRow(stack.peek(), getFKFromTabName(tab), pkOld+"", pkNew+"");
			}
		}
		// irgendwie neue row applyen
		return;
	}

	private static String getFKFromTabName(String tab) {
		if (tab.equals("Ort")) return "FS_PlaceID";
		else return "FS_"+tab+"ID";
	}
}
