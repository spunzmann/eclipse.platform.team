/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * A wizard for changing the keyword substitution mode of files.
 * 
 * 1.  Ask the user select to select the desired keyword substitution mode.
 * 2.  Compute the set of possibly affected resources
 * 3.  If the affected resources include existing committed files, warn the user
 *     and provide an option to include them in the operation anyways.
 * 4.  If the affected resources include dirty files, warn the user and provide
 *     an option to include them in the operation anyways.
 * 5.  Perform the operation on Finish.
 */
public class ModeWizard extends ResizableWizard {
    
    public static class ModeChange {
        
        private final IFile fFile;
        private final KSubstOption fMode;
        
        private KSubstOption fNewMode;
        
        public ModeChange(IFile file, KSubstOption mode) {
            fFile = file;
            fMode= mode;
            fNewMode= mode;
        }
        
        public IFile getFile() {
            return fFile;
        }
        
        public KSubstOption getMode() {
            return fMode;
        }
        
        public KSubstOption getNewMode() {
            return fNewMode;
        }
        
        public boolean hasChanged() {
            return !fMode.equals(fNewMode);
        }
        
        public void setNewMode(KSubstOption mode) {
            fNewMode= mode;
        }
        
        public int compareTo(Object o) {
            return fFile.getName().compareTo(((ModeChange)o).getFile().getName());
        }
    }
    
    protected List fChanges;
    final ModeWizardSelectionPage fPage;
    
    public static ModeWizard run(final Shell shell, final IResource [] resources) {
        
        final ModeWizard [] wizard= new ModeWizard[1];

        BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
            public void run() {
                wizard[0]= new ModeWizard(shell, resources);
            }
        });
        
        open(shell, wizard[0]);
        return wizard[0];
    }
    
    /**
     * Creates a wizard to set the keyword substitution mode for the specified resources.
     * 
     * @param resources the resources to alter
     * @param depth the recursion depth
     * @param defaultOption the keyword substitution option to select by default
     */
    
    protected ModeWizard(Shell shell, final IResource[] resources) {
        super(CVSUIMessages.ModeWizard_0, CVSUIPlugin.getPlugin().getDialogSettings(), 700, 480); 
        setWindowTitle(CVSUIMessages.ModeWizard_1); 
        
        fChanges= getModeChanges(shell, resources);
        fPage= new ModeWizardSelectionPage(fChanges);
//            Workbench.getInstance().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                    
    }
    
    public void addPages() {
        addPage(fPage);
    }
    
    /* (Non-javadoc)
     * Method declared on IWizard.
     */
    public boolean needsProgressMonitor() {
        return true;
    }
    
    protected static List getModeChanges(Shell shell, IResource [] resources) {
        
        final ArrayList changes= new ArrayList();
        final HashSet visited= new HashSet();
        
        for (int i = 0; i < resources.length; i++) {
            final IResource currentResource = resources[i];
            try {
                currentResource.accept(new IResourceVisitor() {
                    public boolean visit(IResource resource) throws CoreException {
                        try {
                            if (visited.contains(resource) || resource.getType() != IResource.FILE || !resource.exists()) 
                                return true;
                            visited.add(resource);
                            IFile file = (IFile) resource;
                            ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
                            if (!cvsFile.isManaged()) 
                                return true;
                            final ResourceSyncInfo info = cvsFile.getSyncInfo();
                            final KSubstOption mode = info.getKeywordMode();
                            
                            changes.add(new ModeChange(file, mode));
                            
                        } catch (TeamException e) {
                            throw new CoreException(e.getStatus());
                        }
                        // always return true and let the depth determine if children are visited
                        return true;
                    }
                }, IResource.DEPTH_INFINITE, false); 
            } catch (CoreException e) {
                CVSUIPlugin.openError(shell, CVSUIMessages.ModeWizard_2, null, e); 
            }
        }
        return changes;
    }
    
    public boolean performFinish() {
        try {
            final List messages = new ArrayList();
            final List changes= fPage.getChanges();
            if (changes.size() == 0)
                return true;
            
            final String comment = fPage.getComment(getShell());
            if (comment == null)
                return false;
            
            getContainer().run(false /*fork*/, true /*cancelable*/, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        final int totalWork= 10000;
                        monitor.beginTask(CVSUIMessages.ModeWizard_3, totalWork); 
                        
                        final Map changesPerProvider= getProviderMapping(changes);
                        
                        final int initialWork= totalWork / 10;
                        monitor.worked(initialWork);
                        
                        final int workPerProvider = (totalWork - initialWork) / changesPerProvider.size();

                        for (Iterator iter = changesPerProvider.entrySet().iterator(); iter.hasNext();) {
                            final Map.Entry entry = (Map.Entry) iter.next();
                            final CVSTeamProvider provider = (CVSTeamProvider)entry.getKey();
                            final Map providerFiles = (Map) entry.getValue();

                            final IStatus status = provider.setKeywordSubstitution(providerFiles, comment, Policy.subMonitorFor(monitor, workPerProvider));
                            if (status.getCode() != IStatus.OK) {
                                messages.add(status);
                            }
                        }
                        // Broadcast a decorator change so all interested parties will update their labels.
                        // This is done in particular because the syncview will not see this change
                        // as a change in state for the resources involved
                        CVSUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, CVSUIPlugin.P_DECORATORS_CHANGED, null, null));
                    } catch (TeamException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
            // Check for any status messages and display them
            if (!messages.isEmpty()) {
                boolean error = false;
                final MultiStatus combinedStatus = new MultiStatus(CVSUIPlugin.ID, 0, CVSUIMessages.ModeWizard_4, null); 
                for (int i = 0; i < messages.size(); i++) {
                    final IStatus status = (IStatus)messages.get(i);
                    if (status.getSeverity() == IStatus.ERROR || status.getCode() == CVSStatus.SERVER_ERROR) {
                        error = true;
                    }
                    combinedStatus.merge(status);
                }
                String message = null;
                IStatus statusToDisplay;
                if (combinedStatus.getChildren().length == 1) {
                    message = combinedStatus.getMessage();
                    statusToDisplay = combinedStatus.getChildren()[0];
                } else {
                    statusToDisplay = combinedStatus;
                }
                final String title= error ? CVSUIMessages.ModeWizard_5 : CVSUIMessages.ModeWizard_6; // 
                CVSUIPlugin.openError(getShell(), title, message, new CVSException(statusToDisplay));
            }
            return super.performFinish();
        } catch (InterruptedException e) {
            return true;
        } catch (InvocationTargetException e) {
            CVSUIPlugin.openError(getShell(), CVSUIMessages.ModeWizard_4, null, e); 
            return false;
        }
    }
    
    /**
     * Get a map 
     * @param changes
     * @return
     */
    static Map getProviderMapping(Collection changes) {
        
        final Map table = new HashMap();
        
        for (Iterator iter = changes.iterator(); iter.hasNext();) {
            final ModeChange change= (ModeChange)iter.next();
            
            if (!change.hasChanged())
                continue;
            
            final IFile file = change.getFile();
            final RepositoryProvider provider = RepositoryProvider.getProvider(file.getProject(), CVSProviderPlugin.getTypeId());
            
            if (!table.containsKey(provider)) {
                table.put(provider, new HashMap());
            }
            final Map providerMap = (Map)table.get(provider);
            providerMap.put(file, change.getNewMode());
        }
        return table;
    }


}
