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
 * YAJPG - Yet another Java Parser Generator.
 * <p>
 * Generates a parser class based on a grammar.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import ch.eskaton.commons.StringUtils;

public class Generator {

	private String gotoTableVar = "$GOTO_TABLE$";

	private String actionTableVar = "$ACTION_TABLE$";

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

	/** Table to process terminals */
	private Action actionTable[][];

	/** Table to process non-terminals */
	private Action gotoTable[][];

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
	 * Main method to generate the parser.
	 * 
	 * @param sourcePath
	 *            Path for generated code
	 * @throws IOException
	 * @throws ConfigException
	 */
	public void run(String sourcePath) throws IOException, ConfigException {
		StringBuilder parserFile = new StringBuilder(200);

		parserFile.append(sourcePath).append("/")
				.append(config.getParserPackage().replace('.', '/'));

		File path = new File(parserFile.toString());

		if (!path.exists() && !path.mkdirs()) {
			throw new IOException("Couldn't create directory " + sourcePath);
		}

		parserFile.append("/").append(config.getParserClass()).append(".java");

		prepareParserStates();
		buildParseTables();
		generateParser(parserFile.toString());
	}

	/**
	 * Prepares states for the parser automaton.
	 */
	private void prepareParserStates() {
		Map<RulePositionGroup, Action> actions = new HashMap<RulePositionGroup, Action>();

		/* Create initial state */
		State state = new State(maxState++);
		states.add(state);

		/**
		 * Add rules for all accepted grammars to initial state.
		 */
		for (Rule r : getAcceptRules()) {
			state.addRulePosition(new RulePosition(0, r));
		}

		/**
		 * Iterates over states to add all necessary rules and determine an
		 * action per rule group, thereby new states occur. The loop terminates
		 * if no new states occur and all rules have actions associated.
		 */
		for (int i = 0; i < states.size(); i++) {
			state = states.get(i);
			addNecessaryRulesToState(state);

			for (RulePositionGroup rpg : state.getRuleGroups()) {
				processRuleGroup(rpg, actions);
			}
		}
	}

	/**
	 * Processes a group of rules. All rules expect the same input symbol have
	 * the same action associated and lead to the same state.
	 * 
	 * @param group
	 *            Group of rules
	 * @param actions
	 *            Actions per group
	 */
	private void processRuleGroup(RulePositionGroup group,
			Map<RulePositionGroup, Action> actions) {

		if (actions.containsKey(group)) {
			/* We already know the action for this group */
			Action action = actions.get(group);
			group.setAction(action);
		} else {
			Action action;

			if (group.hasMoreSymbols()) {
				Symbol currentSymbol = group.getCurrentSymbol();
				State newState = new State(maxState);
				states.add(newState);

				if (currentSymbol instanceof Terminal) {
					action = new Action(Action.ActionType.Shift, maxState);
				} else {
					action = new Action(Action.ActionType.Goto, maxState);
				}

				group.setAction(action);

				actions.put(group, action);

				/**
				 * Add the rules of this group to the next state and advance
				 * each rule for one symbol.
				 */
				for (RulePosition rp : group.getGroup()) {
					newState.addRulePosition(rp.getNext());
				}

				maxState++;
			} else {
				/**
				 * If a group has no more symbols, it must consist of a single
				 * rule.
				 */
				RulePosition rp = group.getGroup().iterator().next();

				if (rp.getRule().isAccept()) {
					/* Accept */
					rp.setAction(new Action(Action.ActionType.Accept, rp
							.getRule().getRuleNumber()));
				} else {
					/* Reduction */
					rp.setAction(new Action(Action.ActionType.Reduce, rp
							.getRule().getRuleNumber()));
				}
			}

		}
	}

