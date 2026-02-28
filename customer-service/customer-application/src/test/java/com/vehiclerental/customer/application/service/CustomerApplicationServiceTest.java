package com.vehiclerental.customer.application.service;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.customer.application.dto.command.*;
import com.vehiclerental.customer.application.dto.response.CustomerResponse;
import com.vehiclerental.customer.application.exception.CustomerNotFoundException;
import com.vehiclerental.customer.application.mapper.CustomerApplicationMapper;
import com.vehiclerental.customer.application.port.output.CustomerDomainEventPublisher;
import com.vehiclerental.customer.domain.event.CustomerRejectedEvent;
import com.vehiclerental.customer.domain.event.CustomerValidatedEvent;
import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import com.vehiclerental.customer.domain.port.output.CustomerRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerDomainEventPublisher eventPublisher;

    private final CustomerApplicationMapper mapper = new CustomerApplicationMapper();

    private CustomerApplicationService service;

    private static final UUID CUSTOMER_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String CUSTOMER_ID_STR = CUSTOMER_UUID.toString();

    @BeforeEach
    void setUp() {
        service = new CustomerApplicationService(customerRepository, eventPublisher, mapper);
    }

    @Nested
    class CreateCustomer {

        @Test
        void savesCustomerAndPublishesEventsAndClearsEventsAndReturnsResponse() {
            var command = new CreateCustomerCommand("John", "Doe", "john@example.com", "+1234567890");
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CustomerResponse response = service.execute(command);

            var customerCaptor = ArgumentCaptor.forClass(Customer.class);
            InOrder inOrder = inOrder(customerRepository, eventPublisher);
            inOrder.verify(customerRepository).save(customerCaptor.capture());
            inOrder.verify(eventPublisher).publish(any());

            Customer savedCustomer = customerCaptor.getValue();
            assertThat(savedCustomer.getFirstName()).isEqualTo("John");
            assertThat(savedCustomer.getLastName()).isEqualTo("Doe");
            assertThat(savedCustomer.getEmail().value()).isEqualTo("john@example.com");

            assertThat(response).isNotNull();
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Doe");
            assertThat(response.email()).isEqualTo("john@example.com");
        }
    }

    @Nested
    class GetCustomer {

        @Test
        void returnsResponseWhenCustomerFound() {
            var customer = buildActiveCustomer();
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.of(customer));

            var command = new GetCustomerCommand(CUSTOMER_ID_STR);
            CustomerResponse response = service.execute(command);

            assertThat(response.customerId()).isEqualTo(CUSTOMER_ID_STR);
            assertThat(response.firstName()).isEqualTo("John");
        }

        @Test
        void throwsCustomerNotFoundExceptionWhenNotFound() {
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.empty());

            var command = new GetCustomerCommand(CUSTOMER_ID_STR);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(CUSTOMER_ID_STR);
        }
    }

    @Nested
    class SuspendCustomer {

        @Test
        void loadsAndSuspendsAndSavesAndPublishesEventsAndClearsEvents() {
            var customer = buildActiveCustomer();
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new SuspendCustomerCommand(CUSTOMER_ID_STR));

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);

            InOrder inOrder = inOrder(customerRepository, eventPublisher);
            inOrder.verify(customerRepository).findById(any());
            inOrder.verify(customerRepository).save(customer);
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void throwsCustomerNotFoundExceptionWhenNotFound() {
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new SuspendCustomerCommand(CUSTOMER_ID_STR)))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    class ActivateCustomer {

        @Test
        void loadsAndActivatesAndSavesAndPublishesEventsAndClearsEvents() {
            var customer = buildSuspendedCustomer();
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new ActivateCustomerCommand(CUSTOMER_ID_STR));

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);

            InOrder inOrder = inOrder(customerRepository, eventPublisher);
            inOrder.verify(customerRepository).findById(any());
            inOrder.verify(customerRepository).save(customer);
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void throwsCustomerNotFoundExceptionWhenNotFound() {
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new ActivateCustomerCommand(CUSTOMER_ID_STR)))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    class DeleteCustomer {

        @Test
        void loadsAndDeletesAndSavesAndPublishesEventsAndClearsEvents() {
            var customer = buildActiveCustomer();
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new DeleteCustomerCommand(CUSTOMER_ID_STR));

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);

            InOrder inOrder = inOrder(customerRepository, eventPublisher);
            inOrder.verify(customerRepository).findById(any());
            inOrder.verify(customerRepository).save(customer);
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void throwsCustomerNotFoundExceptionWhenNotFound() {
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new DeleteCustomerCommand(CUSTOMER_ID_STR)))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    class ValidateCustomer {

        private static final UUID RESERVATION_UUID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

        @Test
        @SuppressWarnings("unchecked")
        void publishesValidatedEventWhenCustomerExistsAndIsActive() {
            var customer = buildActiveCustomer();
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.of(customer));

            var command = new ValidateCustomerCommand(CUSTOMER_ID_STR, RESERVATION_UUID.toString());
            service.execute(command);

            var eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            List<DomainEvent> publishedEvents = eventsCaptor.getValue();

            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.get(0)).isInstanceOf(CustomerValidatedEvent.class);
            var validatedEvent = (CustomerValidatedEvent) publishedEvents.get(0);
            assertThat(validatedEvent.customerId()).isEqualTo(new CustomerId(CUSTOMER_UUID));
            assertThat(validatedEvent.reservationId()).isEqualTo(RESERVATION_UUID);

            verify(customerRepository, never()).save(any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void publishesRejectedEventWhenCustomerNotFound() {
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.empty());

            var command = new ValidateCustomerCommand(CUSTOMER_ID_STR, RESERVATION_UUID.toString());
            service.execute(command);

            var eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            List<DomainEvent> publishedEvents = eventsCaptor.getValue();

            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.get(0)).isInstanceOf(CustomerRejectedEvent.class);
            var rejectedEvent = (CustomerRejectedEvent) publishedEvents.get(0);
            assertThat(rejectedEvent.reservationId()).isEqualTo(RESERVATION_UUID);
            assertThat(rejectedEvent.failureMessages()).containsExactly("Customer not found: " + CUSTOMER_ID_STR);
        }

        @Test
        @SuppressWarnings("unchecked")
        void publishesRejectedEventWhenCustomerNotActive() {
            var customer = buildSuspendedCustomer();
            when(customerRepository.findById(new CustomerId(CUSTOMER_UUID)))
                    .thenReturn(Optional.of(customer));

            var command = new ValidateCustomerCommand(CUSTOMER_ID_STR, RESERVATION_UUID.toString());
            service.execute(command);

            var eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publish(eventsCaptor.capture());
            List<DomainEvent> publishedEvents = eventsCaptor.getValue();

            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.get(0)).isInstanceOf(CustomerRejectedEvent.class);
            var rejectedEvent = (CustomerRejectedEvent) publishedEvents.get(0);
            assertThat(rejectedEvent.reservationId()).isEqualTo(RESERVATION_UUID);
            assertThat(rejectedEvent.failureMessages()).containsExactly("Customer is not active, current status: SUSPENDED");

            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    class AnnotationChecks {

        @Test
        void classHasNoServiceOrComponentAnnotation() {
            assertThat(CustomerApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(CustomerApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }

        @Test
        void writeMethodsAreTransactional() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", CreateCustomerCommand.class, false);
            assertMethodIsTransactional("execute", SuspendCustomerCommand.class, false);
            assertMethodIsTransactional("execute", ActivateCustomerCommand.class, false);
            assertMethodIsTransactional("execute", DeleteCustomerCommand.class, false);
        }

        @Test
        void getMethodIsReadOnlyTransactional() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", GetCustomerCommand.class, true);
        }

        @Test
        void validateMethodIsTransactionalNotReadOnly() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", ValidateCustomerCommand.class, false);
        }

        private void assertMethodIsTransactional(String methodName, Class<?> paramType, boolean readOnly)
                throws NoSuchMethodException {
            Method method = CustomerApplicationService.class.getMethod(methodName, paramType);
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isEqualTo(readOnly);
        }
    }

    private Customer buildActiveCustomer() {
        return Customer.reconstruct(
                new CustomerId(CUSTOMER_UUID),
                "John", "Doe",
                new Email("john@example.com"),
                new PhoneNumber("+1234567890"),
                CustomerStatus.ACTIVE,
                Instant.parse("2024-01-15T10:30:00Z"));
    }

    private Customer buildSuspendedCustomer() {
        return Customer.reconstruct(
                new CustomerId(CUSTOMER_UUID),
                "John", "Doe",
                new Email("john@example.com"),
                new PhoneNumber("+1234567890"),
                CustomerStatus.SUSPENDED,
                Instant.parse("2024-01-15T10:30:00Z"));
    }
}
