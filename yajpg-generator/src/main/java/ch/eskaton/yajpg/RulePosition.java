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
 * Implements the state of a rule, i.e. how much is parsed and which action must
 * be executed if the next symbol matches.
 */
public class RulePosition implements Comparable<RulePosition> {

	private Rule rule;

	/** Position in rule starting with 0 */
	private int position;

	/** Action to execute if the next symbol is recognised */
	private Action action;

	public RulePosition(int position, Rule rule) {
		super();
		this.position = position;
		this.rule = rule;
	}

	/**
	 * Advances one symbol to the right and returns the new {@code RulePositon}.
	 * 
	 * @return A {@code RulePosition}
	 */
	public RulePosition getNext() {
		return new RulePosition(position + 1, rule);
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
		return rule + " (" + position + ")" + "\t" + action;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RulePosition))
			return false;

		return rule.equals(((RulePosition) o).rule)
				&& position == ((RulePosition) o).position;
	}

	public int hashCode() {
		return rule.hashCode() * 37 + position;
	}

	public int compareTo(RulePosition rp) {
		return rule.compareTo(rp.rule);
	}

}
