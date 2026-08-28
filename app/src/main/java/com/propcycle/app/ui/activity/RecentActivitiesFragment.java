package com.propcycle.app.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.propcycle.app.R;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.databinding.FragmentRecentActivitiesBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Collections;
import java.util.List;

/** Account-scoped local history; it never invents a completed recycling or exchange action. */
public final class RecentActivitiesFragment extends Fragment {

    private FragmentRecentActivitiesBinding binding;
    private RecentActivityAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRecentActivitiesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!ScreenNavigation.navigateAuthenticated(this, R.id.recentActivitiesFragment, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);
        adapter = new RecentActivityAdapter(this::open);
        binding.recentActivityList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentActivityList.setAdapter(adapter);
        RecentActivitiesViewModel viewModel =
                new ViewModelProvider(this).get(RecentActivitiesViewModel.class);
        viewModel.getActivities().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@Nullable List<ActivityRecord> value) {
        List<ActivityRecord> activities = value == null ? Collections.emptyList() : value;
        adapter.submit(activities);
        binding.recentActivityStatus.setVisibility(
                activities.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void open(@NonNull ActivityRecord record) {
        Bundle arguments = new Bundle();
        int destination;
        switch (record.getDestination()) {
            case ActivityLogRepository.DESTINATION_AI_RESULT -> {
                destination = R.id.aiResultFragment;
                arguments.putString("analysisJson", record.getPayload());
            }
            case ActivityLogRepository.DESTINATION_MARKETPLACE -> {
                destination = R.id.marketDetailFragment;
                arguments.putString("listingId", record.getPayload());
            }
            case ActivityLogRepository.DESTINATION_LENDING_ITEM -> {
                destination = R.id.lendingDetailFragment;
                arguments.putString("itemId", record.getPayload());
            }
            case ActivityLogRepository.DESTINATION_LENDING_REQUESTS ->
                    destination = R.id.notificationsFragment;
            case ActivityLogRepository.DESTINATION_RECYCLE ->
                    destination = R.id.recycleCenterFragment;
            default -> {
                return;
            }
        }
        ScreenNavigation.navigateAuthenticated(this, destination, arguments);
    }

    @Override
    public void onDestroyView() {
        binding.recentActivityList.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}
