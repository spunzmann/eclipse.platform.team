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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.IRepositoryProviderListener;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IDecoratorManager;

/**
 * A state change tester is used by logical models to communicate the
 * synchronization state of their logical model elements to 
 * the lightweight label decorators of team providers. 
 * <p>
 * There are two different types of elements being decorated: those
 * that have a one-to-one mapping to a resource and those that do not.
 * Those that do should adapt to their corresponding resource. Doing
 * so will ensure that label updates occur when the state of that
 * resource changes (i.e. the team provider will generate label updates
 * for those resources and the modle can translate them to appropriate
 * label updates of their model elements).
 * <p>
 * For those elements that do not have a one-to-one mapping to resources,
 * the model must do extra work. The purpose of this class is to allow
 * the model to decide when a label update for a logical model element is
 * required and to communicate the dirty state of their logical model
 * elements to the team decorator. For logical model elements, the team decorator
 * will only decorate based on the supervised state and the dirty state.
 * This class provides methods for determining both of these so that 
 * logical models can track whether a label update is required for a
 * model element. 
 * <p>
 * Model providers need to re-evaluate the state of a 
 * model element whenever a change in the resources occurs by listening
 * to both resource deltas and subscriber change events.
 * <p>
 * Decoration enablement changes and decoration configuration changes
 * are handled by the {@link IDecoratorManager#update(String)} API.
 * A call to this method will result in label changes to all elements.
 * The {@link #isDecorationEnabled(Object)} API on this class can 
 * be used to determine if an element will receive team decorations.
 * If decoration is disabled. team state changes on the element can
 * be ignored.
 *
 * @since 3.2
 * @see IWorkspace#addResourceChangeListener(IResourceChangeListener)
 * @see Subscriber#addListener(org.eclipse.team.core.subscribers.ISubscriberChangeListener)
 */
public class SynchronizationStateTester extends DecoratedStateProvider {

	/**
	 * Constant that is used as the property key on an {@link IDecorationContext}.
	 * If a context passed to a team decorator has this property, the associated
	 * state tester will be used by the deocator to determine whether elements
	 * have an outgoing change.
	 */
	public static final String PROP_TESTER = "org.eclipse.team.ui.syncStateTester"; //$NON-NLS-1$
	
	/**
	 * Create a synchronization state tester.
	 */
	public SynchronizationStateTester() {
		super();
	}

	/**
	 * Return whether decoration is enabled for the given
	 * model element. If decoration is not enabled, the model
	 * does not need to fire label change events when the team state
	 * of the element changes.
	 * @param element the model element
	 * @return whether decoration is enabled for the given
	 * model element
	 */
	public final boolean isDecorationEnabled(Object element) {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			provider.isDecorationEnabled(element);
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#isDecorated(java.lang.Object)
	 */
	public boolean isDecorated(Object element) throws CoreException {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			provider.isDecorated(element);
		return false;
	}
	
	/**
	 * Return the mask that indicates what state the appropriate team decorator
	 * is capable of decorating. The state is determined by querying the
	 * <code>org.eclipse.team.ui.teamDecorators</code> extension point.
	 * 
	 * <p>
	 * The state mask can consist of the following flags:
	 * <ul>
	 * <li>The diff kinds of {@link IDiff#ADD}, {@link IDiff#REMOVE}
	 * and {@link IDiff#CHANGE}.
	 * <li>The directions {@link IThreeWayDiff#INCOMING} and
	 * {@link IThreeWayDiff#OUTGOING}.
	 * </ul>
	 * For convenience sake, if there are no kind flags but there is at least
	 * one direction flag then all kinds are assumed.
	 * 
	 * @param element
	 *            the model element to be decorated
	 * @return the mask that indicates what state the appropriate team decorator
	 *         will decorate
	 * @see IDiff
	 * @see IThreeWayDiff
	 */
	public final int getDecoratedStateMask(Object element) {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getDecoratedStateMask(element);
		return 0;
	}

	/**
	 * Return the synchronization state of the given element. Only the portion
	 * of the synchronization state covered by <code>stateMask</code> is
	 * returned. By default, this method calls
	 * {@link Subscriber#getState(ResourceMapping, int, IProgressMonitor)}.
	 * <p>
	 * Team decorators will use this method to detemine how to decorate the
	 * provided element. The {@link #getDecoratedStateMask(Object)} returns the
	 * state that the corresponding team decorator is capable of decorating but
	 * the decorator may be configured to decorate only a portion of that state.
	 * When the team decorator invokes this method, it will pass the stateMask that
	 * it is currently configured to show. If a mask of zero is provided, this indicates
	 * that the team decorator is not configured to decorate the synchronization state
	 * of model elements.
	 * <p>
	 * Subclasses may want to override this method in the following cases:
	 * <ol>
	 * <li>The subclass wishes to fire appropriate label change events when the
	 * decorated state of a model element changes. In this case the subclass
	 * can override this method to record the stateMask and returned state. It can
	 * use this recorded information to determine whether local changes or subscriber changes
	 * result in a change in the deocrated sstate of the model element.
	 * <li>The subclasses wishes to provide a more accurate change description for a model
	 * element that represents only a portion of the file. In this case, the subclass can
	 * use the remote file contents available from the subscriber to determine whether
	 * </ol>
	 * 
	 * @param element the model element
	 * @param stateMask the mask that identifies which state flags are desired if
	 *            present
	 * @param monitor a progress monitor
	 * @return the synchronization state of the given element
	 * @throws CoreException
	 * @see Subscriber#getState(ResourceMapping, int, IProgressMonitor)
	 */
	public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getState(element, stateMask, monitor);
		return 0;
	}

	/**
	 * Return whether state decoration is enabled for the context
	 * to which this tester is associated. If <code>true</code>
	 * is returned, team decorators will use the state methods provided
	 * on this class to calculate the synchronization state of model
	 * elements for the purpose of decoration. If <code>false</code>
	 * is returned, team decorators will not decorate the elements with any
	 * synchronization related decorations. Subclasses will want to disable
	 * state decoration if state decoration is being provided another way
	 * (e.g. by a {@link SynchronizationLabelProvider}). By default, 
	 * <code>true</code>is returned. Subclasses may override.
	 * @return whether state decoration is enabled
	 */
	public boolean isStateDecorationEnabled() {
		return true;
	}
	

}
