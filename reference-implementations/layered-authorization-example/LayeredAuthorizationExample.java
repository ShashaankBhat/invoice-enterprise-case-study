import java.util.Set;

/**
 * Reference implementation of the "Coarse-Then-Fine Authorization" pattern
 * (see ADR-005): every action is authorized through two independent
 * questions, always in this order.
 *
 * Question 1 -- Eligibility: can a user with this role reach this feature
 * at all? Cheap, coarse, and resolved without loading any specific record.
 *
 * Question 2 -- Entitlement: does this specific user have a legitimate
 * relationship to this specific record? Narrower, more precise, and only
 * evaluated once a specific record is already in hand.
 */
public class LayeredAuthorizationExample {

    enum Role { FINANCE_USER, FINANCE_ADMINISTRATOR, SYSTEM_ADMINISTRATOR }

    record User(String id, Role role) {}
    record PurchaseRecord(String id, String createdByUserId) {}

    static class AuthorizationResult {
        final boolean allowed;
        final String reason;
        private AuthorizationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
        static AuthorizationResult denied(String reason) { return new AuthorizationResult(false, reason); }
        static AuthorizationResult allowed(String reason) { return new AuthorizationResult(true, reason); }
    }

    private static final Set<Role> ROLES_ELIGIBLE_FOR_PURCHASE_VIEW =
            Set.of(Role.FINANCE_USER, Role.FINANCE_ADMINISTRATOR, Role.SYSTEM_ADMINISTRATOR);

    /**
     * Question 1: Eligibility. Cheap -- no PurchaseRecord is loaded here.
     * This check alone answers "can this role reach this feature at all,"
     * nothing more.
     */
    static AuthorizationResult checkEligibility(User user) {
        if (!ROLES_ELIGIBLE_FOR_PURCHASE_VIEW.contains(user.role())) {
            return AuthorizationResult.denied(
                    "Eligibility failed: role " + user.role() + " cannot reach this feature at all.");
        }
        return AuthorizationResult.allowed("Eligibility passed for role " + user.role() + ".");
    }

    /**
     * Question 2: Entitlement. Only ever called after Eligibility has
     * already passed, and only once a specific record is already loaded.
     * A System Administrator is entitled to everything; anyone else is
     * entitled only to records they created.
     */
    static AuthorizationResult checkEntitlement(User user, PurchaseRecord record) {
        if (user.role() == Role.SYSTEM_ADMINISTRATOR) {
            return AuthorizationResult.allowed("Entitlement passed: System Administrator.");
        }
        if (record.createdByUserId().equals(user.id())) {
            return AuthorizationResult.allowed("Entitlement passed: user created this record.");
        }
        return AuthorizationResult.denied(
                "Entitlement failed: user " + user.id() + " has no relationship to record " + record.id() + ".");
    }

    /** The two checks, always run in this order, never just one. */
    static AuthorizationResult authorize(User user, PurchaseRecord record) {
        AuthorizationResult eligibility = checkEligibility(user);
        if (!eligibility.allowed) {
            return eligibility;
        }
        return checkEntitlement(user, record);
    }

    public static void main(String[] args) {
        User financeUserA = new User("user-a", Role.FINANCE_USER);
        User financeUserB = new User("user-b", Role.FINANCE_USER);
        User sysAdmin = new User("admin-1", Role.SYSTEM_ADMINISTRATOR);
        PurchaseRecord recordOwnedByA = new PurchaseRecord("P-1", "user-a");

        System.out.println("--- Scenario 1: owner viewing their own record ---");
        report(authorize(financeUserA, recordOwnedByA));

        System.out.println();
        System.out.println("--- Scenario 2: a different Finance User, same role, wrong record ---");
        System.out.println("Eligibility alone cannot catch this -- both users hold the exact same role.");
        report(authorize(financeUserB, recordOwnedByA));

        System.out.println();
        System.out.println("--- Scenario 3: a System Administrator, entitled to everything ---");
        report(authorize(sysAdmin, recordOwnedByA));

        System.out.println();
        System.out.println("--- Scenario 4: what a role-only check would have missed ---");
        System.out.println("If this system only ran checkEligibility() and skipped checkEntitlement(),");
        System.out.println("Scenario 2 would have been silently ALLOWED -- financeUserB holds an eligible");
        System.out.println("role and nothing about a role alone reveals it isn't THIS user's record.");
        System.out.println("Eligibility result for user-b: " + describe(checkEligibility(financeUserB)));
    }

    private static void report(AuthorizationResult result) {
        System.out.println((result.allowed ? "ALLOWED" : "DENIED") + " -- " + result.reason);
    }

    private static String describe(AuthorizationResult result) {
        return (result.allowed ? "would pass" : "would fail") + " (" + result.reason + ")";
    }
}
