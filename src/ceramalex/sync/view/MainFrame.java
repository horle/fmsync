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
import java.util.ArrayList;
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
import ceramalex.sync.model.ComparisonResult;
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
	private JButton btnCancel;

	private boolean connected;
	private boolean inProgress;
	private JScrollPane scrollPane;
	// private JProgressBar progressBar;
	
	private ComparisonResult currComp;

	private final int CONN_TIMEOUT = 5;
	private static Logger logger = Logger.getLogger(MainFrame.class);
	private static ConfigController config;
	public static SQLDataModel data;

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

		DOMConfigurator.configureAndWatch("log4j.xml");
		try {
			config = ConfigController.getInstance();
			data = new SQLDataModel();
		} catch (IOException e1) {
			JOptionPane
					.showMessageDialog(
							null,
							"I was not able to create a new config file. Missing permissions?",
							"Error", JOptionPane.WARNING_MESSAGE);
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
		closed = new FrameStatus("", true, false);

		connecting = new FrameStatus("Trying to connect to "
				+ config.getShortMySQLURL() + ":" + config.getMySQLPort() + " as "
				+ config.getMySQLUser() + " ...\nTrying to connect to "
				+ config.getShortFMURL() + ":2399" + " as " + config.getFmUser()
				+ " ...", false, false);

		open = new FrameStatus(
				"Successfully connected to MySQL.\nSuccessfully connected to FM.",
				false, true);

		closedError = new FrameStatus("Connection failed. ", true, false);
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
		frame.setLocationRelativeTo(null);
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
				ConfigDialog confDia = new ConfigDialog();
				confDia.setLocationRelativeTo(frame);
				confDia.setVisible(true);
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
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AboutDialog about = new AboutDialog();
				about.setLocationRelativeTo(frame);
				about.setVisible(true);
			}
		});
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

				/**
				 * Check connections
				 */
				if (!connected) {
					invokeConnectWorker();
				}
			}
		});
		btnConnect.setBounds(12, 24, 141, 25);
		panelActions.add(btnConnect);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnCancel.setEnabled(false);
		btnCancel.setBounds(12, 61, 141, 25);
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				invokeDisconnectWorker();
			}
		});
		panelActions.add(btnCancel);

		URL url = getClass().getResource("/ceramalex/sync/resources/logo.png");
		JLabel lblPicture = new JLabel(new ImageIcon(url));
		lblPicture.setBounds(10, 38, 774, 70);
		frame.getContentPane().add(lblPicture);

	}

	private void invokeConnectWorker() {
		/**
		 * Create watch dog SwingWorker to control timeout
		 */
		SwingWorker<Void, FrameStatus> watcher = new SwingWorker<Void, FrameStatus>() {

			@Override
			protected Void doInBackground() throws Exception {

				/**
				 * SwingWorker to connect to DBs and update GUI
				 * elements
				 */
				worker = new ConnectionWorker(btnConnect,
						btnCancel, txtLog) {

					/**
					 * @return true if connection established; false if connection failed
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
								logger.info(open.getLogMsg());
								return true;
							} else {
								connected = false;
								publish(closedError);
								logger.error(closedError.getLogMsg());
								this.getSQLControl().close();
								return false;
							}
						} catch (SQLException e) {

							String msg = manageErrorMsg(e);

							publish(closedError.setLogAppend(msg));
							logger.info(closedError.getLogMsg());
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
							logger.info(closedError.getLogMsg());
							this.cancel(true);

							SwingUtilities
									.invokeLater(new Runnable() {
										@Override
										public void run() {

											JOptionPane
													.showMessageDialog(
															null,
															"I was not able to create a new config file. Missing permissions?",
															"Error",
															JOptionPane.WARNING_MESSAGE);
										};
									});
							return false;
						}
					}

					@Override
					protected void done() {
//						try {
//							// close connection!
//							if (worker.getSQLControl() != null
//									&& !worker.getSQLControl()
//											.close())
//								throw new SQLException();
//							worker.cancel(true);
//							if (worker.isCancelled()) {
//								try {
//									if (worker.get()) {
//										publish(closed);
//										logger.info(closed.getLogMsg());
//									} else{
//										publish(closedError);
//										logger.info(closedError.getLogMsg());
//									}
//								} catch (CancellationException e) {
//									publish(closedError);
//								}
//							} else {
//								publish(closedError);
//								logger.info(closedError.getLogMsg());
//							}
//
//						} catch (InterruptedException
//								| ExecutionException e) {
//							publish(closedError
//									.setLogAppend(manageErrorMsg(e)));
//						} catch (SQLException e) {
//							publish(closedError
//									.setLogAppend("Error closing the connection."));
//						}
					}
				};
				worker.execute();
				try {
					worker.get(CONN_TIMEOUT, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException e1) {
					worker.cancel(true);
					publish(closedError
							.setLogAppend("Connection aborted by error."));
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane.showMessageDialog(
									frame,
									"Connection aborted by error: "
											+ e1.getMessage(),
									"Aborted by error",
									JOptionPane.INFORMATION_MESSAGE);
						};
					});
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
				}
				return null;
			}
			@Override
			protected void done() {
				invokeComparisonDialog();
			}
		};
		watcher.execute();
	}

	private void invokeDisconnectWorker() {
		/**
		 * SwingWorker to disconnect and update GUI elements
		 */
		worker = new ConnectionWorker(btnConnect,btnCancel,txtLog) {

			/**
			 * @return true if connection closed
			 * @return false if closing connection failed
			 */
			@Override
			protected Boolean doInBackground() {

				SQLAccessController connector = null;
				try {
					connector = SQLAccessController.getInstance();
				} catch (IOException e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane
									.showMessageDialog(
											null,
											"I was not able to create a new config file. Missing permissions?",
											"Error",
											JOptionPane.WARNING_MESSAGE);
						}
					});
				}

				if (connector.close()) {
					connected = false;
					publish(closed
							.setLogMsg("Connection closed."));
					logger.info(closed.getLogMsg());
					return true;
				} else {
					publish(closedError
							.setLogAppend("Error: Connection could not be closed."));
					logger.error(closed.getLogMsg());
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

	private void invokeComparisonDialog() {
		//TODO doppelte progressbar 
		TableProgressDialog progress = new TableProgressDialog(txtLog);
		progress.setLocationRelativeTo(frame);
		progress.showAndStart();

		
		ComparisonDialog comp = new ComparisonDialog();
		comp.setLocationRelativeTo(frame);
		currComp = comp.showDialog();
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
