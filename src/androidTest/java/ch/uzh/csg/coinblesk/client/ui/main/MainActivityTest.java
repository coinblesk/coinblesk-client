package ch.uzh.csg.coinblesk.client.ui.main;

import com.robotium.solo.Solo;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.UnitTestParams;
import org.junit.After;
import org.junit.Before;

import java.math.BigDecimal;

import basetests.BaseInstrumentationTest;
import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
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

    public void testIncomingTransactions() throws Exception {
        waitForWalletService();

        MainActivity activity = (MainActivity) solo.getCurrentActivity();
        WalletService walletService = activity.getWalletService();

        BigDecimal amount = new BigDecimal("0.1234");
        int numTransactions = 7;

        for(int i = 0; i < numTransactions; i++) {
            BitcoinTestUtils.sendFakeCoins(PARAMS, amount, walletService.getBitcoinAddress(), walletService.getCoinBleskWalletAppKit());
            Thread.sleep(200);
        }

        // check if balance is displayed correctly
        solo.waitForText(CurrencyFormatter.formatBTC(amount.multiply(BigDecimal.valueOf(numTransactions))), 0, TIMEOUT);

    }



}