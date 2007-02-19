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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm.hibernate;

import java.util.Date;
import java.util.List;

import org.alfresco.repo.avm.AVMNode;
import org.alfresco.repo.avm.AVMStore;
import org.alfresco.repo.avm.VersionRoot;
import org.alfresco.repo.avm.VersionRootDAO;
import org.hibernate.Query;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * This is the Hibernate version of the DAO for version roots.
 * @author britt
 */
class VersionRootDAOHibernate extends HibernateDaoSupport implements
        VersionRootDAO
{
    /**
     * Do nothing constructor.
     */
    public VersionRootDAOHibernate()
    {
        super();
    }
    
    /**
     * Save an unsaved VersionRoot.
     * @param vr The VersionRoot to save.
     */
    public void save(VersionRoot vr)
    {
        getSession().save(vr);
    }
    
    /**
     * Delete a VersionRoot.
     * @param vr The VersionRoot to delete.
     */
    public void delete(VersionRoot vr)
    {
        getSession().delete(vr);
        getSession().flush();
    }
    
    /**
     * Get all the version roots in a given store.
     * @param store The store.
     * @return A List of VersionRoots.  In id order.
     */
    @SuppressWarnings("unchecked")
    public List<VersionRoot> getAllInAVMStore(AVMStore store)
    {
        Query query = getSession().createQuery("from VersionRootImpl v where v.avmStore = :store order by v.versionID");
        query.setEntity("store", store);
        return (List<VersionRoot>)query.list();
    }
    
    /**
     * Get the version of a store by dates.
     * @param store The store.
     * @param from The starting date.  May be null but not with to null also.
     * @param to The ending date.  May be null but not with from null also.
     * @return A List of VersionRoots.
     */
    @SuppressWarnings("unchecked")
    public List<VersionRoot> getByDates(AVMStore store, Date from, Date to)
    {
        Query query;
        if (from == null)
        {
            query = 
                getSession().createQuery("from VersionRootImpl vr where vr.createDate <= :to " +
                                         "and vr.avmStore = :store " +
                                         "order by vr.versionID");
            query.setLong("to", to.getTime());
        }
        else if (to == null)
        {
            query =
                getSession().createQuery("from VersionRootImpl vr " +
                                         "where vr.createDate >= :from " +
                                         "and vr.avmStore = :store " +
                                         "order by vr.versionID");
            query.setLong("from", from.getTime());
        }
        else
        {
            query =
                getSession().createQuery("from VersionRootImpl vr "+ 
                                         "where vr.createDate between :from and :to " +
                                         "and vr.avmStore = :store " +
                                         "order by vr.versionID");
            query.setLong("from", from.getTime());
            query.setLong("to", to.getTime());
        }
        query.setEntity("store", store);
        return (List<VersionRoot>)query.list();
    }
    
    /**
     * Get the VersionRoot corresponding to the given id.
     * @param store The store
     * @param id The version id.
     * @return The VersionRoot or null if not found.
     */
    public VersionRoot getByVersionID(AVMStore store, int id)
    {
        Query query = getSession().getNamedQuery("VersionRoot.VersionByID");
        query.setEntity("store", store);
        query.setInteger("version", id);
        return (VersionRoot)query.uniqueResult();
    }

    /**
     * Get one from its root.
     * @param root The root to match.
     * @return The version root or null.
     */
    public VersionRoot getByRoot(AVMNode root)
    {
        Query query = getSession().createQuery("from VersionRootImpl vr " +
                                               "where vr.root = :root");
        query.setEntity("root", root);
        return (VersionRoot)query.uniqueResult();
    }

    /**
     * Get the highest numbered version in a repository.
     * @param rep The repository.
     * @return The highest numbered version.
     */
    public VersionRoot getMaxVersion(AVMStore rep)
    {
        Query query = getSession().createQuery("from VersionRootImpl vr " +
                                               "where vr.versionID = " +
                                               "(select max(v.versionID) from VersionRootImpl v)");
        return (VersionRoot)query.uniqueResult();
    }
    
    /**
     * Get the highest numbered id from all the versions in a store.
     * @param store The store.
     * @return The highest numbered id.
     */
    public Integer getMaxVersionID(AVMStore store)
    {
        Query query = getSession().createQuery("select max(vr.versionID) from VersionRootImpl vr " + 
                                               "where vr.avmStore = :store");
        query.setEntity("store", store);
        return (Integer)query.uniqueResult();
    }
}
