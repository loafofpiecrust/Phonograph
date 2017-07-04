package com.kabouzeid.gramophone.preferences;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by snead on 6/28/17.
 */

public class SummaryTextPreference extends EditTextPreference {
    public SummaryTextPreference(Context context) {
        super(context);
        this.init(context, (AttributeSet)null);
    }

    public SummaryTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context, attrs);
    }

    public SummaryTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs);
    }

    public SummaryTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.setLayoutResource(com.kabouzeid.appthemehelper.R.layout.ate_preference_custom);
        if(this.getSummary() == null || this.getSummary().toString().trim().isEmpty()) {
            this.setSummary("%s");
        }
    }

    @Override
    public CharSequence getSummary() {
        final String entry = super.getText();
        CharSequence summary = super.getSummary();
        if (summary == null) {
            return null;
        } else {
            return String.format(summary.toString(), entry == null ? "" : entry);
        }
    }


}
