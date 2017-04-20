/*
 * Copyright (c) 2017 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.buildship.core.workspace.internal;

import org.gradle.tooling.CancellationToken;

import com.google.common.base.Preconditions;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.configuration.BuildConfiguration;

/**
 * Synchronizes the project configuration.
 *
 * @author Donat Csikos
 */
public final class SynchronizeProjectConfigOperation {

    private final BuildConfiguration buildConfig;

    public SynchronizeProjectConfigOperation(BuildConfiguration buildConfig) {
        this.buildConfig = Preconditions.checkNotNull(buildConfig);
    }

    public void run(IProgressMonitor monitor, CancellationToken token) {
        CorePlugin.configurationManager().saveBuildConfiguration(this.buildConfig);
    }
}
