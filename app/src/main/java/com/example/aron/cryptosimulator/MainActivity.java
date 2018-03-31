package com.example.aron.cryptosimulator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by Aron on 07.03.2018.
 */

public class MainActivity extends AppCompatActivity implements AsyncResponse {

    //the java class responsible for the main activity/ the start page.
    //per default the list fragment is active.
    //if the user uses the switch button the list is replaced with the pie chart fragment.

    public static final String DATE_FORMAT_PATTERN = "hh:mm:ss";
    public static final String SHARED_PREFERENCES = "MyPrefsFile";
    public static final String BALANCE = "balance";
    public static final String TWO_DIGIT_DECIMAL = "##.00";
    public static final String JSON_STRING = "jsonString";
    public static final String POSITIONS_ARRAY = "positions";
    public static final String PRICE_IN_EURO = "EUR";
    public static final String FIRST_TIME = "firstTime";
    public static final int MINIMUM_INPUT = 50;
    public static final int MAXIMUM_INPUT = 1000000000;
    CryptoSimulatorDatabase db;
    Context c;
    AsyncResponse ar;
    Switch diagramSwitch;
    SwipeRefreshLayout mSwipeRefreshLayout;
    JsonTask jsonTask;
    ArrayList<Position> positions;
    private JSONObject jObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isFirstTime()) showDialog();

        initDB();
        initPositions();
        initBalanceUI();
        initSwipeLayout();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        c = this;
        ar = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if the user resumes to the home screen all the values have to be actualized
        initPositions();
        initBalanceUI();
        setupSwitch();
    }

    public void initSwipeLayout() {
        ////initate the refresh functionality that gets activated when the user swipes up on either the list or the pie fragment
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                jsonTask = new JsonTask();
                jsonTask.delegate = ar;
                //register this activity as an AsyncResponse at the HTTP service so the results can be passed via the 'onPostExecute' method
                jsonTask.execute(getString(R.string.json_string));
            }
        });
    }

    public void refreshActualized() {
        TextView t = findViewById(R.id.textview_actualized);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        t.setText(getString(R.string.text_actualized) + dateFormatter.format(new Timestamp(System.currentTimeMillis())));
        //set the actualized textview to the current time
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //configurate the toolbar
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_transaction_history:
                intent = new Intent(MainActivity.this, TransactionsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_add_transaction:
                intent = new Intent(MainActivity.this, BuyActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the toolbar and remove the home icon (because the user is already there)
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar, menu);
        MenuItem settingsItem = menu.findItem(R.id.action_home);
        settingsItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    public void setupSwitch() {
        //show the fragment that the user chose using the switch
        diagramSwitch = findViewById(R.id.diagram_switch);
        diagramSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    showPieFragment();
                } else {
                    showListFragment();
                }
            }
        });
    }

    public void initDB() {
        db = new CryptoSimulatorDatabase(this);
        db.open();
    }

    public void initBalanceUI() {
        //get the balance from shared preferences and update the corresponding textview
        SharedPreferences mPreferences = this.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        int balance = mPreferences.getInt(BALANCE, 0);
        TextView t = findViewById(R.id.textview_balance);
        DecimalFormat f = new DecimalFormat(TWO_DIGIT_DECIMAL);
        t.setText(f.format((double) balance / 100) + getString(R.string.euro_symbol));
    }

    private boolean isFirstTime() {
        //check if the user uses the app for the first time or has not entered the amount of his starting money yet
        SharedPreferences mPreferences = this.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return mPreferences.getBoolean(FIRST_TIME, true);
    }

    public void showDialog() {
        //use a builder to configurate the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.text_start_money));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.text_positive_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(input.getText().toString().length() <1) return;
                handleDialogInput(Integer.parseInt(input.getText().toString()));
                SharedPreferences mPreferences = c.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(FIRST_TIME, false);
                editor.commit();
            }
        });
        builder.setNegativeButton(getString(R.string.text_negative_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void handleDialogInput(int input) {
        //user input from the starting money dialog is verified here
        if (input < MINIMUM_INPUT || input > MAXIMUM_INPUT) {
            Toast.makeText(this, getString(R.string.text_wrong_input), Toast.LENGTH_LONG).show();
            showDialog();
        } else {
            saveStartingMoneyInput(input);
        }
    }

    public void saveStartingMoneyInput(int input) {
        //user input from the starting money dialog is saved here
        SharedPreferences mPreferences = this.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(BALANCE, input * 100);
        editor.commit();
        initBalanceUI();
    }

    @Override
    public void processFinish(String output) {
        //overriding method from the AsyncResponse Interface
        //this method gets called from the onPostExecute with the result from the http response
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

        try {
            jObject = new JSONObject(output);
            if (diagramSwitch.isChecked()) {
                showPieFragment();
            } else {
                showListFragment();
            }
            initPortfolioValue();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        refreshActualized();
    }

    public void showListFragment() {
        //initialize the listfragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        PositionListFragment fragment = new PositionListFragment();

        Bundle bundle = new Bundle();
        bundle.putString(JSON_STRING, jObject.toString());
        bundle.putSerializable(POSITIONS_ARRAY, positions);
        fragment.setArguments(bundle);

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public void showPieFragment() {
        //initialize the pie chart fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PieFragment fragment = new PieFragment();

        Bundle bundle = new Bundle();
        bundle.putString(JSON_STRING, jObject.toString());
        bundle.putSerializable(POSITIONS_ARRAY, positions);
        fragment.setArguments(bundle);

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void initPositions() {
        //fill the positions ArrayList and make a http response via the JsonTask class
        positions = db.getAllPositions();
        jsonTask = new JsonTask();
        jsonTask.delegate = this;
        jsonTask.execute(getString(R.string.json_string));
    }

    public void initPortfolioValue() {
        //iterate through the positions list to accumulate the value of the portfolio
        TextView t = (TextView) findViewById(R.id.portfolio_value_textview);
        double portfoltioV = 0;
        for (Position p : positions) {
            portfoltioV += getValueFromP(p);
        }
        DecimalFormat f = new DecimalFormat(TWO_DIGIT_DECIMAL);
        t.setText("" + f.format(portfoltioV) + getString(R.string.euro_symbol));
    }

    public double getValueFromP(Position p) {
        //give the double value of a Position object
        if (jObject != null) {
            JSONObject price;
            try {
                p.getAmount();
                price = jObject.getJSONObject(p.getCode());
                return p.getAmount().doubleValue() * price.getDouble(PRICE_IN_EURO);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }
}