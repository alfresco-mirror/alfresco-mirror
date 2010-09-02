/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.wcm.client.controller;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.wcm.client.Asset;
import org.alfresco.wcm.client.AssetFactory;
import org.alfresco.wcm.client.ContentStream;
import org.alfresco.wcm.client.Rendition;
import org.alfresco.wcm.client.Resource;
import org.alfresco.wcm.client.impl.ContentStreamCmisRenditionImpl;
import org.alfresco.wcm.client.util.UrlUtils;
import org.alfresco.wcm.client.view.StreamedAssetView;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * StreamedAssetController uses an id in the url to look-up an asset in the
 * repository, eg an image. It then returns a view object which can render a
 * stream.
 * 
 * @author Chris Lack
 */
public class StreamedAssetController extends AbstractController
{
	private static final long EXPIRES = 300000L; // 5 mins in ms
	
    private static ThreadLocal<SimpleDateFormat> httpDateFormat = new ThreadLocal<SimpleDateFormat>() {};  	     

    private UrlUtils urlUtils;
    private AssetFactory assetFactory;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        // Get the asset Id from the url
        String uri = request.getPathInfo();
        String objectId = urlUtils.getAssetIdFromShortUrl(uri);
        String renditionName = request.getParameter("rendition");

        // Fetch the asset from the repository
        Asset asset = assetFactory.getAssetById(objectId);
        if (asset == null)
        {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        // Decide if the request should redirect to the full url
        String template = asset.getTemplate();
        if (template != null && renditionName == null)
        {
            String fullUri = urlUtils.getUrl(asset);
            RedirectView redirect = new RedirectView(fullUri, true, false);
            redirect.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return new ModelAndView(redirect);
        }

        // Set headers
        Date modifiedDate = ((Date) asset.getProperty(Resource.PROPERTY_MODIFIED_TIME));
        long modifiedTime = modifiedDate.getTime();
        modifiedTime = (modifiedTime / 1000) * 1000; // remove ms
        response.addDateHeader("Last-Modified", modifiedTime);
        response.addDateHeader("Expires", new Date().getTime() + EXPIRES); 
        String etag = Long.toHexString(modifiedTime);
        response.addHeader("ETag", etag);
        
        // Check if the asset has been changed since the last request
        String requestIfNoneMatch = request.getHeader("If-None-Match");
        if (requestIfNoneMatch != null)
        {
            if (etag.equals(requestIfNoneMatch))
            {
                response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                return null;
            }
        } 
        else
        {
            String requestIfModifiedSince = request.getHeader("If-Modified-Since");
            if (requestIfModifiedSince != null)
            {
                Date requestDate = getDateFromHttpDate(requestIfModifiedSince);
                if (requestDate.getTime() >= modifiedTime)
                {
                    response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    return null;
                }
            }
        }
        
        InputStream stream = null;
        String mimeType = null;
        
        // If a rendition is required then use the content stream of that
        if (renditionName != null) 
        {
        	Map<String,Rendition> renditions = asset.getRenditions();
        	Rendition rendition = renditions.get(renditionName);
        	if (rendition != null)
        	{
        		ContentStreamCmisRenditionImpl streamRendition = (ContentStreamCmisRenditionImpl)rendition;
        		stream = streamRendition.getStream();
        		mimeType = streamRendition.getMimeType();
        	}
        }
        
        // Else get the asset's content stream
        if (stream == null)
        {
        	ContentStream contentStream = asset.getContentAsInputStream();
        	if (contentStream != null) 
        	{
        		stream = contentStream.getStream();
        		mimeType = contentStream.getMimeType();
        	}
        }
        
        // Else no stream!
        if (stream == null)
        {
            return null;
        }
        
        // Return a StreamedAssetView to render the stream
        return new ModelAndView(new StreamedAssetView(stream, mimeType));
    }

    public String getHttpDate(Date date)
    {
        return dateFormatter().format(date);
    }

    public Date getDateFromHttpDate(String date) throws ParseException
    {
        return dateFormatter().parse(date);
    }
    
    /**
     * Get a date formatter for the thread as SimpleDateFormat is not thread-safe
     * @return
     */
    public SimpleDateFormat dateFormatter() 
    {
    	SimpleDateFormat formatter = httpDateFormat.get();
    	if (formatter == null)
    	{
    		formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    		httpDateFormat.set(formatter);
    	}
    	return formatter;
    }
 
    public void setUrlUtils(UrlUtils urlUtils)
    {
        this.urlUtils = urlUtils;
    }

    public void setAssetFactory(AssetFactory assetFactory)
    {
        this.assetFactory = assetFactory;
    }

}