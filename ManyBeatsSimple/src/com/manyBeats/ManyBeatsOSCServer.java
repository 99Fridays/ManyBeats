package com.manyBeats;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.relivethefuture.osc.data.BasicOscListener;
import com.relivethefuture.osc.data.OscMessage;
import com.relivethefuture.osc.transport.OscClient;
import com.relivethefuture.osc.transport.OscServer;

import java.net.InetSocketAddress;

/**
 * This is an example app that listens for OSC messages (i.e. an OSC server).
 * <p/>
 * All it needs is a port, and some defined messages that you can send it.
 * The user will take care of the rest, including running their own OSC client to send you messages.
 * <p/>
 * It uses the OSCLib library from:
 * <p/>
 * http://www.assembla.com/wiki/show/osclib
 *
 * @author odbol
 */
public class ManyBeatsOSCServer extends Activity
{
	/*Defaults. Changable via preferences */
	public static final int DEFAULT_OSC_LISTEN_PORT = 9999;

	protected static final int EDIT_PREFS = 1;

	private OscServer server;

	/**
	 * The network port your application will listen to.
	 * <p/>
	 * You should let the user set this in your own preferences.
	 */
	private int oscListenPort = DEFAULT_OSC_LISTEN_PORT;

	/**
	 * This listens for incoming OSC messages.
	 * <p/>
	 * For testing, it just prints the message, but you can do anything you want with it!
	 * <p/>
	 * It is run in a separate thread, so if you are going to update the UI you'll need to use a Handler.
	 *
	 * @author odbol
	 */
	public class LooperListener extends BasicOscListener
	{
		public Context c;

		/**
		 * this is used to update the textview in the UI thread from the listening thread.
		 *
		 * @author odbol
		 */
		private final class TextViewUpdater implements Runnable
		{
			public String msg;

			public TextViewUpdater(String message)
			{
				msg = message;
			}

			@Override
			public void run()
			{
				TextView t = (TextView) findViewById(R.id.out_text);
				t.append(msg);

				ScrollView sc = (ScrollView) findViewById(R.id.out_scroll);
				sc.smoothScrollTo(0, t.getBottom());
			}
		}

		private final class VisualizerUpdater implements Runnable
		{
			public float msg;

			public VisualizerUpdater(float message)
			{
				msg = message;
			}

			@Override
			public void run()
			{
				SimpleBeatVisualizer visualizer = (SimpleBeatVisualizer) findViewById(R.id.visualizer);
				visualizer.update(msg);
			}
		}

		/**
		 * This is the main place where you will handle individual osc messages as they come in.
		 * <p/>
		 * The message's address specifies what the user wants to change in your application: think of it as an API call.
		 * The message's arguments specify what to change those things to. You can accept multiple arguments of any primitive types.
		 */
		@Override
		public void handleMessage(OscMessage msg)
		{
			//get all the arguments for the message. in this case we're assuming one and only one argument.

			if (msg.getAddress().equals("/audio/levels"))
			{
				float left = (Float) msg.getArguments().get(0);
				float right = (Float) msg.getArguments().get(0);
				float average = (left + right) / 2;

				Handler h = new Handler(ManyBeatsOSCServer.this.getMainLooper());
				VisualizerUpdater u = new VisualizerUpdater(average);
				h.post(u);

			} else
			{
				String val = msg.getArguments().get(0).toString();
				//now update the textview.
				//since it's in the UI thread, we need to access it using a handler
				Handler h = new Handler(ManyBeatsOSCServer.this.getMainLooper());
				TextViewUpdater u = new TextViewUpdater("\nReceived: " + msg.getAddress() + " " + val);
				h.post(u);
			}
		}
	}


	/**
	 * This starts your app listening for OSC messages.
	 * <p/>
	 * You want to call this once your user chooses to start the OSC server - it probably shouldn't be started
	 * by default since it will block that port for any other apps.
	 */
	private void startListening()
	{
		stopListening(); //unbind from port

		try
		{
			server = new OscServer(oscListenPort);
			server.setUDP(
					true); //as of now, the TCP implementation of OSCLib is broken (getting buffer overflows!), so we have to use UDP.
			server.start();
		} catch (Exception e)
		{
			Toast.makeText(this, "Failed to start OSC server: " + e.getMessage(), Toast.LENGTH_LONG);
			return;
		}
		server.addOscListener(new LooperListener());

		TextView t = (TextView) findViewById(R.id.out_text);
		t.append("\nListening on port " + oscListenPort);

		SimpleBeatVisualizer visualizer = (SimpleBeatVisualizer) findViewById(R.id.visualizer);
		visualizer.setVisibility(View.VISIBLE);

		LinearLayout controlsGroup = (LinearLayout) findViewById(R.id.controlsGroup);
		controlsGroup.setVisibility(View.INVISIBLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							 WindowManager.LayoutParams.FLAG_FULLSCREEN);

//		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

		subscribe();
	}

	private void subscribe()
	{
		OscClient sender = createSender();

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		String oscMsgPath = "/subscribe";
		String oscMsgArg = "subscribe osc://:9999 +/audio/levels";
//		oscMsgPath  = p.getString("pref_osc_msg", oscMsgPath);
//		oscMsgArg  = p.getString("pref_osc_msg_arg", oscMsgArg);
		OscMessage m = new OscMessage(oscMsgPath);
		m.addArgument(oscMsgArg);
		try
		{
			sender.sendPacket(m);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private OscClient createSender()
	{
		String oscAddress = "simonetti.media.mit.edu";
		int oscPort = 9002;

/*
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		try
		{
			oscPort = Integer.parseInt(p.getString("pref_osc_port", String.valueOf(oscPort)));
		} catch (NumberFormatException e)
		{
			Toast.makeText(this, "Invalid port in preferences", Toast.LENGTH_LONG);
		}
		oscAddress = p.getString("pref_osc_addr", oscAddress);
*/

		OscClient sender;

		//start the osc client
		sender = new OscClient(true);
		InetSocketAddress address = new InetSocketAddress(oscAddress, oscPort);
		sender.connect(address);
		return sender;
	}

	private void stopListening()
	{
		OscClient sender = createSender();
		OscMessage m = new OscMessage("/unsubscribe");
		m.addArgument("unsubscribe");
		try
		{
			sender.sendPacket(m);
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		if (server != null)
		{
			server.stop();
			server = null;
		}
	}

	@Override
	public void onDestroy()
	{
		stopListening();

		super.onDestroy();
	}

	private boolean isListening = false;

	/**
	 * Called when the activity is first created. The rest of this code is just for demonstration.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		Button l = (Button) findViewById(R.id.start_listening_button);
		l.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isListening)
				{
					startListening();

					((Button) v).setText("Stop Listening");
				} else
				{
					stopListening();

					((Button) v).setText("Start Listening");
				}

				isListening = !isListening;
			}
		});

		Button prefs = (Button) findViewById(R.id.prefs_button);
		prefs.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent intent = new Intent(ManyBeatsOSCServer.this, OSCTesterClientPreferences.class);
				startActivityForResult(intent, EDIT_PREFS);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// See which child activity is calling us back.
		switch (requestCode)
		{
			case EDIT_PREFS:
				//reload prefs
				SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
				try
				{
					oscListenPort = Integer.parseInt(
							p.getString("pref_osc_port", String.valueOf(DEFAULT_OSC_LISTEN_PORT)));
				} catch (NumberFormatException e)
				{
					Toast.makeText(this, "Invalid port in preferences", Toast.LENGTH_LONG);
				}

				break;
		}
	}

}