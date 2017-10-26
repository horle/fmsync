package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import ceramalex.sync.controller.ConfigController;

public class ConfigDialog extends JDialog {
	private JTextField txtMySQLAddress;
	private JTextField txtFMAddress;
	private JTextField txtMySQLUser;
	private JPasswordField txtMySQLPass;
	private JPasswordField txtFMPass;
	private JTextField txtFMUser;
	private JTextField txtMySQLDB;
	private JTextField txtFMDB;
	private JFormattedTextField txtMySQLPort;
	
	private final JFileChooser fc;

	private ConfigController conf;
	private JDialog self = this;
	private JTextField txtImgPath;

	/**
	 * @author horle
	 * A dialog to edit and save configuration details
	 */
	public ConfigDialog() {
		try {
			conf = ConfigController.getInstance();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "I was not able to create a new config file. Missing permissions?","Error",JOptionPane.WARNING_MESSAGE);
		}
		
		setModal(true);
		setTitle("Configuration");
		setBounds(100, 100, 610, 470);
		getContentPane().setLayout(new BorderLayout());
		
		fc = new JFileChooser();

		JPanel container = new JPanel();
		container.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(container, BorderLayout.CENTER);
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
		
		//%%%%%%% DB Preferences %%%%%%%
		
		JPanel lblDB = new JPanel();
		lblDB.setPreferredSize(new Dimension(0, 220));
		container.add(lblDB);
		
