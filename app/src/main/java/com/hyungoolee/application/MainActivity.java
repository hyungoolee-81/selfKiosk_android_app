package com.hyungoolee.application;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.loader.AssetsProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.EditText;
import android.widget.TextSwitcher;
import android.widget.Toast;

import com.escpos.app.resource.ResourceInstaller;
import com.rexod.escpos.usblib.Rexodusb;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WebView mWebView;           //웹뷰 선언
    private WebSettings mWebSettings;   //웹뷰 세팅
    private static final String TAG = "USB_PRINT";

    private Context mContext = null;
    private Rexodusb mUsbConn = null;
    private EditText edText;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        //세로 화면으로 고정
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);

        Toast.makeText(getApplicationContext(), "Rotation : " + getWindowManager().getDefaultDisplay().getRotation(), Toast.LENGTH_LONG).show();

        //가로 화면으로 고정
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        edText = (EditText) findViewById(R.id.hiddentext);
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        edText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String result = s.toString();
                if(result.endsWith("\n")){
                    inputMethodManager.hideSoftInputFromWindow(edText.getWindowToken(), 0);
                    mWebView.loadUrl("javascript:exFunction('"+result.trim()+"')");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // copy bitmap
        ResourceInstaller resource = new ResourceInstaller();
        resource.copyAssets(getAssets(), "temp/bitmap");
        //웹뷰 시작
        mWebView = (WebView) findViewById(R.id.webView);

        mWebView.setWebViewClient(new WebViewClient());                 //클릭시 새창 안뜨게
        mWebSettings = mWebView.getSettings();                          //세부 세팅 등록
        mWebView.setInitialScale(100); // 35%
        mWebSettings.setJavaScriptEnabled(true);                        //웹페이지 자바스크립트 허용 여부
        mWebSettings.setSupportMultipleWindows(false);                  //새창 듸우기 허용 여부
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false);   //자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        //mWebSettings.setLoadWithOverviewMode(true);                     //메타태그 허용 여부
        //mWebSettings.setUseWideViewPort(true);                          //화면 사이즈 맞추기 허용 여부
        //mWebSettings.setSupportZoom(true);                             //화면 줌 허용 여부
        //mWebSettings.setBuiltInZoomControls(true);                     //화면 확대 축소 허용
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);       //브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true);        //로컬저장소 허용 여부
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);
        mWebView.setFocusable(false);
        mWebView.requestFocus(View.FOCUS_DOWN);
        mWebView.addJavascriptInterface(new AndroidBridge(), "android2");
        mWebView.addJavascriptInterface(new AndroidBridge(), "urlCheck");
        mWebView.setLayerType(mWebView.LAYER_TYPE_HARDWARE, null);
        mWebView.loadUrl("http://10.15.5.102/self/index?co_div=435");

        //system resource
        mContext = getApplicationContext();

        mUsbConn = new Rexodusb(mContext);
        mUsbConn.Initialize();
        mUsbConn.Setchar("EUC-KR");
        int nResult = 0;
        nResult = mUsbConn.OpenPort();
        if (nResult == 0) {
            Toast.makeText(getApplicationContext(), "Printer Port Open", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Printer Port Open Fail", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * PRINT CALL
     */
    public class AndroidBridge {
        private final Handler handler = new Handler();

        @JavascriptInterface
        public void callAndroid(final String arg1, final String arg2, final String arg3, final String arg4, final String arg5, final String arg6) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    printing(arg1, arg2, arg3, arg4, arg5, arg6);
                }
            });
        }

        @JavascriptInterface
        public void urlCheck() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mWebView.getUrl(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
/*
    public void onPageFinished() {
        // Display the keyboard automatically when relevant
        if (mWebView.getUrl().equals("Login")) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mWebView, 0);
        }
    }
*/

    /**
     * CHECK-IN LOCKER PRINTER
     *
     * @param nowDate
     * @param couse_name
     * @param teeup_time
     * @param name
     * @param sex
     * @param locker
     */
    public void printing(String nowDate, String couse_name, String teeup_time, String name, String sex, String locker) {
        // TODO Auto-generated method stub

        char LF = 0x0a;
        char ESC = 0x1b;
        int nRes;

        mUsbConn.PrintBitMap("//sdcard//temp//bitmap//guide_b_logo2.bmp", 1);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + "즐거운 라운드 되십시오." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + nowDate + " " + teeup_time + " " + couse_name + " " + name + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|4C" + "  락커번호      " + locker + "  " + sex + " " + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|bC" + "------------------------------------------------" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + "★ 락카 사용 방법" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "1. 비밀번호 4자리를 입력합니다." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "2. \"삐삐\" 소리 후 잠깁니다." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "3. 비밀번호 4자리를 입력합니다." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "4. \"삐삐\" 소리 후 열립니다." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|bC" + "------------------------------------------------" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|cA" + "☆ 귀중품은 프론트에 보관하여 주십시오." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + "골프존카운티 무주(을)를" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + "방문해 주셔서 감사합니다." + LF);

        /* 순천
        mUsbConn.PrintBitMap("//sdcard//temp//bitmap//guide_b_logo2.bmp", 1);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + "즐거운 라운드 되십시오." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + nowDate + " " + teeup_time + " " + couse_name + " " + name + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|4C" + "  락커번호      " + locker + "  " + sex + " " + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|bC" + "------------------------------------------------" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + "▶ 락카 사용 방법" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "처음엔 열려 있습니다." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "잠글때 : 비밀번호" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "열  때 : 비밀번호" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|bC" + "------------------------------------------------" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + "▶ 해택을 챙겨주세요!" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "골프장 5회 라운드 시 50% 쿠폰 발급!!" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "인터넷회원으로 예약 시 그린피 할인" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|bC" + "------------------------------------------------" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|cA" + "귀중품은 프론트에 보관하여 주십시오." + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + "" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + "골프존카운티 순천(을)를" + LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + ESC + "|cA" + "방문해 주셔서 감사합니다." + LF);

        */
/*
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|2C" + "Print double wide"+ LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|3C" + "Print double high"+ LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|4C" + "Double Wide + high"+ LF);

        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|cA" + "Print center"+ LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|rA" + "Print right"+ LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|#uC" + "Print underline"+ LF);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|rvC" + "Print reverse"+ LF);
        mUsbConn.PrintNormal(ESC + "|N");
        mUsbConn.PrintBarCode("1234567890", mUsbConn.BCS_CODE39, 50, 2,mUsbConn.ALIGNMENT_CENTER, mUsbConn.BCS_HRI_BELOW);
        mUsbConn.PrintNormal(ESC + "|N" + ESC + "|#fp");// Feed and Paper cut
*/

        nRes = mUsbConn.Printingcomplete(3000);
        if (nRes != 0) {
            Toast.makeText(getApplicationContext(), "Print Fail", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Print OK", Toast.LENGTH_LONG).show();
        }
        mUsbConn.NlineFeed(4);
        mUsbConn.Partialcut();
    }
}