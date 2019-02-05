package osetsky;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class GrantBuyerAgent extends Agent {
    // The title of the book to buy
    private String targetBookTitle;
    // The list of known seller agents
    private AID[] sellerAgents;

    // Put agent initializations here
    protected void setup() {
        // Printout a welcome message
        System.out.println("Hallo! Buyer-agent "+getAID().getName()+" is ready.");

        // Get the title of the grant to buy as a start-up argument
        // Get the title of the grant to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            targetBookTitle = (String) args[0];
            System.out.println("Target grant is " + targetBookTitle);

            // Add a TickerBehaviour that schedules a request to seller agents every minute
            addBehaviour(new TickerBehaviour(this, 600) {
                protected void onTick() {
                    System.out.println("Trying to buy " + targetBookTitle);
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("grant-selling");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following seller agents:");
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println("sellerAgents[i]" + sellerAgents[i].getName());
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            } );
        }
        else {
            // Make the agent terminate
            System.out.println("No target grant title specified");
            doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
    }

    /**
     Inner class RequestPerformer.
     This is the behaviour used by Grant-buyer agents to request seller
     agents the target grant.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer
        private int coefficient;
        private int bestPrice;  // The best offered price
        private int bestPeriod = -1; // The best offered period
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetBookTitle);
                    cfp.setConversationId("grant-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("grant-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            String content  = reply.getContent().toString();
                            System.out.println(content);
                            int price = Integer.parseInt(content.substring(0,content.indexOf(",")));
                            int period = Integer.parseInt(content.
                                    substring(content.indexOf(",") + 1),content.length());
                            System.out.println("price: " + price);
                            System.out.println("bestPrice: " + price);
                            System.out.println("period: " + period);
                            System.out.println("bestPeriod: " + bestPeriod);
                            if (bestSeller == null || price < bestPrice) {
                                // This is the best offer at present
                                if (bestPeriod < 0) {
                                    System.out.println(1);
                                    bestPrice = price;
                                    bestPeriod = period;
                                    bestSeller = reply.getSender();
                                } else if (price < bestPrice && period < bestPeriod) {
                                    System.out.println(2);
                                    bestPrice = price;
                                    bestPeriod = period;
                                    bestSeller = reply.getSender();
                                } else if (price < bestPrice){
                                    System.out.println(3);
                                    bestPrice = price;
                                    bestSeller = reply.getSender();
                                }
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                            // We received all replies
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("grant-trade");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("grant-trade"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(targetBookTitle+" successfully purchased from agent "+reply.getSender().getName());
                            System.out.println("Price = "+bestPrice);
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println("Attempt failed: requested grant already sold.");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            System.out.println("bestSeller: " + bestSeller);
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: " + targetBookTitle + " not available for sale");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer
}

