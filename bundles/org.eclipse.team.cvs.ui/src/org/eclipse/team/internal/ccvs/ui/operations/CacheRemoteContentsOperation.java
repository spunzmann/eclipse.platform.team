/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation that ensures that the contents for remote
 * of each local resource is cached.
 */
public class CacheRemoteContentsOperation extends CacheTreeContentsOperation {

	public CacheRemoteContentsOperation(IWorkbenchPart part, ResourceMapping[] mappers, IResourceDiffTree tree) {
		super(part, mappers, tree);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CacheTreeContentsOperation#getRemoteFileState(org.eclipse.team.core.diff.IThreeWayDiff)
	 */
	protected IFileRevision getRemoteFileState(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff == null)
			return null;
		return diff.getAfterState();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CacheTreeContentsOperation#isEnabledForDirection(int)
	 */
	protected boolean isEnabledForDirection(int direction) {
		return direction == IThreeWayDiff.CONFLICTING || 
			direction == IThreeWayDiff.INCOMING;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CacheTreeContentsOperation#buildTree(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected ICVSRemoteResource buildTree(CVSTeamProvider provider) throws TeamException {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().buildRemoteTree(provider.getProject(), true, new NullProgressMonitor());
	}

}
