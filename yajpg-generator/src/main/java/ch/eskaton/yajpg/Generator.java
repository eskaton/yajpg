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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import ch.eskaton.commons.utils.CollectionUtils;
import ch.eskaton.regex.fsm.RegexCharacterEvent;
import ch.eskaton.regex.fsm.RegexEvent;
import ch.eskaton.regex.fsm.RegexState;
import ch.eskaton.regex.fsm.RegexStateMachine;
import ch.eskaton.regex.parser.RegexCharacterRange;
import ch.eskaton.regex.parser.RegexCompiler;
import ch.eskaton.regex.parser.RegexLexer;
import ch.eskaton.regex.parser.RegexNode;
import ch.eskaton.regex.parser.RegexParser;
import ch.eskaton.yajpg.api.ParseException;
import ch.eskaton.yajpg.api.Token;

/**
 * YAJPG - Yet another Java Parser Generator.
 * <p>
 * Generates a parser class and optionally also a lexer class based
 * on a grammar defined in a configuration file.
 */
public class Generator {

    private static final int LINE_LEN = 60;

    private String stateTableVar = "$STATE_TABLE$";

    private String tokenListVar = "$TOKEN_LIST$";

    private String eventMappingVar = "$EVENT_MAPPING$";

    private String initialStateVar = "$INITIAL_STATE$";

    private String tokenHandlerVar = "$TOKEN_HANDLER$";

    private String gotoTableVar = "$GOTO_TABLE$";

    private String gotoTableIndexVar = "$GOTO_TABLE_INDEX$";

    private String actionTableVar = "$ACTION_TABLE$";

    private String actionTableIndexVar = "$ACTION_TABLE_INDEX$";

    private String terminalNamesVar = "$TERMINALS_NAMES$";

    private String terminalsVar = "$TERMINALS$";

    private String nonTerminalsVar = "$NON_TERMINALS$";

    private String ruleTableVar = "$RULE_TABLE$";

    private String ruleSwitchVar = "$RULE_SWITCH$";

    private String tokenImportVar = "$TOKEN_IMPORT$";

    private String importsVar = "$IMPORTS$";

    private String classVar = "$CLASS$";

    private String packageVar = "$PACKAGE$";

    /** Id of next state */
    private int maxState = 0;

    /** Configuration of parser */
    private Config config;

    /** Table with states for the lexer */
    private int lexerStates[][];

    /** List to assign lexer states to tokens */
    private String tokenList[];

    /** Initial state of lexer */
    private int initialState;

    /** Mapping of character events to event numbers */
    private Map<RegexCharacterRange, Integer> eventMap;

    /** Table to process terminals */
    private Action actionTable[][];

    /** Indices to access actionTable */
    private int actionTableIndex[];

    /** Table to process non-terminals */
    private Action gotoTable[][];

    /** Indices to access gotoTable */
    private int gotoTableIndex[];

    /** States */
    private List<State> states = new ArrayList<State>();;

    /** List of grammar rules */
    private List<Rule> grammar;

    /**
     * Constructor.
     * 
     * @param configFile
     *            Name of the configuration file
     * @throws IOException
     * @throws ConfigException
     */
    public Generator(String configFile) throws IOException, ConfigException {
        config = new Config(configFile);
        grammar = config.getGrammar();
        NonTerminal.initialize();
        Terminal.initialize();
        Rule.initialize();
    }

    /**
     * Runs the generator.
     * 
     * @param sourcePath
     *           Path for generated code
     * @param generateLexer
     *           Generate a lexer?
     * @throws IOException
     * @throws ConfigException
     */
    public void run(String sourcePath, boolean generateLexer)
            throws IOException, ConfigException {

        StringBuilder parserFile = new StringBuilder(200);

        parserFile.append(sourcePath).append("/").append(
                config.getParserPackage().replace('.', '/'));

        File path = new File(parserFile.toString());

        if (!path.exists() && !path.mkdirs()) {
            throw new IOException("Couldn't create directory " + sourcePath);
        }

        parserFile.append("/").append(config.getParserClass()).append(".java");

        if (generateLexer) {
            StringBuilder lexerFile = new StringBuilder(200);
            lexerFile.append(sourcePath).append("/").append(
                    config.getParserPackage().replace('.', '/')).append("/")
                    .append(config.getLexerClass()).append(".java");
            buildLexerTables();
            generateLexer(lexerFile.toString());
        }
        prepareParserStates();
        buildParseTables();
        generateParser(parserFile.toString());
    }

