package ch.uzh.csg.coinblesk.client.ui.payment;

import android.widget.Button;
import android.widget.EditText;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.UnitTestParams;
import org.junit.After;
import org.junit.Before;

import java.math.BigDecimal;

import basetests.BaseInstrumentationTest;
import ch.uzh.csg.coinblesk.bitcoin.BitcoinNet;
import ch.uzh.csg.coinblesk.client.R;
import ch.uzh.csg.coinblesk.client.wallet.WalletService;
import testutils.BitcoinTestUtils;

/**
 * Created by rvoellmy on 7/12/15.
 */
public class PayOutActivityTest extends BaseInstrumentationTest {

    public PayOutActivityTest() {
        super(PayOutActivity.class);
    }

    private final static long TIMEOUT = 15000;
    private final static NetworkParameters PARAMS = UnitTestParams.get();

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

    public void testSendBitcoins_invalidValues() throws Exception {

        final Button sendButton = (Button) solo.getView(R.id.payOut_payOutButton);

        // wait for the send button to become active
        boolean active = solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return sendButton.isEnabled();
            }
        }, (int) TIMEOUT);
        assertTrue(active);

        //try sending without amount entered
        solo.clickOnView(sendButton);
        assertTrue(solo.waitForText(solo.getString(R.string.payOut_error_enterAmount), 0, TIMEOUT));

        // try sending without bitcoin address entered
        solo.enterText((EditText) solo.getView(R.id.payOut_Amount), "1.00");
        solo.clickOnView(sendButton);
        assertTrue(solo.waitForText(solo.getString(R.string.payOut_error_enterBitcoinAddress), 0, TIMEOUT));

        // try sending with an invalid bitcoin address
        solo.enterText((EditText) solo.getView(R.id.payOut_Address), "fakeBtcAddress");
        solo.clickOnView(sendButton);
        assertTrue(solo.waitForText(solo.getString(R.string.payOut_error_address), 0, TIMEOUT));

        // try sending with insufficient funds
        solo.clearEditText((EditText) solo.getView(R.id.payOut_Address));
        solo.enterText((EditText) solo.getView(R.id.payOut_Address), BitcoinTestUtils.getBitcoinAddress(PARAMS));
        solo.clickOnView(sendButton);
        assertTrue(solo.waitForText(solo.getString(R.string.payOut_error_balance), 0, TIMEOUT));

    }

    public void testSendBitcoins() throws Exception {

        // wait for the service connect (-> when button is active)
        final Button sendButton = (Button) solo.getView(R.id.payOut_payOutButton);
        boolean active = solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return sendButton.isEnabled();
            }
        }, (int) TIMEOUT);
        assertTrue(active);

        // add fake bitcoins to the wallet
        PayOutActivity activity = (PayOutActivity) solo.getCurrentActivity();
        WalletService walletService = activity.getWalletService();
        assertNotNull(walletService);

        BigDecimal receiveAmount = BigDecimal.ONE;
        BitcoinTestUtils.sendFakeCoins(PARAMS, receiveAmount, walletService.getBitcoinAddress(), walletService.getCoinBleskWalletAppKit());

        Thread.sleep(3000);
        assertTrue(walletService.getBalance().compareTo(receiveAmount) == 0);

        // send the bitcoins...
        BigDecimal sendAmount = new BigDecimal("0.50");
        solo.clearEditText((EditText) solo.getView(R.id.payOut_Amount));
        solo.enterText((EditText) solo.getView(R.id.payOut_Amount), sendAmount.toString());
        solo.clearEditText((EditText) solo.getView(R.id.payOut_Address));
        solo.enterText((EditText) solo.getView(R.id.payOut_Address), BitcoinTestUtils.getBitcoinAddress(PARAMS));
        solo.clickOnView(sendButton);

        assertTrue(solo.waitForText(solo.getString(R.string.payment_success), 0, TIMEOUT));

    }



}