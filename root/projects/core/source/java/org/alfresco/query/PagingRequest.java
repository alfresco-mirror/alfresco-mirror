/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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
package org.alfresco.query;



/**
 * Simple wrapper for single page request (with optional request for total count up to a given max)
 * 
 * @author janv
 * @since 4.0
 */
public class PagingRequest
{
    private int skipCount = 0;
    private int maxItems;
    
    private int requestTotalCountMax = 0; // request total count up to a given max (0 => do not request total count)
    private String queryExecutionId;
    
    public PagingRequest(int maxItems, String queryExecutionId)
    {
        this.maxItems = maxItems;
        this.queryExecutionId = queryExecutionId;
    }
    
    public PagingRequest(int skipCount, int maxItems, String queryExecutionId)
    {
        this.skipCount = skipCount;
        this.maxItems = maxItems;
        this.queryExecutionId = queryExecutionId;
    }
    
    /**
     * Results to skip before retrieving the page.  Usually a multiple of page size (ie. page size * num pages to skip).
     * Default is 0.
     * 
     * @return
     */
    public int getSkipCount()
    {
        return skipCount;
    }
    
    /**
     * Change the skip count. Must be called before the paging query is run. 
     */
    protected void setSkipCount(int skipCount)
    {
        this.skipCount = skipCount;
    }
    
    /**
     * Size of the page - if skip count is 0 then return up to max items.
     * 
     * @return
     */
    public int getMaxItems()
    {
        return maxItems;
    }
    
    /**
     * Change the size of the page. Must be called before the paging query is run.
     */
    protected void setMaxItems(int maxItems)
    {
        this.maxItems = maxItems;
    }
    
    /**
     * Get requested total count (up to a given maximum).
     */
    public int getRequestTotalCountMax()
    {
        return requestTotalCountMax;
    }
    
    /**
     * Set request total count (up to a given maximum).  Default is 0 => do not request total count (which allows possible query optimisation).
     * 
     * @param requestTotalCountMax
     */
    public void setRequestTotalCountMax(int requestTotalCountMax)
    {
        this.requestTotalCountMax = requestTotalCountMax;
    }
    
    /**
     * Get a unique ID associated with these query results.  This must be available before and
     * after execution i.e. it must depend on the type of query and the query parameters
     * rather than the execution results.  Client has the option to pass this back as a hint when
     * paging.
     * 
     * @return                      a unique ID associated with the query execution results
     */
    public String getQueryExecutionId()
    {
        return queryExecutionId;
    }
    
    /**
     * Change the unique query ID for the results. Must be called before the paging query is run.
     */
    protected void setQueryExecutionId(String queryExecutionId)
    {
        this.queryExecutionId = queryExecutionId; 
    }
}