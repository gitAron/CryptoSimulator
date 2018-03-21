package com.example.aron.cryptosimulator;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements AsyncResponse{

    //ArrayList<String> transactions;
    //ArrayAdapter<String> transactions_adapter;
    CryptoSimulatorDatabase db;
    ArrayList<Position> positions;
    private PositionAdapter positions_adapter;

    private boolean firstTime;
    private JSONObject jObject;
    JsonTask jsonTask;
    Context c;
    AsyncResponse ar;
    Timestamp actualized;

    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDB();
        initListAdapter();

        if(isFirstTime()) showDialog();

        initBalanceUI();

        initPositionUI();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        c= this;
        ar = this;

        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                Log.v("v", "onrefreshlistener");
                jsonTask =new JsonTask();
                jsonTask.delegate = ar;

                jsonTask.execute("https://min-api.cryptocompare.com/data/pricemulti?fsyms=BTC,ETH,XRP,BCH,LTC,ADA,NEO,XLM,EOS,XMR&tsyms=EUR");

            }
        });

    }

    public void refreshActualized() {
        TextView t = findViewById(R.id.portfolio_value_actualized);
        actualized = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss");

        t.setText("Last actualized: "+dateFormatter.format(actualized));
    }


    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        switch (item.getItemId())
        {
            case R.id.action_transaction_history: //Your task
                intent = new Intent(MainActivity.this, TransactionsActivity.class);
                Log.v("v", "inside onNavTrans");
                startActivity(intent);
                return true;

            case R.id.action_add_transaction: //Your task

                intent = new Intent(MainActivity.this, BuyActivity.class);
            startActivity(intent);
            return true;

            case R.id.action_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;


            default:return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void initPositionUI() {

    }

    public void initBalanceUI() {
        SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        int balance = mPreferences.getInt("balance", 0);
        TextView t = findViewById(R.id.balance_textview);

        DecimalFormat f = new DecimalFormat("##.00");
        t.setText(f.format((double) balance / 100) + "€");
    }

    private boolean isFirstTime() {
            SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
            return mPreferences.getBoolean("firstTime", true);
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose your starting money (Euro/€)");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);
        final Context c = this;

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleDialogInput(Integer.parseInt(input.getText().toString()));
                SharedPreferences mPreferences = c.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean("firstTime", false);
                editor.commit();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void handleDialogInput(int input) {
        if(input <50 || input > 1000000) {
            Toast.makeText(this, "Choose a value between 50 and 1000000", Toast.LENGTH_LONG).show();
            showDialog();
        } else {
            saveStartingMoneyInput(input);
        }
    }

    public void saveStartingMoneyInput(int input) {
        Log.v("v", "inside savesmi: " + input);

        SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        //firstTime = mPreferences.getInt("balance", 0);
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putInt("balance", input*100);
            editor.commit();

            initBalanceUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initListAdapter();
        initBalanceUI();
    }

    public void initDB() {
        db = new CryptoSimulatorDatabase(this);
        db.open();
    }

    @Override
    public void processFinish(String output){
        //Here you will receive the result fired from async class
        //of onPostExecute(result) method.
        Log.v("processFinish MA", "hey hey heeyyyy ");

        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

        try {
            jObject = new JSONObject(output);
            updateListAdapter();

        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
        refreshActualized();
    }

    private void updateListAdapter() {
        ListView list = (ListView) findViewById(R.id.positions_list);
        positions = db.getAllPositions();
        Log.v("MainActivity, iLA", "positionsArray: " + positions);
        positions_adapter = new PositionAdapter(this, positions, jObject);
        list.setAdapter(positions_adapter);
        positions_adapter.notifyDataSetChanged();
        initPortfolioValue();
    }

    private void initListAdapter() {
        updateListAdapter();

        jsonTask =new JsonTask();
        jsonTask.delegate = this;
        //Log.v("getView", "hey hey heeeyyyy");

        jsonTask.execute("https://min-api.cryptocompare.com/data/pricemulti?fsyms=BTC,ETH,XRP,BCH,LTC,ADA,NEO,XLM,EOS,XMR&tsyms=EUR");
    }

    public void initPortfolioValue() {
        TextView t = (TextView) findViewById(R.id.portfolio_value_textview);
         JSONObject price;

         double portfoltioV = 0;

        for(Position p : positions) {
            if (jObject != null) {
                try {
                    p.getAmount();
                    price = jObject.getJSONObject(p.getCode());
                    portfoltioV += p.getAmount().doubleValue() * price.getDouble("EUR");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        DecimalFormat f = new DecimalFormat("##.00");
        t.setText("" + f.format(portfoltioV) + "€");
    }
}