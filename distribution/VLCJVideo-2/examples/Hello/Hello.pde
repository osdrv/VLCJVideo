import ru.devdep.processing.vlcj.*;

VLCJVideo video;

protected final static int WIDTH = 1920;
protected final static int HEIGHT = 1080;

void setup() {
  size( WIDTH, HEIGHT );
  video = new VLCJVideo( this, "--no-video-title-show" );
  video.openMedia( "http://example.com/movie.mp4" );
  video.loop();
  video.play();
}

void draw() {
  background( 0 );
  image( video, 0, 0 );
}
