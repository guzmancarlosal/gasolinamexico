package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Welcome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                Intent mainIntent = new Intent(Welcome.this,MainActivity.class);
                Welcome.this.startActivity(mainIntent);
                Welcome.this.finish();
            }
        },3000);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getExtras() != null) {
            Bundle b = getIntent().getExtras();
            boolean cameFromNotification = b.getBoolean("fromNotification",false);
            String alertMsj = b.getString("messageAlert");
            if (cameFromNotification==true) {
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Precio Gasolina").setMessage(alertMsj).setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        }

    }

}
