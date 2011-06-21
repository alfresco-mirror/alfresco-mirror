/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.module.org_alfresco_module_dod5015.test;

import org.alfresco.module.org_alfresco_module_dod5015.test.service.DispositionServiceImplTest;
import org.alfresco.module.org_alfresco_module_dod5015.test.service.RecordsManagementAdminServiceImplTest;
import org.alfresco.module.org_alfresco_module_dod5015.test.service.RecordsManagementSearchServiceImplTest;
import org.alfresco.module.org_alfresco_module_dod5015.test.service.RecordsManagementServiceImplTest;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * RM test suite
 * 
 * @author Roy Wetherall
 */
public class ServicesTestSuite extends TestSuite
{
    /**
     * Creates the test suite
     * 
     * @return  the test suite
     */
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(RecordsManagementServiceImplTest.class);
        suite.addTestSuite(DispositionServiceImplTest.class);
        //suite.addTestSuite(RecordsManagementActionServiceImplTest.class);
        suite.addTestSuite(RecordsManagementAdminServiceImplTest.class);
        //suite.addTestSuite(RecordsManagementAuditServiceImplTest.class);
        //suite.addTestSuite(RecordsManagementEventServiceImplTest.class);
        //suite.addTestSuite(RecordsManagementSecurityServiceImplTest.class);
        suite.addTestSuite(RecordsManagementSearchServiceImplTest.class);
        return suite;
    }
}
