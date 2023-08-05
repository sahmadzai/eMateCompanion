package com.emate.ematecompanion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class DebugActivity extends AppCompatActivity {

    /**
     * Class variables that are used to store the different values that are selected by the user
     * and some other constant values.
     * Speed conversions:
     * MPH  | KPH
     * 03    |	05
     * 04    |	06
     * 05    |	08
     * 06    |	10
     * 07    |	11
     * 08    |	13
     * 09    |	14
     * 10    |	16
     * 11    |	18
     * 12    |	19
     * 13    |	21
     * 14    |	23
     * 15    |	24
     * 16    |	26
     * 17    |	27
     * 18    |	29
     * 19    |	31
     * 20    |	32
     */
    private double conversionFactor = 1.609;
    private int max_speed = 15; // In kph, default 15mph or 24 kph
    private int current_speed = 0; // In kph, default 0 mph/kph
    private final int LENGTH_SHORT = 800;
    private final String LOG_TAG = "DebugActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_page);

        /* Setting View(s) variables */
        TextView maxSpeedLabel = (TextView) findViewById(R.id.setSpeed);
        TextView currentSpeedLabel = (TextView) findViewById(R.id.speed);
        TextView headlightStatusLabel = (TextView) findViewById(R.id.headlight_status);
        Button headlight_switch = (Button) findViewById(R.id.headlight_switch);

        // Setup and start the seekbar listener
        initSeekBar(maxSpeedLabel);

        // Setup the default values for all fields
        initFields(maxSpeedLabel, currentSpeedLabel, headlightStatusLabel);

        // Setup the toggle button(s)
        initToggleButtons(headlight_switch, headlightStatusLabel);
    }

    private void initFields(TextView maxSpeedLabel, TextView currentSpeedLabel, TextView headlightStatusLabel) {
        maxSpeedLabel.setText(getString(R.string.setSpeed_value, max_speed));
        currentSpeedLabel.setText(getString(R.string.speed_value, current_speed));
        Log.v(LOG_TAG, "Setting headlight status");
        headlightStatusLabel.setText(getString(R.string.headlight_value, "OFF"));
    }

    /**
     * Method that sets up the seekbar and starts the listener for it.
     * The listener updates the skill level variable when the user changes the value of the seekbar.
     */
    private void initSeekBar(TextView maxSpeedLabel) {
        // Initialize seekbar
        SeekBar skillSeekBar = findViewById(R.id.setSpeed_slider);
        skillSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                max_speed = i+3;
                Log.v(LOG_TAG, "Max Speed is now: " + max_speed);
                String text = "Max Speed is now: " + max_speed;
                Snackbar.make(seekBar, text, Snackbar.LENGTH_SHORT).setDuration(LENGTH_SHORT).show();

                // Set the Max Speed Field

                maxSpeedLabel.setText(getString(R.string.setSpeed_value, max_speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * This method sets up the toggle buttons that allow the user to show the solution path, full map
     * of the maze, or the walls of the maze. It also sets up a click listener for each button that
     * logs the event.
     */
    private void initToggleButtons(Button headlight_switch, TextView headlightStatusLabel) {
        headlight_switch.setOnClickListener(view -> {
            if(headlight_switch.getText().equals(getString(R.string.headlight_status_ON))) {
                headlight_switch.setText(R.string.headlight_status_OFF);
                headlightStatusLabel.setText(getString(R.string.headlight_value, "ON"));
                headlight_switch.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
                Log.v(LOG_TAG, "Headlights ON");
            } else {
                headlight_switch.setText(R.string.headlight_status_ON);
                headlightStatusLabel.setText(getString(R.string.headlight_value, "OFF"));
                headlight_switch.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));
                Log.v(LOG_TAG, "Headlights OFF");
            }
        });
    }

}