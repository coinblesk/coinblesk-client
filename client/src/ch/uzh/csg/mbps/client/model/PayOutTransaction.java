package ch.uzh.csg.mbps.client.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * This class creates a model for the payout transaction.
 */
public class PayOutTransaction implements Serializable {
	private static final long serialVersionUID = 2189044547576984423L;
	private long id;
	private long userID;
	private Date timestamp;
	private BigDecimal amount;
	private String BtcAddress;
	private boolean verified;
	private String transactionID;
	
	public PayOutTransaction() {
	}
	
	public PayOutTransaction(long userID, BigDecimal amount, String address) {
		setTimestamp(new Date());
		setUserID(userID);
		setAmount(amount);
		setBtcAddress(address);
		verified = false;
		transactionID = null;
	}

	public String getBtcAddress() {
		return BtcAddress;
	}

	public void setBtcAddress(String btcAddress) {
		BtcAddress = btcAddress;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getUserID() {
		return userID;
	}

	public void setUserID(long userID) {
		this.userID = userID;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}
	
	public String getTransactionID() {
		return transactionID;
	}

	public void setTransactionID(String transactionID) {
		this.transactionID = transactionID;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ");
		sb.append(getId());
		return sb.toString();
	}
}
