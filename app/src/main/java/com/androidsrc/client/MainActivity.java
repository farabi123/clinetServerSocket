package com.androidsrc.client;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.LinkedList;

import static com.androidsrc.client.MainActivity.Robots.CARLA;
import static com.androidsrc.client.MainActivity.Robots.CARLETON;
import static com.androidsrc.client.MainActivity.Robots.CARLITO;
import static com.androidsrc.client.MainActivity.Robots.CARLOS;
import static com.androidsrc.client.MainActivity.Robots.CARLY;
import static com.androidsrc.client.MainActivity.Robots.DOC;
import static com.androidsrc.client.MainActivity.Robots.MR;
import static com.androidsrc.client.MainActivity.Robots.MRS;

public class MainActivity extends Activity  implements SensorEventListener {

	Client myClient;
	TextView response;
	String mainResponse;
	Button buttonConnect;
	Server server;
	TextView infoip, msg;
	// IP ADDRESS OF BLACK PHONE/MINION
	String phoneIpAddress = "169.234." +"94.219";
	boolean pressed = false;
	//variables for compass
	private SensorManager mSensorManager;
	private Sensor mCompass, mAccelerometer;
	float[] mGeomagnetic;

	//grid variables
	public boolean autoMode = false;

	//variables for logging
	float[] mGrav;
	float[] mAcc;
	float[] mGeo;
	String TAG1 = "MASTER";
	//variables for logging
	private Sensor mGyroscope;
	private Sensor mGravityS;
	float[] mGravity;
	float[] mGyro;

	//Master variables
	boolean initialFieldScan = true,
			isMannequinFound = false;

	double[] destinationLoc = new double[2];

	double[][] gpsList = new double[25][2];
	double[][] searchingList = new double[25][2];
	//ArrayList<double[]>searchingList=new ArrayList<double[]>(25);
	double[][] confirmedList = new double[25][2];


	String gpsListString = "",
			searhcingListString = "",
			confirmedListString = "";

	enum Robots {
		DOC, MR, MRS, CARLITO, CARLOS, CARLY, CARLA, CARLETON,
	}
	Robots minion; //initialize minion

	double[] gps_coords;    //initialize minion gps

	Robot doc = new Robot(DOC, gps_coords, true);
	Robot mr = new Robot(MR, gps_coords, true);
	Robot mrs = new Robot(MRS, gps_coords, true);
	Robot carlito = new Robot(CARLITO, gps_coords, true);
	Robot carlos = new Robot(CARLOS, gps_coords, false);
	Robot carly = new Robot(CARLY, gps_coords, false);
	Robot carla = new Robot(CARLA, gps_coords, false);
	Robot carleton = new Robot(CARLETON, gps_coords, false);

	Robot robot;

	LinkedList<Robot> free_robots=new LinkedList<>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ADDED FROM SERVER

		msg = (TextView) findViewById(R.id.msg);
		server = new Server(this);
	//	buttonConnect = (Button) findViewById(R.id.connectButton);
		response = (TextView) findViewById(R.id.responseTextView);
	/*	buttonConnect.setOnClickListener(new OnClickListener() {


	/*		@Override
			public void onClick(View arg0) {
				pressed=true;
			}
		});*/

