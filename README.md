# Empik Coupon Management Service 🎟️

## Wstęp
Projekt realizuje system zarządzania kuponami rabatowymi REST API. Zamiast budować skomplikowaną architekturę rozproszoną ("overengineering") dla prostej domeny, rozwiązanie skupia się na: optymalnym wykorzystaniu bazy relacyjnej, bezpiecznej obsłudze współbieżności i wysokiej dostępności przy zakładanej skali (3000 zapytań na sekundę).

## Decyzje Architektoniczne (Design Choices)

### 1. Współbieżność i Skalowalność (Rozwiązanie Race Conditions)
Zrezygnowano z pobierania encji do pamięci i stosowania blokowania optymistycznego (JPA `@Version`). Przy dużym, równoległym ruchu na jeden popularny kupon, doprowadziłoby to do lawiny wyjątków `OptimisticLockException` i zmusiło do implementacji kosztownych wydajnościowo mechanizmów "retry". 

Zamiast tego użyto **atomowej operacji na poziomie bazy danych**:
```sql
UPDATE coupons SET current_uses = current_uses + 1 WHERE code = :code AND current_uses < max_uses
```
Dzięki temu silnik PostgreSQL natywnie (poprzez row-level lock w trakcie modyfikacji) dba o spójność licznika. Aplikacja pozostaje całkowicie bezstanowa, a proces można horyzontalnie skalować, dodając kolejne instancje.

### 2. Architektura Hybrydowa i YAGNI (Brak Kafki)
Zgodnie z zasadą YAGNI (You Aren't Gonna Need It) nie wprowadzono w projekcie brokera wiadomości w głównym przepływie (Critical Path). Wymagania zadania narzucają synchroniczną odpowiedź HTTP z natychmiastowym statusem zużycia. Oczekiwanie na odpowiedź z konsumenta Kafki w głównym wątku serwera doprowadziłoby do drastycznego wzrostu opóźnień (Latency) oraz wyczerpania puli wątków (Thread Exhaustion). Zastosowano synchroniczny flow zoptymalizowany pod bazę relacyjną.

### 3. Zabezpieczenie Zewnętrznych Zależności (Resilience)
Integracja z zewnętrznym API weryfikującym Geo-IP została zrealizowana przy użyciu Spring `RestClient`. Celowo skonfigurowano krótkie timeouty połączenia i odczytu (2 sekundy). W przypadku awarii lub opóźnień zewnętrznego dostawcy, serwis zastosuje strategię *Fail-Fast*, odrzucając żądanie i chroniąc własną pulę wątków przed zapchaniem.

### 4. Gwarancja Unikalności Użycia (Wymaganie Opcjonalne)
Aby spełnić wymóg "jeden użytkownik = jedno użycie kuponu", stworzono encję asocjacyjną `CouponUsage` ze złożonym kluczem głównym. Próba wielokrotnego użycia tego samego kuponu przez to samo `userId` kończy się rzuceniem przez bazę wyjątku `DataIntegrityViolationException`. Błąd ten jest globalnie przechwytywany i mapowany na HTTP 409 Conflict. Zwalnia to serwis z konieczności wykonywania dodatkowych zapytań `SELECT` przed wstawieniem rekordu (unikanie dodatkowych race conditions).

## Stos Technologiczny
* **Java 25** (Wykorzystanie rekordów do niemutowalnych DTO)
* **Spring Boot 4.1.0** (Web, Data JPA, Validation)
* **PostgreSQL** + **Flyway** (Deklaratywne zarządzanie schematem)
* **Testcontainers** & **WireMock** (Wyizolowane środowiska integracyjne)

## Uruchomienie Projektu Lokalnie

### Wymagania
* Zainstalowany demon Docker / Docker Compose
* Dostęp do powłoki bash/terminala

### Krok 1: Testy Integracyjne (w tym test współbieżności)
Projekt zawiera zaawansowane testy integracyjne podnoszące bazę PostgreSQL w locie (Testcontainers) oraz symulujące jednoczesne żądania (wielowątkowy test z użyciem `ExecutorService`), aby dowieść odporności systemu na Race Conditions.
```bash
./mvnw clean verify
```

### Krok 2: Uruchomienie Bazy Danych
W głównym katalogu projektu podnieś instancję bazy:
```bash
docker compose up -d
```

### Krok 3: Start Aplikacji
Uruchom główny proces Spring Boota:
```bash
./mvnw spring-boot:run
```
Serwer uruchomi się na porcie `8080`. Aplikacja wykorzystuje plik konfiguracyjny `application.yml`. Przy starcie Flyway automatycznie zaaplikuje pliki migracyjne, przygotowując tabele w bazie `coupon_db`.

### Testy Obciążeniowe (Load Testing)
Do projektu dołączono skrypt `load_test_k6.js` dla narzędzia **k6**, weryfikujący zachowanie systemu przy ruchu rzędu 3000 zapytań na sekundę (zgodnie z wymaganiami). 

Uruchomienie testu:
`k6 run load_test_k6.js`

**Cel testu:** Skrypt celowo doprowadza do przeciążenia zewnętrznej usługi GeoIP, aby udowodnić skuteczność zaimplementowanej architektury *Resilience*. Weryfikuje on, czy w warunkach kaskadowej awarii (Cascading Failure) aplikacja poprawnie izoluje pule wątków bazy danych (HikariCP) od operacji I/O, stosując wzorzec *Fail-Fast* i utrzymując stabilność głównego procesu.