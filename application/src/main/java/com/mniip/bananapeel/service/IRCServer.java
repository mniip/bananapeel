package com.mniip.bananapeel.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.util.Collators;
import com.mniip.bananapeel.util.IRCMessage;
import com.mniip.bananapeel.util.IRCServerConfig;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.OrderedList;
import com.mniip.bananapeel.util.TextEvent;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.mniip.bananapeel.util.TextEvent.Type.*;

public class IRCServer
{
	private IRCService service;
	private ServerTab serverTab;
	private IRCConnection connection;
	private IRCServerPreferences preferences;

	private boolean registered = false;
	private boolean authenticating = false;
	public String ourNick = "";
	private List<String> waitingNames = new ArrayList<>();

	public IRCServerConfig config;

	private static class ComposedComparator<T> implements Comparator<T>
	{
		Comparator<T> x, y;

		public ComposedComparator(Comparator<T> x, Comparator<T> y)
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public int compare(T a, T b)
		{
			int result = x.compare(a, b);
			if(result == 0)
				return y.compare(a, b);
			return result;
		}
	}

	public IRCServer(IRCService service, ServerTab serverTab, IRCServerPreferences preferences)
	{
		this.service = service;
		this.serverTab = serverTab;
		this.preferences = preferences;
	}

	public IRCService getService()
	{
		return service;
	}

	public ServerTab getTab()
	{
		return serverTab;
	}

	public void setPreferences(IRCServerPreferences preferences)
	{
		this.preferences = preferences;
		serverTab.setTitle(preferences.getName());
	}

	private static class Reconnect implements Runnable
	{
		private boolean cancelled = false;
		private IRCServer server;

		public Reconnect(IRCServer server)
		{
			this.server = server;
		}

		public void cancel()
		{
			cancelled = true;
		}

		@Override
		public void run()
		{
			if(!cancelled)
				server.connect();
		}
	}

	private Reconnect reconnect = null;

	private static TrustManager[] trustAllCerts = new TrustManager[]
			{
					new X509TrustManager()
					{
						public X509Certificate[] getAcceptedIssuers()
						{
							return new X509Certificate[0];
						}
						public void checkClientTrusted(X509Certificate[] certs, String authType) { }
						public void checkServerTrusted(X509Certificate[] certs, String authType) { }
					}
			}; // TODO: proper trust management

	public void connect()
	{
		if(reconnect != null)
			reconnect.cancel(); // No race condition because it all runs on the main thread
		if(connection != null)
			connection.disconnect();

		SocketFactory factory;
		if(preferences.isSSL())
			try
			{
				SSLContext context = SSLContext.getInstance("SSL");
				context.init(null, trustAllCerts, new SecureRandom());
				factory = context.getSocketFactory();
			}
			catch(GeneralSecurityException e)
			{
				serverTab.putLine(new TextEvent(ERROR, e.toString()));
				Log.d("BananaPeel", "", e);
				return;
			}
		else
			factory = SocketFactory.getDefault();

		connection = new IRCConnection(this, factory);
		connection.connect(preferences.getHost(), preferences.getPort());
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
		registered = false;
		config = IRCServerConfig.rfc1459();
		ourNick = preferences.getNick();

		send(new IRCMessage("CAP", "LS"));

		if(preferences.getAuthMode().equals("pass"))
			send(new IRCMessage("PASS", preferences.getPassword()));

		send(new IRCMessage("NICK", ourNick));
		send(new IRCMessage("USER", preferences.getUser(), "*", "*", preferences.getRealName()));
	}

	public void onError(Exception e)
	{
		for(Tab tab : service.tabs)
			if(tab.getServerTab() == getTab())
			{
				tab.putLine(new TextEvent(ERROR, e.toString()));
				Log.d("BananaPeel", "", e);
			}
		connection = null;
		reconnect = new Reconnect(this);
		new Handler(Looper.getMainLooper()).postDelayed(reconnect, 5000);
	}

	public void onRegistered()
	{
		if(!registered)
		{
			registered = true;
		}
	}

