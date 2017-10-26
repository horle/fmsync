package ceramalex.sync.view;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

class JResizeTable extends JTable {

	public JResizeTable() {
		super();
	}
	
	public JResizeTable(DefaultTableModel dm) {
		super(dm);
	}

	@Override
	public Point getToolTipLocation(MouseEvent event) {
		return new Point(10, 10);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return getPreferredSize().width < getParent().getWidth();
	}

	@Override
	public void doLayout() {
		TableColumn resizingColumn = null;

		if (tableHeader != null)
			resizingColumn = tableHeader.getResizingColumn();

		// Viewport size changed. May need to increase columns widths

		if (resizingColumn == null) {
			setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			super.doLayout();
		}

		// Specific column resized. Reset preferred widths

		else {
			TableColumnModel tcm = getColumnModel();

			for (int i = 0; i < tcm.getColumnCount(); i++) {
				TableColumn tc = tcm.getColumn(i);
				tc.setPreferredWidth(tc.getWidth());
			}

			// Columns don't fill the viewport, invoke default layout

			if (tcm.getTotalColumnWidth() < getParent().getWidth())
				setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			super.doLayout();
		}

		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}
}