/**
 * Copyright (C) 2026, 1C-Soft LLC
 */
package com.e1c.edt.internal.ibtools.cli;

import org.eclipse.osgi.util.NLS;

/**
 * Localization class
 *
 * @author Alexander Tretyakevich
 */
class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$

    public static String GenerateIBSyncStateCmd_Description;
    public static String GenerateIBSyncStateCmd_Project_source_project_location;
    public static String GenerateIBSyncStateCmd_Project_source_config_dump_info_file;
    public static String GenerateIBSyncStateCmd_Project_generation_id;
    public static String GenerateIBSyncStateCmd_Project_IB_UUID;
    public static String GenerateIBSyncStateCmd_Project_Sync_State_Target_Folder;

    public static String GenerateIBSyncStateCmd_SourceProjectFolder__0__does_not_exist;
    public static String GenerateIBSyncStateCmd_SourceConfigDumpInfoFile__0__does_not_exist;
    public static String GenerateIBSyncStateCmd_TargetIBUuid__0__is_invalid;

    public static String CompareIBSyncStatesCmd_Description;
    public static String CompareIBSyncStatesCmd_Source_Synchronization_Index_Location;
    public static String CompareIBSyncStatesCmd_Destination_Synchronization_Index_Location;
    public static String CompareIBSyncStatesCmd_SourceIndexFolder__0__does_not_exist;
    public static String CompareIBSyncStatesCmd_DestinationIndexFolder__0__does_not_exist;

    static
    {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
        //nop
    }
}