	private static boolean isMangled(String nick, String orig)
	{
		if(nick.length() < orig.length())
			return false;
		for(int i = 0; i < nick.length(); i++)
		{
			char ch = i < orig.length() ? orig.charAt(i) : '_';
			if(nick.charAt(i) != ch)
			{
				if(!Character.isDigit(nick.charAt(i)))
					return false;
				while(i < 9 && i < nick.length() && Character.isDigit(nick.charAt(i)))
					i++;
				if(i != 9)
					return false;
				return nick.substring(i).equals(i < orig.length() ? orig.substring(i) : "");
			}
		}
		return true;
	}

	private static String mangle(String nick)
	{
		while(nick.length() < 9)
			nick += '_';
		int i = 8;
		while(nick.charAt(i) == '9')
		{
			nick = nick.substring(0, i) + '0' + nick.substring(i + 1);
			i--;
			if(i < 0)
				return nick;
		}
		nick = nick.substring(0, i) + (char)((Character.isDigit(nick.charAt(i)) ? nick.charAt(i) : '0') + 1) + nick.substring(i + 1);
		return nick;
	}

	private String nextNick(String nick)
	{
		String defNick = preferences.getNick();
		String defNickAlt = preferences.getNickAlt();

		if(isMangled(nick, defNickAlt))
			return mangle(nick);
		if(nick.equals(defNick))
			return defNickAlt;
		return defNick;
	}

	private Comparator<NickListEntry> nickListEntryComparator = NickListEntry.nickComparator(Collators.rfc1459());

	private void updateComparator()
	{
		nickListEntryComparator = new ComposedComparator<>(NickListEntry.statusComparator(config.statusChars), NickListEntry.nickComparator(config.nickCollator));
		for(Tab tab : service.tabs)
			if(tab.getServerTab().server == this && tab.nickList != null)
			{
				tab.nickList.setComparator(nickListEntryComparator);
				service.changeNickList(tab);
			}
	}

	public static class IRCCommandHandler
	{
		@Retention(RetentionPolicy.RUNTIME)
		private @interface Hook
		{
			String command();
			int minParams() default 0;
			boolean requireSource() default false;
		}

		@Retention(RetentionPolicy.RUNTIME)
		private @interface Hooks
		{
			Hook[] value();
		}

		private static boolean matchesHook(Hook hook, IRCMessage msg)
		{
			// Collation in command names?
			if(!hook.command().equalsIgnoreCase(msg.command))
				return false;
			if(hook.requireSource() && msg.source == null)
				return false;
			return hook.minParams() <= msg.args.length;
		}

		public static void handle(IRCServer srv, IRCMessage msg)
		{
			try
			{
				for(Method m : IRCCommandHandler.class.getDeclaredMethods())
					for(Annotation a : m.getDeclaredAnnotations())
						if(a instanceof Hook)
						{
							if(matchesHook((Hook)a, msg))
								if((Boolean)m.invoke(null, srv, msg))
									return;
						}
						else if(a instanceof Hooks)
							for(Hook h : ((Hooks)a).value())
								if(matchesHook(h, msg))
									if((Boolean)m.invoke(null, srv, msg))
										return;
				handleUnhandled(srv, msg);
			}
			catch(IllegalAccessException e) { }
			catch(InvocationTargetException e)
			{
				if(e.getCause() instanceof RuntimeException)
					throw (RuntimeException)e.getCause();
			}
		}

		@Hook(command = "001")
		private static boolean onWelcome(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 1)
				srv.ourNick = msg.args[0];
			srv.onRegistered();
			if(srv.preferences.getAuthMode().equals("nickserv"))
				srv.send(new IRCMessage("PRIVMSG", "NickServ", "IDENTIFY " + srv.preferences.getPassword()));
			return false;
		}

		@Hook(command = "005", minParams = 2)
		private static boolean onISupport(IRCServer srv, IRCMessage msg)
		{
			srv.config.parseISupport(Arrays.asList(msg.args).subList(1, msg.args.length - 1));
			srv.updateComparator();
			return false;
		}

