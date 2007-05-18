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
package org.alfresco.repo.search.impl.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.search.IndexerException;
import org.alfresco.repo.search.impl.lucene.fts.FTSIndexerAware;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.ISO9075;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

/**
 * The implementation of the lucene based indexer. Supports basic transactional behaviour if used on its own.
 * 
 * @author andyh
 */
public class ADMLuceneIndexerImpl extends AbstractLuceneIndexerImpl<NodeRef> implements ADMLuceneIndexer
{
    static Logger s_logger = Logger.getLogger(ADMLuceneIndexerImpl.class);

    /**
     * The node service we use to get information about nodes
     */
    NodeService nodeService;

    /**
     * Content service to get content for indexing.
     */
    ContentService contentService;

    /**
     * Call back to make after doing non atomic indexing
     */
    FTSIndexerAware callBack;

    /**
     * Count of remaining items to index non atomically
     */
    int remainingCount = 0;

    /**
     * A list of stuff that requires non atomic indexing
     */
    private ArrayList<Helper> toFTSIndex = new ArrayList<Helper>();

    /**
     * Default construction
     */
    ADMLuceneIndexerImpl()
    {
        super();
    }

    /**
     * IOC setting of dictionary service
     */

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        super.setDictionaryService(dictionaryService);
    }

    /**
     * Setter for getting the node service via IOC Used in the Spring container
     * 
     * @param nodeService
     */

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * IOC setting of the content service
     * 
     * @param contentService
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /*
     * Indexer Implementation
     */

    public void createNode(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Create node " + relationshipRef.getChildRef());
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            NodeRef childRef = relationshipRef.getChildRef();
            // If we have the root node we delete all other root nodes first
            if ((relationshipRef.getParentRef() == null)
                    && childRef.equals(nodeService.getRootNode(childRef.getStoreRef())))
            {
                addRootNodesToDeletionList();
                s_logger.warn("Detected root node addition: deleting all nodes from the index");
            }
            index(childRef);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Create node failed", e);
        }
    }

    private void addRootNodesToDeletionList()
    {
        IndexReader mainReader = null;
        try
        {
            try
            {
                mainReader = getReader();
                TermDocs td = mainReader.termDocs(new Term("ISROOT", "T"));
                while (td.next())
                {
                    int doc = td.doc();
                    Document document = mainReader.document(doc);
                    String id = document.get("ID");
                    NodeRef ref = new NodeRef(id);
                    deleteImpl(ref.toString(), false, true, mainReader);
                }
            }
            catch (IOException e)
            {
                throw new LuceneIndexException("Failed to delete all primary nodes", e);
            }
        }
        finally
        {
            if (mainReader != null)
            {
                try
                {
                    mainReader.close();
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Filed to close main reader", e);
                }
            }
        }
    }

    public void updateNode(NodeRef nodeRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Update node " + nodeRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            reindex(nodeRef, false);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Update node failed", e);
        }
    }

    public void deleteNode(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Delete node " + relationshipRef.getChildRef());
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            // The requires a reindex - a delete may remove too much from under this node - that also lives under
            // other nodes via secondary associations. All the nodes below require reindex.
            // This is true if the deleted node is via secondary or primary assoc.
            delete(relationshipRef.getChildRef());
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Delete node failed", e);
        }
    }

    public void createChildRelationship(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Create child " + relationshipRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            // TODO: Optimise
            // reindex(relationshipRef.getParentRef());
            reindex(relationshipRef.getChildRef(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to create child relationship", e);
        }
    }

    public void updateChildRelationship(ChildAssociationRef relationshipBeforeRef,
            ChildAssociationRef relationshipAfterRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Update child " + relationshipBeforeRef + " to " + relationshipAfterRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            // TODO: Optimise
            if (relationshipBeforeRef.getParentRef() != null)
            {
                // reindex(relationshipBeforeRef.getParentRef());
            }
            reindex(relationshipBeforeRef.getChildRef(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to update child relationship", e);
        }
    }

    public void deleteChildRelationship(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Delete child " + relationshipRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            // TODO: Optimise
            if (relationshipRef.getParentRef() != null)
            {
                // reindex(relationshipRef.getParentRef());
            }
            reindex(relationshipRef.getChildRef(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to delete child relationship", e);
        }
    }

    /**
     * Generate an indexer
     * 
     * @param storeRef
     * @param deltaId
     * @param config 
     * @return - the indexer instance
     * @throws LuceneIndexException 
     */
    public static ADMLuceneIndexerImpl getUpdateIndexer(StoreRef storeRef, String deltaId, LuceneConfig config)
            throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Creating indexer");
        }
        ADMLuceneIndexerImpl indexer = new ADMLuceneIndexerImpl();
        indexer.setLuceneConfig(config);
        indexer.initialise(storeRef, deltaId);
        return indexer;
    }

    /*
     * Transactional support Used by the resource manager for indexers.
     */

    void doFTSIndexCommit() throws LuceneIndexException
    {
        IndexReader mainReader = null;
        IndexReader deltaReader = null;
        IndexSearcher mainSearcher = null;
        IndexSearcher deltaSearcher = null;

        try
        {
            try
            {
                mainReader = getReader();
                deltaReader = getDeltaReader();
                mainSearcher = new IndexSearcher(mainReader);
                deltaSearcher = new IndexSearcher(deltaReader);

                for (Helper helper : toFTSIndex)
                {
                    deletions.add(helper.ref);
                }

            }
            finally
            {
                if (deltaSearcher != null)
                {
                    try
                    {
                        deltaSearcher.close();
                    }
                    catch (IOException e)
                    {
                        s_logger.warn("Failed to close delta searcher", e);
                    }
                }
                if (mainSearcher != null)
                {
                    try
                    {
                        mainSearcher.close();
                    }
                    catch (IOException e)
                    {
                        s_logger.warn("Failed to close main searcher", e);
                    }
                }
                try
                {
                    closeDeltaReader();
                }
                catch (LuceneIndexException e)
                {
                    s_logger.warn("Failed to close delta reader", e);
                }
                if (mainReader != null)
                {
                    try
                    {
                        mainReader.close();
                    }
                    catch (IOException e)
                    {
                        s_logger.warn("Failed to close main reader", e);
                    }
                }
            }

            setInfo(docs, getDeletions(), true);
            // mergeDeltaIntoMain(new LinkedHashSet<Term>());
        }
        catch (IOException e)
        {
            // If anything goes wrong we try and do a roll back
            rollback();
            throw new LuceneIndexException("Commit failed", e);
        }
        catch (LuceneIndexException e)
        {
            // If anything goes wrong we try and do a roll back
            rollback();
            throw new LuceneIndexException("Commit failed", e);
        }
        finally
        {
            // Make sure we tidy up
            // deleteDelta();
        }

    }

    

    

    static class Counter
    {
        int countInParent = 0;

        int count = -1;

        int getCountInParent()
        {
            return countInParent;
        }

        int getRepeat()
        {
            return (count / countInParent) + 1;
        }

        void incrementParentCount()
        {
            countInParent++;
        }

        void increment()
        {
            count++;
        }

    }

    private class Pair<F, S>
    {
        private F first;

        private S second;

        /**
         * Helper class to hold two related objects
         * @param first
         * @param second
         */
        public Pair(F first, S second)
        {
            this.first = first;
            this.second = second;
        }

        /**
         * Get the first
         * @return - first
         */
        public F getFirst()
        {
            return first;
        }

        /**
         * Get the second
         * @return -second
         */
        public S getSecond()
        {
            return second;
        }
    }

    public List<Document> createDocuments(String stringNodeRef, boolean isNew, boolean indexAllProperties,
            boolean includeDirectoryDocuments)
    {
        NodeRef nodeRef = new NodeRef(stringNodeRef);
        
        
        Map<ChildAssociationRef, Counter> nodeCounts = getNodeCounts(nodeRef);
        List<Document> docs = new ArrayList<Document>();
        ChildAssociationRef qNameRef = null;
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        NodeRef.Status nodeStatus = nodeService.getNodeStatus(nodeRef);

        Collection<Path> directPaths = nodeService.getPaths(nodeRef, false);
        Collection<Pair<Path, QName>> categoryPaths = getCategoryPaths(nodeRef, properties);
        Collection<Pair<Path, QName>> paths = new ArrayList<Pair<Path, QName>>(directPaths.size()
                + categoryPaths.size());
        for (Path path : directPaths)
        {
            paths.add(new Pair<Path, QName>(path, null));
        }
        paths.addAll(categoryPaths);

        Document xdoc = new Document();
        xdoc.add(new Field("ID", nodeRef.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
        xdoc.add(new Field("TX", nodeStatus.getChangeTxnId(), Field.Store.YES, Field.Index.UN_TOKENIZED,
                Field.TermVector.NO));
        boolean isAtomic = true;
        for (QName propertyName : properties.keySet())
        {
            Serializable value = properties.get(propertyName);
            if (indexAllProperties)
            {
                indexProperty(nodeRef, propertyName, value, xdoc, false);
            }
            else
            {
                isAtomic &= indexProperty(nodeRef, propertyName, value, xdoc, true);
            }
        }

        boolean isRoot = nodeRef.equals(nodeService.getRootNode(nodeRef.getStoreRef()));

        StringBuilder qNameBuffer = new StringBuilder(64);

        for (Iterator<Pair<Path, QName>> it = paths.iterator(); it.hasNext(); /**/)
        {
            Pair<Path, QName> pair = it.next();
            // Lucene flags in order are: Stored, indexed, tokenised

            qNameRef = getLastRefOrNull(pair.getFirst());

            String pathString = pair.getFirst().toString();
            if ((pathString.length() > 0) && (pathString.charAt(0) == '/'))
            {
                pathString = pathString.substring(1);
            }

            if (isRoot)
            {
                // Root node
            }
            else if (pair.getFirst().size() == 1)
            {
                // Pseudo root node ignore
            }
            else
            // not a root node
            {
                Counter counter = nodeCounts.get(qNameRef);
                // If we have something in a container with root aspect we will
                // not find it

                if ((counter == null) || (counter.getRepeat() < counter.getCountInParent()))
                {
                    if ((qNameRef != null) && (qNameRef.getParentRef() != null) && (qNameRef.getQName() != null))
                    {
                        if (qNameBuffer.length() > 0)
                        {
                            qNameBuffer.append(";/");
                        }
                        qNameBuffer.append(ISO9075.getXPathName(qNameRef.getQName()));
                        xdoc.add(new Field("PARENT", qNameRef.getParentRef().toString(), Field.Store.YES,
                                Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                        xdoc.add(new Field("ASSOCTYPEQNAME", ISO9075.getXPathName(qNameRef.getTypeQName()),
                                Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
                        xdoc.add(new Field("LINKASPECT", (pair.getSecond() == null) ? "" : ISO9075.getXPathName(pair
                                .getSecond()), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                    }
                }

                if (counter != null)
                {
                    counter.increment();
                }

                // check for child associations

                if (includeDirectoryDocuments)
                {
                    if (mayHaveChildren(nodeRef))
                    {
                        if (directPaths.contains(pair.getFirst()))
                        {
                            Document directoryEntry = new Document();
                            directoryEntry.add(new Field("ID", nodeRef.toString(), Field.Store.YES,
                                    Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                            directoryEntry.add(new Field("PATH", pathString, Field.Store.YES, Field.Index.TOKENIZED,
                                    Field.TermVector.NO));
                            for (NodeRef parent : getParents(pair.getFirst()))
                            {
                                directoryEntry.add(new Field("ANCESTOR", parent.toString(), Field.Store.NO,
                                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                            }
                            directoryEntry.add(new Field("ISCONTAINER", "T", Field.Store.YES, Field.Index.UN_TOKENIZED,
                                    Field.TermVector.NO));

                            if (isCategory(getDictionaryService().getType(nodeService.getType(nodeRef))))
                            {
                                directoryEntry.add(new Field("ISCATEGORY", "T", Field.Store.YES,
                                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                            }

                            docs.add(directoryEntry);
                        }
                    }
                }
            }
        }

        // Root Node
        if (isRoot)
        {
            // TODO: Does the root element have a QName?
            xdoc.add(new Field("ISCONTAINER", "T", Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("PATH", "", Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("QNAME", "", Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("ISROOT", "T", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("PRIMARYASSOCTYPEQNAME", ISO9075.getXPathName(ContentModel.ASSOC_CHILDREN),
                    Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
            xdoc.add(new Field("ISNODE", "T", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            docs.add(xdoc);

        }
        else
        // not a root node
        {
            xdoc.add(new Field("QNAME", qNameBuffer.toString(), Field.Store.YES, Field.Index.TOKENIZED,
                    Field.TermVector.NO));
            // xdoc.add(new Field("PARENT", parentBuffer.toString(), true, true,
            // true));

            ChildAssociationRef primary = nodeService.getPrimaryParent(nodeRef);
            xdoc.add(new Field("PRIMARYPARENT", primary.getParentRef().toString(), Field.Store.YES,
                    Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("PRIMARYASSOCTYPEQNAME", ISO9075.getXPathName(primary.getTypeQName()), Field.Store.YES,
                    Field.Index.NO, Field.TermVector.NO));
            QName typeQName = nodeService.getType(nodeRef);

            xdoc.add(new Field("TYPE", ISO9075.getXPathName(typeQName), Field.Store.YES, Field.Index.UN_TOKENIZED,
                    Field.TermVector.NO));
            for (QName classRef : nodeService.getAspects(nodeRef))
            {
                xdoc.add(new Field("ASPECT", ISO9075.getXPathName(classRef), Field.Store.YES, Field.Index.UN_TOKENIZED,
                        Field.TermVector.NO));
            }

            xdoc.add(new Field("ISROOT", "F", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("ISNODE", "T", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            if (isAtomic || indexAllProperties)
            {
                xdoc
                        .add(new Field("FTSSTATUS", "Clean", Field.Store.NO, Field.Index.UN_TOKENIZED,
                                Field.TermVector.NO));
            }
            else
            {
                if (isNew)
                {
                    xdoc.add(new Field("FTSSTATUS", "New", Field.Store.NO, Field.Index.UN_TOKENIZED,
                            Field.TermVector.NO));
                }
                else
                {
                    xdoc.add(new Field("FTSSTATUS", "Dirty", Field.Store.NO, Field.Index.UN_TOKENIZED,
                            Field.TermVector.NO));
                }
            }

            // {
            docs.add(xdoc);
            // }
        }

        return docs;
    }

    /**
     * @param indexAtomicPropertiesOnly
     *            true to ignore all properties that must be indexed non-atomically
     * @return Returns true if the property was indexed atomically, or false if it should be done asynchronously
     */
    protected boolean indexProperty(NodeRef nodeRef, QName propertyName, Serializable value, Document doc, boolean indexAtomicPropertiesOnly)
    {
        String attributeName = "@"
                + QName.createQName(propertyName.getNamespaceURI(), ISO9075.encode(propertyName.getLocalName()));
    
        boolean store = true;
        boolean index = true;
        boolean tokenise = true;
        boolean atomic = true;
        boolean isContent = false;
        boolean isMultiLingual = false;
        boolean isText = false;
    
        PropertyDefinition propertyDef = getDictionaryService().getProperty(propertyName);
        if (propertyDef != null)
        {
            index = propertyDef.isIndexed();
            store = propertyDef.isStoredInIndex();
            tokenise = propertyDef.isTokenisedInIndex();
            atomic = propertyDef.isIndexedAtomically();
            isContent = propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT);
            isMultiLingual = propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT);
            isText = propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT);
        }
        if (value == null)
        {
            // the value is null
            return true;
        }
        else if (indexAtomicPropertiesOnly && !atomic)
        {
            // we are only doing atomic properties and the property is definitely non-atomic
            return false;
        }
    
        if (!indexAtomicPropertiesOnly)
        {
            doc.removeFields(propertyName.toString());
        }
        boolean wereAllAtomic = true;
        // convert value to String
        for (Serializable serializableValue : DefaultTypeConverter.INSTANCE.getCollection(Serializable.class, value))
        {
            String strValue = null;
            try
            {
                strValue = DefaultTypeConverter.INSTANCE.convert(String.class, serializableValue);
            }
            catch (TypeConversionException e)
            {
                doc.add(new Field(attributeName, NOT_INDEXED_NO_TYPE_CONVERSION, Field.Store.NO,
                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                continue;
            }
            if (strValue == null)
            {
                // nothing to index
                continue;
            }
    
            if (isContent)
            {
                ContentData contentData = DefaultTypeConverter.INSTANCE.convert(ContentData.class, serializableValue);
                if (!index || contentData.getMimetype() == null)
                {
                    // no mimetype or property not indexed
                    continue;
                }
                // store mimetype in index - even if content does not index it is useful
                // Added szie and locale - size needs to be tokenised correctly
                doc.add(new Field(attributeName + ".mimetype", contentData.getMimetype(), Field.Store.NO,
                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                doc.add(new Field(attributeName + ".size", Long.toString(contentData.getSize()), Field.Store.NO,
                        Field.Index.TOKENIZED, Field.TermVector.NO));
    
                // TODO: Use the node locale in preferanced to the system locale
                Locale locale = contentData.getLocale();
                if (locale == null)
                {
                    Serializable localeProperty = nodeService.getProperty(nodeRef, ContentModel.PROP_LOCALE);
                    if (localeProperty != null)
                    {
                        locale = DefaultTypeConverter.INSTANCE.convert(Locale.class, localeProperty);
                    }
                }
                if (locale == null)
                {
                    locale = I18NUtil.getLocale();
                }
                doc.add(new Field(attributeName + ".locale", locale.toString().toLowerCase(), Field.Store.NO,
                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
    
                ContentReader reader = contentService.getReader(nodeRef, propertyName);
                if (reader != null && reader.exists())
                {
                    boolean readerReady = true;
                    // transform if necessary (it is not a UTF-8 text document)
                    if (!EqualsHelper.nullSafeEquals(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN)
                            || !EqualsHelper.nullSafeEquals(reader.getEncoding(), "UTF-8"))
                    {
                        // get the transformer
                        ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(),
                                MimetypeMap.MIMETYPE_TEXT_PLAIN);
                        // is this transformer good enough?
                        if (transformer == null)
                        {
                            // log it
                            if (s_logger.isDebugEnabled())
                            {
                                s_logger.debug("Not indexed: No transformation: \n"
                                        + "   source: " + reader + "\n" + "   target: "
                                        + MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            }
                            // don't index from the reader
                            readerReady = false;
                            // not indexed: no transformation
                            // doc.add(new Field("TEXT", NOT_INDEXED_NO_TRANSFORMATION, Field.Store.NO,
                            // Field.Index.TOKENIZED, Field.TermVector.NO));
                            doc.add(new Field(attributeName, NOT_INDEXED_NO_TRANSFORMATION, Field.Store.NO,
                                    Field.Index.TOKENIZED, Field.TermVector.NO));
                        }
                        else if (indexAtomicPropertiesOnly
                                && transformer.getTransformationTime() > maxAtomicTransformationTime)
                        {
                            // only indexing atomic properties
                            // indexing will take too long, so push it to the background
                            wereAllAtomic = false;
                        }
                        else
                        {
                            // We have a transformer that is fast enough
                            ContentWriter writer = contentService.getTempWriter();
                            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            // this is what the analyzers expect on the stream
                            writer.setEncoding("UTF-8");
                            try
                            {
    
                                transformer.transform(reader, writer);
                                // point the reader to the new-written content
                                reader = writer.getReader();
                            }
                            catch (ContentIOException e)
                            {
                                // log it
                                if (s_logger.isDebugEnabled())
                                {
                                    s_logger.debug("Not indexed: Transformation failed", e);
                                }
                                // don't index from the reader
                                readerReady = false;
                                // not indexed: transformation
                                // failed
                                // doc.add(new Field("TEXT", NOT_INDEXED_TRANSFORMATION_FAILED, Field.Store.NO,
                                // Field.Index.TOKENIZED, Field.TermVector.NO));
                                doc.add(new Field(attributeName, NOT_INDEXED_TRANSFORMATION_FAILED, Field.Store.NO,
                                        Field.Index.TOKENIZED, Field.TermVector.NO));
                            }
                        }
                    }
                    // add the text field using the stream from the
                    // reader, but only if the reader is valid
                    if (readerReady)
                    {
                        InputStreamReader isr = null;
                        // InputStream ris = reader.getContentInputStream();
                        // try
                        // {
                        // isr = new InputStreamReader(ris, "UTF-8");
                        // }
                        // catch (UnsupportedEncodingException e)
                        // {
                        // isr = new InputStreamReader(ris);
                        // }
                        // doc.add(new Field("TEXT", isr, Field.TermVector.NO));
    
                        InputStream ris = reader.getReader().getContentInputStream();
                        try
                        {
                            isr = new InputStreamReader(ris, "UTF-8");
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            isr = new InputStreamReader(ris);
                        }
                        StringBuilder builder = new StringBuilder();
                        builder.append("\u0000").append(locale.toString()).append("\u0000");
                        StringReader prefix = new StringReader(builder.toString());
                        Reader multiReader = new MultiReader(prefix, isr);
                        doc.add(new Field(attributeName, multiReader, Field.TermVector.NO));
                    }
                }
                else
                // URL not present (null reader) or no content at the URL (file missing)
                {
                    // log it
                    if (s_logger.isDebugEnabled())
                    {
                        s_logger.debug("Not indexed: Content Missing \n"
                                + "   node: " + nodeRef + "\n" + "   reader: " + reader + "\n" + "   content exists: "
                                + (reader == null ? " --- " : Boolean.toString(reader.exists())));
                    }
                    // not indexed: content missing
                    doc.add(new Field("TEXT", NOT_INDEXED_CONTENT_MISSING, Field.Store.NO, Field.Index.TOKENIZED,
                            Field.TermVector.NO));
                    doc.add(new Field(attributeName, NOT_INDEXED_CONTENT_MISSING, Field.Store.NO,
                            Field.Index.TOKENIZED, Field.TermVector.NO));
                }
            }
            else
            {
                Field.Store fieldStore = store ? Field.Store.YES : Field.Store.NO;
                Field.Index fieldIndex;
    
                if (index)
                {
                    if (tokenise)
                    {
                        fieldIndex = Field.Index.TOKENIZED;
                    }
                    else
                    {
                        fieldIndex = Field.Index.UN_TOKENIZED;
                    }
                }
                else
                {
                    fieldIndex = Field.Index.NO;
                }
    
                if ((fieldIndex != Field.Index.NO) || (fieldStore != Field.Store.NO))
                {
                    if (isMultiLingual)
                    {
                        MLText mlText = DefaultTypeConverter.INSTANCE.convert(MLText.class, serializableValue);
                        for (Locale locale : mlText.getLocales())
                        {
                            String localeString = mlText.getValue(locale);
                            StringBuilder builder = new StringBuilder();
                            builder.append("\u0000").append(locale.toString()).append("\u0000").append(localeString);
                            doc.add(new Field(attributeName, builder.toString(), fieldStore, fieldIndex,
                                    Field.TermVector.NO));
                        }
                    }
                    else if (isText)
                    {
                        // Temporary special case for uids and gids
                        if(propertyName.equals(ContentModel.PROP_USER_USERNAME) || propertyName.equals(ContentModel.PROP_USERNAME) || propertyName.equals(ContentModel.PROP_AUTHORITY_NAME))
                        {
                            doc.add(new Field(attributeName, strValue, fieldStore, fieldIndex, Field.TermVector.NO));
                        }
    
                        // TODO: Use the node locale in preferanced to the system locale
                        Locale locale = null;
    
                        Serializable localeProperty = nodeService.getProperty(nodeRef, ContentModel.PROP_LOCALE);
                        if (localeProperty != null)
                        {
                            locale = DefaultTypeConverter.INSTANCE.convert(Locale.class, localeProperty);
                        }
    
                        if (locale == null)
                        {
                            locale = I18NUtil.getLocale();
                        }
                        if (tokenise)
                        {
                            StringBuilder builder = new StringBuilder();
                            builder.append("\u0000").append(locale.toString()).append("\u0000").append(strValue);
                            doc.add(new Field(attributeName, builder.toString(), fieldStore, fieldIndex,
                                    Field.TermVector.NO));
                        }
                        else
                        {
                            doc.add(new Field(attributeName, strValue, fieldStore, fieldIndex, Field.TermVector.NO));
                        }
                    }
                    else
                    {
                        doc.add(new Field(attributeName, strValue, fieldStore, fieldIndex, Field.TermVector.NO));
                    }
                }
            }
        }
    
        return wereAllAtomic;
    }
    
    /**
     * Does the node type or any applied aspect allow this node to have child associations?
     * 
     * @param nodeRef
     * @return true if the node may have children
     */
    private boolean mayHaveChildren(NodeRef nodeRef)
    {
        // 1) Does the type support children?
        QName nodeTypeRef = nodeService.getType(nodeRef);
        TypeDefinition nodeTypeDef = getDictionaryService().getType(nodeTypeRef);
        if ((nodeTypeDef != null) && (nodeTypeDef.getChildAssociations().size() > 0))
        {
            return true;
        }
        // 2) Do any of the applied aspects support children?
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        for (QName aspect : aspects)
        {
            AspectDefinition aspectDef = getDictionaryService().getAspect(aspect);
            if ((aspectDef != null) && (aspectDef.getChildAssociations().size() > 0))
            {
                return true;
            }
        }
        return false;
    }

    private ArrayList<NodeRef> getParents(Path path)
    {
        ArrayList<NodeRef> parentsInDepthOrderStartingWithSelf = new ArrayList<NodeRef>(8);
        for (Iterator<Path.Element> elit = path.iterator(); elit.hasNext(); /**/)
        {
            Path.Element element = elit.next();
            if (!(element instanceof Path.ChildAssocElement))
            {
                throw new IndexerException("Confused path: " + path);
            }
            Path.ChildAssocElement cae = (Path.ChildAssocElement) element;
            parentsInDepthOrderStartingWithSelf.add(0, cae.getRef().getChildRef());

        }
        return parentsInDepthOrderStartingWithSelf;
    }

    private ChildAssociationRef getLastRefOrNull(Path path)
    {
        if (path.last() instanceof Path.ChildAssocElement)
        {
            Path.ChildAssocElement cae = (Path.ChildAssocElement) path.last();
            return cae.getRef();
        }
        else
        {
            return null;
        }
    }

    private Map<ChildAssociationRef, Counter> getNodeCounts(NodeRef nodeRef)
    {
        Map<ChildAssociationRef, Counter> nodeCounts = new HashMap<ChildAssociationRef, Counter>(5);
        List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(nodeRef);
        // count the number of times the association is duplicated
        for (ChildAssociationRef assoc : parentAssocs)
        {
            Counter counter = nodeCounts.get(assoc);
            if (counter == null)
            {
                counter = new Counter();
                nodeCounts.put(assoc, counter);
            }
            counter.incrementParentCount();

        }
        return nodeCounts;
    }

    private Collection<Pair<Path, QName>> getCategoryPaths(NodeRef nodeRef, Map<QName, Serializable> properties)
    {
        ArrayList<Pair<Path, QName>> categoryPaths = new ArrayList<Pair<Path, QName>>();
        Set<QName> aspects = nodeService.getAspects(nodeRef);

        for (QName classRef : aspects)
        {
            AspectDefinition aspDef = getDictionaryService().getAspect(classRef);
            if (isCategorised(aspDef))
            {
                LinkedList<Pair<Path, QName>> aspectPaths = new LinkedList<Pair<Path, QName>>();
                for (PropertyDefinition propDef : aspDef.getProperties().values())
                {
                    if (propDef.getDataType().getName().equals(DataTypeDefinition.CATEGORY))
                    {
                        for (NodeRef catRef : DefaultTypeConverter.INSTANCE.getCollection(NodeRef.class, properties
                                .get(propDef.getName())))
                        {
                            if (catRef != null)
                            {
                                for (Path path : nodeService.getPaths(catRef, false))
                                {
                                    if ((path.size() > 1) && (path.get(1) instanceof Path.ChildAssocElement))
                                    {
                                        Path.ChildAssocElement cae = (Path.ChildAssocElement) path.get(1);
                                        boolean isFakeRoot = true;
                                        for (ChildAssociationRef car : nodeService.getParentAssocs(cae.getRef()
                                                .getChildRef()))
                                        {
                                            if (cae.getRef().equals(car))
                                            {
                                                isFakeRoot = false;
                                                break;
                                            }
                                        }
                                        if (isFakeRoot)
                                        {
                                            if (path.toString().indexOf(aspDef.getName().toString()) != -1)
                                            {
                                                aspectPaths.add(new Pair<Path, QName>(path, aspDef.getName()));
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                categoryPaths.addAll(aspectPaths);
            }
        }
        // Add member final element
        for (Pair<Path, QName> pair : categoryPaths)
        {
            if (pair.getFirst().last() instanceof Path.ChildAssocElement)
            {
                Path.ChildAssocElement cae = (Path.ChildAssocElement) pair.getFirst().last();
                ChildAssociationRef assocRef = cae.getRef();
                pair.getFirst().append(
                        new Path.ChildAssocElement(new ChildAssociationRef(assocRef.getTypeQName(), assocRef
                                .getChildRef(), QName.createQName("member"), nodeRef)));
            }
        }

        return categoryPaths;
    }

    private boolean isCategorised(AspectDefinition aspDef)
    {
        AspectDefinition current = aspDef;
        while (current != null)
        {
            if (current.getName().equals(ContentModel.ASPECT_CLASSIFIABLE))
            {
                return true;
            }
            else
            {
                QName parentName = current.getParentName();
                if (parentName == null)
                {
                    break;
                }
                current = getDictionaryService().getAspect(parentName);
            }
        }
        return false;
    }

    private boolean isCategory(TypeDefinition typeDef)
    {
        if (typeDef == null)
        {
            return false;
        }
        TypeDefinition current = typeDef;
        while (current != null)
        {
            if (current.getName().equals(ContentModel.TYPE_CATEGORY))
            {
                return true;
            }
            else
            {
                QName parentName = current.getParentName();
                if (parentName == null)
                {
                    break;
                }
                current = getDictionaryService().getType(parentName);
            }
        }
        return false;
    }

    public int updateFullTextSearch(int size) throws LuceneIndexException
    {
        checkAbleToDoWork(IndexUpdateStatus.ASYNCHRONOUS);
        // if (!mainIndexExists())
        // {
        // remainingCount = size;
        // return;
        // }
        try
        {
            NodeRef lastId = null;

            toFTSIndex = new ArrayList<Helper>(size);
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(new TermQuery(new Term("FTSSTATUS", "Dirty")), Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term("FTSSTATUS", "New")), Occur.SHOULD);

            int count = 0;
            Searcher searcher = null;
            try
            {
                searcher = getSearcher(null);
                // commit on another thread - appears like there is no index ...try later
                if (searcher == null)
                {
                    remainingCount = size;
                    return 0;
                }
                Hits hits;
                try
                {
                    hits = searcher.search(booleanQuery);
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException(
                            "Failed to execute query to find content which needs updating in the index", e);
                }

                for(int i = 0; i < hits.length(); i++)
                {
                    Document doc = hits.doc(i);
                    Helper helper = new Helper(doc.getField("ID").stringValue(), doc.getField("TX").stringValue());
                    toFTSIndex.add(helper);
                    if (++count >= size)
                    {
                        break;
                    }
                }

                count = hits.length();
            }
            finally
            {
                if (searcher != null)
                {
                    try
                    {
                        searcher.close();
                    }
                    catch (IOException e)
                    {
                        throw new LuceneIndexException("Failed to close searcher", e);
                    }
                }
            }

            if (toFTSIndex.size() > 0)
            {
                checkAbleToDoWork(IndexUpdateStatus.ASYNCHRONOUS);

                IndexWriter writer = null;
                try
                {
                    writer = getDeltaWriter();
                    for (Helper helper : toFTSIndex)
                    {
                        // Document document = helper.document;
                        NodeRef ref = new NodeRef(helper.ref);
//                      bypass nodes that have disappeared
                        if (!nodeService.exists(ref))
                        {
                            continue;
                        }

                        List<Document> docs = createDocuments(ref.toString(), false, true, false);
                        for (Document doc : docs)
                        {
                            try
                            {
                                writer.addDocument(doc /*
                                                         * TODO: Select the language based analyser
                                                         */);
                            }
                            catch (IOException e)
                            {
                                throw new LuceneIndexException("Failed to add document while updating fts index", e);
                            }
                        }

                        // Need to do all the current id in the TX - should all
                        // be
                        // together so skip until id changes
                        if (writer.docCount() > size)
                        {
                            if (lastId == null)
                            {
                                lastId = ref;
                            }
                            if (!lastId.equals(ref))
                            {
                                break;
                            }
                        }
                    }

                    int done = writer.docCount();
                    remainingCount = count - done;
                    return done;
                }
                catch (LuceneIndexException e)
                {
                    if (writer != null)
                    {
                        closeDeltaWriter();
                    }
                    return 0;
                }
            }
            else
            {
                return 0;
            }
        }
        catch (IOException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed FTS update", e);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed FTS update", e);
        }
    }

    public void registerCallBack(FTSIndexerAware callBack)
    {
        this.callBack = callBack;
    }

    private static class Helper
    {
        String ref;

        String tx;

        Helper(String ref, String tx)
        {
            this.ref = ref;
            this.tx = tx;
        }
    }

    FullTextSearchIndexer fullTextSearchIndexer;

    public void setFullTextSearchIndexer(FullTextSearchIndexer fullTextSearchIndexer)
    {
        this.fullTextSearchIndexer = fullTextSearchIndexer;
    }

    protected void doPrepare() throws IOException
    {
        saveDelta();
        flushPending();
        // prepareToMergeIntoMain();
    }
    
    protected void doCommit() throws IOException
    {
        if (indexUpdateStatus == IndexUpdateStatus.ASYNCHRONOUS)
        {
            doFTSIndexCommit();
            // FTS does not trigger indexing request
        }
        else
        {
            setInfo(docs, getDeletions(), false);
            fullTextSearchIndexer.requiresIndex(store);
        }
        if (callBack != null)
        {
            callBack.indexCompleted(store, remainingCount, null);
        }
    }
    
    protected void doRollBack() throws IOException
    {
        if (callBack != null)
        {
            callBack.indexCompleted(store, 0, null);
        }   
    }
    
    protected void doSetRollbackOnly() throws IOException
    {
        
    }

}
