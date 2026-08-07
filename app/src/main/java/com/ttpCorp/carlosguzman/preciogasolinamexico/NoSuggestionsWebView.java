package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class NoSuggestionsWebView extends WebView {

    public NoSuggestionsWebView(Context context) {
        super(context);
    }

    public NoSuggestionsWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoSuggestionsWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // You might also need this constructor if you target API 21+ and use it in XML with a style resource
    // public NoSuggestionsWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    //     super(context, attrs, defStyleAttr, defStyleRes);
    // }

    private void disableAutofill() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);
        disableAutofill();

        if (ic != null) {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT 
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS 
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        }
        return ic;
    }
}
