package com.example.aron.cryptosimulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    CryptoSimulatorDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void onDeleteDb(View v) {
        initDB();
        this.deleteDatabase("cryptosimulator.db");
    }

    public void onDeleteSp(View v) {
        deleteSharedPref();
    }

    private void deleteSharedPref() {
        SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.clear();
        editor.commit();
    }

    public void initDB() {
        db = new CryptoSimulatorDatabase(this);
        db.open();
    }
}
