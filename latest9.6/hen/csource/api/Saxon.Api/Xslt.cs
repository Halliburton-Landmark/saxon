﻿using System;
using System.IO;
using System.Xml;
using System.Collections;
using System.Globalization;
using JStreamSource = javax.xml.transform.stream.StreamSource;
using JResult = javax.xml.transform.Result;
using JTransformerException = javax.xml.transform.TransformerException;
using JOutputURIResolver = net.sf.saxon.lib.OutputURIResolver;
using JAugmentedSource = net.sf.saxon.lib.AugmentedSource;
using JConfiguration = net.sf.saxon.Configuration;
using JLocationProvider = net.sf.saxon.@event.LocationProvider;
using JPipelineConfiguration = net.sf.saxon.@event.PipelineConfiguration;
using JStructuredQName = net.sf.saxon.om.StructuredQName;
using JProperties = java.util.Properties;
using JSequenceWriter = net.sf.saxon.@event.SequenceWriter;
using JSequenceOutputter = net.sf.saxon.@event.SequenceOutputter;
using JReceiver = net.sf.saxon.@event.Receiver;
using JReceiverOptions = net.sf.saxon.@event.ReceiverOptions;
using JCompilerInfo = net.sf.saxon.trans.CompilerInfo;
using JExpressionPresenter = net.sf.saxon.trace.ExpressionPresenter;
using JValidation = net.sf.saxon.lib.Validation;
using JDocumentURI = net.sf.saxon.om.DocumentURI;
using JDecimalValue = net.sf.saxon.value.DecimalValue;
using JIndependentContext = net.sf.saxon.sxpath.IndependentContext;
using JCompilation = net.sf.saxon.style.Compilation;
using net.sf.saxon;
using JItem = net.sf.saxon.om.Item;
using JNodeInfo = net.sf.saxon.om.NodeInfo;
using JItemType = net.sf.saxon.type.ItemType;
using JNodeName = net.sf.saxon.om.NodeName;
using JSchemaType = net.sf.saxon.type.SchemaType;
using JDocumentInfo = net.sf.saxon.om.DocumentInfo;
using JPullProvider = net.sf.saxon.pull.PullProvider;
using JPullSource = net.sf.saxon.pull.PullSource;
using JProcInstParser = net.sf.saxon.tree.util.ProcInstParser;
using JXsltCompiler = net.sf.saxon.s9api.XsltCompiler;
using net.sf.saxon.dotnet;
using CharSequence = java.lang.CharSequence;
using JBoolean = java.lang.Boolean;
using GlobalParameterSet = net.sf.saxon.expr.instruct.GlobalParameterSet;
using JXsltTransformer = net.sf.saxon.s9api.XsltTransformer;
using JUserFunction = net.sf.saxon.expr.instruct.UserFunction;
using JXslt30Transformer = net.sf.saxon.s9api.Xslt30Transformer;
using JRoleLocator = net.sf.saxon.expr.parser.RoleLocator;
using JStylesheetPackage = net.sf.saxon.style.StylesheetPackage;
using JXsltPackage = net.sf.saxon.s9api.XsltPackage;
using System.Collections.Generic;
using JMap = java.util.Map;




namespace Saxon.Api
{

	/// <summary>
	/// An <c>XsltCompiler</c> object allows XSLT 2.0 stylesheets to be compiled.
	/// The compiler holds information that represents the static context
	/// for the compilation.
	/// </summary>
	/// <remarks>
	/// <para>To construct an <c>XsltCompiler</c>, use the factory method
	/// <c>NewXsltCompiler</c> on the <c>Processor</c> object.</para>
	/// <para>An <c>XsltCompiler</c> may be used repeatedly to compile multiple
	/// queries. Any changes made to the <c>XsltCompiler</c> (that is, to the
	/// static context) do not affect queries that have already been compiled.
	/// An <c>XsltCompiler</c> may be used concurrently in multiple threads, but
	/// it should not then be modified once initialized.</para>
	/// </remarks>

	[Serializable]
	public class XsltCompiler
	{

		//private TransformerFactoryImpl factory;
		private Processor processor;
		//private JConfiguration config;
		//private JCompilerInfo info;
		//private JIndependentContext env;
		private Uri baseUri;
		private ErrorGatherer errorGatherer;
		private Hashtable variableList = new Hashtable();
		private JXsltCompiler xsltCompiler;

		// internal constructor: the public interface is a factory method
		// on the Processor object

		internal XsltCompiler(Processor processor)
		{
			this.processor = processor;
			xsltCompiler = processor.JProcessor.newXsltCompiler();
			errorGatherer = new ErrorGatherer (new List<StaticError> ());
			xsltCompiler.setErrorListener (errorGatherer);
			xsltCompiler.setURIResolver (processor.Implementation.getURIResolver());
		}

		/// <summary>
		/// The base URI of the stylesheet, which forms part of the static context
		/// of the stylesheet. This is used for resolving any relative URIs appearing
		/// within the stylesheet, for example in <c>xsl:include</c> and <c>xsl:import</c>
		/// declarations, in schema locations defined to <c>xsl:import-schema</c>, 
		/// or as an argument to the <c>document()</c> or <c>doc()</c> function.
		/// </summary>
		/// <remarks>
		/// This base URI is used only if the input supplied to the <c>Compile</c> method
		/// does not provide its own base URI. It is therefore used on the version of the
		/// method that supplies input from a <c>Stream</c>. On the version that supplies
		/// input from an <c>XmlReader</c>, this base URI is used only if the <c>XmlReader</c>
		/// does not have its own base URI.
		/// </remarks>


		public Uri BaseUri
		{
			get { return baseUri; }
			set { baseUri = value; }
		}

		/// <summary>
		/// Create a collation based on a given <c>CompareInfo</c> and <c>CompareOptions</c>    
		/// </summary>
		/// <param name="uri">The collation URI to be used within the XPath expression to refer to this collation</param>
		/// <param name="compareInfo">The <c>CompareInfo</c>, which determines the language-specific
		/// collation rules to be used</param>
		/// <param name="options">Options to be used in performing comparisons, for example
		/// whether they are to be case-blind and/or accent-blind</param>
		/// <param name="isDefault">If true, this collation will be used as the default collation</param>

		public void DeclareCollation(Uri uri, CompareInfo compareInfo, CompareOptions options, Boolean isDefault)
		{
			DotNetComparator comparator = new DotNetComparator(compareInfo, options);
			processor.JProcessor.getUnderlyingConfiguration().registerCollation(uri.ToString(), comparator);



		}

		/// <summary>
		/// Get the Processor from which this XsltCompiler was constructed
		/// </summary>
		public Processor Processor
		{
			get { return processor; }
			set { processor = value; }
		}

		/// <summary>
		/// An <c>XmlResolver</c>, which will be used to resolve URI references while compiling
		/// a stylesheet
		/// </summary>
		/// <remarks>
		/// If no <c>XmlResolver</c> is set for the <c>XsltCompiler</c>, the <c>XmlResolver</c>
		/// is used that was set on the <c>Processor</c> at the time <c>NewXsltCompiler</c>
		/// was called.
		/// </remarks>

		public XmlResolver XmlResolver
		{
			get
			{
				return ((DotNetURIResolver)xsltCompiler.getUnderlyingCompilerInfo().getURIResolver()).getXmlResolver();
			}
			set
			{
				xsltCompiler.getUnderlyingCompilerInfo().setURIResolver(new DotNetURIResolver(value));
			}
		}

		/// <summary>
		/// The <c>SchemaAware</c> property determines whether the stylesheet is schema-aware. By default, a stylesheet
		/// is schema-aware if it contains one or more <code>xsl:import-schema</code> declarations. This option allows
		/// a stylesheet to be marked as schema-aware even if it does not contain such a declaration.
		/// </summary>
		/// <remarks>
		/// <para>If the stylesheet is not schema-aware, then schema-validated input documents will be rejected.</para>
		/// <para>The reason for this option is that it is expensive to generate code that can handle typed input
		/// documents when they will never arise in practice.</para>
		/// <para>The initial setting of this property is false, regardless of whether or not the <c>Processor</c>
		/// is schema-aware. Setting this property to true if the processor is not schema-aware will cause an Exception.</para>
		/// </remarks>

		public bool SchemaAware
		{
			get
			{
				return xsltCompiler.isSchemaAware();
			}
			set
			{
				xsltCompiler.setSchemaAware(value);
			}
		}

		/// <summary>
		/// The <c>XsltLanguageVersion</c> property determines whether the version of the XSLT language specification
		/// implemented by the compiler. The values 2.0 and 3.0 refer to the XSLT 2.0 and XSLT 3.0 (formerly XSLT 2.1) specifications.
		/// The value 0.0 (which is the initial default) indicates that the value is to be taken from the <c>version</c>
		/// attribute of the <c>xsl:stylesheet</c> element.
		/// </summary>
		/// <remarks>
		/// <para>Values that are not numerically equal to one of the above values are rejected.</para>
		/// <para>A warning is output (unless suppressed) when the XSLT language version supported by the processor
		/// is different from the value of the <c>version</c> attribute of the <c>xsl:stylesheet</c> element.</para>
		/// <para>XSLT 3.0 features are supported only in Saxon-PE and Saxon-EE. Setting the value to 3.0 under 
		/// Saxon-HE will cause an error if (and only if) the stylesheet actually uses XSLT 3.0 constructs.</para>
		/// </remarks>

		public string XsltLanguageVersion
		{
			get
			{
				return xsltCompiler.getXsltLanguageVersion ();
			}
			set
			{
				xsltCompiler.setXsltLanguageVersion(value);
			}
		}

		/// <summary>
		/// List of errors. The caller should supply an empty list before calling Compile;
		/// the processor will then populate the list with error information obtained during
		/// the compilation. Each error will be included as an object of type StaticError.
		/// If no error list is supplied by the caller, error information will be written to
		/// the standard error stream.
		/// </summary>
		/// <remarks>
		/// By supplying a custom List with a user-written add() method, it is possible to
		/// intercept error conditions as they occur.
		/// </remarks>

		public IList ErrorList
		{
			set
			{
				errorGatherer = new ErrorGatherer (value);
				xsltCompiler.setErrorListener(errorGatherer);
			}
			get
			{
				return errorGatherer.ErrorList;
			}
		}

