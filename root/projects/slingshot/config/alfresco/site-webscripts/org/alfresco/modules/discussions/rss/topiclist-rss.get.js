<import resource="classpath:alfresco/site-webscripts/org/alfresco/modules/discussions/topics.lib.js">
<import resource="classpath:alfresco/site-webscripts/org/alfresco/paginationutils.js">

function main()
{
    
    // gather all required data
    var site = args["site"];
    var container = (args["container"] != undefined) ? args["container"] : "discussions";
    var path = (args["path"] != undefined) ? args["path"] : "";

    var paginationData = fetchPaginationDataFromRequest(0, 10);
    
    // fetch the data
    fetchAndAssignTopics(site, container, path, "" /*filter*/, "" /*tag*/, paginationData);

    // set additional properties
    // PENDING: where to get this information?
    var lang = "en-us";
    model.lang = lang;
    model.site = site;
    model.container = container;
}

main();
