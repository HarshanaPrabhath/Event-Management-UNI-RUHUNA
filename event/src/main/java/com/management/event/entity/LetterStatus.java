package com.management.event.entity;

public enum LetterStatus {
    PENDING,
    PENDING_BOOKING,
    // Legacy terminal rejection (old flow). New flow never sets this.
    REJECTED,
    APPROVED,
    // New club flow: a rejection has sent the letter back to the club secretary for revision.
    RETURNED_TO_SECRETARY,
    // New club flow: permanently closed by the secretary or the club senior treasurer.
    CANCELLED
}
