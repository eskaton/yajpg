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

/**
 * Implements the state of a rule, i.e. the parser's position in a rule, and
 * defines which actions to execute when the next symbol is processed.
 */
public class Item implements Comparable<Item> {

    /** Rule */
    private Rule rule;

    /** Position in rule starting with 0 */
    private int position;

    /** Action for the next processed symbol */
    private Action action;

    public Item(int position, Rule rule) {
        this.position = position;
        this.rule = rule;
    }

    /**
     * Advances on symbol to the right and returns a corresponding item.
     * 
     * @return Next item
     */
    public Item getNext() {
        return new Item(position + 1, rule);
    }

    public int getPosition() {
        return position;
    }

    public Symbol getCurrentSymbol() {
        return rule.getRhs(position);
    }

    public Symbol getNextSymbol() {
        return rule.getRhs(position + 1);
    }

    public boolean hasMoreSymbols() {
        if (rule.isAccept()) {
            return rule.getLastRhsPosition() - 1 >= position;
        }
        return rule.getLastRhsPosition() >= position;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public Rule getRule() {
        return rule;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(rule.getRuleNumber()).append(") ");
        sb.append(rule.getLhs()).append(": ");

        for (int i = 0; i < rule.getRhsCount(); i++) {
            if (position == i) {
                sb.append(". ");
            }
            sb.append(rule.getRhs(i)).append(" ");
        }

        if (position == rule.getRhsCount()) {
            sb.append(". ");
        }

        sb.append("\t").append(action);

        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Item))
            return false;

        return rule.equals(((Item) o).rule) && position == ((Item) o).position;
    }

    public int hashCode() {
        return rule.hashCode() * 37 + position;
    }

    public int compareTo(Item rp) {
        return rule.compareTo(rp.rule);
    }

}
