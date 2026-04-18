package de.nulide.findmydevice.ui;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.utils.FmdLogKt;


public class LockScreenMessage extends FmdActivity {

    private final String TAG = LockScreenMessage.class.getSimpleName();

    public static final String SENDER = "sender";
    public static final String SENDER_TYPE = "type";
    public static final String CUSTOM_TEXT = "ctext";
    // private SmsTransport transport = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lock_screen_message);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        Bundle bundle = getIntent().getExtras();
        // TODO: bring back passing this data??
        //if (bundle.getString(SENDER_TYPE) == "SMS") {
        //    transport = new SmsTransport(bundle.getString(SENDER));
        //}

        TextView tvLockScreenMessage = findViewById(R.id.textViewLockScreenMessage);
        if (bundle != null && bundle.containsKey(CUSTOM_TEXT)) {
            tvLockScreenMessage.setText(bundle.getString(CUSTOM_TEXT));
        } else {
            FmdLogKt.log(this).w(TAG, "No message to show, finishing LockScreenMessage.");
            finish();
        }
    }

    /*
    @Override
    protected void onPause() {
        transport.send(this, getString(R.string.LockScreen_Usage_detectd));
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        transport.send(this, getString(R.string.LockScreen_Backbutton_pressed));
        finish();
    }
     */
}
