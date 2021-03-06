package com.mniip.bananapeel.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.util.BiSet;
import com.mniip.bananapeel.util.Collators;
import com.mniip.bananapeel.util.Hook;
import com.mniip.bananapeel.util.IRCMessage;
import com.mniip.bananapeel.util.IRCServerConfig;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.mniip.bananapeel.util.TextEvent.Type.*;

public class IRCServer
{
	public final IRCService service;
	public final ServerTab tab;
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
		this.tab = serverTab;
		this.preferences = preferences;
	}

	public void setPreferences(IRCServerPreferences preferences)
	{
		this.preferences = preferences;
		tab.setTitle(preferences.getName());
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
				tab.putLine(new TextEvent(ERROR, e.toString()));
				Log.d("BananaPeel", "", e);
				return;
			}
		else
			factory = SocketFactory.getDefault();

		connection = new IRCConnection(this, factory);
		connection.connect(preferences.getHost(), preferences.getPort());
	}

	public void disconnect(String reason)
	{
		if(connection != null)
			connection.disconnect();
		connection = null;
		for(Tab tab : service.getTabs())
			if(tab.serverTab == tab)
				tab.putLine(new TextEvent(ERROR, "Disconnected (" + reason + ")"));
	}

	public void send(IRCMessage msg)
	{
		if(connection != null)
			connection.send(msg);
	}

	public void onIRCMessageReceived(IRCMessage msg)
	{
		commandHandler.invoke(this, msg);
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
		boolean hadConnection = connection != null;
		disconnect(e.toString());
		Log.d("BananaPeel", "", e);
		if(hadConnection)
		{
			reconnect = new Reconnect(this);
			new Handler(Looper.getMainLooper()).postDelayed(reconnect, 5000);
		}
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

	private boolean containsNick(String nick, String text)
	{
		if(nick.length() == 0)
			return false;
		String regex = Pattern.quote(nick);
		if(Character.isLetterOrDigit(nick.charAt(0)))
			regex = "\\b" + regex;
		if(Character.isLetterOrDigit(nick.charAt(nick.length() - 1)))
			regex = regex + "\\b";
		return Pattern.compile(regex).matcher(text).find();
	}

	private boolean containsOurNick(String text)
	{
		return containsNick(ourNick, text);
	}

	private Comparator<NickListEntry> nickListEntryComparator = NickListEntry.nickComparator(Collators.rfc1459());

	private void updateComparator()
	{
		nickListEntryComparator = new ComposedComparator<>(NickListEntry.statusComparator(config.statusChars), NickListEntry.nickComparator(config.nickCollator));
		for(Tab tab : service.getTabs())
			if(tab.serverTab.server == this && tab.nickList != null)
			{
				tab.nickList.setSecondaryComparator(nickListEntryComparator);
				service.changeNickList(tab);
			}
	}

	abstract private static class Command implements Hook<IRCServer, IRCMessage> {}

	private static class Require extends Hook.Filtered<IRCServer, IRCMessage>
	{
		private boolean source = false;
		private int minArgs = 0;

		private Require(Command command)
		{
			super(command);
		}

		private Require needSource(boolean source)
		{
			this.source = source;
			return this;
		}

		private Require needArgs(int minArgs)
		{
			this.minArgs = minArgs;
			return this;
		}

		public boolean filter(IRCMessage msg)
		{
			if(source && msg.source == null)
				return false;
			return minArgs <= msg.args.length;
		}
	}

	private static Command rawHook = new Command()
	{
		public boolean invoke(IRCServer srv, IRCMessage msg)
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
				srv.tab.putLine(new TextEvent(NUMERIC, msg.command, args));
			}
			else
				srv.tab.putLine(new TextEvent(RAW, msg.toIRC()));
			return true;
		}
	};

	private static Hook.Binned<String, IRCServer, IRCMessage> commandBins = new Hook.Binned<String,IRCServer,IRCMessage>()
	{
		public String resolve(IRCMessage data)
		{
			return data.command.toUpperCase();
		}
	};

	private static Hook<IRCServer, IRCMessage> commandHandler = new Hook.Sequence<>(Arrays.asList(commandBins, rawHook));

	static
	{
		commandBins.add("001", new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				if(msg.args.length >= 1)
					srv.ourNick = msg.args[0];
				srv.onRegistered();
				if(srv.preferences.getAuthMode().equals("nickserv"))
					srv.send(new IRCMessage("PRIVMSG", "NickServ", "IDENTIFY " + srv.preferences.getPassword()));
				return false;
			}
		});

		commandBins.add("005", new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.config.parseISupport(Arrays.asList(msg.args).subList(1, msg.args.length - 1));
				srv.updateComparator();
				return false;
			}
		});

		commandBins.add("353", new Require(new Command()
		{
			@Override
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String channel = msg.args[2];
				String names = msg.args[3];
				if(srv.waitingNames.contains(channel))
				{
					Tab tab = srv.service.findTab(srv.tab, channel);
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
							tab.nickList.add(entry);
						}
						srv.service.changeNickList(tab);
						return true;
					}
				}
				return false;
			}
		}).needArgs(4));

		commandBins.add("366", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String channel = msg.args[1];
				if(srv.waitingNames.contains(channel))
				{
					srv.waitingNames.remove(channel);
					return true;
				}
				return false;
			}
		}).needArgs(2));

		Command onMotd = new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.onRegistered();
				return false;
			}
		};
		commandBins.add("376", onMotd);
		commandBins.add("422", onMotd);

		commandBins.add("433", new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				if(!srv.registered)
				{
					srv.ourNick = srv.nextNick(srv.ourNick);
					srv.send(new IRCMessage("NICK", srv.ourNick));
				}
				return false;
			}
		});

		Require whoisData = new Require(new Command()
		{
			@Override
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.service.getFrontServerTab(srv).putLine(new TextEvent(WHOIS_DATA, msg.args[1], msg.args[2]));
				return true;
			}
		}).needArgs(3);
		for(String bin : Arrays.asList("276", "313", "318", "319", "320", "671"))
			commandBins.add(bin, whoisData);

		commandBins.add("311", new Require(new Command()
		{
			@Override
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.service.getFrontServerTab(srv).putLine(new TextEvent(WHOIS_USERHOST, msg.args[1], msg.args[2], msg.args[3], msg.args[5]));
				return true;
			}
		}).needArgs(6));

		commandBins.add("312", new Require(new Command()
		{
			@Override
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.service.getFrontServerTab(srv).putLine(new TextEvent(WHOIS_DATA, msg.args[1], msg.args[2] + ", " + msg.args[3]));
				return true;
			}
		}).needArgs(4));

		commandBins.add("317", new Require(new Command()
		{
			@Override
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String idle = msg.args[2];
				try
				{
					long total = Long.valueOf(idle);
					idle = String.format("%02d:%02d:%02d", total / 3600, total / 60 % 60, total % 60);
				}
				catch(NumberFormatException e) {}

				String signon = msg.args[3];
				try
				{
					Date date = new Date(Long.valueOf(signon) * 1000);
					signon = String.format("%tc", date);
				}
				catch(NumberFormatException e) {}

				srv.service.getFrontServerTab(srv).putLine(new TextEvent(WHOIS_IDLE, msg.args[1], idle, signon));
				return true;
			}
		}).needArgs(4));

		commandBins.add("330", new Require(new Command()
		{
			@Override
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.service.getFrontServerTab(srv).putLine(new TextEvent(WHOIS_LOGIN, msg.args[1], msg.args[2]));
				return true;
			}
		}).needArgs(3));
	}

	private void startSASL()
	{
		authenticating = true;
		if(preferences.getAuthMode().equals("sasl") && config.capsEnabled.contains("sasl"))
			send(new IRCMessage("AUTHENTICATE", "PLAIN"));
		else
			finishSASL();
	}

	private void finishSASL()
	{
		if(authenticating)
		{
			send(new IRCMessage("CAP", "END"));
			authenticating = false;
		}
	}

	static
	{
		Command onSASLEnd = new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.finishSASL();
				return false;
			}
		};
		commandBins.add("902", onSASLEnd);
		commandBins.add("903", onSASLEnd);
		commandBins.add("904", onSASLEnd);
		commandBins.add("906", onSASLEnd);

		commandBins.add("AUTHENTICATE", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
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
		}).needArgs(1));

		commandBins.add("CAP", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.config.haveCaps = true;
				String command = msg.args[1];
				if(command.equalsIgnoreCase("LS") && msg.args.length >= 3)
				{
					srv.config.capsSupported.addAll(Arrays.asList(msg.args[2].split(" ")));

					String request = "";

					List<String> wantCaps = new ArrayList<>(Arrays.asList("account-notify", "extended-join", "multi-prefix", "server-time", "znc.in/server-time", "znc.in/server-time-iso"));
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
					srv.startSASL();
					return true;
				}
				return false;
			}
		}).needArgs(2));


		commandBins.add("ERROR", new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.disconnect(msg.args.length > 0 ? msg.args[0] : "");
				return true;
			}
		});

		commandBins.add("JOIN", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String nick = msg.getNick();
				String channel = msg.args[0];
				Tab tab = srv.service.findTab(srv.tab, channel);
				if(srv.config.nickCollator.equals(nick, srv.ourNick))
				{
					if(tab == null)
					{
						tab = srv.service.createTab(srv.tab, Tab.Type.CHANNEL, channel);
						tab.nickList = new BiSet<>(NickListEntry.nickComparator(srv.config.nickCollator), srv.nickListEntryComparator);
					}
					else
					{
						tab.nickList.clear();
						srv.service.changeNickList(tab);
					}
					tab.nickList.setSecondaryComparator(srv.nickListEntryComparator);
					srv.waitingNames.add(channel);
				}
				else if(tab != null)
				{
					tab.nickList.add(new NickListEntry(nick));
					srv.service.changeNickList(tab);
				}
				if(tab != null)
					tab.putLine(new TextEvent(JOIN, nick, msg.getUserHost(), channel));
				return tab != null;
			}
		}).needSource(true).needArgs(1));

		commandBins.add("KICK", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String kicker = msg.getNick();
				String channel = msg.args[0];
				String target = msg.args[1];
				String reason = msg.args.length >= 3 ? msg.args[2] : "";
				Tab tab = srv.service.findTab(srv.tab, channel);
				if(srv.config.nickCollator.equals(target, srv.ourNick))
				{
					if(tab != null)
						srv.service.deleteTab(tab.id);
					srv.tab.putLine(new TextEvent(KICK, kicker, target, channel, reason));
					return true;
				}
				else if(tab != null)
				{
					NickListEntry entry = tab.nickList.findPrimary(new NickListEntry(target));
					if(entry != null)
					{
						tab.nickList.remove(entry);
						srv.service.changeNickList(tab);
						tab.putLine(new TextEvent(KICK, kicker, target, channel, reason));
					}
				}
				return tab != null;
			}
		}).needSource(true).needArgs(2));

		commandBins.add("MODE", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String from = msg.getNick();
				String channel = msg.args[0];
				String mode = msg.args[1];
				Tab tab = srv.service.findTab(srv.tab, channel);
				if(tab != null)
				{
					boolean changed = false;
					for(IRCServerConfig.Mode m : srv.config.parseModes(mode, Arrays.asList(msg.args).subList(2, msg.args.length)))
						if(m.isStatus())
						{
							NickListEntry entry = tab.nickList.findPrimary(new NickListEntry(m.getArgument()));
							if(entry != null)
							{
								tab.nickList.remove(entry);
								if(m.isSet())
									entry.status.add(m.getStatus());
								else
									entry.status.remove(m.getStatus());
								entry.updateStatus(srv);
								tab.nickList.add(entry);
								changed = true;
							}
						}
					String modes = mode;
					for(int i = 2; i < msg.args.length; i++)
						modes += " " + msg.args[i];
					tab.putLine(new TextEvent(MODE_CHANGE, from, channel, modes));
					if(changed)
						srv.service.changeNickList(tab);
				}
				return tab != null;
			}
		}).needSource(true).needArgs(2));

		commandBins.add("NICK", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String from = msg.getNick();
				String to = msg.args[0];
				boolean seen = false;
				if(srv.config.nickCollator.equals(from, srv.ourNick))
				{
					srv.ourNick = to;
					srv.tab.putLine(new TextEvent(NICK_CHANGE, from, to));
					seen = true;
				}
				NickListEntry search = new NickListEntry(from);
				for(Tab tab : srv.service.getTabs())
					if(tab.serverTab == srv.tab && tab.nickList != null)
					{
						NickListEntry entry = tab.nickList.findPrimary(search);
						if(entry != null)
						{
							tab.nickList.remove(entry);
							entry.nick = to;
							entry.updateStatus(srv);
							tab.nickList.add(entry);
							srv.service.changeNickList(tab);
							tab.putLine(new TextEvent(NICK_CHANGE, from, to));
							seen = true;
						}
					}
				return seen;
			}
		}).needSource(true).needArgs(1));

		commandBins.add("NOTICE", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String nick = msg.getNick();
				String target = msg.args[0];
				String text = msg.args[1];

				Date time = getTimestamp(srv, msg);
				if(srv.config.nickCollator.equals(target, srv.ourNick))
				{
					if(text.length() >= 2 && text.charAt(0) == '\001' && text.charAt(text.length() - 1) == '\001')
						srv.service.getFrontServerTab(srv).putLine(new TextEvent(time, CTCP_REPLY, nick, text.substring(1, text.length() - 1)));
					else
						srv.service.getFrontServerTab(srv).putLine(new TextEvent(time, NOTICE, nick, text));
					return true;
				}
				else
				{
					Tab tab = srv.service.findTab(srv.tab, target);
					if(tab != null)
						tab.putLine(new TextEvent(time, CHANNEL_NOTICE, nick, target, text));
					return tab != null;
				}
			}
		}).needSource(true).needArgs(2));

		commandBins.add("PART", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String nick = msg.getNick();
				String channel = msg.args[0];
				String reason = msg.args.length >= 2 ? msg.args[1] : null;
				Tab tab = srv.service.findTab(srv.tab, channel);
				if(srv.config.nickCollator.equals(nick, srv.ourNick))
				{
					if(tab != null)
						srv.service.deleteTab(tab.id);
					if(reason == null)
						srv.tab.putLine(new TextEvent(PART, nick, msg.getUserHost(), channel));
					else
						srv.tab.putLine(new TextEvent(PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
					return true;
				}
				else if(tab != null)
				{
					NickListEntry entry = tab.nickList.findPrimary(new NickListEntry(nick));
					if(entry != null)
					{
						tab.nickList.remove(entry);
						srv.service.changeNickList(tab);
						if(reason == null)
							tab.putLine(new TextEvent(PART, nick, msg.getUserHost(), channel));
						else
							tab.putLine(new TextEvent(PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
					}
				}
				return tab != null;
			}
		}).needSource(true).needArgs(1));

		commandBins.add("PING", new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				srv.send(new IRCMessage("PONG", msg.args));
				return true;
			}
		});

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
				Tab tab = srv.service.findTab(srv.tab, nick);
				if(tab == null)
					tab = srv.service.createTab(srv.tab, Tab.Type.QUERY, nick);
				tab.putLine(new TextEvent(srv.containsOurNick(args) ? CTCP_ACTION_HIGHLIGHT : CTCP_ACTION, nick, args));
				return true;
			}
			else
			{
				Tab tab = srv.service.findTab(srv.tab, target);
				if(tab != null)
					tab.putLine(new TextEvent(srv.containsOurNick(args) ? CTCP_ACTION_HIGHLIGHT : CTCP_ACTION, nick, args));
				return tab != null;
			}

		if(srv.config.nickCollator.equals(target, srv.ourNick))
			srv.service.getFrontServerTab(srv).putLine(new TextEvent(CTCP_PRIVATE, nick, command, args));
		else
			srv.service.getFrontServerTab(srv).putLine(new TextEvent(CTCP_CHANNEL, nick, command, args, target));

		if(command.equalsIgnoreCase("VERSION"))
			sendCTCPReply(srv, nick, "VERSION " + srv.service.getString(R.string.app_name));
		else if(command.equalsIgnoreCase("TIME"))
			sendCTCPReply(srv, nick, "TIME " + new Date().toString());
		else if(command.equalsIgnoreCase("PING"))
			sendCTCPReply(srv, nick, "PING " + args);

		return true;
	}

	private static Date getTimestamp(IRCServer srv, IRCMessage msg)
	{
		if(srv.config.capsEnabled.contains("znc.in/server-time-iso") || srv.config.capsEnabled.contains("server-time"))
			if(msg.tags != null && msg.tags.get("time") != null)
				try
				{
					return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(msg.tags.get("time"));
				}
				catch(ParseException e) {}
		if(srv.config.capsEnabled.contains("znc.in/server-time"))
			if(msg.tags != null && msg.tags.get("t") != null)
				try
				{
					return new Date(Long.valueOf(msg.tags.get("t")) * 1000);
				}
				catch(NumberFormatException e) {}
		return null;
	}

	static
	{
		commandBins.add("PRIVMSG", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String nick = msg.getNick();
				String target = msg.args[0];
				String text = msg.args[1];

				Date time = getTimestamp(srv, msg);
				if(text.length() >= 2 && text.charAt(0) == '\001' && text.charAt(text.length() - 1) == '\001')
					return onCTCP(srv, nick, target, text.substring(1, text.length() - 1));
				else if(srv.config.nickCollator.equals(target, srv.ourNick))
				{
					Tab tab = srv.service.findTab(srv.tab, nick);
					if(tab == null)
						tab = srv.service.createTab(srv.tab, Tab.Type.QUERY, nick);
					tab.putLine(new TextEvent(time, srv.containsOurNick(text) ? MESSAGE_HIGHLIGHT : MESSAGE, nick, text));
					return true;
				}
				else
				{
					Tab tab = srv.service.findTab(srv.tab, target);
					if(tab != null)
						tab.putLine(new TextEvent(time, srv.containsOurNick(text) ? MESSAGE_HIGHLIGHT : MESSAGE, nick, text));
					return tab != null;
				}
			}
		}).needSource(true).needArgs(2));

		commandBins.add("QUIT", new Require(new Command()
		{
			public boolean invoke(IRCServer srv, IRCMessage msg)
			{
				String nick = msg.getNick();
				String reason = msg.args.length >= 1 ? msg.args[0] : null;
				boolean seen = false;
				NickListEntry search = new NickListEntry(nick);
				for(Tab tab : srv.service.getTabs())
					if(tab.serverTab == srv.tab && tab.nickList != null)
					{
						NickListEntry entry = tab.nickList.findPrimary(search);
						if(entry != null)
						{
							tab.nickList.remove(entry);
							srv.service.changeNickList(tab);
							seen = true;
							if(reason == null)
								tab.putLine(new TextEvent(QUIT, nick, msg.getUserHost()));
							else
								tab.putLine(new TextEvent(QUIT_WITH_REASON, nick, msg.getUserHost(), reason));
						}
					}
				return seen;
			}
		}).needSource(true));
	}
}
