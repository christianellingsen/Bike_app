package com.dtu.helmet_alert.friends;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Created by chris on 12-08-2016.
 */
public class SearchUsers extends Fragment {

    private String TAG = "SearchUsers";

    private View root;

    private SearchView searchView;
    private RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;

    private ArrayList<User> foundUsers=  new ArrayList<>();

    private DatabaseReference mDatabase;

    //Firebase ref = new Firebase(MyApplication.firebase_URL);
    //Firebase tournamentRef = ref.child(MyApplication.tournamentsString);
    //Query tournamnetNameQuery;
    private String queryUserName ="";

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup container, Bundle savedInstanceState){

        root = i.inflate(R.layout.search_users, container, false);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Search");

        searchView = (SearchView) root.findViewById(R.id.search_tournament_field);

        mRecyclerView = (RecyclerView) root.findViewById(R.id.search_tournaments_recyclerview);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new FoundUsersAdapter(foundUsers,SearchUsers.this);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setClickable(true);

        searchView.setIconified(false);
        searchView.setQueryHint("User name");

        mDatabase = FirebaseDatabase.getInstance().getReference();

        //***setOnQueryTextListener***
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                queryUserName = query;
                Log.d(TAG,"OnSearchClick. Query: "+ queryUserName);
                //tournamnetNameQuery = tournamentRef.orderByChild("name").startAt(queryUserName);
                makeQuery();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return root;
    }

    public void makeQuery(){

        foundUsers.clear();

        Query query = mDatabase.child("users").orderByChild("fullName");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    User t = child.getValue(User.class);
                    if (t.getFullName().toLowerCase().contains(queryUserName.toLowerCase()))
                        foundUsers.add(t);
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}