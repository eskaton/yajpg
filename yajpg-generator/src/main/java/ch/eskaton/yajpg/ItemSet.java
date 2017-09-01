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
 * A set of items combines all rules that expect the same next symbol and 
 * therefore lead to the same state.
 */
public class ItemSet {

    private Set<Item> set;

    public ItemSet(Set<Item> set) {
        this.set = set;
    }

    public Set<Item> getSet() {
        return set;
    }

    /**
     * Returns the next expected symbol.
     * 
     * @return The expected symbol
     */
    public Symbol getCurrentSymbol() {
        return set.iterator().next().getCurrentSymbol();
    }

    /**
     * Returns whether there more rules which contain symbols.
     * 
     * @return True if there are more rules
     */
    public boolean hasMoreSymbols() {
        return set.iterator().next().hasMoreSymbols();
    }

    /**
     * Sets the action.
     * 
     * @param action
     *            An action
     */
    public void setAction(Action action) {
        for (Item item : set) {
            item.setAction(action);
        }
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof ItemSet)) {
            return false;
        }

        Set<Item> otherSet = new HashSet<Item>((Set<Item>) ((ItemSet) o).set);

        for (Item item : set) {
            if (!otherSet.contains(item)) {
                return false;
            } else {
                otherSet.remove(item);
            }
        }

        return otherSet.size() == 0;
    }

    public int hashCode() {
        int hash = 0;

        for (Item item : set) {
            hash += 37 * item.hashCode();
        }

        return hash;
    }

}
