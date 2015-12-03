package net.sf.saxon.resource;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.URIQueryParameters;
import net.sf.saxon.lib.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.XMLReader;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * AbstractCollection is an abstract superclass for the various implementations
 * of ResourceCollection within Saxon. It provides common services such as
 * mapping of file extensions to MIME types, and mapping of MIME types to
 * resource factories.
 */
public abstract class AbstractResourceCollection implements ResourceCollection {

    protected String collectionURI;
    protected URIQueryParameters params = null;

    private Map<String, String> contentTypeMapping = new HashMap<String, String>();
    private Map<String, ResourceFactory> resourceFactoryMapping = new HashMap<String, ResourceFactory>();

    public AbstractResourceCollection() {
        registerFileExtension("xml", "application/xml");
        registerFileExtension("html", "application/html");
        registerFileExtension("atom", "application/atom");
        registerFileExtension("xsl", "application/xml+xslt");
        registerFileExtension("xslt", "application/xml+xslt");
        registerFileExtension("xsd", "application/xml+xsd");
        registerFileExtension("txt", "text/plain");
        registerFileExtension("MF", "text/plain");
        registerFileExtension("class", "application/java");
        registerFileExtension("json", "application/json");
        registerFileExtension("", "application/binary");

        registerContentType("application/xml", XmlResource.FACTORY);
        registerContentType("text/xml", XmlResource.FACTORY);
        registerContentType("application/html", XmlResource.FACTORY);
        registerContentType("text/html", XmlResource.FACTORY);
        registerContentType("application/atom", XmlResource.FACTORY);
        registerContentType("application/xml+xslt", XmlResource.FACTORY);
        registerContentType("application/xml+xsd", XmlResource.FACTORY);
        registerContentType("text/plain", UnparsedTextResource.FACTORY);
        registerContentType("application/java", BinaryResource.FACTORY);
        registerContentType("application/binary", BinaryResource.FACTORY);
        registerContentType("application/json", JSONResource.FACTORY);
    }

    public String getCollectionURI() {
        return collectionURI;
    }

    /**
     * Ask whether the collection is stable. This method should only be called after
     * calling {@link #getResources(XPathContext)} or {@link #getResourceURIs(XPathContext)}
     * @return true if the collection is defined to be stable, that is, if a subsequent call
     * on collection() with the same URI is guaranteed to return the same result. The method returns
     * true if the query parameter stable=yes is present in the URI, or if the configuration property
     * {@link }FeatureKeys.STABLE_COLLECTION_URI} is set.
     * @param context the XPath evaluation context.
     */

    public boolean isStable(XPathContext context) {
        if (params == null) {
            return false;
        }
        Boolean stable = params.getStable();
        if (stable == null) {
            return context.getConfiguration().getBooleanProperty(FeatureKeys.STABLE_COLLECTION_URI);
        } else {
            return stable;
        }
    }

    /**
     * Associate a file extension with a media type. This method may
     * be called to customize the behaviour of a JarCollection to recognize different file extensions
     */

    public void registerFileExtension(String extension, String contentType) {
        contentTypeMapping.put(extension, contentType);
    }

    /**
     * Associate a media type with a resource factory. This method may
     * be called to customize the behaviour of a JarCollection to recognize different file extensions
     */

    public void registerContentType(String contentType, ResourceFactory factory) {
        resourceFactoryMapping.put(contentType, factory);
    }

    protected ParseOptions optionsFromQueryParameters(URIQueryParameters params, XPathContext context) {
        ParseOptions options = new ParseOptions(context.getConfiguration().getParseOptions());

        if (params != null) {
            Integer v = params.getValidationMode();
            if (v != null) {
                options.setSchemaValidationMode(v);
            }

            Boolean xInclude = params.getXInclude();
            if (xInclude != null) {
                options.setXIncludeAware(xInclude);
            }

            int strip = params.getStripSpace();
            if (strip != Whitespace.UNSPECIFIED) {
                options.setStripSpace(strip);
            }

            XMLReader p = params.getXMLReader();
            if (p != null) {
                options.setXMLReader(p);
            }


            // If the URI requested suppression of errors, or that errors should be treated
            // as warnings, we set up a special ErrorListener to achieve this

            int onError = URIQueryParameters.ON_ERROR_FAIL;
            if (params.getOnError() != null) {
                onError = params.getOnError();
            }
            final Controller controller = context.getController();
            //        final PipelineConfiguration oldPipe = context.getConfiguration().makePipelineConfiguration();
            //        oldPipe.setController(context.getController());
            //        final PipelineConfiguration newPipe = new PipelineConfiguration(oldPipe);
            final UnfailingErrorListener oldErrorListener =
                controller == null ? new StandardErrorListener() : controller.getErrorListener();
            if (onError == URIQueryParameters.ON_ERROR_IGNORE) {
                options.setErrorListener(new UnfailingErrorListener() {
                    public void warning(TransformerException exception) {
                    }

                    public void error(TransformerException exception) {
                    }

                    public void fatalError(TransformerException exception) {
                    }
                });
            } else if (onError == URIQueryParameters.ON_ERROR_WARNING) {
                options.setErrorListener(new UnfailingErrorListener() {
                    public void warning(TransformerException exception) {
                        oldErrorListener.warning(exception);
                    }

                    public void error(TransformerException exception) {
                        oldErrorListener.warning(exception);
                        XPathException supp = new XPathException("The document will be excluded from the collection");
                        supp.setLocator(exception.getLocator());
                        oldErrorListener.warning(supp);
                    }

                    public void fatalError(TransformerException exception) {
                        error(exception);
                    }
                });
            }
        }
        return options;
    }

