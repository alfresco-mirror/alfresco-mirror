<#macro printRuleConfig el class configType msgKey enableButton relationMenu>
<script type="text/javascript">//<![CDATA[
   new ${component}("${el}").setOptions(
   {
      <#if (menuMap?exists)>menuMap: ${menuMap},</#if>
      <#if (ruleConfigDefinitions?exists)>ruleConfigDefinitions: ${ruleConfigDefinitions},</#if>
      <#if (customisationsMap?exists)>customisationsMap: ${customisationsMap},</#if>
      <#if (constraints?exists)>constraints: ${constraints},</#if>
      ruleConfigType: "${ruleConfigType}"
   }).setMessages(
      ${messages}
   );
//]]></script>
<div id="${el}-body" class="rule-config ${configType}">
   <div class="rule-config-header">
      <#if enableButton?length &gt; 0>
      <div class="rule-config-title">
         <input type="checkbox" id="${el}-${configType}-checkbox" name="-" <#if enableButton == "checked">checked</#if>>
         <label for="${el}-${configType}-checkbox">${msg("header." + msgKey)}</label>
      </div>
      <#else>
      <div class="rule-config-title">${msg("header." + msgKey)}</div>
      </#if>
      <#if relationMenu>
      <input class="rule-config-relation" type="button" id="${el}-${configType}-menubutton" name="${el}-${configType}-menubutton_button" value="${msg("label.and")}">
      <select id="${el}-${configType}-menubuttonselect" name="${el}-${configType}-menubuttonselect">
          <option value="and">${msg("label.and")}</option>
          <option value="or">${msg("label.or")}</option>
      </select>
      <div class="clear"></div>
      </#if>
   </div>
   <ul id="${el}-configs" class="rule-config-body <#if enableButton == "unchecked">hidden</#if>">
   </ul>
</div>
<div class="hidden">
   <li id="${el}-configTemplate" class="config">
      <input type="hidden" name="id" value=""/>
      <div class="actions"><!-- add and remove buttons will be placed here--></div>
      <div class="name"><!-- select element will be placed in here --></div>
      <div class="parameters"><!-- parameter controls will be placed here--></div>
   </li>
</div>
</#macro>