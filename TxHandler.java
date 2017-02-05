import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
    }

    public boolean isValidTx(Transaction tx) {
        double inputSum = 0;
        double outputSum = 0;

        HashSet<UTXO> usedUtxo = new HashSet<>();

        int index = 0;

        for (Transaction.Input input : tx.getInputs()) {
            UTXO curUtxo = new UTXO(input.prevTxHash, input.outputIndex);

            // (1) all outputs claimed by {@code tx} are in the current UTXO pool
            if (!utxoPool.contains(curUtxo)) { return false; }

            // (2) the signatures on each input of {@code tx} are valid,
            if (!Crypto.verifySignature(utxoPool.getTxOutput(curUtxo).address,
                    tx.getRawDataToSign(index), input.signature)) { return false; }

            // (3) no UTXO is claimed multiple times by tx
            if (usedUtxo.contains(curUtxo)) { return false; }
            usedUtxo.add(curUtxo);

            inputSum += utxoPool.getTxOutput(curUtxo).value;

            ++index;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            //(4) all of {@code tx}s output values are non-negative
            if (output.value < 0) { return false; }
            outputSum += output.value;
        }

        //(5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        //  values; and false otherwise.

        if (inputSum < outputSum) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTxs = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                acceptedTxs.add(tx);

                for (Transaction.Input input : tx.getInputs()) {
                    UTXO curUtxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(curUtxo);
                }


                ArrayList<Transaction.Output> outputs = tx.getOutputs();
                for (int i = 0; i < outputs.size(); ++i) {
                    UTXO newUtxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(newUtxo, outputs.get(i));
                }
            }
        }
        return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }
}
