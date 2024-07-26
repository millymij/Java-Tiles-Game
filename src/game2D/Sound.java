package game2D;
import java.io.*;
import javax.sound.sampled.*;
public class Sound extends Thread
{
	String filename;
	boolean finished;
	public Sound(String fname) {
		filename = fname;
		finished = false;
	}

	public void run()
	{
		try
		{
			File file = new File(filename);
			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
			AudioFormat format = stream.getFormat();

			ReverseFilter filter = new ReverseFilter(stream);
			AudioInputStream f = new AudioInputStream(filter, format, stream.getFrameLength());

			DataLine.Info info = new DataLine.Info(Clip.class, format);
			Clip clip = (Clip)AudioSystem.getLine(info);

			// comment out to have all sounds reversed
			//clip.open(f);

			// comment out to have all sounds normal
			clip.open(stream);

			clip.start();
			Thread.sleep(100);
			while (clip.isRunning()) { Thread.sleep(100); }
			clip.close();
		}
		catch (Exception e) { }
		finished = true;
	}
}