    private void generateLexer(String outFile) throws IOException,
            ConfigException {
        InputStreamReader isr = new InputStreamReader(getClass()
                .getClassLoader().getResourceAsStream("lexer.tmpl"));
        BufferedReader br = new BufferedReader(isr);
        FileOutputStream fos = new FileOutputStream(outFile);
        PrintWriter pw = new PrintWriter(fos);
        StringBuilder sb = new StringBuilder(10000);
        String stateTable = getStateTable();
        String tokenList = getTokenList();
        String eventMapping = getEventMapping();
        String terminals = getTerminalsEnum();
        String initialState = getInitialState();
        String tokenHandler = getTokenHandler();
        String lineSep = System.getProperty("line.separator");
        String line;
        String template;
        int index;

        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append(lineSep);
        }

        List<String> imports = config.getImports();
        index = sb.indexOf(importsVar);

        if (imports == null) {
            sb.replace(index, index + importsVar.length() + 1, "");
        } else {
            sb.replace(index, index + importsVar.length(), "import "
                    + CollectionUtils.join(config.getImports(), ";\nimport "));
        }

        index = sb.indexOf(packageVar);
        sb.replace(index, index + packageVar.length(), config
                .getParserPackage());

        while ((index = sb.indexOf(classVar)) != -1) {
            sb
                    .replace(index, index + classVar.length(), config
                            .getLexerClass());
        }

        index = sb.indexOf(stateTableVar);
        sb.replace(index, index + stateTableVar.length(), stateTable);

        index = sb.indexOf(tokenListVar);
        sb.replace(index, index + tokenListVar.length(), tokenList);

        index = sb.indexOf(terminalsVar);
        sb.replace(index, index + terminalsVar.length(), terminals);

        index = sb.indexOf(eventMappingVar);
        sb.replace(index, index + eventMappingVar.length(), eventMapping);

        index = sb.indexOf(initialStateVar);
        sb.replace(index, index + initialStateVar.length(), initialState);

        index = sb.indexOf(tokenHandlerVar);
        sb.replace(index, index + tokenHandlerVar.length(), tokenHandler);