			myClient = new Client(phoneIpAddress, 8080, response);
			myClient.execute();

//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		free_robots.add(doc);
		free_robots.add(mr);
		free_robots.add(mrs);
		free_robots.add(carlito);
		free_robots.add(carlos);
		free_robots.add(carly);
		free_robots.add(carla);
		free_robots.add(carleton);

	}


	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	//Called whenever the value of a sensor changes
	@Override
	public final void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_GRAVITY){
			mGrav = event.values;}
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
			mGyro = event.values;
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mAcc = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeo = event.values;

		mainResponse = myClient.getResponse();

		Log.i("app.main.client","mainresponse"+mainResponse+"\n");

		receive_from_m (mainResponse);      //place string from wifi network in function here
		Robot assign_mission;
		while(!free_robots.isEmpty()){
			assign_mission=free_robots.pop();
			assign_mission.isSearching=true;    //Ask Jeffrey  <-----
			//setClosestObjectDistance(assign_mission);

		}

		//updateDisplay();

	}
	/******************************************************************MASTER CONTROLLER FUNCTIONS****************************************/

	//receives info in form of string & parses to variables' appropriate types
	void receive_from_m (String data) {
		Log.i("app.main.client","data"+data+"\n");
		String string_name; /*= data.substring(data.indexOf("NAME"),data.indexOf("GPS")),
				string_gps = data.substring(data.indexOf("GPS"),data.indexOf("MANN")),
				string_mann = data.substring(data.indexOf("MANN"),data.indexOf("LGPS")),
				string_lidarGPS = data.substring(data.indexOf("LGPS"),data.length());*/

		//get name of minion that sent string
		string_name =""; /*string_name.substring(string_name.indexOf(":")+2,string_name.indexOf(","));*/

		// Set robot name from message string
		switch (string_name) {
			case "DOC":
				robot = doc;
				break;
			case "MR":
				robot = mr;
				break;
			case "MRS":
				robot = mrs;
				break;
			case "CARLITO":
				robot = carlito;
				break;
			case "CARLOS":
				robot = carlos;
				break;
			case "CARLY":
				robot = carly;
				break;
			case "CARLA":
				robot = carla;
				break;
			case "CARLETON":
				robot = carleton;
				break;
			default:
				Log.i("ERROR","Invalid robot name. Refer to Robot name list in code.");
		}


/*
		System.out.println("HELLO " + string_name);
		System.out.println("HELLO " + string_gps);
		System.out.println("HELLO " + string_mann);
		System.out.println("HELLO " + string_lidarGPS);

		// Set robot location from message string
		robot.setLocation(getCoords(string_gps));

		// Set mannequin found status from message string
		robot.setStatus(string_mann.contains("true"));

		// Set LIDAR GPS reading
		robot.setObjectLocation(getCoords(string_lidarGPS));

        /*
        //add LIDAR gps calculation of victim to list of gps coordinates
        if (hasLIDAR) {
            if (gpsListCounter < gpsList[0].length) {
                gpsList[gpsListCounter] = getCoords(string_lidarGPS);
                gpsListCounter++;
            } else {
                Log.i(TAG1,"gpsList is full!");
            }
        }
        */
	}
	double[] getCoords (String str) {
		int find_comma, find_colon;

		//find lat
		find_colon = str.indexOf(':');
		find_comma = str.indexOf(',');
		String first_num = str.substring(find_colon+1, find_comma-1);

		//cut out lat
		str = str.substring(find_comma + 1, str.length());

		//find lon
		find_colon = str.indexOf(':');
		find_comma = str.indexOf(']');
		String sec_num = str.substring(find_colon+1, find_comma-1);


		double[] coords={0,0};
		double d = Double.parseDouble(first_num);
		double d2 = Double.parseDouble(sec_num);
		coords[0] = d;
		coords[1] = d2;
		return coords;
	}
	private class Robot {
		Robots name;
		double[] location = new double[2];
		boolean isMannequinFound = false;
		boolean isSearching = false;
		boolean hasLidar = false;
		double[] destination = new double[2];
		double[] lgps = new double[2];

		Robot(Robots name, double[] location, boolean hasLidar) {
			this.name = name;
			this.location = location;
			this.hasLidar = hasLidar;
		}

		Robots getName () {
			return name;
		}

		double[] getRobotLocation() {
			return location;
		}

		void setLocation (double[] location) {
			this.location = location;
		}

		void setDestination (double[] destination) {
			this.destination = destination;
			this.isSearching = true;
		}

		void setStatus(boolean isMannequinFound) {
			this.isMannequinFound = isMannequinFound;

			if (isMannequinFound) {
				this.isSearching = false;
			}
		}

		boolean getStatus() {
			return this.isMannequinFound;
		}

		boolean getIsSearching() {
			return this.isSearching;
		}

		void setObjectLocation (double[] lidarGPS) {
			this.lgps = lidarGPS;
		}

		double[] getObjectLocation () { return lgps; }
	}

	//Called whenever activity resumes from pause
	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGravityS, SensorManager.SENSOR_DELAY_NORMAL);

	}

	//Called when activity pauses
	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);

	}


	//Called when activity restarts. onCreate() will then be called
	@Override
	public void onRestart() {
		super.onRestart();
		Log.i("activity cycle","main activity restarting");
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		server.onDestroy();
	}

}