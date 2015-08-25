package com.example.asad.ctrlf;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.io.File;


public class MainActivity extends Activity{
	public static  final int GET_IMAGE_FROM_CAMERA = 1;
	public static final int  GET_IMAGE_FROM_GALLERY = 2;

	@Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button selectCameraBtn  = (Button) findViewById(R.id.camerabtn);
        Button selectGalleryBtn = (Button) findViewById(R.id.gallerybtn);

        selectCameraBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				File file = new File(Environment.getExternalStorageDirectory()+File.separator + "image.jpg");
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
				startActivityForResult(intent, GET_IMAGE_FROM_CAMERA);
			}
		});
		selectGalleryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, GET_IMAGE_FROM_GALLERY);
			}
		});

}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String imagePath = "";

		if (requestCode == GET_IMAGE_FROM_CAMERA && resultCode == RESULT_OK ) {
			//Image is coming from camera
			File file = new File(Environment.getExternalStorageDirectory()+File.separator + "image.jpg");
			imagePath = file.getAbsolutePath();
		}

		else if (requestCode == GET_IMAGE_FROM_GALLERY && resultCode == RESULT_OK ) {
			//Image is coming from Gallery
			Uri tempUri = data.getData();
			File finalFile = new File(getRealPathFromURI(tempUri));
			imagePath = finalFile.getAbsolutePath();
		}

		if(resultCode == RESULT_OK){
			//Image path set, now attach imagePath to intent
			//And fire off FindActivity
			Intent i = new Intent(this,FindActivity.class);
			i.putExtra("imagePath",imagePath);
			startActivity(i);
		}
    }

    private String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
		String path =cursor.getString(idx);
		cursor.close();
        return path;
    }

}
