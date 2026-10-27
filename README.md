# Healthcare Appointment Scheduling and Lab Result Pipeline

Spring Boot 3 / Java 21 backend with a small web UI.

| PPT objective | Where it lives |
| --- | --- |
| Async booking with ActiveMQ, instant HTTP 202 | `booking/BookingController`, `BookingWorker`, queue `appointmentQueue` |
| Chunk-oriented Spring Batch pipeline | `lab/LabBatchConfig` (reader, `LabProcessor`, JPA writer, chunk size 50, skip listener) |
| REST and SOAP together | REST under `/api`, mock insurance SOAP service at `/ws` (WSDL: `/ws/insurance.wsdl`), client in `insurance/InsuranceClient` |
| Optimistic locking | `Slot.version` (`@Version`), `BookingService.book` |
| Critical alerts on a priority topic | `LabProcessor` publishes to topic `labAlerts`, consumed by `AlertService` |
| Entity inheritance, named HQL query | `LabResult` / `BloodTestResult` / `UrineTestResult`, `@NamedQuery LabResult.findBySeverity` |

## Run

```bash
mvn spring-boot:run                                   # H2 in memory, embedded ActiveMQ, http://localhost:8080
mvn spring-boot:run -Dspring-boot.run.profiles=mysql  # MySQL on localhost:3306 (DB_USER, DB_PASSWORD env vars)
mvn test                                              # concurrency and batch tests
```

Insurance ids starting with `INV` are refused by the SOAP service; ids starting with `SLOW` answer after 1.5 s.

## Key endpoints

- `POST /api/appointments` returns 202 with a reference id; `GET /api/appointments/{ref}` shows progress
- `POST /api/demo/race?slotId=&count=` fires many requests at one slot
- `POST /api/lab/run` runs the batch (optional CSV upload); `GET /api/lab/status`, `/results`, `/alerts`
