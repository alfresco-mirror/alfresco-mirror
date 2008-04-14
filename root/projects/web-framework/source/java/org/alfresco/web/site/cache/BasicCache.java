/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.web.site.cache;

import java.util.WeakHashMap;

/** 
 * This is an implementation of a purely in-memory cache that uses a
 * WeakHashMap to provide a basic form of caching.
 * 
 * @author muzquiano 
 */

public class BasicCache implements IContentCache
{
    protected BasicCache(long default_timeout)
    {
        m_default_timeout = default_timeout;
        m_cache = new WeakHashMap(256);
    }

    public synchronized Object get(String key)
    {
        // get the content item from the cache
        CacheItem item = (CacheItem) m_cache.get(key);
        debugLog("Cache lookup of '" + key + "' returns: " + item);

        // if the cache item is null, return right away
        if (item == null)
            return null;
        else
        {
            // ask the cache item if it's still valid
            if (item.isExpired())
            {
                debugLog("Cache item '" + key + "' expired.");

                // it's not valid, throw it away
                remove(key);
                return null;
            }
            else
                debugLog("Cache item '" + key + "' returned from cache.");

            // return this
            return item.m_object;
        }
    }

    public synchronized void remove(String key)
    {
        if (key == null)
            return;
        m_cache.remove(key);
    }

    public synchronized void put(String key, Object obj)
    {
        put(key, obj, m_default_timeout);
    }

    public synchronized void put(String key, Object obj, long timeout)
    {
        // create the cache item
        CacheItem item = new CacheItem(key, obj, timeout);
        m_cache.put(key, item);

        // debug feature
        debugLog("Cached '" + key + "' with timeout of " + timeout);
    }

    public void invalidateAll()
    {
        m_cache.clear();
    }

    protected WeakHashMap m_cache;
    protected long m_default_timeout;

    public boolean isReporting()
    {
        return m_bReporting;
    }

    public void setReporting(boolean b)
    {
        m_bReporting = b;
    }

    protected String getReportTitle()
    {
        return "[Cache Thread " + Thread.currentThread().getName() + "]";
    }

    protected void debugLog(String str)
    {
        //System.out.println(str);
    }

    private boolean m_bReporting;
}
