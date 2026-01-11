/**
 * Copyright (C) 2026, 1C-Soft LLC
 */
package com.e1c.edt.internal.ibtools;

import org.eclipse.core.runtime.Plugin;

import com.e1c.g5.v8.dt.cli.api.components.BaseCliCommandExternalDependencyModule;

/**
 * External services bindings for plugin.
 *
 * @author Alexander Tretyakevich
 */
public class ExternalDependenciesModule
    extends BaseCliCommandExternalDependencyModule
{

    /**
     * Constructor of {@link ExternalDependenciesModule}.
     *
     * @param bundle the parent bundle, cannot be {@code null}
     */
    public ExternalDependenciesModule(Plugin bundle)
    {
        super(bundle);
    }
}
