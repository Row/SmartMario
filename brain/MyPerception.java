package SmartMario.brain;

import pl.gdan.elsy.qconf.Perception;
import SmartMario.SmartAgent;

public class MyPerception extends Perception {
	private static final long serialVersionUID = 1L;
	private SmartAgent agent;
        
        private float bestPosX;
        private float lastPosX;
        private float startPosX;
        private int prevMode;

        private int killCount;
        public int brainCalculations; 
	public MyPerception(SmartAgent agent) {
		this.agent = agent;
                this.bestPosX = 0;
                this.lastPosX = 0;
                this.startPosX = agent.getPosX();
                this.brainCalculations = 0;
	}
        
        public void setAgent(SmartAgent agent) {
            this.agent = agent;
        }
        public void reset() {
            bestPosX = 0;
            lastPosX = 0;
            prevMode = 2;
            this.startPosX = agent.getPosX();
        }

	public double getReward() {
            double reward = 0; 
            brainCalculations++;
            // Moving forward is good
            if (bestPosX < agent.getPosX()) {
                bestPosX = agent.getPosX(); // Higher reward the further on map?
                reward += 0.1;
            }
            
            // travel forward is good
            
            // Hit by enemy
            if (prevMode < agent.getMode()) {
                 reward += -0.2;
            }
            
            prevMode = agent.getMode();
            
            // Kill enemy
            if (killCount < agent.getKills()) {
                //reward += 0.01;
                killCount = agent.getKills();
            }
            
            // Win
            if (agent.getStatus() == 1) {
                reward = 1;
                System.out.println("Reward:" + reward + "bc " + brainCalculations);

            }
            
            // Death
           if (agent.getStatus() == 0) {
               reward = -0.5; 
               //reward += (agent.getPosX() - startPosX)/6400.0; // map length is not constant
               System.out.println("Reward:" + reward + "bc " + brainCalculations);

            }
           
            return reward;
	}

	protected void updateInputValues() {
            
            double[] inputs = agent.getInputs();
            int numOfInputs = inputs.length;
            for (int i = 0; i < numOfInputs; i++) {
                setNextValue(inputs[i]);
            }
	}
        
        public boolean isUnipolar() {
		return true;
	}
}
