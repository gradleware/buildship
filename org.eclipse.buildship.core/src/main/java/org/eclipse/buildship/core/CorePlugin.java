/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 *     Simon Scholz (vogella GmbH) - Bug 465723
 */

package org.eclipse.buildship.core;

import java.util.Dictionary;
import java.util.Hashtable;

import com.gradleware.tooling.toolingclient.ToolingClient;
import com.gradleware.tooling.toolingmodel.repository.Environment;
import com.gradleware.tooling.toolingmodel.repository.ModelRepositoryProvider;
import com.gradleware.tooling.toolingmodel.repository.internal.DefaultModelRepositoryProvider;
import com.gradleware.tooling.toolingutils.distribution.PublishedGradleVersions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import org.eclipse.core.runtime.Plugin;

import org.eclipse.buildship.core.configuration.ProjectConfigurationManager;
import org.eclipse.buildship.core.configuration.internal.DefaultProjectConfigurationManager;
import org.eclipse.buildship.core.console.ProcessStreamsProvider;
import org.eclipse.buildship.core.console.internal.StdProcessStreamsProvider;
import org.eclipse.buildship.core.event.EventBroker;
import org.eclipse.buildship.core.event.internal.GuavaEventBroker;
import org.eclipse.buildship.core.launch.GradleLaunchConfigurationManager;
import org.eclipse.buildship.core.launch.internal.DefaultGradleLaunchConfigurationManager;
import org.eclipse.buildship.core.util.logging.EclipseLogger;
import org.eclipse.buildship.core.workbench.WorkbenchOperations;
import org.eclipse.buildship.core.workbench.internal.EmptyWorkbenchOperations;
import org.eclipse.buildship.core.workspace.WorkspaceOperations;
import org.eclipse.buildship.core.workspace.internal.DefaultWorkspaceOperations;

