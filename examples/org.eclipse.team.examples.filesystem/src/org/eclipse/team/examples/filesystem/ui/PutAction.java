/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.examples.filesystem.Policy;

/**
 * Action for checking in the selected resources
 */
public class PutAction extends FileSystemAction {

	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		try {
			PutOperation operation = new PutOperation(getTargetPart(), 
					FileSystemOperation.createScopeManager(Policy.bind("PutAction.working"), getSelectedMappings())); //$NON-NLS-1$
			operation.setOverwriteIncoming(isOverrideIncoming());
			operation.run();
		} catch (InvocationTargetException e) {
			handle(e, null, Policy.bind("PutAction.problemMessage")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Ignore
		}
	}
	
	/**
	 * Indicate whether the put should override incoming changes.
	 * @return whether the put should override incoming changes.
	 */
	protected boolean isOverrideIncoming() {
		return false;
	}
}