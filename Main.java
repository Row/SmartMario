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

import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.GamePlayTask;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.MarioAIOptions;
//import ch.idsia.agents.AgentsPool;
import ch.idsia.agents.Agent;
import pl.gdan.elsy.qconf.Brain;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import java.util.Random;
import java.io.*;

public final class Main
{
public static void main(String[] args)
{
// final String argsString = "-vis on";
    final MarioAIOptions marioAIOptions = new MarioAIOptions(args);
    final SmartAgent agent = new SmartAgent();    
    marioAIOptions.setAgent(agent);
    marioAIOptions.setFPS(50);
    //marioAIOptions.setLevelHeight(40);
    String brainPath = "brain";
    final GamePlayTask basicTask = new GamePlayTask(marioAIOptions);
    
    Random r = new Random();
    int numberOfPlayedGames = 0;
    int timeToBackup = 10000;
    int lastHundred = 0;
    double hundredWR = 0.0;
    String data[] = new String[11];
    int dataIndex = 0;
    int backupNo = 0;
    int lastBackup = 0;
    
    for (int i = 0; true; ++i)
    {
        int seed = r.nextInt(20000);
        do
        {
            if(timeToBackup % 1000 == 0){
                hundredWR = lastHundred/1000.0;
                lastHundred = 0;
                if(dataIndex >= data.length){
                    String temp[] = new String[2*data.length];
                    System.arraycopy(data, 0, temp, 0, dataIndex);
                    data = temp;
                }
                data[dataIndex] = "Winn ratio last 1000 epochs: " + hundredWR + " ";
                dataIndex++;
            }
            marioAIOptions.setLevelDifficulty(1);
            marioAIOptions.setLevelRandSeed(1);
            //marioAIOptions.setFlatLevel(true);
            //marioAIOptions.setLevelLength(50);
            seed = r.nextInt(20000);
            basicTask.setOptionsAndReset(marioAIOptions);
            basicTask.runSingleEpisode(1);
            basicTask.doEpisodes(1, true, 1);
            //System.out.println(basicTask.getEnvironment().getEvaluationInfoAsString());
            System.out.println(  "Winns: " + i + " Games played: " + numberOfPlayedGames + 
                                " Total winns percentage: " + Integer.valueOf(i).floatValue()/Integer.valueOf(numberOfPlayedGames).floatValue()
                                + " Last hundred winns percentage" + hundredWR);
            if(timeToBackup <= 0){
                try{
                    agent.brain.save(brainPath + backupNo + ".ser");
                    FileWriter fstream = new FileWriter("data" + backupNo + ".txt");
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write("Backup no " + backupNo);
                    out.newLine();
                    for(int backupI = 0; backupI < dataIndex; backupI++){
                        out.write(data[backupI]);
                        out.newLine();
                    }
                    dataIndex = 0;
                    backupNo++;
                    out.close();

                }catch(Exception e){System.out.println("Failed to save brain"); System.exit(1);}
                timeToBackup = 10000;
                System.out.println("Winns:" + i);
            }
            timeToBackup--;
            numberOfPlayedGames++;
        } while (basicTask.getEnvironment().getEvaluationInfo().marioStatus != Environment.MARIO_STATUS_WIN);
        lastHundred++;
    }
}
}