	/**
	 * Adds all rules to {@code state} that produce the next expected symbol.
	 * 
	 * @param state
	 *            A state
	 */
	private void addNecessaryRulesToState(State state) {
		List<Rule> rules = new ArrayList<Rule>();

		for (RulePosition rp : state.getRules()) {
			if (rp.hasMoreSymbols()) {
				rules.addAll(getAllRulesForSymbol(rp.getCurrentSymbol()
						.getName()));
			}
		}

		for (int i = 0; i < rules.size(); i++) {
			state.addRulePosition(new RulePosition(0, rules.get(i)));
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
		String actionTable = getActionTable();
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
					+ StringUtils.join(config.getImports(), ";\nimport "));
		}

		index = sb.indexOf(packageVar);
		sb.replace(index, index + packageVar.length(),
				config.getParserPackage());

		while ((index = sb.indexOf(classVar)) != -1) {
			sb.replace(index, index + classVar.length(),
					config.getParserClass());
		}

		String tokenEnum = config.getTokenEnum();

		index = sb.indexOf(tokenImportVar);
		sb.replace(index, index + tokenImportVar.length(), tokenEnum);

		index = sb.indexOf(gotoTableVar);
		sb.replace(index, index + gotoTableVar.length(), gotoTable);

		index = sb.indexOf(actionTableVar);
		sb.replace(index, index + actionTableVar.length(), actionTable);

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
	 * @return Code
	 */
	private String getTerminals() {
		HashMap<String, Terminal> terminals = config.getTerminals();
		StringBuilder sb = new StringBuilder(500);
		String[] s = new String[terminals.size()];

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
			sb.append(getTerminalEnum() + "." + s[i]);
		}

		sb.append("\n\t};");

