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
 import jade.lang.acl.ACLMessage;
 import jade.domain.FIPAAgentManagement.ServiceDescription;
 import jade.domain.FIPAAgentManagement.DFAgentDescription;
 import jade.domain.DFService;
 import jade.domain.FIPAException;
 import jade.lang.acl.MessageTemplate;
 import jade.util.Logger;
 import jade.core.behaviours.*;
 import org.json.simple.JSONArray;
 import org.json.simple.JSONObject;
 import org.json.simple.parser.JSONParser;
 import org.json.simple.parser.ParseException;
 import jade.wrapper.StateBase;
 import jade.wrapper.AgentState;
 /**
 
  * @author Zannatul Ferdous - CSELT S.p.A.
  * @version  $Date: 2024-04-3 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
  */
 public class ConveyorAgent extends Agent {
     // The title of the book to buy
     private String neighbour;
 
     // The list of known seller agents
     private AID[] conveyorAgents;
     private Logger myLogger;
     private String state ;
     private boolean finished = false;
 
     // Put agent initializations here
     protected void setup() {
         DFAgentDescription dfd = new DFAgentDescription();
         dfd.setName(getAID());
         ServiceDescription sd = new ServiceDescription();
         sd.setType("Pallet moving");
         sd.setName("conv-Agent");
         dfd.addServices(sd);
 
 
         try {
             DFService.register(this, dfd);
             System.out.println("succesfully registered");
         }
         catch (FIPAException fe) {
             fe.printStackTrace();
         }
         // Printout a welcome message
         System.out.println("Hello! I am conveyor agent " + getAID().getName() + " .");
 
         // Get the title of the book to buy as a start-up argument
         Object[] args = getArguments();
         if (args != null && args.length > 0) {
             neighbour = (String) args[0];
             System.out.println(neighbour + " is available");
 
             // Add a TickerBehaviour that schedules a request to seller agents every minute
             addBehaviour(new TickerBehaviour(this, 30000) {
 
                 protected void onTick() {
                     myAgent.doWait();
                     System.out.println("Trying to reach " + neighbour);
                     // Update the list of seller agents
                     DFAgentDescription template = new DFAgentDescription();
                     ServiceDescription sd = new ServiceDescription();
                     sd.setType("Pallet moving");
                     template.addServices(sd);
 
                     try {
                         DFAgentDescription[] result = DFService.search(myAgent, template);
                         //System.out.println(myAgent);
 
 
                         conveyorAgents = new AID[result.length];
                         System.out.println("conveyorAgent num: "+ result.length);
 
                         for (int i = 0; i < result.length; ++i) {
 
                             conveyorAgents[i] = result[i].getName();
 
                         }
                     } catch (FIPAException fe) {
                         fe.printStackTrace();
                     }
 
                     // Perform the request
 
                     myAgent.addBehaviour(new RequestPerformer());
                 }
             });
         }
     }
 
     // Put agent clean-up operations here
     protected void takeDown() {
         // Printout a dismissal message
         System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
     }
 
     /**
      * Inner class RequestPerformer.
      * This is the behaviour used by Book-buyer agents to request seller
      * agents the target book. ACLMessage reply = myAgent.receive(mt);
      */
     private class RequestPerformer extends Behaviour {
 
         private MessageTemplate mt; // The template to receive replies
         private int step = 0;
 
 
 
         public void action() {
             switch (step) {
                 case 0:
                     // Send the cfp to all sellers
                     ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
 
                     try {
 
                         // Parse the JSON string
                         JSONParser parser = new JSONParser();
                         JSONObject jsonObject = (JSONObject) parser.parse(neighbour);
 
                         // Get the "neighbours" array
                         JSONArray neighboursArray = (JSONArray) jsonObject.get("neighbours");
                         System.out.println("neighboursArray"+ neighboursArray);
                        // String neighbour = (String) jsonObject.get("neighbour");
 
 
 
                         // Access the first element of the array (index 0)
                         String convValue= (String) neighboursArray.get(0);
                         for (int i = 0; i < conveyorAgents.length; ++i) {
 
                             if (conveyorAgents[i].getName().contains(convValue)) {
                                 String correctNeighbour;
                                 correctNeighbour= convValue;
 
 
                                 System.out.println("correctNeighbour  "+ correctNeighbour);
                                 cfp.addReceiver(conveyorAgents[i]);
                                 Object[] args = getArguments();
                                 if (args != null && args.length > 0) {
                                     neighbour = (String) args[0];
                                     System.out.println(neighbour + " is available");
                                     JSONObject jo= new JSONObject();
                                     jo.put("neighbour",neighbour);
                                 state = "Busy";
                                 jo.put("status",state);
                                 cfp.setContent(jo.toJSONString());}
                                 cfp.setConversationId("Move-Pallet");
                                 cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                                 myAgent.send(cfp);
 
 
                             } else {
                                 state = "Idle";
                                 cfp.setContent(state);
 
                             }
                             // Print the value of CONV4
 
 
                         }
                     } catch (ParseException e) {
                         e.printStackTrace();
                     }
 
                     // Prepare the template to get proposals
                     mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                             MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                     step = 1;
                     break;
                 case 1:
                     // Receive all proposals/refusals from seller agents
                     ACLMessage reply = myAgent.receive(mt);
 
                     if (reply != null) {
                         // Reply received
                         if (reply.getPerformative() == ACLMessage.INFORM) {
                             String content = reply.getContent();
 
                             if ((content != null)) {
                                 myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received PING Request from " + reply.getSender().getLocalName());
                                 reply.setPerformative(ACLMessage.INFORM);
                                 reply.setContent("pong");
                             } else {
                                 myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected request [" + content + "] received from " + reply.getSender().getLocalName());
                                 reply.setPerformative(ACLMessage.REFUSE);
                                 reply.setContent("( UnexpectedContent (" + content + "))");
                             }
 
                         } else {
                             myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(reply.getPerformative()) + "] received from " + reply.getSender().getLocalName());
                             reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                             reply.setContent("( (Unexpected-act " + ACLMessage.getPerformative(reply.getPerformative()) + ") )");
                         }
                         send(reply);
                         System.out.println("reply " +
                                 "" +
                                 "" +
                                 "" + reply);
                     } else {
                         block();
                     }
             }
         } // END of inner class
 
         @Override
         public boolean done() {
             return false;
         }
     }
 }
 
 
 
 
         /*ACLMessage  reply = myAgent.receive();
                if(reply != null){
                    ACLMessage reply = reply.createReply();
 
                    if(reply.getPerformative()== ACLMessage.INFORM){
                        String content = reply.getContent();
                        if ((content != null) && (content.indexOf(content) != -1)){
                            myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received "+content+" Request from "+reply.getSender().getLocalName());
                            reply.setPerformative(ACLMessage.INFORM);
                            JSONParser parser = new JSONParser();
                            try {
                                JSONObject jsonObj = (JSONObject) parser.parse(content);
 
                                String dest = (String) jsonObj.get("dest");
                                route= (JSONArray) jsonObj.get("midpoints");
                                System.out.println("Required destination "+dest + "routes: "+ route);
 
                                System.out.println(jsonObj);
 
                                //Adding midPoint
                                String name = getLocalName();
                                route.add(name);
                                System.out.println("after adding "+jsonObj);
 
 
                                String jsonStr=jsonObj.toString();
 
                                reply.setContent(jsonStr);
 
 
                            } catch (org.json.simple.parser.ParseException e) {
                                e.printStackTrace();
                            }
                            reply.setPerformative(ACLMessage.INFORM);
                            send(reply);
 
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
                }*/
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
 import jade.lang.acl.ACLMessage;
 import jade.domain.FIPAAgentManagement.ServiceDescription;
 import jade.domain.FIPAAgentManagement.DFAgentDescription;
 import jade.domain.DFService;
 import jade.domain.FIPAException;
 import jade.lang.acl.MessageTemplate;
 import jade.util.Logger;
 import jade.core.behaviours.*;
 import org.json.simple.JSONArray;
 import org.json.simple.JSONObject;
 import org.json.simple.parser.JSONParser;
 import org.json.simple.parser.ParseException;
 import jade.wrapper.StateBase;
 import jade.wrapper.AgentState;
 /**
 
  * @author Zannatul Ferdous - CSELT S.p.A.
  * @version  $Date: 2024-04-3 13:08:55 +0200 (gio, 08 apr 2010) $ $Revision: 6297 $
  */
 public class ConveyorAgent extends Agent {
     // The title of the book to buy
     private String neighbour;
 
     // The list of known seller agents
     private AID[] conveyorAgents;
     private Logger myLogger;
     private String state ;
     private boolean finished = false;
 
     // Put agent initializations here
     protected void setup() {
         DFAgentDescription dfd = new DFAgentDescription();
         dfd.setName(getAID());
         ServiceDescription sd = new ServiceDescription();
         sd.setType("Pallet moving");
         sd.setName("conv-Agent");
         dfd.addServices(sd);
 
 
         try {
             DFService.register(this, dfd);
             System.out.println("succesfully registered");
         }
         catch (FIPAException fe) {
             fe.printStackTrace();
         }
         // Printout a welcome message
         System.out.println("Hello! I am conveyor agent " + getAID().getName() + " .");
 
         // Get the title of the book to buy as a start-up argument
         Object[] args = getArguments();
         if (args != null && args.length > 0) {
             neighbour = (String) args[0];
             System.out.println(neighbour + " is available");
 
             // Add a TickerBehaviour that schedules a request to seller agents every minute
             addBehaviour(new TickerBehaviour(this, 30000) {
 
                 protected void onTick() {
                     myAgent.doWait();
                     System.out.println("Trying to reach " + neighbour);
                     // Update the list of seller agents
                     DFAgentDescription template = new DFAgentDescription();
                     ServiceDescription sd = new ServiceDescription();
                     sd.setType("Pallet moving");
                     template.addServices(sd);
 
                     try {
                         DFAgentDescription[] result = DFService.search(myAgent, template);
                         //System.out.println(myAgent);
 
 
                         conveyorAgents = new AID[result.length];
                         System.out.println("conveyorAgent num: "+ result.length);
 
                         for (int i = 0; i < result.length; ++i) {
 
                             conveyorAgents[i] = result[i].getName();
 
                         }
                     } catch (FIPAException fe) {
                         fe.printStackTrace();
                     }
 
                     // Perform the request
 
                     myAgent.addBehaviour(new RequestPerformer());
                 }
             });
         }
     }
 
     // Put agent clean-up operations here
     protected void takeDown() {
         // Printout a dismissal message
         System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
     }
 
     /**
      * Inner class RequestPerformer.
      * This is the behaviour used by Book-buyer agents to request seller
      * agents the target book. ACLMessage reply = myAgent.receive(mt);
      */
     private class RequestPerformer extends Behaviour {
 
         private MessageTemplate mt; // The template to receive replies
         private int step = 0;
 
 
 
         public void action() {
             switch (step) {
                 case 0:
                     // Send the cfp to all sellers
                     ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
 
                     try {
 
                         // Parse the JSON string
                         JSONParser parser = new JSONParser();
                         JSONObject jsonObject = (JSONObject) parser.parse(neighbour);
 
                         // Get the "neighbours" array
                         JSONArray neighboursArray = (JSONArray) jsonObject.get("neighbours");
                         System.out.println("neighboursArray"+ neighboursArray);
                        // String neighbour = (String) jsonObject.get("neighbour");
 
 
 
                         // Access the first element of the array (index 0)
                         String convValue= (String) neighboursArray.get(0);
                         for (int i = 0; i < conveyorAgents.length; ++i) {
 
                             if (conveyorAgents[i].getName().contains(convValue)) {
                                 String correctNeighbour;
                                 correctNeighbour= convValue;
 
 
                                 System.out.println("correctNeighbour  "+ correctNeighbour);
                                 cfp.addReceiver(conveyorAgents[i]);
                                 Object[] args = getArguments();
                                 if (args != null && args.length > 0) {
                                     neighbour = (String) args[0];
                                     System.out.println(neighbour + " is available");
                                     JSONObject jo= new JSONObject();
                                     jo.put("neighbour",neighbour);
                                 state = "Busy";
                                 jo.put("status",state);
                                 cfp.setContent(jo.toJSONString());}
                                 cfp.setConversationId("Move-Pallet");
                                 cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                                 myAgent.send(cfp);
 
 
                             } else {
                                 state = "Idle";
                                 cfp.setContent(state);
 
                             }
                             // Print the value of CONV4
 
 
                         }
                     } catch (ParseException e) {
                         e.printStackTrace();
                     }
 
                     // Prepare the template to get proposals
                     mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                             MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                     step = 1;
                     break;
                 case 1:
                     // Receive all proposals/refusals from seller agents
                     ACLMessage reply = myAgent.receive(mt);
 
                     if (reply != null) {
                         // Reply received
                         if (reply.getPerformative() == ACLMessage.INFORM) {
                             String content = reply.getContent();
 
                             if ((content != null)) {
                                 myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received PING Request from " + reply.getSender().getLocalName());
                                 reply.setPerformative(ACLMessage.INFORM);
                                 reply.setContent("pong");
                             } else {
                                 myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected request [" + content + "] received from " + reply.getSender().getLocalName());
                                 reply.setPerformative(ACLMessage.REFUSE);
                                 reply.setContent("( UnexpectedContent (" + content + "))");
                             }
 
                         } else {
                             myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(reply.getPerformative()) + "] received from " + reply.getSender().getLocalName());
                             reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                             reply.setContent("( (Unexpected-act " + ACLMessage.getPerformative(reply.getPerformative()) + ") )");
                         }
                         send(reply);
                         System.out.println("reply " +
                                 "" +
                                 "" +
                                 "" + reply);
                     } else {
                         block();
                     }
             }
         } // END of inner class
 
         @Override
         public boolean done() {
             return false;
         }
     }
 }
 
 
 
 
         /*ACLMessage  reply = myAgent.receive();
                if(reply != null){
                    ACLMessage reply = reply.createReply();
 
                    if(reply.getPerformative()== ACLMessage.INFORM){
                        String content = reply.getContent();
                        if ((content != null) && (content.indexOf(content) != -1)){
                            myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received "+content+" Request from "+reply.getSender().getLocalName());
                            reply.setPerformative(ACLMessage.INFORM);
                            JSONParser parser = new JSONParser();
                            try {
                                JSONObject jsonObj = (JSONObject) parser.parse(content);
 
                                String dest = (String) jsonObj.get("dest");
                                route= (JSONArray) jsonObj.get("midpoints");
                                System.out.println("Required destination "+dest + "routes: "+ route);
 
                                System.out.println(jsonObj);
 
                                //Adding midPoint
                                String name = getLocalName();
                                route.add(name);
                                System.out.println("after adding "+jsonObj);
 
 
                                String jsonStr=jsonObj.toString();
 
                                reply.setContent(jsonStr);
 
 
                            } catch (org.json.simple.parser.ParseException e) {
                                e.printStackTrace();
                            }
                            reply.setPerformative(ACLMessage.INFORM);
                            send(reply);
 
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
                }*/
  