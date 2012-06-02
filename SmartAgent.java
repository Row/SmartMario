/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package SmartMario;

import SmartMario.brain.SmartAction;
import SmartMario.brain.MyPerception;
import ch.idsia.agents.Agent;
import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import pl.gdan.elsy.qconf.Action;
import pl.gdan.elsy.qconf.Brain;
import pl.gdan.elsy.qconf.Perception;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.GeneralizerEnemies;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;



/**
 * Jon & Thomas Smart Reinforced Learning Agent
*/

public class SmartAgent extends BasicMarioAIAgent implements Agent
{
public static final int IDLE        = 0;
public static final int MOVE_LEFT   = 1;
public static final int MOVE_RIGHT  = 2;
public static final int JUMP        = 3;
public static final int JUMP_LEFT   = 4;
public static final int JUMP_RIGHT  = 5;
public static final int FIRE        = 6;
public static final int FIRE_LEFT   = 7;
public static final int FIRE_RIGHT  = 8;
public static final int FIRE_JUMP   = 9;
public static final int FIRE_JUMP_LEFT  = 10;
public static final int FIRE_JUMP_RIGHT = 11;

float lastPos;

public Brain brain = null;
public MyPerception perception = null;
private String brainPath; 

/* TODO forward/back etc */
public int viewSize        = 19;
public int numTileOptions  = 5;
public int numMarioOptions = 9;

// "brain" related
public int numHiddenNodes  = 13;

public double qAlpha  = 0.03; // Learning rate 
public double qGamma  = 0.95; // Discount factor
public double qLambda = 0.99; // ?
public double qTemp   = 0.05; // Chance of choosing other than maxQ

private int counter = 0;

// Environment
public int timeSpent = 0;

public SmartAgent()
{
    super("SmartAgent");
    reset();
    setObservationDetails(viewSize, viewSize, viewSize/2, viewSize/2);
    
    brainPath = "brain";
}
public float getPosX()
{
    return marioFloatPos[0];
}
public float getPosY()
{
    return marioFloatPos[1];
}

public int getMode()
{
    return marioMode;
}

public int getStatus()
{
    return marioStatus; // 
}

public int getKills()
{
    return getKillsTotal;
}

public boolean getIsMarioOnGround() 
{
    return isMarioOnGround;
}
public void setBrainPath(String bPath){
    brainPath = bPath;
}

public void reset()
{
    action = new boolean[Environment.numberOfKeys];
    if (perception != null) {
        perception.reset();
    }
}

