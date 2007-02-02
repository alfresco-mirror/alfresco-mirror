/**
 * 
 */
package org.alfresco.repo.copy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.CrossRepositoryCopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

/**
 * Cross Repository Copying.
 * @author britt
 */
public class CrossRepositoryCopyServiceImpl implements
        CrossRepositoryCopyService 
{
    /**
     * The NodeService reference.
     */
    private NodeService fNodeService;
    
    /**
     * The FileFolderService reference.
     */
    private FileFolderService fFileFolderService;
    
    /**
     * The regular CopyService reference.
     */
    private CopyService fCopyService;
    
    /**
     * The AVMService.
     */
    private AVMService fAVMService;
    
    /**
     * The ContentService.
     */
    private ContentService fContentService;
    
    /**
     * The DictionaryService.
     */
    private DictionaryService fDictionaryService;

    /**
     * A default constructor.
     */
    public CrossRepositoryCopyServiceImpl()
    {
    }
    
    // Setters for Spring.
    
    public void setAvmService(AVMService service)
    {
        fAVMService = service;
    }
    
    public void setContentService(ContentService service)
    {
        fContentService = service;
    }
    
    public void setCopyService(CopyService service)
    {
        fCopyService = service;
    }
    
    public void setDictionaryService(DictionaryService service)
    {
        fDictionaryService = service;
    }
    
    public void setFileFolderService(FileFolderService service)
    {
        fFileFolderService = service;
    }
    
    public void setNodeService(NodeService service)
    {
        fNodeService = service;
    }
    
    /**
     * This copies recursively src, which may be a container or a content type
     * to dst, which must be a container. Copied nodes will have the copied from aspect
     * applied to them.
     * @param src The node to copy.
     * @param dst The container to copy it into.
     * @param name The name to give the copy.
     */
    public void copy(NodeRef src, NodeRef dst, String name) 
    {
        StoreRef srcStoreRef = src.getStoreRef();
        StoreRef dstStoreRef = dst.getStoreRef();
        if (srcStoreRef.getProtocol().equals("avm"))
        {
            if (dstStoreRef.getProtocol().equals("avm"))
            {
                copyAVMToAVM(src, dst, name);
            }
            else
            {
                copyAVMToRepo(src, dst, name);
            }
        }
        else
        {
            if (dstStoreRef.getProtocol().equals("avm"))
            {
                copyRepoToAVM(src, dst, name);
            }
            else
            {
                copyRepoToRepo(src, dst, name);
            }
        }
    }
    
    /**
     * Handle copying from AVM to AVM
     * @param src Source node.
     * @param dst Destination directory node.
     * @param name Name to give copy.
     */
    private void copyAVMToAVM(NodeRef src, NodeRef dst, String name)
    {
        Pair<Integer, String> srcStorePath = AVMNodeConverter.ToAVMVersionPath(src);
        Pair<Integer, String> dstStorePath = AVMNodeConverter.ToAVMVersionPath(dst);
        fAVMService.copy(srcStorePath.getFirst(), srcStorePath.getSecond(), 
                         dstStorePath.getSecond(), name);        
    }
    
    /**
     * Handle copying from AVM to Repo.
     * @param src Source node.
     * @param dst Destination Container.
     * @param name The name to give the copy.
     */
    private void copyAVMToRepo(NodeRef src, NodeRef dst, String name)
    {
        Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(src);
        AVMNodeDescriptor desc = fAVMService.lookup(versionPath.getFirst(), versionPath.getSecond());
        if (desc.isFile())
        {
            FileInfo newChild = fFileFolderService.create(dst, name, ContentModel.TYPE_CONTENT);
            NodeRef childRef = newChild.getNodeRef();
            InputStream in = fAVMService.getFileInputStream(desc);
            OutputStream out = fContentService.getWriter(childRef, ContentModel.PROP_CONTENT, true).getContentOutputStream();
            copyData(in, out);
            copyPropsAndAspectsAVMToRepo(src, childRef);
        }
        else
        {
            FileInfo newChild = fFileFolderService.create(dst, name, ContentModel.TYPE_FOLDER);
            NodeRef childRef = newChild.getNodeRef();
            copyPropsAndAspectsAVMToRepo(src, childRef);
            Map<String, AVMNodeDescriptor> listing = fAVMService.getDirectoryListing(desc);
            for (Map.Entry<String, AVMNodeDescriptor> entry : listing.entrySet())
            {
                NodeRef srcChild = AVMNodeConverter.ToNodeRef(versionPath.getFirst(), entry.getValue().getPath());
                copyAVMToRepo(srcChild, childRef, entry.getKey());
            }
        }
    }
    
    /**
     * Helper that copies aspects and properties.
     * @param src The source AVM node.
     * @param dst The destination Repo node.
     */
    private void copyPropsAndAspectsAVMToRepo(NodeRef src, NodeRef dst)
    {
        Map<QName, Serializable> props = fNodeService.getProperties(src);
        fNodeService.setProperties(dst, props);
        Set<QName> aspects = fNodeService.getAspects(src);
        Map<QName, Serializable> empty = new HashMap<QName, Serializable>();
        for (QName aspect : aspects)
        {
            fNodeService.addAspect(dst, aspect, empty);
        }
        if (!fNodeService.hasAspect(dst, ContentModel.ASPECT_COPIEDFROM))
        {
            empty.put(ContentModel.PROP_COPY_REFERENCE, src);
            fNodeService.addAspect(dst, ContentModel.ASPECT_COPIEDFROM, empty);
        }
        else
        {
            fNodeService.setProperty(dst, ContentModel.PROP_COPY_REFERENCE, src);
        }
    }

    /**
     * Handle copying from Repo to AVM.
     * @param src The source node.
     * @param dst The destingation directory.
     * @param name The name to give the copy.
     */
    private void copyRepoToAVM(NodeRef src, NodeRef dst, String name)
    {
        QName srcType = fNodeService.getType(src);
        Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(dst);
        String childPath = AVMNodeConverter.ExtendAVMPath(versionPath.getSecond(), name);
        NodeRef childNodeRef = AVMNodeConverter.ToNodeRef(-1, childPath);
        if (fDictionaryService.isSubClass(srcType, ContentModel.TYPE_CONTENT))
        {
            InputStream in = fContentService.getReader(src, ContentModel.PROP_CONTENT).getContentInputStream();
            OutputStream out = fAVMService.createFile(versionPath.getSecond(), name);
            copyData(in, out);
            copyPropsAndAspectsRepoToAVM(src, childNodeRef, childPath);
            return;
        }
        if (fDictionaryService.isSubClass(srcType, ContentModel.TYPE_FOLDER))
        {
            fAVMService.createDirectory(versionPath.getSecond(), name);
            copyPropsAndAspectsRepoToAVM(src, childNodeRef, childPath);
            List<FileInfo> listing = fFileFolderService.list(src);
            for (FileInfo info : listing)
            {
                copyRepoToAVM(info.getNodeRef(), childNodeRef, info.getName());
            }
            return;
        }
    }
    
    /**
     * Helper to copy properties and aspects.
     * @param src The source node.
     * @param dst The destination node.
     * @param dstPath The destination AVM path.
     */
    private void copyPropsAndAspectsRepoToAVM(NodeRef src, NodeRef dst, String dstPath)
    {
        Map<QName, Serializable> props = fNodeService.getProperties(src);
        fNodeService.setProperties(dst, props);
        Set<QName> aspects = fNodeService.getAspects(src);
        for (QName aspect : aspects)
        {
            fAVMService.addAspect(dstPath, aspect);
        }
        if (!fAVMService.hasAspect(-1, dstPath, ContentModel.ASPECT_COPIEDFROM))
        {
            fAVMService.addAspect(dstPath, ContentModel.ASPECT_COPIEDFROM);
        }
        fNodeService.setProperty(dst, ContentModel.PROP_COPY_REFERENCE, src);
    }
    
    /**
     * Handle copying from Repo to Repo.
     * @param src The source node.
     * @param dst The destination container.
     * @param name The name to give the copy.
     */
    private void copyRepoToRepo(NodeRef src, NodeRef dst, String name)
    {
    }
    
    private void copyData(InputStream in, OutputStream out)
    {
        try
        {
            byte [] buff = new byte[8192];
            int read = 0;
            while ((read = in.read(buff)) != -1)
            {
                out.write(buff, 0, read);
            }
            in.close();
            out.close();
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("I/O Error.", e);
        }
    }
}
