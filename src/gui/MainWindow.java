package gui;

import gui.ConnectionWorker.SyncStatus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import data.SQLSyncConnector;

public class MainWindow {

	private JFrame frame;
	private JPanel panelFMF;
	private JPanel panelPrefs;
	private JPanel panelLog;
	private JPanel panelActions;
	
	private JTextField txtAddress;
	private JFormattedTextField txtPort;
	private JTextArea txtLog;
	private JList<String> listFiles;
	private DefaultListModel<String> model;
	
	private JLabel lblPort;
	private JLabel lblAddress;
	private JLabel lblAuthor;
	private JLabel lblPlace;
	private JLabel lblConnect;
	
	private JComboBox<String> cmbPlace;
	private JComboBox<String> cmbAuthor;
	
	private JToggleButton btnDownload;
	private JToggleButton btnUpload;
	private JButton btnConnect;
	private JButton btnStart;
	private JButton btnCancel;
	private JButton btnAddFile;
	private JButton btnRemFile;
	
	private final String URL_PREFIX = "jdbc:mysql://";
	private boolean connected;
	private JTextField txtUser;
	private JPasswordField txtPass;
	private JScrollPane scrollPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		connected = false;
		
		frame = new JFrame();
		frame.setTitle("CeramAlexSync");
		frame.setBounds(100, 100, 697, 583);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		panelFMF = new JPanel();
		panelFMF.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "FileMaker File(s)", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelFMF.setBounds(12, 89, 478, 127);
		frame.getContentPane().add(panelFMF);
		panelFMF.setLayout(null);
		
		btnAddFile = new JButton("Directory");
		btnAddFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("FileMaker Files (*.fmp12, *.tab)", "fmp12", "tab");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.addChoosableFileFilter(filter);
				chooser.setAcceptAllFileFilterUsed(false);
				