    @Override
public void integrateObservation(Environment environment) {
    //System.out.println("Counter :" + counter);
    
    super.integrateObservation(environment);
    timeSpent = environment.getTimeSpent();    
}


public double[] getInputs() {
    double inputs[] = new double[viewSize * viewSize * numTileOptions + numMarioOptions];
    
    // Input each grid tile
    for (int row = 0; row < viewSize; row++){
        for (int col = 0; col < viewSize; col++){
            // If jumpable land (jump through)
            int index = row*viewSize*numTileOptions+col*numTileOptions;
            inputs[index] = (levelScene[row][col] == GeneralizerLevelScene.BORDER_HILL) ? 1 : 0;
                
            // If land
            inputs[index+1] =  (levelScene[row][col] == GeneralizerLevelScene.BREAKABLE_BRICK ||
                levelScene[row][col] == GeneralizerLevelScene.UNBREAKABLE_BRICK ||
                levelScene[row][col] == GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH ||
                levelScene[row][col] == GeneralizerLevelScene.CANNON_MUZZLE ||
                levelScene[row][col] == GeneralizerLevelScene.CANNON_TRUNK ||
                levelScene[row][col] == GeneralizerLevelScene.FLOWER_POT) ? 1 : 0;
            
            inputs[index+2] = (levelScene[row][col] == GeneralizerLevelScene.PRINCESS) ? 1 : 0;
            
            inputs[index+3] =  (enemies[row][col] == Sprite.KIND_GOOMBA) ? 1 : 0;
            inputs[index+4] = (enemies[row][col] == Sprite.KIND_SPIKY) ? 1 : 0;
              
        }
    }
    
    // Input each mario option
    int index = viewSize * viewSize * numTileOptions;
    
    // Mario is fire
    inputs[index] =  marioMode == 2 ? 1 : 0;
    
    // Mario big
    inputs[index + 1] =  marioMode == 1 ? 1 : 0;
    
    // Mario is on ground
    inputs[index + 2] =  isMarioOnGround ? 1 : 0;
    
    //Is mario able to jump
    inputs[index + 3] =  isMarioAbleToJump ? 1 : 0;
    
    //Is mario able to shoot
    inputs[index + 4] =  isMarioAbleToShoot ? 1 : 0;
    
    //How far has mario traveled
    inputs[index + 5] =  getPosX() / 3200;
    
    inputs[index + 6] =  getPosY() / 300;
    
    //Marios speed (this is since it print large number now and then)
    float speed = lastPos - getPosX();
    if (speed > 20 || speed < -20) {
        speed = 0;
    }
    inputs[index + 7] = speed/20;
    
    inputs[index + 8] = timeSpent/300;
    
    //System.out.println("iputs: x " + inputs[index + 5] + " y "+ inputs[index + 6] + " s "+ inputs[index + 7]);
    lastPos = getPosX();
    
    return inputs;
}
public void initBrain(){
    Action actionArray[] = new Action[12];
    actionArray[IDLE]            = new SmartAction(IDLE);
    actionArray[MOVE_LEFT]       = new SmartAction(MOVE_LEFT);
    actionArray[MOVE_RIGHT]      = new SmartAction(MOVE_RIGHT);
    actionArray[JUMP]            = new SmartAction(JUMP);
    actionArray[JUMP_LEFT]       = new SmartAction(JUMP_LEFT);
    actionArray[JUMP_RIGHT]      = new SmartAction(JUMP_RIGHT);
    actionArray[FIRE]            = new SmartAction(FIRE);
    actionArray[FIRE_LEFT]       = new SmartAction(FIRE_LEFT);
    actionArray[FIRE_RIGHT]      = new SmartAction(FIRE_RIGHT);
    actionArray[FIRE_JUMP]       = new SmartAction(FIRE_JUMP);
    actionArray[FIRE_JUMP_LEFT]  = new SmartAction(FIRE_JUMP_LEFT);
    actionArray[FIRE_JUMP_RIGHT] = new SmartAction(FIRE_JUMP_RIGHT);
                
    perception = new MyPerception(this);
   
    int hiddenNeurons[] = new int[1];
    hiddenNeurons[0] = numHiddenNodes;
    brain = new Brain(perception, actionArray, hiddenNeurons);
    try{
        brain.load(brainPath + ".ser");
        System.out.println("Old brain");
    }catch(Exception e) {
        brain = new Brain(perception, actionArray, hiddenNeurons);
        System.out.println("New brain");

    }
    brain.setAlpha(qAlpha); // Learning rate 
    brain.setGamma(qGamma); // Discount factor
    brain.setLambda(qLambda); // 
    brain.setUseBoltzmann(true); 
    brain.setTemperature(qTemp); // The chance of choosing a random value instead of best Q?
}

public boolean[] getAction()
{
    if(brain == null){
        initBrain();
        lastPos = getPosX();
    }
    // Always run
    action[Mario.KEY_SPEED] = true;
    
    if(true) { //counter % 6 == 0
        perception.perceive();
        brain.count();
        brain.executeAction();

        switch(brain.getExecutionResult()) {
            case IDLE:
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                break;
            case MOVE_LEFT: 
                action[Mario.KEY_LEFT]  = true;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                break;
            case MOVE_RIGHT: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = true;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                break;
        case JUMP: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = true;
                action[Mario.KEY_UP]    = false;
                break;
        case JUMP_LEFT: 
                action[Mario.KEY_LEFT]  = true;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = true;
                action[Mario.KEY_UP]    = false;
                break;
        case JUMP_RIGHT: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = true;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = true;
                action[Mario.KEY_UP]    = false;
                break;
        case FIRE: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                action[Mario.KEY_SPEED] = false;
                break;
        case FIRE_LEFT:
                action[Mario.KEY_LEFT]  = true;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                action[Mario.KEY_SPEED] = false;
                break;
        case FIRE_RIGHT: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = true;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                action[Mario.KEY_SPEED] = false;
                break;
        case FIRE_JUMP: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = true;
                action[Mario.KEY_UP]    = false;
                action[Mario.KEY_SPEED] = false;
                break;
        case FIRE_JUMP_LEFT: 
                action[Mario.KEY_LEFT]  = true;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = true;
                action[Mario.KEY_UP]    = false;
                action[Mario.KEY_SPEED] = false;
                break;
        case FIRE_JUMP_RIGHT: 
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = true;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = true;
                action[Mario.KEY_UP]    = false;
                action[Mario.KEY_SPEED] = false;
                break;            
            default:
                action[Mario.KEY_LEFT]  = false;
                action[Mario.KEY_RIGHT] = false;
                action[Mario.KEY_DOWN]  = false;
                action[Mario.KEY_JUMP]  = false;
                action[Mario.KEY_UP]    = false;
                break;
        } 
    }
    counter++;
    // prevent mario from holding down jump
    if(isMarioAbleToJump == false && isMarioOnGround == true) {
        action[Mario.KEY_JUMP] = false;
    }
    return action;
}
}