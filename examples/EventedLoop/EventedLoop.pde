// Video Loop playback based on event handling. Just for example,
// one should use loop() instead

import ru.devdep.processing.vlcj.*;

import uk.co.caprica.vlcj.player.events.MediaPlayerEventType;

VLCJVideo video;

protected final static int WIDTH = 640;
protected final static int HEIGHT = 360;

void setup() {
  size( WIDTH, HEIGHT );
  video = new VLCJVideo( this, "--no-video-title-show" );
  bindVideoEvents();
  video.openMedia( "video.mp4" );
  // look: no loop() call here
  video.play();
}

void bindVideoEvents() {
  video.bind( MediaPlayerEventType.FINISHED, new Runnable() { public void run() {
    // we've catched finished event, let's play it one more time!
    println( "finished" );
    println( "play one more time!" );
    // Sure we could rewind video back and play one more time
    // But this way you could open different video to play next
    video.openMedia( "video.mp4" );
    video.play();
  } } );
}

void draw() {
  background( 0 );
  image( video, 0, 0, displayWidth, displayHeight );
}
