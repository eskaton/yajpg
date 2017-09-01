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
package ch.eskaton.yajpg.example.parser;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;

import ch.eskaton.yajpg.api.Lexer;
import ch.eskaton.yajpg.api.ParseException;
import ch.eskaton.yajpg.api.Token;

public class CalcLexer implements Lexer {

	private PushbackReader reader;

	private int pos = 0;

	public CalcLexer(String string) {
		reader = new PushbackReader(new StringReader(string));
	}

	public Token nextToken() throws ParseException, IOException {
		Token token = getToken();
		return token;
	}

	private int getChar() throws IOException {
		int c = reader.read();
		pos++;
		return c;
	}

	private void ungetChar(int c) throws IOException {
		reader.unread(c);
		pos--;
	}

	private Token getToken() throws IOException, ParseException {
		int c;

		while ((c = getChar()) != -1) {
			switch (c) {
				case ' ':
				case '\t':
					break;
				case '+':
					return new Token(CalcToken.PLUS.ordinal(), "+", pos);
				case '-':
					return new Token(CalcToken.MINUS.ordinal(), "-", pos);
				case '/':
					return new Token(CalcToken.DIV.ordinal(), "/", pos);
				case '*':
					return new Token(CalcToken.TIMES.ordinal(), "*", pos);
				case '\n':
				case '\r':
				case 65535:
					return new Token(CalcToken.EOF.ordinal(), "", pos);
				default:
					Token num = parseNumber(c, reader);

					if (num != null) {
						return num;
					}

					throw new ParseException("Invalid char " + c, pos);
			}

		}

		return new Token(CalcToken.EOF.ordinal(), null, pos);
	}

	private Token parseNumber(int c, PushbackReader reader)
			throws ParseException, IOException {
		int nPos = pos - 1;
		StringBuilder sb = new StringBuilder();

		while (true) {
			if (c >= '0' && c <= '9') {
				sb.append((char) c);
				c = getChar();
			} else {
				ungetChar(c);
				break;
			}
		}

		c = getChar();

		if (c == '.') {
			sb.append(c);

			while (true) {
				if (c >= '0' && c <= '9') {
					sb.append(c);
					c = getChar();
				} else {
					ungetChar(c);
					break;
				}
			}
		} else {
			ungetChar(c);
		}

		if (nPos == pos) {
			return null;
		}

		return new Token(CalcToken.NUMBER.ordinal(), sb.toString(), nPos);
	}

}
