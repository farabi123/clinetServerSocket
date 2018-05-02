package com.androidsrc.client;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;

import static java.util.Arrays.asList;

public class MainActivity extends Activity  implements SensorEventListener {

	TextView response;
	String mainResponse="";
	Server server;
	DisplayLists displayLists;
	TextView infoip, msg;
	// IP ADDRESS OF BLACK PHONE/MINION
	boolean pressed = false;
	//variables for compass
	private SensorManager mSensorManager;
	private Sensor mCompass, mAccelerometer;
	float[] mGeomagnetic;

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
	boolean isFieldScanComplete = false,
			isMannequinFound = false;

	double[] destinationLoc = new double[2];

	public double[][] gpsList = new double[25][2],
			   searchingList = new double[25][2],
			   confirmedList = new double[25][2];


	String gpsListString = "",
			searhcingListString = "",
			confirmedListString = "";

	// defines ip addresses for each robot's (/minion's) phone
	String test = "169.234." + "77.136";
	String ip_doc = test,
			ip_mr = test,
			ip_mrs = test,
			ip_carlito = test,
			ip_carlos = test,
			ip_carly = test,
			ip_carla = test,
			ip_carleton = test;

	// initializes the starting position of robots (/minions)
	double[] gps_coords = {12,13};    //initialize minion gps

	// creates minions for master app to communicate w/ & command
	Robot doc, mr, mrs, carlito, carlos, carly, carla, carleton;

	// defines a variable used by master app to switch communication among minions
	Robot robot;

	LinkedList<Robot> free_robots=new LinkedList<>();

	TextView list1 ;
	TextView list2 ;
	TextView list3 ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		list1 = (TextView) findViewById(R.id.gps_string_tv);
		list2 = (TextView) findViewById(R.id.gps_searching_string_tv);
		list3 = (TextView) findViewById(R.id.gps_confirm_string_tv);

		// print object lists on separate thread (removes app lag)
		displayLists = new DisplayLists(MainActivity.this,
				list1,list2,list3,
				gpsList,searchingList,confirmedList);

		// ADDED FROM SERVER
		msg = (TextView) findViewById(R.id.msg);
		server = new Server(this,"");
	//	buttonConnect = (Button) findViewById(R.id.connectButton);
		response = (TextView) findViewById(R.id.responseTextView);
	/*	buttonConnect.setOnClickListener(new OnClickListener() {


	/*		@Override
			public void onClick(View arg0) {
				pressed=true;
			}
		});*/

			//myClient = new Client(phoneIpAddress, 8080, response);
			//myClient.execute();

		doc = new Robot(ip_doc,true, gps_coords);
		mr = new Robot(ip_mr,true, gps_coords);
		mrs = new Robot(ip_mrs,true, gps_coords);
		carlito = new Robot(ip_carlito,true, gps_coords);
		carlos = new Robot(ip_carlos,true, gps_coords);
		carly = new Robot(ip_carly,true, gps_coords);
		carla = new Robot(ip_carla,true, gps_coords);
		carleton = new Robot(ip_carleton,true, gps_coords);

		// execute threads to run
		displayLists.execute();
//		doc.mClient.execute();
//		mr.mClient.execute();
//		mrs.mClient.execute();
//		carlito.mClient.execute();
		carlos.mClient.execute();
//		carly.mClient.execute();
//		carla.mClient.execute();
//		carleton.mClient.execute();


//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		// add free robot to link list to be assigned search locations later
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

		/***** MAIN CODE *****/
		//mainResponse = myClient.response;
		mainResponse = carlos.mClient.response;
		//Log.i("app.main.client","mainresponse: "+mainResponse+"\n");


