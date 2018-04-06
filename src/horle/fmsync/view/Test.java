package horle.fmsync.view;

import java.awt.EventQueue;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

public class Test extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Test frame = new Test();
					frame.pack();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Test() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 413, 276);
		
		JSplitPane splitPane = new JSplitPane();
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 1, 0, 0));
		
		JTable t1 = new JTable();
		DefaultTableModel model = new DefaultTableModel(new Object[][]{},new String[]{"A","B","C"});
		t1.setModel(model);
		JScrollPane scrollPane_2 = new JScrollPane(t1);
		scrollPane_2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane_2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new GridLayout(1, 1, 0, 0));
		
		JTable t2 = new JTable();
		t2.setModel(model);
		JScrollPane scrollPane_1 = new JScrollPane(t2);
		scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		panel_1.add(scrollPane_1);
		panel.add(scrollPane_2);
		
		splitPane.setRightComponent(panel_1);
		splitPane.setLeftComponent(panel);
		
		JScrollPane scrollPane = new JScrollPane(splitPane);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPane);
	}

}
