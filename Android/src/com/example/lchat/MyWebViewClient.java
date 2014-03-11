package com.example.lchat;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class MyWebViewClient extends WebViewClient {
	@Override
	public void onPageFinished (WebView view, String url) {
		super.onPageFinished(view, url);
	}
}
