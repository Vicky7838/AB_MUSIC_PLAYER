package com.music.player.bhandari.m.activity;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MyApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Amit Bhandari on 2/11/2017.
 */

public class RemoveAdsActivity extends AppCompatActivity {

    IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);

            //check if user has already removed ads
            //in case, he removed ads, and reinstalled app
            Bundle ownedItems = null;
            try {
                ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
            } catch (RemoteException e) {

                e.printStackTrace();
                return;
            }

            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> ownedSkus =
                        ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");

                    for (int i = 0; i < ownedSkus.size(); ++i) {
                        String sku = ownedSkus.get(i);
                        if (sku.equals(getString(R.string.donate_beer))) {
                            startActivity(new Intent(RemoveAdsActivity.this,SettingsActivity.class));
                            Toast.makeText(getApplicationContext(), "You already have baught me beer!",Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }else if(sku.equals(getString(R.string.donate_beer_box))){
                            startActivity(new Intent(RemoveAdsActivity.this,SettingsActivity.class));
                            Toast.makeText(getApplicationContext(), "You already have baught me beer box!",Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }else if(sku.equals(getString(R.string.donate_beer))){
                            startActivity(new Intent(RemoveAdsActivity.this,SettingsActivity.class));
                            Toast.makeText(getApplicationContext(), "You already have baught me coffee!",Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                }
                // if continuationToken != null, call getPurchases again
                // and pass in the token to retrieve more items
            }

            //start buy procedure
            String product_id = "";
            switch (getIntent().getIntExtra("donate_type",-1)){
                case Constants.DONATE.BEER:
                    product_id = getString(R.string.donate_beer);
                    break;

                case Constants.DONATE.BEER_BOX:
                    product_id = getString(R.string.donate_beer_box);
                    break;

                case Constants.DONATE.COFFEE:
                    product_id = getString(R.string.donate_coffee);
                    break;

                default:
                    startActivity(new Intent(RemoveAdsActivity.this,SettingsActivity.class));
                    finish();
            }
            //String product_id = "android.test.purchased";
            Bundle buyIntentBundle = null;
            try {
                buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                        product_id , "inapp", "");
            }catch (Exception e){

            }
            if(buyIntentBundle.getInt("RESPONSE_CODE", 1)==0) {
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

                try {
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                            Integer.valueOf(0));
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }


        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Toast.makeText(getApplicationContext(), "Thank you for you contribution towards the app Development!"
                            ,Toast.LENGTH_LONG).show();
                }
                catch (JSONException e) {
                   /* alert("Failed to parse purchase data.");*/
                    Toast.makeText(this,"FAILED!",Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }else {
                Toast.makeText(getApplicationContext(),"Transaction failed!",Toast.LENGTH_LONG).show();

            }
        }
        startActivity(new Intent(RemoveAdsActivity.this,SettingsActivity.class));
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }

}
