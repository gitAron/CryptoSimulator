package com.example.aron.cryptosimulator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Aron on 10.03.2018.
 */

public class TransactionsActivity extends AppCompatActivity {

    private ArrayList<Transaction> transactions;
    private TransactionAdapter transactions_adapter;
    private  CryptoSimulatorDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("v", "inside onCreate TransactionsActivity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        initTransactionsList();
    }


    private void initTransactionsList() {
        db = new CryptoSimulatorDatabase(this);
        db.open();
        transactions = db.getAllTransactions();
        initListAdapter();
    }

    private void initListAdapter() {
        ListView list = (ListView) findViewById(R.id.transactions_list);
        transactions_adapter = new TransactionAdapter(this, transactions);
        list.setAdapter(transactions_adapter);
        transactions_adapter.notifyDataSetChanged();
    }
}
