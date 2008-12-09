/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.controller.print;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.freeplane.controller.Controller;
import org.freeplane.map.tree.view.MapView;
import org.freeplane.ui.FreemindMenuBar;

class PrintPreviewAction extends AbstractPrintAction {
	PrintPreviewAction(final PrintController controller) {
		super(controller);
		FreemindMenuBar.setLabelAndMnemonic(this, Controller
		    .getText("print_preview"));
	}

	public void actionPerformed(final ActionEvent e) {
		if (!getPrintController().acquirePrinterJobAndPageFormat()) {
			return;
		}
		final MapView mapView = Controller.getController().getMapView();
		final PreviewDialog previewDialog = new PreviewDialog(Controller
		    .getText("print_preview_title"), mapView);
		previewDialog.pack();
		previewDialog.setLocationRelativeTo(JOptionPane
		    .getFrameForComponent(mapView));
		previewDialog.setVisible(true);
	}
}
