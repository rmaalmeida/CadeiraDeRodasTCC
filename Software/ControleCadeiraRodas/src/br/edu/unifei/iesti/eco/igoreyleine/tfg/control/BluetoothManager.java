package br.edu.unifei.iesti.eco.igoreyleine.tfg.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.edu.unifei.iesti.eco.igoreyleine.tfg.R;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.activities.MainActivity;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.interfaces.OnConnectionStatusChange;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BluetoothManager {
	
	/* A variable that holds a reference to the device's bluetooth adapter	 */
	private static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	/* 
	 * A cosntant variable that holds the UUID of the type of connection 
	 * that will be performed with the remote bluetooth adapter.
	 * That UUID represents a serial mode connection. This UUID 
	 * ensures that the bluetooth connection will only happen with 
	 * a bluetooth device with serial connection emulation capabilities
	 */
	private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	/*
	 * Constant String field used to dispatch a broadcast Intent to those who are concerned 
	 * telling that the connection could not be done.
	 */
	public static final String ACTION_IMPOSSIBLE_CONNECT = "br.edu.unifei.iesti.eco.igoreyleine.tfg.IMPOSSIBLE_CONNECT";
	
	/* A variable that will hold a reference of the bluetooth connection socket */
	private static BluetoothSocket bluetoothSocket = null;
	
	/* A variable that will hold a reference of the connected bluetooth device */
	private static BluetoothDevice connectedDevice = null;
	
	/* A variable that will hold a reference to our Activity's context */
	private static Activity context;
	
	/* 
	 * List of objects that implements the OnConnectionStatusChange interface.
	 * (which in our case are all the fragments). We use that List to 
	 * implement the Observer pattern, in order to notify all the objects 
	 * a change in the bluetooth connection state.
	 */
	private static final List<OnConnectionStatusChange> connectionAwareFragments = new ArrayList<>(); 
	
	/* Method used to initialize the activity's context reference of our class.  */
	public static void init(Activity context){
		BluetoothManager.context = context;
	}
	
	/* Method to retrieve a list of paired bluetooth devices.
	 * Those values can be used to setup a bluetooth connection 
	 * with one of these devices.
	 */
	public static Set<BluetoothDevice> getPairedDevices(){
		return getBluetoothAdapter().getBondedDevices();
	}
	
	
	/* Method used to retrieve the names of the paired devices */
	public static String[] getPairedDevicesName(){
		Set<BluetoothDevice> pairedDevices = getPairedDevices();
		String[] names = new String[pairedDevices.size()];
		int count = 0;
		
		for(BluetoothDevice bd : pairedDevices)
			names[count++] = bd.getName();
		
		return names;
	}
	
	/* Method used to enable the bluetooth if it is not enabled. */
	public static void enableBluetooth(){
		if(!getBluetoothAdapter().isEnabled()){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    context.startActivityForResult(enableBtIntent, 1);
		}
	}
	
	/* 
	 * Method to create a bluetooth connection with the device with 
	 * the specified name.
	 */
	public static void connect(String deviceName){
		for(BluetoothDevice bd : getBluetoothAdapter().getBondedDevices()){
			if(bd.getName().equals(deviceName)){
				ConnectThread ct = new ConnectThread(bd);
				ct.run();
				break;
			}
		}
	}
	
	/* 
	 * Method used to close the actual connection.
	 */
	public static void disconnect(){
		try {
			getBluetoothSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* 
	 * Method used to register objects that want to be aware of the 
	 * bluetooth connection status
	 */
	public static void addConnectionAwareFragment(OnConnectionStatusChange fragment){
		if(fragment != null)
			connectionAwareFragments.add(fragment);
	}
	
	/* 
	 * Method used to send a stream of bytes that represent a 
	 * command, to the hardware, using the bluetooth connection.
	 * This method is synchronized in order to avoid race conditions.
	 */
	synchronized public static void sendCommand(int commandId){
		if(commandId < 0 || commandId >= MainActivity.COMMANDS.length)
			return;
		
		if(!isConnected())
			return;
		
		byte[] command;
		
		switch(commandId){
		case MainActivity.FORWARD_COMMAND_ID:
			command = "&WF".getBytes();
			break;
		case MainActivity.BACKWARDS_COMMAND_ID:
			command = "&WB".getBytes();
			break;
		case MainActivity.RIGHT_COMMAND_ID:
			command = "&WR".getBytes();
			break;
		case MainActivity.LEFT_COMMAND_ID:
			command = "&WL".getBytes();
			break;
		case MainActivity.STOP_COMMAND_ID:
			command = "&WS".getBytes();
			break;
		default:
			command = null;
			break;
		}
		try{
			getBluetoothSocket().getOutputStream().write(command);			
		}catch(IOException e){
			Toast.makeText(context, R.string.impossibleSendData, Toast.LENGTH_SHORT).show();
		}
		
	}
	
	/* 
	 * An overload of the method described above.
	 * The difference between then is the protocol used to 
	 * communicate with the hardware. The former is used 
	 * when we want to control the system acceleration at 
	 * hardware level while this one is used to 
	 * control the acceleration at software level. That's 
	 * why there's a second argument in here. 
	 * We use it to specify the angle of the accelerometer.
	 * 
	 * See the TCC report for more details.
	 */
	@SuppressLint("DefaultLocale")
	synchronized public static void sendCommand(int commandId, double angle){
		if(commandId < 0 || commandId >= MainActivity.COMMANDS.length)
			return;
		
		/* 
		 * Here we convert the angle of the accelerometer to a number
		 * between 0 and 195. That will be sent to the hardware as 
		 * part of the message.
		 */
		String param = String.format("%03d", (int)((90-angle)*195/80));
		
		if(!isConnected())
			return;
		
		
		byte[] command;
		
		switch(commandId){
		case MainActivity.FORWARD_COMMAND_ID:
			command = ("&RF"+param).getBytes();
			break;
		case MainActivity.BACKWARDS_COMMAND_ID:
			command = ("&RB"+param).getBytes();
			break;
		case MainActivity.RIGHT_COMMAND_ID:
			command = "&WR".getBytes();
			break;
		case MainActivity.LEFT_COMMAND_ID:
			command = "&WL".getBytes();
			break;
		case MainActivity.STOP_COMMAND_ID:
			command = "&WS".getBytes();
			break;
		default:
			command = null;
			break;
		}
		
		try{
			getBluetoothSocket().getOutputStream().write(command);
		}catch(IOException e){
			Toast.makeText(context, R.string.impossibleSendData, Toast.LENGTH_SHORT).show();
		}
		
	}
	
	/* 
	 * Method used to notify the objects that need to be aware of the Bluetooth 
	 * connection status of a status change.
	 */
	synchronized private static void notifyConnectionAwareFragments(boolean isConnected){
		for(OnConnectionStatusChange fragment : connectionAwareFragments)
			fragment.onConnectionStatusChanged(isConnected);
	}
	
	/* Getter for the bluetooth socket reference */
	synchronized private static BluetoothSocket getBluetoothSocket() {
		return bluetoothSocket;
	}

	/* Setter for the bluetooth socket reference */
	synchronized private static void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
		BluetoothManager.bluetoothSocket = bluetoothSocket;
	}

	/* Getter for the connected bluetooth device reference */
	@SuppressWarnings("unused")
	synchronized private static BluetoothDevice getConnectedDevice() {
		return connectedDevice;
	}

	/* Setter for the connected bluetooth device reference */
	synchronized private static void setConnectedDevice(BluetoothDevice connectedDevice) {
		BluetoothManager.connectedDevice = connectedDevice;
	}

	/* Getter for the bluetooth adapter reference */
	synchronized private static BluetoothAdapter getBluetoothAdapter() {
		return bluetoothAdapter;
	}
	
	/* Method to retrieve the bluetooth connection status */
	synchronized public static boolean isConnected(){
		return getBluetoothSocket() != null && getBluetoothSocket().isConnected();
	}
	
	/* 
	 * Thread used to make the bluetooth connection.
	 * That action is performed outside the UI Thread because 
	 * it may take a while and we do not wan't to hang the application 
	 * while it takes place. 
	 */
	private static class ConnectThread extends Thread{		 
	    public ConnectThread(BluetoothDevice device) {
	        setConnectedDevice(device);	        
	       // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            /* 
	             * We initialize the manager socket reference with a socket prepared 
	             * to handle a serial bluetooth connection.
	             */
	            setBluetoothSocket(device.createRfcommSocketToServiceRecord(SERIAL_UUID));
	        } catch (IOException e) { }
	    }
	 
	    public void run() {	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            getBluetoothSocket().connect();
	        } catch (IOException connectException) {
	            /* The connection was unsuccessful. We close the socket and broadcast a message. */
	        	context.sendBroadcast(new Intent(ACTION_IMPOSSIBLE_CONNECT));	        	
	            try {
	                getBluetoothSocket().close();
	            } catch (IOException closeException) { }
	            return;
	        }	        
	        /* If the connection succeeds, we notify the connection aware objects  */
	        notifyConnectionAwareFragments(true);	 
	    }	 
	}
	
	/* 
	 * Broadcast receiver used for those who want to receive a broadcast 
	 * of the connection status, whenever it happens.
	 */
	public static class BluetoothStatusReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			
			if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				switch(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)){
				case BluetoothAdapter.STATE_OFF:
				case BluetoothAdapter.STATE_TURNING_OFF:
					Toast.makeText(context, R.string.connectionLost, Toast.LENGTH_SHORT).show();
					notifyConnectionAwareFragments(false);
					break;
				
				case BluetoothAdapter.STATE_CONNECTED:
					Toast.makeText(context, R.string.connected, Toast.LENGTH_SHORT).show();
					notifyConnectionAwareFragments(true);
					break;
				
				case BluetoothAdapter.STATE_DISCONNECTED:
					Toast.makeText(context, R.string.impossibleConnect, Toast.LENGTH_SHORT).show();
					notifyConnectionAwareFragments(false);
					break;
				}
			}else{
				Toast.makeText(context, R.string.impossibleConnect, Toast.LENGTH_SHORT).show();
				notifyConnectionAwareFragments(false);
			}
		}
		
	}
}
