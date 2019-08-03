package me.scf37.jmxhttp.client.urlconnection;

import me.scf37.jmxhttp.common.command.ClassLoaderObjectInputStream;
import me.scf37.jmxhttp.common.command.Command;
import me.scf37.jmxhttp.common.http.Registration;
import me.scf37.jmxhttp.common.http.RemoteNotification;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.MarshalException;
import java.rmi.UnmarshalException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static me.scf37.jmxhttp.common.http.HttpConstant.*;

/**
 * Stateless HTTP client
 */
class JmxHttpClient {
    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private final URL url;
    private final Optional<String> credentials;
    private final ClassLoader classLoader;

    public JmxHttpClient(URL url, Optional<String> credentials, ClassLoader classLoader) {
        this.credentials = credentials;
        this.classLoader = classLoader;
        this.url = url;
    }

    public <R> R send(Command<R> command, long correlationId) throws Exception {
        return send(ACTION_COMMAND, correlationId, command);
    }

    public List<RemoteNotification> pollNotifications(long correlationId) throws Exception {
        return send(ACTION_LISTEN, correlationId, null);
    }

    public Registration getRegistration() throws Exception {
        return send(ACTION_REGISTER, 0L, null);
    }

    public long getConnectionId(long correlationId) throws IOException {
        try {
            return send(ACTION_CONNECTION_ID, correlationId, null);
        } catch (Exception e) {
            throw new IOException("Failed to get connectionId: " + e, e);
        }
    }

    private <R> R send(int action, long correlationId, Object request) throws Exception {
        HttpURLConnection urlConnection = (HttpURLConnection) this.url.openConnection();
        urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setChunkedStreamingMode(0);
        if (credentials.isPresent()) {
            urlConnection.setRequestProperty("Authorization", credentials.get());
        }

        try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(urlConnection.getOutputStream()))) {
            os.writeInt(ACTION_MAGIC);
            os.writeInt(action);
            os.writeLong(correlationId);

            if (request != null) {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(request);
                } catch (IOException e) {
                    throw new MarshalException("error marshalling arguments", e);
                }
            }
            os.flush();

            return readResponseAsObject(urlConnection, classLoader);
        }
    }

    @SuppressWarnings("unchecked")
    <R> R readResponseAsObject(HttpURLConnection urlConnection, ClassLoader classLoader) throws IOException, Exception {
        int status = urlConnection.getResponseCode();

        if (status != 200) {
            readBody(urlConnection);
            throw new IOException("http request failed with status: " + status + " body " + readBody(urlConnection));
        }

        String contentEncoding = urlConnection.getHeaderField("Content-Encoding");

        try (InputStream in = urlConnection.getInputStream();
             BufferedInputStream buffered = new BufferedInputStream(in)) {
            if ("gzip".equals(contentEncoding)) {
                try (GZIPInputStream stream = new GZIPInputStream(buffered)) {
                    return (R) readFromStream(stream, classLoader);
                }
            } else {
                return (R) readFromStream(buffered, classLoader);
            }
        }
    }

    private static String readBody(HttpURLConnection urlConnection) {
        String contentEncoding = Optional.ofNullable(urlConnection.getContentEncoding()).orElse("utf-8");

        try (InputStream in = urlConnection.getInputStream()) {
            // we buffer in readToString -> no need to buffer here
            try (Reader reader = new InputStreamReader(in, contentEncoding)) {
                return readToString(reader);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "could not read response body", e);
            // the body is just for debug purposes we don't have to fail here
            return "failed to read response: " + e;
        }
    }

    private static String readToString(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) > 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }

    private Object readFromStream(InputStream in, ClassLoader classLoader) throws Exception {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(in))) {
            long magic = stream.readInt();
            if (magic != ACTION_MAGIC) {
                throw new IOException("Remote " + url + " is not an jmx-http endpoint");
            }

            ObjectInputStream ois = new ClassLoaderObjectInputStream(stream, classLoader);
            Object result;
            try {
                result = ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new UnmarshalException("error unmarshalling return", e);
            }

            if (result instanceof Exception) {
                throw (Exception) result;
            }

            return result;
        }
    }

}