		@Hook(command = "353", minParams = 4)
		private static boolean onNamesList(IRCServer srv, IRCMessage msg)
		{
			String channel = msg.args[2];
			String names = msg.args[3];
			if(srv.waitingNames.contains(channel))
			{
				Tab tab = srv.getService().findTab(srv.getTab(), channel);
				if(tab != null)
				{
					for(String nick : names.split(" "))
					{
						NickListEntry entry = new NickListEntry();
						while(nick.length() > 0 && srv.config.statusChars.contains(nick.charAt(0)))
						{
							entry.status.add(nick.charAt(0));
							nick = nick.substring(1);
						}
						entry.updateStatus(srv);
						entry.nick = nick;
						tab.nickList.addOrdered(entry);
					}
					srv.getService().changeNickList(tab);
					return true;
				}
			}
			return false;
		}

		@Hook(command = "366", minParams = 2)
		private static boolean onEndOfNames(IRCServer srv, IRCMessage msg)
		{
			String channel = msg.args[1];
			if(srv.waitingNames.contains(channel))
			{
				srv.waitingNames.remove(channel);
				return true;
			}
			return false;
		}

		@Hooks({@Hook(command = "376"), @Hook(command = "422")})
		private static boolean onMotd(IRCServer srv, IRCMessage msg)
		{
			srv.onRegistered();
			return false;
		}

		@Hook(command = "433")
		private static boolean onNickInUse(IRCServer srv, IRCMessage msg)
		{
			if(!srv.registered)
			{
				srv.ourNick = srv.nextNick(srv.ourNick);
				srv.send(new IRCMessage("NICK", srv.ourNick));
			}
			return false;
		}

		@Hooks({@Hook(command = "902"), @Hook(command = "903"), @Hook(command = "904"), @Hook(command = "906")})
		private static boolean onSASLReply(IRCServer srv, IRCMessage msg)
		{
			finishSASL(srv);
			return false;
		}

		private static void startSASL(IRCServer srv)
		{
			srv.authenticating = true;
			if(srv.preferences.getAuthMode().equals("sasl") && srv.config.capsEnabled.contains("sasl"))
				srv.send(new IRCMessage("AUTHENTICATE", "PLAIN"));
			else
				finishSASL(srv);
		}

		private static void finishSASL(IRCServer srv)
		{
			if(srv.authenticating)
			{
				srv.send(new IRCMessage("CAP", "END"));
				srv.authenticating = false;
			}
		}

		@Hook(command = "AUTHENTICATE", minParams = 1)
		private static boolean onAuthenticate(IRCServer srv, IRCMessage msg)
		{
			if(srv.preferences.getAuthMode().equals("sasl"))
				if(msg.args[0].equals("+"))
				{
					String payload = srv.preferences.getUser() + "\0" + srv.preferences.getUser() + "\0" + srv.preferences.getPassword();
					String encoded = Base64.encodeToString(payload.getBytes(), Base64.NO_WRAP);
					int packetSize = 400;
					for(int i = 0; i <= encoded.length(); i += packetSize)
					{
						if(i <= encoded.length() - packetSize)
							srv.send(new IRCMessage("AUTHENTICATE", encoded.substring(i, i + packetSize)));
						else if(i < encoded.length())
							srv.send(new IRCMessage("AUTHENTICATE", encoded.substring(i)));
						else
							srv.send(new IRCMessage("AUTHENTICATE", "+"));
					}
				}
			return true;
		}

		@Hook(command = "CAP", minParams = 2)
		private static boolean onCap(IRCServer srv, IRCMessage msg)
		{
			srv.config.haveCaps = true;
			String command = msg.args[1];
			if(command.equalsIgnoreCase("LS") && msg.args.length >= 3)
			{
				srv.config.capsSupported.addAll(Arrays.asList(msg.args[2].split(" ")));

				String request = "";

				List<String> wantCaps = new ArrayList<>(Arrays.asList("account-notify", "extended-join", "multi-prefix"));
				if(srv.preferences.getAuthMode().equals("sasl"))
					wantCaps.add("sasl");
				for(String cap : wantCaps)
					if(srv.config.capsSupported.contains(cap))
						request += cap + " ";

				srv.send(new IRCMessage("CAP", "REQ", request));
				return true;
			}
			else if(command.equalsIgnoreCase("ACK"))
			{
				srv.config.capsEnabled.addAll(Arrays.asList(msg.args[2].split(" ")));
				startSASL(srv);
				return true;
			}
			return false;
		}

