package com.mniip.bananapeel;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class SenderThread implements Runnable
{
    private Socket socket;
    private LinkedBlockingQueue<IRCMessage> queue = new LinkedBlockingQueue<>();

    public void putMessage(IRCMessage msg)
    {
        queue.offer(msg);
    }

    public SenderThread(Socket sock)
    {
        socket = sock;
    }

    public void run()
    {
        try
        {
            OutputStream stream = socket.getOutputStream();
            while(true)
            {
                IRCMessage msg = queue.take();
                stream.write(msg.toIRC().getBytes());
                stream.flush();
            }
        }
        catch(InterruptedException e)
        {
        }
        catch(IOException e)
        {
        }
    }
}