package com.example.aron.cryptosimulator;

/**
 * Created by Aron on 10.03.2018.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class TransactionAdapter extends ArrayAdapter<Transaction> {
    private ArrayList<Transaction> transactions;
    private Context context;

    public TransactionAdapter(Context context, ArrayList<Transaction> transactions) {
        super(context, R.layout.transaction_list, transactions);
        this.context = context;
        this.transactions = transactions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.transaction_list, null);

        }

        Transaction transaction = transactions.get(position);

        if (transaction != null) {
            TextView transCode = v.findViewById(R.id.transaction_code);
            TextView transDate = v.findViewById(R.id.transaction_date);
            TextView transAmount = v.findViewById(R.id.transaction_amount);
            TextView transEurAmount = v.findViewById(R.id.transaction_eur_amount);


            double eurAmount = transaction.getEurAmount() / 100;
            DecimalFormat f = new DecimalFormat("##.00");

            transEurAmount.setText(f.format(eurAmount));

            f = new DecimalFormat("##.00#####");

            transCode.setText(transaction.getCurrency());
            transDate.setText(transaction.getFormattedDate());
            transAmount.setText(""+f.format(transaction.getAmount()));

        }

        return v;
    }

}