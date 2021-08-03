package com.dymux.dream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
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


public class DeepDreamFragment extends Fragment {
    private static final String TAG = "ImageGrab";
    private Uri mainUri;
    private String apiKey;

    private ImageItem mItem = new ImageItem();
    private static final int REQUEST_GET_SINGLE_FILE = 0;
    private static final int REQUEST_GET_STYLE_FILE = 1;
    private static final int READ_EXTERNAL_PERMISSION_CODE = 100;
    private static final int WRITE_EXTERNAL_PERMISSION_CODE = 101;
    static final int REQUEST_TAKE_PHOTO = 2;
    private Button mMainButton;
    private Button mUploadButton;
    private Button mMainCameraButton;
    private Button mSaveButton;
    private ImageView mMainView;
    private ImageView mOutputView;
    private String mMainURL;
    String currentPhotoPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_deepdream, container, false);
        mOutputView = (ImageView) v.findViewById(R.id.DDoutputView);
        mMainButton = (Button) v.findViewById(R.id.DDmainButton);
        mMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GET_SINGLE_FILE);
            }
        });

        mUploadButton = (Button) v.findViewById(R.id.DDuploadButton);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputStream mainfile = null;
                try {
                    mainfile = getContext().getContentResolver().openInputStream(mainUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                uploadFile(mainfile,"https://api.deepai.org/api/deepdream");
            }
        });

        mSaveButton = (Button) v.findViewById(R.id.DDsaveButton);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ask for permissions
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_PERMISSION_CODE);
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_PERMISSION_CODE);

                File file = saveToGallery(mOutputView);
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
                    mMainView = (ImageView) getActivity().findViewById(R.id.DDmainView);
                    mMainView.setImageURI(selectedImageUri);
                    mainUri = selectedImageUri;
                }
            }
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_TAKE_PHOTO) {
                    mMainView = (ImageView) getActivity().findViewById(R.id.DDmainView);
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
        mOutputView = (ImageView) getActivity().findViewById(R.id.DDoutputView);
        Picasso.get().load(item.getUrl()).placeholder( R.drawable.progress_animation ).into(mOutputView);
        mMainURL = item.getUrl();

    }

    private void uploadFile(InputStream is, String url) {
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
        params.put("image", is);
        Log.i("DreamMainFragment", "File Maybe Found");

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
        File image = File.createTempFile(
                imageFileName,".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private File saveToGallery(ImageView imageView){
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

        return outFile;

    }

    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(getActivity(), new String[] { permission }, requestCode);
        }
    }
}



