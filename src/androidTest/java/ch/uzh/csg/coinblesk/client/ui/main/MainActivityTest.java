package ch.uzh.csg.coinblesk.client.ui.main;

import com.robotium.solo.Solo;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.UnitTestParams;
import org.junit.After;
import org.junit.Before;

import java.math.BigDecimal;

import basetests.BaseInstrumentationTest;
import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.util.formatter.CurrencyFormatter;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import testutils.BitcoinTestUtils;

/**
 * Created by rvoellmy on 7/13/15.
 */
public class MainActivityTest extends BaseInstrumentationTest {

    private final static NetworkParameters PARAMS = UnitTestParams.get();

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @After
    public void tearDown() {
        clearInternalData();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        mApplication = getCoinBleskApplication();

        prepareResponses();
        prepareRequestFactory();
        prepareInternalStorage();

        mApplication.getStorageHandler().clear();
        mApplication.getStorageHandler().setWatchingKey(BitcoinTestUtils.getServerWatchingKey(PARAMS));
        mApplication.getStorageHandler().setBitcoinNet(BitcoinNet.UNITTEST);

        solo = new Solo(getInstrumentation(), getActivity());

    }

    public void testTransactionHistoryAndBalanceUpdate() throws Exception {
        waitForWalletService();

        MainActivity activity = (MainActivity) solo.getCurrentActivity();
        WalletService walletService = activity.getWalletService();

        // send some bitcoins to the wallet
        BigDecimal receiveAmount = new BigDecimal("0.1234");
        int numTransactions = 7;
        for(int i = 0; i < numTransactions; i++) {
            BitcoinTestUtils.sendFakeCoins(PARAMS, receiveAmount, walletService.getBitcoinAddress(), walletService.getCoinBleskWalletAppKit());

        }
        solo.waitForText(solo.getString(R.string.history_payInUn), 0, TIMEOUT);
        // check if balance is displayed correctly
        BigDecimal balance = receiveAmount.multiply(BigDecimal.valueOf(numTransactions));
        solo.waitForText(CurrencyFormatter.formatBTC(balance), 0, TIMEOUT);

        // now send some coins away
        BigDecimal sendAmount = new BigDecimal("0.0321");
        walletService.createPayment(BitcoinTestUtils.getBitcoinAddress(PARAMS), sendAmount);

        solo.waitForText(solo.getString(R.string.history_payOut), 0, TIMEOUT);

    }



}