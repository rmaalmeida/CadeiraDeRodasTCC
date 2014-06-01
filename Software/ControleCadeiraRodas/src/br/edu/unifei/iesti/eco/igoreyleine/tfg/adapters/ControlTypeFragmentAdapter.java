package br.edu.unifei.iesti.eco.igoreyleine.tfg.adapters;

import br.edu.unifei.iesti.eco.igoreyleine.tfg.control.BluetoothManager;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments.AccelerometerControlFragment;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments.SettingsFragment;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments.TouchControlFragment;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.interfaces.OnConnectionStatusChange;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ControlTypeFragmentAdapter extends FragmentPagerAdapter {

	/* 
	 * Possible position of each fragment inside the adapter
	 */
	public static final int SETTINGS_POSITION = 0;
	public static final int TOUCH_CONTROL_POSITION = 1;
	public static final int ACCELEROMETER_CONTROL_POSITION = 2;
	
	/*
	 * Maximum number of fragments we have in our application.
	 * 
	 * 1 - Configurations
	 * 2 - Touch Control
	 * 3 - Accelerometer Control
	 * 
	 */
	public static final int FRAGMENTS_COUNT = 3;
	
	/* Fragments */
	private Fragment[] fragments = new Fragment[FRAGMENTS_COUNT];
	
	public ControlTypeFragmentAdapter(FragmentManager fm) {
		super(fm);
	}
	
	/* 
	 * Returns the fragment relative to the given position
	 * Instatiates the given fragment if it wasn't done yet 
	 * and adds it to the fragments list that need to be aware 
	 * of the bluetooth connection state, if that is the case.
	 */
	@Override
	public Fragment getItem(int position) {
		switch(position){
		case SETTINGS_POSITION:
			if(fragments[SETTINGS_POSITION] == null){
				fragments[SETTINGS_POSITION] = new SettingsFragment();
				BluetoothManager.addConnectionAwareFragment((OnConnectionStatusChange)fragments[SETTINGS_POSITION]);
			}
			
			return fragments[SETTINGS_POSITION];
			
		case TOUCH_CONTROL_POSITION:
			if(fragments[TOUCH_CONTROL_POSITION] == null){
				fragments[TOUCH_CONTROL_POSITION] = new TouchControlFragment();
				BluetoothManager.addConnectionAwareFragment((OnConnectionStatusChange)fragments[TOUCH_CONTROL_POSITION]);
			}
			
			return fragments[TOUCH_CONTROL_POSITION];
			
		case ACCELEROMETER_CONTROL_POSITION:
			if(fragments[ACCELEROMETER_CONTROL_POSITION] == null){
				fragments[ACCELEROMETER_CONTROL_POSITION] = new AccelerometerControlFragment();
				BluetoothManager.addConnectionAwareFragment((OnConnectionStatusChange)fragments[ACCELEROMETER_CONTROL_POSITION]);
			}
			
			return fragments[ACCELEROMETER_CONTROL_POSITION];
		}
		return null;
	}

	/* Retorns the maximum number of fragments */
	@Override
	public int getCount() {
		return FRAGMENTS_COUNT;
	}

}
