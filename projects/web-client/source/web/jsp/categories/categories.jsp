<%--
  Copyright (C) 2005 Alfresco, Inc.
 
  Licensed under the Mozilla Public License version 1.1 
  with a permitted attribution clause. You may obtain a
  copy of the License at
 
    http://www.alfresco.org/legal/license.txt
 
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

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page titleId="title_categories_list">

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages.webclient" var="msg"/>
   
   <h:form acceptCharset="UTF-8" id="users">
   
   <%-- Main outer table --%>
   <table cellspacing="0" cellpadding="2">
      
      <%-- Title bar --%>
      <tr>
         <td colspan="2">
            <%@ include file="../parts/titlebar.jsp" %>
         </td>
      </tr>
      
      <%-- Main area --%>
      <tr valign="top">
         <%-- Shelf --%>
         <td>
            <%@ include file="../parts/shelf.jsp" %>
         </td>
         
         <%-- Work Area --%>
         <td width="100%">
            <table cellspacing="0" cellpadding="0" width="100%">
               <%-- Breadcrumb --%>
               <%@ include file="../parts/breadcrumb.jsp" %>
               
               <%-- Status and Actions --%>
               <tr>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_4.gif)" width="4"></td>
                  <td bgcolor="#EEEEEE">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing="4" cellpadding="0" width="100%">
                        <tr valign="top">
                           <td width="32">
                              <h:graphicImage id="logo" url="/images/icons/category.gif" />
                           </td>
                           <td>
                              <div class="mainSubTitle"><h:outputText value='#{NavigationBean.nodeProperties.name}' /></div>
                              <div class="mainTitle"><h:outputText value="#{msg.category_management}" /></div>
                              <div class="mainSubTitle">
                                 <%-- show either root message or the current category name --%>
                                 <h:outputText value="#{msg.categories}" rendered="#{CategoriesBean.currentCategoryId == null}" />
                                 <h:outputText value="#{CategoriesBean.currentCategory.name}" rendered="#{CategoriesBean.currentCategoryId != null}" />
                              </div>
                              <div class="mainSubText"><h:outputText value="#{msg.categories_description}" /></div>
                           </td>
                           <td bgcolor="#465F7D" width=1></td>
                           <td width=110 style="padding-left:2px">
                              <%-- Current object actions --%>
                              <h:outputText style="padding-left:20px;" styleClass="mainSubTitle" value="#{msg.actions}" /><br/>
                              <a:actionLink value="#{msg.add_category}" image="/images/icons/add_category.gif" padding="4" action="addCategory" actionListener="#{CategoriesBean.clearCategoryAction}" />
                              <a:booleanEvaluator value="#{CategoriesBean.currentCategoryId != null}">
                                 <a:actionLink value="#{msg.edit_category}" image="/images/icons/edit_category.gif" padding="4" action="editCategory" actionListener="#{CategoriesBean.setupCategoryAction}">
                                    <f:param name="id" value="#{CategoriesBean.currentCategoryId}" />
                                 </a:actionLink>
                                 <a:actionLink value="#{msg.delete_category}" image="/images/icons/delete_category.gif" padding="4" action="deleteCategory" actionListener="#{CategoriesBean.setupCategoryAction}">
                                    <f:param name="id" value="#{CategoriesBean.currentCategoryId}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                           </td>
                           <td bgcolor="#465F7D" width=1></td>
                           <td width=100 style="padding-left:2px">
                              <%-- View mode settings --%>
                              <h:outputText style="padding-left:26px" styleClass="mainSubTitle" value="#{msg.view}"/><br>
                              <a:modeList itemSpacing="3" iconColumnWidth="20" selectedStyleClass="statusListHighlight" selectedImage="/images/icons/Details.gif" value="0">
                                 <a:listItem value="0" label="#{msg.category_icons}" />
                              </a:modeList>
                           </td>
                        </tr>
                     </table>
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_6.gif)" width="4"></td>
               </tr>
               
               <%-- separator row with gradient shadow --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_7.gif" width="4" height="9"></td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_9.gif" width="4" height="9"></td>
               </tr>
               
               <%-- Details --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width="4"></td>
                  <td>
                     <table cellspacing="0" cellpadding="3" border="0" width="100%">
                        <tr>
                           <td width="100%" valign="top">
                              
                              <%-- Category Path Breadcrumb --%>
                              <div style="padding-left:8px;padding-top:4px;padding-bottom:4px">
                                 <a:breadcrumb value="#{CategoriesBean.location}" styleClass="title" />
                              </div>
                              
                              <%-- Categories List --%>
                              <div style="padding:4px">
                              
                              <a:panel id="categories-panel" border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE" styleClass="mainSubTitle" label="#{msg.items}">
                              
                              <a:richList id="categories-list" binding="#{CategoriesBean.categoriesRichList}" viewMode="icons" pageSize="15"
                                    styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                                    value="#{CategoriesBean.categories}" var="r" initialSortColumn="name" initialSortDescending="true">
                                 
                                 <%-- Primary column for icons view mode --%>
                                 <a:column primary="true" style="padding:2px;text-align:left;vertical-align:top;">
                                    <f:facet name="large-icon">
                                       <a:actionLink value="#{r.name}" image="/images/icons/category.gif" actionListener="#{CategoriesBean.clickCategory}" showLink="false">
                                          <f:param name="id" value="#{r.id}" />
                                       </a:actionLink>
                                    </f:facet>
                                    <a:actionLink value="#{r.name}" actionListener="#{CategoriesBean.clickCategory}" styleClass="header">
                                       <f:param name="id" value="#{r.id}" />
                                    </a:actionLink>
                                 </a:column>
                                 
                                 <%-- Actions column --%>
                                 <a:column actions="true" style="text-align:left">
                                    <f:facet name="header">
                                       <h:outputText value="#{msg.actions}"/>
                                    </f:facet>
                                    <a:actionLink value="#{msg.modify}" image="/images/icons/edit_category.gif" showLink="false" action="editCategory" style="padding-right:2px" actionListener="#{CategoriesBean.setupCategoryAction}">
                                       <f:param name="id" value="#{r.id}" />
                                    </a:actionLink>
                                    <a:actionLink value="#{msg.delete}" image="/images/icons/delete_category.gif" showLink="false" action="deleteCategory" actionListener="#{CategoriesBean.setupCategoryAction}">
                                       <f:param name="id" value="#{r.id}" />
                                    </a:actionLink>
                                 </a:column>
                                 
                                 <a:dataPager/>
                              </a:richList>
                              
                              </a:panel>
                     
                              <div>
                              
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.close}" action="adminConsole" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "blue"); %>
                           </td>
                        </tr>
                     </table>
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width="4"></td>
               </tr>
               
               <%-- separator row with bottom panel graphics --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_7.gif" width="4" height="4"></td>
                  <td width="100%" align="center" style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_9.gif" width="4" height="4"></td>
               </tr>
               
            </table>
          </td>
       </tr>
    </table>
    
    </h:form>
    
</f:view>

</r:page>