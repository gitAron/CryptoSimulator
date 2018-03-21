package com.example.aron.cryptosimulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy);
        Spinner spinner = (Spinner) findViewById(R.id.cryptocurrencies_spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cryptocurrencies_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        db = new CryptoSimulatorDatabase(this);
        db.open();

        //doInternetThings();
        Log.v("doIT", " bitcoin Kurs: " + price);
        final AsyncResponse r = this;

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                jsonTask =new JsonTask();
                jsonTask.delegate = r;
                mySpinner=(Spinner) findViewById(R.id.cryptocurrencies_spinner);

                requestedCurrency = mySpinner.getSelectedItem().toString();
                jsonTask.execute("https://min-api.cryptocompare.com/data/pricemulti?fsyms=BTC,ETH,XRP,BCH,LTC,ADA,NEO,XLM,EOS,XMR&tsyms=EUR");

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        initBalanceUI();

        EditText et = (EditText) findViewById(R.id.input_amount);


        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {

                // TODO Auto-generated method stub
                TextView t = findViewById(R.id.price_text);
                DecimalFormat f = new DecimalFormat("##.00##");

                if(s.toString().length() < 1) {
                    t.setText("Price: " + 0);

                } else if(xRate != 0) {
                    Log.v("afterTC ", "s.toString: " + s.toString() + " S: " + s);
                    t.setText("Price: " + f.format(xRate * Double.parseDouble(s.toString())));
                }

            }
        });
    }

    public void initBalanceUI() {
        SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        int balance = mPreferences.getInt("balance", 0);
        TextView t = findViewById(R.id.balance_textview);

        DecimalFormat f = new DecimalFormat("##.00");

        t.setText(f.format((double) balance / 100));
    }

    @Override
    public void processFinish(String output){
        //Here you will receive the result fired from async class
        //of onPostExecute(result) method.

        try {
            jObject = new JSONObject(output);
            price = jObject.getJSONObject(mySpinner.getSelectedItem().toString());
            TextView t = findViewById(R.id.price_text);
            xRate = price.getDouble("EUR");
            DecimalFormat f = new DecimalFormat("##.00##");
            t.setText("Price: " + f.format(xRate));

            actualized = System.currentTimeMillis();
            Log.v("try block ", "timeinmillis: " + actualized);



        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void onCBuy(View v)
    {
        //check if sufficient funds are available

        EditText et = (EditText) findViewById(R.id.input_amount);
        if(!checkTransaction()) return;

        BigDecimal amount = new BigDecimal(Double.parseDouble(et.getText().toString()));

        balance = getBalance();

        if (balance < amount.multiply(new BigDecimal(xRate)).doubleValue() * 100) {
            // convert amount * xRate to Cents with * 100
            Toast.makeText(this, "You have insufficient funds to make this transaction", Toast.LENGTH_LONG).show();
            return;
        }
        double paid = Math.round(amount.multiply(new BigDecimal(xRate)).doubleValue() * 100);
        changeBalance(- (int) paid);

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Transaction t = new Transaction(requestedCurrency, amount, paid, new SimpleDateFormat("dd/MM/yyyy").format(ts));
        Log.d("transaction id: ", "id: " + db.insertTransaction(t));
        Log.d("position ", "created/updated: " + db.updatePosition(amount, requestedCurrency));

        Toast.makeText(this, "You have bought the Cryptocurrency", Toast.LENGTH_LONG).show();
        initBalanceUI();
    }


    public void onCSell(View v) {

        EditText et = (EditText) findViewById(R.id.input_amount);
        if(!checkTransaction()) return;

        BigDecimal amount = new BigDecimal(Double.parseDouble(et.getText().toString()));

        BigDecimal amountCurrency = new BigDecimal(db.getPosition(requestedCurrency));

        if (amountCurrency.subtract( amount).doubleValue() < 0) {
            // convert amount * xRate to Cents with * 100
            Toast.makeText(this, "You don´t own as much as you want to sell", Toast.LENGTH_LONG).show();
            return;
        }

        double plus = Math.round(amount.multiply(new BigDecimal(xRate)).doubleValue() * 100);
        changeBalance( (int) plus);

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Transaction t = new Transaction(requestedCurrency, amount.negate(), - plus, new SimpleDateFormat("dd/MM/yyyy").format(ts));
        Log.d("transaction id: ", "id: " + db.insertTransaction(t));
        Log.d("position ", "created/updated: " + db.updatePosition(amount.negate(), requestedCurrency));

        Toast.makeText(this, "You have sold the Cryptocurrency", Toast.LENGTH_LONG).show();
        initBalanceUI();
    }

    public void changeBalance(int difference) {
        SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt("balance", getBalance() + difference);
        editor.commit();
    }

    public int getBalance() {
        SharedPreferences mPreferences = this.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        return mPreferences.getInt("balance", 0);
    }

    public boolean checkTransaction() {
        if (System.currentTimeMillis() - actualized > 60000) {
            Toast.makeText(this, "Refresh the page before making a transaction", Toast.LENGTH_LONG).show();
            return false;
        }

        EditText et =
                (EditText) findViewById(R.id.input_amount);
        if (et.getText().toString().length() < 1) return false;

        return true;
    }

}