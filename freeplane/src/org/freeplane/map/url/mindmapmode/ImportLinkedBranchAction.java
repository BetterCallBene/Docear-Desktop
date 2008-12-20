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
package org.freeplane.io.url.mindmapmode;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.freeplane.controller.Controller;
import org.freeplane.controller.FreeplaneAction;
import org.freeplane.main.Tools;
import org.freeplane.map.clipboard.mindmapmode.MClipboardController;
import org.freeplane.map.tree.MapModel;
import org.freeplane.map.tree.NodeModel;
import org.freeplane.map.tree.mindmapmode.MMapController;

class ImportLinkedBranchAction extends FreeplaneAction {
	public ImportLinkedBranchAction() {
		super("import_linked_branch");
	}

	public void actionPerformed(final ActionEvent e) {
		final MapModel map = Controller.getController().getMap();
		final NodeModel selected = getModeController().getSelectedNode();
		if (selected == null || selected.getLink() == null) {
			JOptionPane.showMessageDialog(getModeController().getMapView(),
			    getModeController().getText("import_linked_branch_no_link"));
			return;
		}
		URL absolute = null;
		try {
			final String relative = selected.getLink();
			absolute = Tools.isAbsolutePath(relative) ? Tools
			    .fileToUrl(new File(relative)) : new URL(Tools.fileToUrl(map
			    .getFile()), relative);
		}
		catch (final MalformedURLException ex) {
			JOptionPane.showMessageDialog(getModeController().getMapView(),
			    "Couldn't create valid URL for:" + map.getFile());
			org.freeplane.main.Tools.logException(ex);
			return;
		}
		try {
			final NodeModel node = ((MMapController) getMModeController()
			    .getMapController())
			    .loadTree(map, new File(absolute.getFile()));
			((MClipboardController) getMModeController()
			    .getClipboardController()).paste(node, selected);
		}
		catch (final Exception ex) {
			getModeController().getUrlManager().handleLoadingException(ex);
		}
	}
}
