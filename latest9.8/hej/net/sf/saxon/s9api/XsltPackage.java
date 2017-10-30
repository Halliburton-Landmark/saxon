////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.style.PackageVersion;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.XPathException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * An XsltPackage object represents the result of compiling an XSLT 3.0 package, as
 * represented by an XML document containing an <code>xsl:package</code> element.
 *
 * @since 9.6
 */

public class XsltPackage {

    private Processor processor;
    private StylesheetPackage stylesheetPackage;

    protected XsltPackage(Processor p, StylesheetPackage pp) {
        this.processor = p;
        this.stylesheetPackage = pp;
    }

    /**
     * Get the processor under which this package was created
     *
     * @return the corresponding Processor
     */

    public Processor getProcessor() {
        return processor;
    }

    /**
     * Get the name of the package (the URI appearing as the value of <code>xsl:package/@name</code>)
     *
     * @return the package name
     */

    public String getName() {
        return stylesheetPackage.getPackageName();
    }

    /**
     * Get the version number of the package (the value of the attribute <code>xsl:package/@package-version</code>.
     * Note that this may have had trailing zeroes truncated.
     *
     * @return the package version number(s)+suffix?
     */

    public String getVersion() {
        return stylesheetPackage.getPackageVersion().toString();
    }

    /**
     * Get the version of the package as a structured object that meets the requirements of
     * <a href="http://www.w3.org/TR/xslt-30/#package-versions">http://www.w3.org/TR/xslt-30/#package-versions</a>
     *
     * @return  the package version of the stylesheet
     */
    public PackageVersion getPackageVersion() {
        return stylesheetPackage.getPackageVersion();
    }

    /**
     * Get the whitespace stripping policy defined by this stylesheet package, that is, the policy
     * defined by the xsl:strip-space and xsl:preserve-space elements in the source XSLT code of the package.
     *
     * @return a newly constructed WhitespaceStrippingPolicy based on the declarations in
     * this stylesheet package. This policy can be used as input to a {@link DocumentBuilder}.
     */

    public WhitespaceStrippingPolicy getWhitespaceStrippingPolicy() {
        return new WhitespaceStrippingPolicy(stylesheetPackage);
    }


    /**
     * Link this package with the packages it uses, to form an executable stylesheet. This process fixes
     * up any cross-package references to files, templates, and other components, and checks to ensure
     * that all such references are consistent.
     *
     * @return the resulting XsltExecutable
     * @throws SaxonApiException if any error is found during the linking process, for example
     * if the constituent packages containing duplicate component names, or if abstract components
     * are not resolved.
     */

    public XsltExecutable link() throws SaxonApiException {
        try {
            Configuration config = processor.getUnderlyingConfiguration();
            CompilerInfo info = config.makeCompilerInfo();
            Compilation compilation = new Compilation(config, info);
            stylesheetPackage.checkForAbstractComponents();
            PreparedStylesheet pss = new PreparedStylesheet(compilation);
            stylesheetPackage.updatePreparedStylesheet(pss);
            pss.addPackage(stylesheetPackage);
            return new XsltExecutable(processor, pss);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Save this compiled package to filestore.
     *
     * @param file the file to which the compiled package should be saved
     * @throws SaxonApiException if the compiled package cannot be saved to the specified
     *                           location.
     * @since 9.7
     */

    public void save(File file) throws SaxonApiException {
        String target = stylesheetPackage.getTargetEdition();
        if (target == null) {
            target = getProcessor().getSaxonEdition();
        }
        save(file, target);
    }

    /**
     * Save this compiled package to filestore for a particular target environment
     *
     * @param file the file to which the compiled package should be saved
     * @param target the target environment. The only values currently recognized are "JS" and "JS2,
     *               which export the package for running under Saxon-JS 1.0 or 2.0 respectively.
     * @throws SaxonApiException if the compiled package cannot be saved to the specified
     *                           location.
     * @since 9.7.0.5 (experimental and subject to change)
     */

    public void save(File file, String target) throws SaxonApiException {
        try {
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            ExpressionPresenter presenter = getProcessor().getUnderlyingConfiguration()
                    .newExpressionExporter(target, new FileOutputStream(file));
            presenter.setRelocatable(stylesheetPackage.isRelocatable());
            stylesheetPackage.export(presenter);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        } catch (FileNotFoundException e) {
            throw new SaxonApiException(e);
        } catch (IOException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Escape-hatch interface to the underlying implementation class.
     * @return the underlying StylesheetPackage. The interface to StylesheetPackage
     * is not a stable part of the s9api API definition.
     */

    public StylesheetPackage getUnderlyingPreparedPackage() {
        return stylesheetPackage;
    }
}

