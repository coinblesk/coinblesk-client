package ch.uzh.csg.coinblesk.client.storage.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.math.BigDecimal;
import java.util.Date;

/**
 * This saves meta data of a bitcoin transaction.
 */
@Table(name = "TransactionMetaData")
public class TransactionMetaData extends Model {

    // do not delete! this default constructor is required
    public TransactionMetaData() {
        super();
    }

    /**
     * Creates a new TransactionMetaData object with a given transaction id (hash of the transaction)
     * @param txId the ytransaction id of the transaction to store meta data for
     */
    public TransactionMetaData(String txId) {
        super();
        this.txId = txId;
    }

    public enum TransactionType {
        PAY_IN, PAY_IN_UNVERIFIED, PAY_OUT, COINBLESK_PAY_IN, COINBLESK_PAY_OUT,
    }

    @Column(name = "TxId", index = true)
    private String txId;

    @Column(name = "Sender")
    private String sender;

    @Column(name = "Receiver")
    private String receiver;

    @Column(name = "Type")
    private TransactionType type;

    // these fields are filled with actual data from the bitcoin transactions stored in the wallet file
    private transient BigDecimal amount;
    private transient Date timestamp;
    private transient long confirmations;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public long getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(long confirmations) {
        this.confirmations = confirmations;
    }

    /**
     * Sets the type of the transaction, if not already set
     * @param type
     */
    public void maybeSetType(TransactionType type) {
        this.type = this.type != null ? this.type : type;
    }


}
