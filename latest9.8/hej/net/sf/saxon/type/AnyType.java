////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import com.saxonica.ee.schema.UserComplexType;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.value.UntypedAtomicValue;

import java.util.HashSet;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anyType,
 * also known as the urtype.
 * <p/>
 * See XML Schema 1.1 Part 1 section 3.4.7
 */

public final class AnyType implements ComplexType {

    /*@NotNull*/ private static AnyType theInstance = new AnyType();

    /**
     * Private constructor
     */
    private AnyType() {
        super();
    }

    /**
     * Get the singular instance of this class
     *
     * @return the singular object representing xs:anyType
     */

    /*@NotNull*/
    public static AnyType getInstance() {
        return theInstance;
    }

    /**
     * Get the local name of this type
     *
     * @return the local name of this type definition, if it has one. Return null in the case of an
     *         anonymous type.
     */

    /*@NotNull*/
    public String getName() {
        return "anyType";
    }

    /**
     * Get the name of this type as an EQName, that is, a string in the format Q{uri}local.
     *
     * @return an EQName identifying the type, specifically "Q{http://www.w3.org/2001/XMLSchema}anyType"
     */
    public String getEQName() {
        return "Q{" + NamespaceConstant.SCHEMA + "}anyType";
    }

    /**
     * Get the target namespace of this type
     *
     * @return the target namespace of this type definition, if it has one. Return null in the case
     *         of an anonymous type, and in the case of a global type defined in a no-namespace schema.
     */

    public String getTargetNamespace() {
        return NamespaceConstant.SCHEMA;
    }

    /**
     * Get the variety of this complex type. This will be one of the values
     * {@link #VARIETY_EMPTY}, {@link #VARIETY_MIXED}, {@link #VARIETY_SIMPLE}, or
     * {@link #VARIETY_ELEMENT_ONLY}
     */

    public int getVariety() {
        return VARIETY_MIXED;
    }

    /**
     * Get the validation status - always valid
     */
    public int getValidationStatus() {
        return VALIDATED;
    }

    /**
     * Get the redefinition level. This is zero for a component that has not been redefined;
     * for a redefinition of a level-0 component, it is 1; for a redefinition of a level-N
     * component, it is N+1. This concept is used to support the notion of "pervasive" redefinition:
     * if a component is redefined at several levels, the top level wins, but it is an error to have
     * two versions of the component at the same redefinition level.
     *
     * @return the redefinition level
     */

    public int getRedefinitionLevel() {
        return 0;
    }

    /**
     * Get the base type
     *
     * @return null (this is the root of the type hierarchy)
     */

