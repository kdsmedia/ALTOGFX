package com.altomedia.altogfx;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Rack extends Fragment {
    MainActivity mainActivity ;
    LinearProgressIndicator quickPatchProgress ;
    String TAG = getClass().getSimpleName();
    PopupMenu optionsMenu ;
    LinearLayout mixer ;
    Button patchUp, patchDown ;
    JSONObject jsonObject = new JSONObject();
    LinearLayout rackMaster, pane_2 ;
    boolean mixerInit = false ;
    boolean autoHideMixer = true ;
    /*
    Rack () {
        mainActivity = (MainActivity) getActivity();
    }

    Rack (MainActivity activity) {
        mainActivity = activity ;
    }

     */

    public void printPlugins () {
        Log.d(TAG, "printPlugins: " + jsonObject.toString());
    }

    public void writeJSON () {
        File file = new File(mainActivity.dir, "plugins.json");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            stream.write(jsonObject.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        setRetainInstance(true);
        return inflater.inflate(R.layout.rack,null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        if (mainActivity.pluginDialog != null) {
            // we did this already
            return ;
        }

        quickPatchProgress = mainActivity.findViewById(R.id.patch_loading);

        mainActivity.onOff = view.findViewById(R.id.onoff);
        mainActivity.onOff.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(mainActivity.onOff, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });

        rackMaster = view.findViewById(R.id.rack_master);
        pane_2 = view.findViewById(R.id.pane_2);
        /*
        mainActivity.onOff.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey() called with: v = [" + v + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                return mainActivity.hotkeys(keyCode, event);
            }
        });

         */

        mainActivity.onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainActivity.toggleEffect(!b);
            }
        });

        patchDown = mainActivity.findViewById(R.id.patch_down);
        mainActivity.pluginDialog = mainActivity.createPluginDialog();
        if (mainActivity.useTheme)
            mainActivity.linearLayoutPluginDialog.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card(mainActivity.linearLayoutPluginDialog);

                }
            });

        mainActivity.tuner = mainActivity.findViewById(R.id.patch_label);
        Switch tunerSwitch = mainActivity.findViewById(R.id.tuner_switch);
        tunerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView _v = (TextView) mainActivity.tuner ;
                _v.setText("Please wait ...");
                mainActivity.tunerEnabled = isChecked;
                if (!mainActivity.tunerEnabled) {
                    AudioEngine.setTunerEnabled(false);
                    mainActivity.tuner.setText("Tuner");
                } else {
                    if (! mainActivity.onOff.isChecked()) {
                        mainActivity.toast ("Starting audio engine");
                        patchDown.performClick();
                    }
                    AudioEngine.setTunerEnabled(true);
                }

            }
        });

        RecyclerView recyclerView1 = (RecyclerView) mainActivity.linearLayoutPluginDialog.findViewById(R.id.plugin_dialog_recycler_view);
        recyclerView1.setLayoutManager(new LinearLayoutManager(mainActivity));
        mainActivity.pluginDialogAdapter = new PluginDialogAdapter();
        mainActivity.pluginDialogAdapter.setMainActivity(getContext(), mainActivity);
        recyclerView1.setAdapter(mainActivity.pluginDialogAdapter);

        mainActivity.fab = view.findViewById(R.id.fab);
        mainActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                linearLayout.setBackground();
                MainActivity.applyWallpaper(mainActivity, mainActivity.pluginDialog.getWindow(), getResources(), mainActivity.pluginDialogWallpaper, mainActivity.deviceWidth, mainActivity.deviceHeight);
                mainActivity.pluginDialog.show();
            }
        });

        mainActivity.recyclerView = view.findViewById(R.id.recyclerView);
        mainActivity.recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (autoHideMixer && mixer.getVisibility() == View.VISIBLE) {
                    mainActivity.hidePanel.performClick();
                    autoHideMixer = false;
                    mainActivity.recyclerView.getLayoutManager().scrollToPosition(0);
                }
                return false;
            }
        });

        mainActivity.recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        mainActivity.dataAdapter = new DataAdapter();
        mainActivity.dataAdapter.mainActivity = mainActivity ;
        mainActivity.recyclerView.setAdapter(mainActivity.dataAdapter);

        File dir = Environment.getExternalStorageDirectory();
        String path = dir.getAbsolutePath();

        AudioEngine.setExternalStoragePath(path);
        File defaultDir = new File (path + "/AmpRack/") ;
        if (!defaultDir.exists()) {
            Log.d(TAG, "making directory " + path + "/AmpRack/");
            try {
                if (!defaultDir.mkdir())
                    Log.wtf (TAG, "Unable to create directory!");
            }  catch (Exception e) {
                Log.w(TAG, "UNable to create directory: " + e.getMessage());
            }
        }
        AudioEngine.setDefaultStreamValues(getContext());
        if (mainActivity.lazyLoad == false)
            mainActivity.loadPlugins();
        mainActivity.loadActivePreset();

        int libraries = 0 ;
        if (mainActivity.lazyLoad == false)
            libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");

        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(mainActivity, "blacklist.json");
        boolean enableBlacklisted = mainActivity.defaultSharedPreferences.getBoolean("enableBlacklisted", false);

        // run this only once
        if (mainActivity.pluginDialogAdapter.plugins.size() == 0 && mainActivity.lazyLoad == false) {
            for (int i = 0; i < libraries; i++) {
                for (int plugin = 0; plugin < AudioEngine.getPlugins(i); plugin++) {
                    JSONObject object = new JSONObject();
                    String name = AudioEngine.getPluginName(i, plugin);
                    int uniqueID = AudioEngine.getPluginUniqueID(i, plugin);

                    int finalI = i;
                    int finalPlugin = plugin;

                    try {
                        object.put("name", name);
                        object.put("id", uniqueID);
                        object.put("plugin", finalPlugin);
                        object.put("library", mainActivity.sharedLibraries[i]);
                        jsonObject.put(String.valueOf((finalI * 100) + finalPlugin), object);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.d("Plugin: ", finalI * 100 + finalPlugin + ": " + name + "=>" + uniqueID);
                    mainActivity.pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name, uniqueID, name);
                }
            }
        } else if (mainActivity.pluginDialogAdapter.plugins.size() == 0 && mainActivity.lazyLoad) {
            JSONObject plugins = mainActivity.availablePluginsLV2 ;
            Iterator<String> keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (blacklist.has(key) && ! enableBlacklisted) {
                        Log.d(TAG, String.format ("[lv2] blacklisted: %s", key));
                        continue;
                    }

                    if (plugins.get(key) instanceof JSONObject) {
//                        Log.d(TAG, "onCreate: key " + key);
                        JSONObject object = plugins.getJSONObject(key);
                        // do something with jsonObject here
                        String name = object.getString("name");
                        String id = object.getString("id");
                        String desc = object.getString("desc");
                        mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id), desc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            plugins = mainActivity.availablePlugins ;
            keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (plugins.get(key) instanceof JSONObject) {
//                        Log.d(TAG, "onCreate: key " + key);
                        JSONObject object = plugins.getJSONObject(key);
                        // do something with jsonObject here
                        String name = object.getString("name");
                        String desc = object.getString("desc");
                        String id = object.getString("id");
                        if (blacklist.has(id) && ! enableBlacklisted) {
                            Log.d(TAG, String.format ("[ladspa] blacklisted: %s", key));
                            continue;
                        }
//                        Log.d(TAG, "[LV2 plugin]: " + name + ": " + id);
                        mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id), desc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        MainActivity.totalPlugins = mainActivity.pluginDialogAdapter.plugins.size();
        mainActivity.hashCommands.add (this, "printPlugins");
        mainActivity.hashCommands.add (this, "writeJSON");

        MaterialButton optionsBtn = view.findViewById(R.id.menu_button);
        optionsMenu = new PopupMenu(mainActivity, optionsBtn);
        optionsMenu.getMenuInflater().inflate(R.menu.options_menu, optionsMenu.getMenu());

        MenuItem settings = optionsMenu.getMenu().findItem(R.id.settings_menu_item);
        MenuItem debug = optionsMenu.getMenu().findItem(R.id.debug_menu_item);

        TextView textView = view.findViewById(R.id.app_main_title);
        textView.setText("Pro");
        (view.findViewById(R.id.pro_label)).setVisibility(View.VISIBLE);

        MenuItem connectGuitar = optionsMenu.getMenu().findItem(R.id.connect_guitar_menu);
        connectGuitar.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getActivity(), ConnectGuitar.class);
                startActivity(intent);

                return true;
            }
        });

        MenuItem saveCollection = optionsMenu.getMenu().findItem(R.id.save_collection);
        saveCollection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mainActivity.saveCollection();
                return false;
            }
        });

        MenuItem loadCollection = optionsMenu.getMenu().findItem(R.id.load_collection);
        loadCollection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent_upload = new Intent();
                intent_upload.setType("*/*");
                intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity().startActivityForResult(intent_upload,100);
                return true;
            }
        });

        MenuItem tuneLatency = optionsMenu.getMenu().findItem(R.id.menu_tune);
        tuneLatency.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                latencyDialog();
                return false;
            }
        });

        MenuItem clear_midi = optionsMenu.getMenu().findItem(R.id.clear_midi);
        clear_midi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                mainActivity.midiControls.clear();
                Toast.makeText(mainActivity, "MIDI Controls cleared", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        mainActivity.midiDisplay = mainActivity.findViewById(R.id.midi_display);
        MenuItem bt_menu = optionsMenu.getMenu().findItem(R.id.bt_menu);
        bt_menu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                mainActivity.scanBLE();
                return false;
            }
        });

        MenuItem midiSelect = optionsMenu.getMenu().findItem(R.id.menu_select_midi);
        midiSelect.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                mainActivity.selectMidiDevice();
                return false;
            }
        });

        MenuItem exit_item = optionsMenu.getMenu().findItem(R.id.menu_exit);
        exit_item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mainActivity.finish();
                return true;
            }
        });

        debug.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                AudioEngine.clearActiveQueue();
                mainActivity.dataAdapter.reset();
                mainActivity.lastPresetLoadedUID = null ;
                mainActivity.lastPresetLoadedPath = null ;
                mainActivity.patchName.setText("");
                mainActivity.patchDesc.setText("");
                return true;
            }
        });

        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                return false;
            }
        });


        optionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsMenu.show();
            }
        });

        TextView midi_map = mainActivity.findViewById(R.id.midi_port_mappings);
        midi_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.midiMappingsDialog();;
            }
        });

        TextView midiDevice = mainActivity.findViewById(R.id.midi_port_button);
        midiDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.selectMidiDevice();
            }
        });

        ((ImageView) mainActivity.findViewById(R.id.midi_bt_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.scanBLE();
            }
        });

        ImageView logoBtn = view.findViewById(R.id.logo_img) ;
        /*
        logoBtn.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey() called with: v = [" + v + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                if (event.getAction() == KeyEvent.ACTION_UP)
                    mainActivity.hotkeys(keyCode, event);
                return false;
            }
        });

         */

        logoBtn.setLongClickable(true);
        logoBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                HashCommands commands = new HashCommands(mainActivity.context);
                commands.mainActivity = mainActivity;
                commands.show();
                return true;
            }
        });

        logoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.setAudioDevice();
            }
        });

        LinearLayout inputMixer = mainActivity.findViewById(R.id.mixer_input);
        LinearLayout outputMixer = mainActivity.findViewById(R.id.mixer_output);
        mixer = mainActivity.findViewById(R.id.mixer);

        mainActivity. toggleMixer = mainActivity.findViewById(R.id.mixer_toggle);
        mainActivity.toggleMixer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });

        mainActivity.hidePanel = mainActivity.findViewById(R.id.hide_panel);
        mainActivity.hidePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.toggleMixer.setChecked(!mainActivity.toggleMixer.isChecked());
            }
        });

        mainActivity.toggleMixer.setOnCheckedChangeListener((compoundButton, b) -> {
            Log.d(TAG, "onViewCreated() called with: view = [" + view + "], savedInstanceState = [" + savedInstanceState + "]");
            AudioEngine.toggleMixer(!b);
            if (mainActivity.useTheme)
                mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", !b);
            if (!b) {

                if (! mixerInit) {
                    mixer.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "run: theming mixer");
                            if (mainActivity.useTheme)
                                mainActivity.skinEngine.card(mixer);

                        }
                    });
                }
                mixer.setVisibility(View.VISIBLE);
                ((TextView) mainActivity.findViewById(R.id.m_on)).setVisibility(View.VISIBLE);
                ((TextView) mainActivity.findViewById(R.id.m_mixer)).setVisibility(View.VISIBLE);
            } else {
                mixer.setVisibility(GONE);
                ((TextView) mainActivity.findViewById(R.id.m_on)).setVisibility(GONE);
                ((TextView) mainActivity.findViewById(R.id.m_mixer)).setVisibility(GONE);
            }
        });

        mainActivity.toggleMixer.setChecked(mainActivity.defaultSharedPreferences.getBoolean("toggleMixer", false));
        Log.d(TAG, "onViewCreated: toggle mixer: " + mainActivity.defaultSharedPreferences.getBoolean("toggleMixer", false));
        mainActivity. inputVolume = mainActivity.findViewById(R.id.mixer_input_slider);
        mainActivity. outputVolume = mainActivity.findViewById(R.id.mixer_output_slider);

        mainActivity.inputMeter = mainActivity.findViewById(R.id.mixer_input_progress);
        mainActivity.outputMeter = mainActivity.findViewById(R.id.mixer_output_progress);
        mainActivity.inputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        mainActivity.outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

        mainActivity. inputVolume.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                AudioEngine.setInputVolume(value);
            }
        });

        mainActivity. outputVolume.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                AudioEngine.setOutputVolume(value);
            }
        });

        mainActivity.inputVolume.setValue(mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f));
        mainActivity.outputVolume.setValue(mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f));

        ToggleButton toggleButton = mainActivity.findViewById(R.id.onofftoggle);
        toggleButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(toggleButton, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });

        patchUp = mainActivity.findViewById(R.id.patch_up);
        patchUp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });

        TextView patchName = mainActivity.findViewById(R.id.patch_name),
                patchNo = mainActivity.findViewById(R.id.patch_no);

        TextView patchDesc = mainActivity.findViewById(R.id.patch_desc);
        mainActivity.patchDesc = patchDesc ;
        mainActivity.patchName = patchName ;
        mainActivity.patchNo = patchNo;

        patchDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patchName.performClick();
            }
        });

        LinearLayout patchMaster = mainActivity.findViewById(R.id.patch_master);
        patchMaster.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (patchName.getText().equals("Tap to load"))
                patchName.performClick();
                return false;
            }
        });

        patchName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (! patchName.getText().equals("Tap to load"))
