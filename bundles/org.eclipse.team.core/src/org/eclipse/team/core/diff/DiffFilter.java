/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.diff;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * A <code>DiffNodeFilter</code> tests an {@link IDiff} for inclusion,
 * typically in an {@link IDiffTree}.
 *
 * @see IDiff
 * @see IDiffTree
 *
 * @since 3.2
 */
public abstract class DiffFilter {

	/**
	 * Return <code>true</code> if the provided <code>IDiffNode</code> matches the filter.
	 *
	 * @param diff the <code>IDiffNode</code> to be tested
	 * @param monitor a progress monitor
	 * @return <code>true</code> if the <code>IDiffNode</code> matches the filter
	 */
	public abstract boolean select(IDiff diff, IProgressMonitor monitor);
}
