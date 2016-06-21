package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.functions.AbstractFunction;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyFunctionType;
import net.sf.saxon.type.FunctionItemType;

/**
 * A function item that wraps a Callable
 */

public class CallableFunction extends AbstractFunction {

    private Callable callable;
    private SymbolicName name;
    FunctionItemType type;

    public CallableFunction(SymbolicName name, Callable callable, FunctionItemType type) {
        this.name = name;
        this.callable = callable;
        this.type = type;
    }

    public CallableFunction(int arity, Callable callable, FunctionItemType type) {
        this.name = new SymbolicName(StandardNames.XSL_FUNCTION, new StructuredQName("", "anon", "anon"), arity);
        this.callable = callable;
        this.type = type;
    }

    public Callable getCallable() {
        return callable;
    }

    public void setCallable(Callable callable) {
        this.callable = callable;
    }

    public void setType(FunctionItemType type) {
        this.type = type;
    }

    /**
     * Get the item type of the function item
     *
     * @return the function item's type
     */
    public FunctionItemType getFunctionItemType() {
        if (type == AnyFunctionType.getInstance() && callable instanceof XQueryFunctionLibrary.UnresolvedCallable) {
            UserFunction uf = ((XQueryFunctionLibrary.UnresolvedCallable) callable).getFunction();
            if (uf != null) {
                // the previously unresolved function reference is now resolved
                type = uf.getFunctionItemType();
            }
        }
        return type;
    }

    /**
     * Get the name of the function, or null if it is anonymous
     *
     * @return the function name, or null for an anonymous inline function
     */
    public StructuredQName getFunctionName() {
        return name.getComponentName();
    }

    /**
     * Get a description of this function for use in error messages. For named functions, the description
     * is the function name (as a lexical QName). For others, it might be, for example, "inline function",
     * or "partially-applied ends-with function".
     *
     * @return a description of the function for use in error messages
     */
    public String getDescription() {
        return "internal callable";
    }

    /**
     * Get the arity of the function
     *
     * @return the number of arguments in the function signature
     */
    public int getArity() {
        return name.getArity();
    }

    /**
     * Invoke the function
     *
     * @param context the XPath dynamic evaluation context
     * @param args    the actual arguments to be supplied
     * @return the result of invoking the function
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs within the function
     */
    public Sequence call(XPathContext context, Sequence[] args) throws XPathException {
        return callable.call(context, args);
    }

    /**
     * Output information about this function item to the diagnostic explain() output
     *
     * @param out
     */
    @Override
    public void export(ExpressionPresenter out) throws XPathException {
        if (callable instanceof UserFunction) {
            out.startElement("userFI");
            out.emitAttribute("name", getFunctionName());
            out.emitAttribute("arity", getArity()+"");
        } else {
            super.export(out);
        }
    }
}
