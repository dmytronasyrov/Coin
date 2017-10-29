package com.pharosproduction.coin.blockchain;

import java.util.ArrayList;

public class TxHandler {

  // Variables

  private UTXOPool mPool;

  // Public

  /**
   * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
   * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
   * constructor.
   */
  public TxHandler(UTXOPool utxoPool) {
    mPool = new UTXOPool(utxoPool);
  }

  /**
   * @return true if:
   * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
   * (2) the signatures on each input of {@code tx} are valid,
   * (3) no UTXO is claimed multiple times by {@code tx},
   * (4) all of {@code tx}s output values are non-negative, and
   * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
   * values; and false otherwise.
   */
  public boolean isValidTx(Transaction tx) {
    // (1) All outputs are in the pool
    for (Transaction.Input input : tx.getInputs()) {
      UTXO txUTXO = new UTXO(input.prevTxHash, input.outputIndex);

      if (!mPool.contains(txUTXO))
        return false;
    }

    // (2) Signatures on inputs are valid
    for (int i = 0; i < tx.numInputs(); i++) {
      Transaction.Input input = tx.getInput(i);
      UTXO inputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
      Transaction.Output output = mPool.getTxOutput(inputUTXO);

      if (output == null || !Crypto.verifySignature(output.mAddress, tx.getRawDataToSign(i), input.signature))
        return false;
    }

    // (3) no UTXO is claimed multiple times
    UTXOPool uniquePool = new UTXOPool();

    for (int i = 0; i < tx.numInputs(); i++) {
      Transaction.Input input = tx.getInput(i);
      UTXO inputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
      Transaction.Output output = mPool.getTxOutput(inputUTXO);

      if (uniquePool.contains(inputUTXO))
        return false;

      uniquePool.addUTXO(inputUTXO, output);
    }

    // (4) non negative outputs
    for (Transaction.Output output : tx.getOutputs()) {
      if (output.mValue < 0)
        return false;
    }

    // (5) double spend
    ArrayList<Transaction.Output> uniqueInputOutputs = new ArrayList<>();

    for (int i = 0; i < tx.numInputs(); i++) {
      Transaction.Input input = tx.getInput(i);
      UTXO inputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
      Transaction.Output output = mPool.getTxOutput(inputUTXO);

      if (!uniqueInputOutputs.contains(output))
        uniqueInputOutputs.add(output);
      else
        return false;
    }

    Double inputSum = uniqueInputOutputs.stream().map(output -> output.mValue).reduce(0.0, Double::sum);
    Double outputSum = tx.getOutputs().stream().map(output -> output.mValue).reduce(0.0, Double::sum);

    return inputSum >= outputSum;
  }

  /**
   * Handles each epoch by receiving an unordered array of proposed transactions, checking each
   * transaction for correctness, returning a mutually valid array of accepted transactions, and
   * updating the current UTXO pool as appropriate.
   */
  public Transaction[] handleTxs(Transaction[] possibleTxs) {
    ArrayList<Transaction> validTxs = new ArrayList<>();

    for (Transaction tx : possibleTxs) {
      if(!isValidTx(tx))
        continue;

      validTxs.add(tx);

      for (int i = 0; i < tx.numOutputs(); i++) {
        UTXO currOutUTXO = new UTXO(tx.getHash(), i);
        mPool.addUTXO(currOutUTXO, tx.getOutput(i)); // add unspent
      }

      for (Transaction.Input in : tx.getInputs()) {
        UTXO currInUTXO = new UTXO(in.prevTxHash, in.outputIndex);
        mPool.removeUTXO(currInUTXO); // remove spent
      }
    }

    Transaction[] resultTxs = new Transaction[validTxs.size()];
    return validTxs.toArray(resultTxs);
  }

  // Misc

  @Override
  public String toString() {
    return "Transaction Handler:\n" + mPool.toString();
  }
}
