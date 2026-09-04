package com.propcycle.app.data.lending;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class LendingRequestActionPolicyTest {

    @Test
    public void pendingActionsMatchOwnerAndBorrowerLifecycle() {
        LendingRequest request = request("pending", false);

        assertEquals(
                List.of(
                        LendingRequestActionPolicy.Action.APPROVE,
                        LendingRequestActionPolicy.Action.REJECT),
                LendingRequestActionPolicy.availableActions(request, "owner"));
        assertEquals(
                List.of(LendingRequestActionPolicy.Action.CANCEL),
                LendingRequestActionPolicy.availableActions(request, "borrower"));
        assertTrue(LendingRequestActionPolicy.availableActions(request, "outsider").isEmpty());
    }

    @Test
    public void activeReturnActionsMatchExistingNotificationRules() {
        LendingRequest awaitingBorrower = request("active", false);
        LendingRequest awaitingOwner = request("active", true);

        assertEquals(
                List.of(LendingRequestActionPolicy.Action.REPORT_RETURN),
                LendingRequestActionPolicy.availableActions(awaitingBorrower, "borrower"));
        assertEquals(
                List.of(LendingRequestActionPolicy.Action.CONFIRM_RETURN),
                LendingRequestActionPolicy.availableActions(awaitingOwner, "owner"));
    }

    @Test
    public void returnedCanOnlyBeRatedByBorrower() {
        LendingRequest request = request("returned", true);

        assertEquals(
                List.of(LendingRequestActionPolicy.Action.RATE),
                LendingRequestActionPolicy.availableActions(request, "borrower"));
        assertTrue(LendingRequestActionPolicy.availableActions(request, "owner").isEmpty());
    }

    @Test
    public void chatRetryOnlySelectsARelevantBorrowerRequest() {
        assertTrue(LendingRequestActionPolicy.isRelevantForBorrower(request("pending", false), "borrower"));
        assertTrue(!LendingRequestActionPolicy.isRelevantForBorrower(request("cancelled", false), "borrower"));
        assertTrue(!LendingRequestActionPolicy.isRelevantForBorrower(request("rejected", false), "borrower"));
    }

    private static LendingRequest request(String status, boolean returnReported) {
        LendingRequest request = new LendingRequest();
        request.setId("request-one");
        request.setItemId("item-one");
        request.setItemTitle("Shared item");
        request.setOwnerUid("owner");
        request.setBorrowerUid("borrower");
        request.setStatus(status);
        request.setReturnReported(returnReported);
        return request;
    }
}
