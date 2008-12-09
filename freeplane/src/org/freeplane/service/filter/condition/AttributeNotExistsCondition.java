/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
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
package org.freeplane.service.filter.condition;

import org.freeplane.controller.Controller;
import org.freeplane.io.xml.n3.nanoxml.XMLElement;
import org.freeplane.map.attribute.IAttributeTableModel;
import org.freeplane.map.tree.NodeModel;

/**
 * @author Dimitry Polivaev
 */
public class AttributeNotExistsCondition extends NodeCondition {
	static final String ATTRIBUTE = "attribute";
	static final String NAME = "attribute_not_exists_condition";

	static ICondition load(final XMLElement element) {
		return new AttributeNotExistsCondition(element.getAttribute(
		    AttributeNotExistsCondition.ATTRIBUTE, null));
	}

	final private String attribute;

	/**
	 */
	public AttributeNotExistsCondition(final String attribute) {
		super();
		this.attribute = attribute;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freemind.controller.filter.condition.Condition#checkNode(freemind.modes
	 * .MindMapNode)
	 */
	public boolean checkNode(final NodeModel node) {
		final IAttributeTableModel attributes = node.getAttributes();
		for (int i = 0; i < attributes.getRowCount(); i++) {
			if (attributes.getValueAt(i, 0).equals(attribute)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected String createDesctiption() {
		final String simpleCondition = Controller
		    .getText(ConditionFactory.FILTER_DOES_NOT_EXIST);
		return ConditionFactory.createDescription(attribute, simpleCondition,
		    null, false);
	}

	public void toXml(final XMLElement element) {
		final XMLElement child = new XMLElement();
		child.setName(AttributeNotExistsCondition.NAME);
		super.attributesToXml(child);
		child.setAttribute(AttributeNotExistsCondition.ATTRIBUTE, attribute);
		element.addChild(child);
	}
}
