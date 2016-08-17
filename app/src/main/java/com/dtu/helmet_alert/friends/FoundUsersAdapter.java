package com.dtu.helmet_alert.friends;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dtu.helmet_alert.MyApplication;
import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

/**
 * Created by chris on 12-08-2016.
 */
public class FoundUsersAdapter extends RecyclerView.Adapter<FoundUsersAdapter.ViewHolder> {

    private List<User> tList;
    private SearchUsers fragment;

    private DatabaseReference mDatabase;


    public FoundUsersAdapter(List<User> tournaments, SearchUsers frag) {
        this.tList = tournaments;
        this.fragment=frag;
        mDatabase = FirebaseDatabase.getInstance().getReference();

    }


    @Override
    public FoundUsersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.found_users_listeelement, null);
        ViewHolder viewHolder = new ViewHolder(itemLayoutView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {

        final int pos = position;
        final User user = tList.get(pos);
        viewHolder.tName.setText(user.getFullName());

        final Friend f = new Friend();
        f.setEmail(user.getEmail());
        f.setFirstName(user.getFirstName());
        f.setLastName(user.getLastName());
        f.setU_key(user.getU_key());


        viewHolder.addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Add friends","clicked on "+f.getFirstName());
                MyApplication.getUser().getStoredFriends().add(f);
                // Update list of friends in database
                mDatabase.child("users").child(MyApplication.getUser().getU_key()).setValue(MyApplication.getUser());

                viewHolder.addFriend.setText("Already added");
                viewHolder.addFriend.setClickable(false);
                viewHolder.addFriend.setEnabled(false);

            }
        });

        for (Friend friend : MyApplication.getUser().getStoredFriends()) {
            if (friend.getU_key().equals(f.getU_key())) {
                viewHolder.addFriend.setText("Already added");
                viewHolder.addFriend.setClickable(false);
                viewHolder.addFriend.setEnabled(false);
            }
        }




/**
        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.d("SearchTournamnetAdapter","Clicked on "+pos + " and name: "+tList.get(pos).getName());

                if (tList.get(pos).getIsStarted()){
                    MyApplication.spectateT_ID = tList.get(pos).getT_ID();
                    MyApplication.spectateT_name = tList.get(pos).getName();

                    fragment.getFragmentManager().beginTransaction()
                            .replace(R.id.main_frame, new SpectateTournament())
                            .addToBackStack(null)
                            .commit();
                }
                else {
                    Toast.makeText(fragment.getActivity(),"Tournament not started yet",Toast.LENGTH_SHORT).show();
                }



            }
        });
**/

    }

    // Return the size arraylist
    @Override
    public int getItemCount() {
        return tList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View mView;
        TextView tName;
        Button addFriend;

        public ViewHolder(View v) {
            super(v);
            tName = (TextView) v.findViewById(R.id.found_tournament_list_name);
            addFriend = (Button) v.findViewById(R.id.add_friend);
            mView = v;
        }
    }

    // method to access in activity after updating selection
    public List<User> getTournamentList() {
        return tList;
    }

}