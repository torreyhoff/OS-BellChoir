import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class BellRinger extends Thread{
	private List<BellNote> bellNotes;
	private List<Integer> turnToPlay;
	private final SourceDataLine line;
	private Thread t;
	private Tone mutex;
	
	public BellRinger(Tone c, SourceDataLine audioLine) {
		line = audioLine;
		t = new Thread(this);
		mutex = c;
		bellNotes = new ArrayList<BellNote>();
		turnToPlay = new ArrayList<Integer>();
	}
	
	public void addNoteToPlay(BellNote b, int t) {
		bellNotes.add(b);
		turnToPlay.add(t);
	}

	public void startRinging() {
		t.start();
	}

	public void run() {
		for (int i = 0; i<bellNotes.size();i++) { 
			mutex.acquire(turnToPlay.get(i));
			
			try {
				play(bellNotes.get(i));
			} catch (LineUnavailableException ignore) {ignore.printStackTrace();}
			
			mutex.release();
		}
	}

	void play(BellNote note) throws LineUnavailableException {
        playNote(line, note);
    }

    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
		System.out.println(bn.note+" "+bn.length); // display note name and length
    }

    public void musicIsOver() {
    	try {
			t.join();
		} catch (InterruptedException e) {e.printStackTrace();}
    }

}
