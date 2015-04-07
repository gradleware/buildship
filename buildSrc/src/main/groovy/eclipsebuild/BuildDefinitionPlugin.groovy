/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 */

package eclipsebuild

import eclipsebuild.mavenize.BundleMavenDeployer
import eclipsebuild.builddefinition.AssembleTargetPlatformConvention
import eclipsebuild.builddefinition.AssembleTargetPlatformTask
import eclipsebuild.mavenize.BundleMavenDeployer
import eclipsebuild.util.file.FileSemaphore
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

/**
 * Gradle plugin for the root project of the Eclipse plugin build.
 * <p/>
 * Applying this plugin offers a DSL to specify Eclipse target platforms which will be the base
 * of the compilation of the sub-projects applying applying the following plug-ins:
 * {@link BundlePlugin}, {@link TestBundlePlugin}, {@link FeaturePlugin}, {@link UpdateSitePlugin}.
 * <p/>
 * A target platform consists of a set of Eclipse update sites and a subset of their contained
 * features. Upon each build the plug-in ensures if the specified features are downloaded and
 * converted to a Maven repository.
 * <p/>
 * A valid target platform definition DSL looks like this:
 * <pre>
 * eclipseBuild {
 *     defaultEclipseVersion = '44'
 *
 *     targetPlatform {
 *         eclipseVersion = '44'
 *         sdkVersion = "4.4.2.M20150204-1700"
 *         updateSites = [
 *            "http://download.eclipse.org/release/luna",
 *            "http://download.eclipse.org/technology/swtbot/releases/latest/"
 *         ]
 *         features = [
 *             "org.eclipse.swtbot.eclipse.feature.group",
 *             "org.eclipse.swtbot.ide.feature.group"
 *         ]
 *         versionMapping = [
 *             'org.eclipse.core.runtime' : '3.10.0.v20140318-2214'
 *         ]
 *     }
 *
 *     targetPlatform {
 *        eclipseVersion = '43'
 *        ...
 *     }
 * }
 * </pre>
 * The result is a target platform containing the Eclipse 4.4.2 SDK and the latest SWTBot. The
 * sub-projects can reference the plugins simply by defining a dependency to the required bundle
 * just like with with other dependency management tools:
 * {@code compile "eclipse:org.eclipse.swtbot.eclipse.finder:+"}. <b>Note</b> that the Eclipse SDK
 * feature is always included in the target platform.
 * <p/>
 * If no target platform version is defined for the build then the one matches to the value of the
 * {@link defaultEclipseVersion} attribute will be selected. This can be changed by appending the
 * the {@code -Peclipse.version=[version-number]} argument to he build. In the context of the
 * example above it would be:
 * <pre>
 * gradle clean build -Peclipse.version=43
 * </pre>
 * The directory layout where the target platform and it's mavenized counterpart stored is defined
 * in the {@link Config} class. The directory containing the target platforms can be redefined with
 * the {@code -PtargetPlatformsDir=<path>} argument.
 * <p/>
 * The {@code versionMapping} can be used to define exact plugin dependency versions per target platform.
 * A bundle can define a dependency through the {@code withEclipseBundle()} method like
 * <pre>
 * compile withEclipseBundle('org.eclipse.core.runtime')
 * </pre>
 * If the active target platform has a version mapped for the dependency then that version is used,
 * otherwise an unbound version range (+) is applied.
 */
class BuildDefinitionPlugin implements Plugin<Project> {

    /**
     *  Extension class providing top-level content of the DSL definition for the plug-in.
     */
    static class EclipseBuild {

        def defaultEclipseVersion
        final def targetPlatforms

        EclipseBuild() {
            targetPlatforms = [:]
        }

        def targetPlatform(Closure closure) {
            def tp = new TargetPlatform()
            tp.apply(closure)
            targetPlatforms[tp.eclipseVersion] = tp
        }
    }

    /**
     * POJO class describing one target platform. Instances are stored in the {@link EclipseBuild#targetPlatforms} map.
     */
    static class TargetPlatform {

        def eclipseVersion
        def sdkVersion
        def updateSites
        def features
        def versionMapping

        TargetPlatform() {
            this.updateSites = []
            this.features = []
            this.versionMapping = [:]
        }

