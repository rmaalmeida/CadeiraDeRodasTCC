package br.edu.unifei.iesti.eco.igoreyleine.tfg.activities;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;

import edu.cmu.pocketsphinx.AssetsTask;
import edu.cmu.pocketsphinx.AssetsTaskCallback;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.R;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.adapters.ControlTypeFragmentAdapter;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.control.BluetoothManager;
import br.edu.unifei.iesti.eco.igoreyleine.tfg.fragments.AccelerometerControlFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.Tab;
import android.util.Log;
import android.widget.Toast;

/* 
 * Main app class. Represents the Activity. Furthermore,
 * implements some important interfaces for voice recognition
 * and a interface to respond to changes in the selected 
 * tab.
 * 
 * The interface RecognitionListener corresponds to actions 
 * to be taken when the user starts to spell a command, 
 * finish to spell a command and when partial and final 
 * results arise.
 * 
 * The AssetsTaskCallback interface corresponds to actions 
 * to be taken depending on the state of the AsyncTask that 
 * is executed to copy the app assets to the device hard drive.
 * 
 * The interface ActionBar.TabListener corresponds to actions 
 * to be taken when the user navigate through the app's tabs.
 * 
 */
public class MainActivity extends ActionBarActivity implements
		RecognitionListener, AssetsTaskCallback, ActionBar.TabListener {
	
	/* 
	 * Key used to retrieve the variable that holds the state of the voice commands, 
	 * during the retrieval of the app instance state.
	 */
	public static final String VOICE_COMMANDS_STATE_KEY = "VOICE_COMMANDS_STATE";
	
	/* Defines the position of each command inside the COMMANDS vector */
	public static final int FORWARD_COMMAND_ID = 0;
	public static final int BACKWARDS_COMMAND_ID = 1;
	public static final int RIGHT_COMMAND_ID = 2;
	public static final int LEFT_COMMAND_ID = 3;
	public static final int STOP_COMMAND_ID = 4;
	
	/* COMMANDS vector with each possible voice command. */
	public static final String[] COMMANDS = { "FORWARD", "BACKWARDS", "RIGHT", "LEFT", "STOP"};

	/* Number of received partial results */
	private int parcialResultsIteration = 0;

	/* Handler to the speech recognition class */
	private SpeechRecognizer recognizer;
	
	/* ProgressDialog used during the copy of the assets to the device */
	private ProgressDialog dialog;

	/* Boolean variable that tells whether the assets have been copied to the device or not */
	private boolean setupDone = false;
	
	/* 
	 * Boolean that indicates if the delivery of the final result was
	 * forced by the partial results analysis, indicating that a potential 
	 * candidate is certain.
	 * 
	 */
	private boolean hardStopped = false;
	
	/*
	 * Booleano used to know whether the voice commands are enabled
	 */
	private boolean voiceCommandsEnabled = true;
	
	/* ViewPager used to respond to tab switching via swipes */
	private ViewPager viewPager;
	
	/* ViewPager adapter reference */
	private ControlTypeFragmentAdapter adapter;
	
	/* BroadcastReceiver that will be registered to listen for Bluetooth status changes */
	private BluetoothManager.BluetoothStatusReceiver statusReceiver;

	
	/* Method called by the Android OS when the Activity is being created. */
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		/* 
		 * We retrieve a handler for the app ActionBar, in order to 
		 * control the app's tabs.
		 */
		final ActionBar actionBar = getSupportActionBar();
		
		/* We inflate the app layout to the activity */
		setContentView(R.layout.activity_main);
		
		/*
		 * We retrieve a reference of the layout's ViewPager to our 
		 * activity. This way we can handle tab switching.
		 * Futhermore, we create a instance of our adapter and 
		 * define him as our ViewPager adapter. Finally we create 
		 * a handler that changes the selected tab when the user 
		 * changes the current selected tab through a swipe 
		 * movement.
		 */
		viewPager = (ViewPager)findViewById(R.id.pager);
		adapter = new ControlTypeFragmentAdapter(getSupportFragmentManager());
		viewPager.setAdapter(adapter);
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
			@Override
			public void onPageSelected(int position){
				boolean showAccelerometerFragment;
				if(position == ControlTypeFragmentAdapter.ACCELEROMETER_CONTROL_POSITION)
					showAccelerometerFragment = true;
				else
					showAccelerometerFragment = false;
				
				((AccelerometerControlFragment)adapter.getItem(ControlTypeFragmentAdapter.ACCELEROMETER_CONTROL_POSITION)).setVisibility(showAccelerometerFragment);
				actionBar.setSelectedNavigationItem(position);
			}
		});
		
		/* Navigation tabs are created */				
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		actionBar.addTab(actionBar.newTab().setText(R.string.settings).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.touchControl).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.accelerometerControl).setTabListener(this));
		
		/*
		 * The AsyncTask respondible for copying the assets to the device hard disk
		 * is executed
		 */		
		new AssetsTask(this, this).execute();
		
		/* The BluetoothManager is initialized */
		BluetoothManager.init(this);
		statusReceiver = new BluetoothManager.BluetoothStatusReceiver();
		
		if(state != null)
			voiceCommandsEnabled = state.getBoolean(VOICE_COMMANDS_STATE_KEY);
	}

	@Override
	public void onPause() {
		super.onPause();
		
		unregisterReceiver(statusReceiver);
		
		if(recognizer != null){
			recognizer.cancel();
			recognizer.stop();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
		/* 
		 * Whenever the activity is shown, we register a BroadcastReceiver 
		 * that will be continuously listening for broadcast intents about 
		 * a impossible bluetooth connection. 
		 * */
		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothManager.ACTION_IMPOSSIBLE_CONNECT);
		registerReceiver(statusReceiver, filter);
		
		/* Whenever the activity is shown, we try to enable the bluetooth. */
		BluetoothManager.enableBluetooth();
		
		/* If the user wants to use voice commands, and the environment is 
		 * correctly set up, we start listening for commands. */
		if (setupDone && voiceCommandsEnabled && recognizer != null)
			recognizer.startListening("");
	}
	
	/* Method called by the Android OS when the Activity is going 
	 * out of focus. Here, we save variables that represents 
	 * the state of the activity, for when it comes to focus again.
	 * In our case, we only save the status of the voice commands. */
	@Override
	public void onSaveInstanceState(Bundle instanceState){
		instanceState.putBoolean(VOICE_COMMANDS_STATE_KEY, voiceCommandsEnabled);
	}

	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		String hyp = hypothesis.getHypstr();
		Log.d("TAG", "PartialResult: " + hyp + " PartialScore: " + hypothesis.getBestScore());
		
		/* 
		 * Updates the possibility score and, given a certain threshold (10, in this case), 
		 * verify if more than 80% of all the score, were given to the same possibility.
		 *  
		 */
		//TODO: implementar isso direito.
		//Updates the score
		parcialResultsIteration++;
		
		if(parcialResultsIteration >= 10){
			recognizer.stop();
			hardStopped = true;			
		}
	}

	@Override
	public void onResult(Hypothesis hypothesis) {
		String text = hypothesis.getHypstr();
		Log.d("TAG", "FinalResult: " + text + " Score: " + hypothesis.getBestScore());
		
		if(!text.isEmpty() && text.split(" ").length > 0){
			makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
			int count = 0;
			for(String s : COMMANDS){
				if(s.equals(text)){
					BluetoothManager.sendCommand(count);
				}
				count++;
			}
		}
		
		if(hardStopped){
			hardStopped = false;
			recognizer.startListening("");
		}
		
		parcialResultsIteration = 0;
	}

	@Override
	public void onBeginningOfSpeech() {
		Log.d("TAG", "Beginning speech");
	}

	@Override
	public void onEndOfSpeech() {
		Log.d("TAG", "End of speech");
		recognizer.stop();
		recognizer.startListening("");
	}

	@Override
	public void onTaskCancelled() {	}

	@Override
	public void onTaskComplete(File assetsDir) {
		File modelsDir = new File(assetsDir, "models");
		recognizer = defaultSetup()
				.setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
				.setDictionary(new File(modelsDir, "lm/2856.dic"))
				.setRawLogDir(assetsDir).setKeywordThreshold(1e-5f)
				.getRecognizer();

		recognizer.addListener(this);
		File languageModel = new File(modelsDir, "lm/2856.lm");
		recognizer.addNgramSearch("", languageModel);

		
		recognizer.startListening("");
		dialog.dismiss();
		setupDone = true;
	}

	@Override
	public void onTaskError(Throwable e) {
		if (dialog.isShowing())
			dialog.dismiss();
	}

	@Override
	public void onTaskProgress(File file) {
		dialog.incrementProgressBy(1);
	}

	@Override
	public void onTaskStart(int size) {
		dialog = new ProgressDialog(this);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setTitle("Copying model files...");
		dialog.setMax(size);
		dialog.show();
	}

	/*
	 * Method called when a tab that is already loaded is reselected.
	 * We do not need to worry about this method, because the 
	 * reselection scenario is being handled inside our adapter
	 * class.
	 */
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	/*
	 * Method called when a new tab is loaded. 
	 * Here, when the user selects a tab, we update the 
	 * viewpager in order for it to show the corresponding item.
	 */
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		boolean showAccelerometerFragment;
		if(tab.getPosition() == ControlTypeFragmentAdapter.ACCELEROMETER_CONTROL_POSITION)
			showAccelerometerFragment = true;
		else
			showAccelerometerFragment = false;
		
		((AccelerometerControlFragment)adapter.getItem(ControlTypeFragmentAdapter.ACCELEROMETER_CONTROL_POSITION)).setVisibility(showAccelerometerFragment);
		
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		
	}
	
	public boolean getVoiceCommands(){
		return voiceCommandsEnabled;
	}

	/* Method used to change the voice commands state */
	public void setVoiceCommands(boolean voiceCommandsEnabled) {
		this.voiceCommandsEnabled = voiceCommandsEnabled;
		
		if(setupDone && recognizer != null)
			if(voiceCommandsEnabled)
				recognizer.startListening("");
			else
				recognizer.stop();
	}
	
	public void setSelectedViewPage(int position){
		viewPager.setCurrentItem(position, true);
	}
	
	public int getSelectedViewPage(){
		return viewPager.getCurrentItem();
	}

}
