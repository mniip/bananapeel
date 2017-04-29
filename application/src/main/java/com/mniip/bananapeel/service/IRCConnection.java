package com.mniip.bananapeel.service;

import android.os.Handler;
import android.os.Looper;

import com.mniip.bananapeel.util.IRCMessage;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.SocketFactory;

public class IRCConnection
{
	private IRCServer server;
	private SocketFactory factory;
	private Socket socket;
	private ReceiverThread receiver;
	private SenderThread sender;
	private AtomicBoolean hadError;

	public IRCConnection(IRCServer server, SocketFactory factory)
	{
		this.server = server;
		this.factory = factory;
		hadError = new AtomicBoolean(false);
	}

	public IRCServer getServer()
	{
		return server;
	}

	public void connect(String hostname, int port)
	{
		try
		{
			socket = factory.createSocket();
		} catch(IOException e)
		{
			onError(e);
			return;
		}

		sender = new SenderThread(this, socket, hostname, port);
		sender.start();
	}

	public void onConnected()
	{
		receiver = new ReceiverThread(this, socket);
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

	private Runnable closeBackground = new Runnable()
	{
		public void run()
		{
			try
			{
				socket.close();
			}
			catch(IOException ee)
			{
			}
		}
	};

	public void onError(final Exception e)
	{
		if(hadError.compareAndSet(false, true))
		{
			sender.interrupt();
			if(receiver != null)
				receiver.interrupt();

			new Thread(closeBackground).start();
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

		new Thread(closeBackground).start();

		sender = null;
		receiver = null;
	}

	public void send(IRCMessage msg)
	{
		if(sender != null)
			sender.queueMessage(msg);
	}
}
