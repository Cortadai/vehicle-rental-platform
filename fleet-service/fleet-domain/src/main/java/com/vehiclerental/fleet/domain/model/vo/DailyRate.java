package com.vehiclerental.fleet.domain.model.vo;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;

public record DailyRate(Money money) {

    public DailyRate {
        if (money == null) {
            throw new FleetDomainException("DailyRate money must not be null", "DAILY_RATE_NULL");
        }
        if (money.amount().signum() <= 0) {
            throw new FleetDomainException("DailyRate must be strictly positive", "DAILY_RATE_NOT_POSITIVE");
        }
    }
}
