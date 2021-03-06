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
package org.eclipse.team.internal.ccvs.core.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;

/**
 * This class acts as a super class for those CVS commands that do not send up the local file structure
 */
public abstract class RemoteCommand extends Command {

	protected ICVSResource[] computeWorkResources(Session session, LocalOption[] localOptions,
		String[] arguments) throws CVSException {
		return new ICVSResource[0];
	}

	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {
			
		// do nothing
		monitor.beginTask(null, 100);
		monitor.done();
		return resources;
	}
	
	protected void sendLocalWorkingDirectory(Session session) throws CVSException {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.Command#convertArgumentsForOpenSession(org.eclipse.team.internal.ccvs.core.ICVSResource[], org.eclipse.team.internal.ccvs.core.client.Session)
	 */
	protected String[] convertArgumentsForOpenSession(
		ICVSResource[] arguments,
		Session openSession)
		throws CVSException {
		
			// Convert arguments
			List stringArguments = new ArrayList(arguments.length);
			for (int i = 0; i < arguments.length; i++) {
			    ICVSResource resource = arguments[i];
			    String remotePath;
                if (isDefinedModule(resource)) {
			        remotePath = resource.getName();
			    } else {
			        remotePath = resource.getRepositoryRelativePath();
                    
			    }
                stringArguments.add(remotePath);
			}
			return (String[]) stringArguments.toArray(new String[stringArguments.size()]);
	}

    private boolean isDefinedModule(ICVSResource resource) {
        return resource instanceof ICVSRemoteFolder && ((ICVSRemoteFolder)resource).isDefinedModule();
    }

}
