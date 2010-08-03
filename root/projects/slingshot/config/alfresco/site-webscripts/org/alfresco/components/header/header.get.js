/**
 * Share Header component GET method
 */

const PREF_COLLAPSED_TWISTERS = "org.alfresco.share.twisters.collapsed";

/**
 * Twister Preferences
 */
function getTwisterPrefs()
{
   var collapsedTwisters = "",
      result,
      response;

   result = remote.call("/api/people/" + stringUtils.urlEncode(user.name) + "/preferences?pf=" + PREF_COLLAPSED_TWISTERS);
   if (result.status == 200 && result != "{}")
   {
      response = eval('(' + result + ')');
      collapsedTwisters = eval('try{(response.' + PREF_COLLAPSED_TWISTERS + ')}catch(e){}');
      if (typeof collapsedTwisters != "string")
      {
         collapsedTwisters = "";
      }
   }
   model.collapsedTwisters = collapsedTwisters;
}

/**
 * Site Title
 * @return siteId
 */
function getSiteTitle()
{
   var siteTitle = "",
      result,
      response;

   var siteId = page.url.templateArgs.site || "";
   if (siteId !== "")
   {
      result = remote.call("/api/sites/" + stringUtils.urlEncode(siteId));
      if (result.status == 200 && result != "{}")
      {
         response = eval('(' + result + ')');
         siteTitle = response.title;
         if (typeof siteTitle != "string")
         {
            siteTitle = "";
         }
      }
   }
   model.siteTitle = siteTitle;
   // Save the site title for downstream components - saves remote calls for Site Profile
   context.setValue("site-title", siteTitle);
   
   return siteId;
}

/**
 * Theme Override
 */
function getThemeOverride()
{
   if (page.url.args["theme"] != null)
   {
      model.theme = page.url.args["theme"];
   }
}

/**
 * Customizable Header
 */
function getHeader(siteId)
{
   // Array of tokenised values for use in i18n messages
   model.labelTokens = [ user.name || "", user.firstName || "", user.lastName || "", user.fullName || ""];
   model.permissions =
   {
      guest: user.isGuest,
      admin: user.isAdmin
   };
}

/**
 * User Status
 */
function getUserStatus()
{
   var statusDefault = msg.get("status.default"),
      userStatus = statusDefault,
      userStatusTime = null;
   
   // TODO: Replace with user.* calls when ALF-4209 resolved
   var result = remote.call("/api/people/" + stringUtils.urlEncode(user.name));
   if (result.status == 200 && result != "{}")
   {
      var response = eval('(' + result + ')'),
         userStatus = response.userStatus || statusDefault,
         userStatusTime = response.userStatusTime || "";
      
      if (userStatusTime !== null)
      {
         userStatusTime = userStatusTime.iso8601;
      }
   }

   model.userStatus = userStatus;
   model.userStatusTime = userStatusTime;
}

function main()
{
   getTwisterPrefs();
   var siteId = getSiteTitle();
   getThemeOverride();
   getHeader(siteId);
   getUserStatus();
}

if (!user.isGuest)
{
   main();
}