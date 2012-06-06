package moe.lolis.metroirc.irc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.SocketFactory;

import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.events.ConnectEvent;

public class Client extends PircBotX {

	public static final String VERSION = "1.0";
	protected ArrayList<String> availableNicks;
	protected String realname = "John Doe";
	protected ServerPreferences serverPreferences;
	
	public Client() {
		super();
		this.setName("JohnDoe");
		this.setLogin("johndoe");
		this.setVersion("MetroIRC v" + VERSION);
	}
	
	@Override
    public synchronized void connect(String hostname, int port, String password, SocketFactory socketFactory) 
    		throws IOException, IrcException, NickAlreadyInUseException {
        this.server = hostname;
        this.port = port;
        this.password = password;
        this.socketFactory = socketFactory;

        if (isConnected()) {
            throw new IrcException("The client is already connected to an IRC server.  Disconnect first.");
        }
        
        // Clear everything we may have know about channels.
        this.userChanInfo.clear();

        // Connect to the server.
        if (socketFactory == null) {
            this.socket = new Socket(hostname, port, this.inetAddress, 0);
        } else {
            this.socket = socketFactory.createSocket(hostname, port, this.inetAddress, 0);
        }
        this.inetAddress = this.socket.getLocalAddress();
        this.log("*** Connected to server.");

        InputStreamReader inputStreamReader = new InputStreamReader(this.socket.getInputStream(), getEncoding());
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.socket.getOutputStream(), getEncoding());
        BufferedReader breader = new BufferedReader(inputStreamReader);
        BufferedWriter bwriter = new BufferedWriter(outputStreamWriter);

        //Construct the output and input threads
        this.inputThread = createInputThread(socket, breader);
        this.outputThread = createOutputThread(bwriter);
        this.outputThread.start();

        // Attempt to join the server.
        if (this.webIrcPassword != null) {
            this.outputThread.sendRawLineNow("WEBIRC " + this.webIrcPassword + " cgiirc " + this.webIrcHostname + " " + this.webIrcAddress.getHostAddress());
        }
        if (password != null && !password.trim().equals("")) {
            this.outputThread.sendRawLineNow("PASS " + password);
        }
        
        String tempNick = this.availableNicks.get(0);
        this.outputThread.sendRawLineNow("NICK " + tempNick);
        this.outputThread.sendRawLineNow("USER " + this.getLogin() + " 8 * :" + this.getRealname());

        // Read stuff back from the server to see if we connected.
        String line;
        int tries = 1;
        while ((line = breader.readLine()) != null) {
            this.handleLine(line);

            int firstSpace = line.indexOf(" ");
            int secondSpace = line.indexOf(" ", firstSpace + 1);
            if (secondSpace >= 0) {
                String code = line.substring(firstSpace + 1, secondSpace);

                // Check for both a successful connection. Inital connection (001-4), user stats (251-5), or MOTD (375-6)
                String[] codes = {"001", "002", "003", "004", "005", "251", "252", "253", "254", "255", "375", "376"};
                if (Arrays.asList(codes).contains(code)) {
                     // We're connected to the server.
                     break;
                } else if (code.equals("433")) {
                     // Example: AnAlreadyUsedName :Nickname already in use
                     // Iterate through all available nicknames. If we reach the end,
                     // keep adding _ to the last nickname in the list.
                     if(++tries > this.availableNicks.size()) {
                    	 if(this.autoNickChange) { 
                    		 this.availableNicks.add(this.availableNicks.get(this.availableNicks.size() - 1) + "_");
                    	 } else {
                    		 this.socket.close();
                             this.inputThread = null;
                             throw new NickAlreadyInUseException(line);
                    	 }
                     }
                     tempNick = this.availableNicks.get(tries - 1);
                     outputThread.sendRawLineNow("NICK " + tempNick);
                } else if (code.equals("439")) {
                    // Example: Client: Target change too fast. Please wait 104 seconds.
                    // No action required.
                } else if (code.startsWith("5") || code.startsWith("4")) {
                    this.socket.close();
                    this.inputThread = null;
                    throw new IrcException("Could not log into the IRC server: " + line);
                }
            }
            this.nick = tempNick;
        }

        this.loggedIn = true;
        this.log("*** Logged onto server.");

        // This makes the socket timeout on read operations the set timeout, which is 5 minutes by default.
        this.socket.setSoTimeout(getSocketTimeout());
        // Start input to start accepting lines.
        this.inputThread.start();

        this.getListenerManager().dispatchEvent(new ConnectEvent(this));
    }

	
	@Override
	public void setName(String name) {
		this.availableNicks = new ArrayList<String>();
		this.availableNicks.add(name);
		this.name = name;
	}
	
	public void addNick(String nick) {
		this.availableNicks.add(nick);
	}
	
	public void setNicks(ArrayList<String> nicks) {
		this.availableNicks = nicks;
	}
	
	public void setRealname(String realname) {
		this.realname = realname;
	}
	
	public String getRealname() {
		return this.realname;
	}
	
	public void setServerPreferences(ServerPreferences serverPrefs) {
		this.serverPreferences = serverPrefs;
	}
	
	public ServerPreferences getServerPreferences() {
		return this.serverPreferences;
	}
	
	public void loadFromPreferences(ServerPreferences preferences) {
		this.setNicks(preferences.getNicknames());
		this.setLogin(preferences.getUsername());
		this.setFinger(preferences.getNicknames().get(0));
		this.setRealname(preferences.getRealname());
		this.setServerPreferences(preferences);
	}
}
