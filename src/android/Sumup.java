package com.sumup.cordova.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sumup.merchant.api.SumUpAPI;
import com.sumup.merchant.api.SumUpPayment;
import com.sumup.merchant.api.SumUpState;
import com.sumup.merchant.api.SumUpLogin;
import com.sumup.merchant.Models.TransactionInfo;
import com.sumup.merchant.Models.Merchant;

import java.util.UUID;
import java.math.BigDecimal;

public class Sumup extends CordovaPlugin {
  private static final String TAG = "SumUp";

  private static final int REQUEST_CODE_LOGIN = 1;
  private static final int REQUEST_CODE_PAYMENT = 2;
  private static final int REQUEST_CODE_SETTINGS = 3;

  private CallbackContext callback = null;
  private String affiliateKey = null;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
      super.initialize(cordova, webView);

      SumUpState.init(cordova.getActivity());

      affiliateKey = this.cordova.getActivity().getString(cordova.getActivity().getResources().getIdentifier("SUMUP_API_KEY", "string", cordova.getActivity().getPackageName()));

  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    if (action.equals("pay")) {
      BigDecimal amount = null;

      try {
        amount = new BigDecimal(args.getString(0));
      } catch (Exception e) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Can't parse amount"));
      }

      SumUpPayment.Currency currency = null;
      try {
        currency = SumUpPayment.Currency.valueOf(args.getString(1));
      } catch (Exception e) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Can't parse currency"));
      }

      callback = callbackContext;
      cordova.setActivityResultCallback(this);

      SumUpPayment.Builder payment = SumUpPayment.builder(amount, currency).skipSuccessScreen();
      SumUpAPI.checkout(this.cordova.getActivity(), payment.build(), REQUEST_CODE_PAYMENT);

      return true;
    }

    if (action.equals("login")) {
      callback = callbackContext;
      cordova.setActivityResultCallback(this);

      SumUpLogin sumUplogin = SumUpLogin.builder(affiliateKey).build();
      SumUpAPI.openLoginActivity(this.cordova.getActivity(), sumUplogin, REQUEST_CODE_LOGIN);
      return true;
    }

    if (action.equals("logout")) {
      SumUpAPI.logout();
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
      return true;
    }

    if (action.equals("isLoggedIn")) {
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, SumUpAPI.isLoggedIn()));
      return true;
    }

    if (action.equals("settings")) {
      callback = callbackContext;
      cordova.setActivityResultCallback(this);

      SumUpAPI.openPaymentSettingsActivity(this.cordova.getActivity(), REQUEST_CODE_SETTINGS);
      return true;
    }

    if (action.equals("prepareForCheckout")) {
      SumUpAPI.prepareForCheckout();
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
      return true;
    }

    return false; // raises "MethodNotFound"
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // no intent data given: Sumup activity has been cancelled
    if (data == null) {
      callback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, getErrorMessage(requestCode, resultCode, "Action cancelled")));
      return;
    }

    Bundle extra = data.getExtras();
    String message = extra.getString(SumUpAPI.Response.MESSAGE);
    int code = extra.getInt(SumUpAPI.Response.RESULT_CODE);

    if (code != SumUpAPI.Response.ResultCode.SUCCESSFUL) {
      callback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, getErrorMessage(requestCode, code, message)));
      return;
    }

    JSONObject res = new JSONObject();

    try {
      res.put("code", code);
      res.put("message", message);
    } catch (Exception e) {}

    if (requestCode == REQUEST_CODE_LOGIN) {
      try {
        Merchant currentMerchant = SumUpAPI.getCurrentMerchant();
        res.put("merchantCode", currentMerchant.getMerchantCode());
        res.put("merchantCurrency", currentMerchant.getCurrency().toString());
      } catch (Exception e) {
        Log.e(TAG, "Error parsing login result", e);
      }
    }

    if (requestCode == REQUEST_CODE_PAYMENT) {
      try {
        res.put("txcode", extra.getString(SumUpAPI.Response.TX_CODE));

        // get additional transaction details
        TransactionInfo info = (TransactionInfo) extra.get(SumUpAPI.Response.TX_INFO);

        res.put("txid", info.getForeignTransactionId());
        res.put("amount", info.getAmount());
        res.put("vat_amount", info.getVatAmount());
        res.put("tip_amount", info.getTipAmount());
        res.put("currency", info.getCurrency());
        res.put("status", info.getStatus());
        res.put("payment_type", info.getPaymentType());
        res.put("card_type", info.getCard().getType());
        res.put("card_last4digits", info.getCard().getLast4Digits());

      } catch (Exception e) {
        Log.e(TAG, "Error parsing payment result", e);
      }
    }

    PluginResult result = new PluginResult(PluginResult.Status.OK, res);
    result.setKeepCallback(true);
    callback.sendPluginResult(result);
  }

  private String getErrorMessage(int requestCode, int resultCode, String message) {
    int errClass = 1;
    int errCode = 0;

    switch (requestCode) {
      case REQUEST_CODE_PAYMENT:
        errClass = 2;
        break;
      case REQUEST_CODE_SETTINGS:
        errClass = 3;
        break;
      default:
        errClass = 0;
    }

    switch (resultCode) {
      case SumUpAPI.Response.ResultCode.ERROR_ALREADY_LOGGED_IN:
        errCode = 22;
        break;
      default:
        errCode = resultCode;
    }

    return String.format("Error %d%02d: %s", errClass, errCode, message);
  }
}
