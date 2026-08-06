import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reference implementation of the "Aggregate Invariant" pattern (see ADR-002):
 * a set of child records may never, together, exceed a value fixed on their
 * parent. The check sums every existing child, including the new one being
 * added, and rejects the addition if the total would exceed the commitment.
 *
 * IMPORTANT -- what this sample deliberately does NOT show:
 * In a real system, the "sum existing children, then write the new one" logic
 * below must run inside a single transaction with a lock (or an equivalent
 * concurrency guard) held on the parent for the duration of both steps.
 * Without that, two concurrent calls to tryAddInvoice() against the same
 * PurchaseCommitment can each read a total that doesn't yet include the
 * other, and both can pass the check -- together violating the invariant
 * that neither violated alone. This is the concurrency gap discussed
 * explicitly in ADR-002's trade-offs and in Lessons Learned, Section 2.
 * This sample is single-threaded on purpose, to keep the invariant check
 * itself the only thing being demonstrated.
 */
public class FinancialInvariantValidator {

    static class InvariantViolationException extends RuntimeException {
        InvariantViolationException(String message) { super(message); }
    }

    record Invoice(String id, BigDecimal amount) {}

    static class PurchaseCommitment {
        final String id;
        final BigDecimal committedValue;
        final List<Invoice> invoices = new ArrayList<>();

        PurchaseCommitment(String id, BigDecimal committedValue) {
            this.id = id;
            this.committedValue = committedValue;
        }

        BigDecimal currentTotal() {
            return invoices.stream()
                    .map(Invoice::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * The invariant check itself: sum what's already there, add the
         * candidate, and reject if the result would exceed the commitment.
         * This runs synchronously, before the invoice is accepted -- not
         * after, and not on a delay. See ADR-002 for why.
         */
        void tryAddInvoice(Invoice candidate) {
            BigDecimal prospectiveTotal = currentTotal().add(candidate.amount());
            if (prospectiveTotal.compareTo(committedValue) > 0) {
                throw new InvariantViolationException(
                        "Rejected " + candidate.id() + " ($" + candidate.amount() + "): " +
                        "existing total $" + currentTotal() + " + this invoice would reach $" +
                        prospectiveTotal + ", exceeding the committed value of $" + committedValue);
            }
            invoices.add(candidate);
        }
    }

    public static void main(String[] args) {
        PurchaseCommitment purchase = new PurchaseCommitment("P-1", new BigDecimal("10000.00"));

        System.out.println("Committed value: $" + purchase.committedValue);
        System.out.println();

        attempt(purchase, new Invoice("I-1", new BigDecimal("4000.00")));
        attempt(purchase, new Invoice("I-2", new BigDecimal("6000.00")));

        System.out.println();
        System.out.println("Running total is now exactly at the commitment: $" + purchase.currentTotal());
        System.out.println("A third invoice, even for $1, should now be rejected:");
        System.out.println();

        attempt(purchase, new Invoice("I-3", new BigDecimal("1.00")));

        System.out.println();
        System.out.println("Note: I-3's rejection has nothing to do with $1 being an invalid amount.");
        System.out.println("The same $1 invoice against a purchase with remaining headroom would be accepted --");
        System.out.println("its validity was never a property of the number by itself. See Business Workflows, Section 3.");
    }

    private static void attempt(PurchaseCommitment purchase, Invoice invoice) {
        try {
            purchase.tryAddInvoice(invoice);
            System.out.println("ACCEPTED " + invoice.id() + " ($" + invoice.amount() +
                    ") -- running total now $" + purchase.currentTotal());
        } catch (InvariantViolationException e) {
            System.out.println("REJECTED " + invoice.id() + ": " + e.getMessage());
        }
    }
}
