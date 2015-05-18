package ch.uzh.csg.coinblesk.client.wallet;

import java.util.List;

import javax.annotation.Nullable;

import ch.uzh.csg.coinblesk.model.HistoryPayInTransaction;
import ch.uzh.csg.coinblesk.model.HistoryPayInTransactionUnverified;
import ch.uzh.csg.coinblesk.model.HistoryPayOutTransaction;
import ch.uzh.csg.coinblesk.model.HistoryTransaction;

public class TransactionHistory  {

    @Nullable
    private List<HistoryTransaction> transactionHistory;

    private List<HistoryPayInTransaction> payInTransactionHistory;
    private List<HistoryPayInTransactionUnverified> payInTransactionUnverifiedHistory;
    private List<HistoryPayOutTransaction> payOutTransactionHistory;

    /**
     * <Strong>Deprecated:</Strong> HistoryTransactions are not used anymore
     */
    @Deprecated
    public TransactionHistory(List<HistoryTransaction> transactions, List<HistoryPayInTransaction> payInTransactions, List<HistoryPayInTransactionUnverified> payInTransactionUnverifiedHistory, List<HistoryPayOutTransaction> payOutTransactions) {
        this.transactionHistory = transactions;
        this.payInTransactionHistory = payInTransactions;
        this.payInTransactionUnverifiedHistory = payInTransactionUnverifiedHistory;
        this.payOutTransactionHistory = payOutTransactions;
    }

    public TransactionHistory(List<HistoryPayInTransaction> payInTransactions, List<HistoryPayInTransactionUnverified> payInTransactionUnverifiedHistory, List<HistoryPayOutTransaction> payOutTransactions) {
        this.payInTransactionHistory = payInTransactions;
        this.payInTransactionUnverifiedHistory = payInTransactionUnverifiedHistory;
        this.payOutTransactionHistory = payOutTransactions;
    }

    /**
     * The class HistoryTransactions is not used anymore
     * @return
     */
    @Deprecated
    public List<HistoryTransaction> getTransactionHistory() {
        return this.transactionHistory;
    }

    /**
     * The class HistoryTransactions is not used anymore
     * @return
     */
    @Deprecated
    public void setTransactionHistory(List<HistoryTransaction> transactionHistory) {
        this.transactionHistory = transactionHistory;
    }

    public List<HistoryPayInTransaction> getPayInTransactionHistory() {
        return this.payInTransactionHistory;
    }

    public void setPayInTransactionHistory(List<HistoryPayInTransaction> payInTransactionHistory) {
        this.payInTransactionHistory = payInTransactionHistory;
    }

    public List<HistoryPayInTransactionUnverified> getPayInTransactionUnverifiedHistory() {
        return this.payInTransactionUnverifiedHistory;
    }

    public void setPayInTransactionUnverifiedHistory(List<HistoryPayInTransactionUnverified> payInTransactionUnverifiedHistory) {
        this.payInTransactionUnverifiedHistory = payInTransactionUnverifiedHistory;
    }

    public List<HistoryPayOutTransaction> getPayOutTransactionHistory() {
        return this.payOutTransactionHistory;
    }

    public void setPayOutTransactionHistory(List<HistoryPayOutTransaction> payOutTransactionHistory) {
        this.payOutTransactionHistory = payOutTransactionHistory;
    }

}
