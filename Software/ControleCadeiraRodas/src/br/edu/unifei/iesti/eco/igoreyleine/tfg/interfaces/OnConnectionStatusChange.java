package br.edu.unifei.iesti.eco.igoreyleine.tfg.interfaces;

/*
 * Interface used by classes that want to be aware of the bluetooth connection.
 * The class BluetoothManager uses this interface to implement a Observer
 * Software Design Pattern.
 */
public interface OnConnectionStatusChange {

	public void onConnectionStatusChanged(boolean isConnectedNow);
	
}
