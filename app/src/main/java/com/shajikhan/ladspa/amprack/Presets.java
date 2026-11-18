package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Map;

public class Presets extends Fragment {
    View view ;
    ViewPager2 viewPager;
    TabLayout tabLayout ;
    PresetAdapter fragmentStateAdapter ;
    private static final String TAG = "Presets";
    MainActivity mainActivity;
    LocalPresetDB db ;
    ProgressBar progressPreset ;
    LinearLayout progressLayout ;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: view created");
        view = inflater.inflate(R.layout.activity_presets,null) ;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        db = new LocalPresetDB (getContext());

        fragmentStateAdapter = new PresetAdapter(this);
        ConstraintLayout constraintLayout = (ConstraintLayout) view  ;
        tabLayout = (TabLayout) constraintLayout.getChildAt(0);
        progressLayout = (LinearLayout) constraintLayout.getChildAt(2);
        progressPreset = (ProgressBar) progressLayout.getChildAt(0);
        viewPager = view.findViewById(R.id.presets_pager);

        viewPager.setSaveEnabled(false);
        fragmentStateAdapter.createFragment(0);

        viewPager.setAdapter(fragmentStateAdapter);

        tabLayout.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        LinearLayout layout = (LinearLayout) constraintLayout.getChildAt(3);
        ExtendedFloatingActionButton presetFab = (ExtendedFloatingActionButton) layout.getChildAt(0);
        presetFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: showing preset");
                Map preset = mainActivity.presetToMap();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = requireActivity().getLayoutInflater();

                LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.save_preset_dialog, null);
                builder.setView(linearLayout)
                        .setCancelable(false);

                LinearLayout linearLayout1 = (LinearLayout) linearLayout.getChildAt(3);
                MaterialButton save = (MaterialButton) linearLayout1.getChildAt(1);
                ProgressBar progressBar = (ProgressBar) linearLayout.getChildAt(4);
                AlertDialog dialog = builder.create() ;

                EditText name = (EditText) linearLayout.getChildAt(1);
                EditText desc = (EditText) linearLayout.getChildAt(2);

                if (mainActivity.lastPresetLoadedPath != null) {
                    name.setText(mainActivity.patchName.getText());
                    desc.setText(mainActivity.patchDesc.getText());
                }

                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout layout = (LinearLayout) linearLayout.getChildAt(0);
                        SwitchMaterial switchMaterial = (SwitchMaterial) layout.getChildAt(1);

                        if (name.getText().toString().length() == 0) {
                            mainActivity.toast("Name of preset is required.");
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);
                        String presetName = name.getText().toString();
                        if (mainActivity.lastPresetLoadedPath != null
                                && presetName.equals(mainActivity.patchName.getText().toString())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage("Do you want to overwrite this preset?")
                                    .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface _dialog, int id) {
                                            db.savePreset(presetName, desc.getText().toString(), switchMaterial.isChecked(), preset, dialog, fragmentStateAdapter.getMyPresets(), mainActivity.lastPresetLoadedPath);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            builder.show();
                        } else {
                            db.savePreset(presetName, desc.getText().toString(), switchMaterial.isChecked(), preset, dialog, fragmentStateAdapter.getMyPresets(), null);
                            mainActivity.patchName.setText(presetName);
                            mainActivity.patchDesc.setText(desc.getText().toString());
                        }
                    }
                });

                MaterialButton cancel = (MaterialButton) linearLayout1.getChildAt(0);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss ();
                    }
                });

                dialog.show();
            }
        });

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.fab(presetFab, SkinEngine.Resize.Width,1);
        }
    }

    public class PresetAdapter extends FragmentStateAdapter {
        public ArrayList<Fragment> arrayList = new ArrayList<>();
        public MyPresets myPresets;

        public MyPresets getMyPresets() {
            return myPresets;
        }

        public PresetAdapter(@NonNull Presets fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            myPresets = new MyPresets(progressPreset);
            arrayList.add(myPresets);
            return myPresets;
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    public static class PresetFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.my_presets, container, false);
        }
    }
}
