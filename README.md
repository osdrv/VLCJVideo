## Intro

VLCJ binding for Processing.

Simple wrapper with unified processing video interface.

Built considering GSVideo live streaming video troubles.

[Github project page](https://github.com/4pcbr/VLCJVideo)

[VLCJ project home](http://caprica.github.com/vlcj/)

[VLCJ github page](https://github.com/caprica/vlcj)

## Example

```java
import ru.devdep.processing.vlcj.*;

VLCJVideo video;

protected final static int WIDTH = 1920;
protected final static int HEIGHT = 1080;

void setup() {
  size( WIDTH, HEIGHT );
  video = new VLCJVideo( this, "--no-video-title-show" );
  video.openMedia( "http://fotobank.home/5sec_horiz-Desktop.mp4" );
  video.loop();
  video.play();
}

void draw() {
  background( 0 );
  image( video, 0, 0 );
}
```
