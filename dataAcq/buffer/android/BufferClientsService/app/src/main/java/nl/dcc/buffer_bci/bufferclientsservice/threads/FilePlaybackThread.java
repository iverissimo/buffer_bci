package nl.dcc.buffer_bci.bufferclientsservice.threads;

import android.os.Environment;
import android.util.Log;

import nl.dcc.buffer_bci.bufferclientsservice.base.Argument;
import nl.dcc.buffer_bci.bufferclientsservice.base.ThreadBase;
import nl.dcc.buffer_bci.FilePlayback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class FilePlaybackThread extends ThreadBase {

    public static final String TAG = FilePlaybackThread.class.toString();

    private int VERB = 1; // global verbosity level
    private String hostname;
    private int port;
    private double speedup;
    private int blockSize;
    private String dataDir;
    private InputStream dataReader;
    private InputStream eventReader;
    private InputStream headerReader;
    FilePlayback filePlayback=null;

    @Override
    public Argument[] getArguments() {
        final Argument[] arguments = new Argument[4];

        arguments[0] = new Argument("Buffer Address", "localhost:1972");
        arguments[1] = new Argument("Speedup", 1.0, false);
        arguments[2] = new Argument("Buffer size", 5, false);
        arguments[3] = new Argument("Data directory", "res/raw/");
        return arguments;
    }

    @Override
    public String getName() {
        return "File Playback";
    }


    @Override
    public void validateArguments(Argument[] arguments) {
        final String address = arguments[0].getString();

        try {
            final String[] split = address.split(":");
            arguments[0].validate();
            try {
                Integer.parseInt(split[1]);
            } catch (final NumberFormatException e) {
                arguments[0].invalidate("Wrong hostname format.");
            }

        } catch (final ArrayIndexOutOfBoundsException e) {
            arguments[0].invalidate("Integer expected after colon.");
        }
    }

    private void initialize() {
        hostname = arguments[0].getString();
        int sep = hostname.indexOf(':');
        if ( sep>0 ) {
            port=Integer.parseInt(hostname.substring(sep+1,hostname.length()));
            hostname=hostname.substring(0,sep);
        }
        speedup = arguments[1].getDouble();
        blockSize = arguments[2].getInteger();
        dataDir = arguments[3].getString();
        androidHandle.updateStatus("Address: " + hostname + ":" + String.valueOf(port));
    }

    @Override
    public void mainloop() {
        initialize();
        initFiles();
        filePlayback = new FilePlayback(hostname,port,dataReader,eventReader,headerReader,speedup,blockSize);
        filePlayback.mainloop();
        try {
            cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        filePlayback=null;
    }

    @Override
    public void stop() { filePlayback.stop(); }

    void initFiles() {
        String samples_str = dataDir + "/samples";
        String events_str = dataDir + "/events";
        String header_str = dataDir + "/header";
		  try {
				if ( isExternalStorageReadable() ){ // if available read from external storage
				  dataReader  =androidHandle.openReadFile(samples_str);
				  eventReader =androidHandle.openReadFile(events_str);
			  	  headerReader=androidHandle.openReadFile(header_str);
				}
              if ( dataReader == null ){ // fall back on the resources directory
                  Log.w("FilePlayback","External storage is not readable.");
					 dataReader = this.getClass().getClassLoader().getResourceAsStream(samples_str);
					 eventReader = this.getClass().getClassLoader().getResourceAsStream(events_str);
					 headerReader = this.getClass().getClassLoader().getResourceAsStream(header_str);
				}
		  } catch ( FileNotFoundException e ) {
				e.printStackTrace();
		  } catch ( IOException e ) {
				e.printStackTrace();
		  }
		  if ( dataReader==null ) {
                Log.w("FilePlayback", "Huh, couldnt open file stream : " + samples_str);
		  }
	 }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    void cleanup() throws IOException {
        if (headerReader != null) {
            headerReader.close();
            headerReader = null;
        }
        if (eventReader != null) {
            eventReader.close();
            eventReader = null;
        }
        if (dataReader != null) {
            dataReader.close();
            dataReader = null;
        }
        run = false;
    }


}