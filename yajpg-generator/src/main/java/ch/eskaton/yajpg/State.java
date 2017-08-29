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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * State of the automaton which is used by the parser.
 */
public class State {

	/** Valid rules in the active state */
	private Set<RulePosition> rules;

	private int id;

	public State(int id) {
		this.id = id;
		rules = new HashSet<RulePosition>();
	}

	public void addRulePosition(RulePosition r) {
		rules.add(r);
	}

	public Set<RulePosition> getRules() {
		return rules;
	}

	/**
	 * Creates a group for all rules which have the same next input symbol and
	 * returns a list of this group.
	 * 
	 * @return A list of rule groups
	 */
	public List<RulePositionGroup> getRuleGroups() {
		List<RulePositionGroup> groupList = new ArrayList<RulePositionGroup>();
		Map<String, Set<RulePosition>> groups = new HashMap<String, Set<RulePosition>>();
		String end = "__END__";

		for (RulePosition rp : rules) {
			Symbol sym = rp.getCurrentSymbol();
			String symName;

			if (sym == null) {
				symName = end;
			} else {
				symName = sym.getName();
			}

			Set<RulePosition> srp = groups.get(symName);

			if (srp == null) {
				srp = new HashSet<RulePosition>();
				groups.put(symName, srp);
			}

			srp.add(rp);
		}

		for (Entry<String, Set<RulePosition>> e : groups.entrySet()) {
			groupList.add(new RulePositionGroup(e.getValue()));
		}

		return groupList;
	}

	public int getId() {
		return id;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(500);
		List<RulePosition> list;

		sb.append("State " + id + "\n");
		sb.append("------------\n");

		list = new LinkedList<RulePosition>(rules);
		Collections.sort(list);

		for (RulePosition rp : list) {
			sb.append(rp + "\n");
		}

		sb.append("\n");
		return sb.toString();
	}
}
