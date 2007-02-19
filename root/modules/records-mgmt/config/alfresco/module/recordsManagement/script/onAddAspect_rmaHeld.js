/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing"
 *
 * Script: onAddAspect_rmaHeld.js
 * Author: Roy Wetherall
 * 
 * Behaviour script executed when the held aspect is added.
 */

var record = behaviour.args[0];

if (record.hasAspect(rm.ASPECT_RECORD) == true)
{
    var filePlan = rm.getFilePlan(record);
    
    // Set the hold schedule details
    record.properties[rm.PROP_HOLD_UNTIL_EVENT] = filePlan.properties[rm.PROP_DISPOSITION_INSTRUCTIONS];
    record.properties[rm.PROP_FROZEN] = false;  
     
    var cutoffDateTime = null;    
    if (filePlan.properties[rm.PROP_DISCRETIONARY_HOLD] == true) 
    {
        logger.log("Creating discretionary hold.");
        cutoffDateTime = new Date();
        var newYear = cutoffDateTime.getFullYear() + 100;
        cutoffDateTime.setFullYear(newYear);
    }
    else
    {
        logger.log("Normal hold");
        cutoffDateTime = rm.calculateDateInterval(
                                      filePlan.properties[rm.PROP_HOLD_PERIOD_UNIT], 
                                      filePlan.properties[rm.PROP_HOLD_PERIOD_VALUE], 
                                      new Date());
    }
    
    if (cutoffDateTime != null)
    {
        record.properties[rm.PROP_HOLD_UNTIL] = cutoffDateTime;
    }
    
    record.save();
}