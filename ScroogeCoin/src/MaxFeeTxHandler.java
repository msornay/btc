import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;

    private static double fee(UTXOPool pool, Transaction tx) {
    	double inputTotal = 0.0;
    	for (int i = 0; i < tx.numInputs(); i++) {
    		Transaction.Input input = tx.getInput(i);
    		inputTotal += pool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex)).value;
    	}
    	double outputTotal = 0.0;
    	for (int i = 0; i < tx.numOutputs(); i++) {
    		Transaction.Output output = tx.getOutput(i);
    		outputTotal += output.value;
    	}
    	return inputTotal - outputTotal;
    }
    
    private static UTXOPool applyValidTx(UTXOPool pool, Transaction tx) {   	   	
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
    
	/**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
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

    private class FeeComparator implements Comparator<Transaction> {
    	
    	private final UTXOPool pool;
    	
    	public FeeComparator(UTXOPool pool) {
			this.pool = pool;
		}
    	
		@Override
		public int compare(Transaction txA, Transaction txB) {
			return Double.compare(fee(pool, txA), fee(pool, txB));
		}    	
    }
    
    private boolean tryHandle(LinkedList<Transaction> possibleBlock) {
    	UTXOPool pool = new UTXOPool(utxoPool);
    	for (Transaction tx : possibleBlock) {
        	UTXOPool newPool = applyValidTx(pool, tx);
        	if (newPool == null) {
        		return false;
        	}         	
        	pool = newPool;
        }
    	utxoPool = pool;
    	return true;
    }
    
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	LinkedList<Transaction> block = new LinkedList<>();    	
    	Collections.sort(block, new FeeComparator(utxoPool));
    	
    	while(!tryHandle(block)) {
    		block.pop();
    	}
    	
    	return block.toArray(new Transaction[block.size()]);
    }
    
}