/**
 * The plug-in runtime class for the Gradle integration plugin containing the non-UI elements.
 * <p>
 * This class is automatically instantiated by the Eclipse runtime and wired through the
 * <tt>Bundle-Activator</tt> entry in the <tt>META-INF/MANIFEST.MF</tt> file. The registered
 * instance can be obtained during runtime through the {@link CorePlugin#getInstance()} method.
 * <p>
 * Moreover, this is the entry point for accessing associated services:
 * <ul>
 * <li>{@link #modelRepositoryProvider()}: toolingmodel entry point</li>
 * <li>{@link #logger()}: logging facility</li>
 * <li>{@link #workspaceOperations()}: workspace operations to add/modify/delete projects in the
 * workspace</li>
 * <li>{@link #publishedGradleVersions()}: to retrieve all released Gradle versions</li>
 * <li>{@link #eventBroker()}: to be able to use one common {@link EventBroker} within Buildship for
 * event communication. The events, which are send by this {@link EventBroker} should be an
 * implementation of GradleEvent, in order to have a common interface for events.</li>
 * </ul>
 * <p>
 * The {@link #start(BundleContext)} and {@link #stop(BundleContext)} methods' responsibility is to
 * assign and free the managed services along the plugin runtime lifecycle.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class CorePlugin extends Plugin {

    public static final String PLUGIN_ID = "org.eclipse.buildship.core"; //$NON-NLS-1$

    private static CorePlugin plugin;

    // do not use generics-aware signature since this causes compilation troubles (JDK, Spock)
    // search the web for -target jsr14 to find out more about this obscurity
    private ServiceRegistration loggerService;
    private ServiceRegistration publishedGradleVersionsService;
    private ServiceRegistration modelRepositoryProviderService;
    private ServiceRegistration workspaceOperationsService;
    private ServiceRegistration projectConfigurationManagerService;
    private ServiceRegistration processStreamsProviderService;
    private ServiceRegistration gradleLaunchConfigurationService;
    private ServiceRegistration workbenchOperationsService;
    private ServiceRegistration eventBrokerService;

    // service tracker for each service to allow to register other service implementations of the
    // same type but with higher prioritization, useful for testing
    private ServiceTracker loggerServiceTracker;
    private ServiceTracker publishedGradleVersionsServiceTracker;
    private ServiceTracker modelRepositoryProviderServiceTracker;
    private ServiceTracker workspaceOperationsServiceTracker;
    private ServiceTracker projectConfigurationManagerServiceTracker;
    private ServiceTracker processStreamsProviderServiceTracker;
    private ServiceTracker gradleLaunchConfigurationServiceTracker;
    private ServiceTracker workbenchOperationsServiceTracker;
    private ServiceTracker eventBrokerServiceTracker;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        registerServices(bundleContext);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        unregisterServices();
        super.stop(context);
    }

    private void registerServices(BundleContext context) {
        // store services with low ranking such that they can be overridden
        // during testing or the like
        Dictionary<String, Object> preferences = new Hashtable<String, Object>();
        preferences.put(Constants.SERVICE_RANKING, 1);

        // initialize service trackers before the services are created
        this.loggerServiceTracker = createServiceTracker(context, Logger.class);
        this.publishedGradleVersionsServiceTracker = createServiceTracker(context, PublishedGradleVersions.class);
        this.modelRepositoryProviderServiceTracker = createServiceTracker(context, ModelRepositoryProvider.class);
        this.workspaceOperationsServiceTracker = createServiceTracker(context, WorkspaceOperations.class);
        this.projectConfigurationManagerServiceTracker = createServiceTracker(context, ProjectConfigurationManager.class);
        this.processStreamsProviderServiceTracker = createServiceTracker(context, ProcessStreamsProvider.class);
        this.gradleLaunchConfigurationServiceTracker = createServiceTracker(context, GradleLaunchConfigurationManager.class);
        this.workbenchOperationsServiceTracker = createServiceTracker(context, WorkbenchOperations.class);
        this.eventBrokerServiceTracker = createServiceTracker(context, EventBroker.class);

        // register all services
        this.loggerService = registerService(context, Logger.class, createLogger(), preferences);
        this.publishedGradleVersionsService = registerService(context, PublishedGradleVersions.class, createPublishedGradleVersions(), preferences);
        this.modelRepositoryProviderService = registerService(context, ModelRepositoryProvider.class, createModelRepositoryProvider(), preferences);
        this.workspaceOperationsService = registerService(context, WorkspaceOperations.class, createWorkspaceOperations(), preferences);
        this.projectConfigurationManagerService = registerService(context, ProjectConfigurationManager.class, createProjectConfigurationManager(), preferences);
        this.processStreamsProviderService = registerService(context, ProcessStreamsProvider.class, createProcessStreamsProvider(), preferences);
        this.gradleLaunchConfigurationService = registerService(context, GradleLaunchConfigurationManager.class, createGradleLaunchConfigurationManager(), preferences);
        this.workbenchOperationsService = registerService(context, WorkbenchOperations.class, createWorkbenchOperations(), preferences);
        this.eventBrokerService = registerService(context, EventBroker.class, createEventBroker(), preferences);
    }

    private ServiceTracker createServiceTracker(BundleContext context, Class<?> clazz) {
        ServiceTracker serviceTracker = new ServiceTracker(context, clazz.getName(), null);
        serviceTracker.open();
        return serviceTracker;
    }

    private <T> ServiceRegistration registerService(BundleContext context, Class<T> clazz, T service, Dictionary<String, Object> properties) {
        return context.registerService(clazz.getName(), service, properties);
    }

    private EclipseLogger createLogger() {
        return new EclipseLogger(getLog(), PLUGIN_ID);
    }

    private PublishedGradleVersions createPublishedGradleVersions() {
        return PublishedGradleVersions.create(true);
    }

    private ModelRepositoryProvider createModelRepositoryProvider() {
        ToolingClient toolingClient = ToolingClient.newClient();
        return new DefaultModelRepositoryProvider(toolingClient, Environment.ECLIPSE);
    }

    private WorkspaceOperations createWorkspaceOperations() {
        return new DefaultWorkspaceOperations();
    }

    private ProjectConfigurationManager createProjectConfigurationManager() {
        WorkspaceOperations workspaceOperations = (WorkspaceOperations) this.workspaceOperationsServiceTracker.getService();
        return new DefaultProjectConfigurationManager(workspaceOperations);
    }

    private ProcessStreamsProvider createProcessStreamsProvider() {
        return new StdProcessStreamsProvider();
    }

    private GradleLaunchConfigurationManager createGradleLaunchConfigurationManager() {
        return new DefaultGradleLaunchConfigurationManager();
    }

    private WorkbenchOperations createWorkbenchOperations() {
        return new EmptyWorkbenchOperations();
    }

    private EventBroker createEventBroker() {
        return new GuavaEventBroker();
    }

    private void unregisterServices() {
        this.workbenchOperationsService.unregister();
        this.gradleLaunchConfigurationService.unregister();
        this.processStreamsProviderService.unregister();
        this.projectConfigurationManagerService.unregister();
        this.workspaceOperationsService.unregister();
        this.modelRepositoryProviderService.unregister();
        this.publishedGradleVersionsService.unregister();
        this.loggerService.unregister();
        this.eventBrokerService.unregister();

        this.workbenchOperationsServiceTracker.close();
        this.gradleLaunchConfigurationServiceTracker.close();
        this.processStreamsProviderServiceTracker.close();
        this.projectConfigurationManagerServiceTracker.close();
        this.workspaceOperationsServiceTracker.close();
        this.modelRepositoryProviderServiceTracker.close();
        this.publishedGradleVersionsServiceTracker.close();
        this.loggerServiceTracker.close();
        this.eventBrokerServiceTracker.close();
    }

    public static CorePlugin getInstance() {
        return plugin;
    }

    public static Logger logger() {
        return (Logger) getInstance().loggerServiceTracker.getService();
    }

    public static PublishedGradleVersions publishedGradleVersions() {
        return (PublishedGradleVersions) getInstance().publishedGradleVersionsServiceTracker.getService();
    }

    public static ModelRepositoryProvider modelRepositoryProvider() {
        return (ModelRepositoryProvider) getInstance().modelRepositoryProviderServiceTracker.getService();
    }

    public static WorkspaceOperations workspaceOperations() {
        return (WorkspaceOperations) getInstance().workspaceOperationsServiceTracker.getService();
    }

    public static ProjectConfigurationManager projectConfigurationManager() {
        return (ProjectConfigurationManager) getInstance().projectConfigurationManagerServiceTracker.getService();
    }

    public static ProcessStreamsProvider processStreamsProvider() {
        return (ProcessStreamsProvider) getInstance().processStreamsProviderServiceTracker.getService();
    }

    public static GradleLaunchConfigurationManager gradleLaunchConfigurationManager() {
        return (GradleLaunchConfigurationManager) getInstance().gradleLaunchConfigurationServiceTracker.getService();
    }

    public static WorkbenchOperations workbenchOperations() {
        return (WorkbenchOperations) getInstance().workbenchOperationsServiceTracker.getService();
    }

    public static EventBroker eventBroker() {
        return (EventBroker) getInstance().eventBrokerServiceTracker.getService();
    }

}
