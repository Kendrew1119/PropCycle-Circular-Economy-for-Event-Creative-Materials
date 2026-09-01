package com.propcycle.app.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.chat.ChatParticipantPolicy;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;
import com.propcycle.app.data.profile.PublicProfile;
import com.propcycle.app.databinding.FragmentConversationBinding;
import com.propcycle.app.ui.common.ProfileAvatarRenderer;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Participant-only, bounded real-time text conversation. */
public final class ConversationFragment extends Fragment {

    private FragmentConversationBinding binding;
    private ConversationViewModel viewModel;
    private ChatMessageAdapter adapter;
    private String threadId = "";
    private String otherUserId = "";
    private PublicProfile otherProfile;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentConversationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        Bundle arguments = getArguments();
        threadId = arguments == null ? "" : arguments.getString("threadId", "");

        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.messageList.setLayoutManager(layoutManager);
        binding.messageList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getOtherProfile().observe(getViewLifecycleOwner(), profile -> {
            otherProfile = profile;
            renderConversationAvatar();
        });
        viewModel.getSendSucceeded().observe(getViewLifecycleOwner(), event -> {
            Boolean sent = event == null ? null : event.consume();
            if (Boolean.TRUE.equals(sent) && binding != null) {
                binding.messageComposer.setText("");
            }
        });

        binding.primaryAction.setOnClickListener(
                ignored -> viewModel.sendMessage(binding.messageComposer.getText().toString()));
        binding.threadAvatar.setOnClickListener(ignored -> openOtherUserProfile());
        binding.messageComposer.setOnEditorActionListener((field, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEND) {
                return false;
            }
            viewModel.sendMessage(field.getText().toString());
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (FirebaseEnvironment.isConfigured(requireContext())
                && !ScreenNavigation.navigateAuthenticated(
                        this, R.id.conversationFragment, null)) {
            return;
        }
        viewModel.start(threadId);
    }

    @Override
    public void onStop() {
        viewModel.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding.messageList.setAdapter(null);
        otherProfile = null;
        binding = null;
        super.onDestroyView();
    }

    private void render(@NonNull ConversationUiState state) {
        if (binding == null) {
            return;
        }
        if (state.getThread() != null) {
            ChatThread thread = state.getThread();
            String title = state.getThread().getContextTitle();
            binding.contextTitle.setText(title);
            otherUserId = ChatParticipantPolicy.otherUserId(
                    thread, viewModel.currentUserId());
            binding.threadAvatar.setEnabled(!otherUserId.isEmpty());
            renderConversationAvatar();
        } else {
            otherUserId = "";
            binding.contextTitle.setText("Conversation");
            ProfileAvatarRenderer.render(
                    binding.threadAvatar,
                    ProfileAvatarPolicy.DEFAULT,
                    "PropCycle Member");
            binding.threadAvatar.setEnabled(false);
            binding.threadAvatar.setContentDescription(null);
        }
        binding.contextCaption.setText(
                state.getThread() != null
                        && "lending".equals(state.getThread().getContextType())
                        ? "Lending item"
                        : "Marketplace listing");

        adapter.submitList(state.getMessages(), viewModel.currentUserId());
        if (!state.getMessages().isEmpty()) {
            binding.messageList.scrollToPosition(state.getMessages().size() - 1);
        }
        boolean hasMessages = !state.getMessages().isEmpty();
        binding.loadingIndicator.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        binding.emptyText.setVisibility(
                !state.isLoading()
                                && !hasMessages
                                && state.getErrorMessage() == null
                                && !state.isConfigurationRequired()
                        ? View.VISIBLE
                        : View.GONE);

        String status = state.getErrorMessage();
        if (status == null && state.isFromCache()) {
            status = "Offline - showing cached messages. Sending requires a connection.";
        }
        binding.statusText.setText(status == null ? "" : status);
        binding.statusText.setVisibility(status == null ? View.GONE : View.VISIBLE);

        boolean canSend = state.getThread() != null
                && !state.isSending()
                && !state.isConfigurationRequired();
        binding.messageComposer.setEnabled(canSend);
        binding.primaryAction.setEnabled(canSend);
        binding.primaryAction.setAlpha(canSend ? 1f : 0.45f);
    }

    private void renderConversationAvatar() {
        if (binding == null || otherUserId.isEmpty()) {
            return;
        }
        boolean matchingProfile = otherProfile != null
                && otherUserId.equals(otherProfile.getUserId());
        String displayName = matchingProfile
                ? otherProfile.getDisplayName() : "PropCycle Member";
        String avatarKey = matchingProfile
                ? otherProfile.getAvatarKey() : ProfileAvatarPolicy.DEFAULT;
        ProfileAvatarRenderer.render(binding.threadAvatar, avatarKey, displayName);
        binding.threadAvatar.setContentDescription("Open " + displayName + "'s profile");
    }

    private void openOtherUserProfile() {
        if (otherUserId.isEmpty()) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("userId", otherUserId);
        ScreenNavigation.navigateAuthenticated(this, R.id.profileFragment, arguments);
    }
}
