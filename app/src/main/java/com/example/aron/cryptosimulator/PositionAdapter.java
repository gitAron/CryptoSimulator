package com.example.aron.cryptosimulator;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by Aron on 10.03.2018.
 */

public class PositionAdapter extends ArrayAdapter<Position> {

    private ArrayList<Position> positions;
    private Context context;
    JsonTask jsonTask;
    JSONObject jObject;
    JSONObject price;
    double xRate;
    String code;
    View v;

    public PositionAdapter(Context context, ArrayList<Position> positions, JSONObject jObject) {
        super(context, R.layout.position_list, positions);
        this.context = context;
        this.positions = positions;
        Log.v("Constructor PA",  " Object = null - enough for a crash?");
        this.jObject = jObject;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.position_list, null);

        }

        Position pos = positions.get(position);


        if (pos != null) {
            TextView posCode = (TextView) v.findViewById(R.id.position_code);
            TextView posAmount = (TextView) v.findViewById(R.id.position_amount);
            TextView posValue = (TextView) v.findViewById(R.id.position_value);

            code = pos.getCode();


            //jsonTask =new JsonTask();
            //jsonTask.delegate = this;
            //Log.v("getView", "hey hey heeeyyyy");

            //jsonTask.execute("https://min-api.cryptocompare.com/data/pricemulti?fsyms=BTC,ETH,XRP,BCH,LTC,ADA,NEO,XLM,EOS,XMR&tsyms=EUR");
            DecimalFormat f = new DecimalFormat("##.00#####");
            posCode.setText(pos.getCode());
            posAmount.setText("" + f.format(pos.getAmount()));

            try {
            if(jObject != null) {
                price = jObject.getJSONObject(code);
                TextView t = v.findViewById(R.id.position_value);
                f = new DecimalFormat("##.00");

                t.setText(f.format(pos.getAmount().multiply(new BigDecimal(price.getDouble("EUR")))));
            }
            } catch(JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return v;
    }
}
