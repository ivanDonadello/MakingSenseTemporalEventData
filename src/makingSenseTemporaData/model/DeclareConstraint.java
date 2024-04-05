package makingSenseTemporaData.model;

public class DeclareConstraint {
	private String constraintString;
	private DeclareTemplate template;
	private String activationActivity;
	private String targetActivity;
	private boolean is_negated;
	
	public DeclareConstraint(String constraintString, DeclareTemplate template, String activationActivity, String targetActivity, boolean is_negated) {
		super();
		this.constraintString = constraintString;
		this.template = template;
		this.activationActivity = activationActivity;
		this.targetActivity = targetActivity;
		this.is_negated = is_negated;
	}
	
	public boolean getConstraintNegation() {
		return is_negated;
	}

	public String getConstraintString() {
		return constraintString;
	}
	
	public DeclareTemplate getTemplate() {
		return template;
	}

	public String getActivationActivity() {
		return activationActivity;
	}

	public String getTargetActivity() {
		return targetActivity;
	}

	@Override
	public String toString() {
		String negation_symbol = "";
		
		if (this.is_negated == true){
			negation_symbol = "!";
		}
		return "DeclareConstraint [constraintString=" + constraintString + ", template=" + negation_symbol + template
				+ ", activationActivity=" + activationActivity + ", targetActivity=" + targetActivity + "]";
	}
}
