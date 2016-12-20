import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	
	private final int numRound;
	private int currentRound = 0;
	
	private int nFollowees;
	
	private Set<Transaction> pendingTransactions;
	
	private final HashMap<Transaction, HashSet<Integer>> votes = new HashMap<>(); 
	
	
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.numRound = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        nFollowees = followees.length;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        return pendingTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) { 	
    	  	
        for (Candidate c: candidates) {
        	HashSet<Integer> senders = votes.getOrDefault(c.tx, new HashSet<>());
        	senders.add(c.sender);
        	votes.put(c.tx, senders);        	
        } 
                	
        int threshold = (int) ((currentRound * 0.5 / numRound) * nFollowees);
        
        
        // XXX : redo pending transactio nfrom scratch according to threshold
        // XXX : non-linear threshold
        
        for (Map.Entry<Transaction, HashSet<Integer>> entry : votes.entrySet()) {
        	
        	if (entry.getValue().size() >= threshold) {
        		pendingTransactions.add(entry.getKey());
        	}
        }
        
        currentRound += 1;
    }
}
