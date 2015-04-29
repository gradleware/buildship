/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Simon Scholz (vogella GmbH) - initial API and implementation and initial documentation
 */

package org.eclipse.buildship.ui.executionview.model;

import java.util.ArrayList;
import java.util.List;

import org.gradle.tooling.events.OperationDescriptor;
import org.gradle.tooling.events.ProgressEvent;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.buildship.ui.executionview.ExecutionView;

/**
 * <p>
 * ProgressItems are the actual elements, which are shown in the {@link ExecutionView}.
 * </p>
 * <p>
 * These object can be obtained by using the global selection provider. By calling
 * {@link #getAdapter(Class)} on this class you can get the associated {@link OperationDescriptor}
 * and the last {@link ProgressEvent}, which was reflected by this ProgressItem.
 * </p>
 * <p>
 *
 * <pre>
 * <code>
 * ISelection selection = HandlerUtil.getCurrentSelection(event);
 * if (selection instanceof IStructuredSelection) {
 *     IStructuredSelection structuredSelection = (IStructuredSelection) selection;
 *     Object firstElement = structuredSelection.getFirstElement();
 *     if (firstElement instanceof IAdaptable) {
 *         IAdaptable adaptable = (IAdaptable) firstElement;
 *         OperationDescriptor adapter = (OperationDescriptor) adaptable.getAdapter(OperationDescriptor.class);
 *         // ... do something with the OperationDescriptor
 *     }
 * }
 * </code>
 * </pre>
 *
 * </p>
 *
 */
public class ExecutionItem extends AbstractModelObject implements IAdaptable {

    public static final String FIELD_LAST_PROGRESSEVENT = "lastProgressEvent";
    public static final String FIELD_LABEL = "label";
    public static final String FIELD_IMAGE = "image";
    public static final String FIELD_DURATION = "duration";
    public static final String FIELD_CHILDREN = "children";

    private final OperationDescriptor operationDescriptor;

    private ProgressEvent lastProgressEvent;

	private String label;

	private ImageDescriptor image;

	private String duration;

	private List<ExecutionItem> children = new ArrayList<ExecutionItem>();

	public ExecutionItem(OperationDescriptor operationDescriptor) {
	    this(operationDescriptor, operationDescriptor == null ? null : operationDescriptor.getDisplayName());
	}

	public ExecutionItem(OperationDescriptor operationDescriptor, String label) {
		this.operationDescriptor = operationDescriptor;
        this.label = label;
	}

	@Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
	    if(getOperationDescriptor() != null && OperationDescriptor.class.equals(adapter)) {
	        return getOperationDescriptor();
	    } else if (getLastProgressEvent() != null && ProgressEvent.class.equals(adapter)) {
            return getLastProgressEvent();
        }

    	return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    public List<ExecutionItem> getChildren() {
		return children;
	}

	public void setChildren(List<ExecutionItem> children) {
        firePropertyChange(FIELD_CHILDREN, this.children, this.children = children);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
        firePropertyChange(FIELD_LABEL, this.label, this.label = label);
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
        firePropertyChange(FIELD_DURATION, this.duration, this.duration = duration);
	}

	public ImageDescriptor getImage() {
        return image;
    }

    public void setImage(ImageDescriptor image) {
        firePropertyChange(FIELD_IMAGE, this.image, this.image = image);
    }

    public OperationDescriptor getOperationDescriptor() {
        return operationDescriptor;
    }

    public ProgressEvent getLastProgressEvent() {
        return lastProgressEvent;
    }

    public void setLastProgressEvent(ProgressEvent lastProgressEvent) {
        firePropertyChange(FIELD_LAST_PROGRESSEVENT, this.lastProgressEvent, this.lastProgressEvent = lastProgressEvent);
    }

}
