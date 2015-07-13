package ch.uzh.csg.coinblesk.client.wallet;

import java.math.BigDecimal;
import java.util.Date;

/**
 * This class is an abstraction of a bitcoin transaction.
 */
public class TransactionObject {

    public enum TransactionType {
        PAY_IN, PAY_IN_UNVERIFIED, PAY_OUT
    }

    private TransactionType type;
    private BigDecimal amount;
    private Date timestamp;

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