		if(!mainResponse.isEmpty()) {
			receive_from_m(mainResponse);
			Robot assign_mission;

			// FOR TESTING ONLY
			double[] newPos = {33,33};
			//gpsList = addToList(gpsList,newPos);
			//robot.setRobotLocation(searchingList[0]);
			//robot.setMannStatus(true);
			// END

			if (isFieldScanComplete) {
				while (!free_robots.isEmpty()) {
					assign_mission = free_robots.pop();
					setClosestObjectDistance(assign_mission, gpsList);
				}

				double[] loc = {13,117};
				robot.setRobotLocation(loc);
			}
			updateDisplay();

			if (isFieldScanComplete) {
				robot.setMannStatus(true);
			}

			isFieldScanComplete = true;
			//send_to_m(robot,true,true, newPos);

		}
		/***** END *****/



	}
	/******************************************************************MASTER CONTROLLER FUNCTIONS****************************************/
	private class Robot {
		double[] location;
		boolean isMannequinFound = false;
		boolean isSearching = false;
		boolean hasLidar;
		double[] destination = new double[2];
		double[] lgps = new double[2];
		Client mClient;
		//Server mServer;

		Robot(String phoneIp, boolean hasLidar, double[] location) {
			this.mClient = new Client(phoneIp,8080, response);
			//this.mServer = new Server(MainActivity.this);
			this.hasLidar = hasLidar;
			this.location = location;
		}

		double[] getRobotLocation() {
			return location;
		}

		void setRobotLocation (double[] location) {
			this.location = location;
		}

		void setDestination (double[] destination) {
			this.destination = destination;
			this.isSearching = true;
		}

		void setMannStatus(boolean state) {
			this.isMannequinFound = state;

			if (isMannequinFound) {
				this.isSearching = false;
			}
		}

		boolean isHasLidar() { return this.hasLidar; }

		boolean getMannStatus() {
			return this.isMannequinFound;
		}

		boolean getSearchingStatus() {
			return this.isSearching;
		}

		void setObjectLocation (double[] lidarGPS) {
			this.lgps = lidarGPS;
		}

		double[] getObjectLocation () { return lgps; }
	}

	//receives info in form of string & parses to variables' appropriate types
	void receive_from_m (String data) {
		//Log.i("app.main.client","data: "+data+"\n");
		String string_name = data.substring(data.indexOf("NAME"),data.indexOf("GPS")),
				string_gps = data.substring(data.indexOf("GPS"),data.indexOf("MANN")),
				string_mann = data.substring(data.indexOf("MANN"),data.indexOf("LGPS")),
				string_lidarGPS = data.substring(data.indexOf("LGPS"),data.length());

		//get name of minion that sent string
		string_name = string_name.substring(string_name.indexOf(":")+2,string_name.indexOf(","));

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

		// Set robot location from message string
		robot.setRobotLocation(getCoords(string_gps));

		// Set mannequin found status from message string
		robot.setMannStatus(string_mann.contains("true"));

        //add LIDAR gps calculation of victim to list of gps coordinates
        if (robot.isHasLidar()) {
            robot.setObjectLocation(getCoords(string_lidarGPS));
        }
	}

	// updates phone display
	void updateDisplay () {
		double[] robotLoc = robot.getRobotLocation();

		if (!isFieldScanComplete) { // robots not have completed scan of field
			if (robot.isHasLidar()) {
				// add object location to gps list

				gpsList = addToList(gpsList, robot.getObjectLocation());
			}
		} else { // robots have completed scan of field yet

			// move locations from searching to confirmed list once robot has confirmed
			if (robot.getMannStatus()) {
				Log.i("HERE","NOW2");
				// match GPS coords that were found w/ coords on the list
				for (int i=0; i<searchingList.length; i++) {
					if (searchingList[i][0] != -1 && searchingList[i][1] != -1 &&
							searchingList[i][0] >= robotLoc[0]-0.000008993 &&
							searchingList[i][0] <= robotLoc[0]+0.000008993 &&
							searchingList[i][1] >= robotLoc[1]-0.000008993 &&
							searchingList[i][1] <= robotLoc[1]+0.000008993)
					{
						// add GPS coords to confirmed list
						confirmedList[i][0] = searchingList[i][0];
						confirmedList[i][1] = searchingList[i][1];

						// erase GPS coords from unconfirmed list///////////////////////////////////////////////////////////////////
						searchingList[i][0] = -1;
						searchingList[i][1] = -1;

						Log.i("HERE","NOW");
					}
				}
			}
		}

		displayLists = new DisplayLists(MainActivity.this,
				list1,list2,list3,
				gpsList,searchingList,confirmedList);
		displayLists.execute();
	}

	// sends instructions to minion
	void send_to_m (Robot robot, boolean mode, boolean scanMode, double[] dest) {
		String toMinion = "AUTOMODE: " + mode + ", " +
				"SCANMODE: " + scanMode + ", " +
				"DEST[LAT:" + dest[0] + ", LON:" + dest[1] + "]";

		if (scanMode) {
			if (robot.hasLidar) {
				server = new Server(this,toMinion);
				//Log.i("HIYA",server.msgReply);
			} else {
				Log.i(TAG1, "Invalid robot name. Robot not listed as enabled with LIDAR. Only robots with LIDAR can move right now.");
			}
		} else {
			//robot.mServer.msgReply = toMinion;
		}
	}

	// assigns robot to closest gps location on list
	void setClosestObjectDistance(Robot robot, double[][] list) {
		double[] minLoc = new double[2];
		//double tooFar = 100000.0;
		double min = 100000.0; //should be large, not small. I am assuming the position data is positive
		int minIndex = 0;
		double[] never_reach_location = {-1,-1};
		double objDis;

		// Calculate distances of objects from robot's current location
		for (int i=0; i<25; i++) {
			objDis = distanceFormula(robot.getRobotLocation(),list[i]);

			if(list[i][0]!=-1 && list[i][1]!=-1 && objDis < min )
			{//the second condition limit the min to be impossible location
				min = distanceFormula(robot.getRobotLocation(),list[i]);
				minLoc = list[i];
				minIndex = i;
			}
		}

		if (list[minIndex][0] != -1 && list[minIndex][1] != -1 && list[minIndex][0] != 0 && list[minIndex][1] != 0) {
			robot.setDestination(minLoc);
			searchingList[minIndex] = list[minIndex];
			gpsList[minIndex] = never_reach_location;
		}
	}

	double distanceFormula(double[] gps1, double[] gps2) {
		return Math.sqrt((gps2[0]-gps1[0])*(gps2[0]-gps1[0]) - (gps2[1]-gps1[1])*(gps2[1]-gps1[1]) );
	}

	// takes string coordinates of the form LAT:##,LON:##; returns double[]
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

	// add a new entry to a list of double[] if not already in list
	private double[][] addToList (double[][] list, double[] newEntry) {
		int mCounter = 0;
		boolean isCopy = false;

		for ( double[] entry : list
			 ) {
			if (entry[0] == newEntry[0] && entry[1] == newEntry[1]) {
				isCopy = true;
			}

		}

		if(!isCopy) {
			// find a spot on list that isn't already taken
			while (list[mCounter][0] != 0 && list[mCounter][1] != 0 && mCounter < list.length - 1) {
				mCounter++;
			}


			// return error message if list is full; otherwise, add to new entry to list
			if (mCounter == list.length) {
				Log.i(TAG1, "GPS List is full!");
			} else {

//			for (int counter = 0; counter < list.length-1; counter++) {
//				if (list[mCounter][0] == newEntry[0] && list[mCounter][1] == newEntry[1]) {
//					isDup = true;
//				}
//			}

				list[mCounter] = newEntry;
			}
		}
		return list;

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