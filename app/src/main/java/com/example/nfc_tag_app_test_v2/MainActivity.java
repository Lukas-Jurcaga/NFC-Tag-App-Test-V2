package com.example.nfc_tag_app_test_v2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    TextView nfc_input;
    TextView nfc_output;
    NfcAdapter nfcAdapter;
    Tag myTag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfc_input = findViewById(R.id.nfc_input);
        nfc_output = findViewById(R.id.nfc_output);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
        }
        Log.d("logCheck", "Wagwan");

        Intent newIntent = getIntent();
        if(newIntent.getAction() != null){
            onNewIntent(newIntent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        Log.d("logCheck", "Intent successfully received");
        super.onNewIntent(intent);
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if(rawMessages != null){
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for(int i = 0; i < rawMessages.length; i++){
                    messages[i] = (NdefMessage) rawMessages[i];
                }
                buildTagView(messages);
            }
        }
    }

    protected void buildTagView(NdefMessage[] msgs){
        if(!(msgs == null || msgs.length == 0)){
            String text = "";
            byte[] payload = msgs[0].getRecords()[0].getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8":"UTF-16";
            int languageCodeLength = payload[0] & 51;

            try{
                text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            }catch (UnsupportedEncodingException e){
                Log.e("UnsupportedEncoding", e.toString());
            }

            nfc_output.setText(text);
        }
    }
}