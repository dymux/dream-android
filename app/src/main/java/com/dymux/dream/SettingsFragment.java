package com.dymux.dream;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;


public class SettingsFragment extends Fragment {

    private String key;
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

    private EditText mEditText;

    private View v;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Ask for permissions
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_PERMISSION_CODE);

        v = inflater.inflate(R.layout.fragment_settings, container, false);
        mEditText = (EditText) v.findViewById(R.id.api_key);
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

        mEditText.setText(line);


        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                // Clear the contents of the file
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter("/data/data/com.example.dream/files/store.txt");
                    pw.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // Write to the file
                try (PrintWriter p = new PrintWriter(new FileOutputStream("/data/data/com.example.dream/files/store.txt"))) {
                    p.println(editable.toString());
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        });

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();
        mEditText = (EditText) v.findViewById(R.id.api_key);
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

        mEditText.setText(line);
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



