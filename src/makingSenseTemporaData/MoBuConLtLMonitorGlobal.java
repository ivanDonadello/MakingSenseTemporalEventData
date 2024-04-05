package makingSenseTemporaData;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import makingSenseTemporaData.model.DeclareConstraint;
import makingSenseTemporaData.utils.AutomatonUtils;
import makingSenseTemporaData.utils.ModelUtils;

/* 
 * This class creates and colours global automaton that is then used for monitoring
 * The global automaton can take a lot of memory, but is fast to use once created
 * This implementation does not support conflict check and recovery strategies
 */


public class MoBuConLtLMonitorGlobal {

	private List<DeclareConstraint> declareConstrains;
	private Map<String, String> activityToEncoding;
	private Map<DeclareConstraint, ExecutableAutomaton> constraintAutomatonMap = new HashMap<DeclareConstraint, ExecutableAutomaton>();
	private ExecutableAutomaton globalAutomaton;
	private Map<State, Map<DeclareConstraint, MonitoringState>> globalAutomatonColours;

	public MoBuConLtLMonitorGlobal() {
	}


	//Loading the input model and creates the global automaton for monitoring
	public void setModel(File declModel) throws IOException, SyntaxParserException {

		//Loading constraints from the model
		declareConstrains = ModelUtils.readConstraints(declModel);
		System.out.println("Constraints: " + declareConstrains);


		//Creating activity name encodings to avoid issues with activity names containing dashes etc.
		activityToEncoding = ModelUtils.encodeActivities(declareConstrains);
		System.out.println("Activity encodings: " + activityToEncoding);


		//Creating constraint automata and the global automaton
		DeterministicAutomaton deterministicGlobalAutomaton = null;
		System.out.println("Encoded LTL formulas:");
		for (DeclareConstraint declareConstraint : declareConstrains) {
			String ltlFormula = AutomatonUtils.getGenericLtlFormula(declareConstraint.getTemplate());

			//Replacing activity placeholders in the generic formula with activity encodings based on the model
			ltlFormula = ltlFormula.replace("\"A\"", activityToEncoding.get(declareConstraint.getActivationActivity()));
			if (declareConstraint.getTemplate().getIsBinary()) {
				ltlFormula = ltlFormula.replace("\"B\"", activityToEncoding.get(declareConstraint.getTargetActivity()));
			}
			System.out.println("\t" + ltlFormula);

			//Creating the constraint automaton and intersecting it with the global automaton
			DeterministicAutomaton constraintAutomaton = AutomatonUtils.createAutomatonForLtlFormula(ltlFormula);
			if (deterministicGlobalAutomaton == null) {
				deterministicGlobalAutomaton =  new DeterministicAutomaton(constraintAutomaton, constraintAutomaton.isCompleted());
			} else {
				deterministicGlobalAutomaton = deterministicGlobalAutomaton.op.intersect(constraintAutomaton);
			}
			constraintAutomatonMap.put(declareConstraint, new ExecutableAutomaton(constraintAutomaton));
		}
		globalAutomaton = new ExecutableAutomaton(deterministicGlobalAutomaton);
		System.out.println("Automata created");


		//Colouring the global automaton
		globalAutomatonColours = AutomatonUtils.getGlobalAutomatonColours(constraintAutomatonMap, globalAutomaton);
		System.out.println("Global automata coloured");

		//Moving the global automaton to its initial state
		globalAutomaton.ini();

		System.out.println("Model processing done!");
	}


	//Processes the next event in a trace, assumes that processing occurs one trace at a time
	//isTraceEnd=True returns permanent states instead of temporary states and resets the global automaton for monitoring the next trace
	public Map<String, String> processNextEvent(XEvent xevent, boolean isTraceEnd) {
		Map<String, String> constraintStates = new HashMap<String, String>();

		String eventName = XConceptExtension.instance().extractName(xevent);
		globalAutomaton.next(activityToEncoding.getOrDefault(eventName, "actx"));
		State globalState = globalAutomaton.currentState().get(0);

		for (DeclareConstraint declareConstraint : declareConstrains) {
			MonitoringState monitoringState = globalAutomatonColours.get(globalState).get(declareConstraint);
			constraintStates.put(declareConstraint.getConstraintString(), monitoringState.getMobuconltlName());
		}

		if (isTraceEnd) {
			for (String constraint : constraintStates.keySet()) {
				if (constraintStates.get(constraint).equals("poss.sat")) {
					constraintStates.put(constraint, "sat");
				}
				if (constraintStates.get(constraint).equals("poss.viol")) {
					constraintStates.put(constraint, "viol");
				}
			}
			globalAutomaton.ini();
		}

		return constraintStates;
	}
}
