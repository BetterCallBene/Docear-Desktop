/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
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
package org.freeplane.features.mindmapmode.file;

import java.awt.Component;
import java.awt.dnd.DropTarget;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.StringBufferInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.controller.FreeplaneVersion;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.MapWriter.Mode;
import org.freeplane.core.modecontroller.MapChangeEvent;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.model.MapModel;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.resources.FpStringUtils;
import org.freeplane.core.resources.ResourceBundles;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.url.UrlManager;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogTool;
import org.freeplane.features.common.link.LinkController;
import org.freeplane.features.mindmapmode.MMapController;
import org.freeplane.features.mindmapmode.MMapModel;
import org.freeplane.n3.nanoxml.XMLException;
import org.freeplane.n3.nanoxml.XMLParseException;

/**
 * @author Dimitry Polivaev
 */
public class MFileManager extends UrlManager {
	static private class BackupFlag implements IExtension {
	}

	private class MindMapFilter extends FileFilter {
		@Override
		public boolean accept(final File f) {
			if (f.isDirectory()) {
				return true;
			}
			final String extension = UrlManager.getExtension(f.getName());
			if (extension != null) {
				if (extension.equals(UrlManager.FREEPLANE_FILE_EXTENSION_WITHOUT_DOT)) {
					return true;
				}
				else {
					return false;
				}
			}
			return false;
		}

		@Override
		public String getDescription() {
			return ResourceBundles.getText("mindmaps_desc");
		}
	}

	private static final String BACKUP_FILE_NUMBER = "backup_file_number";
	private static final String EXPECTED_START_STRINGS[] = { "<map version=\"" + FreeplaneVersion.XML_VERSION + "\"",
	        "<map version=\"0.7.1\"" };
	private static final String FREEPLANE_VERSION_UPDATER_XSLT = "/xslt/freeplane_version_updater.xslt";

	private static void backupFile(final File file, final int backupFileNumber, final String dir, final String extension) {
		if (backupFileNumber == 0) {
			return;
		}
		final String name = file.getName();
		final File backupDir = new File(file.getParentFile(), dir);
		backupDir.mkdir();
		if (backupDir.exists()) {
			final File backupFile = MFileManager.renameBackupFiles(backupDir, name, backupFileNumber, extension);
			if (!backupFile.exists()) {
				file.renameTo(backupFile);
			}
		}
	}

	static File createBackupFile(final File backupDir, final String name, final int number, final String extension) {
		return new File(backupDir, name + '.' + number + '.' + extension);
	}

	static File renameBackupFiles(final File backupDir, final String name, final int backupFileNumber,
	                              final String extension) {
		if (backupFileNumber == 0) {
			return null;
		}
		for (int i = backupFileNumber + 1;; i++) {
			final File newFile = MFileManager.createBackupFile(backupDir, name, i, extension);
			if (!newFile.exists()) {
				break;
			}
			newFile.delete();
		}
		int i = backupFileNumber;
		for (;;) {
			final File newFile = MFileManager.createBackupFile(backupDir, name, i, extension);
			if (newFile.exists()) {
				break;
			}
			i--;
			if (i == 0) {
				break;
			}
		}
		if (i < backupFileNumber) {
			return MFileManager.createBackupFile(backupDir, name, i + 1, extension);
		}
		for (i = 1; i < backupFileNumber; i++) {
			final File newFile = MFileManager.createBackupFile(backupDir, name, i, extension);
			final File oldFile = MFileManager.createBackupFile(backupDir, name, i + 1, extension);
			newFile.delete();
			if (!oldFile.renameTo(newFile)) {
				return null;
			}
		}
		return MFileManager.createBackupFile(backupDir, name, backupFileNumber, extension);
	}

	FileFilter filefilter = new MindMapFilter();

	/**
	 * @param modeController
	 */
	public MFileManager(final ModeController modeController) {
		super(modeController);
		createActions(modeController);
	}

	private void backup(final File file) {
		if (file == null) {
			return;
		}
		final int backupFileNumber = ResourceController.getResourceController().getIntProperty(BACKUP_FILE_NUMBER, 0);
		MFileManager.backupFile(file, backupFileNumber, ".backup", "bak");
	}

	/**
	 *
	 */
	private void createActions(final ModeController modeController) {
		final Controller controller = modeController.getController();
		getController().addAction(new OpenAction(controller));
		modeController.addAction(new SaveAction(controller));
		modeController.addAction(new SaveAsAction(controller));
		modeController.addAction(new ExportBranchAction(controller));
		modeController.addAction(new ImportBranchAction(controller));
		modeController.addAction(new ImportLinkedBranchAction(controller));
		modeController.addAction(new ImportLinkedBranchWithoutRootAction(controller));
		modeController.addAction(new ImportExplorerFavoritesAction(controller));
		modeController.addAction(new ImportFolderStructureAction(controller));
		modeController.addAction(new RevertAction(controller));
	}

