package ch.uzh.csg.coinblesk.client.util.formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ch.uzh.csg.coinblesk.client.util.Constants;

/**
 * Created by rvoellmy on 5/27/15.
 */
public class DateFormatter {

    private static final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());

    public static String formatDate(Date date) {
        return simpleDateFormatter.format(date);
    }
}
