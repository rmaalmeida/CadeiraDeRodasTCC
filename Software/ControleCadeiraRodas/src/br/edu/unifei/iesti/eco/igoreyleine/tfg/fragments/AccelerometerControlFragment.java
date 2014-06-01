package br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments;

import br.edu.unifei.iesti.eco.igoreyleine.tfg.R;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.activities.MainActivity;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.control.BluetoothManager;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.interfaces.OnConnectionStatusChange;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AccelerometerControlFragment extends Fragment implements
		OnConnectionStatusChange, SensorEventListener {
	
	/* 
	 * Limiting angle between the state STOP and the other states.
	 * In order for a command to be recognized, the user has to 
	 * tilt the device in an angle (in degrees) greater than 
	 * this value, in any direction.
	 */
	private static final int REST_ANGLE_THRESHOLD = 10;
	
	/* 
	 * Minimum time interval between the pooling of two 
	 * accelerometer values.
	 * That ensures that the app won't be sending information 
	 * to the hardware in a frequency greater than the arduino 
	 * can handle, what can cause unexpected behavior.
	 */
	private static final long ACCELEROMETER_POOLING_INTERVAL = 200;

	private RelativeLayout notConnectedLayout;
	private RelativeLayout mainLayout;
	
	private TextView txtCommand;
	
	private boolean initialized = false;
	
	/* 
	 * Variable that will hold a reference to the sensors manager
	 * and the accelerometer itself, in order for the 
	 * system to register data listeners and retrieve accelererometer
	 * data. 
	 */
	private SensorManager sensorManager; 
	private Sensor accelerometer;
	
	/* 
	 * Boolean variables used to control the fragment visibility.
	 * This way we can register and unregister the accelerometer
	 * data listener taking in account changes in the bluetooth 
	 * connection state.
	 */
	private boolean isVisible = false;
	
	private long lastCommandTick = 0;
	
	public void setVisibility(boolean isNowVisible){
		isVisible = isNowVisible;
		
		if( BluetoothManager.isConnected() && isVisible )
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	/*
	 * Method called by the Android OS when this fragment is first created,
	 * before its exhibition. Here we prepare the fragment content.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		//Here we inflate the fragment's layout
		View rootView = inflater.inflate(R.layout.accelerometer_control_fragment_layout, container, false);
		
		/*
		 * We load a reference to the two possible layouts we can use
		 * in this fragment to our class. This way, we can take care
		 * of the current layout in runtime, depending on the 
		 * bluetooth connection state.
		 */
		notConnectedLayout = (RelativeLayout)rootView.findViewById(R.id.notConnectedLayout);
		mainLayout = (RelativeLayout)rootView.findViewById(R.id.mainLayout);
		
		/*
		 * Verification of whether the app is connected to the hardware 
		 * via the bluetooth connection.
		 * If it is not, we show a TextView, with a error message. 
		 * Otherwise, we show the expected layout.
		 */
		
		onConnectionStatusChanged(BluetoothManager.isConnected());
		
		txtCommand = (TextView)rootView.findViewById(R.id.command);
		
		/* We obtain a reference of the sensors manager and of the accelerometer */
		sensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			
		initialized = true;
		
		//We return the layout view, properly initialized
		return rootView;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		/* 
		 * Whenever the fragment becomes visible and the bluetooth connection is 
		 * active, we register this class as a listener in order to receive 
		 * data from teh accelerometer in a default frequency.
		 */
		if( BluetoothManager.isConnected() && isVisible )
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	public void onPause(){
		super.onPause();
		
		/* 
		 * Whenever the fragment becomes invisbile, we unregister 
		 * our clas as a listener to the accelerometer data.
		 */
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onConnectionStatusChanged(boolean isConnectedNow) {
		if(!initialized) return;
		
		if(!isConnectedNow){
			notConnectedLayout.setVisibility(View.VISIBLE);
			mainLayout.setVisibility(View.GONE);
			if(sensorManager != null)
				sensorManager.unregisterListener(this);
		}else{
			notConnectedLayout.setVisibility(View.GONE);
			mainLayout.setVisibility(View.VISIBLE);
			
			if(isVisible && sensorManager != null)
				sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		long clockMillis = SystemClock.elapsedRealtime();
		
		if(clockMillis - lastCommandTick < ACCELEROMETER_POOLING_INTERVAL)
			return;
		
		lastCommandTick = clockMillis;
		
		int commandStringId = R.string.commandStop, commandId = MainActivity.STOP_COMMAND_ID;
		double mostSignificantAngle = 0;
		/* 
		 * x < 0: Tilted forward
		 * x > 0: Tilted backwards
		 * 
		 * y > 0: Tilted to the right
		 * y < 0: Tilted to the left
		 * 
		 * PS: Using the device with the screen pointing up and with its 
		 * superior side pointing to the left of the user.
		 */
		double xAngle = 90 - Math.toDegrees(Math.acos(event.values[0]/9.81d));
		double yAngle = 90 - Math.toDegrees(Math.acos(event.values[1]/9.81d));
		
		if(Math.abs(xAngle) < REST_ANGLE_THRESHOLD && Math.abs(yAngle) < REST_ANGLE_THRESHOLD){
			commandStringId = R.string.commandStop;
			commandId = MainActivity.STOP_COMMAND_ID;
		}
		
		else if(yAngle > REST_ANGLE_THRESHOLD){
			commandStringId = R.string.commandRight;
			commandId = MainActivity.RIGHT_COMMAND_ID;
			mostSignificantAngle = yAngle - REST_ANGLE_THRESHOLD;
		}
		
		else if(yAngle < -REST_ANGLE_THRESHOLD){
			commandStringId = R.string.commandLeft;
			commandId = MainActivity.LEFT_COMMAND_ID;
			mostSignificantAngle = -yAngle - REST_ANGLE_THRESHOLD;
		}
		
		else if(xAngle < -REST_ANGLE_THRESHOLD){
			commandStringId = R.string.commandForward;
			commandId = MainActivity.FORWARD_COMMAND_ID;
			mostSignificantAngle = -xAngle  -REST_ANGLE_THRESHOLD;
		}
		
		else if(xAngle > REST_ANGLE_THRESHOLD){
			commandStringId = R.string.commandBackwards;
			commandId = MainActivity.BACKWARDS_COMMAND_ID;
			mostSignificantAngle = xAngle - REST_ANGLE_THRESHOLD;
		}	
				
		
		if(mostSignificantAngle >= 90) mostSignificantAngle = 90;
		
		txtCommand.setText(commandStringId);
		BluetoothManager.sendCommand(commandId, mostSignificantAngle);

	}

	/*
	 * Method triggered when the registered sensor's accuracy 
	 * changes.
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}
