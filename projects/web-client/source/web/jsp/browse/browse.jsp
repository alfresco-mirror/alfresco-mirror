<%--
  Copyright (C) 2005 Alfresco, Inc.

  Licensed under the GNU Lesser General Public License as
  published by the Free Software Foundation; either version
  2.1 of the License, or (at your option) any later version.
  You may obtain a copy of the License at

    http://www.gnu.org/licenses/lgpl.txt

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  either express or implied. See the License for the specific
  language governing permissions and limitations under the
  License.
--%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="50kb" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page>

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="messages" var="msg"/>
   
   <h:form id="browse">
   
   <%-- Main outer table --%>
   <table cellspacing=0 cellpadding=2>
      
      <%-- Title bar --%>
      <tr>
         <td colspan=2>
            <%@ include file="../parts/titlebar.jsp" %>
         </td>
      </tr>
      
      <%-- Main area --%>
      <tr valign=top>
         <%-- Shelf --%>
         <td>
            <%@ include file="../parts/shelf.jsp" %>
         </td>
         
         <%-- Work Area --%>
         <td width=100%>
            <table cellspacing=0 cellpadding=0 width=100%>
               <%-- Breadcrumb --%>
               <%@ include file="../parts/breadcrumb.jsp" %>
               
               <%-- Status and Actions --%>
               <tr>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_4.gif)" width=4></td>
                  <td bgcolor="#EEEEEE">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing=4 cellpadding=0 width=100%>
                        <tr valign=top>
 
                           <%-- actions for browse mode --%>
                           <a:panel id="browse-actions" rendered="#{NavigationBean.searchContext == null}">
                              <td width=32>
                                 <img src="<%=request.getContextPath()%>/images/icons/space.gif" width=32 height=32>
                              </td>
                              <td>
                                 <%-- Summary --%>
                                 <div class="mainSubTitle"><h:outputText value="#{msg.product_name}" id="msg1" /></div>
                                 <div class="mainTitle"><h:outputText value="#{NavigationBean.nodeProperties.name}" id="msg2" /></div>
                                 <div class="mainSubText"><h:outputText value="#{msg.view_description}" id="msg3" /></div>
                                 <div class="mainSubText"><h:outputText value="#{NavigationBean.nodeProperties.description}" id="msg4" /></div>
                              </td>
                              <td bgcolor="#465F7D" width=1></td>
                              <td width=100 style="padding-left:2px">
                                 <%-- Current object actions --%>
                                 <h:outputText style="padding-left:20px" styleClass="mainSubTitle" value="#{msg.actions}" id="msg5" /><br>
                                 <a:actionLink value="#{msg.new_space}" image="/images/icons/create_space.gif" padding="4" action="createSpace" actionListener="#{NewSpaceDialog.startWizard}" id="link1" />
                                 <a:actionLink value="#{msg.delete_space}" image="/images/icons/delete.gif" padding="4" action="deleteSpace" actionListener="#{BrowseBean.setupSpaceAction}" id="link2">
                                    <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param1" />
                                 </a:actionLink>
                                 <a:actionLink value="#{msg.add_content}" image="/images/icons/add.gif" padding="4" action="addContent" actionListener="#{AddContentWizard.startWizard}" id="link3" />
                                 <a:menu id="spaceMenu" itemSpacing="4" label="More..." image="/images/icons/more.gif" tooltip="More Actions for this Space" menuStyleClass="moreActionsMenu" style="padding-left:20px">
                                    <a:actionLink value="#{msg.create_content}" image="/images/icons/new_content.gif" id="link3_1" action="createContent" actionListener="#{CreateContentWizard.startWizard}" />
                                    <a:actionLink value="#{msg.invite}" image="/images/icons/invite.gif" id="link4" />
                                    <a:actionLink value="#{msg.view_details}" image="/images/icons/View_details.gif" id="link5" action="showSpaceDetails" actionListener="#{BrowseBean.setupSpaceAction}">
                                       <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param2" />
                                    </a:actionLink>
                                    <a:actionLink value="#{msg.cut}" image="/images/icons/cut.gif" id="link6" actionListener="#{ClipboardBean.cutNode}">
                                       <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param3" />
                                    </a:actionLink>
                                    <a:actionLink value="#{msg.copy}" image="/images/icons/copy.gif" id="link7" actionListener="#{ClipboardBean.copyNode}">
                                       <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param4" />
                                    </a:actionLink>
                                    <a:actionLink value="#{msg.paste_all}" image="/images/icons/paste.gif" actionListener="#{ClipboardBean.pasteAll}" id="link8" />
                                    <a:actionLink value="#{msg.advanced_space_wizard}" image="/images/icons/create_space.gif" action="createAdvancedSpace" actionListener="#{NewSpaceWizard.startWizard}" id="link9" />
                                    <a:actionLink value="#{msg.manage_rules}" image="/images/icons/rule.gif" action="manageRules" actionListener="#{BrowseBean.setupSpaceAction}" id="link10">
                                       <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param5" />
                                    </a:actionLink>
                                    <%-- TODO: add evaluator based on "admin" role --%>
                                    <a:actionLink value="#{msg.manage_users}" image="/images/icons/people.gif" action="manageUsers" actionListener="#{NewUserWizard.setupUsers}" id="link11" />
                                 </a:menu>
                              </td>
                           </a:panel>
                           
                           <%-- actions for search results mode --%>
                           <a:panel id="search-actions" rendered="#{NavigationBean.searchContext != null}">
                              <td width=32>
                                 <img src="<%=request.getContextPath()%>/images/icons/search_results_large.gif" width=32 height=32>
                              </td>
                              <td>
                                 <%-- Summary --%>
                                 <div class="mainSubTitle"><h:outputText value="#{msg.product_name}" id="msg10" /></div>
                                 <div class="mainTitle"><h:outputText value="#{msg.search_results}" id="msg11" /></div>
                                 <div class="mainSubText">
                                    <h:outputFormat value="#{msg.search_detail}" id="msg12">
                                       <f:param value="#{NavigationBean.searchContext.text}" id="param2" />
                                    </h:outputFormat>
                                 </div>
                                 <div class="mainSubText"><h:outputText value="#{msg.search_description}" id="msg13" /></div>
                              </td>
                              <td bgcolor="#465F7D" width=1></td>
                              <td width=100 style="padding-left:2px">
                                 <%-- Current object actions --%>
                                 <h:outputText style="padding-left:20px" styleClass="mainSubTitle" value="#{msg.actions}" id="msg14" /><br>
                                 <a:actionLink value="#{msg.close_search}" image="/images/icons/action.gif" padding="4" actionListener="#{BrowseBean.closeSearch}" id="link20" />
                              </td>
                           </a:panel>
                           
                           <td bgcolor="#465F7D" width=1></td>
                           <td width=110>
                              <%-- View mode settings --%>
                              <h:outputText style="padding-left:26px" styleClass="mainSubTitle" value="#{msg.view}"/><br>
                              <a:modeList itemSpacing="3" iconColumnWidth="20" selectedStyleClass="statusListHighlight" selectedImage="/images/icons/Details.gif"
                                    value="#{BrowseBean.browseViewMode}" actionListener="#{BrowseBean.viewModeChanged}">
                                 <a:listItem value="details" label="Details View" />
                                 <a:listItem value="icons" label="Icon View" />
                                 <a:listItem value="list" label="Browse View" />
                              </a:modeList>
                           </td>
                        </tr>
                     </table>
                     
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_6.gif)" width=4></td>
               </tr>
               
               <%-- separator row with gradient shadow --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_7.gif" width=4 height=9></td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_9.gif" width=4 height=9></td>
               </tr>
               
               <%-- Toolbar --%>
               <%-- NOTE: removed toolbar until multi-select implemented
               <tr style="padding-top:4px">
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <table cellspacing=0 cellpadding=4>
                        <tr>
                           <td>
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "bluetoolbar", "#E9F0F4"); %>
                                 <table cellspacing=0 cellpadding=0><tr>
                                    <td><a:actionLink value="#{msg.cut}" image="/images/icons/cut.gif" showLink="false" /></td><td>&nbsp;|&nbsp;</td>
                                    <td><a:actionLink value="#{msg.copy}" image="/images/icons/copy.gif" showLink="false" /></td><td>&nbsp;|&nbsp;</td>
                                    <td><a:actionLink value="#{msg.paste}" image="/images/icons/paste.gif" showLink="false" /></td><td>&nbsp;|&nbsp;</td>
                                    <td><a:actionLink value="#{msg.delete}" image="/images/icons/delete.gif" showLink="false" /></td>
                                 </tr></table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "bluetoolbar"); %>
                           </td>
                        </tr>
                     </table>
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               --%>
               
               <%-- Details - Spaces --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <div style="padding:4px">
                     
                     <a:panel id="spaces-panel" border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE" styleClass="mainSubTitle" label="#{msg.browse_spaces}">
                     
                     <%-- Spaces List --%>
                     <a:richList id="spacesList" binding="#{BrowseBean.spacesRichList}" viewMode="#{BrowseBean.browseViewMode}" pageSize="#{BrowseBean.browsePageSize}"
                           styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                           value="#{BrowseBean.nodes}" var="r" initialSortColumn="name" initialSortDescending="true">
                        
                        <%-- Primary column for details view mode --%>
                        <a:column primary="true" width="200" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="Name" value="name" mode="case-insensitive" styleClass="header"/>
                           </f:facet>
                           <f:facet name="small-icon">
                              <a:actionLink value="#{r.name}" image="/images/icons/space_small.gif" actionListener="#{BrowseBean.clickSpace}" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Primary column for icons view mode --%>
                        <a:column primary="true" style="padding:2px;text-align:left;vertical-align:top;" rendered="#{BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="large-icon">
                              <a:actionLink value="#{r.name}" image="/images/icons/#{r.icon}.gif" actionListener="#{BrowseBean.clickSpace}" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}" styleClass="header">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Primary column for list view mode --%>
                        <a:column primary="true" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'list'}">
                           <f:facet name="large-icon">
                              <a:actionLink value="#{r.name}" image="/images/icons/#{r.icon}.gif" actionListener="#{BrowseBean.clickSpace}" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}" styleClass="title">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Description column for all view modes --%>
                        <a:column style="text-align:left">
                           <f:facet name="header">
                              <a:sortLink label="Description" value="description" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.description}" />
                        </a:column>
                        
                        <%-- Path column for search mode in details view mode --%>
                        <a:column style="text-align:left" rendered="#{NavigationBean.searchContext != null && BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="Path" value="displayPath" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.displayPath}" />
                        </a:column>
                        
                        <%-- Created Date column for details view mode --%>
                        <a:column style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="Created" value="created" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.created}">
                              <a:convertXMLDate dateStyle="long" type="both" timeStyle="short" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Modified Date column for details/icons view modes --%>
                        <a:column style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details' || BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="header">
                              <a:sortLink label="Modified" value="modified" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.modified}">
                              <a:convertXMLDate dateStyle="long" type="both" timeStyle="short" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Node Descendants links for list view mode --%>
                        <a:column style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'list'}">
                           <r:nodeDescendants value="#{r.nodeRef}" styleClass="header" actionListener="#{BrowseBean.clickDescendantSpace}" />
                        </a:column>
                        
                        <%-- Actions column --%>
                        <a:column actions="true" style="text-align:left">
                           <f:facet name="header">
                              <h:outputText value="#{msg.actions}"/>
                           </f:facet>
                           <a:actionLink value="#{msg.cut}" image="/images/icons/cut.gif" showLink="false" styleClass="inlineAction" actionListener="#{ClipboardBean.cutNode}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                           <a:actionLink value="#{msg.copy}" image="/images/icons/copy.gif" showLink="false" styleClass="inlineAction" actionListener="#{ClipboardBean.copyNode}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                           <a:actionLink value="#{msg.delete}" image="/images/icons/delete.gif" showLink="false" styleClass="inlineAction" action="deleteSpace" actionListener="#{BrowseBean.setupSpaceAction}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                           <a:actionLink value="#{msg.view_details}" image="/images/icons/View_details.gif" showLink="false" styleClass="inlineAction" action="showSpaceDetails" actionListener="#{BrowseBean.setupSpaceAction}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <a:dataPager/>
                     </a:richList>
                     
                     </a:panel>
                     
                     <div>
                     
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               
               <%-- Details - Content --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <div style="padding:4px">
                     
                     <a:panel id="content-panel" border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE" styleClass="mainSubTitle" label="#{msg.browse_content}">
                     
                     <%-- Content list --%>
                     <a:richList id="contentRichList" binding="#{BrowseBean.contentRichList}" viewMode="#{BrowseBean.browseViewMode}" pageSize="#{BrowseBean.browsePageSize}"
                           styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                           value="#{BrowseBean.content}" var="r" initialSortColumn="name" initialSortDescending="true">
                        
                        <%-- Primary column for details view mode --%>
                        <a:column primary="true" width="200" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="Name" value="name" mode="case-insensitive" styleClass="header"/>
                           </f:facet>
                           <f:facet name="small-icon">
                              <a:actionLink value="#{r.name}" href="#{r.url}" image="#{r.fileType16}" showLink="false" styleClass="inlineAction" />
                           </f:facet>
                           <a:actionLink value="#{r.name}" href="#{r.url}" />
                           <h:outputText value="&nbsp; #{msg.workingcopy}" rendered="#{r.workingCopy == true}" escape="false" />
                           <h:graphicImage url="/images/icons/locked.gif" align="absmiddle" width="16" height="16" rendered="#{r.locked == true}" />
                        </a:column>
                        
                        <%-- Primary column for icons view mode --%>
                        <a:column primary="true" style="padding:2px;text-align:left;vertical-align:top;" rendered="#{BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="large-icon">
                              <a:actionLink value="#{r.name}" href="#{r.url}" image="#{r.fileType32}" showLink="false" styleClass="inlineAction" />
                           </f:facet>
                           <a:actionLink value="#{r.name}" href="#{r.url}" styleClass="header" />
                           <h:outputText value="&nbsp; #{msg.workingcopy}" rendered="#{r.workingCopy == true}" escape="false" />
                           <h:graphicImage url="/images/icons/locked.gif" align="absmiddle"  width="16" height="16" rendered="#{r.locked == true}" />
                        </a:column>
                        
                        <%-- Primary column for list view mode --%>
                        <a:column primary="true" style="padding:2px;text-align:left" rendered="#{BrowseBean.browseViewMode == 'list'}">
                           <f:facet name="large-icon">
                              <a:actionLink value="#{r.name}" href="#{r.url}" image="#{r.fileType32}" showLink="false" styleClass="inlineAction" />
                           </f:facet>
                           <a:actionLink value="#{r.name}" href="#{r.url}" styleClass="title" />
                           <h:outputText value="&nbsp; #{msg.workingcopy}" rendered="#{r.workingCopy == true}" escape="false" />
                           <h:graphicImage url="/images/icons/locked.gif" align="absmiddle" width="16" height="16" rendered="#{r.locked == true}" />
                        </a:column>
                        
                        <%-- Description column for all view modes --%>
                        <a:column style="text-align:left">
                           <f:facet name="header">
                              <a:sortLink label="Description" value="description" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.description}" />
                        </a:column>
                        
                        <%-- Path column for search mode in details view mode --%>
                        <a:column style="text-align:left" rendered="#{NavigationBean.searchContext != null && BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="Path" value="displayPath" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.displayPath}" />
                        </a:column>
                        
                        <%-- Size for details/icons view modes --%>
                        <a:column style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details' || BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="header">
                              <a:sortLink label="Size" value="size" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.size}">
                              <a:convertSize />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Created Date column for details view mode --%>
                        <a:column style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="Created" value="created" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.created}">
                              <a:convertXMLDate dateStyle="long" type="both" timeStyle="short" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Modified Date column for details/icons view modes --%>
                        <a:column style="text-align:left" rendered="#{BrowseBean.browseViewMode == 'details' || BrowseBean.browseViewMode == 'icons'}">
                           <f:facet name="header">
                              <a:sortLink label="Modified" value="modified" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.modified}">
                              <a:convertXMLDate dateStyle="long" type="both" timeStyle="short" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Actions column --%>
                        <a:column actions="true" style="text-align:left">
                           <f:facet name="header">
                              <h:outputText value="#{msg.actions}"/>
                           </f:facet>
                           <a:booleanEvaluator value="#{r.locked == false}">
                              <a:actionLink value="#{msg.edit}" image="/images/icons/edit_icon.gif" showLink="false" styleClass="inlineAction" actionListener="#{CheckinCheckoutBean.editFile}">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </a:booleanEvaluator>
                           <a:actionLink value="#{msg.cut}" image="/images/icons/cut.gif" showLink="false" styleClass="inlineAction" actionListener="#{ClipboardBean.cutNode}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                           <a:actionLink value="#{msg.copy}" image="/images/icons/copy.gif" showLink="false" styleClass="inlineAction" actionListener="#{ClipboardBean.copyNode}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                           <a:booleanEvaluator value="#{r.locked == false && r.workingCopy == false}">
                              <a:actionLink value="#{msg.checkout}" image="/images/icons/CheckOut_icon.gif" showLink="false" styleClass="inlineAction" actionListener="#{CheckinCheckoutBean.setupContentAction}" action="checkoutFile">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </a:booleanEvaluator>
                           <a:booleanEvaluator value="#{r.workingCopy == true}">
                              <a:actionLink value="#{msg.checkin}" image="/images/icons/CheckIn_icon.gif" showLink="false" styleClass="inlineAction" actionListener="#{CheckinCheckoutBean.setupContentAction}" action="checkinFile">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </a:booleanEvaluator>
                           <a:actionLink value="#{msg.view_details}" image="/images/icons/View_details.gif" showLink="false" styleClass="inlineAction" actionListener="#{BrowseBean.setupContentAction}" action="showDocDetails">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                           <%-- More actions menu --%>
                           <a:menu itemSpacing="4" image="/images/icons/more.gif" tooltip="More Actions" menuStyleClass="moreActionsMenu">
                              <a:booleanEvaluator value="#{r.locked == false}">
                                 <a:actionLink value="#{msg.update}" image="/images/icons/update.gif" actionListener="#{CheckinCheckoutBean.setupContentAction}" action="updateFile">
                                    <f:param name="id" value="#{r.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{r.locked == false && r.workingCopy == false}">
                                 <a:actionLink value="#{msg.delete}" image="/images/icons/delete.gif" actionListener="#{BrowseBean.setupContentAction}" action="deleteFile">
                                    <f:param name="id" value="#{r.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{r.workingCopy == true}"> <%-- TODO: add "|| r.locked == true" later if possible to do --%>
                                 <a:actionLink value="#{msg.undocheckout}" image="/images/icons/undo_checkout.gif" showLink="false" styleClass="inlineAction" actionListener="#{CheckinCheckoutBean.setupContentAction}" action="undoCheckoutFile">
                                    <f:param name="id" value="#{r.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{r.approveStep != null && r.workingCopy == false && r.locked == false}">
                                 <a:actionLink value="#{r.approveStep}" image="/images/icons/approve.gif" showLink="false" styleClass="inlineAction" actionListener="#{DocumentDetailsBean.approve}">
                                    <f:param name="id" value="#{r.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{r.rejectStep != null && r.workingCopy == false && r.locked == false}">
                                 <a:actionLink value="#{r.rejectStep}" image="/images/icons/reject.gif" showLink="false" styleClass="inlineAction" actionListener="#{DocumentDetailsBean.reject}">
                                    <f:param name="id" value="#{r.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                           </a:menu>
                        </a:column>
                        
                        <a:dataPager/>
                     </a:richList>
                     
                     </a:panel>
                     
                     </div>
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               
               <%-- Error Messages --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <%-- messages tag to show messages not handled by other specific message tags --%>
                     <h:messages globalOnly="true" styleClass="errorMessage" layout="table" />
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               
               <%-- separator row with bottom panel graphics --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_7.gif" width=4 height=4></td>
                  <td width=100% align=center style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_9.gif" width=4 height=4></td>
               </tr>
               
            </table>
          </td>
       </tr>
    </table>
    
    </h:form>
    
</f:view>

</r:page>
