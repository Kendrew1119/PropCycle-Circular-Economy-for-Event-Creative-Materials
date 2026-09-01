package com.propcycle.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.chat.ChatParticipantPolicy;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;
import com.propcycle.app.data.profile.PublicProfile;
import com.propcycle.app.data.profile.PublicProfileRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns the bounded real-time conversation-list listener. */
public final class MessagesViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final PublicProfileRepository profileRepository;
    private final MutableLiveData<MessagesUiState> state =
            new MutableLiveData<>(MessagesUiState.loading());
    private ChatRepository.Subscription subscription = ChatRepository.Subscription.NONE;
    private final MutableLiveData<Map<String, PublicProfile>> publicProfiles =
            new MutableLiveData<>(Collections.emptyMap());
    private final Set<String> requestedProfileIds = new HashSet<>();
    private int listenerGeneration;

    public MessagesViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
        profileRepository = new PublicProfileRepository(application);
    }

    @NonNull
    public LiveData<MessagesUiState> getState() {
        return state;
    }

    @NonNull
    public LiveData<Map<String, PublicProfile>> getPublicProfiles() {
        return publicProfiles;
    }

    @NonNull
    public String currentUserId() {
        String userId = repository.currentUserId();
        return userId == null ? "" : userId;
    }

    public void start() {
        stop();
        if (!repository.isConfigured()) {
            state.setValue(new MessagesUiState(
                    false,
                    true,
                    false,
                    FirebaseEnvironment.SETUP_MESSAGE,
                    Collections.emptyList()));
            return;
        }
        if (repository.currentUserId() == null) {
            state.setValue(new MessagesUiState(
                    false,
                    false,
                    false,
                    "Sign in to view your conversations.",
                    Collections.emptyList()));
            return;
        }

        MessagesUiState current = state.getValue();
        List<ChatThread> previous = current == null
                ? Collections.emptyList()
                : current.getThreads();
        state.setValue(new MessagesUiState(
                previous.isEmpty(), false, false, null, previous));
        int generation = ++listenerGeneration;
        subscription = repository.listenToThreads(new ChatRepository.SnapshotCallback<>() {
            @Override
            public void onData(@NonNull List<ChatThread> value, boolean fromCache) {
                if (generation != listenerGeneration) {
                    return;
                }
                state.setValue(new MessagesUiState(false, false, fromCache, null, value));
                loadPublicProfiles(generation, value);
            }

            @Override
            public void onError(@NonNull Exception error) {
                if (generation != listenerGeneration) {
                    return;
                }
                MessagesUiState currentState = state.getValue();
                List<ChatThread> retained = currentState == null
                        ? Collections.emptyList()
                        : currentState.getThreads();
                state.setValue(new MessagesUiState(
                        false, false, false, ChatUiError.message(error), retained));
            }
        });
    }

    private void loadPublicProfiles(int generation, @NonNull List<ChatThread> threads) {
        String currentUserId = currentUserId();
        if (currentUserId.isEmpty()) {
            return;
        }
        Set<String> neededIds = new HashSet<>();
        for (ChatThread thread : threads) {
            String otherUserId = ChatParticipantPolicy.otherUserId(thread, currentUserId);
            if (!otherUserId.isEmpty()) {
                neededIds.add(otherUserId);
            }
        }
        Map<String, PublicProfile> current = publicProfiles.getValue();
        Map<String, PublicProfile> retained = new HashMap<>();
        if (current != null) {
            for (String userId : neededIds) {
                PublicProfile profile = current.get(userId);
                if (profile != null) {
                    retained.put(userId, profile);
                }
            }
        }
        publicProfiles.setValue(Collections.unmodifiableMap(retained));
        requestedProfileIds.retainAll(neededIds);
        for (String userId : new ArrayList<>(neededIds)) {
            if (retained.containsKey(userId) || !requestedProfileIds.add(userId)) {
                continue;
            }
            profileRepository.get(userId)
                    .addOnSuccessListener(profile -> {
                        if (generation != listenerGeneration) {
                            return;
                        }
                        Map<String, PublicProfile> updated = new HashMap<>();
                        Map<String, PublicProfile> available = publicProfiles.getValue();
                        if (available != null) {
                            updated.putAll(available);
                        }
                        updated.put(userId, profile);
                        publicProfiles.setValue(Collections.unmodifiableMap(updated));
                    })
                    .addOnFailureListener(error -> {
                        if (generation != listenerGeneration) {
                            return;
                        }
                        Map<String, PublicProfile> updated = new HashMap<>();
                        Map<String, PublicProfile> available = publicProfiles.getValue();
                        if (available != null) {
                            updated.putAll(available);
                        }
                        updated.put(userId, new PublicProfile(
                                userId,
                                "PropCycle Member",
                                ProfileAvatarPolicy.DEFAULT));
                        publicProfiles.setValue(Collections.unmodifiableMap(updated));
                    });
        }
    }

    public void stop() {
        listenerGeneration++;
        subscription.remove();
        subscription = ChatRepository.Subscription.NONE;
        requestedProfileIds.clear();
    }

    @Override
    protected void onCleared() {
        stop();
    }
}
