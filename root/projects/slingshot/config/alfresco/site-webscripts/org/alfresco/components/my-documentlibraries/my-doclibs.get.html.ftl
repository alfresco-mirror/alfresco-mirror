<#include "../../include/alfresco-macros.lib.ftl" />
<#macro siteHTML site>
   <#assign href>documentlibrary</#assign>
      <div class="icon"><a href="${siteURL(href, site.shortName)}"><img src="${url.context}/components/documentlibrary/images/folder-32.png" /></a></div>
      <div class="details">
         <h4><a href="${siteURL(href, site.shortName)}" class="theme-color-1">${(site.title!site.shortName)?html}</a></h4>
         <div>${(site.description!"")?html}</div>
      <#if site.visibility = "MODERATED">
         <span class="visibility theme-bg-color-1">${msg("label.moderated")}</span>
      <#elseif site.visibility = "PRIVATE">
         <span class="visibility theme-bg-color-1">${msg("label.private")}</span>
      </#if>
      </div>
</#macro>

<#assign el=args.htmlid>
<div id="${el}-body" class="my-doclibs">

   <div class="header-bar"><h2>${msg("label.header-my")}</h2></div>
<#if (mySites?size > 0)>
   <ul class="sites">
   <#list mySites as site>
      <li<#if (site_index == 0)> class="first"</#if>>
         <@siteHTML site />
      </li>
   </#list>
   </ul>
<#else>
   <p>${msg("label.no-site-memberships")}</p>
</#if>         

<#if (otherSites?size > 0)>
   <p>&nbsp;</p>
   <div class="header-bar"><h2>${msg("label.header-other")}</h2></div>
   <ul class="sites">
   <#list otherSites as site>
      <li<#if (site_index == 0)> class="first"</#if>>
         <@siteHTML site />
      </li>
   </#list>
   </ul>
</#if>         
</div>