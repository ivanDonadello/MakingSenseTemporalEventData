package makingSenseTemporaData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.processmining.ltl2automaton.plugins.automaton.DOTExporter;
import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import makingSenseTemporaData.model.DeclareConstraint;
import makingSenseTemporaData.utils.AutomatonUtils;
import makingSenseTemporaData.utils.ModelUtils;

/* 
 * This class creates an individual automaton for each constraint in the model
 * It is slower than the approach with a global automaton, but takes less memory
 * This implementation does not support recovery strategies
 */

public class MoBuConLtlMonitorLocal {

	private boolean conflictCheck;

	private List<DeclareConstraint> declareConstrains;
	private Map<String, String> activityToEncoding;
	private Map<DeclareConstraint, ExecutableAutomaton> constraintAutomatonMap = new HashMap<DeclareConstraint, ExecutableAutomaton>();
	private List<String> ltlFormulas = new ArrayList<String>();
	private ExecutableAutomaton globalAutomaton;
	private DeterministicAutomaton det_tmp_automaton;
	private boolean permVioloccurred;

	public MoBuConLtlMonitorLocal(boolean conflictCheck) {
		this.conflictCheck = conflictCheck;
	}

	public ExecutableAutomaton getGlobalAutomaton() {
		return this.globalAutomaton;
	}

	public Map<String, String> getActivityToEncoding() {
		return this.activityToEncoding;
	}
	
	public DeterministicAutomaton getDeterministicAutomaton() {
		return det_tmp_automaton;
	}

	// Loads the input model and creates the individual automata for monitoring
	public void setModel(File declModel, boolean debug, Map<String, String> act_id_map) throws IOException, SyntaxParserException {

		// Loading constraints from the model
		declareConstrains = ModelUtils.readConstraints(declModel);
		if (debug) System.out.println("Constraints: " + declareConstrains);

		// Creating activity name encodings to avoid issues with activity names
		// containing dashes etc.
		if (act_id_map == null) this.activityToEncoding = ModelUtils.encodeActivities(declareConstrains);
		else activityToEncoding = act_id_map;
		
		if (debug) System.out.println("Activity encodings: " + activityToEncoding);

		// Creating automata for monitoring
		if (debug) System.out.println("Encoded LTL formulas:");
		
		for (DeclareConstraint declareConstraint : declareConstrains) {
			String ltlFormula = AutomatonUtils.getGenericLtlFormula(declareConstraint.getTemplate());
			if (declareConstraint.getConstraintNegation()) {
				ltlFormula = "! ( " + ltlFormula + " ) ";
				//ltlFormula = "! " + ltlFormula;
			}

			// Replacing activity placeholders in the generic formula with activity
			// encodings based on the model
			ltlFormula = ltlFormula.replace("\"A\"", activityToEncoding.get(declareConstraint.getActivationActivity()));
			if (declareConstraint.getTemplate().getIsBinary()) {
				ltlFormula = ltlFormula.replace("\"B\"", activityToEncoding.get(declareConstraint.getTargetActivity()));
			}
			if (debug) {
				System.out.println("\t" + ltlFormula);
			}
			// Creating the constraint automaton, initialising it, and placing it in the
			// automata map
			ExecutableAutomaton constraintAutomaton = new ExecutableAutomaton(
					AutomatonUtils.createAutomatonForLtlFormula(ltlFormula));
			constraintAutomaton.ini();
			constraintAutomatonMap.put(declareConstraint, constraintAutomaton);

			if (conflictCheck) {
				ltlFormulas.add(ltlFormula);
			}
		}
		String globalLtlFormula = "";
		// Creating global automata for conflict check
		if (conflictCheck) {
			globalLtlFormula = "(" + String.join(") && (", ltlFormulas) + ")";
			det_tmp_automaton = AutomatonUtils.createAutomatonForLtlFormula(globalLtlFormula);
			globalAutomaton = new ExecutableAutomaton(det_tmp_automaton);
			globalAutomaton.ini();

			if (debug) {
				DOTExporter.exportToDot(det_tmp_automaton, "Model Name", new FileWriter("modelName.dot"));
			}

		}
		if (debug) {
			System.out.println("globalLtlFormula" + globalLtlFormula);
			System.out.println("Model processing done!");
		}
	}

	// Processes the next event in a trace, assumes that processing occurs one trace
	// at a time
	// isTraceEnd=True returns permanent states instead of temporary states and
	// resets automata for monitoring the next trace
	public Map<String, String> processNextEvent(XEvent xevent, boolean isTraceEnd) {
		Map<String, String> constraintStates = new HashMap<String, String>();

		String eventName = XConceptExtension.instance().extractName(xevent);
		String encodedEventName = activityToEncoding.getOrDefault(eventName, "actx");

		for (DeclareConstraint declareConstraint : declareConstrains) {
			ExecutableAutomaton constraintAutomaton = constraintAutomatonMap.get(declareConstraint);
			constraintAutomaton.next(encodedEventName);

			MonitoringState monitoringState = AutomatonUtils.getMonitoringState(constraintAutomaton.currentState());
			constraintStates.put(declareConstraint.getConstraintString(), monitoringState.getMobuconltlName());
		}

		// Using the global automaton to detect conflicting states
		if (conflictCheck) {
			if (isTraceEnd) {
				globalAutomaton.ini();
				permVioloccurred = false;
			} else {
				if (!permVioloccurred) {
					for (String monitoringStateString : constraintStates.values()) {
						if ("viol".equals(monitoringStateString)) {
							permVioloccurred = true;
						}
					}
				}

				if (!permVioloccurred) {
					globalAutomaton.next(encodedEventName);
					if (!globalAutomaton.currentState().acceptingReachable()) {
						for (String constraint : constraintStates.keySet()) {
							// Marking all constraints (except sat) as conflicting
							if (!constraintStates.get(constraint).equals("sat")) {
								constraintStates.put(constraint, "conflict");
							}
						}
					}
				}
			}
		}

		// All monitoring states become permanent at the end of the trace
		if (isTraceEnd) {
			for (String constraint : constraintStates.keySet()) {
				if (constraintStates.get(constraint).equals("poss.sat")) {
					constraintStates.put(constraint, "sat");
				}
				if (constraintStates.get(constraint).equals("poss.viol")) {
					constraintStates.put(constraint, "viol");
				}
			}

			for (ExecutableAutomaton constraintAutomaton : constraintAutomatonMap.values()) {
				constraintAutomaton.ini();
			}
		}

		return constraintStates;
	}

}
