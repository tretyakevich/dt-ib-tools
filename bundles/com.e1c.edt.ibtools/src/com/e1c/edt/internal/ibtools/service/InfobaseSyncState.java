/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.internal.ibtools.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Infobase synchronization state holder
 *
 * @author Alexander Tretyakevich
 */
public final class InfobaseSyncState
{
    private final long timestamp;
    private final String generationId;
    private final String configurationUUID;

    private final Map<String, byte[]> edtResourceSignatures;
    private final Map<String, String> platformResourceVersions;

    private final Map<String, InfobaseSyncState> extensionSyncStates;

    /**
     * Constructs an instance.
     *
     * @param timestamp Last sync. state chaning timestamp
     * @param configurationUUID An UUID of a target configuration. Cannot be {@code null}
     * @param edtResourceHashes Versions of project resources.
     * Keys are resource relative paths (starting from 'src' folder, inclusive)
     * and values are their content hashes. Must not be {@code null}.
     * @param platformResourceVersions Versions of metadata objects as seen in infobase configuration by platform.
     * Keys are fully qualified metadata element names like
     * "Catalog.Справочник.Form.ФормаЭлемента.Form" and values are versions of element content as deemed by platform.
     * Must not be {@code null}.
     * @param generationId The global generation identifier from the platform. Cannot be {@code null}
     */
    public InfobaseSyncState(long timestamp, String configurationUUID, Map<String, byte[]> edtResourceHashes,
        Map<String, String> platformResourceVersions, String generationId)
    {
        this.timestamp = timestamp;
        this.configurationUUID = configurationUUID;
        this.edtResourceSignatures =
            new HashMap<>(edtResourceHashes != null ? edtResourceHashes : Collections.emptyMap());
        this.platformResourceVersions =
            new HashMap<>(platformResourceVersions != null ? platformResourceVersions : Collections.emptyMap());
        this.generationId = generationId;
        this.extensionSyncStates = new HashMap<>();
    }

    @Override
    public InfobaseSyncState clone()
    {
        InfobaseSyncState clone = new InfobaseSyncState(timestamp, configurationUUID, edtResourceSignatures,
            platformResourceVersions, generationId);

        for (Entry<String, InfobaseSyncState> extensionEntry : extensionSyncStates.entrySet())
        {
            InfobaseSyncState extensionState = extensionEntry.getValue();
            clone.getExtensionSyncStates()
                .put(extensionEntry.getKey(),
                    new InfobaseSyncState(extensionState.getTimestamp(), extensionState.getConfigurationUUID(),
                        extensionState.getEdtResourceSignatures(), extensionState.getPlatformResourceVersions(),
                        extensionState.getGenerationId()));
        }

        return clone;
    }

    /**
     * Gets a timestamp of the sync state
     *
     * @return The timestamp the sync state was written
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Gets signatures of EDT resources.
     *
     * @return Map where keys are resources relative paths (starting from 'src' folder, inclusive)
     * and values are their content hashes. Never {@code null}.
     */
    public Map<String, byte[]> getEdtResourceSignatures()
    {
        return edtResourceSignatures;
    }

    /**
     * Gets versions of metadata objects as seen in infobase configuration by platform.
     *
     * @return Map where keys are metadata element names (FQN) like "Catalog.Справочник.Form.ФормаЭлемента.Form"
     * and values are versions of element content as deemed by platform. Never null.
     */
    public Map<String, String> getPlatformResourceVersions()
    {
        return platformResourceVersions;
    }

    /**
     * The global generation identifier being received from the platform
     *
     * @return The global generation identifier which is being changed as a result of any configuration change on the
     * platform side. Cannot be {@code null}. May be empty
     */
    public String getGenerationId()
    {
        return generationId;
    }

    /**
     * The UUID of the Configuration object.
     *
     * @return The configuration UUID. Cannot be {@code null}. May be empty
     */
    public String getConfigurationUUID()
    {
        return configurationUUID;
    }

    /**
     * Gets extension sync states
     *
     * @return {@link InfobaseSyncState} mapped to a respective name of an extension project
     */
    public Map<String, InfobaseSyncState> getExtensionSyncStates()
    {
        return extensionSyncStates;
    }
}