		/// <summary>
		/// Compile a stylesheet supplied as a Stream.
		/// </summary>
		/// <example>
		/// <code>
		/// Stream source = new FileStream("input.xsl", FileMode.Open, FileAccess.Read);
		/// XsltExecutable q = compiler.Compile(source);
		/// source.Close();
		/// </code>
		/// </example>
		/// <param name="input">A stream containing the source text of the stylesheet</param>
		/// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
		/// The XsltExecutable may be loaded as many times as required, in the same or a different
		/// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
		/// once it has been compiled.</returns>
		/// <remarks>
		/// <para>If the stylesheet contains any <c>xsl:include</c> or <c>xsl:import</c> declarations,
		/// then the <c>BaseURI</c> property must be set to allow these to be resolved.</para>
		/// <para>The stylesheet is contained in the part of the input stream between its current
		/// position and the end of the stream. It is the caller's responsibility to close the input 
		/// stream after use. If the compilation succeeded, then on exit the stream will be 
		/// exhausted; if compilation failed, the current position of the stream on exit is
		/// undefined.</para>
		/// </remarks>

		public XsltExecutable Compile(Stream input)
		{
			try
			{
				JStreamSource ss = new JStreamSource(new DotNetInputStream(input));
				if (baseUri != null)
				{
					ss.setSystemId(baseUri.ToString());
				}

				PreparedStylesheet pss = JCompilation.compileSingletonPackage(processor.Implementation, xsltCompiler.getUnderlyingCompilerInfo(),ss);
				return new XsltExecutable(processor, pss);
			}
			catch (JTransformerException err)
			{
				throw new StaticError(err);
			}
		}



		/// <summary>Compile a library package.</summary>
		/// <para>The source argument identifies an XML file containing an &lt;xsl:package&gt; element. Any packages
		/// on which this package depends must have been made available to the <code>XsltCompiler</code>
		/// by importing them using {@link #importPackage}.</para></summary>
		/// <param name='input'>source identifies an XML document holding the the XSLT package to be compiled</param>
		/// <returns the XsltPackage that results from the compilation. Note that this package
		///is not automatically imported to this <code>XsltCompiler</code>; if the package is required
		///for use in subsequent compilations then it must be explicitly imported.</returns>
		/// <remarks><para>@since 9.6</para></remarks>

		public XsltPackage CompilePackage(Stream input)
		{
			try
			{
				JStreamSource ss = new JStreamSource(new DotNetInputStream(input));
				if (baseUri != null)
				{
					ss.setSystemId(baseUri.ToString());
				}

				//JCompilation compilation = new JCompilation(processor.Implementation, GetUnderlyingCompilerInfo());

				XsltPackage pack = new XsltPackage(processor, xsltCompiler.compilePackage(ss));

				return pack;
			}
			catch (JTransformerException err)
			{
				throw new StaticError(err);
			}
		}




		/// <summary>Compile a list of packages.</summary>
		/// <param name='input'> sources the collection of packages to be compiled, in the form of an Iterable</param
		/// <returns> the collection of compiled packages, in the form of an Iterable.</returns>
		/// <remarks><para>Since 9.6</para></remarks>

		public IList<XsltPackage> CompilePackages(IList<Stream> sources)
		{
			java.util.List sourcesJList = new java.util.ArrayList();
			foreach (Stream ss in sources) {
				sourcesJList.add(new JStreamSource(new DotNetInputStream(ss)));

			}
			java.lang.Iterable resultJList = null;

			try {

				resultJList = xsltCompiler.compilePackages (sourcesJList);


			} catch(JTransformerException ex){
				throw new StaticError (ex);
			}
			IList<XsltPackage> result = new List<XsltPackage>();
			java.util.Iterator iter = resultJList.iterator ();

			for(;iter.hasNext();){
				JXsltPackage pp = (JXsltPackage)iter.next ();
				result.Add (new XsltPackage(processor, pp));
			}

			return result;
		}



		/// <summary>Import a library package. Calling this method makes the supplied package available for reference
		/// in the <code>xsl:use-package</code> declaration of subsequent compilations performed using this
		/// <code>XsltCompiler</code>.</summary>
		/// <param name='thePackage'> thePackage the package to be imported</param>
		/// <remarks>since 9.6</remarks>

		public void ImportPackage(XsltPackage thePackage) {
			if (thePackage.Processor != this.processor) {
				throw new StaticError(new JTransformerException("The imported package and the XsltCompiler must belong to the same Processor"));
			}
			GetUnderlyingCompilerInfo().getPackageLibrary().addPackage(thePackage.PackageName, thePackage.getUnderlyingPreparedPackage());
		}


		///<summary>  
		///  Get the underlying CompilerInfo object, which provides more detailed (but less stable) control
		///  over some compilation options
		///  </summary>
		/// <returns> the underlying CompilerInfo object, which holds compilation-time options. The methods on
		/// this object are not guaranteed stable from release to release.
		/// </returns>

		public JCompilerInfo GetUnderlyingCompilerInfo()
		{
			return xsltCompiler.getUnderlyingCompilerInfo();
		}


		/// <summary>
		/// Externally set the value of a static parameter (new facility in XSLT 3.0) 
		/// </summary>
		/// <param name="name">The name of the parameter, expressed
		/// as a QName. If a parameter of this name has been declared in the
		/// stylesheet, the given value will be assigned to the variable. If the
		/// variable has not been declared, calling this method has no effect (it is
		/// not an error).</param>
		/// <param name="value">The value to be given to the parameter.
		/// If the parameter declaration defines a required type for the variable, then
		/// this value will be converted in the same way as arguments to function calls
		/// (for example, numeric promotion is applied).</param>
		public void SetParameter(QName name, XdmValue value)
		{
			variableList.Add(name, value);
			xsltCompiler.getUnderlyingCompilerInfo().setParameter(name.ToStructuredQName(), value.Unwrap());
		}

		/// <summary>
		/// Compile a stylesheet supplied as a TextReader.
		/// </summary>
		/// <example>
		/// <code>
		/// String ss = "<![CDATA[<xsl:stylesheet version='2.0'>....</xsl:stylesheet>]]>";
		/// TextReader source = new StringReader(ss);
		/// XsltExecutable q = compiler.Compile(source);
		/// source.Close();
		/// </code>
		/// </example>
		/// <param name="input">A <c>TextReader</c> containing the source text of the stylesheet</param>
		/// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
		/// The XsltExecutable may be loaded as many times as required, in the same or a different
		/// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
		/// once it has been compiled.</returns>
		/// <remarks>
		/// <para>If the stylesheet contains any <c>xsl:include</c> or <c>xsl:import</c> declarations,
		/// then the <c>BaseURI</c> property must be set to allow these to be resolved.</para>
		/// <para>The stylesheet is contained in the part of the input stream between its current
		/// position and the end of the stream. It is the caller's responsibility to close the 
		/// <c>TextReader</c> after use. If the compilation succeeded, then on exit the stream will be 
		/// exhausted; if compilation failed, the current position of the stream on exit is
		/// undefined.</para>
		/// </remarks>

		public XsltExecutable Compile(TextReader input)
		{
			JStreamSource ss = new JStreamSource(new DotNetReader(input));
			if (baseUri != null)
			{
				ss.setSystemId(baseUri.ToString());
			}
			PreparedStylesheet pss = JCompilation.compileSingletonPackage(processor.Implementation ,xsltCompiler.getUnderlyingCompilerInfo(), ss);
			return new XsltExecutable(processor, pss);
		}

		/// <summary>
		/// Compile a stylesheet, retrieving the source using a URI.
		/// </summary>
		/// <remarks>
		/// The document located via the URI is parsed using the <c>System.Xml</c> parser. This
		/// URI is used as the base URI of the stylesheet: the <c>BaseUri</c> property of the
		/// <c>Compiler</c> is ignored.
		/// </remarks>
		/// <param name="uri">The URI identifying the location where the stylesheet document can be
		/// found</param>
		/// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
		/// The XsltExecutable may be run as many times as required, in the same or a different
		/// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
		/// once it has been compiled.</returns>

		public XsltExecutable Compile(Uri uri)
		{
			Object obj = XmlResolver.GetEntity(uri, "application/xml", Type.GetType("System.IO.Stream"));
			if (obj is Stream)
			{
				try
				{
					XmlReaderSettings settings = new XmlReaderSettings();
					settings.DtdProcessing = DtdProcessing.Parse;   // must expand entity references
					settings.XmlResolver = XmlResolver;
					settings.IgnoreWhitespace = false;
					settings.ValidationType = ValidationType.None;
					XmlReader parser = XmlReader.Create((Stream)obj, settings, uri.ToString());
					//XmlReader parser = new XmlTextReader(uri.ToString(), (Stream)obj);
					//((XmlTextReader)parser).Normalization = true;
					//((XmlTextReader)parser).WhitespaceHandling = WhitespaceHandling.All;
					//((XmlTextReader)parser).XmlResolver = XmlResolver;
					// Always need a validating parser, because that's the only way to get entity references expanded
					//parser = new XmlValidatingReader(parser);
					//((XmlValidatingReader)parser).ValidationType = ValidationType.None;
					JPullSource source = new JPullSource(new DotNetPullProvider(parser));
					PreparedStylesheet pss = JCompilation.compileSingletonPackage(processor.Implementation, xsltCompiler.getUnderlyingCompilerInfo(), source);
					return new XsltExecutable(processor, pss);
				}
				finally
				{
					((Stream)obj).Close();
				}
			}
			else
			{
				throw new ArgumentException("Invalid type of result from XmlResolver.GetEntity: " + obj);
			}
		}

		/// <summary>
		/// Compile a stylesheet, delivered using an XmlReader.
		/// </summary>
		/// <remarks>
		/// The <c>XmlReader</c> is responsible for parsing the document; this method builds a tree
		/// representation of the document (in an internal Saxon format) and compiles it.
		/// The <c>XmlReader</c> will be used as supplied; it is the caller's responsibility to
		/// ensure that the settings of the <c>XmlReader</c> are consistent with the requirements
		/// of the XSLT specification (for example, that entity references are expanded and whitespace
		/// is preserved).
		/// </remarks>
		/// <remarks>
		/// If the <c>XmlReader</c> has a <c>BaseUri</c> property, then that property determines
		/// the base URI of the stylesheet module, which is used when resolving any <c>xsl:include</c>
		/// or <c>xsl:import</c> declarations. If the <c>XmlReader</c> has no <c>BaseUri</c>
		/// property, then the <c>BaseUri</c> property of the <c>Compiler</c> is used instead.
		/// An <c>ArgumentNullException</c> is thrown if this property has not been supplied.
		/// </remarks>
		/// <param name="reader">The XmlReader (that is, the XML parser) used to supply the document containing
		/// the principal stylesheet module.</param>
		/// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
		/// The XsltExecutable may be run as many times as required, in the same or a different
		/// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
		/// once it has been compiled.</returns>


