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
    private Set<Item> items;

    private int id;

    public State(int id) {
        this.id = id;
        items = new HashSet<Item>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public Set<Item> getItems() {
        return items;
    }

    /**
     * Groups items with the same next symbol to an item set.
     * 
     * @return A list of item sets
     */
    public List<ItemSet> getItemSets() {
        List<ItemSet> itemSetList = new ArrayList<ItemSet>();
        Map<String, Set<Item>> itemSets = new HashMap<String, Set<Item>>();
        String end = "__END__";

        for (Item item : items) {
            Symbol sym = item.getCurrentSymbol();
            String symName;

            if (sym == null) {
                symName = end;
            } else {
                symName = sym.getName();
            }

            Set<Item> srp = itemSets.get(symName);

            if (srp == null) {
                srp = new HashSet<Item>();
                itemSets.put(symName, srp);
            }

            srp.add(item);
        }

        for (Entry<String, Set<Item>> e : itemSets.entrySet()) {
            itemSetList.add(new ItemSet(e.getValue()));
        }

        return itemSetList;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(500);
        List<Item> list;

        sb.append("State " + id + "\n");
        sb.append("------------\n");

        list = new LinkedList<Item>(items);
        Collections.sort(list);

        for (Item item : list) {
            sb.append(item + "\n");
        }

        sb.append("\n");
        return sb.toString();
    }

}
