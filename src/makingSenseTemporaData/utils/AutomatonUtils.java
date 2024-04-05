package makingSenseTemporaData.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.ltl2automaton.plugins.automaton.DFAOperations;
import org.processmining.ltl2automaton.plugins.automaton.DOTExporter;
import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.ltl2automaton.plugins.formula.DefaultParser;
import org.processmining.ltl2automaton.plugins.formula.Formula;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeLeaf;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeNode;
import org.processmining.ltl2automaton.plugins.formula.conjunction.DefaultTreeFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.GroupedTreeConjunction;
import org.processmining.ltl2automaton.plugins.formula.conjunction.TreeFactory;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;

import makingSenseTemporaData.MonitoringState;
import makingSenseTemporaData.model.DeclareConstraint;
import makingSenseTemporaData.model.DeclareTemplate;

public class AutomatonUtils {

	private static TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
	private static ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);

	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}
	
	// returns the number of accepted strings in a log by an executable automata
	public static int count_accepted_strings(ExecutableAutomaton automata, XLog log, Map<String, String> activityToEncoding) {
		int cnt = 0;
		for (XTrace xtrace : log) {
			automata.ini();
			for (int i = 0; i < xtrace.size(); i++) {
				String eventName = XConceptExtension.instance().extractName(xtrace.get(i));
				String encodedEventName = activityToEncoding.getOrDefault(eventName, "actx");
				automata.next(encodedEventName);

			}
			if (automata.currentState().isAccepting() == true) {
				// System.out.println(1);
				cnt = cnt + 1;
			}
			else {
				//System.out.println("------------------------------");
				//System.out.println(automata.currentState());
				//automata.ini();
				//System.out.println(automata.currentState());
				//System.out.println(xtrace[0]);
				//for (int i = 0; i < xtrace.size(); i++) {
					//String eventName = XConceptExtension.instance().extractName(xtrace.get(i));
					//String encodedEventName = activityToEncoding.getOrDefault(eventName, "actx");
					//automata.next(encodedEventName);
					//System.out.println(automata.currentState());
					//System.out.println(encodedEventName);
					//System.out.println(eventName);

				//}
			}
		}
		return cnt;
	}
	
	public static Map<String, String> mergeActivityEncodingMaps(Map<String, String> activityToEncoding_a, Map<String, String> activityToEncoding_b){
		Map<String, String> activityToEncodingMerged = new HashMap<String, String>();
		int cnt = 0;
		
		for (String key : activityToEncoding_a.keySet()) {
			if (!activityToEncodingMerged.containsKey(key)){
				activityToEncodingMerged.put(key, "act" + cnt);
				cnt++;
			}
		}
		for (String key : activityToEncoding_b.keySet()) {
			if (!activityToEncodingMerged.containsKey(key)){
				activityToEncodingMerged.put(key, "act" + cnt);
				cnt++;
			}
		}
		return activityToEncodingMerged;
	}
	
	public static void DFA2dot(DeterministicAutomaton automata, String automata_name) throws IOException {
		DOTExporter.exportToDot(automata, "Model Name", new FileWriter(automata_name + ".dot"));
	}
	
	// Checks whether the language accepted by the first automata is contained in the language accepted by the second automata
	public static boolean automataSubset(DeterministicAutomaton a, DeterministicAutomaton b) {
		DeterministicAutomaton negated_b = b.op.determinize().op.negate();
		DeterministicAutomaton intersetc_a_negated_b = a.op.intersect(negated_b);
		boolean res = false;
		return intersetc_a_negated_b.op.minimize().op.isEmpty();
	}
		
	// Checks whether two automata accept the same language
	public static boolean automataEquality(DeterministicAutomaton a, DeterministicAutomaton b) {
		boolean equality_result = automataSubset(a, b) && automataSubset(b, a);
		return equality_result;
	}

	//Creates an automaton for LTL formula
	public static DeterministicAutomaton createAutomatonForLtlFormula(String ltlFormula) throws SyntaxParserException {
		Formula parsedFormula = new DefaultParser(ltlFormula).parse();
		//System.out.println("Parsed formula: " + parsedFormula);
		GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
		return conjunction.getAutomaton().op.determinize().op.complete().op.minimize();
	}

	//Creates a map of the colours of each state in the global automaton
	public static Map<State, Map<DeclareConstraint, MonitoringState>> getGlobalAutomatonColours(Map<DeclareConstraint, ExecutableAutomaton> constraintAutomatonMap, ExecutableAutomaton globalAutomaton) {
		Map<State, Map<DeclareConstraint, MonitoringState>> globalAutomatonColours = new HashMap<State, Map<DeclareConstraint,MonitoringState>>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];

		//Just to make sure the initial states are correct
		for (ExecutableAutomaton executableAutomaton : constraintAutomatonMap.values()) {
			executableAutomaton.ini();
		}
		globalAutomaton.ini();

		for (State state : globalAutomaton.currentState()) { //There should always be exactly one initial state
			Stack<String> executedEncodings = new Stack<String>();
			colourGlobalAutomatonState(state, visited, constraintAutomatonMap, globalAutomatonColours, executedEncodings);
		}

		return globalAutomatonColours;
	}

	//For visiting the global automaton states recursively
	private static void colourGlobalAutomatonState(State state, boolean[] visited, Map<DeclareConstraint, ExecutableAutomaton> constraintAutomatonMap, Map<State, Map<DeclareConstraint, MonitoringState>> globalAutomatonColours, Stack<String> executedEncodings) {
		visited[state.getId()] = true;
		//System.out.println("\tGlobal state colours " + state.toString());
		HashMap<DeclareConstraint, MonitoringState> globalStateColours = new HashMap<DeclareConstraint, MonitoringState>();

		for (DeclareConstraint declareConstraint : constraintAutomatonMap.keySet()) {
			ExecutableAutomaton executableAutomaton = constraintAutomatonMap.get(declareConstraint);
			if (!executedEncodings.empty()) {
				executableAutomaton.ini();
				for (String executedEncoding : executedEncodings) {
					executableAutomaton.next(executedEncoding);
				}
			}
			MonitoringState monitoringState = getMonitoringState(executableAutomaton.currentState());
			globalStateColours.put(declareConstraint, monitoringState);
		}
		globalAutomatonColours.put(state, globalStateColours);

		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				executedEncodings.push(t.getPositiveLabel()); //Returns a label even if the transition is negative which is useful in this case
				colourGlobalAutomatonState(t.getTarget(), visited, constraintAutomatonMap, globalAutomatonColours, executedEncodings);
				executedEncodings.pop();
			}
		}
	}

	//Gets the monitoring state that corresponds to the current state of the automaton (referred to as truthValue in original implementation)
	public static MonitoringState getMonitoringState(PossibleNodes currentState) {
		MonitoringState monitoringState;

		if (currentState.isAccepting()) {
			monitoringState = MonitoringState.POSS_SAT;
			for (State state : currentState) {
				for (Transition t : state.getOutput()) {
					if (t.isAll()) {
						monitoringState = MonitoringState.SAT;
					}
				}
			}
		} else {
			monitoringState = MonitoringState.POSS_VIOL;
			for (State state : currentState) {
				if (!currentState.acceptingReachable(state)) {
					monitoringState = MonitoringState.VIOL;
				}
			}
		}

		return monitoringState;
	}

	//Gets the monitoring state that corresponds to the current state of the automaton (referred to as truthValue in original implementation)
	public static MonitoringState getMonitoringState(State currentState) {
		MonitoringState monitoringState;

		if (currentState.isAccepting()) {
			monitoringState = MonitoringState.POSS_SAT;
			for (Transition t : currentState.getOutput()) {
				if (t.isAll()) {
					monitoringState = MonitoringState.SAT;
				}
			}
		} else {
			monitoringState = MonitoringState.POSS_VIOL;
			for (Transition t : currentState.getOutput()) {
				if (t.isAll()) { //Assumes that non-accepting sink states are collapsed
					monitoringState = MonitoringState.VIOL;
				}
			}

		}

		return monitoringState;
	}


	//Returns a generic LTL formula for a given Declare template
	public static String getGenericLtlFormula(DeclareTemplate declareTemplate) {
		String formula = "";
		switch (declareTemplate) {
		case Absence:
			formula = "!( <> ( \"A\" ) )";
			break;
		case Absence2:
			formula = "! ( <> ( ( \"A\" /\\ X(<>(\"A\")) ) ) )";
			break;
		case Absence3:
			formula = "! ( <> ( ( \"A\" /\\  X ( <> ((\"A\" /\\  X ( <> ( \"A\" ) )) ) ) ) ))";
			break;
		case Alternate_Precedence:
			formula = "(((( !(\"B\") U \"A\") \\/ []( !(\"B\"))) /\\ []((\"B\" ->( (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) \\/ X((( !(\"B\") U \"A\") \\/ []( !(\"B\")))))))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) ))";
			break;
		case Alternate_Response:
			formula = "( []( ( \"A\" -> X(( (! ( \"A\" )) U \"B\" ) )) ) )";
			break;
		case Alternate_Succession:
			formula = "( []((\"A\" -> X(( !(\"A\") U \"B\")))) /\\ (((( !(\"B\") U \"A\") \\/ []( !(\"B\"))) /\\ []((\"B\" ->( (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) \\/ X((( !(\"B\") U \"A\") \\/ []( !(\"B\")))))))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )))";
			break;
		case Chain_Precedence:
			formula = "[]( ( X( \"B\" ) -> \"A\") )/\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )";
			break;
		case Chain_Response:
			formula = "[] ( ( \"A\" -> X( \"B\" ) ) )";
			break;
		case Chain_Succession:
			formula = "([]( ( \"A\" -> X( \"B\" ) ) )) /\\ ([]( ( X( \"B\" ) ->  \"A\") ) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) ))";
			break;
		case Choice:
			formula = "(  <> ( \"A\" ) \\/ <>( \"B\" )  )";
			break;
		case CoExistence:
			formula = "( ( <>(\"A\") -> <>( \"B\" ) ) /\\ ( <>(\"B\") -> <>( \"A\" ) )  )";
			break;
		case End:
			//formula = "( []((\"A\") -> ( !(X(\"A\" /\\ (!(\"A\")))) )";
			//formula = "(\"A\") && !X (true)";
			//formula = "<>( (\"A\") && !X (true))";
			//formula = "( <>((\"A\") && ( ! (X(\"A\" /\\  (!(\"A\")))))) )";
			//formula = " ( <>((\"A\") && ( (X(\"A\" /\\  (!(\"A\")))))) )";
			
			formula = "( <> ( \"A\" && !X( \"A\" U ( !\"A\" ) ) ) )";
			
			break;
		case Exactly1:
			formula = "(  <> (\"A\") /\\ ! ( <> ( ( \"A\" /\\ X(<>(\"A\")) ) ) ) )";
			break;
		case Exactly2:
			formula = "( <> (\"A\" /\\ (\"A\" -> (X(<>(\"A\"))))) /\\  ! ( <>( \"A\" /\\ (\"A\" -> X( <>( \"A\" /\\ (\"A\" -> X ( <> ( \"A\" ) ))) ) ) ) ) )";
			break;
		case Exactly3:
			formula = "( <>( \"A\" /\\ (\"A\" -> X (<>( \"A\" /\\ (\"A\" -> X ( <> ( \"A\" ) ))) ) ) ) ) /\\  ! ( <>( \"A\" /\\ (\"A\" -> X ( <>( \"A\" /\\ (\"A\" -> X ( <> ( \"A\"  /\\ (\"A\" -> X ( <> ( \"A\" ) ))) )  )  ) ) ) ) )";
			break;
		case Exclusive_Choice:
			formula = "(  ( <>( \"A\" ) \\/ <>( \"B\" )  )  /\\ !( (  <>( \"A\" ) /\\ <>( \"B\" ) ) ) )";
			break;
		case Existence:
			formula = "( <> ( \"A\" ) )";
			break;
		case Existence2:
			formula = "<> ( ( \"A\" /\\ X(<>(\"A\")) ) )";
			break;
		case Existence3:
			formula = "<>( \"A\" /\\ X(  <>( \"A\" /\\ X( <> \"A\" )) ))";
			break;
		case Init:
			formula = "( \"A\" )";
			break;
		case Not_Chain_Precedence:
			formula = "[] ( \"A\" -> !( X ( \"B\" ) ) )";
			break;
		case Not_Chain_Response:
			formula = "[] ( \"A\" -> !( X ( \"B\" ) ) )";
			break;
		case Not_Chain_Succession:
			formula = "[]( ( \"A\" -> !(X( \"B\" ) ) ))";
			break;
		case Not_CoExistence:
			formula = "(<>( \"A\" )) -> (!(<>( \"B\" )))";
			break;
		case Not_Precedence:
			formula = "[] ( \"A\" -> !( <> ( \"B\" ) ) )";
			break;
		case Not_Responded_Existence:
			formula = "(<>( \"A\" )) -> (!(<>( \"B\" )))";
			break;
		case Not_Response:
			formula = "[] ( \"A\" -> !( <> ( \"B\" ) ) )";
			break;
		case Not_Succession:
			formula = "[]( ( \"A\" -> !(<>( \"B\" ) ) ))";
			break;
		case Precedence:
			formula = "( ! (\"B\" ) U \"A\" ) \\/ ([](!(\"B\"))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )";
			break;
		case Responded_Existence:
			formula = "(( ( <>( \"A\" ) -> (<>( \"B\" ) )) ))";
			break;
		case Response:
			formula = "( []( ( \"A\" -> <>( \"B\" ) ) ))";
			break;
		case Succession:
			formula = "(( []( ( \"A\" -> <>( \"B\" ) ) ))) /\\ (( ! (\"B\" ) U \"A\" ) \\/ ([](!(\"B\"))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )   )";
			break;
		default:
			break;
		}
		return formula;
	}


}