    /*@Nullable*/
    public SchemaType getBaseType() {
        return null;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    /*@Nullable*/
    public SchemaType getKnownBaseType() throws IllegalStateException {
        return null;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example
     *         {@link SchemaType#DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return 0;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Get the types of derivation that are not permitted, by virtue of the "final" property.
     *
     * @return the types of derivation that are not permitted, as a bit-significant integer
     *         containing bits such as {@link net.sf.saxon.type.SchemaType#DERIVATION_EXTENSION}
     */
    public int getFinalProhibitions() {
        return 0;
    }

    //#ifdefined  SCHEMA

    /**
     * Get the schema component in the form of a function item. This allows schema information
     * to be made visible to XSLT or XQuery code. The function makes available the contents of the
     * schema component as defined in the XSD specification. The function takes a string as argument
     * representing a property name, and returns the corresponding property of the schema component.
     * There is also a property "class" which returns the kind of schema component, for example
     * "Attribute Declaration".
     *
     * @return the schema component represented as a function from property names to property values.
     */
    public Function getComponentAsFunction() {
        return UserComplexType.getComponentAsFunction(this);
    }
//#endif

    /**
     * Test whether this ComplexType has been marked as abstract.
     *
     * @return false: this class is not abstract.
     */

    public boolean isAbstract() {
        return false;
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public boolean isComplexType() {
        return true;
    }

    /**
     * Test whether this is an anonymous type
     *
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
        return false;
    }

    /**
     * Test whether this SchemaType is a simple type
     *
     * @return true if this SchemaType is a simple type
     */

    public boolean isSimpleType() {
        return false;
    }

    /**
     * Test whether this SchemaType is an atomic type
     *
     * @return true if this SchemaType is an atomic type
     */

    public boolean isAtomicType() {
        return false;
    }

    /**
     * Ask whether this type is an ID type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:ID: that is, it includes types derived
     * from ID by restriction, list, or union. Note that for a node to be treated
     * as an ID, its typed value must be a *single* atomic value of type ID; the type of the
     * node, however, can still allow a list.
     */

    public boolean isIdType() {
        return false;
    }

    /**
     * Ask whether this type is an IDREF or IDREFS type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:IDREF: that is, it includes types derived
     * from IDREF or IDREFS by restriction, list, or union
     */

    public boolean isIdRefType() {
        return false;
    }

    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public int getBlock() {
        return 0;
    }

    /**
     * Test whether this complex type has complex content
     *
     * @return true: this complex type has complex content
     */
    public boolean isComplexContent() {
        return true;
    }

    /**
     * Test whether this complex type has simple content
     *
     * @return false: this complex type has complex content
     */

    public boolean isSimpleContent() {
        return false;
    }

    /**
     * Test whether this complex type has "all" content, that is, a content model
     * using an xs:all compositor
     *
     * @return false: this complex type does not use an "all" compositor
     */

    public boolean isAllContent() {
        return false;
    }

    /**
     * For a complex type with simple content, return the simple type of the content.
     * Otherwise, return null.
     *
     * @return null: this complex type does not have simple content
     */

    /*@Nullable*/
    public SimpleType getSimpleContentType() {
        return null;
    }

    /**
     * Test whether this complex type is derived by restriction
     *
     * @return false: this type is not a restriction
     */
    public boolean isRestricted() {
        return false;
    }

    /**
     * Test whether the content type of this complex type is empty
     *
     * @return false: the content model is not empty
     */

    public boolean isEmptyContent() {
        return false;
    }

    /**
     * Test whether the content model of this complexType allows empty content
     *
     * @return true: the content is allowed to be empty
     */

    public boolean isEmptiable() {
        return true;
    }

    /**
     * Test whether this complex type allows mixed content
     *
     * @return true: mixed content is allowed
     */

    public boolean isMixedContent() {
        return true;
    }

    /**
     * Get the fingerprint of the name of this type
     *
     * @return the fingerprint.
     */

    public int getFingerprint() {
        return StandardNames.XS_ANY_TYPE;
    }

    /**
     * Get the name of the type as a StructuredQName
     *
     * @return a StructuredQName identifying the type.  In the case of an anonymous type, an internally-generated
     * name is returned
     */
    public StructuredQName getStructuredQName() {
        return QNAME;
    }

    public static final StructuredQName QNAME = new StructuredQName("xs", NamespaceConstant.SCHEMA, "anyType");

    /**
     * Get a description of this type for use in diagnostics
     *
     * @return the string "xs:anyType"
     */

    /*@NotNull*/
    public String getDescription() {
        return "xs:anyType";
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    /*@NotNull*/
    public String getDisplayName() {
        return "xs:anyType";
    }


    /**
     * Get the URI of the schema document containing the definition of this type
     *
     * @return null for a built-in type
     */

    /*@Nullable*/
    public String getSystemId() {
        return null;
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return other instanceof AnyType;
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
*                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     */

    public void analyzeContentExpression(Expression expression, int kind) {
        //return;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type.
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    /*@NotNull*/
    public AtomicSequence atomize(/*@NotNull*/ NodeInfo node) {
        return new UntypedAtomicValue(node.getStringValue());
    }

    /**
     * Test whether this complex type subsumes another complex type. The algorithm
     * used is as published by Thompson and Tobin, XML Europe 2003.
     * @param sub the other type (the type that is derived by restriction, validly or otherwise)
     * @param compiler
     * @return null indicating that this type does indeed subsume the other; or a string indicating
     * why it doesn't.
     */

//    public String subsumes(ComplexType sub, ISchemaCompiler compiler) {
//        return null;
//    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException {
        if (!(type instanceof AnyType)) {
            throw new SchemaException("Cannot derive xs:anyType from another type");
        }
    }

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the schema type associated with that element particle.
     * If there is no such particle, return null. If the fingerprint matches an element wildcard,
     * return the type of the global element declaration with the given name if one exists, or AnyType
     * if none exists and lax validation is permitted by the wildcard.
     *  @param elementName        Identifies the name of the child element within this content model
     * @param considerExtensions true if the analysis should take into account types derived by extension
     */

    /*@NotNull*/
    public SchemaType getElementParticleType(StructuredQName elementName, boolean considerExtensions) {
        return this;
    }

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the cardinality associated with that element particle,
     * that is, the number of times the element can occur within this complex type. The value is one of
     * {@link net.sf.saxon.expr.StaticProperty#EXACTLY_ONE}, {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_ONE},
     * {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_MORE}, {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ONE_OR_MORE},
     * If there is no such particle, return zero.
     *  @param elementName        Identifies the name of the child element within this content model
     * @param considerExtensions  true if the analysis should take into account types derived by extension
     */

    public int getElementParticleCardinality(StructuredQName elementName, boolean considerExtensions) {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Find an attribute use within this complex type definition having a given attribute name
     * (identified by fingerprint), and return the schema type associated with that attribute.
     * If there is no such attribute use, return null. If the fingerprint matches an attribute wildcard,
     * return the type of the global attribute declaration with the given name if one exists, or AnySimpleType
     * if none exists and lax validation is permitted by the wildcard.
     *
     * @param attributeName Identifies the name of the child element within this content model
     */

    /*@NotNull*/
    public SimpleType getAttributeUseType(StructuredQName attributeName) {
        return AnySimpleType.getInstance();
    }

    /**
     * Find an attribute use within this complex type definition having a given attribute name
     * (identified by fingerprint), and return the cardinality associated with that attribute,
     * which will always be 0, 1, or 0-or-1.
     * If there is no such attribute use, return null. If the fingerprint matches an attribute wildcard,
     * return the type of the global attribute declaration with the given name if one exists, or AnySimpleType
     * if none exists and lax validation is permitted by the wildcard.
     * <p/>
     * If there are types derived from this type by extension, search those too.
     *
     * @param attributeName Identifies the name of the child element within this content model
     * @return the schema type associated with the attribute use identified by the fingerprint.
     *         If there is no such attribute use, return null.
     */

    public int getAttributeUseCardinality(StructuredQName attributeName) throws SchemaException {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }

    /**
     * Return true if this type (or any known type derived from it by extension) allows the element
     * to have one or more attributes.
     *
     * @return true if attributes are allowed
     */

    public boolean allowsAttributes() {
        return true;
    }

    /**
     * Get a list of all the names of elements that can appear as children of an element having this
     * complex type, as integer fingerprints. If the list is unbounded (because of wildcards or the use
     * of xs:anyType), return null.
     *  @param children        an integer set, initially empty, which on return will hold the fingerprints of all permitted
     *                        child elements; if the result contains the value -1, this indicates that it is not possible to enumerate
     *                        all the children, typically because of wildcards. In this case the other contents of the set should
     * @param ignoreWildcards  true if wildcards are to be ignored, rather than a wildcard causing the result to be considered
     *                         an infinite set.
     */

    public void gatherAllPermittedChildren(/*@NotNull*/ HashSet<StructuredQName> children, boolean ignoreWildcards) throws SchemaException {
        children.add(StandardNames.getStructuredQName(StandardNames.XS_INVALID_NAME));
    }

    /**
     * Get a list of all the names of elements that can appear as descendants of an element having this
     * complex type, as integer fingerprints. If the list is unbounded (because of wildcards or the use
     * of xs:anyType), return null.
     *
     * @param descendants an integer set, initially empty, which on return will hold the fingerprints of all permitted
     *                    descendant elements; if the result contains the value -1, this indicates that it is not possible to enumerate
     *                    all the descendants, typically because of wildcards. In this case the other contents of the set should
     *                    be ignored.
     */

    public void gatherAllPermittedDescendants(/*@NotNull*/ HashSet<StructuredQName> descendants) throws SchemaException {
        descendants.add(StandardNames.getStructuredQName(StandardNames.XS_INVALID_NAME));
    }

    /**
     * Assuming an element is a permitted descendant in the content model of this type, determine
     * the type of the element when it appears as a descendant. If it appears with more than one type,
     * return xs:anyType.
     *
     * @param fingerprint the name of the required descendant element
     * @return the type of the descendant element; null if the element cannot appear as a descendant;
     *         anyType if it can appear with several different types
     */

    /*@NotNull*/
    public SchemaType getDescendantElementType(StructuredQName fingerprint) throws SchemaException {
        return this;
    }

    /**
     * Assuming an element is a permitted descendant in the content model of this type, determine
     * the cardinality of the element when it appears as a descendant.
     *
     * @param elementName the name of the required descendant element
     * @return the cardinality of the descendant element within this complex type
     */

    public int getDescendantElementCardinality(StructuredQName elementName) throws SchemaException {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Ask whether this type (or any known type derived from it by extension) allows the element
     * to have children that match a wildcard
     *
     * @return true if the content model of this type, or its extensions, contains an element wildcard
     */

    public boolean containsElementWildcard() {
        return true;
    }

    /**
     * Ask whether there are any assertions defined on this complex type
     *
     * @return true if there are any assertions
     */
    public boolean hasAssertions() {
        return false;
    }
}

