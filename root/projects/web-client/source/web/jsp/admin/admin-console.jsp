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

<r:page titleId="title_admin_console">

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages.webclient" var="msg"/>
   
   <h:form acceptCharset="UTF-8" id="admin-console">
   
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
                  <td bgcolor="#dfe6ed">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing="4" cellpadding="0" width="100%">
                        <tr>
                           <td width=32>
                              <h:graphicImage id="logo" url="/images/icons/admin_console_large.gif" width="32" height="32" />
                           </td>
                           <td>
                              <div class="mainTitle"><h:outputText value="#{msg.admin_console}" /></div>
                              <div class="mainSubText"><h:outputText value="#{msg.admin_description}" /></div>
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
                  <td valign=top>
                     
                     <table cellspacing=4 cellpadding=4 border=0 width=100%>
                        <tr>
                           <td width=100%>
                           
                                 <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "ballongrey", "#EEEEEE"); %>
                                 <table cellpadding="6" cellspacing="6" border="0" width="100%">
                                    <tr>
                                       <td><a:actionLink value="#{msg.manage_users}" image="/images/icons/users.gif" action="dialog:manageUsers" styleClass="title" actionListener="#{NewUserWizard.setupUsers}" /></td>
                                    </tr>
                                    <tr>
                                       <td><a:actionLink value="#{msg.manage_groups}" image="/images/icons/group.gif" action="dialog:manageGroups" styleClass="title" /></td>
                                    </tr>
                                    <tr>
                                       <td><a:actionLink value="#{msg.category_management}" image="/images/icons/categories.gif" action="dialog:manageCategories" styleClass="title" /></td>
                                    </tr>
                                    <tr>
                                       <td>
                                          <a:actionLink value="#{msg.import}" image="/images/icons/import.gif" action="dialog:import" actionListener="#{BrowseBean.setupSpaceAction}" styleClass="title">
                                             <f:param name="id" value="#{NavigationBean.currentNodeId}" />
                                          </a:actionLink>
                                       </td>
                                    </tr>
                                    <tr>
                                       <td>
                                          <a:actionLink value="#{msg.export}" image="/images/icons/export.gif" action="dialog:export" actionListener="#{BrowseBean.setupSpaceAction}" styleClass="title">
                                             <f:param name="id" value="#{NavigationBean.currentNodeId}" />
                                          </a:actionLink>
                                       </td>
                                    </tr>
                                    <tr>
                                       <td><a:actionLink value="#{msg.system_info}" image="/images/icons/info_icon.gif" action="dialog:showSystemInfo" styleClass="title" /></td>
                                    </tr>
                                    <tr>
                                       <td><a:actionLink value="#{msg.node_browser}" image="/images/icons/node_browser.gif" action="dialog:showNodeBrowser" styleClass="title" /></td>
                                    </tr>
                                 </table>
                                 <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "ballongrey"); %>
                           
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "greyround", "#F5F5F5"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.close}" action="dialog:close" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "greyround"); %>
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