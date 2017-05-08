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
import ceramalex.sync.model.ComparisonResult;
import ceramalex.sync.model.Pair;
import ceramalex.sync.model.SQLDataModel;

public class ProgressWorker extends SwingWorker<Void, String> {
	public static final int JOB_CALC_DIFF = 0;
	public static final int JOB_APPLY_CHANGES = 1;
	private JTextArea txtLog;
	private SQLDataModel data;
	private int job;
	
	public ProgressWorker(JTextArea txtLog, int job) throws IOException, SQLException{
		this.data = SQLDataModel.getInstance();
		this.txtLog = txtLog;
		this.job = job;
	}
	
	@Override
	protected Void doInBackground() throws Exception {
		ArrayList<Pair> commonTables = data.fetchCommonTables();
		
		setProgress(0);

		switch (job) {
		case JOB_CALC_DIFF:
			for (int i = 0; i < commonTables.size(); i++) {
				if (!isCancelled()) {
					Pair p = commonTables.get(i);
					String out = "Preparing table " + p.getLeft() +" ...";
					
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
		case JOB_APPLY_CHANGES:
			ArrayList<ComparisonResult> results = data.getResults();
			publish("\n%%%%%%%%%%%%%% APPLYING CHANGES %%%%%%%%%%%%%%\n");
			for (int i = 0; i < results.size(); i++) {
				setProgress(0);
				if (!isCancelled()) {
					ComparisonResult c = results.get(i);
					int total = c.getDeleteList().size()
							+ c.getConflictList().size() 
							+ c.getDownloadList().size() 
							+ c.getUploadList().size() 
							+ c.getLocalUpdateList().size()
							+ c.getRemoteUpdateList().size();
					
					publish("Applying changes to table " + c.getTableName().getLeft() +":\n");
					
					if (!c.getDownloadList().isEmpty()) {
						publish("Downloading " + c.getDownloadList().size() +" entries ... ");
						data.prepareRowsAndDownload(c.getTableName(), c.getDownloadList(), 25);
						setProgress(100*(i/results.size()) + total/4*(100/results.size()));
						publish("done.\n");
					}
					if (!c.getUploadList().isEmpty()) {
						publish("Uploading " + c.getUploadList().size() +" entries ... ");
						data.prepareRowsAndUpload(c.getTableName(), c.getUploadList(), 25);
						setProgress(100*(i/results.size()) + total/2*(100/results.size()));
						publish("done.\n");
					}
					setProgress(100*(i/results.size()));
					publish(" done.\n");
				} else return null;
			}
			publish("\n%%%%%%%%%%%%%%%%%% DONE %%%%%%%%%%%%%%%%%%\n");
			return null;
		
		default:
			return null;
		}
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
