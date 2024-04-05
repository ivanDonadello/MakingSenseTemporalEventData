package makingSenseTemporaData.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

public class ProvaDL {
	
	private static final DecimalFormat df = new DecimalFormat("0.00");

	public static void main(String[] args) {
		Path log_path = Paths.get(System.getProperty("user.dir"), "input", "test_time_DL", "prova.xes");
		System.out.println("	Loading logs ...");
		XLog log = convertToXlog(log_path.toString());
		
		String declModelPath = Paths.get(System.getProperty("user.dir"), "input", "test_time_DL", "prova" + ".decl").toString();
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
		
		long startTime = System.currentTimeMillis();
		
		int cnt = AutomatonUtils.count_accepted_strings(globalAutomaton, log, activityToEncoding);
		
		long estimatedTime = System.currentTimeMillis() - startTime;
		System.out.println("Took: " + ((estimatedTime) / 1000.0));





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
