package SmartMario.brain;

import pl.gdan.elsy.qconf.Action;
import SmartMario.SmartAgent;

public class SmartAction extends Action {
	private static final long serialVersionUID = 1L;
        private int action;

	public SmartAction(int action) {
		this.action = action;
	}

	public int execute() {
		return action;
	}
}
