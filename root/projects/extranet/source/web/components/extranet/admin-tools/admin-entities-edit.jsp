<%@ page import="java.util.*" %>
<%@ page import="org.alfresco.connector.*" %>
<%@ page import="org.alfresco.extranet.*" %>
<%@ page import="org.alfresco.extranet.database.*" %>
<%@ page import="org.alfresco.extranet.ldap.*" %>
<%@ page import="org.alfresco.extranet.webhelpdesk.*" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page buffer="0kb" contentType="text/html;charset=UTF-8" autoFlush="true"%>
<%@ page isELIgnored="false" %>
<%@ taglib uri="/WEB-INF/tlds/alf.tld" prefix="alf" %>
<%
	// safety check
	org.alfresco.connector.User user = org.alfresco.web.site.RequestUtil.getRequestContext(request).getUser();
	if(user == null || !user.isAdmin())
	{
		out.println("Access denied");
		return;
	}
%>
<%
	// get the selected object
	String entityId = request.getParameter("selectedId");
		
	// select the entity type
	String entityType = request.getParameter("entity_type");
	
	// get the appropriate entity service
	EntityService entityService = ExtranetHelper.getEntityService(request, entityType);

	// load the entity
	Entity entity = entityService.get(entityId);
	String entityTitle = entityType;
	String[] propertyNames = ExtranetHelper.getEntityPropertyNames(entityType);
		
	// command processing
	String command = request.getParameter("command");
	if("save".equals(command))
	{
		// store properties onto entity
		for(int i = 0; i < propertyNames.length; i++)
		{
			String value = request.getParameter(propertyNames[i]);
			if(value != null)
			{
				entity.setProperty(propertyNames[i], value);
			}
		}
		
		// update
		entityService.update(entity);
		
		out.println(entityTitle + " updated!");
		out.println("<br/>");
		out.println("<a href='?p=admin-tools&dispatchTo=admin-entities'>Entities</a>");
		
		return;
	}
%>
<html>
   <head><title>Add <%=entityTitle%></title></head>
   <body>
   	<form method="POST" action="/extranet/">
   		<input type="hidden" name="p" value="admin-tools"/>
   		<input type="hidden" name="dispatchTo" value="admin-entities-edit"/>
   		<input type="hidden" name="entity_type" value="<%=entityType%>"/>
   		<input type="hidden" name="selectedId" value="<%=entityId%>"/>

		<table>
<%
	for(int i = 0; i < propertyNames.length; i++)
	{
%>	
			<tr>
				<td><%=propertyNames[i]%></td>
				<td>
					<input name="<%=propertyNames[i]%>" value="<%=(entity.getProperty(propertyNames[i]) != null ? entity.getProperty(propertyNames[i]) : "")%>" />
				</td>
			</tr>
<%
	}
%>
		</table>
		
		<input type="submit" value="save" name="command" />
		<input type="button" value="cancel" onclick="window.location.href='?p=admin-tools&dispatchTo=admin-entities';" />
	</form>
			
   </body>
</html>
