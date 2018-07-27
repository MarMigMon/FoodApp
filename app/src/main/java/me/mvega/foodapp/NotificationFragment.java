package me.mvega.foodapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import me.mvega.foodapp.model.Notification;


public class NotificationFragment extends Fragment {

    private SwipeRefreshLayout swipeContainerNotifications;
    private RecyclerView rvNotifications;
    ArrayList<Notification> notifications;
    private NotificationAdapter notificationAdapter;
    NotificationFragmentCommunication notificationListenerFragment;

    // implement listener interface
    public interface NotificationFragmentCommunication {
        void respond(ParseObject notificationRecipe);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        return inflater.inflate(R.layout.fragment_notification, parent, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NotificationFragmentCommunication) {
            notificationListenerFragment = (NotificationFragmentCommunication) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement NotificationFragmentCommunication");
        }
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        rvNotifications = view.findViewById(R.id.rvNotifications);
        swipeContainerNotifications = view.findViewById(R.id.swipeContainerNotifications);


        notifications = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notifications);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvNotifications.setLayoutManager(linearLayoutManager);
        rvNotifications.setAdapter(notificationAdapter);

        notificationAdapter.setNotificationListener(new NotificationAdapter.NotificationAdapterCommunication() {
            @Override
            public void respond(ParseObject notificationRecipe) {
                notificationListenerFragment.respond(notificationRecipe);
            }
        });

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        rvNotifications.addItemDecoration(itemDecoration);

        loadYourNotifications();
        setSwipeContainer();
    }


    public void setSwipeContainer() {
        // Setup refresh listener which triggers new data loading
        swipeContainerNotifications.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                loadYourNotifications();
            }
        });
        // Configure the refreshing colors
        swipeContainerNotifications.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    public static NotificationFragment newInstance() {
        NotificationFragment fragmentNotification = new NotificationFragment();
        fragmentNotification.setArguments(new Bundle());
        return fragmentNotification;
    }

    private void loadYourNotifications() {
        final Notification.Query notificationQuery = new Notification.Query();
        notificationQuery.recipeUser(ParseUser.getCurrentUser());
        notificationQuery.getTop().newestFirst();
        notificationQuery.include("activeUser.username")
                .include("activeUser.image")
                .include("recipe.user")
                .include("recipe.image")
                .include("recipe.views");
        notificationQuery.findInBackground(new FindCallback<Notification>() {
            @Override
            public void done(List<Notification> newNotification, ParseException e) {
                if (e == null) {
                    notificationAdapter.clear();
                    notificationAdapter.addAll(newNotification);
                    swipeContainerNotifications.setRefreshing(false);
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

}
