package com.dtu.helmet_alert.friends;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dtu.helmet_alert.MyApplication;
import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

/**
 * Created by chris on 13-08-2016.
 */
public class FriendsListAdapter extends RecyclerView.Adapter<FriendsListAdapter.ViewHolder> {

    private List<Friend> tList;
    private FriendsList fragment;

    private DatabaseReference mDatabase;


    public FriendsListAdapter(List<Friend> tournaments, FriendsList frag) {
        this.tList = tournaments;
        this.fragment=frag;
        mDatabase = FirebaseDatabase.getInstance().getReference();

    }


    @Override
    public FriendsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.found_users_listeelement, null);
        ViewHolder viewHolder = new ViewHolder(itemLayoutView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {

        final int pos = position;
        final Friend t = tList.get(pos);
        viewHolder.tName.setText(t.getFullName());

        viewHolder.removeFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.getUser().getStoredFriends().remove(t);
                // Update list of friends in database
                mDatabase.child("users").child(MyApplication.getUser().getU_key()).setValue(MyApplication.getUser());

                fragment.updateAdapter();

            }
        });


    }

    // Return the size arraylist
    @Override
    public int getItemCount() {
        return tList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View mView;
        TextView tName;
        Button removeFriend;

        public ViewHolder(View v) {
            super(v);
            tName = (TextView) v.findViewById(R.id.found_tournament_list_name);
            removeFriend = (Button) v.findViewById(R.id.add_friend);
            removeFriend.setText("Remove");
            mView = v;
        }
    }

    // method to access in activity after updating selection
    public List<Friend> getTournamentList() {
        return tList;
    }

}