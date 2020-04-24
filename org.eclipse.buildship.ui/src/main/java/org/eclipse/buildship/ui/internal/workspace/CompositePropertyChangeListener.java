/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Kuzniarz (Diebold Nixdorf Inc.) - initial implemenation
 */

package org.eclipse.buildship.ui.internal.workspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.buildship.core.internal.CorePlugin;
import org.eclipse.buildship.ui.internal.wizard.workspacecomposite.IGradleCompositeIDs;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * Listener class for {@link org.eclipse.ui.internal.dialogs.WorkingSetSelectionDialog}. Contains file composite properties file 
 * deletion algorithm with backup to ensure cancel function for WorkingSetSelectionDialog.
 * @author kuzniarz
 */
public class CompositePropertyChangeListener implements IPropertyChangeListener {
	
	private final Map<String, Properties> compositePropertiesBackup = new HashMap<String, Properties>();
	IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		  if (compositeRemovalCausedEvent(event)) {
		        try {
		        	IWorkingSet removed = (IWorkingSet)event.getOldValue();
		        	if (isNotAggregate(removed) && removed.getId().equals(IGradleCompositeIDs.NATURE)) {
				        File compositePropertiesFile = CorePlugin.getInstance().getStateLocation().append("workspace-composites").append(removed.getName()).toFile();
				        backupCompositeProperties(removed, compositePropertiesFile);
						Files.deleteIfExists(compositePropertiesFile.toPath());
		        	}
				} catch (IOException e) {
					e.printStackTrace();
				}
		  } else if (compositeAddingCausedEvent(event)) {
			  try {
				  IWorkingSet added = (IWorkingSet)event.getNewValue();
				  if (isNotAggregate(added) && added.getId().equals(IGradleCompositeIDs.NATURE)) {
					  restoreCompositeProperties(added.getName());
				  }
			  } catch (IOException e) {
				  e.printStackTrace();
			  }
		  }
	}

	private boolean isNotAggregate(IWorkingSet removed) {
		return !removed.getName().contains(":");
	}

	private void restoreCompositeProperties(String compositeName) throws FileNotFoundException, IOException {
		if (compositePropertiesBackup.containsKey(compositeName)) {
		        File compositePropertiesFile = CorePlugin.getInstance().getStateLocation().append("workspace-composites").append(compositeName).toFile();
				FileOutputStream out = new FileOutputStream(compositePropertiesFile.getAbsoluteFile());
				compositePropertiesBackup.get(compositeName).store(out, " ");
				compositePropertiesBackup.remove(compositeName);
				out.close();
		  } else {
			  //New Composite is being created!
		  }
	}

	private void backupCompositeProperties(IWorkingSet removed, File compositePropertiesFile)
			throws FileNotFoundException, IOException {
		Properties compositeProperties = new Properties();
		FileInputStream input = new FileInputStream(compositePropertiesFile);
		compositeProperties.load(input);
		input.close();
		compositePropertiesBackup.put(removed.getName(), compositeProperties);
	}

	private boolean compositeAddingCausedEvent(PropertyChangeEvent event) {
		return 	event.getProperty().equals(IWorkingSetManager.CHANGE_WORKING_SET_ADD) && 
				event.getNewValue() != null;
	}

	private boolean compositeRemovalCausedEvent(PropertyChangeEvent event) {
		return 	event.getProperty().equals(IWorkingSetManager.CHANGE_WORKING_SET_REMOVE) && 
				event.getOldValue() != null;
	}

}
