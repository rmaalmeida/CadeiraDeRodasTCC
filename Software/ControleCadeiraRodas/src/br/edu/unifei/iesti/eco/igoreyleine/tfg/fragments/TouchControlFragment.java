package br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments;

import br.edu.unifei.iesti.eco.igoreyleine.tfg.R;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.activities.MainActivity;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.control.BluetoothManager;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.interfaces.OnConnectionStatusChange;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;


/*
 * Fragment responsible for controlling the hardware via touch 
 * commands. This class implements the OnTouchListener interface 
 * in order to respomd to touch events over specific views.
 */
public class TouchControlFragment extends Fragment implements OnConnectionStatusChange, OnTouchListener{

	private RelativeLayout notConnectedLayout;
	private RelativeLayout mainLayout;
	/* 
	 * Method called by the Android OS when this fragment is first created, 
	 * before its exihbition.
	 * Here we prepare the fragment's content.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		
		/* We inflate the fragment's layout inside a View */
		View rootView = inflater.inflate(R.layout.touch_control_fragment_layout, container, false);
		
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
		
		/*
		 * We tell to the Android OS, that touch events performed 
		 * over those ImageView buttons must be handled by our 
		 * class
		 */
		ImageView button = (ImageView)rootView.findViewById(R.id.forwardButton);
		button.setOnTouchListener(this);
		
		button = (ImageView)rootView.findViewById(R.id.backwardsButton);
		button.setOnTouchListener(this);
		
		button = (ImageView)rootView.findViewById(R.id.rightButton);
		button.setOnTouchListener(this);
		
		button = (ImageView)rootView.findViewById(R.id.leftButton);
		button.setOnTouchListener(this);
		
		/* Finally, we return the layout view, properly initialized */
		return rootView;
	}

	@Override
	public void onConnectionStatusChanged(boolean isConnectedNow) {
		if(!isConnectedNow){
			notConnectedLayout.setVisibility(View.VISIBLE);
			mainLayout.setVisibility(View.GONE);
		}else{
			notConnectedLayout.setVisibility(View.GONE);
			mainLayout.setVisibility(View.VISIBLE);
		}
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {		
		/* 
		 * If a DOWN type touch event was performed over one of the buttons, the respective 
		 * command is sent.
		 */
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			/* We verify which button was touched by its ID. */
			switch(v.getId()){
			case R.id.forwardButton:
				BluetoothManager.sendCommand(MainActivity.FORWARD_COMMAND_ID);
				break;
			case R.id.backwardsButton:
				BluetoothManager.sendCommand(MainActivity.BACKWARDS_COMMAND_ID);
				break;
			case R.id.rightButton:
				BluetoothManager.sendCommand(MainActivity.RIGHT_COMMAND_ID);
				break;
			case R.id.leftButton:
				BluetoothManager.sendCommand(MainActivity.LEFT_COMMAND_ID);
				break;
			}
		}		
		/* If a button was released, we should stop the last command. */
		else if(event.getAction() == MotionEvent.ACTION_UP){
			BluetoothManager.sendCommand(MainActivity.STOP_COMMAND_ID);
		}
		
		return false;
	}
}
