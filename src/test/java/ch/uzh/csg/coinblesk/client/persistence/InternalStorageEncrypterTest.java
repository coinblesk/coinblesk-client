package ch.uzh.csg.coinblesk.client.persistence;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Created by rvoellmy on 5/16/15.
 */
public class InternalStorageEncrypterTest extends TestCase {

    public InternalStorageEncrypterTest() {
        super();
    }

    public void testEncryptDecrypt() throws Exception {
        String msg = "some secret message";
        String pwd = "supersecurepassword";

        InternalStorageEncrypter encrypter = new InternalStorageEncrypter();

        String cyphertext = encrypter.encrypt(msg, pwd);
        Assert.assertNotNull(cyphertext);
        String decrypted = encrypter.decrypt(cyphertext, pwd);
        Assert.assertNotNull(decrypted);
        Assert.assertEquals(msg, decrypted);

        // do it again
        String cyphertext2 = encrypter.encrypt(msg+msg, pwd);
        Assert.assertNotNull(cyphertext2);
        String decrypted2 = encrypter.decrypt(cyphertext2, pwd);
        Assert.assertNotNull(decrypted2);
        Assert.assertEquals(msg+msg, decrypted2);

    }

}