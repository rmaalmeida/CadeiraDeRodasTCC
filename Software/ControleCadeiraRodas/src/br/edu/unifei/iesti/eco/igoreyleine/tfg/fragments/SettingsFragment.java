package br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments;

import br.edu.unifei.iesti.eco.igoreyleine.tfg.R;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.activities.MainActivity;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.adapters.ControlTypeFragmentAdapter;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.control.BluetoothManager;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.interfaces.OnConnectionStatusChange;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;

public class SettingsFragment extends Fragment implements OnClickListener, OnConnectionStatusChange{

	/* 
	 * Variable used to hold a reference of the spinner used to 
	 * show the peired bluetooth devices. 
	 */
	private Spinner spinner;
	
	/* 
	 * Variables used to hold a reference to the buttons used to connect and disconnect 
	 * the bluetooth and refresh the paired devices' list.
	 */
	private Button connectButton;
	private Button refreshButton;
	
	
	/* 
	 * Method called by the Android OS when this fragment is first created, 
	 * before its exihbition.
	 * Here we prepare the fragment's content.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){		
		/*
		 * Resources manager reference. We use it to obtain strings from the strings.xml file.
		 */
		final Resources r = getResources();
		/*
		 * We generate a array of strings with the control types, in order to show them 
		 * in our ListView
		 */
		String[] controls = new String[]{ r.getString(R.string.touchControl), r.getString(R.string.accelerometerControl) };
		
		/* We inflate the fragment's layout inside a View */
		View rootView = inflater.inflate(R.layout.settings_fragment_layout, container, false);
		
		/* 
		 * We obtain the ListView declared inside the layout file and 
		 * create an adapter to handle its content.
		 * This adapter is a simple string array adapter and uses 
		 * a predefined default android layout to show the contents 
		 * inside TextViews.
		 */
		final ListView lv = (ListView)rootView.findViewById(R.id.listView);
		lv.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, controls));
		
		/* We create a handler to manage touch events over the ListView's items */
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(lv.getAdapter().getItem(position).equals(r.getString(R.string.touchControl)))
					((MainActivity)getActivity()).setSelectedViewPage(ControlTypeFragmentAdapter.TOUCH_CONTROL_POSITION);			
				else
					((MainActivity)getActivity()).setSelectedViewPage(ControlTypeFragmentAdapter.ACCELEROMETER_CONTROL_POSITION);
				
			}
		});
		
		/* 
		 * We copy a reference of the fragment's spinner declared inside 
		 * the layout file to our class.
		 */
		spinner = (Spinner)rootView.findViewById(R.id.ddlBluetoothDevices);
		
		/*
		 * Now we make our class the callback class of the refresh and connect 
		 * buttons' click event.
		 */
		connectButton = (Button)rootView.findViewById(R.id.btnConnect);
		connectButton.setOnClickListener(this);
		
		refreshButton = (Button)rootView.findViewById(R.id.btnRefresh);
		refreshButton.setOnClickListener(this);
		
		CompoundButton voiceCommandsEnabled = (CompoundButton)rootView.findViewById(R.id.toggleVoiceControl);
		voiceCommandsEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				((MainActivity)getActivity()).setVoiceCommands(isChecked);				
			}
		});
		
		voiceCommandsEnabled.setChecked(((MainActivity)getActivity()).getVoiceCommands());
		
		/* Finally, we return the layout view, properly initialized */
		return rootView;
	}
	
	/* 
	 * Whenever the fragment is shown, we update the spinner 
	 * and the buttons state.
	 */
	@Override
	public void onResume(){
		super.onResume();
		updateSpinner();
		
		if(!BluetoothManager.isConnected()){
			spinner.setEnabled(true);
			refreshButton.setEnabled(true);
			connectButton.setText(R.string.connect);
		}else{
			spinner.setEnabled(false);
			connectButton.setText(R.string.disconnect);
			refreshButton.setEnabled(false);
		}
	}
	
	/*
	 * Method used to update the spinner content.
	 */
	public void updateSpinner(){
		spinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, BluetoothManager.getPairedDevicesName()));
	}

	/*
	 * Here we handle the click events of the connect and refresh buttons.
	 */
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		
		/* 
		 * If tge click event corresponds to a click over the Connect button,
		 * a connection with the selected paired device is atempted.
		 */
		case R.id.btnConnect:
			if(!BluetoothManager.isConnected()){
				BluetoothManager.connect((String)spinner.getSelectedItem());
				spinner.setEnabled(false);
				connectButton.setText(R.string.disconnect);
				refreshButton.setEnabled(false);
				
			}else{
				BluetoothManager.disconnect();
				spinner.setEnabled(true);
				refreshButton.setEnabled(true);
				connectButton.setText(R.string.connect);
			}
			break;
			
		/* 
		 * Otherwise, we update the spinner's content.
		 */
		case R.id.btnRefresh:
			updateSpinner();
			break;
		}
	}

	/*
	 * Implementation of our onConnectionStatus interface, 
	 * where we update the layout state based on the bluetooth 
	 * connection status.
	 */
	@Override
	public void onConnectionStatusChanged(boolean isConnectedNow) {
		if(!isConnectedNow){
			spinner.setEnabled(true);
			refreshButton.setEnabled(true);
			connectButton.setText(R.string.connect);
		}
		
	}

}
