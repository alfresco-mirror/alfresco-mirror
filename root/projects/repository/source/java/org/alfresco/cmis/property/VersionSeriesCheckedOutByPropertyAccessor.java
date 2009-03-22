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
package org.alfresco.cmis.property;

import java.io.Serializable;
import java.util.Collection;

import org.alfresco.cmis.dictionary.CMISMapping;
import org.alfresco.cmis.dictionary.CMISScope;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.querymodel.PredicateMode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

/**
 * Get the CMIS version series checked out by property
 * 
 * @author andyh
 */
public class VersionSeriesCheckedOutByPropertyAccessor extends AbstractNamedPropertyAccessor
{

    protected VersionSeriesCheckedOutByPropertyAccessor(CMISMapping cmisMapping, ServiceRegistry serviceRegistry)
    {
        super(cmisMapping, serviceRegistry, CMISScope.DOCUMENT, CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_BY);
    }

    public Serializable getProperty(NodeRef nodeRef)
    {
        if (getServiceRegistry().getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY))
        {
            return getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_WORKING_COPY_OWNER);
        }
        else
        {
            LockType type = getServiceRegistry().getLockService().getLockType(nodeRef);
            if (type == LockType.READ_ONLY_LOCK)
            {
                NodeRef wc = getServiceRegistry().getCheckOutCheckInService().getWorkingCopy(nodeRef);
                if (wc != null)
                {
                    return getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER);
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
    }
    
    public void setProperty(NodeRef nodeRef, Serializable value)
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneEquality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneEquality(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneExists(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.lang.Boolean)
     */
    public Query buildLuceneExists(LuceneQueryParser lqp, String propertyName, Boolean not) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneGreaterThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneGreaterThan(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneGreaterThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneGreaterThanOrEquals(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneIn(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.util.Collection, java.lang.Boolean,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneIn(LuceneQueryParser lqp, String propertyName, Collection<Serializable> values, Boolean not, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneInequality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneInequality(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneLessThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneLessThan(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneLessThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneLessThanOrEquals(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#buildLuceneLike(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      java.lang.String, java.io.Serializable, java.lang.Boolean)
     */
    public Query buildLuceneLike(LuceneQueryParser lqp, String propertyName, Serializable value, Boolean not) throws ParseException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.property.NamedPropertyAccessor#getLuceneSortField(java.lang.String)
     */
    public String getLuceneSortField(String propertyName)
    {
        throw new UnsupportedOperationException();
    }

}
