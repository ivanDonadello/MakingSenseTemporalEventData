package makingSenseTemporaData.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import makingSenseTemporaData.model.DeclareConstraint;
import makingSenseTemporaData.model.DeclareTemplate;

public class ModelUtils {

	//Private constructor to avoid unnecessary instantiation of the class
	private ModelUtils() {
	}

	//Finds constraint strings in the Declare model and creates a list of Declare constraint objects
	public static List<DeclareConstraint> readConstraints(File declareModelPath) throws IOException {
		List<DeclareConstraint> declareConstraints = new ArrayList<DeclareConstraint>();

		Scanner sc = new Scanner(declareModelPath);
		Pattern constraintPattern = Pattern.compile("\\w+(\\[.*\\]) \\|");

		while(sc.hasNextLine()) {
			String line = sc.nextLine();

			if(line.startsWith("activity") && line.length() > 9) {
				//Skipping activity definitions
			} else if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				//Skipping activity-attribute bindings
			} else {
				Matcher constraintMatcher = constraintPattern.matcher(line);
				if (constraintMatcher.find()) { //Constraints
					DeclareConstraint declareConstraint = readConstraintString(line);
					declareConstraints.add(declareConstraint);
				}
			}
		}
		sc.close();

		return declareConstraints;
	}

	//Creates a single Declare constraint object from Declare constraint string
	private static DeclareConstraint readConstraintString(String constraintString) {
		DeclareTemplate template = null;
		String activationActivity = "";
		String targetActivity = "";
		char firstChar = constraintString.charAt(0);
		boolean is_negated = false;
		if (firstChar == '!') {
			constraintString= constraintString.substring(1);
			is_negated = true;
		}
		
		Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraintString);
		Matcher mUnary = Pattern.compile(".*\\[(.*)\\] \\|(.*) \\|(.*)").matcher(constraintString);

		//Processing the constraint
		if(mBinary.find()) { //Binary constraints
			template = DeclareTemplate.getByTemplateName(mBinary.group(1));
			if(template.getReverseActivationTarget()) {
				targetActivity = mBinary.group(2);
				activationActivity = mBinary.group(3);
			}
			else {
				activationActivity = mBinary.group(2);
				targetActivity = mBinary.group(3);
			}
			constraintString = mBinary.group(1) + "[" + mBinary.group(2) + ", " + mBinary.group(3) + "]";
		} else if(mUnary.find()) { //Unary constraints
			String templateString = mUnary.group(0).substring(0, mUnary.group(0).indexOf("[")); //TODO: Should be done more intelligently
			template = DeclareTemplate.getByTemplateName(templateString);
			activationActivity = mUnary.group(1);
			constraintString = template + "[" + mUnary.group(1) + "]";
		}
		
		return new DeclareConstraint(constraintString, template, activationActivity, targetActivity, is_negated);
	}

	public static Map<String, String> encodeActivities(List<DeclareConstraint> declareConstrains) {
		Map<String, String> activityToEncoding = new HashMap<String, String>();
		
		for (DeclareConstraint declareConstraint : declareConstrains) {
			String activity = declareConstraint.getActivationActivity();
			if (!activityToEncoding.containsKey(activity)) {
				activityToEncoding.put(activity, "act" + activityToEncoding.size());
			}
			if (declareConstraint.getTemplate().getIsBinary()) {
				activity = declareConstraint.getTargetActivity();
				if (!activityToEncoding.containsKey(activity)) {
					activityToEncoding.put(activity, "act" + activityToEncoding.size());
				}
			}
		}
		return activityToEncoding;
	}
}
