package com.example.xyzreader.materialUI;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 * Second variant of {@link com.example.xyzreader.ui.ArticleDetailActivity} material implementation.
 * Contains static collapsing toolbar and fab and viewpager to swipe between articles.
 * See also other implementation: {@link MaterialDetailActivity}.
 */
public class MaterialArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ArticleDetailActivity";
    private Cursor mCursor;
    private long mStartId;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private SquareImageView mToolbarLogo;
    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageScrollStateChanged(int state) {}

        @Override
        public void onPageSelected(int position) {
            Log.d(TAG, "onPageSelected() called with: position = [" + position + "]");
            if (mCursor != null) {
                mCursor.moveToPosition(position);

                mCollapsingTb.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
                mCollapsingTb.setExpandedTitleColor(ActivityCompat.getColor(MaterialArticleDetailActivity.this,
                        android.R.color.transparent));

                Picasso.with(MaterialArticleDetailActivity.this).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                        .placeholder(R.drawable.empty_detail)
                        .error(R.drawable.empty_detail)
                        .into(mToolbarLogo, new Callback() {
                            @Override
                            public void onSuccess() {
                                if (mToolbarLogo == null) return;

                                Bitmap bitmap = ((BitmapDrawable) mToolbarLogo.getDrawable()).getBitmap();
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
            }
        }
    };

    private CollapsingToolbarLayout mCollapsingTb;

    private void applyPalette(Palette palette) {
        if (mCollapsingTb == null) return;

        int primaryDark = ActivityCompat.getColor(this, R.color.primary_dark);
        int primary = ActivityCompat.getColor(this, R.color.primary);
        mCollapsingTb.setContentScrimColor(palette.getMutedColor(primary));
        mCollapsingTb.setStatusBarScrimColor(palette.getDarkMutedColor(primaryDark));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_article_detail);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.tb_detail);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCollapsingTb = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mToolbarLogo = (SquareImageView) findViewById(R.id.tb_image);

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.addOnPageChangeListener(pageChangeListener);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_share);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(MaterialArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished: " + mStartId);
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return MaterialArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
