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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ui.Utils;

public class MergeSubscriberContext extends CVSSubscriberMergeContext {

	public static MergeSubscriberContext createContext(ISynchronizationScopeManager manager, Subscriber subscriber) {
		MergeSubscriberContext mergeContext = new MergeSubscriberContext(subscriber, manager);
		mergeContext.initialize();
		return mergeContext;
	}

	private boolean cancel = true;
	
	public MergeSubscriberContext(Subscriber subscriber, ISynchronizationScopeManager manager) {
		super(subscriber, manager);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IMergeContext#markAsMerged(org.eclipse.team.core.diff.IDiffNode, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void markAsMerged(final IDiff diff, boolean inSyncHint, IProgressMonitor monitor) throws CoreException {
		run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				((CVSMergeSubscriber)getSubscriber()).merged(new IResource[] { getDiffTree().getResource(diff)});
			}
		}, getMergeRule(diff), IResource.NONE, monitor);
	}
	
	public void markAsMerged(final IDiff[] diffs, boolean inSyncHint, IProgressMonitor monitor) throws CoreException {
		run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				List result = new ArrayList();
				for (int i = 0; i < diffs.length; i++) {
					IDiff diff = diffs[i];
					result.add(getDiffTree().getResource(diff));
				}
				((CVSMergeSubscriber)getSubscriber()).merged((IResource[]) result.toArray(new IResource[result.size()]));
			}
		}, getMergeRule(diffs), IResource.NONE, monitor);
	}
	
	public void dispose() {
		if (cancel)
			((CVSMergeSubscriber)getSubscriber()).cancel();
		super.dispose();
	}

	public void setCancelSubscriber(boolean b) {
		cancel  = b;
	}
	
	public IStatus merge(final IDiff diff, final boolean ignoreLocalChanges, IProgressMonitor monitor) throws CoreException {
		final IStatus[] status = new IStatus[] { Status.OK_STATUS };
		run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IThreeWayDiff currentDiff = (IThreeWayDiff)getSubscriber().getDiff(getDiffTree().getResource(diff));
				if (!MergeSubscriberContext.this.equals(currentDiff, (IThreeWayDiff)diff)) {
					throw new CVSException(NLS.bind(CVSUIMessages.CVSMergeContext_1, diff.getPath()));
				}
				status[0] = MergeSubscriberContext.super.merge(diff, ignoreLocalChanges, monitor);
				if (status[0].isOK()) {
					IResource resource = ResourceDiffTree.getResourceFor(diff);
					if (resource.getType() == IResource.FILE && resource.exists() && diff instanceof IThreeWayDiff) {
						IThreeWayDiff twd = (IThreeWayDiff) diff;
						if (twd.getKind() == IDiff.ADD && twd.getDirection() == IThreeWayDiff.INCOMING) {
							IFileRevision remote = Utils.getRemote(diff);
							IResourceVariant variant = (IResourceVariant)Adapters.adapt(remote, IResourceVariant.class);
							byte[] syncBytes = variant.asBytes();
							MutableResourceSyncInfo info = new MutableResourceSyncInfo(resource.getName(), ResourceSyncInfo.ADDED_REVISION);
							info.setKeywordMode(ResourceSyncInfo.getKeywordMode(syncBytes));
							info.setTag(getTag(resource.getParent()));
							CVSWorkspaceRoot.getCVSFileFor((IFile)resource).setSyncInfo(info, ICVSFile.DIRTY);
						}
					}
				}
			}
		
		}, getMergeRule(diff), IWorkspace.AVOID_UPDATE, monitor);
		return status[0];
	}

	CVSTag getTag(IContainer parent) throws CVSException {
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(parent);
		FolderSyncInfo info = folder.getFolderSyncInfo();
		if (info != null)
			return info.getTag();
		return null;
	}
	
	boolean equals(IThreeWayDiff currentDiff, IThreeWayDiff diffTreeDiff) {
		return currentDiff != null 
		    && currentDiff.getKind() == diffTreeDiff.getKind() 
			&& currentDiff.getDirection() == diffTreeDiff.getDirection();
	}
}
