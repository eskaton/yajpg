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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.eskaton.commons.utils.CollectionUtils;
import ch.eskaton.commons.utils.StringUtils;

/**
 * The config class reads a grammar file, does some checks and provides data to
 * the generator. A config file has the following syntax:
 * 
 * <pre>
 *    settings {
 *      parser-class:   &lt;Name of the parser&gt;;
 *      lexer-class:    &lt;Name of the lexer&gt;;
 *      parser-package: &lt;Package of the parser&gt;;
 *      token-enum:     &lt;Enum of tokens&gt;;
 *      imports:        &lt;Import 1&gt;,
 *                      &lt;Import n&gt;;
 *    }
 *    
 *    token {
 *      &lt;Token 1&gt; (':' '&quot;' &lt;regular expression&gt; '&quot;' ( '{' &lt;code&gt; '}' )? )?, 
 *      ...
 *      &lt;Token n&gt; (':' '&quot;' &lt;regular expression&gt; '&quot;' ( '{' &lt;code&gt; '}' )? )?;
 *    }
 *    
 *    precedence {
 *   	  &lt;Precedence 1&amp;gt: ':' &lt;terminal&gt; (',' (&lt;terminal&gt;) )*;
 *   	  ...
 *   	  &lt;Precedence n&amp;gt: ':' &lt;terminal&gt; (',' (&lt;terminal&gt;) )*;
 *    }
 *     
 *    rules {
 *      &lt;non-terminal 1&gt;('[' class ']')? ':' (&lt;non-terminal|terminal&gt;)* ( '{' &lt;code&gt; '}' )?
 *                                       ('|'; (&lt;non-terminal|terminal&gt;)* ( '{' &lt;code&gt; '}' )? )*;
 *      ...
 *      &lt;non-terminal n&gt;('[' class ']')? ':' (&lt;non-terminal|terminal&gt;)* ( '{' &lt;code&gt; '}' )?
 *                                       ('|' (&lt;non-terminal|terminal&gt;)* ( '{' &lt;code&gt; '}' )? )*;
 *    }
 *    
 *    accept {
 *      expression
 *    }
 * </pre>
 *
 * Consider the following rules:
 * 
 * <ul>
 * <li>
 * imports: Specifies the classes which are used in the code of the rules</li>
 * <li>
 * If no lexer class is specified, no lexer will be generated. In this case
 * token-enum is mandatory. It must define all tokens including an EOF token
 * to be used in a manually written lexer</li>
 * <li>
 * token: A token may contain Java code in curly braces. It must contain an
 * assignment to $$ which resembles the LHS non-terminal.</li>
 * <li>rules: A rule may contain Java code in curly braces. It must contain an
 * assignment to $$ which resembles the LHS non-terminal. The rule's symbols may
 * be referenced by $1 to $n. The class of the non-terminal may be specified in
 * square-brackets. Example of a rule:
 * <p>
 * <code>term[TermNode]: term PLUS variable { $$ = new TermNode($1, PLUS, $2); };</code>
 * </li>
 * <li>Mask closing curly brackets in a code section with a backslash</li>
 * <li>The accept section specifies the non-terminals which accept a grammar</li>
 * </ul>
 */
public class Config {

    static final String EOF = "EOF";

    /** Collection of defined tokens */
    private HashMap<String, Terminal> tokens;

    /** Collection of defined non-terminals */
    private HashMap<String, NonTerminal> nonTerminals;

    /** List of precedence rules */
    private List<PrecedenceRule> precedences;

    /** Set of referenced non-terminals, i.e. non-terminals on the RHS */
    private HashSet<String> referencedNonTerminals;

    /** Set of referenced non-terminals, i.e. non-terminals on the LHS */
    private HashSet<String> referencingNonTerminals;

    /** Non-terminals that accept a grammar */
    private HashMap<String, Rule> acceptStates;

    /** List of grammar rules */
    private ArrayList<Rule> grammar;

    /** Name of parser class */
    private String parserClass;

    /** Name of lexer class */
    private String lexerClass;

