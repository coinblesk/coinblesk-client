package ch.uzh.csg.coinblesk.client.comm;

import org.junit.Assert;
import org.junit.Test;

import java.security.KeyPair;
import java.util.Random;

public class PaymentProtocolTest {
	
	@Test
	public void testPaymentMessage1() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentRequest(kp.getPublic(), "hallo", new byte[6], 5, new byte[20]);
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, null);
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}
	
	@Test(expected = RuntimeException.class)
	public void testPaymentMessageLong() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentRequest(kp.getPublic(),
				"halouoeauoeauoeauoauoeauaoeuoeauauoeauaoeuoeauauaoeuaoeuaoeuoeauoeauoeauaoeuoeauoeauoauaoeuoeauoeauaoeuoeauoeauoeauaoeuoeauaoeuaoeuoeauaoeuoeaoeauoeauaoeaoeauauaoeuoeaaoeuauaouauoeaoeaoeauaoeuaueoaeoeauaoeuoeauoeaaoaooauaouaouaoeueauoeauoealooeauaoeuaoeuuaoeuaoeuaoeuoaeuaouaoeuoaeu", new byte[6], 5, new byte[20]);
		pm1.toBytes(kp.getPrivate());
	}
	
	@Test
	public void testPaymentMessage2() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		Random rnd = new Random(1);
		byte[] bt = new byte[6];
		rnd.nextBytes(bt);
		byte[] ad = new byte[20];
		rnd.nextBytes(ad);
		PaymentProtocol pm1 = PaymentProtocol.paymentRequest(kp.getPublic(), "hallo", bt, 2143434245, ad);
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, null);
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}
	
	@Test
	public void testPaymentMessageFail() throws Exception {
		KeyPair kp1 = PaymentProtocol.generateKeys();
		KeyPair kp2 = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentRequest(kp1.getPublic(), "hallo", new byte[6], 5, new byte[20]);
		byte[] transfer = pm1.toBytes(kp2.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, null);
		Assert.assertEquals(pm1, pm2);
		Assert.assertFalse(pm2.isVerified());
	}
	
	@Test
	public void testPaymentMessageResponse() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		Random rnd = new Random(1);
		byte[] tx = new byte[2000];
		rnd.nextBytes(tx);
		PaymentProtocol pm1 = PaymentProtocol.paymentRequestResponse(kp.getPublic(), "test", new byte[6], tx, new int[] {777, 666, 444});
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, null);
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}
	
	@Test
	public void testPaymentMessageFromServer1() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentNok();
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, kp.getPublic());
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}
	
	@Test
	public void testPaymentMessageFromServer2() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		KeyPair kp2 = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentNok();
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, kp2.getPublic());
		Assert.assertEquals(pm1, pm2);
		Assert.assertFalse(pm2.isVerified());
	}
	
	@Test
	public void testPaymentMessageFromServerOk() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		byte[] tx = new byte[5000];
		Random rnd = new Random(1);
		rnd.nextBytes(tx);
		PaymentProtocol pm1 = PaymentProtocol.fullTransaction(tx, new int[] {777, 666, 444});
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, kp.getPublic());
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}

	@Test
	public void testSendRequest() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentSend(kp.getPublic(), "hallo", new byte[6]);
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, kp.getPublic());
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}

	@Test
	public void testSendRequestResponse() throws Exception {
		KeyPair kp = PaymentProtocol.generateKeys();
		PaymentProtocol pm1 = PaymentProtocol.paymentSendResponse(kp.getPublic(), "hallo", new byte[6], new byte[20]);
		byte[] transfer = pm1.toBytes(kp.getPrivate());
		PaymentProtocol pm2 = PaymentProtocol.fromBytes(transfer, kp.getPublic());
		Assert.assertEquals(pm1, pm2);
		Assert.assertTrue(pm2.isVerified());
	}

}
