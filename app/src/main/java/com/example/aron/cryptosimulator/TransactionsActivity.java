package com.example.aron.cryptosimulator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Aron on 10.03.2018.
 */

public class TransactionsActivity extends AppCompatActivity {

    //The TransactionsActivity java class is mainly responsible to display a list of
    //all transactions to a user.

    private ArrayList<Transaction> transactions;
    private TransactionAdapter transactions_adapter;
    private  CryptoSimulatorDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        initTransactionsList();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));
    }

    private void initTransactionsList() {
        //get all transaction from the database
        db = new CryptoSimulatorDatabase(this);
        db.open();
        transactions = db.getAllTransactions();
        initListAdapter();
    }

    private void initListAdapter() {
        //set up the TransactionAdapter
        ListView list = (ListView) findViewById(R.id.transactions_list);
        transactions_adapter = new TransactionAdapter(this, transactions);
        list.setAdapter(transactions_adapter);
        transactions_adapter.notifyDataSetChanged();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_home:
                intent = new Intent(TransactionsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            case R.id.action_add_transaction:
                intent = new Intent(TransactionsActivity.this, BuyActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_settings:
                intent = new Intent(TransactionsActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar, menu);

        MenuItem settingsItem = menu.findItem(R.id.action_transaction_history);
        settingsItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }
}