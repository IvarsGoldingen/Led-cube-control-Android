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
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

/**
 * Created by Ivars on 2017.06.04..
 */

public class BTControlActivity extends AppCompatActivity {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    static final int YOUR_REQUEST_CODE = 200; // could be something else..
    static final int MAX_SLIDER_VALUE = 100; //
    static final int SLIDER_RATIO = 32768 / MAX_SLIDER_VALUE; // because 32768 is max value form mic
    static final int DEFAULT_SOUND_LEVEL1 = 5; // because 32768 is max value form mic
    static final int DEFAULT_SOUND_LEVEL2 = 10; // because 32768 is max value form mic
    static final int DEFAULT_SOUND_LEVEL3 = 20; // because 32768 is max value form mic
    static final int DEFAULT_SOUND_LEVEL4 = 40; // because 32768 is max value form mic
    private static final int STATE_MESSAGE = 1;
    private static final int READ_MESSAGE = 2;
    private static final int WRITE_MESSAGE = 3;
    private static final int ERROR_MESSAGE = 4;
    boolean spinnerInSetup = true;
    @BindView(R.id.testTextview)
    TextView testTextView;
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
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //handker msg has three argumenst what arg1 and arg2. what = type of message
            switch (msg.what) {
                case STATE_MESSAGE:
                    switch (msg.arg1) {
                        case STATE_NONE:
                            Toast.makeText(BTControlActivity.this, "State none", Toast.LENGTH_SHORT).show();
                            break;
                        case STATE_CONNECTED:
                            //Toast.makeText(BTControlActivity.this, "State connected", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            break;
                        case STATE_CONNECTING:
                            Toast.makeText(BTControlActivity.this, "Connecting", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case ERROR_MESSAGE:
                    Toast.makeText(BTControlActivity.this, msg.getData().getString("Connecting failed"),
                            Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case READ_MESSAGE:
                    int numberOfReceivedBytes = msg.arg1;
                    byte[] readBuf = (byte[]) msg.obj;
                    if (numberOfReceivedBytes > 0) {
                        String receivedData = new String(readBuf, 0, msg.arg1);
                        if (!receivedData.isEmpty()) {
                            Log.e("Read message", receivedData);
                        }
                    }
                    break;
                default:
                    //Toast.makeText(BTControlActivity.this, "unknown message in handler", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    @BindView(R.id.button_play_pattern)
    Button playStopPatternButton;
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    BluetoothAdapter mBluetoothAdapter;
    private int soundLevel1 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL1;
    private int soundLevel2 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL2;
    private int soundLevel3 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL3;
    private int soundLevel4 = SLIDER_RATIO * DEFAULT_SOUND_LEVEL4;
    private Handler mVisualizerHandler;
    private SoundMeter mSoundmeter = null;
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private int amplitude = 0;
    private ArrayList<LedScene> selectedArrayList = new ArrayList<LedScene>();
    private int selectedArrayListSize = 0;
    private int patternToSend = 0;
    private boolean sendNewScenes = false;
    private BTService mBTService = null;

    @OnClick(R.id.button_play_pattern)
    public void playStopButtonPressed() {
        String currentButtonFunction = String.valueOf(playStopPatternButton.getText());
        if (currentButtonFunction.equals("PLAY")) {
            stopEQ();
            sendNewScenes = true;
            playStopPatternButton.setText("STOP");
            scheduledExecutorService = Executors.newScheduledThreadPool(0);
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
                    Toast.makeText(this, "File read error\nIncorrect number of bytes", Toast.LENGTH_SHORT).show();
                    playStopPatternButton.setText("PLAY");
                    break;
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to play pattern", Toast.LENGTH_SHORT).show();
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
                        //Toast.makeText(BTControlActivity.this, "Scene Number: " + patternToSend, Toast.LENGTH_SHORT).show();
                        sendMessage(selectedArrayList.get(patternToSend).getLevels());
                        patternToSend++;
                        if (patternToSend >= selectedArrayListSize) {
                            patternToSend = 0;
                        }
                        if (sendNewScenes) {
                            startPatternSending(selectedArrayList.get(patternToSend).getSceneTimeInMillis());
                        }
                    }
                });
            }
        }, sceneTime, TimeUnit.MILLISECONDS);
    }

    @OnClick(R.id.button_open_pattern_creator)
    public void openPatternCreator() {
        stopEQ();
        Intent patternCreatorIntent = new Intent(BTControlActivity.this, PatternCreator.class);
        startActivity(patternCreatorIntent);
    }

    @OnClick(R.id.button_select_pattern)
    public void selectPattern() {
        createOpenPatternDialog();
    }

    @OnClick(R.id.button_static_pattern)
    public void staticPattern() {
        stopEQ();
        stopPatternSending();
        sendByteMessage((byte) 11);
    }

    @OnClick(R.id.button_random)
    public void random() {
        stopEQ();
        stopPatternSending();
        sendByteMessage((byte) 12);
    }

    @OnClick(R.id.button_snake1)
    public void snake1() {
        stopEQ();
        stopPatternSending();
        sendByteMessage((byte) 15);
    }

    @OnClick(R.id.button_sound1)
    public void sound1() {
        startEQ();
        sendByteMessage((byte) 13);
    }

    @OnClick(R.id.button_sound2)
    public void sound2() {
        startEQ();
        sendByteMessage((byte) 14);
    }

    @OnClick(R.id.button_snake2)
    public void snake2() {
        stopEQ();
        stopPatternSending();
        sendByteMessage((byte) 16);
    }

    public void startEQ() {
        //init timer
        stopPatternSending();
        if (mSoundmeter == null) {
            mSoundmeter = new SoundMeter();
            mTimer = new Timer();
            initTimerTask();
            mSoundmeter.strart();
            mTimer.schedule(mTimerTask, 0, 25);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @OnClick(R.id.button_stop)
    public void stopEQ() {
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

        //Toast.makeText(this, "EQ stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visualizer_layout);

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
        stopEQ();
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
                    sendByteMessage((byte) (31 + position));
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
        Toast.makeText(this, "Levels must be in order", Toast.LENGTH_SHORT).show();
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

        mBTService = new BTService(this, mHandler);
        mBTService.connect(mDevice);
    }

    private void sendByteMessage(byte message) {
        if (mBTService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "No one to send to", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, String.valueOf(message), Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] send = {message};

        mBTService.write(send);

    }

    private void sendMessage(byte[] message) {
        if (mBTService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, "No one to send to", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length == 8) {
            //8 bytes of scenes + 1 command ID byte
            byte[] temp = new byte[9];
            temp[0] = 17;
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
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, YOUR_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == YOUR_REQUEST_CODE) {
                Log.i("tets", "Permission granted");
                //do what you wanted to do
            }
        } else {
            Log.d("tets", "Permission failed");
        }
    }

    private void initTimerTask() {
        //A task that can be scheduled for one-time or repeated execution by a Timer.
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                amplitude = mSoundmeter.getAmplitude();
                //the below code will be executed on the thread where the handler is attached
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        testTextView.setText(valueOf(amplitude));
                        updateBars(amplitude);
                        //32768
                    }
                });
            }
        };
    }

    public void updateBars(int loudness) {
        if (loudness < soundLevel1) {

            viewLevel1.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else if (loudness < soundLevel2) {
            sendByteMessage((byte) 1);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else if (loudness < soundLevel3) {
            sendByteMessage((byte) 2);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.silent));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else if (loudness < soundLevel4) {
            sendByteMessage((byte) 3);
            viewLevel1.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel2.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel3.setBackgroundColor(getResources().getColor(R.color.loud));
            viewLevel4.setBackgroundColor(getResources().getColor(R.color.silent));
        } else {
            sendByteMessage((byte) 4);
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
            Toast.makeText(BTControlActivity.this, "No pattern files saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectPattern(String fileName) {
        currentlySelectedPatternText.setText(fileName);
    }


}
