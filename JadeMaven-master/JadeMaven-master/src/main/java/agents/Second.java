package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class Second extends Agent {
    private Logger myLogger = Logger.getMyLogger(getClass().getName());
    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {
                doWait();
                ACLMessage msg = receive();
                if(msg != null) {
                    ACLMessage reply = msg.createReply();
                    System.out.print("i received "+msg.getContent());
                    if(msg.getPerformative()== ACLMessage.REQUEST){
                        String content = msg.getContent();

                        if ((content != null) && (content.indexOf(content) != -1)){
                            //System.out.println(content+" at line 25");
                            myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received PING Request from "+msg.getSender().getLocalName());
                            reply.setPerformative(ACLMessage.INFORM);
                            System.out.println(reply+"   reply");
                            reply.setContent("pong");
                            System.out.println("pong");
                        }
                        else {
                            myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                            reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");
                        }
                        send(reply);
                } else {
                    System.out.print("i didn't receive msg ");
                    block();
                }





            }
        }});
    }
}