//                    return false;

                if (! mainActivity.running) {
                    if (patchName.getText().equals("Tap to load") && mainActivity.dataAdapter.totalItems == 0) {
                        MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                            @Override
                            void run() {
                                patchDown.performClick();
                            }
                        };
                    } else if (patchName.getText().equals("Tap to load")){
                        patchName.setText("Custom");
                    }

                    if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                        mainActivity.onOff.setChecked(true);

                    else if (mainActivity.useTheme && ! toggleButton.isChecked())
                        toggleButton.setChecked(true);

                } else {
                    if (patchName.getText().equals("Tap to load"))
                        patchDown.performClick();
                }
                return false;
            }
        });
        patchUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                        @Override
                        void run() {
                            patchMove(true);
                        }
                    };

                } else {
                    patchMove(true);
                    return;
                }

                if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                    mainActivity.onOff.setChecked(true);
                else if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);

            }
        });

        patchDown.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });
        patchDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                        @Override
                        void run() {
                            patchMove(false);
                        }
                    };
                } else {
                    patchMove(false);
                    return;
                }

                if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                    mainActivity.onOff.setChecked(true);
                else if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);
            }
        });

        TextView mixerLabel = mainActivity.findViewById(R.id.mixer_label),
                inLabel = mainActivity.findViewById(R.id.mixer_input_label),
                patchLabel = mainActivity.findViewById(R.id.patch_label),
                inRotaryDisplay = mainActivity.findViewById(R.id.rotary_input_display),
                inRotaryLabel = mainActivity.findViewById(R.id.rotary_input_label),
                outRotaryDisplay = mainActivity.findViewById(R.id.rotary_output_display),
                outRotaryLabel = mainActivity.findViewById(R.id.rotary_output_label),
                outLabel = mainActivity.findViewById(R.id.mixer_output_label);


        if (mainActivity.skinEngine == null || ! mainActivity.skinEngine.hasKnob()) {
            inLabel.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mainActivity.setMidiControl(mainActivity.inputVolume, -1, -1, MIDIControl.Type.SLIDER, MIDIControl.Scope.GLOBAL);
                    return false;
                }
            });

            outLabel.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mainActivity.setMidiControl(mainActivity.outputVolume, -1, -1, MIDIControl.Type.SLIDER, MIDIControl.Scope.GLOBAL);
                    return false;
                }
            });
        }

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.cardText(mainActivity.patchDesc);
            mainActivity.skinEngine.cardText(mainActivity.hidePanel);
            mainActivity.toggleMixer.setCompoundDrawables(null,null,null,null);

            mainActivity.skinEngine.cardText(mixerLabel);
            mainActivity.skinEngine.cardText(patchName);
            mainActivity.skinEngine.cardText(inLabel);
            mainActivity.skinEngine.cardText(outLabel);
            mainActivity.skinEngine.cardText(patchLabel);
            mainActivity.skinEngine.cardText(patchNo);

            mainActivity.skinEngine.cardText(inRotaryDisplay);
            mainActivity.skinEngine.cardText(outRotaryDisplay);
            mainActivity.skinEngine.cardText(inRotaryLabel);
            mainActivity.skinEngine.cardText(outRotaryLabel);

            boolean mixerIsVisible  = false ;
            if (mixer.getVisibility() == View.VISIBLE)
                mixerIsVisible = true ;

            mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", mixerIsVisible);

            ImageView wallpaper = mainActivity.findViewById(R.id.wallpaper);
            mainActivity.skinEngine.wallpaper(wallpaper);
            mainActivity.skinEngine.header(mainActivity.findViewById(R.id.master_button_box));

            mainActivity.skinEngine.toggle(toggleButton, false);
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, "onCheckedChanged() called with: buttonView = [" + buttonView + "], isChecked = [" + isChecked + "]");
//                    mainActivity.toggleEffect(!isChecked);
                    mainActivity.onOff.setChecked(isChecked);
                    mainActivity.skinEngine.toggle(toggleButton, isChecked);
                }
            });

            toggleButton.setVisibility(View.VISIBLE);
            mainActivity.onOff.setVisibility(GONE);

            mainActivity.skinEngine.view (optionsBtn, "menu", "overflow", SkinEngine.Resize.Height, .5f);
            optionsBtn.setCompoundDrawables(null, null, null, null);
            mainActivity.skinEngine.setLogo(mainActivity.findViewById(R.id.logo_img));

            mainActivity.skinEngine.fab(mainActivity.fab,  SkinEngine.Resize.Width, 1);

            mainActivity.skinEngine.slider(mainActivity.inputVolume);
            mainActivity.skinEngine.slider(mainActivity.outputVolume);
            mainActivity.skinEngine.card (mixer);

            if (mixer.getVisibility() == View.VISIBLE) {
                mixerInit = true ;
//                mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", true);

                mixer.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: theming mixer");
                        mainActivity.skinEngine.card(mixer);
                        mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", true);

                    }
                });
            }

            if (mainActivity.skinEngine.hasKnob()) {
                LinearLayout rotaryRack = mainActivity.findViewById(R.id.rotary_rack);
                rotaryRack.setVisibility(View.VISIBLE);

                mainActivity.rotarySeekbarIn = mainActivity.findViewById(R.id.rotary_input_volume);
                mainActivity.rotarySeekbarOut = mainActivity.findViewById(R.id.rotary_output_volume);
                mainActivity.displayIn = mainActivity.findViewById(R.id.rotary_input_display);
                mainActivity.displayOut = mainActivity.findViewById(R.id.rotary_output_display);
                mainActivity.skinEngine.rotary(mainActivity.rotarySeekbarIn, 3, 0, 100, 50);

                mainActivity.skinEngine.rotary(mainActivity.rotarySeekbarOut, 3, 0, 100, 50);
                mainActivity.rotarySeekbarIn.setValue(mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f) * 100);
                mainActivity.rotarySeekbarOut.setValue(mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f)*100);
                mainActivity.displayIn.setText(String.valueOf((int) (mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f)* 100)));
                mainActivity.displayOut.setText(String.valueOf((int) (mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f)* 100)));

                mainActivity.rotarySeekbarIn.setVisibility(View.VISIBLE);
                mainActivity.rotarySeekbarOut.setVisibility(View.VISIBLE);

                mainActivity.rotarySeekbarIn.setOnValueChangedListener(new RotarySeekbar.OnValueChangedListener() {
                    @Override
                    public void onValueChanged(RotarySeekbar sourceSeekbar, float value) {
                        mainActivity.displayIn.setText(String.valueOf((int) value));
                        mainActivity.inputVolume.setValue(value/100);

                    }
                });

                mainActivity.rotarySeekbarIn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float value = mainActivity.rotarySeekbarIn.getValue() ;
                        mainActivity.displayIn.setText(String.valueOf((int) value));
                        mainActivity.inputVolume.setValue(value/100);
                        return false;
                    }
                });

                mainActivity.rotarySeekbarOut.setOnValueChangedListener(new RotarySeekbar.OnValueChangedListener() {
                    @Override
                    public void onValueChanged(RotarySeekbar sourceSeekbar, float value) {
                        mainActivity.displayOut.setText(String.valueOf((int) value));
                        mainActivity.outputVolume.setValue(value/100);
                    }
                });

                mainActivity.rotarySeekbarOut.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float value = mainActivity.rotarySeekbarOut.getValue() ;
                        mainActivity.displayOut.setText(String.valueOf((int) value));
                        mainActivity.outputVolume.setValue(value/100);
                        return false;
                    }
                });


                inRotaryLabel.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mainActivity.setMidiControl(mainActivity.rotarySeekbarIn, -1, -1, MIDIControl.Type.KNOB, MIDIControl.Scope.GLOBAL);
                        return false;
                    }
                });

                outRotaryLabel.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mainActivity.setMidiControl(mainActivity.rotarySeekbarOut, -1, -1, MIDIControl.Type.KNOB, MIDIControl.Scope.GLOBAL);
                        return false;
                    }
                });

                mainActivity.inputVolume.setVisibility(GONE);
                mainActivity.outputVolume.setVisibility(GONE);
            }
        }

        mainActivity.sampleRateLabel = mainActivity.findViewById(R.id.sample_rate_display);
        mainActivity.srLayout = mainActivity.findViewById(R.id.sr_layout);
        mainActivity.latencyWarnLogo = mainActivity.findViewById(R.id.lowLatencyWarning);
        mainActivity.latencyWarnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.lowLatencyDialog();
            }
        });

        if (mainActivity.tabletMode) {
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(mainActivity.deviceWidth/2, mainActivity.deviceHeight);
            pane_2.setLayoutParams(layoutParams);
//            rackMaster.setLayoutParams(layoutParams);

            ConstraintLayout constraintLayout = mainActivity.findViewById(R.id.super_parent);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            constraintSet.connect(R.id.pane_2, ConstraintSet.RIGHT,R.id.super_parent,ConstraintSet.RIGHT,0);
//            constraintSet.connect(R.id.rack_master, ConstraintSet.LEFT,R.id.super_parent,ConstraintSet.LEFT,0);
            constraintSet.connect(R.id.pane_2,ConstraintSet.TOP,R.id.super_parent,ConstraintSet.TOP,0);
//            constraintSet.connect(R.id.pane_2,ConstraintSet.LEFT,R.id.rack_master,ConstraintSet.RIGHT,0);
            constraintSet.applyTo(constraintLayout);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            rackMaster.setLayoutParams(lp);
            MainActivity.applyWallpaper(mainActivity, mainActivity.pluginDialog.getWindow(), getResources(), mainActivity.pluginDialogWallpaper, mainActivity.deviceWidth, mainActivity.deviceHeight);

            mainActivity.fab.setVisibility(GONE);
            Log.w(TAG, "onViewCreated: tablet mode activated");
        } else {
            Log.d(TAG, String.format ("[display dimensions]: %d x %d {%f}", mainActivity.deviceWidth, mainActivity.deviceHeight, (float) (1.0 * mainActivity.deviceWidth/mainActivity.deviceHeight)));
        }

        if (mainActivity.experimentalBuild) {
//            TextView mixerLabel = mainActivity.findViewById(R.id.mixer_label);
            mixerLabel.setText("Beta " + String.valueOf(BuildConfig.VERSION_CODE));
//            mixerLabel.setTextColor(getResources().getColor(R.color.dark_red));
//            mixerLabel.setBackgroundColor(getResources().getColor(R.color.wheat));
        }

    }

    ArrayList<Map> loadedPresets () {
        if (mainActivity.presets == null || mainActivity.presets.fragmentStateAdapter == null)
            return null ;
        MyPresets myPresets = mainActivity.presets.fragmentStateAdapter.myPresets ;
        if (myPresets == null || myPresets.myPresetsAdapter == null)
            return null ;
        return myPresets.myPresetsAdapter.allPresets ;
    }

    public void patchMove (boolean up) {
        String text = String.valueOf(mainActivity.patchNo.getText()) ;
        if (text == "-")
            text = "0" ;
        int p = Integer.valueOf(text);
        if (up)
            p ++ ;
        else
            p -- ;

        ArrayList<Map> allPresets = loadedPresets();

        if (allPresets == null || allPresets.isEmpty()) {
            MainActivity.alert("Restart the app to load patches", "Patches are not loaded. Restart the app to load patches.");
            return ;
        }

        if (p >= allPresets.size() || p < 0)
            return ;

        mainActivity.loadPreset(allPresets.get(p));
        mainActivity.patchNo.setText(String.valueOf(p));
        mainActivity.patchName.setText((CharSequence) allPresets.get(p).get("name"));
        mainActivity.patchDesc.setText((CharSequence) allPresets.get(p).get("desc"));

    }

    public void latencyDialog () {
        LinearLayout linearLayout = (LinearLayout) mainActivity.getLayoutInflater().inflate(R.layout.latency_tuner, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Latency Tuner")
                .setView(linearLayout)
                .setPositiveButton("Close", null);

        Button latency  = linearLayout.findViewById(R.id.latency);
        latency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

//                AudioEngine.setBufferSizeFactor(0.5f);
                AudioEngine.latencyTuner();
                latency.setText(
                        String.format( "%.0f ms",
                                AudioEngine.getLatency(true) +
                                AudioEngine.getLatency(false)
                        )
                );
            }
        });

        if (mainActivity.running)
            latency.performClick();
        else
            Toast.makeText(mainActivity, "Turn on the audio engine to use Latency Tuner", Toast.LENGTH_SHORT).show();

        Button ato = linearLayout.findViewById(R.id.autofix);
        ato.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

                AudioEngine.tuneLatency();
                latency.performClick();
            }
        });

        Button minimize = linearLayout.findViewById(R.id.minimize);
        minimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

                AudioEngine.minimizeLatency();
                latency.performClick();
            }
        });

        Button glitch = linearLayout.findViewById(R.id.glitch);
        glitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

                AudioEngine.fixGlitches();
                latency.performClick();
            }
        });

        android.os.Handler handler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mainActivity.running)
                    latency.performClick();
                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "onDismiss: closing handler");
                handler.removeCallbacks(r);
            }
        });

        builder.show();
    }

}