	protected JFileChooser getFileChooser() {
		return getFileChooser(getFileFilter());
	}

	public FileFilter getFileFilter() {
		return filefilter;
	};

	/**
	 * Creates a proposal for a file name to save the map. Removes all illegal
	 * characters. Fixed: When creating file names based on the text of the root
	 * node, now all the extra unicode characters are replaced with _. This is
	 * not very good. For chinese content, you would only get a list of ______
	 * as a file name. Only characters special for building file paths shall be
	 * removed (rather than replaced with _), like : or /. The exact list of
	 * dangeous characters needs to be investigated. 0.8.0RC3. Keywords: suggest
	 * file name.
	 *
	 * @param map
	 */
	private String getFileNameProposal(final MapModel map) {
		String rootText = (map.getRootNode()).getPlainTextContent();
		rootText = rootText.replaceAll("[&:/\\\\\0%$#~\\?\\*]+", "");
		return rootText;
	}

	/**
	 * @return
	 */
	public URI getLinkByFileChooser(final MapModel map) {
		return getLinkByFileChooser(map, getFileFilter());
	}

	public URI getLinkByFileChooser(final MapModel map, final FileFilter fileFilter) {
		File input;
		JFileChooser chooser = null;
		if (map.getFile() == null) {
			JOptionPane.showMessageDialog(getController().getViewController().getContentPane(), ResourceBundles
			    .getText("not_saved_for_link_error"), "Freeplane", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		if (getLastCurrentDir() != null) {
			chooser = new JFileChooser(getLastCurrentDir());
		}
		else {
			chooser = new JFileChooser();
		}
		if (fileFilter != null) {
			chooser.setFileFilter(fileFilter);
		}
		else {
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
		}
		final int returnVal = chooser.showOpenDialog(getController().getViewController().getContentPane());
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		input = chooser.getSelectedFile();
		setLastCurrentDir(input.getParentFile());
		if (ResourceController.getResourceController().getProperty("links").equals("relative")) {
			return LinkController.toRelativeURI(map.getFile(), input);
		}
		return input.toURI();
	}

	@Override
	public void load(final URL url, final MapModel map) throws FileNotFoundException, IOException, XMLParseException,
	        URISyntaxException {
		final File file = Compat.urlToFile(url);
		if (!file.exists()) {
			throw new FileNotFoundException(FpStringUtils.formatText("file_not_found", file.getPath()));
		}
		if (!file.canWrite()) {
			((MMapModel) map).setReadOnly(true);
		}
		else {
			try {
				final String lockingUser = tryToLock(map, file);
				if (lockingUser != null) {
					UITools.informationMessage(getController().getViewController().getFrame(), FpStringUtils
					    .formatText("map_locked_by_open", file.getName(), lockingUser));
					((MMapModel) map).setReadOnly(true);
				}
				else {
					((MMapModel) map).setReadOnly(false);
				}
			}
			catch (final Exception e) {
				LogTool.severe(e);
				UITools.informationMessage(getController().getViewController().getFrame(), FpStringUtils.formatText(
				    "locking_failed_by_open", file.getName()));
				((MMapModel) map).setReadOnly(true);
			}
		}
		final NodeModel root = loadTree(map, file);
		if (root != null) {
			((MMapModel) map).setRoot(root);
		}
	}

	public NodeModel loadTree(final MapModel map, final File file) throws XMLParseException, IOException {
		final FileInputStream input = new FileInputStream(file);
		final FileChannel channel = input.getChannel();
		final FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true);
		if (lock == null) {
			throw new IOException("can not obtain file lock for " + file);
		}
		try{
			final NodeModel rootNode = loadTreeImpl(map, input);
			return rootNode;
		}
		catch (final Exception ex) {
			final String errorMessage = "Error while parsing file:" + file;
			LogTool.warn(errorMessage, ex);
			UITools.errorMessage(errorMessage);
			final NodeModel result = new NodeModel(map);
			result.setText(errorMessage);
			return result;
		}
		finally{
			setFile(map, file);
		}
	}

	private NodeModel loadTreeImpl(final MapModel map, final FileInputStream input) throws FileNotFoundException,
	        IOException, XMLException {
		int versionInfoLength = EXPECTED_START_STRINGS[0].length();
		final String buffer = readFileStart(input, versionInfoLength).toString();
		final StringBufferInputStream startInput = new StringBufferInputStream(buffer);
		final InputStream sequencedInput = new SequenceInputStream(startInput, input);
		Reader reader = null;
		for (int i = 0; i < EXPECTED_START_STRINGS.length; i++) {
			versionInfoLength = EXPECTED_START_STRINGS[i].length();
			String mapStart = "";
			if (buffer.length() >= versionInfoLength) {
				mapStart = buffer.substring(0, versionInfoLength);
			}
			if (mapStart.startsWith(EXPECTED_START_STRINGS[i])) {
				reader = UrlManager.getActualReader(sequencedInput);
				break;
			}
		}
		if (reader == null) {
			final Controller controller = getController();
			final int showResult = OptionalDontShowMeAgainDialog.show(controller, "really_convert_to_current_version",
			    "confirmation", MMapController.RESOURCES_CONVERT_TO_CURRENT_VERSION,
			    OptionalDontShowMeAgainDialog.ONLY_OK_SELECTION_IS_STORED);
			if (showResult != JOptionPane.OK_OPTION) {
				reader = UrlManager.getActualReader(sequencedInput);
			}
			else {
				reader = UrlManager.getUpdateReader(sequencedInput, FREEPLANE_VERSION_UPDATER_XSLT);
			}
		}
		try {
			return getModeController().getMapController().getMapReader().createNodeTreeFromXml(map, reader, Mode.FILE);
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Override
	public void loadURL(final URI relative) {
		final MapModel map = getController().getMap();
		if (map.getFile() == null) {
			if (!relative.isAbsolute() || relative.isOpaque()) {
				getController().getViewController().out("You must save the current map first!");
				final boolean result = ((MFileManager) UrlManager.getController(getModeController())).save(map);
				if (!result) {
					return;
				}
			}
		}
		super.loadURL(relative);
	}

	public void open() {
		final JFileChooser chooser = getFileChooser();
		final int returnVal = chooser.showOpenDialog(getController().getViewController().getMapView());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] selectedFiles;
			if (chooser.isMultiSelectionEnabled()) {
				selectedFiles = chooser.getSelectedFiles();
			}
			else {
				selectedFiles = new File[] { chooser.getSelectedFile() };
			}
			for (int i = 0; i < selectedFiles.length; i++) {
				final File theFile = selectedFiles[i];
				try {
					setLastCurrentDir(theFile.getParentFile());
					getModeController().getMapController().newMap(Compat.fileToUrl(theFile));
				}
				catch (final Exception ex) {
					handleLoadingException(ex);
					break;
				}
			}
		}
		getController().getViewController().setTitle();
	}

	/**
	 * Returns pMinimumLength bytes of the files content.
	 * @throws IOException 
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private String readFileStart(final FileInputStream input, final int pMinimumLength) throws IOException {
		final byte[] buffer = new byte[pMinimumLength];
		input.read(buffer);
		return new String(buffer);
	}

	public boolean save(final MapModel map) {
		if (map.isSaved()) {
			return true;
		}
		if (map.getURL() == null || map.isReadOnly()) {
			return saveAs(map);
		}
		else {
			return save(map, map.getFile());
		}
	}

	/**
	 * Return the success of saving
	 */
	public boolean save(final MapModel map, final File file) {
		try {
			if (null == map.getExtension(BackupFlag.class)) {
				map.addExtension(new BackupFlag());
				backup(file);
			}
			final String lockingUser = tryToLock(map, file);
			if (lockingUser != null) {
				UITools.informationMessage(getController().getViewController().getFrame(), FpStringUtils.formatText(
				    "map_locked_by_save_as", file.getName(), lockingUser));
				return false;
			}
		}
		catch (final Exception e) {
			UITools.informationMessage(getController().getViewController().getFrame(), FpStringUtils.formatText(
			    "locking_failed_by_save_as", file.getName()));
			return false;
		}
		final URL urlBefore = map.getURL();
		final boolean saved = saveInternal((MMapModel) map, file, false);
		if (!saved) {
			return false;
		}
		final URL urlAfter = map.getURL();
		final MMapController mapController = (MMapController) getModeController().getMapController();
		mapController.fireMapChanged(new MapChangeEvent(this, map, UrlManager.MAP_URL, urlBefore, urlAfter));
		mapController.setSaved(map, true);
		return true;
	}

	/**
	 * Save as; return false is the action was cancelled
	 */
	public boolean saveAs(final MapModel map) {
		final JFileChooser chooser = getFileChooser();
		if (getMapsParentFile() == null) {
			chooser.setSelectedFile(new File(getFileNameProposal(map)
			        + org.freeplane.core.url.UrlManager.FREEPLANE_FILE_EXTENSION));
		}
		else {
			chooser.setSelectedFile(map.getFile());
		}
		chooser.setDialogTitle(ResourceBundles.getText("SaveAsAction.text"));
		final int returnVal = chooser.showSaveDialog(getController().getViewController().getMapView());
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return false;
		}
		File f = chooser.getSelectedFile();
		setLastCurrentDir(f.getParentFile());
		final String ext = UrlManager.getExtension(f.getName());
		if (!ext.equals(org.freeplane.core.url.UrlManager.FREEPLANE_FILE_EXTENSION_WITHOUT_DOT)) {
			f = new File(f.getParent(), f.getName() + org.freeplane.core.url.UrlManager.FREEPLANE_FILE_EXTENSION);
		}
		if (f.exists()) {
			final int overwriteMap = JOptionPane.showConfirmDialog(getController().getViewController().getMapView(),
			    ResourceBundles.getText("map_already_exists"), "Freeplane", JOptionPane.YES_NO_OPTION);
			if (overwriteMap != JOptionPane.YES_OPTION) {
				return false;
			}
		}
		// extra backup in this case.
		File oldFile = map.getFile();
		if (oldFile != null) {
			oldFile = oldFile.getAbsoluteFile();
		}
		if (!f.getAbsoluteFile().equals(oldFile) && null != map.getExtension(BackupFlag.class)) {
			map.removeExtension(BackupFlag.class);
		}
		if (save(map, f)) {
			getController().getMapViewManager().updateMapViewName();
			return true;
		}
		return false;
	}

