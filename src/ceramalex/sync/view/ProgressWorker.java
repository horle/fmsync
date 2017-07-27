package ceramalex.sync.view;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

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
			try {
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
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		case JOB_APPLY_CHANGES:
			try {
				Collection<ComparisonResult> results = data.getResults().values();
				publish("\n%%%%%%%%%%%%%% APPLYING CHANGES %%%%%%%%%%%%%%\n");
				int i = 0;
				for (ComparisonResult c : results) {
					setProgress(0);
					if (!isCancelled()) {
						int total = c.getDeleteList().size()
								+ c.getConflictList().size() 
								+ c.getDownloadList().size() 
								+ c.getUploadList().size() 
								+ c.getLocalUpdateList().size()
								+ c.getRemoteUpdateList().size();
						
						if (total > 0) {
							publish("Applying changes to table " + c.getTableName().getLeft() +":\n");
							
							if (!c.getDownloadList().isEmpty()) {
								publish("Downloading " + c.getDownloadList().size() +" entries ... ");
								data.prepareRowsAndDownload(c.getTableName(), c.getDownloadList(), 25);
								publish("done.\n");
							}
							if (!c.getUploadList().isEmpty()) {
								publish("Uploading " + c.getUploadList().size() +" entries ... ");
								data.prepareRowsAndUpload(c.getTableName(), c.getUploadList(), 25);
								publish("done.\n");
							}
							if (!c.getLocalUpdateList().isEmpty()) {
								publish("Updating " + c.getLocalUpdateList().size() +" entries in local database ... ");
								data.updateRowsLocally(c.getTableName(), c.getLocalUpdateList());
								publish("done.\n");
							}
							if (!c.getRemoteUpdateList().isEmpty()) {
								publish("Updating " + c.getRemoteUpdateList().size() +" entries in remote database ... ");
								data.updateRowsRemotely(c.getTableName(), c.getRemoteUpdateList());
								publish("done.\n");
							}
							setProgress(100*(i/results.size()));
							publish("Table "+c.getTableName().getLeft()+" done.\n");
						}
					} else return null;
					i++;
				}
				publish("%%%%%%%%%%%%%%%%%%%% DONE %%%%%%%%%%%%%%%%%%%%\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
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
