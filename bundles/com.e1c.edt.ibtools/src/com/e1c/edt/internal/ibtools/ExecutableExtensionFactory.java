/**
 * Copyright (C) 2026, 1C-Soft LLC
 */
package com.e1c.edt.internal.ibtools;

import org.osgi.framework.Bundle;

import com.e1c.g5.v8.dt.cli.api.components.BaseCliCommandExtensionFactory;
import com.google.inject.Injector;

/**
 * Guice module aware executable extension factory.
 *
 * @author Alexander Tretyakevich
 */
public class ExecutableExtensionFactory
    extends BaseCliCommandExtensionFactory
{
    @Override
    protected Bundle getBundle()
    {
        return IBToolsPlugin.getDefault().getBundle();
    }

    @Override
    protected Injector getInjector()
    {
        return IBToolsPlugin.getDefault().getInjector();
    }
}
