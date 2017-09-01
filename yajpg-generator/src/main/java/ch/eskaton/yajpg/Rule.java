/*
 *  Copyright (c) 2009, Adrian Moser
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  * Neither the name of the author nor the
 *  names of its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL AUTHOR BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.eskaton.yajpg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A grammar rule.
 */
public class Rule implements Comparable<Rule> {

    private static int ruleCount = 0;

    /** Id of a rule */
    private int ruleNumber;

    /** A code block */
    private String code;

    /** Does this rule accept the grammar ? */
    private boolean accept = false;

    /** Left-hand side of a rule */
    private Symbol lhs;

    /** Right-hand side of a rule */
    private ArrayList<Symbol> rhs;

    /** A precedence rule */
    private PrecedenceRule precedenceRule;

    public static void initialize() {
        ruleCount = 0;
    }

    public Rule(Symbol lhs, ArrayList<Symbol> rhs, PrecedenceRule precedenceRule) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.precedenceRule = precedenceRule;
        ruleNumber = ruleCount++;
    }

    public Rule(Symbol lhs, ArrayList<Symbol> rhs,
            PrecedenceRule precedenceRule, String code) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.precedenceRule = precedenceRule;
        this.code = code;
        ruleNumber = ruleCount++;
    }

    /**
     * Returns the non-terminal which is produced by this rule.
     * 
     * @return The non-terminal which is produced by this rule
     */
    public Symbol getLhs() {
        return lhs;
    }

    /**
     * Returns the symbol at position {@code position} starting with 0.
     * 
     * @param position
     *            Position of the symbol
     * @return Symbol at position {@code position}
     */
    public Symbol getRhs(int position) {
        if (position > rhs.size() - 1)
            return null;
        return rhs.get(position);
    }

    /**
     * Returns the position of the last symbol.
     * 
     * @return The position of the last symbol
     */
    public int getLastRhsPosition() {
        return rhs.size() - 1;
    }

    /**
     * Returns the count of symbols in this rule.
     * 
     * @return The count of symbols in this rule
     */
    public int getRhsCount() {
        return rhs.size();
    }

    /**
     * Returns the precedence rule.
     * 
     * @return The precedence rule
     */
    public PrecedenceRule getPrecedenceRule() {
        return precedenceRule;
    }

    /**
     * Returns the number of the rule.
     * 
     * @return The number of the rule
     */
    public int getRuleNumber() {
        return ruleNumber;
    }

    /**
     * Returns true if this rule produces the accept state.
     * 
     * @return True if this rule produces the accept state
     */
    public boolean isAccept() {
        return accept;
    }

    /**
     * Defines whether this rule produces the accept state.
     * 
     * @param accept
     *            True, if this rule produces the accept state
     */
    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    /**
     * Returns a code block which has to be executed for this rule. $-variables
     * are substituted with Java variables.
     * 
     * @param lvalue
     *            Name of the variable to which to assign a value
     * @param variableNames
     *            Names of the variables on the right-hand side
     * @return A code block
     * @throws ConfigException
     *             is thrown if no assignment to $$ takes place
     */
    public String getCode(String lvalue, String[] variableNames)
            throws ConfigException {
        StringBuilder sb = new StringBuilder(code);
        int vInd, lInd;

        if (code == null || code.length() == 0) {
            return ";";
        }

        lInd = sb.indexOf("$$");

        if (lInd == -1) {
            throw new ConfigException("Missing lvalue ($$) in rule: " + this);
        }

        while ((lInd = sb.indexOf("$$")) != -1) {
            sb.replace(lInd, lInd + 2, lvalue);
        }

        for (int i = 1; i <= variableNames.length; i++) {
            while ((vInd = sb.indexOf("$" + i)) != -1) {
                sb.replace(vInd, vInd + String.valueOf(i).length() + 1,
                        variableNames[i - 1]);
            }
        }

        return sb.toString();
    }

    public Set<Integer> getUsedVariables(int varCount) {
        Set<Integer> indices = new HashSet<Integer>();

        if (code == null || code.length() == 0) {
            return indices;
        }

        for (int i = 1; i <= varCount; i++) {
            if (code.indexOf("$" + i) != -1) {
                indices.add(i);
            }
        }

        return indices;

    }

    public String toString() {
        StringBuilder sb = new StringBuilder(50);

        for (Symbol s : rhs) {
            sb.append(s + " ");
        }

        return "(" + ruleNumber + ") " + lhs + ": " + sb.toString();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Rule))
            return false;

        if (((Rule) o).ruleNumber == ruleNumber)
            return true;

        return false;
    }

    public int hashCode() {
        return ruleNumber;
    }

    public int compareTo(Rule rule) {
        return ruleNumber < rule.ruleNumber ? -1
                : ruleNumber > rule.ruleNumber ? 1 : 0;

    }

}
