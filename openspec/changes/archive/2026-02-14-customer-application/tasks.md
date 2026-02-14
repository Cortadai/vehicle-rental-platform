1. Module Setup

---------------

* [x] 1.1 Create `customer-service/customer-application/pom.xml` — inherits from root parent POM, depends on `customer-domain` and `spring-tx`, packaging `jar`

* [x] 1.2 Add `customer-service/customer-application` to root POM modules list (after `customer-domain`, before `customer-infrastructure`)

* [x] 1.3 Add `customer-application` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`

* [x] 1.4 Create source directory structure under `customer-service/customer-application/src/main/java/com/vehiclerental/customer/application/` with subdirectories: `port/input/`, `port/output/`, `service/`, `dto/command/`, `dto/response/`, `mapper/`, `exception/`

* [x] 1.5 Create test directory structure under `customer-service/customer-application/src/test/java/com/vehiclerental/customer/application/` with subdirectories: `service/`, `exception/`
2. DTOs (Commands and Responses)

--------------------------------

* [x] 2.1 Create `CreateCustomerCommand.java` — record with firstName, lastName, email, phone (nullable)

* [x] 2.2 Create `GetCustomerCommand.java` — record with customerId (String)

* [x] 2.3 Create `SuspendCustomerCommand.java` — record with customerId (String)

* [x] 2.4 Create `ActivateCustomerCommand.java` — record with customerId (String)

* [x] 2.5 Create `DeleteCustomerCommand.java` — record with customerId (String)

* [x] 2.6 Create `CustomerResponse.java` — record with customerId, firstName, lastName, email, phone, status, createdAt
3. Exception

------------

* [x] 3.1 Write `CustomerNotFoundExceptionTest.java` — tests for: message contains customer ID, extends RuntimeException (NOT CustomerDomainException)

* [x] 3.2 Create `CustomerNotFoundException.java` — extends RuntimeException, message includes customer ID, lives in `application/exception/`
4. Output Port

--------------

* [x] 4.1 Create `CustomerDomainEventPublisher.java` — interface with `publish(List<DomainEvent>)` method
5. Input Ports

--------------

* [x] 5.1 Create `CreateCustomerUseCase.java` — interface with `CustomerResponse execute(CreateCustomerCommand)`

* [x] 5.2 Create `GetCustomerUseCase.java` — interface with `CustomerResponse execute(GetCustomerCommand)`

* [x] 5.3 Create `SuspendCustomerUseCase.java` — interface with `void execute(SuspendCustomerCommand)`

* [x] 5.4 Create `ActivateCustomerUseCase.java` — interface with `void execute(ActivateCustomerCommand)`

* [x] 5.5 Create `DeleteCustomerUseCase.java` — interface with `void execute(DeleteCustomerCommand)`
6. Mapper

---------

* [x] 6.1 Write `CustomerApplicationMapperTest.java` — test for: toResponse maps all 7 fields correctly from Customer aggregate

* [x] 6.2 Create `CustomerApplicationMapper.java` — plain Java class, `toResponse(Customer)` returns CustomerResponse
7. Application Service

----------------------

* [x] 7.1 Write `CustomerApplicationServiceTest.java` — tests for: create flow (save + publish + clearDomainEvents + return response), get found returns response, get not found throws CustomerNotFoundException, suspend flow (load + suspend + save + publish + clearDomainEvents), suspend not found throws, activate flow (load + activate + save + publish + clearDomainEvents), activate not found throws, delete flow (load + delete + save + publish + clearDomainEvents), delete not found throws, no @Service or @Component annotations on class, @Transactional on write methods, @Transactional(readOnly=true) on get method

* [x] 7.2 Create `CustomerApplicationService.java` — implements all 5 input ports, constructor injection (repository, eventPublisher, mapper), `@Transactional` on writes, `@Transactional(readOnly = true)` on reads, zero business logic
8. Verification

---------------

* [x] 8.1 Run `mvn clean install` from `customer-service/customer-application/` — compile + all tests pass
* [x] 8.2 Verify no `@Service` or `@Component` annotations: `grep -r "@Service\|@Component" customer-service/customer-application/src/main/` returns nothing
* [x] 8.3 Verify `customer-domain` still compiles cleanly: `mvn clean compile` from `customer-service/customer-domain/`


