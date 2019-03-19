// Phil Grunewald 
// A simple tool for recording a AC signal on the microphone jack and writing it to file
// see earlier commits for more elaborate versions with onClick events
//
package com.Phil.DEMon;

import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;              // to get IMEI number

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;

import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.provider.MediaStore;
import android.net.Uri;
// import android.content.ContentProvider;
import android.support.v4.content.FileProvider;
// import android.hardware.camera;


//Main Class
public class MainActivity extends Activity{
	private String versionDate="16_04_26";
	private FileWriter writer;
	private FileWriter metawriter;
	private File root;
	private File file;
	private File metafile;
	private String filename;
	private String imei;
	private File folder;
	private Date recordingDate;  
	private String formattedDateString;
	private String mylogtext="";
	private double final_sumrms=0.0;	
	private double bufSumSqr = 0.0;	
	private	double bufAvg=0.0;
	short[] buffer = new short[100];
	private int running=0;
	private AudioRecord recorder;
	private int minbuffsize=0;
	private int Nread=0;
	private double scaleFactor = 1;
	private double offset = 0;			// /Data/14_11_METER/14_11_12_calibration/14_11_12_calibration.xls;

	private SimpleDateFormat formatter_datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	private SimpleDateFormat formatter_date = new SimpleDateFormat("yy-MM-dd");  
	private DecimalFormat mydecimalformat = new DecimalFormat("0.0");
	private DecimalFormat myDoubleFormat = new DecimalFormat("0.00");
	private TextView mylog;
	private String metaID;			// read from txt file

    private String mCurrentPhotoPath;

@Override
public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);   
	mylog = (TextView) findViewById(R.id.Debugview);
	mylog.setMovementMethod(new ScrollingMovementMethod());
	mylog.setText(mylogtext);
	
// get imei number of this phone
	TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
	imei = tm.getDeviceId();

// prepare METER folder
	root = Environment.getExternalStorageDirectory();
	folder = new File(root,"/METER"); // reinstated 3 Nov 14
	// folder /sdcard/METER is assumed to exist - it gets created by meter.py on phone setup
//	if (!folder.exists()) {
//		folder.mkdir();
//	}
//Get the ID from text file
	File idFile = new File(root,"/METER/id.txt");
	try {
		BufferedReader br = new BufferedReader(new FileReader(idFile));
		metaID = br.readLine();
		br.close();
	}
	catch (IOException e) {
		metaID = "0";
	}
