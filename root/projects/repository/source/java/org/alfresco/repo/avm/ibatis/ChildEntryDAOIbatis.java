/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm.ibatis;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.avm.AVMDAOs;
import org.alfresco.repo.avm.AVMNode;
import org.alfresco.repo.avm.ChildEntry;
import org.alfresco.repo.avm.ChildEntryDAO;
import org.alfresco.repo.avm.ChildEntryImpl;
import org.alfresco.repo.avm.ChildKey;
import org.alfresco.repo.avm.DirectoryNode;
import org.alfresco.repo.domain.avm.AVMChildEntryEntity;

/**
 * iBATIS DAO wrapper for ChildEntry
 * 
 * @author jan
 */
class ChildEntryDAOIbatis implements ChildEntryDAO
{
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#save(org.alfresco.repo.avm.ChildEntry)
     */
    public void save(ChildEntry entry)
    {
        AVMDAOs.Instance().newAVMNodeLinksDAO.createChildEntry(entry.getKey().getParent().getId(), entry.getKey().getName(), entry.getChild().getId());
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#get(org.alfresco.repo.avm.ChildKey)
     */
    public ChildEntry get(ChildKey key)
    {
        AVMChildEntryEntity childEntryEntity = AVMDAOs.Instance().newAVMNodeLinksDAO.getChildEntry(key.getParent().getId(), key.getName());
        return getChildEntryForParent(key.getParent(), childEntryEntity);
        
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#getByParent(org.alfresco.repo.avm.DirectoryNode, java.lang.String)
     */
    public List<ChildEntry> getByParent(DirectoryNode parent, String childNamePattern)
    {
        // TODO - add option for childNamePattern
        
        List<AVMChildEntryEntity> childEntryEntities = AVMDAOs.Instance().newAVMNodeLinksDAO.getChildEntriesByParent(parent.getId());
        
        List<ChildEntry> result = new ArrayList<ChildEntry>(childEntryEntities.size());
        for (AVMChildEntryEntity childEntryEntity : childEntryEntities)
        {
            result.add(getChildEntryForParent(parent, childEntryEntity));
        }
        
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#getByParentChild(org.alfresco.repo.avm.DirectoryNode, org.alfresco.repo.avm.AVMNode)
     */
    public boolean existsParentChild(DirectoryNode parent, AVMNode child)
    {
        AVMChildEntryEntity childEntryEntity = AVMDAOs.Instance().newAVMNodeLinksDAO.getChildEntry(parent.getId(), child.getId());
        return (childEntryEntity != null);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#getByChild(org.alfresco.repo.avm.AVMNode)
     */
    public List<ChildEntry> getByChild(AVMNode child)
    {
        List<AVMChildEntryEntity> childEntryEntities = AVMDAOs.Instance().newAVMNodeLinksDAO.getChildEntriesByChild(child.getId());
        
        List<ChildEntry> result = new ArrayList<ChildEntry>(childEntryEntities.size());
        for (AVMChildEntryEntity childEntryEntity : childEntryEntities)
        {
            result.add(getChildEntryForChild(child, childEntryEntity));
        }
        
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#update(org.alfresco.repo.avm.ChildEntry)
     */
    public void update(ChildEntry child)
    {
        // NOOP
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#delete(org.alfresco.repo.avm.ChildEntry)
     */
    public void delete(ChildEntry child)
    {
        AVMChildEntryEntity childEntryEntity = new AVMChildEntryEntity();
        childEntryEntity.setParentNodeId(child.getKey().getParent().getId());
        childEntryEntity.setName(child.getKey().getName());
        childEntryEntity.setChildNodeId(child.getChild().getId());
        
        AVMDAOs.Instance().newAVMNodeLinksDAO.deleteChildEntry(childEntryEntity);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#deleteByParent(org.alfresco.repo.avm.AVMNode)
     */
    public void deleteByParent(AVMNode parent)
    {
        AVMDAOs.Instance().newAVMNodeLinksDAO.deleteChildEntriesByParent(parent.getId());
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.ChildEntryDAO#evict(org.alfresco.repo.avm.ChildEntry)
     */
    public void evict(ChildEntry entry)
    {
        // NOOP
    }
    
    private ChildEntry getChildEntryForParent(DirectoryNode parentNode, AVMChildEntryEntity childEntryEntity)
    {
        if (childEntryEntity == null)
        {
            return null;
        }
        
        AVMNode childNode = AVMDAOs.Instance().fAVMNodeDAO.getByID(childEntryEntity.getChildId());
        
        ChildEntry ce = new ChildEntryImpl(new ChildKey(parentNode, childEntryEntity.getName()), childNode);
        ce.setKey(new ChildKey(parentNode, childEntryEntity.getName()));
        ce.setChild(childNode);
        return ce;
    }
    
    private ChildEntry getChildEntryForChild(AVMNode childNode, AVMChildEntryEntity childEntryEntity)
    {
        if (childEntryEntity == null)
        {
            return null;
        }
        
        DirectoryNode parentNode = (DirectoryNode)AVMDAOs.Instance().fAVMNodeDAO.getByID(childEntryEntity.getParentNodeId());
        
        ChildEntry ce = new ChildEntryImpl(new ChildKey(parentNode, childEntryEntity.getName()), childNode);
        ce.setKey(new ChildKey(parentNode, childEntryEntity.getName()));
        ce.setChild(childNode);
        return ce;
    }
}
