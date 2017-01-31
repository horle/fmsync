package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
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
import javax.swing.JProgressBar;
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
import javax.swing.text.DefaultCaret;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import data.SQLSyncConnector;
import data.CallManager;

/**
 * CeramalexSync main window to control DB sync actions
 * 
 * @author horle (Felix Kussmaul)
 */

public class MainWindow {

	private JFrame frame;
	private JPanel panelFMF;
	private JPanel panelPrefs;
	private JPanel panelLog;
	private JPanel panelActions;
	
	private JTextField txtAddress;
	private JFormattedTextField txtPort;
	private JTextArea txtLog;
	private JList<File> listFiles;
	private DefaultListModel<File> model;
	
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
	private JButton btnAddFile;
	private JButton btnRemFile;
	
	private final String URL_PREFIX = "jdbc:mysql://";
	private boolean connected;
	private JTextField txtUser;
	private JPasswordField txtPass;
	private JScrollPane scrollPane;
	private JProgressBar progressBar;

	private final int CONN_TIMEOUT = 5;
	protected static Logger logger = Logger.getLogger("gui.mainwindow");

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		DOMConfigurator.configureAndWatch("ressource/log4j.xml");
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
		frame.setResizable(false);
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
						model.addElement(f);
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
		
		model = new DefaultListModel<File>();
		listFiles = new JList<File>();
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
		DefaultCaret caret = (DefaultCaret)txtLog.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
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
				
				String a = txtAddress.getText();
				int port = Integer.parseInt(txtPort.getText());
				String user = txtUser.getText();
				String pwd = new String(txtPass.getPassword());// "pvnnyEMpQHSfBXvW";
				
				/**
				 * Establish new connection to DB
				 */
				if (!connected){
					
					/**
					 * Create watch dog SwingWorker to control timeout 
					 */
					SwingWorker<Void,Void> watcher = new SwingWorker<Void,Void>() {
						
						@Override
						protected Void doInBackground() throws Exception {
							
							/**
							 * SwingWorker to connect to DB and update GUI elements
							 * 
							 * @return true if connection established
							 * @return false if connection failed
							 */
							ConnectionWorker worker = new ConnectionWorker(txtLog, btnConnect, lblConnect, txtPort, txtAddress) {
								
								SyncStatus closed = new SyncStatus(null, "Not connected.", "Connect to DB", true, true, true);
								SyncStatus connecting = new SyncStatus("Trying to connect to "+a+" as "+ user +" ...\n", "Not connected.", "Connecting ...", false, false, false);
								SyncStatus open = new SyncStatus("Connected to "+a+".\n", "Connected to DB.", "Disconnect", true, false, false);
								SyncStatus closedError = new SyncStatus("Connection as "+user+" to "+a+" on port "+port+" failed!\n", "Not connected.", "Connect to DB", true, true, true);
								SyncStatus timeoutError = new SyncStatus("Connection establishment to DB timed out.\n", "Not connected.", "Connect to DB", true, true, true);
								
								@Override
								protected Boolean doInBackground() throws InterruptedException {
									
									publish(connecting);
									
									try {
										SQLSyncConnector connector = SQLSyncConnector.initPrefs(a+":"+port+"/",user,pwd,"ceramalex");
										if (connector.isConnected()){
											connected = true;
											publish(open);
											return true;
										}
										else
											return false;
									} catch (SQLException e) {
										publish(closedError);
//										this.cancel(true);
										SwingUtilities.invokeLater(new Runnable(){
											@Override
											public void run() {
												String msg = "";
												if (e.getMessage().startsWith("Communications link failure"))
													msg = "Server does not answer.";
												if (e.getMessage().startsWith("Access denied"))
													msg = "Access denied. Wrong user name or password.";
												JOptionPane.showMessageDialog(frame, "Connection error: "+msg, "Connection failure", JOptionPane.WARNING_MESSAGE);
											};
										});
										return false;
									}
								}
								@Override
								protected void done() {
									try {
										// connection open?
										if (!this.get()){
											publish(closed);
										}
									} catch (InterruptedException | ExecutionException e) {
										publish(closedError);
									} catch (CancellationException e) {
										publish(timeoutError);
									}
								}
							};
							worker.execute();
							try {
								worker.get(CONN_TIMEOUT, TimeUnit.SECONDS);
							} catch (InterruptedException | ExecutionException | TimeoutException e1) {
								worker.cancel(true);
								JOptionPane.showMessageDialog(frame, "Timeout while trying to contact the server.", "Timeout", JOptionPane.INFORMATION_MESSAGE);
							}
							return null;
						}
					};
					watcher.execute();
				}
				