	/**
	 * This method is intended to provide both normal save routines and saving
	 * of temporary (internal) files.
	 */
	boolean saveInternal(final MMapModel map, final File file, final boolean isInternal) {
		if (!isInternal && map.isReadOnly()) {
			System.err.println("Attempt to save read-only map.");
			return false;
		}
		try {
			if (map.getTimerForAutomaticSaving() != null) {
				map.getTimerForAutomaticSaving().cancel();
			}
			final FileOutputStream out = new FileOutputStream(file);
			final FileLock lock = out.getChannel().tryLock();
			if (lock == null) {
				throw new IOException("can not obtain file lock for " + file);
			}
			final BufferedWriter fileout = new BufferedWriter(new OutputStreamWriter(out));
			getModeController().getMapController().getMapWriter().writeMapAsXml(map, fileout, Mode.FILE, true);
			if (!isInternal) {
				setFile(map, file);
				map.setSaved(true);
			}
			map.scheduleTimerForAutomaticSaving(getModeController());
			return true;
		}
		catch (final IOException e) {
			final String message = FpStringUtils.formatText("save_failed", file.getName());
			if (!isInternal) {
				UITools.errorMessage(message);
			}
			else {
				getController().getViewController().out(message);
			}
		}
		catch (final Exception e) {
			LogTool.severe("Error in MapModel.save(): ", e);
		}
		map.scheduleTimerForAutomaticSaving(getModeController());
		return false;
	}

