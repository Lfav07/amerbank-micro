package com.amerbank.customer.customer.dto;

import lombok.Getter;

@Getter
public class CustomerDeletedEvent {
    private Long userId;

    public CustomerDeletedEvent() {}

    public CustomerDeletedEvent(Long userId) {
        this.userId = userId;
    }

}
