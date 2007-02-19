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
package org.alfresco.web.api.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigService;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.web.api.APIException;
import org.alfresco.web.api.APIRequest;
import org.alfresco.web.api.APIResponse;
import org.alfresco.web.api.FormatRegistry;
import org.alfresco.web.api.APIRequest.HttpMethod;
import org.alfresco.web.api.APIRequest.RequiredAuthentication;
import org.alfresco.web.app.servlet.HTTPProxy;
import org.alfresco.web.config.OpenSearchConfigElement;
import org.alfresco.web.config.OpenSearchConfigElement.EngineConfig;
import org.alfresco.web.config.OpenSearchConfigElement.ProxyConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.InitializingBean;


/**
 * Alfresco OpenSearch Proxy Service
 * 
 * Provides the ability to submit a request to a registered search engine
 * via the Alfresco server.
 * 
 * @author davidc
 */
public class SearchProxy extends APIServiceImpl implements InitializingBean
{
    // Logger
    private static final Log logger = LogFactory.getLog(SearchProxy.class);

    // dependencies
    protected FormatRegistry formatRegistry;
    protected ConfigService configService;
    protected OpenSearchConfigElement searchConfig; 
    protected String proxyPath;
    
    /**
     * @param formatRegistry
     */
    public void setFormatRegistry(FormatRegistry formatRegistry)
    {
        this.formatRegistry = formatRegistry;
    }

    /**
     * @param configService
     */
    public void setConfigService(ConfigService configService)
    {
        this.configService = configService;
    }
        
