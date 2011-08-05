<#macro aclChangeSetJSON aclChangeSet>
      {
         "id": ${aclChangeSet.id?c},
         "commitTimeMs": ${aclChangeSet.commitTimeMs?c},
         "aclCount": ${aclChangeSet.aclCount?c}
      }
</#macro>

<#macro aclJSON acl>
      {
         "id": ${acl.id?c},
         "aclChangeSetId": ${acl.aclChangeSetId?c}
      }
</#macro>

<#macro aclReadersJSON aclReaders>
      {
         "aclId": ${aclReaders.aclId?c},
         "readers" :
         [
            <#list aclReaders.readers as reader>
               ${reader?string}
               <#if reader_has_next>,</#if>
            </#list>
         ]
      }
</#macro>

<#macro transactionJSON txn>
      {
         "id": ${txn.id?c},
         "commitTimeMs": ${txn.commitTimeMs?c},
         "updates": ${txn.updates?c},
         "deletes": ${txn.deletes?c}
      }
</#macro>

<#macro nodeJSON node>
      {
         "id": ${node.id?c},
         "txnId": ${node.transaction.id?c},
         "status": "<#if node.deleted>d<#else>u</#if>"
      }
</#macro>

<#macro nodeMetaDataJSON nodeMetaData filter>
      {
         "id": ${nodeMetaData.nodeId?c},
         "nodeRef": <#if filter.includeNodeRef??>"${nodeMetaData.nodeRef.toString()}",</#if>
         "type": <#if filter.includeType??><@qNameJSON qName=nodeMetaData.nodeType/>,</#if>
         "aclId": <#if filter.includeAclId??><#if nodeMetaData.aclId??>${nodeMetaData.aclId?c}<#else>null</#if>,</#if>
         <#if filter.includeProperties??>
         "properties": {
           <#list nodeMetaData.properties?keys as propName>
               "${propName}": ${nodeMetaData.properties[propName]}<#if propName_has_next>,</#if>
           </#list>
         },
         </#if>
         <#if filter.includeAspects??>
         "aspects": [
           <#list nodeMetaData.aspects as aspectQName>
               <@nodeAspectJSON aspectQName=aspectQName indent=""/><#if aspectQName_has_next>,</#if>
           </#list>
         ],
         </#if>
         <#if filter.includePaths??>
         "paths": [
           <#list nodeMetaData.paths as path>
           ${path}<#if path_has_next>,</#if>
           </#list>
         ],
         </#if>
         <#if filter.includeParentAssociations??>
         <#if nodeMetaData.parentAssocs??>
         <#if (nodeMetaData.parentAssocs?size > 0)>
         "parentAssocs": [
           <#list nodeMetaData.parentAssocs as pa>
           "${pa}"<#if pa_has_next>,</#if>
           </#list>
         ],
         "parentAssocsCrc": <#if nodeMetaData.parentAssocsCrc??>${nodeMetaData.parentAssocsCrc?c}<#else>null</#if>,
         </#if>
         </#if>
         </#if>
         <#if filter.includeChildAssociations??>
         <#if nodeMetaData.childAssocs??>
         <#if (nodeMetaData.childAssocs?size > 0)>
         "childAssocs": [
           <#list nodeMetaData.childAssocs as ca>
           "${ca}"<#if ca_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         </#if>
      }
</#macro>

<#macro pathJSON path indent="">
${indent}[
<#list path as element>
${indent}${element}<#if element_has_next>,</#if>
</#list>
${indent}]
</#macro>

<#macro qNameJSON qName indent="">
${indent}"${jsonUtils.encodeJSONString(shortQName(qName))}"
</#macro>

<#macro nodePropertyJSON propQName propValue>
<@qNameJSON qName=propQName/>: <#if propValue??>"propValue"<#else>null</#if>
</#macro>

<#macro nodeAspectJSON aspectQName indent="">
${indent}<@qNameJSON qName=aspectQName/>
</#macro>


