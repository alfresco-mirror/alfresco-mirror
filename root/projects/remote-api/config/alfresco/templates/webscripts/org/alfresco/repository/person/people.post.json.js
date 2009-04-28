function main()
{
   //
   // Get the person details
   //
   
   if ((json.isNull("userName")) || (json.get("userName").length() == 0)) 
   {
      status.setCode(status.STATUS_BAD_REQUEST, "User name missing when creating person");
      return;
   }
   
   if ((json.isNull("firstName")) || (json.get("firstName").length() == 0))
   {
      status.setCode(status.STATUS_BAD_REQUEST, "First name missing when creating person");
      return;
   }
   
   if ((json.isNull("lastName")) || (json.get("lastName").length() == 0))
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Last name missing when creating person");
      return;
   }
   
   if ((json.isNull("email")) || (json.get("email").length() == 0))
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Email missing when creating person");
      return;
   }
   
   // Create the person with the supplied user name
   var userName = json.get("userName");
   var person = people.createPerson(userName, json.get("firstName"), json.get("lastName"), json.get("email"));
   
   // return error message if a person with that user name could not be created
   if (person === null)
   {
      status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, "Person could not be created with user name: " + userName);
      return;
   }
   
   // assign values to the person's properties
   if (json.has("title"))
   {
      person.properties["title"] = json.get("title");
   }
   if (json.has("organisation"))
   {
      person.properties["organization"] = json.get("organisation");
   }
   if (json.has("jobtitle"))
   {
      person.properties["jobtitle"] = json.get("jobtitle");
   }
   person.save();
   
   // Put the created person into the model
   model.person = person;
}

main();