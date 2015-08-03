package ch.uzh.csg.coinblesk.client.util;

import android.content.Context;
import android.content.Intent;
import android.util.Patterns;
import android.widget.Toast;

import ch.uzh.csg.coinblesk.client.R;

/**
 * Created by rvoellmy on 7/27/15.
 */
public class Mailer {

    private String message;
    private String[] recipients;
    private String subject = "";

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRecipient(String... recipients) {
        this.recipients = recipients;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void sendEmail(Context context) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL  , recipients);
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT   , message);
        try {
            context.startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, context.getString(R.string.no_email_client_installed), Toast.LENGTH_SHORT).show();
        }
    }


    public static boolean validEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
