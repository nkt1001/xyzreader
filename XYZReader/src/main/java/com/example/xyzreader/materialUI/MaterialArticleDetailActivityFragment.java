package com.example.xyzreader.materialUI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class MaterialArticleDetailActivityFragment extends android.support.v4.app.Fragment {

    public MaterialArticleDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_material_article_detail, container, false);
    }
}