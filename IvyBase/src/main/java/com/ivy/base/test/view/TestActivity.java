package com.ivy.base.test.view;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ivy.base.ActivityUtil;
import com.ivy.base.R;
import com.ivy.base.test.view.fragments.CommonFragment;
import com.ivy.base.test.view.fragments.EventFragment;
import com.ivy.base.test.view.fragments.FuncFragment;
import com.ivy.base.util.Func;

import java.util.Arrays;
import java.util.List;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        List<String> titles = Arrays.asList("流程信息", "关键事件检测", "功能测试");
        List<Fragment> fragments = Arrays.asList(
                CommonFragment.newInstance("流程信息"),
                EventFragment.newInstance("关键事件检测"),
                FuncFragment.newInstance("功能测试")
        );
        PagerAdapter adapter = new PagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(titles.get(position))
        ).attach();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityUtil.getInstance().setTestActivity(TestActivity.this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ActivityUtil.getInstance().setTestActivity(null);
    }

}
