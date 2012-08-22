import ru.devdep.processing.vlcj.*;

import uk.co.caprica.vlcj.player.events.MediaPlayerEventType;

VLCJVideo video;

protected final static int WIDTH = 1920;
protected final static int HEIGHT = 1080;

void setup() {
  size( WIDTH, HEIGHT );
  video = new VLCJVideo( this, "--no-video-title-show" );
  video.openMedia( "http://fotobank.home/5sec_horiz-Desktop.mp4" );
  video.loop();
  video.play();
  bindVideoEvents();
}

void bindVideoEvents() {
  video.bind( MediaPlayerEventType.FINISHED, new Runnable() { public void run() {
    println( "finished" );
  } } );
  video.bind( MediaPlayerEventType.OPENING, new Runnable() { public void run() {
    println( "opened" );
  } } );
  video.bind( MediaPlayerEventType.ERROR, new Runnable() { public void run() {
    println( "error" );
  } } );
  video.bind( MediaPlayerEventType.PAUSED, new Runnable() { public void run() {
    println( "paused" );
  } } );
  video.bind( MediaPlayerEventType.STOPPED, new Runnable() { public void run() {
    println( "stopped" );
  } } );
  video.bind( MediaPlayerEventType.PLAYING, new Runnable() { public void run() {
    println( "playing" );
  } } );
  video.bind( MediaPlayerEventType.MEDIA_STATE_CHANGED, new Runnable() { public void run() {
    println( "state changed" );
  } } );
}

void draw() {
  background( 0 );
  image( video, 0, 0 );
}