		return sb.toString();
	}

	/**
	 * Returns the name of the enum which defines the terminals.
	 * 
	 * @return Name of the terminal enum
	 */
	private String getTerminalEnum() {
		return config.getTokenEnum().substring(
				config.getTokenEnum().lastIndexOf(".") + 1);
	}

	/**
	 * Generates code for an array containing non-terminal names.
	 * 
	 * @return Code
	 */
	private String getTerminalNames() {
		HashMap<String, Terminal> terminals = config.getTerminals();
		StringBuilder sb = new StringBuilder(500);
		String[] s = new String[terminals.size()];

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
			sb.append("\"" + s[i] + "\"");
		}

		sb.append("\n\t};");

		return sb.toString();
	}

	/**
	 * Generates code for the non-terminal array.
	 * 
	 * @return Code
	 */
	private String getNonTerminals() {
		HashMap<String, NonTerminal> nonTerminals = config.getNonTerminals();
		StringBuilder sb = new StringBuilder(500);
		String[] s = new String[nonTerminals.size()];

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
			sb.append("\"" + s[i] + "\"");
		}

		sb.append("\n\t};\n");

		return sb.toString();
	}

	/**
	 * Generates code for the action table.
	 * 
	 * @return Code
	 */
	private String getActionTable() {
		HashMap<String, Terminal> terminals = config.getTerminals();
		StringBuilder sb = new StringBuilder(500);
		int rows = states.size();
		int cols = terminals.size();

		sb.append("private final int[][] actionTable = {\n");

		for (int r = 0; r < rows; r++) {
			sb.append("\t\t{ ");
			for (int c = 0; c < cols; c++) {
				Action a = actionTable[r][c];
				if (c > 0) {
					sb.append(", ");
				}
				sb.append((a.getIndex() << 3) + a.getType().ordinal());
			}
			if (r < rows - 1) {
				sb.append(" }, \n");
			} else {
				sb.append(" }\n");
			}
		}

		sb.append("\t};\n");

		return sb.toString();
	}

	/**
	 * Generates code for the goto table.
	 * 
	 * @return Code
	 */
	private String getGotoTable() {
		HashMap<String, NonTerminal> nonTerminals = config.getNonTerminals();
		StringBuilder sb = new StringBuilder(500);
		int rows = states.size();
		int cols = nonTerminals.size();

		sb.append("private final int[][] gotoTable = {\n");

		for (int r = 0; r < rows; r++) {
			sb.append(" \t\t{ ");
			for (int c = 0; c < cols; c++) {
				Action a = gotoTable[r][c];
				if (c > 0) {
					sb.append(", ");
				}
				sb.append((a.getIndex() << 3) + a.getType().ordinal());
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
	 * Generates code to reduce rules.
	 * 
	 * @return Code
	 * @throws ConfigException
	 */
	private String getRuleSwitch() throws ConfigException {
		StringBuilder sb = new StringBuilder(1000);
		int varCount = 1;

		sb.append("switch(actionTable[currentState.getState()][actionColumn]>>3) {\n");

		for (int i = 0; i < grammar.size(); i++) {
			Rule r = grammar.get(i);
			int c = r.getRhsCount();
			String[] variables = new String[c];

			sb.append("\t\t\t\t\t\tcase " + i + ":\n");

			for (int j = 1; j <= c; j++) {
				Symbol symbol = r.getRhs(j - 1);
				String symbolClass = (symbol instanceof Terminal ? "Token"
						: ((NonTerminal) symbol).getClassName());
				String varName = "n" + varCount++;
				variables[j - 1] = varName;
				sb.append("\t\t\t\t\t\t\t" + symbolClass + " " + varName
						+ " = ((" + symbolClass + ")ps[" + (j - 1)
						+ "].getSymbol());\n");
			}

			sb.append("\t\t\t\t\t\t\t" + r.getCode("node", variables) + "\n");
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
		actionTable = new Action[rows][actionCols];
		gotoTable = new Action[rows][gotoCols];
		Action reductionAction = null;

		for (int i = 0; i < states.size(); i++) {
			State s = states.get(i);

			for (RulePosition rp : s.getRules()) {
				Action action = rp.getAction();

				if (action.getType() == Action.ActionType.Goto) {
					int symbolPos = nonTerminals.get(
							rp.getCurrentSymbol().getName())
							.getNonTerminalNumber();
					gotoTable[i][symbolPos] = action;
				} else if (action.getType() == Action.ActionType.Reduce) {
					reductionAction = action;
				} else {
					int symbolPos = terminals.get(
							rp.getCurrentSymbol().getName())
							.getTerminalNumber();
					actionTable[i][symbolPos] = action;
				}
			}

			/**
			 * On a reduce action all undefined events must lead to the
			 * reduction.
			 */
			if (reductionAction != null) {
				for (int c = 0; c < actionCols; c++) {
					if (actionTable[i][c] == null) {
						actionTable[i][c] = reductionAction;
					}
				}
				reductionAction = null;
			}

			/**
			 * Set all undefined table cells to the error action.
			 */
			for (int c = 0; c < actionCols; c++) {
				if (actionTable[i][c] == null) {
					actionTable[i][c] = new Action(Action.ActionType.Error, -1);
				}
			}

			for (int c = 0; c < gotoCols; c++) {
				if (gotoTable[i][c] == null) {
					gotoTable[i][c] = new Action(Action.ActionType.Error, -1);
				}
			}
		}

	}

	/**
	 * Returns a list of rules that lead to an accept state.
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
		System.out.println("\t-v");
		System.out.println("\t-e");
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
		boolean errors = false;

		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("-") || args[i].length() != 2) {
				printUsage();
			}
			if (args[i].charAt(1) == 'v') {
				verbose = true;
			} else if (args[i].charAt(1) == 'e') {
				errors = true;
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
			g.run(buildDir);

			if (verbose) {
				g.printStates();
			}

		} catch (IOException e) {
			if (errors) {
				System.err.println("I/O exception encountered: ");
				e.printStackTrace(System.err);
			} else {
				System.err.println("I/O exception encountered: "
						+ e.getMessage());
			}
			System.exit(1);
		} catch (ConfigException e) {
			if (errors) {
				System.err.println("Configuration error: ");
				e.printStackTrace(System.err);
			} else {
				System.err.println("Configuration error: " + e.getMessage());
			}
			System.exit(1);
		}
	}
}
