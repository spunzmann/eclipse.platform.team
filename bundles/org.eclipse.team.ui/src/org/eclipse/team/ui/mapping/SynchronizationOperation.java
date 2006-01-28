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
package org.eclipse.team.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.IDiffNode;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.team.ui.compare.IModelBuffer;
import org.eclipse.team.ui.operations.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This operation class can be used by model providers when performing
 * merge operations triggered from a synchronize participant page
 * associated with a synchronization or merge context.
 * <p>
 * This class may be subclasses by clients
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @see ISynchronizationContext
 * @see IMergeContext
 * 
 * @since 3.2
 */
public abstract class SynchronizationOperation extends TeamOperation {

	private final ISynchronizePageConfiguration configuration;
	private final Object[] elements;

	/*
	 * Helper method for extracting the part safely from a configuration
	 */
	private static IWorkbenchPart getPart(ISynchronizePageConfiguration configuration) {
		if (configuration != null) {
			ISynchronizePageSite site = configuration.getSite();
			if (site != null) {
				return site.getPart();
			}
		}
		return null;
	}
	
	/**
	 * Create a synchronize operation that operations on the given elements
	 * @param configuration the configuration for the page the operation is associated with
	 * @param elements the elements to be operated on
	 */
	protected SynchronizationOperation(ISynchronizePageConfiguration configuration, Object[] elements) {
		super(getPart(configuration));
		this.configuration = configuration;
		this.elements = elements;
	}

	/**
	 * Return the configuration for the page from which this
	 * operation was launched.
	 * @return the configuration for the page from which this
	 * operation was launched
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the synchronization context associated with this action.
	 * @return the synchronization context associated with this action
	 */
	protected ISynchronizationContext getContext() {
		return ((ModelSynchronizeParticipant)getConfiguration().getParticipant()).getContext();
	}
	
	/**
	 * Return the model elements that are the target of this operation.
	 * @return the model elements that are the target of this operation
	 */
	public Object[] getElements() {
		return elements;
	}
	
	/**
	 * Make <code>shouldRun</code> public so the result
	 * can be used to provide handler enablement
	 */
	public boolean shouldRun() {
		return super.shouldRun();
	}

	/**
	 * Return the buffer that this operation will write its results
	 * to or <code>null</code> if the operation does not buffer
	 * its results. By default, <code>null</code> is returned but 
	 * subclasses may override.
	 * @return the buffer that this operation will write its results
	 * to or <code>null</code>
	 */
	public IModelBuffer getTargetBuffer() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			setContextBusy(monitor);
			execute(monitor);
		} finally {
			clearContextBusy(monitor);
		}
	}

	private void clearContextBusy(final IProgressMonitor monitor) {
		final IResourceDiffTree diffTree = getContext().getDiffTree();
		diffTree.clearBusy(monitor);
	}

	private void setContextBusy(final IProgressMonitor monitor) {
		try {
			ResourceTraversal[] traversals = Utils.getTraversals(getElements());
			final IResourceDiffTree diffTree = getContext().getDiffTree();
			IDiffNode[] diffs = diffTree.getDiffs(traversals);
			diffTree.setBusy(diffs, monitor);
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
	}

	/**
	 * Execute the operation. Subclasses should implement the operations behavior in the
	 * execute method. Clients should call either {@link #run()} or {@link #run(IProgressMonitor)}
	 * to invoke the operation.
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	protected abstract void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException;

}