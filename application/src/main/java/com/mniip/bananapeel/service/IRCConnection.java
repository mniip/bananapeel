package com.mniip.bananapeel.service;

import android.os.Handler;
import android.os.Looper;

import com.mniip.bananapeel.util.IRCMessage;

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

	public IRCConnection(IRCServer server)
	{
		this.server = server;
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

	public void onError(final Exception e)
	{
		sender.interrupt();
		if(receiver != null)
			receiver.interrupt();

		try
		{
			socket.close();
		}
		catch(IOException ee)
		{
		}

		sender = null;
		receiver = null;

		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				server.onError(e);
			}
		});
	}

	public void disconnect()
	{
		hadError.set(true);

		sender.interrupt();
		if(receiver != null)
			receiver.interrupt();

		try
		{
			socket.close();
		}
		catch(IOException ee)
		{
		}

		sender = null;
		receiver = null;
	}

	public void send(IRCMessage msg)
	{
		sender.queueMessage(msg);
	}
}