		public XsltExecutable Compile(XmlReader reader)
		{
			DotNetPullProvider pp = new DotNetPullProvider(reader);
			JPipelineConfiguration pipe = processor.Implementation.makePipelineConfiguration();
			pipe.setLocationProvider(pp);
			pp.setPipelineConfiguration(pipe);
			// pp = new PullTracer(pp);  /* diagnostics */
			JPullSource source = new JPullSource(pp);
			String baseu = reader.BaseURI;
			if (baseu == null || baseu == String.Empty)
			{
				// if no baseURI is supplied by the XmlReader, use the one supplied to this Compiler
				if (baseUri == null)
				{
					throw new ArgumentNullException("BaseUri");
				}
				baseu = baseUri.ToString();
				pp.setBaseURI(baseu);
			}
			source.setSystemId(baseu);
			PreparedStylesheet pss = JCompilation.compileSingletonPackage(processor.Implementation, xsltCompiler.getUnderlyingCompilerInfo(), source);
			return new XsltExecutable(processor, pss);
		}

		/// <summary>
		/// Compile a stylesheet, located at an XdmNode. This may be a document node whose
		/// child is an <c>xsl:stylesheet</c> or <c>xsl:transform</c> element, or it may be
		/// the <c>xsl:stylesheet</c> or <c>xsl:transform</c> element itself.
		/// </summary>
		/// <param name="node">The document node or the outermost element node of the document
		/// containing the principal stylesheet module.</param>
		/// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
		/// The XsltExecutable may be run as many times as required, in the same or a different
		/// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
		/// once it has been compiled.</returns>

		public XsltExecutable Compile(XdmNode node)
		{
			PreparedStylesheet pss = JCompilation.compileSingletonPackage(processor.Implementation, xsltCompiler.getUnderlyingCompilerInfo(), node.Implementation);
			return new XsltExecutable(processor, pss);
		}

		/// <summary>Locate and compile a stylesheet identified by an &lt;?xml-stylesheet?&gt;
		/// processing instruction within a source document.
		/// </summary>
		/// <param name="source">The document node of the source document containing the
		/// xml-stylesheet processing instruction.</param>
		/// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.</returns>
		/// <remarks>There are some limitations in the current implementation. The media type
		/// is ignored, as are the other parameters of the xml-stylesheet instruction. The
		/// href attribute must either reference an embedded stylesheet within the same
		/// document or a non-embedded external stylesheet.</remarks>

		public XsltExecutable CompileAssociatedStylesheet(XdmNode source)
		{
			// TODO: lift the restrictions
			if (source == null || source.NodeKind != XmlNodeType.Document)
			{
				throw new ArgumentException("Source must be a document node");
			}
			IEnumerator kids = source.EnumerateAxis(XdmAxis.Child);
			QName xmlstyle = new QName("", "xml-stylesheet");
			while (kids.MoveNext())
			{
				XdmNode n = (XdmNode)kids.Current;
				if (n.NodeKind == XmlNodeType.ProcessingInstruction &&
					n.NodeName.Equals(xmlstyle))
				{
					// TODO: check the media type
					String href = JProcInstParser.getPseudoAttribute(n.StringValue, "href");
					if (href == null)
					{
						throw new DynamicError("xml-stylesheet processing instruction has no href attribute");
					}
					String fragment = null;
					int hash = href.LastIndexOf('#');
					if (hash == 0)
					{
						if (href.Length == 1)
						{
							throw new DynamicError("Relative URI of '#' is invalid");
						}
						fragment = href.Substring(1);
						JNodeInfo target = ((JDocumentInfo)source.value).selectID(fragment, true);
						XdmNode targetWrapper = null;
						if (target == null)
						{
							// There's a problem here because the Microsoft XML parser doesn't
							// report id values, so selectID() will never work. We work around that
							// by looking for an attribute named "id" appearing on an xsl:stylesheet
							// or xsl:transform element
							QName qid = new QName("", "id");
							IEnumerator en = source.EnumerateAxis(XdmAxis.Descendant);
							while (en.MoveNext())
							{
								XdmNode x = (XdmNode)en.Current;
								if (x.NodeKind == XmlNodeType.Element &&
									x.NodeName.Uri == "http://www.w3.org/1999/XSL/Transform" &&
									(x.NodeName.LocalName == "stylesheet" || x.NodeName.LocalName == "transform" &&
										x.GetAttributeValue(qid) == fragment))
								{
									targetWrapper = x;
								}
							}
						}
						else
						{
							targetWrapper = (XdmNode)XdmValue.Wrap(target);
						}
						if (targetWrapper == null)
						{
							throw new DynamicError("No element with id='" + fragment + "' found");
						}
						return Compile(targetWrapper);
					}
					else if (hash > 0)
					{
						throw new NotImplementedException("href cannot identify an embedded stylesheet in a different document");
					}
					else
					{
						Uri uri = new Uri(n.BaseUri, href);
						return Compile(uri);
					}
				}
			}
			throw new DynamicError("xml-stylesheet processing instruction not found");
		}
	}

	/// <summary>
	/// An <c>XsltExecutable</c> represents the compiled form of a stylesheet. To execute the stylesheet,
	/// it must first be loaded to form an <c>XsltTransformer</c>.
	/// </summary>
	/// <remarks>
	/// <para>An <c>XsltExecutable</c> is immutable, and therefore thread-safe. It is simplest to
	/// load a new <c>XsltEvaluator</c> each time the stylesheet is to be run. However, the 
	/// <c>XsltEvaluator</c> is serially reusable within a single thread.</para>
	/// <para>An <c>XsltExecutable</c> is created by using one of the <c>Compile</c>
	/// methods on the <c>XsltCompiler</c> class.</para>
	/// </remarks>    

	[Serializable]
	public class XsltExecutable
	{

		private PreparedStylesheet pss;
		private Processor processor;

		// internal constructor

		internal XsltExecutable(Processor proc, PreparedStylesheet pss)
		{
			this.processor = proc;
			this.pss = pss;
		}

		/// <summary>
		/// Load the stylesheet to prepare it for execution.
		/// </summary>
		/// <returns>
		/// An <c>XsltTransformer</c>. The returned <c>XsltTransformer</c> can be used to
		/// set up the dynamic context for stylesheet evaluation, and to run the stylesheet.
		/// </returns>

		public XsltTransformer Load()
		{

			Controller c = pss.newController ();
			return new XsltTransformer(c, pss.getCompileTimeParams());
		}



		/// <summary>
		/// Load the stylesheet to prepare it for execution. This version of the load() method
		/// creates an <code>Xslt30Transformer</code> which offers interfaces for stylesheet
		/// invocation corresponding to those described in the XSLT 3.0 specification. It can be used
		/// with XSLT 2.0 or XSLT 3.0 stylesheets, and in both cases it offers new XSLT 3.0 functionality such
		/// as the ability to supply parameters to the initial template, or the ability to invoke
		/// stylesheet-defined functions, or the ability to return an arbitrary sequence as a result
		/// without wrapping it in a document node.
		/// </summary>
		/// <returns>
		/// An <c>Xslt30Transformer</c>. The returned <c>Xslt30Transformer</c> can be used to
		/// set up the dynamic context for stylesheet evaluation, and to run the stylesheet.
		/// </returns>

		public Xslt30Transformer Load30()
		{
			Controller c = pss.newController ();
			return new Xslt30Transformer(processor, c, pss.getCompileTimeParams());
		}

		/// <summary>
		/// Output an XML representation of the compiled code of the stylesheet, for purposes of 
		/// diagnostics and instrumentation
		/// </summary>
		/// <param name="destination">The destination for the diagnostic output</param>

		public void Explain(XmlDestination destination)
		{
			JConfiguration config = pss.getConfiguration();
			JResult result = destination.GetReceiver(config.makePipelineConfiguration());
			JProperties properties = new JProperties();
			properties.setProperty("indent", "yes");
			properties.setProperty("{http://saxon.sf.net/}indent-spaces", "2");
			JReceiver receiver = config.getSerializerFactory().getReceiver(
				result, config.makePipelineConfiguration(), properties);
			JExpressionPresenter presenter = new JExpressionPresenter(config, receiver);
			pss.explain(presenter);
		}

		/// <summary>
		/// Escape hatch to the underlying Java implementation object
		/// </summary>

		public PreparedStylesheet Implementation
		{
			get
			{
				return pss;
			}
		}


		/// <summary>
		/// Get the names of the xsl:param elements defined in this stylesheet, with details
		/// of each parameter including its required type, and whether it is required or optional
		/// </summary>
		/// <returns>
		/// a Dictionary whose keys are the names of global parameters in the stylesheet,
		/// and whose values are {@link ParameterDetails} objects giving information about the
		/// corresponding parameter.
		/// </returns>
		public Dictionary<QName, ParameterDetails> GetGlobalParameters()
		{
			java.util.List globals = pss.getCompiledGlobalVariables();
			Dictionary<QName, ParameterDetails> params1 = new Dictionary<QName, ParameterDetails>();
			java.util.Iterator iter = globals.iterator();
			while (iter.hasNext())
			{
				net.sf.saxon.expr.instruct.GlobalVariable var = (net.sf.saxon.expr.instruct.GlobalVariable) iter.next ();
				if(var is net.sf.saxon.expr.instruct.GlobalParam){
					ParameterDetails details = new ParameterDetails(XdmSequenceType.FromSequenceType(var.getRequiredType()), var.isRequiredParam());
					params1.Add(QName.FromClarkName(var.getVariableQName().getClarkName()), details);
				}

			}

			return params1;
		}




		public class ParameterDetails
		{

			private XdmSequenceType type;
			private bool isRequired;

			public ParameterDetails(XdmSequenceType type1, bool isRequired1)
			{
				this.type = type1;
				this.isRequired = isRequired1;
			}

			/**
             * Get the declared item type of the parameter
             *
             * @return the type defined in the <code>as</code> attribute of the <code>xsl:param</code> element,
             *         without its occurrence indicator
             */

			public XdmItemType getDeclaredItemType()
			{
				return type.itemType;
			}

			/**
             * Get the declared cardinality of the parameter
             *
             * @return the occurrence indicator from the type appearing in the <code>as</code> attribute
             *         of the <code>xsl:param</code> element
             */

			public int getDeclaredCardinality()
			{
				return type.occurrences;
			}

			/**
             *
             */

			public XdmSequenceType getUnderlyingDeclaredType()
			{
				return type;
			}

			/**
             * Ask whether the parameter is required (mandatory) or optional
             *
             * @return true if the parameter is mandatory (<code>required="yes"</code>), false
             *         if it is optional
             */

