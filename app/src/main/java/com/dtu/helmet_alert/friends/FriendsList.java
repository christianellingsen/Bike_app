package com.dtu.helmet_alert.friends;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dtu.helmet_alert.CalibrateHelmet;
import com.dtu.helmet_alert.MyApplication;
import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.User;

import java.util.ArrayList;

/**
 * Created by chris on 12-08-2016.
 */
public class FriendsList extends Fragment {

    private String TAG = "FriendsList";
    private View root;
    private Button find_friends_b;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private ArrayList<Friend> userFriends=  new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup container, Bundle savedInstanceState) {

        root = i.inflate(R.layout.friend_list, container, false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Search");

        mRecyclerView = (RecyclerView) root.findViewById(R.id.friends_list_addedFriends);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        userFriends = MyApplication.getUser().getStoredFriends();

        mAdapter = new FriendsListAdapter(userFriends,FriendsList.this);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setClickable(true);

        find_friends_b = (Button) root.findViewById(R.id.find_friends);

        find_friends_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.main_frame, new SearchUsers())
                        .commit();
            }
        });


        return root;
    }

    public void updateAdapter(){
        mAdapter.notifyDataSetChanged();
    }

}
