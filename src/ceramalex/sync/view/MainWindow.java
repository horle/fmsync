package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.data.CallManager;

/**
 * CeramalexSync main window to control DB sync actions
 * 
 * @author horle (Felix Kussmaul)
 */

public class MainWindow {

	private static final String FM_URL_PREFIX = "jdbc:filemaker://";
	private static final String MYSQL_URL_PREFIX = "jdbc:mysql://";
	private JFrame frame;
	private JPanel panelPrefs;
	private JPanel panelLog;
	private JPanel panelActions;

	private JTextField txtMySQLAddress;
	private JFormattedTextField txtMySQLPort;
	private JTextArea txtLog;

	private JLabel lblMySQLPort;
	private JLabel lblMySQLAddress;
	private JLabel lblAuthor;
	private JLabel lblPlace;
	private JLabel lblMySQLConnect;

	private JComboBox<String> cmbPlace;
	private JComboBox<String> cmbAuthor;

	private JToggleButton btnDownload;
	private JToggleButton btnUpload;
	private JButton btnConnect;
	private JButton btnStart;

	private boolean connected;
	private JTextField txtMySQLUser;
	private JPasswordField txtMySQLPass;
	private JScrollPane scrollPane;
	// private JProgressBar progressBar;

	private final int CONN_TIMEOUT = 5;
	protected static Logger logger = Logger
			.getLogger("ceramalex.sync.view.mainwindow");
	private static ConfigController config;
	private JTextField txtFMAddress;
	private JFormattedTextField txtFMPort;
	private JLabel lblFMPort;
	private JLabel lblFMAddress;
	private JLabel lblFMPass;
	private JPasswordField txtFMPass;
	private JLabel lblFMUser;
	private JTextField txtFMUser;
	private JLabel lblFMConnect;
	private JLabel lblMySQLDB;
	private JTextField txtMySQLDB;
	private JTextField txtFMDB;
	private JLabel lblFMDB;
	private JLabel lblMySQLPass;
	private JLabel lblMySQLUser;

	private ConnectionWorker worker;

