/*****************************************************************
 JADE - Java Agent DEvelopment Framework is a framework to develop
 multi-agent systems in compliance with the FIPA specifications.
 Copyright (C) 2000 CSELT S.p.A.

 GNU Lesser General Public License

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation,
 version 2.1 of the License.


 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/

package agents;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;
//Create agents systematically


import jade.wrapper.ContainerController;

import jade.wrapper.StaleProxyException;

import jade.wrapper.AgentController;

/**
 * This agent implements a simple Ping Agent that registers itself with the DF and
 * then waits for ACLMessages.
 * If  a REQUEST message is received containing the string "ping" within the content
 * then it replies with an INFORM message whose content will be the string "pong".
 *
 * @author Tiziana Trucco - CSELT S.p.A.
 * @version  $Date: 2010-04-08 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
 */
public class LayoutAgent extends Agent{

    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    private class WaitPingAndReplyBehaviour extends OneShotBehaviour{


        public WaitPingAndReplyBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            System.out.println("test");//running all time
            ACLMessage  msg = myAgent.receive();
            if(msg != null){
                ACLMessage reply = msg.createReply();

                if(msg.getPerformative()== ACLMessage.REQUEST){
                    String content = msg.getContent();
                    System.out.println("content "+content);

                    if ((content != null) && (content.indexOf(content) != -1)){
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received PING Request from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("pong");
                    }
                    else{
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected request ["+content+"] received from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("( UnexpectedContent ("+content+"))");
                    }

                }
                else {
                    myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");
                }
                send(reply);
            }
            else {
                block();
            }
        }
    } // END of inner class WaitPingAndReplyBehaviour


    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription(); //DISCOVER AGENTS
        ServiceDescription sd = new ServiceDescription();
        sd.setType("LayoutAgent");
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
            WaitPingAndReplyBehaviour PingBehaviour = new  WaitPingAndReplyBehaviour(this);
            addBehaviour(PingBehaviour);
            ContainerController cc = getContainerController();

            AgentController t1 = null;
            t1 = cc.createNewAgent("CONV1", "agents.ConveyorAgent", new Object[] {"{\"neighbours\":[\"CONV2\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV2", "agents.ConveyorAgent", new Object[]{"{\"neighbours\":[\"CONV3\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV3", "agents.ConveyorAgent", new Object[] {"{\"neighbours\":[\"CONV4\",\"CONV13\"]}"});
            t1.start();
            /*t1 = cc.createNewAgent("CONV4", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV5\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV5", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV6\",\"CONV3\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV6", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV7\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV7", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV8\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV8", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV9\",\"CONV14\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV9", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV10\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV10", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV11\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV11", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV12\",\"CONV14\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV12", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV1\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV13", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV9\",\"CONV14\"]}"});
            t1.start();
            t1 = cc.createNewAgent("CONV14", "agents.ConveyorAgent", new Object[]{"{\"cost\":\"2\",\"neighbours\":[\"CONV12\"]}"});
            t1.start();
*/


        } catch (FIPAException | StaleProxyException e) {
            myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }
}