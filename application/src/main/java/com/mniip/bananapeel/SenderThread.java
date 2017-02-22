package com.mniip.bananapeel;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SenderThread extends Thread
{
    private Server server;
    private Socket socket;
    private AtomicBoolean hadError;
    private LinkedBlockingQueue<IRCMessage> queue = new LinkedBlockingQueue<>();
    private String hostname;
    private int port;

    public SenderThread(Server srv, Socket sock, AtomicBoolean error, String host, int portNum)
    {
        server = srv;
        socket = sock;
        hadError = error;
        hostname = host;
        port = portNum;
    }

    @Override
    public void run()
    {
        try
        {
            socket.connect(new InetSocketAddress(hostname, port));
            server.onConnected();

            OutputStream stream = socket.getOutputStream();
            while(!Thread.interrupted())
            {
                IRCMessage msg = queue.take();
                stream.write(msg.toIRC().getBytes());
                stream.write("\r\n".getBytes());
                stream.flush();
            }
        }
        catch(InterruptedException e)
        {
        }
        catch(IOException e)
        {
            if(hadError.compareAndSet(false, true))
                server.onError(e);
        }
    }

    public void queueMessage(IRCMessage msg)
    {
        queue.offer(msg);
    }
}