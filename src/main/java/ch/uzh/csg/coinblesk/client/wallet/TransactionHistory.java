package ch.uzh.csg.coinblesk.client.wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a list of transactions. Provides utility methods, eg. for retrieving only a certain kind of transactions.
 */
public class TransactionHistory  {

    private List<TransactionObject> allTransactions;

    public TransactionHistory(List<TransactionObject> transactions) {
        this.allTransactions = transactions;
    }

    public List<TransactionObject> getPayInTransactions() {
        return getTransactions(TransactionObject.TransactionType.PAY_IN);
    }

    public List<TransactionObject> getUnverifiedPayInTransactions() {
        return getTransactions(TransactionObject.TransactionType.PAY_IN_UNVERIFIED);
    }

    public List<TransactionObject> getPayOutTransactions() {
        return getTransactions(TransactionObject.TransactionType.PAY_OUT);
    }

    public List<TransactionObject> getAllTransactions() {
        return allTransactions;
    }


        private List<TransactionObject> getTransactions(TransactionObject.TransactionType type) {
        List<TransactionObject> filteredTxs = new ArrayList<>();
        for(TransactionObject tx : allTransactions) {
            if(tx.getType() == type) {
                filteredTxs.add(tx);
            }
        }
        return filteredTxs;
    }


}
