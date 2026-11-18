package com.example.listadetarefas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private AudioHelper audioHelper;
    private TextToSpeech textToSpeech;
    private TextView audioStatus;
    private final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            updateAudioStatus();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            updateAudioStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        audioStatus = findViewById(R.id.audio_status);
        audioHelper = new AudioHelper(this);
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        Button speakButton = findViewById(R.id.speak_button);
        speakButton.setOnClickListener(v -> speakTasks());
        updateAudioStatus();

        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (audioHelper != null) {
            audioHelper.getAudioManager().registerAudioDeviceCallback(audioDeviceCallback, null);
            updateAudioStatus();
        }
    }

    @Override
    protected void onPause() {
        if (audioHelper != null) {
            audioHelper.getAudioManager().unregisterAudioDeviceCallback(audioDeviceCallback);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void updateAudioStatus() {
        boolean available = audioHelper != null
                && (audioHelper.audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                || audioHelper.audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP));
        audioStatus.setText(available
                ? R.string.audio_status_available
                : R.string.audio_status_unavailable);
    }

    private void speakTasks() {
        if (!audioHelper.audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                && !audioHelper.audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("EXTRA_CONNECTION_ONLY", true);
            intent.putExtra("EXTRA_CLOSE_ON_CONNECT", true);
            intent.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1);
            startActivity(intent);
            return;
        }

        textToSpeech.speak("Verificar as notificações. Conferir os alertas de segurança. "
                + "Responder aos comandos de voz.", TextToSpeech.QUEUE_FLUSH, null, "tasks");
    }
}