			public bool IsRequired
			{
				set { this.isRequired = value; }
				get { return this.isRequired; }

			}
		}




	}


	/// <summary inherits="IEnumerable">
	/// An <c>XsltTransformer</c> represents a compiled and loaded stylesheet ready for execution.
	/// The <c>XsltTransformer</c> holds details of the dynamic evaluation context for the stylesheet.
	/// </summary>
	/// <remarks>
	/// <para>An <c>XsltTransformer</c> should not be used concurrently in multiple threads. It is safe,
	/// however, to reuse the object within a single thread to run the same stylesheet several times.
	/// Running the stylesheet does not change the context that has been established.</para>
	/// <para>An <c>XsltTransformer</c> is always constructed by running the <c>Load</c> method of
	/// an <c>XsltExecutable</c>.</para>
	/// </remarks>     

	[Serializable]
	public class XsltTransformer : XdmDestination
	{

		private Controller controller;
		private JNodeInfo initialContextNode;
		private IResultDocumentHandler resultDocumentHandler;
		private IMessageListener messageListener;
		private JStreamSource streamSource;
		private StandardLogger traceFunctionDestination;
		private XmlDestination destination;		
		private GlobalParameterSet staticParameter;
		private bool baseOutputUriWasSet = false;


		// internal constructor

		internal XsltTransformer(Controller controller, GlobalParameterSet staticParameter)
		{
			this.controller = controller;
			this.staticParameter = new GlobalParameterSet(staticParameter);
		}

		/// <summary>
		/// The initial context item for the stylesheet.
		/// </summary>
		/// <remarks><para>This may be either a node or an atomic
		/// value. Most commonly it will be a document node, which might be constructed
		/// using the <c>Build</c> method of the <c>DocumentBuilder</c> object.</para>
		/// <para>Note that this can be inefficient if the stylesheet uses <c>xsl:strip-space</c>
		/// to strip whitespace, or <c>input-type-annotations="strip"</c> to remove type
		/// annotations, since this will result in the transformation operating on a virtual document
		/// implemented as a view or wrapper of the supplied document.</para>
		/// </remarks>

		public XdmNode InitialContextNode
		{
			get { return (initialContextNode == null ? null : (XdmNode)XdmValue.Wrap(initialContextNode)); }
			set { initialContextNode = (value == null ? null : (JNodeInfo)value.Unwrap()); }
		}

		/// <summary>
		/// Supply the principal input document for the transformation in the form of a stream.
		/// </summary>
		/// <remarks>
		/// <para>If this method is used, the <c>InitialContextNode</c> is ignored.</para>
		/// <para>The supplied stream will be consumed by the <c>Run()</c> method.
		/// Closing the input stream after use is the client's responsibility.</para>
		/// <para>A base URI must be supplied in all cases. It is used to resolve relative
		/// URI references appearing within the input document.</para>
		/// <para>Schema validation is applied to the input document according to the value of
		/// the <c>SchemaValidationMode</c> property.</para>
		/// <para>Whitespace stripping is applied according to the value of the
		/// <c>xsl:strip-space</c> and <c>xsl:preserve-space</c> declarations in the stylesheet.</para>
		/// </remarks>
		/// <param name="input">
		/// The stream containing the source code of the principal input document to the transformation. The document
		/// node at the root of this document will be the initial context node for the transformation.
		/// </param>
		/// <param name="baseUri">
		/// The base URI of the principal input document. This is used for example by the <c>document()</c>
		/// function if the document contains links to other documents in the form of relative URIs.</param>

		public void SetInputStream(Stream input, Uri baseUri)
		{
			streamSource = new JStreamSource(new DotNetInputStream(input), baseUri.ToString());
		}

		/// <summary>
		/// The initial mode for the stylesheet. This is either a QName, for a 
		/// named mode, or null, for the unnamed (default) mode.
		/// </summary>

		public QName InitialMode
		{
			get
			{
				JStructuredQName mode = controller.getInitialMode().getModeName();
				if (mode == null)
				{
					return null;
				}
				return QName.FromClarkName(mode.getClarkName());
			}
			set
			{
				controller.setInitialMode(value.ToStructuredQName());
			}
		}

		/// <summary>
		/// The initial template for the stylesheet. This is either a QName, for a 
		/// named template, or null, if no initial template has been set.
		/// </summary>
		/// <exception cref="DynamicError">Setting this property to the name of a template
		/// that does not exist in the stylesheet throws a DynamicError with error 
		/// code XTDE0040. Setting it to the name of a template that has template
		/// parameters throws a DynamicError with error code XTDE0060.</exception>

		public QName InitialTemplate
		{
			get
			{
				String name = controller.getInitialTemplate().getClarkName();
				if (name == null)
				{
					return null;
				}
				return QName.FromClarkName(name);
			}
			set
			{
				try
				{
					controller.setInitialTemplate(value.ToStructuredQName());
				}
				catch (javax.xml.transform.TransformerException err)
				{
					throw new DynamicError(err);
				}
			}
		}



		/// <summary>
		/// The base output URI, which acts as the base URI for resolving the <c>href</c>
		/// attribute of <c>xsl:result-document</c>.
		/// </summary>

		public Uri BaseOutputUri
		{
			get
			{
				return new Uri(controller.getBaseOutputURI());
			}
			set
			{
				baseOutputUriWasSet = true;
				controller.setBaseOutputURI(value.ToString());
			}
		}

		public RecoveryPolicy RecoveryPolicy
		{
			get
			{
				switch (controller.getRecoveryPolicy())
				{
				case Configuration.RECOVER_SILENTLY:
					return RecoveryPolicy.RecoverSilently;
				case Configuration.RECOVER_WITH_WARNINGS:
					return RecoveryPolicy.RecoverWithWarnings;
				default: return RecoveryPolicy.DoNotRecover;
				}
			}
			set
			{
				controller.setRecoveryPolicy(
					value == RecoveryPolicy.RecoverSilently ? Configuration.RECOVER_SILENTLY :
					value == RecoveryPolicy.RecoverWithWarnings ? Configuration.RECOVER_WITH_WARNINGS :
					Configuration.DO_NOT_RECOVER);
			}
		}

		/// <summary>
		/// The <c>SchemaValidationMode</c> to be used in this transformation, especially for documents
		/// loaded using the <c>doc()</c>, <c>document()</c>, or <c>collection()</c> functions.
		/// </summary>
		/// 

		public SchemaValidationMode SchemaValidationMode
		{
			get
			{
				switch (controller.getSchemaValidationMode())
				{
				case JValidation.STRICT:
					return SchemaValidationMode.Strict;
				case JValidation.LAX:
					return SchemaValidationMode.Lax;
				case JValidation.STRIP:
					return SchemaValidationMode.None;
				case JValidation.PRESERVE:
					return SchemaValidationMode.Preserve;
				case JValidation.DEFAULT:
				default:
					return SchemaValidationMode.Unspecified;
				}
			}

			set
			{
				switch (value)
				{
				case SchemaValidationMode.Strict:
					controller.setSchemaValidationMode(JValidation.STRICT);
					break;
				case SchemaValidationMode.Lax:
					controller.setSchemaValidationMode(JValidation.LAX);
					break;
				case SchemaValidationMode.None:
					controller.setSchemaValidationMode(JValidation.STRIP);
					break;
				case SchemaValidationMode.Preserve:
					controller.setSchemaValidationMode(JValidation.PRESERVE);
					break;
				case SchemaValidationMode.Unspecified:
				default:
					controller.setSchemaValidationMode(JValidation.DEFAULT);
					break;
				}
			}
		}





		/// <summary>
		/// The <c>XmlResolver</c> to be used at run-time to resolve and dereference URIs
		/// supplied to the <c>doc()</c> and <c>document()</c> functions.
		/// </summary>

		public XmlResolver InputXmlResolver
		{
			get
			{
				return ((DotNetURIResolver)controller.getURIResolver()).getXmlResolver();
			}
			set
			{
				controller.setURIResolver(new DotNetURIResolver(value));
			}
		}

		/// <summary>
		/// The <c>IResultDocumentHandler</c> to be used at run-time to process the output
		/// produced by any <c>xsl:result-document</c> instruction with an <c>href</c>
		/// attribute.
		/// </summary>
		/// <remarks>
		/// In the absence of a user-supplied result document handler, the <c>href</c>
		/// attribute of the <c>xsl:result-document</c> instruction must be a valid relative
		/// URI, which is resolved against the value of the <c>BaseOutputUri</c> property,
		/// and the resulting absolute URI must identify a writable resource (typically
		/// a file in filestore, using the <c>file:</c> URI scheme).
		/// </remarks>

		public IResultDocumentHandler ResultDocumentHandler
		{
			get
			{
				return resultDocumentHandler;
			}
			set
			{
				resultDocumentHandler = value;
				controller.setOutputURIResolver(new ResultDocumentHandlerWrapper(value, controller.makePipelineConfiguration()));
			}
		}

		/// <summary>
		/// Listener for messages output using &lt;xsl:message&gt;. 
		/// <para>The caller may supply a message listener before calling <c>Run</c>;
		/// the processor will then invoke the listener once for each message generated during
		/// the transformation. Each message will be output as an object of type <c>XdmNode</c>
		/// representing a document node.</para>
		/// <para>If no message listener is supplied by the caller, message information will be written to
		/// the standard error stream.</para>
		/// </summary>
		/// <remarks>
		/// <para>Each message is presented as an XML document node. Calling <c>ToString()</c>
		/// on the message object will usually generate an acceptable representation of the
		/// message.</para>
		/// <para>When the &lt;xsl:message&gt; instruction specifies <c>terminate="yes"</c>,
		/// the message is first notified using this interface, and then an exception is thrown
		/// which terminates the transformation.</para>
		/// </remarks>

		public IMessageListener MessageListener
		{
			set
			{
				messageListener = value;
				JPipelineConfiguration pipe = controller.makePipelineConfiguration();
				controller.setMessageEmitter(new MessageListenerProxy(pipe, value));
			}
			get
			{
				return messageListener;
			}
		}

		/// <summary>
		/// Destination for output of messages using &lt;trace()&gt;. 
		/// <para>If no message listener is supplied by the caller, message information will be written to
		/// the standard error stream.</para>
		/// </summary>
		/// <remarks>
		/// <para>The supplied destination is ignored if a <c>TraceListener</c> is in use.</para>
		/// <para>Since 9.6. Changed in 9.6 to use a StandardLogger</para>
		/// </remarks>

