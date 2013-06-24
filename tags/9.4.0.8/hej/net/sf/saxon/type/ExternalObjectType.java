package net.sf.saxon.type;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.ValueOf;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the type of an external Java object returned by
 * an extension function, or supplied as an external variable/parameter.
 */

public class ExternalObjectType implements AtomicType, Serializable {

    private Class javaClass;
    private Configuration config;
    int fingerprint;

    /**
     * Create an anonymous external object type. When constructed this way, the type will not
     * have a nameCode or fingerprint in the name pool, so methods asking for the name of the type
     * will return no value.
     * @param javaClass the Java class to which this type corresponds
     */

    public ExternalObjectType(Class javaClass) {
        this.javaClass = javaClass;
    }

    /**
     * Create an external object type. When constructed this way, the type will
     * have a fingerprint in the name pool based on the Java class name
     * @param javaClass the Java class to which this type corresponds
     * @param config the Saxon configuration
     */

    public ExternalObjectType(/*@NotNull*/ Class javaClass, /*@NotNull*/ Configuration config) {
        this.javaClass = javaClass;
        this.config = config;
        final String localName = javaClass.getName().replace('$', '_');
        fingerprint = config.getNamePool().allocate("", NamespaceConstant.JAVA_TYPE, localName);
    }

    /**
     * Get the local name of this type
     *
     * @return the local name of this type definition, if it has one. Return null in the case of an
     *         anonymous type.
     */

    /*@Nullable*/ public String getName() {
        if (config == null) {
            return null;
        } else {
            return config.getNamePool().getLocalName(fingerprint);
        }
    }

    /**
     * Get the target namespace of this type
     *
     * @return the target namespace of this type definition, if it has one. Return null in the case
     *         of an anonymous type, and in the case of a global type defined in a no-namespace schema.
     */

    /*@Nullable*/ public String getTargetNamespace() {
        if (config == null) {
            return null;
        } else {
            return config.getNamePool().getURI(fingerprint);
        }
    }

    /**
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     */

    public boolean isExternalType() {
        return true;
    }

    /**
     * Determine whether this is a built-in type or a user-defined type
     * @return false - external types are not built in
     */

    public boolean isBuiltInType() {
        return false;
    }

    /**
     * Get the name of this type as a StructuredQName, unless the type is anonymous, in which case
     * return null
     * @return the name of the atomic type, or null if the type is anonymous.
     */

    /*@Nullable*/ public StructuredQName getTypeName() {
        return null;
    }

    /**
     * Get the redefinition level. This is zero for a component that has not been redefined;
     * for a redefinition of a level-0 component, it is 1; for a redefinition of a level-N
     * component, it is N+1. This concept is used to support the notion of "pervasive" redefinition:
     * if a component is redefined at several levels, the top level wins, but it is an error to have
     * two versions of the component at the same redefinition level.
     * @return the redefinition level
     */

    public int getRedefinitionLevel() {
        return 0;
    }

    /**
     * Determine whether the type is abstract, that is, whether it cannot have instances that are not also
     * instances of some concrete subtype
     * @return false - external types are not abstract
     */

    public boolean isAbstract() {
        return false;
    }

    /**
     * Determine whether the atomic type is a primitive type.  The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; and all supertypes of these (xs:anyAtomicType, xs:numeric, ...)
     *
     * @return true if the type is considered primitive under the above rules
     */

    public boolean isPrimitiveType() {
        return false;
    }

    /**
     * Determine whether the atomic type is ordered, that is, whether less-than and greater-than comparisons
     * are permitted
     *
     * @return true if ordering operations are permitted
     */

    public boolean isOrdered() {
        return false;  
    }

    /**
     * Ask whether this is a plain type (a type whose instances are always atomic values)
     * @return true
     */

    public boolean isPlainType() {
        return true;
    }    

    /**
     * Get the URI of the schema document where the type was originally defined.
     *
     * @return the URI of the schema document. Returns null if the information is unknown or if this
     *         is a built-in type
     */

    /*@Nullable*/ public String getSystemId() {
        return null;
    }

