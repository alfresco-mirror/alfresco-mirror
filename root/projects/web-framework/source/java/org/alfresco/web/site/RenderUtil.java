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
package org.alfresco.web.site;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.tools.FakeHttpServletResponse;
import org.alfresco.tools.WrappedHttpServletRequest;
import org.alfresco.tools.WrappedHttpServletResponse;
import org.alfresco.web.site.exception.ComponentChromeRenderException;
import org.alfresco.web.site.exception.ComponentRenderException;
import org.alfresco.web.site.exception.JspRenderException;
import org.alfresco.web.site.exception.PageRenderException;
import org.alfresco.web.site.exception.RegionRenderException;
import org.alfresco.web.site.exception.RequestDispatchException;
import org.alfresco.web.site.exception.TemplateRenderException;
import org.alfresco.web.site.model.Chrome;
import org.alfresco.web.site.model.Component;
import org.alfresco.web.site.model.Configuration;
import org.alfresco.web.site.model.Page;
import org.alfresco.web.site.model.TemplateInstance;
import org.alfresco.web.site.renderer.Renderable;
import org.alfresco.web.site.renderer.RendererContext;
import org.alfresco.web.site.renderer.RendererContextHelper;
import org.alfresco.web.site.renderer.RendererDescriptor;
import org.alfresco.web.site.renderer.RendererFactory;

/**
 * @author muzquiano
 */
public class RenderUtil
{
    /**
     * Renders a given JSP page.
     * 
     * This wraps the JSP rendering in servlet wrappers and will
     * do variable substitution on HEAD tags.
     * 
     * This method should really only be used for top-level page
     * elements (i.e. the first dispatch to a JSP page).
     * 
     * If you use it for downstream JSP includes, it will work fine,
     * but it will be less efficient.  For each call to this method,
     * there exists some extra overhead for the wrapping/unwrapping
     * of servlet objects and substitution within response text.
     * 
     * @param context
     * @param request
     * @param response
     * @param dispatchPath
     * @throws Exception
     */
    public static void renderJspPage(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String dispatchPath) throws JspRenderException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderJspPage-" + dispatchPath);
        
        // bind the rendering to this page
        RendererContextHelper.bind(context, request, response);
        
