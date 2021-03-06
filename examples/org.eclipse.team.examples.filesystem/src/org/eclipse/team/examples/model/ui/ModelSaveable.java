/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.ui;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.ui.Saveable;

/**
 * A Saveable that represents a modified model object definition file.
 */
public class ModelSaveable extends Saveable {

	private ModelObject modelObject;
	private boolean dirty;
	private final ModelSaveablesProvider modelSaveablesProvider;

	public ModelSaveable(ModelSaveablesProvider modelSaveablesProvider, ModelObjectDefinitionFile mo) {
		this.modelSaveablesProvider = modelSaveablesProvider;
		modelObject = mo;
	}

	public boolean equals(Object object) {
		if (object instanceof ModelSaveable) {
			ModelSaveable other = (ModelSaveable) object;
			return (other.getModelObject().equals(getModelObject()));
		}
		return false;
	}

	public ModelObject getModelObject() {
		return modelObject;
	}

	public ImageDescriptor getImageDescriptor() {
		return ModelWorkbenchAdapter.createImageDescriptor("obj/mod_obj.gif");
	}

	public String getName() {
		return modelObject.getName();
	}

	public String getToolTipText() {
		return "Saveable for " + getName();
	}

	public int hashCode() {
		return modelObject.hashCode();
	}

	public boolean isDirty() {
		return dirty;
	}
	
	public void doSave(IProgressMonitor monitor) {
		dirty = false;
		modelSaveablesProvider.saved(this);
	}

	public void makeDirty() {
		dirty = true;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == ResourceMapping.class) {
			return Adapters.adapt(getModelObject(), ResourceMapping.class);
		}
		return super.getAdapter(adapter);
	}
}
