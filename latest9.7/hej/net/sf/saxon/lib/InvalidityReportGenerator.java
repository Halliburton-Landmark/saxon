package net.sf.saxon.lib;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This class <code>InvalidityReportGenerator</code> extends the standard error handler for errors found during validation
 * of an instance document against a schema, used if user specifies -report option on validate. Its effect is to output
 * the validation errors found into the filename specified in an XML format.
 */

public class InvalidityReportGenerator extends StandardInvalidityHandler {

    public static final String REPORT_NS = "http://saxon.sf.net/ns/validation";


    public InvalidityReportGenerator(Configuration config) {
            super(config);
        }



    /**
     * Set the XML document that is to be validated
     *
     * @param id of the source document
     */
    public void setSystemId(String id) {

    }

    /**
     * Set the XSD document used to validation process
     *
     * @param name of xsd file
     */
    public void setSchemaName(String name) {

    }

    public int getErrorCount() {
        return 0;
    }

    public int getWarningCount() {
        return 0;
    }

    public void setXsdVersion(String version) {

    }

    public XMLStreamWriter getWriter() {
        return null;
    }

    /**
     * Receive notification of a validity error.
     *
     * @param failure Information about the nature of the invalidity
     */
    @Override
    public void reportInvalidity(Invalidity failure) throws XPathException {

    }


    public void startReporting(String systemId) throws XPathException {

    }

    public Sequence endReporting() throws XPathException {
       return null;

    }

    /**
     * Create metedata element which contains summary information in the output XML document
     *
     * @throws XPathException
     */

    public void createMetaData() throws XPathException {

    }

}
