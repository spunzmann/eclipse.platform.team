/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.team.core.diff.DiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.core.subscribers.ContentComparisonDiffFilter;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.synchronize.RegexDiffFilter;

public class CompareSubscriberContext extends CVSSubscriberMergeContext implements IPreferenceChangeListener {

	public static SynchronizationContext createContext(ISynchronizationScopeManager manager, CVSCompareSubscriber subscriber) {
		CompareSubscriberContext mergeContext = new CompareSubscriberContext(subscriber, manager);
		mergeContext.initialize();
		return mergeContext;
	}
	
	protected CompareSubscriberContext(Subscriber subscriber, ISynchronizationScopeManager manager) {
		super(subscriber, manager);
		((IEclipsePreferences) CVSUIPlugin.getPlugin().getInstancePreferences().node("")).addPreferenceChangeListener(this); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SubscriberMergeContext#dispose()
	 */
	public void dispose() {
		super.dispose();
		((IEclipsePreferences) CVSUIPlugin.getPlugin().getInstancePreferences().node("")).removePreferenceChangeListener(this); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IMergeContext#markAsMerged(org.eclipse.team.core.diff.IDiff, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		// Do nothing 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SubscriberMergeContext#getDiffFilter()
	 */
	protected DiffFilter getDiffFilter() {
		final DiffFilter contentFilter = createContentFilter();
		final DiffFilter regexFilter = createRegexFilter();
		if (contentFilter != null && regexFilter != null) {
			return new DiffFilter() {
				public boolean select(IDiff diff, IProgressMonitor monitor) {
					return !contentFilter.select(diff, monitor)
							&& !regexFilter.select(diff, monitor);
				}
			};
		} else if (contentFilter != null) {
			return new DiffFilter() {
				public boolean select(IDiff diff, IProgressMonitor monitor) {
					return !contentFilter.select(diff, monitor);
				}
			};
		} else if (regexFilter != null) {
			return new DiffFilter() {
				public boolean select(IDiff diff, IProgressMonitor monitor) {
					return !regexFilter.select(diff, monitor);
				}
			};
		}
		return null;
	}

	private boolean isConsiderContents() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_CONSIDER_CONTENTS);
	}

	private DiffFilter createContentFilter() {
		if (isConsiderContents()) {
			// Return a filter that selects any diffs whose contents are not equal
			return new ContentComparisonDiffFilter(false);
		}
		return null;
	}

	private DiffFilter createRegexFilter() {
		if (isConsiderContents()) {
			String pattern = CVSUIPlugin.getPlugin().getPreferenceStore().getString(
					ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN);
			if (pattern != null && !pattern.equals("")) { //$NON-NLS-1$
				return new RegexDiffFilter(pattern);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
	 */
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(ICVSUIConstants.PREF_CONSIDER_CONTENTS) || event.getKey().equals(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN)) {
			SubscriberDiffTreeEventHandler handler = getHandler();
			if (handler != null) {
				handler.setFilter(getDiffFilter());
				handler.reset();
			}
		}
	}
}
