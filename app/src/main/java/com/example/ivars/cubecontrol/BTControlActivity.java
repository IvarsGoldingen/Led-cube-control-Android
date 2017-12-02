package com.example.ivars.cubecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static java.lang.String.valueOf;

public class BTControlActivity extends AppCompatActivity {

    public static final int DIM_SPPED_FOR_VISUALIZER_MODE = 31;
    //how often is sound level sent to the microcontroller
    public static final int SEND_LOUDNESS_PERIOD_TIME = 25;
    //Mode IDs that are recognised by the microcontroller
    private static final int STATIC_PATTERN_ID = 11;
    private static final int RANDOM_ID = 12;
    private static final int SNAKE_1_ID = 15;
    private static final int SNAKE_2_ID = 16;
    private static final int SOUND_1_ID = 13;
    private static final int SOUND_2_ID = 14;
    private static final int SCENE_MODE_ID = 17;
    //Sound levels for visualizer
    private static final int LOUDNESS_1 = 1;
    private static final int LOUDNESS_2 = 2;
    private static final int LOUDNESS_3 = 3;
    private static final int LOUDNESS_4 = 4;
    // Constants that indicate the current connection state
    private static final int STATE_NONE = 0;       // we're doing nothing
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3;  // now connected to a remote device

    //Message types for the handler that communicates between BTService and this activity
    private static final int STATE_MESSAGE = 1;
    private static final int READ_MESSAGE = 2;
    private static final int WRITE_MESSAGE = 3;
    private static final int ERROR_MESSAGE = 4;

    //Code for audio recording permission
    private static final int RECORD_AUDIO_REQUEST_CODE = 200; // could be something else..

