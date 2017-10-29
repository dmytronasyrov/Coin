package com.pharosproduction.coin.blockchain;

import javafx.util.Builder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class Transaction {

  // Variables
  /**
   * hash of the transaction, its unique id
   */
  private byte[] mHash;
  private ArrayList<Input> mInputs;
  private ArrayList<Output> mOutputs;

  // Public

  public Transaction() {
    mInputs = new ArrayList<>();
    mOutputs = new ArrayList<>();
  }

  public Transaction(Transaction tx) {
    mHash = tx.mHash.clone();
    mInputs = new ArrayList<>(tx.mInputs);
    mOutputs = new ArrayList<>(tx.mOutputs);
  }

  public int numInputs() {
    return mInputs.size();
  }

  public int numOutputs() {
    return mOutputs.size();
  }

  public void addInput(byte[] prevTxHash, int outputIndex) {
    mInputs.add(new Input(prevTxHash, outputIndex));
  }

  public Input getInput(int index) {
    return (index < mInputs.size() ? mInputs.get(index) : null);
  }

  public void addOutput(double value, PublicKey address) {
    mOutputs.add(new Output(value, address));
  }

  public Output getOutput(int index) {
    return (index < mOutputs.size() ? mOutputs.get(index) : null);
  }

  public void removeInput(int index) {
    mInputs.remove(index);
  }

  public void removeInput(UTXO ut) {
    for (Input in : mInputs) {
      UTXO u = new UTXO(in.prevTxHash, in.outputIndex);

      if (u.equals(ut)) {
        mInputs.remove(in);
        break;
      }
    }
  }

  public byte[] getRawDataToSign(int index) {
    ArrayList<Byte> sigData = new ArrayList<>(); // ith input and all outputs

    if (index > mInputs.size())
      return null;

    Input in = mInputs.get(index);
    byte[] prevTxHash = in.prevTxHash;
    ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
    b.putInt(in.outputIndex);
    byte[] outputIndex = b.array();

    if (prevTxHash != null)
      for (byte aPrevTxHash : prevTxHash)
        sigData.add(aPrevTxHash);

    for (byte anOutputIndex : outputIndex)
      sigData.add(anOutputIndex);

    outputsToByteData(sigData);

    byte[] sigD = new byte[sigData.size()];
    int i = 0;

    for (Byte sb : sigData)
      sigD[i++] = sb;

    return sigD;
  }

  public void addSignature(byte[] signature, int index) {
    mInputs.get(index).addSignature(signature);
  }

  public byte[] getRawTx() {
    ArrayList<Byte> rawTx = new ArrayList<>();
    inputsToByteData(rawTx);
    outputsToByteData(rawTx);

    byte[] tx = new byte[rawTx.size()];
    int i = 0;

    for (Byte b : rawTx)
      tx[i++] = b;

    return tx;
  }

  public void finalize() {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(getRawTx());
      mHash = md.digest();
    } catch (NoSuchAlgorithmException x) {
      x.printStackTrace(System.err);
    }
  }

  // Private

  private void inputsToByteData(ArrayList<Byte> raw) {
    for (Input in : mInputs) {
      byte[] prevTxHash = in.prevTxHash;
      ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
      b.putInt(in.outputIndex);
      byte[] outputIndex = b.array();
      byte[] signature = in.signature;

      if (prevTxHash != null)
        for (byte aPrevTxHash : prevTxHash)
          raw.add(aPrevTxHash);

      for (byte anOutputIndex : outputIndex)
        raw.add(anOutputIndex);

      if (signature != null)
        for (byte aSignature : signature)
          raw.add(aSignature);
    }
  }

  private void outputsToByteData(ArrayList<Byte> raw) {
    for (Output op : mOutputs) {
      ByteBuffer b = ByteBuffer.allocate(Double.SIZE / 8);
      b.putDouble(op.mValue);
      byte[] value = b.array();
      byte[] addressBytes = op.mAddress.getEncoded();

      for (byte aValue : value)
        raw.add(aValue);

      for (byte addressByte : addressBytes)
        raw.add(addressByte);
    }
  }

  // Accessors

  public void setHash(byte[] hash) {
    mHash = hash;
  }

  public byte[] getHash() {
    return mHash;
  }

  public ArrayList<Input> getInputs() {
    return mInputs;
  }

  public ArrayList<Output> getOutputs() {
    return mOutputs;
  }

  // Misc

  @Override
  public String toString() {
    return "Transaction:\n"
        + "Hash: " + (getHash() == null ? "NULL" : getHash().toString()) + "\n"
        + "Inputs: " + getInputs().toString() + "\n"
        + "Outputs: " + getOutputs().toString() + "\n";
  }

  // Input

  public class Input {

    /**
     * hash of the Transaction whose output is being used
     */
    public byte[] prevTxHash;
    /**
     * used output's index in the previous transaction
     */
    public int outputIndex;
    /**
     * the signature produced to check validity
     */
    public byte[] signature;

    public Input(byte[] prevHash, int index) {
      prevTxHash = (prevHash == null ? null : Arrays.copyOf(prevHash, prevHash.length));
      outputIndex = index;
    }

    public void addSignature(byte[] sig) {
      signature = (sig == null ? null : Arrays.copyOf(sig, sig.length));
    }

    @Override
    public String toString() {
      return "Transaction Input:\n"
          + "Prev Hash: " + prevTxHash.toString() + ", "
          + "Out Index: " + String.valueOf(outputIndex) + ", "
          + "Signature: " + signature.toString();
    }
  }

  // Output

  public class Output {

    /**
     * value in bitcoins of the output
     */
    public double mValue;
    /**
     * the address or public key of the recipient
     */
    public PublicKey mAddress;

    public Output(double value, PublicKey address) {
      mValue = value;
      mAddress = address;
    }

    @Override
    public String toString() {
      return "Transaction Output:\n"
          + "Value: " + String.valueOf(mValue) + ", "
          + "Address: " + mAddress.toString();
    }
  }
}
