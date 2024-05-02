

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

import java.util.*;

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
    private String state;
    private int transferTime = 5; // Configurable transfer time
    private String path;
    private String goal="CONV12";
    private String smallest_dis;

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
                    System.out.println("Trying to reach " + neighbour);
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Pallet moving");
                    template.addServices(sd);
                    state="Idle";
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        //System.out.println(myAgent);


                        conveyorAgents = new AID[result.length];
                        System.out.println("conveyorAgent no: "+ result.length);

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
        } else {
            // Make the agent terminate
            System.out.println("No neighbour specified");
            doDelete();
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
     * agents the target book.
     */
    private class RequestPerformer extends Behaviour {

        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {

                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    try {
                        // Parse the JSON string
                        JSONParser parser = new JSONParser();
                        JSONObject jsonObject = (JSONObject) parser.parse(neighbour    );

                        // Get the "neighbours" array
                        JSONArray neighboursArray = (JSONArray) jsonObject.get("neighbours"); //destination remains

                        // Access the first element of the array (index 0)
                        String convValue= (String) neighboursArray.get(0);

                        for (int i = 0; i < conveyorAgents.length; ++i) {

                            // Print the value of CONV4

                            if (conveyorAgents[i].getName().contains(convValue)) {
                                System.out.println(conveyorAgents[i].getName());
                                cfp.addReceiver(conveyorAgents[i]);

                                state = "Busy";
                                jsonObject.put("source",myAgent.getLocalName()   );

                                jsonObject.put("goal",goal);
                                jsonObject.put("path",convValue);
                                System.out.println(jsonObject);
                                cfp.setContent(jsonObject.toJSONString());


                                cfp.setConversationId("book-trade");
                                cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                                myAgent.send(cfp);


                            }


                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    System.out.println("replyContent"+ reply);
                    if (reply != null) {
                        // Parse the JSON string
                        JSONParser parser = new JSONParser();
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = (JSONObject) parser.parse(neighbour    );
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                        // Get the "neighbours" array
                        JSONArray neighboursArray = (JSONArray) jsonObject.get("neighbours"); //destination remains
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            String content = reply.getContent();


                            JSONObject replyContent = null;
                            try {
                                replyContent = (JSONObject) parser.parse(reply.getContent());
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }

                            // get the cost value from the json
                            String replied_goal = (String) replyContent.get("goal");
                            // String source_return = (String) replyContent.get("source");
                            // parse neighbour array and store in the neighbours attribute
                            JSONArray replied_path = (JSONArray) replyContent.get("path");

                            // Determine the shortest path from source to destination
                            String source = "CONV1"; // Example source conveyor
                            String destination = "CONV14"; // Example destination conveyor

                            List<String> shortestPath = findShortestPath(source, destination, neighboursArray);

                            // Transfer the pallet along the shortest path
                            if (!shortestPath.isEmpty()) {
                                System.out.println("Shortest path from " + source + " to " + destination + ": " + shortestPath);
                                // Implement pallet transfer logic here
                                // Send messages to conveyors along the shortest path to initiate pallet transfer
                            } else {
                                System.out.println("No path found from " + source + " to " + destination);
                            }



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

        @Override
        public boolean done() {
            return false;
        }
    } // END of inner class



    private List<String> findShortestPath(String source, String destination, JSONArray neighbors) {
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parentMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(source);
        visited.add(source);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(destination)) {
                // Reconstruct the path
                List<String> path = new ArrayList<>();
                while (current != null) {
                    path.add(current);
                    current = parentMap.get(current);
                }
                Collections.reverse(path);
                return path;
            }
            // Iterate over neighbors of the current conveyor
            for (Object neighborObj : neighbors) {
                String neighbor = (String) neighborObj;
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

}



        // Other methods...

