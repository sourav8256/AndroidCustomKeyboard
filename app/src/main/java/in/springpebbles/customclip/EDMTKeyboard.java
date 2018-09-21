package in.springpebbles.customclip;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static in.springpebbles.customclip.DataUtils.BACKUP_FILE_NAME;
import static in.springpebbles.customclip.DataUtils.BACKUP_FOLDER_PATH;
import static in.springpebbles.customclip.DataUtils.NOTES_FILE_NAME;
import static in.springpebbles.customclip.DataUtils.NOTE_BODY;
import static in.springpebbles.customclip.DataUtils.NOTE_TITLE;
import static in.springpebbles.customclip.DataUtils.isExternalStorageReadable;
import static in.springpebbles.customclip.DataUtils.isExternalStorageWritable;
import static in.springpebbles.customclip.DataUtils.retrieveData;

public class EDMTKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;

    private  boolean isCaps = false;
    JSONArray notes;

    //Press Ctrl+O


    @Override
    public View onCreateInputView() {
        notes = getNotes();
        kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard,null);
        keyboard = new Keyboard(this,R.xml.qwerty);
        resetLabels();
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        return kv;
    }


    void resetLabels(){

        List<Keyboard.Key> keys = keyboard.getKeys();


            Keyboard.Key key = keys.get(0);
            key.label = " ";
            key = keys.get(1);
            key.label = " ";
            key = keys.get(2);
            key.label = " ";

        kv.invalidateAllKeys();
    }



    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    InputConnection ic;

    // making searchnotes global so it retains the search
    JSONArray searchNotes = new JSONArray();

    @Override
    public void onKey(int i, int[] ints) {

        notes = getNotes();
        ic = getCurrentInputConnection();
        //ic.commitText();
        CharSequence currentText = null;




            // we shouldn't search if one of the suggestions is clicked
            if((i!=150 && i!=151 && i!=152) && ic!=null && ic.getExtractedText(new ExtractedTextRequest(), 0)!=null) {
                currentText = ic.getExtractedText(new ExtractedTextRequest(), 0).text;
                String text = currentText.toString();
                String lastWord = text.substring(text.lastIndexOf(" ") + 1);
                if(i==32){
                    searchNotes = searchResult("", notes);
                } else {
                    searchNotes = searchResult(lastWord, notes);
                }
                updateLabels(searchNotes);
            }

        playClick(i);
        getNotes();

        List<Keyboard.Key> keys = keyboard.getKeys();
        Keyboard.Key key;

        switch (i)
        {
            case 150:
                key = keys.get(0);
                try {
                    replaceAll(searchNotes.getJSONObject(0).getString(NOTE_BODY));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 151:
                key = keys.get(1);
                try {
                    replaceAll(searchNotes.getJSONObject(1).getString(NOTE_BODY));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 152:
                key = keys.get(2);
                try {
                    replaceAll(searchNotes.getJSONObject(2).getString(NOTE_BODY));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1,0);
            break;
            case Keyboard.KEYCODE_SHIFT:
                isCaps = !isCaps;
                keyboard.setShifted(isCaps);
                kv.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                break;
                default:
                    char code = (char)i;
                    if(Character.isLetter(code) && isCaps)
                        code = Character.toUpperCase(code);
                    ic.commitText(String.valueOf(code),1);
        }

    }


    void updateLabels(JSONArray notes){

        List<Keyboard.Key> keys = keyboard.getKeys();
        int LENGTH = 3;

        try {

            String[] suggestions = new String[LENGTH];


            Keyboard.Key key;

            for(int i = 0;i<LENGTH;i++){
                if(i<notes.length()) {
                    suggestions[i] = notes.getJSONObject(i).getString(NOTE_BODY);
                    if (suggestions[i] == null && i > 0 && suggestions[i].equals(suggestions[i - 1]))
                        suggestions[i] = " ";
                    key = keys.get(i);
                    key.label = (i + 1) + ". " + formatStringforKeypad(suggestions[i]);
                } else {
                    key = keys.get(i);
                    key.label = " ";
                }
            }

/*            int i = 0;

            Keyboard.Key key = keys.get(0);
            key.label = (i+1)+". "+formatStringforKeypad(suggestions[i++]);
            key = keys.get(1);
            key.label = (i+1)+". "+formatStringforKeypad(suggestions[i++]);
            key = keys.get(2);
            key.label = (i+1)+". "+formatStringforKeypad(suggestions[i++]);*/
        } catch (JSONException e) {
            e.printStackTrace();
        }

        kv.invalidateAllKeys();
    }


    String formatStringforKeypad(String s){

        int LENGTH = 50;

        int endIndex = Math.min(LENGTH,s.length());

        String res = s.substring(0,endIndex);
        StringBuffer sb = new StringBuffer();
        sb.append(res);
        if(endIndex  == LENGTH){
            sb.append("...");
        } else {
            for(int i = -15; i<LENGTH-s.length();i++){
                sb.append(" ");
            }
        }

        res = sb.toString();
        return res;
    }

    private void playClick(int i) {

        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        switch(i)
        {
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    void replaceAll(CharSequence cs){
        CharSequence currentText = ic.getExtractedText(new ExtractedTextRequest(), 0).text;
        CharSequence beforCursorText = ic.getTextBeforeCursor(currentText.length(), 0);
        CharSequence afterCursorText = ic.getTextAfterCursor(currentText.length(), 0);
        ic.deleteSurroundingText(beforCursorText.length(), afterCursorText.length());
        String someStr = currentText.toString();
        try {
            // a space " " has to be given manually between the joining of words
            someStr = someStr.substring(0, someStr.lastIndexOf(" ")) +" "+ cs.toString();
        } catch (Exception e){
            someStr = cs.toString();
        }
        ic.commitText(someStr,0);
    }

    JSONArray getNotes(){
        File localPath, backupPath;
        JSONArray notes; // Main notes array

        // Initialize local file path and backup file path
        localPath = new File(getFilesDir() + "/" + NOTES_FILE_NAME);

        File backupFolder = new File(Environment.getExternalStorageDirectory() +
                BACKUP_FOLDER_PATH);

        if (isExternalStorageReadable() && isExternalStorageWritable() && !backupFolder.exists())
            backupFolder.mkdir();

        backupPath = new File(backupFolder, BACKUP_FILE_NAME);

/*
        // Android version >= 18 -> set orientation userPortrait
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);

            // Android version < 18 -> set orientation sensorPortrait
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
*/

        // Init notes array
        notes = new JSONArray();

        // Retrieve from local path
        JSONArray tempNotes = retrieveData(localPath);

        // If not null -> equal main notes to retrieved notes
        if (tempNotes != null)
            notes = tempNotes;

        return notes;

    }

    public JSONArray searchResult(String s,JSONArray notes) {

        ArrayList<Integer> realIndexesOfSearchResults;

        s = s.toLowerCase(); // Turn string into lowercase

        // If query text length longer than 0
        if (s.length() > 0) {
            // Create new JSONArray and reset realIndexes array
            JSONArray notesFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main notes list
            for (int i = 0; i < notes.length(); i++) {
                JSONObject note = null;

                // Get note at position i
                try {
                    note = notes.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If note not null and title/body contain query text
                // -> Put in new notes array and add i to realIndexes array
                if (note != null) {
                    try {
                        if (note.getString(NOTE_TITLE).toLowerCase().contains(s) ||
                                note.getString(NOTE_BODY).toLowerCase().contains(s)) {

                            notesFound.put(note);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            return notesFound;

            // Create and set adapter with notesFound to refresh ListView
//            NoteAdapter searchAdapter = new NoteAdapter(getApplicationContext(), notesFound);
//            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < notes.length(); i++)
                realIndexesOfSearchResults.add(i);

            return notes;

/*            adapter = new NoteAdapter(getApplicationContext(), notes);
            listView.setAdapter(adapter);*/
        }


    }

    @Override
    public void onText(CharSequence charSequence) {
        Log.d("mylog",charSequence.toString());
    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}
