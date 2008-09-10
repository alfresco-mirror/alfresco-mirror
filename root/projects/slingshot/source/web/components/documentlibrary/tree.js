/**
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * DocumentList TreeView component.
 * 
 * @namespace Alfresco
 * @class Alfresco.DocListTree
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event,
      Element = YAHOO.util.Element;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
    * DocumentList TreeView constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.DocListTree} The new DocListTree instance
    * @constructor
    */
   Alfresco.DocListTree = function DLT_constructor(htmlId)
   {
      // Mandatory properties
      this.name = "Alfresco.DocListTree";
      this.id = htmlId;
      
      // Initialise prototype properties
      this.widgets = {};
      this.currentFilter =
      {
         filterId: null,
         filterOwner: null,
         filterData: null
      };
      this.pathsToExpand = [];

      // Register this component
      Alfresco.util.ComponentManager.register(this);
      
      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["treeview"], this.onComponentsLoaded, this);
      
      // Decoupled event listeners
      YAHOO.Bubbling.on("pathChanged", this.onPathChanged, this);
      YAHOO.Bubbling.on("folderCopied", this.onFolderCopied, this);
      YAHOO.Bubbling.on("folderCreated", this.onFolderCreated, this);
      YAHOO.Bubbling.on("folderDeleted", this.onFolderDeleted, this);
      YAHOO.Bubbling.on("folderMoved", this.onFolderMoved, this);
      YAHOO.Bubbling.on("folderRenamed", this.onFolderRenamed, this);
      YAHOO.Bubbling.on("filterChanged", this.onFilterChanged, this);

      return this;
   }
   
   Alfresco.DocListTree.prototype =
   {
      /**
       * Object container for initialization options
       */
      options:
      {
         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",

         /**
          * ContainerId representing root container
          *
          * @property containerId
          * @type string
          * @default "documentLibrary"
          */
         containerId: "documentLibrary"
      },
      
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,

      /**
       * Flag set after TreeView instantiated.
       * 
       * @property isReady
       * @type boolean
       */
      isReady: false,

      /**
       * Initial path on page load.
       * 
       * @property initialPath
       * @type string
       */
      initialPath: null,

      /**
       * Initial filter on page load.
       * 
       * @property initialFilter
       * @type string
       */
      initialFilter: null,

      /**
       * Current path being browsed.
       * 
       * @property currentPath
       * @type string
       */
      currentPath: "",

      /**
       * Currently active filter
       * 
       * @property currentFilter
       * @type object
       */
       currentFilter: null,

      /**
       * Tracks if this component is the active filter owner.
       * 
       * @property isFilterOwner
       * @type boolean
       */
      isFilterOwner: false,

      /**
       * Paths we have to expand as a result of a deep navigation event.
       * 
       * @property pathsToExpand
       * @type array
       */
      pathsToExpand: null,

      /**
       * Selected tree node.
       * 
       * @property selectedNode
       * @type {YAHOO.widget.Node}
       */
      selectedNode: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.DocListTree} returns 'this' for method chaining
       */
      setOptions: function DLT_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },

      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.DocListTree} returns 'this' for method chaining
       */
      setMessages: function DLT_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function DLT_onComponentsLoaded()
      {
         // Register the onReady callback when the component parent element has been loaded
         Event.onContentReady(this.id, this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting
       * @method onReady
       */
      onReady: function DLT_onReady()
      {
         // Reference to self - used in inline functions
         var me = this;
         
         /**
          * Dynamically loads TreeView nodes.
          * This MUST be inline in order to have access to the Alfresco.DocListTree class.
          * @method fnLoadNodeData
          * @param node {object} Parent node
          * @param fnLoadComplete {function} Expanding node's callback function
          */
         this.fnLoadNodeData = function DLT_oR_fnLoadNodeData(node, fnLoadComplete)
         {
            // Get the path this node refers to
            var nodePath = node.data.path;

            // Prepare URI for XHR data request
            var uri = me._buildTreeNodeUrl.call(me, nodePath);

            // Prepare the XHR callback object
            var callback =
            {
               success: function DLT_lND_success(oResponse)
               {
                  var results = eval("(" + oResponse.responseText + ")");

                  if (results.items)
                  {
                     for (var i = 0, j = results.items.length; i < j; i++)
                     {
                        var item = results.items[i];
                        var tempNode = new YAHOO.widget.TextNode(
                        {
                           label: item.name,
                           path: nodePath + "/" + item.name,
                           nodeRef: item.nodeRef,
                           description: item.description
                        }, node, false);

                        if (!item.hasChildren)
                        {
                           tempNode.isLeaf = true;
                        }
                     }
                  }
                  
                  /**
                  * Execute the node's loadComplete callback method which comes in via the argument
                  * in the response object
                  */
                  oResponse.argument.fnLoadComplete();
               },

               // If the XHR call is not successful, fire the TreeView callback anyway
               failure: function DLT_lND_failure(oResponse)
               {
                  if (oResponse.status == 401)
                  {
                     // Our session has likely timed-out, so refresh to offer the login page
                     window.location.reload();
                  }
                  else
                  {
                     try
                     {
                        var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                        
                        // Get the "Documents" node
                        var rootNode = this.widgets.treeview.getRoot();
                        var docNode = rootNode.children[0];
                        docNode.isLoading = false;
                        docNode.isLeaf = true;
                        docNode.label = response.message;
                        docNode.labelStyle = "ygtverror";
                        rootNode.refresh();
                     }
                     catch(e)
                     {
                     }
                  }
               },
               
               // Callback function scope
               scope: me,

               // XHR response argument information
               argument:
               {
                  "node": node,
                  "fnLoadComplete": fnLoadComplete
               }
            };

            // Make the XHR call using Connection Manager's asyncRequest method
            YAHOO.util.Connect.asyncRequest('GET', uri, callback);
         };

         // Build the TreeView widget
         this._buildTree();
         
         this.isReady = true;
         if (this.initialPath !== null)
         {
            // We missed the pathChanged event, so fake it here
            this.onPathChanged("pathChanged",
            [
               null,
               {
                  path: this.initialPath
               }
            ]);
         }
         if (this.initialFilter !== null)
         {
            // We missed the pathChanged event, so fake it here
            this.onFilterChanged("filterChanged",
            [
               null,
               this.initialFilter
            ]);
         }
      },

      /**
       * Fired by YUI TreeView when a node has finished expanding
       * @method onExpandComplete
       * @param oNode {YAHOO.widget.Node} the node recently expanded
       */
      onExpandComplete: function DLT_onExpandComplete(oNode)
      {
         // Make sure the tree's Dom has been updated
         this.widgets.treeview.draw();
         // Redrawing the tree will clear the highlight
         if (this.isFilterOwner)
         {
            this._showHighlight(true);
         }
         
         if (this.pathsToExpand.length > 0)
         {
            var node = this.widgets.treeview.getNodeByProperty("path", this.pathsToExpand.shift());
            if (node !== null)
            {
               if (node.data.path == this.currentPath)
               {
                  this._updateSelectedNode(node);
               }
               node.expand();
            }
         }
         else if (this.initialFilter !== null)
         {
            // We missed the filterChanged event, so fake it here
            this.onFilterChanged("filterChanged",
            [
               null,
               {
                  filterId: this.initialFilter.filterId,
                  filterOwner: this.initialFilter.filterOwner,
                  filterData: this.initialFilter.filterData
               }
            ]);
            this.initialFilter = null;
         }
      },

      /**
       * Fired by YUI TreeView when a node label is clicked
       * @method onNodeClicked
       * @param node {YAHOO.widget.Node} the node clicked
       * @return allowExpand {boolean} allow or disallow node expansion
       */
      onNodeClicked: function DLT_onNodeClicked(node)
      {
         this._updateSelectedNode(node);
         
         // Fire the filter changed event
         YAHOO.Bubbling.fire("filterChanged",
         {
            filterId: "path",
            filterOwner: this.name,
            filterdata: node.data.path
         });
         // Fire the path changed event
         YAHOO.Bubbling.fire("pathChanged",
         {
            path: node.data.path
         });
         
         // Prevent the tree node from expanding (TODO: user preference?)
         return false;
      },

      
      /**
       * BUBBLING LIBRARY EVENT HANDLERS FOR PAGE EVENTS
       * Disconnected event handlers for inter-component event notification
       */

      /**
       * Fired when the path has changed
       * @method onPathChanged
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onPathChanged: function DLT_onPathChanged(layer, args)
      {
         var obj = args[1];
         if ((obj !== null) && (obj.path !== null))
         {
            // ensure path starts with leading slash if not the root node
            if ((obj.path != "") && (obj.path.charAt(0) != "/"))
            {
               obj.path = "/" + obj.path;
            }
            // Defer if event received before we're ready
            if (!this.isReady)
            {
               this.initialPath = obj.path;
               return;
            }
            
            this.currentPath = obj.path;
            
            // check we're the current filter owner
            if (this.currentFilter.filterOwner != this.name)
            {
               YAHOO.Bubbling.fire("filterChanged",
               {
                  filterOwner: this.name,
                  filterId: "path",
                  filterdata: this.currentPath
               });
            }

            // Search the tree to see if this path's node is expanded
            var node = this.widgets.treeview.getNodeByProperty("path", obj.path);
            if (node !== null)
            {
               // Node found
               this._updateSelectedNode(node);
               node.expand();
               while (node.parent !== null)
               {
                  node = node.parent;
                  node.expand();
               }
               return;
            }
            
            /**
             * The path's node hasn't been loaded into the tree. Create a stack
             * of parent paths that we need to expand one-by-one in order to
             * eventually display the current path's node
             */
            var paths = obj.path.split("/");
            var expandPath = "";
            for (var i = 0; i < paths.length; i++)
            {
               if (paths[i] != "")
               {
                  // Push the path onto the list of paths to be expanded
                  expandPath += "/" + paths[i];
                  this.pathsToExpand.push(expandPath);
               }
            }
            
            // Kick off the expansion process by expanding the root node
            node = this.widgets.treeview.getNodeByProperty("path", "");
            if (node !== null)
            {
               node.expand();
            }
         }
      },
      
      /**
       * Fired when a folder has been renamed
       * @method onFolderRenamed
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFolderRenamed: function DLT_onFolderRenamed(layer, args)
      {
         var obj = args[1];
         if (obj && (obj.file !== null))
         {
            var node = this.widgets.treeview.getNodeByProperty("nodeRef", obj.file.nodeRef);
            if (node !== null)
            {
               // Node found, so rename it
               node.label = obj.file.displayName;
               this.widgets.treeview.draw();
               this._showHighlight(true);
            }
         }
      },

      /**
       * Fired when a folder has been copied
       *
       * Event data contains:
       *    nodeRef - the nodeRef of the newly copied object
       *    destination - new parent path
       * @method onFolderCopied
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFolderCopied: function DLT_onFolderCopied(layer, args)
      {
         var destPath;
         var obj = args[1];
         if (obj !== null)
         {
            if (obj.nodeRef && obj.destination)
            {
               destPath = obj.destination;
               
               // ensure path starts with leading slash if not the root node
               if ((destPath != "") && (destPath.charAt(0) != "/"))
               {
                  destPath = "/" + destPath;
               }
               
               // Do we have the parent of the node's copy loaded?
               var nodeDest = this.widgets.treeview.getNodeByProperty("path", destPath);
               if (nodeDest)
               {
                  if (nodeDest.expanded)
                  {
                     this._sortNodeChildren(nodeDest);
                  }
                  else
                  {
                     nodeDest.isLeaf = false;
                  }
               }
               
               this.widgets.treeview.draw();
               this._showHighlight(true);
            }
         }
      },

      /**
       * Fired when a folder has been created
       * @method onFolderCreated
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFolderCreated: function DLT_onFolderCreated(layer, args)
      {
         var obj = args[1];
         if ((obj !== null) && (obj.path !== null))
         {
            // ensure path starts with leading slash if not the root node
            if ((obj.parentPath != "") && (obj.parentPath.charAt(0) != "/"))
            {
               obj.parentPath = "/" + obj.parentPath;
            }
            var parentNode = this.widgets.treeview.getNodeByProperty("path", obj.parentPath);
            this._sortNodeChildren(parentNode);
         }
      },

      /**
       * Fired when a folder has been deleted
       * @method onFolderDeleted
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFolderDeleted: function DLT_onFolderDeleted(layer, args)
      {
         var obj = args[1];
         if (obj !== null)
         {
            var node = null;
            
            if (obj.path)
            {
               // ensure path starts with leading slash if not the root node
               if ((obj.path != "") && (obj.path.charAt(0) != "/"))
               {
                  obj.path = "/" + obj.path;
               }
               node = this.widgets.treeview.getNodeByProperty("path", obj.path);
            }
            else if (obj.nodeRef)
            {
               node = this.widgets.treeview.getNodeByProperty("nodeRef", obj.nodeRef);
            }
            
            if (node !== null)
            {
               var parentNode = node.parent;
               // Node found, so delete it
               this.widgets.treeview.removeNode(node);
               // Have all the parent child nodes been removed now?
               if (parentNode != null)
               {
                  if (!parentNode.hasChildren())
                  {
                     parentNode.isLeaf = true;
                  }
               }
               this.widgets.treeview.draw();
               this._showHighlight(true);
            }
         }
      },

      /**
       * Fired when a folder has been moved
       *
       * Event data contains:
       *    nodeRef - the nodeRef of the moved object
       *    destination - new parent path
       * @method onFolderMoved
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFolderMoved: function DLT_onFolderMoved(layer, args)
      {
         var obj = args[1];
         if (obj !== null)
         {
            if (obj.nodeRef && obj.destination)
            {
               var nodeSrc = null;
               var dest = obj.destination;

               // ensure path starts with leading slash if not the root node
               if ((dest != "") && (dest.charAt(0) != "/"))
               {
                  dest = "/" + dest;
               }
               
               // we should be able to find the original node
               nodeSrc = this.widgets.treeview.getNodeByProperty("nodeRef", obj.nodeRef);
            
               if (nodeSrc !== null)
               {
                  var parentNode = nodeSrc.parent;
                  // Node found, so delete it
                  this.widgets.treeview.removeNode(nodeSrc, true);
                  // Have all the parent child nodes been removed now?
                  if (parentNode != null)
                  {
                     if (!parentNode.hasChildren())
                     {
                        parentNode.isLeaf = true;
                     }
                  }
                  // Do we have the node's new parent loaded?
                  var nodeDest = this.widgets.treeview.getNodeByProperty("path", dest);
                  if (nodeDest)
                  {
                     if (nodeDest.expanded)
                     {
                        this._sortNodeChildren(nodeDest);
                     }
                     else
                     {
                        nodeDest.isLeaf = false;
                     }
                  }
                  
                  this.widgets.treeview.draw();
                  this._showHighlight(true);
               }
            }
         }
      },

      /**
       * Fired when the currently active filter has changed
       * @method onFilterChanged
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFilterChanged: function DLT_onFilterChanged(layer, args)
      {
         var obj = args[1];
         if ((obj !== null) && (obj.filterId !== null))
         {
            // Defer if event received before we're ready
            if (!this.isReady)
            {
               this.initialFilter =
               {
                  filterId: obj.filterId,
                  filterOwner: obj.filterOwner,
                  filterData: obj.filterData
               };
               return;
            }
            
            this.initialFilter = null;
            
            /**
             * If this is the first filterChanged event and it's not a path then we
             * need to kick off the the expansion process by expanding the root node.
             */
            if ((this.currentFilter.filterOwner == null) && (obj.filterId != "path"))
            {
               var node = this.widgets.treeview.getNodeByProperty("path", "");
               if (node !== null)
               {
                  node.expand();
               }
            }
            
            // Should be a filterId in the arguments
            this.currentFilter =
            {
               filterId: obj.filterId,
               filterOwner: obj.filterOwner,
               filterData: obj.filterData
            };

            this.isFilterOwner = (obj.filterOwner == this.name);
            this._showHighlight(this.isFilterOwner);
         }
      },


      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Creates the TreeView control and renders it to the parent element.
       * @method _buildTree
       * @private
       */
      _buildTree: function DLT__buildTree()
      {
         // Create a new tree
         var tree = new YAHOO.widget.TreeView(this.id + "-treeview");
         this.widgets.treeview = tree;

         // Turn dynamic loading on for entire tree
         tree.setDynamicLoad(this.fnLoadNodeData);

         // Get root node for tree
         var root = tree.getRoot();

         // Add default top-level node
         var tempNode = new YAHOO.widget.TextNode(
         {
            label: Alfresco.util.message("node.root", this.name),
            path: "",
            nodeRef: ""
         }, root, false);

         // Register tree-level listeners
         tree.subscribe("labelClick", this.onNodeClicked, this, true);
         tree.subscribe("expandComplete", this.onExpandComplete, this, true);

         // Render tree with this one top-level node
         tree.draw();
      },

      /**
       * @method _sortNodeChildren
       * @param node {object} Parent node
       * @param onSortComplete {object} Optional callback object literal
       * @private
       */
      _sortNodeChildren: function DLT__sortNodeChildren(node, onSortComplete)
      {
         // Is the node a leaf?
         if (node.isLeaf)
         {
            // Yes, so clearing the leaf flag and redrawing will automatically query the child nodes
            node.isLeaf = false;
            this.widgets.treeview.draw();
            this._showHighlight(true);
            return;
         }
         
         // Get the path this node refers to
         var nodePath = node.data.path;

         // Prepare URI for XHR data request
         var uri = this._buildTreeNodeUrl(nodePath);

         // Prepare the XHR callback object
         var callback =
         {
            success: function DLT_sNC_success(oResponse)
            {
               var results = YAHOO.lang.JSON.parse(oResponse.responseText);

               if (results.items)
               {
                  var kids = oResponse.argument.node.children;
                  var items = results.items;
                  for (var i = 0, j = items.length; i < j; i++)
                  {
                     if ((kids.length <= i) || (kids[i].data.nodeRef != items[i].nodeRef))
                     {
                        // Node has moved - search for correct node for this position and swap if found
                        var kidFound = false;
                        for (var m = i, n = kids.length; m < n; m++)
                        {
                           if (kids[m].data.nodeRef == items[i].nodeRef)
                           {
                              var temp = kids[i];
                              kids[i] = kids[m];
                              kids[m] = temp;
                              kidFound = true;
                              break;
                           }
                        }
                           
                        // If we get here we couldn't find the node, so create one and insert it
                        if (!kidFound)
                        {
                           var item = items[i];
                           var tempNode = new YAHOO.widget.TextNode(
                           {
                              label: item.name,
                              path: oResponse.argument.node.data.path + "/" + item.name,
                              nodeRef: item.nodeRef,
                              description: item.description
                           });

                           if (!item.hasChildren)
                           {
                              tempNode.isLeaf = true;
                           }
                           
                           if (kids.length == 0)
                           {
                              var parentNode = oResponse.argument.node;
                              parentNode.isLeaf = false;
                              tempNode.appendTo(parentNode);
                           }
                           else if (kids.length > i)
                           {
                              tempNode.insertBefore(kids[i]);
                           }
                           else
                           {
                              tempNode.insertAfter(kids[kids.length - 1]);
                           }
                        }
                     }
                  }
                  
                  // Update the tree
                  this.widgets.treeview.draw();
                  this._showHighlight(true);
                  
                  // Execute the onSortComplete callback
                  var callback = oResponse.argument.onSortComplete;
                  if (callback && typeof callback.fn == "function")
                  {
                     callback.fn.call(callback.scope ? callback.scope : this, callback.obj);
                  }
               }
            },

            // If the XHR call is not successful, no further processing - tree may not be sorted correctly
            failure: function DLT_sNC_failure(oResponse)
            {
               Alfresco.logger.debug("DLT_sNC_failure");
            },

            // XHR response argument information
            argument:
            {
               node: node,
               onSortComplete: onSortComplete
            },
            
            scope: this,

            // Timeout -- abort the transaction after 7 seconds
            timeout: 7000
         };

         // Make the XHR call using Connection Manager's asyncRequest method
         YAHOO.util.Connect.asyncRequest('GET', uri, callback);
      },

      _showHighlight: function DLT__showHighlight(isVisible)
      {
         if (this.selectedNode !== null)
         {
            if (isVisible)
            {
               Dom.addClass(this.selectedNode.getEl(), "selected");
            }
            else
            {
               Dom.removeClass(this.selectedNode.getEl(), "selected");
            }
         }
      },
      
      _updateSelectedNode: function DLT__updateSelectedNode(node)
      {
         if (this.isFilterOwner)
         {
            this._showHighlight(false);
            this.selectedNode = node;
            this._showHighlight(true);
         }
         else
         {
            this.selectedNode = node;
         }
      },

      /**
       * Build URI parameter string for treenode JSON data webscript
       *
       * @method _buildTreeNodeUrl
       * @param path {string} Path to query
       */
       _buildTreeNodeUrl: function DLT__buildTreeNodeUrl(path)
       {
          var uriTemplate = Alfresco.constants.PROXY_URI + "slingshot/doclib/treenode/site/{site}/{container}{path}";

          var url = YAHOO.lang.substitute(uriTemplate,
          {
             site: encodeURIComponent(this.options.siteId),
             container: encodeURIComponent(this.options.containerId),
             path: encodeURI(path)
          });

          return url;
       }
   };
})();
