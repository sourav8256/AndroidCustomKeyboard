package in.springpebbles.customclip;

import android.content.Context;
import android.inputmethodservice.Keyboard;

import java.util.List;

public class CustomKeyboard extends Keyboard {
    public CustomKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public CustomKeyboard(Context context, int xmlLayoutResId, int modeId, int width, int height) {
        super(context, xmlLayoutResId, modeId, width, height);
    }

    public CustomKeyboard(Context context, int xmlLayoutResId, int modeId) {
        super(context, xmlLayoutResId, modeId);
    }

    public CustomKeyboard(Context context, int layoutTemplateResId, CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

    @Override
    public int[] getNearestKeys(int x, int y) {

        List<Key> keys = getKeys();
        for(int i=0;i<keys.size();i++){
            if(keys.get(i).isInside(x,y)) return new int[]{i};
        }

        return new int[0];
        //return super.getNearestKeys(x, y);
    }
}
