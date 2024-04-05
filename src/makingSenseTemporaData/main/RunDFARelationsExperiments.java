package makingSenseTemporaData.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import com.opencsv.CSVWriter;

import makingSenseTemporaData.MoBuConLtlMonitorLocal;
import makingSenseTemporaData.model.DeclareConstraint;
import makingSenseTemporaData.utils.AutomatonUtils;
import makingSenseTemporaData.utils.ModelUtils;

public class RunDFARelationsExperiments {
	
	private static final DecimalFormat df = new DecimalFormat("0.00");
	
	public static String boolToStr(boolean b) {
	    return b ? String.valueOf(1) : String.valueOf(0);
	}


	public static void main(String[] args) {


		Map<String, String> models_lbl_map = new HashMap<String, String>();
		models_lbl_map.put("negdis", "\\Dem");
		models_lbl_map.put("explainer", "\\E");
		models_lbl_map.put("RF_ripper", "\\Dvm");

		ArrayList<String> datasets = new ArrayList<String>();
		datasets.add("sepsis_1");
		datasets.add("sepsis_5");
		//datasets.add("BPIC11");
		datasets.add("BPIC17_O_Accepted");
		datasets.add("sepsis_cases_1");
		datasets.add("sepsis_cases_2");
		datasets.add("sepsis_cases_4");
		datasets.add("DreyersFoundation");
		datasets.add("Production");
		
		ArrayList<String> polarities = new ArrayList<String>();
		polarities.add("pos");
		polarities.add("neg");
		
		ArrayList<String> models = new ArrayList<String>();
		models.add("negdis");
		models.add("explainer");
		models.add("RF_ripper");

		for (String dataset_name : datasets) {

			// load the logs
			Path posLogPath = Paths.get(System.getProperty("user.dir"), "input", dataset_name, "logs",
					"original_pos.xes");
			Path negLogPath = Paths.get(System.getProperty("user.dir"), "input", dataset_name, "logs",
					"original_neg.xes");
			System.out.println("	Loading logs ...");
			XLog posXLog = convertToXlog(posLogPath.toString());
			XLog negXLog = convertToXlog(negLogPath.toString());
			Map<String, XLog> polarity_log_map = new HashMap<String, XLog>();
			polarity_log_map.put("pos", posXLog);
			polarity_log_map.put("neg", negXLog);
			
			
			// write header of the results file
			try {
				FileWriter outputfile = new FileWriter(Paths
						.get(System.getProperty("user.dir"), "results", String.format("%s_DFA_relations.csv", dataset_name)).toString());
			
				CSVWriter writer = new CSVWriter(outputfile, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
				String[] header = { "Dataset", "\\model{A_1}", "\\model{A_2}", "Polarities",  "$=$", "$\\subseteq$", "$\\supseteq$", "$\\cap = \\emptyset$", "acc(\\cap, \\PosLog)", "acc(\\cap, \\NegLog)"};
				writer.writeNext(header);
				double accuracy = 0.0;
				
				for (String pol_a : polarities) {
					for (String pol_b : polarities) {
						for (int i = 0; i < models.size(); i++) {
							for (int j = i+1; j < models.size(); j++) {
								
								ArrayList<String> model_results = new ArrayList<String>();
								String model_a = models.get(i);
								String model_b = models.get(j);
								
								System.out.println(String.format("Dataset %s, First Polarity %s, Second Polarity %s, First Model %s, Second Model %s",
										dataset_name, pol_a, pol_b, model_a, model_b));
								
								String declModelAPath = Paths.get(System.getProperty("user.dir"), "input", dataset_name,
										model_a, pol_a + ".decl").toString();
								String declModelBPath = Paths.get(System.getProperty("user.dir"), "input", dataset_name,
										model_b, pol_b + ".decl").toString();
								
								MoBuConLtlMonitorLocal monitor_a = new MoBuConLtlMonitorLocal(true);
								MoBuConLtlMonitorLocal monitor_b = new MoBuConLtlMonitorLocal(true);
								
								File declModelA = new File(declModelAPath);
								File declModelB = new File(declModelBPath);
								
								
								// Loading constraints from the model
								List<DeclareConstraint> declareConstrains_a = ModelUtils.readConstraints(declModelA);
								Map<String, String> activityToEncoding_a = ModelUtils.encodeActivities(declareConstrains_a);
								List<DeclareConstraint> declareConstrains_b = ModelUtils.readConstraints(declModelB);
								Map<String, String> activityToEncoding_b = ModelUtils.encodeActivities(declareConstrains_b);
								Map<String, String> activityToEncoding = AutomatonUtils.mergeActivityEncodingMaps(activityToEncoding_a, activityToEncoding_b);
								try {
									monitor_a.setModel(declModelA, false, activityToEncoding);
									monitor_b.setModel(declModelB, false, activityToEncoding);
								} catch (IOException | SyntaxParserException e) {
									e.printStackTrace();
								}

								DeterministicAutomaton detAutomaton_a = monitor_a.getDeterministicAutomaton();
								DeterministicAutomaton detAutomaton_b = monitor_b.getDeterministicAutomaton();
								
								model_results.add(dataset_name);
								model_results.add(model_a);
								model_results.add(model_b);
								model_results.add(pol_a + "_" + pol_b);
								
								// AutomatonUtils.DFA2dot(detAutomaton_a, model_a);
								// AutomatonUtils.DFA2dot(detAutomaton_b, model_b);
								
								//automata equality
								boolean automata_equality = AutomatonUtils.automataEquality(detAutomaton_a, detAutomaton_b);
								model_results.add(boolToStr(automata_equality));
								
								// first automata subset of the second
								boolean automata_subset = AutomatonUtils.automataSubset(detAutomaton_a, detAutomaton_b);
								model_results.add(boolToStr(automata_subset));
								
								// first automata superset of the second
								boolean automata_superset = AutomatonUtils.automataSubset(detAutomaton_b, detAutomaton_a);
								model_results.add(boolToStr(automata_superset));
								
								// non empty intersection
								DeterministicAutomaton intersec_automata = detAutomaton_a.op.intersect(detAutomaton_b).op.minimize().op.determinize();
								boolean empty_intersection = intersec_automata.op.isEmpty();
								model_results.add(boolToStr(empty_intersection));
								
								// accuracy of the automata intersection on the positive log
								int cnt = AutomatonUtils.count_accepted_strings(new ExecutableAutomaton(intersec_automata), posXLog, activityToEncoding);
								accuracy = ((double) cnt) / posXLog.size();
								model_results.add(df.format(100*accuracy));
								
								// accuracy of the automata intersection on the negative log
								cnt = AutomatonUtils.count_accepted_strings(new ExecutableAutomaton(intersec_automata), negXLog, activityToEncoding);
								accuracy = ((double) cnt) / negXLog.size();
								model_results.add(df.format(100*accuracy));
								writer.writeNext(model_results.toArray(new String[0]));
							}
						}
					}
				}
				
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
	}
	

	private static XLog convertToXlog(String logPath) {
		XLog xlog = null;
		File logFile = new File(logPath);

		if (logFile.getName().toLowerCase().endsWith(".mxml")) {
			XMxmlParser parser = new XMxmlParser();
			if (parser.canParse(logFile)) {
				try {
					xlog = parser.parse(logFile).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (logFile.getName().toLowerCase().endsWith(".xes")) {
			XesXmlParser parser = new XesXmlParser();
			if (parser.canParse(logFile)) {
				try {
					xlog = parser.parse(logFile).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return xlog;
	}

}