    /** Package of parser class */
    private String parserPackage;

    /** Enum of lexer tokens */
    private String tokenEnum;

    /** List of classes to be imported */
    private ArrayList<String> imports;

    private Config() {
    };

    /**
     * The constructor loads the grammar.
     * 
     * @param filename
     *             Name of grammar file
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    public Config(String filename) throws IOException, ConfigException {
        tokens = new HashMap<String, Terminal>();
        nonTerminals = new HashMap<String, NonTerminal>();
        precedences = new ArrayList<PrecedenceRule>();
        grammar = new ArrayList<Rule>();
        acceptStates = new HashMap<String, Rule>();
        referencedNonTerminals = new HashSet<String>();
        referencingNonTerminals = new HashSet<String>();
        loadConfig(filename);
        postProcessConfig();
    }

    /**
     * Checks and enriches the configuration.
     * 
     * @throws ConfigException
     */
    private void postProcessConfig() throws ConfigException {
        if (parserClass == null) {
            throw new ConfigException("parser-class is missing");
        } else if (lexerClass == null && tokenEnum == null) {
            throw new ConfigException("lexer-class and token-enum are missing");
        } else if (parserPackage == null) {
            throw new ConfigException("parser-package is missing");
        }

        if (tokens.containsKey(EOF)) {
            throw new ConfigException("token name '" + EOF + "' is reserved");
        }

        tokens.put(EOF, new Terminal(EOF, "", ""));
    }

    /**
     * Liefert einen Tokenizer zurueck
     * 
     * @param reader
     *            Reader, welcher den Input fuer den Tokenizer liefert
     * @return Tokenizer
     */

   /**
    * Returns a tokenizer.
    * 
    * @param reader
    *            Reader which delivers the input to the tokenizer
    */
    private StreamTokenizer initTokenizer(Reader reader) {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        setSyntax(tokenizer);
        return tokenizer;
    }

