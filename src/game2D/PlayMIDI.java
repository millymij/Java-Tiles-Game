package game2D;

import java.io.*;
import javax.sound.midi.*;
public class PlayMIDI {

    /**
     * Play a MIDI file
     * @param filename - the name of the MIDI file to play
     * @throws Exception
     */
    public void play(String filename) throws Exception {
        Sequence score = MidiSystem.getSequence(new File(filename));
        Sequencer seq = MidiSystem.getSequencer();
        seq.open();
        seq.setSequence(score);
        seq.start();
        // while (seq.isRunning()) { Thread.sleep(100); }
        //seq.close();
    }
}

