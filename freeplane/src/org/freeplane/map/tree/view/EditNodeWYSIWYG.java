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
package org.freeplane.map.tree.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

import org.freeplane.controller.Controller;
import org.freeplane.main.HtmlTools;
import org.freeplane.main.Tools;
import org.freeplane.modes.ModeController;
import org.freeplane.ui.FreemindMenuBar;

import com.lightdev.app.shtm.SHTMLPanel;

/**
 * @author Daniel Polansky
 */
public class EditNodeWYSIWYG extends EditNodeBase {
	private static class HTMLDialog extends EditDialog {
		private SHTMLPanel htmlEditorPanel;

		HTMLDialog(final EditNodeBase base) throws Exception {
			super(base);
			createEditorPanel();
			getContentPane().add(htmlEditorPanel, BorderLayout.CENTER);
			Tools.addEscapeActionToDialog(this, new CancelAction());
			final JButton okButton = new JButton();
			final JButton cancelButton = new JButton();
			final JButton splitButton = new JButton();
			FreemindMenuBar.setLabelAndMnemonic(okButton, base.getText("ok"));
			FreemindMenuBar.setLabelAndMnemonic(cancelButton, base
			    .getText("cancel"));
			FreemindMenuBar.setLabelAndMnemonic(splitButton, base
			    .getText("split"));
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					submit();
				}
			});
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					cancel();
				}
			});
			splitButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					split();
				}
			});
			Tools.addKeyActionToDialog(this, new SubmitAction(), "alt ENTER",
			    "submit");
			final JPanel buttonPane = new JPanel();
			buttonPane.add(okButton);
			buttonPane.add(cancelButton);
			buttonPane.add(splitButton);
			buttonPane.setMaximumSize(new Dimension(1000, 20));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
		}

		/*
		 * (non-Javadoc)
		 * @see freemind.view.mindmapview.EditNodeBase.Dialog#close()
		 */
		@Override
		protected void cancel() {
			htmlEditorPanel.getDocument().getStyleSheet().removeStyle("body");
			getBase().getEditControl().cancel();
			super.cancel();
		}

		private SHTMLPanel createEditorPanel() throws Exception {
			if (htmlEditorPanel == null) {
				htmlEditorPanel = SHTMLPanel.createSHTMLPanel();
			}
			return htmlEditorPanel;
		}

		/**
		 * @return Returns the htmlEditorPanel.
		 */
		public SHTMLPanel getHtmlEditorPanel() {
			return htmlEditorPanel;
		}

		@Override
		public Component getMostRecentFocusOwner() {
			if (isFocused()) {
				return getFocusOwner();
			}
			else {
				return htmlEditorPanel.getMostRecentFocusOwner();
			}
		}

		@Override
		protected boolean isChanged() {
			return htmlEditorPanel.needsSaving();
		}

		/*
		 * (non-Javadoc)
		 * @see freemind.view.mindmapview.EditNodeBase.Dialog#split()
		 */
		@Override
		protected void split() {
			htmlEditorPanel.getDocument().getStyleSheet().removeStyle("body");
			getBase().getEditControl().split(
			    HtmlTools.unescapeHTMLUnicodeEntity(htmlEditorPanel
			        .getDocumentText()), htmlEditorPanel.getCaretPosition());
			super.split();
		}

		/*
		 * (non-Javadoc)
		 * @see freemind.view.mindmapview.EditNodeBase.Dialog#close()
		 */
		@Override
		protected void submit() {
			htmlEditorPanel.getDocument().getStyleSheet().removeStyle("body");
			if (htmlEditorPanel.needsSaving()) {
				getBase().getEditControl().ok(
				    HtmlTools.unescapeHTMLUnicodeEntity(htmlEditorPanel
				        .getDocumentText()));
			}
			else {
				getBase().getEditControl().cancel();
			}
			super.submit();
		}
	}

	private static HTMLDialog htmlEditorWindow;
	final private KeyEvent firstEvent;

	public EditNodeWYSIWYG(final NodeView node, final String text,
	                       final KeyEvent firstEvent,
	                       final ModeController controller,
	                       final IEditControl editControl) {
		super(node, text, controller, editControl);
		this.firstEvent = firstEvent;
	}

	public void show() {
		try {
			if (EditNodeWYSIWYG.htmlEditorWindow == null) {
				EditNodeWYSIWYG.htmlEditorWindow = new HTMLDialog(this);
			}
			EditNodeWYSIWYG.htmlEditorWindow.setBase(this);
			final SHTMLPanel htmlEditorPanel = (EditNodeWYSIWYG.htmlEditorWindow)
			    .getHtmlEditorPanel();
			String rule = "BODY {";
			final Font font = node.getTextFont();
			final Color nodeTextBackground = node.getTextBackground();
			rule += "font-family: " + font.getFamily() + ";";
			rule += "font-size: " + font.getSize() + "pt;";
			if (font.isItalic()) {
				rule += "font-style: italic; ";
			}
			if (font.isBold()) {
				rule += "font-weight: bold; ";
			}
			final Color nodeTextColor = node.getTextColor();
			rule += "color: " + Tools.colorToXml(nodeTextColor) + ";";
			rule += "}\n";
			rule += "p {";
			rule += "margin-top:0;";
			rule += "}\n";
			final HTMLDocument document = htmlEditorPanel.getDocument();
			final JEditorPane editorPane = htmlEditorPanel.getEditorPane();
			editorPane.setForeground(nodeTextColor);
			editorPane.setBackground(nodeTextBackground);
			editorPane.setCaretColor(nodeTextColor);
			document.getStyleSheet().addRule(rule);
			document.setBase(node.getMap().getModel().getURL());
			int preferredHeight = (int) (node.getMainView().getHeight() * 1.2);
			preferredHeight = Math.max(preferredHeight, Integer
			    .parseInt(Controller.getResourceController().getProperty(
			        "el__min_default_window_height")));
			preferredHeight = Math.min(preferredHeight, Integer
			    .parseInt(Controller.getResourceController().getProperty(
			        "el__max_default_window_height")));
			int preferredWidth = (int) (node.getMainView().getWidth() * 1.2);
			preferredWidth = Math.max(preferredWidth, Integer
			    .parseInt(Controller.getResourceController().getProperty(
			        "el__min_default_window_width")));
			preferredWidth = Math.min(preferredWidth, Integer
			    .parseInt(Controller.getResourceController().getProperty(
			        "el__max_default_window_width")));
			htmlEditorPanel.setContentPanePreferredSize(new Dimension(
			    preferredWidth, preferredHeight));
			EditNodeWYSIWYG.htmlEditorWindow.pack();
			Tools.setDialogLocationRelativeTo(EditNodeWYSIWYG.htmlEditorWindow,
			    node);
			String content = node.getModel().toString();
			if (!HtmlTools.isHtmlNode(content)) {
				content = HtmlTools.plainToHTML(content);
			}
			htmlEditorPanel.setCurrentDocumentContent(content);
			if (firstEvent instanceof KeyEvent) {
				final KeyEvent firstKeyEvent = firstEvent;
				final JTextComponent currentPane = htmlEditorPanel
				    .getEditorPane();
				if (currentPane == htmlEditorPanel.getMostRecentFocusOwner()) {
					redispatchKeyEvents(currentPane, firstKeyEvent);
				}
			}
			else {
				editorPane.setCaretPosition(htmlEditorPanel.getDocument()
				    .getLength());
			}
			htmlEditorPanel.getMostRecentFocusOwner().requestFocus();
			EditNodeWYSIWYG.htmlEditorWindow.show();
		}
		catch (final Exception ex) {
			org.freeplane.main.Tools.logException(ex);
			System.err
			    .println("Loading of WYSIWYG HTML editor failed. Use the other editors instead.");
		}
	}
}
