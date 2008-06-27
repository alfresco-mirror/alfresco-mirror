<import resource="/org/alfresco/web/site/include/ads-support.js">





wizard.addGridColumn("associationId", "Association ID");
wizard.addGridColumn("xformtype", "Type");
wizard.addGridColumn("formatId", "Format");
wizard.addGridColumn("pageName", "Page");
wizard.addGridColumn("pageId", "PageId");

wizard.addGridColumnFormat("associationId", 120, true);
wizard.addGridColumnFormat("xformtype", 120, true);
wizard.addGridColumnFormat("formatId", 60, false);
wizard.addGridColumnFormat("pageName", 120, true);
wizard.addGridColumnFormat("pageId", 120, true);

wizard.addGridToolbar("add_content_template_association", "New Association", "New Association", "add");
wizard.addGridToolbarSpacer();
wizard.addGridToolbar("remove_content_template_association", "Remove Association", "Remove Association", "delete");

wizard.addGridNoDataMessage("There are no associations currently defined.");


// get all of the content template associations
var associations = sitedata.findContentAssociations(null, null, null, null);
for(var i = 0; i < associations.length; i++)
{
	var sourceId = associations[i].getProperty("source-id");
	if(sourceId != null)
	{
		var associationId = associations[i].getId();;
		var formatId = associations[i].getProperty("format-id");

		var destId = associations[i].getProperty("dest-id");
		var pageName = "NOT FOUND";
		var page = sitedata.getObject("page", destId);
		if(page != null)
		{
			pageName = page.getTitle();
		}

		var array = new Array();
		array[0] = associationId;
		array[1] = sourceId;
		array[2] = formatId;
		array[3] = pageName;
		array[4] = destId;
		
		wizard.addGridData(array);
	}	
}

