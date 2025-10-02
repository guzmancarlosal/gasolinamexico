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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);

        if (ic != null) {
            // Modify the EditorInfo to disable suggestions
            // This is the primary flag to disable suggestions.
            outAttrs.inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

            // Sometimes, for certain keyboards or Android versions,
            // you might need to ensure other flags that enable suggestions are off,
            // or explicitly set a variation that typically doesn't have suggestions.
            // For example, TYPE_TEXT_VARIATION_VISIBLE_PASSWORD often disables suggestions.
            // However, TYPE_TEXT_FLAG_NO_SUGGESTIONS should usually be sufficient.
            //
            // If you still see suggestions, you could try more aggressive flags like:
            // outAttrs.inputType |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            // or ensure flags like TYPE_TEXT_FLAG_AUTO_COMPLETE are not set,
            // but be careful as this can alter other input behaviors.
        }
        return ic;
    }
}
