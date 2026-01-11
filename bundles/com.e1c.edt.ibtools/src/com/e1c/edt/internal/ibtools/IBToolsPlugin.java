/**
 * Copyright (C) 2026, 1C-Soft LLC
 */
package com.e1c.edt.internal.ibtools;

import static java.text.MessageFormat.format;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com._1c.g5.wiring.InjectorAwareServiceRegistrator;
import com._1c.g5.wiring.ServiceInitialization;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The IB Tools plugin activator
 *
 * @author Alexander Tretyakevich
 */
public class IBToolsPlugin
    extends Plugin
{
    public static final String PLUGIN_ID = "com.e1c.edt.ibtools"; //$NON-NLS-1$

    private static IBToolsPlugin instance;

    private volatile Injector injector;
    private InjectorAwareServiceRegistrator registrator;

    /**
     * This method shall be called to get instance of plugin
     *
     * @return plug-in instance, cannot be <code>null</code>
     */
    public static IBToolsPlugin getInstance()
    {
        return instance;
    }

    /**
     * Returns the plugin Guice-injector. Method is synchronized.
     *
     * @return the plugin Guice-injector, never {@code null}
     */
    /* package */ synchronized Injector getInjector()
    {
        if (injector == null)
        {
            return injector = createInjector();
        }
        return injector;
    }

    private Injector createInjector()
    {
        try
        {
            return Guice.createInjector(new ExternalDependenciesModule(this));
        }
        catch (Exception e)
        {
            String message = format("Failed to create injector for {0}", getBundle().getSymbolicName()); //$NON-NLS-1$
            log(createErrorStatus(message, e));
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Gets default implementation
     * @return default implementation, cannot <code>null</code>
     */
    public static IBToolsPlugin getDefault()
    {
        return instance;
    }

    /**
     * Logs {@link IStatus}
     * @param status logging {@link IStatus}, cannot be <code>null</code>
     */
    public static void log(IStatus status)
    {
        instance.getLog().log(status);
    }

    /**
     * Logs the provided message as {@code INFO} status if plug-in is in debug mode.
     *
     * @param message the message to log, cannot be {@code null}
     */
    public static void logDebug(String message)
    {
        if (getDefault().isDebugging())
        {
            log(createInfoStatus(message));
        }
    }

    /**
     * Creates an info status by the provided message.
     *
     * @param message the status message, cannot be {@code null}
     * @return the status created info status, never {@code null}
     */
    public static IStatus createInfoStatus(String message)
    {
        return new Status(IStatus.INFO, PLUGIN_ID, 0, message, null);
    }

    /**
     * Creates error {@link IStatus} by the message and {@link Throwable}
     * @param msg error message, cannot be <code>null</code>
     * @param e exception for creating {@link IStatus}, can be <code>null</code>
     * @return created {@link IStatus}, cannot <code>null</code>
     */
    public static IStatus createErrorStatus(String msg, Throwable e)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, e);
    }

    /**
     * Creates warning {@link IStatus} by the message
     * @param msg warning message, cannot be <code>null</code>
     * @return created {@link IStatus}, cannot <code>null</code>
     */
    public static IStatus createWarningStatus(String msg)
    {
        return new Status(IStatus.WARNING, PLUGIN_ID, 0, msg, null);
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception
    {
        super.start(bundleContext);
        instance = this;

        registrator = new InjectorAwareServiceRegistrator(bundleContext, this::getInjector);
        ServiceInitialization.schedule(() -> {
            // Do nothing by default
        });
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception
    {
        registrator.unregisterServices();
        instance = null;
        super.stop(bundleContext);
    }

}
