/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.filesys.server.core;

/**
 * <p>
 * This exception may be thrown by a SharedDevice when the device interface has not been specified,
 * the device interface does not match the shared device type, or the device interface driver class
 * cannot be loaded.
 */
public class InvalidDeviceInterfaceException extends Exception
{
    private static final long serialVersionUID = 3834029177581222198L;

    /**
     * InvalidDeviceInterfaceException constructor.
     */
    public InvalidDeviceInterfaceException()
    {
        super();
    }

    /**
     * InvalidDeviceInterfaceException constructor.
     * 
     * @param s java.lang.String
     */
    public InvalidDeviceInterfaceException(String s)
    {
        super(s);
    }
}