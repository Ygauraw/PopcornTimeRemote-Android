package eu.se_bastiaan.popcorntimeremote.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import eu.se_bastiaan.popcorntimeremote.models.ZeroConfMessage;
import eu.se_bastiaan.popcorntimeremote.utils.LogUtils;

public class ZeroConfClient {

    protected static final Integer BUFFER_SIZE = 4096;
    protected int BROADCAST_PORT;

    private boolean mReceiveMessages = false;

    protected Context mContext;
    private DatagramSocket mDatagramSocket;
    protected List<ZeroConfMessage> mMessageQueue = new ArrayList<ZeroConfMessage>();

    private final Handler mIncomingMessageHandler;
    protected ZeroConfMessage mIncomingMessage;
    private Thread mThread;
    private Runnable mMessageAnalyseRunnable;

    /**
     * Class constructor
     * @param context the application's context
     * @param broadcastPort the port to broadcast to. Must be between 1025 and 49151 (inclusive)
     */
    public ZeroConfClient(Context context, int broadcastPort) throws IllegalArgumentException {
        if(context == null || broadcastPort <= 1024)
            throw new IllegalArgumentException();

        mContext = context.getApplicationContext();
        BROADCAST_PORT = broadcastPort;

        openSocket();
        mIncomingMessageHandler = new Handler(Looper.getMainLooper());
    }

    public boolean openSocket() {
        if(mDatagramSocket != null) return true;

        try {
            mDatagramSocket = new DatagramSocket();
            mDatagramSocket.setBroadcast(true);
            return true;
        } catch (SocketException e) {
            LogUtils.d("ZeroConf", "There was a problem creating the sending socket. Aborting.");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sends a broadcast message. Opens a new socket in case it's closed.
     * @param message the message to send.
     * @return
     * @throws IllegalArgumentException
     */
    public boolean sendMessage(String message) throws IllegalArgumentException {
        if(message == null || message.length() == 0)
            throw new IllegalArgumentException();

        // Check for WiFi connectivity
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if(mWifi == null || !mWifi.isConnected())
        {
            LogUtils.d("ZeroConf", "Sorry! You need to be in a WiFi network in order to send UDP broadcast packets. Aborting.");
            return false;
        }

        // Create the send socket
        if(mDatagramSocket == null) {
            LogUtils.d("ZeroConf", "Client not running. Adding to message queue but not sending.");
        }

        // Build the packet
        final ZeroConfMessage msg;

        try {
            msg = new ZeroConfMessage(message, InetAddress.getByName("255.255.255.255"));
        } catch (UnknownHostException e) {
            LogUtils.d("ZeroConf", "It seems that 255.255.255.255 is not a valid ip! Aborting.");
            e.printStackTrace();
            return false;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    LogUtils.d("ZeroConf", "Sending the UDP packet");
                    mDatagramSocket.send(msg.getPacket(BROADCAST_PORT));
                } catch (IOException e) {
                    LogUtils.d("ZeroConf", "There was an error sending the UDP packet. Aborted.");
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

        return true;
    }

    public ZeroConfMessage getIncoming() {
        return mIncomingMessage;
    }

    public void setIncomingMessageAnalyseRunnable(Runnable r) {
        mMessageAnalyseRunnable = r;
    }

    public void startClient(Runnable messageAnalyseRunnable) {
        mMessageAnalyseRunnable = messageAnalyseRunnable;
        startClient();
    }

    public Boolean isStarted() {
        return mReceiveMessages;
    }

    public void startClient() {
        LogUtils.d("ZeroConf", "Starting ZeroConf client");

        mReceiveMessages = true;

        if(mThread == null)
            mThread = new Thread(mReceiveRunnable);

        if(!mThread.isAlive())
            mThread.start();
    }

    public void stopClient() {
        mReceiveMessages = false;
    }

    Runnable mReceiveRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);

            while(mReceiveMessages) {
                try {
                    LogUtils.d("ZeroConf", "Receiving the UDP packet");
                    mDatagramSocket.receive(rPacket);
                    LogUtils.d("ZeroConf", "Done receiving the UDP packet");
                } catch (IOException e) {
                    LogUtils.d("ZeroConf", "There was a problem receiving the incoming message.");
                    e.printStackTrace();
                    continue;
                }

                if (!mReceiveMessages) {
                    LogUtils.d("ZeroConf", "Stopping thread...");
                    break;
                }

                byte data[] = rPacket.getData();

                String messageText;

                try {
                    messageText = new String(data, 0, rPacket.getLength(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    LogUtils.d("ZeroConf", "UTF-8 encoding is not supported. Can't receive the incoming message.");
                    e.printStackTrace();
                    continue;
                }

                try {
                    mIncomingMessage = new ZeroConfMessage(messageText, rPacket.getAddress());
                } catch (IllegalArgumentException ex) {
                    LogUtils.d("ZeroConf", "There was a problem processing the message: " + messageText);
                    ex.printStackTrace();
                    continue;
                }

                LogUtils.d("ZeroConf", "Posting message to handler, if available");
                if(mMessageAnalyseRunnable != null) {
                    mIncomingMessageHandler.post(mMessageAnalyseRunnable);
                    LogUtils.d("ZeroConf", "Posted message to handler");
                }
            }
            LogUtils.d("ZeroConf", "Stopping thread");
        }
    };

}