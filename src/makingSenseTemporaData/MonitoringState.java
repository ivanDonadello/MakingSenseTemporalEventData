package makingSenseTemporaData;

public enum MonitoringState {
	INIT("init"), //Initial state of the constraint automata, could probably be removed
	SAT("sat"), //Permanently satisfied
	VIOL("viol"), //Permanently violated
	POSS_SAT("poss.sat"), //Possibly satisfied
	POSS_VIOL("poss.viol"); //Possibly violated
	
	
	private final String mobuconltlName;
	
	private MonitoringState(String mobuconltlName) {
		this.mobuconltlName=mobuconltlName;
	}
	
	public String getMobuconltlName() {
		return mobuconltlName;
	}
	
	@Override
	public String toString() {
		return mobuconltlName;
	}
}
