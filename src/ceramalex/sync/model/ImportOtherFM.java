package ceramalex.sync.model;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeMap;

import com.google.common.collect.TreeBasedTable;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.exception.FilemakerIsCrapException;

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
			SQLDataModel m = SQLDataModel.getInstance();
			
			for (String currTab : m.fetchFMTables()) {
				
				TreeBasedTable<Integer, Integer, TreeMap<String, String>> table = m.getWholeFMTable(currTab);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (FilemakerIsCrapException e) {
			e.printStackTrace();
		}
		
		
	}

}
