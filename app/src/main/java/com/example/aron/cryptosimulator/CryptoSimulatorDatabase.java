package com.example.aron.cryptosimulator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by Aron on 10.03.2018.
 */

public class CryptoSimulatorDatabase {

    //the database has two tables: transactions and positions.
    //additionally, it offers methods to save or retrieve data.
    private static final String DATABASE_NAME = "cryptosimulator.db";
    private static final int DATABASE_VERSION = 1;
    private static final double DELETE_THRESHOLD = 0.0000001;

    private static final String DATABASE_TABLE_TRANSACTIONS = "transactions";
    private static final String DATABASE_TABLE_POSITIONS = "positions";
    public static final String KEY_ID_TRANSACTION = "_transaction_id";
    public static final String KEY_CODE = "currency_code";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_DATE = "date";
    public static final String KEY_EUR_AMOUNT = "amount_eur";

    private ToDoDBOpenHelper dbHelper;
    private SQLiteDatabase db;
    private double portfolioValue;

    public CryptoSimulatorDatabase(Context context) {

        dbHelper = new ToDoDBOpenHelper(context, DATABASE_NAME, null,
                DATABASE_VERSION);
    }

    public void open() throws SQLException {

        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLException e) {
            db = dbHelper.getReadableDatabase();
        }
    }

    public void close() {
        db.close();
    }

    public long insertTransaction(Transaction t) {

        ContentValues v = new ContentValues();
        v.put(KEY_CODE, t.getCurrency());
        v.put(KEY_AMOUNT, t.getAmount().doubleValue());
        v.put(KEY_DATE, t.getFormattedDate());
        v.put(KEY_EUR_AMOUNT, Math.round(t.getEurAmount()));

        long newInsertId = db.insert(DATABASE_TABLE_TRANSACTIONS, null, v);
        return newInsertId;
    }

    public void removeTransaction(Transaction t) {

        String[] deleteArgs = new String[]{String.valueOf(t.getCurrency()), t.getFormattedDate()};
        db.delete(DATABASE_TABLE_TRANSACTIONS, KEY_CODE+"=? and " + KEY_DATE+"=?", deleteArgs);
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> transactions = new ArrayList<Transaction>();
        Cursor cursor = db.query(DATABASE_TABLE_TRANSACTIONS, new String[]{KEY_ID_TRANSACTION, KEY_CODE, KEY_AMOUNT, KEY_EUR_AMOUNT,
                KEY_DATE}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String code = cursor.getString(1);
                BigDecimal amount = new BigDecimal(cursor.getDouble(2));
                double eurAmount = cursor.getDouble(3);
                String date = cursor.getString(4);

                transactions.add(new Transaction(code, amount, eurAmount, date));
            } while (cursor.moveToNext());
        }
        return transactions;
    }

    private class ToDoDBOpenHelper extends SQLiteOpenHelper {
        private static final String CREATE_TABLE_TRANSACTIONS = "create table "
                + DATABASE_TABLE_TRANSACTIONS + " (" + KEY_ID_TRANSACTION
                + " integer primary key autoincrement, " + KEY_CODE
                + " text not null, " + KEY_AMOUNT + " decimal(28,8) not null, "
                + KEY_EUR_AMOUNT + " decimal(19,2) not null, "
                + KEY_DATE + " text)";

        private static final String CREATE_TABLE_POSITIONS = "create table "
                + DATABASE_TABLE_POSITIONS + " (" + KEY_CODE
                + " text primary key, " + KEY_AMOUNT + " decimal(28,8) not null)";

        public ToDoDBOpenHelper(Context c, String dbname,
                                SQLiteDatabase.CursorFactory factory, int version) {
            super(c, dbname, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_TRANSACTIONS);
            db.execSQL(CREATE_TABLE_POSITIONS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public boolean updatePosition(BigDecimal amount, String code) {

        ContentValues v = new ContentValues();
        if (getPosition(code) != 0) {
            BigDecimal oldAmount = new BigDecimal(getPosition(code));
            double newAmount = 0;
            newAmount = oldAmount.add(amount).doubleValue();
            if(newAmount < DELETE_THRESHOLD ) {
                deletePosition(code);
                return true;
            }
            v.put(KEY_CODE, code);
            v.put(KEY_AMOUNT, newAmount);

            db.update(DATABASE_TABLE_POSITIONS, v, KEY_CODE + "=?" , new String[]{code});
            return true;
        }
        createNewPosition(code, amount.doubleValue());

        return false;
    }

    public void deletePosition(String code) {

        String[] deleteArgs = new String[]{code};
        db.delete(DATABASE_TABLE_POSITIONS, KEY_CODE+"=?", deleteArgs);
    }

    public boolean createNewPosition(String code, double amount) {

        ContentValues v = new ContentValues();
        v.put(KEY_CODE, code);
        v.put(KEY_AMOUNT, amount);
        db.insert(DATABASE_TABLE_POSITIONS, null, v);
        return true;
    }

    public double getPosition(String code) {
        Cursor cursor = db.query(DATABASE_TABLE_POSITIONS, new String[]{KEY_CODE, KEY_AMOUNT}, KEY_CODE + "=?",
                new String[]{code}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String c = cursor.getString(0);
                double amount = cursor.getDouble(1);

                return amount;
            } while (cursor.moveToNext());
        }
        return 0;
    }

    public ArrayList<Position> getAllPositions() {
        ArrayList<Position> positions = new ArrayList<Position>();
        Cursor cursor = db.query(DATABASE_TABLE_POSITIONS, new String[]{KEY_CODE, KEY_AMOUNT},
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            portfolioValue = 0;
            do {
                String code = cursor.getString(0);
                BigDecimal amount = new BigDecimal(cursor.getDouble(1));

                portfolioValue += cursor.getDouble(1);
                positions.add(new Position(code, amount));

            } while (cursor.moveToNext());
        }
        return positions;
    }
    public double getPortfolioValue() {
        return portfolioValue;
    }
}