        try
        {
            String renderer = dispatchPath;
            String rendererType = WebFrameworkConstants.RENDERER_TYPE_JSP;
            
            executePageRenderer(context, request, response, rendererType, renderer);
        }
        catch (Exception ex)
        {
            throw new JspRenderException("Unable to render JSP page", ex);
        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);
            
            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderJspPage-" + dispatchPath);
        }
    }

    /**
     * Renders the current page instance.

     */
    public static void renderPage(RequestContext context,
            HttpServletRequest request, HttpServletResponse response)
            throws PageRenderException
    {
        Page page = context.getCurrentPage();
        if (page == null)
        {
            throw new PageRenderException(
                    "Unable to locate current page in request context");
        }
        
        renderPage(context, request, response, page.getId());
    }
    
    /**
     * Renders a given page instance
     */
    public static void renderPage(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String pageId) throws PageRenderException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderPage-" + pageId);
        
        // look up the page
        Page page = (Page) context.getModel().loadPage(context, pageId);
        if (page == null)
        {
            throw new PageRenderException("Unable to locate page: " + pageId);
        }

        // look up the page template
        TemplateInstance currentTemplate = page.getTemplate(context);
        if (currentTemplate == null)
        {
            throw new PageRenderException(
                    "Unable to locate template for page: " + pageId);
        }
        
        // render the template
        try
        {
            // bind the rendering to this page
            RendererContextHelper.bind(context, page, request, response);
            
            // Wrap the Request and Response
            WrappedHttpServletRequest wrappedRequest = new WrappedHttpServletRequest(request);
            WrappedHttpServletResponse wrappedResponse = new WrappedHttpServletResponse(response);

            // Execute the template        
            RenderUtil.renderTemplate(context, wrappedRequest, wrappedResponse, currentTemplate.getId());

            // At this point, the template and all of the components
            // have executed.  We must now stamp the <!--${head}-->
            // onto the output.  To do so, we must first generate
            // the stamp.        
            String headTags = generateHeader(context, request, response);

            // Now do a replace on all of the stamp placeholders
            String responseBody = wrappedResponse.getOutput();
            int i = -1;
            do
            {
                i = responseBody.indexOf(WebFrameworkConstants.PAGE_HEAD_DEPENDENCIES_STAMP);
                if (i > -1)
                {
                    responseBody = responseBody.substring(0, i) + headTags + responseBody.substring(
                            i + WebFrameworkConstants.PAGE_HEAD_DEPENDENCIES_STAMP.length(),
                            responseBody.length());
                }
            }
            while (i > -1);

            // Finally, commit the entire thing to the output stream
            response.getWriter().print(responseBody);
        }
        catch (Exception ex)
        {
            throw new PageRenderException(
                    "An exception occurred while rendering page: " + page.getId(),
                    ex);
        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);

            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderPage-" + page.getId());
        }
    }

    /**
     * Renders a given template instance.  This fetches the abstract renderer
     * instance for the given template's type and then binds configuration data
     * to the rendering engine.  It then executes the template.
     * 
     * @param context
     * @param request
     * @param response
     * @param templateId
     * @throws Exception
     */
    public static void renderTemplate(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String templateId) throws TemplateRenderException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderTemplate-" + templateId);

        TemplateInstance template = (TemplateInstance) context.getModel().loadTemplate(context,
                templateId);
        if (template == null)
        {
            throw new TemplateRenderException(
                    "Unable to locate template: " + templateId);
        }
        
        try
        {
            // bind the rendering to this template
            RendererContext rendererContext = RendererContextHelper.bind(context, template, request, response);
            
            // get the renderer and execute it
            Renderable renderer = RendererFactory.newRenderer(context, template);
            renderer.execute(rendererContext);
        }
        catch (Exception ex)
        {
            throw new TemplateRenderException(
                    "An exception occurred while rendering template: " + templateId,
                    ex);
        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);
            
            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderTemplate-" + templateId);            
        }
    }

    /**
     * Renders a region for a given template.
     *  
     * @param context
     * @param request
     * @param response
     * @param templateId
     * @param regionId
     * @param regionScopeId
     * @throws Exception
     */    
    public static void renderRegion(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String templateId, String regionId, String regionScopeId)
            throws RegionRenderException
    {
        renderRegion(context, request, response, templateId, regionId, regionScopeId, null);
    }

    /**
     * Renders a region for a given template.
     *  
     * @param context
     * @param request
     * @param response
     * @param templateId
     * @param regionId
     * @param regionScopeId
     * @throws Exception
     */
    public static void renderRegion(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String templateId, String regionId, String regionScopeId,
            String overrideChromeId)
            throws RegionRenderException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderRegion-" + templateId+"-"+regionId+"-"+regionScopeId);
        
        // get the template
        TemplateInstance template = (TemplateInstance) context.getModel().loadTemplate(context,
                templateId);
        if (template == null)
        {
            throw new RegionRenderException(
                    "Unable to locate template: " + templateId);
        }

        try
        {
            // bind the rendering to this template
            RendererContext rendererContext = RendererContextHelper.bind(context, template, request, response);
            
            // regions have to set by hand (not auto populated)
            String regionSourceId = getSourceId(context, regionScopeId);
            rendererContext.put(WebFrameworkConstants.RENDER_DATA_REGION_ID, regionId);
            rendererContext.put(WebFrameworkConstants.RENDER_DATA_REGION_SCOPE_ID, regionScopeId);
            rendererContext.put(WebFrameworkConstants.RENDER_DATA_REGION_SOURCE_ID, regionSourceId);

            // determine the region renderer
            RendererDescriptor descriptor = getRegionRendererDescriptor(context, template, regionId, overrideChromeId);            
            
            // render in either one of two ways
            // if there is a component bound, then continue processing downstream
            // if not, then render a "no component" screen
            Component[] components = ModelUtil.findComponents(context,
                    regionScopeId, regionSourceId, regionId, null);
            if (components.length > 0)
            {
                // merge in component to render data
                RendererContext compRenderData = RendererContextHelper.generate(context, components[0]);
                rendererContext.putAll(compRenderData);
                
                // execute renderer
                RenderUtil.executeRenderer(context, request, response, descriptor);
            }
            else
            {
                // if we couldn't find a component, then redirect to a
                // region "no-component" renderer
                RenderUtil.renderErrorHandlerPage(context, request, 
                        response, 
                        WebFrameworkConstants.DISPATCHER_HANDLER_REGION_NO_COMPONENT,
                        WebFrameworkConstants.DEFAULT_DISPATCHER_HANDLER_REGION_NO_COMPONENT  );
            }
        }
        catch (Exception ex)
        {
            throw new RegionRenderException(
                    "Unable to render region: " + regionId, ex);
        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);
            
            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderRegion-" + templateId+"-"+regionId+"-"+regionScopeId);
        }
    }

    /**
     * Renders a given component with default chrome.
     * 
     * If you would like to render a component without chrome, then
     * see the renderRawComponent method.
     * 
     * @param context
     * @param request
     * @param response
     * @param componentId
     * @throws Exception
     */
    public static void renderComponent(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String componentId) throws ComponentRenderException, ComponentChromeRenderException
    {
        renderComponent(context, request, response, componentId, null);
    }

    /**
     * Renders a given component with the given chrome.
     * 
     * If you would like to render a component without chrome, then
     * see the renderRawComponent method.
     * 
     * @param context
     * @param request
     * @param response
     * @param componentId
     * @throws Exception
     */    
    public static void renderComponent(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String componentId, String overrideChromeId) throws ComponentChromeRenderException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderComponent-" + componentId);
        
        Component component = context.getModel().loadComponent(context,
                componentId);
        if (component == null)
        {
            throw new ComponentChromeRenderException(
                    "Unable to locate component: " + componentId);
        }

        try
        {
            // bind the rendering to this component
            RendererContextHelper.bind(context, component, request, response);
            
            // determine the component renderer
            RendererDescriptor descriptor = getComponentRendererDescriptor(context, component, overrideChromeId);
            
            // execute
            RenderUtil.executeRenderer(context, request, response, descriptor);
        }
        catch (Exception ex)
        {
            throw new ComponentChromeRenderException(
                    "Unable to render component chrome: " + overrideChromeId + " with component: " + componentId, ex);
        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);
            
            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderComponent-" + componentId);
        }
    }
    
    /**
     * Renders a given component instance.  This fetches the abstract renderer
     * instance for the given component's type and then binds configuration data
     * to the rendering engine.  It then executes the component.
     * 
     * @param context
     * @param request
     * @param response
     * @param componentId
     * @throws Exception
     */
    public static void renderRawComponent(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String componentId) throws ComponentRenderException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderRawComponent-" + componentId);
        
        Component component = context.getModel().loadComponent(context,
                componentId);
        if (component == null)
        {
            throw new ComponentRenderException(
                    "Unable to locate component: " + componentId);
        }

        try
        {
            // bind the rendering to this component
            RendererContext rendererContext = RendererContextHelper.bind(context, component, request, response);
            
            // build a renderer for this component
            Renderable renderer = RendererFactory.newRenderer(context,
                    component);
            renderer.execute(rendererContext);
        }
        catch (Exception ex)
        {
            throw new ComponentRenderException(
                    "An exception occurred while rendering component: " + componentId,
                    ex);

        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);
            
            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderRawComponent-" + componentId);
        }
    }

    /**
     * Renders the fully formed URL string fo
     * @param context
     * @param request
     * @param response
     * @param objectId
     * @param formatId
     */
    public static void page(RequestContext context, HttpServletRequest request,
            HttpServletResponse response, String pageId, String formatId, String objectId)
    {
        String url = context.getLinkBuilder().page(context, pageId, formatId);
        if (url != null)
        {
            try
            {
                response.getWriter().write(url);
            }
            catch (Exception ex)
            {
                Framework.getLogger().error(ex);
            }
        }
    }

    public static void content(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String objectId, String formatId)
    {
        String pageId = context.getCurrentPage().getId();
        String url = context.getLinkBuilder().page(context, pageId, formatId);
        if (url != null)
        {
            try
            {
                response.getWriter().write(url);
            }
            catch (Exception ex)
            {
                Framework.getLogger().error(ex);
            }
        }
    }

    public static void appendHeadTags(RequestContext context, String tags)
    {
        getHeadTags(context).add(tags);
    }

    public static List getHeadTags(RequestContext context)
    {
        List list = (List) context.getValue(RequestContext.VALUE_HEAD_TAGS);
        if (list == null)
        {
            list = new ArrayList(64);
            context.setValue(RequestContext.VALUE_HEAD_TAGS, list);
        }
        return list;
    }

    public static String renderScriptImport(RequestContext context, String src)
    {
        if (context instanceof HttpRequestContext)
        {
            HttpServletRequest request = (HttpServletRequest) ((HttpRequestContext) context).getRequest();
            return renderScriptImport(request, src);
        }
        return null;
    }

    public static String renderScriptImport(HttpServletRequest request,
            String src)
    {
        return renderScriptImport(request, src, true);
    }

    public static String renderScriptImport(HttpServletRequest request,
            String src, boolean includeQueryString)
    {
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 4)
            src = src + "?" + queryString;

        // make sure references resolve to the configured servlet
        src = URLUtil.browser(request, src);

        return "<script type=\"text/javascript\" src=\"" + src + "\"></script>";
    }

    public static String renderLinkImport(RequestContext context, String href)
    {
        if (context instanceof HttpRequestContext)
        {
            HttpServletRequest request = (HttpServletRequest) ((HttpRequestContext) context).getRequest();
            return renderLinkImport(request, href, null, true);
        }
        return null;
    }

    public static String renderLinkImport(RequestContext context, String href,
            String id)
    {
        if (context instanceof HttpRequestContext)
        {
            HttpServletRequest request = (HttpServletRequest) ((HttpRequestContext) context).getRequest();
            return renderLinkImport(request, href, id, true);
        }
        return null;
    }

    public static String renderLinkImport(HttpServletRequest request,
            String href)
    {
        return renderLinkImport(request, href, null, true);
    }

    public static String renderLinkImport(HttpServletRequest request,
            String href, String id, boolean includeQueryString)
    {
        if (includeQueryString)
        {
            String queryString = request.getQueryString();
            if (queryString != null && queryString.length() > 4)
                href = href + "?" + queryString;
        }

        // make sure references resolve to the configured servlet
        href = URLUtil.browser(request, href);

        String value = "<link ";
        if (id != null)
        {
            value += "id=\"" + id + "\" ";
        }
        value += "rel=\"stylesheet\" type=\"text/css\" href=\"" + href + "\"></link>";

        return value;
    }

    protected static String getSourceId(RequestContext context, String scopeId)
    {
        // rendering objects
        Page page = context.getCurrentPage();
        TemplateInstance template = context.getCurrentTemplate();

        // get the component association in that scope
        String sourceId = null;
        if (WebFrameworkConstants.REGION_SCOPE_GLOBAL.equalsIgnoreCase(scopeId))
        {
            sourceId = WebFrameworkConstants.REGION_SCOPE_GLOBAL;
        }
        if (WebFrameworkConstants.REGION_SCOPE_TEMPLATE.equalsIgnoreCase(scopeId))
        {
            sourceId = template.getId();
        }
        if (WebFrameworkConstants.REGION_SCOPE_PAGE.equalsIgnoreCase(scopeId))
        {
            sourceId = page.getId();
        }

        return sourceId;
    }

    // TODO: Introduce some caching for this
    protected static String generateHeader(RequestContext context, HttpServletRequest request, HttpServletResponse response)
        throws Exception
    {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append("\r\n");
        buffer.append(WebFrameworkConstants.WEB_FRAMEWORK_SIGNATURE);
        buffer.append("\r\n");

        /**
         * This is a work in progress.  Still not sure what the best
         * way is to define a "global" include.
         * 
         * With this approach, allow a global.head.renderer.xml file
         * to live as a Configuration object.
         * 
         * If this file is available, it is automatically read
         * and the renderer described therein is executed.
         */
        Configuration config = context.getModel().loadConfiguration(context, "global.head.renderer");
        if(config != null)
        {
            // renderer properties
            String rendererType = config.getProperty("renderer-type");
            String renderer = config.getProperty("renderer");
            
            // execute renderer
            String tags = processRenderer(context, request, response, rendererType, renderer);
            buffer.append(tags);
        }
        
        // Now import the stuff that the components on the page needed us to import
        List tagsList = RenderUtil.getHeadTags(context);
        for (int i = 0; i < tagsList.size(); i++)
        {
            String tags = (String) tagsList.get(i);
            buffer.append(tags);
        }

        return buffer.toString();
    }

    protected static void print(HttpServletResponse response, String str)
            throws IOException
    {
        response.getWriter().print(str);
    }
        
    public static String processRenderer(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            RendererDescriptor descriptor) throws Exception
    {
        return processRenderer(context, request, response, descriptor.getRendererType(), descriptor.getRenderer());
    }
    
    public static String processRenderer(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String rendererType, String renderer) throws Exception
    {
        // wrap the request and response
        WrappedHttpServletRequest wrappedRequest = new WrappedHttpServletRequest(
                request);
        FakeHttpServletResponse fakeResponse = new FakeHttpServletResponse();
        
        // execute
        executeRenderer(context, wrappedRequest, fakeResponse, rendererType, renderer);
                
        // return the result
        return fakeResponse.getContentAsString();        
    }

    public static void executeRenderer(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            RendererDescriptor descriptor) throws Exception
    {
        executeRenderer(context, request, response, descriptor.getRendererType(), descriptor.getRenderer());
    }
    
    public static void executeRenderer(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String rendererType, String renderer) throws Exception
    {
        // grab the renderer config
        RendererContext rendererContext = RendererContextHelper.current(context);
                
        // build a renderer for this descriptor
        Renderable rendererInstance = RendererFactory.newRenderer(context, rendererType, renderer); 
        rendererInstance.execute(rendererContext);
    }
    
    public static void executePageRenderer(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String rendererType, String renderer) throws Exception
    {
        // this executes against buffer objects and returns a buffer
        String responseBody = RenderUtil.processRenderer(context, request, response, rendererType, renderer);

        // generate the HEAD tag
        String headTags = generateHeader(context, request, response);

        // Now do a replace on all of the stamp placeholders
        //String responseBody = wrappedResponse.getOutput();
        int i = -1;
        do
        {
            i = responseBody.indexOf(WebFrameworkConstants.PAGE_HEAD_DEPENDENCIES_STAMP);
            if (i > -1)
            {
                responseBody = responseBody.substring(0, i) + headTags + responseBody.substring(
                        i + WebFrameworkConstants.PAGE_HEAD_DEPENDENCIES_STAMP.length(),
                        responseBody.length());
            }
        }
        while (i > -1);

        // Finally, commit the entire thing to the output stream
        response.getWriter().print(responseBody);
    }
    
    
    /**
     * Renders a system page
     * 
     * A system page is a "special page" designed to handle one of a few
     * exception cases such as when an error occurs or a page has not
     * yet been configured.  We want to show something rather than
     * have an exception purely occur.
     * 
     * @param context
     * @param request
     * @param response
     * @param systemPageId
     */
    public static void renderErrorHandlerPage(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String errorHandlerPageId, String defaultErrorHandlerPageRenderer) throws RequestDispatchException
    {
        String renderer = null;
        String rendererType = null;
        try
        {
            // go to unconfigured page display
            renderer = context.getConfig().getDispatcherErrorHandlerRenderer(errorHandlerPageId);
            rendererType = context.getConfig().getDispatcherErrorHandlerRendererType(errorHandlerPageId);
            if(rendererType == null || rendererType.length() == 0)
            {
                rendererType = WebFrameworkConstants.DEFAULT_RENDERER_TYPE;
                if(defaultErrorHandlerPageRenderer != null)
                {
                    renderer = defaultErrorHandlerPageRenderer;
                }
            }

            RenderUtil.executeRenderer(context, request, response, rendererType, renderer);
        }
        catch (Exception ex)
        {
            throw new RequestDispatchException("Failed to render the error handler page '" + errorHandlerPageId + "' for renderer: " + renderer + " of type: " + rendererType, ex);
        }
    }

    /**
     * Renders a system container
     * 
     * A system container is a page fragment that is rendered
     * as a container of other elements like components.
     * 
     * @param context
     * @param request
     * @param response
     * @param systemContainerId
     */
    public static void renderSystemPage(RequestContext context,
            HttpServletRequest request, HttpServletResponse response,
            String systemPageId, String defaultSystemPageRenderer) throws RequestDispatchException
    {
        // start a timer
        if (Timer.isTimerEnabled())
            Timer.start(request, "RenderSystemPage-" + systemPageId);
        
        // bind the rendering to this page
        RendererContextHelper.bind(context, request, response);
        
        String renderer = null;
        String rendererType = null;
        try
        {
            // go to unconfigured page display
            renderer = context.getConfig().getDispatcherSystemPageRenderer(systemPageId);
            rendererType = context.getConfig().getDispatcherSystemPageRendererType(systemPageId); 
            if(rendererType == null || rendererType.length() == 0)
            {
                rendererType = WebFrameworkConstants.DEFAULT_RENDERER_TYPE;
                if(defaultSystemPageRenderer != null)
                {
                    renderer = defaultSystemPageRenderer;
                }
            }

            RenderUtil.executePageRenderer(context, request, response, rendererType, renderer);
        }
        catch (Exception ex)
        {
            throw new RequestDispatchException("Failed to render the system page '" + systemPageId + "' for renderer: " + renderer + " of type: " + rendererType, ex);
        }
        finally
        {
            // unbind the rendering context
            RendererContextHelper.unbind(context);
            
            if (Timer.isTimerEnabled())
                Timer.stop(request, "RenderSystemPage-" + systemPageId);
        }
    }
    

    // logic that I want to move somewhere else

    /**
     * Returns the renderer to use to render the given region
     * 
     * Currently, this just resorts to using the system default but
     * the idea is that it could be overridden at various levels.
     * 
     * For example, the Theme could change the default chrome for a region.
     * 
     * Or, a specific region might be "forced" to another chrome.
     * 
     */
    protected static RendererDescriptor getRegionRendererDescriptor(RequestContext context, TemplateInstance template, String regionId, String chromeId)
    {
        // if the chrome id is empty, see if there is an override
        // this allows the template to "override" the chrome on a
        // per-region basis
        if(chromeId == null)
        {
            chromeId = template.getSetting("region-" + regionId + "-chrome-id");
        }
        
        // see if a default chrome was specified
        if(chromeId == null)
        {
            chromeId = context.getConfig().getDefaultRegionChrome();
        }
        
        // if there still isn't a chrome, then pick the system default
        if(chromeId == null)
        {
            chromeId = WebFrameworkConstants.DEFAULT_REGION_CHROME_ID;
        }
        
        // load the chrome
        Chrome chrome = context.getModel().loadChrome(context, chromeId);
        if(chrome != null)
        {
            // return the renderer for this chrome
            return new RendererDescriptor(chrome.getRenderer(), chrome.getRendererType());
        }

        // assume it is a freemarker chrome        
        return new RendererDescriptor(chromeId, WebFrameworkConstants.RENDERER_TYPE_FREEMARKER);
    }

    /**
     * Returns the renderer to use to render the given component chrome
     * 
     * Currently, this just resorts to using the system default but
     * the idea is that it could be overridden at various levels.
     * 
     * For example, the Theme could change the default chrome for
     * all components in the site.
     * 
     * Or a specific component might override its settings.
     */
    protected static RendererDescriptor getComponentRendererDescriptor(RequestContext context, Component component, String chromeId)
    {
        // if the chrome id is empty, see if there is an override
        // this allows the component to "override" the chrome on a
        // per-component basis
        if(chromeId == null)
        {
            chromeId = component.getSetting("chrome");
            if(chromeId == null)
            {
                chromeId = component.getSetting("chrome-id");
            }
        }
        
        // see if a default chrome was specified
        if(chromeId == null)
        {
            chromeId = context.getConfig().getDefaultComponentChrome();
        }        
        
        // if there still isn't a chrome, then pick the default
        if(chromeId == null)
        {
            chromeId = WebFrameworkConstants.DEFAULT_COMPONENT_CHROME_ID;
        }
        
        // load the chrome
        Chrome chrome = context.getModel().loadChrome(context, chromeId);
        if(chrome != null)
        {
            // return the renderer for this chrome
            return new RendererDescriptor(chrome.getRenderer(), chrome.getRendererType());
        }

        // assume it is a freemarker chrome
        return new RendererDescriptor(chromeId, WebFrameworkConstants.RENDERER_TYPE_FREEMARKER);
    }

}