				int result = chooser.showOpenDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					File[] filesInDir = chooser.getSelectedFile().listFiles(new FilenameFilter() {
						public boolean accept (File dir, String name) {
							return name.toLowerCase().endsWith(".fmp12") || name.toLowerCase().endsWith(".tab");
						}
					});
					model.clear();
					for (File f : filesInDir) {
						model.addElement(f.getName());
					}
				}
			}
		});
		btnAddFile.setBounds(340, 36, 126, 25);
		panelFMF.add(btnAddFile);
		
		btnRemFile = new JButton("Clear list");
		btnRemFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				model.clear();
			}
		});
		btnRemFile.setBounds(340, 73, 126, 25);
		panelFMF.add(btnRemFile);
		
		model = new DefaultListModel<String>();
		listFiles = new JList<String>();
		listFiles.setModel(model);
		listFiles.setBounds(12, 25, 316, 90);
		panelFMF.add(listFiles);
		
		panelPrefs = new JPanel();
		panelPrefs.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Preferences", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelPrefs.setBounds(12, 228, 478, 190);
		frame.getContentPane().add(panelPrefs);
		panelPrefs.setLayout(null);
		
		lblAddress = new JLabel("MySQL Server Address");
		lblAddress.setBounds(12, 22, 325, 15);
		panelPrefs.add(lblAddress);
		
		txtAddress = new JTextField(50);
		txtAddress.setBounds(12, 43, 325, 24);
		panelPrefs.add(txtAddress);
		txtAddress.setText(URL_PREFIX + "arachne.dainst.org");
		((AbstractDocument) txtAddress.getDocument()).setDocumentFilter(new DocumentFilter() {
			@Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (offset < URL_PREFIX.length()) {
                    return;
                }
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (offset < URL_PREFIX.length()) {
                    length = Math.max(0, length - URL_PREFIX.length());
                    offset = URL_PREFIX.length();
                }
                super.replace(fb, offset, length, text, attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (offset < URL_PREFIX.length()) {
                    length = Math.max(0, length + offset - URL_PREFIX.length());
                    offset = URL_PREFIX.length();
                }
                if (length > 0) {
                    super.remove(fb, offset, length);
                }
            }
		});
		txtAddress.setColumns(10);
		
		NumberFormat format = NumberFormat.getInstance();
	    NumberFormatter formatter = new NumberFormatter(format);
	    format.setGroupingUsed(false);
		formatter.setValueClass(Integer.class);
	    formatter.setMinimum(0);
	    formatter.setMaximum(65535);
	    formatter.setAllowsInvalid(false);
		txtPort = new JFormattedTextField(formatter);
		txtPort.setBounds(343, 43, 123, 24);
		panelPrefs.add(txtPort);
		txtPort.setValue(3306);	//default port mySQL
		txtPort.setColumns(10);
		
		lblPort = new JLabel("Port");
		lblPort.setBounds(343, 22, 123, 15);
		panelPrefs.add(lblPort);
		
		lblPlace = new JLabel("Particular Place");
		lblPlace.setBounds(12, 79, 454, 15);
		panelPrefs.add(lblPlace);
		
		lblAuthor = new JLabel("Particular Author");
		lblAuthor.setBounds(12, 136, 454, 15);
		panelPrefs.add(lblAuthor);
		
		cmbPlace = new JComboBox<String>();
		cmbPlace.setBounds(12, 100, 454, 24);
		panelPrefs.add(cmbPlace);
		
		cmbAuthor = new JComboBox<String>();
		cmbAuthor.setBounds(12, 155, 454, 24);
		panelPrefs.add(cmbAuthor);
		
		panelLog = new JPanel();
		panelLog.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelLog.setBounds(12, 430, 478, 112);
		frame.getContentPane().add(panelLog);
		panelLog.setLayout(new BorderLayout(0, 0));
				
		txtLog = new JTextArea();
		txtLog.setEditable(false);
		txtLog.setLineWrap(true);
		txtLog.setWrapStyleWord(true);
		
		scrollPane = new JScrollPane(txtLog);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panelLog.add(scrollPane);
		
		panelActions = new JPanel();
		panelActions.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Actions", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelActions.setBounds(502, 89, 181, 453);
		frame.getContentPane().add(panelActions);
		panelActions.setLayout(null);
		
		btnDownload = new JToggleButton("Download");
		btnDownload.setBounds(12, 73, 157, 25);
		panelActions.add(btnDownload);
		
		btnUpload = new JToggleButton("Upload");
		btnUpload.setBounds(12, 36, 157, 25);
		panelActions.add(btnUpload);
		
		btnConnect = new JButton("Connect to DB");
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if (!connected){
					String a = txtAddress.getText();
					int port = Integer.parseInt(txtPort.getText());
					String user = txtUser.getText();
					String pwd = new String(txtPass.getPassword());// "pvnnyEMpQHSfBXvW";
					
					SwingWorker<Void,Void> watcher = new SwingWorker<Void,Void>() {

						@Override
						protected Void doInBackground() throws Exception {
							
							ConnectionWorker worker = new ConnectionWorker(txtLog, btnConnect, lblConnect, txtPort, txtAddress) {
						
								// neuer thread für hintergrund
								@Override
								protected Void doInBackground() {
									SyncStatus connecting = new SyncStatus("Trying to connect to "+a+" as "+ user +" ...\n", "Not connected.", "Connecting ...", false, false, false);
									SyncStatus open = new SyncStatus("Connected to "+a+".\n", "Connected to MySQL DB.", "Disconnect", true, false, false);
									SyncStatus closedError = new SyncStatus("Connection as "+user+" to "+a+" on port "+port+" failed!\n", "Not connected.", "Connect to DB", true, true, true);
									
									publish(connecting);
									
									try {
										SQLSyncConnector connector = SQLSyncConnector.initPrefs(a+":"+port+"/",user,pwd,"ceramalex");
										if (connector.isConnected()){
											connected = true;
											publish(open);
										}
									} catch (Exception e) {
										publish(closedError);
										JOptionPane.showMessageDialog(frame, "Connection error. Wrong URL, username or password.", "Connection failure", JOptionPane.WARNING_MESSAGE);
										this.cancel(true);
									}
									return null;
								}
								@Override
								protected void done() {
									SyncStatus closed = new SyncStatus(null, "Not connected.", "Connect to DB", true, true, true);
									publish(closed);
								}
							};
							worker.execute();
							try {
								worker.get(5, TimeUnit.SECONDS);
							} catch (InterruptedException | ExecutionException | TimeoutException e1) {
								worker.cancel(true);
								txtLog.append("Connection timed out.\n");
								JOptionPane.showMessageDialog(frame, "Timeout while trying to contact the server.", "Timeout", JOptionPane.INFORMATION_MESSAGE);
							}
							return null;
						}
					};
					watcher.execute();
				}
				// disconnect ...
				else {
					
					ConnectionWorker worker = new ConnectionWorker(txtLog, btnConnect, lblConnect, txtPort, txtAddress) {
						
						// neuer thread für hintergrund
						@Override
						protected Void doInBackground() {
							System.out.println(EventQueue.isDispatchThread());
							String a = txtAddress.getText();
							int port = Integer.parseInt(txtPort.getText());
							String user = txtUser.getText();
							
							SyncStatus closed = new SyncStatus("Connection closed.\n", "Not connected.", "Connect to DB", true, true, true);
							SyncStatus closedError = new SyncStatus("Connection as "+user+" to "+a+" on port "+port+" failed!\n", "Not connected.", "Connect to DB", true, true, true);
										
							SQLSyncConnector connector = SQLSyncConnector.getInstance();
							
							if (connector.close()){
								connected = false;
								publish(closed);
							}
							else{
								publish(closedError);
								JOptionPane.showMessageDialog(frame, "Connection could not be closed!", "Closing not successful", JOptionPane.ERROR_MESSAGE);
							}
							return null;
						}
					};
					worker.execute();
				}
			}
		});
		btnConnect.setBounds(12, 259, 157, 25);
		panelActions.add(btnConnect);
		
		lblConnect = new JLabel("Not connected.");
		lblConnect.setBounds(12, 296, 157, 15);
		panelActions.add(lblConnect);
		
		btnStart = new JButton("START");
		btnStart.setBounds(12, 405, 157, 25);
		panelActions.add(btnStart);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(12, 368, 157, 25);
		panelActions.add(btnCancel);
		
		txtUser = new JTextField();
		txtUser.setText("ceramalex");
		txtUser.setBounds(12, 167, 157, 24);
		panelActions.add(txtUser);
		txtUser.setColumns(10);
		
		txtPass = new JPasswordField();
		txtPass.setBounds(12, 223, 157, 24);
		panelActions.add(txtPass);
		
		JLabel lblPass = new JLabel("MySQL Password");
		lblPass.setBounds(12, 203, 157, 15);
		panelActions.add(lblPass);
		
		JLabel lblUser = new JLabel("MySQL Username");
		lblUser.setBounds(12, 143, 157, 15);
		panelActions.add(lblUser);
		
		JLabel lblPicture = new JLabel("");
		lblPicture.setBounds(12, 12, 671, 70);
		frame.getContentPane().add(lblPicture);
	
	}
}