    //Values for slider, used for Visualizer modes
    private static final int MAX_SLIDER_VALUE = 100; //
    private static final int SLIDER_RATIO = 32768 / MAX_SLIDER_VALUE; // because 32768 is max value form mic
    private static final int DEFAULT_SOUND_LEVEL1 = 5; // because 32768 is max value form mic
    private static final int DEFAULT_SOUND_LEVEL2 = 10; // because 32768 is max value form mic
    private static final int DEFAULT_SOUND_LEVEL3 = 20; // because 32768 is max value form mic
    private static final int DEFAULT_SOUND_LEVEL4 = 40; // because 32768 is max value form mic
    //Butterknife binds
    @BindView(R.id.text_currently_selected_pattern)
    TextView currentlySelectedPatternText;
    @BindView(R.id.level_1)
    View viewLevel1;
    @BindView(R.id.level_2)
    View viewLevel2;
    @BindView(R.id.level_3)
    View viewLevel3;
    @BindView(R.id.level_4)
    View viewLevel4;
    @BindView(R.id.slide_bar_level1)
    SeekBar slideBar_level1;
    @BindView(R.id.slide_bar_level2)
    SeekBar slideBar_level2;
    @BindView(R.id.slide_bar_level3)
    SeekBar slideBar_level3;
    @BindView(R.id.slide_bar_level4)
    SeekBar slideBar_level4;
    @BindView(R.id.textview_level1)
    TextView textviewLevel1;
    @BindView(R.id.textview_level2)
    TextView textviewLevel2;
    @BindView(R.id.textview_level3)
    TextView textviewLevel3;
    @BindView(R.id.textview_level4)
    TextView textviewLevel4;
    @BindView(R.id.dim_spinner)
    Spinner dimSpinner;
    @BindView(R.id.loading_indicator)
    ProgressBar progressBar;
    @BindView(R.id.button_play_pattern)
    Button playStopPatternButton;
    @BindView(R.id.state_title_textview)
    TextView stateTitleTextView;
    //Other
    private boolean spinnerInSetup = true;
    //scheduledExecutorService allows to send messages periodically. Used for patterns
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private BluetoothAdapter mBluetoothAdapter;
    private int soundLevel1 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL1;
    private int soundLevel2 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL2;
    private int soundLevel3 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL3;
    private int soundLevel4 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL4;
    private SoundMeter mSoundmeter = null;
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private int amplitude = 0;
    private ArrayList<LedScene> selectedArrayList = new ArrayList<LedScene>();
    private int selectedArrayListSize = 0;
    private int patternToSend = 0;
    private boolean sendNewScenes = false;
    private BTService mBTService = null;
    private Handler mHandler = null;
    private String targetBTDeviceName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_layout);
        getSupportActionBar().hide();
        mHandler = new MessageHandler(this);
        ButterKnife.bind(this);
        askPermissions();
        connectToDevice();
        setUpSlideBars();
        setUpSpinner();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBTService != null) {
            mBTService.stop();
        }
        stopVisualizer();
    }

    @OnClick(R.id.button_play_pattern)
    public void playStopButtonPressed() {
        String currentButtonFunction = String.valueOf(playStopPatternButton.getText());
        if (currentButtonFunction.equals("PLAY")) {
            stopVisualizer();
            sendNewScenes = true;
            playStopPatternButton.setText("STOP");
            //Initialize the scheduledExecutorService to schedule sending messages
            scheduledExecutorService = Executors.newScheduledThreadPool(5);
            playPattern();
        } else {
            stopPatternSending();
        }
    }

    private void stopPatternSending() {
        sendNewScenes = false;
        playStopPatternButton.setText("PLAY");
        scheduledExecutorService.shutdown();
    }

    public void playPattern() {
        //read the pattern from file
        File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
        String patternToPlay = String.valueOf(currentlySelectedPatternText.getText());
        selectedArrayList.clear();
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(patternDirectory + "/" + patternToPlay));
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            int currentSceneNumber = 1;
            byte[] tempArray = new byte[12];
            int ofset = 0;
            int numberOfReadBytes = 0;
            //read the full scene
            while (true) {
                //read 12 bytes from the file
                numberOfReadBytes = bufferedInputStream.read(tempArray, ofset, 12);
                if (numberOfReadBytes == 12) {
                    selectedArrayList.add(new LedScene(tempArray, currentSceneNumber));
                } else if (numberOfReadBytes == -1) {
                    //if the count of bytes in the file divide with 12 it should be correct
                    //Toast.makeText(this, "File read correctly", Toast.LENGTH_SHORT).show();
                    selectedArrayListSize = selectedArrayList.size();
                    startPatternSending(100);//start pattern sending with small delay
                    break;
                } else {
                    //if less than 12 bytes are read the file is corrupt
                    SingleToast.show(this, "File read error\nIncorrect number of bytes");
                    playStopPatternButton.setText("PLAY");
                    break;
                }
            }
        } catch (IOException e) {
            SingleToast.show(this, "Failed to play pattern");
            playStopPatternButton.setText("PLAY");
            Log.e("Error", String.valueOf(e));
            e.printStackTrace();
        }

    }

    private void startPatternSending(int sceneTime) {
        //this is preferable to timer when multiple worker threads are needed
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //send pattern data after the st time limit
                        sendMessage(selectedArrayList.get(patternToSend).getLevels());
                        patternToSend++;
                        if (patternToSend >= selectedArrayListSize) {
                            //if at the end of pattern start from begining
                            patternToSend = 0;
                        }
                        if (sendNewScenes) {
                            //if pattern creator still active send the next scene
                            startPatternSending(selectedArrayList.get(patternToSend).getSceneTimeInMillis());
                        }
                    }
                });
            }
        }, sceneTime, TimeUnit.MILLISECONDS);
    }

    @OnClick(R.id.button_open_pattern_creator)
    public void openPatternCreator() {
        stopVisualizer();
        Intent patternCreatorIntent = new Intent(BTControlActivity.this, PatternCreator.class);
        startActivity(patternCreatorIntent);
    }

    @OnClick(R.id.button_select_pattern)
    public void selectPattern() {
        createOpenPatternDialog();
    }

    @OnClick(R.id.button_static_pattern)
    public void staticPattern() {
        stopVisualizer();
        stopPatternSending();
        sendSingleByteMessage((byte) STATIC_PATTERN_ID);
    }

    @OnClick(R.id.button_random)
    public void random() {
        stopVisualizer();
        stopPatternSending();
        sendSingleByteMessage((byte) RANDOM_ID);
    }

    @OnClick(R.id.button_snake1)
    public void snake1() {
        stopVisualizer();
        stopPatternSending();
        sendSingleByteMessage((byte) SNAKE_1_ID);
    }

    @OnClick(R.id.button_sound1)
    public void sound1() {
        startVisualizer();
        sendSingleByteMessage((byte) SOUND_1_ID);
    }

    @OnClick(R.id.button_sound2)
    public void sound2() {
        startVisualizer();
        sendSingleByteMessage((byte) SOUND_2_ID);
    }

    @OnClick(R.id.button_snake2)
    public void snake2() {
        stopVisualizer();
        stopPatternSending();
        sendSingleByteMessage((byte) SNAKE_2_ID);
    }

    public void startVisualizer() {
        //init timer
        stopPatternSending();
        if (mSoundmeter == null) {
            mSoundmeter = new SoundMeter();
            mTimer = new Timer();
            initTimerTask();
            mSoundmeter.strart();
            mTimer.schedule(mTimerTask, 0, SEND_LOUDNESS_PERIOD_TIME);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @OnClick(R.id.button_stop)
    public void stopVisualizer() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
        }
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mSoundmeter != null) {
            mSoundmeter.stop();
        }
        mTimer = null;
        mTimerTask = null;
        mSoundmeter = null;
    }

    private void setUpSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.dim_level_array, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dimSpinner.setAdapter(adapter);
        dimSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerInSetup) {
                    //Microcontroller will use this value for setting the dim speed
                    sendSingleByteMessage((byte) (DIM_SPPED_FOR_VISUALIZER_MODE + position));
                } else {
                    spinnerInSetup = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void levelsInOrderToast() {
        SingleToast.show(this, "Levels must be in order");
    }

    private void setUpSlideBars() {
        SharedPreferences sharedPreferences = this.getPreferences(MODE_PRIVATE);
        final SharedPreferences.Editor shPrefEditor = sharedPreferences.edit();

        slideBar_level1.setMax(MAX_SLIDER_VALUE);
        slideBar_level1.setProgress(sharedPreferences.getInt("LEVEL1_PREFERENCE", DEFAULT_SOUND_LEVEL1));
        textviewLevel1.setText(valueOf(slideBar_level1.getProgress()));
        slideBar_level1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textviewLevel1.setText(valueOf(slideBar_level1.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= slideBar_level2.getProgress()) {
                    seekBar.setProgress(slideBar_level2.getProgress() - 1);
                    levelsInOrderToast();
                }
                if (seekBar.getProgress() == 0) {
                    seekBar.setProgress(1);
                }
                shPrefEditor.putInt("LEVEL1_PREFERENCE", seekBar.getProgress());
                shPrefEditor.apply();
                soundLevel1 = seekBar.getProgress() * SLIDER_RATIO;
            }
        });

        slideBar_level2.setMax(MAX_SLIDER_VALUE);
        slideBar_level2.setProgress(sharedPreferences.getInt("LEVEL2_PREFERENCE", DEFAULT_SOUND_LEVEL2));
        textviewLevel2.setText(valueOf(slideBar_level2.getProgress()));
        slideBar_level2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textviewLevel2.setText(valueOf(slideBar_level2.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress >= slideBar_level3.getProgress() ||
                        progress <= slideBar_level1.getProgress()) {
                    seekBar.setProgress(slideBar_level3.getProgress() - 1);
                    levelsInOrderToast();
                }
                soundLevel2 = progress * SLIDER_RATIO;
                shPrefEditor.putInt("LEVEL2_PREFERENCE", seekBar.getProgress());
                shPrefEditor.apply();
            }
        });

        slideBar_level3.setMax(MAX_SLIDER_VALUE);
        slideBar_level3.setProgress(sharedPreferences.getInt("LEVEL3_PREFERENCE", DEFAULT_SOUND_LEVEL3));
        textviewLevel3.setText(valueOf(slideBar_level3.getProgress()));
        slideBar_level3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textviewLevel3.setText(valueOf(slideBar_level3.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress >= slideBar_level4.getProgress() ||
                        progress <= slideBar_level2.getProgress()) {
                    seekBar.setProgress(slideBar_level4.getProgress() - 1);
                    levelsInOrderToast();
                }
                soundLevel3 = progress * SLIDER_RATIO;
                shPrefEditor.putInt("LEVEL3_PREFERENCE", seekBar.getProgress());
                shPrefEditor.apply();
            }
        });

        slideBar_level4.setMax(MAX_SLIDER_VALUE);
        slideBar_level4.setProgress(sharedPreferences.getInt("LEVEL4_PREFERENCE", DEFAULT_SOUND_LEVEL4));
        textviewLevel4.setText(valueOf(slideBar_level4.getProgress()));
        slideBar_level4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textviewLevel4.setText(valueOf(slideBar_level4.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress <= slideBar_level3.getProgress()) {
                    seekBar.setProgress(slideBar_level3.getProgress() + 1);
                    levelsInOrderToast();
                }
                soundLevel4 = progress * SLIDER_RATIO;
                shPrefEditor.putInt("LEVEL4_PREFERENCE", seekBar.getProgress());
                shPrefEditor.apply();
            }
        });
    }

    private void connectToDevice() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String btDeviceAddress = (String) getIntent().getSerializableExtra("deviceAddress");
        BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(btDeviceAddress);
        targetBTDeviceName = mDevice.getName();
        mBTService = new BTService(mHandler);
        mBTService.connect(mDevice);
    }

    private void sendSingleByteMessage(byte message) {
        if (mBTService.getState() != BTService.STATE_CONNECTED) {
            Log.e("Send byte message: ", "Not connected to a device");
            return;
        }
        byte[] send = {message};
        mBTService.write(send);
    }

    private void sendMessage(byte[] message) {
        if (mBTService.getState() != BTService.STATE_CONNECTED) {
            Log.e("Send byte message: ", "Not connected to a device");
            return;
        }
        //if the message is correct length
        if (message.length == 8) {
            //8 bytes of scenes + 1 command ID byte
            byte[] temp = new byte[9];
            temp[0] = SCENE_MODE_ID;
            for (int x = 0; x < 8; x++) {
                temp[x + 1] = message[x];
            }
            mBTService.write(temp);
        }
    }

    private void askPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) //check if permission request is necessary
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
                Log.i("test", "Permission granted");
            }
        } else {
            Log.e("test", "Permission failed");
        }
    }

    //init showing sound loudnes in UI and sending to microcontroller
    private void initTimerTask() {
        //A task that can be scheduled for one-time or repeated execution by a Timer.
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                //get loudness from soundmeter
                amplitude = mSoundmeter.getAmplitude();
                //the below code will be executed on the thread where the handler is attached
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //send the loudness to the microcontroller and update UI
                        updateBars(amplitude);
                    }
                });
            }
        };
    }

    public void updateBars(int loudness) {
        //set UI for app and also send the info to the microcontroller depending on the loudness
        if (loudness < soundLevel1) {
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else if (loudness < soundLevel2) {
            sendSingleByteMessage((byte) LOUDNESS_1);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else if (loudness < soundLevel3) {
            sendSingleByteMessage((byte) LOUDNESS_2);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else if (loudness < soundLevel4) {
            sendSingleByteMessage((byte) LOUDNESS_3);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else {
            sendSingleByteMessage((byte) LOUDNESS_4);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.loud));
        }

    }

    private void createOpenPatternDialog() {
        File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
        String fileList[] = patternDirectory.list();
        int numberOfFiles = fileList.length;
        if (numberOfFiles > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose pattern file to open:");

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                    (this, android.R.layout.select_dialog_singlechoice);
            for (int i = 0; i < numberOfFiles; i++) {
                arrayAdapter.add(fileList[i]);
            }

            builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectPattern(arrayAdapter.getItem(which));
                }
            });
            builder.show();

        } else {
            SingleToast.show(this, "No pattern files saved");
        }
    }

    private void selectPattern(String fileName) {
        currentlySelectedPatternText.setText(fileName);
    }

    //handler for getting messages from the BTService
    private static class MessageHandler extends Handler {
        private final WeakReference<BTControlActivity> mWeakReference;

        public MessageHandler(BTControlActivity activity) {
            mWeakReference = new WeakReference<BTControlActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            //handler msg has three argumenst what arg1 and arg2. what = type of message
            switch (msg.what) {
                case STATE_MESSAGE:
                    switch (msg.arg1) {
                        case STATE_NONE:
                            SingleToast.show(mWeakReference.get(), "Disconnected");
                            mWeakReference.get().stateTitleTextView.setText("Disconnected");
                            break;
                        case STATE_CONNECTED:
                            mWeakReference.get().progressBar.setVisibility(View.GONE);
                            SingleToast.show(mWeakReference.get(), "Successfully connected");
                            mWeakReference.get().stateTitleTextView.setText("Connected to " + mWeakReference.get().targetBTDeviceName);
                            break;
                        case STATE_CONNECTING:
                            SingleToast.show(mWeakReference.get(), "Connecting");
                            mWeakReference.get().stateTitleTextView.setText("Connecting to " + mWeakReference.get().targetBTDeviceName);
                            break;
                    }
                    break;
                case ERROR_MESSAGE:
                    SingleToast.show(mWeakReference.get(), msg.getData().getString("Connecting failed"));
                    //go back to the connect activity
                    mWeakReference.get().finish();
                    break;
                case READ_MESSAGE:
                    int numberOfReceivedBytes = msg.arg1;
                    byte[] readBuf = (byte[]) msg.obj;
                    if (numberOfReceivedBytes > 0) {
                        String receivedData = new String(readBuf, 0, msg.arg1);
                        if (!receivedData.isEmpty()) {
                            Log.i("Read message", receivedData);
                        }
                    }
                    break;
                case WRITE_MESSAGE:
                    int numberOfReceivedWriteBytes = msg.arg1;
                    byte[] writeBuf = (byte[]) msg.obj;
                    if (numberOfReceivedWriteBytes > 0) {
                        String receivedData = new String(writeBuf, 0, msg.arg1);
                        if (!receivedData.isEmpty()) {
                            Log.i("Write message", receivedData);
                        }
                    }
                    break;
                default:
                    Log.e("BTControlactivity: ", "Unknown message ID in handler");
                    break;
            }
        }
    }
}
