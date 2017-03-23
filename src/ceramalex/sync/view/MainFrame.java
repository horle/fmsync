package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import javax.swing.text.NumberFormatter;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import ceramalex.sync.controller.ConfigController;
import ceramalex.sync.controller.SQLAccessController;
import ceramalex.sync.model.SQLDataModel;

/**
 * CeramalexSync main window to control DB sync actions
 * 
 * @author horle (Felix Kussmaul)
 */

public class MainFrame {

	private JFrame frame;
	private JPanel panelLog;
	private JPanel panelActions;
	private JTextArea txtLog;
	private JButton btnConnect;
	private JButton btnStart;

	private boolean connected;
	private boolean inProgress;
	private JScrollPane scrollPane;
	// private JProgressBar progressBar;

	private final int CONN_TIMEOUT = 5;
	protected static Logger logger = Logger
			.getLogger("ceramalex.sync.view.mainwindow");
	private static ConfigController config;
	private static SQLDataModel data;

	private ConnectionWorker worker;

	private FrameStatus closed;
	private FrameStatus connecting;
	private FrameStatus open;
	private FrameStatus closedError;
	private JMenu mnFile;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		DOMConfigurator.configureAndWatch("ressource/log4j.xml");
		try {
			config = ConfigController.getInstance();
			data = new SQLDataModel();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "I was not able to create a new config file. Missing permissions?","Error",JOptionPane.WARNING_MESSAGE);
		}
		

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame window = new MainFrame();
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
	public MainFrame() {
		initialize();
		initFrameStatus();
	}

	private void initFrameStatus() {
		closed = new FrameStatus("", "Not connected to MySQL.",
				"Not connected to FM.", "Connect to DBs", true, true, false);
		connecting = new FrameStatus("Trying to connect to "
				+ config.getMySQLURL() + ":" + config.getMySQLPort() + " as "
				+ config.getMySQLUser() + " ...\nTrying to connect to "
				+ config.getFmURL() + ":2399" + " as " + config.getFmUser()
				+ " ...", "Not connected to MySQL.", "Not connected to FM.",
				"Connecting ...", false, false, false);
		open = new FrameStatus(
				"Successfully connected to MySQL.\nSuccessfully connected to FM.",
				"Disconnect", "Connected to MySQL.", "Connected to FM.", false,
				false, true);
		closedError = new FrameStatus("Connection failed. ",
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
		frame.setTitle("CeramalexSync");
		frame.setBounds(100, 100, 800, 346);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		format.setGroupingUsed(false);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(0);
		formatter.setMaximum(65535);
		formatter.setAllowsInvalid(false);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, 794, 23);
		frame.getContentPane().add(menuBar);

		mnFile = new JMenu("File");
		mnFile.setFont(new Font("Dialog", Font.PLAIN, 12));
		menuBar.add(mnFile);

		JMenuItem mntmPreferences = new JMenuItem("Configuration");
		mntmPreferences.setFont(new Font("Dialog", Font.PLAIN, 12));
		mnFile.add(mntmPreferences);
		mntmPreferences.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new ConfigDialog().setVisible(true);
			}
		});

		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.setFont(new Font("Dialog", Font.PLAIN, 12));
		mntmClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!inProgress) {
					System.exit(0);
				} else {
					if (JOptionPane
							.showConfirmDialog(
									null,
									"The sync is still in progress. Do you really want to quit and RISK DATA LOSS?",
									"Really interrupt?",
									JOptionPane.OK_CANCEL_OPTION,
									JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
						// SAFELY INTERRUPT TODO
						System.exit(0);
					}
				}
			}
		});
		mnFile.add(mntmClose);

		JMenu mnHelp = new JMenu("?");
		mnHelp.setFont(new Font("Dialog", Font.PLAIN, 12));
		menuBar.add(mnHelp);

		JMenuItem mntmOpenHelpDocument = new JMenuItem("Open Help Document");
		mntmOpenHelpDocument.setFont(new Font("Dialog", Font.PLAIN, 12));
		mnHelp.add(mntmOpenHelpDocument);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Dialog", Font.PLAIN, 12));
		mnHelp.add(mntmAbout);

		panelLog = new JPanel();
		panelLog.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
				"Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelLog.setBounds(10, 115, 595, 190);
		frame.getContentPane().add(panelLog);
		panelLog.setLayout(new BorderLayout(0, 0));

		txtLog = new JTextArea();
		txtLog.setFont(new Font("monospaced", Font.PLAIN, 11));
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
		panelActions.setBounds(617, 115, 165, 190);
		frame.getContentPane().add(panelActions);
		panelActions.setLayout(null);

		btnConnect = new JButton("Connect");
		btnConnect.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				ComparisonDialog comp = new ComparisonDialog();
				comp.setVisible(true);

				/**
				 * Establish new connections to DBs
				 */
				if (!connected) {
					/**
					 * Create watch dog SwingWorker to control timeout
					 */
					SwingWorker<Void, FrameStatus> watcher = new SwingWorker<Void, FrameStatus>() {

						@Override
						protected Void doInBackground() throws Exception {

							/**
							 * SwingWorkers to connect to DBs and update GUI
							 * elements
							 */
							worker = new ConnectionWorker(btnStart, btnConnect,
									txtLog) {

								/**
								 * @return true if connection established
								 * @return false if connection failed
								 */
								@Override
								protected Boolean doInBackground()
										throws InterruptedException {

									publish(connecting);

									try {
										this.setSQLControl(SQLAccessController
												.getInstance());
										if (this.getSQLControl().connect()) {
											connected = true;
											publish(open);
											return true;
										} else {
											this.getSQLControl().close();
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
									} catch (IOException e) {

										String msg = manageErrorMsg(e);

										publish(closedError.setLogAppend(msg));
										this.cancel(true);
										
										SwingUtilities
										.invokeLater(new Runnable() {
											@Override
											public void run() {

												JOptionPane.showMessageDialog(null, "I was not able to create a new config file. Missing permissions?","Error",JOptionPane.WARNING_MESSAGE);
											};
										});
										return false;
									}
								}

								@Override
								protected void done() {
									try {
										// close connection!
										if (worker.getSQLControl() != null
												&& !worker.getSQLControl()
														.close())
											throw new SQLException();
										worker.cancel(true);
										if (worker.isCancelled()) {
											if (worker.get()) {
												publish(closed);
											} else
												publish(closedError);
										} else
											publish(closedError);

									} catch (InterruptedException
											| ExecutionException e) {
										publish(closedError
												.setLogAppend(manageErrorMsg(e)));
									} catch (CancellationException e) {
										publish(closedError
												.setLogAppend("Connection aborted."));
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
								JOptionPane.showMessageDialog(
										frame,
										"Connection aborted by error: "
												+ e1.getMessage(),
										"Abort by error",
										JOptionPane.INFORMATION_MESSAGE);

							} catch (TimeoutException e2) {
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
								System.out.println("cancelled: "
										+ worker.isCancelled() + "; done: "
										+ worker.isDone());
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
					worker = new ConnectionWorker(btnStart, btnConnect, txtLog) {

						/**
						 * @return true if connection closed
						 * @return false if closing connection failed
						 */
						@Override
						protected Boolean doInBackground() {

							SQLAccessController connector = null;
							try {
								connector = SQLAccessController
										.getInstance();
							} catch (IOException e) {
								JOptionPane.showMessageDialog(null, "I was not able to create a new config file. Missing permissions?","Error",JOptionPane.WARNING_MESSAGE);
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
		btnConnect.setBounds(12, 24, 141, 25);
		panelActions.add(btnConnect);

		btnStart = new JButton("START SYNC");
		btnStart.setEnabled(false);
		btnStart.setBounds(12, 61, 141, 25);
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				/**
				 * Create watch dog SwingWorker to manage progress
				 */
				SwingWorker<Void, Void> watcher = new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {

						if (true) {

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

									return true;
								}

							};
							worker.execute();
						}
						if (true) {

							/**
							 * Create SwingWorker to manage upload
							 */
							SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

								@Override
								protected Boolean doInBackground()
										throws Exception {

									return true;
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

		URL url = getClass().getResource("/ceramalex/sync/resources/logo.png");
		JLabel lblPicture = new JLabel(new ImageIcon(url));
		lblPicture.setBounds(10, 38, 774, 70);
		frame.getContentPane().add(lblPicture);

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
