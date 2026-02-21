package com.vehiclerental.payment.application.service;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.application.dto.command.GetPaymentCommand;
import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.dto.command.RefundPaymentCommand;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;
import com.vehiclerental.payment.application.exception.PaymentNotFoundException;
import com.vehiclerental.payment.application.mapper.PaymentApplicationMapper;
import com.vehiclerental.payment.application.port.output.PaymentDomainEventPublisher;
import com.vehiclerental.payment.application.port.output.PaymentGateway;
import com.vehiclerental.payment.application.port.output.PaymentGatewayResult;
import com.vehiclerental.payment.domain.event.PaymentCompletedEvent;
import com.vehiclerental.payment.domain.event.PaymentFailedEvent;
import com.vehiclerental.payment.domain.event.PaymentRefundedEvent;
import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.PaymentStatus;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import com.vehiclerental.payment.domain.port.output.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentDomainEventPublisher eventPublisher;

    @Mock
    private PaymentGateway paymentGateway;

    private final PaymentApplicationMapper mapper = new PaymentApplicationMapper();

    private PaymentApplicationService service;

    private static final UUID PAYMENT_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID RESERVATION_UUID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
    private static final UUID CUSTOMER_UUID = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");
    private static final String PAYMENT_ID_STR = PAYMENT_UUID.toString();
    private static final String RESERVATION_ID_STR = RESERVATION_UUID.toString();
    private static final String CUSTOMER_ID_STR = CUSTOMER_UUID.toString();

    @BeforeEach
    void setUp() {
        service = new PaymentApplicationService(paymentRepository, eventPublisher, paymentGateway, mapper);
    }

    @Nested
    class ProcessPayment {

        @Test
        void chargeSucceeds_savesCompletedPaymentAndPublishesCompletedEvent() {
            var command = new ProcessPaymentCommand(RESERVATION_ID_STR, CUSTOMER_ID_STR,
                    new BigDecimal("150.00"), "USD");
            when(paymentRepository.findByReservationId(any(ReservationId.class)))
                    .thenReturn(Optional.empty());
            when(paymentGateway.charge(any(Money.class)))
                    .thenReturn(new PaymentGatewayResult(true, List.of()));
            // save returns a reconstructed Payment (no events) to test original-aggregate publish
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment original = invocation.getArgument(0);
                return Payment.reconstruct(
                        original.getId(), original.getReservationId(), original.getCustomerId(),
                        original.getAmount(), original.getStatus(), original.getFailureMessages(),
                        original.getCreatedAt(), original.getUpdatedAt());
            });

            PaymentResponse response = service.execute(command);

            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID_STR);
            assertThat(response.customerId()).isEqualTo(CUSTOMER_ID_STR);
            assertThat(response.amount()).isEqualTo(new BigDecimal("150.00"));
            assertThat(response.currency()).isEqualTo("USD");

            // Verify events are from original aggregate (not the saved copy)
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            List<DomainEvent> publishedEvents = eventsCaptor.getValue();
            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.get(0)).isInstanceOf(PaymentCompletedEvent.class);
        }

        @Test
        void chargeFails_savesFailedPaymentAndPublishesFailedEvent() {
            var command = new ProcessPaymentCommand(RESERVATION_ID_STR, CUSTOMER_ID_STR,
                    new BigDecimal("150.00"), "USD");
            when(paymentRepository.findByReservationId(any(ReservationId.class)))
                    .thenReturn(Optional.empty());
            when(paymentGateway.charge(any(Money.class)))
                    .thenReturn(new PaymentGatewayResult(false, List.of("Card declined")));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment original = invocation.getArgument(0);
                return Payment.reconstruct(
                        original.getId(), original.getReservationId(), original.getCustomerId(),
                        original.getAmount(), original.getStatus(), original.getFailureMessages(),
                        original.getCreatedAt(), original.getUpdatedAt());
            });

            PaymentResponse response = service.execute(command);

            assertThat(response.status()).isEqualTo("FAILED");
            assertThat(response.failureMessages()).containsExactly("Card declined");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            List<DomainEvent> publishedEvents = eventsCaptor.getValue();
            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.get(0)).isInstanceOf(PaymentFailedEvent.class);
        }

        @Test
        void existingPayment_returnsWithoutChargingOrSaving() {
            var existingPayment = Payment.reconstruct(
                    new PaymentId(PAYMENT_UUID),
                    new ReservationId(RESERVATION_UUID),
                    new CustomerId(CUSTOMER_UUID),
                    new Money(new BigDecimal("150.00"), Currency.getInstance("USD")),
                    PaymentStatus.COMPLETED, List.of(),
                    Instant.now(), Instant.now());
            when(paymentRepository.findByReservationId(new ReservationId(RESERVATION_UUID)))
                    .thenReturn(Optional.of(existingPayment));

            var command = new ProcessPaymentCommand(RESERVATION_ID_STR, CUSTOMER_ID_STR,
                    new BigDecimal("150.00"), "USD");
            PaymentResponse response = service.execute(command);

            assertThat(response.paymentId()).isEqualTo(PAYMENT_ID_STR);
            assertThat(response.status()).isEqualTo("COMPLETED");

            verify(paymentGateway, never()).charge(any());
            verify(paymentRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        void eventsPublishedFromOriginalAggregate_notFromSaveResult() {
            var command = new ProcessPaymentCommand(RESERVATION_ID_STR, CUSTOMER_ID_STR,
                    new BigDecimal("100.00"), "USD");
            when(paymentRepository.findByReservationId(any(ReservationId.class)))
                    .thenReturn(Optional.empty());
            when(paymentGateway.charge(any(Money.class)))
                    .thenReturn(new PaymentGatewayResult(true, List.of()));
            // save returns a reconstructed Payment — its getDomainEvents() is empty
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment original = invocation.getArgument(0);
                return Payment.reconstruct(
                        original.getId(), original.getReservationId(), original.getCustomerId(),
                        original.getAmount(), original.getStatus(), original.getFailureMessages(),
                        original.getCreatedAt(), original.getUpdatedAt());
            });

            service.execute(command);

            // The key assertion: events were published (non-empty list).
            // If events came from the saved copy (reconstruct), they would be empty.
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            assertThat(eventsCaptor.getValue()).isNotEmpty();
        }

        @Test
        void saveBeforePublish_publishBeforeClear() {
            var command = new ProcessPaymentCommand(RESERVATION_ID_STR, CUSTOMER_ID_STR,
                    new BigDecimal("100.00"), "USD");
            when(paymentRepository.findByReservationId(any(ReservationId.class)))
                    .thenReturn(Optional.empty());
            when(paymentGateway.charge(any(Money.class)))
                    .thenReturn(new PaymentGatewayResult(true, List.of()));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment original = invocation.getArgument(0);
                return Payment.reconstruct(
                        original.getId(), original.getReservationId(), original.getCustomerId(),
                        original.getAmount(), original.getStatus(), original.getFailureMessages(),
                        original.getCreatedAt(), original.getUpdatedAt());
            });

            service.execute(command);

            InOrder inOrder = inOrder(paymentRepository, eventPublisher);
            inOrder.verify(paymentRepository).save(any(Payment.class));
            inOrder.verify(eventPublisher).publish(any());
        }
    }

    @Nested
    class RefundPayment {

        @Test
        void refundsCompletedPaymentAndPublishesRefundedEvent() {
            var completedPayment = Payment.reconstruct(
                    new PaymentId(PAYMENT_UUID),
                    new ReservationId(RESERVATION_UUID),
                    new CustomerId(CUSTOMER_UUID),
                    new Money(new BigDecimal("150.00"), Currency.getInstance("USD")),
                    PaymentStatus.COMPLETED, List.of(),
                    Instant.now(), Instant.now());
            when(paymentRepository.findByReservationId(new ReservationId(RESERVATION_UUID)))
                    .thenReturn(Optional.of(completedPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment original = invocation.getArgument(0);
                return Payment.reconstruct(
                        original.getId(), original.getReservationId(), original.getCustomerId(),
                        original.getAmount(), original.getStatus(), original.getFailureMessages(),
                        original.getCreatedAt(), original.getUpdatedAt());
            });

            PaymentResponse response = service.execute(new RefundPaymentCommand(RESERVATION_ID_STR));

            assertThat(response.status()).isEqualTo("REFUNDED");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            List<DomainEvent> publishedEvents = eventsCaptor.getValue();
            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.get(0)).isInstanceOf(PaymentRefundedEvent.class);
        }

        @Test
        void throwsPaymentNotFoundExceptionWhenNotFound() {
            when(paymentRepository.findByReservationId(new ReservationId(RESERVATION_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new RefundPaymentCommand(RESERVATION_ID_STR)))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining(RESERVATION_ID_STR);
        }
    }

    @Nested
    class GetPayment {

        @Test
        void returnsResponseWhenPaymentFound() {
            var payment = Payment.reconstruct(
                    new PaymentId(PAYMENT_UUID),
                    new ReservationId(RESERVATION_UUID),
                    new CustomerId(CUSTOMER_UUID),
                    new Money(new BigDecimal("150.00"), Currency.getInstance("USD")),
                    PaymentStatus.COMPLETED, List.of(),
                    Instant.now(), Instant.now());
            when(paymentRepository.findById(new PaymentId(PAYMENT_UUID)))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = service.execute(new GetPaymentCommand(PAYMENT_ID_STR));

            assertThat(response.paymentId()).isEqualTo(PAYMENT_ID_STR);
            assertThat(response.status()).isEqualTo("COMPLETED");
        }

        @Test
        void throwsPaymentNotFoundExceptionWhenNotFound() {
            when(paymentRepository.findById(new PaymentId(PAYMENT_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new GetPaymentCommand(PAYMENT_ID_STR)))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining(PAYMENT_ID_STR);
        }
    }

    @Nested
    class AnnotationChecks {

        @Test
        void classHasNoServiceOrComponentAnnotation() {
            assertThat(PaymentApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(PaymentApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }

        @Test
        void writeMethodsAreTransactional() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", ProcessPaymentCommand.class, false);
            assertMethodIsTransactional("execute", RefundPaymentCommand.class, false);
        }

        @Test
        void getMethodIsReadOnlyTransactional() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", GetPaymentCommand.class, true);
        }

        private void assertMethodIsTransactional(String methodName, Class<?> paramType, boolean readOnly)
                throws NoSuchMethodException {
            Method method = PaymentApplicationService.class.getMethod(methodName, paramType);
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isEqualTo(readOnly);
        }
    }
}