		@Hook(command = "JOIN", minParams = 1, requireSource = true)
		private static boolean onJoin(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			Tab tab = srv.getService().findTab(srv.getTab(), channel);
			if(srv.config.nickCollator.equals(nick, srv.ourNick))
			{
				if(tab == null)
				{
					tab = srv.getService().createTab(srv.getTab(), channel);
					tab.nickList = new OrderedList<>(srv.nickListEntryComparator);
				}
				else
				{
					tab.nickList.clear();
					srv.service.changeNickList(tab);
				}
				tab.nickList.setComparator(srv.nickListEntryComparator);
				srv.waitingNames.add(channel);
			}
			else if(tab != null)
			{
				tab.nickList.addOrdered(new NickListEntry(nick));
				srv.getService().changeNickList(tab);
			}
			if(tab != null)
				tab.putLine(new TextEvent(JOIN, nick, msg.getUserHost(), channel));
			return tab != null;
		}

		@Hook(command = "MODE", minParams = 2, requireSource = true)
		private static boolean onMode(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String channel = msg.args[0];
			String mode = msg.args[1];
			Tab tab = srv.getService().findTab(srv.getTab(), channel);
			if(tab != null)
			{
				boolean changed = false;
				for(IRCServerConfig.Mode m : srv.config.parseModes(mode, Arrays.asList(msg.args).subList(2, msg.args.length)))
					if(m.isStatus())
						for(int j = 0; j < tab.nickList.size(); j++)
						{
							NickListEntry entry = tab.nickList.get(j);
							if(srv.config.nickCollator.equals(entry.nick, m.getArgument()))
							{
								if(m.isSet())
									entry.status.add(m.getStatus());
								else
									entry.status.remove(m.getStatus());
								entry.updateStatus(srv);
								tab.nickList.setOrdered(j, entry);
								changed = true;
								break;
							}
						}
				String modes = mode;
				for(int i = 2; i < msg.args.length; i++)
					modes += " " + msg.args[i];
				tab.putLine(new TextEvent(MODE_CHANGE, from, channel, modes));
				if(changed)
					srv.getService().changeNickList(tab);
			}
			return tab != null;
		}

		@Hook(command = "NICK", minParams = 1, requireSource = true)
		private static boolean onNick(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String to = msg.args[0];
			boolean seen = false;
			if(srv.config.nickCollator.equals(from, srv.ourNick))
			{
				srv.ourNick = to;
				srv.getTab().putLine(new TextEvent(NICK_CHANGE, from, to));
				seen = true;
			}
			for(Tab tab : srv.getService().tabs)
				if(tab.getServerTab() == srv.serverTab && tab.nickList != null)
					for(int i = 0; i < tab.nickList.size(); i++)
					{
						NickListEntry entry = tab.nickList.get(i);
						if(srv.config.nickCollator.equals(entry.nick, from))
						{
							entry.nick = to;
							tab.nickList.setOrdered(i, entry);
							srv.getService().changeNickList(tab);
							tab.putLine(new TextEvent(NICK_CHANGE, from, to));
							seen = true;
							break;
						}
					}
			return seen;
		}

