package data;

import org.apache.log4j.xml.DOMConfigurator;

import ceramalex.filemaker.controller.FMPResetController;
import ceramalex.filemaker.controller.ImportController;
import ceramalex.filemaker.controller.StandardRelationshipController;
import controller.MySQLToFilemakerController;

/**
 * Singleton class to manage calls to PGunia projects
 * 
 * @author horle (Felix Kussmaul)
 */

public class CallManager {

	private static CallManager manager;
	
	public static CallManager getInstance(){
		if (CallManager.manager == null)
			CallManager.manager = new CallManager();
		
		return CallManager.manager;
	}
	
	private CallManager(){
		
	}
	
	public boolean startUpload(){
		DOMConfigurator.configureAndWatch("ressource/log4j.xml");
		ImportController importController = new ImportController();
		// importController.process();
		
		StandardRelationshipController relationshipUpdateController = new StandardRelationshipController();
		relationshipUpdateController.process();
		
		FMPResetController resetController = new FMPResetController();
		resetController.process();
		
		return true;
	}
	
	public boolean startDownload(){
		DOMConfigurator.configureAndWatch("ressource/log4j.xml");
		MySQLToFilemakerController controller = new MySQLToFilemakerController();
		
		return true;
	}
	
}
