package view;

import java.util.List;

import javax.swing.SwingWorker;

public class ProgressWorker extends SwingWorker<Boolean, Integer> {

	public ProgressWorker(){
		
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		int progress = 0;
		setProgress(0);
		
		while (progress < 100){
			try{Thread.sleep(1000);}
			catch (Exception e){};
			progress +=1;
			setProgress(Math.min(progress, 100));
		}
		return true;
	}
	
	@Override
	protected void process(List<Integer> listProgress) {
		// TODO Auto-generated method stub
		return;
	}
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		return;
	}

}
