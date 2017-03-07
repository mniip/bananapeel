package com.mniip.bananapeel;

public class IRCServer
{
	private IRCService service;
	private ServerTab serverTab;
	private IRCConnection connection;
	private boolean registered = false;
	public String ourNick = "";

	public IRCServer(IRCService srv, ServerTab sTab)
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

	public void connect(String host, int port)
	{
		connection = new IRCConnection(this);
		connection.connect(host, port);
	}

	public void send(IRCMessage msg)
	{
		if(connection != null)
			connection.send(msg);
	}

	public void onIRCMessageReceived(IRCMessage msg)
	{
		IRCCommandHandler.handle(this, msg);
	}

	public void onConnected()
	{
		ourNick = service.preferences.getDefaultNick();
		send(new IRCMessage("NICK", ourNick));
		send(new IRCMessage("USER", service.preferences.getDefaultUser(), "*", "*", service.preferences.getDefaultRealName()));
	}

	public void onError(Exception e)
	{
		getTab().putLine(e.toString());
		connection = null;
		registered = false;
	}
}
