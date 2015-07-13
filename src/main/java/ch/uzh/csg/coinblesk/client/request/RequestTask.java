package ch.uzh.csg.coinblesk.client.request;

import android.content.Context;
import android.os.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import ch.uzh.csg.coinblesk.JsonConverter;
import ch.uzh.csg.coinblesk.client.util.RequestCompleteListener;
import ch.uzh.csg.coinblesk.responseobject.TransferObject;

/**
 * This abstract class is used to send asynchronous requests to the server and
 * to notify the caller when the response arrives.
 */
public abstract class RequestTask<I extends TransferObject, O extends TransferObject> extends AsyncTask<Void, Void, O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTask.class);

    private static final int HTTP_CONNECTION_TIMEOUT = 5 * 1000;

    final private I requestObject;
    final private O responseObject;
    final private String url;

    final private RequestCompleteListener<O> callback;
    final private Context context;

    /**
     * Creates a new POST request
     *
     * @param requestObject
     * @param responseObject
     * @param url
     * @param callback
     * @param context
     */
    public RequestTask(I requestObject, O responseObject, String url, RequestCompleteListener<O> callback, Context context) {
        this.requestObject = requestObject;
        this.responseObject = responseObject;
        this.url = url;
        this.callback = callback;
        this.context = context;
    }

    /**
     * Creates a new GET request
     *
     * @param responseObject
     * @param url
     * @param callback
     * @param context
     */
    public RequestTask(O responseObject, String url, RequestCompleteListener<O> callback, Context context) {
        this(null, responseObject, url, callback, context);
    }

    @Override
    protected O doInBackground(Void... params) {
        try {
            String res;
            if (requestObject == null) {
                res = executeGet(url);
            } else {
                res = executePost(url, requestObject);
            }
            if (res != null) {
                return JsonConverter.fromJson(res, (Class<O>) responseObject.getClass());
            }
        } catch (Exception e) {
            LOGGER.error("Request failed: {}", e);
            responseObject.setMessage(e.getMessage());
        }

        responseObject.setSuccessful(false);
        return responseObject;
    }

    /**
     * Gets the response and sends it to the object that launched the request.
     *
     * @param result Response of the request. The result is an
     *               {@link TransferObject} and includes always a boolean and
     *               message.
     */
    @Override
    final protected void onPostExecute(O result) {
        callback.onTaskComplete(result);
    }

    private String executePost(String endpoint, TransferObject params) {
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);

            //Send request
            String json = params.toJson();

            DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
            printout.writeUTF(URLEncoder.encode(json, "UTF-8"));
            printout.flush();
            printout.close();


            //Get Response
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                return readString(is);
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readString(InputStream is) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();
        return response.toString();
    }


    private String executeGet(String endpoint) throws IOException {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoInput(true);

            //Get Response
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                return readString(is);
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


}
