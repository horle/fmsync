package horle.fmsync.model;

import java.io.FileWriter;
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

import horle.fmsync.controller.ConfigController;
import horle.fmsync.controller.SQLAccessController;
import horle.fmsync.exception.EntityManagementException;
import horle.fmsync.exception.FilemakerIsCrapException;
import horle.fmsync.exception.MissingPermissionException;

public class ImportOtherFM {
	
	private static Logger logger = Logger.getLogger(SQLDataModel.class);
	private static TreeMap<String, Vector<TreeMap<String,String>>> newRowsPerTable;
	private static ArrayDeque<String> stack;
	private static ArrayList<String> tables;
	private static HashSet<String> cycles;
	// string: table name + ":" +  old id; int: new id
	private static HashMap<String,Integer> addedList;
	private static SQLDataModel m;
	private static HashMap<String, Tuple<Integer,Integer>> addCounts;
	
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
			addCounts = new HashMap<String, Tuple<Integer,Integer>>();
			
			Scanner scan = new Scanner(System.in);
			
			TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>> newList = new TreeMap<String, TreeBasedTable<String, Integer, TreeMap<String, String>>>();
			
			conf.setFmPassword("btbw");
			conf.setFmDB("iDAIAbstractCeramalex");
			
			System.out.println("STARTING? CHANGE TO NEW DB");
			scan.nextLine();
			
			sqlAccess.connectFM();
			tables = m.fetchFMTables();
			
			// copy all relevant tables as pairs (name, table-content) to memory
			for (String currTab : tables) {
				if (!("S".equals(currTab) || "Sprache".equals(currTab))) {
					TreeBasedTable<String, Integer, TreeMap<String, String>> table = m.getWholeFMTable(currTab);
					newList.put(currTab, table);
				}
			}

			sqlAccess.close();
			System.out.println("DONE; CHANGE DB AND PRESS ENTER WHEN READY!");
			scan.nextLine();
			
			sqlAccess.connectFM();
			tables = m.fetchFMTables();
			
			// loop through existing tables
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
				
