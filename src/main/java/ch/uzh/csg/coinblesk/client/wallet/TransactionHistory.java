package ch.uzh.csg.coinblesk.client.wallet;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.csg.coinblesk.model.Transaction;

/**
 * Wrapper for a list of transactions. Provides utility methods, eg. for retrieving only a certain kind of transactions.
 */
public class TransactionHistory  {

    private List<Transaction> allTransactions;

    public TransactionHistory(List<Transaction> transactions) {
        this.allTransactions = transactions;
    }

    public List<Transaction> getPayInTransactions() {
        return getTransactions(Transaction.TransactionType.PAY_IN);
    }

    public List<Transaction> getUnverifiedPayInTransactions() {
        return getTransactions(Transaction.TransactionType.PAY_IN_UNVERIFIED);
    }

    public List<Transaction> getPayOutTransactions() {
        return getTransactions(Transaction.TransactionType.PAY_OUT);
    }

    public List<Transaction> getAllTransactions() {
        return allTransactions;
    }


        private List<Transaction> getTransactions(Transaction.TransactionType type) {
        List<Transaction> filteredTxs = new ArrayList<>();
        for(Transaction tx : allTransactions) {
            if(tx.getType() == type) {
                filteredTxs.add(tx);
            }
        }
        return filteredTxs;
    }


}