        template = sb.toString();
        pw.print(template);
        pw.flush();
    }

    /**
     * Prepares the tables for the lexer's automaton. 
     * 
     * @throws IOException
     * @throws ConfigException
     */
    private void buildLexerTables() throws ConfigException, IOException {
        Map<String, Terminal> terminals = config.getTerminals();
        Set<String> keys = terminals.keySet();
        List<RegexStateMachine> rsmList = new ArrayList<RegexStateMachine>();

        for (String key : keys) {
            if (key.equals(Config.EOF)) {
                continue;
            }
            Terminal terminal = terminals.get(key);
            try {
                rsmList.add(new RegexCompiler().compile(
                        (RegexNode) new RegexParser(new RegexLexer(terminal
                                .getRegex())).parse(), terminal));
            } catch (ParseException e) {
                throw new ConfigException(
                        "invalid regular expression for terminal: "
                                + terminal.getName(), e);
            }
        }

        RegexStateMachine rsm = new RegexCompiler().combine(rsmList);
        lexerStates = rsm.getStateTable();

        initialState = rsm.getStateNumber(rsm.getInitialState());

        Set<RegexState> allStates = rsm.getStates();
        tokenList = new String[allStates.size()];

        for (int i = 0; i < tokenList.length; i++) {
            tokenList[i] = null;
        }

        for (RegexState state : rsm.getFinalStates()) {
            Terminal terminal = (Terminal) state.getObject();
            tokenList[rsm.getStateNumber(state)] = terminal == null ? null
                    : terminal.getName();
        }

        Set<RegexEvent> events = rsm.getEvents();
        eventMap = new HashMap<RegexCharacterRange, Integer>();

        for (RegexEvent event : events) {
            Set<RegexCharacterRange> chars = ((RegexCharacterEvent) event)
                    .getCharClass().getCharacterRanges();
            for (RegexCharacterRange range : chars) {
                eventMap.put(range, rsm.getEventNumber(event));
            }
        }

    }

    /**
     * Prepares the states for the parser's automaton.
     */
    private void prepareParserStates() {
        Map<ItemSet, Action> actions = new HashMap<ItemSet, Action>();

        /* Create the initial state */
        State state = new State(maxState++);
        states.add(state);

        /*
         * Add the rules for all accepted grammars.
         */
        for (Rule r : getAcceptRules()) {
            state.addItem(new Item(0, r));
        }

        /*
         * Iterate over all states, add all necessary items to the states and
         * determine the action to be executed for each item set. The loop
         * terminates, if no more states are created and all items are assigned
         * to actions.
         */
        for (int i = 0; i < states.size(); i++) {
            state = states.get(i);
            addNecessaryItemsToState(state);

            for (ItemSet itemSet : state.getItemSets()) {
                processItemSet(itemSet, actions);
            }
        }
    }

    /**
     * Processes a set of items. In a set all items have the same next symbol,
     * therefore they all execute the same action and lead to the same state.
     * 
     * @@param itemSet
     *            Set of items
     * @@param actions
     *            Actions per item set
     */
    private void processItemSet(ItemSet itemSet, Map<ItemSet, Action> actions) {

        if (actions.containsKey(itemSet)) {
            /* We already know the action for this item set */
            Action action = actions.get(itemSet);
            itemSet.setAction(action);
        } else {
            Action action;

            if (itemSet.hasMoreSymbols()) {
                Symbol currentSymbol = itemSet.getCurrentSymbol();
                State newState = new State(maxState);
                states.add(newState);

                if (currentSymbol instanceof Terminal) {
                    action = new Action(Action.ActionType.Shift, maxState);
                } else {
                    action = new Action(Action.ActionType.Goto, maxState);
                }

                itemSet.setAction(action);

                /* Save the action for this item item set for later use */
                actions.put(itemSet, action);

                /* Add the items of this set to the following state and 
                 * advance one symbol in each item
                 */
                for (Item item : itemSet.getSet()) {
                    newState.addItem(item.getNext());
                }

                maxState++;
            } else {
                /* If a set has no symbols it must be a single item */
                Item item = itemSet.getSet().iterator().next();

                if (item.getRule().isAccept()) {
                    /* Accept */
                    item.setAction(new Action(Action.ActionType.Accept, item
                            .getRule().getRuleNumber()));
                } else {
                    /* Reduction */
                    item.setAction(new Action(Action.ActionType.Reduce, item
                            .getRule().getRuleNumber()));
                }
            }

        }
    }

    /**
     * Adds all items to {@code state} that produce the next expected symbol.
     * 
     * @param state
     *            A state
     */
    private void addNecessaryItemsToState(State state) {
        List<Rule> rules = new ArrayList<Rule>();

        for (Item item : state.getItems()) {
            if (item.hasMoreSymbols()) {
                rules.addAll(getAllRulesForSymbol(item.getCurrentSymbol()
                        .getName()));
            }
        }

        for (int i = 0; i < rules.size(); i++) {
            state.addItem(new Item(0, rules.get(i)));
        }
    }

    /**
     * Generates Java code for the parser.
     * 
     * @param outFile
     *            Name of the Java source file
     * @throws ConfigException
     * @throws IOException
     */
    private void generateParser(String outFile) throws ConfigException,
            IOException {
        InputStreamReader isr = new InputStreamReader(getClass()
                .getClassLoader().getResourceAsStream("parser.tmpl"));
        BufferedReader br = new BufferedReader(isr);
        FileOutputStream fos = new FileOutputStream(outFile);
        PrintWriter pw = new PrintWriter(fos);
        StringBuilder sb = new StringBuilder(10000);
        String lineSep = System.getProperty("line.separator");
        String gotoTable = getGotoTable();
        String gotoTableIndex = getGotoTableIndex();
        String actionTable = getActionTable();
        String actionTableIndex = getActionTableIndex();
        String ruleTable = getRuleTable();
        String ruleSwitch = getRuleSwitch();
        String terminals = getTerminals();
        String terminalNames = getTerminalNames();
        String nonTerminals = getNonTerminals();
        String line;
        String template;
        int index;

        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append(lineSep);
        }

        List<String> imports = config.getImports();
        index = sb.indexOf(importsVar);

        if (imports == null) {
            sb.replace(index, index + importsVar.length() + 1, "");
        } else {
            sb.replace(index, index + importsVar.length(), "import "
                    + CollectionUtils.join(config.getImports(), ";\nimport "));
        }

        index = sb.indexOf(packageVar);
        sb.replace(index, index + packageVar.length(), config
                .getParserPackage());

        while ((index = sb.indexOf(classVar)) != -1) {
            sb.replace(index, index + classVar.length(), config
                    .getParserClass());
        }

        String tokenEnum;

        if (config.getLexerClass() == null) {
            tokenEnum = config.getTokenEnum();
        } else {
            tokenEnum = config.getParserPackage() + "."
                    + config.getLexerClass() + ".Terminals";
        }

        index = sb.indexOf(tokenImportVar);
        sb.replace(index, index + tokenImportVar.length(), tokenEnum);

        index = sb.indexOf(gotoTableVar);
        sb.replace(index, index + gotoTableVar.length(), gotoTable);

        index = sb.indexOf(gotoTableIndexVar);
        sb.replace(index, index + gotoTableIndexVar.length(), gotoTableIndex);

        index = sb.indexOf(actionTableVar);
        sb.replace(index, index + actionTableVar.length(), actionTable);

        index = sb.indexOf(actionTableIndexVar);
        sb.replace(index, index + actionTableIndexVar.length(),
                actionTableIndex);

        index = sb.indexOf(ruleTableVar);
        sb.replace(index, index + ruleTableVar.length(), ruleTable);

        index = sb.indexOf(terminalsVar);
        sb.replace(index, index + terminalsVar.length(), terminals);

        index = sb.indexOf(terminalNamesVar);
        sb.replace(index, index + terminalNamesVar.length(), terminalNames);

        index = sb.indexOf(nonTerminalsVar);
        sb.replace(index, index + nonTerminalsVar.length(), nonTerminals);

        index = sb.indexOf(ruleSwitchVar);
        sb.replace(index, index + ruleSwitchVar.length(), ruleSwitch);

        template = sb.toString();
        pw.print(template);
        pw.flush();
    }

    /**
     * Generates the Java code for the lexer's token handler.
     * 
     * @return A code segment
     */
    private String getTokenHandler() {
        StringBuilder sb = new StringBuilder(1000);
        boolean firstIf = true;

        sb.append("\t\tcurrentToken = token;\n\n");

        for (Entry<String, Terminal> e : config.getTerminals().entrySet()) {
            Terminal terminal = e.getValue();
            if (!"".equals(terminal.getCode())) {
                if (firstIf) {
                    firstIf = false;
                } else {
                    sb.append(" else ");
                }
                sb.append("if( token.getType() == ").append(getTerminalEnum())
                        .append(".").append(terminal.getName()).append(
                                ".ordinal() )").append("{\n");
                sb.append("\t\t\t").append(terminal.getCode()).append("\n");
                sb.append("\t\t\treturn;\n");
                sb.append("\t\t}\n");
            }
        }

        return sb.toString();
    }

    /**
     * Generates the Java code for the lexer's initial state.
     * 
     * @return A code segment
     */
    private String getInitialState() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("private int initialState = ").append(initialState).append(
                ";");
        return sb.toString();
    }

    /**
     * Generates code for the rule table.
     * 
     * @return Java code
     */
    private String getRuleTable() {
        Rule[] rules = new Rule[grammar.size()];
        StringBuilder sb = new StringBuilder(500);

        for (int i = 0; i < grammar.size(); i++) {
            rules[grammar.get(i).getRuleNumber()] = grammar.get(i);
        }

        sb.append("private final String[][] ruleTable = {\n");

        for (int i = 0; i < rules.length; i++) {
            sb.append("\t\t{ ");

            sb.append("\"" + rules[i].getLhs() + "\", ");

            for (int j = 0; j <= rules[i].getLastRhsPosition(); j++) {
                sb.append("\"" + rules[i].getRhs(j) + "\"");
                if (j < rules[i].getLastRhsPosition()) {
                    sb.append(", ");
                }
            }

            if (i < rules.length - 1) {
                sb.append(" }, \n");
            } else {
                sb.append(" }\n");
            }
        }

        sb.append("\t};\n");

        return sb.toString();
    }

    /**
     * Generates code for the terminal table.
     * 
     * @return Java code
     */
    private String getTerminals() {
        HashMap<String, Terminal> terminals = config.getTerminals();
        StringBuilder sb = new StringBuilder(500);
        String[] s = new String[terminals.size()];
        int len = LINE_LEN;

        for (Entry<String, Terminal> e : terminals.entrySet()) {
            Terminal t = e.getValue();
            s[t.getTerminalNumber()] = t.getName();
        }

        sb.append("private final " + getTerminalEnum() + "[] actions = {\n");

        for (int i = 0; i < s.length; i++) {

            if (i > 0) {
                sb.append(", ");
            } else {
                sb.append("\t\t");
            }

            if (len < 0) {
                len = LINE_LEN;
                sb.append("\n\t\t");
            }

            String terminal = getTerminalEnum() + "." + s[i];
            sb.append(terminal);

            len -= terminal.length() + 2;
        }

        sb.append("\n\t};");

        return sb.toString();
    }

    /**
     * Returns the name of the enum that defines terminals.
     * 
     * @return Name of the terminal enum
     */
    private String getTerminalEnum() {
        if (config.getLexerClass() == null) {
            return config.getTokenEnum().substring(
                    config.getTokenEnum().lastIndexOf(".") + 1);
        }
        return "Terminals";
    }

    /**
     * Generates code for an array containing non-terminal names.
     * 
     * @return Java code
     */
    private String getTerminalNames() {
        HashMap<String, Terminal> terminals = config.getTerminals();
        StringBuilder sb = new StringBuilder(500);
        String[] s = new String[terminals.size()];
        int len = LINE_LEN;

        for (Entry<String, Terminal> e : terminals.entrySet()) {
            Terminal t = e.getValue();
            s[t.getTerminalNumber()] = t.getName();
        }

        sb.append("private final String[] actionNames = {\n");

        for (int i = 0; i < s.length; i++) {
            if (i > 0) {
                sb.append(", ");
            } else {
                sb.append("\t\t");
            }

            if (len < 0) {
                sb.append("\n\t\t");
                len = LINE_LEN;
            }

            sb.append("\"" + s[i] + "\"");

            len -= s[i].length() + 2;
        }

        sb.append("\n\t};");

        return sb.toString();
    }

    /**
     * Generates code for the terminal enum. 
     * 
     * @return Java code
     */
    private String getTerminalsEnum() {
        HashMap<String, Terminal> terminals = config.getTerminals();
        StringBuilder sb = new StringBuilder(500);
        String[] s = new String[terminals.size()];
        int len = LINE_LEN;

        for (Entry<String, Terminal> e : terminals.entrySet()) {
            Terminal nt = e.getValue();
            s[nt.getTerminalNumber()] = nt.getName();
        }

        sb.append("public enum Terminals {\n");

        for (int i = 0; i < s.length; i++) {
            if (i > 0) {
                sb.append(", ");
            } else {
                sb.append("\t\t");
            }

            if (len < 0) {
                len = LINE_LEN;
                sb.append("\n\t\t");
            }

            sb.append(s[i]);
            len -= s[i].length() + 2;
        }

        sb.append("\n\t};\n");

        return sb.toString();
    }

    /**
     * Generates code for the non-terminal array.
     * 
     * @return Java code
     */
    private String getNonTerminals() {
        HashMap<String, NonTerminal> nonTerminals = config.getNonTerminals();
        StringBuilder sb = new StringBuilder(500);
        String[] s = new String[nonTerminals.size()];
        int len = LINE_LEN;

        for (Entry<String, NonTerminal> e : nonTerminals.entrySet()) {
            NonTerminal nt = e.getValue();
            s[nt.getNonTerminalNumber()] = nt.getName();
        }

        sb.append("private final String[] nonTerminals = {\n");

        for (int i = 0; i < s.length; i++) {
            if (i > 0) {
                sb.append(", ");
            } else {
                sb.append("\t\t");
            }

            if (len < 0) {
                len = LINE_LEN;
                sb.append("\n\t\t");
            }

            sb.append("\"" + s[i] + "\"");
            len -= s[i].length() + 2;
        }

        sb.append("\n\t};\n");

        return sb.toString();
    }

    /**
     * Generates code for the state table.
     * 
     * @return Java code
     */
    private String getStateTable() {
        StringBuilder sb = new StringBuilder(10000);
        int rows = lexerStates.length;

        sb.append("private final int[][] stateTable = {\n");

        for (int r = 0; r < rows; r++) {
            sb.append(" \t\t{ ");
            for (int c = 0; c < lexerStates[r].length; c++) {
                sb.append(lexerStates[r][c]);
                if (c < lexerStates[r].length - 1) {
                    sb.append(", ");
                }
            }
            if (r < rows - 1) {
                sb.append(" }, \n");
            } else {
                sb.append(" }\n");
            }
        }

        sb.append("\t};");

        return sb.toString();
    }

    /**
     * Generates code for a list to map states to tokens.
     * 
     * @return Java code
     */
    private String getTokenList() {
        StringBuilder sb = new StringBuilder(10000);
        String enumName = getTerminalEnum();
        int len = LINE_LEN;

        sb.append("public final " + enumName + "[] tokenList = {\n\t\t");

        for (int i = 0; i < tokenList.length; i++) {
            if (len < 0) {
                len = LINE_LEN;
                sb.append("\n\t\t");
            }

            String tokenName = tokenList[i] == null ? "null" : enumName + "."
                    + tokenList[i];
            sb.append(tokenName);

            len -= tokenName.length() + 2;

            if (i < tokenList.length - 1) {
                sb.append(", ");
            }
        }

        sb.append("\n\t};");

        return sb.toString();
    }

    /**
     * Generates code to map from character events to event numbers.
     * 
     * @return Java code
     */
    private String getEventMapping() {
        StringBuilder sb = new StringBuilder(1000);
        boolean firstIf = true;

        for (RegexCharacterRange key : eventMap.keySet()) {
            int eventNumber = eventMap.get(key);
            if (firstIf) {
                firstIf = false;
            } else {
                sb.append(" else ");
            }
            if (key.getFrom().equals(key.getTo())) {
                sb.append("if(c == ").append((int) key.getFrom().charAt(0))
                        .append(") {\n");
            } else {
                sb.append("if(c >= ").append((int) key.getFrom().charAt(0))
                        .append(" && c <= ")
                        .append((int) key.getTo().charAt(0)).append(") {\n");
            }
            sb.append("\t\t\treturn ").append(eventNumber).append(";\n");
            sb.append("\t\t}");
        }

        sb.append(" else {\n");
        sb.append("\t\t\treturn " + eventMap.size() + ";\n");
        sb.append("\t\t}\n");

        return sb.toString();
    }

    /**
     * Generates code for the action table.
     * 
     * @return Java code
     */
    private String getActionTable() {
        HashMap<String, Terminal> terminals = config.getTerminals();
        StringBuilder sb = new StringBuilder(500);
        int rows = actionTable.length;
        int cols = terminals.size();

        sb.append("return new int[][] {\n");

        for (int r = 0; r < rows; r++) {
            sb.append("\t\t\t{ ");
            for (int c = 0; c < cols; c++) {
                Action a = actionTable[r][c];
                if (c > 0) {
                    sb.append(", ");
                }
                sb.append(a.encode());
            }
            if (r < rows - 1) {
                sb.append(" }, \n");
            } else {
                sb.append(" }\n");
            }
        }

        sb.append("\t\t};\n");

        return sb.toString();
    }

    /**
     * Generates code for the action index array.
     * 
     * @return Java code
     */
    private String getActionTableIndex() {
        return getTableIndex(actionTableIndex);
    }

    /**
     * Generate code for the goto table.
     * 
     * @return Java code
     */
    private String getGotoTable() {
        HashMap<String, NonTerminal> nonTerminals = config.getNonTerminals();
        StringBuilder sb = new StringBuilder(500);
        int rows = gotoTable.length;
        int cols = nonTerminals.size();

        sb.append("return new int[][] {\n");

        for (int r = 0; r < rows; r++) {
            sb.append(" \t\t\t{ ");
            for (int c = 0; c < cols; c++) {
                Action a = gotoTable[r][c];
                if (c > 0) {
                    sb.append(", ");
                }
                sb.append(a.encode());
            }
            if (r < rows - 1) {
                sb.append(" }, \n");
            } else {
                sb.append(" }\n");
            }
        }

        sb.append("\t\t};");

        return sb.toString();
    }

    /**
     * Generates code for the goto index array.
     * 
     * @return Java code
     */
    private String getGotoTableIndex() {
        return getTableIndex(gotoTableIndex);
    }

    /**
     * Generates code for an index array.
     * 
     * @return Java code
     */
    private String getTableIndex(int[] table) {
        StringBuilder sb = new StringBuilder(500);
        int rows = table.length;

        sb.append("return new int[] {");

        for (int r = 0; r < rows; r++) {
            if (r > 0) {
                sb.append(", ");
            }
            if (r % 15 == 0) {
                sb.append("\n\t\t\t");
            }
            sb.append(table[r]);
        }

        sb.append("\n\t\t};");

        return sb.toString();
    }

    /**
     * Generates code to reduce rules.
     * 
     * @return Java code
     * @throws ConfigException
     */
    private String getRuleSwitch() throws ConfigException {
        StringBuilder sb = new StringBuilder(1000);
        int varCount = 1;

        sb.append("switch(action>>3) {\n");

        for (int i = 0; i < grammar.size(); i++) {
            Rule r = grammar.get(i);
            int c = r.getRhsCount();
            Set<Integer> usedVariables = r.getUsedVariables(c);
            String[] variables = new String[c];

            sb.append("\t\t\t\t\t\tcase ").append(i).append(":\n");
            sb.append("\t\t\t\t\t\t    //").append(r.toString()).append("\n");

            for (int j = 1; j <= c; j++) {
                if (!usedVariables.contains(j)) {
                    continue;
                }

                Symbol symbol = r.getRhs(j - 1);
                String symbolClass = (symbol instanceof Terminal ? Token.class
                        .getCanonicalName() : ((NonTerminal) symbol)
                        .getClassName());
                String varName = "n" + varCount++;
                variables[j - 1] = varName;
                sb.append("\t\t\t\t\t\t\t").append(symbolClass).append(" ")
                        .append(varName).append(" = ((").append(symbolClass)
                        .append(")ps[").append((j - 1)).append(
                                "].getSymbol());\n");
            }

            sb.append("\t\t\t\t\t\t\t").append(r.getCode("node", variables))
                    .append("\n");
            sb.append("\t\t\t\t\t\t\tbreak;\n");
        }

        sb.append("\t\t\t\t\t}");

        return sb.toString();
    }

    /**
     * Builds the action and goto table.
     */
    private void buildParseTables() {
        Map<String, Terminal> terminals = config.getTerminals();
        Map<String, NonTerminal> nonTerminals = config.getNonTerminals();
        int rows = states.size();
        int actionCols = terminals.size();
        int gotoCols = nonTerminals.size();
        Action gotoState[];
        Action actionState[];
        gotoTableIndex = new int[rows];
        actionTableIndex = new int[rows];
        List<Action[]> gotoList = new ArrayList<Action[]>();
        List<Action[]> actionList = new ArrayList<Action[]>();
        Set<Action> allActionsTheSame;

        for (int i = 0; i < states.size(); i++) {
            gotoState = new Action[gotoCols];
            actionState = new Action[actionCols];
            allActionsTheSame = new HashSet<Action>();
            State s = states.get(i);

            Action reductionAction = processItem(gotoState, actionState, s);

            for (Item item : s.getItems()) {
                allActionsTheSame.add(item.getAction());
            }

            /*
             * On a reduce action all undefined events must lead to the
             * reduction.
             */
            if (reductionAction != null) {
                for (int c = 0; c < actionCols; c++) {
                    if (actionState[c] == null) {
                        actionState[c] = reductionAction;
                        allActionsTheSame.add(reductionAction);
                    }
                }

                reductionAction = null;
            }

            /**
             * Set all undefined table cells to the error action.
             */
            for (int c = 0; c < actionCols; c++) {
                Action error = new Action(Action.ActionType.Error, -1);
                if (actionState[c] == null) {
                    actionState[c] = error;
                    allActionsTheSame.add(error);
                }
            }

            for (int c = 0; c < gotoCols; c++) {
                if (gotoState[c] == null) {
                    gotoState[c] = new Action(Action.ActionType.Error, -1);
                }
            }

            /*
             * Update index tables and compress goto and action tables. 
             */
            int actionIndex = lookup(actionList, actionState);

            if (actionIndex == -1) {
                if (allActionsTheSame.size() == 1) {
                    actionTableIndex[i] = (actionState[0].encode()) << 1;
                    actionTableIndex[i] |= 1;
                } else {
                    actionTableIndex[i] = actionList.size() << 1;
                    actionList.add(actionState);
                }
            } else {
                actionTableIndex[i] = actionIndex << 1;
            }

            int gotoIndex = lookup(gotoList, gotoState);

            if (gotoIndex == -1) {
                gotoTableIndex[i] = gotoList.size();
                gotoList.add(gotoState);
            } else {
                gotoTableIndex[i] = gotoIndex;
            }
        }

        gotoTable = gotoList.toArray(new Action[][] {});
        actionTable = actionList.toArray(new Action[][] {});
    }

    /**
     * @param gotoState
     * @param actionState
     * @param s
     * @return
     */
    private Action processItem(Action[] gotoState, Action[] actionState, State s) {
        Map<String, Terminal> terminals = config.getTerminals();
        Map<String, NonTerminal> nonTerminals = config.getNonTerminals();
        Action reductionAction = null;
        PrecedenceRule reducePr = null;
        final int SHIFT = 0x1;
        final int REDUCE = 0x2;
        int actions = 0;

        for (Item item : s.getItems()) {
            Action action = item.getAction();

            if (action.getType() == Action.ActionType.Goto) {
                int symbolPos = nonTerminals.get(
                        item.getCurrentSymbol().getName())
                        .getNonTerminalNumber();
                gotoState[symbolPos] = action;
            } else if (action.getType() == Action.ActionType.Reduce) {
                reductionAction = action;
                reducePr = item.getRule().getPrecedenceRule();
                actions |= REDUCE;
            } else {
                int symbolPos = terminals
                        .get(item.getCurrentSymbol().getName())
                        .getTerminalNumber();
                actionState[symbolPos] = action;
                actions |= SHIFT;
            }

        }

        if (actions == (REDUCE | SHIFT)) {
            System.err.println("shift/reduce conflict detected: ");
            System.err.println(s);

            Action error = new Action(Action.ActionType.Error, -1);

            for (Item item : s.getItems()) {
                Action action = item.getAction();

                if (action.getType() == Action.ActionType.Shift) {
                    PrecedenceRule shiftPr = item.getRule().getPrecedenceRule();

                    if (reducePr != null && shiftPr != null) {
                        int symbolPos = terminals.get(
                                item.getCurrentSymbol().getName())
                                .getTerminalNumber();
                        if (reducePr.getPrecedence() > shiftPr.getPrecedence()) {
                            actionState[symbolPos] = reductionAction;
                            item.setAction(reductionAction);
                        } else if (reducePr.getPrecedence() == shiftPr
                                .getPrecedence()) {
                            if (reducePr.getAssociativity() == Associativity.LEFTASSOC) {
                                actionState[symbolPos] = reductionAction;
                                item.setAction(reductionAction);
                            } else if (reducePr.getAssociativity() == Associativity.NONASSOC) {
                                actionState[symbolPos] = error;
                                item.setAction(error);
                            }
                        }
                    }
                }
            }

            System.err.println("solved as follows: ");
            System.err.println(s);
        }

        return reductionAction;
    }

    /**
     * Searches in a list of state for a state with the same actions.
     * 
     * @param list
     *            List with an array of actions per state
     * @param array
     *            An array of actions
     * @return Index of the matching state or -1 if none was found 
     */
    private int lookup(List<Action[]> list, Action[] array) {
        for (int i = 0; i < list.size(); i++) {
            if (Arrays.deepEquals(list.get(i), array)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns all rules which directly generate a start symbol.
     * 
     * @return List of rules
     */
    private List<Rule> getAcceptRules() {
        List<Rule> rules = new ArrayList<Rule>();

        for (Rule r : grammar) {
            if (r.isAccept()) {
                rules.add(r);
            }
        }

        return rules;
    }

    /**
     * Returns all rules which produce the symbol {@code sym} directly.
     * 
     * @param sym
     *            Symbol for which to search the producing rules
     * 
     * @return All rules which can produce {@code sym}
     */
    private List<Rule> getAllRulesForSymbol(String sym) {
        List<Rule> matchingRules;
        List<Rule> allRules = new ArrayList<Rule>();
        Stack<String> symbols = new Stack<String>();
        Set<String> alreadyKnownSymbols = new HashSet<String>();

        symbols.push(sym);

        /*
         * Search all rules which have sym on the left-hand side and collect the
         * first symbol on the right-hand side.
         */
        while (!symbols.empty()) {
            sym = symbols.pop();
            matchingRules = getRulesForSymbolAtLhs(sym);

            for (Rule rule : matchingRules) {
                sym = rule.getRhs(0).getName();
                if (!alreadyKnownSymbols.contains(sym)) {
                    symbols.push(sym);
                    alreadyKnownSymbols.add(sym);
                }
                allRules.add(rule);
            }
        }

        return allRules;
    }

    /**
     * Returns all rules, which produce the symbol designated by {@code sym}
     * 
     * @param sym
     *            Symbol on the left-hand side
     * @return Rules that produce {@code sym}
     */
    private List<Rule> getRulesForSymbolAtLhs(String sym) {
        List<Rule> matchingRules = new ArrayList<Rule>();

        for (Rule r : grammar) {
            if (r.getLhs().getName().equals(sym)) {
                matchingRules.add(r);
            }
        }

        return matchingRules;
    }

    /**
     * Prints states for debugging purposes.
     */
    public void printStates() {
        for (State s : states) {
            System.out.print(s);
        }
    }

    /**
     * Prints usage and exits.
     */
    public static void printUsage() {
        System.out.println("Usage: java Generator");
        System.out.println("\t-g <grammar-rules> (mandatory)");
        System.out.println("\t-b <build directory>");
        System.out.println("\t-l (generate a lexer class)");
        System.out.println("\t-v");
        System.exit(1);
    }

    /**
     * Entry point of the generator.
     */
    public static void main(String[] args) {
        Generator g;
        String grammar = null;
        String buildDir = null;
        boolean verbose = false;
        boolean generateLexer = false;

        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-") || args[i].length() != 2) {
                printUsage();
            }
            if (args[i].charAt(1) == 'v') {
                verbose = true;
            } else if (args[i].charAt(1) == 'l') {
                generateLexer = true;
            } else {
                if (i + 1 == args.length) {
                    printUsage();
                }
                switch (args[i].charAt(1)) {
                    case 'g':
                        grammar = args[++i];
                        if (!grammar.endsWith(".yajpg")) {
                            printUsage();
                        }
                        break;
                    case 'b':
                        buildDir = args[++i];
                        File dir = new File(buildDir);
                        if (!dir.exists() && !dir.isDirectory()) {
                            printUsage();
                        }
                        break;
                    default:
                        printUsage();
                }
            }
        }

        if (grammar == null || buildDir == null) {
            printUsage();
        }

        try {

            g = new Generator(grammar);
            g.run(buildDir, generateLexer);

            if (verbose) {
                g.printStates();
            }

        } catch (IOException e) {
            System.err.println("I/O exception encountered: " + e.getMessage());
            System.exit(1);
        } catch (ConfigException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
        }
    }

}
