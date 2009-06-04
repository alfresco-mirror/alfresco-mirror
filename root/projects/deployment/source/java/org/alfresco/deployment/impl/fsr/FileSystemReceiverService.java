/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * and Open Source Software ("FLOSS") applications as described in Alfresco's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing
 */

package org.alfresco.deployment.impl.fsr;

public interface FileSystemReceiverService 
{
	/**
	 * Get the directory in which metadata is stored
	 * @return the metaData directory
	 */
	public String getMetaDataDirectory();

	/**
	 * Get the directory to which log (as in journal) files will be written.
	 * @return the logDirectory
	 */
	public String getLogDirectory();

	/**
	 * Get the directory to which work phase files get written.
	 * @return the data directory
	 */
	public String getDataDirectory();
	
	/**
	 * Should an error be generated on overwriting content ?
	 */
	public boolean isErrorOnOverwrite();
	
	/**
	 *  Queue a command object for execution. 
	 */
	public void queueCommand(Runnable command);
	
	/**
	 * poll a queued command.
	 * @return the command or null if there is no command on the queue
	 */
	public Runnable pollCommand();

}
