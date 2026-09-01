package com.propcycle.app.ui.chat;

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
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.databinding.FragmentMessagesBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Real-time list of marketplace conversations for the authenticated user. */
public final class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private MessagesViewModel viewModel;
    private ChatThreadAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        adapter = new ChatThreadAdapter(new ChatThreadAdapter.Listener() {
            @Override
            public void onThreadClick(@NonNull ChatThread thread) {
                openThread(thread);
            }

            @Override
            public void onProfileClick(@NonNull String userId) {
                openProfile(userId);
            }
        });
        binding.conversationList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.conversationList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(MessagesViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getPublicProfiles().observe(
                getViewLifecycleOwner(), adapter::submitProfiles);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (FirebaseEnvironment.isConfigured(requireContext())
                && !ScreenNavigation.navigateAuthenticated(
                        this, R.id.messagesFragment, null)) {
            return;
        }
        viewModel.start();
    }

    @Override
    public void onStop() {
        viewModel.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding.conversationList.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }

    private void openThread(@NonNull ChatThread thread) {
        Bundle arguments = new Bundle();
        arguments.putString("threadId", thread.getThreadId());
        ScreenNavigation.navigateAuthenticated(
                this, R.id.conversationFragment, arguments);
    }

    private void openProfile(@NonNull String userId) {
        Bundle arguments = new Bundle();
        arguments.putString("userId", userId);
        ScreenNavigation.navigateAuthenticated(this, R.id.profileFragment, arguments);
    }

    private void render(@NonNull MessagesUiState state) {
        if (binding == null) {
            return;
        }
        adapter.submitList(state.getThreads(), viewModel.currentUserId());
        boolean hasThreads = !state.getThreads().isEmpty();
        binding.loadingIndicator.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        boolean showEmptyState = !state.isLoading()
                && !hasThreads
                && state.getErrorMessage() == null
                && !state.isConfigurationRequired();
        binding.emptyStateContainer.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
        binding.emptyText.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
        binding.conversationList.setVisibility(hasThreads ? View.VISIBLE : View.GONE);

        String status = state.getErrorMessage();
        if (status == null && state.isFromCache()) {
            status = hasThreads
                    ? "Offline - showing cached conversations."
                    : "Offline - no cached conversations are available.";
        }
        binding.statusText.setText(status == null ? "" : status);
        binding.statusText.setVisibility(status == null ? View.GONE : View.VISIBLE);
    }
}