		public StandardLogger TraceFunctionDestination
		{
			set
			{
				traceFunctionDestination = value;
				controller.setTraceFunctionDestination(value);
			}
			get
			{
				return traceFunctionDestination;
			}
		}



		/// <summary>
		/// Set the value of a stylesheet parameter.
		/// </summary>
		/// <param name="name">The name of the parameter, expressed
		/// as a QName. If a parameter of this name has been declared in the
		/// stylesheet, the given value will be assigned to the variable. If the
		/// variable has not been declared, calling this method has no effect (it is
		/// not an error).</param>
		/// <param name="value">The value to be given to the parameter.
		/// If the parameter declaration defines a required type for the variable, then
		/// this value will be converted in the same way as arguments to function calls
		/// (for example, numeric promotion is applied).</param>

		public void SetParameter(QName name, XdmValue value)
		{
			staticParameter.put (name.ToStructuredQName(), value.Unwrap());
		}



		/// <summary>
		/// The destination for the results of the stylesheet. The class XmlDestination is an abstraction 
		/// that allows a number of different kinds of destination to be specified.
		/// </summary>
		/// <para>Set the destination to be used for the result of the transformation.</para>
		/// <remarks>
		/// <para>The Destination can be used to chain transformations into a pipeline, by using one
		/// <c>XsltTransformer</c> as the destination of another</para>
		/// </remarks>
		public XmlDestination Destination
		{
			get
			{
				return this.destination;
			}
			set
			{
				this.destination = value;
			}

		}


		/// <summary>
		/// Close the Destination, releasing any resources that need to be released.
		/// </summary>
		/// <remarks>
		/// This method is called by the system on completion of a query or transformation.
		/// Some kinds of Destination may need to close an output stream, others might
		/// not need to do anything. The default implementation does nothing.
		/// </remarks>

		public override void Close()
		{
			XdmNode doc = XdmNode;
			if (doc == null) {
				throw new StaticError("No source document has been built by the previous pipeline stage");
			}
			Reset ();
			JReceiver result = GetReceiver (controller.makePipelineConfiguration());
			try{
				controller.transform(doc, result);
			}catch (javax.xml.transform.TransformerException err)
			{
				throw new DynamicError(err);
			}
			destination.Close();

		}

		/// <summary>
		/// Run the transformation, sending the result to a specified destination.
		/// </summary>
		/// <param name="destination">
		/// The destination for the results of the stylesheet. The class <c>XmlDestination</c>
		/// is an abstraction that allows a number of different kinds of destination
		/// to be specified.
		/// </param>
		/// <exception cref="DynamicError">Throws a DynamicError if the transformation
		/// fails.</exception>

		public void Run(XmlDestination destination)
		{
			if (destination == null) {
				throw new DynamicError ("Destination is null");
			} else {
				this.destination = destination;
			}
			try
			{

				if(staticParameter != null) {
					controller.initializeController(staticParameter);
				}
				if(baseOutputUriWasSet && destination is XdmDestination && 
					((XdmDestination)destination).BaseUri == null && 
					controller.getBaseOutputURI() != null) {
					((XdmDestination)destination).BaseUri = BaseOutputUri;
				}

				if (streamSource != null)
				{
					controller.transform(streamSource, destination.GetReceiver(controller.makePipelineConfiguration()));
				}

				else if (initialContextNode != null)
				{
					JDocumentInfo doc = initialContextNode.getDocumentRoot();
					if (doc != null)
					{
						controller.registerDocument(doc, (doc.getBaseURI() == null ? null : new JDocumentURI(doc.getBaseURI())));
					}
					controller.transform(initialContextNode, destination.GetReceiver(controller.makePipelineConfiguration()));
				}
				else
				{
					controller.transform(null, destination.GetReceiver(controller.makePipelineConfiguration()));
				}

				destination.Close();
			}
			catch (javax.xml.transform.TransformerException err)
			{
				throw new DynamicError(err);
			}
		}

		/// <summary>
		/// Escape hatch to the underlying Java implementation
		/// </summary>

		public Controller Implementation
		{
			get { return controller; }
		}


	}

	/// <summary>
	/// RecoveryPolicy is an enumeration of the different actions that can be taken when a "recoverable error" occurs
	/// </summary>

	public enum RecoveryPolicy
	{
		/// <summary>
		/// Ignore the error, take the recovery action, do not produce any message
		/// </summary>
		RecoverSilently,

		/// <summary>
		/// Take the recovery action after outputting a warning message
		/// </summary>
		RecoverWithWarnings,

		/// <summary>
		/// Treat the error as fatal
		/// </summary>
		DoNotRecover

	}



	///<summary>An <c>IResultDocumentHandler</c> can be nominated to handle output
	/// produced by the <c>xsl:result-document</c> instruction in an XSLT stylesheet.
	///</summary>
	///<remarks>
	///<para>This interface affects any <c>xsl:result-document</c> instruction
	/// executed by the stylesheet, provided that it has an <c>href</c> attribute.</para> 
	///<para>If no <c>IResultDocumentHandler</c> is nominated (in the
	/// <c>IResultDocumentHandler</c> property of the <c>XsltTransformer</c>), the output
	/// of <code>xsl:result-document</code> is serialized, and is written to the file
	/// or other resource identified by the URI in the <c>href</c> attribute, resolved
	/// (if it is relative) against the URI supplied in the <c>BaseOutputUri</c> property
	/// of the <c>XsltTransformer</c>.</para>
	///<para>If an <c>IResultDocumentHandler</c> is nominated, however, its
	/// <c>HandleResultDocument</c> method will be called whenever an <c>xsl:result-document</c>
	/// instruction with an <c>href</c> attribute is evaluated, and the generated result tree
	/// will be passed to the <c>XmlDestination</c> returned by that method.</para> 
	///</remarks>

	public interface IResultDocumentHandler
	{

		/// <summary> Handle output produced by the <c>xsl:result-document</c>
		/// instruction in an XSLT stylesheet. This method is called by the XSLT processor
		/// when an <c>xsl:result-document</c> with an <c>href</c> attribute is evaluated.
		/// </summary>
		/// <param name="href">An absolute or relative URI. This will be the effective value of the 
		/// <c>href</c> attribute of the <c>xsl:result-document</c> in the stylesheet.</param>
		/// <param name="baseUri">The base URI that should be used for resolving the value of
		/// <c>href</c> if it is relative. This will always be the value of the <c>BaseOutputUri</c>
		/// property of the <c>XsltTransformer</c>.</param>
		/// <returns>An <c>XmlDestination</c> to handle the result tree produced by the
		/// <c>xsl:result-document</c> instruction. The <c>Close</c> method of the returned
		/// <c>XmlDestination</c> will be called when the output is complete.</returns>
		/// <remarks>
		/// <para>The XSLT processor will ensure that the stylesheet cannot create
		/// two distinct result documents which are sent to the same URI. It is the responsibility
		/// of the <c>IResultDocumentHandler</c> to ensure that two distinct result documents are
		/// not sent to the same <c>XmlDestination</c>. Failure to observe this rule can result
		/// in output streams being incorrectly closed.
		/// </para>
		/// <para>Note that more than one result document can be open at the same time,
		/// and that the order of opening, writing, and closing result documents chosen
		/// by the processor does not necessarily bear any direct resemblance to the way
		/// that the XSLT source code is written.</para></remarks>

		XmlDestination HandleResultDocument(string href, Uri baseUri);

	}

	///<summary>Internal wrapper class for <c>IResultDocumentHandler</c></summary>
	internal class ResultDocumentHandlerWrapper : JOutputURIResolver
	{

		private IResultDocumentHandler handler;
		private ArrayList resultList = new ArrayList();
		private ArrayList destinationList = new ArrayList();
		private JPipelineConfiguration pipe;

		/// <summary>
		/// Initializes a new instance of the <see cref="Saxon.Api.ResultDocumentHandlerWrapper"/> class.
		/// </summary>
		/// <param name="handler">Handler.</param>
		/// <param name="pipe">Pipe.</param>
		public ResultDocumentHandlerWrapper(IResultDocumentHandler handler, JPipelineConfiguration pipe)
		{
			this.handler = handler;
			this.pipe = pipe;
		}

		/// <summary>
		/// Create new instance
		/// </summary>
		/// <returns>The <c>JOutURIResolver</c> instance.</returns>
		public JOutputURIResolver newInstance()
		{
			return new ResultDocumentHandlerWrapper(handler, pipe);
		}

		/// <summary>
		/// Resolve the specified href and baseString.
		/// </summary>
		/// <param name="href">Href.</param>
		/// <param name="baseString">Base string.</param>
		public JResult resolve(String href, String baseString)
		{
			Uri baseUri;
			try
			{
				baseUri = new Uri(baseString);
			}
			catch (System.UriFormatException err)
			{
				throw new JTransformerException("Invalid base output URI " + baseString, err);
			}
			XmlDestination destination = handler.HandleResultDocument(href, baseUri);
			JResult result = destination.GetReceiver(pipe);
			resultList.Add(result);
			destinationList.Add(destination);
			return result;
		}


		/// <summary>
		/// Close the specified result.
		/// </summary>
		/// <param name="result">Result.</param>
		public void close(JResult result)
		{
			for (int i = 0; i < resultList.Count; i++)
			{
				if (Object.ReferenceEquals(resultList[i], result))
				{
					((XmlDestination)destinationList[i]).Close();
					resultList.RemoveAt(i);
					destinationList.RemoveAt(i);
					return;
				}
			}
		}
	}

	///<summary>An <c>IMessageListener</c> can be nominated to handle output
	/// produced by the <c>xsl:message</c> instruction in an XSLT stylesheet.
	///</summary>
	///<remarks>
	///<para>This interface affects any <c>xsl:message</c> instruction
	/// executed by the stylesheet.</para> 
	///<para>If no <c>IMessageListener</c> is nominated (in the
	/// <c>MessageListener</c> property of the <c>XsltTransformer</c>), the output
	/// of <code>xsl:message</code> is serialized, and is written to standard error
	/// output stream.</para>
	///<para>If an <c>IMessageListener</c> is nominated, however, its
	/// <c>Message</c> method will be called whenever an <c>xsl:message</c>
	/// instruction is evaluated.</para> 
	///</remarks>


	public interface IMessageListener
	{

		///<summary>Handle the output of an <c>xsl:message</c> instruction
		///in the stylesheet
		///</summary>
		///<param name="content"></param>
		/// <param name="terminate"></param>
		/// <param name="location"></param>

		void Message(XdmNode content, bool terminate, IXmlLocation location);

	}

