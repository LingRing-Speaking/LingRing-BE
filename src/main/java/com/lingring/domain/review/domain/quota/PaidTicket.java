package com.lingring.domain.review.domain.quota;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Embeddable
@Getter
@NoArgsConstructor(access = PROTECTED)
public class PaidTicket {

    @ColumnDefault("0")
    @Column(name = "paid_ticket", nullable = false)
    private int count;

    private PaidTicket(final int count) {
        this.count = count;
    }

    public static PaidTicket empty() {
        return new PaidTicket(0);
    }

    public boolean consume() {
        if (count <= 0) {
            return false;
        }
        this.count -= 1;
        return true;
    }

    public void charge(final int amount) {
        this.count += amount;
    }
}
