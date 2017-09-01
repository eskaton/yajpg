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

import java.util.HashSet;
import java.util.Set;

/**
 * A set of rule positions which accept the same input symbol and lead to the
 * same state.
 */
public class RulePositionGroup {

	private Set<RulePosition> group;

	public RulePositionGroup(Set<RulePosition> group) {
		super();
		this.group = group;
	}

	public Set<RulePosition> getGroup() {
		return group;
	}

	/**
	 * Returns the symbol which is expected next.
	 * 
	 * @return The symbol which is expected next
	 */
	public Symbol getCurrentSymbol() {
		return group.iterator().next().getCurrentSymbol();
	}

	/**
	 * Returns true, if there are rules which accept more symbols.
	 * 
	 * @return True, if there are rules which accept more symbols
	 */
	public boolean hasMoreSymbols() {
		return group.iterator().next().hasMoreSymbols();
	}

	/**
	 * Sets an action.
	 * 
	 * @param action
	 *            An action
	 */
	public void setAction(Action action) {
		for (RulePosition rp : group) {
			rp.setAction(action);
		}
	}

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (!(o instanceof RulePositionGroup)) {
			return false;
		}

		Set<RulePosition> otherGroup = new HashSet<RulePosition>(
				(Set<RulePosition>) ((RulePositionGroup) o).group);

		for (RulePosition rp : group) {
			if (!otherGroup.contains(rp)) {
				return false;
			} else {
				otherGroup.remove(rp);
			}
		}

		return otherGroup.size() == 0;
	}

	public int hashCode() {
		int hash = 0;

		for (RulePosition rp : group) {
			hash += 37 * rp.hashCode();
		}

		return hash;
	}

}
