package ch.uzh.csg.coinblesk.client.util;

import android.content.Intent;

/**
 * Created by rvoellmy on 7/27/15.
 */
public class Mailer {

    private String message;
    private String recipient;
    private String subject = "";

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Intent getIntent() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{recipient});
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT   , message);
        return i;
    }
}