		lblDB.setLayout(null);
		lblDB.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
				"Database preferences", TitledBorder.LEADING, TitledBorder.TOP,
				null, new Color(51, 51, 51)));

		JLabel lblMAdd;
		lblMAdd = new JLabel("MySQL Server Address");
		lblMAdd.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMAdd.setBounds(12, 25, 315, 15);
		lblDB.add(lblMAdd);

		txtMySQLAddress = new JTextField(ConfigController.MYSQL_URL_PREFIX+conf.getShortMySQLURL());
		txtMySQLAddress.setBounds(12, 46, 315, 24);
		((AbstractDocument) txtMySQLAddress.getDocument())
				.setDocumentFilter(new DocumentFilter() {
					@Override
					public void insertString(FilterBypass fb, int offset,
							String string, AttributeSet attr)
							throws BadLocationException {
						if (offset < ConfigController.MYSQL_URL_PREFIX.length()) {
							return;
						}
						super.insertString(fb, offset, string, attr);
					}

					@Override
					public void replace(FilterBypass fb, int offset,
							int length, String text, AttributeSet attrs)
							throws BadLocationException {
						if (offset < ConfigController.MYSQL_URL_PREFIX.length()) {
							length = Math.max(0,
									length - ConfigController.MYSQL_URL_PREFIX.length());
							offset = ConfigController.MYSQL_URL_PREFIX.length();
						}
						super.replace(fb, offset, length, text, attrs);
					}

					@Override
					public void remove(FilterBypass fb, int offset, int length)
							throws BadLocationException {
						if (offset < ConfigController.MYSQL_URL_PREFIX.length()) {
							length = Math.max(0, length + offset
									- ConfigController.MYSQL_URL_PREFIX.length());
							offset = ConfigController.MYSQL_URL_PREFIX.length();
						}
						if (length > 0) {
							super.remove(fb, offset, length);
						}
					}
				});
		lblDB.add(txtMySQLAddress);

		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		format.setGroupingUsed(false);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(0);
		formatter.setMaximum(65535);
		formatter.setAllowsInvalid(false);
		txtMySQLPort = new JFormattedTextField(formatter);
		txtMySQLPort.setValue(3306);
		txtMySQLPort.setBounds(334, 46, 65, 24);
		lblDB.add(txtMySQLPort);

		JLabel lblMPort = new JLabel("Port");
		lblMPort.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMPort.setBounds(334, 25, 65, 15);
		lblDB.add(lblMPort);

		JLabel lblFMAdd = new JLabel("FileMaker 14 Server Address");
		lblFMAdd.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMAdd.setBounds(12, 164, 315, 15);
		lblDB.add(lblFMAdd);

		txtFMAddress = new JTextField(ConfigController.FM_URL_PREFIX + conf.getShortFMURL());
		txtFMAddress.setEditable(false);
		((AbstractDocument) txtFMAddress.getDocument())
				.setDocumentFilter(new DocumentFilter() {
					@Override
					public void insertString(FilterBypass fb, int offset,
							String string, AttributeSet attr)
							throws BadLocationException {
						if (offset < ConfigController.FM_URL_PREFIX.length()) {
							return;
						}
						super.insertString(fb, offset, string, attr);
					}

					@Override
					public void replace(FilterBypass fb, int offset,
							int length, String text, AttributeSet attrs)
							throws BadLocationException {
						if (offset < ConfigController.FM_URL_PREFIX.length()) {
							length = Math.max(0,
									length - ConfigController.FM_URL_PREFIX.length());
							offset = ConfigController.FM_URL_PREFIX.length();
						}
						super.replace(fb, offset, length, text, attrs);
					}

					@Override
					public void remove(FilterBypass fb, int offset, int length)
							throws BadLocationException {
						if (offset < ConfigController.FM_URL_PREFIX.length()) {
							length = Math.max(0, length + offset
									- ConfigController.FM_URL_PREFIX.length());
							offset = ConfigController.FM_URL_PREFIX.length();
						}
						if (length > 0) {
							super.remove(fb, offset, length);
						}
					}
				});
		txtFMAddress.setBounds(12, 185, 315, 24);
		lblDB.add(txtFMAddress);
		JLabel lblFMPort = new JLabel("Port");
		lblFMPort.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMPort.setBounds(334, 164, 65, 15);
		lblDB.add(lblFMPort);
		JTextField txtFMPort = new JTextField("2399");
		txtFMPort.setEditable(false);
		txtFMPort.setBounds(334, 185, 65, 24);
		lblDB.add(txtFMPort);

		JLabel lblMUser = new JLabel("MySQL Username");
		lblMUser.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMUser.setBounds(12, 82, 200, 15);
		lblDB.add(lblMUser);

		txtMySQLUser = new JTextField(conf.getMySQLUser());
		txtMySQLUser.setBounds(12, 106, 200, 24);
		lblDB.add(txtMySQLUser);

		JLabel lblMPass = new JLabel("MySQL Password");
		lblMPass.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMPass.setBounds(220, 82, 165, 15);
		lblDB.add(lblMPass);

		txtMySQLPass = new JPasswordField(conf.getMySQLPassword());
		txtMySQLPass.setBounds(220, 106, 165, 24);
		lblDB.add(txtMySQLPass);

		JLabel lblFMPass = new JLabel("FileMaker 14 Password");
		lblFMPass.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMPass.setBounds(220, 221, 165, 15);
		lblDB.add(lblFMPass);

		txtFMPass = new JPasswordField(conf.getFmPassword());
		txtFMPass.setBounds(220, 245, 165, 24);
		lblDB.add(txtFMPass);

		JLabel lblFMUser = new JLabel("FileMaker 14 Username");
		lblFMUser.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMUser.setBounds(12, 221, 200, 15);
		lblDB.add(lblFMUser);

		txtFMUser = new JTextField(conf.getFmUser());
		txtFMUser.setBounds(12, 245, 200, 24);
		lblDB.add(txtFMUser);

		JSeparator separator = new JSeparator();
		separator.setBounds(12, 148, 542, 2);
		lblDB.add(separator);

		JLabel lblMDB = new JLabel("Database");
		lblMDB.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMDB.setBounds(406, 25, 148, 15);
		lblDB.add(lblMDB);

		txtMySQLDB = new JTextField(conf.getMySQLDB());
		txtMySQLDB.setBounds(406, 46, 148, 24);
		lblDB.add(txtMySQLDB);

		txtFMDB = new JTextField(conf.getFmDB());
		txtFMDB.setBounds(406, 185, 148, 24);
		lblDB.add(txtFMDB);

		JLabel lblFMDB;
		lblFMDB = new JLabel("Database");
		lblFMDB.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMDB.setBounds(406, 164, 148, 15);
		lblDB.add(lblFMDB);
		
		container.add(Box.createVerticalStrut(5));

		// %%%%%%%% IMAGES %%%%%%%%%
		
		JPanel lblImage = new JPanel();
		lblImage.setPreferredSize(new Dimension(0, 20));
		lblImage.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Image path", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		container.add(lblImage);
		lblImage.setLayout(null);
		
		txtImgPath = new JTextField(conf.getImagePath());
		txtImgPath.setBounds(12, 31, 371, 24);
		lblImage.add(txtImgPath);
		
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		JButton btnFileChooser = new JButton("Choose folder ...");
		btnFileChooser.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnFileChooser.setBounds(395, 31, 158, 24);
		lblImage.add(btnFileChooser);
		
		btnFileChooser.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fc.setCurrentDirectory(new File(conf.getImagePath()==null?"":conf.getImagePath()));
				if (fc.showDialog(self, "Select") == JFileChooser.APPROVE_OPTION) {
					txtImgPath.setText(fc.getSelectedFile().getAbsolutePath());
					conf.setImagePath(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});
		
		//%%%%% BUTTONS %%%%%%%%
		
		JPanel buttonPane = new JPanel();
		buttonPane.setBorder(new EmptyBorder(0, 5, 5, 5));
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (conf.setPrefs(txtMySQLAddress.getText().replace(ConfigController.MYSQL_URL_PREFIX, ""),
						txtMySQLPort.getText(), txtMySQLUser.getText(),
						new String(txtMySQLPass.getPassword()),
						txtMySQLDB.getText(), txtFMAddress.getText().replace(ConfigController.FM_URL_PREFIX, ""),
						txtFMUser.getText(),
						new String(txtFMPass.getPassword()),
						txtFMDB.getText(),
						txtImgPath.getText()))
					self.dispose();
				else {
					JOptionPane.showMessageDialog(null, "Could not save configuration file! Missing file permissions?", "Unable to write", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		btnSave.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnSave.setActionCommand("OK");
		buttonPane.add(btnSave);
		getRootPane().setDefaultButton(btnSave);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				self.dispose();
			}
		});
		btnCancel.setFont(new Font("Dialog", Font.PLAIN, 12));
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);
	}
}