				// strange filemaker tables. not related to content
				if (!("S".equals(currTab) || currTab.startsWith("["))) {
					int smID = 0;
					int bgID = 0;
					
					// choose table copy
					TreeBasedTable<String, Integer, TreeMap<String, String>> newTable = newList.get(pair.getRight());
					if (newTable != null) {
						// map old primary key column name to english import
						String pkold = m.getActualPrimaryKey(currTab);
						String pknew = pkold;
						if (pkold.contains("Ortsbezug")) pknew = "PS_PlaceConnectionID";
						else if (pkold.contains("XOrtX")) pknew = "PS_XPlaceIDX";
						else if (pkold.contains("Ort")) pknew = "PS_PlaceID";
						
						// google's "Cell" here corresponds to a row
						for (Cell<String, Integer, TreeMap<String, String>> newCell : newTable.cellSet()) {
							int pkval = newCell.getColumnKey();
							TreeMap<String,String> newRow = new TreeMap<String,String>(newCell.getValue());

							// get PK value and remove it from row (for local search)
//							String pknewval = newRow.remove(pknew);
							for (String key : newCell.getValue().keySet()) {
								if (key.startsWith("[")) {
									newRow.remove(key);
								}
							}
							
//							TreeMap<String, String> oldRow = m.isRowOnLocal(pair.getLeft(), pkold, newRow);
							
//							oldRow.remove(pkold);
//							for (String key : newCell.getValue().keySet()) {
//								if (key.startsWith("[")) {
//									oldRow.remove(key);
//								}
//							}
//							
//							if (!oldRow.isEmpty()) {
//								existCount++;
//								if (m.compareFields(oldRow, newRow, pair) != 0) {
//									if (smID == 0) smID = pkval;
//									bgID = pkval;
//									diffCount++;
//									// pk raus, zur add liste TODO?
//								}
//							}
//							else {
								count++;
								// after local search, out PK value back
//								newRow.put(pknew, pknewval);
								newRows.addElement(newRow);
//							}
						}
						// check, if sth has happened
						if (count + diffCount > 0) {
							addCounts.put(currTab, new Tuple<Integer,Integer>(count,0));
							System.out.println(count +" n,\t"+existCount+" e,\t"+diffCount +" d\t"+currTab+(diffCount!=0?": "+smID+"-"+bgID:""));
						}
						
					} else 
						System.out.println("Table "+pair+" in new but not in old");
				}
				if (!newRows.isEmpty())
					newRowsPerTable.put(currTab, newRows);
			}
			
			for (String currTab : newRowsPerTable.keySet()) {
				System.out.println("Table "+currTab+" generated cross table?! press 'y' and enter!");
				String in = scan.next();
				// they are not relevant.
				if (in.equals("y")) continue;
				
				String pk = m.getActualPrimaryKey(currTab);
				
				// recursive foreign key handling
				for (TreeMap<String,String> newRow : newRowsPerTable.get(currTab)) {
					editFKsRecursively(newRow, currTab, pk);
				}
			}
			
			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n         NOW ALL CYCLES\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			for (String cycle : cycles) {
				System.out.println(cycle);
			}
			
			// grep -E "FS_[[:alpha:]]+ID=[0-9]{6}"  output.txt -o
			FileWriter f = new FileWriter("output.txt");
			f.write(newRowsPerTable.toString().replaceAll("\n", " "));
			f.close();
			// cat addedlist.txt | grep -o -E "[[:alpha:]]+:[0-9]{6}=[0-9]{1,6}" | sort > addedlist.sorted
			f = new FileWriter("addedlist.txt");
			f.write(addedList.toString());
			f.close();
			System.out.println("done\n");
			
			for (String key : addCounts.keySet()) {
				Tuple<Integer,Integer> t = addCounts.get(key);
				if (t.getRight() != 0)
					// print all new rows, and finally imported ones
					System.out.println(t.getLeft() + ", " + t.getRight() + "\t" + key);
			}

			scan.close();
			
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
	
	/**
	 * 
	 * @param tabForKey
	 * @param oldVal
	 * @param newVal
	 */
	private static void replaceFKandPKinAllTables(String tabForKey, String oldVal, String newVal) {
		for (String table : newRowsPerTable.keySet()) {
			replaceFKinTable(table, tabForKey, oldVal, newVal);
		}
	}
	
	/**
	 * replaces all occurences of fk and old value in table with new value
	 * @param table table to search in
	 * @param key table name for fk and pk to look for
	 * @param oldval old value
	 * @param newval new value
	 */
	private static void replaceFKinTable(String table, String key, String oldval, String newval) {
		if (!key.startsWith("FS_")) return;
		if (!newRowsPerTable.get(table).isEmpty()
				&& newRowsPerTable.get(table).firstElement().containsKey(key)) {
			for (TreeMap<String,String> row : newRowsPerTable.get(table)) {
				if (row.get(key) != null && row.get(key).equals(oldval)) {
					row.put(key, newval);
				}
			}
		}
	}
	
	private static String getTableFromKey(String key) {
		String result = key.endsWith("S_IsolatedSherdID")?"IsolatedSherdMainAbstract": key.replace("FS_", "").replace("PS_", "").replace("ID", "");
		return result;
	}
	
	private static void editFKsRecursively(TreeMap<String, String> newRow, String table, String pkField) throws FilemakerIsCrapException, SQLException, IOException, EntityManagementException, MissingPermissionException {
		if (stack == null) stack = new ArrayDeque<String>();
		if (cycles == null) cycles = new HashSet<String>();
		// table name + oldID + newID
		if (addedList == null) addedList = new HashMap<String,Integer>();
		
		// duplicate row in source? replace everywhere!
		String dup = isRowDuplicateInSource(table, pkField, newRow);
		if (!dup.equals("")) {
			replaceFKandPKinAllTables(table, newRow.get(pkField), dup);
			newRow.put(pkField, dup);
			return;
		}
		
		String pkVal = newRow.get(pkField);
		
		// huch? nochmal mit neuem pk aufgerufen!
		if (!pkVal.equals("Deutsch") && Integer.parseInt(pkVal) < 100000) {
			return;
		}
		
		if (addCounts.get(table).getLeft() < addCounts.get(table).getRight()) {
			System.out.println("!STOP! "+table+" "+addCounts.get(table).getLeft()+ " <= " + addCounts.get(table).getRight());
		}
		
		// current row already added?
		if (addedList.containsKey(getTableFromKey(pkField) + ":" + pkVal)) {
			System.out.println("top level row in "+table+" with ID "+pkVal+" already existent!");
			newRow.put(pkField, ""+addedList.get(getTableFromKey(pkField) + ":" + pkVal));
			return;
		}
		
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
				String currFK = newRow.get(key)==null?"0":newRow.get(key);
				if (!currFK.equals("Deutsch") && stack.size()>= 1 && Integer.parseInt(currFK) >= 100000) {
					
					// if row behind fk already added
					Integer alreadyAdded = addedList.get(getTableFromKey(key)+":"+ currFK);
					if (alreadyAdded != null) {
						newRow.put(key, ""+alreadyAdded);
						continue;
					}
					
					// FK table in stack? => cycle
					if (stack.contains(getTableFromKey(key))) {
						cycles.add(stack.peek() + "." + key + ":"+ newRow.get(key));
						System.out.println("CYCLE FOUND: "+stack.peek() + "." + key + ":"+ newRow.get(key));
						// if no other FKs, but cycle, then just pop up
						if (!it.hasNext()) stack.pop();
						continue;
					}
					// FK not in stack? => jump into table of fk
					else {
						String tableName = getTableFromKey(key);
						// table existent?
						if (newRowsPerTable.keySet().contains(tableName)) {
							// bool to check, if row for key val is existent at all
							boolean keyExists = false;
							// nested pk name
							String pknew = m.getActualPrimaryKey(tableName);
							// jump into table
							// look for correct row
							for (TreeMap<String,String> row : newRowsPerTable.get(tableName)) {
								// pk of child row equal to fk of current row?
								if (row.get(pknew).equals(currFK)) {
									keyExists = true;
									editFKsRecursively(row, tableName, pknew);
									break;
								}
							}
							// row for given fk is not existent! -> overwrite with null
							if (!keyExists) {
								newRow.put(key, null);
							}
						}
					}
				}
			}
			// looped through all FKs? add row to local db
			addAndReplace(newRow.get(pkField), pkField, newRow);
		}
		// no FK's
		else {
			addAndReplace(newRow.get(pkField), pkField, newRow);
		}
		// irgendwie neue row applyen
		return;
	}

	private static String isRowDuplicateInSource(String table, String pkField, TreeMap<String, String> nRow) {
		TreeMap<String, String> newRow = new TreeMap<String, String>(nRow);
		newRow.remove(pkField);
		
		int count = 0;
		
		for (TreeMap<String, String> r : newRowsPerTable.get(table)) {
			TreeMap<String, String> row = new TreeMap<String, String>(r);
			String dup = row.remove(pkField);
			if (row.equals(newRow)) {
				if (count >= 1) return dup;
				else count++;
			}
		}
		return "";
	}

	private static void addAndReplace(String pkVal, String pk, TreeMap<String, String> newRow) throws SQLException, IOException, EntityManagementException, FilemakerIsCrapException, MissingPermissionException {
		String tab = stack.pop();
		int pkNew = 0;
		int pkOld = Integer.parseInt(pkVal);
		// if this row already added, get this id
		if (addedList.containsKey(getTableFromKey(pk)+":"+pkOld)){
			pkNew = addedList.get(getTableFromKey(pk)+":"+pkOld);
		}
		// else add row, keep new id!
		else {
			// add row to db, pkNew = return value
			pkNew = m.insertRowIntoLocal(tab, pk, newRow);
			increaseCount(tab);
			// add row with OLD pk to addedList
			addedList.put(getTableFromKey(pk)+":"+pkOld, pkNew);
		}
		// ENTWEDER
		// != stack top?
		if (stack.size() > 0) {
			// replace old fk value in parent row with new value
			replaceFKinTable(stack.peek(), getTableFromKey(pk), pkOld+"", pkNew+"");
		}
		// ODER
//		replaceFKandPKinAllTables(tab, pkOld+"", pkNew+"");
	}
	
	private static void increaseCount(String tab) {
		Tuple<Integer, Integer> tmp;
		if (addCounts.containsKey(tab)) {
			tmp = addCounts.get(tab);
			tmp.setRight(tmp.getRight()+1);
			addCounts.put(tab, tmp);
		}
	}

	private static String getFKFromTabName(String tab) {
		if (tab.equals("Ort")) return "FS_PlaceID";
		if (tab.equals("IsolatedSherdMainAbstract")) return "FS_IsolatedSherdID";
		else return "FS_"+tab+"ID";
	}
}