    /**
     * Sets the syntax for the tokenizer.
     * 
     * @param tokenizer
     *            Tokenizer
     */
    private void setSyntax(StreamTokenizer tokenizer) {
        tokenizer.resetSyntax();
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('.', '.');
        tokenizer.quoteChar('"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.slashSlashComments(true);
        tokenizer.slashStarComments(true);
    }

    /**
     * Loads the configuration.
     * 
     * @param filename
     *            Name of grammar file
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private void loadConfig(String filename) throws IOException,
            ConfigException {
        StreamTokenizer tokenizer = initTokenizer(new BufferedReader(
                new InputStreamReader(new FileInputStream(filename))));

        if (tokenizer.nextToken() != StreamTokenizer.TT_WORD
                || !"settings".equals(tokenizer.sval)) {
            throw new ConfigException(
                    "Parse error: keyword 'settings' expected");
        } else {
            parseSettings(tokenizer);
        }

        if (tokenizer.nextToken() != StreamTokenizer.TT_WORD
                || !"token".equals(tokenizer.sval)) {
            throw new ConfigException("Parse error: keyword 'token' expected");
        } else {
            parseTokens(tokenizer);
        }

        if (tokenizer.nextToken() != StreamTokenizer.TT_WORD) {
            throw new ConfigException(
                    "Parse error: keyword 'precedence' or 'rules' expected");
        }

        if ("precedence".equals(tokenizer.sval)) {
            parsePrecedence(tokenizer);
        } else {
            tokenizer.pushBack();
        }

        if (tokenizer.nextToken() != StreamTokenizer.TT_WORD
                || !"rules".equals(tokenizer.sval)) {
            throw new ConfigException("Parse error: keyword 'rules' expected");
        } else {
            parseRules(tokenizer);
        }

        if (tokenizer.nextToken() != StreamTokenizer.TT_WORD
                || !"accept".equals(tokenizer.sval)) {
            throw new ConfigException("Parse error: keyword 'accept' expected");
        } else {
            parseAccept(tokenizer);
        }

        if (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            throw new ConfigException(
                    "Parse error: unexpected token at end of file");
        }
    }

    /**
     * Reads the settings section.
     * 
     * @param tokenizer
     *            Tokenizer
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private void parseSettings(StreamTokenizer tokenizer)
            throws ConfigException, IOException {
        int ttype;

        if (tokenizer.nextToken() != '{') {
            throw new ConfigException("Parse error: '{' expected");
        }

        while (true) {
            ttype = tokenizer.nextToken();
            if (ttype == StreamTokenizer.TT_WORD) {
                String setting = tokenizer.sval;
                if (tokenizer.nextToken() != ':') {
                    genericParseError(tokenizer);
                }
                if ("parser-class".equals(setting)) {
                    parserClass = parseString(tokenizer);
                } else if ("lexer-class".equals(setting)) {
                    lexerClass = parseString(tokenizer);
                } else if ("parser-package".equals(setting)) {
                    parserPackage = parseString(tokenizer);
                } else if ("token-enum".equals(setting)) {
                    tokenEnum = parseString(tokenizer);
                } else if ("imports".equals(setting)) {
                    imports = parseList(tokenizer);
                } else {
                    throw new ConfigException(
                            "Parse error: unexpected setting '"
                                    + tokenizer.sval + "'");
                }
            } else if (ttype == '}') {
                break;
            } else {
                genericParseError(tokenizer);
            }
        }
    }

    /**
     * Reads the acept section.
     * 
     * @param tokenizer
     *            Tokenizer
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private void parseAccept(StreamTokenizer tokenizer) throws ConfigException,
            IOException {
        boolean expectAcceptState = true;
        int ttype;

        if (tokenizer.nextToken() != '{') {
            throw new ConfigException("Parse error: '{' expected");
        }

        while (true) {
            ttype = tokenizer.nextToken();
            if (expectAcceptState) {
                if (ttype == StreamTokenizer.TT_WORD) {
                    String acceptState = tokenizer.sval;
                    if (acceptStates.containsKey(acceptState)) {
                        throw new ConfigException("Duplicate accept state: "
                                + acceptState);
                    }
                    Rule acceptRule = findAcceptRule(acceptState);
                    if (acceptRule == null) {
                        throw new ConfigException("No rule for accept state "
                                + acceptState + " defined");
                    }
                    acceptRule.setAccept(true);
                    acceptStates.put(acceptState, acceptRule);
                    expectAcceptState = false;
                } else {
                    genericParseError(tokenizer);
                }
            } else {
                if (ttype == ',') {
                    expectAcceptState = true;
                } else if (ttype == '}') {
                    break;
                } else {
                    genericParseError(tokenizer);
                }
            }
        }

        if (acceptStates.size() == 0) {
            throw new ConfigException("No accept states defined");
        }
    }

    /**
     * Reads the rules section.
     * 
     * @param tokenizer
     *            Tokenizer
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private void parseRules(StreamTokenizer tokenizer) throws IOException,
            ConfigException {
        ArrayList<Symbol> rhs = new ArrayList<Symbol>();
        String code = "";
        int ttype;

        if (tokenizer.nextToken() != '{') {
            throw new ConfigException("Parse error: '{' expected");
        }

        while (true) {
            NonTerminal lhs;
            String clazz = "Node";
            String nonTerminal;

            ttype = tokenizer.nextToken();

            if (ttype == '}') {
                break;
            }

            if (ttype != StreamTokenizer.TT_WORD) {
                throw new ConfigException("Parse error: rule expected");
            }

            /*
             * The left non-terminal may be used multiple times (for different rules)
             */
            nonTerminal = tokenizer.sval;

            /* Optionally a class can be specified in square-brackets */
            if (tokenizer.nextToken() == '[') {
                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD) {
                    throw new ConfigException(
                            "Parse error: class name expected");
                }
                clazz = tokenizer.sval;
                if (tokenizer.nextToken() != ']') {
                    throw new ConfigException("Parse error: ']'");
                }
            } else {
                tokenizer.pushBack();
            }

