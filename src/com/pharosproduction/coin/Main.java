package com.pharosproduction.coin;

import com.pharosproduction.coin.blockchain.Transaction;
import com.pharosproduction.coin.blockchain.TxHandler;
import com.pharosproduction.coin.blockchain.UTXO;
import com.pharosproduction.coin.blockchain.UTXOPool;

import java.math.BigInteger;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

  // Life

  public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException {
    final KeyPair duckKP = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    final KeyPair snakeKP = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    final KeyPair dogKP = KeyPairGenerator.getInstance("RSA").generateKeyPair();

    // Tx 1
    Map<PublicKey, Double> values1 = Map.of(duckKP.getPublic(), 100.0);
    Transaction tx1 = buildTransaction(duckKP.getPrivate(), values1, null);
    System.out.println("\nTx1: \n" + tx1.toString());

    // Add to ledger

    UTXOPool pool = new UTXOPool();
    buildUTXO(tx1, pool);

    TxHandler handler = new TxHandler(pool);
    System.out.println("\n" + handler.toString());

    // Tx 2
    Map<PublicKey, Double> values2 = Map.of(snakeKP.getPublic(), 80.0, dogKP.getPublic(), 20.0);
    Transaction tx2 = buildTransaction(duckKP.getPrivate(), values2, tx1);
    System.out.println("\nTx2: \n" + tx2.toString());

    // Add to ledger

    System.out.println("\nAdd two tx2 to ledger");
    Transaction[] transaction = new Transaction[] {tx2, tx2};
    Transaction[] handled = handler.handleTxs(transaction);
    System.out.println("\nProcessed transactions: " + handled.length);
  }

  // Private

  private static Transaction buildTransaction(PrivateKey pk, Map<PublicKey, Double> values, Transaction prevTx) {
    Transaction tx = new Transaction();

    // Add inputs

    if (prevTx != null)
      tx.addInput(prevTx.getHash(), prevTx.numOutputs() - 1);
    else {
      byte[] genesisInputHash = BigInteger.valueOf(0).toByteArray();
      tx.addInput(genesisInputHash, 0);
    }

    // Add outputs

    for (Map.Entry<PublicKey, Double> el : values.entrySet())
      tx.addOutput(el.getValue(), el.getKey());

    // Sign

    try {
      for (int i = 0; i < tx.numInputs(); i++) {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pk);
        sig.update(tx.getRawDataToSign(i));
        tx.addSignature(sig.sign(), i);
      }

      tx.finalize();

      return tx;
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  private static void buildUTXO(Transaction tx, UTXOPool pool) {
    int numOutputs = tx.numOutputs() - 1;
    UTXO utxo = new UTXO(tx.getHash(), numOutputs);
    pool.addUTXO(utxo, tx.getOutput(numOutputs));
  }
}
