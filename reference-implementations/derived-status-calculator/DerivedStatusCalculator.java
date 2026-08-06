import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reference implementation of the "Computed State" pattern (see ADR-003).
 *
 * Neither PurchaseStatus nor InvoiceStatus is ever stored. Both are derived
 * fresh from source data every time they're asked for, which means there is
 * no separate field any write path could forget to update.
 */
public class DerivedStatusCalculator {

    enum PurchaseStatus { OPEN, PARTIALLY_INVOICED, FULLY_SETTLED }
    enum InvoiceStatus { PENDING, OVERDUE, SETTLED }

    record Invoice(String id, BigDecimal amount, boolean paid, LocalDate dueDate) {}
    record Purchase(String id, BigDecimal committedValue, List<Invoice> invoices) {}

    /**
     * Purchase status is RELATIONAL: it depends on the state of every invoice
     * tied to this purchase, not on any field of the Purchase record itself.
     */
    static PurchaseStatus purchaseStatus(Purchase purchase) {
        if (purchase.invoices().isEmpty()) {
            return PurchaseStatus.OPEN;
        }
        BigDecimal paidTotal = purchase.invoices().stream()
                .filter(Invoice::paid)
                .map(Invoice::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (paidTotal.compareTo(purchase.committedValue()) >= 0) {
            return PurchaseStatus.FULLY_SETTLED;
        }
        return PurchaseStatus.PARTIALLY_INVOICED;
    }

    /**
     * Invoice status is LOCAL: it depends only on the invoice's own fields
     * and today's date -- never on any other invoice.
     */
    static InvoiceStatus invoiceStatus(Invoice invoice, LocalDate today) {
        if (invoice.paid()) {
            return InvoiceStatus.SETTLED;
        }
        if (invoice.dueDate().isBefore(today)) {
            return InvoiceStatus.OVERDUE;
        }
        return InvoiceStatus.PENDING;
    }

    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 6, 15);

        System.out.println("--- Scenario 1: a purchase with no invoices yet ---");
        Purchase p1 = new Purchase("P-1", new BigDecimal("10000.00"), List.of());
        System.out.println("Purchase status: " + purchaseStatus(p1) + " (expected OPEN)");

        System.out.println();
        System.out.println("--- Scenario 2: one invoice, unpaid, not yet due ---");
        Invoice inv1 = new Invoice("I-1", new BigDecimal("4000.00"), false, today.plusDays(10));
        Purchase p2 = new Purchase("P-2", new BigDecimal("10000.00"), List.of(inv1));
        System.out.println("Purchase status: " + purchaseStatus(p2) + " (expected PARTIALLY_INVOICED)");
        System.out.println("Invoice status:  " + invoiceStatus(inv1, today) + " (expected PENDING)");

        System.out.println();
        System.out.println("--- Scenario 3: the same invoice, now past its due date ---");
        System.out.println("Note: no write happened. Only 'today' changed. The status still updates correctly,");
        System.out.println("because it was never stored -- there was nothing that needed updating.");
        LocalDate later = today.plusDays(20);
        System.out.println("Invoice status:  " + invoiceStatus(inv1, later) + " (expected OVERDUE)");

        System.out.println();
        System.out.println("--- Scenario 4: two invoices, fully paid, reaching the commitment ---");
        Invoice inv2a = new Invoice("I-2a", new BigDecimal("4000.00"), true, today.minusDays(5));
        Invoice inv2b = new Invoice("I-2b", new BigDecimal("6000.00"), true, today.minusDays(2));
        Purchase p3 = new Purchase("P-3", new BigDecimal("10000.00"), List.of(inv2a, inv2b));
        System.out.println("Purchase status: " + purchaseStatus(p3) + " (expected FULLY_SETTLED)");
        System.out.println("This status depends on TWO invoice records at once -- it is relational,");
        System.out.println("not a property of the Purchase record's own fields.");
    }
}