    /* (non-Javadoc)
     * @see org.alfresco.web.api.APIService#getRequiredAuthentication()
     */
    public RequiredAuthentication getRequiredAuthentication()
    {
        return APIRequest.RequiredAuthentication.None;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.api.APIService#getHttpMethod()
     */
    public HttpMethod getHttpMethod()
    {
        return APIRequest.HttpMethod.GET;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.api.APIService#getDefaultFormat()
     */
    public String getDefaultFormat()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.api.APIService#getDescription()
     */
    public String getDescription()
    {
        return "Issue an OpenSearch query via Alfresco";
    }
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception
    {
        Config config = configService.getConfig("OpenSearch");
        searchConfig = (OpenSearchConfigElement)config.getConfigElement(OpenSearchConfigElement.CONFIG_ELEMENT_ID);
        if (searchConfig == null)
        {
            throw new APIException("OpenSearch configuration not found");
        }
        ProxyConfig proxyConfig = searchConfig.getProxy();
        if (proxyConfig == null)
        {
            throw new APIException("OpenSearch proxy configuration not found");
        }
        proxyPath = proxyConfig.getUrl();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.api.APIService#execute(org.alfresco.web.api.APIRequest, org.alfresco.web.api.APIResponse)
     */
    public void execute(APIRequest req, APIResponse res)
        throws IOException
    {
        String extensionPath = req.getExtensionPath(this);
        String[] extensionPaths = extensionPath.split("/");
        if (extensionPaths.length != 2)
        {
            throw new APIException("OpenSearch engine has not been specified as /<engine>/<format>");
        }
        
        // retrieve search engine configuration
        String engine = extensionPaths[0];
        EngineConfig engineConfig = searchConfig.getEngine(engine);
        if (engineConfig == null)
        {
            throw new APIException("OpenSearch engine '" + engine + "' does not exist");
        }
        
        // retrieve engine url as specified by format
        String format = extensionPaths[1];
        String mimetype = formatRegistry.getMimeType(null, format);
        if (mimetype == null)
        {
            throw new APIException("Format '" + format + "' does not map to a registered mimetype");
        }
        Map<String, String> engineUrls = engineConfig.getUrls();
        String engineUrl = engineUrls.get(mimetype);
        if (engineUrl == null)
        {
            throw new APIException("Url mimetype '" + mimetype + "' does not exist for engine '" + engine + "'");
        }

        // replace template url arguments with actual arguments specified on request
        int engineUrlArgIdx = engineUrl.indexOf("?");
        if (engineUrlArgIdx != -1)
        {
            engineUrl = engineUrl.substring(0, engineUrlArgIdx);
        }
        if (req.getQueryString() != null)
        {
            engineUrl += "?" + req.getQueryString();
        }
        
        if (logger.isDebugEnabled())
            logger.debug("Mapping engine '" + engine + "' (mimetype '" + mimetype + "') to url '" + engineUrl + "'");        
        
        // issue request against search engine
        SearchEngineHttpProxy proxy = new SearchEngineHttpProxy(req.getPath(), engine, engineUrl, res);
        proxy.service();
    }
    
    /**
     * OpenSearch HTTPProxy
     * 
     * This proxy remaps OpenSearch links (e.g. previous, next) found in search results.
     * 
     * @author davidc
     */
    private class SearchEngineHttpProxy extends HTTPProxy
    {
        private final static String ATOM_NS_URI = "http://www.w3.org/2005/Atom";
        private final static String ATOM_NS_PREFIX = "atom";
        private final static String ATOM_LINK_XPATH = "atom:link[@rel=\"first\" or @rel=\"last\" or @rel=\"next\" or @rel=\"previous\" or @rel=\"self\" or @rel=\"alternate\"]";
        private String engine;
        private String rootPath;
        
        /**
         * Construct
         * 
         * @param requestUrl
         * @param response
         * @throws MalformedURLException
         */
        public SearchEngineHttpProxy(String rootPath, String engine, String engineUrl, HttpServletResponse response)
            throws MalformedURLException
        {
            super(engineUrl.startsWith("/") ? rootPath + engineUrl : engineUrl, response);
            this.engine = engine;
            this.rootPath = rootPath;
        }

        /* (non-Javadoc)
         * @see org.alfresco.web.app.servlet.HTTPProxy#writeResponse(java.io.InputStream, java.io.OutputStream)
         */
        @Override
        protected void writeResponse(InputStream input, OutputStream output)
            throws IOException
        {
            if (response.getContentType().startsWith(MimetypeMap.MIMETYPE_ATOM) ||
                response.getContentType().startsWith(MimetypeMap.MIMETYPE_RSS))
            {
                // Only post-process ATOM and RSS feeds
                // Replace all navigation links with "proxied" versions
                SAXReader reader = new SAXReader();
                try
                {
                    Document document = reader.read(input);
                    Element rootElement = document.getRootElement();
        
                    XPath xpath = rootElement.createXPath(ATOM_LINK_XPATH);
                    Map<String,String> uris = new HashMap<String,String>();
                    uris.put(ATOM_NS_PREFIX, ATOM_NS_URI);
                    xpath.setNamespaceURIs(uris);
        
                    List nodes = xpath.selectNodes(rootElement);
                    Iterator iter = nodes.iterator();
                    while (iter.hasNext())
                    {
                        Element element = (Element)iter.next();
                        Attribute hrefAttr = element.attribute("href");
                        String mimetype = element.attributeValue("type");
                        if (mimetype == null || mimetype.length() == 0)
                        {
                            mimetype = MimetypeMap.MIMETYPE_HTML;
                        }
                        String url = createUrl(engine, hrefAttr.getValue(), mimetype);
                        if (url.startsWith("/"))
                        {
                            url = rootPath + url;
                        }
                        hrefAttr.setValue(url);
                    }
                    
                    OutputFormat outputFormat = OutputFormat.createPrettyPrint();
                    XMLWriter writer = new XMLWriter(output, outputFormat);
                    writer.write(rootElement);
                    writer.flush();                
                }
                catch(DocumentException e)
                {
                    throw new IOException(e.toString());
                }
            }
            else
            {
                super.writeResponse(input, output);
            }
        }
    }
    
    /**
     * Construct a "proxied" search engine url
     * 
     * @param engine  engine name (as identified by <engine proxy="<name>">)
     * @param mimetype  url to proxy (as identified by mimetype)
     * @return  "proxied" url
     */
    public String createUrl(OpenSearchConfigElement.EngineConfig engine, String mimetype)
    {
        Map<String, String> urls = engine.getUrls();
        String url = urls.get(mimetype);
        if (url != null)
        {
            String proxy = engine.getProxy();
            if (proxy != null && !mimetype.equals(MimetypeMap.MIMETYPE_OPENSEARCH_DESCRIPTION))
            {
                url = createUrl(proxy, url, mimetype);
            }
        }
        return url;
    }
 
    /**
     * Construct a "proxied" search engine url
     * 
     * @param engine  engine name (as identified by <engine proxy="<name>">)
     * @param url  engine url
     * @param mimetype  mimetype of url
     * @return  "proxied" url
     */
    public String createUrl(String engine, String url, String mimetype)
    {
        String format = formatRegistry.getFormat(null, mimetype);
        if (format == null)
        {
            throw new APIException("Mimetype '" + mimetype + "' is not registered.");
        }
        
        String proxyUrl = null;
        int argIdx = url.indexOf("?");
        if (argIdx == -1)
        {
            proxyUrl = proxyPath + "/" + engine + "/" + format;  
        }
        else
        {
            proxyUrl = proxyPath + "/" + engine + "/" + format + url.substring(argIdx);  
        }
        return proxyUrl;
    }

}