	public void setFile(final MapModel map, final File file) {
		try {
			final URL url = Compat.fileToUrl(file);
			setURL(map, url);
		}
		catch (final MalformedURLException e) {
			LogTool.severe(e);
		}
	}

	@Override
	public void startup() {
		final ModeController modeController = getModeController();
		final Component mapView = getController().getViewController().getMapView();
		if (mapView != null) {
			final FileOpener fileOpener = new FileOpener(modeController);
			new DropTarget(mapView, fileOpener);
		}
	}

	/**
	 * Attempts to lock the map using a semaphore file
	 *
	 * @return If the map is locked, return the name of the locking user,
	 *         otherwise return null.
	 * @throws Exception
	 *             , when the locking failed for other reasons than that the
	 *             file is being edited.
	 */
	public String tryToLock(final MapModel map, final File file) throws Exception {
		final String lockingUser = ((MMapModel) map).getLockManager().tryToLock(file);
		final String lockingUserOfOldLock = ((MMapModel) map).getLockManager().popLockingUserOfOldLock();
		if (lockingUserOfOldLock != null) {
			UITools.informationMessage(getController().getViewController().getFrame(), FpStringUtils.formatText(
			    "locking_old_lock_removed", file.getName(), lockingUserOfOldLock));
		}
		if (lockingUser == null) {
			((MMapModel) map).setReadOnly(false);
		}
		return lockingUser;
	}
}
