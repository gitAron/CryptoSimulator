package com.example.aron.cryptosimulator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class BuyActivity extends AppCompatActivity implements AsyncResponse {

    CryptoSimulatorDatabase db;
    JsonTask jsonTask;
    JSONObject jObject;
    JSONObject price;
    String requestedCurrency;
    double xRate;
    int balance;
    long actualized;
    Spinner mySpinner;
    AsyncResponse r;
    String selected;
    String inputType;
    EditText et;
    TextView t;

    public static final String FOUR_DIGIT_DECIMAL = "##.00##";
    public static final String CRYPTOCURRENCY_INPUT = "crypto";
    public static final String FIAT_INPUT = "fiat";
    public static final String SHARED_PREFERENCES = "MyPrefsFile";
    public static final String BALANCE = "balance";
    public static final String TWO_DIGIT_DECIMAL = "##.00";
    public static final String DATE_PATTERN = "\"dd/MM/yyyy\"";
    public static final String PRICE_IN_EURO = "EUR";
    public static final String BUY = "buy";
    public static final String SELL = "sell";
    public static final String ZERO = "0";

    public static final int ONE_MINUTE = 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy);

        db = new CryptoSimulatorDatabase(this);
        db.open();

        setupSpinner();
        initBalanceUI();
        initInputWatcher();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));

        r = this;
    }

    public void setupSpinner() {
        //initiate the spinner
        Spinner spinner = (Spinner) findViewById(R.id.cryptocurrencies_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cryptocurrencies_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                executeJsonTask();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });
    }

    public void executeJsonTask() {
        jsonTask =new JsonTask();
        jsonTask.delegate = r;
        mySpinner=(Spinner) findViewById(R.id.cryptocurrencies_spinner);

        requestedCurrency = mySpinner.getSelectedItem().toString();
        jsonTask.execute(getString(R.string.json_string));
    }

    public void initInputWatcher() {
        //this method reacts to the user input
        //depending on the entered amount the price is displayed
        et = (EditText) findViewById(R.id.input_amount);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                TextView t = findViewById(R.id.cost_textview);
                DecimalFormat f = new DecimalFormat(FOUR_DIGIT_DECIMAL);

                if (s.toString().length() < 1 || inputType == null || selected == null) {
                    t.setText(ZERO);
                    return;
                }
                if (xRate != 0) {

                    if (inputType.equals(CRYPTOCURRENCY_INPUT)) {
                        t.setText(f.format(xRate * Double.parseDouble(s.toString())));
                    }

                    if (inputType.equals(FIAT_INPUT)) {
                        t.setText(f.format(Double.parseDouble(s.toString())));
                    }
                }
            }
        });
    }

    public void initBalanceUI() {
        SharedPreferences mPreferences = this.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        int balance = mPreferences.getInt(BALANCE, 0);
        TextView t = findViewById(R.id.balance_textview);
        DecimalFormat f = new DecimalFormat(TWO_DIGIT_DECIMAL);
        t.setText(f.format((double) balance / 100) + getString(R.string.euro_symbol));
    }

    @Override
    public void processFinish(String output){

        try {
            jObject = new JSONObject(output);
            price = jObject.getJSONObject(mySpinner.getSelectedItem().toString());
            TextView t = findViewById(R.id.price_text);
            xRate = price.getDouble(PRICE_IN_EURO);
            DecimalFormat f = new DecimalFormat(FOUR_DIGIT_DECIMAL);
            t.setText(getString(R.string.text_price) + f.format(xRate));

            actualized = System.currentTimeMillis();
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void buy() {
        //method that gets called if the user buys a cryptocurrency
        EditText et = (EditText) findViewById(R.id.input_amount);
        if(!checkTransaction()) return;

        BigDecimal amount;
        int price; // price in Eurocents
        if(inputType.equals(CRYPTOCURRENCY_INPUT)) {
            amount = new BigDecimal(Double.parseDouble(et.getText().toString()));
            price = (int) (amount.multiply(new BigDecimal(xRate)).doubleValue() * 100);
        } else if(inputType.equals(FIAT_INPUT)) {
            amount = new BigDecimal(Double.parseDouble(et.getText().toString()) / xRate);
            price = new BigDecimal(Double.parseDouble(et.getText().toString())* 100).intValue();
        } else {
            Toast.makeText(this, getString(R.string.text_wrong_input), Toast.LENGTH_LONG).show();
            return;
        }

        balance = getBalance();

        if (balance < price) {
            // convert amount * xRate to Cents with * 100
            Toast.makeText(this, getString(R.string.insufficient_funds_message), Toast.LENGTH_LONG).show();
            return;
        }
        double paid = Math.round(amount.multiply(new BigDecimal(xRate)).doubleValue() * 100);
        changeBalance(- (int) paid);

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Transaction t = new Transaction(requestedCurrency, amount, paid, new SimpleDateFormat(DATE_PATTERN).format(ts));
        Log.d("transaction id: ", "id: " + db.insertTransaction(t));
        Log.d("position ", "created/updated: " + db.updatePosition(amount, requestedCurrency));

        Toast.makeText(this, getString(R.string.transaction_successful_message), Toast.LENGTH_LONG).show();
        initBalanceUI();
    }

    public void sell() {
        //method that gets called if the user buys a cryptocurrency

        EditText et = (EditText) findViewById(R.id.input_amount);
        if(!checkTransaction()) return;

        BigDecimal amount;
        int price; // price in Eurocents
        if(inputType.equals(CRYPTOCURRENCY_INPUT)) {
            amount = new BigDecimal(Double.parseDouble(et.getText().toString()));
            price = (int) (amount.multiply(new BigDecimal(xRate)).doubleValue() * 100);
        } else if(inputType.equals(FIAT_INPUT)) {
            amount = new BigDecimal(Double.parseDouble(et.getText().toString()) / xRate);
            price = new BigDecimal(Double.parseDouble(et.getText().toString())* 100).intValue();
        } else {
            Toast.makeText(this, getString(R.string.text_no_input_type), Toast.LENGTH_LONG).show();
            return;
        }

        BigDecimal amountCurrency = new BigDecimal(db.getPosition(requestedCurrency));

        if (amountCurrency.subtract(amount).doubleValue() < 0) {
            // convert amount * xRate to Cents with * 100
            Toast.makeText(this, getString(R.string.insufficient_currency_message), Toast.LENGTH_LONG).show();
            return;
        }

        changeBalance(price);

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Transaction t = new Transaction(requestedCurrency, amount.negate(), - price, new SimpleDateFormat(DATE_PATTERN).format(ts));
        Log.d("transaction id: ", "id: " + db.insertTransaction(t));
        Log.d("position ", "created/updated: " + db.updatePosition(amount.negate(), requestedCurrency));

        Toast.makeText(this, getString(R.string.choose_input_type_message), Toast.LENGTH_LONG).show();
        initBalanceUI();
    }

    public void onCTransaction(View v) {
        //after the click of the transaction button this method delegates the call
        //to either the buy() or the sell() method
        if(inputType != null && selected != null) {
            if (selected.equals(BUY)) buy();
            if (selected.equals(SELL)) sell();
        }
    }

    public void changeBalance(int difference) {
        SharedPreferences mPreferences = this.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(BALANCE, getBalance() + difference);
        editor.commit();
    }

    public int getBalance() {
        SharedPreferences mPreferences = this.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return mPreferences.getInt(BALANCE, 0);
    }

    public boolean checkTransaction() {
        //verify that the displayed price is not too old
        if (System.currentTimeMillis() - actualized > ONE_MINUTE) {
            Toast.makeText(this, getString(R.string.not_actualized_message), Toast.LENGTH_LONG).show();
            executeJsonTask();
            return false;
        }

        EditText et =
                (EditText) findViewById(R.id.input_amount);
        if (et.getText().toString().length() < 1) return false;

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //configurate the toolbar
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_transaction_history:
                intent = new Intent(BuyActivity.this, TransactionsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_settings:
                intent = new Intent(BuyActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_home:
                intent = new Intent(BuyActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //intent.putExtra("EXIT", true);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar, menu);
        MenuItem settingsItem = menu.findItem(R.id.action_add_transaction);
        settingsItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    public void onRadioButtonClicked(View view) {
        //handle the input for the radio buttons
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.radio_buy:
                if (checked)
                    selected = BUY;
                initInputWatcher();
                    break;
            case R.id.radio_sell:
                if (checked)
                    selected = SELL;
                    break;
            case R.id.radio_currency_amount:
                if (checked)
                    inputType = CRYPTOCURRENCY_INPUT;
                initInputWatcher();
                break;
            case R.id.radio_euro_amount:
                if (checked)
                    inputType = FIAT_INPUT;
                break;
        }
    }
}