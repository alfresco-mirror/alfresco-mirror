<script type="text/javascript">//<![CDATA[
   new Alfresco.CreateComment("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!""}",
      containerId: "${args.container!"blog"}",
      height: ${args.editorHeight!250},
      width: ${args.editorWidth!538}
   }).setMessages(
      ${messages}
   );
//]]></script>

<div id="${args.htmlid}-form-container" class="addCommentForm hidden">
	<div class="commentFormTitle">
		<label for="${htmlid}-content">${msg("addComment")}:</label>
	</div>
	<div class="editComment">
		<form id="${htmlid}-form" method="post" action="">
		    <div>
            <input type="hidden" id="${args.htmlid}-nodeRef" name="nodeRef" value="" />
            <input type="hidden" id="${args.htmlid}-site" name="site" value="" />
            <input type="hidden" id="${args.htmlid}-container" name="container" value="" />
            <input type="hidden" id="${args.htmlid}-itemTitle" name="itemTitle" value="" />
            <input type="hidden" id="${args.htmlid}-page" name="page" value="" />
            <input type="hidden" id="${args.htmlid}-pageParams" name="pageParams" value="" />
            
			   <textarea id="${htmlid}-content" rows="8" cols="80" name="content"></textarea>
			</div>
			<div class="commentFormAction">
				<input type="submit" id="${htmlid}-submit" value="${msg('postComment')}" />
			</div>
		</form>
	</div>
</div>