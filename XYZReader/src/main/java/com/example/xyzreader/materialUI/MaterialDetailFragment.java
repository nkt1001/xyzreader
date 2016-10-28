package com.example.xyzreader.materialUI;


import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class MaterialDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "MaterialDetailFragment";

    private static final String ARG_ITEM_ID = "ARG_ITEM_ID";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private CollapsingToolbarLayout mCollapsingTb;
    private FloatingActionButton fab;
    private SquareImageView mTbImage;
    private NestedScrollView mScrollText;

    private MaterialDetailFragmentCallback mCallback;


    public MaterialDetailFragment() {}

    public static MaterialDetailFragment newInstance(long _id) {
        MaterialDetailFragment fragment = new MaterialDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, _id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader((int)mItemId, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_material_detail, container, false);

        mCollapsingTb = (CollapsingToolbarLayout) mRootView.findViewById(R.id.toolbar_layout);
        mTbImage = (SquareImageView) mRootView.findViewById(R.id.tb_image);
        fab = (FloatingActionButton) mRootView.findViewById(R.id.fab_share);
        mScrollText = (NestedScrollView) mRootView.findViewById(R.id.scroll_text);

        Toolbar tb = (Toolbar) mRootView.findViewById(R.id.tb_detail);
        mCallback.toolbarCreated(tb);

        bindViews();
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof MaterialDetailFragmentCallback) {
            mCallback = (MaterialDetailFragmentCallback) activity;
        } else {
            throw new ClassCastException();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.tv_article);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.tv_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.tv_text);

        if (mCursor != null) {

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText("Some sample text")
                            .getIntent(), getString(R.string.action_share)));
                }
            });

            mScrollText.setAlpha(0);
            mScrollText.setVisibility(View.VISIBLE);
            mScrollText.animate().alpha(1);

            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            mCollapsingTb.setTitle(title);
            mCollapsingTb.setExpandedTitleColor(ActivityCompat.getColor(getActivity(), android.R.color.transparent));

            titleView.setText(title);

            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            Picasso.with(getActivity()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .placeholder(R.drawable.empty_detail)
                    .error(R.drawable.empty_detail)
                    .into(mTbImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) mTbImage.getDrawable()).getBitmap();
                            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                public void onGenerated(Palette palette) {
                                    applyPalette(palette);
                                }
                            });
                        }

                        @Override
                        public void onError() {

                        }
                    });

        } else {
            mScrollText.setVisibility(View.GONE);
            titleView.setText("");
            bylineView.setText("");
            bodyView.setText("");
        }
    }

    private void applyPalette(Palette palette) {

        if (mCallback == null || getResources() == null) return;

        int primaryDark = getResources().getColor(R.color.primary_dark);
        int primary = getResources().getColor(R.color.primary);
        mCollapsingTb.setContentScrimColor(palette.getMutedColor(primary));
        mCollapsingTb.setStatusBarScrimColor(palette.getDarkMutedColor(primaryDark));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    interface MaterialDetailFragmentCallback {
        void toolbarCreated(Toolbar toolbar);
    }
}
