// Phil Grunewald 
// log
// June 14 - commented out buttons, auto-start at startup
// July 14 - shorten file names, only store two columns, experiment with high sample rates
// 16 July 14 - major change: instead of summing 100 readings and do an RMS with the 441 readings per second, the RMS is taken of the 100 samples (over 1/441s) and the 441 readings per second are AVERAGED.
// May 15 - added ID edit field
// Added this file to GitHub repository
// 19 Jul 15 - removed the redundant 'draw text'
//
package com.Phil.DEMon;

import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

// for graph
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
// end graph

//Main Class
public class MainActivity extends Activity implements android.view.View.OnClickListener{

	private String versionDate="15_05_22";
	private FileWriter writer;
	private FileWriter metawriter;
	File file;
	File metafile;
	String filename;
	String imei;
	private File root;
	File folder;
	Date date1;  
	String formattedDateString;
	String mylogtext="";
	String myLocationText="";

	private double final_sumrms=0.0;	

	private double bufSumSqr = 0.0;	
	private	double bufAvg=0.0;

	short[] buffer = new short[100];
	
	private int running=0;
	
	private AudioRecord recorder;
	private int minbuffsize=0;
	private int Nread=0;
	
	private double offset = -11.3;// clamp type 2 see /Users/pg1008/Documents/Data/14_11_METER/14_11_12_calibration/14_11_12_calibration.xls;
	private double scaleFactor = 0.41;//;
	private SimpleDateFormat formatter_sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	private SimpleDateFormat formatter_timestamp = new SimpleDateFormat("HH:mm:ss");  
	private SimpleDateFormat formatter_date = new SimpleDateFormat("yy-MM-dd");  
	
	private DecimalFormat mydecimalformat = new DecimalFormat("0.0");
	private DecimalFormat myDoubleFormat = new DecimalFormat("0.00");
	       	 
	private TextView mylog;
	private Button myButton2;
	//private Button my_btn_setLocation;
	//private EditText myEditLocation;

	private Button my_btn_setID;
	private EditText myEditID;

	private List<String> fileList = new ArrayList<String>();

	private String ContactID;

// graph

    DrawView drawView;

public class DrawView extends View {
    Paint paint = new Paint();

    public DrawView(Context context) {
	super(context);
	paint.setColor(Color.BLACK);
    }

    @Override
    public void onDraw(Canvas canvas) {
	    canvas.drawLine(0, 0, 200, 200, paint);
	    canvas.drawLine(200, 0, 0, 200, paint);
    }

}

// end graph
@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);   
    mylog = (TextView) findViewById(R.id.Debugview);
    mylog.setMovementMethod(new ScrollingMovementMethod());
    mylog.setText(mylogtext);

	//  15_05_19_
	//
	ContactID = getString(R.string.contactID);
    // myEditLocation = (EditText) findViewById(R.id.edit_ID);
    myEditID = (EditText) findViewById(R.id.edit_ID);

    myButton2 = (Button) findViewById(R.id.button2);
    myButton2.setOnClickListener((OnClickListener) this);

    my_btn_setID = (Button) findViewById(R.id.btn_setID);
    my_btn_setID.setOnClickListener((OnClickListener) this);
    //my_btn_setLocation = (Button) findViewById(R.id.btn_setLocation);
    //my_btn_setLocation.setOnClickListener((OnClickListener) this);

	TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

	imei = tm.getDeviceId();
    root = Environment.getExternalStorageDirectory();
    folder = new File(root,"/METER"); // reinstated 3 Nov 14
    boolean success = true;
    if (!folder.exists()) {
	success = folder.mkdir();
    }
    //list files in this directory
    File[] files = folder.listFiles();
    fileList.clear();
    
    for(int i=0;i<files.length;i++) {
	Log.d("micro","hh"+i+files[i].getName());
    }

}

@Override
public void onClick(View v) {
	if (v==myButton2 && running==1){
		printToAndroid("Taking last sample...");
		Log.d("micro", "stop running");
		running=0;
	}
	if (v==my_btn_setID) {
		ContactID = myEditID.getText().toString();
	}
//// remove field and button
//		ViewGroup edit_layout = (ViewGroup) myEditLocation.getParent();
//		edit_layout.removeView(myEditLocation);
//		ViewGroup btn_layout = (ViewGroup) my_btn_setLocation.getParent();
//		btn_layout.removeView(my_btn_setLocation);
////EditText myEditText = (EditText) findViewById(R.id.myEditText);  
//InputMethodManager imm = (InputMethodManager)getSystemService(
//      Context.INPUT_METHOD_SERVICE);
//imm.hideSoftInputFromWindow(myEditLocation.getWindowToken(), 0);
//			try {
//				metawriter.write("Location: " + myLocationText + "\n");
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
}


