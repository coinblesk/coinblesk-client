package ch.uzh.csg.coinblesk.client;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ch.uzh.csg.coinblesk.client.request.HistoryEmailRequestTask;
import ch.uzh.csg.coinblesk.client.request.RequestTask;
import ch.uzh.csg.coinblesk.client.ui.WalletActivity;
import ch.uzh.csg.coinblesk.client.util.ClientController;
import ch.uzh.csg.coinblesk.client.util.HistoryTransactionFormatter;
import ch.uzh.csg.coinblesk.client.util.TimeHandler;
import ch.uzh.csg.coinblesk.client.wallet.TransactionHistory;
import ch.uzh.csg.coinblesk.model.AbstractHistory;
import ch.uzh.csg.coinblesk.model.HistoryPayInTransaction;
import ch.uzh.csg.coinblesk.model.HistoryPayInTransactionUnverified;
import ch.uzh.csg.coinblesk.model.HistoryPayOutTransaction;
import ch.uzh.csg.coinblesk.model.HistoryTransaction;
import ch.uzh.csg.coinblesk.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.coinblesk.responseobject.HistoryTransferRequestObject;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This class is a UI class, showing the history of transactions of the
 * authenticated user.
 */
public class HistoryActivity extends WalletActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryActivity.class);

    private Spinner filterSpinner;

    private int payInResultsPerPage;
    private int payInUnverifiedResultsPerPage;
    private int payOutResultsPerPage;
    private int txPage, txPayInPage, txPayInUnverifiedPage, txPayOutPage = 0;

    private MenuItem menuWarning;
    private MenuItem offlineMode;
    private MenuItem sessionCountdownMenuItem;
    private MenuItem sessionRefreshMenuItem;
    private TextView sessionCountdown;
    private CountDownTimer timer;

    private enum Filter {
        PAY_IN(1),
        PAY_IN_UN(2),
        PAY_OUT(3);

        private int i;

        private Filter(int i) {
            this.i = i;
        }

        protected int getCode() {
            return i;
        }
    }

    private Filter filter = Filter.PAY_IN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setScreenOrientation();

        getActionBar().setDisplayHomeAsUpEnabled(true);
        int filterValue = 0;
        Bundle b = getIntent().getExtras();
        if (b != null)
            filterValue = b.getInt("filter");

        setupSpinner();
        filterSpinner.setSelection(filterValue);
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        initializeMenuItems(menu);
        invalidateOptionsMenu();
        return true;
    }

    protected void initializeMenuItems(Menu menu) {
        menuWarning = menu.findItem(R.id.action_warning);
        offlineMode = menu.findItem(R.id.menu_offlineMode);
        TextView offlineModeTV = (TextView) offlineMode.getActionView();
        offlineModeTV.setText(getResources().getString(R.string.menu_offlineModeText));

        menuWarning.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                setupSpinner();
                return false;
            }
        });

        //setup timer
        sessionCountdownMenuItem = menu.findItem(R.id.menu_session_countdown);
        sessionCountdown = (TextView) sessionCountdownMenuItem.getActionView();
        sessionRefreshMenuItem = menu.findItem(R.id.menu_refresh_session);
        sessionRefreshMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                setupSpinner();
                return false;
            }
        });
    }

    @Override
    public void invalidateOptionsMenu() {
        if (menuWarning != null) {
            if (ClientController.isOnline()) {
                menuWarning.setVisible(false);
                offlineMode.setVisible(false);
                sessionCountdownMenuItem.setVisible(true);
                sessionRefreshMenuItem.setVisible(true);
                if (filterSpinner != null) {
                    filterSpinner.setEnabled(true);
                }
            } else {
                menuWarning.setVisible(true);
                offlineMode.setVisible(true);
                sessionCountdownMenuItem.setVisible(false);
                sessionRefreshMenuItem.setVisible(false);
                if (filterSpinner != null) {
                    filterSpinner.setEnabled(false);
                }
            }
        }
    }

    protected void startTimer(long duration, long interval) {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(duration, interval) {

            @Override
            public void onFinish() {
                //Session Timeout is already handled by TimeHandler
            }

            @Override
            public void onTick(long millisecondsLeft) {
                int secondsLeft = (int) Math.round((millisecondsLeft / (double) 1000));
                sessionCountdown.setText(getResources().getString(R.string.menu_sessionCountdown) + " " + TimeHandler.getInstance().formatCountdown(secondsLeft));
            }
        };

        timer.start();
    }


    /**
     * Creates the spinner where the user can choose between custom
     * transactions, pay in transactions or pay out transactions to be shown in
     * this view.
     */
    private void setupSpinner() {
        filterSpinner = (Spinner) findViewById(R.id.history_filter_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.history_filter, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == Filter.PAY_IN.getCode()) {
                    filter = Filter.PAY_IN;
                    launchHistoryRequest(-1, 0, -1, -1);
                } else if (pos == Filter.PAY_IN_UN.getCode()) {
                    filter = Filter.PAY_IN_UN;
                    launchHistoryRequest(-1, -1, 0, -1);
                } else if (pos == Filter.PAY_OUT.getCode()) {
                    filter = Filter.PAY_OUT;
                    launchHistoryRequest(-1, -1, -1, 0);
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }

        });
    }

    /**
     * Makes a server call to retrieve the history items. The items are loaded
     * only per page (based on the page size defined on the server). If a
     * parameter is negative, the given transaction type is not retrieved.
     */
    private void launchHistoryRequest(int txPage, int txPayInPage, int txPayInUnverifiedPage, int txPayOutPage) {
        if (ClientController.isOnline()) {
            showLoadingProgressDialog();

            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.history_linearLayout);
            if (linearLayout.getChildCount() > 0)
                linearLayout.removeAllViews();

            this.txPage = txPage;
            this.txPayInPage = txPayInPage;
            this.txPayInUnverifiedPage = txPayInUnverifiedPage;
            this.txPayOutPage = txPayOutPage;

            HistoryTransferRequestObject request = new HistoryTransferRequestObject();
            request.setTxPage(txPage);
            request.setTxPayInPage(txPayInPage);
            request.setTxPayInUnverifiedPage(txPayInUnverifiedPage);
            request.setTxPayOutPage(txPayOutPage);

            TransactionHistory txHistory = getWalletService().getTransactionHistory();
            writeHistory(txHistory);

//            AsyncTask getTransactionHistory = new AsyncTask<Void, Void, TransactionHistory>() {
//                @Override
//                protected TransactionHistory doInBackground(Void... params) {
//                    LOGGER.debug("Retrieving transaction history from wallet...");
//                    return getWalletService().getTransactionHistory();
//                }
//
//                @Override
//                protected void onPostExecute(TransactionHistory txHistory) {
//                    LOGGER.debug("Finished retrieving transaction history from wallet");
//                    writeHistory(txHistory);
//                }
//            };
//            getTransactionHistory.execute();

//			HistoryRequestTask getHistory = new HistoryRequestTask(new IAsyncTaskCompleteListener<GetHistoryTransferObject>() {
//
//				public void onTaskComplete(GetHistoryTransferObject response) {
//					dismissProgressDialog();
//
//					if (response.isSuccessful()) {
//						//renew Session Timeout Countdown
//						if(ClientController.isOnline()){
//							startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
//						}
//						ghto = response;
//						if (ghto != null) {
//							writeHistory();
//						}
//					} else {
//						ghto = null;
//						if (response.getMessage().equals(Constants.CONNECTION_ERROR)) {
//							reload(getIntent());
//							invalidateOptionsMenu();
//						}
//					}
//				}
//			}, request, new GetHistoryTransferObject(), getApplicationContext());
//			getHistory.execute();
        }
    }

    private void writeHistory(TransactionHistory txHistory) {
        ArrayList<AbstractHistory> history = new ArrayList<AbstractHistory>();

        if (txHistory == null)
            return;

        switch (filter) {
            case PAY_IN:
                history.addAll(txHistory.getPayInTransactionHistory());
                if (txHistory.getPayInTransactionHistory().size() > payInResultsPerPage)
                    payInResultsPerPage = txHistory.getPayInTransactionHistory().size();

                break;
            case PAY_IN_UN:
                history.addAll(txHistory.getPayInTransactionUnverifiedHistory());
                if (txHistory.getPayInTransactionUnverifiedHistory().size() > payInUnverifiedResultsPerPage)
                    payInUnverifiedResultsPerPage = txHistory.getPayInTransactionUnverifiedHistory().size();

                break;
            case PAY_OUT:
                history.addAll(txHistory.getPayOutTransactionHistory());
                if (txHistory.getPayOutTransactionHistory().size() > payOutResultsPerPage)
                    payOutResultsPerPage = txHistory.getPayOutTransactionHistory().size();

                break;
        }

        Collections.sort(history, new CustomComparator());
        createHistoryViews(history);
    }

    private void createHistoryViews(ArrayList<AbstractHistory> history) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.history_linearLayout);

        if (history.size() == 0) {
            TextView tv = new TextView(getApplicationContext());
            tv.setGravity(Gravity.LEFT);
            tv.setText(R.string.history_activity_no_transactions_text);
            tv.setTextColor(Color.BLACK);
            linearLayout.addView(tv);
            return;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            TextView tv = new TextView(getApplicationContext());
            tv.setGravity(Gravity.LEFT);
            int drawable = getImage(history.get(i));
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
            tv.setText(HistoryTransactionFormatter.formatHistoryTransaction(history.get(i), getApplicationContext()));
            tv.setPadding(0, 5, 0, 5);
            tv.setTextColor(Color.BLACK);
            if (i % 2 == 0) {
                tv.setBackgroundColor(Color.LTGRAY);
            }
            linearLayout.addView(tv);
            //TB: add a horizontal separator
            View ruler = new View(getApplicationContext());
            ruler.setBackgroundColor(Color.DKGRAY);
            linearLayout.addView(ruler);
        }

        createNavigationButtons(linearLayout);
        createEmailCSVButton(linearLayout);
    }

    private void createNavigationButtons(LinearLayout linearLayout) {
        LinearLayout innerLayout = new LinearLayout(getApplicationContext());
        innerLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);

        LayoutParams params = new LayoutParams(120, LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        params.gravity = Gravity.LEFT;
        params.setMargins(30, 0, 0, 15);
        createPreviousButton(innerLayout, params);

        LayoutParams params2 = new LayoutParams(120, LayoutParams.WRAP_CONTENT);
        params2.weight = 1.0f;
        params2.gravity = Gravity.RIGHT;
        params.setMargins(0, 0, 30, 15);

        linearLayout.addView(innerLayout);
    }

    private void createPreviousButton(LinearLayout innerLayout, LayoutParams params) {
        Button previous = new Button(getApplicationContext());
        previous.setText("<< Previous");
        previous.setTextColor(Color.BLACK);
        previous.setBackgroundResource(android.R.drawable.btn_default);
        previous.setLayoutParams(params);
        previous.setTextColor(Color.BLACK);
        previous.setBackgroundColor(Color.LTGRAY);

        switch (filter) {
            case PAY_IN:
                if (txPayInPage == 0) {
                    previous.setEnabled(false);
                    previous.setVisibility(View.INVISIBLE);
                } else {
                    previous.setVisibility(View.VISIBLE);
                    previous.setEnabled(true);
                    previous.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            launchHistoryRequest(txPage, txPayInPage - 1, txPayInUnverifiedPage, txPayOutPage);
                        }
                    });
                }
                break;

            case PAY_IN_UN:
                if (txPayInUnverifiedPage == 0) {
                    previous.setEnabled(false);
                    previous.setVisibility(View.INVISIBLE);
                } else {
                    previous.setVisibility(View.VISIBLE);
                    previous.setEnabled(true);
                    previous.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            launchHistoryRequest(txPage, txPayInPage, txPayInUnverifiedPage - 1, txPayOutPage);
                        }
                    });
                }
                break;

            case PAY_OUT:
                if (txPayOutPage == 0) {
                    previous.setEnabled(false);
                    previous.setVisibility(View.INVISIBLE);
                } else {
                    previous.setVisibility(View.VISIBLE);
                    previous.setEnabled(true);
                    previous.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            launchHistoryRequest(txPage, txPayInPage, txPayInUnverifiedPage, txPayOutPage - 1);
                        }
                    });
                }
                break;
        }

        innerLayout.addView(previous);
    }

    private void createEmailCSVButton(LinearLayout linearLayout) {
        Button email = new Button(getApplicationContext());
        email.setText(R.string.history_activity_get_by_email);
        email.setTextColor(Color.BLACK);
        email.setBackgroundResource(android.R.drawable.btn_default);
        email.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        email.setTextColor(Color.BLACK);
        email.setBackgroundColor(Color.LTGRAY);

        email.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                switch (filter) {
                    case PAY_IN:
                        launchHistoryEmailRequest(HistoryEmailRequestTask.PAY_IN_HISTORY);
                        break;
                    case PAY_IN_UN:
                        launchHistoryEmailRequest(HistoryEmailRequestTask.PAY_IN_HISTORY_UNVERIFIED);
                        break;
                    case PAY_OUT:
                        launchHistoryEmailRequest(HistoryEmailRequestTask.PAY_OUT_HISTORY);
                        break;
                }
            }
        });

        linearLayout.addView(email);
    }

    private void launchHistoryEmailRequest(int type) {
        if (ClientController.isOnline()) {
            showLoadingProgressDialog();
            TransferObject input = new TransferObject();
            input.setMessage(Integer.toString(type));
            RequestTask<TransferObject, TransferObject> getHistory = new HistoryEmailRequestTask(new IAsyncTaskCompleteListener<TransferObject>() {
                @Override
                public void onTaskComplete(TransferObject response) {
                    dismissProgressDialog();

                    //renew Session Timeout Countdown
                    if (ClientController.isOnline()) {
                        startTimer(TimeHandler.getInstance().getRemainingTime(), 1000);
                    }
                    displayResponse(response.getMessage());
                    return;
                }
            }, input, new TransferObject(), getApplicationContext());
            getHistory.execute();
        }
    }

    private int getImage(AbstractHistory history) {
        if (history instanceof HistoryTransaction) {
            if (((HistoryTransaction) history).getSeller().equals(ClientController.getStorageHandler().getUserAccount().getUsername())) {
                return R.drawable.ic_receive_payment;
            } else {
                return R.drawable.ic_pay_payment;
            }
        } else if (history instanceof HistoryPayInTransaction) {
            return R.drawable.ic_pay_in;
        } else if (history instanceof HistoryPayInTransactionUnverified) {
            return R.drawable.ic_pay_in_un;
        } else if (history instanceof HistoryPayOutTransaction) {
            return R.drawable.ic_pay_out;
        }
        return 0;
    }

    private class CustomComparator implements Comparator<AbstractHistory> {
        public int compare(AbstractHistory o1, AbstractHistory o2) {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    }

}