    public static class InputDetails {
        public InputStream inputStream;
        public String contentType;
        public String encoding;
        public ParseOptions parseOptions;
        public int onError = URIQueryParameters.ON_ERROR_FAIL;
    }

    protected InputDetails getInputDetails(String resourceURI) throws XPathException {

        InputDetails inputDetails = new InputDetails();
        try {

            URI uri = new URI(resourceURI);

            if ("file".equals(uri.getScheme())) {
                File file = new File(uri);
                inputDetails.inputStream = new BufferedInputStream(new FileInputStream(file));

            } else {
                //TODO: check for redirects
                URL url = uri.toURL();
                URLConnection connection = url.openConnection();
                inputDetails.inputStream = connection.getInputStream();
                inputDetails.contentType = connection.getContentType();
                inputDetails.encoding = connection.getContentEncoding();

            }
        } catch (URISyntaxException e) {
            throw new XPathException(e);
        } catch (MalformedURLException e) {
            throw new XPathException(e);
        } catch (IOException e) {
            throw new XPathException(e);
        }
        if (inputDetails.contentType == null) {
            inputDetails.contentType = guessContentType(resourceURI, inputDetails.inputStream);
        }
        if (params != null && params.getOnError() != null) {
            inputDetails.onError = params.getOnError();
        }
        return inputDetails;

    }

    /**
     * Guess the content type of a resource from its name and/or its content
     * @param resourceURI the resource URI
     * @param stream the content of the resource. The stream must be positioned at the start.
     *               The method looks ahead in this stream
     *               but resets the current position on exit.
     * @return the media type, or null.
     */

    protected String guessContentType(String resourceURI, InputStream stream) {
        String contentTypeFromStream = null;
        try {
            contentTypeFromStream = URLConnection.guessContentTypeFromStream(stream);
        } catch (IOException err) {
            // ignore the error
        }
        String contentTypeFromName = URLConnection.guessContentTypeFromName(resourceURI);
        String extension = null;
        if (contentTypeFromName == null) {
            extension = getFileExtension(resourceURI);
            if (extension != null) {
                contentTypeFromName = contentTypeMapping.get(extension);
            }
        }
        if (contentTypeFromName == null) {
            return contentTypeFromStream;
        } else {
            if (contentTypeFromStream == null) {
                return contentTypeFromName;
            } else if (contentTypeFromStream.equals(contentTypeFromName)) {
                return contentTypeFromStream;
            } else {
                // we've got two candidates: which is more reliable?
                // At this stage, it's pure pragmatism
                if ("xsl".equals(extension) || "xslt".equals(extension) || "xml".equals(extension)) {
                    return contentTypeFromName;
                } else {
                    return contentTypeFromStream;
                }
            }
        }
    }

    /**
     * Get the file extension from a file name or URI
     * @param name the file name or URI
     * @return the part after the last dot, or null if there is no dot after the last slash or backslash.
     */

    private String getFileExtension(String name) {
        int i = name.lastIndexOf('.');
        int p = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (i > p && i+1 < name.length()) {
            return name.substring(i + 1);
        }
        return null;
    }

    /**
     * Internal method to make a resource for a single entry in the ZIP or JAR file. This involves
     * making decisions about the type of resource. This method can be overridden in a user-defined
     * subclass.
     *
     * @param config     The Saxon configuration
     * @param details     Details of the input, including the input stream delivering the content of the resource.
     *                    The method is expected to
     *                    consume this input stream; the caller will close it on return.
     * @param resourceURI the URI of the entry within the ZIP or JAR file; this will by default be
     *                    in the form collectionURI!path
     * @return a newly created Resource representing the content of this entry in the ZIP or JAR file
     */

    public Resource makeResource(Configuration config, InputDetails details, String resourceURI) throws XPathException {

        ResourceFactory factory = null;
        String contentType = details.contentType;
        if (contentType != null) {
            factory = resourceFactoryMapping.get(contentType);
        }
        if (factory == null) {
            factory = BinaryResource.FACTORY;
        }

        return factory.makeResource(config, resourceURI, contentType, details);
    }

}
