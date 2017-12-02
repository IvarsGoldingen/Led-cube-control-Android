package com.example.ivars.cubecontrol;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PatternCreator extends AppCompatActivity {

    @BindView(R.id.button_level1)
    Button buttonLevel1;
    @BindView(R.id.button_level2)
    Button buttonLevel2;
    @BindView(R.id.button_level3)
    Button buttonLevel3;
    @BindView(R.id.button_level4)
    Button buttonLevel4;
    @BindView(R.id.button_led1)
    Button buttonLed1;
    @BindView(R.id.button_led2)
    Button buttonLed2;
    @BindView(R.id.button_led3)
    Button buttonLed3;
    @BindView(R.id.button_led4)
    Button buttonLed4;
    @BindView(R.id.button_led5)
    Button buttonLed5;
    @BindView(R.id.button_led6)
    Button buttonLed6;
    @BindView(R.id.button_led7)
    Button buttonLed7;
    @BindView(R.id.button_led8)
    Button buttonLed8;
    @BindView(R.id.button_led9)
    Button buttonLed9;
    @BindView(R.id.button_led10)
    Button buttonLed10;
    @BindView(R.id.button_led11)
    Button buttonLed11;
    @BindView(R.id.button_led12)
    Button buttonLed12;
    @BindView(R.id.button_led13)
    Button buttonLed13;
    @BindView(R.id.button_led14)
    Button buttonLed14;
    @BindView(R.id.button_led15)
    Button buttonLed15;
    @BindView(R.id.button_led16)
    Button buttonLed16;
    @BindView(R.id.text_scene_to_be_edited)
    TextView textViewSceneToBeEdited;
    //current level
    private int currentLevel = 0;
    //time in millis of the pattern
    private int sceneTimeMillis = 1000;
    //stores current edited scene
    private int[][] ledScene = new int[4][16];
    //stores scene for copying and pasting
    private int[][] ledSceneCopy = new int[4][16];
    private String fileNameTemp = "";
    //stores current scene number, one that is being created or edited
    private int currentSceneNumber = 1;
    //stores total number of scenes int the pattern that is being edited
    private int totalNumberOfScenes = 0;
    private ArrayList mLedSceneArrayList = null;
    private LedSceneAdapter mLedSceneAdapter = null;

    @OnClick(R.id.button_clear_scene)
    public void clearScene() {
        for (int i = 0; i < 4; i++) {
            for (int y = 0; y < 16; y++) {
                ledScene[i][y] = 0;
            }
        }
        currentLevel = 0;
        updateCurrentLedUI();
        updateCurrentLevelUI();
    }

    @OnClick(R.id.text_scene_to_be_edited)
    public void discardChanges() {
        currentSceneNumber = totalNumberOfScenes + 1;
        updateLedToBeEditedText();
        textViewSceneToBeEdited.setBackgroundColor(getResources().getColor(R.color.scene_background_color));
    }

    @OnClick(R.id.button_save)
    public void saveCurrentScene() {
        LedScene newScene = new LedScene(ledScene, sceneTimeMillis, currentSceneNumber);
        if (currentSceneNumber > totalNumberOfScenes) {
            //creating a new scene
            totalNumberOfScenes++;
            currentSceneNumber = totalNumberOfScenes + 1;
            mLedSceneAdapter.add(newScene);
        } else {
            //editing an existing scene
            textViewSceneToBeEdited.setBackgroundColor(getResources().getColor(R.color.scene_background_color));
            mLedSceneArrayList.set(currentSceneNumber - 1, newScene);
            mLedSceneAdapter.notifyDataSetChanged();
            currentSceneNumber = totalNumberOfScenes + 1;
        }
        updateLedToBeEditedText();
    }

    @OnClick(R.id.button_copy)
    public void copyCurrentScene() {
        //Array copying
        for (int i = 0; i < 4; i++) {
            //this is how to make a deep copy of 2d array
            ledSceneCopy[i] = Arrays.copyOf(ledScene[i], ledScene[i].length);
        }
        //whe using this a shallow copy is made, we get only the reference to the original
        //System.arraycopy(ledScene, 0, ledSceneCopy, 0, ledScene.length);
    }

    @OnClick(R.id.button_paste)
    public void pasteToCurrentScene() {
        for (int i = 0; i < 4; i++) {
            //this is how to make a deep copy of 2d array
            ledScene[i] = Arrays.copyOf(ledSceneCopy[i], ledSceneCopy[i].length);
        }
        updateCurrentLedUI();
    }

    @OnClick(R.id.button_level1)
    public void level1pressed() {
        currentLevel = 0;
        updateCurrentLevelUI();
        updateCurrentLedUI();
    }

    @OnClick(R.id.button_level2)
    public void level2pressed() {
        currentLevel = 1;
        updateCurrentLevelUI();
        updateCurrentLedUI();
    }

    @OnClick(R.id.button_level3)
    public void level3pressed() {
        currentLevel = 2;
        updateCurrentLevelUI();
        updateCurrentLedUI();
    }

    @OnClick(R.id.button_level4)
    public void level4pressed() {
        currentLevel = 3;
        updateCurrentLevelUI();
        updateCurrentLedUI();
    }

    @OnClick(R.id.button_led1)
    public void led1pressed() {
        if (ledScene[currentLevel][0] == 1) {
            buttonLed1.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][0] = 0;
        } else {
            buttonLed1.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][0] = 1;
        }
    }

    @OnClick(R.id.button_led2)
    public void led2pressed() {
        if (ledScene[currentLevel][1] == 1) {
            buttonLed2.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][1] = 0;
        } else {
            buttonLed2.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][1] = 1;
        }
    }

    @OnClick(R.id.button_led3)
    public void led3pressed() {
        if (ledScene[currentLevel][2] == 1) {
            buttonLed3.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][2] = 0;
        } else {
            buttonLed3.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][2] = 1;
        }
    }

    @OnClick(R.id.button_led4)
    public void led4pressed() {
        if (ledScene[currentLevel][3] == 1) {
            buttonLed4.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][3] = 0;
        } else {
            buttonLed4.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][3] = 1;
        }
    }

    @OnClick(R.id.button_led5)
    public void led5pressed() {
        if (ledScene[currentLevel][4] == 1) {
            buttonLed5.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][4] = 0;
        } else {
            buttonLed5.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][4] = 1;
        }
    }

    @OnClick(R.id.button_led6)
    public void led6pressed() {
        if (ledScene[currentLevel][5] == 1) {
            buttonLed6.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][5] = 0;
        } else {
            buttonLed6.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][5] = 1;
        }
    }

    @OnClick(R.id.button_led7)
    public void led7pressed() {
        if (ledScene[currentLevel][6] == 1) {
            buttonLed7.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][6] = 0;
        } else {
            buttonLed7.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][6] = 1;
        }
    }

    @OnClick(R.id.button_led8)
    public void led8pressed() {
        if (ledScene[currentLevel][7] == 1) {
            buttonLed8.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][7] = 0;
        } else {
            buttonLed8.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][7] = 1;
        }
    }

    @OnClick(R.id.button_led9)
    public void led9pressed() {
        if (ledScene[currentLevel][8] == 1) {
            buttonLed9.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][8] = 0;
        } else {
            buttonLed9.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][8] = 1;
        }
    }

    @OnClick(R.id.button_led10)
    public void led10pressed() {
        if (ledScene[currentLevel][9] == 1) {
            buttonLed10.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][9] = 0;
        } else {
            buttonLed10.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][9] = 1;
        }
    }

    @OnClick(R.id.button_led11)
    public void led11pressed() {
        if (ledScene[currentLevel][10] == 1) {
            buttonLed11.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][10] = 0;
        } else {
            buttonLed11.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][10] = 1;
        }
    }

    @OnClick(R.id.button_led12)
    public void led12pressed() {
        if (ledScene[currentLevel][11] == 1) {
            buttonLed12.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][11] = 0;
        } else {
            buttonLed12.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][11] = 1;
        }
    }

    @OnClick(R.id.button_led13)
    public void led13pressed() {
        if (ledScene[currentLevel][12] == 1) {
            buttonLed13.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][12] = 0;
        } else {
            buttonLed13.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][12] = 1;
        }
    }

    @OnClick(R.id.button_led14)
    public void led14pressed() {
        if (ledScene[currentLevel][13] == 1) {
            buttonLed14.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][13] = 0;
        } else {
            buttonLed14.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][13] = 1;
        }
    }

    @OnClick(R.id.button_led15)
    public void led15pressed() {
        if (ledScene[currentLevel][14] == 1) {
            buttonLed15.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][14] = 0;
        } else {
            buttonLed15.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][14] = 1;
        }
    }

    @OnClick(R.id.button_led16)
    public void led16pressed() {
        if (ledScene[currentLevel][15] == 1) {
            buttonLed16.setBackground(getResources().getDrawable(R.drawable.ripple));
            ledScene[currentLevel][15] = 0;
        } else {
            buttonLed16.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
            ledScene[currentLevel][15] = 1;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_creator);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);

        ListView sceneListView = (ListView) findViewById(R.id.scene_list);
        mLedSceneArrayList = new ArrayList<LedScene>();
        mLedSceneAdapter = new LedSceneAdapter(this, mLedSceneArrayList);
        sceneListView.setAdapter(mLedSceneAdapter);
        sceneListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int sum = 0;

                LedScene selectedScene = mLedSceneAdapter.getItem(position);
                currentSceneNumber = selectedScene.getSceneNumber();
                byte[] selectedSceneBytes = selectedScene.getLevels();
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 16; j++) {
                        sum += ledSceneCopy[i][j];
                    }
                }
                createSceneUIfromBytes(selectedSceneBytes);

                updateCurrentLedUI();
                updateLedToBeEditedText();
                updateCurrentLevelUI();
                textViewSceneToBeEdited.setBackgroundColor(getResources().getColor(R.color.editing_scene_textview_color));

                Log.e("On item click", String.valueOf(sum));
            }
        });
        updateLedToBeEditedText();
        updateCurrentLevelUI();
    }

    private void updateLedToBeEditedText() {
        if (totalNumberOfScenes >= currentSceneNumber) {
            textViewSceneToBeEdited.setText("Currently edditing scene number: " + currentSceneNumber);
        } else {
            textViewSceneToBeEdited.setText("Scene to be created: " + currentSceneNumber);
        }
    }

    private void createSceneUIfromBytes(byte[] sceneBytes) {
//        for (int i = 0; i < 8; i++){
//            Log.e("DEBUG: ", sceneBytes[i] + "");
//        }
        int byteCounter = 0;
        int shiftCounter = 0;
        for (int x = 0; x < 4; x++) {
            //loop through levels
            for (int y = 0; y < 16; y++) {
                ledScene[x][y] = (sceneBytes[byteCounter] >> shiftCounter) & 1;
                shiftCounter++;
                if (shiftCounter > 7) {
                    //Log.e("DEBUG: ", sceneBytes[byteCounter] + "");
                    shiftCounter = 0;
                    byteCounter++;
                }
            }
        }
    }

    private void updateCurrentLedUI() {
        if (ledScene[currentLevel][0] == 1) {
            buttonLed1.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed1.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][1] == 1) {
            buttonLed2.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed2.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][2] == 1) {
            buttonLed3.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed3.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][3] == 1) {
            buttonLed4.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed4.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][4] == 1) {
            buttonLed5.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed5.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][5] == 1) {
            buttonLed6.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed6.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][6] == 1) {
            buttonLed7.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed7.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][7] == 1) {
            buttonLed8.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed8.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][8] == 1) {
            buttonLed9.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed9.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][9] == 1) {
            buttonLed10.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed10.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][10] == 1) {
            buttonLed11.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed11.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][11] == 1) {
            buttonLed12.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed12.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][12] == 1) {
            buttonLed13.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed13.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][13] == 1) {
            buttonLed14.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed14.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][14] == 1) {
            buttonLed15.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed15.setBackground(getResources().getDrawable(R.drawable.ripple));
        }
        if (ledScene[currentLevel][15] == 1) {
            buttonLed16.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
        } else {
            buttonLed16.setBackground(getResources().getDrawable(R.drawable.ripple));
        }


    }

    private void updateCurrentLevelUI() {
        switch (currentLevel) {
            case 0:
                buttonLevel1.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
                buttonLevel2.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel3.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel4.setBackground(getResources().getDrawable(R.drawable.ripple));
                break;
            case 1:
                buttonLevel1.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel2.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
                buttonLevel3.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel4.setBackground(getResources().getDrawable(R.drawable.ripple));
                break;
            case 2:
                buttonLevel1.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel2.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel3.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
                buttonLevel4.setBackground(getResources().getDrawable(R.drawable.ripple));
                break;
            case 3:
                buttonLevel1.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel2.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel3.setBackground(getResources().getDrawable(R.drawable.ripple));
                buttonLevel4.setBackground(getResources().getDrawable(R.drawable.ripple_led_on));
                break;
            default:
                Toast.makeText(this, "currentLevel error", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pattern_creator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                createSavePatternDialog();
                return true;
            case R.id.action_delete:
                createDeletePatternDialog();
                return true;
            case R.id.action_open:
                createOpenPatternDialog();
                return true;
            case R.id.action_clear_pattern:
                clearPattern();
                return true;
            case R.id.action_set_time:
                createSetTimeDialog();
                return true;
            case R.id.action_exit_creator:
                finish();
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createSetTimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set time for scene in seconds");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);
        input.setText(String.valueOf((double) sceneTimeMillis / 1000));

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing here because this gets overriden later
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        //override on OK button click, so dialog does not close when invalid time value is given
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double inputValueInSeconds = Double.valueOf(input.getText().toString());
                if (inputValueInSeconds >= 0.1 && inputValueInSeconds <= 60) {
                    double inputValueInMillis = inputValueInSeconds * 1000;
                    sceneTimeMillis = (int) inputValueInMillis;
                    dialog.dismiss();
                } else {
                    Toast.makeText(PatternCreator.this, "Allowed range:\n0.1 to 60 seconds",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void clearPattern() {
        mLedSceneAdapter.clear();
        for (int x = 0; x < 4; x++) {
            //loop through levels
            for (int y = 0; y < 16; y++) {
                ledScene[x][y] = 0;
            }
        }
        currentLevel = 0;
        totalNumberOfScenes = 0;
        currentSceneNumber = 1;
        updateCurrentLevelUI();
        updateCurrentLedUI();
        updateLedToBeEditedText();
    }

    private void createDeletePatternDialog() {
        File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
        String fileList[] = patternDirectory.list();
        int numberOfFiles = fileList.length;
        if (numberOfFiles > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose pattern file to delete:");

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
                    deletePatternFile(arrayAdapter.getItem(which));
                }
            });
            builder.show();

        } else {
            Toast.makeText(PatternCreator.this, "No pattern files saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePatternFile(String fileName) {
        File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
        File patternFile = new File(patternDirectory + "/" + fileName);
        if (patternFile.delete()) {
            Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete fail", Toast.LENGTH_SHORT).show();
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
                    //clear current pattern
                    mLedSceneAdapter.clear();
                    new openPatternTask().execute(arrayAdapter.getItem(which));
                }
            });
            builder.show();

        } else {
            Toast.makeText(PatternCreator.this, "No pattern files saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void createSavePatternDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        if (!fileNameTemp.isEmpty()) {
            input.setText(fileNameTemp);
        }

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fileNameTemp = input.getText().toString();
                File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
                String folderPath = patternDirectory.getPath();
                //do the saving in a background thread
                //possibly not necesseray
                new SavePatternTask().execute(folderPath);
//                try {
//                    File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
//                    String folderPath = patternDirectory.getPath();
//                    //FileOutputStream outputStream = openFileOutput(folderPath + "/" + fileNameTemp, Context.MODE_PRIVATE);
//                    //thissyntax is needed because a subfolder of the internal memory is used
//                    FileOutputStream outputStream = new FileOutputStream(new File(folderPath + "/" + fileNameTemp));
//                    //Stream writer is used for encoding - to read write characters, not good her because bytes are written
//                    //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "utf8");
//                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
//                    //write all patterns to the file
//                    int numberOfScenes = mLedSceneAdapter.getCount();
//                    for (int i = 0; i < numberOfScenes; i++){
//                        bufferedOutputStream.write(mLedSceneAdapter.getItem(i).getBytes());
//                    }
//                    bufferedOutputStream.close();
//                    outputStream.close();
//                    Toast.makeText(PatternCreator.this, "Save successful", Toast.LENGTH_SHORT).show();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private class SavePatternTask extends AsyncTask<String, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                //FileOutputStream outputStream = openFileOutput(folderPath + "/" + fileNameTemp, Context.MODE_PRIVATE);
                //thissyntax is needed because a subfolder of the internal memory is used
                FileOutputStream outputStream = new FileOutputStream(new File(strings[0] + "/" + fileNameTemp));
                //Stream writer is used for encoding - to read write characters, not good her because bytes are written
                //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "utf8");
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                //write all patterns to the file
                int numberOfScenes = mLedSceneAdapter.getCount();
                for (int i = 0; i < numberOfScenes; i++) {
                    bufferedOutputStream.write(mLedSceneAdapter.getItem(i).getBytes());
                }
                bufferedOutputStream.close();
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(PatternCreator.this, "Save successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PatternCreator.this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }

    private class openPatternTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mLedSceneAdapter.notifyDataSetChanged();
                Toast.makeText(PatternCreator.this, "File read correctly", Toast.LENGTH_SHORT).show();
                updateLedToBeEditedText();
            } else {
                Toast.makeText(PatternCreator.this, "File read error", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            File patternDirectory = getDir("Patterns", Context.MODE_PRIVATE);
            //this will be changed to true if succesfull file read
            boolean result = false;
            try {
                //save the opened file name so it can be saved with the same name
                fileNameTemp = strings[0];

                //this syntax because the file is in a folder
                FileInputStream fileInputStream = new FileInputStream(new File(patternDirectory + "/" + fileNameTemp));
                //InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "utf8");
                //BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                //int readArrayLenght;
                currentSceneNumber = 1;
                totalNumberOfScenes = 0;
                byte[] tempArray = new byte[12];
                int ofset = 0;
                int numberOfReadBytes = 0;
                while (true) {
                    numberOfReadBytes = bufferedInputStream.read(tempArray, ofset, 12);
                    if (numberOfReadBytes == 12) {
                        mLedSceneArrayList.add(new LedScene(tempArray, currentSceneNumber));
                        currentSceneNumber++;
                        totalNumberOfScenes++;
                    } else if (numberOfReadBytes == -1) {
                        result = true;
                        break;
                    } else {
                        Log.e("File open error", "incorrect number of bytes");
                        break;
                    }
                }
                bufferedInputStream.close();
                fileInputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return result;
        }
    }

}
