////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.instruct.TemplateRule;
import net.sf.saxon.expr.parser.IdentityWrapper;
import net.sf.saxon.om.Item;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;

import java.util.HashMap;

/**
 * Rule: a template rule, or a strip-space rule used to support the implementation
 */

public class Rule {
    protected Pattern pattern;      // The pattern that fires this rule
    protected RuleTarget action;      // The action associated with this rule (usually a Template)
    protected int precedence;         // The import precedence
    protected int minImportPrecedence;// The minimum import precedence to be considered by xsl:apply-imports
    protected double priority;        // The priority of the rule

    protected Rule next;              // The next rule after this one in the chain of rules
    protected int sequence;           // The relative position of this rule, its position in declaration order
    private boolean alwaysMatches;  // True if the pattern does not need to be tested, because the rule
    // is on a rule-chain such that the pattern is necessarily satisfied
    private int rank;               // Indicates the relative precedence/priority of a rule within a mode;
    // used for quick comparison


    public Rule() {}
    /**
     * Create a Rule.
     *
     * @param p    the pattern that this rule matches
     * @param o    the object invoked by this rule (usually a Template)
     * @param prec the precedence of the rule
     * @param min  the minumum import precedence for xsl:apply-imports
     * @param prio the priority of the rule
     * @param seq  a sequence number for ordering of rules
     */

    public Rule(/*@NotNull*/ Pattern p, /*@NotNull*/ RuleTarget o, int prec, int min, double prio, int seq) {
        pattern = p;
        action = o;
        precedence = prec;
        minImportPrecedence = min;
        priority = prio;
        next = null;
        sequence = seq;
        o.registerRule(this);
    }

    /**
     * Copy a rule, including optionally the chain of rules linked to it
     *
     * @param r the rule to be copied
     * @param copyChain true if the whole chain of rules is to be copied
     */

    public Rule(Rule r, boolean copyChain) {
        pattern = r.pattern.copy(new HashMap<IdentityWrapper<Binding>, Binding>());
        action = r.action instanceof TemplateRule ? ((TemplateRule) r.action).copy() : r.action;
        precedence = r.precedence;
        minImportPrecedence = r.minImportPrecedence;
        priority = r.priority;
        sequence = r.sequence;
        if (r.next == null || !copyChain) {
            next = null;
        } else {
            next = new Rule(r.next, true);
        }
        action.registerRule(this);
    }

    public int getSequence() {
        return sequence;
    }

    public void setAction(/*@NotNull*/ RuleTarget action) {
        this.action = action;
    }

    /*@NotNull*/
    public RuleTarget getAction() {
        return action;
    }

    /*@Nullable*/
    public Rule getNext() {
        return next;
    }

    public void setNext( /*@Nullable*/Rule next) {
        this.next = next;
    }

    /*@NotNull*/
    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public int getPrecedence() {
        return precedence;
    }

    public int getMinImportPrecedence() {
        return minImportPrecedence;
    }

    public double getPriority() {
        return priority;
    }

    public void setAlwaysMatches(boolean matches) {
        alwaysMatches = matches;
    }

    public boolean isAlwaysMatches() {
        return alwaysMatches;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }


    /**
     * Export this rule
     * @param out   the destination for the export
     * @param modeStreamable    if the mode for this rule is streamable (should be EE only?)
     */
    public void export(ExpressionPresenter out, boolean modeStreamable) throws XPathException {
        RuleTarget target = getAction();
        TemplateRule template = null;
        if (target instanceof TemplateRule) {
            template = (TemplateRule) target;
            int s = out.startElement("templateRule");
            out.emitAttribute("prec", getPrecedence() + "");
            out.emitAttribute("prio", getPriority() + "");
            out.emitAttribute("seq", getSequence() + "");
            out.emitAttribute("rank", "" + getRank()); // project: preconditions
            out.emitAttribute("minImp", getMinImportPrecedence() + "");
            out.emitAttribute("slots", template.getStackFrameMap().getNumberOfVariables() + "");
            //out.emitAttribute("binds", template.getDeclaringComponent().getComponentBindings().size() + "");
            //#ifdefined STREAM
            if (modeStreamable) {
                out.emitAttribute("W3C_streamable", Boolean.toString(template.isActuallyStreamable(false, null)));
            }
            //#endif
            template.explainProperties(out);
            exportOtherProperties(out);
            /*int[] preconditionIndexes = getPreconditionIndexes();
            if (preconditionIndexes != null && preconditionIndexes.length > 0) {
                boolean first = true;
                String indexes = "";
                for (int i : preconditionIndexes) {
                    indexes = indexes + (!first ? " " : "") + i;
                    first = false;
                }
                out.emitAttribute("conditions", indexes);
            }
            Set<Expression> preconditions = getPreconditions();
            if (preconditions != null && preconditions.size() > 0) {
                out.startSubsidiaryElement("c.pre");
                for (Expression e : preconditions) {
                    e.export(out);
                }
                out.endSubsidiaryElement();
            }*/
            out.setChildRole("match");
            getPattern().export(out);
            if (template.getBody() != null) {
                out.setChildRole("action");
                template.getBody().export(out);
            }
            int e = out.endElement();
            if (s != e) {
                throw new IllegalStateException(
                        "exported expression tree unbalanced in template at line " +
                                (template != null ?
                                        template.getLineNumber() + " of " + template.getSystemId() : ""));
            }
        } else {
            target.export(out);
        }
    }