@Override
public void onStart(){
    super.onStart();  
	Log.d("micro", "in onstart");
	Log.d("micro", "start running main()");
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
		   //  String ContactID = getString(R.string.contactID);
	       mylogtext=str1+"\n"+mylogtext; 
	       mylog.setText("Consumption for Contact ID " + ContactID + " at " + myLocationText + ": \n"+mylogtext);
	       if (mylogtext.length() > 5000) {
		 mylogtext=""; 
	       }
	    }
	});
}

private void main() {
	date1 = new Date();	
	formattedDateString = formatter_date.format(date1);	

	int FileIndex=1;
	filename=formattedDateString+"_"+imei+"_"+FileIndex+".csv";
	file = new File(folder, filename);
	metafile = new File(folder, filename);

	while (file.exists()) {
		FileIndex++;
		filename=formattedDateString+"_"+imei+"_"+FileIndex+".csv";
		file = new File(folder, filename);
	} 
	filename=formattedDateString+"_"+imei+"_"+FileIndex+".meta";
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
	Log.d("micro","set up");
	minbuffsize = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);

	long starttime=System.currentTimeMillis();
	Log.d("micro","time="+starttime);

	Date thisMoment = new Date();
	final String formatedDate = formatter_date.format(thisMoment);

// WRITE META FILE
	//  String ContactID = getString(R.string.contactID);
	String dataType = getString(R.string.dataType);
	try {
		metawriter.write("Software version: "+ versionDate +"\n");
		metawriter.write("Device ID: "+ imei +"\n");
		metawriter.write("Contact ID: "+ ContactID +"\n");
		metawriter.write("Data type: "+ dataType +"\n");
		metawriter.write("Date: "+formatedDate+"\n");
		metawriter.write("Offset: "+myDoubleFormat.format(offset)+"\n");
		metawriter.write("Scale factor: "+myDoubleFormat.format(scaleFactor)+"\n");
		metawriter.write("X label: Time [hours]\n");
		metawriter.write("Y label: " + dataType + " [Watt]\n");
		metawriter.write("Title: " + myLocationText +"\n");
	    } catch (IOException e) {
	     // TODO Auto-generated catch block
	     e.printStackTrace();
	    }
    //loop until stop button callback	 
    while(running==1) { 
	Log.d("micro","about to open recorder stream at time="+(System.currentTimeMillis()-starttime)+" ms");
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
	Log.d("micro","recorder stream opened at time="+(System.currentTimeMillis()-starttime)+" ms");


	for (int loop=0;loop<1000;loop++){ 
	    if (running==1) { 
		bufSumSqr = 0.0;	
		bufAvg=0.0;
    	//for (int z2=0;z2<5;z2++){
		  //loop 441 times reading 100 from buffer each time - this loop takes 1 second	
		  for (int z1=0;z1<441;z1++) { 
			try {
				Nread = recorder.read(buffer,0,buffer.length);
			    } catch (NullPointerException e) {
			       for (int i=0; i<100; i++) {
				buffer[i]=-1;
				}	
			    }

			//bufsum is the total of 100 audio samples - ie 1/441 of a second
			double bufsum=0.0;
			for (int i=0; i<Nread; i++) {
				bufsum += buffer[i];
			}   
			bufAvg	  = bufsum / Nread;
			bufSumSqr += bufAvg*bufAvg;
		  }	     
	//}	
		//this needs to be square rooted
		final_sumrms=Math.sqrt(bufSumSqr/(441.0));//rms is the mean rms over 441 samples in a second * nseconds interval
		final_sumrms=(final_sumrms+offset)*scaleFactor; // correction factor

		Date date1 = new Date();  
		final String formattedDateString_sd = formatter_sd.format(date1);
		final String formattedDateString_timestamp = formatter_timestamp.format(date1);

		printToAndroid(formattedDateString_timestamp+": "+mydecimalformat.format(final_sumrms)+" W"); 
		
		try {
		     writer.write(formattedDateString_timestamp+","+mydecimalformat.format(final_sumrms)+"\n");
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

	
	Log.d("micro","about to close recorder stream");
	recorder.stop();
	recorder.release();
	recorder = null;
 	} // if initialised

      //end of while running
      }

      printToAndroid("D-Mon stopped.");
}
}
