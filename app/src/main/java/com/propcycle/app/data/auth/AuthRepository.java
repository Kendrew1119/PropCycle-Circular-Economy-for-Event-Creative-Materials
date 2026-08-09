package com.propcycle.app.data.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

import java.util.HashMap;
import java.util.Map;

/** Firebase Authentication and the minimal Firestore account profile boundary. */
public final class AuthRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String DEFAULT_DISPLAY_NAME = "PropCycle Member";

    private final Context applicationContext;

    public AuthRepository(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
    }

    public boolean isConfigured() {
        return FirebaseEnvironment.isConfigured(applicationContext);
    }

    public void signIn(
            @NonNull String email,
            @NonNull String password,
            @NonNull Completion completion) {
        FirebaseAuth auth = FirebaseEnvironment.auth(applicationContext);
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(applicationContext);
        if (auth == null || firestore == null) {
            completion.onFailure(Failure.configurationRequired());
            return;
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        completion.onFailure(mapFailure(
                                task.getException(),
                                "Unable to sign in. Please try again."));
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        completion.onFailure(Failure.service(
                                "Sign in completed without an active account. Please try again."));
                        return;
                    }
                    ensureProfileExists(firestore, user, signOutOnFailure(auth, completion));
                });
    }

    public void register(
            @NonNull String displayName,
            @NonNull String email,
            @NonNull String password,
            @NonNull Completion completion) {
        FirebaseAuth auth = FirebaseEnvironment.auth(applicationContext);
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(applicationContext);
        if (auth == null || firestore == null) {
            completion.onFailure(Failure.configurationRequired());
            return;
        }

        String normalizedEmail = email.trim();
        String normalizedName = displayName.trim();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentEmail = currentUser.getEmail();
            if (currentEmail != null && currentEmail.equalsIgnoreCase(normalizedEmail)) {
                finishRegistration(
                        firestore,
                        currentUser,
                        normalizedName,
                        signOutOnFailure(auth, completion));
            } else {
                completion.onFailure(Failure.service(
                        "Sign out before registering a different account."));
            }
            return;
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        completion.onFailure(mapFailure(
                                task.getException(),
                                "Unable to create the account. Please try again."));
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        completion.onFailure(Failure.service(
                                "Account creation completed without an active account."));
                        return;
                    }
                    finishRegistration(
                            firestore,
                            user,
                            normalizedName,
                            signOutOnFailure(auth, completion));
                });
    }

    private static void finishRegistration(
            @NonNull FirebaseFirestore firestore,
            @NonNull FirebaseUser user,
            @NonNull String displayName,
            @NonNull Completion completion) {
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        user.updateProfile(request).addOnCompleteListener(profileTask -> {
            if (!profileTask.isSuccessful()) {
                Exception profileException = profileTask.getException();
                // Preserve the submitted name in Firestore even if the Auth profile update
                // fails. A later sign-in can repair the Auth display name from this document.
                writeRegistrationProfile(
                        firestore,
                        user.getUid(),
                        displayName,
                        new Completion() {
                            @Override
                            public void onSuccess() {
                                completion.onFailure(mapFailure(
                                        profileException,
                                        "The account was created, but profile setup failed. "
                                                + "Sign in to retry."));
                            }

                            @Override
                            public void onFailure(@NonNull Failure failure) {
                                completion.onFailure(failure);
                            }
                        });
                return;
            }
            writeRegistrationProfile(firestore, user.getUid(), displayName, completion);
        });
    }

    private static void writeRegistrationProfile(
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId,
            @NonNull String displayName,
            @NonNull Completion completion) {
        DocumentReference reference = firestore.collection(USERS_COLLECTION).document(userId);
        reference.get().addOnCompleteListener(readTask -> {
            if (!readTask.isSuccessful()) {
                completion.onFailure(mapFailure(
                        readTask.getException(),
                        "The account was created, but profile setup could not be checked. Sign in to retry."));
                return;
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("displayName", displayName);
            profile.put("updatedAt", FieldValue.serverTimestamp());

            if (readTask.getResult().exists()) {
                reference.update(profile).addOnCompleteListener(writeTask ->
                        completeProfileWrite(writeTask.isSuccessful(), writeTask.getException(), completion));
                return;
            }

            profile.put("createdAt", FieldValue.serverTimestamp());
            reference.set(profile).addOnCompleteListener(writeTask ->
                    completeProfileWrite(writeTask.isSuccessful(), writeTask.getException(), completion));
        });
    }

    private static void ensureProfileExists(
            @NonNull FirebaseFirestore firestore,
            @NonNull FirebaseUser user,
            @NonNull Completion completion) {
        DocumentReference reference = firestore.collection(USERS_COLLECTION).document(user.getUid());
        reference.get().addOnCompleteListener(readTask -> {
            if (!readTask.isSuccessful()) {
                completion.onFailure(mapFailure(
                        readTask.getException(),
                        "Signed in, but the profile could not be loaded."));
                return;
            }
            if (readTask.getResult().exists()) {
                String authDisplayName = user.getDisplayName();
                String storedDisplayName = readTask.getResult().getString("displayName");
                if ((authDisplayName == null || authDisplayName.trim().isEmpty())
                        && storedDisplayName != null
                        && !storedDisplayName.trim().isEmpty()) {
                    UserProfileChangeRequest repairRequest =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(storedDisplayName.trim())
                                    .build();
                    user.updateProfile(repairRequest).addOnCompleteListener(repairTask -> {
                        if (repairTask.isSuccessful()) {
                            completion.onSuccess();
                        } else {
                            completion.onFailure(mapFailure(
                                    repairTask.getException(),
                                    "Signed in, but the account display name could not be restored."));
                        }
                    });
                } else {
                    completion.onSuccess();
                }
                return;
            }

            String displayName = user.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = DEFAULT_DISPLAY_NAME;
            }
            Map<String, Object> profile = new HashMap<>();
            profile.put("displayName", displayName.trim());
            profile.put("createdAt", FieldValue.serverTimestamp());
            profile.put("updatedAt", FieldValue.serverTimestamp());
            reference.set(profile).addOnCompleteListener(writeTask ->
                    completeProfileWrite(writeTask.isSuccessful(), writeTask.getException(), completion));
        });
    }

    private static void completeProfileWrite(
            boolean successful,
            @Nullable Exception exception,
            @NonNull Completion completion) {
        if (successful) {
            completion.onSuccess();
        } else {
            completion.onFailure(mapFailure(
                    exception,
                    "The account is ready, but the profile could not be saved. Sign in to retry."));
        }
    }

    @NonNull
    private static Completion signOutOnFailure(
            @NonNull FirebaseAuth auth,
            @NonNull Completion delegate) {
        return new Completion() {
            @Override
            public void onSuccess() {
                delegate.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Failure failure) {
                auth.signOut();
                delegate.onFailure(failure);
            }
        };
    }

    private static Failure mapFailure(
            @Nullable Exception exception,
            @NonNull String fallbackMessage) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return Failure.service("An account already exists for this email. Sign in instead.");
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException
                || exception instanceof FirebaseAuthInvalidUserException) {
            return Failure.service("Email or password is incorrect.");
        }
        if (exception instanceof FirebaseTooManyRequestsException) {
            return Failure.service("Too many attempts. Wait a moment, then try again.");
        }
        if (exception instanceof FirebaseNetworkException) {
            return Failure.service("Unable to reach Firebase. Check your connection and try again.");
        }
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return Failure.service(
                        "Profile access was denied. Deploy the latest Firestore security rules.");
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return Failure.service(
                        "Firestore is unavailable. Check your connection and try again.");
            }
        }
        return Failure.service(fallbackMessage);
    }

    public interface Completion {
        void onSuccess();

        void onFailure(@NonNull Failure failure);
    }

    public enum FailureKind {
        CONFIGURATION_REQUIRED,
        SERVICE
    }

    public static final class Failure {

        private final FailureKind kind;
        private final String message;

        private Failure(@NonNull FailureKind kind, @NonNull String message) {
            this.kind = kind;
            this.message = message;
        }

        public static Failure configurationRequired() {
            return new Failure(
                    FailureKind.CONFIGURATION_REQUIRED,
                    FirebaseEnvironment.SETUP_MESSAGE);
        }

        public static Failure service(@NonNull String message) {
            return new Failure(FailureKind.SERVICE, message);
        }

        @NonNull
        public FailureKind getKind() {
            return kind;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }
}
