package ceramalex.sync.view;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import ceramalex.sync.exception.EntityManagementException;
import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;

public class ProgressWorker extends SwingWorker<Void, String> {
	
	private JTextArea txtLog;
	private SQLDataModel data;
	
	public ProgressWorker(JTextArea txtLog) throws IOException, SQLException{
		this.data = SQLDataModel.getInstance();
		this.txtLog = txtLog;
	}
	
	@Override
	protected Void doInBackground() throws Exception {
		ArrayList<Pair> commonTables = data.fetchCommonTables();
		
		setProgress(1);
		
		for (int i = 0; i < commonTables.size(); i++) {
			if (!isCancelled()) {
				Pair p = commonTables.get(i);
				String out = "Processing table " + p.getLeft() +" ...";
				
				publish(out);
				setProgress(100*(i/commonTables.size()));
				
				try {
					data.calcDiff(p, true, true);
				} catch (EntityManagementException e) {
					e.printStackTrace();
				}
				publish(" done.\n");
			} else return null;
		}
		return null;
	}
	
	@Override
	protected void process(List<String> listProgress) {
		for (String arr : listProgress) {
//			monitor.setText(arr[0]);
			txtLog.append(arr);
		}
		return;
	}
	
	@Override
	protected void done() {
		try { get();
		} catch (Exception e) {e.printStackTrace();
		
		}
	}
}
