package com.mniip.bananapeel;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class IRCConnection
{
	private IRCServer server;
	private Socket socket;
	private ReceiverThread receiver;
	private SenderThread sender;
	private AtomicBoolean hadError;

	public String ourNick;

	public IRCConnection(IRCServer srv)
	{
		server = srv;
	}

	public IRCServer getServer()
	{
		return server;
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
		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				server.onConnected();
			}
		});
	}

	public void onMessageReceived(final IRCMessage msg)
	{
		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				server.onIRCMessageReceived(msg);
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

		server.onError(e);
	}

	public void send(IRCMessage msg)
	{
		sender.queueMessage(msg);
	}
}