    /**
     * Add other exported properties as required
     * @param out  the export destination
     */
    public void exportOtherProperties(ExpressionPresenter out) throws XPathException {}


    /**
     * Rules have an ordering, based on their precedence and priority. This method compares
     * them using the precomputed rank value.
     *
     * @param other Another rule whose ordering rank is to be compared with this one
     * @return <0 if this rule has lower rank, that is if it has lower precedence or equal
     * precedence and lower priority. 0 if the two rules have equal precedence and
     * priority. >0 if this rule has higher rank in precedence/priority order
     */

    public int compareRank(Rule other) {
        return rank - other.rank;
    }

    /**
     * Rules have an ordering, based on their precedence and priority.
     *
     * @param other Another rule whose ordering rank is to be compared with this one
     * @return <0 if this rule has lower rank, that is if it has lower precedence or equal
     * precedence and lower priority. 0 if the two rules have equal precedence and
     * priority. >0 if this rule has higher rank in precedence/priority order
     */

    public int compareComputedRank(Rule other) {
        if (precedence == other.precedence) {
            if (priority == other.priority) {
                return 0;
            } else if (priority < other.priority) {
                return -1;
            } else {
                return +1;
            }
        } else if (precedence < other.precedence) {
            return -1;
        } else {
            return +1;
        }
    }

    public boolean matches(Item item, XPathContextMajor context) throws XPathException {
        return alwaysMatches || pattern.matches(item, context);
    }


    /*// project:preconditions
    // TODO - shift these details into the EE sections.

    *//**
     * Optimize the pattern for a rule with respect to a named node (element or attribute).
     * This is usually carried out on the base pattern, to determine if it will match the
     * named node to be matched.
     *
     * @param fingerprint the fingerprint of the name of the node to be matched.
     *//*
    public void optimizeForName(int fingerprint) {
        if (pattern != null) {
            PatternADJ pa = Adjunct.getPatternAdjunct(pattern);
            Pattern o = pa.optimizeForName(fingerprint);
            if (o == null) {
                o = UniversalPattern.getInstance();
                ExpressionTool.copyLocationInfo(pattern, o);
            }
            pattern = o;
        }
    }

    *//**
     * Optimize the pattern of a rule with respect to a typed node.
     * This is usually carried out on the base pattern, to determine if it will match the
     * typed node to be matched.
     *
     * @param type the type of the node to be matched.
     *//*
    public void optimizeForType(int type) {
        if (pattern != null) {
            PatternADJ pa = Adjunct.getPatternAdjunct(pattern);
            Pattern o = pa.optimizeForType(type);
            if (o == null) {
                o = UniversalPattern.getInstance();
                ExpressionTool.copyLocationInfo(pattern, o);
            }
            pattern = o;
        }
    }

    *//**
     * Generic optimization of the pattern of a rule
     *//*
    public void optimizePattern() {
        if (pattern != null) {
            PatternADJ pa = Adjunct.getPatternAdjunct(pattern);
            pattern = pa.optimizePattern();
        }
    }*/


    /**
     * Retain only preconditions that are retained globally and set the index
     * Set the indexes of the preconditions, so that simple access into the matcher state can be used
     *
     * @param index map of the indexes for each precondition
     *//*
    public void reduceAndIndexPreconditions(Map<Expression, Integer> index) {
        preconditions = RealPreconditions.intersect(preconditions, index.keySet());
        List<Integer> temp = new ArrayList<Integer>();
        for (Expression e : preconditions) {
            temp.add(index.get(e));
        }
        preconditionIndexes = new int[temp.size()];
        int i = 0;
        for (Integer ind : temp) {
            preconditionIndexes[i++] = ind;
        }
    }*/


   /* *//**
     * Evaluate the effect of the preconditions on the rule pattern, i.e. they can be assumed to be true
     *//*
    public void evalPreconditions() {
        for (Expression e : preconditions) {
            PatternADJ pa = Adjunct.getPatternAdjunct(pattern);
            pattern = pa.evaluateAgainstBoolean(e);
        }
    }*/
}