package ch.uzh.csg.coinblesk.client.wallet;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.csg.coinblesk.client.storage.model.TransactionMetaData;

/**
 * Wrapper for a list of transactions. Provides utility methods, eg. for retrieving only a certain kind of transactions.
 */
public class TransactionHistory {

    private List<TransactionMetaData> allTransactions;

    public TransactionHistory(List<TransactionMetaData> transactions) {
        this.allTransactions = transactions;
    }

    public List<TransactionMetaData> getPayInTransactions() {
        return getTransactions(TransactionMetaData.TransactionType.PAY_IN);
    }

    public List<TransactionMetaData> getUnverifiedPayInTransactions() {
        return getTransactions(TransactionMetaData.TransactionType.PAY_IN_UNVERIFIED);
    }

    public List<TransactionMetaData> getPayOutTransactions() {
        return getTransactions(TransactionMetaData.TransactionType.PAY_OUT);
    }

    public List<TransactionMetaData> getAllTransactions() {
        return allTransactions;
    }


    private List<TransactionMetaData> getTransactions(TransactionMetaData.TransactionType type) {
        List<TransactionMetaData> filteredTxs = new ArrayList<>();
        for (TransactionMetaData tx : allTransactions) {
            if (tx.getType() == type) {
                filteredTxs.add(tx);
            }
        }
        return filteredTxs;
    }


}
