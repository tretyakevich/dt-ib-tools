/**
 * Copyright (C) 2026, 1C-Soft LLC
 */
package com.e1c.edt.internal.ibtools.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com._1c.g5.v8.dt.core.ICoreConstants;
import com._1c.g5.v8.dt.platform.services.core.infobases.sync.IConfigDumpInfoStore;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

/**
 * 1C:EDT IB synchronization state management facility.
 * It ressembles the logic of IB sync. state production to be able to perform without active 1C:EDT project contexts
 * (as it done in com._1c.g5.v8.dt.core.resource.IResourceStoreManager)
 *
 * @author Alexander Tretyakevich
 */
public class EdtIBSynchronizationStateManager
{
    private static final String PROJECT_FILE = ".project"; //$NON-NLS-1$
    private static final String SOURCE_FOLDER = "src"; //$NON-NLS-1$

    private static final String NAME_START_TAG = "<name>"; //$NON-NLS-1$
    private static final String NATURES_START_TAG = "<natures>"; //$NON-NLS-1$
    private static final String NATURES_END_TAG = "</natures>"; //$NON-NLS-1$
    private static final Pattern NATURE_PATTERN = Pattern.compile("<nature>(.*?)</nature>"); //$NON-NLS-1$
    private static final Pattern NAME_PATTERN = Pattern.compile("<name>(.*?)</name>"); //$NON-NLS-1$

    private static final String EXTENSION_SYNC_STATE_HOLDER = "ext"; //$NON-NLS-1$
    private static final String INDEX_FILE = "index.idx"; //$NON-NLS-1$

    private static final String CONFIGURATION_RECORD_PATTERN = "Configuration."; //$NON-NLS-1$

    /**
     * Generates a synthetic IB synchronization state for a given source project (both configuration and extensions project
     * types are supported) and a target IB
     *
     * @param sourceProjectFolder A source 1C:EDT project folder. Cannot be {@code null}
     * @param sourceConfigDumpInfoFile A path to a ConfigDumpInfo.xml being received from the source IB. Cannot be {@code null}
     * @param generationId A global generation identifier being received from the source IB. Cannot be {@code null}
     * @param targetIBUuid A target IB UUID. Cannot be {@code null}
     * @param syncStateTargetFolder A target folder for sync states of IBs. Cannot be {@code null}
     */
    public void generateIBSyncState(Path sourceProjectFolder, Path sourceConfigDumpInfoFile, String generationId,
        UUID targetIBUuid, Path syncStateTargetFolder)
    {
        // Step 1: Determine the type of the project - a Configuration or an Extension
        ProjectInfo projectInfo = getProjectInfo(sourceProjectFolder);
        // Step 2: Create/update target folders
        Path targetFolder = initAndGetTargetFolders(projectInfo, targetIBUuid, syncStateTargetFolder);
        // Step 3: Copy source ConfigDumpInfo.xml directly to the destination
        copyConfigDumpInfo(sourceConfigDumpInfoFile, targetFolder);
        // Step 4: Re-create EDT source signatures directly
        Map<Path, byte[]> signatures = collectSignatures(sourceProjectFolder);
        // Step 5: Parse source config dump info to receive generation id
        ConfigDumpParseResult configDumpInfoParseResult = parseConfigDump(sourceConfigDumpInfoFile);
        // Step 6: Create/Update combined synchronization state
        updateIBSynchronizationState(signatures, configDumpInfoParseResult, generationId, sourceProjectFolder,
            targetFolder);
    }

