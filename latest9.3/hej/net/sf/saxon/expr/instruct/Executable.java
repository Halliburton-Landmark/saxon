package net.sf.saxon.expr.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.RuleBasedStripper;
import net.sf.saxon.expr.CollationMap;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.QueryModule;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.StripSpaceRules;
import net.sf.saxon.trans.XPathException;

import java.io.Serializable;
import java.util.*;

/**
 * A compiled stylesheet or a query in executable form.
 * Note that the original stylesheet tree is not retained.
 */

public class Executable implements Serializable {

    // the Configuration options
    private Configuration config;

    // definitions of strip/preserve space action
    private StripSpaceRules stripperRules;

    // boolean indicating whether any whitespace is stripped
    private boolean stripsWhitespace;

    // definitions of keys, including keys created by the optimizer
    private KeyManager keyManager;

    // the map of slots used for global variables and params
    private SlotManager globalVariableMap;

    // Index of global variables and parameters, by name
    // The key is the StructuredQName representing the variable name
    // The value is the compiled GlobalVariable object.
    private HashMap<StructuredQName, GlobalVariable> compiledGlobalVariables;

    // default output properties (for the unnamed output format)
    private Properties defaultOutputProperties;


    // count of the maximum number of local variables in the match pattern of any template rule
    private int largestPatternStackFrame = 0;

    // table of named collations defined in the stylesheet/query
    private CollationMap collationTable;

    // table of character maps indexed by StructuredQName
    private CharacterMapIndex characterMapIndex;

    // location map for expressions in this executable
    private LocationMap locationMap = new LocationMap();

    // hash table of query library modules
    private HashMap<String, List<QueryModule>> queryLibraryModules;

    // flag to indicate that source documents are to have their type annotations stripped
    private boolean stripsInputTypeAnnotations;

    // list of functions available in the static context
    private FunctionLibraryList functionLibrary;

    // flag to indicate whether the principal language is for example XSLT or XQuery
    private int hostLanguage = Configuration.XSLT;

    // a list of required parameters, identified by the structured QName of their names
    private HashSet<StructuredQName> requiredParams = null;

    // Hash table of named (and unnamed) output declarations. This is assembled only
    // if there is a need for it: that is, if there is a call on xsl:result-document
    // with a format attribute computed at run-time. The key is a StructuredQName object,
    // the value is a Properties object
    private HashMap<StructuredQName, Properties> outputDeclarations = null;

    // a boolean, true if the executable represents a stylesheet that uses xsl:result-document
    private boolean createsSecondaryResult = false;

    // a boolean, indicates that the executable is schema-aware. This will true by default only
    // if it statically imports a schema. If the executable is not schema-aware, then
    // all input documents must be untyped.
    private boolean schemaAware = false;

    // The name of a global variable that is coupled to the initial context item. If not null,
    // the initial context item will be assigned to this variable. Properties of the variable
    // such as its required type, its default value, and whether it is required or not, thus
    // apply implicitly to the initial context item.
    private StructuredQName initialContextItemVariableName = null;

    /**
     * Create a new Executable (a collection of stylesheet modules and/or query modules)
     * @param config the Saxon Configuration
     */

    public Executable(Configuration config) {
        setConfiguration(config);
    }

    /**
     * Set the configuration
     * @param config the Configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration
     * @return the Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }



    /**
     * Set the host language
     * @param language the host language, as a constant such as {@link Configuration#XSLT} or
     * {@link Configuration#XQUERY}
     */

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    /**
     * Get the host language
     *
     * @return a value identifying the host language: {@link Configuration#XQUERY} or {@link Configuration#XSLT}
     *         or {@link Configuration#JAVA_APPLICATION}
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Get the library containing all the in-scope functions in the static context
     *
     * @return the function libary
     */

    public FunctionLibraryList getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Set the library containing all the in-scope functions in the static context
     *
     * @param functionLibrary the function libary
     */

