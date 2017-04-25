/*
 * Copyright (c) 2017 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import com.gradleware.tooling.toolingclient.GradleDistribution;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.configuration.BuildConfiguration;
import org.eclipse.buildship.core.configuration.ConfigurationManager;
import org.eclipse.buildship.core.configuration.GradleProjectNature;
import org.eclipse.buildship.core.configuration.ProjectConfiguration;
import org.eclipse.buildship.core.configuration.RunConfiguration;
import org.eclipse.buildship.core.configuration.WorkspaceConfiguration;
import org.eclipse.buildship.core.launch.GradleRunConfigurationAttributes;
import org.eclipse.buildship.core.util.file.RelativePathUtils;

/**
 * Default implementation for {@link ConfigurationManager}.
 */
public class DefaultConfigurationManager implements ConfigurationManager {

    WorkspaceConfigurationPersistence workspaceConfigurationPersistence = new WorkspaceConfigurationPersistence();
    BuildConfigurationPersistence buildConfigurationPersistence = new BuildConfigurationPersistence();

    @Override
    public WorkspaceConfiguration loadWorkspaceConfiguration() {
        return this.workspaceConfigurationPersistence.readWorkspaceConfig();
    }

    @Override
    public void saveWorkspaceConfiguration(WorkspaceConfiguration config) {
        this.workspaceConfigurationPersistence.saveWorkspaceConfiguration(config);
    }

    @Override
    public BuildConfiguration createBuildConfiguration(File rootProjectDirectory, GradleDistribution gradleDistribution, boolean overrideWorkspaceSettings,
            boolean buildScansEnabled, boolean offlineMode) {
        BuildConfigurationProperties persistentBuildConfigProperties = new BuildConfigurationProperties(rootProjectDirectory,
                                                                                                        gradleDistribution,
                                                                                                        overrideWorkspaceSettings,
                                                                                                        buildScansEnabled,
                                                                                                        offlineMode);
        return new DefaultBuildConfiguration(persistentBuildConfigProperties, loadWorkspaceConfiguration());
    }

    @Override
    public Set<BuildConfiguration> loadAllBuildConfigurations() {
        LinkedHashSet<BuildConfiguration> result = Sets.newLinkedHashSet();
        for (IProject project : CorePlugin.workspaceOperations().getAllProjects()) {
            if(project.isAccessible() && GradleProjectNature.isPresentOn(project)) {
                try {
                    result.add(loadProjectConfiguration(project).getBuildConfiguration());
                } catch(Exception e) {
                    CorePlugin.logger().debug("Can't load build configuration for project " + project.getName(), e);
                }
            }
        }

        return result;
    }

    @Override
    public BuildConfiguration loadBuildConfiguration(File rootDir) {
        Preconditions.checkNotNull(rootDir);
        Preconditions.checkArgument(rootDir.exists());
        Optional<IProject> rootProject = CorePlugin.workspaceOperations().findProjectByLocation(rootDir);
        BuildConfigurationProperties buildConfigProperties;
        if (rootProject.isPresent() && rootProject.get().isAccessible()) {
            buildConfigProperties = this.buildConfigurationPersistence.readBuildConfiguratonProperties(rootProject.get());
        } else {
            buildConfigProperties = this.buildConfigurationPersistence.readBuildConfiguratonProperties(rootDir);
        }
        return new DefaultBuildConfiguration(buildConfigProperties, loadWorkspaceConfiguration());
    }

    @Override
    public void saveBuildConfiguration(BuildConfiguration configuration) {
        Preconditions.checkArgument(configuration instanceof DefaultBuildConfiguration, "Unknow configuration type: ", configuration.getClass());
        BuildConfigurationProperties properties = ((DefaultBuildConfiguration)configuration).getProperties();
        File rootDir = configuration.getRootProjectDirectory();
        Optional<IProject> rootProject = CorePlugin.workspaceOperations().findProjectByLocation(rootDir);
        if (rootProject.isPresent() && rootProject.get().isAccessible()) {
            this.buildConfigurationPersistence.saveBuildConfiguration(rootProject.get(), properties);
        } else {
            this.buildConfigurationPersistence.saveBuildConfiguration(rootDir, properties);
        }
    }

    @Override
    public ProjectConfiguration createProjectConfiguration(BuildConfiguration configuration, File projectDir) {
        return new DefaultProjectConfiguration(projectDir, configuration);
    }

    @Override
    public ProjectConfiguration loadProjectConfiguration(IProject project) {
        String pathToRoot = this.buildConfigurationPersistence.readPathToRoot(project.getLocation().toFile());
        File rootDir = relativePathToProjectRoot(project, pathToRoot);
        BuildConfiguration buildConfig = loadBuildConfiguration(rootDir);
        return new DefaultProjectConfiguration(project.getLocation().toFile(), buildConfig);
    }

    @Override
    public void saveProjectConfiguration(ProjectConfiguration projectConfiguration) {
        BuildConfiguration buildConfiguration = projectConfiguration.getBuildConfiguration();
        File projectDir = projectConfiguration.getProjectDir();
        File rootDir = buildConfiguration.getRootProjectDirectory();
        String pathToRoot = projectRootToRelativePath(projectDir, rootDir);

        Optional<IProject> project = CorePlugin.workspaceOperations().findProjectByLocation(projectDir);
        if (project.isPresent() && project.get().isAccessible()) {
            this.buildConfigurationPersistence.savePathToRoot(project.get(), pathToRoot);
        } else {
            this.buildConfigurationPersistence.savePathToRoot(projectDir, pathToRoot);
        }
        saveBuildConfiguration(buildConfiguration);
    }

    @Override
    public void deleteProjectConfiguration(IProject project) {
        if (project.isAccessible()) {
            this.buildConfigurationPersistence.deleteProjectConfiguration(project);
        } else {
            this.buildConfigurationPersistence.deleteProjectConfiguration(project.getLocation().toFile());
        }
    }

    @Override
    public RunConfiguration loadRunConfiguration(ILaunchConfiguration launchConfiguration) {
        GradleRunConfigurationAttributes attributes = GradleRunConfigurationAttributes.from(launchConfiguration);
        BuildConfigurationProperties buildConfig = new BuildConfigurationProperties(attributes.getWorkingDir(),
                attributes.getGradleDistribution(),
                attributes.isOverrideWorkspaceSettings(),
                attributes.isBuildScansEnabled(),
                attributes.isOffline());
        RunConfigurationProperties runConfig = new RunConfigurationProperties(attributes.getTasks(),
                  attributes.getJavaHome(),
                  attributes.getJvmArguments(),
                  attributes.getArgumentExpressions(),
                  attributes.isShowConsoleView(),
                  attributes.isShowExecutionView());
        return new DefaultRunConfiguration(loadWorkspaceConfiguration(), buildConfig, runConfig);
    }

    private static File relativePathToProjectRoot(IProject project, String path) {
        IPath pathToRoot = new Path(path);
        return canonicalize(RelativePathUtils.getAbsolutePath(project.getLocation(), pathToRoot).toFile());
    }

    private static File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String projectRootToRelativePath(File projectDir, File rootDir) {
        IPath rootProjectPath = new Path(rootDir.getPath());
        IPath projectPath = new Path(projectDir.getPath());
        return RelativePathUtils.getRelativePath(projectPath, rootProjectPath).toPortableString();
    }
}
