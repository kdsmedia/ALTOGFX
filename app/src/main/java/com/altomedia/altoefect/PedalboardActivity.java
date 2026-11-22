package com.altomedia.altoefect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PedalboardActivity extends AppCompatActivity
        implements EngineBridge.Listener, PedalEngine.Listener {

    private static final int REQUEST_AUDIO = 2201;
    private static final int BG = 0xFF07070B;
    private static final int CREAM = 0xFFF6EFE4;
    private static final int MUTED = 0xFF9A9AA4;
    private static final int ACCENT = 0xFFFFB35C;
    private static final int CARD_FILL = 0x14FFFFFF;
    private static final int CARD_STROKE = 0x26FFFFFF;

    private static final class DeviceEntry {
        final int id;
        final String label;

        DeviceEntry(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private PedalEngine engine;
    private FrameLayout content;
    private TextView subtitle;
    private MeterView meter;
    private PowerButtonView power;
    private BottomNavView nav;
    private TextView masterStatus;
    private TextView masterRate;
    private PowerButtonView masterPower;
    private PowerButtonView setupPower;
    private TextView routingIn;
    private TextView routingOut;

    private LinearLayout pedalboardScreen;
    private ScrollView presetsScreen;
    private ScrollView setupScreen;
    private final List<PedalView> boardPedalViews = new ArrayList<>();

    private LinearLayout presetRow;
    private LinearLayout presetPedalRow;
    private TextView presetDescription;
    private PedalView presetPedalView;
    private int presetPedalIndex;
    private Preset selectedPreset;

    private Spinner inputSpinner;
    private Spinner outputSpinner;
    private Spinner sampleRateSpinner;
    private SeekBar inputGain;
    private SeekBar outputVolume;
    private TextView inputGainValue;
    private TextView outputVolumeValue;
    private Switch lowLatencySwitch;
    private TextView headsetStatus;
    private final List<DeviceEntry> inputEntries = new ArrayList<>();
    private final List<DeviceEntry> outputEntries = new ArrayList<>();
    private boolean suppressSpinner;

    private Typeface displayTypeface;
    private Typeface bodyTypeface;
    private boolean pendingStart;
    private boolean uiReady;
    private float lastInput;
    private float lastOutput;
    private int lastSampleRate = 48000;

    private final PedalView.Listener boardPedalListener = new PedalView.Listener() {
        @Override
        public void onControlChanged(PedalView view, RuntimeControl control) {
            refreshState(view.getPedal());
            markEdited();
        }

        @Override
        public void onPedalToggled(PedalView view, PedalState state) {
            refreshState(state);
            markEdited();
        }

        @Override
        public void onOpenEditor(PedalView view, PedalState state) {
            showPedalEditor(state);
        }
    };

    private final PedalView.Listener largePedalListener = new PedalView.Listener() {
        @Override
        public void onControlChanged(PedalView view, RuntimeControl control) {
            refreshState(view.getPedal());
            markEdited();
        }

        @Override
        public void onPedalToggled(PedalView view, PedalState state) {
            refreshState(state);
            markEdited();
        }

        @Override
        public void onOpenEditor(PedalView view, PedalState state) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_pedalboard);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(0xFF050507);

        View shell = findViewById(R.id.shell);
        ViewCompat.setOnApplyWindowInsetsListener(shell, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });

        displayTypeface = ResourcesCompat.getFont(this, R.font.major_mono_display);
        bodyTypeface = ResourcesCompat.getFont(this, R.font.montserrat_alternates);
        if (displayTypeface == null) {
            displayTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        }
        if (bodyTypeface == null) {
            bodyTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        }

        content = findViewById(R.id.content);
        subtitle = findViewById(R.id.subtitle);
        meter = findViewById(R.id.meter);
        power = findViewById(R.id.power);
        nav = findViewById(R.id.nav);

        subtitle.setTypeface(bodyTypeface, Typeface.BOLD);
        nav.setListener(this::showScreen);
        power.setListener(this::setPower);

        engine = PedalEngine.get(this);
        engine.setListener(this);
        EngineBridge.setListener(this);

        buildLoadingScreen();
        new Thread(() -> {
            engine.initialize();
            runOnUiThread(this::onEngineReady);
        }, "altoefect-init").start();
    }

    private void buildLoadingScreen() {
        LinearLayout loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER);
        loading.setBackgroundColor(BG);

        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminate(true);
        bar.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        loading.addView(bar, barParams);

        TextView title = new TextView(this);
        title.setText("WARMING UP THE TUBES");
        title.setTextColor(CREAM);
        title.setTextSize(13);
        title.setTypeface(displayTypeface);
        title.setLetterSpacing(0.18f);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(22);
        loading.addView(title, titleParams);

        TextView note = new TextView(this);
        note.setText("Loading amplifiers, cabinets and stompboxes");
        note.setTextColor(MUTED);
        note.setTextSize(10);
        note.setTypeface(bodyTypeface);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(8);
        loading.addView(note, noteParams);

        content.removeAllViews();
        content.addView(loading, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void onEngineReady() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        buildPedalboardScreen();
        buildPresetsScreen();
        buildSetupScreen();
        uiReady = true;
        showScreen(0);
        refreshDeviceLists();
        updatePower(engine.isStarted());
        selectPreset(engine.getCurrentPreset(), false);
        maybeShowConnectionTip();
    }

    private void buildPedalboardScreen() {
        pedalboardScreen = new LinearLayout(this);
        pedalboardScreen.setOrientation(LinearLayout.VERTICAL);
        pedalboardScreen.setPadding(0, dp(2), 0, 0);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(4), dp(18), dp(2));
        TextView title = sectionTitle("SIGNAL CHAIN");
        header.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Tap a pedal for its controls  •  Stomp the switch to bypass");
        hint.setTextColor(MUTED);
        hint.setTextSize(9.5f);
        hint.setTypeface(bodyTypeface);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(3);
        header.addView(hint, hintParams);
        pedalboardScreen.addView(header);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(14), dp(6), dp(14), dp(2));

        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);

        for (PedalState state : engine.getPedals()) {
            PedalView pedalView = new PedalView(this);
            pedalView.setPedal(state);
            pedalView.setLarge(false);
            pedalView.setListener(boardPedalListener);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(158), dp(314));
            params.rightMargin = dp(12);
            strip.addView(pedalView, params);
            boardPedalViews.add(pedalView);
        }

        strip.addView(buildMasterCard());
        scroll.addView(strip, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        pedalboardScreen.addView(scroll, scrollParams);

        LinearLayout routing = new LinearLayout(this);
        routing.setOrientation(LinearLayout.HORIZONTAL);
        routing.setPadding(dp(18), dp(6), dp(18), dp(10));
        routingIn = routingLabel("IN  •  SYSTEM DEFAULT");
        routingOut = routingLabel("OUT  •  SYSTEM DEFAULT");
        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        outParams.gravity = Gravity.END;
        routingOut.setGravity(Gravity.END);
        routing.addView(routingIn, inParams);
        routing.addView(routingOut, outParams);
        pedalboardScreen.addView(routing);
    }

    private LinearLayout buildMasterCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(16), dp(10), dp(14));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF2C2218, 0xFF0D0C0B, 0xFF050505});
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), 0x55FFB35C);
        card.setBackground(background);

        TextView brand = new TextView(this);
        brand.setText("ALTO EFECT");
        brand.setTextColor(0x99FFB35C);
        brand.setTextSize(7);
        brand.setTypeface(bodyTypeface, Typeface.BOLD);
        brand.setLetterSpacing(0.22f);
        brand.setGravity(Gravity.CENTER);
        card.addView(brand);

        TextView title = new TextView(this);
        title.setText("MASTER");
        title.setTextColor(CREAM);
        title.setTextSize(17);
        title.setTypeface(displayTypeface);
        title.setLetterSpacing(0.05f);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(8);
        card.addView(title, titleParams);

        masterPower = new PowerButtonView(this);
        masterPower.setListener(this::setPower);
        LinearLayout.LayoutParams powerParams = new LinearLayout.LayoutParams(dp(84), dp(84));
        powerParams.topMargin = dp(22);
        card.addView(masterPower, powerParams);

        masterStatus = new TextView(this);
        masterStatus.setText("STANDBY");
        masterStatus.setTextColor(MUTED);
        masterStatus.setTextSize(9);
        masterStatus.setTypeface(bodyTypeface, Typeface.BOLD);
        masterStatus.setLetterSpacing(0.16f);
        masterStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(12);
        card.addView(masterStatus, statusParams);

        masterRate = new TextView(this);
        masterRate.setText("48 kHz");
        masterRate.setTextColor(0x88F6EFE4);
        masterRate.setTextSize(8);
        masterRate.setTypeface(bodyTypeface);
        masterRate.setLetterSpacing(0.12f);
        masterRate.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rateParams.topMargin = dp(4);
        card.addView(masterRate, rateParams);

        TextView hint = new TextView(this);
        hint.setText("ENGINE ON / OFF");
        hint.setTextColor(0x66F6EFE4);
        hint.setTextSize(7);
        hint.setTypeface(bodyTypeface);
        hint.setLetterSpacing(0.12f);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(10);
        card.addView(hint, hintParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(132), dp(314));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void buildPresetsScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(6), dp(16), dp(18));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(sectionTitle("PRESET LIBRARY"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView badge = new TextView(this);
        badge.setText("FACTORY + USER");
        badge.setTextColor(0x99FFB35C);
        badge.setTextSize(7.5f);
        badge.setTypeface(bodyTypeface, Typeface.BOLD);
        badge.setLetterSpacing(0.14f);
        titleRow.addView(badge);
        root.addView(titleRow);

        HorizontalScrollView presetScroll = new HorizontalScrollView(this);
        presetScroll.setHorizontalScrollBarEnabled(false);
        presetScroll.setClipToPadding(false);
        presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetScroll.addView(presetRow);
        LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        presetParams.topMargin = dp(10);
        root.addView(presetScroll, presetParams);

        presetDescription = new TextView(this);
        presetDescription.setTextColor(0xFFD7D0C6);
        presetDescription.setTextSize(10.5f);
        presetDescription.setTypeface(bodyTypeface);
        presetDescription.setLineSpacing(dp(2), 1f);
        presetDescription.setPadding(dp(14), dp(11), dp(14), dp(11));
        presetDescription.setBackground(cardBackground());
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(10);
        root.addView(presetDescription, descParams);

        TextView focus = new TextView(this);
        focus.setText("PEDAL FOCUS");
        focus.setTextColor(0x99FFB35C);
        focus.setTextSize(8);
        focus.setTypeface(bodyTypeface, Typeface.BOLD);
        focus.setLetterSpacing(0.18f);
        LinearLayout.LayoutParams focusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        focusParams.topMargin = dp(14);
        root.addView(focus, focusParams);

        HorizontalScrollView pedalScroll = new HorizontalScrollView(this);
        pedalScroll.setHorizontalScrollBarEnabled(false);
        pedalScroll.setClipToPadding(false);
        presetPedalRow = new LinearLayout(this);
        presetPedalRow.setOrientation(LinearLayout.HORIZONTAL);
        pedalScroll.addView(presetPedalRow);
        LinearLayout.LayoutParams pedalParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pedalParams.topMargin = dp(8);
        root.addView(pedalScroll, pedalParams);

        presetPedalView = new PedalView(this);
        presetPedalView.setLarge(true);
        presetPedalView.setListener(largePedalListener);
        LinearLayout.LayoutParams largeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(438));
        largeParams.topMargin = dp(10);
        root.addView(presetPedalView, largeParams);

        TextView hint = new TextView(this);
        hint.setText("Drag a knob vertically to dial the tone  •  Tap the footswitch to bypass");
        hint.setTextColor(MUTED);
        hint.setTextSize(9);
        hint.setTypeface(bodyTypeface);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(8);
        root.addView(hint, hintParams);

        TextView save = makeButton("SAVE CURRENT TONE AS PRESET");
        save.setOnClickListener(v -> showSavePresetDialog());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        saveParams.topMargin = dp(14);
        root.addView(save, saveParams);

        scroll.addView(root);
        presetsScreen = scroll;
    }

    private void buildSetupScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(6), dp(16), dp(20));

        root.addView(sectionTitle("AUDIO SETUP"));
        TextView intro = new TextView(this);
        intro.setText("Route your guitar through the phone's headset jack or a USB audio interface.");
        intro.setTextColor(MUTED);
        intro.setTextSize(10);
        intro.setTypeface(bodyTypeface);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introParams.topMargin = dp(4);
        root.addView(intro, introParams);

        LinearLayout routingCard = makeCard("INPUT & OUTPUT");
        inputSpinner = makeSpinner();
        outputSpinner = makeSpinner();
        addField(routingCard, "INPUT SOURCE", inputSpinner);
        addField(routingCard, "OUTPUT DESTINATION", outputSpinner);

        TextView detect = makeButton("AUTO-DETECT HEADSET");
        detect.setOnClickListener(v -> autoDetectHeadset());
        LinearLayout.LayoutParams detectParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        detectParams.topMargin = dp(2);
        routingCard.addView(detect, detectParams);

        headsetStatus = new TextView(this);
        headsetStatus.setTextColor(MUTED);
        headsetStatus.setTextSize(9.5f);
        headsetStatus.setTypeface(bodyTypeface);
        headsetStatus.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams headsetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headsetParams.topMargin = dp(10);
        routingCard.addView(headsetStatus, headsetParams);
        addCard(root, routingCard);

        LinearLayout engineCard = makeCard("ENGINE");
        lowLatencySwitch = new Switch(this);
        lowLatencySwitch.setText("Low latency mode");
        lowLatencySwitch.setTextColor(CREAM);
        lowLatencySwitch.setTextSize(12);
        lowLatencySwitch.setTypeface(bodyTypeface);
        lowLatencySwitch.setChecked(engine.isLowLatency());
        lowLatencySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> engine.setLowLatency(isChecked));
        engineCard.addView(lowLatencySwitch);

        sampleRateSpinner = makeSpinner();
        String[] rates = {"44.1 kHz", "48 kHz", "96 kHz"};
        ArrayAdapter<String> rateAdapter = spinnerAdapter(rates);
        sampleRateSpinner.setAdapter(rateAdapter);
        int rateIndex = engine.getSampleRate() == 44100 ? 0 : engine.getSampleRate() == 96000 ? 2 : 1;
        sampleRateSpinner.setSelection(rateIndex);
        sampleRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinner) {
                    return;
                }
                int rate = position == 0 ? 44100 : position == 2 ? 96000 : 48000;
                engine.setSampleRate(rate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        addField(engineCard, "SAMPLE RATE", sampleRateSpinner);

        inputGainValue = new TextView(this);
        inputGainValue.setTextColor(ACCENT);
        inputGainValue.setTextSize(10);
        inputGainValue.setTypeface(bodyTypeface, Typeface.BOLD);
        inputGain = makeSeekBar((int) (engine.getInputGain() * 100));
        inputGain.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                inputGainValue.setText(progress + "%");
                if (fromUser) {
                    engine.setInputGain(progress / 100f);
                }
            }
        });
        inputGainValue.setText((int) (engine.getInputGain() * 100) + "%");
        addFieldWithValue(engineCard, "INPUT GAIN", inputGain, inputGainValue);

        outputVolumeValue = new TextView(this);
        outputVolumeValue.setTextColor(ACCENT);
        outputVolumeValue.setTextSize(10);
        outputVolumeValue.setTypeface(bodyTypeface, Typeface.BOLD);
        outputVolume = makeSeekBar((int) (engine.getOutputVolume() * 100));
        outputVolume.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                outputVolumeValue.setText(progress + "%");
                if (fromUser) {
                    engine.setOutputVolume(progress / 100f);
                }
            }
        });
        outputVolumeValue.setText((int) (engine.getOutputVolume() * 100) + "%");
        addFieldWithValue(engineCard, "OUTPUT VOLUME", outputVolume, outputVolumeValue);
        addCard(root, engineCard);

        LinearLayout guideCard = makeCard("CONNECTION GUIDE");
        TextView guide = new TextView(this);
        guide.setText("1.  Use a 4-pole TRRS headset, or a TRRS headset splitter, in the phone's 3.5 mm jack.\n\n"
                + "2.  Plug the guitar into the microphone input with a 1/4\" to 3.5 mm adapter.\n\n"
                + "3.  Listen through the headphone output. A USB audio interface also works and usually gives the lowest latency.\n\n"
                + "4.  Start with INPUT GAIN low, then raise it until the IN meter peaks in the green.");
        guide.setTextColor(0xFFD7D0C6);
        guide.setTextSize(10.5f);
        guide.setTypeface(bodyTypeface);
        guide.setLineSpacing(dp(2), 1f);
        guideCard.addView(guide);
        addCard(root, guideCard);

        LinearLayout powerCard = makeCard("MASTER POWER");
        LinearLayout powerRow = new LinearLayout(this);
        powerRow.setOrientation(LinearLayout.HORIZONTAL);
        powerRow.setGravity(Gravity.CENTER_VERTICAL);
        setupPower = new PowerButtonView(this);
        setupPower.setOn(engine.isStarted());
        setupPower.setListener(this::setPower);
        powerRow.addView(setupPower, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView powerText = new TextView(this);
        powerText.setText("Turn the engine on before you play. Keep ALTO EFECT in the foreground for the lowest latency.");
        powerText.setTextColor(0xFFD7D0C6);
        powerText.setTextSize(10.5f);
        powerText.setTypeface(bodyTypeface);
        powerText.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams powerTextParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        powerTextParams.leftMargin = dp(12);
        powerRow.addView(powerText, powerTextParams);
        powerCard.addView(powerRow);
        addCard(root, powerCard);

        TextView footer = new TextView(this);
        footer.setText("ALTO EFECT  •  v1.0  •  100% OFFLINE");
        footer.setTextColor(0x66F6EFE4);
        footer.setTextSize(8);
        footer.setTypeface(bodyTypeface);
        footer.setLetterSpacing(0.12f);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(18);
        root.addView(footer, footerParams);

        scroll.addView(root);
        setupScreen = scroll;
    }

    private void showScreen(int index) {
        if (!uiReady) {
            return;
        }
        content.removeAllViews();
        View screen;
        if (index == 1) {
            screen = presetsScreen;
        } else if (index == 2) {
            screen = setupScreen;
        } else {
            screen = pedalboardScreen;
        }
        content.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        nav.setSelected(index);
        if (index == 1) {
            subtitle.setText("PRESET LIBRARY");
            rebuildPresetChips();
            updatePresetPedal();
        } else if (index == 2) {
            subtitle.setText("AUDIO SETUP");
            refreshDeviceLists();
        } else if (selectedPreset != null) {
            subtitle.setText(selectedPreset.name.toUpperCase(Locale.US));
        } else {
            subtitle.setText("PEDALBOARD");
        }
    }

    private void setPower(boolean on) {
        if (!uiReady) {
            return;
        }
        if (!on) {
            engine.stop();
            updatePower(false);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingStart = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            updatePower(false);
            return;
        }
        if (engine.findHeadsetOutput() == -1) {
            new AlertDialog.Builder(this)
                    .setTitle("Headphones recommended")
                    .setMessage("No headset output was detected. Running through the phone speaker can cause loud feedback. Connect headphones or a USB interface, or continue at low volume.")
                    .setPositiveButton("Start anyway", (dialog, which) -> startAudio())
                    .setNegativeButton("Cancel", (dialog, which) -> updatePower(false))
                    .show();
            return;
        }
        startAudio();
    }

    private void startAudio() {
        boolean started = engine.start();
        updatePower(started);
        if (started) {
            toast("Engine running  •  " + (engine.getSampleRate() / 1000) + " kHz");
        } else {
            toast("Could not start the audio engine");
        }
    }

    private void updatePower(boolean running) {
        if (power != null) {
            power.setOn(running);
        }
        if (masterPower != null) {
            masterPower.setOn(running);
        }
        if (setupPower != null) {
            setupPower.setOn(running);
        }
        if (masterStatus != null) {
            masterStatus.setText(running ? "RUNNING" : "STANDBY");
            masterStatus.setTextColor(running ? ACCENT : MUTED);
        }
        if (running) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void selectPreset(Preset preset, boolean apply) {
        if (preset == null) {
            return;
        }
        if (apply) {
            engine.applyPreset(preset, true);
        }
        selectedPreset = preset;
        if (presetDescription != null) {
            presetDescription.setText(preset.tag + "  •  " + preset.description);
        }
        if (nav.getSelected() != 2) {
            subtitle.setText(preset.name.toUpperCase(Locale.US));
        }
        rebuildPresetChips();
        updateAllPedalViews();
        updatePresetPedal();
    }

    private void rebuildPresetChips() {
        if (presetRow == null) {
            return;
        }
        presetRow.removeAllViews();
        List<Preset> presets = engine.getPresets();
        for (Preset preset : presets) {
            boolean selected = selectedPreset != null && preset.name.equalsIgnoreCase(selectedPreset.name);
            TextView chip = chip(preset.name, selected, ACCENT);
            chip.setOnClickListener(v -> selectPreset(preset, true));
            if (!preset.factory) {
                chip.setOnLongClickListener(v -> {
                    confirmDeletePreset(preset);
                    return true;
                });
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(8);
            presetRow.addView(chip, params);
        }
    }

    private void rebuildPedalChips() {
        if (presetPedalRow == null) {
            return;
        }
        presetPedalRow.removeAllViews();
        List<PedalState> pedals = engine.getPedals();
        for (int i = 0; i < pedals.size(); i++) {
            PedalState state = pedals.get(i);
            boolean selected = i == presetPedalIndex;
            TextView chip = pedalChip(state, selected);
            int index = i;
            chip.setOnClickListener(v -> {
                presetPedalIndex = index;
                updatePresetPedal();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(7);
            presetPedalRow.addView(chip, params);
        }
    }

    private void updatePresetPedal() {
        if (presetPedalView == null) {
            return;
        }
        List<PedalState> pedals = engine.getPedals();
        if (pedals.isEmpty()) {
            return;
        }
        if (presetPedalIndex < 0 || presetPedalIndex >= pedals.size()) {
            presetPedalIndex = 0;
        }
        presetPedalView.setPedal(pedals.get(presetPedalIndex));
        presetPedalView.refresh();
        rebuildPedalChips();
    }

    private void updateAllPedalViews() {
        for (PedalView view : boardPedalViews) {
            view.refresh();
        }
        if (presetPedalView != null) {
            presetPedalView.refresh();
        }
        updateRoutingLabels();
    }

    private void refreshState(PedalState state) {
        for (PedalView view : boardPedalViews) {
            if (view.getPedal() == state) {
                view.refresh();
            }
        }
        if (presetPedalView != null && presetPedalView.getPedal() == state) {
            presetPedalView.refresh();
        }
    }

    private void markEdited() {
        if (selectedPreset != null && nav.getSelected() != 2) {
            subtitle.setText((selectedPreset.name + "  •  EDIT").toUpperCase(Locale.US));
        }
    }

    private void showPedalEditor(PedalState state) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(this);
        root.addView(new StageBackgroundView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(12), dp(16), dp(20));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = sectionTitle(state.spec.model + " CONTROLS");
        top.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = makeButton("CLOSE");
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(84), dp(38));
        top.addView(close, closeParams);
        layout.addView(top);

        PedalView large = new PedalView(this);
        large.setLarge(true);
        large.setPedal(state);
        large.setListener(largePedalListener);
        LinearLayout.LayoutParams largeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(438));
        largeParams.topMargin = dp(10);
        layout.addView(large, largeParams);

        TextView hint = new TextView(this);
        hint.setText("Drag knobs vertically  •  Tap the footswitch to bypass this pedal");
        hint.setTextColor(MUTED);
        hint.setTextSize(9);
        hint.setTypeface(bodyTypeface);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(8);
        layout.addView(hint, hintParams);

        scroll.addView(layout);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setContentView(root);
        dialog.show();
    }

    private void showSavePresetDialog() {
        EditText input = new EditText(this);
        input.setHint("Preset name");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setTextColor(CREAM);
        input.setHintTextColor(0x88F6EFE4);
        input.setBackground(cardBackground());
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        FrameLayout wrapper = new FrameLayout(this);
        int pad = dp(18);
        wrapper.setPadding(pad, dp(6), pad, 0);
        wrapper.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Save current tone")
                .setView(wrapper)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("Please enter a preset name");
                        return;
                    }
                    engine.saveCurrentAsPreset(name, "USER", "Captured from the pedalboard");
                    Preset saved = null;
                    for (Preset preset : engine.getPresets()) {
                        if (preset.name.equalsIgnoreCase(name)) {
                            saved = preset;
                            break;
                        }
                    }
                    selectedPreset = saved;
                    rebuildPresetChips();
                    if (saved != null) {
                        selectPreset(saved, false);
                    }
                    toast("Preset saved");
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void confirmDeletePreset(Preset preset) {
        new AlertDialog.Builder(this)
                .setTitle("Delete preset")
                .setMessage("Delete \"" + preset.name + "\"?")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    engine.deleteUserPreset(preset.name);
                    if (selectedPreset != null && selectedPreset.name.equalsIgnoreCase(preset.name)) {
                        List<Preset> presets = engine.getPresets();
                        selectedPreset = presets.isEmpty() ? null : presets.get(0);
                    }
                    rebuildPresetChips();
                    updatePresetPedal();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void autoDetectHeadset() {
        int input = engine.findHeadsetInput();
        int output = engine.findHeadsetOutput();
        if (input == -1 && output == -1) {
            toast("No headset or USB audio device detected");
            return;
        }
        engine.setDevices(input != -1 ? input : Integer.MIN_VALUE,
                output != -1 ? output : Integer.MIN_VALUE);
        refreshDeviceLists();
        toast("Headset routing selected");
    }

    private void refreshDeviceLists() {
        if (!uiReady || inputSpinner == null || outputSpinner == null) {
            return;
        }
        inputEntries.clear();
        inputEntries.add(new DeviceEntry(-1, "Auto  •  system default"));
        for (AudioDeviceInfo device : engine.inputDevices()) {
            inputEntries.add(new DeviceEntry(device.getId(), deviceLabel(device)));
        }
        outputEntries.clear();
        outputEntries.add(new DeviceEntry(-1, "Auto  •  system default"));
        for (AudioDeviceInfo device : engine.outputDevices()) {
            outputEntries.add(new DeviceEntry(device.getId(), deviceLabel(device)));
        }

        String[] inputLabels = new String[inputEntries.size()];
        for (int i = 0; i < inputEntries.size(); i++) {
            inputLabels[i] = inputEntries.get(i).label;
        }
        String[] outputLabels = new String[outputEntries.size()];
        for (int i = 0; i < outputEntries.size(); i++) {
            outputLabels[i] = outputEntries.get(i).label;
        }

        suppressSpinner = true;
        inputSpinner.setAdapter(spinnerAdapter(inputLabels));
        outputSpinner.setAdapter(spinnerAdapter(outputLabels));
        inputSpinner.setSelection(indexOf(inputEntries, engine.getInputDevice()));
        outputSpinner.setSelection(indexOf(outputEntries, engine.getOutputDevice()));
        suppressSpinner = false;

        inputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinner) {
                    return;
                }
                engine.setInputDevice(inputEntries.get(position).id);
                updateRoutingLabels();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        outputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinner) {
                    return;
                }
                engine.setOutputDevice(outputEntries.get(position).id);
                updateRoutingLabels();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        boolean headset = engine.findHeadsetOutput() != -1 || engine.findHeadsetInput() != -1;
        headsetStatus.setText(headset
                ? "Headset / interface detected. Input and output are ready."
                : "No headset detected yet. Plug in a TRRS headset or USB interface, then tap AUTO-DETECT HEADSET.");
        headsetStatus.setTextColor(headset ? 0xFF8FE3A8 : 0xFFFFB35C);
        updateRoutingLabels();
    }

    private void updateRoutingLabels() {
        if (routingIn == null || routingOut == null) {
            return;
        }
        routingIn.setText("IN  •  " + describeDevice(engine.getInputDevice(), true));
        routingOut.setText("OUT  •  " + describeDevice(engine.getOutputDevice(), false));
    }

    private String describeDevice(int id, boolean input) {
        if (id == -1) {
            return "SYSTEM DEFAULT";
        }
        List<AudioDeviceInfo> devices = input ? engine.inputDevices() : engine.outputDevices();
        for (AudioDeviceInfo device : devices) {
            if (device.getId() == id) {
                return PedalEngine.deviceTypeName(device.getType()).toUpperCase(Locale.US);
            }
        }
        return "DEVICE " + id;
    }

    private String deviceLabel(AudioDeviceInfo device) {
        CharSequence productName = device.getProductName();
        String product = productName == null ? "" : productName.toString().trim();
        String type = PedalEngine.deviceTypeName(device.getType());
        if (product.isEmpty() || product.equalsIgnoreCase(type)) {
            return type + "  (#" + device.getId() + ")";
        }
        return type + "  •  " + product + "  (#" + device.getId() + ")";
    }

    private int indexOf(List<DeviceEntry> entries, int id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id == id) {
                return i;
            }
        }
        return 0;
    }

    private void maybeShowConnectionTip() {
        SharedPreferences prefs = getSharedPreferences("altoefect", Context.MODE_PRIVATE);
        if (prefs.getBoolean("tip_shown", false)) {
            return;
        }
        prefs.edit().putBoolean("tip_shown", true).apply();
        if (engine.findHeadsetOutput() != -1) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Plug in and play")
                .setMessage("ALTO EFECT uses the phone's headset jack: guitar in through the microphone input, headphones out through the headphone output. Use a TRRS headset or splitter, or a USB audio interface.\n\nOpen AUDIO SETUP to choose your devices.")
                .setPositiveButton("OPEN SETUP", (dialog, which) -> {
                    showScreen(2);
                    nav.setSelected(2);
                })
                .setNegativeButton("GOT IT", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_AUDIO) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingStart) {
            pendingStart = false;
            setPower(true);
        } else if (!granted) {
            pendingStart = false;
            updatePower(false);
            toast("Microphone permission is required for guitar input");
        }
    }

    @Override
    public void onMeter(float value, boolean input) {
        if (input) {
            lastInput = value;
        } else {
            lastOutput = value;
        }
        if (meter != null) {
            meter.setLevels(lastInput, lastOutput);
        }
    }

    @Override
    public void onSampleRate(int sampleRate, boolean lowLatency) {
        lastSampleRate = sampleRate;
        if (masterRate != null) {
            masterRate.setText((sampleRate / 1000) + " kHz" + (lowLatency ? "  •  LOW LAT" : ""));
        }
    }

    @Override
    public void onAudioStateChanged(boolean running) {
        runOnUiThread(() -> updatePower(running));
    }

    @Override
    public void onBackPressed() {
        if (uiReady && nav.getSelected() != 0) {
            showScreen(0);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        engine.setListener(null);
        EngineBridge.setListener(null);
        if (isFinishing()) {
            engine.stop();
        }
        super.onDestroy();
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(CREAM);
        view.setTextSize(20);
        view.setTypeface(displayTypeface);
        view.setLetterSpacing(0.04f);
        return view;
    }

    private TextView routingLabel(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0x99F6EFE4);
        view.setTextSize(7.5f);
        view.setTypeface(bodyTypeface, Typeface.BOLD);
        view.setLetterSpacing(0.1f);
        return view;
    }

    private LinearLayout makeCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardBackground());

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(0x99FFB35C);
        heading.setTextSize(8);
        heading.setTypeface(bodyTypeface, Typeface.BOLD);
        heading.setLetterSpacing(0.18f);
        card.addView(heading);
        return card;
    }

    private void addCard(LinearLayout root, LinearLayout card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        root.addView(card, params);
    }

    private void addField(LinearLayout card, String label, View field) {
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(MUTED);
        caption.setTextSize(8.5f);
        caption.setTypeface(bodyTypeface, Typeface.BOLD);
        caption.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        captionParams.topMargin = dp(12);
        card.addView(caption, captionParams);

        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldParams.topMargin = dp(5);
        card.addView(field, fieldParams);
    }

    private void addFieldWithValue(LinearLayout card, String label, View field, TextView value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(MUTED);
        caption.setTextSize(8.5f);
        caption.setTypeface(bodyTypeface, Typeface.BOLD);
        caption.setLetterSpacing(0.12f);
        row.addView(caption, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(value);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(12);
        card.addView(row, rowParams);

        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldParams.topMargin = dp(4);
        card.addView(field, fieldParams);
    }

    private Spinner makeSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackground(cardBackground());
        spinner.setPadding(dp(10), dp(2), dp(10), dp(2));
        return spinner;
    }

    private ArrayAdapter<String> spinnerAdapter(String[] values) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(CREAM);
                view.setTextSize(12);
                view.setTypeface(bodyTypeface);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(0xFF14100C);
                view.setBackgroundColor(0xFFF6EFE4);
                view.setTextSize(12);
                view.setPadding(dp(14), dp(10), dp(14), dp(10));
                view.setTypeface(bodyTypeface);
                return view;
            }
        };
    }

    private SeekBar makeSeekBar(int progress) {
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(progress);
        seekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seekBar.setThumbTintList(ColorStateList.valueOf(0xFFFFD79A));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
        return seekBar;
    }

    private TextView makeButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(0xFF15100A);
        button.setTextSize(10);
        button.setTypeface(bodyTypeface, Typeface.BOLD);
        button.setLetterSpacing(0.12f);
        button.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFFFD08A, 0xFFFFB35C, 0xFFE88A2C});
        background.setCornerRadius(dp(12));
        button.setBackground(background);
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView chip(String text, boolean selected, int accent) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(10.5f);
        chip.setTypeface(bodyTypeface, Typeface.BOLD);
        chip.setLetterSpacing(0.06f);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(9), dp(14), dp(9));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));
        if (selected) {
            background.setColors(new int[]{0xFFFFD08A, accent, 0xFFE88A2C});
            background.setOrientation(GradientDrawable.Orientation.TL_BR);
            chip.setTextColor(0xFF15100A);
        } else {
            background.setColor(0x1FFFFFFF);
            background.setStroke(dp(1), 0x33FFFFFF);
            chip.setTextColor(CREAM);
        }
        chip.setBackground(background);
        chip.setClickable(true);
        chip.setFocusable(true);
        return chip;
    }

    private TextView pedalChip(PedalState state, boolean selected) {
        TextView chip = new TextView(this);
        String dot = "●  ";
        String text = dot + state.spec.model;
        android.text.SpannableString span = new android.text.SpannableString(text);
        span.setSpan(new android.text.style.ForegroundColorSpan(
                        state.available ? state.spec.accentColor : 0xFF555555),
                0, dot.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        chip.setText(span);
        chip.setTextSize(9.5f);
        chip.setTypeface(bodyTypeface, Typeface.BOLD);
        chip.setLetterSpacing(0.06f);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(11), dp(7), dp(11), dp(7));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(16));
        if (selected) {
            background.setColor(state.spec.accentColor);
            chip.setTextColor(0xFF15100A);
            span.setSpan(new android.text.style.ForegroundColorSpan(0xFF15100A),
                    0, dot.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            background.setColor(0x14FFFFFF);
            background.setStroke(dp(1), 0x26FFFFFF);
            chip.setTextColor(CREAM);
        }
        chip.setBackground(background);
        chip.setClickable(true);
        chip.setFocusable(true);
        return chip;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD_FILL);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), CARD_STROKE);
        return background;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
