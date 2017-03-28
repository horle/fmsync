package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
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

	private ConfigController conf;
	private JDialog self = this;

	/**
	 * Create the dialog.
	 */
	public ConfigDialog() {
		try {
			conf = ConfigController.getInstance();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "I was not able to create a new config file. Missing permissions?","Error",JOptionPane.WARNING_MESSAGE);
		}
		
		setModal(true);
		setTitle("Configuration");
		setBounds(100, 100, 581, 363);
		getContentPane().setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		panel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
				"Database preferences", TitledBorder.LEADING, TitledBorder.TOP,
				null, new Color(51, 51, 51)));

		JLabel lblMAdd;
		lblMAdd = new JLabel("MySQL Server Address");
		lblMAdd.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMAdd.setBounds(12, 25, 315, 15);
		panel.add(lblMAdd);

		txtMySQLAddress = new JTextField(10);
		txtMySQLAddress.setText(ConfigController.MYSQL_URL_PREFIX+conf.getShortMySQLURL());
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
		panel.add(txtMySQLAddress);

		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		format.setGroupingUsed(false);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(0);
		formatter.setMaximum(65535);
		formatter.setAllowsInvalid(false);
		txtMySQLPort = new JFormattedTextField(formatter);
		txtMySQLPort.setColumns(10);
		txtMySQLPort.setValue(3306);
		txtMySQLPort.setBounds(334, 46, 65, 24);
		panel.add(txtMySQLPort);

		JLabel lblMPort = new JLabel("Port");
		lblMPort.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMPort.setBounds(334, 25, 65, 15);
		panel.add(lblMPort);

		JLabel lblFMAdd = new JLabel("FileMaker 14 Server Address");
		lblFMAdd.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMAdd.setBounds(12, 164, 315, 15);
		panel.add(lblFMAdd);

		txtFMAddress = new JTextField(10);
		txtFMAddress.setEditable(false);
		txtFMAddress.setText(ConfigController.FM_URL_PREFIX + conf.getShortFMURL());
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
		panel.add(txtFMAddress);
		JLabel lblFMPort = new JLabel("Port");
		lblFMPort.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMPort.setBounds(334, 164, 65, 15);
		panel.add(lblFMPort);
		JTextField txtFMPort = new JTextField();
		txtFMPort.setText("2399");
		txtFMPort.setEditable(false);
		txtFMPort.setColumns(10);
		txtFMPort.setBounds(334, 185, 65, 24);
		panel.add(txtFMPort);

		JLabel lblMUser = new JLabel("MySQL Username");
		lblMUser.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMUser.setBounds(12, 82, 200, 15);
		panel.add(lblMUser);

		txtMySQLUser = new JTextField();
		txtMySQLUser.setText(conf.getMySQLUser());
		txtMySQLUser.setColumns(10);
		txtMySQLUser.setBounds(12, 106, 200, 24);
		panel.add(txtMySQLUser);

		JLabel lblMPass = new JLabel("MySQL Password");
		lblMPass.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMPass.setBounds(220, 82, 165, 15);
		panel.add(lblMPass);

		txtMySQLPass = new JPasswordField();
		txtMySQLPass.setText(conf.getMySQLPassword());
		txtMySQLPass.setBounds(220, 106, 165, 24);
		panel.add(txtMySQLPass);

		JLabel lblFMPass = new JLabel("FileMaker 14 Password");
		lblFMPass.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMPass.setBounds(220, 221, 165, 15);
		panel.add(lblFMPass);

		txtFMPass = new JPasswordField();
		txtFMPass.setText(conf.getFmPassword());
		txtFMPass.setBounds(220, 245, 165, 24);
		panel.add(txtFMPass);

		JLabel lblFMUser = new JLabel("FileMaker 14 Username");
		lblFMUser.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMUser.setBounds(12, 221, 200, 15);
		panel.add(lblFMUser);

		txtFMUser = new JTextField();
		txtFMUser.setText(conf.getFmUser());
		txtFMUser.setColumns(10);
		txtFMUser.setBounds(12, 245, 200, 24);
		panel.add(txtFMUser);

		JSeparator separator = new JSeparator();
		separator.setBounds(12, 148, 542, 2);
		panel.add(separator);

		JLabel lblMDB = new JLabel("Database");
		lblMDB.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblMDB.setBounds(406, 25, 148, 15);
		panel.add(lblMDB);

		txtMySQLDB = new JTextField();
		txtMySQLDB.setText(conf.getMySQLDB());
		txtMySQLDB.setColumns(10);
		txtMySQLDB.setBounds(406, 46, 148, 24);
		panel.add(txtMySQLDB);

		txtFMDB = new JTextField();
		txtFMDB.setText(conf.getFmDB());
		txtFMDB.setColumns(10);
		txtFMDB.setBounds(406, 185, 148, 24);
		panel.add(txtFMDB);

		JLabel lblFMDB;
		lblFMDB = new JLabel("Database");
		lblFMDB.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblFMDB.setBounds(406, 164, 148, 15);
		panel.add(lblFMDB);

		JPanel buttonPane = new JPanel();
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
						txtFMDB.getText()))
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
