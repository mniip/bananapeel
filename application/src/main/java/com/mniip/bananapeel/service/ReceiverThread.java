package com.mniip.bananapeel.service;

import com.mniip.bananapeel.util.IRCMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiverThread extends Thread
{
	private IRCConnection server;
	private AtomicBoolean hadError;
	private Socket socket;

	public ReceiverThread(IRCConnection server, Socket socket, AtomicBoolean hadError)
	{
		this.server = server;
		this.socket = socket;
		this.hadError = hadError;
	}

	@Override
	public void run()
	{
		try
		{
			InputStream stream = socket.getInputStream();
			ByteArrayOutputStream line = new ByteArrayOutputStream();
			while(!Thread.interrupted())
			{
				byte[] buffer = new byte[4096];
				int len = stream.read(buffer, 0, buffer.length);
				if(len == -1)
					break;

				int off = 0;
				for(int i = 0; i < len; i++)
					if(buffer[i] == '\n' || buffer[i] == '\r')
					{
						line.write(buffer, off, i - off);
						server.onMessageReceived(IRCMessage.fromIRC(new String(line.toByteArray())));
						line.reset();
						if(i + 1 < len && buffer[i] == '\r' && buffer[i + 1] == '\n')
							i++;
						off = i + 1;
					}
				line.write(buffer, off, len - off);
			}
		}
		catch(IOException e)
		{
			if(hadError.compareAndSet(false, true))
				server.onError(e);
		}
	}
}
