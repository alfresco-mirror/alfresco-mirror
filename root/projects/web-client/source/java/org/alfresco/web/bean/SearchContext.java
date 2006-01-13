/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.bean;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.QueryParser;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Holds the context required to build a search query and can return the populated query.
 * 
 * @author Kevin Roast
 */
public final class SearchContext implements Serializable
{
   private static final long serialVersionUID = 6730844584074229969L;
   
   /** XML serialization elements */
   private static final String ELEMENT_VALUE = "value";
   private static final String ELEMENT_FIXED_VALUES = "fixed-values";
   private static final String ELEMENT_INCLUSIVE = "inclusive";
   private static final String ELEMENT_UPPER = "upper";
   private static final String ELEMENT_LOWER = "lower";
   private static final String ELEMENT_RANGE = "range";
   private static final String ELEMENT_RANGES = "ranges";
   private static final String ELEMENT_NAME = "name";
   private static final String ELEMENT_ATTRIBUTE = "attribute";
   private static final String ELEMENT_ATTRIBUTES = "attributes";
   private static final String ELEMENT_MIMETYPE = "mimetype";
   private static final String ELEMENT_CONTENT_TYPE = "content-type";
   private static final String ELEMENT_CATEGORY = "category";
   private static final String ELEMENT_CATEGORIES = "categories";
   private static final String ELEMENT_LOCATION = "location";
   private static final String ELEMENT_MODE = "mode";
   private static final String ELEMENT_TEXT = "text";
   private static final String ELEMENT_SEARCH = "search";
   
   /** Search mode constants */
   public final static int SEARCH_ALL = 0;
   public final static int SEARCH_FILE_NAMES_CONTENTS = 1;
   public final static int SEARCH_FILE_NAMES = 2;
   public final static int SEARCH_SPACE_NAMES = 3;
   
   /** the search text string */
   private String text = "";
   
   /** mode for the search */
   private int mode = SearchContext.SEARCH_ALL;
   
   /** folder XPath location for the search */
   private String location = null;
   
   /** categories to add to the search */
   private String[] categories = new String[0];
   
   /** content type to restrict search against */
   private String contentType = null;
   
   /** content mimetype to restrict search against */
   private String mimeType = null;
   
   /** any extra query attributes to add to the search */
   private Map<QName, String> queryAttributes = new HashMap<QName, String>(5, 1.0f);
   
   /** any additional range attribute to add to the search */
   private Map<QName, RangeProperties> rangeAttributes = new HashMap<QName, RangeProperties>(5, 1.0f);
   
   /** any additional fixed value attributes to add to the search, such as boolean or noderef */
   private Map<QName, String> queryFixedValues = new HashMap<QName, String>(5, 1.0f);
   
   /** logger */
   private static Log logger = LogFactory.getLog(SearchContext.class);
   
   
   /**
    * Build the search query string based on the current search context members.
    * 
    * @param minimum       small possible textual string used for a match
    *                      this does not effect fixed values searches (e.g. boolean, int values) or date ranges
    * 
    * @return prepared search query string
    */
   public String buildQuery(int minimum)
   {
      String query;
      boolean validQuery = false;
      
      // the QName for the well known "name" attribute
      String nameAttr = Repository.escapeQName(QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, ELEMENT_NAME));
      
      // match against content text
      String text = this.text.trim();
      String fullTextQuery = null;
      String nameAttrQuery = null;
      
