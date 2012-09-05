/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package ru.devdep.processing.vlcj;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaMeta;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.TrackInfo;
import uk.co.caprica.vlcj.player.VideoTrackInfo;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.player.events.MediaPlayerEventType;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.sun.jna.Memory;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class VLCJVideo extends PImage implements PConstants, RenderCallback {

	public final static String VERSION = "##library.prettyVersion##";

	protected int[] copyPixels = null;
	protected PApplet parent = null;

	public int width;
	public int height;

	protected String filename;
	protected Boolean firstFrame;
	protected Boolean ready = false;
	protected Boolean repeat = false;
	protected float volume = 1.0f;
	
	protected MediaPlayerFactory factory;
	protected DirectMediaPlayer mediaPlayer;
	protected HeadlessMediaPlayer headlessMediaPlayer;

	protected static Boolean inited = false;
	public static String vlcLibPath = "";

	protected final HashMap<MediaPlayerEventType, ArrayList<Runnable>> handlers;
	protected final Stack<Runnable> tasks;
	
	public static void setVLCLibPath(String path) {
		vlcLibPath = path;
	}

	public void bind(MediaPlayerEventType type, Runnable handler) {
		ArrayList<Runnable> eventHandlers;
		if (!handlers.containsKey(type)) {
			eventHandlers = new ArrayList<Runnable>();
			handlers.put(type, eventHandlers);
		} else {
			eventHandlers = handlers.get(type);
		}
		eventHandlers.add(handler);
	}

	public void handleEvent(MediaPlayerEventType type) {
		if (handlers.containsKey(type)) {
			ArrayList<Runnable> eventHandlers = handlers.get(type);
			Iterator<Runnable> it = eventHandlers.iterator();
			while (it.hasNext()) {
				it.next().run();
			}
		}
	}

	protected static void init() {
		if (inited)
			return;
		
		inited = true;

		if (vlcLibPath == "") {
			if (PApplet.platform == MACOSX) {
				vlcLibPath = "/Applications/VLC.app/Contents/MacOS/lib";
			} else if (PApplet.platform == WINDOWS) {
				vlcLibPath = "C:\\Program Files\\VideoLAN\\VLC";
			} else if (PApplet.platform == LINUX) {
				vlcLibPath = "/home/linux/vlc/install/lib";
			}
		}
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(),
				vlcLibPath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
	}

	public VLCJVideo(PApplet parent, String... options) {
		super(0, 0, PApplet.RGB);
		width = 0;
		height = 0;
		VLCJVideo.init();
		
		tasks = new Stack<Runnable>();
		handlers = new HashMap<MediaPlayerEventType, ArrayList<Runnable>>();
		
		initVLC(parent, options);
	}

	protected void initVLC(PApplet parent, String... options) {
		this.parent = parent;
		firstFrame = true;
		factory = new MediaPlayerFactory(options);
		headlessMediaPlayer = factory.newHeadlessMediaPlayer();
		bindHeadlessMediaPlayerEvents( headlessMediaPlayer );
	}
	
	protected void scheduleTask( Runnable task ) {
		this.tasks.push( task );
		if ( isReady() ) {
			runTasks();
		}
	}
	
	protected void runTasks() {
		while ( !tasks.empty() ) {
			tasks.pop().run();
		}
	}

	protected void bindHeadlessMediaPlayerEvents( HeadlessMediaPlayer hmp ) {
		
		hmp.addMediaPlayerEventListener( new MediaPlayerEventAdapter() {
			
			public void mediaChanged( MediaPlayer mp, libvlc_media_t media, String mrl ) {
				System.out.println( mrl );
				setReady( false );
			}
			
			public void error(MediaPlayer mediaPlayer) {
				handleEvent(MediaPlayerEventType.ERROR);
		    }
			
			public void videoOutput( MediaPlayer mp, int newCount ) {
				
				List<TrackInfo> info = mp.getTrackInfo();
				Iterator<TrackInfo> it = info.iterator();
				
				boolean dim_parsed = false;
				
				System.out.println( info.toString() );
				
				while ( it.hasNext() ) {
					TrackInfo ti = it.next();
					if ( ti instanceof VideoTrackInfo ) {
						width = ((VideoTrackInfo) ti).width();
						height = ((VideoTrackInfo) ti).height();
						if ( width == 0 ) width = parent.width;
						if ( height == 0 ) height = parent.height;
						System.out.println( ti.toString() );
						System.out.println( String.format( "video dim: %dx%d", width, height ) );
						dim_parsed = true;
						break;
					}
				}
				if ( !dim_parsed ) {
					System.out.println(
						String.format( "Unable to parse media data: %s", filename )
					);
				} else {
					mp.stop();
					setReady( true );
					initNewMediaPlayer();
				}
			}
			
			public void mediaStateChanged( MediaPlayer mediaPlayer, int newState ) {
				System.out.println( String.format( "New media state: %d", newState ) );
			}
		} );
		
	}
	
	protected void initNewMediaPlayer() {
		if ( mediaPlayer != null )
			releaseMediaPlayer( mediaPlayer );
		mediaPlayer = factory.newDirectMediaPlayer(width, height, this);
		copyPixels = new int[width * height];
		firstFrame = true;
		bindMediaPlayerEvents( mediaPlayer );
		mediaPlayer.prepareMedia(filename);
		mediaPlayer.setRepeat( repeat );
		setVolume( volume );
		runTasks();
	}
	
	protected void bindMediaPlayerEvents( MediaPlayer mp1 ) {
		mp1.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

			public void opening(MediaPlayer mp) {
				handleEvent(MediaPlayerEventType.OPENING);
			}

			public void error(MediaPlayer mediaPlayer) {
				handleEvent(MediaPlayerEventType.ERROR);
			}

			public void finished(MediaPlayer mediaPlayer) {
				handleEvent(MediaPlayerEventType.FINISHED);
			}

			public void paused(MediaPlayer mediaPlayer) {
				handleEvent(MediaPlayerEventType.PAUSED);
			}

			public void stopped(MediaPlayer mediaPlayer) {
				handleEvent(MediaPlayerEventType.STOPPED);
			}

			public void playing(MediaPlayer mediaPlayer) {
				handleEvent(MediaPlayerEventType.PLAYING);
			}

			public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
				handleEvent(MediaPlayerEventType.MEDIA_STATE_CHANGED);
			}

		});
	}

	public void openMedia(String mrl) {
		try {
			filename = parent.dataPath(mrl);
			File f = new File(filename);
			if (!f.exists()) {
				filename = mrl;
			}
		} finally {
			headlessMediaPlayer.prepareMedia(filename);
			headlessMediaPlayer.parseMedia();
			headlessMediaPlayer.start();
		}
	}

	public void play() {
		scheduleTask( new Runnable() { public void run() {
			if ( isReady() )
				mediaPlayer.play();
		} } );
	}

	public void stop() {
		scheduleTask( new Runnable() { public void run() {
			if ( isReady() )
				mediaPlayer.stop();
		} } );
	}

	public void pause() {
		scheduleTask( new Runnable() { public void run() {
			if ( isReady() )
				mediaPlayer.pause();
		} } );
	}

	public float time() {
		return isReady() ? (float) ((float) mediaPlayer.getTime() / 1000.0) : 0.0f;
	}

	public float duration() {
		return isReady() ? (float) ((float) mediaPlayer.getLength() / 1000.0) : 0.0f;
	}

	public void jump(final float pos) {
		scheduleTask( new Runnable() { public void run() {
			if ( isReady() )
				mediaPlayer.setTime(Math.round(pos * 1000));
		} } );
	}

	public boolean isReady() {
		return mediaPlayer != null && ready;
	}
	
	protected void setReady( Boolean ready ) {
		this.ready = ready;
	}
	
	public boolean isPlaying() {
		return isReady() && mediaPlayer.isPlaying();
	}
	
	public boolean isPlayable() {
		return isReady() && isReady() && mediaPlayer.isPlayable();
	}
	
	public boolean isSeekable() {
		return isReady()  && isReady() && mediaPlayer.isSeekable();
	}
	
	public boolean canPause() {
		return isReady() && mediaPlayer.canPause();
	}
	
	public void loop() {
		repeat = true;
		if ( isReady() )
			mediaPlayer.setRepeat(true);
	}

	public void noLoop() {
		repeat = false;
		if ( isReady() )
			mediaPlayer.setRepeat(false);
	}
	
	public void mute() {
		setVolume( 0.0f );
	}
	
	public void setVolume(float volume) {
		if (volume < 0.0) {
			volume = (float) 0.0;
		} else if (volume > 1.0) {
			volume = (float) 1.0;
		}
		this.volume = volume;
		if ( isReady() )
			mediaPlayer.setVolume(parent.round((float) (200.0) * volume));
	}

	public synchronized void display(Memory memory) {
		if ( !isReady() )
			return;
		
		memory.read(0, copyPixels, 0, width * height);
		if (firstFrame) {
			super.init(width, height, parent.ARGB);
			firstFrame = false;
		}
		pixels = copyPixels;
		updatePixels();
	}

	public void dispose() {
		if (isReady()) {
			releaseMediaPlayer( mediaPlayer );
		}
		if (isReady()) {
			releaseMediaPlayer( headlessMediaPlayer );
		}
		factory.release();
		copyPixels = null;
	}

	protected void releaseMediaPlayer(MediaPlayer mp) {
		if (mp.isPlaying()) {
			mp.stop();
		}
		mp.release();
	}

	protected void finalize() throws Throwable {
		try {
			dispose();
		} finally {
			super.finalize();
		}
	}

}
