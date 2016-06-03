////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.ValueOfCompiler;
import net.sf.saxon.Controller;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.IdentityWrapper;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;

import java.util.Map;

/**
 * An xsl:value-of element in the stylesheet. <br>
 * The xsl:value-of element takes attributes:<ul>
 * <li>a mandatory attribute select="expression".
 * This must be a valid String expression</li>
 * <li>an optional disable-output-escaping attribute, value "yes" or "no"</li>
 * <li>an optional separator attribute. This is handled at compile-time: if the separator attribute
 * is present, the select expression passed in here will be a call to the string-join() function.</li>
 * </ul>
 */

public final class ValueOf extends SimpleNodeConstructor {

    private int options;
    private boolean isNumberingInstruction = false;  // set to true if generated by xsl:number
    private boolean noNodeIfEmpty;

    /**
     * Create a new ValueOf expression
     *
     * @param select        the select expression
     * @param disable       true if disable-output-escaping is in force
     * @param noNodeIfEmpty true if the instruction is to return () if the select expression is (),
     *                      false if it is to return an empty text node
     */

    public ValueOf(Expression select, boolean disable, boolean noNodeIfEmpty) {
        setSelect(select);
        options = disable ? ReceiverOptions.DISABLE_ESCAPING : 0;
        this.noNodeIfEmpty = noNodeIfEmpty;
        adoptChildExpression(select);

        // If value is fixed, test whether there are any special characters that might need to be
        // escaped when the time comes for serialization
        if (select instanceof StringLiteral) {
            boolean special = false;
            CharSequence val = ((StringLiteral) select).getStringValue();
            for (int k = 0; k < val.length(); k++) {
                char c = val.charAt(k);
                if ((int) c < 33 || (int) c > 126 ||
                        c == '<' || c == '>' || c == '&') {
                    special = true;
                    break;
                }
            }
            if (!special) {
                options |= ReceiverOptions.NO_SPECIAL_CHARS;
            }
        }
    }

    /**
     * Indicate that this is really an xsl:nunber instruction
     */

    public void setIsNumberingInstruction() {
        isNumberingInstruction = true;
    }

    /**
     * Determine whether this is really an xsl:number instruction
     *
     * @return true if this derives from xsl:number
     */

    public boolean isNumberingInstruction() {
        return isNumberingInstruction;
    }

