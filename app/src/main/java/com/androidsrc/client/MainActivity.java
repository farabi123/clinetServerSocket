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
import android.widget.Button;
import android.widget.TextView;

import java.util.Collections;
import java.util.LinkedList;


public class MainActivity extends Activity  implements SensorEventListener {

	TextView response, msg;
	String mainResponse="";
	Server server;
	DisplayLists displayLists;

	//variables for compass
	private SensorManager mSensorManager;
	private Sensor mCompass, mAccelerometer;

	//variables for logging
	float[] mGrav;
	float[] mAcc;
	float[] mGeo;
	String TAG1 = "MASTER";

	//variables for logging
	private Sensor mGyroscope;
	private Sensor mGravityS;
	float[] mGyro;

	//variables for display
	TextView list1 ;
	TextView list2 ;
	TextView list3 ;

	//Master variables
	boolean autoMode = false,
			isFieldScanComplete = false,
			hasSentMessage = false;

	public double[][] gpsList = new double[25][2],
			   searchingList = new double[25][2],
			   confirmedList = new double[25][2];

	// creates minions for master app to communicate w/ & command
	Robot doc, mr, mrs, carlito, carlos, carly, carla, carleton;

	// defines a variable used by master app to switch communication among minions
	Robot robot, assign_mission;

	Robot[] mRobot;			// list of robot minions
	int robotCounter = 0;	// used to switch between robots in mRobots
	Robot[] libot; 			// robot minions with LIDARs
	LinkedList<Robot> free_robots = new LinkedList<>();		// used to track free robot minions

	// defines ip addresses for each robot minion's phone
	String test = "169.234." + "65.236";
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
	double gpsError = 0.000008993;

	// grid corners
	double[] topLeft = {111,222},
			topRight = {111,222},
			bottomLeft = {111,222},
			bottomRight = {111,222};


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

