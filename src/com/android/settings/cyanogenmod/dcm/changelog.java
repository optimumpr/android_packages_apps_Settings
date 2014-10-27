package com.android.settings.cyanogenmod.dcm;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.settings.R;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class changelog extends Fragment {
    private static final String CHANGELOG_PATH = "/system/etc/CHANGELOG-DCM.txt";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        InputStreamReader inputReader = null;
        String text = null;

        try {
            StringBuilder data = new StringBuilder();
            char tmp[] = new char[2048];
            int numRead;

            inputReader = new FileReader(CHANGELOG_PATH);
            while ((numRead = inputReader.read(tmp)) >= 0) {
                data.append(tmp, 0, numRead);
            }
            text = data.toString();
        } catch (IOException e) {
            text = getString(R.string.changelog_dcm_error);
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
            } catch (IOException e) {
            }
        }

        final TextView textView = new TextView(getActivity());
        textView.setText(text);

        final ScrollView scrollView = new ScrollView(getActivity());
        scrollView.addView(textView);

        return scrollView;
    }
}
