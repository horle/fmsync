package ceramalex.sync.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import ceramalex.sync.model.ComparisonResult;
import ceramalex.sync.model.Pair;

class QueryWorker extends SwingWorker<Void, Integer[]> {

	private CountDownLatch latch;
	private JLabel lblEntire;
	private JLabel lblCurrTab;
	private JProgressBar proEntire;
	private JProgressBar proTab;
	private JTextArea txtLog;
	
	private Pair currTab;

	public QueryWorker(Pair pair, JLabel lblEntire, JLabel lblCurrTab,
			JProgressBar proEntire, JProgressBar proTab, JTextArea txtLog,
			CountDownLatch latch) {
		this.latch = latch;
		this.lblEntire = lblEntire;
		this.lblCurrTab = lblCurrTab;
		this.proEntire = proEntire;
		this.proTab = proTab;
		this.txtLog = txtLog;
		this.currTab = pair;
	}

	@Override
	protected Void doInBackground() throws Exception {
		
		ComparisonResult comp = MainFrame.data.getDiffByUUID(currTab, true, true);
		Integer[] arr = new Integer[3];
		arr[0] = MainFrame.data.getCommonTables().indexOf(currTab);
		arr[1] = 
		publish(arr);
		
		return null;
	}

	@Override
	protected void process(List<Integer[]> listProgress) {
		for (Integer[] arr : listProgress) {
			lblEntire.setText("Current table: " + currTab + "(" + arr[0] + "/" + MainFrame.data.getCommonTables().size() + ")");
			lblCurrTab.setText(arr[1]);
			txtLog.append("Fetching " + currTab.getLeft() + " ... \n");
		}
		return;
	}

	@Override
	protected void done() {
		latch.countDown();
	}
}