		//add functionality to autoMode button
		Button buttonAuto = (Button) findViewById(R.id.btnAuto);
		buttonAuto.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!autoMode) {
					v.setBackgroundResource(R.drawable.button_auto_on);
					autoMode = true;
				} else {
					v.setBackgroundResource(R.drawable.button_auto_off);
					autoMode = false;
				}
			}
		});

		// ADDED FROM SERVER
		msg = (TextView) findViewById(R.id.msg);
		server = new Server(this,"HI");
		response = (TextView) findViewById(R.id.responseTextView);

		// create robots master controls
		doc = new Robot(ip_doc,false, gps_coords, response);
		mr = new Robot(ip_mr,false, gps_coords, response);
		mrs = new Robot(ip_mrs,false, gps_coords, response);
		carlito = new Robot(ip_carlito,true, gps_coords, response);
		carlos = new Robot(ip_carlos,true, gps_coords, response);
		carly = new Robot(ip_carly,true, gps_coords, response);
		carla = new Robot(ip_carla,true, gps_coords, response);
		carleton = new Robot(ip_carleton,false, gps_coords, response);

		// execute robot client threads to run
		for (Robot robot : mRobot) {
			robot.mClient.execute();
		}

		libot = new Robot[] {carlito, carlos, carly, carla};
		mRobot = new Robot[] {doc, mr, mrs, carlito, carlos, carly, carla, carleton};

		//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		// add free robot to link list to be assigned search locations later
		Collections.addAll(free_robots, mRobot);
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

		/*      MAIN CODE STARTS HERE    */

		if (autoMode && !mainResponse.isEmpty()) {

			// cycle through minions & read messages
			robot =  mRobot[robotCounter % mRobot.length];
			mainResponse =robot.mClient.response;
			receive_from_m(mainResponse);

			//Log.i("app.main.client","mainresponse: "+mainResponse+"\n");

			if (!isFieldScanComplete) { // search heat has started; scan field for mannequins

				if (!hasSentMessage) { // send messages once
					// send 4 robots w/ LIDARs to grid corners
					send_to_m(libot[0], true, true, topLeft);
					send_to_m(libot[1], true, true, topRight);
					send_to_m(libot[2], true, true, bottomLeft);
					send_to_m(libot[3], true, true, bottomRight);
					hasSentMessage = true;
				}

				if (isAtDestination(libot[0].getRobotLocation(), libot[0].getDestination()) &&
					isAtDestination(libot[1].getRobotLocation(), libot[1].getDestination()) &&
					isAtDestination(libot[2].getRobotLocation(), libot[2].getDestination()) &&
					isAtDestination(libot[3].getRobotLocation(), libot[3].getDestination()))
				{ // wait until robots are at grid corners

					if (libot[0].getFieldScanStatus() && libot[1].getFieldScanStatus() &&
							libot[2].getFieldScanStatus() && libot[3].getFieldScanStatus())
					{ // end field scan phase once all robots have finished scans
						isFieldScanComplete = true;
					}
				}
			} else { // scanned field; confirm mannequin locations now
				// assign free robots to search recorded locations near them
				while (!free_robots.isEmpty()) {
					assign_mission = free_robots.pop();
					setClosestObjectDistance(assign_mission, gpsList);
					send_to_m(assign_mission,true, false, robot.getDestination());
				}
			}

			updateDisplay();
			robotCounter++;

		/*
		if(!mainResponse.isEmpty()) {


			receive_from_m(mainResponse);

			if (isFieldScanComplete) {
				while (!free_robots.isEmpty()) {
					assign_mission = free_robots.pop();
					setClosestObjectDistance(assign_mission, gpsList);
				}


				double[] loc = {13.555,117.85};
				robot.setRobotLocation(loc);
				robot.setMannStatus(true);
			}
			updateDisplay();

			//isFieldScanComplete = true;
			send_to_m(robot,true,true, newPos);
			objectLocCounter++;

		}
		*/
		}
		/*   END    */



	}
	/************************************MASTER CONTROLLER FUNCTIONS*******************************/
	//receives info in form of string & parses to variables' appropriate types
	void receive_from_m (String data) {
		//Log.i("app.main.client",""+data+"\n");
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

	// updates gps, searching & confirmed lists arrays & on phone display
	void updateDisplay () {
		double[] robotLoc = robot.getRobotLocation();
		double currLAT, currLON;

		if (!isFieldScanComplete) { // robots not have completed scan of field
			if (robot.isHasLidar()) {
				// add object location to gps list
				gpsList = addToList(gpsList, robot.getObjectLocation());
			}
		} else { // robots have completed scan of field yet

			// move locations from searching to confirmed list once robot has confirmed
			if (robot.getMannStatus()) {
				// match GPS coords that were found w/ coords on the list
				for (int i=0; i<searchingList.length; i++) {

					currLAT = searchingList[i][0];
					currLON = searchingList[i][1];
					if(!(currLAT==0 &&currLON==0)&& !(currLAT==-1 &&currLON==-1)){

						if ((currLAT >= (robotLoc[0] - gpsError) && (currLAT <= robotLoc[0] + gpsError)) &&
								(currLON>= (robotLoc[1] - gpsError) && (currLON <= (robotLoc[1] + gpsError))))
						{
							// add GPS coords to confirmed list
							confirmedList[i][0] = searchingList[i][0];
							confirmedList[i][1] = searchingList[i][1];

							// erase GPS coords from unconfirmed list
							searchingList[i][0] = -1;
							searchingList[i][1] = -1;
							break;  //question?????????
						}
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
				server.msgReply123 = toMinion;
				//Log.i("HERE",server.replyToClient);
			} else {
				Log.i(TAG1, "Invalid robot name. Robot not listed as enabled with LIDAR. " +
						"Only robots with LIDAR can move right now.");
			}
		} else {
			server.msgReply123 = toMinion;
		}
	}

	// assigns robot to closest gps location on list
	void setClosestObjectDistance(Robot robot, double[][] list) {
		double[] minLoc = new double[2];
		//double tooFar = 100000.0;
		double min = 100000.0; //should be large, not small. Assuming the position data is positive
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
		String first_num = str.substring(find_colon+1, find_comma);

		//cut out lat
		str = str.substring(find_comma + 1, str.length());

		//find lon
		find_colon = str.indexOf(':');
		find_comma = str.indexOf(']');
		String sec_num = str.substring(find_colon+1, find_comma);


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
				list[mCounter] = newEntry;
			}
		}
		return list;

	}

	private boolean isAtDestination (double[] robotLocation, double[] destination) {
		boolean hasArrived = false;
		double currLAT = robotLocation[0],
				currLON = robotLocation[1],
				destLat = destination[0],
				destLon = destination[1];

		if ((destLat >= (currLAT - gpsError) && (destLat <= currLAT + gpsError)) &&
				(destLon >= (currLON - gpsError) && (destLon <= (currLON + gpsError))))
		{
			hasArrived = true;
		}
		return hasArrived;
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