    public void setFunctionLibrary(FunctionLibraryList functionLibrary) {
        //System.err.println("***" + this + " setFunctionLib to " + functionLibrary);
        this.functionLibrary = functionLibrary;
    }

    /**
     * Set the index of named character maps
     *
     * @param cmi a hash table that maps the names of character maps
     *            to the HashMap objects representing the character maps
     */

    public void setCharacterMapIndex(CharacterMapIndex cmi) {
        characterMapIndex = cmi;
    }

    /**
     * Get the index of named character maps
     *
     * @return the hash table that maps the names of character maps
     *         to the IntHashMap objects representing the character maps
     */

    public CharacterMapIndex getCharacterMapIndex() {
        if (characterMapIndex == null) {
            characterMapIndex = new CharacterMapIndex();
        }
        return characterMapIndex;
    }

    /**
     * Set the rules determining which nodes are to be stripped from the tree
     *
     * @param rules a Mode object containing the whitespace stripping rules. A Mode
     *              is generally a collection of template rules, but it is reused here to represent
     *              a collection of stripping rules.
     */

    public void setStripperRules(StripSpaceRules rules) {
        stripperRules = rules;
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     *
     * @return a Mode object containing the whitespace stripping rules. A Mode
     *         is generally a collection of template rules, but it is reused here to represent
     *         a collection of stripping rules.
     */

    public StripSpaceRules getStripperRules() {
        return stripperRules;
    }

    /**
     * Indicate that the stylesheet does some whitespace stripping
     *
     * @param strips true if the stylesheet performs whitespace stripping
     *               of one or more elements.
     */

    public void setStripsWhitespace(boolean strips) {
        stripsWhitespace = strips;
    }

    /**
     * Create a Stripper which handles whitespace stripping definitions
     *
     * @return the constructed Stripper object
     */

    public RuleBasedStripper newStripper() {
        return new RuleBasedStripper(stripperRules);
    }

    /**
     * Determine whether this stylesheet does any whitespace stripping
     *
     * @return true if the stylesheet performs whitespace stripping
     *         of one or more elements.
     */

    public boolean stripsWhitespace() {
        return stripsWhitespace;
    }

    /**
     * Set whether source documents are to have their type annotations stripped
     * @param strips true if type annotations are to be stripped
     */

    public void setStripsInputTypeAnnotations(boolean strips) {
        stripsInputTypeAnnotations = strips;
    }

    /**
     * Ask whether source documents are to have their type annotations stripped
     * @return true if type annotations are stripped from source documents
     */

    public boolean stripsInputTypeAnnotations() {
        return stripsInputTypeAnnotations;
    }

    /**
     * Set the KeyManager which handles key definitions
     * @param km the KeyManager containing the xsl:key definitions
     */

    public void setKeyManager(KeyManager km) {
        keyManager = km;
    }

    /**
     * Get the KeyManager which handles key definitions
     *
     * @return the KeyManager containing the xsl:key definitions
     */

    public KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = new KeyManager(getConfiguration());
        }
        return keyManager;
    }

    /**
     * Set the default output properties (the properties for the unnamed output format)
     *
     * @param properties the output properties to be used when the unnamed output format
     *                   is selected
     */

    public void setDefaultOutputProperties(Properties properties) {
        defaultOutputProperties = properties;
    }

    /**
     * Get the default output properties
     *
     * @return the properties for the unnamed output format
     */

    public Properties getDefaultOutputProperties() {
        if (defaultOutputProperties == null) {
            defaultOutputProperties = new Properties();
        }
        return defaultOutputProperties;
    }

    /**
     * Add a named output format
     *
     * @param qName the structured QName of the output format
     * @param properties  the properties of the output format
     */

    public void setOutputProperties(StructuredQName qName, Properties properties) {
        if (outputDeclarations == null) {
            outputDeclarations = new HashMap<StructuredQName, Properties>(5);
        }
        outputDeclarations.put(qName, properties);
    }

    /**
     * Get a named output format
     *
     * @param qName the name of the output format
     * @return properties the properties of the output format. Return null if there are
     *         no output properties with the given name
     */

    public Properties getOutputProperties(StructuredQName qName) {
        if (outputDeclarations == null) {
            return null;
        } else {
            return outputDeclarations.get(qName);
        }
    }

    /**
     * Set the table of collations
     *
     * @param table a hash table that maps collation names (URIs) to objects representing the
     *              collation information
     */

    public void setCollationTable(CollationMap table) {
        collationTable = table;
    }

    /**
     * Get the table of collations
     *
     * @return a hash table that maps collation names (URIs) to objects representing the
     *         collation information
     */

    public CollationMap getCollationTable() {
        if (collationTable == null) {
            collationTable = new CollationMap(config);
        }
        return collationTable;
    }

    /**
     * Find a named collation.
     *
     * @param name identifies the name of the collation required; null indicates that the default
     *             collation is required
     * @return the requested collation, or null if the collation is not found
     */

    public StringCollator getNamedCollation(String name) {
        if (collationTable == null) {
            collationTable = new CollationMap(config);
        }
        return collationTable.getNamedCollation(name);
    }

    /**
     * Add an XQuery library module to the configuration. The Executable maintains a table indicating
     * for each module namespace, the set of modules that have been loaded from that namespace. If a
     * module import is encountered that specifies no location hint, all the known modules for that
     * namespace are imported.
     * @param module the library module to be added to this executable
     */

    public void addQueryLibraryModule(QueryModule module) {
        if (queryLibraryModules == null) {
            queryLibraryModules = new HashMap(5);
        }
        String uri = module.getModuleNamespace();
        List<QueryModule> existing = queryLibraryModules.get(uri);
        if (existing == null) {
            existing = new ArrayList(5);
            existing.add(module);
            queryLibraryModules.put(uri, existing);
        } else {
            existing.add(module);
        }
    }

    /**
     * Locate the known XQuery library modules for a given module namespace.
     *
     * @param namespace the module namespace URI
     * @return a list of items each of which is the StaticQueryContext representing a module, or
     *         null if the module namespace is unknown
     */

    public List<QueryModule> getQueryLibraryModules(String namespace) {
        if (queryLibraryModules == null) {
            return null;
        }
        return queryLibraryModules.get(namespace);
    }

    /**
     * Get the query library module with a given systemID
     * @param systemId the SystemId of the required module
     * @param topModule the top-level query module (usually a main module, except when
     * importing library modules into XSLT)
     * @return the module with that system id if found, otherwise null
     */

    public QueryModule getQueryModuleWithSystemId(String systemId, QueryModule topModule) {
        if (systemId.equals(topModule.getSystemId())) {
            return topModule;
        }
        Iterator miter = getQueryLibraryModules();
        while (miter.hasNext()) {
            QueryModule sqc = (QueryModule)miter.next();
            if (sqc.getSystemId().equals(systemId)) {
                return sqc;
            }
        }
        return null;
    }

    /**
     * Get an iterator over all the query library modules (does not include the main module)
     * @return an iterator whose returned items are instances of {@link QueryModule}
     */

    public Iterator getQueryLibraryModules() {
        if (queryLibraryModules == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            List modules = new ArrayList();
            Iterator iter = queryLibraryModules.values().iterator();
            while (iter.hasNext()) {
                List mods = (List)iter.next();
                modules.addAll(mods);
            }
            return modules.iterator();
        }
    }

    /**
     * Fix up global variables and functions in all query modules. This is done right at the end, because
     * recursive imports are permitted
     * @param main the main query module
     * @param checkForCycles if a check for cyclicity among modules is to be performed. This is a check for
     * cycles at the level of a module (error XQST0093)
     */

    public void fixupQueryModules(QueryModule main, boolean checkForCycles) throws XPathException {

        main.bindUnboundVariables();

        if (queryLibraryModules != null) {
            Iterator iter = queryLibraryModules.values().iterator();
            while (iter.hasNext()) {
                List modules = (List)iter.next();
                Iterator iter2 = modules.iterator();
                while (iter2.hasNext()) {
                    QueryModule env = (QueryModule)iter2.next();
                    env.bindUnboundVariables();
                }
            }
        }

        List varDefinitions = main.fixupGlobalVariables(main.getGlobalStackFrameMap());

        main.bindUnboundFunctionCalls();

        if (queryLibraryModules != null) {
            Iterator iter = queryLibraryModules.values().iterator();
            while (iter.hasNext()) {
                List modules = (List)iter.next();
                Iterator iter2 = modules.iterator();
                while (iter2.hasNext()) {
                    QueryModule env = (QueryModule)iter2.next();
                    env.bindUnboundFunctionCalls();
                }
            }
        }

        // Note: the checks for circularities between variables and functions have to happen
        // before functions are compiled and optimized, as the optimization can involve function
        // inlining which eliminates the circularities (tests K-InternalVariablesWith-17, errata8-002)
        
        main.checkForCircularities(varDefinitions, main.getGlobalFunctionLibrary());
        main.fixupGlobalFunctions();       

        if (checkForCycles) {
            Iterator miter = getQueryLibraryModules();
            while (miter.hasNext()) {
                QueryModule module = (QueryModule)miter.next();
                module.lookForModuleCycles(new Stack(), 1);
            }
        }

        main.typeCheckGlobalVariables(varDefinitions);
        main.optimizeGlobalFunctions();
    }

    /**
     * Set the space requirements for variables used in template match patterns
     *
     * @param patternLocals The largest number of local variables used in the match pattern of any template rule
     */

    public void setPatternSlotSpace(int patternLocals) {
        largestPatternStackFrame = patternLocals;
    }

    /**
     * Get the global variable with a given name
     * @param name the name of the required variable
     * @return the GlobalVariable with this name, or null if not found
     */

    public GlobalVariable getGlobalVariable(StructuredQName name) {
        if (compiledGlobalVariables != null) {
            return compiledGlobalVariables.get(name);
        }
        return null;
    }

    /**
     * Get the global variable map
     *
     * @return the SlotManager defining the allocation of slots to global variables
     */

    public SlotManager getGlobalVariableMap() {
        if (globalVariableMap == null) {
            globalVariableMap = config.makeSlotManager();
        }
        return globalVariableMap;
    }

    /**
     * Get the index of global variables
     *
     * @return the index of global variables. This is a HashMap in which the key is the
     *         {@link net.sf.saxon.om.StructuredQName}
     *         of the variable name, and the value is the GlobalVariable object representing the compiled
     *         global variable. If there are no global variables, the method may return null.
     */

    public HashMap<StructuredQName, GlobalVariable> getCompiledGlobalVariables() {
        return compiledGlobalVariables;
    }

    /**
     * Explain (that is, output an expression tree) the global variables
     * @param presenter the destination for the explanation of the global variables
     */

    public void explainGlobalVariables(ExpressionPresenter presenter) {
        if (compiledGlobalVariables != null) {
            presenter.startElement("globalVariables");
            Iterator<GlobalVariable> iter = compiledGlobalVariables.values().iterator();
            while (iter.hasNext()) {
                GlobalVariable var = iter.next();
                presenter.startElement("declareVariable");
                presenter.emitAttribute("name", var.getVariableQName().getDisplayName());
                if (var.isAssignable()) {
                    presenter.emitAttribute("assignable", "true");
                }
                if (var.getSelectExpression() != null) {
                    var.getSelectExpression().explain(presenter);
                }
                presenter.endElement();
            }
            presenter.endElement();
        }
    }

    /**
     * Register a global variable
     * @param variable the global variable to be registered
     */

    public void registerGlobalVariable(GlobalVariable variable) {
        if (compiledGlobalVariables == null) {
            compiledGlobalVariables = new HashMap<StructuredQName, GlobalVariable>(32);
        }
        compiledGlobalVariables.put(variable.getVariableQName(), variable);
    }

    /**
     * Allocate space in bindery for all the variables needed
     *
     * @param bindery The bindery to be initialized
     */

    public void initializeBindery(Bindery bindery) {
        bindery.allocateGlobals(getGlobalVariableMap());
    }

    /**
     * Determine the size of the stack frame needed for evaluating match patterns
     * @return the size of the largest stack frame needed for evaluating the match patterns
     * that appear in XSLT template rules
     */

    public int getLargestPatternStackFrame() {
        return largestPatternStackFrame;
    }

    /**
     * Set the location map
     * @param map the location map, which is used to identify the module URI and line number of locations of errors
     */

    public void setLocationMap(LocationMap map) {
        locationMap = map;
    }

    /**
     * Get the location map
     * @return the location map, which is used to identify the locations of errors
     */

    public LocationMap getLocationMap() {
        return locationMap;
    }

    /**
     * Add a required parameter. Used in XSLT only.
     * @param qName the name of the required parameter
     */

    public void addRequiredParam(StructuredQName qName) {
        if (requiredParams == null) {
            requiredParams = new HashSet<StructuredQName>(5);
        }
        requiredParams.add(qName);
    }

    /**
     * Check that all required parameters have been supplied. Used in XSLT only.
     * @param params the set of parameters that have been supplied
     * @throws XPathException if there is a required parameter for which no value has been supplied
     */

    public void checkAllRequiredParamsArePresent(GlobalParameterSet params) throws XPathException {
        if (requiredParams == null) {
            return;
        }
        Iterator<StructuredQName> iter = requiredParams.iterator();
        while (iter.hasNext()) {
            StructuredQName req = iter.next();
            if (params == null || params.get(req) == null) {
                XPathException err = new XPathException("No value supplied for required parameter " +
                        req.getDisplayName());
                err.setErrorCode("XTDE0050");
                throw err;
            }
        }
    }


    /**
     * Set whether this executable represents a stylesheet that uses xsl:result-document
     * to create secondary output documents
     * @param flag true if the executable uses xsl:result-document
     */

    public void setCreatesSecondaryResult(boolean flag) {
        createsSecondaryResult = flag;
    }

    /**
     * Ask whether this executable represents a stylesheet that uses xsl:result-document
     * to create secondary output documents
     * @return true if the executable uses xsl:result-document
     */

    public boolean createsSecondaryResult() {
        return createsSecondaryResult;
    }

    /**
     * Set the name of the variable that will implicitly contain the value of the
     * initial context item. The properties of this variable, such as its required
     * type and initial value, automatically apply to the initial context item
     * @param name the name of the global variable that mirrors the initial context item
     */

    public void setInitialContextItemVariableName(StructuredQName name) {
        initialContextItemVariableName = name;
    }

    /**
     * Get the the name of the variable that will implicitly contain the value of the
     * initial context item. The properties of this variable, such as its required
     * type and initial value, automatically apply to the initial context item
     * @return the name of the global variable that mirrors the initial context item
     */

    public StructuredQName getInitialContextItemVariableName() {
        return initialContextItemVariableName;
    }

    /**
     * Set whether this executable is schema-aware. The initial value is false; it is set to true
     * at compile time if the query or transformation imports a schema. If the value is false, then
     * all documents used at run-time must be untyped
     * @param aware true if the executable is schema-aware
     * @throws IllegalArgumentException if schema-aware processing is requested in a Configuration
     * that is not schema-aware
     */

    public void setSchemaAware(boolean aware) {
        if (aware && !config.isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION)) {
            throw new IllegalArgumentException(
                    "Schema-aware processing is not possible: needs Saxon Enterprise Edition");
        }
        schemaAware = aware;
    }

    /**
     * Ask whether this executable is schema-aware, that is, whether the query or transformation
     * imports a schema.
     * @return true if the executable is schema-aware, false if not.
     */

    public boolean isSchemaAware() {
        return schemaAware;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//