				/**
				 * Disconnect current connection
				 */
				else {
					
					/**
					 * SwingWorker to disconnect and update GUI elements
					 * 
					 * @return true if connection closed
					 * @return false if closing connection failed
					 */
					ConnectionWorker worker = new ConnectionWorker(txtLog, btnConnect, lblConnect, txtPort, txtAddress) {
						
						SyncStatus closed = new SyncStatus("Connection closed.\n", "Not connected.", "Connect to DB", true, true, true);
						SyncStatus closedError = new SyncStatus("Connection as "+user+" to "+a+" on port "+port+" failed!\n", "Not connected.", "Connect to DB", true, true, true);
						
						@Override
						protected Boolean doInBackground() {
										
							SQLSyncConnector connector = SQLSyncConnector.getInstance();
							
							if (connector.close()){
								connected = false;
								publish(closed);
								return true;
							}
							else{
								publish(closedError);
								SwingUtilities.invokeLater(new Runnable(){
									@Override
									public void run() {
										JOptionPane.showMessageDialog(frame, "Connection could not be closed!", "Closing not successful", JOptionPane.ERROR_MESSAGE);
									};
								});
								return false;
							}
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
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				/**
				 * Create watch dog SwingWorker to manage progress
				 */
				SwingWorker<Void,Void> watcher = new SwingWorker<Void,Void>(){

					@Override
					protected Void doInBackground() throws Exception {
						
						ArrayList<File> files = new ArrayList<File>();
						
						if (listFiles.getModel().getSize() == 0){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run() {
									JOptionPane.showMessageDialog(frame, "No FileMaker files selected!", "Missing input", JOptionPane.WARNING_MESSAGE);
									return;
								};
							});
						}
						
						for (int i = 0; i < listFiles.getModel().getSize(); i ++){
							files.add(listFiles.getModel().getElementAt(i));
						}
						
						if(btnDownload.isSelected()){
							
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run() {
									progressBar.setVisible(true);
									progressBar.setStringPainted(true);
									progressBar.setValue(0);
								};
							});
							
							/**
							 * Create SwingWorker to manage download
							 */
							ProgressWorker worker = new ProgressWorker(){
		
								@Override
								protected Boolean doInBackground() throws Exception {
									
									return CallManager.getInstance().startDownload();
								}
								
							};
							worker.execute();
						}
						if(btnUpload.isSelected()){
							
							/**
							 * Create SwingWorker to manage download
							 */		
							SwingWorker<Boolean,Void> worker = new SwingWorker<Boolean,Void>(){
		
								@Override
								protected Boolean doInBackground() throws Exception {
									
									return CallManager.getInstance().startUpload();
								}
							};
							worker.execute();
						}
						return null;
					}
				};
				watcher.execute();
			}
		});
		panelActions.add(btnStart);
		
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
		
		progressBar = new JProgressBar(0, listFiles.getModel().getSize());
		progressBar.setBounds(12, 368, 157, 25);
		panelActions.add(progressBar);
		
		URL url = getClass().getResource("/resources/logo.png");		
		JLabel lblPicture = new JLabel(new ImageIcon(url));
		lblPicture.setBounds(12, 12, 671, 70);
		frame.getContentPane().add(lblPicture);
	
	}
}