	/// <summary>
	/// An <c>IXmlLocation</c> represents the location of a node within an XML document.
	/// It is in two parts: the base URI (or system ID) of the external entity (which will usually
	/// be the XML document entity itself), and the line number of a node relative
	/// to the base URI of the containing external entity.
	/// </summary>
	/// 

	public interface IXmlLocation
	{

		/// <summary>
		/// The base URI (system ID) of an external entity within an XML document.
		/// Set to null if the base URI is not known (for example, for an XML document
		/// created programmatically where no base URI has been set up).
		/// </summary>

		Uri BaseUri { get; set; }

		/// <summary>
		/// The line number of a node relative to the start of the external entity.
		/// The value -1 indicates that the line number is not known or not applicable.
		/// </summary>

		int LineNumber { get; set; }
	}


	/// <summary>
	/// Xml location. An implementation of IXmlLocation
	/// </summary>
	internal class XmlLocation : IXmlLocation
	{
		private Uri baseUri;
		private int lineNumber;
		public Uri BaseUri
		{
			get { return baseUri; }
			set { baseUri = value; }
		}
		public int LineNumber
		{
			get { return lineNumber; }
			set { lineNumber = value; }
		}
	}


	/// <summary>
	/// Message listener proxy. This class implements a Receiver that can receive xsl:message output and send it to a
	/// user-supplied MessageListener
	/// </summary>
	[Serializable]
	internal class MessageListenerProxy : JSequenceWriter
	{

		public IMessageListener listener;
		public bool terminate;
		public int locationId;

		/// <summary>
		/// Initializes a new instance of the <see cref="Saxon.Api.MessageListenerProxy"/> class.
		/// </summary>
		/// <param name="pipe">pipe.</param>
		/// <param name="ml">ml.</param>
		public MessageListenerProxy(JPipelineConfiguration pipe, IMessageListener ml)
			: base(pipe)
		{
			listener = ml;
			base.setTreeModel(net.sf.saxon.om.TreeModel.LINKED_TREE);
		}


		/// <summary>
		/// Starts the document.
		/// </summary>
		/// <param name="properties">Properties.</param>
		public override void startDocument(int properties)
		{
			terminate = (properties & JReceiverOptions.TERMINATE) != 0;
			locationId = -1;
			base.startDocument(properties);
		}


		/// <summary>
		/// Starts the element.
		/// </summary>
		/// <param name="nameCode">Name code.</param>
		/// <param name="typeCode">Type code.</param>
		/// <param name="locationId">Location identifier.</param>
		/// <param name="properties">Properties.</param>
		public override void startElement(JNodeName nameCode, JSchemaType typeCode, int locationId, int properties)
		{
			if (this.locationId == -1)
			{
				this.locationId = locationId;
			}
			base.startElement(nameCode, typeCode, locationId, properties);
		}

		/// <summary>
		/// Characters the specified s, locationId and properties.
		/// </summary>
		/// <param name="s">S.</param>
		/// <param name="locationId">Location identifier.</param>
		/// <param name="properties">Properties.</param>
		public override void characters(CharSequence s, int locationId, int properties)
		{
			if (this.locationId == -1)
			{
				this.locationId = locationId;
			}
			base.characters(s, locationId, properties);
		}


		/// <summary>
		/// Append the specified item, locationId and copyNamespaces.
		/// </summary>
		/// <param name="item">Item.</param>
		/// <param name="locationId">Location identifier.</param>
		/// <param name="copyNamespaces">Copy namespaces.</param>
		public override void append(JItem item, int locationId, int copyNamespaces)
		{
			if (this.locationId == -1)
			{
				this.locationId = locationId;
			}
			base.append(item, locationId, copyNamespaces);
		}


		/// <summary>
		/// Write the specified item.
		/// </summary>
		/// <param name="item">Item.</param>
		public override void write(JItem item)
		{
			XmlLocation loc = new XmlLocation();
			if (locationId != -1)
			{
				JLocationProvider provider = getPipelineConfiguration().getLocationProvider();
				loc.BaseUri = new Uri(provider.getSystemId(locationId));
				loc.LineNumber = provider.getLineNumber(locationId);
			}
			listener.Message((XdmNode)XdmItem.Wrap(item), terminate, loc);
		}
	}



	/// <summary>An <code>Xslt30Transformer</code> represents a compiled and loaded stylesheet ready for execution.
	/// The <code>Xslt30Transformer</code> holds details of the dynamic evaluation context for the stylesheet.</summary>

	///<remarks><para>The <code>Xslt30Transformer</code> differs from {@link XsltTransformer} is supporting new options
	/// for invoking a stylesheet, corresponding to facilities defined in the XSLT 3.0 specification. However,
	/// it is not confined to use with XSLT 3.0, and most of the new invocation facilities (for example,
	/// calling a stylesheet-defined function directly) work equally well with XSLT 2.0 and in some cases
	/// XSLT 1.0 stylesheets.</para>
	/// <para>An <code>Xslt30Transformer</code> must not be used concurrently in multiple threads.
	/// It is safe, however, to reuse the object within a single thread to run the same
	/// stylesheet several times. Running the stylesheet does not change the context
	/// that has been established.</para>

	/// <para>An <code>Xslt30Transformer</code> is always constructed by running the <code>Load30</code>
	/// method of an {@link XsltExecutable}.</para>

	/// <para>Unlike <code>XsltTransformer</code>, an <code>Xslt30Transformer</code> is not a <code>Destination</code>. T
	/// To pipe the results of one transformation into another, the target should be an <code>XsltTransfomer</code>
	/// rather than an <code>Xslt30Transformer</code>.</para>

	/// <para>Evaluation of an Xslt30Transformer proceeds in a number of phases:</para>
	///<list type="number">
	/// <item><term>First<term><description> values may be supplied for stylesheet parameters and for the global context item. The
	/// global context item is used when initializing global variables. Unlike earlier transformation APIs,
	/// the global context item is quite independent of the "principal Source document".
	/// </item>
	/// <item><term>stylesheet<term>/ may now be repeatedly invoked. Each invocation takes one of three forms:
	/// <list type="bullet">
	/// <item>Invocation by applying templates. In this case, the information required is (i) an initial
	/// mode (which defaults to the unnamed mode), (ii) an initial match sequence, which is any
	/// XDM value, which is used as the effective "select" expression of the implicit apply-templates
	/// call, and (iii) optionally, values for the tunnel and non-tunnel parameters defined on the
	/// templates that get invoked (equivalent to using <code>xsl:with-param</code> on the implicit
	/// <code>apply-templates</code> call).</item>
	/// <item>Invocation by calling a named template. In this case, the information required is
	/// (i) the name of the initial template (which defaults to "xsl:initial-template"), and
	/// (ii) optionally, values for the tunnel and non-tunnel parameters defined on the
	/// templates that get invoked (equivalent to using <code>xsl:with-param</code> on the implicit
	/// <code>call-template</code> instruction).</item>
	/// <item>Invocation by calling a named function. In this case, the information required is
	/// the sequence of arguments to the function call.</item>
	/// </list>
	/// </item>
	/// <item><description>Whichever invocation method is chosen, the result may either be returned directly, as an arbitrary
	/// XDM value, or it may effectively be wrapped in an XML document. If it is wrapped in an XML document,
	/// that document can be processed in a number of ways, for example it can be materialized as a tree in
	/// memory, it can be serialized as XML or HTML, or it can be subjected to further transformation.</description></item>
	/// </list>
	/// <p>Once the stylesheet has been invoked (using any of these methods), the values of the global context
	/// item and stylesheet parameters cannot be changed. If it is necessary to run another transformation with
	/// a different context item or different stylesheet parameters, a new <c>Xslt30Transformer</c>
	/// should be created from the original <c>XsltExecutable</c>.</p></remarks>
	/// <para> @since 9.6</para> 

	[Serializable]
	public class Xslt30Transformer
	{

		private Controller controller;
		private GlobalParameterSet globalParameterSet;
		private bool primed;
		private bool baseOutputUriWasSet;
		private IMessageListener messageListener;
		private StandardLogger traceFunctionDestination;
		private JStreamSource streamSource;
		private Processor processor;


		internal Xslt30Transformer(Processor proc, Controller controller, GlobalParameterSet staticParameter)
		{

			this.controller = controller;
			this.processor = proc;
			this.globalParameterSet = new GlobalParameterSet(staticParameter);
		}


		///<summary> Supply the context item to be used when evaluating global variables and parameters.
		/// The item to be used as the context item within the initializers
		/// of global variables and parameters. This argument can be null if no context item is to be
		///  supplied.</summary>
		public XdmItem GlobalContextItem
		{
			set { 
				if (primed) {
					throw new DynamicError("Stylesheet has already been evaluated");
				}
				controller.setInitialContextItem(value==null ? null : value.value.head());
			}
			get { return (XdmItem)XdmItem.Wrap(controller.getInitialContextItem()); }

		}


		///<summary> Get the underlying Controller used to implement this XsltTransformer. This provides access
		/// to lower-level methods not otherwise available in the s9api interface. Note that classes
		/// and methods obtained by this route cannot be guaranteed stable from release to release.</summary>
		///<returns> The underlying {@link Controller}</returns>
		public Controller GetUnderlyingController {
			get { return controller; }
		}

		internal JReceiver GetDestinationReceiver(XmlDestination destination){
			if (destination is Serializer) {
				Serializer serializer = (Serializer) destination;
				serializer.SetDefaultOutputProperties(controller.getExecutable ().getDefaultOutputProperties());
				serializer.SetCharacterMap (controller.getExecutable().getCharacterMapIndex());
				String filename = serializer.GetFilename ();
				java.io.OutputStream dest = serializer.GetOutputDestination();
				if (!baseOutputUriWasSet) {
					if (dest is java.io.FileOutputStream) {
						controller.setBaseOutputURI(filename==null ? "" : filename);
					}
				}
				JReceiver r = serializer.GetReceiver(controller.getConfiguration());
				JPipelineConfiguration pipe = r.getPipelineConfiguration();
				pipe.setController(controller);
				pipe.setLocationProvider(controller.getExecutable().getLocationMap());
				return new net.sf.saxon.serialize.ReconfigurableSerializer(r, serializer.GetOutputProperties(), serializer.GetResult(pipe));
			} else {
				JPipelineConfiguration pipe = controller.getConfiguration().makePipelineConfiguration();
				JReceiver r = destination.GetReceiver(pipe);
				pipe.setController(controller);
				pipe.setLocationProvider(controller.getExecutable().getLocationMap());
				return r;
			}
		}

