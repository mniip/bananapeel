package com.mniip.bananapeel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server
{
	private IRCService service;
	private ServerTab serverTab;
	private Socket socket;
	private ReceiverThread receiver;
	private SenderThread sender;
	private AtomicBoolean hadError;

	public String ourNick;

	public Server(IRCService srv, ServerTab sTab)
	{
		service = srv;
		serverTab = sTab;
	}

	public IRCService getService()
	{
		return service;
	}

	public ServerTab getTab()
	{
		return serverTab;
	}

	public void connect(String hostname, int port)
	{
		hadError = new AtomicBoolean(false);
		socket = new Socket();

		sender = new SenderThread(this, socket, hadError, hostname, port);
		sender.start();
	}

	public void onConnected()
	{
		receiver = new ReceiverThread(this, socket, hadError);
		receiver.start();
		service.onServerConnected(this);
	}

	public void onMessageReceived(final IRCMessage msg)
	{
		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				service.onIRCMessageReceived(Server.this, msg);
			}
		});
	}

	public void onError(Exception e)
	{
		try
		{
			socket.close();
		}
		catch(IOException ee)
		{
		}
		receiver.interrupt();
		sender.interrupt();

		receiver = null;
		sender = null;
	}

	public void send(IRCMessage msg)
	{
		sender.queueMessage(msg);
	}
}
