package com.dymux.dream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.*;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;


import cz.msebera.android.httpclient.Header;

import static android.app.Activity.RESULT_OK;


public class DeepStyleFragment extends Fragment {
    private static final String TAG = "ImageGrab";
    private String apiKey;
    private Uri mainUri;
    private Uri styleUri;

    private ImageItem mItem = new ImageItem();
    private static final int REQUEST_GET_SINGLE_FILE = 0;
    private static final int REQUEST_GET_STYLE_FILE = 1;
    private static final int READ_EXTERNAL_PERMISSION_CODE = 100;
    private static final int WRITE_EXTERNAL_PERMISSION_CODE = 101;
    static final int REQUEST_TAKE_PHOTO = 2;
    private Button mMainButton;
    private Button mStyleButton;
    private Button mUploadButton;
    private Button mMainCameraButton;
    private Button mSaveButton;
    private ImageView mMainView;
    private ImageView mStyleView;
    private ImageView mOutputView;

    private String mMainURL;
    String currentPhotoPath;


    public static DeepStyleFragment newInstance() {
        return new DeepStyleFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_deepstyle, container, false);
        mMainButton = (Button) v.findViewById(R.id.mainButton);
        mStyleButton = (Button) v.findViewById(R.id.styleButton);
        mUploadButton = (Button) v.findViewById(R.id.uploadButton);
        mSaveButton = (Button) v.findViewById(R.id.saveButton);
        mMainView = (ImageView) v.findViewById(R.id.mainView);
        mStyleView = (ImageView) v.findViewById(R.id.styleView);
        mOutputView = (ImageView) v.findViewById(R.id.outputView);

        mMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GET_SINGLE_FILE);
            }
        });

        mStyleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GET_STYLE_FILE);
            }
        });

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputStream mainfile = null;
                InputStream stylefile = null;
                try {
                    mainfile = getContext().getContentResolver().openInputStream(mainUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    stylefile = getContext().getContentResolver().openInputStream(styleUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                uploadFiles(mainfile,stylefile,"https://api.deepai.org/api/neural-style");
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ask for permissions
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_PERMISSION_CODE);
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_PERMISSION_CODE);

                saveToGallery(mOutputView);
                Snackbar.make(v, "Image saved!", Snackbar.LENGTH_LONG).show();
            }
        });
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_GET_SINGLE_FILE) {
                    Uri selectedImageUri = data.getData();
                    // Get the path from the Uri
                    final String path = getPathFromURI(selectedImageUri);
                    if (path != null) {
                        File f = new File(path);
                        selectedImageUri = Uri.fromFile(f);
                    }
                    // Set the image in ImageView
                    mMainView.setImageURI(selectedImageUri);
                    mainUri = selectedImageUri;
                }
            }
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_GET_STYLE_FILE) {
                    Uri selectedImageUri = data.getData();
                    // Get the path from the Uri
                    final String path = getPathFromURI(selectedImageUri);
                    if (path != null) {
                        File f = new File(path);
                        selectedImageUri = Uri.fromFile(f);
                    }
                    // Set the image in ImageView
                    mStyleView.setImageURI(selectedImageUri);
                    styleUri = selectedImageUri;
                }
            }
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_TAKE_PHOTO) {
                    mMainView.setImageURI(mainUri);
                }
            }
        } catch (Exception e) {
            Log.e("FileSelectorActivity", "File select error", e);
        }


    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private void parseItem(ImageItem item, JSONObject jsonBody) throws IOException, JSONException {
        try{
        item.setUrl(jsonBody.getString("output_url"));
        }catch (JSONException e){
            Log.i("DreamMainFragment", jsonBody.getString("err"));
        }
        mOutputView = (ImageView) getActivity().findViewById(R.id.outputView);
        Picasso.get().load(item.getUrl()).placeholder( R.drawable.progress_animation ).into(mOutputView);
        mMainURL = item.getUrl();
    }

    private void uploadFiles(InputStream mainis,InputStream styleis, String url) {

        // Read the key from the store file
        String line = null;

        try {
            FileInputStream fileInputStream = new FileInputStream (new File("/data/data/com.example.dream/files/store.txt"));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            while ( (line = bufferedReader.readLine()) != null )
            {
                stringBuilder.append(line);
            }
            fileInputStream.close();
            line = stringBuilder.toString();

            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        apiKey = line;
        if(apiKey == ""){
            Toast.makeText(getContext(),"Please set the API Key in the settings tab",Toast.LENGTH_LONG).show();
            return;
        }
        final ImageItem item = new ImageItem();
        RequestParams params = new RequestParams();
        params.put("content", mainis);
        params.put("style", styleis);

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Api-Key",apiKey);
        client.setConnectTimeout(1200000);
        client.post(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    parseItem(item, response);
                } catch (IOException ioe) {
                    Log.e(TAG, "Failed to fetch items", ioe);
                } catch (JSONException je) {
                    Log.e(TAG, "Failed to parse JSON", je);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                Log.i("DreamMainFragment", "Failed");
            }
        });

    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getContext(),
                        "com.example.dream.fileprovider",
                        photoFile);
                mainUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void saveToGallery(ImageView imageView){
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        FileOutputStream outputStream = null;
        File file = Environment.getExternalStorageDirectory();
        File dir = new File(file.getAbsolutePath() + "/Dreams");
        dir.mkdir();

        String filename = String.format("%d.png",System.currentTimeMillis());
        File outFile = new File(dir,filename);
        try {
            outputStream = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(getActivity(), new String[] { permission }, requestCode);
        }
    }
}