		/// <summary>
		/// The <c>SchemaValidationMode</c> to be used in this transformation, especially for documents
		/// loaded using the <code>doc()</code>, <code>document()</code>, or <code>collection()</code> functions.
		/// </summary>
		/// 

		public SchemaValidationMode SchemaValidationMode
		{
			get
			{
				switch (controller.getSchemaValidationMode())
				{
				case JValidation.STRICT:
					return SchemaValidationMode.Strict;
				case JValidation.LAX:
					return SchemaValidationMode.Lax;
				case JValidation.STRIP:
					return SchemaValidationMode.None;
				case JValidation.PRESERVE:
					return SchemaValidationMode.Preserve;
				case JValidation.DEFAULT:
				default:
					return SchemaValidationMode.Unspecified;
				}
			}


			set
			{
				switch (value)
				{
				case SchemaValidationMode.Strict:
					controller.setSchemaValidationMode(JValidation.STRICT);
					break;
				case SchemaValidationMode.Lax:
					controller.setSchemaValidationMode(JValidation.LAX);
					break;
				case SchemaValidationMode.None:
					controller.setSchemaValidationMode(JValidation.STRIP);
					break;
				case SchemaValidationMode.Preserve:
					controller.setSchemaValidationMode(JValidation.PRESERVE);
					break;
				case SchemaValidationMode.Unspecified:
				default:
					controller.setSchemaValidationMode(JValidation.DEFAULT);
					break;
				}
			}
		}




		///<summary> Supply the values of global stylesheet variables and parameters.</summary>

		/// <param> parameters a map whose keys are QNames identifying global stylesheet parameters,
		/// and whose corresponding values are the values to be assigned to those parameters. If necessary
		/// the supplied values are converted to the declared type of the parameter.
		/// The contents of the supplied map are copied by this method,
		/// so subsequent changes to the map have no effect.
		/// </param>

		public void SetStylesheetParameters(Dictionary<QName, XdmValue> parameters){

			if (primed) {
				throw new DynamicError("Stylesheet has already been evaluated");
			}
			//try {
			GlobalParameterSet params1 = new GlobalParameterSet();
			foreach (KeyValuePair<QName, XdmValue> entry in parameters) {
				QName qname = entry.Key;
				params1.put (qname.ToStructuredQName(), entry.Value.value);
			}

			globalParameterSet = params1;


		}


		internal void prime() {
			if (!primed) {
				if (globalParameterSet == null) {
					globalParameterSet = new GlobalParameterSet();
				}
				try {
					controller.initializeController(globalParameterSet);
				} catch (net.sf.saxon.trans.XPathException e) {
					throw new DynamicError(e);
				}
			}
			primed = true;
		}



		/// <summary> Get the base output URI.</summary>
		/// <remarks><para> This returns the value set using the {@link #setBaseOutputURI} method. If no value has been set
		/// explicitly, then the method returns null if called before the transformation, or the computed
		/// default base output URI if called after the transformation.</p>
		/// </para>
		/// <para> The base output URI is used for resolving relative URIs in the <code>href</code> attribute
		/// of the <code>xsl:result-document</code> instruction.</para>
		/// <returns> The base output URI</returns>

		public String BaseOutputURI {
			set {
				controller.setBaseOutputURI(value);
				baseOutputUriWasSet = value != null;
			}
			get { return controller.getBaseOutputURI ();}
		}

		/// <summary>
		/// The <c>XmlResolver</c> to be used at run-time to resolve and dereference URIs
		/// supplied to the <c>doc()</c> and <c>document()</c> functions.
		/// </summary>

		public XmlResolver InputXmlResolver
		{
			get
			{
				return ((DotNetURIResolver)controller.getURIResolver()).getXmlResolver();
			}
			set
			{
				controller.setURIResolver(new DotNetURIResolver(value));
			}
		}


		/// <summary>
		/// Listener for messages output using &lt;xsl:message&gt;. 
		/// <para>The caller may supply a message listener before calling <c>Run</c>;
		/// the processor will then invoke the listener once for each message generated during
		/// the transformation. Each message will be output as an object of type <c>XdmNode</c>
		/// representing a document node.</para>
		/// <para>If no message listener is supplied by the caller, message information will be written to
		/// the standard error stream.</para>
		/// </summary>
		/// <remarks>
		/// <para>Each message is presented as an XML document node. Calling <c>ToString()</c>
		/// on the message object will usually generate an acceptable representation of the
		/// message.</para>
		/// <para>When the &lt;xsl:message&gt; instruction specifies <c>terminate="yes"</c>,
		/// the message is first notified using this interface, and then an exception is thrown
		/// which terminates the transformation.</para>
		/// </remarks>

		public IMessageListener MessageListener
		{
			set
			{
				messageListener = value;
				JPipelineConfiguration pipe = controller.makePipelineConfiguration();
				controller.setMessageEmitter(new MessageListenerProxy(pipe, value));
			}
			get
			{
				return messageListener;
			}
		}

		/// <summary>
		/// Destination for output of messages using &lt;trace()&gt;. 
		/// <para>If no message listener is supplied by the caller, message information will be written to
		/// the standard error stream.</para>
		/// </summary>
		/// <remarks>
		/// <para>The supplied destination is ignored if a <code>TraceListener</code> is in use.</para>
		/// <para>Since 9.6. Changed in 9.6 to use a StandardLogger</para>
		/// </remarks>

		public StandardLogger TraceFunctionDestination
		{
			set
			{
				traceFunctionDestination = value;
				controller.setTraceFunctionDestination(value);
			}
			get
			{
				return traceFunctionDestination;
			}
		}


		///<summary><para> Set parameters to be passed to the initial template. These are used
		/// whether the transformation is invoked by applying templates to an initial source item,
		/// or by invoking a named template. The parameters in question are the xsl:param elements
		/// appearing as children of the xsl:template element. </para>
		/// <remarks>
		/// <para>The parameters are supplied in the form of a map; the key is a QName which must
		/// match the name of the parameter; the associated value is an XdmValue containing the
		/// value to be used for the parameter. If the initial template defines any required
		/// parameters, the map must include a corresponding value. If the initial template defines
		/// any parameters that are not present in the map, the default value is used. If the map
		/// contains any parameters that are not defined in the initial template, these values
		/// are silently ignored.</para>

		/// <para>The supplied values are converted to the required type using the function conversion
		/// rules. If conversion is not possible, a run-time error occurs (not now, but later, when
		/// the transformation is actually run).</para>
		/// <para>The <code>XsltTransformer</code> retains a reference to the supplied map, so parameters can be added or
		/// changed until the point where the transformation is run.</para>
		/// <para>The XSLT 3.0 specification makes provision for supplying parameters to the initial
		/// template, as well as global stylesheet parameters. Although there is no similar provision
		/// in the XSLT 1.0 or 2.0 specifications, this method works for all stylesheets, regardless whether
		/// XSLT 3.0 is enabled or not.</para></remarks>

		///<param name="parameters"> The parameters to be used for the initial template</param>
		///<param name="tunnel"> true if these values are to be used for setting tunnel parameters;
		///false if they are to be used for non-tunnel parameters</param>

		public void SetInitialTemplateParameters(Dictionary<QName, XdmValue> parameters, bool tunnel){

			JMap templateParameters = new java.util.HashMap ();
			foreach (KeyValuePair<QName, XdmValue> entry in parameters) {
				QName qname = entry.Key;
				templateParameters.put (qname.ToStructuredQName(), entry.Value.value);
			}

			controller.setInitialTemplateParameters (templateParameters, tunnel);


		}


		/// <summary>initial mode for the transformation. This is used if the stylesheet is
		/// subsequently invoked by any of the <code>applyTemplates</code> methods.</summary>
		///<remarks><para>The value may be the name of the initial mode, or null to indicate the default
		/// (unnamed) mode</para></remarks>

		public QName InitialMode {

			set {
				try {
					controller.setInitialMode(value == null ? null : value.ToStructuredQName());
				} catch (net.sf.saxon.trans.XPathException e) {
					throw new DynamicError(e);
				}
			}
			get{
				net.sf.saxon.trans.Mode mode = controller.getInitialMode ();
				if (mode == null)
					return null;
				else
					return new QName (mode.getModeName().ToString());
			}


		}


		/// <summary>Invoke the stylesheet by applying templates to a supplied Source document, sending the results (wrapped
		/// in a document node) to a given Destination. The invocation uses any initial mode set using {@link #setInitialMode(QName)},
		/// and any template parameters set using {@link #setInitialTemplateParameters(java.util.Map, boolean)}.
		/// </summary>
		/// <param name="input">Input. The source document.To apply more than one transformation to the same source document, the source document
		/// tree can be pre-built using a {@link DocumentBuilder}.</param>
		/// <param name="destination">Destination. the destination of the result document produced by wrapping the result of the apply-templates
		/// call in a document node.  If the destination is a {@link Serializer}, then the serialization
		/// parameters set in the serializer are combined with those defined in the stylesheet
		/// (the parameters set in the serializer take precedence).</param>
		public void ApplyTemplates(Stream input, XmlDestination destination){
			prime ();
			streamSource = new JStreamSource(new DotNetInputStream(input));

			try{
				JReceiver outi = GetDestinationReceiver(destination);// destination.GetReceiver(controller.makePipelineConfiguration());
				controller.initializeController (globalParameterSet);
				controller.transform (streamSource, outi);
			} catch(net.sf.saxon.trans.XPathException exp){
				throw new DynamicError(exp);
			}

		}

		/// <summary>
		/// Invoke the stylesheet by applying templates to a supplied Source document, returning the raw results
		/// as an {@link XdmValue}. The invocation uses any initial mode set using {@link #setInitialMode(QName)},
		/// and any template parameters set using {@link #setInitialTemplateParameters(java.util.Map, boolean)}.
		/// </summary>
		/// <param name="input">Input. The source document</param>
		/// <param name="baseUri">Base URI.</param>
		/// <returns>XdmValue. The raw result of processing the supplied Source using the selected template rule, without
		/// wrapping the returned sequence in a document node</returns>
		public XdmValue ApplyTemplates(Stream input, Uri baseUri){
			prime ();
			streamSource = new JStreamSource(new DotNetInputStream(input), baseUri.ToString());

			try{
				JPipelineConfiguration pipe = controller.makePipelineConfiguration();
				JSequenceOutputter outi = new JSequenceOutputter(pipe, controller, 1);
				controller.initializeController(globalParameterSet);
				controller.transform(streamSource, outi);


				controller.initializeController (globalParameterSet);
				controller.transform (streamSource, outi);
				return XdmValue.Wrap(outi.getSequence());
			} catch(net.sf.saxon.trans.XPathException exp){
				throw new DynamicError(exp);
			}

		}


