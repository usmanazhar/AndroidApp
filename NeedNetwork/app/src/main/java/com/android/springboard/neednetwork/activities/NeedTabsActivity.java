package com.android.springboard.neednetwork.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.springboard.neednetwork.R;
import com.android.springboard.neednetwork.constants.ActivityConstants;
import com.android.springboard.neednetwork.constants.Constants;
import com.android.springboard.neednetwork.fragments.CurrentNeedsListFragment;
import com.android.springboard.neednetwork.fragments.MyNeedsListFragment;
import com.android.springboard.neednetwork.listeners.OnActivityInteractionListener;
import com.android.springboard.neednetwork.managers.NeedManager;
import com.android.springboard.neednetwork.managers.UserManager;
import com.android.springboard.neednetwork.models.Need;
import com.android.springboard.neednetwork.models.User;
import com.android.springboard.neednetwork.utils.ActivityUtil;
import com.android.springboard.neednetwork.utils.SharedPrefsUtils;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.android.springboard.neednetwork.constants.Constants.HEADER_AUTHORIZATION;
import static com.android.springboard.neednetwork.constants.Constants.TAB_CURRENT_NEEDS;
import static com.android.springboard.neednetwork.constants.Constants.TAB_MY_NEEDS;

public class NeedTabsActivity extends BaseActivity implements TabLayout.OnTabSelectedListener, View.OnClickListener, OnActivityInteractionListener {

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private FloatingActionButton mAddNewNeedButton;
    private String[] mTitles;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private UserManager mUserManager;
    private NeedManager mNeedManager;
    private MyNeedsListFragment mMyNeedsListFragment;
    private CurrentNeedsListFragment mCurrentNeedsListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_need_tabs);

        mUserManager = new UserManager(this);
        mNeedManager = new NeedManager(this);
        initViews();

        String mobileNumber = SharedPrefsUtils.getStringPreference(this, ActivityConstants.PREF_MOBILE_NUMBER);
        loadNeeds(mobileNumber);
    }

    @Override
    protected void
    onStart() {
        super.onStart();

        mMyNeedsListFragment.addNeed();
    }

    private void initViews() {
        mAddNewNeedButton = (FloatingActionButton) findViewById(R.id.add_need_btn);
        mAddNewNeedButton.setOnClickListener(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(mViewPager);

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.addOnTabSelectedListener(this);
        mTabLayout.setupWithViewPager(mViewPager);

        mTitles = getResources().getStringArray(R.array.left_menu_titles);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,

                R.layout.drawer_list_item, mTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void loadNeeds(String mobileNumber) {
        User user = new User();
        user.setMobileNumber(mobileNumber);
        Log.i("shoeb", "test before making rest call" );
        mUserManager.login(user, mLoginResponseListener, mLoginErrorListener);
    }

    private Response.Listener mLoginResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.i("shoeb", "" + response);
            try {
                JSONObject headers = response.getJSONObject(Constants.RESPONSE_HEADERS);
                String token = headers.getString(HEADER_AUTHORIZATION);
                SharedPrefsUtils.setStringPreference(NeedTabsActivity.this, ActivityConstants.PREF_TOKEN, Base64.encodeToString(token.getBytes(),
                        Base64.DEFAULT));
                JSONArray responseData = response.getJSONArray(Constants.RESPONSE_DATA);
                handleResponseData(responseData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void handleResponseData(JSONArray responseData) {
        Gson gson = new Gson();
        TypeToken<List<Need>> token = new TypeToken<List<Need>>() {};
        List<Need> needList = gson.fromJson(responseData.toString(), token.getType());
        Log.i("shoeb", responseData.toString());
        mMyNeedsListFragment.loadAdapter(needList);
    }

    private Response.ErrorListener mLoginErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.i("shoeb", "" + error);
            Toast.makeText(NeedTabsActivity.this, R.string.text_network_error, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
       /* boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);*/
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRecyclerViewScroll(int dx, int dy) {
        if (dy > 0 && mAddNewNeedButton.getVisibility() == View.VISIBLE) {
            mAddNewNeedButton.hide();
        } else if (dy < 0 && mAddNewNeedButton.getVisibility() != View.VISIBLE) {
            mAddNewNeedButton.show();
        }
    }

/*    @Override
    public void onListFragmentInteraction(Need item) {
        Intent intent = new Intent();
        intent.setClass(this, NeedActivity.class);
        intent.putExtra(ActivityConstants.INTENT_EXTRA_NEED, item);
        startActivity(intent);
    }*/

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
/*        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();*/

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        mMyNeedsListFragment = new MyNeedsListFragment();
        mCurrentNeedsListFragment = new CurrentNeedsListFragment();
        adapter.addFragment(mMyNeedsListFragment, TAB_MY_NEEDS);
        adapter.addFragment(mCurrentNeedsListFragment, TAB_CURRENT_NEEDS);
        viewPager.setAdapter(adapter);
    }

    private void RequestOthersNeeds() {
        if (!mCurrentNeedsListFragment.isAdapterInitialized()) {
            mNeedManager.getCurrentNeeds(mOthersNeedsResponseListener, mOthersNeedsErrorListener);
        }
    }

    private Response.Listener mOthersNeedsResponseListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.i("shoeb", "" + response);
            handleOthersNeedsResponse(response);

        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleOthersNeedsResponse(String responseData) {
        Gson gson = new Gson();
        TypeToken<List<Need>> token = new TypeToken<List<Need>>() {};
        List<Need> needList = gson.fromJson(responseData, token.getType());
        Log.i("shoeb", responseData);
        mCurrentNeedsListFragment.loadAdapter(needList);
    }

    private Response.ErrorListener mOthersNeedsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.i("shoeb", "" + error);
            Toast.makeText(NeedTabsActivity.this, R.string.text_network_error, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        String tabTitle = tab.getText().toString();

        if(tabTitle.equals(TAB_MY_NEEDS)) {
            mAddNewNeedButton.show();
        } else {
            mAddNewNeedButton.hide();
            RequestOthersNeeds();
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if(id == R.id.add_need_btn) {
            ActivityUtil.startActivity(this, NeedActivity.class);
        }
    }


    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
