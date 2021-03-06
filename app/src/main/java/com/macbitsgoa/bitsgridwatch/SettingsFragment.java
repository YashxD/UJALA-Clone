package com.macbitsgoa.bitsgridwatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends Fragment {

    private TextView usernameTextView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private AlertDialog alertDialog;
    private AlertDialog.Builder alertDialogBuilder;

    private String TAG = "Settings Fragment";

    //shared preferences for theme
    private SharedPreferences theme_shared_preferences;
    private  SharedPreferences.Editor theme_editor;

    private Switch allowSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        //-------------------------------
        //Initialize layout elements here
        //-------------------------------
        usernameTextView = view.findViewById(R.id.textview_username_settings);
        Button signOutButton = view.findViewById(R.id.button_signout_settings);
        Button signInButton = view.findViewById(R.id.button_signin_settings);
        allowSwitch = view.findViewById(R.id.switch_monitor_settings);
        sharedPreferences = getContext().getSharedPreferences("AllowMoniSharedPref", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        //shared preferences for theme
        theme_shared_preferences  = getActivity().getSharedPreferences("ThemeOptions",MODE_PRIVATE);
        theme_editor = theme_shared_preferences.edit();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!((MainActivity) getActivity()).getSignedInStatus()) {
                    ((MainActivity) getActivity()).userSignIn();
                    updateUser();
                } else
                    Toast.makeText(getContext(), "Already Signed In!", Toast.LENGTH_SHORT).show();
            }
        });
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).userSignOut();
                usernameTextView.setText(R.string.guest);
            }
        });

        allowSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "entered onClick allowSwitch");
                boolean switchState = allowSwitch.isChecked();
                //editor.clear();

                if (switchState) {
                    Log.d(TAG, "allowSwitch = TRUE");
                    //Display dialog box to check if verified.
                    //Start background work only if user accepts.
                    //Reset to false state if user declines.
                    alertDialogBuilder = new AlertDialog.Builder(getContext());
                    alertDialogBuilder.setMessage(R.string.disclaimer);
                    alertDialogBuilder.setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((MainActivity) Objects.requireNonNull(getActivity())).setAllowMonitoring(true);
                            ((MainActivity) Objects.requireNonNull(getActivity())).startBackgroundWork();
                            updateAllowSwitchState();
                        }
                    });
                    alertDialogBuilder.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((MainActivity) Objects.requireNonNull(getActivity())).setAllowMonitoring(false);
                            ((MainActivity) Objects.requireNonNull(getActivity())).cancelBackgroundWork();
                            updateAllowSwitchState();
                        }
                    });
                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {
                    Log.d(TAG, "allowSwitch = FALSE");
                    ((MainActivity) Objects.requireNonNull(getActivity())).setAllowMonitoring(false);
                    ((MainActivity) Objects.requireNonNull(getActivity())).cancelBackgroundWork();
                }
                //Log.d(TAG, "Updating switch state");
            }
        });

        boolean switchState = sharedPreferences.getBoolean("allow", true);
        ((MainActivity) Objects.requireNonNull(getActivity())).setAllowMonitoring(switchState);
        allowSwitch.setChecked(switchState);


        //theme option

        TextView theme_select = view.findViewById(R.id.theme_select);

        CharSequence[] app_themes = {"Light","Dark","Set by Batter Saver"};

        theme_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog alertDialog = null;

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Select App Theme");
                builder.setSingleChoiceItems(app_themes, theme_shared_preferences.getInt("Theme",0) - 1, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {

                        switch(item)
                        {
                            case 0:

//                                Toast.makeText(getContext(), "Light Theme Selected", Toast.LENGTH_LONG).show();
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                theme_editor.putInt("Theme",AppCompatDelegate.MODE_NIGHT_NO);
                                theme_editor.commit();
                                break;
                            case 1:

//                                Toast.makeText(getContext(), "Dark Theme Selected", Toast.LENGTH_LONG).show();
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                theme_editor.putInt("Theme",AppCompatDelegate.MODE_NIGHT_YES);
                                theme_editor.commit();
                                break;
                            case 2:

//                                Toast.makeText(getContext(), "Theme Set by Battery Saver Selected", Toast.LENGTH_LONG).show();
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                                theme_editor.putInt("Theme",AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                                theme_editor.commit();
                                break;
                        }
                        dialog.dismiss();
                    }
                });
                alertDialog = builder.create();
                alertDialog.show();


            }
        });



        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        //Get last signed in user
        updateUser();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUser();
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private void updateUser() {
        GoogleSignInAccount userAccount = ((MainActivity) getActivity()).getUserAccount();
        if (userAccount != null)
            usernameTextView.setText(userAccount.getDisplayName());
        else
            usernameTextView.setText(R.string.guest);
    }

    private void updateAllowSwitchState(){
        Boolean switchState = ((MainActivity) Objects.requireNonNull(getActivity())).getAllowMonitoring();
        allowSwitch.setChecked(switchState);
        editor.putBoolean("allow", switchState);
        editor.commit();
    }
}