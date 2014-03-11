package com.example.lchat;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.firebase.client.Firebase;

public class MainActivity extends Activity {

	private WebView myWebView;
	private Menu mOptionsMenu;
	private String mGroupKey;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String username = tMgr.getLine1Number();

		// Load or generate password
		String key = getString(R.string.password);
		SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
		String password = getRandomString(10);
		if (pref.contains(key)) {
			password = pref.getString(key,  password);
		}
		else {
			SharedPreferences.Editor editor = pref.edit();
			editor.putString(key, password);
			editor.commit();			
		}
		
		String url = "http://doubledoodle.org/prototypes/lchat/#?username=";
		url += username;
		url += "&password=";
		url += password;
		
		myWebView = (WebView) findViewById(R.id.webview);
		myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient(){
            @Override  
            public void onPageFinished(WebView view, String url) {
            	if (mOptionsMenu != null) {
            		MenuItem item = mOptionsMenu.findItem(R.id.action_add_member);

            		String pattern = ".*/group/(.*)";
	            	Pattern r = Pattern.compile(pattern);
	            	Matcher m = r.matcher(url);
	            	if (m.find()) {
	            		mGroupKey = m.group(1);
	            		item.setVisible(true);	            		
	            	}
	            	else {
	            		mGroupKey = null;
	            		item.setVisible(false);
	            	}
            	}
            	super.onPageFinished(view, url);
            }
        });
        myWebView.loadUrl(url);
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
            case KeyEvent.KEYCODE_BACK:
                if(myWebView.canGoBack() == true){
                    myWebView.goBack();
                }else{
                    finish();
                }
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
    
	private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";
	private static String getRandomString(final int sizeOfRandomString)
	  {
	  final Random random=new Random();
	  final StringBuilder sb=new StringBuilder();
	  for(int i=0;i<sizeOfRandomString;++i)
	    sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
	  return sb.toString();
	  }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity_actions, menu);

		mOptionsMenu = menu;
		
		MenuItem item = menu.findItem(R.id.action_add_member);
		item.setVisible(false);
		item = menu.findItem(R.id.action_add_picture);
		item.setVisible(false);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	private static final int CONTACT_PICKER_RESULT = 1001;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_member:
            	Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
                return true;
            case R.id.action_add_picture:
                // TODO: select local picture
            	// TODO: upload file (S3?)
            	// TODO: call firebase
                return true;
            case R.id.action_refresh:
            	myWebView.reload();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case CONTACT_PICKER_RESULT:
                // handle contact results
            	Uri result = data.getData();
            	String id = result.getLastPathSegment();
            	
            	Cursor phones = getContentResolver().query(
            	        Phone.CONTENT_URI, null,
            	        Phone.CONTACT_ID + "=?",
            	        new String[]{id}, null);
            	String number = null;
            	while (phones.moveToNext()) {
                    int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
                    if (type == Phone.TYPE_MOBILE) {
                    	number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
                    	number = number.replaceAll("[^0-9]", "");
                    	break;
                    }
                }
                phones.close();
                
                if (number != null) {
                	if (mGroupKey != null) {
                    	Firebase groupMembersRef = new Firebase("https://lchat.firebaseio.com/groups/" + mGroupKey + "/members/");
                    	Firebase newGroupMember = groupMembersRef.push();
                    	newGroupMember.setValue(number);
                    	Firebase newMemberGroupsRef = new Firebase("https://lchat.firebaseio.com/users/" + number + "/groups");
                    	Firebase newMemberGroup = newMemberGroupsRef.push();
                    	newMemberGroup.setValue(mGroupKey);
                	}
                	else {
                    	Toast.makeText(getApplicationContext(), "No group for " + number, Toast.LENGTH_SHORT).show();
                	}
                }
                else {
                	Toast.makeText(getApplicationContext(), "No mobile number found!", Toast.LENGTH_SHORT).show();
                }
                break;
            }

        } else {
            // gracefully handle failure
            Log.w("DEBUG", "Warning: activity result not ok");
        }
    }

}
