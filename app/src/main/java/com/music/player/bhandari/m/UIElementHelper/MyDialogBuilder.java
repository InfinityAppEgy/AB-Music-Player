package com.music.player.bhandari.m.UIElementHelper;

import android.content.Context;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.music.player.bhandari.m.R;

/**
 * Custom material dialog builder for creating global animation and setting drawable gradient
 */

public class MyDialogBuilder extends MaterialDialog.Builder{

    public MyDialogBuilder(@NonNull Context context) {
        super(context);
    }

    @Override
    public MaterialDialog build() {
        return new MyDialog(this);
    }
}

class MyDialog extends MaterialDialog {

    MyDialog(Builder builder) {
        super(builder);
    }

    @Override
    public void show() {
        if(getWindow()!=null) {
            getWindow().getAttributes().windowAnimations = R.style.MyAnimation_Window;
            getWindow().setBackgroundDrawable(ColorHelper.GetGradientDrawableDark());
        }
        super.show();
    }
}
