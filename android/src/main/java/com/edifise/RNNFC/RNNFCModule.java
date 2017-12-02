package com.edifise.RNNFC;

import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.edifise.RNNFC.record.ParsedNdefRecord;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.IOException;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class RNNFCModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {

    ReactApplicationContext reactContext;

    private NfcAdapter mAdapter = null;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;

    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private boolean startupIntentProcessed = false;

    private String scannedString = null;

    public RNNFCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
        this.reactContext = reactContext;

    }


    private void initiate(){

        mAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext().getCurrentActivity());
        if (mAdapter == null) {
            //Toast.makeText(getCurrentActivity().getApplicationContext(), "error No NFC", Toast.LENGTH_LONG).show();
            //finish();
            return;
        }
//        else
//            Toast.makeText(getCurrentActivity().getApplicationContext(), "YES NFC", Toast.LENGTH_LONG).show();


        mPendingIntent = PendingIntent.getActivity(getCurrentActivity().getApplicationContext(), 0,
                new Intent(reactContext, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNdefPushMessage = new NdefMessage(new NdefRecord[] { newTextRecord(
                "Message from NFC Reader :-)", Locale.ENGLISH, true) });
    }

    //support
    private NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }




    @Override
    public String getName() {
        return "RNNFC";
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void getInitialID(String url) {
        initiate();
        //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(), "initial", Toast.LENGTH_LONG).show();

//        WritableMap params = Arguments.createMap();
//        params.putString("ID", scannedString);
//        scannedString = null;
//        sendEvent("onScanned", params);

        if(getReactApplicationContext().getCurrentActivity() != null){ // it shouldn't be null but you never know
            if(mAdapter == null) {
                mAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext().getCurrentActivity());
            }
            //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(), "some mesg 22", Toast.LENGTH_LONG).show();
            resolveIntent(getReactApplicationContext().getCurrentActivity().getIntent());
        }
    }

    @ReactMethod
    public void pause() {

        WritableMap params = Arguments.createMap();
        params.putString("state", "PAUSED");
        sendEvent("onScanned", params);
    }

    @ReactMethod
    public void NFCavailability(Callback cb) {

        WritableMap params = Arguments.createMap();
        mAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext().getCurrentActivity());
        if (mAdapter == null)
            params.putString("nfcAvailable", "no"); // status NFC unavailable
        else {
            if(mAdapter.isEnabled())
                params.putString("nfcAvailable", "yes"); //status is ok
            else
                params.putString("nfcAvailable", "off"); //status is turned off
        }
        cb.invoke(params);
    }

    @ReactMethod
    public void isPlaying(Callback cb) {
        WritableMap params = Arguments.createMap();
        params.putString("playing", "testing");
        cb.invoke(params);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        //Toast.makeText(activity.getApplication().getApplicationContext(), "some mesg ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNewIntent(Intent intent) {

        //Toast.makeText(getReactApplicationContext().getApplicationContext(), "some mesg new intent", Toast.LENGTH_LONG).show();
        resolveIntent(intent);
    }

    @Override
    public void onHostResume() {

        /*
        //Toast.makeText(getReactApplicationContext().getApplicationContext(), "some mesg resume", Toast.LENGTH_LONG).show();
        //if(!startupIntentProcessed){
            if(getReactApplicationContext().getCurrentActivity() != null){ // it shouldn't be null but you never know
                // necessary because NFC might cause the activity to start and we need to catch that data too
                //handleIntent(getReactApplicationContext().getCurrentActivity().getIntent(),true);

                if(mAdapter == null) {
                    mAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext().getCurrentActivity());

                }
                resolveIntent(getReactApplicationContext().getCurrentActivity().getIntent());
                //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(), "some mesg 22", Toast.LENGTH_LONG).show();

            }
            //startupIntentProcessed = true;
       // }
        */


        if(getReactApplicationContext().getCurrentActivity() != null){ // it shouldn't be null but you never know
            if(mAdapter == null) {
                mAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext().getCurrentActivity());
            }
            //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(), "some mesg 22", Toast.LENGTH_LONG).show();
            //resolveIntent(getReactApplicationContext().getCurrentActivity().getIntent());
        }


    }

    @Override
    public void onHostPause() {


    }

    @Override
    public void onHostDestroy() {

    }


    static String displayByteArray(byte[] bytes) {
        String res="";
        StringBuilder builder = new StringBuilder().append("[");
        for (int i = 0; i < bytes.length; i++) {
            res+=(char)bytes[i];
        }
        return res;
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();

        Parcelable[] rawMsgs2 = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        Log.d( "TTTT",  rawMsgs2 != null ? rawMsgs2.length+"sdfasf": "EMPTY" );

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            Log.d( "TTTT", action);

            NdefMessage[] msgs;
            ArrayList<String> allMsgs = new ArrayList<String>();
            if (rawMsgs != null) {
                Log.d( "TTTT 11", action);

                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {

                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                byte[] id2 = tag.getId();
                WritableMap params = Arguments.createMap();
                params.putString("ID", toReversedHex(id));
		        params.putString("extra", dumpTagData(tag));
                Log.d("TTTT 22", toReversedHex(id));

                scannedString = toReversedHex(id);

                sendEvent("onScanned", params);

                //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(), "some mesg 22", Toast.LENGTH_LONG).show();

//                sb.append("ID (hex): ").append(toHex(id)).append('\n');
//                sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');


//                byte[] payload = dumpTagData(tag).getBytes();
//                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
//                NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
//                msgs = new NdefMessage[] { msg };

                //***mTags.add(tag);
            }

//            String ms = "";
//
//            for (int i = 0; i < msgs.length; i++) {
//
//                NdefRecord[] ndefRecords = msgs[i].getRecords();
//                if (ndefRecords == null) {
//                    return;
//                }
//                for (NdefRecord record : ndefRecords) {
//                    short tnf = record.getTnf();
//                    String type = new String(record.getType());
//                    ms+= i+":I "+ (new String(record.getPayload()));
//
//                }
//            }

            //List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);

            //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(),"some mesg 22 "+msgs[i].getRecords()[0].getPayload() , Toast.LENGTH_LONG).show();



//            for (int i = 0; i < msgs.length; i++) {
//                ms += msgs[i].getRecords()[0].getPayload();
//            }



            // Setup the views
            //buildTagViews(msgs);

        }
    }

    public static String byteArrayToString(byte[] data){
        String response = Arrays.toString(data);

        String[] byteValues = response.substring(1, response.length() - 1).split(",");
        byte[] bytes = new byte[byteValues.length];

        for (int i=0, len=bytes.length; i<len; i++) {
            bytes[i] = Byte.parseByte(byteValues[i].trim());
        }

        String str = new String(bytes);
        return str;
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                //if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e("TAG", "Unsupported Encoding", e);
                    }
                //}
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            Log.d("TTTT answer",byteArrayToString(payload));
            return byteArrayToString(payload);
            // Get the Text
            //return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {

                Log.d("RESULT", result);

                scannedString = result;
                WritableMap params = Arguments.createMap();
                params.putString("ID", result);
                sendEvent("onScanned", params);

                //mTextView.setText("Read content: " + result);
            }
        }
    }

    private String dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();

        //Toast.makeText(getReactApplicationContext().getCurrentActivity().getApplicationContext(), "some mesg 22", Toast.LENGTH_LONG).show();

        sb.append("ID (hex): ").append(toHex(id)).append('\n');
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
        sb.append("ID (dec): ").append(toDec(id)).append('\n');
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');

        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                String type = "Unknown";
                try {
                    MifareClassic mifareTag;
                    try {
                        mifareTag = MifareClassic.get(tag);
                    } catch (Exception e) {
                        // Fix for Sony Xperia Z3/Z5 phones
                        tag = cleanupTag(tag);
                        mifareTag = MifareClassic.get(tag);
                    }
                    switch (mifareTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                            break;
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                    }
                    sb.append("Mifare Classic type: ");
                    sb.append(type);
                    sb.append('\n');

                    sb.append("Mifare size: ");
                    sb.append(mifareTag.getSize() + " bytes");
                    sb.append('\n');

                    sb.append("Mifare sectors: ");
                    sb.append(mifareTag.getSectorCount());
                    sb.append('\n');

                    sb.append("Mifare blocks: ");
                    sb.append(mifareTag.getBlockCount());
                } catch (Exception e) {
                    sb.append("Mifare classic error: " + e.getMessage());
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }

        return sb.toString();
    }


    private Tag cleanupTag(Tag oTag) {
        if (oTag == null)
            return null;

        String[] sTechList = oTag.getTechList();

        Parcel oParcel = Parcel.obtain();
        oTag.writeToParcel(oParcel, 0);
        oParcel.setDataPosition(0);

        int len = oParcel.readInt();
        byte[] id = null;
        if (len >= 0) {
            id = new byte[len];
            oParcel.readByteArray(id);
        }
        int[] oTechList = new int[oParcel.readInt()];
        oParcel.readIntArray(oTechList);
        Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oParcel.readInt();
        int isMock = oParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oParcel.recycle();

        int nfca_idx = -1;
        int mc_idx = -1;
        short oSak = 0;
        short nSak = 0;

        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx].equals(NfcA.class.getName())) {
                if (nfca_idx == -1) {
                    nfca_idx = idx;
                    if (oTechExtras[idx] != null && oTechExtras[idx].containsKey("sak")) {
                        oSak = oTechExtras[idx].getShort("sak");
                        nSak = oSak;
                    }
                } else {
                    if (oTechExtras[idx] != null && oTechExtras[idx].containsKey("sak")) {
                        nSak = (short) (nSak | oTechExtras[idx].getShort("sak"));
                    }
                }
            } else if (sTechList[idx].equals(MifareClassic.class.getName())) {
                mc_idx = idx;
            }
        }

        boolean modified = false;

        if (oSak != nSak) {
            oTechExtras[nfca_idx].putShort("sak", nSak);
            modified = true;
        }

        if (nfca_idx != -1 && mc_idx != -1 && oTechExtras[mc_idx] == null) {
            oTechExtras[mc_idx] = oTechExtras[nfca_idx];
            modified = true;
        }

        if (!modified) {
            return oTag;
        }

        Parcel nParcel = Parcel.obtain();
        nParcel.writeInt(id.length);
        nParcel.writeByteArray(id);
        nParcel.writeInt(oTechList.length);
        nParcel.writeIntArray(oTechList);
        nParcel.writeTypedArray(oTechExtras, 0);
        nParcel.writeInt(serviceHandle);
        nParcel.writeInt(isMock);
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService);
        }
        nParcel.setDataPosition(0);

        Tag nTag = Tag.CREATOR.createFromParcel(nParcel);

        nParcel.recycle();

        return nTag;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private long toReversedDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }




}