        def apply (Closure closure) {
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.delegate = this
            closure.call()
            // convert GStrings to Strings in the versionMapping key to avoid lookup misses
            versionMapping = versionMapping.collectEntries { k, v -> [k.toString(), v]}
        }
    }

    // name of the root node in the DSL
    static String DSL_EXTENSION_NAME = "eclipseBuild"

    // task names
    static final String TASK_NAME_DOWNLOAD_ECLIPSE_SDK = "downloadEclipseSdk"
    static final String TASK_NAME_ASSEMBLE_TARGET_PLATFORM = "assembleTargetPlatform"
    static final String TASK_NAME_INSTALL_TARGET_PLATFORM = "installTargetPlatform"
    static final String TASK_NAME_UNINSTALL_TARGET_PLATFORM = "uninstallTargetPlatform"
    static final String TASK_NAME_UNINSTALL_ALL_TARGET_PLATFORMS = "uninstallAllTargetPlatforms"

    @Override
    public void apply(Project project) {
        configureProject(project)

        Config config = Config.on(project)
        validateDslBeforeBuildStarts(project, config)
        addTaskDownloadEclipseSdk(project, config)
        addTaskAssembleTargetPlatform(project, config)
        addTaskInstallTargetPlatform(project, config)
        addTaskUninstallTargetPlatform(project, config)
        addTaskUninstallAllTargetPlatforms(project, config)

        defineConventionMapping(project)
    }

    static void configureProject(Project project) {
        // add extension
        project.extensions.create(DSL_EXTENSION_NAME, EclipseBuild)

        // expose some constants to the build files, e.g. for platform-dependent dependencies
        Constants.exposePublicConstantsFor(project)

        // make the withEclipseBundle(String) method available in the build script
        project.ext.withEclipseBundle = { String pluginName -> calculatePluginDependencyVersion(project, pluginName) }
    }

    static def calculatePluginDependencyVersion(Project project, String pluginName) {
        // if the target platform defines a version in the versionMapping
        // for the argument it returns eclipse:pluginName:versionNumber
        // otherwise it returns eclipse:pluginName:+
        Config config = Config.on(project)
        def mappedVersion = config.targetPlatform.versionMapping[pluginName]
        def version = mappedVersion == null ? "+" : mappedVersion
        project.logger.debug("Plugin $pluginName mapped to version $version")
        "${Constants.mavenizedEclipsePluginGroupName}:${pluginName}:${version}"
    }

    static void validateDslBeforeBuildStarts(Project project, Config config) {
        // check if the build definition is valid just before the build starts
        project.gradle.taskGraph.whenReady {
            if (project.eclipseBuild.defaultEclipseVersion == null) {
                throw new RuntimeException("$DSL_EXTENSION_NAME must specify 'defaultEclipseVersion'")
            }
            else if (project.eclipseBuild.targetPlatforms[config.eclipseVersion] == null) {
                throw new RuntimeException("Target platform is not defined for selected Eclipse version '${config.eclipseVersion}'")
            }
        }
    }

    static addTaskDownloadEclipseSdk(Project project, Config config) {
        project.task(TASK_NAME_DOWNLOAD_ECLIPSE_SDK) {
            group = Constants.gradleTaskGroupName
            description = "Downloads an Eclipse SDK to perform P2 operations with."
            outputs.file config.eclipseSdkArchive
            doLast { downloadEclipseSdk(project, config) }
        }
    }

    static downloadEclipseSdk(Project project, Config config) {
        // if multiple builds start on the same machine (which is the case with a CI server)
        // we want to prevent them downloading the same file to the same destination
        def directoryLock = new FileSemaphore(config.eclipseSdkDir)
        try {
            directoryLock.lock()
            downloadEclipseSdkUnprotected(project, config)
        } finally {
            directoryLock.unlock()
        }
    }

