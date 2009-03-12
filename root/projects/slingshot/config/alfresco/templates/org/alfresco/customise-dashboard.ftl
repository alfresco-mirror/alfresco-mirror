<#include "include/alfresco-template.ftl" />
<#import "import/alfresco-layout.ftl" as layout />
<@templateHeader>
   <@link rel="stylesheet" type="text/css" href="${url.context}/templates/dashboard/customise-dashboard.css" />
</@>

<@templateBody>
<div id="hd">
   <@region id="header" scope="global" protected=true />
   <@region id="title" scope="template" protected=true />
   <@region id="navigation" scope="template" protected=true />
   <h1 class="sub-title"><#if page.titleId??>${msg(page.titleId)!page.title}<#else>${page.title}</#if></h1>
</div>
<div id="bd">
   <@region id="customise-layout" scope="template" protected=true />
   <@region id="customise-dashlets" scope="template" protected=true />
</div>
</@>

<@templateFooter>
   <div id="ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</@>