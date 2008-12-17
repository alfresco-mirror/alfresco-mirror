<script type="text/javascript">//<![CDATA[
   var userProfile = new Alfresco.UserProfile("${args.htmlid}").setOptions(
   {
      userId: "${user.name}",
      profileId: "${profile.name}"
   }).setMessages(
      ${messages}
   );
//]]></script>

<#assign editable = (user.name == profile.name)>
<#assign displayname=profile.firstName>
<#if profile.lastName??><#assign displayname=displayname + " " + profile.lastName></#if>
<div id="${args.htmlid}-body" class="profile">
   <div id="${args.htmlid}-readview" class="hidden">
      <#if editable>
      <div class="editcolumn">
         <div class="btn-edit"><button id="${args.htmlid}-button-edit" name="edit">${msg("button.editprofile")}</button></div>
      </div>
      </#if>
      <div class="viewcolumn">
         <h1><#if editable>${msg("label.myprofile")}<#else>${displayname?html} ${msg("label.profile")}</#if></h1>
         <div class="header-bar">${msg("label.about")}</div>
         <div class="photorow">
            <div class="photo">
               <img class="photoimg" src="${url.context}<#if profile.properties.avatar??>/proxy/alfresco/api/node/${profile.properties.avatar?replace('://','/')}/content/thumbnails/avatar?c=force<#else>/components/images/no-user-photo-64.png</#if>" alt="" />
            </div>
            <div class="namelabel">${displayname?html}</div>
            <#if profile.jobTitle?? && profile.jobTitle?length!=0><div class="fieldlabel">${profile.jobTitle?html}</div></#if>
            <#if profile.organization?? && profile.organization?length!=0><div class="fieldlabel">${profile.organization?html}</div></#if>
            <#if profile.location?? && profile.location?length!=0><div class="fieldlabel">${profile.location?html}</div></#if>
         </div>
         <#if biohtml?? && biohtml?length!=0>
         <div class="biorow">
            <hr/>
            <div>${biohtml}</div>
         </div>
         </#if>
         
         <div class="header-bar">${msg("label.contactinfo")}</div>
         <#if profile.email?? && profile.email?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.email")}:</span>
            <span class="fieldvalue">${profile.email?html}</span>
         </div>
         </#if>
         <#if profile.telephone?? && profile.telephone?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.telephone")}:</span>
            <span class="fieldvalue">${profile.telephone?html}</span>
         </div>
         </#if>
         <#if profile.mobilePhone?? && profile.mobilePhone?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.mobile")}:</span>
            <span class="fieldvalue">${profile.mobilePhone?html}</span>
         </div>
         </#if>
         <#if profile.skype?? && profile.skype?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.skype")}:</span>
            <span class="fieldvalue">${profile.skype?html}</span>
         </div>
         </#if>
         <#if profile.instantMsg?? && profile.instantMsg?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.im")}:</span>
            <span class="fieldvalue">${profile.instantMsg?html}</span>
         </div>
         </#if>
         
         <div class="header-bar">${msg("label.companyinfo")}</div>
         <#if profile.organization?? && profile.organization?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.name")}:</span>
            <span class="fieldvalue">${profile.organization?html}</span>
         </div>
         </#if>
         <#if (profile.companyAddress1?? && profile.companyAddress1?length!=0) ||
              (profile.companyAddress2?? && profile.companyAddress2?length!=0) ||
              (profile.companyAddress3?? && profile.companyAddress3?length!=0) ||
              (profile.companyPostcode?? && profile.companyPostcode?length!=0)>
         <div class="row">
            <span class="fieldlabelright">${msg("label.address")}:</span>
            <span class="fieldvalue"><#if profile.companyAddress1?? && profile.companyAddress1?length!=0>${profile.companyAddress1?html}<br /></#if>
               <#if profile.companyAddress2?? && profile.companyAddress2?length!=0>${profile.companyAddress2?html}<br /></#if>
               <#if profile.companyAddress3?? && profile.companyAddress3?length!=0>${profile.companyAddress3?html}<br /></#if>
               <#if profile.companyPostcode?? && profile.companyPostcode?length!=0>${profile.companyPostcode?html}</#if>
            </span>
         </div>
         </#if>
         <!--
         <div class="row">
            <span class="fieldlabelright">${msg("label.map")}:</span>
            <span class="fieldvalue"></span>
         </div>
         -->
         <#if profile.companyTelephone?? && profile.companyTelephone?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.telephone")}:</span>
            <span class="fieldvalue">${profile.companyTelephone?html}</span>
         </div>
         </#if>
         <#if profile.companyFax?? && profile.companyFax?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.fax")}:</span>
            <span class="fieldvalue">${profile.companyFax?html}</span>
         </div>
         </#if>
         <#if profile.companyEmail?? && profile.companyEmail?length!=0>
         <div class="row">
            <span class="fieldlabelright">${msg("label.email")}:</span>
            <span class="fieldvalue">${profile.companyEmail?html}</span>
         </div>
         </#if>
      </div>
   </div>
   
   <#if editable>
   <div id="${args.htmlid}-editview" class="hidden">
      <form id="${htmlid}-form" action="${url.context}/service/components/profile/userprofile" method="post">
      
      <div class="header-bar">${msg("label.about")}</div>
      <div class="drow">
         <div class="reqcolumn">&nbsp;*</div>
         <div class="rightcolumn">
            <span class="label"><label for="${args.htmlid}-input-lastName">${msg("label.lastname")}:</label></span>
            <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-lastName" value="<#if profile.lastName??>${profile.lastName?html}</#if>" /></span>
         </div>
         <div class="leftcolumn">
            <span class="label"><label for="${args.htmlid}-input-firstName">${msg("label.firstname")}:</label></span>
            <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-firstName" value="<#if profile.firstName??>${profile.firstName?html}</#if>" />&nbsp;*</span>
         </div>
      </div>
      <div class="drow">
         <div class="reqcolumn">&nbsp;</div>         
         <div class="leftcolumn">
            <span class="label"><label for="${args.htmlid}-input-jobtitle">${msg("label.title")}:</label></span>
            <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-jobtitle" value="<#if profile.jobTitle??>${profile.jobTitle?html}</#if>" /></span>
         </div>
         <div class="rightcolumn">
            <span class="label"><label for="${args.htmlid}-input-location">${msg("label.location")}:</label></span>
            <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-location" value="<#if profile.location??>${profile.location?html}</#if>" /></span>
         </div>
      </div>
      <!--
      <div class="drow">
         <div class="leftcolumn">
            <span class="label">${msg("label.timezone")}:</span>
            <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-timezone" /></span>
         </div>
         <div class="rightcolumn">
         </div>
      </div>
      -->
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-bio">${msg("label.bio")}:</label></span>
         <span class="input"><textarea id="${args.htmlid}-input-bio" name="${args.htmlid}-text-biography" rows="5" cols="60">${profile.biography!""}</textarea></span>
      </div>
      
      <div class="header-bar">${msg("label.photo")}</div>
      <div class="photorow">
         <div class="photo">
            <img class="photoimg" src="${url.context}<#if profile.properties.avatar??>/proxy/alfresco/api/node/${profile.properties.avatar?replace('://','/')}/content/thumbnails/avatar?c=force<#else>/components/images/no-user-photo-64.png</#if>" alt="" />
         </div>
         <div class="photobtn">
            <button id="${args.htmlid}-button-upload" name="upload">${msg("button.upload")}</button>
            <div class="phototxt">${msg("label.photoimagesize")}</div>
            <div class="phototxt">${msg("label.photonote")}</div>
         </div>
      </div>
      
      <div class="header-bar">${msg("label.contactinfo")}</div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-telephone">${msg("label.telephone")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-telephone" value="<#if profile.telephone??>${profile.telephone?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-mobile">${msg("label.mobile")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-mobile" value="<#if profile.mobilePhone??>${profile.mobilePhone?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-email">${msg("label.email")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-email" value="<#if profile.email??>${profile.email?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-skype">${msg("label.skype")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-skype" value="<#if profile.skype??>${profile.skype?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-instantmsg">${msg("label.im")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-instantmsg" value="<#if profile.instantMsg??>${profile.instantMsg?html}</#if>" /></span>
      </div>
      
      <div class="header-bar">${msg("label.companyinfo")}</div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-organization">${msg("label.name")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-organization" value="<#if profile.organization??>${profile.organization?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-companyaddress1">${msg("label.address")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companyaddress1" value="<#if profile.companyAddress1??>${profile.companyAddress1?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companyaddress2" value="<#if profile.companyAddress2??>${profile.companyAddress2?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companyaddress3" value="<#if profile.companyAddress3??>${profile.companyAddress3?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-companypostcode">${msg("label.postcode")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companypostcode" value="<#if profile.companyPostcode??>${profile.companyPostcode?html}</#if>" /></span>
      </div>
      <!--
      <div class="row">
         <span class="label">${msg("label.map")}:</span>
         <span class="check"><input type="checkbox" id="${args.htmlid}-input-showmap" /> ${msg("label.showmap")}</span>
      </div>
      -->
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-companytelephone">${msg("label.telephone")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companytelephone" value="<#if profile.companyTelephone??>${profile.companyTelephone?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-companyfax">${msg("label.fax")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companyfax" value="<#if profile.companyFax??>${profile.companyFax?html}</#if>" /></span>
      </div>
      <div class="row">
         <span class="label"><label for="${args.htmlid}-input-companyemail">${msg("label.email")}:</label></span>
         <span class="input"><input type="text" maxlength="256" size="30" id="${args.htmlid}-input-companyemail" value="<#if profile.companyEmail??>${profile.companyEmail?html}</#if>" /></span>
      </div>
      
      <hr/>
      
      <div class="buttons">
         <button id="${args.htmlid}-button-save" name="save">${msg("button.savechanges")}</button>
         <button id="${args.htmlid}-button-cancel" name="cancel">${msg("button.cancel")}</button>
      </div>
      
      </form>
   </div>
   </#if>

</div>