            if ((lhs = nonTerminals.get(nonTerminal)) == null) {
                lhs = new NonTerminal(nonTerminal, clazz);
            } else {
                /*
                 * In case the non-terminal has been used before its definition,
                 * the class must be set here
                 */
                lhs.setClassName(clazz);
            }

            referencingNonTerminals.add(nonTerminal);
            nonTerminals.put(nonTerminal, lhs);

            if (tokenizer.nextToken() != ':') {
                throw new ConfigException("Parse error: ':' expected");
            }

            String precendenceRuleName = null;

            while (true) {
                ttype = tokenizer.nextToken();

                if (ttype == ';') {
                    grammar
                            .add(new Rule(
                                    lhs,
                                    rhs,
                                    precendenceRuleName != null ? getPrecedenceRule(precendenceRuleName)
                                            : getPrecedenceRule(rhs), code));
                    rhs = new ArrayList<Symbol>();
                    code = "";
                    precendenceRuleName = null;
                    break;
                } else if (ttype == '|') {
                    grammar
                            .add(new Rule(
                                    lhs,
                                    rhs,
                                    precendenceRuleName != null ? getPrecedenceRule(precendenceRuleName)
                                            : getPrecedenceRule(rhs), code));
                    rhs = new ArrayList<Symbol>();
                    code = "";
                    precendenceRuleName = null;
                } else if (ttype == '{') {
                    /*
                     * In the code section the tokenizer must not interpret
                     * characters. It reads to the last closing curly-brace
                     * that isn't escaped with a backslash
                     */
                    StringBuilder sb = new StringBuilder(100);
                    char lastChar = 0;
                    tokenizer.resetSyntax();

                    while ((ttype = tokenizer.nextToken()) != '}'
                            || lastChar == '\\') {
                        if (lastChar == '\\' && ttype == '}') {
                            sb.setCharAt(sb.length() - 1, (char) ttype);
                            lastChar = 0;
                        } else {
                            lastChar = (char) ttype;
                            sb.append(lastChar);
                        }
                    }

                    code = sb.toString();
                    setSyntax(tokenizer);
                } else if (ttype == StreamTokenizer.TT_WORD) {
                    String symbol = tokenizer.sval;
                    if (!tokens.containsKey(symbol)) {
                        NonTerminal nt = nonTerminals.get(symbol);
                        if (nt == null) {
                            nt = new NonTerminal(symbol, clazz);
                            nonTerminals.put(symbol, nt);
                        }
                        rhs.add(nt);
                        referencedNonTerminals.add(symbol);
                    } else {
                        rhs.add(tokens.get(symbol));
                    }
                } else if (ttype == '%') {
                    if (tokenizer.nextToken() != StreamTokenizer.TT_WORD
                            || !"prec".equals(tokenizer.sval)) {
                        throw new ConfigException(
                                "Parse error: 'prec' expected");
                    }

                    if (tokenizer.nextToken() != StreamTokenizer.TT_WORD) {
                        throw new ConfigException(
                                "Parse error: Name of precedence rule expected");
                    }

                    precendenceRuleName = tokenizer.sval;

                } else {
                    genericParseError(tokenizer);
                }
            }
        }

        HashSet<String> undefinedNonTerminals = (HashSet<String>) referencedNonTerminals
                .clone();
        undefinedNonTerminals.removeAll(referencingNonTerminals);
        undefinedNonTerminals.remove(EOF);

        if (!undefinedNonTerminals.isEmpty()) {
            throw new ConfigException(
                    "The following symbols are referenced, but never defined: "
                            + CollectionUtils.join(undefinedNonTerminals, ", "));
        }
    }

    private PrecedenceRule getPrecedenceRule(String precendenceRuleName) {
        for (PrecedenceRule pr : precedences) {
            if (pr.contains(precendenceRuleName)) {
                return pr;
            }
        }

        return new PrecedenceRule(0, Associativity.RIGHTASSOC, null);
    }

    private PrecedenceRule getPrecedenceRule(ArrayList<Symbol> rhs) {
        Terminal lastTerminal = null;

        for (Symbol symbol : rhs) {
            if (symbol instanceof Terminal) {
                lastTerminal = (Terminal) symbol;
            }
        }

        if (lastTerminal != null) {
            for (PrecedenceRule pr : precedences) {
                if (pr.contains(lastTerminal)) {
                    return pr;
                }
            }
        }

        return new PrecedenceRule(0, Associativity.RIGHTASSOC, null);
    }

    /**
     * Reads the precendence section.
     * 
     * @param tokenizer
     *            Tokenizer
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private void parsePrecedence(StreamTokenizer tokenizer) throws IOException,
            ConfigException {
        Set<String> knownSymbols = new HashSet<String>();
        int precendeceValue = 1;
        int ttype;

        if (tokenizer.nextToken() != '{') {
            throw new ConfigException("Parse error: '{' expected");
        }

        while (true) {
            Associativity assoc;
            ttype = tokenizer.nextToken();

            if (ttype == '}') {
                break;
            }

            if (ttype != StreamTokenizer.TT_WORD) {
                throw new ConfigException("Parse error: Associativity expected");
            }

            String assocName = tokenizer.sval;

            try {
                assoc = Associativity.valueOf(assocName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigException(StringUtils.concat(
                        "Invalid associativity '", assocName,
                        "'. Valid values are: ", StringUtils.join(Associativity
                                .values(), ", ")));
            }

            if (tokenizer.nextToken() != ':') {
                throw new ConfigException("Parse error: ':' expected");
            }

            List<String> precedence = parseList(tokenizer);

            for (String s : precedence) {
                if (knownSymbols.contains(s)) {
                    throw new ConfigException(StringUtils.concat(
                            "Duplicate token '", s, "' in precedence list"));
                }
            }

            knownSymbols.addAll(precedence);

            PrecedenceRule pr = new PrecedenceRule(precendeceValue++, assoc,
                    precedence);

            precedences.add(pr);
        }

    }

    /**
     * Parses the token section.
     * 
     * @param tokenizer
     *            Tokenizer
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private void parseTokens(StreamTokenizer tokenizer) throws IOException,
            ConfigException {
        boolean expectTerminal = true;
        int ttype;

        if (tokenizer.nextToken() != '{') {
            throw new ConfigException("Parse error: '{' expected");
        }

        while (true) {
            ttype = tokenizer.nextToken();
            if (expectTerminal) {
                if (ttype == StreamTokenizer.TT_WORD) {
                    String terminalName = tokenizer.sval;
                    if (tokens.containsKey(terminalName)) {
                        throw new ConfigException("Duplicate token: "
                                + terminalName);
                    }

                    if (tokenizer.nextToken() != ':') {
                        tokenizer.pushBack();
                        tokens.put(terminalName, new Terminal(terminalName, "",
                                ""));
                        expectTerminal = false;
                        continue;
                    }

                    if (tokenizer.nextToken() != '"') {
                        throw new ConfigException("Parse error: '\"' expected");
                    }

                    String regex = tokenizer.sval;
                    String code = "";

                    if (tokenizer.nextToken() == '{') {
                        /*
                         * In the code section the tokenizer must not interpret
                         * characters. It reads to the last closing curly-brace
                         * that isn't escaped with a backslash
                         */
                        StringBuilder sb = new StringBuilder(100);
                        char lastChar = 0;
                        tokenizer.resetSyntax();

                        while ((ttype = tokenizer.nextToken()) != '}'
                                || lastChar == '\\') {
                            if (lastChar == '\\' && ttype == '}') {
                                sb.setCharAt(sb.length() - 1, (char) ttype);
                                lastChar = 0;
                            } else {
                                lastChar = (char) ttype;
                                sb.append(lastChar);
                            }
                        }

                        code = sb.toString();
                        setSyntax(tokenizer);
                    } else {
                        tokenizer.pushBack();
                    }

                    tokens.put(terminalName, new Terminal(terminalName, regex,
                            code));
                    expectTerminal = false;
                } else {
                    genericParseError(tokenizer);
                }
            } else {
                if (ttype == ',') {
                    expectTerminal = true;
                } else if (ttype == '}') {
                    break;
                } else {
                    genericParseError(tokenizer);
                }
            }
        }

        if (tokens.size() == 0) {
            throw new ConfigException("No tokens defined");
        }
    }

    /**
     * Parses a semicolon-terminated list of comma-separated strings.
     * 
     * @param tokenizer
     *            Tokenizer
     * @return ArrayList von Strings
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private ArrayList<String> parseList(StreamTokenizer tokenizer)
            throws IOException, ConfigException {
        ArrayList<String> list = new ArrayList<String>();
        int ttype;

        while (true) {
            ttype = tokenizer.nextToken();
            if (ttype == StreamTokenizer.TT_WORD) {
                list.add(tokenizer.sval);
                ttype = tokenizer.nextToken();
                if (ttype == ',') {
                    continue;
                } else if (ttype == ';') {
                    break;
                } else {
                    genericParseError(tokenizer);
                }
            } else {
                genericParseError(tokenizer);
            }
        }

        return list;
    }

    /**
     * Parses a semicolon-terminated string.
     * 
     * @param tokenizer
     *            Tokenizer
     * @return String
     * @throws IOException
     *             Thrown if the grammar file can't be read
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private String parseString(StreamTokenizer tokenizer) throws IOException,
            ConfigException {
        String s = null;

        if (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
            s = tokenizer.sval;
        } else {
            genericParseError(tokenizer);
        }

        if (tokenizer.nextToken() != ';') {
            genericParseError(tokenizer);
        }

        return s;
    }

    /**
     * Searches the rule which accepts the grammar.
     * 
     * @param lhs
     *            Name of the rule's left non-terminal
     * @return A rule
     * @throws ConfigException
     *             Thrown if there is an error in the grammar
     */
    private Rule findAcceptRule(String lhs) throws ConfigException {
        Rule found = null;

        for (Rule r : grammar) {
            if (r.getLhs().getName().equals(lhs)) {
                if (found != null) {
                    throw new ConfigException(
                            "Configuration error: multiple matching rules for accept state "
                                    + lhs);
                }
                found = r;
            }
        }
        return found;
    }

    /**
     * Returns the grammar.
     * 
     * @return grammar
     */
    public ArrayList<Rule> getGrammar() {
       return grammar;
    }

    /**
     * Returns a {@code HashMap} of terminal symbols.
     * 
     * @return terminal symbols
     */
    public HashMap<String, Terminal> getTerminals() {
       return tokens;
    }

    /**
     * Returns a {@code HashMap} of non-terminal symbols.
     * 
     * @return Non-terminal symbols
     */
    public HashMap<String, NonTerminal> getNonTerminals() {
       return nonTerminals;
    }

    /**
     * Returns a list of classes to be imported.
     * 
     * @return Classes to be imported
     */
    public ArrayList<String> getImports() {
       return imports;
    }

    /**
     * Returns the parser's class name.
     * 
     * @return class name
     */
    public String getParserClass() {
        return parserClass;
    }

    /**
     * Returns the lexer's class name.
     * 
     * @return class name
     */
    public String getLexerClass() {
        return lexerClass;
    }

    /**
     * Returns the parsers package name.
     * 
     * @return package name
     */
    public String getParserPackage() {
        return parserPackage;
    }

    /**
     * Returns the name of the enum which defines the terminals.
     * 
     * @return Class name
     */
    public String getTokenEnum() {
        return tokenEnum;
    }

    /**
     * Throws a generic exception after a parse error.
     * 
     * @param tokenizer
     *            Tokenizer
     * @throws ConfigException
     */
    private void genericParseError(StreamTokenizer tokenizer)
            throws ConfigException {
        throw new ConfigException("Parse error: unexpected token on line "
                + tokenizer.lineno()
                + "'"
                + (tokenizer.ttype == StreamTokenizer.TT_WORD ? tokenizer.sval
                        : (char) tokenizer.ttype) + "'");
    }

}
