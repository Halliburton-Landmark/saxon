////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.SaxonOutputKeys;
import net.sf.saxon.lib.SerializerFactory;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.*;
import net.sf.saxon.regex.UnicodeString;
import net.sf.saxon.serialize.CharacterMap;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.serialize.SerializationParamsHandler;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;
import net.sf.saxon.z.IntHashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of fn:serialize() as defined in XPath 3.1
 */

public class Serialize extends SystemFunction implements Callable {

    private String[] paramNames = new String[]{
        "allow-duplicate-names", "byte-order-mark", "cdata-section-elements", "doctype-public", "doctype-system",
        "encoding", "escape-uri-attributes", "html-version", "include-content-type", "indent", "item-separator",
        "json-node-output-method", "media-type", "method", "normalization-form", "omit-xml-declaration", "standalone",
        "suppress-indentation", "undeclare-prefixes", "use-character-maps", "version"
    };

    private boolean isParamName(String string) {
        for (String s : paramNames) {
            if (s.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private String[] paramNamesSaxon = new String[]{
            "attribute-order", "character-representation", "double-space", "indent-spaces", "line-length",
            /*"next-in-chain",*/ "recognize-binary", "require-well-formed", "supply-source-locator", "suppress-indentation"
    };

    private boolean isParamNameSaxon(String string) {
        for (String s : paramNamesSaxon) {
            if (s.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private final static Map<String, SequenceType> requiredTypes = new HashMap<String, SequenceType>(40);

    // TODO use StandardNames.METHOD instead of "method", etc?

    static {
        requiredTypes.put("allow-duplicate-names", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("byte-order-mark", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("cdata-section-elements", SequenceType.makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.ALLOWS_ZERO_OR_MORE));
        //QNames-param-type - sequence or an array of xs:QName values. Note that an array will be converted to a sequence, as required
        requiredTypes.put("doctype-public", SequenceType.SINGLE_STRING); //doctype-public-param-type pubid-char-string-type
        requiredTypes.put("doctype-system", SequenceType.SINGLE_STRING); //doctype-system-param-type system-id-string-type
        requiredTypes.put("encoding", SequenceType.SINGLE_STRING); //encoding-param-type encoding-string-type
        requiredTypes.put("escape-uri-attributes", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("html-version", SequenceType.SINGLE_DECIMAL); //decimal-param-type
        requiredTypes.put("include-content-type", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("indent", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("item-separator", SequenceType.SINGLE_STRING); //string-param-type
        requiredTypes.put("json-node-output-method", SequenceType.SINGLE_STRING);
        //json-node-output-method-param-type  json-node-output-method-type - xs:string or xs:QName
        requiredTypes.put("media-type", SequenceType.SINGLE_STRING); //string-param-type
        requiredTypes.put("method", SequenceType.SINGLE_STRING);
        //method-param-type method-type - xs:string or xs:QName
        requiredTypes.put("normalization-form", SequenceType.SINGLE_STRING);
        //NMTOKEN-param-type  BuiltInAtomicType.NMTOKEN
        requiredTypes.put("omit-xml-declaration", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("standalone", SequenceType.OPTIONAL_BOOLEAN); //yes-no-omit-type
        requiredTypes.put("suppress-indentation", SequenceType.makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.ALLOWS_ZERO_OR_MORE));
        //QNames-param-type - sequence or an array of xs:QName values. Note that an array will be converted to a sequence, as required
        requiredTypes.put("undeclare-prefixes", SequenceType.SINGLE_BOOLEAN); //yes-no-param-type
        requiredTypes.put("use-character-maps", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        //use-character-maps-param-type
        requiredTypes.put("version", SequenceType.SINGLE_STRING); //string-param-type
    }

    private final static Map<String, SequenceType> requiredTypesSaxon = new HashMap<String, SequenceType>(20);

    static {
        requiredTypesSaxon.put("attribute-order", SequenceType.makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.ALLOWS_ZERO_OR_MORE));
        //eqnames
        requiredTypesSaxon.put("character-representation", SequenceType.SINGLE_STRING); //string
        requiredTypesSaxon.put("double-space", SequenceType.makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.ALLOWS_ZERO_OR_MORE));
        //eqnames
        requiredTypesSaxon.put("indent-spaces", SequenceType.SINGLE_INTEGER); //integer
        requiredTypesSaxon.put("line-length", SequenceType.SINGLE_INTEGER); //integer
        //requiredTypes.put("next-in-chain", SequenceType.SINGLE_STRING); //uri
        requiredTypesSaxon.put("recognize-binary", SequenceType.SINGLE_BOOLEAN); //boolean
        requiredTypesSaxon.put("require-well-formed", SequenceType.SINGLE_BOOLEAN); //boolean
        requiredTypesSaxon.put("supply-source-locator", SequenceType.SINGLE_BOOLEAN); //boolean
        requiredTypesSaxon.put("suppress-indentation", SequenceType.makeSequenceType(BuiltInAtomicType.QNAME, StaticProperty.ALLOWS_ZERO_OR_MORE));
        //eqnames
    }

    /**
     * Check the options supplied:
     * 1. ignore any other options not in the specs;
     * 2. validate the types of the option values supplied.
     */

    private MapItem checkOptions(MapItem map, XPathContext context) throws XPathException {
        HashTrieMap result = new HashTrieMap(context);
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();

        AtomicIterator keysIterator = map.keys();
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String keyName = key.getStringValue();
                if (isParamName(keyName)) {
                    RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.OPTION, keyName, 0);
                    role.setErrorCode("XPTY0004");
                    //If any serialization error occurs, including the detection of an invalid value for a serialization
                    // parameter, this results in the fn:serialize call failing with a dynamic error.
                    Sequence converted = th.applyFunctionConversionRules(
                        map.get(key), requiredTypes.get(keyName), role, ExplicitLocation.UNKNOWN_LOCATION);
                    converted = SequenceTool.toGroundedValue(converted);
                    result = result.addEntry(key, converted);
                }
            } else if (key instanceof QNameValue) {
                if (key.getComponent(AccessorFn.Component.NAMESPACE).getStringValue().equals("http://saxon.sf.net/")) {
                    // Capture Saxon serialization parameters
                    String keyName = ((QNameValue) key).getLocalName();
                    if (isParamNameSaxon(keyName)) {
                        RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.OPTION, keyName, 0);
                        Sequence converted = th.applyFunctionConversionRules(
                                map.get(key), requiredTypesSaxon.get(keyName), role, ExplicitLocation.UNKNOWN_LOCATION);
                        converted = SequenceTool.toGroundedValue(converted);
                        result = result.addEntry(key, converted);
                    }
                }
                // Implementation-defined serialization parameters in an unrecognised namespace are ignored.
            }
        }
        return result;
    }

    // Convert a boolean value to a yes-no-type string.

    private String toYesNoTypeString(Sequence seqVal) throws XPathException {
        String s;
        boolean booleanValue = ((BooleanValue) seqVal.head()).getBooleanValue();
        if (booleanValue) {
            s = "yes";
        } else {
            s = "no";
        }
        return s;
    }

    // Convert a value to a yes-no-omit-type string.

    private String toYesNoOmitTypeString(Sequence seqVal) throws XPathException {
        String stringVal = "";
        if (seqVal instanceof EmptySequence) {
            stringVal = "omit";
        } else if (seqVal.head() instanceof BooleanValue) {
            stringVal = toYesNoTypeString(seqVal);
        }
        // otherwise invalid
        return stringVal;
    }

    // Convert a sequence of QNames to a qnames-type string (containing a space-separated list of the QNames).

    private String toQNamesTypeString(Sequence seqVal) throws XPathException {
        SequenceIterator iterator = seqVal.iterate();
        Item item;
        String stringVal = "";
        while ((item = iterator.next()) != null) {
            QNameValue qNameValue = (QNameValue) item;
            stringVal = stringVal + " {" + qNameValue.getComponent(AccessorFn.Component.NAMESPACE).getStringValue() + '}' + qNameValue.getComponent(AccessorFn.Component.LOCALNAME).getStringValue();
        }
        return stringVal;
    }

    // Convert a QName or string value to a method-type (or json-node-output-method-type) string.

    private String toMethodTypeString(Sequence seqVal) throws XPathException {
        String stringVal;
        if (seqVal.head() instanceof QNameValue) {
            QNameValue qNameValue = (QNameValue) seqVal.head();
            stringVal = '{' + qNameValue.getComponent(AccessorFn.Component.NAMESPACE).toString() + '}' + qNameValue.getComponent(AccessorFn.Component.LOCALNAME);
        } else {
            stringVal = seqVal.head().getStringValue();
        }
        return stringVal;
    }


    /**
     * By the option parameter conventions, check the character map:
     * 1. the keys must all be single character strings;
     * 2. the value must be a string.
     * No conversions are applied.
     */

    private MapItem checkCharacterMapOptions(MapItem map, XPathContext context) throws XPathException {
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        for (KeyValuePair pair : map) {
            AtomicValue key = pair.key;
            if (!(key instanceof StringValue && ((StringValue)key).getStringLength() == 1)) {
                throw new XPathException("Keys in a character map must all be one-character strings. Found " + Err.wrap(key.toString()), "XPTY0004");
            }
            if (!SequenceType.SINGLE_STRING.matches(pair.value, th)) {
                throw new XPathException("Values in a character map must all be single strings. Found " + Err.wrap(key.toString()), "XPTY0004");
            }
        }
        return map;
    }

    // Convert a map defining a character map to a CharacterMap

    private CharacterMap toCharacterMap(Sequence seqVal, XPathContext context) throws XPathException {
        MapItem charMap = checkCharacterMapOptions((MapItem) seqVal.head(), context);
        AtomicIterator iterator = charMap.keys();
        AtomicValue charKey;
        IntHashMap<String> intHashMap = new IntHashMap<String>();
        while ((charKey = iterator.next()) != null) {
            String ch = charKey.getStringValue();
            String str = charMap.get(charKey).head().getStringValue();
            UnicodeString chValue = UnicodeString.makeUnicodeString(ch);
            if (chValue.uLength() != 1) {
                throw new XPathException("In the serialization parameter for the character map, each character to be mapped " +
                    "must be a single Unicode character", "SEPM0016");
            }
            int code = chValue.uCharAt(0);
            String prev = intHashMap.put(code, str);
            if (prev != null) { // This should never happen in this case because keys in a HashTrieMap must be unique
                throw new XPathException("In the serialization parameters, the character map contains two entries for the character \\u" +
                    Integer.toHexString(65536 + code).substring(1), "SEPM0018");
            }
        }
        StructuredQName name = new StructuredQName("output", NamespaceConstant.OUTPUT, "serialization-parameters");
        CharacterMap characterMap = new CharacterMap(name, intHashMap);
        return characterMap;
    }

    // Just set properties using props.setProperty("allow-duplicate-names", stringVal); as below?
    // or should be using something similar to
    // ResultDocument.setSerializationProperty(properties, uri, lname, value,
    // new InscopeNamespaceResolver(child), false, node.getConfiguration());
    // for validation?

    private Properties setSerializationParams(Properties props, MapItem map, CharacterMapIndex charMapIndex, XPathContext context) throws XPathException {
        Sequence seqVal;
        if ((seqVal = map.get(new StringValue("allow-duplicate-names"))) != null) {
            props.setProperty("allow-duplicate-names", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("byte-order-mark"))) != null) {
            props.setProperty("byte-order-mark", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("cdata-section-elements"))) != null) {
            props.setProperty("cdata-section-elements", toQNamesTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("doctype-public"))) != null) {
            props.setProperty("doctype-public", seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new StringValue("doctype-system"))) != null) {
            props.setProperty("doctype-system", seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new StringValue("encoding"))) != null) {
            props.setProperty("encoding", seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new StringValue("escape-uri-attributes"))) != null) {
            props.setProperty("escape-uri-attributes", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("html-version"))) != null) {
            props.setProperty("html-version", (String) ((DecimalValue) seqVal.head()).getPrimitiveStringValue()); // TODO is this right??
        }
        if ((seqVal = map.get(new StringValue("include-content-type"))) != null) {
            props.setProperty("include-content-type", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("indent"))) != null) {
            props.setProperty("indent", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("item-separator"))) != null) {
            props.setProperty("item-separator", seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new StringValue("json-node-output-method"))) != null) {
            props.setProperty("json-node-output-method", toMethodTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("media-type"))) != null) {
            props.setProperty("media-type", seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new StringValue("method"))) != null) {
            props.setProperty(OutputKeys.METHOD, toMethodTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("normalization-form"))) != null) {
            props.setProperty("normalization-form", seqVal.head().getStringValue()); //NMTOKEN param type
        }
        if ((seqVal = map.get(new StringValue("omit-xml-declaration"))) != null) {
            props.setProperty("omit-xml-declaration", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("standalone"))) != null) {
            props.setProperty("standalone", toYesNoOmitTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("suppress-indentation"))) != null) {
            props.setProperty("suppress-indentation", toQNamesTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("undeclare-prefixes"))) != null) {
            props.setProperty("undeclare-prefixes", toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new StringValue("use-character-maps"))) != null) {
            CharacterMap characterMap = toCharacterMap(seqVal, context);
            charMapIndex.putCharacterMap(new StructuredQName("", "", "charMap"), characterMap);
            props.setProperty(SaxonOutputKeys.USE_CHARACTER_MAPS, "charMap");
        }
        if ((seqVal = map.get(new StringValue("version"))) != null) {
            props.setProperty("version", seqVal.head().getStringValue());
        }
        // Saxon extension serialization parameters
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "attribute-order"))) != null) {
            props.setProperty(SaxonOutputKeys.ATTRIBUTE_ORDER, toQNamesTypeString(seqVal));
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "character-representation"))) != null) {
            props.setProperty(SaxonOutputKeys.CHARACTER_REPRESENTATION, seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "double-space"))) != null) {
            props.setProperty(SaxonOutputKeys.DOUBLE_SPACE, toQNamesTypeString(seqVal));
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "indent-spaces"))) != null) {
            props.setProperty(SaxonOutputKeys.INDENT_SPACES, seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "line-length"))) != null) {
            props.setProperty(SaxonOutputKeys.LINE_LENGTH, seqVal.head().getStringValue());
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "recognize-binary"))) != null) {
            props.setProperty(SaxonOutputKeys.RECOGNIZE_BINARY, toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "require-well-formed"))) != null) {
            props.setProperty(SaxonOutputKeys.REQUIRE_WELL_FORMED, toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "supply-source-locator"))) != null) {
            props.setProperty(SaxonOutputKeys.SUPPLY_SOURCE_LOCATOR, toYesNoTypeString(seqVal));
        }
        if ((seqVal = map.get(new QNameValue("saxon", "http://saxon.sf.net/", "suppress-indentation"))) != null) {
            props.setProperty(SaxonOutputKeys.SUPPRESS_INDENTATION, toQNamesTypeString(seqVal));
        }
        return props;
    }


    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        return evalSerialize(arguments[0].iterate(),
            arguments.length == 1 ? null : arguments[1].head(), context);
    }

    private StringValue evalSerialize(SequenceIterator iter, /*@Nullable*/ Item param, XPathContext context) throws XPathException {

        Properties props = new Properties();
        CharacterMapIndex charMapIndex = new CharacterMapIndex();

        // The default parameter values are implementation-defined when an output:serialization-parameters
        // element is used (or when the argument is omitted), but are fixed by this specification in the
        // case where a map (including an empty map) is supplied for the argument.
        if (param != null) {
            if (param instanceof NodeInfo) {
                NodeInfo paramNode = (NodeInfo) param;
                if (paramNode.getNodeKind() != Type.ELEMENT ||
                    !NamespaceConstant.OUTPUT.equals(paramNode.getURI()) ||
                    !"serialization-parameters".equals(paramNode.getLocalPart())) {
                    throw new XPathException("Second argument to fn:serialize() must be an element named {"
                        + NamespaceConstant.OUTPUT + "}serialization-parameters", "XPTY0004");
                }
                SerializationParamsHandler sph = new SerializationParamsHandler();
                //sph.setLocation(getLocation());
                sph.setSerializationParams(paramNode);
                props = sph.getSerializationProperties();
                CharacterMap charMap = sph.getCharacterMap();
                if (charMap != null) {
                    // convoluted code here because the SerializerFactory expects an index of character maps
                    props.setProperty(SaxonOutputKeys.USE_CHARACTER_MAPS, "charMap");
                    charMapIndex = new CharacterMapIndex();
                    charMapIndex.putCharacterMap(new StructuredQName("", "", "charMap"), charMap);
                }
            } else if (param instanceof MapItem) { // XPath 3.1 only??
                MapItem paramMap = (MapItem) param;
                paramMap = checkOptions(paramMap, context);
                props = setSerializationParams(props, paramMap, charMapIndex, context);
            } else {
                throw new XPathException("Second argument to fn:serialize() must either be an element named {"
                    + NamespaceConstant.OUTPUT + "}serialization-parameters, or a map (if using XPath 3.1)", "XPTY0004");
            }


        }

        if (props.getProperty(OutputKeys.METHOD) == null) {
            props.setProperty(OutputKeys.METHOD, "xml");
        }
        if (props.getProperty(OutputKeys.OMIT_XML_DECLARATION) == null) {
            props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "true");
        }

        // TODO add more spec-defined defaults here (for both cases)
        try {
            StringWriter result = new StringWriter();

            SerializerFactory sf = context.getConfiguration().getSerializerFactory();
            PipelineConfiguration pipe = context.getConfiguration().makePipelineConfiguration();
            pipe.setController(context.getController());
            SequenceReceiver out = sf.getReceiver(new StreamResult(result), pipe, props, charMapIndex);
            out.open();
            Item item;
            while ((item = iter.next()) != null) {
                out.append(item);
            }
            out.close();
            return new StringValue(result.toString());
        } catch (Exception e) {
            if (e instanceof XPathException) {
                ((XPathException) e).maybeSetErrorCode("SENR0001");
                throw (XPathException) e;
            } else {
                XPathException se = new XPathException("Serialization unsuccessful", e);
                se.setErrorCode("SEPM0016");
                throw se;
            }
        }


    }

}

// Copyright (c) 2011 Saxonica Limited. All rights reserved.