		/// <summary>
		/// Invoke the stylesheet by applying templates to a supplied input sequence, sending the results (wrapped
		/// in a document node) to a given Destination. The invocation uses any initial mode set using {@link #setInitialMode(QName)},
		/// and any template parameters set using {@link #setInitialTemplateParameters(java.util.Map, boolean)}.
		/// </summary>
		/// <param name="selection">Selection. the initial value to which templates are to be applied (equivalent to the <code>select</code>
		/// attribute of <code>xsl:apply-templates</code>)</param>
		/// <param name="destination">Destination. The destination of the result document produced by wrapping the result of the apply-templates
		/// call in a document node.  If the destination is a {@link Serializer}, then the serialization
		/// parameters set in the serializer are combined with those defined in the stylesheet
		/// (the parameters set in the serializer take precedence).</param>
		public void ApplyTemplates(XdmValue selection, XmlDestination destination){
			prime ();
			try{
				JReceiver outi = GetDestinationReceiver(destination);
				if(baseOutputUriWasSet) {
					outi.setSystemId(controller.getBaseOutputURI());
				}
				controller.applyTemplates(selection.Unwrap(), outi);
				destination.Close();
			}catch(net.sf.saxon.trans.XPathException ex){

				throw new DynamicError (ex);
			}

		}


		/// <summary>
		/// Invoke the stylesheet by applying templates to a supplied input sequence, returning the raw results.
		/// as an {@link XdmValue}. The invocation uses any initial mode set using {@link #setInitialMode(QName)},
		/// and any template parameters set using {@link #setInitialTemplateParameters(java.util.Map, boolean)}.
		/// </summary>
		/// <param name="selection">Selection. selection the initial value to which templates are to be applied (equivalent to the <code>select</code>
		/// attribute of <code>xsl:apply-templates</code>)</param>
		/// <returns>Xdmvalue. he raw result of applying templates to the supplied selection value, without wrapping in
		/// a document node or serializing the result. If there is more that one item in the selection, the result
		/// is the concatenation of the results of applying templates to each item in turn.</returns>
		public XdmValue ApplyTemplates(XdmValue selection){
			prime ();
			try{

				JPipelineConfiguration pipe = controller.makePipelineConfiguration();
				JSequenceOutputter outi = new JSequenceOutputter(pipe, controller, 1);

				if(baseOutputUriWasSet) {
					outi.setSystemId(controller.getBaseOutputURI());
				}
				controller.applyTemplates(selection.Unwrap(), outi);
				return XdmValue.Wrap(outi.getSequence());
			}catch(net.sf.saxon.trans.XPathException ex){

				throw new DynamicError (ex);
			}

		}


		///<summary> Invoke a transformation by calling a named template. The results of calling
		/// the template are wrapped in a document node, which is then sent to the specified
		/// destination. If {@link #setInitialTemplateParameters(java.util.Map, boolean)} has been
		/// called, then the parameters supplied are made available to the called template (no error
		/// occurs if parameters are supplied that are not used).</summary> 
		///<param name="templateName"> The name of the initial template. This must match the name of a
		/// public named template in the stylesheet. If the value is null,
		/// the QName <code>xsl:initial-template</code> is used.</param>
		/// <param name="destination"> The destination of the result document produced by wrapping the result of the apply-templates
		/// call in a document node.  If the destination is a {@link Serializer}, then the serialization
		/// parameters set in the serializer are combined with those defined in the stylesheet
		/// (the parameters set in the serializer take precedence).</param> 
		public void CallTemplate(QName templateName, XmlDestination destination){
			prime ();
			if (templateName == null) {
				templateName = new QName ("xsl", NamespaceConstant.XSLT, "initial-template");
			}
			/*if (destination is Serializer) {
			Serializer serializer = (Serializer)destination;
			serializer.SetDefaultOutputProperties(controller.getExecutable ().getDefaultOutputProperties());

		}*/
			try{
				JReceiver outi = GetDestinationReceiver(destination);
				if(baseOutputUriWasSet) {
					outi.setSystemId(controller.getBaseOutputURI());
				}
				controller.callTemplate(templateName.ToStructuredQName(), outi);
			} catch(net.sf.saxon.trans.XPathException exp){
				throw new DynamicError(exp);
			}
		}




		public XdmValue CallTemplate(QName templateName){
			prime ();
			if (templateName == null) {
				templateName = new QName ("xsl", NamespaceConstant.XSLT, "initial-template");
			}

			try{
				JPipelineConfiguration pipe = controller.makePipelineConfiguration();
				JSequenceOutputter outi = new JSequenceOutputter(pipe, controller, 1);

				controller.callTemplate(templateName.ToStructuredQName(), outi);
				return XdmValue.Wrap(outi.getSequence());
			} catch(net.sf.saxon.trans.XPathException exp){
				throw new DynamicError(exp);
			}
		}


		///<summary> Call a public user-defined function in the stylesheet. </summary>
		/// <param name="function"> The name of the function to be called</param>
		///<param name="argument">  The values of the arguments to be supplied to the function. These
		/// will be converted if necessary to the type as defined in the function signature, using
		/// the function conversion rules.</param>
		/// <returns> the result of calling the function. This is the raw result, without wrapping in a document
		/// node and without serialization.</returns>

		public XdmValue CallFunction(QName function, XdmValue[] arguments){
			prime ();
			try{
				net.sf.saxon.trans.SymbolicName fName = new net.sf.saxon.trans.SymbolicName(net.sf.saxon.om.StandardNames.XSL_FUNCTION, function.ToStructuredQName(), arguments.Length);
				JConfiguration config = processor.Implementation;
				net.sf.saxon.sxpath.IndependentContext env = new net.sf.saxon.sxpath.IndependentContext(config);
				PreparedStylesheet pss = (PreparedStylesheet) controller.getExecutable();
				net.sf.saxon.expr.Component f = pss.getComponent(fName);
				if (f == null) {
					net.sf.saxon.trans.XPathException exception = new net.sf.saxon.trans.XPathException ("No function with name " + function.ClarkName + " and arity " + arguments.Length + " has been declared in the stylesheet", "XTDE0041");
					throw new DynamicError(exception);
				}
				JUserFunction uf = (JUserFunction)f.getProcedure ();
				net.sf.saxon.expr.instruct.UserFunctionParameter [] params1 = uf.getParameterDefinitions();
				net.sf.saxon.om.Sequence [] vr = new net.sf.saxon.om.Sequence[arguments.Length];
				for (int i = 0; i < arguments.Length; i++) {
					net.sf.saxon.value.SequenceType type = params1[i].getRequiredType();
					vr [i] = arguments [i].Unwrap ();
					if (!type.matches(vr[i], config)) {
						JRoleLocator role = new JRoleLocator(JRoleLocator.FUNCTION, function.ToStructuredQName(), i);
						vr[i] = config.getTypeHierarchy().applyFunctionConversionRules(vr[i], type, role, env);
					}
				}

				net.sf.saxon.expr.XPathContextMajor context = controller.newXPathContext ();
				context.setCurrentComponent (pss.getComponent(fName));
				net.sf.saxon.om.Sequence result = uf.call (context, vr);
				return XdmValue.Wrap (result);
			} catch(net.sf.saxon.trans.XPathException ex){
				throw new DynamicError(ex);

			}

		}



		/// <summary>Call a public user-defined function in the stylesheet, wrapping the result in an XML document, and sending
		/// this document to a specified destination</summary>    
		///<param name="function"> The name of the function to be called</param>
		///<param name="arguments"> The values of the arguments to be supplied to the function. These
		///                    will be converted if necessary to the type as defined in the function signature, using
		///                    the function conversion rules.</param>
		///<param name="destination"> The destination of the result document produced by wrapping the result of the apply-templates
		///                    call in a document node.  If the destination is a {@link Serializer}, then the serialization
		///                    parameters set in the serializer are combined with those defined in the stylesheet
		//                    (the parameters set in the serializer take precedence).</param>

		public void CallFunction(QName function, XdmValue[] arguments, XmlDestination destination){
			XdmValue result = CallFunction (function, arguments);
			if (destination is Serializer) {
				// TODO: call the function in push mode, avoiding creation of the result in memory
				Serializer serializer = (Serializer) destination;
				serializer.SetDefaultOutputProperties(controller.getExecutable().getDefaultOutputProperties());
				processor.WriteXdmValue (result, destination);
				destination.Close ();
			}


		}




	}

	///<summary> An <c>XsltPackage</c> object represents the result of compiling an XSLT 3.0 package, as
	/// represented by an XML document containing an <c>xsl:package</c> element.</summary>
	/// <remarks><para>
	/// @since 9.6
	/// </para></remarks>

	[Serializable]
	public class XsltPackage
	{
		private Processor processor;
		private JXsltPackage package;

		internal XsltPackage(Processor p, JXsltPackage pp){
			this.processor = p;
			this.package = pp;

		}

		/// <summary>
		/// Get the Processor from which this XsltCompiler was constructed
		/// </summary>
		public Processor Processor
		{
			get { return processor; }
		}

		/**
	 * <summary>
     * Get the name of the package (the URI appearing as the value of <code>xsl:package/@name</code>)
     *</summary>
     * <returns>return the package name</returns>
     */

		public String PackageName {
			get { return package.getName();}
		}

		/**
     * <summary>Get the version number of the package (the value of the attribute <code>xsl:package/@package-version</code></summary>
     *
     * @return the package version number
     */

		public String Version {
			get { return package.getVersion(); }
		}

		/**
     * <summary>Link this package with the packages it uses to form an executable stylesheet. This process fixes
     * up any cross-package references to files, templates, and other components, and checks to ensure
     * that all such references are consistent.</summary>
     *
     * <returns> the resulting XsltExecutable</returns>
     */

		public XsltExecutable Link()  {
			try {

				JCompilation compilation = new JCompilation(processor.Implementation, new JCompilerInfo());
				PreparedStylesheet pss = new PreparedStylesheet(compilation);
				package.getUnderlyingPreparedPackage().updatePreparedStylesheet(pss);
				return new XsltExecutable(processor, pss);
			} catch (net.sf.saxon.trans.XPathException e) {
				throw new StaticError(e);
			}
		}


		/**
     * <summary>Escape-hatch interface to the underlying implementation class.</summary>
     * <returns>the underlying StylesheetPackage. The interface to StylesheetPackage
     * is not a stable part of the s9api API definition.</returns>
     */

		public JStylesheetPackage getUnderlyingPreparedPackage() {
			return package.getUnderlyingPreparedPackage();
		}

	}


}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////