		@Hook(command = "PART", minParams = 1, requireSource = true)
		private static boolean onPart(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			String reason = msg.args.length >= 2 ? msg.args[1] : null;
			Tab tab = srv.getService().findTab(srv.serverTab, channel);
			if(srv.config.nickCollator.equals(nick, srv.ourNick))
			{
				if(tab != null)
					srv.getService().deleteTab(tab.getId());
				if(reason == null)
					srv.getTab().putLine(new TextEvent(PART, nick, msg.getUserHost(), channel));
				else
					srv.getTab().putLine(new TextEvent(PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
				return true;
			}
			else
				if(tab != null)
				{
					boolean found = false;
					for(int i = 0; i < tab.nickList.size(); )
						if(srv.config.nickCollator.equals(tab.nickList.get(i).nick, nick))
						{
							tab.nickList.remove(i);
							found = true;
						}
						else
							i++;
					if(found)
					{
						srv.getService().changeNickList(tab);
						if(reason == null)
							tab.putLine(new TextEvent(PART, nick, msg.getUserHost(), channel));
						else
							tab.putLine(new TextEvent(PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
					}
				}
			return tab != null;
		}

		@Hook(command = "PING")
		private static boolean onPing(IRCServer srv, IRCMessage msg)
		{
			srv.send(new IRCMessage("PONG", msg.args));
			return true;
		}

		private static void sendCTCPReply(IRCServer srv, String nick, String command)
		{
			srv.send(new IRCMessage("NOTICE", nick, "\001" + command + "\001"));
		}

		private static boolean onCTCP(IRCServer srv, String nick, String target, String request)
		{
			int at = request.indexOf(' ');
			String command = at == -1 ? request : request.substring(0, at);
			String args = at == -1 ? "" : request.substring(at + 1);

			if(command.equalsIgnoreCase("ACTION"))
				if(srv.config.nickCollator.equals(target, srv.ourNick))
				{
					Tab tab = srv.getService().findTab(srv.getTab(), nick);
					if(tab == null)
						tab = srv.getService().createTab(srv.getTab(), nick);
					tab.putLine(new TextEvent(CTCP_ACTION, nick, args));
					return true;
				}
				else
				{
					Tab tab = srv.getService().findTab(srv.getTab(), target);
					if(tab != null)
						tab.putLine(new TextEvent(CTCP_ACTION, nick, args));
					return tab != null;
				}

			if(srv.config.nickCollator.equals(target, srv.ourNick))
				srv.service.getFrontTab().putLine(new TextEvent(CTCP_PRIVATE, nick, command, args));
			else
				srv.service.getFrontTab().putLine(new TextEvent(CTCP_CHANNEL, nick, command, args, target));

			if(command.equalsIgnoreCase("VERSION"))
				sendCTCPReply(srv, nick, "VERSION " + srv.service.getString(R.string.app_name));
			else if(command.equalsIgnoreCase("TIME"))
				sendCTCPReply(srv, nick, "TIME " + new Date().toString());
			else if(command.equalsIgnoreCase("PING"))
				sendCTCPReply(srv, nick, "PING " + args);

			return true;
		}

		@Hook(command = "PRIVMSG", minParams = 2, requireSource = true)
		private static boolean onPrivmsg(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String target = msg.args[0];
			String text = msg.args[1];
			if(text.length() >= 2 && text.charAt(0) == '\001' && text.charAt(text.length() - 1) == '\001')
				return onCTCP(srv, nick, target, text.substring(1, text.length() - 1));
			else if(srv.config.nickCollator.equals(target, srv.ourNick))
			{
				Tab tab = srv.getService().findTab(srv.getTab(), nick);
				if(tab == null)
					tab = srv.getService().createTab(srv.getTab(), nick);
				tab.putLine(new TextEvent(MESSAGE, nick, text));
				return true;
			}
			else
			{
				Tab tab = srv.getService().findTab(srv.getTab(), target);
				if(tab != null)
					tab.putLine(new TextEvent(MESSAGE, nick, text));
				return tab != null;
			}
		}

		@Hook(command = "QUIT", requireSource = true)
		private static boolean onQuit(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String reason = msg.args.length >= 1 ? msg.args[0] : null;
			boolean seen = false;
			for(Tab tab : srv.getService().tabs)
				if(tab.getServerTab() == srv.serverTab && tab.nickList != null)
				{
					boolean found = false;
					for(int i = 0; i < tab.nickList.size(); )
						if(srv.config.nickCollator.equals(tab.nickList.get(i).nick, nick))
						{
							tab.nickList.remove(i);
							found = true;
						}
						else
							i++;
					if(found)
					{
						seen = true;
						srv.getService().changeNickList(tab);
						if(reason == null)
							tab.putLine(new TextEvent(QUIT, nick, msg.getUserHost()));
						else
							tab.putLine(new TextEvent(QUIT_WITH_REASON, nick, msg.getUserHost(), reason));
					}
				}
			return seen;
		}

		private static void handleUnhandled(IRCServer srv, IRCMessage msg)
		{
			if(msg.command.matches("[0-9]*"))
			{
				String args = "";
				for(int i = 1; i < msg.args.length; i++)
				{
					if(i != 1)
						args += " ";
					args += msg.args[i];
				}
				srv.getTab().putLine(new TextEvent(NUMERIC, msg.command, args));
			}
			else
				srv.getTab().putLine(new TextEvent(RAW, msg.toIRC()));
		}
	}
}
