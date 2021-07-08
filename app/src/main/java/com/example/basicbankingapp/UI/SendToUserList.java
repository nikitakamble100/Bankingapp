package com.example.basicbankingapp.UI;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.basicbankingapp.DB.TransactionContract;
import com.example.basicbankingapp.DB.TransactionHelper;
import com.example.basicbankingapp.DB.UserContract;
import com.example.basicbankingapp.DB.UserHelper;
import com.example.basicbankingapp.Data.Transaction;
import com.example.basicbankingapp.Data.User;
import com.example.basicbankingapp.ListAdapters.SendToUserAdapter;
import com.example.basicbankingapp.ListAdapters.UserListAdapter;
import com.example.basicbankingapp.R;
import com.example.basicbankingapp.DB.TransactionContract.TransactionEntry;
import com.example.basicbankingapp.DB.UserContract.UserEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class SendToUserList extends AppCompatActivity implements SendToUserAdapter.OnUserListener {

    RecyclerView recyclerView;
    RecyclerView.Adapter myAdapter;
    RecyclerView.LayoutManager layoutManager;
    ArrayList<User> userArrayList;

    private UserHelper dbHelper;

    String date=null, time=null;
    int fromUserAccountNo, toUserAccountNo, toUserAccountBalance;
    String fromUserAccountName, fromUserAccountBalance, transferAmount, toUserAccountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_to_user);

        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy, hh:mm a");
        String date_and_time = simpleDateFormat.format(calendar.getTime());

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            fromUserAccountName = bundle.getString("FROM_USER_NAME");
            fromUserAccountNo = bundle.getInt("FROM_USER_ACCOUNT_NO");
            fromUserAccountBalance = bundle.getString("FROM_USER_ACCOUNT_BALANCE");
            transferAmount = bundle.getString("TRANSFER_AMOUNT");
        }

        userArrayList = new ArrayList<User>();
        dbHelper = new UserHelper(this);
        recyclerView = findViewById(R.id.send_to_user_list);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        myAdapter = new SendToUserAdapter(userArrayList, this);
        recyclerView.setAdapter(myAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        displayDatabaseInfo();
    }

    @Override
    public void onUserClick(int position) {
        toUserAccountNo = userArrayList.get(position).getAccountNumber();
        toUserAccountName = userArrayList.get(position).getName();
        toUserAccountBalance = userArrayList.get(position).getBalance();

        calculateAmount();

        new TransactionHelper(this).insertTransferData(fromUserAccountName, toUserAccountName, transferAmount, 1);
        Toast.makeText(this, "Transaction Successful!!", Toast.LENGTH_LONG).show();

        startActivity(new Intent(SendToUserList.this, HomeScreen.class));
        finish();
    }

    private void calculateAmount() {
        int currentAmount = Integer.parseInt(fromUserAccountBalance);
        int transferAmountInt = Integer.parseInt(transferAmount);
        int remainingAmount = currentAmount - transferAmountInt;
        int increasedAmount = transferAmountInt + toUserAccountBalance;
        new UserHelper(this).updateAmount(fromUserAccountNo, remainingAmount);
        new UserHelper(this).updateAmount(toUserAccountNo, increasedAmount);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder_exitButton = new AlertDialog.Builder(SendToUserList.this);
        builder_exitButton.setTitle("Do you want to cancel the transaction?").setCancelable(false)
                .setPositiveButton ("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialogInterface, int i) {
                        // Transactions Cancelled
                        TransactionHelper dbHelper = new TransactionHelper(getApplicationContext());
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();

                        values.put(TransactionEntry.COLUMN_FROM_NAME, fromUserAccountName);
                        values.put(TransactionEntry.COLUMN_TO_NAME, toUserAccountName);
                        values.put(TransactionEntry.COLUMN_STATUS, 0);
                        values.put(TransactionEntry.COLUMN_AMOUNT, transferAmount);

                        db.insert(TransactionEntry.TABLE_NAME, null, values);

                        Toast.makeText(SendToUserList.this, "Transaction Cancelled!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SendToUserList.this, UsersList.class));
                        finish();
                    }
                }).setNegativeButton("No", null);
        AlertDialog alertExit = builder_exitButton.create();
        alertExit.show();
    }

    private void displayDatabaseInfo() {
        // Create and/or open a database to read from it
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                UserEntry.COLUMN_USER_NAME,
                UserEntry.COLUMN_USER_ACCOUNT_BALANCE,
                UserEntry.COLUMN_USER_ACCOUNT_NUMBER,
                UserEntry.COLUMN_USER_PHONE_NO,
                UserEntry.COLUMN_USER_EMAIL,
                UserEntry.COLUMN_USER_IFSC_CODE,
        };

        Cursor cursor = db.query(
                UserEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);

        try {
            int phoneNoColumnIndex = cursor.getColumnIndex(UserEntry.COLUMN_USER_PHONE_NO);
            int emailColumnIndex = cursor.getColumnIndex(UserEntry.COLUMN_USER_EMAIL);
            int ifscCodeColumnIndex = cursor.getColumnIndex(UserEntry.COLUMN_USER_IFSC_CODE);
            int accountNumberColumnIndex = cursor.getColumnIndex(UserEntry.COLUMN_USER_ACCOUNT_NUMBER);
            int nameColumnIndex = cursor.getColumnIndex(UserEntry.COLUMN_USER_NAME);
            int accountBalanceColumnIndex = cursor.getColumnIndex(UserEntry.COLUMN_USER_ACCOUNT_BALANCE);

            while (cursor.moveToNext()) {
                String currentName = cursor.getString(nameColumnIndex);
                int accountNumber = cursor.getInt(accountNumberColumnIndex);
                String email = cursor.getString(emailColumnIndex);
                String phoneNumber = cursor.getString(phoneNoColumnIndex);
                String ifscCode = cursor.getString(ifscCodeColumnIndex);
                int accountBalance = cursor.getInt(accountBalanceColumnIndex);

                userArrayList.add(new User(currentName, accountNumber, phoneNumber, ifscCode, accountBalance, email));
            }
        } finally {

            cursor.close();
        }
    }
}