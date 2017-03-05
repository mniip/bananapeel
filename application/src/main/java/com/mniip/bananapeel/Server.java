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
	private Tab serverTab;
	private Socket socket;
	private ReceiverThread receiver;
	private SenderThread sender;
	private AtomicBoolean hadError;

	public Server(IRCService srv, Tab tab)
	{
		service = srv;
		serverTab = tab;
	}

	public IRCService getService()
	{
		return service;
	}

	public Tab getTab()
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
		Log.d("BananaPeel", "Connected");
	}

	public void onMessageReceived(IRCMessage msg)
	{
		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			private Server server;
			private IRCMessage message;

			@Override
			public void run()
			{
				for(int i = 0; i < service.tabs.size(); i++)
				{
					Tab tab = service.tabs.valueAt(i);
					if(tab.getServer() == server)
						tab.putLine(message.toIRC());
				}
			}

			public Runnable set(Server srv, IRCMessage msg)
			{
				server = srv;
				message = msg;
				return this;
			}
		}.set(this, msg));
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
