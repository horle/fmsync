package ceramalex.sync.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;

public class ProgressWorker extends SwingWorker<Boolean, String[]> {

	private JLabel lblEntire;
	private JLabel lblCurrTab;
	private JTextArea txtLog;
	private SQLDataModel data = MainFrame.data;
	
	public ProgressWorker(JLabel lblEntire, JLabel lblCurrTab, JTextArea txtLog){
		this.lblCurrTab = lblCurrTab;
		this.lblEntire = lblEntire;
		this.txtLog = txtLog;
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		int progress = 0;
		
		ArrayList<Pair> commonTables = data.getCommonTables();
		
		setProgress(0);
		
		for (int i = 0; i < commonTables.size(); i++) {
			String[] arr = {"","",""};
			arr[0] = "Current table: " + commonTables.get(i).toString() + " (" + (i+1) + "/" + commonTables.size() + ")";
			arr[1] = "Current row: " + i;
			arr[2] = "Fetching " + commonTables.get(i).toString() +" ...";
			
			publish(arr);
			setProgress(100*(i/commonTables.size()));
		}
		
		return true;
	}
	
	@Override
	protected void process(List<String[]> listProgress) {
		for (String[] arr : listProgress) {
			lblEntire.setText(arr[0]);
			lblCurrTab.setText(arr[1]);
			txtLog.append(arr[2] + "\n");
		}
		return;
	}
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		return;
	}

}
