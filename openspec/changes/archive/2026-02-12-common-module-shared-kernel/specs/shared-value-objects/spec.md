# shared-value-objects

## Purpose

Provides an immutable Money value object with currency-safe arithmetic for use across all service domain layers.

## Requirement: Money construction and validation

Money SHALL be a Java record with `BigDecimal amount` and `Currency currency` (java.util.Currency).

### Scenario: Valid construction

- WHEN Money is constructed with a valid non-negative amount and a valid currency
- THEN the instance SHALL be created successfully

### Scenario: Null amount rejected

- WHEN Money is constructed with a null amount
- THEN it SHALL throw NullPointerException or IllegalArgumentException

### Scenario: Null currency rejected

- WHEN Money is constructed with a null currency
- THEN it SHALL throw NullPointerException or IllegalArgumentException

### Scenario: Negative amount rejected

- WHEN Money is constructed with a negative amount
- THEN it SHALL throw IllegalArgumentException

## Requirement: Money scale normalization

Money SHALL normalize the amount to 2 decimal places (RoundingMode.HALF_UP) during construction.

### Scenario: Scale normalization on construction

- WHEN Money is constructed with amount 10.5 and currency EUR
- THEN the stored amount SHALL be 10.50
- AND two Money instances with amounts 10.5 and 10.50 and same currency SHALL be equal

## Requirement: Money arithmetic operations

All arithmetic operations SHALL return new Money instances. The original SHALL remain unchanged.

### Scenario: Add with same currency

- WHEN `add(Money other)` is called with the same currency
- THEN it SHALL return a new Money with the sum of both amounts and the same currency

### Scenario: Subtract with same currency and non-negative result

- WHEN `subtract(Money other)` is called with the same currency and the result is >= 0
- THEN it SHALL return a new Money with the difference and the same currency

### Scenario: Subtract resulting in negative

- WHEN `subtract(Money other)` is called and the result would be negative
- THEN it SHALL throw IllegalArgumentException

### Scenario: Multiply by non-negative int

- WHEN `multiply(int factor)` is called with a non-negative factor
- THEN it SHALL return a new Money with amount multiplied by factor and the same currency

### Scenario: Multiply by negative factor

- WHEN `multiply(int factor)` is called with a negative factor
- THEN it SHALL throw IllegalArgumentException

### Scenario: Currency mismatch on add or subtract

- WHEN `add()` or `subtract()` is called with a Money of different currency
- THEN it SHALL throw IllegalArgumentException

## Requirement: Money immutability

- WHEN any arithmetic operation is performed
- THEN the original Money instance SHALL NOT be modified (guaranteed by record semantics)

## Requirement: Money equality

- WHEN two Money instances have the same amount and same currency
- THEN equals() SHALL return true (provided by record semantics, enabled by scale normalization)

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.common.domain.vo` SHALL import any type from `org.springframework.*`.