	private SyncStatus closed;
	private SyncStatus connecting;
	private SyncStatus open;
	private SyncStatus closedError;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		DOMConfigurator.configureAndWatch("ressource/log4j.xml");
		config = ConfigController.getInstance();

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
		reconfigure();
		initSyncStatus();
	}

	private void initSyncStatus() {
		closed = new SyncStatus("", "Not connected to MySQL.",
				"Not connected to FM.", "Connect to DBs", true, true, false);
		connecting = new SyncStatus("Trying to connect to "
				+ config.getMySQLURL() + ":" + config.getMySQLPort() + " as "
				+ config.getMySQLUser() + " ...\nTrying to connect to "
				+ config.getFmURL() + ":2399" + " as " + config.getFmUser()
				+ " ...", "Not connected to MySQL.", "Not connected to FM.",
				"Connecting ...", false, false, false);
		open = new SyncStatus(
				"Successfully connected to MySQL.\nSuccessfully connected to FM.",
				"Disconnect", "Connected to MySQL.", "Connected to FM.", false,
				false, true);
		closedError = new SyncStatus("Connection failed. ",
				"Not connected to MySQL.", "Not connected to FM.",
				"Connect to DBs", true, true, false);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		connected = false;

		frame = new JFrame();
		frame.setResizable(false);
		frame.setTitle("CeramAlexSync");
		frame.setBounds(100, 100, 800, 553);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		// DefaultListModel<File> model = new DefaultListModel<File>();

		panelPrefs = new JPanel();
		panelPrefs.setBorder(new TitledBorder(
				new LineBorder(new Color(0, 0, 0)), "Database preferences",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51,
						51)));
		panelPrefs.setBounds(12, 89, 566, 281);
		frame.getContentPane().add(panelPrefs);
		panelPrefs.setLayout(null);

		lblMySQLAddress = new JLabel("MySQL Server Address");
		lblMySQLAddress.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMySQLAddress.setBounds(12, 25, 315, 15);
		panelPrefs.add(lblMySQLAddress);

		txtMySQLAddress = new JTextField(50);
		txtMySQLAddress.setBounds(12, 46, 315, 24);
		panelPrefs.add(txtMySQLAddress);
		txtMySQLAddress.setText(MYSQL_URL_PREFIX + "arachne.dainst.org");
		((AbstractDocument) txtMySQLAddress.getDocument())
				.setDocumentFilter(new DocumentFilter() {
					@Override
					public void insertString(FilterBypass fb, int offset,
							String string, AttributeSet attr)
							throws BadLocationException {
						if (offset < MYSQL_URL_PREFIX.length()) {
							return;
						}
						super.insertString(fb, offset, string, attr);
					}

					@Override
					public void replace(FilterBypass fb, int offset,
							int length, String text, AttributeSet attrs)
							throws BadLocationException {
						if (offset < MYSQL_URL_PREFIX.length()) {
							length = Math.max(0,
									length - MYSQL_URL_PREFIX.length());
							offset = MYSQL_URL_PREFIX.length();
						}
						super.replace(fb, offset, length, text, attrs);
					}

					@Override
					public void remove(FilterBypass fb, int offset, int length)
							throws BadLocationException {
						if (offset < MYSQL_URL_PREFIX.length()) {
							length = Math.max(0, length + offset
									- MYSQL_URL_PREFIX.length());
							offset = MYSQL_URL_PREFIX.length();
						}
						if (length > 0) {
							super.remove(fb, offset, length);
						}
					}
				});
		txtMySQLAddress.setColumns(10);

		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		format.setGroupingUsed(false);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(0);
		formatter.setMaximum(65535);
		formatter.setAllowsInvalid(false);
		txtMySQLPort = new JFormattedTextField(formatter);
		txtMySQLPort.setBounds(334, 46, 65, 24);
		panelPrefs.add(txtMySQLPort);
		txtMySQLPort.setValue(3306); // default port mySQL
		txtMySQLPort.setColumns(10);

		lblMySQLPort = new JLabel("Port");
		lblMySQLPort.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMySQLPort.setBounds(334, 25, 65, 15);
		panelPrefs.add(lblMySQLPort);

		lblFMAddress = new JLabel("FileMaker Server Address");
		lblFMAddress.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMAddress.setBounds(12, 164, 315, 15);
		panelPrefs.add(lblFMAddress);

		txtFMAddress = new JTextField(10);
		txtFMAddress.setText("jdbc:mysql://arachne.dainst.org");
		txtFMAddress.setBounds(12, 185, 315, 24);
		txtFMAddress.setText(FM_URL_PREFIX + "localhost");
		((AbstractDocument) txtFMAddress.getDocument())
				.setDocumentFilter(new DocumentFilter() {
					@Override
					public void insertString(FilterBypass fb, int offset,
							String string, AttributeSet attr)
							throws BadLocationException {
						if (offset < FM_URL_PREFIX.length()) {
							return;
						}
						super.insertString(fb, offset, string, attr);
					}

					@Override
					public void replace(FilterBypass fb, int offset,
							int length, String text, AttributeSet attrs)
							throws BadLocationException {
						if (offset < FM_URL_PREFIX.length()) {
							length = Math.max(0,
									length - FM_URL_PREFIX.length());
							offset = FM_URL_PREFIX.length();
						}
						super.replace(fb, offset, length, text, attrs);
					}

					@Override
					public void remove(FilterBypass fb, int offset, int length)
							throws BadLocationException {
						if (offset < FM_URL_PREFIX.length()) {
							length = Math.max(0, length + offset
									- FM_URL_PREFIX.length());
							offset = FM_URL_PREFIX.length();
						}
						if (length > 0) {
							super.remove(fb, offset, length);
						}
					}
				});
		panelPrefs.add(txtFMAddress);

		lblFMPort = new JLabel("Port");
		lblFMPort.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMPort.setBounds(334, 164, 65, 15);
		panelPrefs.add(lblFMPort);

		txtFMPort = new JFormattedTextField(formatter);
		txtFMPort.setColumns(10);
		txtFMPort.setBounds(334, 185, 65, 24);
		txtFMPort.setValue(2399);
		txtFMPort.setEditable(false);
		panelPrefs.add(txtFMPort);

		lblMySQLUser = new JLabel("MySQL Username");
		lblMySQLUser.setBounds(12, 82, 200, 15);
		panelPrefs.add(lblMySQLUser);
		lblMySQLUser.setFont(new Font("Dialog", Font.PLAIN, 12));

		txtMySQLUser = new JTextField();
		txtMySQLUser.setBounds(12, 106, 200, 24);
		panelPrefs.add(txtMySQLUser);
		txtMySQLUser.setText("ceramalex");
		txtMySQLUser.setColumns(10);

		lblMySQLPass = new JLabel("MySQL Password");
		lblMySQLPass.setBounds(220, 82, 165, 15);
		panelPrefs.add(lblMySQLPass);
		lblMySQLPass.setFont(new Font("Dialog", Font.PLAIN, 12));

		txtMySQLPass = new JPasswordField();
		txtMySQLPass.setBounds(220, 106, 165, 24);
		panelPrefs.add(txtMySQLPass);

		lblFMPass = new JLabel("MySQL Password");
		lblFMPass.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMPass.setBounds(220, 221, 165, 15);
		panelPrefs.add(lblFMPass);

		txtFMPass = new JPasswordField();
		txtFMPass.setBounds(220, 245, 165, 24);
		panelPrefs.add(txtFMPass);

		lblFMUser = new JLabel("MySQL Username");
		lblFMUser.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMUser.setBounds(12, 221, 200, 15);
		panelPrefs.add(lblFMUser);

		txtFMUser = new JTextField();
		txtFMUser.setText("admin");
		txtFMUser.setColumns(10);
		txtFMUser.setBounds(12, 245, 200, 24);
		panelPrefs.add(txtFMUser);

		JSeparator separator = new JSeparator();
		separator.setBounds(12, 148, 542, 2);
		panelPrefs.add(separator);

		lblMySQLDB = new JLabel("Database");
		lblMySQLDB.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMySQLDB.setBounds(406, 25, 148, 15);
		panelPrefs.add(lblMySQLDB);

		txtMySQLDB = new JTextField();
		txtMySQLDB.setColumns(10);
		txtMySQLDB.setBounds(406, 46, 148, 24);
		txtMySQLDB.setText("ceramalex");
		panelPrefs.add(txtMySQLDB);

		txtFMDB = new JTextField();
		txtFMDB.setColumns(10);
		txtFMDB.setBounds(406, 185, 148, 24);
		txtFMDB.setText("");
		panelPrefs.add(txtFMDB);

		lblFMDB = new JLabel("Database");
		lblFMDB.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMDB.setBounds(406, 164, 148, 15);
		panelPrefs.add(lblFMDB);

		lblMySQLConnect = new JLabel("Not connected to MySQL.");
		lblMySQLConnect.setBounds(394, 110, 160, 15);
		panelPrefs.add(lblMySQLConnect);
		lblMySQLConnect.setFont(new Font("Dialog", Font.PLAIN, 12));

		lblFMConnect = new JLabel("Not connected to FM.");
		lblFMConnect.setBounds(394, 249, 160, 15);
		panelPrefs.add(lblFMConnect);
		lblFMConnect.setFont(new Font("Dialog", Font.PLAIN, 12));

		panelLog = new JPanel();
		panelLog.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
				"Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelLog.setBounds(12, 382, 566, 138);
		frame.getContentPane().add(panelLog);
		panelLog.setLayout(new BorderLayout(0, 0));

		txtLog = new JTextArea();
		txtLog.setEditable(false);
		txtLog.setLineWrap(true);
		txtLog.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret) txtLog.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		scrollPane = new JScrollPane(txtLog);
		scrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panelLog.add(scrollPane);

		panelActions = new JPanel();
		panelActions.setBorder(new TitledBorder(new LineBorder(new Color(0, 0,
				0)), "Actions", TitledBorder.LEADING, TitledBorder.TOP, null,
				null));
		panelActions.setBounds(590, 89, 196, 431);
		frame.getContentPane().add(panelActions);
		panelActions.setLayout(null);

		btnDownload = new JToggleButton("Download");
		btnDownload.setEnabled(false);
		btnDownload.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnDownload.setBounds(12, 73, 172, 25);
		panelActions.add(btnDownload);

		btnUpload = new JToggleButton("Upload");
		btnUpload.setEnabled(false);
		btnUpload.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnUpload.setBounds(12, 36, 172, 25);
		panelActions.add(btnUpload);

		btnConnect = new JButton("Connect to DBs");
		btnConnect.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				reconfigure();

				/**
				 * Establish new connections to DBs
				 */
				if (!connected) {
					/**
					 * Create watch dog SwingWorker to control timeout
					 */
					SwingWorker<Void, SyncStatus> watcher = new SwingWorker<Void, SyncStatus>() {

						@Override
						protected Void doInBackground() throws Exception {

							
							
							/**
							 * SwingWorkers to connect to DBs and update GUI
							 * elements
							 */
							worker = new ConnectionWorker(txtLog, btnConnect,
									lblMySQLConnect, lblFMConnect,
									txtMySQLPort, txtMySQLAddress,
									txtFMAddress, txtMySQLDB, txtFMDB,
									txtMySQLUser, txtFMUser, txtMySQLPass,
									txtFMPass, btnUpload, btnDownload,
									btnStart, cmbAuthor, cmbPlace) {

								/**
								 * @return true if connection established
								 * @return false if connection failed
								 */
								@Override
								protected Boolean doInBackground()
										throws InterruptedException {

									publish(connecting);

									try {
										this.setSqlControl(SQLAccessController
												.getInstance());
										this.getSqlControl().connect();
										if (this.getSqlControl().isMySQLConnected()
												&& this.getSqlControl().isFMConnected()) {
											connected = true;
											publish(open);
											return true;
										} else{
											this.getSqlControl().close();
											return false;
										}
									} catch (SQLException e) {

										String msg = manageErrorMsg(e);

										publish(closedError.setLogAppend(msg));
										this.cancel(true);
										SwingUtilities
												.invokeLater(new Runnable() {
													@Override
													public void run() {

														JOptionPane
																.showMessageDialog(
																		frame,
																		"Connection error: "
																				+ msg,
																		"Connection failure",
																		JOptionPane.WARNING_MESSAGE);
													};
												});
										return false;
									}
								}

								@Override
								protected void done() {
									try {
										// close connection!
										if(worker.getSqlControl() != null && !worker.getSqlControl().close())
											throw new SQLException();
										worker.cancel(true);
										if (worker.isCancelled()){
											if (worker.get()) {
												publish(closed);
											}else
												publish(closedError);
										}else
											publish(closedError);
										
									} catch (InterruptedException
											| ExecutionException e) {
										publish(closedError
												.setLogAppend(manageErrorMsg(e)));
									} catch (CancellationException e) {
										publish(closedError
												.setLogAppend("Connection timed out."));
									} catch (SQLException e) {
										publish(closedError
												.setLogAppend("Error closing the connection."));
									}
								}
							};
							worker.execute();
							try {
								worker.get(CONN_TIMEOUT, TimeUnit.SECONDS);
							} catch (InterruptedException | ExecutionException e1) {
								worker.cancel(true);
								JOptionPane
								.showMessageDialog(
										frame,
										"Connection aborted by error: "+e1.getMessage(),
										"Abort by error",
										JOptionPane.INFORMATION_MESSAGE);
								
							} catch (TimeoutException e2){
								worker.cancel(true);
								publish(closedError
										.setLogAppend("Connection timed out."));
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										JOptionPane
											.showMessageDialog(
													frame,
													"Timeout while trying to contact the server.",
													"Timeout",
													JOptionPane.INFORMATION_MESSAGE);
									};
								});
								System.out.println("cancelled: "+worker.isCancelled()+"; done: "+worker.isDone());
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
					 */
					worker = new ConnectionWorker(txtLog, btnConnect,
							lblMySQLConnect, lblFMConnect, txtMySQLPort,
							txtMySQLAddress, txtFMAddress, txtMySQLDB, txtFMDB,
							txtMySQLUser, txtFMUser, txtMySQLPass, txtFMPass,
							btnUpload, btnDownload, btnStart, cmbAuthor,
							cmbPlace) {

						/**
						 * @return true if connection closed
						 * @return false if closing connection failed
						 */
						@Override
						protected Boolean doInBackground() {

							SQLAccessController connector = null;
							try {
								connector = SQLAccessController.getInstance();
							} catch (SQLException e) {

								e.printStackTrace();
							}

							if (connector.close()) {
								connected = false;
								publish(closed
										.setLogAppend("Connection closed."));
								return true;
							} else {
								publish(closedError
										.setLogAppend("Error: Connection could not be closed."));
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										JOptionPane
												.showMessageDialog(
														frame,
														"Connection could not be closed!",
														"Error closing connection",
														JOptionPane.ERROR_MESSAGE);
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
		btnConnect.setBounds(12, 357, 172, 25);
		panelActions.add(btnConnect);

		btnStart = new JButton("START");
		btnStart.setEnabled(false);
		btnStart.setBounds(12, 394, 172, 25);
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				/**
				 * Create watch dog SwingWorker to manage progress
				 */
				SwingWorker<Void, Void> watcher = new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {

						// ArrayList<File> files = new ArrayList<File>();
						//
						// if (listFiles.getModel().getSize() == 0){
						// SwingUtilities.invokeLater(new Runnable(){
						// @Override
						// public void run() {
						// JOptionPane.showMessageDialog(frame,
						// "No FileMaker files selected!", "Missing input",
						// JOptionPane.WARNING_MESSAGE);
						// return;
						// };
						// });
						// }
						//
						// for (int i = 0; i < listFiles.getModel().getSize(); i
						// ++){
						// files.add(listFiles.getModel().getElementAt(i));
						// }

						if (btnDownload.isSelected()) {

							// SwingUtilities.invokeLater(new Runnable(){
							// @Override
							// public void run() {
							// progressBar.setVisible(true);
							// progressBar.setStringPainted(true);
							// progressBar.setValue(0);
							// };
							// });

							/**
							 * Create SwingWorker to manage download
							 */
							ProgressWorker worker = new ProgressWorker() {

								@Override
								protected Boolean doInBackground()
										throws Exception {

									return CallManager.getInstance()
											.startDownload();
								}

							};
							worker.execute();
						}
						if (btnUpload.isSelected()) {

							/**
							 * Create SwingWorker to manage upload
							 */
							SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

								@Override
								protected Boolean doInBackground()
										throws Exception {

									return CallManager.getInstance()
											.startUpload();
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

		cmbPlace = new JComboBox<String>();
		cmbPlace.setBounds(12, 208, 172, 24);
		panelActions.add(cmbPlace);
		cmbPlace.setEnabled(false);

		lblPlace = new JLabel("Particular Place");
		lblPlace.setBounds(12, 187, 157, 15);
		panelActions.add(lblPlace);
		lblPlace.setFont(new Font("Dialog", Font.PLAIN, 12));

		lblAuthor = new JLabel("Particular Author");
		lblAuthor.setBounds(12, 244, 157, 15);
		panelActions.add(lblAuthor);
		lblAuthor.setFont(new Font("Dialog", Font.PLAIN, 12));

		cmbAuthor = new JComboBox<String>();
		cmbAuthor.setBounds(12, 263, 172, 24);
		panelActions.add(cmbAuthor);
		cmbAuthor.setEnabled(false);

		JButton btnChooseTables = new JButton("Choose tables");
		btnChooseTables.setEnabled(false);
		btnChooseTables.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnChooseTables.setBounds(12, 150, 172, 25);
		panelActions.add(btnChooseTables);

		URL url = getClass().getResource("/ceramalex/sync/resources/logo.png");
		JLabel lblPicture = new JLabel(new ImageIcon(url));
		lblPicture.setBounds(12, 12, 774, 70);
		frame.getContentPane().add(lblPicture);

	}

	protected void reconfigure() {
		config.setMySQLURL(txtMySQLAddress.getText() + ":"
				+ txtMySQLPort.getText());
		config.setMySQLUser(txtMySQLUser.getText());
		config.setMySQLDB(txtMySQLDB.getText());
		config.setMySQLPassword(new String(txtMySQLPass.getPassword()));// "pvnnyEMpQHSfBXvW";

		config.setFmURL(txtFMAddress.getText());
		config.setFmUser(txtFMUser.getText());
		config.setFmDB(txtFMDB.getText());
		config.setFmPassword(new String(txtFMPass.getPassword()));
	}

	protected String manageErrorMsg(Exception e) {
		String msg = e.getMessage();
		String prefix = "";
		if (e.getMessage().startsWith("MySQL:"))
			prefix = "MySQL: ";
		
		if (e.getMessage().contains("Communications link failure"))
			msg = "Server does not answer.";
		if (e.getMessage().contains("Access denied"))
			msg = "Access denied: Wrong user name or password.";
		if (e.getMessage().contains("No suitable driver"))
			msg = e.getMessage();
		
		return prefix + msg;
	}
}
