package ceramalex.sync.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ScrollPaneConstants;

public class AboutDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public AboutDialog() {
		setTitle("About CeramalexSync");
		setBounds(100, 100, 530, 300);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JTextPane txtAbout = new JTextPane();
		txtAbout.setAutoscrolls(false);
		txtAbout.setBackground(UIManager.getColor("Label.background"));
		txtAbout.setFocusable(false);
		txtAbout.setEditable(false);
		txtAbout.setContentType("text/html");
		txtAbout.setText("<html><body>\r\n<h3>About CeramalexSync</h3>\r\n<p>\r\nThis program has been developed within the scope of the Ceramalex/CeramEgypt project at the University of Cologne in 2017.\r\n</p>\r\n<p>Copyright &copy; 2017  horle (Felix Ku\u00DFmaul)</p>\r\n<p>This program is free software: you can redistribute it and/or modify\r\n    it under the terms of the GNU General Public License as published by\r\n    the Free Software Foundation, either version 3 of the License, or\r\n    (at your option) any later version.</p>\r\n\r\n    <p>This program is distributed in the hope that it will be useful,\r\n    but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n    GNU General Public License for more details.</p>\r\n\r\n    <p>You should have received a copy of the GNU General Public License\r\n    along with this program.  If not, see <a href=\"http://www.gnu.org/licenses/\">http://www.gnu.org/licenses/</a>.</p>\r\n</body></html>");
		txtAbout.setFont(new Font("Dialog", Font.PLAIN, 12));
		txtAbout.setCaretPosition(0);
		
		JScrollPane scrollPane = new JScrollPane(txtAbout);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(0, 0, 514, 225);
		contentPanel.add(scrollPane);

		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton btnClose = new JButton("Close");
				btnClose.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				btnClose.setFont(new Font("Dialog", Font.PLAIN, 12));
				btnClose.setActionCommand("OK");
				buttonPane.add(btnClose);
			}
		}
	}
}
