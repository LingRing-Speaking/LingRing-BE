package com.lingring.domain.review.domain.quota;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Embeddable
@Getter
@NoArgsConstructor(access = PROTECTED)
public class FreeTicket {

    private static final int DAILY_LIMIT = 1;

    @ColumnDefault("0")
    @Column(name = "free_ticket", nullable = false)
    private int count;

    @Column(name = "free_reset_date")
    private LocalDate resetDate;

    private FreeTicket(final int count, final LocalDate resetDate) {
        this.count = count;
        this.resetDate = resetDate;
    }

    public static FreeTicket initial() {
        return new FreeTicket(DAILY_LIMIT, null);
    }

    public int remainingOn(final LocalDate today) {
        if (isStaleOn(today)) {
            return DAILY_LIMIT;
        }
        return count;
    }

    public boolean consumeOn(final LocalDate today) {
        refillIfStale(today);
        if (count <= 0) {
            return false;
        }
        this.count -= 1;
        return true;
    }

    private void refillIfStale(final LocalDate today) {
        if (isStaleOn(today)) {
            this.count = DAILY_LIMIT;
            this.resetDate = today;
        }
    }

    private boolean isStaleOn(final LocalDate today) {
        return resetDate == null || !resetDate.isEqual(today);
    }
}
