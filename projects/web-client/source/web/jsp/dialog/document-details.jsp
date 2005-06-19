<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="64kb" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page>

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="messages" var="msg"/>
   
   <h:form id="document-details">
   
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
                           <td width="26">
                              <h:graphicImage id="wizard-logo" url="/images/icons/file_large.gif" />
                           </td>
                           <td>
                              <div class="mainSubTitle"><h:outputText value="#{NavigationBean.nodeProperties.name}" /></div>
                              <div class="mainTitle">
                                 Details of '<h:outputText value="#{DocumentDetailsBean.name}" />'
                                 <h:graphicImage url="/images/icons/locked.gif" rendered="#{DocumentDetailsBean.locked == true}" />
                              </div>
                              <div class="mainSubText"><h:outputText value="#{msg.documentdetails_description}" /></div>
                           </td>
                           <td bgcolor="#465F7D" width=1></td>
                           <td width=100 style="padding-left:2px">
                              <%-- Current object actions --%>
                              <h:outputText style="padding-left:20px" styleClass="mainSubTitle" value="#{msg.actions}" />
                                                            
                              <%-- checkin, checkout and undo checkout --%>
                              <a:booleanEvaluator value="#{DocumentDetailsBean.locked == false && DocumentDetailsBean.workingCopy == false}">
                                 <a:actionLink value="#{msg.checkout}" image="/images/icons/CheckOut_icon.gif" padding="4"
                                               actionListener="#{CheckinCheckoutBean.setupContentAction}" action="checkoutFile">
                                    <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{DocumentDetailsBean.workingCopy == true}">
                                 <a:actionLink value="#{msg.checkin}" image="/images/icons/CheckIn_icon.gif" padding="4"
                                               actionListener="#{CheckinCheckoutBean.setupContentAction}" action="checkinFile">
                                    <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{DocumentDetailsBean.workingCopy == true}">
                                 <a:actionLink value="#{msg.undocheckout}" image="/images/icons/UndoCheckOut_icon.gif" padding="4" 
                                               actionListener="#{CheckinCheckoutBean.setupContentAction}" action="undoCheckoutFile">
                                    <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              
                              <%-- approve and reject --%>
                              <a:booleanEvaluator value="#{DocumentDetailsBean.approveStepName != null}">
                                 <a:actionLink value="#{DocumentDetailsBean.approveStepName}" image="/images/icons/forward.gif" padding="4"
                                               actionListener="#{DocumentDetailsBean.approve}" action="browse">
                                    <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              <a:booleanEvaluator value="#{DocumentDetailsBean.rejectStepName != null}">
                                 <a:actionLink value="#{DocumentDetailsBean.rejectStepName}" image="/images/icons/reply.gif" padding="4"
                                               actionListener="#{DocumentDetailsBean.reject}" action="browse">
                                    <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                 </a:actionLink>
                              </a:booleanEvaluator>
                              
                              <a:booleanEvaluator value="#{DocumentDetailsBean.locked == true}">
                                 <f:verbatim><br/><br/></f:verbatim>
                              </a:booleanEvaluator>
                              
                              <a:menu itemSpacing="4" image="/images/icons/more.gif" menuStyleClass="moreActionsMenu"
                                      label="More..." tooltip="More Actions for this document" style="padding-left:20px">
                                 <%-- edit, update and cut --%>
                                 <a:booleanEvaluator value="#{DocumentDetailsBean.locked == false}">
                                    <a:actionLink value="#{msg.edit}" image="/images/icons/edit_icon.gif" padding="4" 
                                                  actionListener="#{CheckinCheckoutBean.editFile}">
                                       <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                    </a:actionLink>
                                    <a:actionLink value="#{msg.update}" image="/images/icons/update.gif" padding="4" 
                                                  actionListener="#{CheckinCheckoutBean.setupContentAction}" action="updateFile">
                                       <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                    </a:actionLink>
                                    <a:actionLink value="#{msg.cut}" image="/images/icons/cut.gif" padding="4" 
                                                  actionListener="#{ClipboardBean.cutNode}">
                                       <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                    </a:actionLink>
                                    
                                 </a:booleanEvaluator>
                                 
                                 <%-- copy --%>
                                 <a:actionLink value="#{msg.copy}" image="/images/icons/copy.gif" padding="4" 
                                               actionListener="#{ClipboardBean.copyNode}">
                                    <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                 </a:actionLink>
                                    
                                 <%-- delete --%>
                                 <a:booleanEvaluator value="#{DocumentDetailsBean.locked == false && DocumentDetailsBean.workingCopy == false}">
                                    <a:actionLink value="#{msg.delete}" image="/images/icons/delete.gif" padding="4"
                                                  actionListener="#{BrowseBean.setupContentAction}" action="deleteFile">
                                       <f:param name="id" value="#{DocumentDetailsBean.id}" />
                                    </a:actionLink>
                                 </a:booleanEvaluator>
                              </a:menu>
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
                              <a:panel label="Preview" id="preview-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE">
                                 <a:actionLink value="#{DocumentDetailsBean.name}" href="#{DocumentDetailsBean.url}" />
                              </a:panel>
                              <br/>
                              <a:panel label="Properties" id="properties-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE"
                                       action="editDocProperties" linkIcon="/images/icons/Change_details.gif"
                                       actionListener="#{EditDocPropsDialog.startWizardForEdit}">
                                 <r:propertySheetGrid id="document-props" value="#{DocumentDetailsBean.document}" var="documentProps" 
                                                      columns="1" mode="view" labelStyleClass="propertiesLabel" 
                                                      externalConfig="true" />
                                 <h:messages styleClass="errorMessage" />
                              </a:panel>
                              <br/>
                              <a:panel label="Workflow" id="workflow-panel" progressive="true" expanded="false"
                                       border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE">
                                 <h:outputText id="workflow-overview" value="#{DocumentDetailsBean.workflowOverviewHTML}" 
                                               escape="false" />
                              </a:panel>
                              <br/>
                              <a:panel label="Categories" id="categories-panel" progressive="true" expanded="false"
                                       border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE">
                                 Categories
                              </a:panel>
                              <br/>
                              <a:panel label="Version History" id="version-history-panel" progressive="true" expanded="false"
                                       border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE">
                                       
                                 <a:richList id="versionHistoryList" viewMode="details" value="#{DocumentDetailsBean.versionHistory}" 
                                             var="r" styleClass="recordSet" headerStyleClass="recordSetHeader" 
                                             rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%" 
                                             pageSize="10" initialSortColumn="versionLabel" initialSortDescending="false">
                        
                                    <%-- Primary column for details view mode --%>
                                    <a:column id="col1" primary="true" width="100" style="padding:2px;text-align:left">
                                       <f:facet name="header">
                                          <a:sortLink label="Version" value="versionLabel" mode="case-insensitive" styleClass="header"/>
                                       </f:facet>
                                       <a:actionLink id="label" value="#{r.versionLabel}" href="#{r.url}" />
                                    </a:column>
                                    
                                    <%-- Description columns --%>
                                    <a:column id="col2" style="text-align:left">
                                       <f:facet name="header">
                                          <a:sortLink label="Author" value="author" styleClass="header"/>
                                       </f:facet>
                                       <h:outputText id="author" value="#{r.author}" />
                                    </a:column>
                                    
                                    <%-- Created Date column for details view mode --%>
                                    <a:column id="col3" style="text-align:left">
                                       <f:facet name="header">
                                          <a:sortLink label="Date" value="versionDate" styleClass="header"/>
                                       </f:facet>
                                       <h:outputText id="date" value="#{r.versionDate}">
                                          <a:convertXMLDate dateStyle="long" type="both" timeStyle="short" />
                                       </h:outputText>
                                    </a:column>
                                    
                                    <%-- view the contents of the specific version --%>
                                    <a:column id="col4" style="text-align: left">
                                       <f:facet name="header">
                                          <h:outputText value="#{msg.actions}"/>
                                       </f:facet>
                                       <a:actionLink id="view-link" value="View" href="#{r.url}" />
                                    </a:column>
              
                                 </a:richList>
                              </a:panel>
                              <br/>
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="Close" action="browse" styleClass="wizardButton" />
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