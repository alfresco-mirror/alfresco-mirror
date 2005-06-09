package org.alfresco.repo.dictionary;

import java.util.Collection;

import org.alfresco.repo.ref.QName;


/**
 * Data Dictionary Service.
 * 
 * The Dictionary Service provides access to Repository
 * meta-data.
 * 
 * @author David Caruana
 */
public interface DictionaryService
{

    /**
     * @return the names of all models that have been registered with the Repository
     */
    public Collection<QName> getAllModels();
    
    /**
     * @param model the model name to retrieve
     * @return the specified model (or null, if it doesn't exist)
     */
    public ModelDefinition getModel(QName model);

    /**
     * @return the names of all property types that have been registered with the Repository
     */
    Collection<QName> getAllPropertyTypes();

    /**
     * @param model the model to retrieve property types for
     * @return the names of all property types defined within the specified model
     */
    Collection<QName> getPropertyTypes(QName model);
    
    /**
     * @param name the name of the property type to retrieve
     * @return the property type definition (or null, if it doesn't exist)
     */
    PropertyTypeDefinition getPropertyType(QName name);

    /**
     * @return the names of all types that have been registered with the Repository
     */
    Collection<QName> getAllTypes();
    
    /**
     * @param model the model to retrieve types for
     * @return the names of all types defined within the specified model
     */
    Collection<QName> getTypes(QName model);

    /**
     * @param name the name of the type to retrieve
     * @return the type definition (or null, if it doesn't exist)
     */
    TypeDefinition getType(QName name);

    /**
     * Construct an anonymous type that combines the definitions of the specified
     * type and aspects.
     *
     * @param type the type to start with 
     * @param aspects the aspects to combine with the type
     * @return the anonymous type definition
     */
    TypeDefinition getAnonymousType(QName type, Collection<QName> aspects);

    /**
     * @return the names of all aspects that have been registered with the Repository
     */
    Collection<QName> getAllAspects();
    
    /**
     * @param model the model to retrieve aspects for
     * @return the names of all aspects defined within the specified model
     */
    Collection<QName> getAspects(QName model);

    /**
     * @param name the name of the aspect to retrieve
     * @return the aspect definition (or null, if it doesn't exist)
     */
    AspectDefinition getAspect(QName name);

    /**
     * @param name the name of the class (type or aspect) to retrieve
     * @return the class definition (or null, if it doesn't exist)
     */
    ClassDefinition getClass(QName name);
    
    /**
     * Determines whether a class is a sub-class of another class
     * 
     * @param className the sub-class to test
     * @param ofClassName the class to test against
     * @return  true => the class is a sub-class (or itself)
     */
    boolean isSubClass(QName className, QName ofClassName);

    /**
     * Gets the definition of the property as defined by the specified Class.
     * 
     * Note: A sub-class may override the definition of a property that's 
     *       defined in a super-class.
     * 
     * @param className the class name
     * @param propertyName the property name
     * @return the property definition (or null, if it doesn't exist)
     */
    PropertyDefinition getProperty(QName className, QName propertyName);

    /**
     * Gets the definition of the property as defined by its owning Class.
     * 
     * @param propertyName the property name
     * @return the property definition (or null, if it doesn't exist)
     */
    PropertyDefinition getProperty(QName propertyName);

    /**
     * Gets the definition of the assocication as defined by the specified Class.
     * 
     * Note: A sub-class may override the definition of an association that's 
     *       defined in a super-class.
     * 
     * @param className the class name
     * @param propertyName the property name
     * @return the property definition (or null, if it doesn't exist)
     */
    AssociationDefinition getAssociation(QName className, QName associationName);
    
    /**
     * Gets the definition of the association as defined by its owning Class.
     * 
     * @param associationName the property name
     * @return the association definition (or null, if it doesn't exist)
     */
    AssociationDefinition getAssociation(QName associationName);
    
    // TODO: Behaviour definitions
    
}
