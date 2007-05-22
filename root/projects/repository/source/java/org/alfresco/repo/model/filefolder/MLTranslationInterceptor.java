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
package org.alfresco.repo.model.filefolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An interceptor that replaces files nodes with their equivalent
 * translations according to the locale.  It is to be used with the
 * {@link FileFolderService}.
 * 
 * @since 2.1
 * @author Derek Hulley
 */
public class MLTranslationInterceptor implements MethodInterceptor
{
    /**
     * Names of methods that return a <code>List</code> or <code>FileInfo</code> instances.
     */
    private static final Set<String> METHOD_NAMES_LIST;
    /**
     * Names of methods that return a <code>FileInfo</code>.
     */
    private static final Set<String> METHOD_NAMES_SINGLE;
    /**
     * Names of methods that don't need interception.  This is used to catch any new methods
     * added to the interface.
     */
    private static final Set<String> METHOD_NAMES_OTHER;
    static
    {
        METHOD_NAMES_LIST = new HashSet<String>(13);
        METHOD_NAMES_LIST.add("list");
        METHOD_NAMES_LIST.add("listFiles");
        METHOD_NAMES_LIST.add("listFolders");
        METHOD_NAMES_LIST.add("search");
        METHOD_NAMES_LIST.add("getNamePath");
        
        METHOD_NAMES_SINGLE = new HashSet<String>(13);
        METHOD_NAMES_SINGLE.add("searchSimple");
        METHOD_NAMES_SINGLE.add("rename");
        METHOD_NAMES_SINGLE.add("move");
        METHOD_NAMES_SINGLE.add("copy");
        METHOD_NAMES_SINGLE.add("create");
        METHOD_NAMES_SINGLE.add("makeFolders");
        METHOD_NAMES_SINGLE.add("getNamePath");
        METHOD_NAMES_SINGLE.add("resolveNamePath");
        METHOD_NAMES_SINGLE.add("getFileInfo");
        
        METHOD_NAMES_OTHER = new HashSet<String>(13);
        METHOD_NAMES_OTHER.add("delete");
        METHOD_NAMES_OTHER.add("getReader");
        METHOD_NAMES_OTHER.add("getWriter");
    }
    
    private static Log logger = LogFactory.getLog(MLTranslationInterceptor.class);

    private NodeService nodeService;
    private MultilingualContentService multilingualContentService;
    
    /**
     * Constructor.
     */
    public MLTranslationInterceptor()
    {
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setMultilingualContentService(MultilingualContentService multilingualContentService)
    {
        this.multilingualContentService = multilingualContentService;
    }

    /**
     * Converts the node referenice where an alternative translation should be used.
     * 
     * @param nodeRef       the basic nodeRef
     * @return              Returns the replacement if required
     */
    private NodeRef getTranslatedNodeRef(NodeRef nodeRef)
    {
        // Ignore null
        if (nodeRef == null)
        {
            return nodeRef;
        }
        // Ignore everything without the correct aspect
        if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_MULTILINGUAL_DOCUMENT))
        {
            return nodeRef;
        }
        // Find the translation
        Map<Locale, NodeRef> translations = multilingualContentService.getTranslations(nodeRef);
        Locale filterLocale = I18NUtil.getContentLocaleOrNull();
        Set<Locale> possibleLocales = translations.keySet();
        Locale localeToUse = I18NUtil.getNearestLocale(filterLocale, possibleLocales);
        // Select the node
        NodeRef translatedNodeRef = translations.get(localeToUse);
        // Done
        if (logger.isDebugEnabled())
        {
            if (nodeRef.equals(translatedNodeRef))
            {
                logger.debug("NodeRef substitution: " + nodeRef + " --> " + translatedNodeRef);
            }
            else
            {
                logger.debug("NodeRef substitution: " + nodeRef + " (no change)");
            }
        }
        return nodeRef;
    }
    
    /**
     * Converts the file info where an alternative translation should be used.
     * 
     * @param fileInfo      the basic file or folder info
     * @return              Returns a replacement if required
     * 
     * @see FileInfo#getTranslations()
     */
    private FileInfo getTranslatedFileInfo(FileInfo fileInfo)
    {
        // Ignore null
        if (fileInfo == null)
        {
            return null;
        }
        // Ignore folders
        if (fileInfo.isFolder())
        {
            return fileInfo;
        }
        // Ignore files without translations
        Map<Locale, FileInfo> translations = fileInfo.getTranslations();
        if (translations.size() == 0)
        {
            return fileInfo;
        }
        // Get the locale to use
        Set<Locale> possibleLocales = translations.keySet();
        Locale filterLocale = I18NUtil.getContentLocaleOrNull();
        Locale localeToUse = I18NUtil.getNearestLocale(filterLocale, possibleLocales);
        FileInfo translatedFileInfo = translations.get(localeToUse);
        // Done
        if (logger.isDebugEnabled())
        {
            if (fileInfo.equals(translatedFileInfo))
            {
                logger.debug("FileInfo substitution: " + fileInfo + " --> " + translatedFileInfo);
            }
            else
            {
                logger.debug("FileInfo substitution: " + fileInfo + " (no change)");
            }
        }
        return translatedFileInfo;
    }
    
    @SuppressWarnings("unchecked")
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        Object ret = null;
        String methodName = invocation.getMethod().getName();
        
        if (METHOD_NAMES_LIST.contains(methodName))
        {
            List<FileInfo> fileInfos = (List<FileInfo>) invocation.proceed();
            // Compile a set to ensure we don't get duplicates
            Map<FileInfo, FileInfo> translatedFileInfos = new HashMap<FileInfo, FileInfo>(17);
            for (FileInfo fileInfo : fileInfos)
            {
                FileInfo translatedFileInfo = getTranslatedFileInfo(fileInfo);
                // Add this to the set
                translatedFileInfos.put(fileInfo, translatedFileInfo);
            }
            // Convert the set back to a list
            List<FileInfo> orderedResults = new ArrayList<FileInfo>(fileInfos.size());
            for (FileInfo info : fileInfos)
            {
                orderedResults.add(translatedFileInfos.get(info));
            }
            ret = orderedResults;
        }
        else if (METHOD_NAMES_SINGLE.contains(methodName))
        {
            Object obj = invocation.proceed();
            if (obj instanceof FileInfo)
            {
                FileInfo fileInfo = (FileInfo) obj;
                ret = getTranslatedFileInfo(fileInfo);
            }
            else if (obj instanceof NodeRef)
            {
                NodeRef nodeRef = (NodeRef) obj;
                ret = getTranslatedNodeRef(nodeRef);
            }
        }
        else if (METHOD_NAMES_OTHER.contains(methodName))
        {
            // There is nothing to do
            ret = invocation.proceed();
        }
        else
        {
            throw new RuntimeException("Method not handled by interceptor: " + methodName);
        }
        
        // Done
        return ret;
    }
}