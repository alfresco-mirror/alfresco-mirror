/*
 * Copyright (C) 2005 Alfresco, Inc.
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
 */package org.alfresco.repo.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.view.Exporter;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.debug.NodeStoreInspector;


public class ExporterComponentTest extends BaseSpringTest
{

    private ExporterService exporterService;
    private StoreRef storeRef;

    
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        exporterService = (ExporterService)applicationContext.getBean("exporterComponent");
        
        // Create the store
        this.storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    }

    
    public void testExport()
        throws Exception
    {
        TestProgress testProgress = new TestProgress();
        Location location = new Location(storeRef);
        
        File tempFile = TempFileProvider.createTempFile("xmlexporttest", ".xml");
        OutputStream output = new FileOutputStream(tempFile);
        exporterService.exportView(output, location, true, testProgress);
        output.close();
        System.out.println(NodeStoreInspector.dumpNodeStore((NodeService)applicationContext.getBean("NodeService"), storeRef));
    }

    
    private static class TestProgress
        implements Exporter
    {

        public void start()
        {
            System.out.println("TestProgress: start");
        }

        public void startNamespace(String prefix, String uri)
        {
            System.out.println("TestProgress: start namespace prefix = " + prefix + " uri = " + uri);
        }

        public void endNamespace(String prefix)
        {
            System.out.println("TestProgress: end namespace prefix = " + prefix);
        }

        public void startNode(NodeRef nodeRef)
        {
//            System.out.println("TestProgress: start node " + nodeRef);
        }

        public void endNode(NodeRef nodeRef)
        {
//            System.out.println("TestProgress: end node " + nodeRef);
        }

        public void startAspect(NodeRef nodeRef, QName aspect)
        {
//            System.out.println("TestProgress: start aspect " + aspect);
        }

        public void endAspect(NodeRef nodeRef, QName aspect)
        {
//            System.out.println("TestProgress: end aspect " + aspect);
        }

        public void startProperty(NodeRef nodeRef, QName property)
        {
//            System.out.println("TestProgress: start property " + property);
        }

        public void endProperty(NodeRef nodeRef, QName property)
        {
//            System.out.println("TestProgress: end property " + property);
        }

        public void value(NodeRef nodeRef, QName property, Object value)
        {
//            System.out.println("TestProgress: value " + value);
        }

        public void content(NodeRef nodeRef, QName property, InputStream content)
        {
//            System.out.println("TestProgress: content stream ");
        }

        public void startAssoc(NodeRef nodeRef, QName assoc)
        {
//            System.out.println("TestProgress: start association " + assocDef.getName());
        }

        public void endAssoc(NodeRef nodeRef, QName assoc)
        {
//            System.out.println("TestProgress: end association " + assocDef.getName());
        }

        public void warning(String warning)
        {
            System.out.println("TestProgress: warning " + warning);   
        }

        public void end()
        {
            System.out.println("TestProgress: end");
        }
    }
    
}
