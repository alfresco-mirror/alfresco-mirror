/*
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;


public class FormFieldTest extends TestCase
{
    public FormFieldTest(String name)
    {
        super(name);
    }
    
    public void testFieldConstruction_PositiveFlow()
    {
        FormConfigElement testElement = new FormConfigElement();
        testElement.addField("id1",
                Arrays.asList(new String[]{"aa", "bb", "cc"}),
                Arrays.asList(new String[]{"AA", "BB", "CC"}));
        testElement.addConstraintForField("id1", "REGEX", "Test message", "Test msg ID", null, null);
        testElement.addControlForField("id1", "test1.ftl",
                Arrays.asList(new String[]{"cp1", "cp2"}),
                Arrays.asList(new String[]{"CP1", "CP2"}));
        
        testElement.addField("id2",
                Arrays.asList(new String[]{"xx", "yy", "zz"}),
                Arrays.asList(new String[]{"XX", "YY", "ZZ"}));
        testElement.addControlForField("id2", "test2.ftl", null, null);

        Map<String, FormField> fields = testElement.getFields();
        assertNotNull(fields);
        assertEquals(2, fields.size());
        
        FormField fieldID1 = fields.get("id1");
        FormField fieldID2 = fields.get("id2");
        
        assertEquals("id1", fieldID1.getId());
        assertEquals("id2", fieldID2.getId());
        
        Map<String, String> expectedAttributes = new LinkedHashMap<String, String>();
        expectedAttributes.put("aa", "AA");
        expectedAttributes.put("bb", "BB");
        expectedAttributes.put("cc", "CC");
        assertEquals(expectedAttributes, fieldID1.getAttributes());

        expectedAttributes = new LinkedHashMap<String, String>();
        expectedAttributes.put("xx", "XX");
        expectedAttributes.put("yy", "YY");
        expectedAttributes.put("zz", "ZZ");
        assertEquals(expectedAttributes, fieldID2.getAttributes());
        
        assertEquals("test1.ftl", fieldID1.getTemplate());
        assertEquals("test2.ftl", fieldID2.getTemplate());
        
        Map<String, ConstraintHandlerDefinition> fieldID1Constraints = fieldID1.getConstraintDefinitionMap();
        Map<String, ConstraintHandlerDefinition> fieldID2Constraints = fieldID2.getConstraintDefinitionMap();
        ConstraintHandlerDefinition firstFieldID1Constraint = fieldID1Constraints.values().iterator().next();
        
        assertEquals("Test message", firstFieldID1Constraint.getMessage());
        assertEquals(Collections.emptyMap(), fieldID2Constraints);
        
        assertEquals("Test msg ID", firstFieldID1Constraint.getMessageId());
        assertEquals(Collections.emptyMap(), fieldID2Constraints);

        assertEquals("REGEX", firstFieldID1Constraint.getType());
        assertEquals(Collections.emptyMap(), fieldID2Constraints);

        List<ControlParam> expectedCPs = new ArrayList<ControlParam>();
        expectedCPs.add(new ControlParam("cp1", "CP1"));
        expectedCPs.add(new ControlParam("cp2", "CP2"));
        assertEquals(expectedCPs, fieldID1.getControlParams());
        assertEquals(Collections.emptyList(), fieldID2.getControlParams());
    }
    
    public void testGetAttributesViaExplicitGetters()
    {
        FormConfigElement testElement = new FormConfigElement();
        testElement.addField("name",
                Arrays.asList(new String[]{"id",   "label", "label-id",
                		"read-only", "set",     "help",
                		"help-id"}),
                Arrays.asList(new String[]{"name", "Name",  "field_label_name",
                		"true",     "details", "This is the name of the node",
                		"field_help_name"}));
        
        FormField testField = testElement.getFields().get("name");
        assertEquals("name", testField.getId());
        assertEquals("Name", testField.getLabel());
        assertEquals("field_label_name", testField.getLabelId());
        assertEquals(true, testField.isReadOnly());
        assertEquals("details", testField.getSet());
        assertEquals("This is the name of the node", testField.getHelpText());
        assertEquals("field_help_name", testField.getHelpTextId());
    }
    
    public void testConstructionWithNulls()
    {
        FormConfigElement fce = new FormConfigElement();
        fce.addField("id1", null, null);
        fce.addConstraintForField("id1", "REGEX", null, null, null, null);
        fce.addControlForField("id1", null, null, null);
        
        FormField recoveredFce = fce.getFields().get("id1");
        assertEquals("Expected no attributes.", Collections.emptyMap(), recoveredFce.getAttributes());
        assertEquals("Expected no template.", null, recoveredFce.getTemplate());
        assertEquals("Expected no control params.", Collections.emptyList(), recoveredFce.getControlParams());

        fce = new FormConfigElement();
        fce.addField("id1", null, null);
        
        recoveredFce = fce.getFields().get("id1");
        assertEquals("Expected no attributes.", Collections.emptyMap(), recoveredFce.getAttributes());
        assertEquals("Expected no template.", null, recoveredFce.getTemplate());
        assertEquals("Expected no control params.", Collections.emptyList(), recoveredFce.getControlParams());
        assertEquals("Expected no constraint msg.", Collections.emptyMap(), recoveredFce.getConstraintDefinitionMap());
    }
    
    public void testExtraControlParamValueIsIgnored()
    {
        FormConfigElement fce = new FormConfigElement();
        fce.addField("id1", null, null);
        fce.addControlForField("id1", "test1.ftl",
                Arrays.asList(new String[]{"cp1", "cp2"}),
                Arrays.asList(new String[]{"CP1", "CP2", "CP3"}));
        List<ControlParam> params = fce.getFields().get("id1").getControlParams();
        assertEquals(2, params.size());
        List<ControlParam> expectedParams = new ArrayList<ControlParam>(2);
        expectedParams.add(new ControlParam("cp1", "CP1"));
        expectedParams.add(new ControlParam("cp2", "CP2"));
        assertEquals(expectedParams, params);
    }
    
    public void testCombineFormFieldsWithAdditiveChanges()
    {
        // <field id="name" label="Name" disabled="true">
        // </field>
        
        // combined with

        // <field id="name" label="Name" disabled="true">
        //     <control template="test.ftl">
        //         <control-param name="foo">bar</control-param>
        //     </control>
        //     <constraint-message type="REGEX" message="msg" message-id="msg-id" />
        // </field>

        Map<String, String> attrs1 = new LinkedHashMap<String, String>();
        attrs1.put("label", "Name");
        attrs1.put("disabled", "true");
        FormField firstInstance = new FormField("name", attrs1);
        
        Map<String, String> attrs2 = new LinkedHashMap<String, String>();
        attrs2.put("label", "Name");
        attrs2.put("disabled", "true");
        FormField secondInstance = new FormField("name", attrs2);
        secondInstance.setTemplate("test.ftl");
        secondInstance.addControlParam("foo", "bar");
        secondInstance.addConstraintDefinition("REGEX", "msg", "msg-id", null, null);
        
        FormField combinedField = firstInstance.combine(secondInstance);
        
        assertEquals("name", combinedField.getId());
        assertEquals("Name", combinedField.getLabel());
        assertEquals(null, combinedField.getLabelId());
        assertEquals(null, combinedField.getSet());
        assertEquals(null, combinedField.getHelpText());
        assertEquals(null, combinedField.getHelpTextId());
    }

    public void testCombineFormFieldsWithModificationChanges()
    {
        // <field id="name" label="Name" disabled="true">
        //     <control template="test.ftl">
        //         <control-param name="foo">bar</control-param>
        //     </control>
        //     <constraint-message type="REGEX" message="msg" message-id="msg-id" />
        // </field>
        
        // combined with

        // <field id="name" label="Name" disabled="false">
        //     <control template="test.ftl">
        //         <control-param name="foo">bar</control-param>
        //     </control>
        //     <constraint-message type="REGEX" message="msg" message-id="msg-id" />
        // </field>

        Map<String, String> attrs1 = new LinkedHashMap<String, String>();
        attrs1.put("label", "Name");
        attrs1.put("disabled", "true");
        FormField firstInstance = new FormField("name", attrs1);
        firstInstance.setTemplate("test.ftl");
        firstInstance.addControlParam("foo", "bar");
        firstInstance.addConstraintDefinition("REGEX", "msg", "msg-id", null, null);
        
        Map<String, String> attrs2 = new LinkedHashMap<String, String>();
        attrs2.put("label", "Name");
        attrs2.put("disabled", "false");
        FormField secondInstance = new FormField("name", attrs2);
        secondInstance.setTemplate("newtest.ftl");
        secondInstance.addControlParam("foo", "barrr");
        secondInstance.addConstraintDefinition("REGEX", "newmsg", "newmsg-id", null, null);
        
        FormField combinedField = firstInstance.combine(secondInstance);
        
        assertEquals("name", combinedField.getId());
        assertEquals("Name", combinedField.getLabel());
        assertEquals(null, combinedField.getLabelId());
        assertEquals(null, combinedField.getSet());
        assertEquals(null, combinedField.getHelpText());
        assertEquals(null, combinedField.getHelpTextId());
    }
}
