package ceramalex.sync.view;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;

public class ProgressWorker extends SwingWorker<Void, String[]> {
	
	private JLabel lblCurrTab;
	private JTextArea txtLog;
	private SQLDataModel data;
	
	public ProgressWorker(JLabel lblCurrTab, JTextArea txtLog) throws IOException, SQLException{
		this.data = SQLDataModel.getInstance();
		this.lblCurrTab = lblCurrTab;
		this.txtLog = txtLog;
	}
	
	@Override
	protected Void doInBackground() throws Exception {
		ArrayList<Pair> commonTables = data.fetchCommonTables();
		
		String[] arr = {"",""};
		setProgress(0);
		
		for (int i = 0; i < commonTables.size(); i++) {
			Pair p = commonTables.get(i);
			arr[0] = "Current table: " + p.getLeft() + " (" + (i+1) + "/" + commonTables.size() + ")";
			arr[1] = "Fetching table " + p.getLeft() +" ...";
			
			publish(arr);
			setProgress(100*(i/commonTables.size()));
			
			try {
				data.getDiffByUUID(p, true, true);
			} catch ( Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	protected void process(List<String[]> listProgress) {
		for (String[] arr : listProgress) {
			lblCurrTab.setText(arr[0]);
			txtLog.append(arr[1] + "\n");
		}
		return;
	}
}
