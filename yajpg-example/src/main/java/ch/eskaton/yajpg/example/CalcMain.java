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
package ch.eskaton.yajpg.example;

import java.io.IOException;

import ch.eskaton.yajpg.api.Node;
import ch.eskaton.yajpg.api.ParseException;
import ch.eskaton.yajpg.example.parser.CalcLexer;
import ch.eskaton.yajpg.example.parser.CalcNode;
import ch.eskaton.yajpg.example.parser.CalcParser;
import ch.eskaton.yajpg.example.parser.CalcToken;
import ch.eskaton.yajpg.example.parser.CalcValue;

public class CalcMain {

    public static void main(String[] args) throws ParseException, IOException {
        if (args.length != 1) {
            System.err.println("Usage: CalcMain <expression>");
            System.exit(1);
        } 

        Node root = new CalcParser(new CalcLexer(args[0])).parse();
        System.out.println(new Eval().eval(root));
    }

    private static class Eval {
        double eval(Node n) {
            if (n instanceof CalcValue) {
                return ((CalcValue) n).getValue();
            } else if (n instanceof CalcNode) {
                switch (((CalcNode) n).getToken()) {
                    case PLUS:
                        return eval(n.getLNode()) + eval(n.getRNode());
                    case MINUS:
                        return eval(n.getLNode()) - eval(n.getRNode());
                    case TIMES:
                        return eval(n.getLNode()) * eval(n.getRNode());
                    case DIV:
                        return eval(n.getLNode()) / eval(n.getRNode());
                }
            }

            return 1;
        }

    }

}
