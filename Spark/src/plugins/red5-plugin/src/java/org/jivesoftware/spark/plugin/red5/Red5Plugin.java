package org.jivesoftware.spark.plugin.red5;

import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.File; // new
import java.io.FileInputStream; // new
import java.io.IOException; // new

import com.centerkey.utils.BareBonesBrowserLaunch;

import org.jivesoftware.spark.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.spark.component.*;
import org.jivesoftware.spark.component.browser.*;
import org.jivesoftware.spark.plugin.*;
import org.jivesoftware.spark.ui.*;
import org.jivesoftware.spark.util.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.spark.util.log.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Message.Type;

public class Red5Plugin
    implements Plugin, PacketFilter, PacketListener, ChatRoomListener, ContactListListener, PresenceListener
{
	private ContextMenuListener contextMenuListener;
	private ChatRoomListenerAdapter chatRoomListener;
	private ContactList contactList;
	private org.jivesoftware.spark.ChatManager chatManager;
	private Workspace workspace;
	private org.jivesoftware.spark.ui.ContactItem contactItem;
	private ImageIcon red5Icon;
	private UserManager userManager;
	private XMPPConnection conn;
	private Collection<String> participants = new ArrayList<String>();
	private BrowserViewer confBrowser;
	private BrowserViewer browser;

	private String bandwidth = "25600";
	private String picQuality = "0";
	private String framesPerSec = "15";
	private String micSetRate = "8";
	private String red5URL = "rtmp:/oflaDemo";
	private String red5Name = "red5";
	private String red5server = SparkManager.getSessionManager().getServerAddress(); // new
	private String red5port = "7070"; // new

	private Presence.Mode previousMode = Presence.Mode.available;
	private String previousStatus = "Available";
	private String currentCallerJID;
	private JFrame browserConfViewer = null;

	private String red5GWChannel = null;

	private boolean isOpen  = false;

	  private static File pluginsettings = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Spark" + System.getProperty("file.separator") + "red5.properties"); //new

    public Red5Plugin() {
		ClassLoader cl = getClass().getClassLoader();
		red5Icon = new ImageIcon(cl.getResource("images/logo_small.gif"));
    }

    public void initialize()  {
		workspace = SparkManager.getWorkspace();
		contactList = workspace.getContactList();
		chatManager = SparkManager.getChatManager();
		userManager = SparkManager.getUserManager();

		// Roster Contact Context Menu Item


		final Action videoRosterAction = new AbstractAction() {
			public static final long serialVersionUID = 24362462L;

			public void actionPerformed(ActionEvent actionEvent) {

				String youAddress =  contactItem.getJID();
				String you = youAddress.substring(0, youAddress.indexOf('@'));

				if (participants.contains(you))
					participants.remove(you);
				else
					participants.add(you);

				displayConfBrowser();
			}
		};

		videoRosterAction.putValue(Action.NAME, "Add/Remove Video Roster");



		// VideoMessage Contact Context Menu Item


		final Action videoMessageAction = new AbstractAction() {
			public static final long serialVersionUID = 24362463L;

			public void actionPerformed(ActionEvent actionEvent) {

				String youAddress =  contactItem.getJID();
				recordMessageWindow(youAddress);

			}
		};

		videoMessageAction.putValue(Action.NAME, "Send Video Message");




		// Screen viewer Contact Context Menu Item


		final Action screenViewerAction = new AbstractAction() {
			public static final long serialVersionUID = 24362463L;

			public void actionPerformed(ActionEvent actionEvent) {

				String youAddress =  contactItem.getJID();
				viewScreenWindow(youAddress);

			}
		};

		screenViewerAction.putValue(Action.NAME, "View Desktop Screen");



		// Listener for Spark Contact Context Menus


		contextMenuListener = new ContextMenuListener() {
			public void poppingUp(Object object, JPopupMenu popup) {

				if(object instanceof ContactItem){
					contactItem =  (ContactItem) object;
					popup.addSeparator();

					JMenuItem rosterMenuItem = popup.add(videoRosterAction);
					rosterMenuItem.setIcon(red5Icon);

					JMenuItem videoMessageMenuItem = popup.add(videoMessageAction);
					videoMessageMenuItem.setIcon(red5Icon);

					JMenuItem screenViewerMenuItem = popup.add(screenViewerAction);
					screenViewerMenuItem.setIcon(red5Icon);
				}
			}

			public void poppingDown(JPopupMenu popup) {

			}

			public boolean handleDefaultAction(MouseEvent e) {
				return false;
			}
		};



		// Roster Video Roster Menu Item

		final Action audioVideoConfAction = new AbstractAction() {
			public static final long serialVersionUID = 24362463L;

			public void actionPerformed(ActionEvent actionEvent) {
				displayConfBrowser();
			}
		};

        audioVideoConfAction.putValue(Action.NAME, "Open Audio/Video Roster");


		// View Video Messages Menu Item

		final Action viewVideoMessagesAction = new AbstractAction() {
			public static final long serialVersionUID = 24362463L;

			public void actionPerformed(ActionEvent actionEvent) {
				playMessageWindow();
			}
		};

        viewVideoMessagesAction.putValue(Action.NAME, "View Audio/Video Messages");



		// Publish Screen Menu Item

		final Action publishScreenAction = new AbstractAction() {
			public static final long serialVersionUID = 24362463L;

			public void actionPerformed(ActionEvent actionEvent) {
				publishScreenWindow();
			}
		};

        publishScreenAction.putValue(Action.NAME, "Publish Desktop Screen");


		// Red5 Menu Item

		JMenu myPluginMenu = new JMenu("Red5");
        myPluginMenu.add(audioVideoConfAction);
        myPluginMenu.add(viewVideoMessagesAction);
        myPluginMenu.add(publishScreenAction);
        SparkManager.getMainWindow().getJMenuBar().add(myPluginMenu);


		// Room Listener for auto video roster add/remove


		contactList.addContextMenuListener(contextMenuListener);
		chatManager.addChatRoomListener(this);


		// Smack Packet Listener

		conn = SparkManager.getConnection();
		conn.addPacketListener(this, this);

		// Spark Prsence Listener

		SparkManager.getSessionManager().addPresenceListener(this);

		// new: Read properties from configuration file
    Properties props = new Properties(); // new

    if (pluginsettings.exists()) { // new
      Log.warning("Red5-Info: Properties-file does exist= " + pluginsettings.getPath()); // new
      try { // new
        props.load(new FileInputStream(pluginsettings)); // new
        red5server = props.getProperty("server"); // new
        Log.warning("Red-Info: Red5-servername from properties-file is= " + red5server); // new
        red5port = props.getProperty("port"); // new
        Log.warning("Red5-Info: Red5-port from properties-file is= " + red5port); // new
      } catch (IOException ioe) { // new
        System.err.println(ioe); // new
        //TODO handle error better. // new
      } // new
    } // new
    else { // new
      Log.error("Red5-Error: Properties-file does not exist= " + pluginsettings.getPath()); // new
    } // new


		// Service Disco for red5 phone component and red5 properties

		try {

			ServiceDiscoveryManager discoManager  = new ServiceDiscoveryManager(conn);

			DiscoverInfo discoInfo = discoManager.discoverInfo("red5." + SparkManager.getSessionManager().getServerAddress());
			Iterator it = discoInfo.getIdentities();

			while (it.hasNext()) {
			  DiscoverInfo.Identity identity = (DiscoverInfo.Identity) it.next();

			  if (identity.getCategory().indexOf("|") > 0 ) {	// evil hack to pass red5 properties
					String [] red5Props = identity.getCategory().split("\\|");

					bandwidth = red5Props[0];
					picQuality = red5Props[1];
					framesPerSec = red5Props[2];
					micSetRate = red5Props[3];
				  	red5URL = red5Props[4];
				  	red5port = red5Props[5];
				  	red5Name = red5Props[6];

				  	Log.warning("Bandwidth = " + bandwidth);
				  	Log.warning("Pic Quality = " + picQuality);
				  	Log.warning("FPS = " + framesPerSec);
				  	Log.warning("Mic Samp = " + micSetRate);
				  	Log.warning("Red5 URL = " + red5URL);
				  	Log.warning("Red5 Web Root = " + red5Name);
				  	Log.warning("Web Port = " + red5port);
				}
			}
		}
		catch (Exception e) {
			Log.error("Error doing disco:", e);
		}
    }

	public void presenceChanged(Presence prs) {

		if (prs.getStatus().indexOf("Red5") == -1) {
			previousStatus = prs.getStatus();
			previousMode = prs.getMode();
		}

	}


    public void shutdown()
    {

    }

    public boolean canShutDown()
    {
        return true;
    }

    public void uninstall()
    {
		SparkManager.getSessionManager().removePresenceListener(this);
		conn.removePacketListener(this);
		contactList.removeContextMenuListener(contextMenuListener);
		chatManager.removeChatRoomListener(this);
    }

    public void contactItemAdded(ContactItem item) {

	}

    public void contactItemRemoved(ContactItem item) {

	}

    public void contactItemDoubleClicked(ContactItem item) {

	}

    public void contactItemClicked(ContactItem item) {

	}

    public void contactGroupAdded(ContactGroup item) {

	}

    public void contactGroupRemoved(ContactGroup item) {

	}


    public void chatRoomLeft(ChatRoom chatroom)
    {
    }

    public void chatRoomClosed(ChatRoom chatroom)
    {
    }

    public void chatRoomActivated(ChatRoom chatroom)
    {
    }

    public void userHasJoined(ChatRoom room, String s)
    {

		String u = room.getRoomname();

		if (!"groupchat".equals(room.getChatType().toString())) {
			u = u.substring(0, u.indexOf('@'));
		} else {
			u = s;
		}

		//Log.error("User " + u + " enters " + room.getChatType());

		if (!"".equals(u)) {

			if (!participants.contains(u)) {
				participants.add(u);

				if (isOpen) {
					displayConfBrowser();
				}
			}
		}
    }

    public void userHasLeft(ChatRoom room, String s)
    {

		String u = room.getRoomname();

		if (!"groupchat".equals(room.getChatType().toString())) {
			u = u.substring(0, u.indexOf('@'));
		} else {
			u = s;
		}

		//Log.error("User " + u + " leaves " + room.getChatType());

		if (!"".equals(u)) {

			if (participants.contains(u)) {
				participants.remove(u);

				if (isOpen) {
					displayConfBrowser();
				}
			}
		}

    }

    public void chatRoomOpened(final ChatRoom room)
    {
    }


	private String getConfUser(String participant) {

		String nickname = null;
		String jid = null;
		String username = "";

		if (participant.indexOf('/')  > -1 ) {
			nickname = participant.substring(participant.indexOf('/') + 1);
			jid = userManager.getJIDFromDisplayName(nickname);
			if (jid != null){
				username = jid.substring(0, jid.indexOf('@'));
			}
			else {
				username = nickname;
			}

		}

		return username;
	}


    private String getOthers() {

		String myAddress =  SparkManager.getSessionManager().getJID();
		String me = myAddress.substring(0, myAddress.indexOf('@'));

		String others = me;

		for (String participant : participants) {

			if (!participant.equals(me)) {
				others = others + "|" + participant;
			}
		}
		return others;
	}


	public boolean accept(Packet arg0) {

		return true;
	}

	public void processPacket(Packet arg0) {

		try {

			if (arg0 instanceof Message) {

				PacketExtension phone = arg0.getExtension("phone-event", "http://jivesoftware.com/xmlns/phone");

				if (phone != null) {

					Log.error("Message  recieved/sent " + phone.toXML());

					String xml = phone.toXML();
					String caller = getTag(xml,"callerIDName");

					if (caller != null ) {

						if (!"null".equals(caller)) {
							String [] callerParams = getTag(xml,"callerID").split("\\|");
							String action = callerParams[0];
							String user = callerParams[1];

							//Log.error("Red5 Audio/Video parameters " + callerParams + " " + caller);


							if ("ring".equals(action)) {

      							int n = JOptionPane.showConfirmDialog (null,"Accept call from " + caller + "?","Red5 Call",JOptionPane.YES_NO_OPTION);

      							if(n == JOptionPane.YES_OPTION) {
									IncomingCallHandler incomingCallHandler = new IncomingCallHandler(arg0);
								}
							} else

							if ("connect".equals(action)) {
								red5GWChannel = arg0.getPacketID();
								show2WayWindow(user);
							}
						}
					}
				}

			} else if (arg0 instanceof Presence) {

				Presence prs = (Presence)arg0;

			} else if (arg0 instanceof IQ) {

				IQ iq = (IQ)arg0;
				//Log.error("IQ  recieved/sent " + iq.toXML());

			}
		}
		catch (Exception e) {
			Log.error("Error process packet:", e);
		}

	}

	private void onThePhone(String caller) {

		Presence presence = new Presence(Presence.Type.available);
		presence.setPriority(1);
		presence.setStatus("On a Red5 Call with " + caller);
		presence.setMode(Presence.Mode.dnd);

		SparkManager.getSessionManager().changePresence(presence);
	}



	private void busyNow(String message) {

		Presence presence = new Presence(Presence.Type.available);
		presence.setPriority(1);
		presence.setStatus(message);
		presence.setMode(Presence.Mode.dnd);

		SparkManager.getSessionManager().changePresence(presence);
	}


	private void show2WayWindow(String address) {

		try {

			String you = address.substring(0, address.indexOf('@'));
			String myAddress =  SparkManager.getSessionManager().getJID();
			String me = myAddress.substring(0, myAddress.indexOf('@'));
			String url = "http://" + red5server + ":" + red5port + "/" + red5Name + "/video/video320x240.html?me=" + me + "&you=" + you + "&bw=" + bandwidth + "&pq=" + picQuality + "&fps=" + framesPerSec + "&msr=" + micSetRate + "&url=" + red5URL + "&date=" + new Date(); //changed
			String title = "Red5 Call - " + you;

			displayBrowser(670, 290, url, title);

			onThePhone(you);
			currentCallerJID = address;

		}
		catch (Exception e) {
			Log.error("show2WayWindow:", e);
		}
	}


	private void recordMessageWindow(String address) {

		try {

			String you = address.substring(0, address.indexOf('@'));
			String myAddress =  SparkManager.getSessionManager().getJID();
			String me = myAddress.substring(0, myAddress.indexOf('@'));
			String url = "http://" + red5server + ":" + red5port + "/" + red5Name + "/video/videomailrecorder.html?me=" + me + "&you=" + you + "&url=" + red5URL + "&date=" + new Date(); //changed
			String title = "Red5 Message Record " + you;

			displayBrowser(322, 322, url, title);

			busyNow("Recording a Red5 Message");

		}
		catch (Exception e) {
			Log.error("recordMessageWindow:", e);
		}
	}


	private void viewScreenWindow(String address) {

		try {

			String you = address.substring(0, address.indexOf('@'));
			String url = "http://" + red5server + ":" + red5port + "/" + red5Name + "/screen/screen.html?username=" + you + "&date=" + new Date();
			String title = "Desktop Screen " + you;

			displayBrowser(610, 460, url, title);

		}
		catch (Exception e) {
			Log.error("viewScreenWindow:", e);
		}
	}



	private void publishScreenWindow() {

		try {

			String myAddress =  SparkManager.getSessionManager().getJID();
			String me = myAddress.substring(0, myAddress.indexOf('@'));
			String url = "http://" + red5server + ":" + red5port + "/" + red5Name + "/viewer?username=" + me; //changed

			BareBonesBrowserLaunch.openURL(url);
		}
		catch (Exception e) {
			Log.error("publishScreenWindow:", e);
		}
	}



	private void playMessageWindow() {

		try {

			String myAddress =  SparkManager.getSessionManager().getJID();
			String me = myAddress.substring(0, myAddress.indexOf('@'));
			String url = "http://" + red5server + ":" + red5port + "/" + red5Name + "/video/videomailplayer.html?me=" + me  + "&app=" + red5Name + "&port=" + red5port + "&server=" + red5server + "&url=" + red5URL + "&date=" + new Date(); //changed
			String title = "Red5 Message Playback ";

			displayBrowser(322, 322, url, title);

			busyNow("Viewing my Red5 Messages");

		}
		catch (Exception e) {
			Log.error("playMessageWindow:", e);
		}
	}



	private void displayBrowser(int width, int height, String url, String title) {

		try {

			final JFrame browserViewer = new JFrame(title);
			browser = BrowserFactory.getBrowser();

			browserViewer.addWindowListener(new java.awt.event.WindowAdapter() {

				public void windowClosing(WindowEvent winEvt) {

					Presence presence = new Presence(Presence.Type.available);
					presence.setPriority(1);
					presence.setStatus(previousStatus);
					presence.setMode(previousMode);

					SparkManager.getSessionManager().changePresence(presence);
					browserViewer.dispose();

					if (red5GWChannel != null) {
						IQ iq = new PhoneIQ();
						iq.setType(IQ.Type.SET );
						iq.setTo("red5." + SparkManager.getSessionManager().getServerAddress());
						PhoneExtension phonePacketExt = new PhoneExtension("phone-action", "http://jivesoftware.com/xmlns/phone", "HANGUP", "Red5", red5GWChannel);
						iq.addExtension(phonePacketExt);
						conn.sendPacket(iq);

						red5GWChannel = null;
					}
				}
			});


			browserViewer.setIconImage(SparkManager.getMainWindow().getIconImage());
			browserViewer.setContentPane(browser);

			browserViewer.pack();
			browserViewer.setSize(width, height);
			browserViewer.setLocationRelativeTo(SparkManager.getMainWindow());
			browserViewer.setVisible(true);

			browser.loadURL(url);

		}
		catch (Exception browserException) {
			Log.error("Error launching browser:", browserException);
		}

	}


	private void displayConfBrowser() {

		try {
			String myAddress =  SparkManager.getSessionManager().getJID();
			String me = myAddress.substring(0, myAddress.indexOf('@'));

			//BrowserLauncher.openURL();
			confBrowser = BrowserFactory.getBrowser();

			if (browserConfViewer == null) {
				openConfWindow();
			}

			browserConfViewer.setIconImage(SparkManager.getMainWindow().getIconImage());
			browserConfViewer.setContentPane(confBrowser);

			browserConfViewer.setSize(360,720);
			browserConfViewer.setLocationRelativeTo(null);
			browserConfViewer.setVisible(true);
			browserConfViewer.pack();

			confBrowser.loadURL("http://" + red5server + ":" + red5port + "/" + red5Name + "/video/videoConf.html?me=" + me + "&others=" + getOthers() + "&bw=" + bandwidth + "&pq=" + picQuality + "&fps=" + framesPerSec + "&msr=" + micSetRate + "&url=" + red5URL + "&date=" + new Date()); //changed
		}
		catch (Exception browserException) {
			Log.error("Error launching browser:", browserException);
		}
	}


	private void openConfWindow() {

		browserConfViewer = new JFrame("Red5 Audio/Video Presence");
		confBrowser = BrowserFactory.getBrowser();

		browserConfViewer.addWindowListener(new java.awt.event.WindowAdapter() {

			public void windowClosing(WindowEvent winEvt) {

				isOpen = false;
				browserConfViewer.dispose();
				browserConfViewer = null;
				Log.error("Closing Video Presence Window ");
			}
		});

		isOpen = true;
	}


	private String getTag(String xml, String tag) {
		String tagValue = null;

		int pos = xml.indexOf("<" + tag + ">");

		if (pos > -1) {
			String temp = xml.substring(pos + tag.length() + 2);
			pos = temp.indexOf("</" + tag + ">");

			if (pos > -1) {
				tagValue = temp.substring(0, pos);
			}
		}

		return (tagValue);
	}


    private class IncomingCallHandler extends TimerTask
    {
		private java.util.Timer  timer;
		private Packet packet;

        public IncomingCallHandler(Packet packet)
        {
            this.timer = new java.util.Timer();
            this.packet = packet;
            timer.schedule(this, 0, 1000);
        }

        public void run()
        {
			try
			{
				PacketExtension phone = packet.getExtension("phone-event", "http://jivesoftware.com/xmlns/phone");
				String xml = phone.toXML();
				String caller = getTag(xml,"callerIDName");
				String [] callerParams = getTag(xml,"callerID").split("\\|");
				String action = callerParams[0];
				String user = callerParams[1];

				red5GWChannel = packet.getPacketID();
				show2WayWindow(user);

				String myAddress =  SparkManager.getSessionManager().getJID();
				String me = myAddress.substring(0, myAddress.indexOf('@'));

				Message message = new Message();
				message.setTo(user);
				PhoneExtension phonePacketExt = new PhoneExtension("phone-event", "http://jivesoftware.com/xmlns/phone", "ON_PHONE", "Red5", packet.getPacketID());
				phonePacketExt.setValue("callerID", "connect|" + myAddress + "|" + callerParams[2] + "|" + callerParams[3] + "|" + callerParams[4] + "|" + callerParams[5] + "|" + callerParams[6]  + "|" + callerParams[7]);
				phonePacketExt.setValue("callerIDName", me);
				message.addExtension(phonePacketExt);
				conn.sendPacket(message);

            	timer.cancel();
			}

			catch (Exception e)
			{
			}
		}

    }
}
