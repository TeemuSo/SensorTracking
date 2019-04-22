package com.example.teemu.trackingapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;


/**
 * This class is for movement control.
 * It's responsible for moving the car, controlled by buttons
 *
 * @author Teemu Sormunen
 */
public class MovementControl extends Fragment implements View.OnTouchListener {

    // Create UI variables
    ImageButton btnForward, btnBackward, btnRight, btnLeft;

    MovementControlInterface movementControlInterface;  // Create interface for movement control


    /**
     * Create interface for communication between main activity
     */
    public interface MovementControlInterface {

        /**
         * Movement control of the mobile robot
         *
         * @param control Possible values:
         *                1 move forward
         *                2 move backward
         *                3 move right
         *                4 move left
         *                stop Stop movements
         */
        void controlMovement(String control);

        /**
         * Initialize movement fragment's UI variables
         *
         * @param btnForward  button: drive forward
         * @param btnBackward button: drive backward
         * @param btnRight    button: drive right
         * @param btnLeft     button: drive left
         */
        void initUIMovement(ImageButton btnForward, ImageButton btnBackward, ImageButton btnRight, ImageButton btnLeft);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.movement_control_fragment, container, false);
        initVar(view);
        movementControlInterface.initUIMovement(btnForward, btnBackward, btnRight, btnLeft);
        return view;
    }

    /**
     * Initialize variables
     *
     * @param v view of the fragment.
     */
    @SuppressLint("ClickableViewAccessibility")
    public void initVar(View v) {
//        this.velocityBar = v.findViewById(R.id.velocity_bar);
        btnBackward = v.findViewById(R.id.btn_backwards);
        btnForward = v.findViewById(R.id.btn_forward);
        btnLeft = v.findViewById(R.id.btn_left);
        btnRight = v.findViewById(R.id.btn_right);

        btnBackward.setOnTouchListener(this);
        btnForward.setOnTouchListener(this);
        btnRight.setOnTouchListener(this);
        btnLeft.setOnTouchListener(this);
    }


    /**
     * Sense buttons up and down events.
     *
     * @param v     Find correct button pressed
     * @param event Find correct event. Can be PRESSED or RELEASED
     * @return true
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Debug.print("TAG", "onTouch called", 2, "console");
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (v.getId()) {
                    case R.id.btn_forward:
                        movementControlInterface.controlMovement("1");
                        break;
                    case R.id.btn_backwards:
                        movementControlInterface.controlMovement("2");
                        break;
                    case R.id.btn_right:
                        movementControlInterface.controlMovement("3");
                        break;
                    case R.id.btn_left:
                        movementControlInterface.controlMovement("4");
                }
                break;
            case MotionEvent.ACTION_UP:
                movementControlInterface.controlMovement("stop");
                v.performClick();   // When click is done, perform the click
                break;
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        try {
            movementControlInterface = (MovementControl.MovementControlInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must override onDataSend");
        }
    }

    /**
     * Force orientation portrait for better user experience
     *
     * @param isVisibleToUser when is the fragment visible to user
     */
    // Set orientation portrait
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Activity curActivity = getActivity();
            if (curActivity != null) {
                curActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }
}
