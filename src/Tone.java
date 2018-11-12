import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Tone {

	private List<BellNote> bellNotes;
    private List<Note> notes;
    private List<BellRinger> bellRingers;
    private Map<Note,BellRinger> map;
	private int turn;
	private boolean songEnded;
	
	public static void main(String[] args) throws InterruptedException {
		//User can make playlist
		System.out.println("Make Playlist\nAvailable songs visible to left\nInclude full song name and extenstion, separated by commas: ");
		Scanner input = new Scanner(System.in);
		String songs = input.nextLine();
		
		String[] songNames = songs.split(",\\s+");
		for(String name : songNames) {
			try {
				new Tone(name);
				System.out.println("Delay between songs");
				Thread.sleep(2000);
			} catch(FileNotFoundException e) {
				System.err.println(e.getMessage());
			}
		}
		input.close();
    }

    Tone(String filename) throws FileNotFoundException {
    	map = new HashMap<Note, BellRinger>();
    	bellNotes = new ArrayList<BellNote>();
    	notes = new ArrayList<Note>();
    	songEnded = false;
    	turn = 0;
    	
    	ArrayList<String> songToPlay = readFile(filename);
    	if(validateSongInput(songToPlay)) {
    		playSong();
    	}
    	
    }
    
    private void playSong() {
    	AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
		try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
			line.open();
			line.start();
			
			getRingers(line);
			startRinging();
			while (!songEnded) { 
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			removeRingers();
			line.drain();
		}
		catch (LineUnavailableException e) {
			System.err.println("Unable to play song. Check audio");
		}
    }

    private ArrayList<String> readFile(String filename) throws FileNotFoundException {
    	System.out.println("Reading file..."); // indicating file read
    	ArrayList<String> input = new ArrayList<String>();
    	
    	File song = new File(filename);
    	if(!song.exists()) {
    		throw new FileNotFoundException("File" + filename + "not found. Please try again");
    	}
    	try (FileReader fileReader = new FileReader(song);
    		BufferedReader in = new BufferedReader(fileReader)) {
    		String nextLine = in.readLine();
    		System.out.println("Playng " + nextLine);// display Song name
    		nextLine = in.readLine();
    		while(nextLine != null) {
    			input.add(nextLine);
    			nextLine = in.readLine();
    		}
    	} catch (IOException e) {
    		System.err.println("Error attempting to read file. Validate filename");
    	}
    	
    	return input;
    }

    private boolean validateSongInput(ArrayList<String> input) {
    	System.out.println("Validating notes..."); // indicating validation
    	boolean valid = true;
    	
    	if(input.size() == 0) {
    		System.err.println("File is empty. Make sure correct file was sent..");
    		return false;
    	}
    	for(String s : input) {
    		String[] line = s.split("\\s+");
    		if(line.length != 2) {
    			valid = false;
    			System.err.println(s + " is not a valid line. Check file");
    		}
    		else {
    			Note tempNote = null;
    			NoteLength tempLen = null;
    			
    			String note = line[0];
    			try {
    				tempNote = Note.valueOf(note);
    				notes.add(tempNote);
    			}catch (IllegalArgumentException e){
    				valid = false;
    				System.err.println(note + "is not a valid note. Check file");
    			}
    			
    			String length = line[1];
    			tempLen = numberToNote(length);
    			if(tempLen == null) {
    				System.err.println(length + " is not a valid note length. Check file.");
    				valid = false;
    			}
    			
    			if(valid) {
    				bellNotes.add(new BellNote(tempNote,tempLen));
    			}
    		}
    	}
    	if(!valid) {
    		System.err.println("Invalid input. Terminating...");
    	}
    	return valid;
    }
    
    private void getRingers(SourceDataLine line) {
    	System.out.println("Getting BellRingers (aka making Threads)");
    	ArrayList<BellRinger> ringers = new ArrayList<BellRinger>();
    	
    	for(int i = 0; i < bellNotes.size(); i++) {
    		int turn = i;
			BellRinger br = map.get(notes.get(i));
			if (br==null) {
				br = new BellRinger(this,line);
				ringers.add(br);
				map.put(notes.get(i), br);
			}
			br.addNoteToPlay(bellNotes.get(i),turn);
    	}
    	bellRingers = ringers;
    }
    
    private void removeRingers() {
    	System.out.println("Joining Threads...");
    	for(BellRinger br : bellRingers) {
    		br.musicIsOver();
    	}
    }
    
    private void startRinging() {
    	for(BellRinger br : bellRingers) {
    		br.startRinging();
    	}
    }
    
	public synchronized void acquire(int x) {
		while (x!=turn) {
			try {
				wait();
			} catch (InterruptedException ignore) {}
		}
	}

	public synchronized void release() {
		turn++;
		if (turn==bellNotes.size()) {
			songEnded = true;
		}
		notifyAll();	
	}
	
    private static NoteLength numberToNote(String length) {
    	switch (length) {
    		case "1":
    			return NoteLength.WHOLE;
    		case "3":
    			return NoteLength.DOTTEDHALF;
    		case "2":
    			return NoteLength.HALF;
    		case "5":
    			return NoteLength.DOTTEDQUARTER;
    		case "4":
    			return NoteLength.QUARTER;
    		case "6":
    			return NoteLength.TRIPLET;
    		case "8":
    			return NoteLength.EIGHTH;
    		case "16":
    			return NoteLength.SIXTEENTH;
    	}
    	return null;
    }
	
}

class BellNote {
    final Note note;
    final NoteLength length;

    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }
}

enum NoteLength {
    WHOLE(1.0f),
    DOTTEDHALF(0.75f),
    HALF(0.5f),
    DOTTEDQUARTER(0.375f),
    QUARTER(0.25f),
    TRIPLET(0.1667f),
    EIGHTH(0.125f),
	SIXTEENTH(0.0625f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }
}

enum Note {
    // REST Must be the first 'Note'
    REST,
    A3,
    A3S,
    B3,
    B3S,
    C3,
    C3S,
    D3,
    D3S,
    E3,
    E3S,
    F3,
    F3S,
    G3,
    G3S,
    A4,
    A4S,
    B4,
    C4,
    C4S,
    D4,
    D4S,
    E4,
    F4,
    F4S,
    G4,
    G4S,
    A5,
    A5S,
    B5,
    C5,
    C5S,
    D5,
    D5S,
    E5,
    F5,
    F5S,
    G5,
    G5S,
    A6;

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 2;

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0 * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0;
    private final double MAX_VOLUME = 127.0;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            // Calculate the frequency!
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0, exp);

            // Create sinusoidal data sample for the desired frequency
            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }
    }

    public byte[] sample() {
        return sinSample;
    }
}