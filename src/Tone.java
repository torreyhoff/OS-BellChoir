import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Tone {

    
    public static void main(String[] args) throws Exception {
    	ArrayList songToPlay = readFile("MaryHadALittleLamb.txt");
    	if(validateSongInput(songToPlay)) {
            final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
            Tone t = new Tone(af);
            t.playSong(song);
    	}
    }
    
    private static List<BellNote> bellNotes;
    private static List<Note> notes;
	
    // Mary had a little lamb
	private static final List<BellNote> song = Stream.of(
            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),
            new BellNote(Note.F4, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),

            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.HALF),

            new BellNote(Note.G4, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.HALF),

            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.HALF),

            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),
            new BellNote(Note.F4, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),

            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.QUARTER),

            new BellNote(Note.G4, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),
            new BellNote(Note.A5, NoteLength.QUARTER),
            new BellNote(Note.G4, NoteLength.QUARTER),

            new BellNote(Note.F4, NoteLength.WHOLE)
        ).collect(Collectors.toList());

 
    private final AudioFormat af;

    Tone(AudioFormat af) {
        this.af = af;
    }

    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn: song) {
                playNote(line, bn);
                System.out.println(bn.note+" "+bn.length);
            }
            line.drain();
        }
    }

    /**
     * readFile() take in a filename, reads said file, and stores the contained information in an
     * 	ArrayList which it returns, for use elsewhere.
     * @param filename, of the file the user would like to read in
     * @return input, as an ArrayList containing the lines of the file
     * @throws FileNotFoundException
     */
    private static ArrayList readFile(String filename) throws FileNotFoundException {
    	ArrayList<String> input = new ArrayList<String>();
    	
    	File song = new File(filename);
    	if(!song.exists()) {
    		throw new FileNotFoundException("File" + filename + "not found. Please try again");
    	}
    	try (FileReader fileReader = new FileReader(song);
    		BufferedReader in = new BufferedReader(fileReader)) {
    		String nextLine = in.readLine();
    		while(nextLine != null) {
    			input.add(nextLine);
    			nextLine = in.readLine();
    		}
    	} catch (IOException e) {
    		System.err.println("Error attempting to read file. Validate filename");
    	}
    	
    	return input;
    }
    
    /**
     * validateSongInput checks to make sure the given song will play correctly.
     * @param input, the ArrayList containing the song to play.
     * @return valid, boolean saying whether the song is valid to play
     */
    private static boolean validateSongInput(ArrayList<String> input) {
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
    
    /**
     * Converts a given value to its corresponding note length
     * @param length, value given
     * @return NoteLength, corresponding note length or null
     */
    private static NoteLength numberToNote(String length) {
    	switch (length) {
    		case "1":
    			return NoteLength.WHOLE;
    		case "2":
    			return NoteLength.HALF;
    		case "4":
    			return NoteLength.QUARTER;
    		case "5":
    			return NoteLength.EIGHTH;
    	}
    	return null;
    }
    
    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int actualLength = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, actualLength);
        line.write(Note.REST.sample(), 0, 50);
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
    HALF(0.5f),
    QUARTER(0.25f),
    EIGHTH(0.125f);

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
    A5;

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 1;

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