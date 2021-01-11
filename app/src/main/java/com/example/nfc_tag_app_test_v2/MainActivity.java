package com.example.nfc_tag_app_test_v2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    public final String NO_TAG_ERROR = "No NFC tag has been detected!";
    public final String WRITE_ERROR = "An error during writing has occurred!";
    public final String WRITE_SUCCESS = "Text written successfully!";
    TextView nfc_input;
    TextView nfc_output;
    NfcAdapter nfcAdapter;
    Tag myTag;
    Context context;
    IntentFilter[] writingTagFilters;
    PendingIntent pendingIntent;
    boolean writeMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfc_input = findViewById(R.id.nfc_input);
        nfc_output = findViewById(R.id.nfc_output);
        context = this;

        //Checks if th device supports NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
        }

        Log.d("logCheck", "Wagwan");

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);

        writingTagFilters = new IntentFilter[]{tagDetected};

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("logCheck", "Intent successfully received");
        super.onNewIntent(intent);

        //Checks if the intent is an NFC tag
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            readFromTag(intent);
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d("logCheck", "Tag " + myTag.toString() + " received");
        }
    }

    private void readFromTag(Intent intent) {
        Log.d("logCheck", "Reading from tag...");
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        //Checks if the NFC tag is not empty
        if (rawMessages != null) {
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            //Converts the raw Message into an NDEF message
            for (int i = 0; i < rawMessages.length; i++) {
                messages[i] = (NdefMessage) rawMessages[i];
            }
            buildTagView(messages);
        } else
            nfc_output.setText("");

    }

    protected void buildTagView(NdefMessage[] msgs) {
        //Checks if the NDEF message is not empty
        if (!(msgs == null || msgs.length == 0)) {
            String text = "";
            //Gets the main payload
            byte[] payload = msgs[0].getRecords()[0].getPayload();
            //Determines the type of encoding and language code length
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 51;

            try {
                //Converts the payload from a byte to a string
                text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            } catch (UnsupportedEncodingException e) {
                //Catches any unsupported encoding errors
                Log.e("UnsupportedEncoding", e.toString());
            }

            //Outputs it to a TextView
            nfc_output.setText(text);
        }
    }

    protected void writeToTag(String text, Tag tag) throws IOException, FormatException {
        Log.d("logCheck", "Writing tag tag...");
        //Creates the NDEF message
        NdefRecord[] records = {createRecord(text)};
        NdefMessage message = new NdefMessage(records);

        //Gets the tag and connects to it
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        //Writes the message to the tag and closes the ndef object
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    protected NdefRecord createRecord(String text) {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes(StandardCharsets.UTF_8);
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        //Formats the payload so that the first byte states how many of the next bytes determine the language code, with the rest of the bytes being the actual message
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        //Returns a text record with the payload and no unique identifier
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    public void getNfcInput(View view) {
        //Checks if any errors occur during writing process
        try {
            //Checks if a tag has been detected
            if (myTag == null) {
                Toast.makeText(context, NO_TAG_ERROR, Toast.LENGTH_LONG).show();
            } else {
                //Writes to the NFC tag
                writeToTag(nfc_input.getText().toString(), myTag);
                Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_LONG).show();
            }
        } catch (IOException | FormatException e) {
            Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
            Log.e("Exception", e.toString());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //Turns the foreground dispatch off
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Turns on the foreground dispatch which intercepts and claims priority over any NFC tag detection intents
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, null);
    }

}