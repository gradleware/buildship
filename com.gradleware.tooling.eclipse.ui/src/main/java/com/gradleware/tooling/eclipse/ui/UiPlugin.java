/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 */

package com.gradleware.tooling.eclipse.ui;

import java.util.Dictionary;
import java.util.Hashtable;

import com.gradleware.tooling.eclipse.core.workbench.WorkbenchOperations;
import com.gradleware.tooling.eclipse.ui.workbench.DefaultWorkbenchOperations;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.gradleware.tooling.eclipse.core.Logger;
import com.gradleware.tooling.eclipse.core.console.ProcessStreamsProvider;
import com.gradleware.tooling.eclipse.core.util.logging.EclipseLogger;
import com.gradleware.tooling.eclipse.ui.console.ConsoleProcessStreamsProvider;

/**
 * The plug-in runtime class for the Gradle integration plug-in containing the UI-related elements.
 * <p>
 * This class is automatically instantiated by the Eclipse runtime and wired through the
 * <tt>Bundle-Activator</tt> entry in the <tt>META-INF/MANIFEST.MF</tt> file. The registered
 * instance can be obtained during runtime through the {@link UiPlugin#getInstance()} method.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class UiPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.gradleware.tooling.eclipse.ui"; //$NON-NLS-1$

    private static UiPlugin plugin;

    // do not use generics-aware signature since this causes compilation troubles (JDK, Spock)
    // search for -target jsr14 to find out more about this obscurity
    private ServiceRegistration loggerService;
    private ServiceRegistration processStreamsProviderService;
    private ServiceRegistration workbenchOperationsService;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        registerServices(context);
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

        Dictionary<String, Object> priorityPreferences = new Hashtable<String, Object>();
        priorityPreferences.put(Constants.SERVICE_RANKING, 2);

        // register all services (override the ProcessStreamsProvider registered in the core plugin)
        this.loggerService = registerService(context, Logger.class, createLogger(), preferences);
        this.processStreamsProviderService = registerService(context, ProcessStreamsProvider.class, createConsoleProcessStreamsProvider(), priorityPreferences);
        this.workbenchOperationsService = registerService(context, WorkbenchOperations.class, createWorkbenchOperations(), priorityPreferences);
    }

    private <T> ServiceRegistration registerService(BundleContext context, Class<T> clazz, T service, Dictionary<String, Object> properties) {
        return context.registerService(clazz.getName(), service, properties);
    }

    private EclipseLogger createLogger() {
        return new EclipseLogger(getLog(), PLUGIN_ID);
    }

    private ProcessStreamsProvider createConsoleProcessStreamsProvider() {
        return new ConsoleProcessStreamsProvider();
    }

    private WorkbenchOperations createWorkbenchOperations() {
        return new DefaultWorkbenchOperations();
    }

    private void unregisterServices() {
        this.workbenchOperationsService.unregister();
        this.processStreamsProviderService.unregister();
        this.loggerService.unregister();
    }

    public static UiPlugin getInstance() {
        return plugin;
    }

    public static Logger logger() {
        return getService(getInstance().loggerService.getReference());
    }

    private static <T> T getService(ServiceReference reference) {
        return (T) reference.getBundle().getBundleContext().getService(reference);
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry imageRegistry) {
        for (PluginImages pluginImage : PluginImages.values()) {
            pluginImage.register();
        }
    }

}
