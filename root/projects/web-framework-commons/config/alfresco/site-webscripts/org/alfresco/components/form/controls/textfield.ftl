<label>${item.label?html}:</label>

<#if form.mode == "view">
<span class="field">${item.value?html}</span>
<#else>
<input id="${item.id}" type="text" name="${item.name}" value="${item.value}" 
       <#if item.control.params.maxLength?exists>maxlength="${item.control.params.maxLength}"</#if>
       <#if item.control.params.width?exists>style="width: ${item.control.params.width};"</#if> />
</#if>