    public boolean isNoNodeIfEmpty() {
        return noNodeIfEmpty;
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     *
     * @return the namecode of the instruction name
     */

    public int getInstructionNameCode() {
        if (isNumberingInstruction) {
            return StandardNames.XSL_NUMBER;
        } else if (getSelect() instanceof StringLiteral) {
            return StandardNames.XSL_TEXT;
        } else {
            return StandardNames.XSL_VALUE_OF;
        }
    }

    /**
     * Test for any special options such as disable-output-escaping
     *
     * @return any special options
     */

    public int getOptions() {
        return options;
    }

    /**
     * Test whether disable-output-escaping was requested
     *
     * @return true if disable-output-escaping was requested
     */

    public boolean isDisableOutputEscaping() {
        return (options & ReceiverOptions.DISABLE_ESCAPING) != 0;
    }

    /*@NotNull*/
    public ItemType getItemType() {
        return NodeKindTest.TEXT;
    }

    public int computeCardinality() {
        if (noNodeIfEmpty) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    public void localTypeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) {
        //
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     * dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */
    @Override
    public int getIntrinsicDependencies() {
        int d = super.getIntrinsicDependencies();
        if (isDisableOutputEscaping()) {
            // Bug 2312 : prevent extraction of global variables
            d |= StaticProperty.DEPENDS_ON_ASSIGNABLE_GLOBALS;
        }
        return d;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        ValueOf exp = new ValueOf(getSelect().copy(rebindings), (options & ReceiverOptions.DISABLE_ESCAPING) != 0, noNodeIfEmpty);
        ExpressionTool.copyLocationInfo(this, exp);
        if (isNumberingInstruction) {
            exp.setIsNumberingInstruction();
        }
        return exp;
    }

    /**
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     *
     * @param parentType The schema type
     * @param whole      true if this expression is to account for the whole value of the type
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, boolean whole) throws XPathException {
        // if the expression is a constant value, check that it is valid for the type
        if (getSelect() instanceof Literal) {
            GroundedValue selectValue = ((Literal) getSelect()).getValue();
            SimpleType stype = null;
            if (parentType instanceof SimpleType && whole) {
                stype = (SimpleType) parentType;
            } else if (parentType instanceof ComplexType && ((ComplexType) parentType).isSimpleContent()) {
                stype = ((ComplexType) parentType).getSimpleContentType();
            }
            if (whole && stype != null && !stype.isNamespaceSensitive()) {
                // Can't validate namespace-sensitive content statically
                ValidationFailure err = stype.validateContent(
                        selectValue.getStringValue(), null, getConfiguration().getConversionRules());
                if (err != null) {
                    err.setLocator(getLocation());
                    err.setErrorCode(isXSLT() ? "XTTE1540" : "XQDY0027");
                    throw err.makeException();
                }
                return;
            }
            if (parentType instanceof ComplexType &&
                    !((ComplexType) parentType).isSimpleContent() &&
                    !((ComplexType) parentType).isMixedContent() &&
                    !Whitespace.isWhite(selectValue.getStringValue())) {
                XPathException err = new XPathException("The containing element must be of type " + parentType.getDescription() +
                        ", which does not allow text content " +
                        Err.wrap(selectValue.getStringValue()));
                err.setLocation(getLocation());
                err.setIsTypeError(true);
                throw err;
            }
        } else {
            // check that the type allows text nodes. If not, this is a warning condition, since the text
            // node might turn out to be whitespace
            // DROPPED because we can't do env.issueWarning
//            if (parentType instanceof ComplexType &&
//                    !((ComplexType) parentType).isSimpleContent() &&
//                    !((ComplexType) parentType).isMixedContent()) {
//                env.issueWarning("The containing element must be of type " + parentType.getDescription() +
//                        ", which does not allow text content other than whitespace", this);
//            }
        }
    }

    /**
     * Convert this value-of instruction to an expression that delivers the string-value of the resulting
     * text node as an untyped atomic value.
     *
     * @return the converted expression
     */

    public Expression convertToCastAsString() {
        if (noNodeIfEmpty || !Cardinality.allowsZero(getSelect().getCardinality())) {
            return new CastExpression(getSelect(), BuiltInAtomicType.UNTYPED_ATOMIC, true);
        } else {
            // must return zero-length string rather than () if empty
            Expression sf = SystemFunction.makeCall("string", getRetainedStaticContext(), getSelect());
            return new CastExpression(sf, BuiltInAtomicType.UNTYPED_ATOMIC, false);
        }
    }

    /**
     * Process this instruction
     *
     * @param context the dynamic context of the transformation
     * @return a TailCall to be executed by the caller, always null for this instruction
     */

    /*@Nullable*/
    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        if (noNodeIfEmpty) {
            StringValue value = (StringValue) getSelect().evaluateItem(context);
            if (value != null) {
                processValue(value.getStringValueCS(), context);
            }
            return null;
        } else {
            return super.processLeavingTail(context);
        }
    }

    /**
     * Process the value of the node, to create the new node.
     *
     * @param value   the string value of the new node
     * @param context the dynamic evaluation context
     * @throws net.sf.saxon.trans.XPathException
     *
     */

    public void processValue(CharSequence value, XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        out.characters(value, getLocation(), options);
    }

    /**
     * Evaluate this expression, returning the resulting text node to the caller
     *
     * @param context the dynamic evaluation context
     * @return the parentless text node that results from evaluating this instruction, or null to
     *         represent an empty sequence
     * @throws XPathException
     */

    public NodeInfo evaluateItem(XPathContext context) throws XPathException {
        try {
            CharSequence val;
            Item item = getSelect().evaluateItem(context);
            if (item == null) {
                if (noNodeIfEmpty) {
                    return null;
                } else {
                    val = "";
                }
            } else {
                val = item.getStringValueCS();
            }
            Controller controller = context.getController();
            assert controller != null;
            Orphan o = new Orphan(controller.getConfiguration());
            o.setNodeKind(Type.TEXT);
            o.setStringValue(val);
            return o;
        } catch (XPathException err) {
            err.maybeSetLocation(getLocation());
            throw err;
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the ValueOf expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ValueOfCompiler();
    }
//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("valueOf", this);
        String flags = "";
        if ((options & ReceiverOptions.DISABLE_ESCAPING) != 0) {
            flags += "d";
        }
        if ((options & ReceiverOptions.NO_SPECIAL_CHARS) != 0) {
            flags += "S";
        }
        if (noNodeIfEmpty) {
            flags += "e";
        }
        if (!flags.isEmpty()) {
            out.emitAttribute("flags", flags);
        }
        getSelect().export(out);
        out.endElement();
    }
}

