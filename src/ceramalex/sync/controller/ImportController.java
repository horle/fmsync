package ceramalex.sync.controller;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import ceramalex.sync.data.MySQLDataAccess;
import model.RecordModel;

public class ImportController {

	/** Logging-Instanz */
	protected static Logger logger = Logger.getLogger("controller.importcontroller");

	/** Filemaker-Connector */
	private MySQLDataAccess mDAO = null;
	
	/** Aktuell verbundene Datenbank */
	private String mCurrentDatabase = null;
	
	//------------------------------------------------------------------------
	/**
	 * Standardkonstruktor
	 */
	public ImportController() {
		
		
	}
	//------------------------------------------------------------------------
	/**
	 * Methode durchlaeuft alle Uebergaberecords und schreibt diese in die Zieldatenbank
	 * @param records 
	 */
	public void process(List<RecordModel> records) {
		ConfigController conf = ConfigController.getInstance();
	
		int count = records.size();
		RecordModel currentRecord = null;
		
		for(int i = 0; i < records.size(); i++) {
			currentRecord = records.get(i);
			
			// wenn fuer die aktuelle Datenbank noch kein DAO erzeugt wurde, erzeuge eins
			if(currentRecord.getDatabase() != mCurrentDatabase) {
				logger.info("Creating DAO for Database: " + currentRecord.getDatabase());
				try {
					mDAO = new MySQLDataAccess(conf.getFmURL(), conf.getFmUser(), conf.getFmPassword(), currentRecord.getDatabase());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mCurrentDatabase = currentRecord.getDatabase();
			}
			
			if(!mDAO.doSQLModify(records.get(i).getSQLInsert())) count--;
			//break;
		}
		
		logger.info(count + " of " + records.size() + " Records have been successfully written to Filemaker.");
	}
	//------------------------------------------------------------------------

}
