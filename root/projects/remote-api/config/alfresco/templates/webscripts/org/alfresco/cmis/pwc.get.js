<import resource="classpath:alfresco/templates/webscripts/org/alfresco/cmis/lib/read.lib.js">

script:
{
    var object = getObjectFromUrl();
    if (object.node === null)
    {
        break script;
    }
    model.node = object.node;
 
    // return version
    var returnVersion = args[cmis.ARG_RETURN_VERSION];
    if (returnVersion === null || returnVersion.length == 0)
    {
        returnVersion = "this";
    }
    model.node = cmis.getReturnVersion(model.node, returnVersion);
    
    // property filter 
    model.filter = args[cmis.ARG_FILTER];
    if (model.filter === null || model.filter == "")
    {
        model.filter = "*";
    }
   
    // ACL
    model.includeACL = args[cmis.ARG_INCLUDE_ACL] == "true";

    // rendition filter
    model.renditionFilter = args[cmis.ARG_RENDITION_FILTER];
    if (model.renditionFilter === null || model.renditionFilter.length == 0)
    {
        model.renditionFilter = "cmis:none";
    }

    // include allowable actions
    model.includeAllowableActions = args[cmis.ARG_INCLUDE_ALLOWABLE_ACTIONS] == "true";
}
