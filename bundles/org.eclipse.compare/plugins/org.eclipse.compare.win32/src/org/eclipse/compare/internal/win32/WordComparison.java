/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.win32;

import java.io.File;
import java.util.Vector;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleClientSite;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Class that manages a Word document comparison using OLE.
 */
public class WordComparison {

	private final OleFrame frame;
	private OleClientSite site;
	private boolean inplace;
	private OleAutomation document;
	
	public WordComparison(Composite composite) {
		frame = new OleFrame(composite, SWT.NONE);
	}

	// These helper methods facilitate writing the OLE apps
	private static Variant invoke(OleAutomation auto, String command) {
		return auto.invoke(property(auto, command), new Variant[0]);
	}

	private static Variant invoke(OleAutomation auto, String command, int value) {
		return auto.invoke(property(auto, command), new Variant[] { new Variant(value) });
	}

	private static Variant invoke(OleAutomation auto, String command, String value) {
		return auto.invoke(property(auto, command), new Variant[] { new Variant(value) });
	}

	private static Variant getVariantProperty(OleAutomation auto, String name) {
		Variant varResult = auto.getProperty(property(auto, name));
		if (varResult != null && varResult.getType() != OLE.VT_EMPTY) {
			return varResult;
		}
		throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_0, name));
	}

	private static OleAutomation getAutomationProperty(OleAutomation auto, String name) {
		Variant varResult = getVariantProperty(auto, name);
		try {
			OleAutomation automation = varResult.getAutomation();
			if (automation != null)
				return automation;
		} finally {
			varResult.dispose();
		}
		throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_1, name));
	}
	
	private static OleAutomation getAutomationResult(OleAutomation auto, String command, int value) {
		Variant varResult = invoke(auto, command, value);
		if (varResult != null) {
			try {
				OleAutomation result = varResult.getAutomation();
				if (result != null)
					return result;
			} finally {
				varResult.dispose();
			}
		}
		throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_2, command, Integer.toString(value)));
	}
	
	private static OleAutomation getAutomationResult(OleAutomation auto, String command, String value) {
		Variant varResult = invoke(auto, command, value);
		if (varResult != null) {
			try {
				OleAutomation result = varResult.getAutomation();
				if (result != null)
					return result;
			} finally {
				varResult.dispose();
			}
		}
		throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_3, command, value));
	}

	private static int property(OleAutomation auto, String name) {
		int[] ids = auto.getIDsOfNames(new String[] { name });
		if (ids == null) throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_4, name));
		return ids[0];
	}

	private static boolean setProperty(OleAutomation auto, String name, boolean value) {
		return auto.setProperty(property(auto, name), new Variant(value));
	}

	/**
	 * Open the file at the given path as a document in Word.
	 * @param filePath the path of the file containing the document
	 * @param inplace whether Word is to be opened in-place or in a separate window
	 * @throws SWTException if the document could not be opened for some reason
	 */
	public void openDocument(String filePath, boolean inplace) throws SWTException {
		resetSite(inplace ? filePath : null);
		if (inplace) {
			site.doVerb(OLE.OLEIVERB_SHOW);
		} else {
			OleAutomation application = createApplication();
			try {
				// Track the external document we just opened
				document = openDocument(application, filePath);
				setDocumentVisible(document, true);
			} finally {
				application.dispose();
			}
		}
	}

	/**
	 * Compares the base document with the revised document and saves the comparison in the
	 * working copy which can then be opened using openDocument.
	 * @param baseDocument the base document
	 * @param revisedDocument the revised document
	 * @param workingCopy the working copy (will be overwritten)
	 * @throws SWTException if an SWT error occurs
	 */
	public void createWorkingCopy(String baseDocument, String revisedDocument, String workingCopy) throws SWTException {
		resetSite(null);
		OleAutomation application = createApplication();
		try {
			OleAutomation document = openDocument(application, revisedDocument);
			try {
				setDocumentVisible(document,false);
				compareDocument(document, baseDocument, revisedDocument);
				OleAutomation activeDocument = getActiveDocument(application);
				try {
					Variant varResult = invoke(activeDocument, "SaveAs", workingCopy); //$NON-NLS-1$
					if (varResult == null)
						throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_6, workingCopy));
					varResult.dispose();
				} finally {
					try {
						closeDocument(activeDocument);
					} finally {
						activeDocument.dispose();
					}
				}
			} finally {
				try {
					closeDocument(document);
				} finally {
					document.dispose();
				}
			}
		} finally {
			application.dispose();
		}
	}
	
	private void closeDocument(OleAutomation document) {
		// Close the first document: destination.Close()
		Variant varResult = invoke(document, "Close"); //$NON-NLS-1$
		if (varResult != null) {
			varResult.dispose();
		}
	}

	private void compareDocument(OleAutomation document, String baseDocument, String revisedDocument) {
		// Compare to the second document: compare = destination.Compare(p1)
		Variant varResult = invoke(document, "Compare", baseDocument); //$NON-NLS-1$
		if (varResult == null)
			throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_9, baseDocument, revisedDocument));
		varResult.dispose();
	}
	
	private boolean getDocumentDirty(OleAutomation document) {
		// 		word.document.Saved
		if (document != null) {
			Variant variantProperty = getVariantProperty(document, "Saved"); //$NON-NLS-1$
			if (variantProperty != null) {
				try {
					return !variantProperty.getBoolean();
				} finally {
					variantProperty.dispose();
				}
			}
		}
		return false;
	}
	
	private void setDocumentVisible(OleAutomation document, boolean visible) {
		// Hide it: destination.Windows[0].Visible=0|1
		OleAutomation windows = getAutomationProperty(document, "Windows"); //$NON-NLS-1$
		try {
			OleAutomation window = getAutomationResult(windows, "Item", 1); //$NON-NLS-1$
			try {
				setProperty(window, "Visible", visible); //$NON-NLS-1$
			} finally {
				window.dispose();
			}
		} finally {
			windows.dispose();
		}
	}

	private OleAutomation openDocument(OleAutomation application, String doc) {
		// Open the first document: word.Documents.Open(p2)
		OleAutomation documents = getAutomationProperty(application, "Documents"); //$NON-NLS-1$
		try {
			OleAutomation document = getAutomationResult(documents, "Open", doc); //$NON-NLS-1$
			if (document == null) {
				throw new SWTException(NLS.bind(CompareWin32Messages.WordComparison_16, doc));
			}
			return document;
		} finally {
			documents.dispose();
		}
	}
	
	private OleAutomation getActiveDocument(OleAutomation application) {
		return getAutomationProperty(application, "ActiveDocument"); //$NON-NLS-1$
	}

	/*
	 * Create a handle to the application
	 */
	private OleAutomation createApplication() {
		return new OleAutomation(site);
	}

	/*
	 * When opening a new comparison, we want to close any existing site 
	 * and create a new one.
	 */
	private void resetSite(String filePath) {
		if (site != null && !site.isDisposed()) {
			disposeSite();
		}
		inplace = filePath != null;
		if (inplace) {
			site = new OleClientSite(frame, SWT.NONE, "Word.Document", new File(filePath)); //$NON-NLS-1$
		} else {
			site = new OleClientSite(frame, SWT.NONE, "Word.Application"); //$NON-NLS-1$
		}
	}

	private void disposeSite() {
		if (document != null) {
			try {
				closeDocument(document);
			} catch (SWTException e) {
				Activator.log(e);
			}
			document.dispose();
			document = null;
			OleAutomation application = createApplication();
			try {
				OleAutomation documents = getAutomationProperty(application, "Documents"); //$NON-NLS-1$
				try {
					Variant property = getVariantProperty(documents, "Count"); //$NON-NLS-1$
					if (property != null) {
						try {
							if (property.getLong() == 0) {
								// There are no other documents open so close the application
								Variant result = invoke(application, "Quit"); //$NON-NLS-1$
								if (result != null) {
									result.dispose();
								}
							}
						} finally {
							property.dispose();
						}
					}
				} finally {
					documents.dispose();
				}
			} finally {
				application.dispose();
			}
		}
		site.dispose();
		site = null;
	}

	public void saveAsDocument(String doc) {
		if (site == null || site.isDisposed()) return;
		if (inplace) {
			site.deactivateInPlaceClient();
			site.save(new File(doc), true);
			site.doVerb(OLE.OLEIVERB_SHOW);
		} else if (document != null) {
			try {
				Variant variant = invoke(document, "SaveAs", doc); //$NON-NLS-1$
				if (variant != null) {
					variant.dispose();
				}
			} catch (SWTException e) {
				// Ignore since this probably means the document was closed by the user
			}
		}
	}

	/**
	 * Return the OLEFrame for the comparison document.
	 * @return the OLEFrame for the comparison document
	 */
	public OleFrame getFrame() {
		return frame;
	}
	
	/**
	 * Dispose of the comparison.
	 */
	public void dispose() {
		try {
			disposeSite();
		} finally {
			if (!frame.isDisposed()) {
				frame.dispose();
			}
		}
	}

	/**
	 * Return whether the comparison document is dirty. This method handles 
	 * both an in-place document and a document opened in a separate window.
	 * @return weather the comparison document is dirty
	 */
	public boolean isDirty() {
		return (inplace && site != null && !site.isDisposed() && site.isDirty())
			|| (!inplace && getDocumentDirty(document));
	}
	
    /**
     *	Initialize the workbench menus for proper menu merging
     *  Copied from org.eclipse.ui.internal.editorsupport.win32OleEditor
     */
    protected void initializeWorkbenchMenus(IWorkbenchWindow window) {
        //If there was an OLE Error or nothing has been created yet
        if (frame == null || frame.isDisposed())
            return;
        // Get the browser menu bar.  If one does not exist then
        // create it.
        Shell shell = frame.getShell();
        Menu menuBar = shell.getMenuBar();
        if (menuBar == null) {
            menuBar = new Menu(shell, SWT.BAR);
            shell.setMenuBar(menuBar);
        }

        // Swap the file and window menus.
        MenuItem[] windowMenu = new MenuItem[1];
        MenuItem[] fileMenu = new MenuItem[1];
        Vector containerItems = new Vector();

        for (int i = 0; i < menuBar.getItemCount(); i++) {
            MenuItem item = menuBar.getItem(i);
            String id = ""; //$NON-NLS-1$
            if (item.getData() instanceof IMenuManager)
                id = ((IMenuManager) item.getData()).getId();
            if (id.equals(IWorkbenchActionConstants.M_FILE))
                fileMenu[0] = item;
            else if (id.equals(IWorkbenchActionConstants.M_WINDOW))
                windowMenu[0] = item;
            else {
                if (window.isApplicationMenu(id)) {
                    containerItems.addElement(item);
                }
            }
        }
        MenuItem[] containerMenu = new MenuItem[containerItems.size()];
        containerItems.copyInto(containerMenu);
        frame.setFileMenus(fileMenu);
        frame.setContainerMenus(containerMenu);
        frame.setWindowMenus(windowMenu);
    }

    /**
     * Return whether the comparison document is being shown in-place or in
     * a separate window.
     * @return whether the comparison document is being shown in-place or in
     * a separate window
     */
	public boolean isInplace() {
		return inplace;
	}

	/**
	 * Return whether the comparison document is open.
	 * @return whether the comparison document is open
	 */
	public boolean isOpen() {
		return site != null && !site.isDisposed();
	}

	/**
	 * Close any open documents
	 */
	public void close() {
		if (isOpen()) {
			disposeSite();
		}	
	}
}
