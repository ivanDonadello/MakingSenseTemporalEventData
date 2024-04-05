package makingSenseTemporaData.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import com.opencsv.CSVWriter;

import makingSenseTemporaData.MoBuConLtlMonitorLocal;
import makingSenseTemporaData.utils.AutomatonUtils;

public class RunAccuracyExperiments {
	
	private static final DecimalFormat df = new DecimalFormat("0.00");

	public static void main(String[] args) {


		Map<String, String> models_lbl_map = new HashMap<String, String>();
		models_lbl_map.put("negdis", "\\NG");
		//models_lbl_map.put("explainer", "\\E");
		//models_lbl_map.put("RF_ripper", "\\DvM");

		ArrayList<String> datasets = new ArrayList<String>();
		datasets.add("sepsis_1");
		datasets.add("sepsis_5");
		datasets.add("BPIC11");
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
		models.add("explainer");
		models.add("negdis");
		models.add("RF_ripper");

		for (String dataset_name : datasets) {

			// Load logs
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

			try {
				FileWriter outputfile = new FileWriter(Paths
						.get(System.getProperty("user.dir"), "results", String.format("%s_accepted.csv", dataset_name)).toString());
			
				CSVWriter writer = new CSVWriter(outputfile, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
				String[] header = { "Dataset", "Approach", "$acc(\\mathcal{A}_{\\Model^+}, \\PosLog)$", "$acc(\\mathcal{A}_{\\Model^+}, \\NegLog)$", "$acc(\\mathcal{A}_{\\Model^-}, \\PosLog)$", "$acc(\\mathcal{A}_{\\Model^-}, \\NegLog)$"};
				writer.writeNext(header);

				for (String disc_model : models) {
					ArrayList<String> model_results = new ArrayList<String>();
					model_results.add(dataset_name);
					model_results.add(models_lbl_map.get(disc_model));
					
					for (String model_polarity : polarities) {
						for (String log_polarity : polarities) {
							System.out.println(String.format("Dataset %s, Model %s, Model Polarity %s, Log Polarity %s",
									dataset_name, disc_model, model_polarity, log_polarity));

							String declModelPath = Paths.get(System.getProperty("user.dir"), "input", dataset_name,
									disc_model, model_polarity + ".decl").toString();
							MoBuConLtlMonitorLocal monitor = new MoBuConLtlMonitorLocal(true);

							System.out.println("	Building the automaton ...");
							File declModel = new File(declModelPath);
							try {
								monitor.setModel(declModel, false, null);
							} catch (IOException | SyntaxParserException e) {
								e.printStackTrace();
							}
							System.out.println("	Automaton built!");
							ExecutableAutomaton globalAutomaton = monitor.getGlobalAutomaton();
							Map<String, String> activityToEncoding = monitor.getActivityToEncoding();

							XLog tmp_log = polarity_log_map.get(log_polarity);
							double accuracy = 0.0;
							
							int cnt = AutomatonUtils.count_accepted_strings(globalAutomaton, tmp_log, activityToEncoding);
							
							accuracy = ((double) cnt) / tmp_log.size();
							model_results.add(df.format(100*accuracy));
							System.out.println("Accuracy: " + accuracy);

						}
					}
					writer.writeNext(model_results.toArray(new String[0]));
				}

				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
		/* For easy debugging
		MoBuConLtlMonitorLocal monitor = new MoBuConLtlMonitorLocal(true);

		String declModelPath = "input/sepsis_cases_4/explainer/neg.decl";
		String eventLogPath = "input/sepsis_cases_4/logs/original_neg.xes";
		File declModel = new File(declModelPath);
		try {
			monitor.setModel(declModel, false);
		} catch (IOException | SyntaxParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ExecutableAutomaton globalAutomaton = monitor.getGlobalAutomaton();
		Map<String, String> activityToEncoding = monitor.getActivityToEncoding();

		XLog xlog = convertToXlog(eventLogPath);
		double accuracy = 0.0;
		double cnt = 0.0;
		for (XTrace xtrace : xlog) {
			globalAutomaton.ini();
			for (int i = 0; i < xtrace.size(); i++) {
				// System.out.println(monitor.processNextEvent(xtrace.get(i), false));

				String eventName = XConceptExtension.instance().extractName(xtrace.get(i));
				String encodedEventName = activityToEncoding.getOrDefault(eventName, "actx");

				globalAutomaton.next(encodedEventName);

			}
			//System.out.println("traccia finita");
			if (globalAutomaton.currentState().isAccepting() == true) {
				// System.out.println(1);
				cnt = cnt + 1;
			}

		}
		accuracy = cnt / xlog.size();
		System.out.println("Accuracy: " + accuracy);
		*/

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
