/**
 * Copyright (C) 2026, 1C-Soft LLC
 */
package com.e1c.edt.internal.ibtools.cli;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.function.Function;

import com._1c.g5.v8.dt.cli.api.CliApiException;
import com.e1c.edt.internal.ibtools.service.EdtIBSynchronizationStateManager;
import com.e1c.g5.v8.dt.cli.api.Argument;
import com.e1c.g5.v8.dt.cli.api.CliCommand;
import com.e1c.g5.v8.dt.cli.api.components.BaseCliCommand;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Provides commands that generate 1C:EDT synchronization state for externally synchronized source state of a target
 * project and a target IB.<br>
 *
 * The result of the command is a target IB synchronization state being placed in a specified folder.<br>
 * The resulting synchronization state does not guarantee the proper funtioning of an IB syncronization process
 * inside the EDT in case if a source project and target IB are not synchronized completelly and flawlessly before
 * the process of the generation
 *
 * @author Alexander Tretyakevich
 */
public class GenerateIBSyncStateCmd
    extends BaseCliCommand
{
    private EdtIBSynchronizationStateManager synchronizationStateManager;

    @Inject
    public GenerateIBSyncStateCmd(EdtIBSynchronizationStateManager synchronizationStateManager)
    {
        this.synchronizationStateManager = synchronizationStateManager;
    }

    /**
     * Formats the BSL files located in a project directory specified by the --project argument.
     * The project directory is resolved against the current working directory.
     *
     * @param projectLocation The location of the project directory, relative to the current working directory, cannot be {@code null}
     */
    @CliCommand(command = "generate-ib-sync-state", value = "GenerateIBSyncStateCmd_Description")
    public void c1_generate_sync_state(
        @Argument(value = "--project",
            descriptor = "GenerateIBSyncStateCmd_Project_source_project_location") String sourceProjectFolder,
        @Argument(value = "--cdi",
            descriptor = "GenerateIBSyncStateCmd_Project_source_config_dump_info_file") String sourceConfigDumpInfoFile,
        @Argument(value = "--gen-id", descriptor = "GenerateIBSyncStateCmd_Project_generation_id") String generationId,
        @Argument(value = "--ib-uuid", descriptor = "GenerateIBSyncStateCmd_Project_IB_UUID") String targetIBUuid,
        @Argument(value = "--target",
            descriptor = "GenerateIBSyncStateCmd_Project_Sync_State_Target_Folder") String syncStateTargetFolder)
    {
        Preconditions.checkNotNull(sourceProjectFolder);
        Preconditions.checkNotNull(sourceConfigDumpInfoFile);
        Preconditions.checkNotNull(generationId);
        Preconditions.checkNotNull(targetIBUuid);
        Preconditions.checkNotNull(syncStateTargetFolder);

        try
        {
            Path sourceProjectFolderPath = validateAndGetFolder(sourceProjectFolder, location -> MessageFormat
                .format(Messages.GenerateIBSyncStateCmd_SourceProjectFolder__0__does_not_exist, location));
            Path sourceConfigDumpInfoFilePath = validateAndGetFile(sourceConfigDumpInfoFile, location -> MessageFormat
                .format(Messages.GenerateIBSyncStateCmd_SourceConfigDumpInfoFile__0__does_not_exist, location));

            UUID uuid = null;
            try
            {
                uuid = UUID.fromString(targetIBUuid);
            }
            catch (IllegalArgumentException e)
            {
                // It is not an UUID
                throw new CliApiException(
                    MessageFormat.format(Messages.GenerateIBSyncStateCmd_TargetIBUuid__0__is_invalid, targetIBUuid));
            }

            synchronizationStateManager.generateIBSyncState(sourceProjectFolderPath, sourceConfigDumpInfoFilePath,
                generationId, uuid, Paths.get(syncStateTargetFolder));
        }
        catch (UncheckedIOException e)
        {
            throw new CliApiException(e.getMessage(), e);
        }
    }

    @CliCommand(command = "compare-ib-sync-states", value = "CompareIBSyncStatesCmd_Description")
    public void c1_compare_sync_states(
        @Argument(value = "--source",
            descriptor = "CompareIBSyncStatesCmd_Source_Synchronization_Index_Location") String sourceIndexFolder,
        @Argument(value = "--destination",
            descriptor = "CompareIBSyncStatesCmd_Destination_Synchronization_Index_Location") String destinationIndexFolder)
    {
        Preconditions.checkNotNull(sourceIndexFolder);
        Preconditions.checkNotNull(destinationIndexFolder);

        try
        {
            Path sourceIndexFolderPath = validateAndGetFolder(sourceIndexFolder, location -> MessageFormat
                .format(Messages.CompareIBSyncStatesCmd_SourceIndexFolder__0__does_not_exist, location));
            Path destinationIndexFolderPath = validateAndGetFile(destinationIndexFolder, location -> MessageFormat
                .format(Messages.CompareIBSyncStatesCmd_DestinationIndexFolder__0__does_not_exist, location));

            synchronizationStateManager.compareIBSyncStates(sourceIndexFolderPath, destinationIndexFolderPath);
        }
        catch (UncheckedIOException e)
        {
            throw new CliApiException(e.getMessage(), e);
        }
    }

    /*
     * Validates input symbolic path and get a target folder path if it exists
     */
    private Path validateAndGetFolder(String folderPath, Function<String, String> errorMessageSupplier)
    {
        Path targetPath = getCurrentWorkDir().resolve(folderPath);
        if (!Files.isDirectory(targetPath))
        {
            throw new CliApiException(errorMessageSupplier.apply(folderPath));
        }

        return targetPath;
    }

    /*
     * Validates input symbolic path and get a target file path if it exists
     */
    private Path validateAndGetFile(String filePath, Function<String, String> errorMessageSupplier)
    {
        Path targetPath = getCurrentWorkDir().resolve(filePath);
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath))
        {
            throw new CliApiException(errorMessageSupplier.apply(filePath));
        }

        return targetPath;
    }
}
