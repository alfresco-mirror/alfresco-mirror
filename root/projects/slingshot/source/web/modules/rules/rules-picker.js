/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * Rules Picker.
 * 
 * @namespace Alfresco.module
 * @class Alfresco.module.RulesPicker
 */
(function()
{
      /**
   * YUI Library aliases
   */
   var Dom = YAHOO.util.Dom,
      KeyListener = YAHOO.util.KeyListener,
      Selector = YAHOO.util.Selector;
   
   /**
    * Alfresco Slingshot aliases
    */
    var $html = Alfresco.util.encodeHTML,
       $combine = Alfresco.util.combinePaths,
       $hasEventInterest = Alfresco.util.hasEventInterest;

   Alfresco.module.RulesPicker = function(htmlId)
   {
      Alfresco.module.RulesPicker.superclass.constructor.call(this, htmlId);

      // Re-register with our own name
      this.name = "Alfresco.module.RulesPicker";

      if (htmlId != "null")
      {
         YAHOO.Bubbling.on("siteChanged", this.resetRules, this);
         YAHOO.Bubbling.on("containerChanged", this.resetRules, this);
      }

      Alfresco.util.ComponentManager.reregister(this);

      return this;
   };


   /**
   * Alias to self
   */
   var RP = Alfresco.module.RulesPicker;

   /**
   * View Mode Constants
   */
   YAHOO.lang.augmentObject(RP,
   {
      /**
       * "Picker" mode constant.
       *
       * Will make dialog fire event with selected rule when Ok is clicked.
       *
       * @property MODE_PICKER
       * @type string
       * @final
       * @default "picker"
       */
      MODE_PICKER: "picker",

      /**
       * "Copy from" mode constant.
       *
       * Will make dialog copy the selected rule to the file in "files".
       *
       * @property MODE_COPY_FROM
       * @type string
       * @final
       * @default "copy-from"
       */
      MODE_COPY_FROM: "copy-from",

      /**
       * "Link to" mode constant.
       *
       * Will make the file in "files" link to the selected rule folder.
       *
       * @property MODE_LINK_TO
       * @type string
       * @final
       * @default "link-to"
       */
      MODE_LINK_TO: "link-to"
   });

   YAHOO.extend(Alfresco.module.RulesPicker, Alfresco.module.DoclibGlobalFolder,
   {
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @override
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.module.RulesPicker} returns 'this' for method chaining
       */
      setOptions: function RP_setOptions(obj)
      {
         var dataWebScripts = {};
         dataWebScripts[RP.MODE_PICKER] = "";
         dataWebScripts[RP.MODE_COPY_FROM] = "copy-from";
         dataWebScripts[RP.MODE_LINK_TO] = "link-to";

         if (typeof dataWebScripts[obj.mode] == "undefined")
         {
            throw new Error("Alfresco.module.RulesPicker: Invalid mode '" + obj.mode + "'");
         }
         
         var allowedViewModes = [Alfresco.module.DoclibGlobalFolder.VIEW_MODE_SITE, Alfresco.module.DoclibGlobalFolder.VIEW_MODE_REPOSITORY];

         return Alfresco.module.RulesPicker.superclass.setOptions.call(this, YAHOO.lang.merge(
         {
            viewMode: Alfresco.module.DoclibGlobalFolder.VIEW_MODE_SITE,
            allowedViewModes: allowedViewModes,
            extendedTemplateUrl: Alfresco.constants.URL_SERVICECONTEXT + "modules/rules/rules-picker",
            dataWebScript: dataWebScripts[obj.mode]                 
         }, obj));
      },

      /**
       * Event callback when superclass' dialog template has been loaded.
       *
       * @method onTemplateLoaded
       * @override
       * @param response {object} Server response from load template XHR request
       */
      onTemplateLoaded: function RP_onTemplateLoaded(response)
      {
         // Load the UI template, which will bring in a rules container and new i18n-messages, from the server
         Alfresco.util.Ajax.request(
         {
            url: this.options.extendedTemplateUrl,
            dataObj:
            {
               htmlid: this.id
            },
            successCallback:
            {
               fn: this.onExtendedTemplateLoaded,
               obj: response,
               scope: this
            },
            failureMessage: "Could not load 'rules-picker' template:" + this.options.extendedTemplateUrl,
            execScripts: true
         });
      },

      /**
       * Event callback when this class' template has been loaded
       *
       * @method onExtendedTemplateLoaded
       * @override
       * @param response {object} Server response from load template XHR request
       */
      onExtendedTemplateLoaded: function RP_onExtendedTemplateLoaded(response, superClassResponse)
      {
         // Inject the template from the XHR request into a new DIV element
         var tmpEl = document.createElement("div");
         tmpEl.setAttribute("style", "display:none");
         tmpEl.innerHTML = response.serverResponse.responseText;
         this.widgets.rulesContainerEl = Dom.getFirstChild(tmpEl);

         // Now that we have loaded this components i18n messages let the original template get rendered.
         Alfresco.module.RulesPicker.superclass.onTemplateLoaded.call(this, superClassResponse);
      },

      /**
       * Fired by YUI TreeView when a node label is clicked
       * @method onNodeClicked
       * @param args.event {HTML Event} the event object
       * @param args.node {YAHOO.widget.Node} the node clicked
       * @return allowExpand {boolean} allow or disallow node expansion
       * @override
       */
      onNodeClicked: function RP_onNodeClicked(args)
      {
         Alfresco.logger.debug("RulesPicker_onNodeClicked");
         Alfresco.module.RulesPicker.superclass.onNodeClicked.call(this, args);
         this._loadRules();
      },

      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */

      /**
       * Dialog OK button event handler
       *
       * @method onOK
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onOK: function RP_onOK(e, p_obj)
      {
         // Collect selected rules if any
         var rules = [],
            ruleCheckboxes = Selector.query('input[type=checkbox]', this.id + "-rulePicker"),
            ruleCheckbox;
         for (var i = 0, l = ruleCheckboxes.length; i < l; i++)
         {
            ruleCheckbox = ruleCheckboxes[i];
            if (ruleCheckbox.checked)
            {
               rules.push(ruleCheckbox.value);
            }
         }

         if (this.options.mode == RP.MODE_PICKER)
         {
            // Fire event so other components will know a rule was selected
            YAHOO.Bubbling.fire("rulesSelected",
            {
               ruleNodeRefs: rules,
               eventGroup: this
            });

            // Hide dialog
            this.widgets.dialog.hide();
         }
         else
         {
            var file;

            // Single/multi files into array of nodeRefs
            if (YAHOO.lang.isArray(this.options.files))
            {
               file = this.options.files[0];
            }
            else
            {
               file = this.options.files;
            }

            // Prepare for the server request and event triggered afterwards
            var url = Alfresco.constants.PROXY_URI,
               dataObj = {},
               eventName = null,
               event = {};

            if (this.options.mode == RP.MODE_COPY_FROM)
            {
               url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "/api/node/{folderNodeRef}/copy",
               {
                  folderNodeRef: file.nodeRef.replace(":/", "")
               });
               dataObj.rules = rules;
               eventName = "rulesCopiedFrom";
               event = {
                  folderNodeRef: file.nodeRef.replace(":/", ""),
                  ruleNodeRefs: rules
               };
            }
            else if (this.options.mode == RP.MODE_LINK_TO)
            {
               url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/sites?uri=/api/node/{folderNodeRef}/link/{ruleNodeRef}",
               {
                  folderNodeRef: file.nodeRef.replace(":/", ""),
                  ruleNodeRef: this.selectedNode.data.nodeRef.replace(":/", "")
               });
               eventName = "ruleLinkedTo";
               event = {
                  folderNodeRef: file.nodeRef.replace(":/", ""),
                  ruleNodeRefs: this.selectedNode.data.nodeRef
               };
            }

            // Success callback function
            var fnSuccess = function RP__onOK_success(p_data)
            {
               this.widgets.dialog.hide();

               YAHOO.Bubbling.fire(eventName, event);

               Alfresco.util.PopupManager.displayMessage(
               {
                  text: this.msg("message.success")
               });
            };

            // Failure callback function
            var fnFailure = function RP__onOK_failure(p_data)
            {
               this.widgets.okButton.set("disabled", false);
               this.widgets.cancelButton.set("disabled", false);
               this.widgets.feedbackMessage.hide();
               Alfresco.util.PopupManager.displayPrompt(
               {
                  text: this.msg("message.failure")
               });
            };

            this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage(
            {
               text: this.msg("message.please-wait"),
               spanClass: "wait",
               displayTime: 0
            });

            Alfresco.util.Ajax.jsonPost(
            {
               url: url,
               dataObj: dataObj,
               successCallback:
               {
                  fn: fnSuccess,
                  scope: this
               },
               failureCallback:
               {
                  fn: fnFailure,
                  scope: this
               }
            });
         }

         this.widgets.okButton.set("disabled", true);
         this.widgets.cancelButton.set("disabled", true);
      },


      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Creates the Site Picker control.
       * @method _loadRules
       * @private
       */
      _loadRules: function RP__loadRules()
      {
         var rulePicker = Dom.get(this.id + "-rulePicker"),
            me = this;

         rulePicker.innerHTML = "";
         Dom.removeClass(rulePicker, "");

         var fnSuccess = function RP__pRP_fnSuccess(response, rulePicker)
         {
            var rules = response.json, element, rule, onclick, i, j;

            for (i = 0, j = rules.length; i < j; i++)
            {
               rule = rules[i];
               element = document.createElement("div");
               if (i == j - 1)
               {
                  Dom.addClass(element, "last");
               }

               onclick = function RP_pRP_onclick(shortName)
               {
                  return function()
                  {
                     YAHOO.Bubbling.fire("ruleChanged",
                     {
                        site: shortName,
                        eventGroup: me
                     });
                  };
               }(rule.shortName);

               var checkbox = (this.options.mode == RP.MODE_COPY_FROM || this.options.mode == RP.MODE_PICKER) ? '<input type="checkbox" value="' + rule.shortName + '">' : "",
                  h4 = '<h4>' + checkbox +  '<span>' + $html(rule.title) + '</span></h4>',
                  description = '<span class="description">' + $html(rule.description) + '</span>',
                  span = '<span class="rule">' + h4 + description + '</span>';

               element.innerHTML = span;
               element.onclick = onclick;
               rulePicker.appendChild(element);
            }
         };

         Alfresco.util.Ajax.jsonGet(
         {
            url: Alfresco.constants.PROXY_URI + "api/sites",
            successCallback:
            {
               fn: fnSuccess,
               scope: this,
               obj: rulePicker
            }
         });
      },


      /**
       * BUBBLING LIBRARY EVENT HANDLERS
       * Disconnected event handlers for event notification
       */

      /**
       * Called when Container or Site has Changed 
       *
       * @method onContainerChanged
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      resetRules: function RP_resetRules(layer, args)
      {
         // Clear rules info
         Dom.get(this.id + "-rulePicker").innerHTML = "";
      },

      /**
       * Gets a custom message depending on current view mode
       * and use superclasses
       *
       * @method msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @override
       */
      msg: function DLCMT_msg(messageId)
      {
         var result = Alfresco.util.message.call(this, this.options.mode + "." + messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
         if (result ==  (this.options.mode + "." + messageId))
         {
            result = Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1))
         }
         if (result == messageId)
         {
            result = Alfresco.util.message(messageId, "Alfresco.module.DoclibGlobalFolder", Array.prototype.slice.call(arguments).slice(1));
         }
         return result;
      },
      
      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Internal show dialog function
       * @method _showDialog
       * @override
       */
      _showDialog: function RP__showDialog()
      {
         // Make sure rules container is added
         if (this.widgets.rulesContainerEl)
         {
            // Extend global folder template with rule picker container & add rules picker class to dialog
            Dom.insertAfter(this.widgets.rulesContainerEl, Dom.get(this.id + "-treeview").parentNode);
            Dom.addClass(this.id + "-dialog", "rules-picker");

            // Make sure we dont add it twice
            this.widgets.rulesContainerEl = null;
         }
         
         Dom.get(this.id + "-rulePicker").innerHTML = "";
         this.widgets.okButton.set("label", this.msg("button"));
         this.widgets.okButton.set("disabled", false);
         this.widgets.cancelButton.set("disabled", false);                     
         return Alfresco.module.RulesPicker.superclass._showDialog.apply(this, arguments);
      }
   });

   /* Dummy instance to load optional YUI components early */
   var dummyInstance = new Alfresco.module.RulesPicker("null");
})();