      if (text.length() >= minimum)
      {
         if (text.indexOf(' ') == -1)
         {
            // simple single word text search
            if (text.charAt(0) != '*')
            {
               // escape characters and append the wildcard character
               String safeText = QueryParser.escape(text);
               fullTextQuery = " TEXT:" + safeText + '*';
               nameAttrQuery = " @" + nameAttr + ":" + safeText + '*';
            }
            else
            {
               // found a leading wildcard - prepend it again after escaping the other characters
               String safeText = QueryParser.escape(text.substring(1));
               fullTextQuery = " TEXT:*" + safeText + '*';
               nameAttrQuery = " @" + nameAttr + ":*" + safeText + '*';
            }
         }
         else
         {
            // multiple word search
            if (text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"')
            {
               // as quoted phrase
               String quotedSafeText = '"' + QueryParser.escape(text.substring(1, text.length() - 1)) + '"';
               fullTextQuery = " TEXT:" + quotedSafeText;
               nameAttrQuery = " @" + nameAttr + ":" + quotedSafeText;
            }
            else
            {
               // as individual search terms
               StringTokenizer t = new StringTokenizer(text, " ");
               StringBuilder fullTextBuf = new StringBuilder(64);
               StringBuilder nameAttrBuf = new StringBuilder(64);
               fullTextBuf.append('(');
               nameAttrBuf.append('(');
               while (t.hasMoreTokens())
               {
                  String term = t.nextToken();
                  if (term.charAt(0) != '*')
                  {
                     String safeTerm = QueryParser.escape(term);
                     fullTextBuf.append("TEXT:").append(safeTerm).append('*');
                     nameAttrBuf.append("@").append(nameAttr).append(":").append(safeTerm).append('*');
                  }
                  else
                  {
                     String safeTerm = QueryParser.escape(term.substring(1));
                     fullTextBuf.append("TEXT:*").append(safeTerm).append('*');
                     nameAttrBuf.append("@").append(nameAttr).append(":*").append(safeTerm).append('*');
                  }
                  if (t.hasMoreTokens())
                  {
                     fullTextBuf.append(" OR ");
                     nameAttrBuf.append(" OR ");
                  }
               }
               fullTextBuf.append(')');
               nameAttrBuf.append(')');
               fullTextQuery = fullTextBuf.toString();
               nameAttrQuery = nameAttrBuf.toString();
            }
         }
         
         validQuery = true;
      }
      
      // match a specific PATH for space location or categories
      StringBuilder pathQuery = null;
      if (location != null || (categories != null && categories.length !=0))
      {
         pathQuery = new StringBuilder(128);
         if (location != null)
         {
            pathQuery.append(" PATH:\"").append(location).append("\" ");
         }
         if (categories != null && categories.length != 0)
         {
            for (int i=0; i<categories.length; i++)
            {
               if (pathQuery.length() != 0)
               {
                  pathQuery.append("OR");
               }
               pathQuery.append(" PATH:\"").append(categories[i]).append("\" "); 
            }
         }
      }
      
      // match any extra query attribute values specified
      StringBuilder attributeQuery = null;
      if (queryAttributes.size() != 0)
      {
         attributeQuery = new StringBuilder(queryAttributes.size() << 6);
         for (QName qname : queryAttributes.keySet())
         {
            String value = queryAttributes.get(qname).trim();
            if (value.length() >= minimum)
            {
               String escapedName = Repository.escapeQName(qname);
               attributeQuery.append(" +@").append(escapedName)
                             .append(":").append(QueryParser.escape(value)).append('*');
            }
         }
         
         // handle the case where we did not add any attributes due to minimum length restrictions
         if (attributeQuery.length() == 0)
         {
            attributeQuery = null;
         }
      }
      
      // match any extra fixed value attributes specified
      if (queryFixedValues.size() != 0)
      {
         if (attributeQuery == null)
         {
            attributeQuery = new StringBuilder(queryFixedValues.size() << 6);
         }
         for (QName qname : queryFixedValues.keySet())
         {
            String escapedName = Repository.escapeQName(qname);
            String value = queryFixedValues.get(qname);
            attributeQuery.append(" +@").append(escapedName)
                          .append(":\"").append(value).append('"');
         }
      }
      
      // range attributes are a special case also
      if (rangeAttributes.size() != 0)
      {
         if (attributeQuery == null)
         {
            attributeQuery = new StringBuilder(rangeAttributes.size() << 6);
         }
         for (QName qname : rangeAttributes.keySet())
         {
            String escapedName = Repository.escapeQName(qname);
            RangeProperties rp = rangeAttributes.get(qname);
            String value1 = QueryParser.escape(rp.lower);
            String value2 = QueryParser.escape(rp.upper);
            attributeQuery.append(" +@").append(escapedName)
                          .append(":").append(rp.inclusive ? "[" : "{").append(value1)
                          .append(" TO ").append(value2).append(rp.inclusive ? "]" : "}");
         }
      }
      
      // mimetype is a special case - it is indexed as a special attribute it comes from the combined
      // ContentData attribute of cm:content - ContentData string cannot be searched directly
      if (mimeType != null && mimeType.length() != 0)
      {
         if (attributeQuery == null)
         {
            attributeQuery = new StringBuilder(64);
         }
         String escapedName = Repository.escapeQName(QName.createQName(ContentModel.PROP_CONTENT + ".mimetype"));
         attributeQuery.append(" +@").append(escapedName)
                       .append(":").append(mimeType);
      }
      
      // match against appropriate content type
      String fileTypeQuery;
      if (contentType != null)
      {
         fileTypeQuery = " TYPE:\"" + contentType + "\" ";
      }
      else
      {
         // default to cm:content
         fileTypeQuery = " TYPE:\"{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}content\" ";
      }
      
      // match against FOLDER type
      String folderTypeQuery = " TYPE:\"{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}folder\" ";
      
      if (text.length() >= minimum)
      {
         // text query for name and/or full text specified
         switch (mode)
         {
            case SearchContext.SEARCH_ALL:
               query = '(' + fileTypeQuery + " AND " + '(' + nameAttrQuery + fullTextQuery + ')' + ')' + " OR " +
                       '(' + folderTypeQuery + " AND " + nameAttrQuery + ')';
               break;
            
            case SearchContext.SEARCH_FILE_NAMES:
               query = fileTypeQuery + " AND " + nameAttrQuery;
               break;
            
            case SearchContext.SEARCH_FILE_NAMES_CONTENTS:
               query = fileTypeQuery + " AND " + '(' + nameAttrQuery + fullTextQuery + ')';
               break;
            
            case SearchContext.SEARCH_SPACE_NAMES:
               query = folderTypeQuery + " AND " + nameAttrQuery;
               break;
            
            default:
               throw new IllegalStateException("Unknown search mode specified: " + mode);
         }
      }
      else
      {
         // no text query specified - must be an attribute/value query only
         switch (mode)
         {
            case SearchContext.SEARCH_ALL:
               query = '(' + fileTypeQuery + " OR " + folderTypeQuery + ')';
               break;
            
            case SearchContext.SEARCH_FILE_NAMES:
            case SearchContext.SEARCH_FILE_NAMES_CONTENTS:
               query = fileTypeQuery;
               break;
            
            case SearchContext.SEARCH_SPACE_NAMES:
               query = folderTypeQuery;
               break;
            
            default:
              throw new IllegalStateException("Unknown search mode specified: " + mode);
         }
      }
      
      // match entire query against any additional attributes specified
      if (attributeQuery != null)
      {
         query = attributeQuery + " AND (" + query + ')';
      }
      
      // match entire query against any specified paths
      if (pathQuery != null)
      {
         query = "(" + pathQuery + ") AND (" + query + ')';
      }
      
      // check that we have a query worth executing - if we have no attributes, paths or text/name search
      // then we'll only have a search against files/type TYPE which does nothing by itself!
      validQuery = validQuery | (attributeQuery != null) | (pathQuery != null);
      if (validQuery == false)
      {
         query = null;
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Query: " + query);
      
      return query;
   }
   
   /**
    * Generate a search XPATH pointing to the specified node, optionally return an XPATH
    * that includes the child nodes.
    *  
    * @param id         Of the node to generate path too
    * @param children   Whether to include children of the node
    * 
    * @return the path
    */
   /*package*/ static String getPathFromSpaceRef(NodeRef ref, boolean children)
   {
      FacesContext context = FacesContext.getCurrentInstance();
      Path path = Repository.getServiceRegistry(context).getNodeService().getPath(ref);
      NamespaceService ns = Repository.getServiceRegistry(context).getNamespaceService();
      StringBuilder buf = new StringBuilder(64);
      for (int i=0; i<path.size(); i++)
      {
         String elementString = "";
         Path.Element element = path.get(i);
         if (element instanceof Path.ChildAssocElement)
         {
            ChildAssociationRef elementRef = ((Path.ChildAssocElement)element).getRef();
            if (elementRef.getParentRef() != null)
            {
               Collection prefixes = ns.getPrefixes(elementRef.getQName().getNamespaceURI());
               if (prefixes.size() >0)
               {
                  elementString = '/' + (String)prefixes.iterator().next() + ':' + ISO9075.encode(elementRef.getQName().getLocalName());
               }
            }
         }
         
         buf.append(elementString);
      }
      if (children == true)
      {
         // append syntax to get all children of the path
         buf.append("//*");
      }
      else
      {
         // append syntax to just represent the path, not the children
         buf.append("/*");
      }
      
      return buf.toString();
   }
   
   /**
    * @return Returns the categories to use for the search
    */
   public String[] getCategories()
   {
      return this.categories;
   }
   
   /**
    * @param categories    The categories to set as a list of search XPATHs
    */
   public void setCategories(String[] categories)
   {
      if (categories != null)
      {
         this.categories = categories;
      }
   }
   
   /**
    * @return Returns the node XPath to search in or null for all.
    */
   public String getLocation()
   {
      return this.location;
   }
   
   /**
    * @param location      The node XPATH to search from or null for all..
    */
   public void setLocation(String location)
   {
      this.location = location;
   }
   
   /**
    * @return Returns the mode to use during the search (see constants)
    */
   public int getMode()
   {
      return this.mode;
   }
   
   /**
    * @param mode The mode to use during the search (see constants)
    */
   public void setMode(int mode)
   {
      this.mode = mode;
   }
   
   /**
    * @return Returns the search text string.
    */
   public String getText()
   {
      return this.text;
   }
   
   /**
    * @param text       The search text string.
    */
   public void setText(String text)
   {
      this.text = text;
   }

   /**
    * @return Returns the contentType.
    */
   public String getContentType()
   {
      return this.contentType;
   }

   /**
    * @param contentType The content type to restrict attribute search against.
    */
   public void setContentType(String contentType)
   {
      this.contentType = contentType;
   }
   
   /**
    * @return Returns the mimeType.
    */
   public String getMimeType()
   {
      return this.mimeType;
   }
   /**
    * @param mimeType The mimeType to set.
    */
   public void setMimeType(String mimeType)
   {
      this.mimeType = mimeType;
   }
   
   /**
    * Add an additional attribute to search against
    * 
    * @param qname      QName of the attribute to search against
    * @param value      Value of the attribute to use
    */
   public void addAttributeQuery(QName qname, String value)
   {
      this.queryAttributes.put(qname, value);
   }
   
   public String getAttributeQuery(QName qname)
   {
      return this.queryAttributes.get(qname);
   }
   
   /**
    * Add an additional range attribute to search against
    * 
    * @param qname      QName of the attribute to search against
    * @param lower      Lower value for range
    * @param upper      Upper value for range
    * @param inclusive  True for inclusive within the range, false otherwise
    */
   public void addRangeQuery(QName qname, String lower, String upper, boolean inclusive)
   {
      this.rangeAttributes.put(qname, new RangeProperties(qname, lower, upper, inclusive));
   }
   
   public RangeProperties getRangeProperty(QName qname)
   {
      return this.rangeAttributes.get(qname);
   }
   
   /**
    * Add an additional fixed value attribute to search against
    * 
    * @param qname      QName of the attribute to search against
    * @param value      Fixed value of the attribute to use
    */
   public void addFixedValueQuery(QName qname, String value)
   {
      this.queryFixedValues.put(qname, value);
   }
   
   public String getFixedValueQuery(QName qname)
   {
      return this.queryFixedValues.get(qname);
   }
   
   /**
    * @return this SearchContext as XML
    * 
    * Example:
    * <code>
    * <?xml version="1.0" encoding="UTF-8"?>
    * <search>
    *    <text>CDATA</text>
    *    <mode>int</mode>
    *    <location>XPath</location>
    *    <categories>
    *       <category>XPath</category>
    *    </categories>
    *    <content-type>String</content-type>
    *    <mimetype>String</mimetype>
    *    <attributes>
    *       <attribute name="String">String</attribute>
    *    </attributes>
    *    <ranges>
    *       <range name="String">
    *          <lower>String</lower>
    *          <upper>String</upper>
    *          <inclusive>boolean</inclusive>
    *       </range>
    *    </ranges>
    *    <fixed-values>
    *       <value name="String">String</value>
    *    </fixed-values>
    * </search>
    * </code>
    */
   public String toXML()
   {
      try
      {
         NamespaceService ns = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService();
         
         Document doc = DocumentHelper.createDocument();
         
         Element root = doc.addElement(ELEMENT_SEARCH);
         
         root.addElement(ELEMENT_TEXT).addCDATA(this.text);
         root.addElement(ELEMENT_MODE).addText(Integer.toString(this.mode));
         if (this.location != null)
         {
            root.addElement(ELEMENT_LOCATION).addText(this.location);
         }
         
         Element categories = root.addElement(ELEMENT_CATEGORIES);
         for (String path : this.categories)
         {
            categories.addElement(ELEMENT_CATEGORY).addText(path);
         }
         
         if (this.contentType != null)
         {
            root.addElement(ELEMENT_CONTENT_TYPE).addText(this.contentType);
         }
         if (this.mimeType != null && this.mimeType.length() != 0)
         {
            root.addElement(ELEMENT_MIMETYPE).addText(this.mimeType);
         }
         
         Element attributes = root.addElement(ELEMENT_ATTRIBUTES);
         for (QName attrName : this.queryAttributes.keySet())
         {
            attributes.addElement(ELEMENT_ATTRIBUTE)
                      .addAttribute(ELEMENT_NAME, attrName.toPrefixString(ns))
                      .addCDATA(this.queryAttributes.get(attrName));
         }
         
         Element ranges = root.addElement(ELEMENT_RANGES);
         for (QName rangeName : this.rangeAttributes.keySet())
         {
            RangeProperties rangeProps = this.rangeAttributes.get(rangeName);
            Element range = ranges.addElement(ELEMENT_RANGE);
            range.addAttribute(ELEMENT_NAME, rangeName.toPrefixString(ns));
            range.addElement(ELEMENT_LOWER).addText(rangeProps.lower);
            range.addElement(ELEMENT_UPPER).addText(rangeProps.upper);
            range.addElement(ELEMENT_INCLUSIVE).addText(Boolean.toString(rangeProps.inclusive));
         }
         
         Element values = root.addElement(ELEMENT_FIXED_VALUES);
         for (QName valueName : this.queryFixedValues.keySet())
         {
            values.addElement(ELEMENT_VALUE)
                  .addAttribute(ELEMENT_NAME, valueName.toPrefixString(ns))
                  .addCDATA(this.queryFixedValues.get(valueName));
         }
         
         StringWriter out = new StringWriter(1024);
         XMLWriter writer = new XMLWriter(OutputFormat.createPrettyPrint());
         writer.setWriter(out);
         writer.write(doc);
         
         return out.toString();
      }
      catch (Throwable err)
      {
         throw new AlfrescoRuntimeException("Failed to export SearchContext to XML.", err);
      }
   }
   
   /**
    * Restore a SearchContext from an XML definition
    * 
    * @param xml     XML format SearchContext @see #toXML()
    */
   public SearchContext fromXML(String xml)
   {
      try
      {
         NamespaceService ns = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService();
         
         // get the root element
         SAXReader reader = new SAXReader();
         Document document = reader.read(new StringReader(xml));
         Element rootElement = document.getRootElement();
         Element textElement = rootElement.element(ELEMENT_TEXT);
         if (textElement != null)
         {
            this.text = textElement.getText();
         }
         Element modeElement = rootElement.element(ELEMENT_MODE);
         if (modeElement != null)
         {
            this.mode = Integer.parseInt(modeElement.getText());
         }
         Element locationElement = rootElement.element(ELEMENT_LOCATION);
         if (locationElement != null)
         {
            this.location = locationElement.getText();
         }
         Element categoriesElement = rootElement.element(ELEMENT_CATEGORIES);
         if (categoriesElement != null)
         {
            List<String> categories = new ArrayList<String>(4);
            for (Iterator i=categoriesElement.elementIterator(ELEMENT_CATEGORY); i.hasNext(); /**/)
            {
               Element categoryElement = (Element)i.next();
               categories.add(categoryElement.getText());
            }
            this.categories = categories.toArray(this.categories);
         }
         Element contentTypeElement = rootElement.element(ELEMENT_CONTENT_TYPE);
         if (contentTypeElement != null)
         {
            this.contentType = contentTypeElement.getText();
         }
         Element mimetypeElement = rootElement.element(ELEMENT_MIMETYPE);
         if (mimetypeElement != null)
         {
            this.mimeType = mimetypeElement.getText();
         }
         Element attributesElement = rootElement.element(ELEMENT_ATTRIBUTES);
         if (attributesElement != null)
         {
            for (Iterator i=attributesElement.elementIterator(ELEMENT_ATTRIBUTE); i.hasNext(); /**/)
            {
               Element attrElement = (Element)i.next();
               QName qname = QName.createQName(attrElement.attributeValue(ELEMENT_NAME), ns);
               addAttributeQuery(qname, attrElement.getText());
            }
         }
         Element rangesElement = rootElement.element(ELEMENT_RANGES);
         if (rangesElement != null)
         {
            for (Iterator i=rangesElement.elementIterator(ELEMENT_RANGE); i.hasNext(); /**/)
            {
               Element rangeElement = (Element)i.next();
               Element lowerElement = rangeElement.element(ELEMENT_LOWER);
               Element upperElement = rangeElement.element(ELEMENT_UPPER);
               Element incElement = rangeElement.element(ELEMENT_INCLUSIVE);
               if (lowerElement != null && upperElement != null && incElement != null)
               {
                  QName qname = QName.createQName(rangeElement.attributeValue(ELEMENT_NAME), ns);
                  addRangeQuery(qname,
                        lowerElement.getText(), upperElement.getText(),
                        Boolean.parseBoolean(incElement.getText()));
               }
            }
         }
         
         Element valuesElement = rootElement.element(ELEMENT_FIXED_VALUES);
         if (valuesElement != null)
         {
            for (Iterator i=valuesElement.elementIterator(ELEMENT_VALUE); i.hasNext(); /**/)
            {
               Element valueElement = (Element)i.next();
               QName qname = QName.createQName(valueElement.attributeValue(ELEMENT_NAME), ns);
               addFixedValueQuery(qname, valueElement.getText());
            }
         }
      }
      catch (Throwable err)
      {
         throw new AlfrescoRuntimeException("Failed to import SearchContext from XML.", err);
      }
      return this;
   }
   
   /**
    * Simple wrapper class for range query attribute properties 
    */
   static class RangeProperties
   {
      QName qname;
      String lower;
      String upper;
      boolean inclusive;
      
      RangeProperties(QName qname, String lower, String upper, boolean inclusive)
      {
         this.qname = qname;
         this.lower = lower;
         this.upper = upper;
         this.inclusive = inclusive;
      }
   }
}
