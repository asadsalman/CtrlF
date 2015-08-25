package com.example.asad.ctrlf;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

public class FindActivity extends Activity {
	String enteredWord;
	EditText wordBox;
	File image;
	SubsamplingScaleImageView imageView;
	String returnedJson;
	Word word;
	Bitmap originalBitmap;
	Paint highlightPaint;

	final static String API_KEY = "882cd86596f740f6bfb54549d667dc50";
	final static String API_URL = "https://api.projectoxford.ai/vision/v1/ocr?language=unk&detectOrientation=true&subscription-key="+ API_KEY;


	@Override
	protected void onCreate(Bundle savedInstanceState) {



		//Set Activity to full-screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find);

		//Get imagePath from intent
		Bundle extras = getIntent().getExtras();
		image = new File(extras.getString("imagePath"));

		//Attach Views
		wordBox = (EditText) findViewById(R.id.textbox);
		imageView = (SubsamplingScaleImageView) findViewById(R.id.imageView);


		try {
			//Get bitmap from Storage
			originalBitmap= MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(image.toURI().toString()));
		} catch (IOException ioe) {	ioe.printStackTrace(); }

		//Set bitmap to ImageView
		imageView.setImage(ImageSource.bitmap(originalBitmap));

		if (haveNetworkConnection()) {
			//if network connection exists, send the image to Azure Server
			new postTask().execute(image);
		} else
			Toast.makeText(this, "No Internet Connection, try again!", Toast.LENGTH_LONG).show();

	}
	private class postTask extends AsyncTask<File, Integer, Integer> {
		ProgressDialog PD;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			PD = new ProgressDialog(FindActivity.this);
			PD.setTitle("Please Wait..");
			PD.setMessage("Sending image to server.");
			PD.setCancelable(false);
			PD.show();
		}

		@Override
		protected Integer doInBackground(File... files) {
			returnedJson = FindActivity.getJsonFromApi(files[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Integer integer) {
			PD.dismiss();
			Toast.makeText(FindActivity.this, "Ready!", Toast.LENGTH_SHORT).show();

			if (Word.hasWords(returnedJson)) {
				word = new Word(returnedJson);
				wordBox.addTextChangedListener(new TextWatcher() {
					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						enteredWord = wordBox.getText().toString().replaceAll("(?!\")\\p{Punct}", "").toLowerCase().trim();
						Set<Rect> rectSet = word.getBoundingBoxSet(enteredWord);

						if(rectSet.size()>0)
							drawBoundingBoxes(rectSet);

					}

					@Override
					public void afterTextChanged(Editable s) {}
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after){}
				});
			}

			else
				Toast.makeText(FindActivity.this, "No Text Found in Image", Toast.LENGTH_LONG).show();
		}
	}
	private void drawBoundingBoxes(Set<Rect> bbRectSet) {
		setUpPaint();
		try {
			if(originalBitmap.isRecycled()) {
				//check to see if originalBitmap is still in memory
					//if not then get bitmap again from File
				originalBitmap= MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(image.toURI().toString()));
			}

					//create a copy of bitmap
			Bitmap newBitmap=originalBitmap.copy(originalBitmap.getConfig(),true);

			//create a Canvas to draw on bitmap
			Canvas rectDrawCanvas = new Canvas(newBitmap);

			for (Rect rectangle : bbRectSet) {
				//loop through all the BoundingBox Rect we got from API
					//Draw rectangle to rectDrawCanvas
				rectDrawCanvas.drawRect(rectangle, highlightPaint);
			}

			//set new image
			imageView.setImage(ImageSource.bitmap(newBitmap));

			//tell system to redraw immediately
			imageView.invalidate();
		} catch (Exception e) {e.printStackTrace();}
	}
	private void setUpPaint(){
		highlightPaint = new Paint();
		highlightPaint.setStyle(Paint.Style.STROKE);
		highlightPaint.setColor(Color.RED);
		highlightPaint.setStrokeWidth(5);
	}
	private static String getJsonFromApi(File image) {
		String jsonVal = "{\"language\":\"unk\",\"orientation\":\"NotDetected\",\"regions\":[]}";

		HttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

		HttpPost httppost = new HttpPost(API_URL);


		FileEntity reqEntity = new FileEntity(image, "application/octet-stream");

		httppost.setEntity(reqEntity);
		reqEntity.setContentType("application/octet-stream");
		try {
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity resEntity = response.getEntity();


			if (resEntity != null)
				jsonVal = EntityUtils.toString(resEntity);

			if (resEntity != null)
				resEntity.consumeContent();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		httpclient.getConnectionManager().shutdown();

		return jsonVal;
	}


	private boolean haveNetworkConnection() {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}

		return haveConnectedWifi || haveConnectedMobile;
	}
}
