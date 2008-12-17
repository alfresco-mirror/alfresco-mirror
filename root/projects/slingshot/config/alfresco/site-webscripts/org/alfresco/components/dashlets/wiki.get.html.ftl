<script type="text/javascript">//<![CDATA[
   new Alfresco.WikiDashlet("${args.htmlid}").setOptions({
      guid: "${instance.object.id}",
      siteId: "${page.url.templateArgs.site!""}",
      pages: [<#if (pageList?? && pageList.pages?size &gt; 0)><#list pageList.pages as p>"${p.name}"<#if p_has_next>, </#if></#list></#if>]
   });  
//]]></script>
<div class="dashlet wiki">
   <div class="title" id="${args.htmlid}-title">${msg("label.header-prefix")} - ${pageTitle!msg("label.header")}</div>
   <div class="toolbar">
       <a href="#" id="${args.htmlid}-wiki-link">${msg("label.configure")}</a>
   </div>
   <div class="body scrollableList">
      <div id="${args.htmlid}-scrollableList" class="rich-content">
<#if wikipage?exists>
         ${wikipage}
<#else>
		   ${msg("label.noConfig")}
</#if>
      </div>
	</div>
</div>