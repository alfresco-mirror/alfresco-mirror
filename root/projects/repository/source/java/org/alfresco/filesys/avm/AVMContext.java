/*
 * Copyright (C) 2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package org.alfresco.filesys.avm;

import org.alfresco.filesys.alfresco.AlfrescoContext;
import org.alfresco.filesys.alfresco.IOControlHandler;
import org.alfresco.filesys.server.filesys.DiskInterface;
import org.alfresco.filesys.server.filesys.FileName;
import org.alfresco.filesys.server.filesys.FileSystem;
import org.alfresco.filesys.server.filesys.NotifyChange;
import org.alfresco.filesys.server.state.FileState;
import org.alfresco.filesys.server.state.FileStateTable;
import org.alfresco.repo.avm.CreateStoreCallback;
import org.alfresco.repo.avm.CreateVersionCallback;
import org.alfresco.repo.avm.PurgeStoreCallback;
import org.alfresco.repo.avm.PurgeVersionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AVM Filesystem Context Class
 * 
 * <p>Contains per filesystem context.
 *
 * @author GKSpencer
 */
public class AVMContext extends AlfrescoContext
	implements CreateStoreCallback, PurgeStoreCallback, CreateVersionCallback, PurgeVersionCallback {

    // Logging
    
    private static final Log logger = LogFactory.getLog(AVMContext.class);
    
	// Constants
	//
	// Version id that indicates the head version
	
	public static final int VERSION_HEAD	= -1;
	
	// Store, root path and version
    
    private String m_storePath;
    private int m_version = VERSION_HEAD; 

    // Flag to indicate if the virtualization view is enabled
    //
    //	The first set of folders then map to the stores and the second layer map to the versions with
    //  paths below.

    private boolean m_virtualView;
    
    /**
     * Class constructor
     * 
     * <p>Construct a context for a normal view onto a single store/version within AVM.
     * 
     * @param filesysName String
     * @param storePath String
     * @param version int
     */
    public AVMContext( String filesysName, String storePath, int version)
    {
    	super( filesysName, storePath + "(" + version + ")");
    	
    	// Set the store root path, remove any trailing slash as relative paths will be appended to this value
    	
    	m_storePath = storePath;
    	if ( m_storePath.endsWith( "/"))
    		m_storePath = m_storePath.substring(0, m_storePath.length() - 1);
    	
    	// Set the store version to use
    	
    	m_version = version;
    }

    /**
     * Class constructor
     * 
     * <p>Construct a context for a virtualization view onto all stores/versions within AVM.
     * 
     * @param filesysName String
     */
    public AVMContext( String filesysName)
    {
    	super( filesysName, "VirtualView");
    	
    	// Enable the virtualization view
    	
    	m_virtualView = true;
    }
    
    /**
     * Return the filesystem type, either FileSystem.TypeFAT or FileSystem.TypeNTFS.
     * 
     * @return String
     */
    public String getFilesystemType()
    {
        return FileSystem.TypeNTFS;
    }
    
    /**
     * Return the store path
     * 
     * @return String
     */
    public final String getStorePath()
    {
        return m_storePath;
    }
    
    /**
     * Return the version
     * 
     * @return int
     */
    public final int isVersion()
    {
    	return m_version;
    }
    
    /**
     * Check if the virtualization view is enabled
     * 
     * @return boolean
     */
    public final boolean isVirtualizationView()
    {
    	return m_virtualView;
    }
    
    /**
     * Close the filesystem context
     */
	public void CloseContext() {
		
		//	Call the base class
		
		super.CloseContext();
	}
	
    /**
     * Create the I/O control handler for this filesystem type
     * 
     * @param filesysDriver DiskInterface
     * @return IOControlHandler
     */
    protected IOControlHandler createIOHandler( DiskInterface filesysDriver)
    {
    	return null;
    }

	/**
	 * Create store call back handler
	 * 
	 * @param storeName String
	 * @param versionID int
	 */
	public void storeCreated(String storeName)
	{
		// Make sure the file state cache is enabled
		
		FileStateTable fsTable = getStateTable();
		if ( fsTable == null)
			return;
		
		// Find the file state for the root folder
		
		FileState rootState = fsTable.findFileState( FileName.DOS_SEPERATOR_STR, true, true);

		if ( rootState != null)
		{
			// Add a pseudo folder for the new store
			
			rootState.addPseudoFile( new StorePseudoFile( storeName));
			
			// DEBUG
			
			if ( logger.isDebugEnabled())
				logger.debug( "Added pseudo folder for new store " + storeName);
			
			// Send a change notification for the new folder
			
			if ( hasChangeHandler())
			{
				// Build the filesystem relative path to the new store folder
				
				StringBuilder str = new StringBuilder();
				
				str.append( FileName.DOS_SEPERATOR);
				str.append( storeName);
				
				// Send the change notification
				
                getChangeHandler().notifyDirectoryChanged(NotifyChange.ActionAdded, str.toString());
			}
		}
	}
	
	/**
	 * Purge store call back handler
	 * 
	 * @param storeName String
	 */
	public void storePurged(String storeName)
	{
		// Make sure the file state cache is enabled
		
		FileStateTable fsTable = getStateTable();
		if ( fsTable == null)
			return;
		
		// Find the file state for the root folder
		
		FileState rootState = fsTable.findFileState( FileName.DOS_SEPERATOR_STR);
		
		if ( rootState != null && rootState.hasPseudoFiles())
		{
			// Remove the pseudo folder for the store

			rootState.getPseudoFileList().removeFile( storeName, false);
			
			// Build the filesystem relative path to the deleted store folder
			
			StringBuilder pathStr = new StringBuilder();
			
			pathStr.append( FileName.DOS_SEPERATOR);
			pathStr.append( storeName);

			// Remove the file state for the deleted store
			
			String storePath = pathStr.toString();
			fsTable.removeFileState( storePath);
			
			// DEBUG
			
			if ( logger.isDebugEnabled())
				logger.debug( "Removed pseudo folder for purged store " + storeName);
			
			// Send a change notification for the deleted folder
			
			if ( hasChangeHandler())
			{
				// Send the change notification
				
	            getChangeHandler().notifyDirectoryChanged(NotifyChange.ActionRemoved, storePath);
			}
		}
	}

	/**
	 * Create version call back handler
	 * 
	 * @param storeName String
	 * @param versionID int
	 */
	public void versionCreated(String storeName, int versionID)
	{
		// Make sure the file state cache is enabled
		
		FileStateTable fsTable = getStateTable();
		if ( fsTable == null)
			return;

		// Build the path to the store version folder
		
		StringBuilder pathStr = new StringBuilder();

		pathStr.append( FileName.DOS_SEPERATOR);
		pathStr.append( storeName);
		pathStr.append( FileName.DOS_SEPERATOR);
		pathStr.append( AVMPath.VersionsFolder);
		
		// Find the file state for the store versions folder
		
		FileState verState = fsTable.findFileState( pathStr.toString());

		if ( verState != null)
		{
			// Create the version folder name
			
			StringBuilder verStr = new StringBuilder();
			
			verStr.append( AVMPath.VersionFolderPrefix);
			verStr.append( versionID);
			
			String verName = verStr.toString();
			
			// Add a pseudo folder for the new version
			
			verState.addPseudoFile( new VersionPseudoFile( verName));
			
			// DEBUG
			
			if ( logger.isDebugEnabled())
				logger.debug( "Added pseudo folder for new version " + storeName + ":/" + verName);
			
			// Send a change notification for the new folder
			
			if ( hasChangeHandler())
			{
				// Build the filesystem relative path to the new version folder
				
				pathStr.append( FileName.DOS_SEPERATOR);
				pathStr.append( verName);
				
				// Send the change notification
				
                getChangeHandler().notifyDirectoryChanged(NotifyChange.ActionAdded, pathStr.toString());
			}
		}
	}

	/**
	 * Purge version call back handler
	 * 
	 * @param storeName String
	 */
	public void versionPurged(String storeName, int versionID)
	{
		// Make sure the file state cache is enabled
		
		FileStateTable fsTable = getStateTable();
		if ( fsTable == null)
			return;

		// Build the path to the store version folder
		
		StringBuilder pathStr = new StringBuilder();

		pathStr.append( FileName.DOS_SEPERATOR);
		pathStr.append( storeName);
		pathStr.append( FileName.DOS_SEPERATOR);
		pathStr.append( AVMPath.VersionsFolder);
		
		// Find the file state for the store versions folder
		
		FileState verState = fsTable.findFileState( pathStr.toString());

		if ( verState != null && verState.hasPseudoFiles())
		{
			// Create the version folder name
			
			StringBuilder verStr = new StringBuilder();
			
			verStr.append( AVMPath.VersionFolderPrefix);
			verStr.append( versionID);
			
			String verName = verStr.toString();
			
			// Remove the pseudo folder for the purged version
			
			verState.getPseudoFileList().removeFile( verName, true);
			
			// DEBUG
			
			if ( logger.isDebugEnabled())
				logger.debug( "Removed pseudo folder for purged version " + storeName + ":/" + verName);
			
			// Send a change notification for the deleted folder
			
			if ( hasChangeHandler())
			{
				// Build the filesystem relative path to the deleted version folder
				
				pathStr.append( FileName.DOS_SEPERATOR);
				pathStr.append( verName);
				
				// Send the change notification
				
                getChangeHandler().notifyDirectoryChanged(NotifyChange.ActionRemoved, pathStr.toString());
			}
		}
	}
}