// create .csv and .meta files
	int FileIndex=1;
	filename=metaID+"_"+FileIndex+".csv";
	file = new File(folder, filename);
	metafile = new File(folder, filename);
	while (file.exists()) {
		FileIndex++;
		filename= metaID+"_"+FileIndex+".csv";
		file = new File(folder, filename);
	} 
	filename=metaID+"_"+FileIndex+".meta";
	metafile = new File(folder, filename);
	try{  
		try{
			file.createNewFile();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		writer = new FileWriter(file, true);
	}
	catch (IOException e) 
	{
		e.printStackTrace();
	}
	try{  
		try{
			metafile.createNewFile();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		metawriter = new FileWriter(metafile, true);
	}
	catch (IOException e) 
	{
		e.printStackTrace();
	}
	// WRITE META FILE
	Date thisMoment = new Date();
	final String formatedDate = formatter_date.format(thisMoment);
	final String dataType = getString(R.string.dataType);
	try {
		metawriter.write("Software version: "+ versionDate +"\n");
		metawriter.write("Device ID: "+ imei +"\n");
		metawriter.write("Meta ID: "+ metaID +"\n");
		metawriter.write("Data type: "+ dataType +"\n");
		metawriter.write("Date: "+formatedDate+"\n");
		metawriter.write("Offset: "+myDoubleFormat.format(offset)+"\n");
		metawriter.write("Scale factor: "+myDoubleFormat.format(scaleFactor)+"\n");
		metawriter.write("X label: Time [hours]\n");
		metawriter.write("Y label: " + dataType + " [Watt]\n");
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

@Override
public void onStart(){
	super.onStart();  
	if (running!=1) {	// 14_07_26_ got the impression new instances are created when waking up the phone	
		running=1;
		new Thread(new Runnable() { 
			public void run(){	    
				main();
			}
		}).start();
	}
}

private void printToAndroid(final String str1){	
	runOnUiThread(new Runnable() {
		public void run() {
			mylogtext=str1+"\n"+mylogtext; 
			mylog.setText("Version: " + versionDate + ": \nID: " + metaID + ": \n"+mylogtext);
			if (mylogtext.length() > 5000) {
				mylogtext=""; 
			}
		}
	});
}

//-------------------- 
// start
// vis insertion 23 Aug 2017
//-------------------- 

private void dispatchTakePictureIntent() {
    // Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	File imgFile = new File(root,"/METER/x.jpg"); // reinstated 3 Nov 14

// File tempFile = File.createTempFile("my_app", ".jpg");
// fileName = imgFile.getAbsolutePath();
    Uri uri = Uri.fromFile(imgFile);
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    startActivityForResult(intent, PICTURE_REQUEST_CODE);



//     // Ensure that there's a camera activity to handle the intent
//     if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//         // Create the File where the photo should go
//         File photoFile = null;
//         try {
//             photoFile = createImageFile();
//         } catch (IOException ex) {
//             // Error occurred while creating the File
//         }
//         // Continue only if the File was successfully created
//         if (photoFile != null) {
//             Uri photoURI = FileProvider.getUriForFile(this,
//                                                   "com.example.android.fileprovider",
//                                                   photoFile);
//             takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//             startActivityForResult(takePictureIntent, 1);
//         }
//     }
}



private File createImageFile() throws IOException {
	folder = new File(root,"/METER"); // reinstated 3 Nov 14
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    // String imageFileName = "JPEG_" + timeStamp + "_";
    //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(
        timeStamp,  /* prefix */
        ".jpg",         /* suffix */
        folder      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    mCurrentPhotoPath = image.getAbsolutePath();
    return image;
}



//-------------------- 
// end
// vis insertion 23 Aug 2017
//-------------------- 
private void main() {
	Date recordingDate = new Date();  
	formattedDateString = formatter_date.format(recordingDate);	
	minbuffsize = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
	//loop until stop button callback	 
	while(running==1) { 
		try {
			recorder = new AudioRecord(AudioSource.MIC,44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10*minbuffsize); // 14_07_26_ removed 10*minbuffsize
		} catch (Throwable t) {
			Log.e("AudioRecord","Recording Failed");
			recorder.stop();
			recorder.release();
			recorder = null;
		}
		if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
			recorder.startRecording();

            dispatchTakePictureIntent();

			for (int loop=0;loop<1000;loop++){ 
				if (running==1) { 
					bufSumSqr = 0.0;	
					bufAvg=0.0;
					//loop 441 times reading 100 from buffer each time - this loop takes 1 second	
					for (int z1=0;z1<441;z1++) { 
						try {
							Nread = recorder.read(buffer,0,buffer.length);
						} catch (NullPointerException e) {
							for (int i=0; i<100; i++) {
								buffer[i]=-1;
							}	
						}
						double bufsum=0.0;							 //bufsum is the total of 100 audio samples - ie 1/441 of a second
						for (int i=0; i<Nread; i++) {
							bufsum += buffer[i];
						}   
						bufAvg	  = bufsum / Nread;
						bufSumSqr += bufAvg*bufAvg;
					}     
					//this needs to be square rooted
					final_sumrms=Math.sqrt(bufSumSqr/(441.0));		//rms is the mean rms over 441 samples in a second * nseconds interval
					final_sumrms=(final_sumrms+offset)*scaleFactor; // correction factor
					// record values
					Date recordingTime = new Date();  
					final String formattedDateTime = formatter_datetime.format(recordingTime);
					printToAndroid(formattedDateTime+": "+mydecimalformat.format(final_sumrms)+" W"); 
					try {
						writer.write(formattedDateTime+","+mydecimalformat.format(final_sumrms)+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} //end of if running
				try {
					writer.flush();
					metawriter.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}  // every 1000 seconds the recorder is reset via recorder.stop
			recorder.stop();
			recorder.release();
			recorder = null;
		} // if initialised
	}	//end of while running
	printToAndroid("D-Mon stopped.");
	} // main()
} // class Main Activity
