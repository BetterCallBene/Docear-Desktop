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
package org.freeplane.map.link.mindmapmode;

import java.awt.event.ActionEvent;
import java.util.List;

import org.freeplane.controller.Controller;
import org.freeplane.controller.FreeMindAction;
import org.freeplane.map.link.ArrowLinkModel;
import org.freeplane.map.link.NodeLinks;
import org.freeplane.map.tree.NodeModel;
import org.freeplane.undo.IUndoableActor;

/**
 * @author foltin
 */
class AddArrowLinkAction extends FreeMindAction {
	/**
	 */
	public AddArrowLinkAction() {
		super("add_link", "images/designer.png");
	}

	public void actionPerformed(final ActionEvent e) {
		final List selecteds = getMModeController().getSelectedNodes();
		if (selecteds.size() < 2) {
			Controller.getController().errorMessage(
			    getMModeController().getText("less_than_two_selected_nodes"));
			return;
		}
		for (int i = 1; i < selecteds.size(); i++) {
			addLink((NodeModel) selecteds.get(i), (NodeModel) selecteds.get(0));
		}
	}

	public void addLink(final NodeModel source, final NodeModel target) {
		final String targetID = target.createID();
		final IUndoableActor actor = new IUndoableActor() {
			private ArrowLinkModel arrowLink;

			public void act() {
				NodeLinks nodeLinks = (NodeLinks) source
				    .getExtension(NodeLinks.class);
				if (nodeLinks == null) {
					nodeLinks = new NodeLinks();
					source.addExtension(nodeLinks);
				}
				arrowLink = new ArrowLinkModel(source, targetID);
				nodeLinks.addArrowlink(arrowLink);
				source.getModeController().getMapController().nodeChanged(
				    source);
			}

			public String getDescription() {
				return "addLink";
			}

			public void undo() {
				final NodeLinks nodeLinks = (NodeLinks) source
				    .getExtension(NodeLinks.class);
				nodeLinks.removeArrowlink(arrowLink);
				source.getModeController().getMapController().nodeChanged(
				    source);
			}
		};
		getMModeController().execute(actor);
	}
}
