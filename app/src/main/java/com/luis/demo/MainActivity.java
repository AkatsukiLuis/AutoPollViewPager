package com.luis.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.luis.autopollviewpager.AutoPollViewPager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        List<Drawable> data = new ArrayList<>();
        data.add(getDrawable(R.drawable.meinv1));
        data.add(getDrawable(R.drawable.meinv2));
        data.add(getDrawable(R.drawable.meinv3));
        AutoPollViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setTimeWait(3000);// 控制速度，默认3s
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ImageView view = new ImageView(container.getContext());
                if (view.getParent() != null) {
                    final ViewGroup parent = ((ViewGroup) view.getParent());
                    parent.removeViewInLayout(view);
                }
                view.setImageDrawable(data.get(position));
                container.addView(view);
                return view;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeViewInLayout((View) object);
            }

            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                return arg0 == arg1;
            }

        });
    }
}