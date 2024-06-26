/**
 * @Author: mdrhri-6
 * @Date:   2016-10-10T00:07:01+02:00
 * @Last modified by:   mdrhri-6
 * @Last modified time: 2016-10-17T16:29:37+02:00
 */



package agent;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;

import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;



public class Requester extends GuiAgent {

    private String receiver = "Amir";
    private String content = "Hola";
    private String messagePerformative="";
    public	ArrayList<String> agentList;
    public static int agentCounterInitial = 0;
    public static int agentCounterFinal = 0;

    protected void setup() {
        // Printout a welcome message
        System.out.println("Messenger agent "+getAID().getName()+" is ready.");

        /*This part will query the AMS to get list of all active agents in all containers*/
        agentList	=	new ArrayList();


           /** This piece of code, to register services with the DF, is explained
         * in the book in section 4.4.2.1, page 73
         **/
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("messenger-agent");
        sd.setName(getLocalName()+"-Messenger agent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new ReceiveMessage());
    }




    //Sending message is an implementation of OneShotBehavior(Send once for one time)
    public class SendMessage extends OneShotBehaviour {

        // Send message from to someone
        public void action() {
            ACLMessage msg;
            if(messagePerformative.equals("Propose")){
                msg = new ACLMessage(ACLMessage.PROPOSE);
            }else{
                msg = new ACLMessage(ACLMessage.REQUEST);
            }

            //the receiver variable is set in getFromGUI() method
            msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
            msg.setLanguage("English");
            msg.setContent(content);

            System.out.println("content"+content);
            send(msg);

            //saveToFile(getAID().getLocalName() +":"+ content);

            System.out.println(getAID().getName()+" sent a message to "+receiver+"\n"+
                    "Content of the message is: "+ msg.getContent());
        }
    }

    //Receiving message is an implementation of CyclicBehavior(Receive until takeDown() is executed)

    public class ReceiveMessage extends CyclicBehaviour {

        // Variable for the contents of the received Message
        private String messagePerformative;
        private String messageContent;
        private String SenderName;
        private String MyPlan;

        // Receive message and append it in the conversation textArea in the GUI
        public void action() {
            ACLMessage msg = receive();
            if(msg != null) {

                messagePerformative = msg.getPerformative(msg.getPerformative());
                messageContent = msg.getContent();
                SenderName = msg.getSender().getLocalName();

                // print the message details in console
                System.out.println("**** " + getAID().getLocalName() + " received a message" +"\n"+
                        "Sender name: "+ SenderName+"\n"+
                        "Content of the message: " + messageContent + "\n"+
                        "Message type: " + messagePerformative + "\n");
                System.out.println("**********************************");


            }

        }
    }
    //get all entered input from gui agent
    public void getFromGui(final String messageType, final String dest, final String messages) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                messagePerformative = messageType;
                receiver = dest;
                content = messages;
            }
        } );
    }

    //predefined function of GuiAgent - see postGuiEvent() in MessageAgentGui.java
    @Override
    protected void onGuiEvent(GuiEvent arg0) {
        // TODO Auto-generated method stub
        addBehaviour(new SendMessage());

    }

    // if new agents are created after instantiating this object
    // this method will keep the lists updated
 }