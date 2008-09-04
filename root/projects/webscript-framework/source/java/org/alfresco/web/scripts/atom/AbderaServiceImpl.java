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
package org.alfresco.web.scripts.atom;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.alfresco.web.scripts.WebScriptException;
import org.apache.abdera.Abdera;
import org.apache.abdera.factory.ExtensionFactory;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.writer.Writer;
import org.springframework.beans.factory.InitializingBean;


/**
 * Abdera Service Implementation
 * 
 * @author davidc
 */
public class AbderaServiceImpl implements AbderaService, InitializingBean
{
    private Abdera abdera;
    private Parser parser;
    private Factory factory;
    private List<String> writerNames;
    private Map<String,Writer> writers;
    private Map<String, QName> qNames;
    

    /**
     * Set available Writer names
     * 
     * @param writerNames  list of writer names
     */
    public void setWriters(List<String> writerNames)
    {
        this.writerNames = writerNames;
    }
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet()
        throws Exception
    {
        // construct Abdera Service
        abdera = new Abdera();
        factory = abdera.getFactory();
        parser = factory.newParser();
        // TODO: parser options
        
        // construct writers
        writers = new HashMap<String, Writer>(writerNames == null ? 1 : writerNames.size() +1);
        writers.put(AbderaService.DEFAULT_WRITER, abdera.getWriter());
        if (writerNames != null)
        {
            for (String writerName : writerNames)
            {
                Writer writer = abdera.getWriterFactory().getWriter(writerName);
                if (writer == null)
                {
                    throw new WebScriptException("Failed to register Atom writer '" + writerName + "'; does not exist.");
                }
                writers.put(writerName, writer);
            }
        }
        
        // construct qnames
        qNames = new HashMap<String, QName>();
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#getAbdera()
     */
    public Abdera getAbdera()
    {
        return abdera;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#getParser()
     */
    public Parser getParser()
    {
        return parser;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#newEntry()
     */
    public Entry createEntry()
    {
        return factory.newEntry();
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#newFeed()
     */
    public Feed createFeed()
    {
        return factory.newFeed();
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#parse(java.io.InputStream, java.lang.String)
     */
    public Element parse(InputStream doc, String base)
    {
        Reader inputReader = new InputStreamReader(doc);
        return parse(inputReader, base);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#parse(java.io.Reader, java.lang.String)
     */
    public Element parse(Reader doc, String base)
    {
        Document<Element> entryDoc;
        if (base != null && base.length() > 0)
        {
            entryDoc = parser.parse(doc, base);
        }
        else
        {
            entryDoc = parser.parse(doc);
        }

        Element root = entryDoc.getRoot();
        return root;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#parseEntry(java.io.InputStream, java.lang.String)
     */
    public Entry parseEntry(InputStream doc, String base)
    {
        Reader inputReader = new InputStreamReader(doc);
        return parseEntry(inputReader, base);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#parseEntry(java.io.Reader, java.lang.String)
     */
    public Entry parseEntry(Reader doc, String base)
    {
        Element root = parse(doc, base);
        if (!Entry.class.isAssignableFrom(root.getClass()))
        {
            throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Expected Atom Entry, but recieved " + root.getClass());
        }
        
        return (Entry)root;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#parseFeed(java.io.Reader, java.lang.String)
     */
    public Feed parseFeed(InputStream doc, String base)
    {
        Reader inputReader = new InputStreamReader(doc);
        return parseFeed(inputReader, base);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#parseFeed(java.io.Reader, java.lang.String)
     */
    public Feed parseFeed(Reader doc, String base)
    {
        Element root = parse(doc, base);
        if (!Feed.class.isAssignableFrom(root.getClass()))
        {
            throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Expected Atom Feed, but recieved " + root.getClass());
        }
        
        return (Feed)root;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#getWriter(java.lang.String)
     */
    public Writer getWriter(String name)
    {
        return writers.get(name);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.atom.AbderaService#getQName(java.lang.String)
     */
    public Map<String, QName> getQNames()
    {
        return Collections.unmodifiableMap(qNames);
    }

    /**
     * Register QName
     * 
     * @param alias
     * @param qname
     */
    public void registerQName(String alias, String qname)
    {
        qNames.put(alias, QName.valueOf(qname));
    }

    /**
     * Register Extension Factory
     * @param extensionFactory
     */
    public void registerExtensionFactory(ExtensionFactory extensionFactory)
    {
        factory.registerExtension(extensionFactory);
    }
    
}
