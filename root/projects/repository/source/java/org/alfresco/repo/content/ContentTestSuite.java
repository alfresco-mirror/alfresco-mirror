/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.content;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.alfresco.repo.content.cleanup.ContentStoreCleanerTest;
import org.alfresco.repo.content.encoding.CharsetFinderTest;
import org.alfresco.repo.content.filestore.FileContentStoreTest;
import org.alfresco.repo.content.filestore.NoRandomAccessFileContentStoreTest;
import org.alfresco.repo.content.filestore.ReadOnlyFileContentStoreTest;
import org.alfresco.repo.content.replication.ContentStoreReplicatorTest;
import org.alfresco.repo.content.replication.ReplicatingContentStoreTest;

/**
 * Suite for content-related tests.
 * 
 * @author Derek Hulley
 */
public class ContentTestSuite extends TestSuite
{
    @SuppressWarnings("unchecked")
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        
        suite.addTestSuite(ContentStoreCleanerTest.class);
        suite.addTestSuite(CharsetFinderTest.class);
        suite.addTestSuite(FileContentStoreTest.class);
        suite.addTestSuite(NoRandomAccessFileContentStoreTest.class);
        suite.addTestSuite(ReadOnlyFileContentStoreTest.class);
        suite.addTestSuite(ContentStoreReplicatorTest.class);
        suite.addTestSuite(ReplicatingContentStoreTest.class);
        suite.addTestSuite(ContentDataTest.class);
        suite.addTestSuite(MimetypeMapTest.class);
        suite.addTestSuite(RoutingContentServiceTest.class);
        suite.addTestSuite(RoutingContentStoreTest.class);
        
        try
        {
            Class clazz = Class.forName("org.alfresco.repo.content.routing.StoreSelectorAspectContentStoreTest");
            suite.addTestSuite(clazz);
        }
        catch (Throwable e)
        {
            // Ignore
        }
        
                
        return suite;
    }
}
