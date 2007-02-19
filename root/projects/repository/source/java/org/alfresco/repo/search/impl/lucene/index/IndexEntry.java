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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.search.impl.lucene.index;

/**
 * Describes an entry in an index
 * 
 * @author Andy Hind
 */
class IndexEntry
{
    /**
     * The type of the index entry
     */
    private IndexType type;

    /**
     * The unique name of the index entry
     */
    private String name;
    
    /**
     * The preceeding index name.
     * Allows deltas etc to apply to the index or an overlay for example. 
     */
    private String parentName;

    /**
     * The status of the index entry
     */
    private TransactionStatus status;
    
    /**
     * If merging, the id where the result is going
     */
    private String mergeId;
    
    private long documentCount; 
    
    private long deletions;
    
    private boolean deletOnlyNodes;
    
    IndexEntry(IndexType type, String name,  String parentName, TransactionStatus status, String mergeId, long documentCount, long deletions, boolean deletOnlyNodes)
    {
        this.type = type;
        this.name = name;
        this.parentName = parentName;
        this.status = status;
        this.mergeId = mergeId;
        this.documentCount = documentCount;
        this.deletions = deletions;
        this.deletOnlyNodes = deletOnlyNodes;
    }

    public String getMergeId()
    {
        return mergeId;
    }

    public void setMergeId(String mergeId)
    {
        this.mergeId = mergeId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getParentName()
    {
        return parentName;
    }

    public void setParentName(String parentName)
    {
        this.parentName = parentName;
    }

    public TransactionStatus getStatus()
    {
        return status;
    }

    public void setStatus(TransactionStatus status)
    {
        this.status = status;
    }

    public IndexType getType()
    {
        return type;
    }

    public void setType(IndexType type)
    {
        this.type = type;
    }

    public long getDocumentCount()
    {
        return documentCount;
    }

    public void setDocumentCount(long documentCount)
    {
        this.documentCount = documentCount;
    }
    
    public long getDeletions()
    {
        return deletions;
    }

    public void setDeletions(long deletions)
    {
        this.deletions = deletions;
    }

    public boolean isDeletOnlyNodes()
    {
        return deletOnlyNodes;
    }

    public void setDeletOnlyNodes(boolean deletOnlyNodes)
    {
        this.deletOnlyNodes = deletOnlyNodes;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(" Name=").append(getName()).append(" ");
        builder.append("Type=").append(getType()).append(" ");
        builder.append("Status=").append(getStatus()).append(" ");
        builder.append("Docs=").append(getDocumentCount()).append(" ");
        builder.append("Deletions=").append(getDeletions()).append(" ");
        return builder.toString();
    }
    
    
}