    /**
     * Get the validation status - always valid
     */
    public final int getValidationStatus()  {
        return VALIDATED;
    }

    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public final int getBlock() {
        return 0;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link SchemaType#DERIVATION_RESTRICTION}
     */

    public final int getDerivationMethod() {
        return SchemaType.DERIVATION_RESTRICTION;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public final boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Get the namecode of the name of this type. This includes the prefix from the original
     * type declaration: in the case of built-in types, there may be a conventional prefix
     * or there may be no prefix.
     */

    public int getNameCode() {
        return fingerprint;
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public final boolean isComplexType() {
        return false;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    /*@NotNull*/ public final SchemaType getBaseType() {
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    /*@NotNull*/ public ItemType getPrimitiveItemType() {
        return this;
    }

    /**
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public int getPrimitiveType() {
        return StandardNames.XS_ANY_ATOMIC_TYPE;
    }

    /**
     * Produce a representation of this type name for use in error messages.
     * Where this is a QName, it will use conventional prefixes
     */

    /*@NotNull*/ public String toString(NamePool pool) {
        return getDisplayName();
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    /*@NotNull*/ public AtomicType getAtomizedItemType() {
        return this;
    }

    /**
     * Ask whether values of this type are atomizable
     * @return true unless it is known that these items will be elements with element-only
     *         content, in which case return false
     */

    public boolean isAtomizable() {
        return true;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    /*@NotNull*/ public SchemaType getKnownBaseType() {
        return getBaseType();
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(/*@NotNull*/ SchemaType other) {
        return (other.getFingerprint() == getFingerprint());
    }

    /**
     * Get the relationship of this external object type to another external object type
     * @param other the other external object type
     * @return the relationship of this external object type to another external object type,
     * as one of the constants in class {@link TypeHierarchy}, for example {@link TypeHierarchy#SUBSUMES}
     */

    public int getRelationship(/*@NotNull*/ ExternalObjectType other) {
        Class j2 = other.javaClass;
        if (javaClass.equals(j2)) {
            return TypeHierarchy.SAME_TYPE;
        } else if (javaClass.isAssignableFrom(j2)) {
            return TypeHierarchy.SUBSUMES;
        } else if (j2.isAssignableFrom(javaClass)) {
            return TypeHierarchy.SUBSUMED_BY;
        } else if (javaClass.isInterface() || j2.isInterface()) {
            return TypeHierarchy.OVERLAPS; // there may be an overlap, we play safe
        } else {
            return TypeHierarchy.DISJOINT;
        }
    }

    /*@NotNull*/ public String getDescription() {
        return getDisplayName();
    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException {
        //return;
    }

    /**
     * Returns true if this SchemaType is a SimpleType
     *
     * @return true (always)
     */

    public final boolean isSimpleType() {
        return true;
    }

    /**
     * Test whether this Simple Type is an atomic type
     * @return true, this is considered to be an atomic type
     */

    public boolean isAtomicType() {
        return true;
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
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     *
     * @return true if this is a list type
     */

    public boolean isListType() {
        return false;
    }

    /**
     * Return true if this type is a union type (that is, if its variety is union)
     *
     * @return true for a union type
     */

    public boolean isUnionType() {
        return false;
    }

    /**
     * Determine the whitespace normalization required for values of this type
     *
     * @return one of PRESERVE, REPLACE, COLLAPSE
     */

    public int getWhitespaceAction() {
        return Whitespace.PRESERVE;
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     *
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public CharSequence applyWhitespaceNormalization(CharSequence value) throws ValidationException {
        return value;
    }

    /**
     * Returns the built-in base type this type is derived from.
     *
     * @return the first built-in type found when searching up the type hierarchy
     */
    /*@NotNull*/ public SchemaType getBuiltInBaseType() {
        return this;
    }

    /**
     * Test whether this simple type is namespace-sensitive, that is, whether
     * it is derived from xs:QName or xs:NOTATION
     *
     * @return true if this type is derived from xs:QName or xs:NOTATION
     */

    public boolean isNamespaceSensitive() {
        return false;
    }

    /**
     * Test whether this is an anonymous type
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
        return false;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type
     *
     * @param node the node whose typed value is required
     * @return an iterator over the items making up the typed value of this node. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    /*@NotNull*/ public final SequenceIterator getTypedValue(NodeInfo node) {
        throw new IllegalStateException("The type annotation of a node cannot be an external object type");
    }

    /**
     * Get the typed value of a node that is annotated with this schema type. The result of this method will always be consistent with the method
     * {@link #getTypedValue}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    /*@NotNull*/ public Value atomize(NodeInfo node) throws XPathException {
        throw new IllegalStateException("The type annotation of a node cannot be an external object type");
    }

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     *
     * @param value    the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     *                 in the content of values. Can supply null, in which case any namespace-sensitive content
     *                 will be rejected.
     * @param rules
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    /*@NotNull*/ public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, ConversionRules rules)
            throws ValidationException {
        throw new ValidationException("Cannot validate a string against an external object type");
    }


    /**
     * Validate that a primitive atomic value is a valid instance of a type derived from the
     * same primitive type.
     *
     * @param primValue    the value in the value space of the primitive type.
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     *                     is used. This value is checked against the pattern facet (if any)
     * @param rules
     * @return null if the value is valid; otherwise, a ValidationFailure object indicating
     *         the nature of the error.
     * @throws UnsupportedOperationException in the case of an external object type
     */

    /*@NotNull*/ public ValidationFailure validate(AtomicValue primValue, CharSequence lexicalValue, ConversionRules rules) {
        throw new UnsupportedOperationException("validate");
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
     *                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @param env        the static evaluation context
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public void analyzeContentExpression(/*@NotNull*/ Expression expression, int kind, StaticContext env) throws XPathException {
        analyzeContentExpression(this, expression, env, kind);
    }

   /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     * @param simpleType the simple type against which the expression is to be checked
     * @param expression the expression that delivers the content
     * @param env the static evaluation context
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
     *                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public static void analyzeContentExpression(SimpleType simpleType, /*@NotNull*/ Expression expression, StaticContext env, int kind)
    throws XPathException {
        if (kind == Type.ELEMENT) {
            expression.checkPermittedContents(simpleType, env, true);
        } else if (kind == Type.ATTRIBUTE) {
            // for attributes, do a check only for text nodes and atomic values: anything else gets atomized
            if (expression instanceof ValueOf || expression instanceof Literal) {
                expression.checkPermittedContents(simpleType, env, true);
            }
        }
    }

    /**
     * Get the Java class to which this external object type corresponds
     * @return the corresponding Java class
     */

    public Class getJavaClass() {
        return javaClass;
    }

    /**
     * Test whether a given item conforms to this type
     *
     * @param item    The item to be tested
     * @param context the XPath dynamic evaluation context
     * @return true if the item is an instance of this type; false otherwise
     */
    public boolean matches(/*@NotNull*/ Item item, /*@NotNull*/ XPathContext context) {
        return matchesItem(item, false, context.getConfiguration());
    }    

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(/*@NotNull*/ Item item, boolean allowURIPromotion, Configuration config) {
        if (item instanceof ObjectValue) {
            Object obj = ((ObjectValue)item).getObject();
            return javaClass.isAssignableFrom(obj.getClass());
        }
        return false;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @param rules
     * @return null if validation succeeds; return a ValidationException describing the validation failure
     * if validation fails, unless throwException is true, in which case the exception is thrown rather than
     * being returned.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    /*@NotNull*/ public ValidationFailure validateContent(/*@NotNull*/ CharSequence value, NamespaceResolver nsResolver, /*@NotNull*/ ConversionRules rules) {
        throw new UnsupportedOperationException("Cannot use an external object type for validation");
    }

    /*@NotNull*/ public ItemType getSuperType(TypeHierarchy th) {
        if (javaClass == Object.class) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
        Class javaSuper = javaClass.getSuperclass();
        if (javaSuper == null) {
            // this happens for an interface
            return BuiltInAtomicType.ANY_ATOMIC;
        }
        return new ExternalObjectType(javaSuper, config);
    }

    public int getFingerprint() {
        return fingerprint;
    }

    /*@NotNull*/ public String toString() {
        String name = javaClass.getName();
        name = name.replace('$', '-');
        return "javatype:" + name;
    }

    /*@NotNull*/ public String getDisplayName() {
        return toString();
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return javaClass.hashCode();
    }

    /**
     * Test whether two ExternalObjectType objects represent the same type
     * @param obj the other ExternalObjectType
     * @return true if the two objects represent the same type
     */

    public boolean equals(/*@NotNull*/ Object obj) {
        return obj instanceof ExternalObjectType && javaClass == ((ExternalObjectType)obj).javaClass;
    }

    /**
     * Apply any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess
     * @param input the value to be preprocessed
     * @return the value after preprocessing
     */

    public CharSequence preprocess(CharSequence input) {
        return input;
    }

    /**
     * Reverse any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess. This is called when converting a value of this type to
     * a string
     * @param input the value to be postprocessed: this is the "ordinary" result of converting
     *              the value to a string
     * @return the value after postprocessing
     */

    public CharSequence postprocess(CharSequence input) throws ValidationException {
        return input;
    }

    /**
     * Visit all the schema components used in this ItemType definition
     * @param visitor the visitor class to be called when each component is visited
     */

    public void visitNamedSchemaComponents(SchemaComponentVisitor visitor) throws XPathException {
        // no action
    }

    /**
     * Get the set of atomic types that are subsumed by this type
     *
     * @return for an atomic type, the type itself; for a union type, the set of atomic types
     *         in its transitive membership
     */
    /*@NotNull*/ public Set<PlainType> getPlainMemberTypes() {
        Set<PlainType> s = new HashSet<PlainType>(1);
        s.add(this);
        return s;
    }

    public double getDefaultPriority() {
        return 0.5;
    }
}
//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//