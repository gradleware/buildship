/*
 * Copyright (c) 2019 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core.internal.workspace;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.gradle.tooling.model.eclipse.EclipseWorkspace;
import org.gradle.tooling.model.eclipse.EclipseWorkspaceProject;

class DefaultEclipseWorkspace implements EclipseWorkspace, Serializable {

    private static final long serialVersionUID = 1L;
    private final File location;
    private final List<EclipseWorkspaceProject> projects;

    public DefaultEclipseWorkspace(File location, List<EclipseWorkspaceProject> projects) {
        super();
        this.location = location;
        this.projects = projects;
    }

    @Override
    public File getLocation() {
        return this.location;
    }

    @Override
    public List<EclipseWorkspaceProject> getProjects() {
        return this.projects;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.projects);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultEclipseWorkspace other = (DefaultEclipseWorkspace) obj;
        return Objects.equals(this.location, other.location) && Objects.equals(this.projects, other.projects);
    }

}