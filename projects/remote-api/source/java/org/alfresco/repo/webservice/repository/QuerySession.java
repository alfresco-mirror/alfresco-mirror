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
package org.alfresco.repo.webservice.repository;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;

/**
 * Interface definition for a QuerySession.
 * 
 * @author gavinc
 */
public interface QuerySession extends Serializable
{
   /**
    * Retrieves the id this query session can be identified as
    * 
    * @return Id of this query session
    */
   public String getId();
   
   /**
    * Returns a QueryResult object representing the next batch of results.
    * QueryResult will contain a maximum of items as determined by the 
    * <code>fetchSize</code> element of the QueryConfiguration SOAP header.
    * 
    * When the last batch of results is being returned the querySession of
    * QueryResult will be null.
    * 
    * @see org.alfresco.repo.webservice.repository.QuerySession#getId()
    * @param searchService The SearchService to use for gathering the results
    * @param nodeService The NodeService to use for gathering the results
    * @return QueryResult containing the next batch of results or null if there
    * are no more results
    */
   public QueryResult getNextResultsBatch(SearchService searchService, NodeService nodeService);
}