    private static void updateIBSynchronizationState(Map<Path, byte[]> signatures,
        ConfigDumpParseResult configDumpInfoParseResult, String generationId, Path sourceProjectFolder,
        Path targetFolder)
    {
        try (DataOutputStream dao =
            new DataOutputStream(new FileOutputStream(targetFolder.resolve(INDEX_FILE).toFile())))
        {
            // Writing the sync. timestamp
            dao.writeLong(System.currentTimeMillis());
            dao.writeInt(signatures.size());
            for (Entry<Path, byte[]> entry : signatures.entrySet())
            {
                // Resource path
                dao.writeUTF(sourceProjectFolder.relativize(entry.getKey()).toString());
                // Signature length
                dao.writeInt(entry.getValue().length);
                // Signature body
                dao.write(entry.getValue());
            }

            // IB data generation identifier
            dao.writeUTF(generationId);
            // Configuration object UUID
            dao.writeUTF(configDumpInfoParseResult.configurationUUID());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<Path, byte[]> collectSignatures(Path sourceProjectFolder)
    {
        // Collect all existent source files at once
        Collection<Path> targetFiles = new HashSet<>();
        try
        {
            Files.walkFileTree(sourceProjectFolder.resolve(SOURCE_FOLDER), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                {
                    targetFiles.add(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        // Collect source signatures using the same alghorithm that 1C:EDT is using
        Map<Path, byte[]> signatures =
            targetFiles.parallelStream().collect(Collectors.toConcurrentMap(file -> file, file -> {
                return computeSignature(file);
            }, (oldValue, newValue) -> oldValue, ConcurrentHashMap::new));

        return signatures;
    }

    private static byte[] computeSignature(Path filePath)
    {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(filePath), 4096))
        {
            return computeSignature(inputStream);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] computeSignature(InputStream content)
    {
        try
        {
            return Hashing.sha256().hashBytes(content.readAllBytes()).asBytes();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyConfigDumpInfo(Path sourceConfigDumpInfoFile, Path targetFolder)
    {
        // TODO validation of file existance
        try
        {
            Files.copy(sourceConfigDumpInfoFile, targetFolder.resolve(IConfigDumpInfoStore.CONFIG_DUMP_INFO),
                StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static Path initAndGetTargetFolders(ProjectInfo projectInfo, UUID targetIBUuid, Path syncStateTargetFolder)
    {
        Path targetFolder = syncStateTargetFolder.resolve(targetIBUuid.toString());
        if (projectInfo.leadingNature.equals(ICoreConstants.V8_EXTENSION_NATURE))
        {
            targetFolder = targetFolder.resolve(EXTENSION_SYNC_STATE_HOLDER).resolve(projectInfo.name);
        }

        if (!Files.exists(targetFolder))
        {
            try
            {
                Files.createDirectories(targetFolder);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        return targetFolder;
    }

    private static ProjectInfo getProjectInfo(Path sourceProjectFolder)
    {
        Path projectManifestPath = sourceProjectFolder.resolve(PROJECT_FILE);
        if (!Files.exists(projectManifestPath))
        {
            // TODO error handling
        }

        Set<String> natures = new HashSet<>();
        String projectName = null;
        try (BufferedReader projectDefinitionReader = new BufferedReader(new FileReader(projectManifestPath.toFile())))
        {
            String line;
            boolean naturesBlock = false;

            while ((line = projectDefinitionReader.readLine().trim()) != null)
            {
                if (projectName == null && line.startsWith(NAME_START_TAG))
                {
                    // Reading the project name
                    Matcher matcher = NAME_PATTERN.matcher(line);
                    if (matcher.find())
                    {
                        projectName = matcher.group(1).trim();
                    }
                }

                // Start to read only if the file starts with natures tag
                if (!naturesBlock && line.trim().startsWith(NATURES_START_TAG))
                {
                    naturesBlock = true;
                }

                // Finishing if finishing natures tag is found
                if (naturesBlock && line.trim().startsWith(NATURES_END_TAG))
                {
                    break;
                }

                if (naturesBlock)
                {
                    Matcher matcher = NATURE_PATTERN.matcher(line);
                    while (matcher.find())
                    {
                        natures.add(matcher.group(1).trim());
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        if (natures.contains(com._1c.g5.v8.dt.core.ICoreConstants.V8_EXTENSION_NATURE))
        {
            return new ProjectInfo(com._1c.g5.v8.dt.core.ICoreConstants.V8_EXTENSION_NATURE, projectName);
        }
        else if (natures.contains(com._1c.g5.v8.dt.core.ICoreConstants.V8_CONFIGURATION_NATURE))
        {
            return new ProjectInfo(com._1c.g5.v8.dt.core.ICoreConstants.V8_CONFIGURATION_NATURE, projectName);
        }
        else
        {
            // TODO throw an exception
        }

        return null;
    }

    /**
     * Parses a given ConfigDumpInfo file
     *
     * @param location The location of the file. Cannot be {@link NullPointerException}
     * @return The result of parsing. Cannot be {@link null}
     * @throws IOException
     */
    public static ConfigDumpParseResult parseConfigDump(Path location)
    {
        Preconditions.checkNotNull(location);

        Map<String, String> versions = new HashMap<>();
        String configurationUUID = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(location.toFile(), StandardCharsets.UTF_8)))
        {
            String line = reader.readLine();
            while (line != null)
            {
                line = reader.readLine();

                if (line != null && line.stripLeading().startsWith("<Metadata")) //$NON-NLS-1$
                {
                    int nameStartIdx = line.indexOf("name=\""); //$NON-NLS-1$
                    String name = line.substring(nameStartIdx + 6, line.indexOf("\"", nameStartIdx + 6)); //$NON-NLS-1$
                    if (configurationUUID == null && isConfigurationRecord(name))
                    {
                        int idStartIdx = line.indexOf("id=\""); //$NON-NLS-1$
                        configurationUUID = line.substring(idStartIdx + 4, line.indexOf("\"", idStartIdx + 4)); //$NON-NLS-1$
                    }

                    int versionStartIdx = line.indexOf("configVersion=\""); //$NON-NLS-1$
                    if (versionStartIdx != -1)
                    {
                        String versionId =
                            line.substring(versionStartIdx + 15, line.indexOf("\"", versionStartIdx + 15)); //$NON-NLS-1$
                        versions.put(name, versionId);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        return new ConfigDumpParseResult(versions, configurationUUID);
    }

    private static boolean isConfigurationRecord(String name)
    {
        boolean result = name.startsWith(CONFIGURATION_RECORD_PATTERN);
        if (result)
        {
            result = name.lastIndexOf(".") == CONFIGURATION_RECORD_PATTERN.length() - 1; //$NON-NLS-1$
        }
        return result;
    }

    private record ProjectInfo(String leadingNature, String name)
    {
        // Left empty intentionally
    }

    /**
     * Internal record which used to transfer parse result of a ConfigDumpInfo files.
     *
     * @param versions Map where keys are metadata element names like "Catalog.Справочник.Form.ФормаЭлемента.Form"
     * and values are versions of element content as deemed by platform. Never null.
     * @param configurationUUID UUID of root metadata object. Could be null or empty if dump file
     * does not contain version for root metadata object.
     */
    private record ConfigDumpParseResult(Map<String, String> versions, String configurationUUID)
    {
        // Left empty intentionally
    }
}
