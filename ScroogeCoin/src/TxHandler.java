import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

	/**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }
    
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	return (applyValidTx(utxoPool, tx) != null);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	ArrayList<Transaction> block = new ArrayList<>();
    	for (Transaction tx : possibleTxs) {
        	UTXOPool newPool = applyValidTx(utxoPool, tx);
        	if (newPool != null) {
        		block.add(tx);
        		utxoPool = newPool;
        	}
        }
    	return block.toArray(new Transaction[block.size()]);
    }
    
    public static UTXOPool applyValidTx(UTXOPool pool, Transaction tx) {   	   	
    	// Create a copy of the pool, used only to check the validity of tx
    	UTXOPool checkPool = new UTXOPool(pool); 
    	
    	double inputTotal = 0;
    	
    	// Check inputs
    	for (int i = 0; i < tx.numInputs(); i++) {
    		Transaction.Input input = tx.getInput(i);
    		
    		// Used UTXO in in the current pool    		
    		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
    		if (!checkPool.contains(utxo)) {
    			return null;
    		}
    		
    		// Verify signature
    		Transaction.Output output = checkPool.getTxOutput(utxo);
    		if (!Crypto.verifySignature(
    				output.address, tx.getRawDataToSign(i), input.signature)) {
    			return null;
    		}
    		
    		// Consume the UTXO for the next checks
    		checkPool.removeUTXO(utxo);
    		
    		inputTotal += output.value;
    	}
    	
    	double outputTotal = 0;
    	
    	// Check outputs
    	for (int i = 0; i < tx.numOutputs(); i++) {
    		Transaction.Output output = tx.getOutput(i);
    		double value = output.value;
    		if (value < 0) {
    			return null;
    		}
    		outputTotal += value;
    	}
    	
    	if (outputTotal > inputTotal) {
    		return null;
    	}
    	
    	// Add the new UTXOs in the pool
    	for (int i = 0; i < tx.numOutputs(); i++) {
    		checkPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
    	} 	
    	
    	return checkPool;
    }

}
