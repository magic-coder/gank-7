package com.jin.gank.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jin.gank.PictureActivity;
import com.jin.gank.R;
import com.jin.gank.animators.OvershootInLeftAnimator;
import com.jin.gank.data.Constant;
import com.jin.gank.data.GankCategory;
import com.jin.gank.network.RetrofitHelp;
import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItem;
import com.vlonjatg.progressactivity.ProgressActivity;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2015/8/25.
 */
public class MyFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, OnMoreListener {
    private static final String TAG = "MyFragment";
    @Bind(R.id.progressActivity)
    ProgressActivity mProgressActivity;
    @Bind(R.id.superRecyclerView_category)
    SuperRecyclerView mSuperRecyclerViewCategory;


    private List<GankCategory.ResultsEntity> mCategorys;
    private String[] mCategoryArray;
    private LayoutInflater mInflater;
    private MyAdapter mAdapter;
    private boolean isLoadMore = true;
    private int position;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
        mCategoryArray = getResources().getStringArray(R.array.category_list);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        position = FragmentPagerItem.getPosition(getArguments());
        mSuperRecyclerViewCategory.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSuperRecyclerViewCategory.getRecyclerView().setItemAnimator(new OvershootInLeftAnimator());
        mSuperRecyclerViewCategory.setRefreshListener(this);
        mSuperRecyclerViewCategory.setupMoreListener(this, 10);
        mSuperRecyclerViewCategory.setRefreshingColorResources(android.R.color.holo_orange_light, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_red_light);
        if (mCategorys == null || mCategorys.size() == 0) {
            loadCategoryData(mCategoryArray[position], Constant.API_COUNT, 1, true);
        } else {
            mAdapter.notifyDataSetChanged();
        }


    }


    private void loadCategoryData(String category, int count, int page, boolean firstLoad) {
        if (firstLoad) {
            mProgressActivity.showLoading();
            if (mCategorys != null)
                mCategorys.clear();
        }

        RetrofitHelp.getApi().listGankCategory(category, count, page)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(Categorys -> {
                    //判断是否已经加载到最后一页
                    if (Categorys.getResults().size() < 10)
                        isLoadMore = false;
                    return Observable.just(Categorys.getResults());
                })
                .subscribe(Categorys -> {
                            mProgressActivity.showContent();
                            if (mAdapter == null) {
                                mCategorys = Categorys;
                                mAdapter = new MyAdapter(Categorys);
                                mSuperRecyclerViewCategory.setAdapter(mAdapter);
                            } else {
                                if (!firstLoad)
                                    mCategorys.addAll(mCategorys.size(), Categorys);

                                mAdapter.setCategorys(mCategorys);
                                mAdapter.notifyDataSetChanged();
                            }

                            mSuperRecyclerViewCategory.hideMoreProgress();
                            mSuperRecyclerViewCategory.getSwipeToRefresh().setRefreshing(false);
                        },
                        err -> mProgressActivity.showError(null, "错误", err.toString(), "重试", v -> loadCategoryData(category, Constant.API_COUNT, 1, true)));
    }

    @Override
    public void onMoreAsked(int numberOfItems, int numberBeforeMore, int currentItemPos) {
        if (isLoadMore) {
            loadCategoryData(mCategoryArray[position], Constant.API_COUNT, numberOfItems / Constant.API_COUNT + 1, false);
        } else {
            mSuperRecyclerViewCategory.hideMoreProgress();
        }
    }

    @Override
    public void onRefresh() {
        isLoadMore = true;
        loadCategoryData(mCategoryArray[position], Constant.API_COUNT, 1, true);
    }


    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> implements View.OnClickListener {

        public void setCategorys(List<GankCategory.ResultsEntity> Categorys) {
            this.Categorys = Categorys;
        }

        private List<GankCategory.ResultsEntity> Categorys;

        public MyAdapter(List<GankCategory.ResultsEntity> Categorys) {
            super();
            this.Categorys = Categorys;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View contentView = mInflater.inflate(R.layout.item_category, parent, false);
            MyViewHolder holder = new MyViewHolder(contentView);
            return holder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            GankCategory.ResultsEntity Category = Categorys.get(position);
            holder.mAuthor.setText("作者： " + Category.getWho());
            holder.mPublishTime.setText("发布日期： " + Category.getPublishedAt());
            holder.mDesc.setText("功能描述： " + Category.getDesc());
            holder.mUrl.setText("链接： " + Category.getUrl());
        }

        @Override
        public int getItemCount() {
            return Categorys.size();
        }

        @Override
        public void onClick(View v) {

            startPictureActivity((GankCategory.ResultsEntity) v.getTag(R.id.image_tag), v);
        }


        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView mAuthor, mPublishTime, mDesc, mUrl;

            public MyViewHolder(View convertView) {
                super(convertView);
                mAuthor = (TextView) convertView.findViewById(R.id.category_author);
                mPublishTime = (TextView) convertView.findViewById(R.id.category_publish_time);
                mDesc = (TextView) convertView.findViewById(R.id.category_desc);
                mUrl = (TextView) convertView.findViewById(R.id.category_url);
            }
        }

        private void startPictureActivity(GankCategory.ResultsEntity girl, View sharedView) {
            Intent i = new Intent(getActivity(), PictureActivity.class);
            i.putExtra(PictureActivity.EXTRA_IMAGE_URL, girl.getUrl());
            i.putExtra(PictureActivity.EXTRA_IMAGE_TITLE, girl.getDesc());

            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), sharedView,
                    PictureActivity.TRANSIT_PIC);
            ActivityCompat.startActivity(getActivity(), i, optionsCompat.toBundle());
        }

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container = (ViewGroup) inflater.inflate(R.layout.fragment_category, null, false);
        ButterKnife.bind(this, container);
        return container;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