    static downloadEclipseSdkUnprotected(Project project, Config config) {
        // download the archive
        File sdkArchive = config.eclipseSdkArchive
        project.logger.info("Download Eclipse SDK from '${Constants.eclipseSdkDownloadUrl}' to '${sdkArchive.absolutePath}'")
        project.ant.get(src: Constants.eclipseSdkDownloadUrl, dest: sdkArchive)

        // extract it to the same location where it was extracted
        project.logger.info("Extract '$sdkArchive' to '$sdkArchive.parentFile.absolutePath'")
        if (OperatingSystem.current().isWindows()) {
            project.ant.unzip(src: sdkArchive, dest: sdkArchive.parentFile, overwrite: true)
        } else {
            project.ant.untar(src: sdkArchive, dest: sdkArchive.parentFile, compression: "gzip", overwrite: true)
        }

        // make it executable
        project.logger.info("Set '${config.eclipseSdkExe}' executable")
        config.eclipseSdkExe.setExecutable(true)
    }

    static addTaskAssembleTargetPlatform(Project project, Config config) {
        project.task(TASK_NAME_ASSEMBLE_TARGET_PLATFORM, dependsOn: [
            TASK_NAME_DOWNLOAD_ECLIPSE_SDK
        ], type: AssembleTargetPlatformTask) {
            group = Constants.gradleTaskGroupName
            description = "Assembles an Eclipse distribution based on the target platform definition."
        }
    }

    static addTaskInstallTargetPlatform(Project project, Config config) {
        project.task(TASK_NAME_INSTALL_TARGET_PLATFORM, dependsOn: TASK_NAME_ASSEMBLE_TARGET_PLATFORM) {
            group = Constants.gradleTaskGroupName
            description = "Converts the assembled Eclipse distribution to a Maven repoository."
            project.afterEvaluate { inputs.dir config.nonMavenizedTargetPlatformDir }
            project.afterEvaluate { outputs.dir config.mavenizedTargetPlatformDir }
            doLast { installTargetPlatform(project, config) }
        }
    }

    static installTargetPlatform(Project project, Config config) {
        // delete the mavenized target platform directory to ensure that the deployment doesn't
        // have outdated artifacts
        if (config.mavenizedTargetPlatformDir.exists()) {
            project.logger.info("Delete mavenized platform directory '${config.mavenizedTargetPlatformDir}'")
            config.mavenizedTargetPlatformDir.deleteDir()
        }

        // install bundles
        project.logger.info("Convert Eclipse target platform '${config.nonMavenizedTargetPlatformDir}' to Maven repository '${config.mavenizedTargetPlatformDir}'")
        def deployer = new BundleMavenDeployer(project.ant, Constants.mavenizedEclipsePluginGroupName, project.logger)
        deployer.deploy(config.nonMavenizedTargetPlatformDir, config.mavenizedTargetPlatformDir)
    }

    static addTaskUninstallTargetPlatform(Project project, Config config) {
        project.task(TASK_NAME_UNINSTALL_TARGET_PLATFORM) {
            group = Constants.gradleTaskGroupName
            description = "Deletes the target platform."
            doLast { deleteFolder(project, config.targetPlatformDir) }
        }
    }

    static deleteFolder(Project project, File folder) {
        if (!folder.exists()) {
            project.logger.info("'$folder' doesn't exist")
        }
        else {
            project.logger.info("Delete '$folder'")
            def success = folder.deleteDir()
            if (!success) {
                throw new RuntimeException("Failed to delete '$folder'")
            }
        }
    }

    static addTaskUninstallAllTargetPlatforms(Project project, Config config) {
        project.task(TASK_NAME_UNINSTALL_ALL_TARGET_PLATFORMS) {
            group = Constants.gradleTaskGroupName
            description = "Deletes all target platforms from the current machine."
            doLast { deleteFolder(project, config.targetPlatformsDir) }
        }
    }

    static defineConventionMapping(Project project) {
        def convention = new AssembleTargetPlatformConvention(project)
        project.convention.plugins.assembletargetplatform = convention
        project.tasks.withType(AssembleTargetPlatformTask.class).all { AssembleTargetPlatformTask task ->
            task.conventionMapping.updateSites = { convention.updateSites }
            task.conventionMapping.features = { convention.features }
            task.conventionMapping.nonMavenizedTargetPlatformDir = { convention.nonMavenizedTargetPlatformDir }
            task.conventionMapping.eclipseSdkExe = { convention.eclipseSdkExe }
            task.conventionMapping.sdkVersion = { convention.sdkVersion }
        }
    }
}
