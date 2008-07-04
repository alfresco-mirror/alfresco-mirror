
<div id="${args.htmlid}-pagelist" class="yui-navset"> 
<#if pageList.pages?size &gt; 0>
<#list pageList.pages as p>
   <div class="wikipage">
   <div class="pageTitle"><a class="pageTitle" href="${url.context}/page/site/${page.url.templateArgs.site}/wiki-page?title=${p.name}">${p.title}</a></div>
   <div class="publishedDetails">
         <span class="attrLabel">Created by:</span> <span class="attrValue"><a href="">${p.createdBy}</a></span>
   		<span class="spacer"> | </span>
			<span class="attrLabel">Created on:</span> <span class="attrValue">${p.createdOn}</span>
			<span class="spacer"> | </span>
			<span class="attrLabel">Modified by:</span> <span class="attrValue"><a href="#">${p.modifiedBy}</a></span>
			<span class="spacer"> | </span>
			<span class="attrLabel">Modified on:</span> <span class="attrValue">${p.modifiedOn}</span>
	</div>
   <div class="pageCopy">${p.text}</div>
   </div>
</#list>
<#else>
There are currently no pages to display.
</#if>
</div>